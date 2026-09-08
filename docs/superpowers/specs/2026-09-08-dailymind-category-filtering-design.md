# DailyMind Category Filtering — Design Spec

**Date:** 2026-09-08
**Status:** Approved (pending user review of written spec)
**Scope:** Add category-based browsing and a Home category preference to the DailyMind Android app. Server unchanged.

---

## 1. Goal

Users can browse the offline quote library by category, and can pin a category preference on Home so the daily/random feed stays within a single mood.

Concretely:

- A new **Browse** tab shows the library as a 2-column grid of category tiles, each linking to a flat list of quotes in that category.
- **Home** gains a horizontal chip strip directly under the greeting. The first chip is **All** (default, no filter). Tapping any other chip filters the daily/random feed strictly to that category.
- The selected category persists across launches via DataStore.

## 2. Non-Goals

- No recommendation algorithm (YAGNI; user explicitly deferred this to a later spec).
- No quote-detail screen — tapping a row in a category list does not navigate further.
- No favorite toggle from inside the Browse list (Browse is for browsing; favorites live on Home + Me).
- No sort options inside the category list (DB insertion order is the order).
- No new network calls and no schema migration.

## 3. Architecture

```
feature/
├── browse/                                    (NEW)
│   ├── BrowseScreen.kt                        2-col LazyVerticalGrid of category tiles
│   ├── BrowseViewModel.kt                     exposes category counts + names
│   ├── CategoryDetailScreen.kt                flat IndexRow list for one category
│   └── CategoryDetailViewModel.kt             observes quotes for the given category
└── home/                                      (CHANGED)
    ├── HomeScreen.kt                          gains CategoryChips composable
    └── HomeViewModel.kt                       gains HomeEvent.SelectCategory

core/
├── database/dao/QuoteDao.kt                   (CHANGED) +5 query methods
├── data/QuoteRepository.kt                    (CHANGED) optional category params + new flows
├── data/QuoteRepositoryImpl.kt                (CHANGED) forwards category to DAO
├── database/CategoryCount.kt                  (NEW)     POJO for COUNT(*) query result
├── datastore/PreferencesDataStore.kt          (CHANGED) +PreferencesKeys.CATEGORY
├── datastore/CategoryPreferenceStore.kt       (NEW)     DataStore-backed selectedCategory
└── navigation/
    ├── Route.kt                               (CHANGED) +Browse, +CategoryDetail(category)
    └── AppNavDisplay.kt                       (CHANGED) 3-tab bottom bar; new entryProvider branches
```

### 3.1 Module dependencies

- `CategoryPreferenceStore` is `@Singleton` and uses the existing `DataStore<Preferences>` provided by `DataStoreModule` (singleton, name `daily_settings`). No new DataStore file is created.
- `CategoryPreferenceStore` is injected into `HomeViewModel` via Hilt (the constructor of `HomeViewModel` gains one parameter).
- `BrowseViewModel` and `CategoryDetailViewModel` only need `QuoteRepository`.

## 4. Data Layer

### 4.1 `QuoteDao` — new queries

```kotlin
// New methods; existing methods remain unchanged.

@Query("""
    SELECT DISTINCT category FROM quote
    WHERE category IS NOT NULL AND deletedAt IS NULL
    ORDER BY category
""")
fun observeCategories(): Flow<List<String>>

@Query("""
    SELECT category AS category, COUNT(*) AS count FROM quote
    WHERE category IS NOT NULL AND deletedAt IS NULL
    GROUP BY category
    ORDER BY count DESC, category ASC
""")
fun observeCategoryCounts(): Flow<List<CategoryCount>>

@Query("""
    SELECT * FROM quote
    WHERE category = :category AND deletedAt IS NULL
    ORDER BY id
""")
fun observeByCategory(category: String): Flow<List<QuoteEntity>>

@Query("""
    SELECT * FROM quote
    WHERE deletedAt IS NULL
      AND (:category IS NULL OR category = :category)
    ORDER BY RANDOM() LIMIT 1
""")
suspend fun getRandomFiltered(category: String?): QuoteEntity?

@Query("""
    SELECT * FROM quote
    WHERE id != :excludeId AND deletedAt IS NULL
      AND (:category IS NULL OR category = :category)
    ORDER BY RANDOM() LIMIT 1
""")
suspend fun getRandomExcludingFiltered(excludeId: String, category: String?): QuoteEntity?
```

`CategoryCount` is a new POJO in `core.database`:

```kotlin
data class CategoryCount(
    val category: String,
    val count: Int,
)
```

### 4.2 `CategoryPreferenceStore`

