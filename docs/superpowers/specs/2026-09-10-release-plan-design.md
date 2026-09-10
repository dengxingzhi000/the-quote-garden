# Release Plan — Quote Garden v0.1.0 → v0.7.0

**Date:** 2026-09-10
**Status:** Approved
**Scope:** Tag, push, and release the 59 unpushed commits on `deploy/neon-pooler-compat` plus the pending uncommitted rebrand + batch-translation work, in 4 batches.

## 1. Goal

Take the in-flight work on `deploy/neon-pooler-compat` and ship it as 7 annotated tags + GitHub Releases following standard GitHub flow (semver tags → `git push` → `gh release create`). Work is split into 4 batches for ergonomics and to keep each push small.

## 2. Version Groupings (confirmed)

| Tag | Endpoint commit | Range | Feature theme |
|---|---|---|---|
| `v0.1.0` | `4af41db` chore(crawler): remove local-only crawl progress artifacts | bbff174..4af41db (15 commits) | Prepopulated SQLite DB replaces JSON seed pipeline; local random Next button; favorite row swipe-to-unfavorite; favorite button underline pulse |
| `v0.2.0` | `f340fb5` feat(home): H1 magazine eyebrow in accent color | 3ab807e..f340fb5 (7 commits) | Editorial Bottom Nav (Home + Me); ThemeStore (system/light/dark); SettingRow; dynamic version in Me |
| `v0.3.0` | `9f69634` feat(home): wire CategoryPreferenceStore, add chip strip and empty-mode switch | a8132c4..9f69634 (17 commits) | Category filtering DAO + repo + datastore; Browse 2-col grid; CategoryDetail; Home chip strip; 3-tab bottom bar; Robolectric deps |
| `v0.4.0` | `66d6f1f` test(home): history cap and load reset coverage | 689e165..66d6f1f (6 commits) | Home history stack + Prev back nav; HomeActionsBar equal-width capsules; sticky horizontal swipe actions bar |
| `v0.5.0` | `7100c6b` fix(home): drop unused import and sync spec scroll-state snippet | 6d58a62..7100c6b (7 commits) | LanguageStore (ZH/EN); Home reader layout with full-zone swipe hotspot |
| `v0.6.0` | `20f263c` docs(images): align onboarding-3 filename with placed asset | f4f0158..20f263c (7 commits) | Brand assets (icon, QR, About, Onboarding drawables); first-run onboarding welcome screen |
| `v0.7.0` | HEAD after new commits | 3 new commits | Rebrand `dailymind → quotegarden` (app + server); batch-translation infrastructure (TranslationService, TranslationPatcher, translations_v2.json, crawler tools); AGENTS.md refresh |

## 3. Branch Strategy (confirmed)

Tag + push directly on `deploy/neon-pooler-compat`. Master is untouched (master has 1 commit `45fa1b4 fix(server): HikariCP Neon pooler (#12)` not on deploy — left for the user to merge separately if desired).

## 4. Push Batching (confirmed — 4 batches)

| Batch | Tags | Push | Release calls |
|---|---|---|---|
| 1 | `v0.1.0` | push branch tip + push tag | 1 (`gh release create v0.1.0`) |
| 2 | `v0.2.0`, `v0.3.0` | push both tags | 2 |
| 3 | `v0.4.0`, `v0.5.0`, `v0.6.0` | push three tags | 3 |
| 4 | `v0.7.0` | 4 new commits + push branch + push tag | 1 |

Total: 7 tags, 7 releases, 2 branch-tip pushes (batch 1 + batch 4). Tag-only pushes between batches 2 and 3 keep pushes small.

## 5. Commit Strategy for v0.7.0 (confirmed — 4 commits)

The user originally asked for 3 commits; a 4th is added because the rebrand introduces a `HomeScreen.kt` smart-cast compile failure (AGENTS.md §"Working-tree state") that must be fixed before `:app:test` can pass.

