"""Dump all current translations from the local prepopulated SQLite to
app/src/main/assets/translations_v2.json. The Room Migration_1_2 then
re-applies these on every existing install, preserving favorites/history.

Output format:
  {
    "version": 2,
    "source": "MyMemory+LibreTranslate",
    "generated_at": "...",
    "entries": [{"id": "<sha256>", "translation": "<zh-CN>"}, ...]
  }
"""
import json, sqlite3, datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
DB_PATH = ROOT / "app" / "src" / "main" / "assets" / "quotegarden.db"
OUT_PATH = ROOT / "app" / "src" / "main" / "assets" / "translations_v2.json"


def main():
    if not DB_PATH.exists():
        raise SystemExit(f"DB not found: {DB_PATH}")
    con = sqlite3.connect(str(DB_PATH))
    cur = con.cursor()
    cur.execute(
        "SELECT id, translation FROM quote "
        "WHERE deletedAt IS NULL AND translation IS NOT NULL AND translation != '' "
        "ORDER BY id"
    )
    rows = cur.fetchall()
    con.close()

    entries = [{"id": qid, "translation": t} for (qid, t) in rows]
    payload = {
        "version": 2,
        "source": "MyMemory+LibreTranslate (disroot.org)",
        "generated_at": datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ"),
        "count": len(entries),
        "entries": entries,
    }
    OUT_PATH.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"wrote {len(entries)} entries to {OUT_PATH}")
    print(f"size: {OUT_PATH.stat().st_size:,} bytes")


if __name__ == "__main__":
    main()
