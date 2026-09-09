# DailyMind Favorite Feedback & Next Local Random — Design Spec

**Date:** 2026-09-08
**Status:** Approved (brainstorming complete, awaiting spec review)
**Scope:** Home favorite feedback + Home `Next` local-random + FavoriteScreen swipe-to-remove.
**Direction:** A — minimal typographic feedback, local-first Next, swipe-left destructive on Favorite.

## 1. Problem

1. **Favorite button feedback is weak.** Pressing `+ Favorite` produces no visible state change — no label morph, no color shift, no animation. `HomeUiState` does not observe favorite state for the current quote, so the screen cannot render a "saved" variant. From the user's perspective it is unclear whether the tap registered.
2. **FavoriteScreen rows are read-only.** The favorite list at `feature/favorite/FavoriteScreen.kt` renders `IndexRow(onClick = {})` — there is no way to remove a saved quote. The only path is "favorite from Home, never unfavorite".
3. **Next hits the network every press.** `HomeViewModel.onEvent(NextRandom)` calls `repo.getRandomQuote()` (`QuoteRepositoryImpl.kt:57`), which always invokes `api.getRandomQuote()` with a local fallback. Every press is a round-trip; offline or slow-network users see no `Next` feedback. Background sync via `DailySyncWorker` already pulls remote quotes into Room — the data is locally available, the UI just doesn't use it.

## 2. Goals

1. The favorite action produces a perceptible, state-aware response (label + brief underline pulse) without violating the editorial typography rule (no icons, no elevation).
2. The user can both add *and* remove a favorite from Home and from the FavoriteScreen.
3. `Next →` resolves instantly from local Room data, excluding the currently-displayed quote. Background `DailySyncWorker` already keeps Room fresh, so the experience stays live without per-press network traffic.
4. No regression of editorial spec constraints (no icons, no shadows, 0dp shapes, ≤8dp rises, 200ms tween animations).

## 3. Non-goals

- Iconography of any kind (still banned by editorial spec).
- Bottom navigation, detail screens, or any new top-level destinations.
- Sharing, copy-to-clipboard, history view.
- Undo snackbar for accidental swipe (defer to v2).
- Sequential (`Prev / Next` page-flip) browsing — explicitly out per user choice.
- Changes to `DailySyncWorker`, `AppNavDisplay`, `Route`, network/DI modules.

## 4. Design tokens

Reuses `editorial` tokens from `EditorialColors.kt` / `EditorialTypography.kt` / `Spacing.kt`. No new tokens needed.

- Filled-state border tint: `MaterialTheme.colorScheme.primary` (= `accent` in editorial mapping).
- Underline pulse color: `MaterialTheme.colorScheme.primary`.
- Swipe background fill: `MaterialTheme.colorScheme.outlineVariant` (matches the existing 1dp divider color in `IndexRow`).
- Swipe "Remove" label: `labelLarge` in `MaterialTheme.colorScheme.primary`, 24dp end padding.
- Animation curves: `tween(200)` for underline pulse and row removal (matches existing `AnimatedContent` in HomeScreen).

## 5. Architecture

### 5.1 Data layer

**`QuoteRepository` (interface, `core/data/QuoteRepository.kt`):**
- Add: `suspend fun getLocalRandomQuote(excludeId: String?): Quote?`
- Keep existing `suspend fun getRandomQuote(): Quote` (network-first with local fallback) for the daily-load path.
- Add: `fun observeFavoriteIds(): Flow<Set<String>>` — derived from existing `observeFavorites()`.

**`QuoteRepositoryImpl`:**
- Implement `getLocalRandomQuote(excludeId)`:
  - `excludeId` non-null → `dao.getRandomExcluding(excludeId)?.toModel()`
  - `excludeId` null → `dao.getRandom()?.toModel()`
  - If both null, throw `IllegalStateException("No cached quotes")`.
- Implement `observeFavoriteIds()`: `observeFavorites().map { list -> list.map { it.id }.toSet() }`.
- `toggleFavorite` unchanged.

**`QuoteDao` (`core/database/dao/QuoteDao.kt`):**
- Add query:
  ```
  @Query("SELECT * FROM quote WHERE deletedAt IS NULL AND id != :excludeId ORDER BY RANDOM() LIMIT 1")
  suspend fun getRandomExcluding(excludeId: String): QuoteEntity?
  ```
- The single-row exclusion is sufficient; `Next` can repeat within a session but rarely does, and session-level repeat is preferable to slow first render.

### 5.2 HomeViewModel (`feature/home/HomeViewModel.kt`)

