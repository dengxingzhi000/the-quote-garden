"""Translate REMOTE PostgreSQL DB (192.168.80.156:5432 / quote_garden)
via MyMemory + LibreTranslate fallback. Same semantics as
translate_local_prepopulated.py but targets the server-side DB.

Usage:
  python tools/crawler/translate_remote.py
  python tools/crawler/translate_remote.py --limit 10 --dry-run
"""
import argparse, hashlib, json, os, time
from pathlib import Path

import psycopg2, requests

ROOT = Path(__file__).resolve().parent.parent.parent
CACHE_PATH = Path(__file__).resolve().parent / "translation_cache_remote.json"

ENDPOINT_MYMEMORY = "https://api.mymemory.translated.net/get"
ENDPOINT_LIBRE = "https://translate.disroot.org/translate"


def cache_key(content: str) -> str:
    return hashlib.sha256(content.encode("utf-8")).hexdigest()


def _valid(t):
    if not t:
        return False
    return not any(b in t for b in ("QUERY LENGTH LIMIT", "MYMEMORY WARNING", "INVALID"))


def translate_mymemory(text: str) -> str | None:
    try:
        r = requests.get(
            ENDPOINT_MYMEMORY,
            params={"q": text[:450], "langpair": "en|zh-CN"},
            timeout=20,
        )
        d = r.json()
        t = (d.get("responseData") or {}).get("translatedText")
        if _valid(t):
            return t
    except Exception as e:
        print(f"  mymemory err: {e}", flush=True)
    return None


def translate_libre(text: str) -> str | None:
    try:
        r = requests.post(
            ENDPOINT_LIBRE,
            json={"q": text[:450], "source": "en", "target": "zh", "format": "text"},
            timeout=20,
        )
        if r.status_code != 200:
            return None
        d = r.json()
        t = d.get("translatedText")
        if _valid(t):
            return t
    except Exception as e:
        print(f"  libre err: {e}", flush=True)
    return None


def translate_one(text: str, cache: dict, provider: str = "auto") -> str | None:
    k = cache_key(text)
    if k in cache:
        return cache[k]
    if provider == "auto":
        order = (translate_mymemory, translate_libre)
    elif provider == "mymemory":
        order = (translate_mymemory,)
    elif provider == "libre":
        order = (translate_libre,)
    else:
        order = (translate_libre, translate_mymemory)
    for fn in order:
        t = fn(text)
        if t:
            cache[k] = t
            return t
    return None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--rate", type=float, default=1.0)
    ap.add_argument("--table", choices=("quote", "article", "both"), default="both")
    args = ap.parse_args()

    cache = json.loads(CACHE_PATH.read_text(encoding="utf-8")) if CACHE_PATH.exists() else {}

    conn = psycopg2.connect(
        host=os.environ.get("PGHOST", "192.168.80.156"),
        port=int(os.environ.get("PGPORT", "5432")),
        dbname=os.environ.get("PGDATABASE", "quote_garden"),
        user=os.environ.get("PGUSER", "admin"),
        password=os.environ.get("PGPASSWORD", "123456"),
        connect_timeout=10,
    )
    conn.set_session(autocommit=False)
    cur = conn.cursor()

    tables = ("quote", "article") if args.table == "both" else (args.table,)
    grand_total = grand_ok = grand_fail = 0

    for table in tables:
        cur.execute(
            f"SELECT id, content FROM {table} WHERE deleted_at IS NULL AND translation IS NULL ORDER BY updated_at DESC"
        )
        rows = cur.fetchall()
        if args.limit:
            rows = rows[: args.limit]
        cached_hits = sum(1 for qid, c in rows if cache_key(c) in cache)
        print(f"[{table}] to translate: {len(rows)} rows (cache hits: {cached_hits})", flush=True)

        n_ok = n_fail = 0
        for i, (qid, content) in enumerate(rows, 1):
            k = cache_key(content)
            if k in cache:
                translated = cache[k]
            else:
                translated = translate_one(content, cache)
                if args.rate > 0:
                    time.sleep(args.rate)
                if translated is None:
                    n_fail += 1
                    print(f"  [{i}/{len(rows)}] {table}:{qid[:12]}... FAIL (len={len(content)})", flush=True)
                    continue
                n_ok += 1

            if not args.dry_run:
                cur.execute(
                    f"UPDATE {table} SET translation = %s, updated_at = EXTRACT(EPOCH FROM NOW()) * 1000 WHERE id = %s",
                    (translated, qid),
                )
                if i % 50 == 0:
                    conn.commit()
            preview = translated[:60].replace("\n", " ")
            print(f"  [{i}/{len(rows)}] {table}:{qid[:12]}... ok ({len(content)}c -> {len(translated)}c) {preview}...", flush=True)

        if not args.dry_run:
            conn.commit()
        print(f"[{table}] done. translated={n_ok}, cached={cached_hits}, failed={n_fail}", flush=True)
        grand_total += len(rows)
        grand_ok += n_ok + cached_hits
        grand_fail += n_fail

    CACHE_PATH.write_text(json.dumps(cache, ensure_ascii=False, indent=2), encoding="utf-8")
    cur.close()
    conn.close()
    print(f"\nGRAND total: translated={grand_ok}, failed={grand_fail}, rows={grand_total}")


if __name__ == "__main__":
    main()
