# DailyMind Editorial Redesign — Design Spec

**Date:** 2026-09-03
**Status:** Approved (brainstorming complete, awaiting spec review)
**Scope:** Home + Favorite + Theme. No BottomNav. No business-logic changes.
**Direction:** A — Pure Editorial Typography (quiet luxury / magazine as an app)
**Decisions:** Override `design-system/MASTER.md` (playful teal kids theme archived for this scope); fonts via Google Fonts downloadable provider with system Serif/Sans fallback (offline-first safe).

## 1. Current UI problems

1. Card-heavy + gradient background (`HomeScreen.kt`: `Brush.verticalGradient` + `Card` 12dp + elevation 6dp + `animateFloatAsState` elevation) — banned by brief.
2. Playful kids theme (`Theme.kt` / `MASTER.md`: teal `#0D9488` + amber `#D97706`, Baloo 2 / Comic Neue) — opposite of quiet luxury.
3. Flat type hierarchy — title, quote, meta lack editorial scale contrast.
4. Heavy solid `Button` + `OutlinedButton` pair (48dp, 8dp radius) — should become typographic actions.
5. `FavoriteScreen.kt` is a stub (`LazyColumn { Text(it.content) }`) — no empty state, no theming.
6. `MASTER.md` forbids dark mode — overridden by this spec (dark scheme required).
7. Hardcoded paddings (16/24dp), no shared spacing/shape/elevation tokens.

## 2. Design tokens (single source of truth, override MASTER for this scope)

### Color

Light — bg `#F7F5F0`, primary text `#171717`, secondary `#77736B`, divider `#DDD9D0`, accent `#8A5A44`.
Dark — bg `#151515`, primary `#F2EFE8`, secondary `#A7A39B`, divider `#383838`, accent `#C49A7A`.

M3 mapping: `background`/`surface` → bg; `onBackground`/`onSurface` → primary; `onSurfaceVariant` → secondary; `outlineVariant` → divider; `primary` → accent. No gradient, no shadow, no tertiary usage. Contrast ≥ 4.5:1 verified pairs: `#171717` on `#F7F5F0`, `#F2EFE8` on `#151515`.

### Typography (Google Fonts download; fallback: system Serif/SansSerif)

- Display (artistic serif, latin only — e.g. `Inspire`, date numerals): Noto Serif Display, 40sp/44sp, -0.5sp.
- Quote (serif, CN+EN): Noto Serif SC, 26sp/36sp, Medium.
- Translation (sans italic): Inter / Noto Sans SC, 16sp/26sp.
- Section label (e.g. `TODAY'S QUOTE`): 11sp, 0.3 tracking, uppercase, secondary color.
- Meta/author: 12sp, secondary, right-aligned.
- Body/footnote: 14sp/20sp, 11–12sp/16sp.

### Spacing / shape / elevation

Scale: 4 / 8 / 16 / 24 / 32 / 48 / 64 (hero top 64, section gaps 48, content padding 24 horizontal, dividers 1dp). Shapes: 0dp everywhere (no cards); text actions use underline or 1dp minimal border. Elevation: 0 everywhere.

## 3. Layout

### Home (replaces centered Card + 2 solid buttons; same `HomeEvent` calls)

```
09 / 03  (labelSmall, secondary, tracking)
Good Morning  (display serif 40)
A quiet mind, retold daily.  (body, secondary)

TODAY'S QUOTE  (11sp uppercase label)
A quiet mind / finds beauty / everywhere.  (quote serif 26/36, start-aligned)
────────────────────────────────  (1dp divider)
<translation, sans-italic 16>
                    — author  (meta 12sp, end-aligned)
#category  (accent label)

Explore  →          + Saved  (typographic actions, min 48dp target)
Offline-first · cached first  (11sp footnote)
```

Container: `LazyColumn` (not centered scroll `Column`), 24dp horizontal padding. Loading: thin text row, no spinner card. Error/empty: serif title + text-action retry, no Card. `onNavigateToFavorite` wired to `+ Saved` text action (currently unused parameter — exposure only, no logic change). Fallback-quote footnote kept as 11sp text.

### Favorite (full rebuild of stub; same `favorites` flow)

```
Collection  (display serif)
A small archive of beautiful thoughts.  (subtitle sans)
12 saved  (count label)
────────────────────────────────
01  "…"  →   (+ author, chevron only; divider per row, min 48dp row target)
────────────────────────────────
Empty: whitespace + serif "No saved quotes yet" + `Explore →` back action.
```

No cards, no FAB, no bottom nav (out of scope).

## 4. Components (new, in `core.designsystem`; zero hardcode in screens)

- `EditorialTopBar(date, greeting)` — text-only, no `TopAppBar` container, no icons, no elevation.
- `QuoteBlock(quote)` — serif content + 1dp divider + italic translation + end-aligned author + accent `#category`.
- `EditorialAction(label, onClick, style)` — `TextAction` (`Explore →`, accent) and `OutlineTextAction` (`+ Saved`, 1dp border, 0dp radius); `heightIn(min 48dp)`, `tween(200)` color fade.
- `IndexRow(number, content, author, onClick)` — row + divider for Favorite.
- `EditorialEmpty(title, body, action)` / `EditorialError(message, retry)` — replace all 3 Card states.
- `DailyMindTheme` extension: `LightQuietLuxury` / `DarkQuietLuxury` schemes + downloadable typography + `Spacing` CompositionLocal.

## 5. Animation

Screen enter `fadeIn(tween(300))` once; quote switch keeps `AnimatedContent fadeIn(200)+fadeOut(200)` plus ≤8dp rise; remove elevation animation; dividers static; respect animator-duration-scale (skip rise when reduced motion). No bounce/particles/rotation/shimmer/blur.

## 6. Architecture / boundaries

- Modify only: `core/designsystem/theme/Theme.kt`, `core/designsystem/` (new `EditorialComponents.kt`, `Spacing.kt`), `feature/home/HomeScreen.kt`, `feature/favorite/FavoriteScreen.kt`, `MainActivity.kt` (edge-to-edge + theme only).
- Do NOT touch: `HomeViewModel`, `HomeUiState`, `FavoriteViewModel`, repositories, Room DAO/entities, WorkManager sync, `AppNavDisplay`, `Route`.
- `MASTER.md`: append override note pointing here (archive, don't delete).
- Edge-to-edge: `enableEdgeToEdge()` + `WindowInsets` padding; transparent system bars.
- Dynamic type: flexible `LazyColumn`, no fixed heights except 48dp min targets; verify at 1.3x scale. Serif never below 12sp for body. Touch targets ≥ 48dp via padding.

## 7. Verification

- `./gradlew :app:assembleDebug` builds.
- `./gradlew :app:testDebugUnitTest` — existing `HomeViewModelTest`, `QuoteRepositoryTest`, `RouteTest` pass unchanged.
- Manual: light/dark, 360dp + 410dp widths, 1.0x/1.3x fonts, empty favorites, error retry, quote-switch fade.
- Success: first glance reads date → serif headline → quote → divider → meta → text actions; zero Cards/shadows/gradients; `Load`/`NextRandom`/`Favorite` behave identically.

## 8. Self-review

- Placeholders: none — all tokens, files, and behaviors concrete.
- Consistency: no BottomNav anywhere (scope excludes it); dark mode required consistently; font fallback covers offline-first concern from Q&A.
- Scope: single spec, Home + Favorite + Theme only; viewmodels/data/nav explicitly excluded.
- Ambiguity resolved: "1"/"a" confirmed as Direction A; "ok" confirmations recorded per section (tokens, layout, components/animation, implementation).