```kotlin
private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
val uiState: StateFlow<HomeUiState> = _uiState

init {
    viewModelScope.launch {
        repo.observeFavoriteIds()
            .combine(_uiState) { favIds, state ->
                state.copy(isCurrentQuoteFavorite = state.quote?.id in favIds)
            }
            .collect { _uiState.value = it }
    }
    onEvent(HomeEvent.Load)
}

fun onEvent(event: HomeEvent) = when (event) {
    HomeEvent.Load -> ... // unchanged
    HomeEvent.NextRandom -> viewModelScope.launch {
        try {
            val current = _uiState.value.quote?.id
            val q = repo.getLocalRandomQuote(excludeId = current)
                ?: throw IllegalStateException("No other quotes yet")
            _uiState.value = _uiState.value.copy(
                quote = q, error = null, isBrowsing = true
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
        }
    }
    is HomeEvent.Favorite -> viewModelScope.launch {
        repo.toggleFavorite(event.id)
        _uiState.value = _uiState.value.copy(
            favoriteTapKey = _uiState.value.favoriteTapKey + 1
        )
    }
}
```

Notes:
- `combine` with `_uiState` produces a single source of truth for UI; observers downstream get `favoriteTapKey` and `isCurrentQuoteFavorite` from one flow.
- `favoriteTapKey` triggers the underline pulse on each tap regardless of resulting state.
- The `combine` is started before `onEvent(Load)` so favorite state is live even during initial load.

### 5.3 HomeUiState

```kotlin
data class HomeUiState(
    val quote: Quote? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBrowsing: Boolean = false,
    val isCurrentQuoteFavorite: Boolean = false,
    val favoriteTapKey: Int = 0,    // increments on every Favorite event
)
```

### 5.4 FavoriteViewModel (`feature/favorite/FavoriteViewModel.kt`)

```kotlin
sealed interface FavoriteEvent {
    data class Unfavorite(val id: String) : FavoriteEvent
}

fun onEvent(event: FavoriteEvent) = when (event) {
    is FavoriteEvent.Unfavorite -> viewModelScope.launch { repo.toggleFavorite(event.id) }
}
```
`val favorites: StateFlow<List<Quote>>` already in place.

### 5.5 Editorial components (`core/designsystem/EditorialComponents.kt`)

**`OutlineTextAction` gains state:**
```kotlin
@Composable
fun OutlineTextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOn: Boolean = false,           // false = outline, true = filled-tint border
    pulseKey: Any = Unit,            // change to replay underline pulse
)
```
- When `isOn`, border becomes `MaterialTheme.colorScheme.primary` instead of `outline`.
- On `pulseKey` change, an internal `Animatable<Float>` runs `animateTo(1f, tween(200))` → `animateTo(0f, tween(200))` driving an underline (`Box(height = 1.dp, color = primary)`) under the label.

**`IndexRow` becomes dismissable:**
```kotlin
@Composable
fun IndexRow(
    number: String,
    content: String,
    author: String?,
    onClick: () -> Unit,             // still no-op for v1
    modifier: Modifier = Modifier,
    onSwipeOut: (() -> Unit)? = null, // when non-null, enables swipe-left dismiss
)
```
- Uses Material3 `SwipeToDismissBox` (state `rememberSwipeToDismissBoxState` with `positionalThreshold = { totalWidth -> totalWidth * 0.5f }`, `confirmValueChange = { it == SwipeToDismissBoxValue.EndToStart }`).
- Background: `Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.outlineVariant), contentAlignment = Alignment.CenterEnd)` containing `Text("Remove", ...)` padded 24dp from end.
- On dismiss → invoke `onSwipeOut()`.

## 6. UI behavior

### 6.1 Home (changes only)

`HomeScreen.kt:155-168` action row:
```kotlin
if (state.quote != null) {
    val q = state.quote!!
    Spacer(modifier = Modifier.height(32.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlineTextAction(
            label = if (state.isCurrentQuoteFavorite) "Saved ✓" else "+ Save",
            onClick = { viewModel.onEvent(HomeEvent.Favorite(q.id)) },
            isOn = state.isCurrentQuoteFavorite,
            pulseKey = state.favoriteTapKey,
            modifier = Modifier.weight(1f),
        )
        TextAction(
            label = "Next →",
            onClick = { viewModel.onEvent(HomeEvent.NextRandom) },
            modifier = Modifier.weight(1f),
        )
    }
}
```
Label width difference between `+ Save` and `Saved ✓` is 1 character — within the action's `weight(1f)` allocation, no layout shift.

### 6.2 FavoriteScreen (changes only)

`FavoriteScreen.kt:67-74`:
```kotlin
itemsIndexed(list, key = { _, q -> q.id }) { index, q ->
    IndexRow(
        number = (index + 1).toString().padStart(2, '0'),
        content = q.content,
        author = q.author,
        onClick = {},
        onSwipeOut = { vm.onEvent(FavoriteEvent.Unfavorite(q.id)) },
    )
}
```

List re-emission after unfavorite naturally collapses the row; `AnimatedContent` not needed.

### 6.3 Empty / error states

- Home `Empty` (no quotes at all): unchanged `EditorialEmpty("Nothing here yet", …)`.
- Home `Error` from `Next` when Room empty: reuses existing `EditorialError(message, onRetry = { Load })`. Message text: `"No other quotes yet"`.
- FavoriteScreen empty: unchanged.

## 7. Tests

### 7.1 New unit tests