1. **`chore(rename): rebrand dailymind → quotegarden (app + server)`** — the package rename plus `.gitignore` updates (`.superpowers/`, `*.db-shm`, `*.db-wal`, `tools/crawler/translation_cache*.json`).
2. **`fix(home): resolve smart-cast on delegated state.quote / state.error`** — the AGENTS.md-flagged `HomeScreen.kt` compile error (delegated `state.quote` / `state.error` properties cannot be smart-cast across closures). Restructured to use `?.let` / local non-null bindings so the build compiles.
3. **`feat(i18n): TranslationService + TranslationPatcher + translations_v2 + crawler tools`** — the translation infrastructure (new `TranslationPatcher`, new `translations_v2.json` asset, new crawler scripts `generate_translations_json.py`, `translate_local_prepopulated.py`, `translate_remote.py`, README touch-up).
4. **`docs(server): AGENTS.md refresh + batch-translation spec + crawler password fix`** — `AGENTS.md` updates, the new `docs/superpowers/specs/2026-09-09-batch-translation-design.md`, and a `fix(crawler): drop hardcoded password fallback in translate_remote.py` (defensive — the `"123456"` default is removed in favor of an empty-string fallback so the env-var-only contract holds even on local dev).

## 6. Release Notes Template

Each `gh release create` uses Markdown with sections:

```markdown
## Highlights
<1-2 sentence summary>

## Features
- …

## Fixes
- …

## Docs
- …

## Build / Tooling
- …

Full diff: https://github.com/dengxingzhi000/the-quote-garden/compare/<previous-tag>...vX.Y.Z
```

## 7. Files NOT to Commit

The following files should be excluded by `.gitignore` before any v0.7.0 commit (added in commit 1 of v0.7.0):

- `.superpowers/` — local-only Claude Code skills state.
- `*.db-shm`, `*.db-wal` — SQLite WAL artifacts in `app/src/main/assets/`.
- `tools/crawler/translation_cache*.json` — translator cache; re-runnable from cache DB via MyMemory.

Already-ignored (per existing `.gitignore`): `build/`, `.gradle/`, `app/build/`, `server/build/`, `.idea/`, `*.iml`, `local.properties`, `.env`, `.env.*`, `.opencode/`.

The prepopulated SQLite DB (`app/src/main/assets/quotegarden.db`, 274 KB) IS committed (mirrors `dailymind.db` from earlier commits).

## 8. Open Issues Flagged

1. **`tools/crawler/translate_remote.py` line 100** hardcodes `password=os.environ.get("PGPASSWORD", "123456")`. The fallback is wrong per AGENTS.md (local DB pass is `quotegarden`). Will be removed in the docs commit (#3 above) — the env var must be set explicitly or the connection fails fast. **Open question: confirm the local DB password really is `quotegarden`, not `123456`.**
2. **Master has 1 commit not on deploy** (`45fa1b4 fix(server): HikariCP Neon pooler (#12)`). User chose not to merge; release proceeds on `deploy/neon-pooler-compat`.
3. **`build/` failure mentioned in AGENTS.md** — `HomeScreen.kt` smart-cast issue stems from the uncommitted rebrand work. Will be verified before tag v0.7.0; if build still fails the release is blocked.

## 9. Verification

Before each tag/release:

- `git status` clean (between batches).
- For v0.7.0: `server/gradlew.bat :app:testDebugUnitTest` and `cd server && .\gradlew.bat test` both pass.
- After all batches: `git ls-remote --tags origin` and `gh release list --repo dengxingzhi000/the-quote-garden` confirm 7 tags and 7 releases.

## 10. Rollback

If a release is published in error:

```bash
gh release delete vX.Y.Z --repo dengxingzhi000/the-quote-garden --yes
git push origin :refs/tags/vX.Y.Z
```

Tags are mutable on the remote (annotated tag objects stay; only the remote ref is removed). Releases can be re-published after the tag is restored locally.