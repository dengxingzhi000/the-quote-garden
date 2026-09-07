# tools/crawler/quotegarden.py
"""QuoteGarden 固定爬虫：分类发现 + HTML解析 + Postgres幂等入库."""
import argparse, hashlib, json, os, random, re, sys, time
from pathlib import Path
import requests
from bs4 import BeautifulSoup, Comment

BASE = "https://www.quotegarden.com"
UA = {"User-Agent": "DailyMindSeed/1.0"}
ROOT = Path(__file__).parent
PROGRESS = ROOT / "progress.json"

def normalize(raw: str | None):
    if raw is None:
        return None
    return re.sub(r"\s+", " ", raw.strip())

def sha256_hex(s: str) -> str:
    import hashlib as hl
    return hl.sha256(s.encode("utf-8")).hexdigest()

def clean_author(raw: str | None):
    if not raw:
        return None
    raw = re.sub(r"\s+", " ", raw.strip())
    first = raw.split(",")[0].strip()
    first = re.sub(r"\s*\(.*?\)\s*", "", first).strip()
    first = re.sub(r"\s*\d{3,4}.*$", "", first).strip()
    return first or None

def parse_category_html(html: str, category: str):
    soup = BeautifulSoup(html, "lxml")
    section = soup.select_one("div.quotes-section")
    if section is None:
        return [], ["no quotes-section"]
    for c in section.find_all(string=lambda t: isinstance(t, Comment)):
        c.extract()
    inner = section.decode_contents()
    blocks = re.split(r"(?:<br\s*/?\s*>[\s\n]*){2,}", inner, flags=re.I)
    out, skipped = [], []
    for b in blocks:
        text = BeautifulSoup(b, "lxml").get_text(separator="\n")
        text = text.replace("\xa0", " ").strip()
        if not text:
            continue
        if "~" not in text:
            skipped.append(text[:60])
            continue
        idx = text.rfind("~")
        content_raw, author_raw = text[:idx].strip(), text[idx + 1:].strip()
        content = normalize(content_raw.replace("\n", " "))
        if content is None or len(content) < 10:
            skipped.append(text[:60])
            continue
        author = clean_author(author_raw.split("\n")[0])
        out.append({"id": sha256_hex(content), "content": content, "author": author, "category": category})
    return out, skipped

def self_test() -> int:
    html = (ROOT / "fixtures" / "happiness_sample.html").read_text(encoding="utf-8")
    quotes, skipped = parse_category_html(html, "happiness")
    assert len(quotes) == 3, f"expected 3 got {len(quotes)}: {quotes}"
    assert quotes[0]["content"].startswith("Whenever you are sincerely pleased"), quotes[0]
    assert "Ralph Waldo Emerson" in (quotes[0]["author"] or ""), quotes[0]
    assert quotes[1]["author"] == "James Oppenheim", quotes[1]
    assert any("Chinese proverb" in (q["author"] or "") for q in quotes), quotes
    assert len(skipped) >= 1, "无~块应被跳过"
    print(f"self-test PASS: {len(quotes)} quotes, {len(skipped)} skipped")
    return 0

def fetch(url: str, retries: int = 3) -> str:
    last = None
    for i in range(retries):
        try:
            r = requests.get(url, headers=UA, timeout=20)
            r.raise_for_status()
            r.encoding = r.apparent_encoding or "utf-8"
            return r.text
        except Exception as e:
            last = e
            time.sleep(2 ** i + random.random())
    raise RuntimeError(f"fetch failed {url}: {last}")

def discover_categories(limit: int | None = None):
    html = fetch(BASE + "/")
    soup = BeautifulSoup(html, "lxml")
    slugs = []
    for a in soup.find_all("a", href=True):
        href = a["href"]
        m = re.fullmatch(r"/([a-z0-9\-]+)\.html", href)
        if not m:
            continue
        slug = m.group(1)
        if slug in ("search", "about", "contact", "index",
                      "whats-new", "privacy-policy", "terms-and-conditions"):
            continue
        if slug not in slugs:
            slugs.append(slug)
    return slugs[:limit] if limit else slugs

def load_progress():
    if PROGRESS.exists():
        return set(json.loads(PROGRESS.read_text(encoding="utf-8")))
    return set()

def save_progress(done: set):
    PROGRESS.write_text(json.dumps(sorted(done), ensure_ascii=False, indent=2), encoding="utf-8")

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--self-test", action="store_true")
    ap.add_argument("--limit", type=int, default=None)
    ap.add_argument("--all", action="store_true")
    ap.add_argument("--no-db", action="store_true", help="只解析不入库")
    args = ap.parse_args()
    if args.self_test:
        sys.exit(self_test())
    slugs = discover_categories(args.limit if not args.all else None)
    done = load_progress()
    all_quotes, failed = [], []
    for slug in slugs:
        if slug in done:
            continue
        try:
            html = fetch(f"{BASE}/{slug}.html")
            quotes, skipped = parse_category_html(html, slug)
            all_quotes.extend(quotes)
            (ROOT / "skipped.log").open("a", encoding="utf-8").write(f"{slug}: {len(quotes)} ok, {len(skipped)} skipped\n")
            done.add(slug)
            save_progress(done)
        except Exception as e:
            failed.append(f"{slug} {e}")
        time.sleep(1.0 + random.random() * 0.5)
    (ROOT / "quotes.jsonl").open("a", encoding="utf-8").write("".join(json.dumps(q, ensure_ascii=False) + "\n" for q in all_quotes))
    (ROOT / "failed.txt").open("a", encoding="utf-8").write("\n".join(failed) + ("\n" if failed else ""))
    print(f"crawled {len(all_quotes)} quotes, {len(failed)} failed")
    if not args.no_db and all_quotes:
        insert_db(all_quotes)

def insert_db(quotes):
    import psycopg2
    conn = psycopg2.connect(host=os.environ.get("PGHOST", "192.168.80.152"), port=int(os.environ.get("PGPORT", "5432")),
        dbname=os.environ.get("PGDATABASE", "quote_garden"), user=os.environ.get("PGUSER", os.environ.get("SPRING_DATASOURCE_USERNAME", "postgres")),
        password=os.environ.get("PGPASSWORD", os.environ.get("SPRING_DATASOURCE_PASSWORD", "")))
    import time as _t
    now = int(_t.time() * 1000)
    with conn, conn.cursor() as cur:
        for q in quotes:
            cur.execute("INSERT INTO quote(id, content, author, category, updated_at) VALUES (%s,%s,%s,%s,%s) ON CONFLICT(id) DO NOTHING",
                (q["id"], q["content"], q["author"], q["category"], now))
    conn.close()
    print(f"inserted {len(quotes)} (dedup by id)")

if __name__ == "__main__":
    main()