```kotlin
@Singleton
class CategoryPreferenceStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val selectedCategory: Flow<String?> =
        dataStore.data.map { it[PreferencesKeys.CATEGORY] }

    suspend fun setSelectedCategory(value: String?) {
        dataStore.edit { prefs ->
            if (value == null) prefs.remove(PreferencesKeys.CATEGORY)
            else prefs[PreferencesKeys.CATEGORY] = value
        }
    }
}
```

`PreferencesKeys.CATEGORY = stringPreferencesKey("selected_category")` is added to `PreferencesDataStore.kt`.

### 4.3 `QuoteRepository` — interface changes

```kotlin
interface QuoteRepository {
    fun observeQuotes(): Flow<List<Quote>>
    fun observeFavorites(): Flow<List<Quote>>

    // CHANGED: optional category. null = no filter ("All").
    suspend fun getDailyQuote(category: String? = null): Quote?

    suspend fun getRandomQuote(): Quote

    // CHANGED: optional category.
    suspend fun getLocalRandomQuote(
        excludeId: String? = null,
        category: String? = null,
    ): Quote?

    fun observeFavoriteIds(): Flow<Set<String>>
    suspend fun sync(): Result<Unit>
    suspend fun toggleFavorite(quoteId: String)

    // NEW
    fun observeCategories(): Flow<List<String>>
    fun observeCategoryCounts(): Flow<List<CategoryCount>>
    fun observeQuotesByCategory(category: String): Flow<List<Quote>>
}
```

### 4.4 `QuoteRepositoryImpl` — behavior

- `getDailyQuote(category)`:
  - If `category == null` → existing logic unchanged (pinned daily → random in unviewed → network fallback).
  - If `category != null` → **ignore** the pinned daily (the pin applies to the "All" universe), then call `dao.getRandomFiltered(category)`. If `null`, return `null` (UI shows empty state). On success, **do not** write to `DailyQuoteStore` (the pin is a global concept; per-category picks are not pinned across the day).
- `getLocalRandomQuote(excludeId, category)` → forwards to `dao.getRandomExcludingFiltered(...)`.
- `observeCategories()` / `observeCategoryCounts()` / `observeQuotesByCategory(category)` → direct DAO forwarding with `.map { it.toModel() }` for the latter.

## 5. UI & Navigation

### 5.1 Bottom navigation