- `HomeViewModelTest`:
  - `NextRandom picks local random excluding current quote` — stub `repo.getLocalRandomQuote(excludeId)` to return `Quote("B")`, verify the call excludes `state.quote.id`.
  - `NextRandom surfaces error when local random returns null` — stub to return null, verify `state.error` populated.
  - `Favorite toggle updates favoriteTapKey` — verify `state.favoriteTapKey` increments after `Favorite`.
  - `isCurrentQuoteFavorite reflects observeFavoriteIds` — emit `setOf("q1")` from `observeFavoriteIds`, set state.quote = q1, verify `state.isCurrentQuoteFavorite == true`.
  - Existing tests that stub `repo.getRandomQuote()` for `NextRandom` need to stub `getLocalRandomQuote(excludeId)` instead. The `getRandomQuote()` call on `Load` fallback path stays.

- `QuoteRepositoryTest`:
  - `getLocalRandomQuote excludes specified id` — seed two quotes, call with excludeId = id-1, verify result is id-2.
  - `getLocalRandomQuote returns null on empty Room` — verify null + (no network call).
  - `getLocalRandomQuote falls back to any when excludeId not found` — seed only id-1, exclude id-1, verify null.

### 7.2 Updated tests

- `FavoriteViewModelTest` (new file if not present, else add):
  - `Unfavorite event calls repo.toggleFavorite` — verify interaction.

### 7.3 Manual verification checklist

- Light + dark themes: `Saved ✓` label readable in both, primary-tint border visible.
- Offline mode: airplane mode, open app → `Next →` still works from cached DB.
- Empty Room: uninstall + reinstall → `Load` shows empty; `Next →` shows error message.
- Slow network: home screen first paint no longer waits on `/api/v1/quotes/random`.
- Swipe threshold at 50% row width feels natural — manually adjust `positionalThreshold` if too sensitive.
- Animation duration 200ms x2 (pulse): does not feel laggy.
- 360dp + 410dp widths: action row stays balanced; underline pulse visible.
- 1.3x font scale: `Saved ✓` and `+ Save` don't truncate.

## 8. Boundaries

**Modify:**
- `core/data/QuoteRepository.kt`
- `core/data/QuoteRepositoryImpl.kt`
- `core/database/dao/QuoteDao.kt`
- `core/designsystem/EditorialComponents.kt`
- `feature/home/HomeViewModel.kt`
- `feature/home/HomeUiState.kt`
- `feature/home/HomeScreen.kt`
- `feature/favorite/FavoriteViewModel.kt`
- `feature/favorite/FavoriteScreen.kt`
- Tests: `HomeViewModelTest.kt`, `QuoteRepositoryTest.kt`, new `FavoriteViewModelTest.kt`

**Do not modify:**
- `DailySyncWorker`, `SyncRepository`, network DI, `AppNavDisplay`, `Route`, `MainActivity`, `Theme.kt`, `MASTER.md`, design token files (no new tokens).

## 9. Risks & mitigations

| Risk | Mitigation |
|---|---|
| `combine` hot-flow back-pressure on every favorite change | `favoriteTapKey` only changes on tap, not on flow re-emission; `_uiState` is `StateFlow` (conflated) |
| `Saved ✓` Unicode glyph rendering on older Android API (minSdk 24) | `✓` is U+2713 in BMP; supported on all API levels. Fall back to `Saved` (no glyph) if device font lacks it — defensive, not expected |
| Swipe gesture conflicts with parent scroll | Material3 `SwipeToDismissBox` already handles this; verify by manual test |
| `getRandomExcluding` SQL with `excludeId = ""` (no current quote) | Use SQL `AND id != :excludeId` — empty string matches no real id since all ids are non-empty UUIDs/strings. Confirmed against existing seed data |
| `editorial` accent on dark scheme might be too subtle for "Saved ✓" border | Tokens already verified ≥4.5:1; manual visual review in dark mode before merge |

## 10. Self-review

- **Placeholders:** none — all queries, methods, parameters, file paths concrete.
- **Internal consistency:** Section 5.4 says `val favorites: StateFlow<List<Quote>>`; existing `FavoriteViewModel.kt` uses `favorites.collectAsStateWithLifecycle()` via `vm.favorites` — verified at `FavoriteScreen.kt:28`. State Flow type matches.
- **Scope:** Single feature spec covering three tightly-coupled UX improvements; each section dependent on the others (e.g., favorite state observation enables Home label state). Not decomposed.
- **Ambiguity resolved:**
    - "本地跟线上都需要" → local-first random, background sync already in place (Section 5.1, §3.3).
    - "能加也需要能取消" → bidirectional toggle from Home + swipe-remove from Favorite (§3.2, §3.5).
    - "能让用户感知到" → state-aware label + underline pulse on every tap (§6.1).
    - "在首页哪里可以取消" → only FavoriteScreen swipe removes; Home uses tap-toggle (§6.1, §6.2).
    - "滑动移除就行" → swipe-left only; right-swipe reserved for future (§6.2).
    - "参考目前最佳方案业内" → Material3 SwipeToDismissBox + accent-color "Remove" background (§4, §5.5).