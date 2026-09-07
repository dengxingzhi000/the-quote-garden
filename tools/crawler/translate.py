# tools/crawler/translate.py
"""MyMemory免费翻译回填：translation IS NULL分批 + 本地缓存."""
import argparse, hashlib, json, os, time
from pathlib import Path
import requests
CACHE = Path(__file__).parent / "translation_cache.json"

def load_cache():
    return json.loads(CACHE.read_text(encoding="utf-8")) if CACHE.exists() else {}

def save_cache(c):
    CACHE.write_text(json.dumps(c, ensure_ascii=False, indent=2), encoding="utf-8")

def key(content: str) -> str:
    return hashlib.sha256(content.encode("utf-8")).hexdigest()

def translate_one(text: str, cache: dict) -> str | None:
    k = key(text)
    if k in cache:
        return cache[k]
    try:
        r = requests.get("https://api.mymemory.translated.net/get",
            params={"q": text[:450], "langpair": "en|zh-CN"}, timeout=20)
        d = r.json()
        t = (d.get("responseData") or {}).get("translatedText")
        if t and "QUERY LENGTH LIMIT" not in t and "MYMEMORY WARNING" not in t:
            cache[k] = t
            return t
    except Exception as e:
        print(f"translate fail: {e}")
    return None

def self_test() -> int:
    c = {}
    c[key("Hello world")] = "你好世界"
    assert translate_one("Hello world", c) == "你好世界"
    print("translate self-test PASS (cache hit, no network)")
    return 0

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--self-test", action="store_true")
    ap.add_argument("--limit", type=int, default=50)
    ap.add_argument("--offset", type=int, default=0)
    a = ap.parse_args()
    if a.self_test:
        raise SystemExit(self_test())
    import psycopg2
    conn = psycopg2.connect(host=os.environ.get("PGHOST", "192.168.80.153"), port=int(os.environ.get("PGPORT", "5432")),
        dbname=os.environ.get("PGDATABASE", "quote_garden"), user=os.environ.get("PGUSER", os.environ.get("SPRING_DATASOURCE_USERNAME", "postgres")),
        password=os.environ.get("PGPASSWORD", os.environ.get("SPRING_DATASOURCE_PASSWORD", "")))
    cache = load_cache()
    with conn, conn.cursor() as cur:
        cur.execute("SELECT id, content FROM quote WHERE translation IS NULL ORDER BY updated_at DESC LIMIT %s OFFSET %s", (a.limit, a.offset))
        rows = cur.fetchall()
        n = 0
        for qid, content in rows:
            t = translate_one(content, cache)
            if t:
                cur.execute("UPDATE quote SET translation=%s WHERE id=%s", (t, qid))
                n += 1
            time.sleep(1.0)
    save_cache(cache)
    conn.close()
    print(f"translated {n}/{len(rows)}")

if __name__ == "__main__":
    main()