`EditorialBottomBar` gains a third tab. Order: **Today · Browse · Me** (today's Home tab keeps its existing label). `BottomTab` ids become `home`, `browse`, `me`. The existing `AppNavDisplay` (Navigation 3, `backStack: SnapshotStateList<Route>`) is extended:

- `Route` sealed interface gains two members:
  - `data object Browse : Route`
  - `data class CategoryDetail(val category: String) : Route`
- `AppNavDisplay.Tabs` becomes a 3-item list in the order above.
- The `entryProvider` adds two branches:
  - `is Route.Browse` → `BrowseScreen(onSelectCategory = { backStack.add(Route.CategoryDetail(it)) })`
  - `is Route.CategoryDetail` → `CategoryDetailScreen(category = route.category, onBack = { backStack.removeLastOrNull() })`
- `Route.tabId()` returns `"browse"` for `Route.Browse` and `Route.CategoryDetail` so the bar stays highlighted on sub-pages.
- Tapping a bottom tab uses the existing `backStack.remove(route); backStack.add(route)` pattern; for the Browse tab, this also clears any `CategoryDetail` instance off the stack (by virtue of `remove` matching only the `Browse` data object instance — the lone copy is kept, sub-pages stay until the user navigates back or pops).

### 5.2 Home — CategoryChips strip

`HomeScreen` renders a `CategoryChips` composable between `EditorialTopBar` and the existing quote block. It is part of the `LazyColumn` as a single non-scrolling item that internally contains a horizontal `LazyRow`.

- Source of categories: `viewModel.availableCategories` (the top-6 categories by count from `observeCategoryCounts()`).
- "All" chip is always first and is selected when `state.selectedCategory == null`.
- If `availableCategories.size > 6`, the 7th chip is **More…** which opens a `ModalBottomSheet` listing all categories alphabetically (`observeCategories()`).
- Chip visual: `labelLarge` text. Selected = `colorScheme.primary` with 1dp underline. Unselected = `colorScheme.onSurfaceVariant`. Min height 40dp, horizontal padding 16dp.
- The chips strip is **always visible** (it is not gated by `isBrowsing`).

`HomeUiState` gains:

```kotlin
val selectedCategory: String? = null,
val availableCategories: List<String> = emptyList(),
val emptyMode: EmptyMode = EmptyMode.Generic,
```

where `EmptyMode` is a sibling of `HomeUiState` in `feature/home/HomeUiState.kt`:

```kotlin
enum class EmptyMode {
    Generic,            // network/no-quotes fallback (default copy)
    NoCategoryLines,    // a specific category is selected and is locally empty
}
```

`HomeEvent` gains `data class SelectCategory(val category: String?) : HomeEvent`.

### 5.3 Browse — main screen

```
EditorialTopBar(date = "09 / 08", greeting = "By mood")   ← greeting hardcoded
 14 categories · 499 lines                                ← subtitle
 LazyVerticalGrid(columns = 2, …)                          ← tiles
```

Each tile (1:1 aspect ratio):

```
#01                          ← labelSmall onSurfaceVariant
励志                          ← headlineSmall onBackground
128 lines                     ← labelSmall onSurfaceVariant
```

Tile click → `backStack.add(Route.CategoryDetail(category))` (Navigation 3, no `navController.navigate` — the app uses `SnapshotStateList<Route>` instead).

The view-model exposes a single `StateFlow<BrowseUiState>`:

```kotlin
data class BrowseUiState(
    val tiles: List<CategoryCount> = emptyList(),
    val totalQuotes: Int = 0,
    val totalCategories: Int = 0,
    val isLoading: Boolean = true,
)
```

### 5.4 CategoryDetail — secondary screen

```
[← back]  励志                              128 lines   ← top row
 IndexRow × N                                             ← LazyColumn
```

- Reuses `EditorialComponents.IndexRow` with `onSwipeOut = null`.
- Top row uses an `IconButton` with the system back arrow (`Icons.AutoMirrored.Filled.ArrowBack`); its `onClick` invokes the `onBack` lambda provided by `AppNavDisplay`. The system back gesture / button also calls `onBack` via the existing `onBack` parameter on `NavDisplay`.
- Empty list → `EditorialEmpty(title = "This category is empty", body = "Pull to sync or pick another.", actionLabel = "Back", onAction = onBack)`.
- Blank/empty `category` nav arg (defense in depth — `Route.CategoryDetail` should never carry an empty string because the caller `Uri.encode`s the chip name, but the type system can't enforce it) → `LaunchedEffect(category)` calls `onBack()` once and renders an empty `Box`.

## 6. Behavior & Error Handling

### 6.1 Home chip-tap reaction

When `HomeEvent.SelectCategory(category)` is received:

1. Write to `CategoryPreferenceStore.setSelectedCategory(category)`.
2. Update `_uiState.value.selectedCategory = category`.
3. **Reload** the quote only when **all** of the following hold:
   - `state.quote == null` (first load, or the previous pick was empty), **OR**
   - `category != null` **and** `state.quote!!.category != category` (user narrowed the universe to a category the current quote is not in).

   In plain words: going back to "All" never reloads (the user broadened, they did not ask for a new line); going from one specific category to another reloads; going from "All" to a category reloads **only if** the currently shown quote is not in that category.

Same-day pin behavior: the existing `DailyQuoteStore` pin is consulted by `getDailyQuote(null)` only. For `getDailyQuote(cat)` the pin is ignored per §4.4.

### 6.2 Empty category on Home

`HomeViewModel.onEvent(Load)` already catches `null` from `getDailyQuote` and sets the existing `state.quote = null`. The Home screen's empty branch needs a small extension because the action label is no longer always "Retry →" — it now depends on whether a category filter is active:

- `HomeUiState` gains a derived `emptyMode: EmptyMode` enum field, computed as:
  - `EmptyMode.Generic` (default — same copy as today: title "Nothing here yet", body "Check your connection and try again.", action "Retry →" → emits `HomeEvent.Load`).
  - `EmptyMode.NoCategoryLines` (when `state.quote == null && !state.isLoading && state.error == null && state.selectedCategory != null`): title "No lines in this category yet", body "This category hasn't been synced to your device.", action "Switch to All" → emits `HomeEvent.SelectCategory(null)`.

`HomeScreen`'s `key == "empty"` branch switches on `state.emptyMode` to pick copy + action.

### 6.3 Browse list empty

If `observeCategoryCounts()` emits an empty list (e.g. a fresh install before prepopulated DB initializes), `BrowseScreen` shows `EditorialEmpty(title = "Nothing to browse yet", body = "Pull to sync on Home.", actionLabel = "Go to Home", onAction = onGoHome)`.

### 6.4 Network & sync

- `DailySyncWorker` (existing) is untouched. The 24-hour sync continues regardless of category.
- No new sync is triggered when a category is selected or when a category list is empty.
- New categories added server-side propagate through the normal sync path and appear in chips and tiles on next Home/Browse open.

### 6.5 Error fallback

- DAO exception → Repository throws → ViewModel catches → existing `EditorialError` shown. This is identical to today's Home error UX.
- `CategoryPreferenceStore` DataStore read/write failure → fall back to `selectedCategory = null`. The chips strip renders with only "All" selected, no crash.
- Nav arg missing/empty on `CategoryDetailScreen` → back-stack popped; no crash.

### 6.6 Internationalization

- Category names are displayed as-is from the DB (existing data is a Chinese/English mix; no translation in v1).
- Static copy ("By mood", "14 categories · 499 lines", "Switch to All", "Nothing to browse yet", etc.) is hardcoded English to match the rest of the app.

## 7. Testing Strategy

Task-driven TDD. Tests live in `app/src/test/java/com/dailymind/...` following the MVP plan.

| Layer | Test class | Cases |
|---|---|---|
| DAO | `QuoteDaoCategoryTest` | `observeCategories` returns distinct non-null sorted asc; `observeCategoryCounts` returns counts ordered desc then asc; `observeByCategory(cat)` returns only rows with that category; `getRandomFiltered(null)` matches `getRandom`; `getRandomFiltered(cat)` returns only that category; `getRandomExcludingFiltered` excludes and filters; deleted rows excluded everywhere |
| Store | `CategoryPreferenceStoreTest` | default null; set then read; set null clears; multiple writes don't throw |
| Repository | `QuoteRepositoryCategoryTest` | `getDailyQuote(cat)` calls `getRandomFiltered(cat)` and never writes to `DailyQuoteStore`; `getDailyQuote(null)` preserves pin behavior; `getLocalRandomQuote` forwards exclude + category; `observeQuotesByCategory` maps entities to `Quote`; `observeCategoryCounts` returns DAO output |
| ViewModel | `HomeViewModelCategoryTest` | `SelectCategory(cat)` writes Store, updates state, triggers `Load` when current quote.category mismatches; `SelectCategory(null)` does **not** trigger Load when current quote has a non-null category; chip list reflects `observeCategoryCounts().take(6)`; `state.emptyMode` is `NoCategoryLines` when quote is null + a category is selected + no error, otherwise `Generic` |
| ViewModel | `BrowseViewModelTest` | empty counts → empty state; non-empty → tiles + totals; combine handles updates |
| ViewModel | `CategoryDetailViewModelTest` | valid category → forwards flow; empty flow → empty state; blank category → onBack called exactly once |
| Compose UI | `BrowseScreenTest` | renders 2-column grid; tile click invokes `onSelectCategory(category)` with the raw category string |
| Compose UI | `HomeChipsTest` | "All" selected by default; tapping non-All chip triggers `HomeEvent.SelectCategory(cat)`; "More…" chip visible when >6 categories; tapping "More…" opens `ModalBottomSheet` |

Existing tests to extend:

- `QuoteRepositoryTest` — add category-filter cases.
- `HomeViewModelTest` — add `SelectCategory` cases (no need to add a file; extend the existing one).

### Verification commands

- Full app unit tests: `server/gradlew.bat :app:test`
- Focused: `server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.browse.*"`

## 8. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| `CategoryPreferenceStore` adds a new key to an existing DataStore file that real users have on disk. | DataStore `Preferences` keys are forward-compatible; existing entries (`THEME`, `DAILY_QUOTE_*`, `LAST_SYNC`) are untouched. New users see default `null`. |
| Top-6 chips feels arbitrary; users in narrow categories miss a preferred one. | "More…" chip surfaces all categories. v1 ships top-6; a future spec can switch to "all in a row" if the category set stays small. |
| Hilt `HomeViewModel` constructor change breaks the `ViewModelProvider` in any test that constructs it manually. | All existing tests use `@HiltViewModel` injection; the only place that constructs `HomeViewModel` directly is its own test file, which will be updated in the same commit. |
| `CategoryDetailScreen` route arg `category` contains URL-unsafe characters. | The route uses a typed `Route.CategoryDetail(category: String)` data class, not a URL path. No encoding/decoding is needed; the String is passed verbatim. |

## 9. Out of Scope (Future)

- Recommendation algorithm for "what category to suggest" (user explicitly deferred).
- A dedicated quote-detail screen reachable from `CategoryDetailScreen` rows.
- Favorite toggle from `CategoryDetailScreen`.
- Sort / search / paginate inside a category list.
- Server-side filtering endpoints (the existing `GET /api/v1/sync/quotes` is sufficient because the client already holds the full 499-quote prepopulated DB).
- Browse tab analytics (which categories are tapped, etc.).

## 10. Open Questions

None at the time of writing.
