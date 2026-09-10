"""Translate LOCAL prepopulated SQLite (assets/quotegarden.db) via MyMemory.
Mirrors tools/crawler/translate.py semantics but targets the bundled SQLite
instead of the remote PostgreSQL. Result: the APK's prepopulated DB ships
with `translation` populated for new installs.

Usage:
  python tools/crawler/translate_local_prepopulated.py
  python tools/crawler/translate_local_prepopulated.py --limit 10 --dry-run
"""
import argparse
import hashlib
import json
import sqlite3
import time
from pathlib import Path

import requests

ROOT = Path(__file__).resolve().parent.parent.parent
DB_PATH = ROOT / "app" / "src" / "main" / "assets" / "quotegarden.db"
CACHE_PATH = Path(__file__).resolve().parent / "translation_cache_local.json"
ENDPOINT_MYMEMORY = "https://api.mymemory.translated.net/get"
ENDPOINT_LIBRE = "https://translate.disroot.org/translate"


def cache_key(content: str) -> str:
    return hashlib.sha256(content.encode("utf-8")).hexdigest()


def _valid(t: str | None) -> bool:
    if not t:
        return False
    bad = ("QUERY LENGTH LIMIT", "MYMEMORY WARNING", "INVALID", "ERROR")
    return not any(b in t for b in bad)


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
    """provider: 'mymemory' | 'libre' | 'auto' (try mymemory then libre)."""
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
    ap.add_argument("--limit", type=int, default=0, help="0 = no limit")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--rate", type=float, default=1.0, help="seconds between API calls")
    args = ap.parse_args()

    if not DB_PATH.exists():
        raise SystemExit(f"DB not found: {DB_PATH}")

    cache = json.loads(CACHE_PATH.read_text(encoding="utf-8")) if CACHE_PATH.exists() else {}

    con = sqlite3.connect(str(DB_PATH))
    cur = con.cursor()
    cur.execute(
        "SELECT id, content FROM quote WHERE deletedAt IS NULL AND translation IS NULL"
    )
    rows = cur.fetchall()
    if args.limit:
        rows = rows[: args.limit]
    print(f"to translate: {len(rows)} rows (cache hits: {sum(1 for qid, c in rows if cache_key(c) in cache)})")

    n_ok = n_fail = n_skip = 0
    for i, (qid, content) in enumerate(rows, 1):
        k = cache_key(content)
        if k in cache:
            translated = cache[k]
            n_skip += 1
        else:
            translated = translate_one(content, cache)
            time.sleep(args.rate)
            if translated is None:
                n_fail += 1
                print(f"  [{i}/{len(rows)}] {qid[:12]}... FAIL (len={len(content)})", flush=True)
                continue
            n_ok += 1

        if not args.dry_run:
            cur.execute("UPDATE quote SET translation = ? WHERE id = ?", (translated, qid))
            print(f"  [{i}/{len(rows)}] {qid[:12]}... ok ({len(content)}c -> {len(translated)}c)", flush=True)
        else:
            preview = translated[:60].replace("\n", " ")
            print(f"  [{i}/{len(rows)}] {qid[:12]}... DRY ok -> {preview}...", flush=True)

    if not args.dry_run:
        con.commit()
    con.close()
    CACHE_PATH.write_text(json.dumps(cache, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"done. translated={n_ok}, cached={n_skip}, failed={n_fail}, total={len(rows)}")


if __name__ == "__main__":
    main()
