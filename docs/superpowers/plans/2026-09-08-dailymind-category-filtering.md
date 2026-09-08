# DailyMind Category Filtering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a single-select Home category chip strip and a new **Browse** tab (2-column category grid → per-category `IndexRow` list). Default selection is **All**; specific categories are strictly filtered. Server unchanged; no schema migration.

**Architecture:** Two-layer design (Material 3 foundation, Editorial visual layer per spec §3.2). Bottom-up TDD: DataStore → DAO → Repository → ViewModels → Screens → Navigation. All new state lives in Room (queries) + DataStore (one `String?` preference); no schema migration because we only add DAO methods and a single DataStore key.

**Tech Stack:** Kotlin 2.3.20 + Room 2.8.4 (DAO-only addition) + DataStore Preferences 1.1.3 + Hilt 2.60.1 + Navigation 3 1.0.0 + Compose BOM 2025.10.01. Tests: JUnit4 + MockK + kotlinx-coroutines-test (existing) + **Robolectric 4.14** + **Compose UI test** (new — added in Task 0 because the project's `app/build.gradle.kts` currently lacks both).

**Spec:** `docs/superpowers/specs/2026-09-08-dailymind-category-filtering-design.md`

**Verification commands (aggregate):**

```bash
# All app unit tests
server/gradlew.bat :app:test

# Focused: browse + home chips only
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.browse.*" --tests "com.dailymind.feature.home.HomeChipsTest" --tests "com.dailymind.feature.home.HomeViewModelCategoryTest"
```

---

## File Structure

### New files
```
app/src/main/java/com/dailymind/core/database/CategoryCount.kt
app/src/main/java/com/dailymind/core/datastore/CategoryPreferenceStore.kt
app/src/main/java/com/dailymind/feature/browse/BrowseScreen.kt
app/src/main/java/com/dailymind/feature/browse/BrowseViewModel.kt
app/src/main/java/com/dailymind/feature/browse/CategoryDetailScreen.kt
app/src/main/java/com/dailymind/feature/browse/CategoryDetailViewModel.kt

app/src/test/java/com/dailymind/core/database/dao/QuoteDaoCategoryTest.kt
app/src/test/java/com/dailymind/core/datastore/CategoryPreferenceStoreTest.kt
app/src/test/java/com/dailymind/core/data/QuoteRepositoryCategoryTest.kt
app/src/test/java/com/dailymind/feature/browse/BrowseViewModelTest.kt
app/src/test/java/com/dailymind/feature/browse/CategoryDetailViewModelTest.kt
app/src/test/java/com/dailymind/feature/browse/BrowseScreenTest.kt
app/src/test/java/com/dailymind/feature/home/HomeViewModelCategoryTest.kt
app/src/test/java/com/dailymind/feature/home/HomeChipsTest.kt
```

### Modified files
```
gradle/libs.versions.toml                                +robolectric, +compose-ui-test, +androidx-test-core
app/build.gradle.kts                                     +test deps; testOptions.unitTests
app/src/main/java/com/dailymind/core/database/dao/QuoteDao.kt        +5 methods
app/src/main/java/com/dailymind/core/datastore/PreferencesDataStore.kt  +CATEGORY key
app/src/main/java/com/dailymind/core/data/QuoteRepository.kt         optional category params + 3 new flows
app/src/main/java/com/dailymind/core/data/QuoteRepositoryImpl.kt     conditional branch on category
app/src/main/java/com/dailymind/core/navigation/Route.kt             +Browse, +CategoryDetail
app/src/main/java/com/dailymind/core/navigation/AppNavDisplay.kt     +Browse tab, +Browse/CategoryDetail entries
app/src/main/java/com/dailymind/feature/home/HomeUiState.kt          +EmptyMode enum, +selectedCategory, +availableCategories, +emptyMode
app/src/main/java/com/dailymind/feature/home/HomeViewModel.kt        +CategoryPreferenceStore ctor param, +SelectCategory event, +category observe, +emptyMode compute
app/src/main/java/com/dailymind/feature/home/HomeScreen.kt           +CategoryChips composable, +EmptyMode switch in empty branch
app/src/test/java/com/dailymind/feature/home/HomeViewModelTest.kt    +SelectCategory cases (extend existing file)
app/src/test/java/com/dailymind/core/data/QuoteRepositoryTest.kt     +category-filter cases (extend existing file)
app/src/test/java/com/dailymind/core/datastore/PreferencesTest.kt    +CATEGORY key existence assertion
app/src/test/java/com/dailymind/core/navigation/RouteTest.kt         +new route distinctness assertions
```

---

## Task 0: Add Robolectric + Compose UI test dependencies

The project's `app/build.gradle.kts` currently has `testImplementation(libs.junit)`, `testImplementation(libs.mockk)`, `testImplementation(libs.kotlinx.coroutines.test)`, and `testImplementation(libs.okhttp.mockwebserver)` only. Robolectric and Compose UI test are not on the classpath, so DAO tests in `:app:test` and Compose UI tests cannot run without them. This task adds them.

**Files:**
- Modify: `gradle/libs.versions.toml` (add `robolectric`, `compose-ui-test`, `compose-ui-test-manifest`)
- Modify: `app/build.gradle.kts` (add `testImplementation(...)`, configure `testOptions.unitTests`)

- [ ] **Step 1: Add version + library entries to `gradle/libs.versions.toml`**

Append inside `[versions]` (alphabetical near the end):
```toml
robolectric = "4.14.1"
```

Append inside `[libraries]`:
```toml
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
androidx-compose-ui-test = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
```

Note: `androidx-compose-ui-test` and `androidx-compose-ui-test-manifest` resolve to BOM-pinned versions automatically.

- [ ] **Step 2: Add test dependencies and `testOptions` to `app/build.gradle.kts`**

Inside the `android { ... }` block, add:
```kotlin
testOptions {
    unitTests {
        isIncludeAndroidResources = true
        isReturnDefaultValues = true
    }
}
```

Inside `dependencies { ... }` (in the `testImplementation` group), add:
```kotlin
testImplementation(libs.robolectric)
testImplementation(platform(libs.androidx.compose.bom))
testImplementation(libs.androidx.compose.ui.test)
testImplementation(libs.androidx.compose.ui.test.manifest)
testImplementation(libs.androidx.test.core)
testImplementation(libs.androidx.room.testing)
```

`androidx.compose.ui:ui-test-manifest` must be on `debugImplementation` too (Robolectric needs it on the classpath at debug compile time):
```kotlin
debugImplementation(libs.androidx.compose.ui.test.manifest)
```

- [ ] **Step 3: Run the existing test suite to confirm nothing regressed**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.ScaffoldingTest"
```
Expected: `BUILD SUCCESSFUL` and the existing scaffolding test still passes.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add Robolectric and Compose UI test deps for category-filter spec"
```

---

## Task 1: Add `CategoryCount` POJO + 5 new `QuoteDao` methods (TDD)

**Files:**
- Create: `app/src/main/java/com/dailymind/core/database/CategoryCount.kt`
- Create: `app/src/test/java/com/dailymind/core/database/dao/QuoteDaoCategoryTest.kt`
- Modify: `app/src/main/java/com/dailymind/core/database/dao/QuoteDao.kt`

- [ ] **Step 1: Write the failing DAO test**

Create `app/src/test/java/com/dailymind/core/database/dao/QuoteDaoCategoryTest.kt`:

```kotlin
package com.dailymind.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dailymind.core.database.CategoryCount
import com.dailymind.core.database.DailyMindDatabase
import com.dailymind.core.database.entity.QuoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class QuoteDaoCategoryTest {
    private lateinit var db: DailyMindDatabase
    private lateinit var dao: QuoteDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DailyMindDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.quoteDao()
    }

    @After fun tearDown() { db.close() }

    private fun entity(id: String, cat: String? = null, deletedAt: Long? = null, updatedAt: Long = 1L) =
        QuoteEntity(id, "Q-$id", null, "A-$id", cat, 1, null, null, updatedAt, deletedAt)

    @Test fun `observeCategories returns distinct non-null categories sorted asc`() = runTest {
        dao.upsertAll(listOf(
            entity("1", "励志"),
            entity("2", "爱情"),
            entity("3", "励志"),
            entity("4", null),
        ))
        assertEquals(listOf("励志", "爱情"), dao.observeCategories().first())
    }

    @Test fun `observeCategories excludes deleted rows`() = runTest {
        dao.upsertAll(listOf(
            entity("1", "励志"),
            entity("2", "爱情", deletedAt = 1L),
        ))
        assertEquals(listOf("励志"), dao.observeCategories().first())
    }

    @Test fun `observeCategoryCounts returns counts ordered desc then asc`() = runTest {
        dao.upsertAll(listOf(
            entity("1", "励志"),
            entity("2", "励志"),
            entity("3", "励志"),
            entity("4", "爱情"),
            entity("5", "爱情"),
            entity("6", "孤独"),
        ))
        assertEquals(
            listOf(
                CategoryCount("励志", 3),
                CategoryCount("爱情", 2),
                CategoryCount("孤独", 1),
            ),
            dao.observeCategoryCounts().first()
        )
    }

    @Test fun `observeByCategory returns only matching rows ordered by id`() = runTest {
        dao.upsertAll(listOf(
            entity("a", "励志"),
            entity("b", "爱情"),
            entity("c", "励志"),
        ))
        val rows = dao.observeByCategory("励志").first()
        assertEquals(listOf("a", "c"), rows.map { it.id })
    }

    @Test fun `getRandomFiltered with null returns any row`() = runTest {
        dao.upsertAll(listOf(entity("1", "励志"), entity("2", "爱情")))
        val picked = dao.getRandomFiltered(null)
        assertNotNull(picked)
        assertTrue(picked!!.id in listOf("1", "2"))
    }

    @Test fun `getRandomFiltered with category returns only that category`() = runTest {
        dao.upsertAll(listOf(entity("1", "励志"), entity("2", "爱情"), entity("3", "励志")))
        repeat(20) {
            assertEquals("励志", dao.getRandomFiltered("励志")?.category)
        }
    }

    @Test fun `getRandomExcludingFiltered excludes id and filters category`() = runTest {
        dao.upsertAll(listOf(
            entity("1", "励志"),
            entity("2", "励志"),
            entity("3", "爱情"),
        ))
        val picked = dao.getRandomExcludingFiltered("1", "励志")
        assertEquals("2", picked?.id)
    }

    @Test fun `getRandomFiltered returns null when category is empty`() = runTest {
        dao.upsertAll(listOf(entity("1", "爱情")))
        assertNull(dao.getRandomFiltered("励志"))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.core.database.dao.QuoteDaoCategoryTest"
```
Expected: COMPILATION FAILURE because `CategoryCount` and the 5 new DAO methods don't exist yet.

- [ ] **Step 3: Add `CategoryCount` POJO**

Create `app/src/main/java/com/dailymind/core/database/CategoryCount.kt`:

```kotlin
package com.dailymind.core.database

data class CategoryCount(
    val category: String,
    val count: Int,
)
```

- [ ] **Step 4: Add 5 new methods to `QuoteDao`**

Modify `app/src/main/java/com/dailymind/core/database/dao/QuoteDao.kt`. Keep all existing methods unchanged. Append:

```kotlin
import com.dailymind.core.database.CategoryCount
// (no other imports needed)

@Query("""
    SELECT DISTINCT category FROM quote
    WHERE category IS NOT NULL AND deletedAt IS NULL
    ORDER BY category
""")
fun observeCategories(): kotlinx.coroutines.flow.Flow<List<String>>

@Query("""
    SELECT category AS category, COUNT(*) AS count FROM quote
    WHERE category IS NOT NULL AND deletedAt IS NULL
    GROUP BY category
    ORDER BY count DESC, category ASC
""")
fun observeCategoryCounts(): kotlinx.coroutines.flow.Flow<List<CategoryCount>>

@Query("""
    SELECT * FROM quote
    WHERE category = :category AND deletedAt IS NULL
    ORDER BY id
""")
fun observeByCategory(category: String): kotlinx.coroutines.flow.Flow<List<QuoteEntity>>

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

- [ ] **Step 5: Run the test to verify it passes**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.core.database.dao.QuoteDaoCategoryTest"
```
Expected: `BUILD SUCCESSFUL` and all 8 tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/dailymind/core/database/CategoryCount.kt \
        app/src/main/java/com/dailymind/core/database/dao/QuoteDao.kt \
        app/src/test/java/com/dailymind/core/database/dao/QuoteDaoCategoryTest.kt
git commit -m "feat(dao): add category-list, category-count, and category-filtered random queries"
```

---

## Task 2: `PreferencesKeys.CATEGORY` + `CategoryPreferenceStore` (TDD)

**Files:**
- Modify: `app/src/main/java/com/dailymind/core/datastore/PreferencesDataStore.kt` (+1 key)
- Create: `app/src/main/java/com/dailymind/core/datastore/CategoryPreferenceStore.kt`
- Modify: `app/src/test/java/com/dailymind/core/datastore/PreferencesTest.kt` (+1 assertion)
- Create: `app/src/test/java/com/dailymind/core/datastore/CategoryPreferenceStoreTest.kt`

- [ ] **Step 1: Add failing assertion to `PreferencesTest`**

Modify `app/src/test/java/com/dailymind/core/datastore/PreferencesTest.kt`. Append inside the class:

```kotlin
@Test fun `category preference key exists`() {
    assertNotNull(PreferencesKeys.CATEGORY)
}
```

- [ ] **Step 2: Add the `CATEGORY` key to `PreferencesDataStore.kt`**

Modify `app/src/main/java/com/dailymind/core/datastore/PreferencesDataStore.kt`. Inside `object PreferencesKeys`, append:

```kotlin
val CATEGORY = stringPreferencesKey("selected_category")
```

- [ ] **Step 3: Run `PreferencesTest` to verify the new assertion passes**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.core.datastore.PreferencesTest"
```
Expected: PASS.

- [ ] **Step 4: Write the failing `CategoryPreferenceStoreTest`**

Create `app/src/test/java/com/dailymind/core/datastore/CategoryPreferenceStoreTest.kt`:

```kotlin
package com.dailymind.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryPreferenceStoreTest {
    private val empty: DataStore<Preferences> = mockk(relaxed = true)
    private val backing: MutablePreferences = mutablePreferencesOf()
    private val store: DataStore<Preferences> = mockk(relaxed = true).also {
        every { it.data } returns flowOf(backing)
    }

    @Test fun `selectedCategory defaults to null when key absent`() = runTest {
        val s = CategoryPreferenceStore(store)
        assertNull(s.selectedCategory.first())
    }

    @Test fun `setSelectedCategory(value) writes the value`() = runTest {
        val s = CategoryPreferenceStore(store)
        s.setSelectedCategory("励志")
        assertEquals("励志", s.selectedCategory.first())
    }

    @Test fun `setSelectedCategory(null) clears the value`() = runTest {
        backing.set(PreferencesKeys.CATEGORY, "励志")
        val s = CategoryPreferenceStore(store)
        s.setSelectedCategory(null)
        assertNull(s.selectedCategory.first())
    }

    @Test fun `setSelectedCategory is safe to call twice`() = runTest {
        val s = CategoryPreferenceStore(store)
        s.setSelectedCategory("励志")
        s.setSelectedCategory("爱情")
        assertEquals("爱情", s.selectedCategory.first())
    }
}
```

- [ ] **Step 5: Run the test to verify it fails (compilation)**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.core.datastore.CategoryPreferenceStoreTest"
```
Expected: COMPILATION FAILURE because `CategoryPreferenceStore` doesn't exist.

- [ ] **Step 6: Implement `CategoryPreferenceStore`**

Create `app/src/main/java/com/dailymind/core/datastore/CategoryPreferenceStore.kt`:

```kotlin
package com.dailymind.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

- [ ] **Step 7: Run the test to verify it passes**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.core.datastore.CategoryPreferenceStoreTest"
```
Expected: PASS for all 4 tests.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/dailymind/core/datastore/PreferencesDataStore.kt \
        app/src/main/java/com/dailymind/core/datastore/CategoryPreferenceStore.kt \
        app/src/test/java/com/dailymind/core/datastore/PreferencesTest.kt \
        app/src/test/java/com/dailymind/core/datastore/CategoryPreferenceStoreTest.kt
git commit -m "feat(datastore): add CategoryPreferenceStore for Home category selection"
```

---

## Task 3: Extend `QuoteRepository` and `QuoteRepositoryImpl` (TDD)

**Files:**
- Modify: `app/src/main/java/com/dailymind/core/data/QuoteRepository.kt`
- Modify: `app/src/main/java/com/dailymind/core/data/QuoteRepositoryImpl.kt`
- Modify: `app/src/test/java/com/dailymind/core/data/QuoteRepositoryTest.kt` (extend existing file)
- Create: `app/src/test/java/com/dailymind/core/data/QuoteRepositoryCategoryTest.kt`

- [ ] **Step 1: Write the failing repository tests**

Create `app/src/test/java/com/dailymind/core/data/QuoteRepositoryCategoryTest.kt`:

```kotlin
package com.dailymind.core.data

import com.dailymind.core.database.CategoryCount
import com.dailymind.core.database.dao.FavoriteDao
import com.dailymind.core.database.dao.HistoryDao
import com.dailymind.core.database.dao.QuoteDao
import com.dailymind.core.database.entity.QuoteEntity
import com.dailymind.core.datastore.DailyQuoteStore
import com.dailymind.core.network.ApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class QuoteRepositoryCategoryTest {
    private val entity = QuoteEntity("9", "c", "t", "a", "love", 1, null, null, 1L, null)
    private val entity2 = QuoteEntity("10", "c2", "t", "a", "life", 1, null, null, 1L, null)

    private fun repo(
        dao: QuoteDao = mockk(relaxed = true),
        api: ApiService = mockk(),
        historyDao: HistoryDao = mockk(relaxed = true),
        store: DailyQuoteStore = mockk(relaxed = true),
        favorites: FavoriteDao = mockk(relaxed = true)
    ) = QuoteRepositoryImpl(dao, api, historyDao, store, favorites)

    @Test fun `getDailyQuote with category ignores pin and calls getRandomFiltered`() = runTest {
        val dao = mockk<QuoteDao>(relaxed = true)
        val store = mockk<DailyQuoteStore>(relaxed = true)
        val api = mockk<ApiService>()
        coEvery { store.getPinned() } returns null
        coEvery { dao.getRandomFiltered("love") } returns entity
        val q = repo(dao = dao, api = api, store = store).getDailyQuote("love")
        assertNotNull(q)
        assertEquals("love", q!!.category)
        coVerify { dao.getRandomFiltered("love") }
        coVerify(exactly = 0) { store.setPinned(any(), any()) }
    }

    @Test fun `getDailyQuote with category returns null when category is empty`() = runTest {
        val dao = mockk<QuoteDao>(relaxed = true)
        val store = mockk<DailyQuoteStore>(relaxed = true)
        val api = mockk<ApiService>()
        coEvery { store.getPinned() } returns null
        coEvery { dao.getRandomFiltered("love") } returns null
        assertNull(repo(dao = dao, api = api, store = store).getDailyQuote("love"))
    }

    @Test fun `getDailyQuote with null preserves pin behavior`() = runTest {
        val dao = mockk<QuoteDao>(relaxed = true)
        val store = mockk<DailyQuoteStore>(relaxed = true)
        coEvery { store.getPinned() } returns null
        coEvery { dao.getRandom() } returns entity
        repo(dao = dao, store = store).getDailyQuote()
        coVerify { dao.getRandom() }
    }

    @Test fun `getLocalRandomQuote forwards excludeId and category to new DAO method`() = runTest {
        val dao = mockk<QuoteDao>()
        coEvery { dao.getRandomExcludingFiltered("1", "love") } returns entity
        val q = repo(dao = dao).getLocalRandomQuote(excludeId = "1", category = "love")
        assertEquals("9", q?.id)
    }

    @Test fun `observeQuotesByCategory maps entities to domain models`() = runTest {
        val dao = mockk<QuoteDao>()
        coEvery { dao.observeByCategory("love") } returns flowOf(listOf(entity))
        val list = repo(dao = dao).observeQuotesByCategory("love").first()
        assertEquals(listOf("9"), list.map { it.id })
        assertEquals("love", list[0].category)
    }

    @Test fun `observeCategories forwards DAO flow`() = runTest {
        val dao = mockk<QuoteDao>()
        coEvery { dao.observeCategories() } returns flowOf(listOf("love", "life"))
        assertEquals(listOf("love", "life"), repo(dao = dao).observeCategories().first())
    }

    @Test fun `observeCategoryCounts forwards DAO flow`() = runTest {
        val dao = mockk<QuoteDao>()
        coEvery { dao.observeCategoryCounts() } returns flowOf(listOf(CategoryCount("love", 1)))
        assertEquals(listOf(CategoryCount("love", 1)), repo(dao = dao).observeCategoryCounts().first())
    }
}
```

- [ ] **Step 2: Run the test to verify it fails (compilation)**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.core.data.QuoteRepositoryCategoryTest"
```
Expected: COMPILATION FAILURE because `QuoteRepository` doesn't have `category` params or the 3 new flows.

- [ ] **Step 3: Update `QuoteRepository` interface**

Modify `app/src/main/java/com/dailymind/core/data/QuoteRepository.kt`:

```kotlin
package com.dailymind.core.data

import com.dailymind.core.database.CategoryCount
import com.dailymind.core.model.Quote
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {
    fun observeQuotes(): Flow<List<Quote>>
    fun observeFavorites(): Flow<List<Quote>>

    suspend fun getDailyQuote(category: String? = null): Quote?

    suspend fun getRandomQuote(): Quote

    suspend fun getLocalRandomQuote(
        excludeId: String? = null,
        category: String? = null,
    ): Quote?

    fun observeFavoriteIds(): Flow<Set<String>>
    suspend fun sync(): Result<Unit>
    suspend fun toggleFavorite(quoteId: String)

    fun observeCategories(): Flow<List<String>>
    fun observeCategoryCounts(): Flow<List<CategoryCount>>
    fun observeQuotesByCategory(category: String): Flow<List<Quote>>
}
```

- [ ] **Step 4: Update `QuoteRepositoryImpl`**

Modify `app/src/main/java/com/dailymind/core/data/QuoteRepositoryImpl.kt`. Replace the entire file with:

```kotlin
package com.dailymind.core.data

import com.dailymind.core.database.CategoryCount
import com.dailymind.core.database.dao.FavoriteDao
import com.dailymind.core.database.dao.HistoryDao
import com.dailymind.core.database.dao.QuoteDao
import com.dailymind.core.database.entity.FavoriteEntity
import com.dailymind.core.database.entity.HistoryEntity
import com.dailymind.core.database.entity.QuoteEntity
import com.dailymind.core.datastore.DailyQuoteStore
import com.dailymind.core.datastore.todayEpochDay
import com.dailymind.core.model.Quote
import com.dailymind.core.network.ApiService
import com.dailymind.core.network.dto.QuoteDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuoteRepositoryImpl @Inject constructor(
    private val dao: QuoteDao,
    private val api: ApiService,
    private val historyDao: HistoryDao,
    private val dailyStore: DailyQuoteStore,
    private val favorites: FavoriteDao
) : QuoteRepository {
    override fun observeQuotes(): Flow<List<Quote>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeFavorites(): Flow<List<Quote>> =
        favorites.observeFavorites().map { list -> list.map { it.toModel() } }

    override suspend fun getDailyQuote(category: String?): Quote? {
        if (category != null) {
            return dao.getRandomFiltered(category)?.toModel()
        }
        val today = todayEpochDay()
        dailyStore.getPinned()?.takeIf { it.day == today }?.let { pinned ->
            dao.getById(pinned.quoteId)
                ?.takeIf { it.deletedAt == null }
                ?.toModel()
                ?.let { return it }
        }
        val seen = historyDao.getAllQuoteIds().toSet()
        var pick: QuoteEntity? = null
        for (i in 0 until MAX_REROLL) {
            val candidate = dao.getRandom() ?: break
            pick = candidate
            if (candidate.id !in seen) break
        }
        val entity = pick?.takeIf { it.deletedAt == null }
            ?: dao.getAll().firstOrNull { it.deletedAt == null }
            ?: return fetchNetworkDaily()
        recordToday(entity.id)
        return entity.toModel()
    }

    override suspend fun getLocalRandomQuote(excludeId: String?, category: String?): Quote? =
        if (excludeId == null) dao.getRandomFiltered(category)?.toModel()
        else dao.getRandomExcludingFiltered(excludeId, category)?.toModel()

    override fun observeFavoriteIds(): Flow<Set<String>> =
        favorites.observeFavorites().map { list -> list.map { it.id }.toSet() }

    override suspend fun getRandomQuote(): Quote =
        try {
            val dto = api.getRandomQuote()
            dao.upsertAll(listOf(dto.toEntity()))
            dto.toModel()
        } catch (e: Exception) {
            dao.getRandom()?.toModel()
                ?: throw IllegalStateException("No cached quote and network unavailable")
        }

    override suspend fun sync(): Result<Unit> = runCatching {
        val max = dao.getMaxUpdatedAt() ?: 0L
        var cursor: String? = null
        do {
            val res = api.syncQuotes(updatedAfter = max, cursor = cursor)
            dao.upsertAll(res.items.map { it.toEntity() })
            cursor = res.nextCursor
        } while (cursor != null)
    }

    override suspend fun toggleFavorite(quoteId: String) {
        if (favorites.getIdsOnce().contains(quoteId)) {
            favorites.deleteById(quoteId)
        } else {
            favorites.upsert(FavoriteEntity(quoteId, System.currentTimeMillis()))
        }
    }

    override fun observeCategories(): Flow<List<String>> = dao.observeCategories()

    override fun observeCategoryCounts(): Flow<List<CategoryCount>> = dao.observeCategoryCounts()

    override fun observeQuotesByCategory(category: String): Flow<List<Quote>> =
        dao.observeByCategory(category).map { list -> list.map { it.toModel() } }

    private suspend fun fetchNetworkDaily(): Quote? = try {
        val dto = api.getDailyQuote()
        dao.upsertAll(listOf(dto.toEntity()))
        recordToday(dto.id)
        dto.toModel()
    } catch (e: Exception) {
        null
    }

    private suspend fun recordToday(quoteId: String) {
        historyDao.insert(HistoryEntity(quoteId = quoteId, viewedAt = System.currentTimeMillis()))
        dailyStore.setPinned(todayEpochDay(), quoteId)
    }

    private fun QuoteEntity.toModel() = Quote(id, content, translation, author, category, difficulty, audioUrl, imageUrl, updatedAt)
    private fun QuoteDto.toModel() = Quote(id, content, translation, author, category, difficulty, audioUrl, imageUrl, updatedAt)
    private fun QuoteDto.toEntity() = QuoteEntity(id, content, translation, author, category, difficulty, audioUrl, imageUrl, updatedAt, deletedAt)

    companion object {
        private const val MAX_REROLL = 20
    }
}
```

- [ ] **Step 5: Run the new repository test to verify it passes**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.core.data.QuoteRepositoryCategoryTest"
```
Expected: PASS for all 7 tests.

- [ ] **Step 6: Re-run the full repository test to confirm no regression**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.core.data.QuoteRepositoryTest"
```
Expected: PASS for all 14 pre-existing tests (note: `getLocalRandomQuote` no longer calls `dao.getRandomExcluding` directly, so the mock-returning test for that path must already work; if a test fails because it set up `dao.getRandomExcluding("1")` and the new impl calls `getRandomExcludingFiltered("1", null)`, the existing test will break — see Step 7).

- [ ] **Step 7: If `QuoteRepositoryTest` regresses, update its 3 `getLocalRandomQuote` cases**

`QuoteRepositoryTest` (lines 131–152) has three `getLocalRandomQuote` cases that use the old `dao.getRandomExcluding(...)` and `dao.getRandom()` methods. With the new impl, these calls are replaced by `dao.getRandomExcludingFiltered("1", null)` and `dao.getRandomFiltered(null)`. Update each case in `QuoteRepositoryTest.kt`:

For the test on line 131 (`getLocalRandomQuote excludes specified id`):
```kotlin
@Test fun `getLocalRandomQuote excludes specified id`() = runTest {
    val dao = mockk<QuoteDao>()
    val q2 = QuoteEntity("2", "B", "乙", "Y", "c", 1, null, null, 2L, null)
    coEvery { dao.getRandomExcludingFiltered("1", null) } returns q2
    val q = repo(dao = dao).getLocalRandomQuote(excludeId = "1")!!
    assertEquals("2", q.id)
    coVerify { dao.getRandomExcludingFiltered("1", null) }
}
```

For the test on line 140 (`getLocalRandomQuote returns null on empty db`):
```kotlin
@Test fun `getLocalRandomQuote returns null on empty db`() = runTest {
    val dao = mockk<QuoteDao>()
    coEvery { dao.getRandomExcludingFiltered("any", null) } returns null
    assertNull(repo(dao = dao).getLocalRandomQuote(excludeId = "any"))
}
```

For the test on line 146 (`getLocalRandomQuote calls getRandom when excludeId null`):
```kotlin
@Test fun `getLocalRandomQuote calls getRandom when excludeId null`() = runTest {
    val dao = mockk<QuoteDao>()
    coEvery { dao.getRandomFiltered(null) } returns entity
    val q = repo(dao = dao).getLocalRandomQuote(excludeId = null)!!
    assertEquals("9", q.id)
    coVerify(exactly = 0) { dao.getRandomExcludingFiltered(any(), any()) }
}
```

- [ ] **Step 8: Re-run both repository tests**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.core.data.QuoteRepositoryTest" --tests "com.dailymind.core.data.QuoteRepositoryCategoryTest"
```
Expected: PASS for all 21 tests combined (14 pre-existing + 7 new).

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/dailymind/core/data/QuoteRepository.kt \
        app/src/main/java/com/dailymind/core/data/QuoteRepositoryImpl.kt \
        app/src/test/java/com/dailymind/core/data/QuoteRepositoryTest.kt \
        app/src/test/java/com/dailymind/core/data/QuoteRepositoryCategoryTest.kt
git commit -m "feat(repo): forward optional category to DAO and expose category flows"
```

---

## Task 4: Add `Route.Browse` and `Route.CategoryDetail`

**Files:**
- Modify: `app/src/main/java/com/dailymind/core/navigation/Route.kt`
- Modify: `app/src/main/java/com/dailymind/core/navigation/AppNavDisplay.kt`
- Modify: `app/src/test/java/com/dailymind/core/navigation/RouteTest.kt` (extend)

- [ ] **Step 1: Extend `RouteTest` with new distinctness assertions**

Modify `app/src/test/java/com/dailymind/core/navigation/RouteTest.kt`. Replace contents with:

```kotlin
package com.dailymind.core.navigation

import org.junit.Assert.*
import org.junit.Test

class RouteTest {
    @Test fun `routes are distinct`() {
        assertNotEquals(Route.Home, Route.Me)
        assertNotEquals(Route.Home, Route.Browse)
        assertNotEquals(Route.Me, Route.Browse)
        assertNotEquals<Route>(Route.Browse, Route.CategoryDetail("love"))
    }

    @Test fun `CategoryDetail carries the category string`() {
        assertEquals("love", Route.CategoryDetail("love").category)
    }
}
```

- [ ] **Step 2: Run to verify it fails (compilation)**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.core.navigation.RouteTest"
```
Expected: COMPILATION FAILURE because `Route.Browse` and `Route.CategoryDetail` don't exist.

- [ ] **Step 3: Add the new `Route` members**

Modify `app/src/main/java/com/dailymind/core/navigation/Route.kt`:

```kotlin
package com.dailymind.core.navigation

sealed interface Route {
    data object Home : Route
    data object Me : Route
    data object Browse : Route
    data class CategoryDetail(val category: String) : Route
    data object History : Route
    data object Profile : Route
}
```

- [ ] **Step 4: Run `RouteTest` to verify it passes**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.core.navigation.RouteTest"
```
Expected: PASS.

- [ ] **Step 5: Update `AppNavDisplay` to 3 tabs + new entries (no `BrowseScreen`/`CategoryDetailScreen` symbols yet — wire them as `{}` placeholders to compile)**

Modify `app/src/main/java/com/dailymind/core/navigation/AppNavDisplay.kt`. Replace contents with:

```kotlin
package com.dailymind.core.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.dailymind.core.designsystem.BottomTab
import com.dailymind.core.designsystem.EditorialBottomBar
import com.dailymind.feature.browse.BrowseScreen
import com.dailymind.feature.browse.CategoryDetailScreen
import com.dailymind.feature.home.HomeScreen
import com.dailymind.feature.me.MeScreen

private val Tabs = listOf(
    BottomTab("home", "Today"),
    BottomTab("browse", "Browse"),
    BottomTab("me", "Me"),
)

private fun Route.tabId(): String = when (this) {
    is Route.Home -> "home"
    is Route.Me -> "me"
    is Route.Browse, is Route.CategoryDetail -> "browse"
    else -> "home"
}

@Composable
fun AppNavDisplay() {
    val backStack = remember { mutableStateListOf<Route>(Route.Home) }
    fun select(route: Route) {
        backStack.remove(route)
        backStack.add(route)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            entryProvider = { route ->
                when (route) {
                    is Route.Home -> NavEntry(route) { HomeScreen() }
                    is Route.Me -> NavEntry(route) { MeScreen(onExplore = { select(Route.Home) }) }
                    is Route.Browse -> NavEntry(route) {
                        BrowseScreen(onSelectCategory = { backStack.add(Route.CategoryDetail(it)) })
                    }
                    is Route.CategoryDetail -> NavEntry(route) {
                        CategoryDetailScreen(
                            category = route.category,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                    else -> NavEntry(route) { }
                }
            },
            modifier = Modifier.weight(1f)
        )
        EditorialBottomBar(
            tabs = Tabs,
            selectedId = backStack.lastOrNull()?.tabId() ?: "home",
            onSelect = { id ->
                select(
                    when (id) {
                        "me" -> Route.Me
                        "browse" -> Route.Browse
                        else -> Route.Home
                    }
                )
            }
        )
    }
}
```

Note: the file imports `BrowseScreen` and `CategoryDetailScreen` from `com.dailymind.feature.browse.*` which don't exist yet — this step will not compile until Task 6 produces them. That's fine; the compile error is resolved once Task 5's ViewModels + Task 6's screens are in place. We proceed and finish the `AppNavDisplay` change here so it's done in one commit.

- [ ] **Step 6: Commit the navigation change (compile-broken; intentional)**

```bash
git add app/src/main/java/com/dailymind/core/navigation/Route.kt \
        app/src/main/java/com/dailymind/core/navigation/AppNavDisplay.kt \
        app/src/test/java/com/dailymind/core/navigation/RouteTest.kt
git commit -m "feat(nav): add Browse and CategoryDetail routes, 3-tab bottom bar"
```

(The next task creates the missing ViewModels and the task after that creates the screens, at which point the whole module compiles again. We don't run `:app:test` until then.)

---

## Task 5: `BrowseViewModel` and `CategoryDetailViewModel` (TDD)

**Files:**
- Create: `app/src/main/java/com/dailymind/feature/browse/BrowseViewModel.kt`
- Create: `app/src/main/java/com/dailymind/feature/browse/CategoryDetailViewModel.kt`
- Create: `app/src/test/java/com/dailymind/feature/browse/BrowseViewModelTest.kt`
- Create: `app/src/test/java/com/dailymind/feature/browse/CategoryDetailViewModelTest.kt`

- [ ] **Step 1: Write the failing `BrowseViewModelTest`**

Create `app/src/test/java/com/dailymind/feature/browse/BrowseViewModelTest.kt`:

```kotlin
package com.dailymind.feature.browse

import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.database.CategoryCount
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `empty counts produces empty state with loading off`() {
        val counts = MutableStateFlow<List<CategoryCount>>(emptyList())
        val repo = mockk<QuoteRepository>()
        every { repo.observeCategoryCounts() } returns counts
        val vm = BrowseViewModel(repo)
        assertEquals(emptyList<CategoryCount>(), vm.state.value.tiles)
        assertEquals(0, vm.state.value.totalQuotes)
        assertEquals(0, vm.state.value.totalCategories)
        assertEquals(false, vm.state.value.isLoading)
    }

    @Test fun `non-empty counts computes totals and tiles`() {
        val counts = MutableStateFlow(
            listOf(CategoryCount("love", 3), CategoryCount("life", 2), CategoryCount("孤独", 1))
        )
        val repo = mockk<QuoteRepository>()
        every { repo.observeCategoryCounts() } returns counts
        val vm = BrowseViewModel(repo)
        assertEquals(6, vm.state.value.totalQuotes)
        assertEquals(3, vm.state.value.totalCategories)
        assertEquals(3, vm.state.value.tiles.size)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails (compilation)**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.browse.BrowseViewModelTest"
```
Expected: COMPILATION FAILURE because `BrowseViewModel` doesn't exist.

- [ ] **Step 3: Write the failing `CategoryDetailViewModelTest`**

Create `app/src/test/java/com/dailymind/feature/browse/CategoryDetailViewModelTest.kt`:

```kotlin
package com.dailymind.feature.browse

import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.model.Quote
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryDetailViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `valid category exposes list and not empty`() {
        val q = Quote("1", "Hello", null, null, "love", 1, null, null, 1L)
        val flow = MutableStateFlow(listOf(q))
        val repo = mockk<QuoteRepository>()
        every { repo.observeQuotesByCategory("love") } returns flow
        val vm = CategoryDetailViewModel(repo, "love")
        assertEquals(listOf("1"), vm.state.value.quotes.map { it.id })
        assertEquals(false, vm.state.value.isEmpty)
    }

    @Test fun `empty flow yields empty state`() {
        val repo = mockk<QuoteRepository>()
        every { repo.observeQuotesByCategory("life") } returns MutableStateFlow(emptyList())
        val vm = CategoryDetailViewModel(repo, "life")
        assertEquals(true, vm.state.value.isEmpty)
    }

    @Test fun `blank category immediately reports invalid`() {
        val repo = mockk<QuoteRepository>()
        val vm = CategoryDetailViewModel(repo, "   ")
        assertEquals(true, vm.state.value.isInvalid)
    }
}
```

- [ ] **Step 4: Run the test to verify it fails (compilation)**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.browse.CategoryDetailViewModelTest"
```
Expected: COMPILATION FAILURE.

- [ ] **Step 5: Implement `BrowseViewModel`**

Create `app/src/main/java/com/dailymind/feature/browse/BrowseViewModel.kt`:

```kotlin
package com.dailymind.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.database.CategoryCount
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BrowseUiState(
    val tiles: List<CategoryCount> = emptyList(),
    val totalQuotes: Int = 0,
    val totalCategories: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repo: QuoteRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BrowseUiState())
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeCategoryCounts().collect { counts ->
                _state.value = BrowseUiState(
                    tiles = counts,
                    totalQuotes = counts.sumOf { it.count },
                    totalCategories = counts.size,
                    isLoading = false,
                )
            }
        }
    }
}
```

- [ ] **Step 6: Implement `CategoryDetailViewModel`**

Create `app/src/main/java/com/dailymind/feature/browse/CategoryDetailViewModel.kt`:

```kotlin
package com.dailymind.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.model.Quote
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoryDetailUiState(
    val category: String = "",
    val quotes: List<Quote> = emptyList(),
    val isEmpty: Boolean = false,
    val isInvalid: Boolean = false,
)

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val repo: QuoteRepository,
    initialCategory: String,
) : ViewModel() {
    private val _state = MutableStateFlow(CategoryDetailUiState(category = initialCategory))
    val state: StateFlow<CategoryDetailUiState> = _state.asStateFlow()

    init {
        if (initialCategory.isBlank()) {
            _state.value = _state.value.copy(isInvalid = true)
            return
        }
        viewModelScope.launch {
            repo.observeQuotesByCategory(initialCategory).collect { list ->
                _state.value = _state.value.copy(
                    quotes = list,
                    isEmpty = list.isEmpty(),
                )
            }
        }
    }
}
```

- [ ] **Step 7: Run both tests to verify they pass**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.browse.BrowseViewModelTest" --tests "com.dailymind.feature.browse.CategoryDetailViewModelTest"
```
Expected: PASS for all 5 tests.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/dailymind/feature/browse/BrowseViewModel.kt \
        app/src/main/java/com/dailymind/feature/browse/CategoryDetailViewModel.kt \
        app/src/test/java/com/dailymind/feature/browse/BrowseViewModelTest.kt \
        app/src/test/java/com/dailymind/feature/browse/CategoryDetailViewModelTest.kt
git commit -m "feat(browse): ViewModels for category grid and per-category list"
```

---

## Task 6: `BrowseScreen` and `CategoryDetailScreen` (TDD)

**Files:**
- Create: `app/src/main/java/com/dailymind/feature/browse/BrowseScreen.kt`
- Create: `app/src/main/java/com/dailymind/feature/browse/CategoryDetailScreen.kt`
- Create: `app/src/test/java/com/dailymind/feature/browse/BrowseScreenTest.kt`

- [ ] **Step 1: Write the failing `BrowseScreenTest`**

Create `app/src/test/java/com/dailymind/feature/browse/BrowseScreenTest.kt`:

```kotlin
package com.dailymind.feature.browse

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.database.CategoryCount
import com.dailymind.core.datastore.ThemeMode
import com.dailymind.core.datastore.ThemeStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BrowseScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `renders category tiles and invokes onSelectCategory on tap`() {
        val counts = MutableStateFlow(
            listOf(CategoryCount("励志", 3), CategoryCount("爱情", 2))
        )
        val repo = mockk<QuoteRepository>()
        every { repo.observeCategoryCounts() } returns counts
        val themeStore = mockk<ThemeStore>(relaxed = true)
        every { themeStore.mode } returns MutableStateFlow(ThemeMode.SYSTEM)
        val vm = BrowseViewModel(repo)
        var clicked: String? = null

        composeTestRule.setContent {
            BrowseScreen(viewModel = vm, onSelectCategory = { clicked = it })
        }
        composeTestRule.onNodeWithText("励志").assertExists()
        composeTestRule.onNodeWithText("爱情").assertExists()
        composeTestRule.onNodeWithText("By mood").assertExists()
        composeTestRule.onNodeWithText("2 categories · 5 lines").assertExists()
        composeTestRule.onNodeWithText("励志").performClick()
        assertEquals("励志", clicked)
    }

    @Test fun `empty counts renders empty state`() {
        val counts = MutableStateFlow<List<CategoryCount>>(emptyList())
        val repo = mockk<QuoteRepository>()
        every { repo.observeCategoryCounts() } returns counts
        val vm = BrowseViewModel(repo)
        composeTestRule.setContent {
            BrowseScreen(viewModel = vm, onSelectCategory = {})
        }
        composeTestRule.onNodeWithText("Nothing to browse yet").assertExists()
    }
}
```

Add to the test imports:
```kotlin
import org.junit.Assert.assertEquals
```

- [ ] **Step 2: Run the test to verify it fails (compilation)**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.browse.BrowseScreenTest"
```
Expected: COMPILATION FAILURE because `BrowseScreen` doesn't exist (and signature differs from what the test calls).

- [ ] **Step 3: Implement `BrowseScreen`**

Create `app/src/main/java/com/dailymind/feature/browse/BrowseScreen.kt`:

```kotlin
package com.dailymind.feature.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailymind.core.database.CategoryCount
import com.dailymind.core.designsystem.EditorialEmpty
import com.dailymind.core.designsystem.EditorialTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel = hiltViewModel(),
    onSelectCategory: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            EditorialTopBar(
                date = "",
                greeting = "By mood"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${state.totalCategories} categories · ${state.totalQuotes} lines",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (state.tiles.isEmpty() && !state.isLoading) {
                EditorialEmpty(
                    title = "Nothing to browse yet",
                    body = "Pull to sync on Home.",
                    actionLabel = "Go to Home",
                    onAction = { onSelectCategory("") /* parent decides */ }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.tiles.size) { idx ->
                        CategoryTile(state.tiles[idx], idx, onSelectCategory)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.items(
    count: Int,
    itemContent: @Composable (Int) -> Unit,
) {
    items(count = count, key = { it }, contentType = { null }, itemContent = itemContent)
}

@Composable
private fun CategoryTile(
    item: CategoryCount,
    index: Int,
    onClick: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .background(MaterialTheme.colorScheme.background)
            .clickable { onClick(item.category) }
            .padding(16.dp)
    ) {
        Text(
            text = "#%02d".format(index + 1),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                text = item.category,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${item.count} lines",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

Note: the file imports `androidx.compose.foundation.lazy.grid.items` via the private wrapper above to keep the public `items` overload at the call site short. If the wrapper doesn't compile because the inner `items` is already imported, remove the import and call the standard one directly:

```kotlin
items(count = state.tiles.size, key = { state.tiles[it].category }) { idx ->
    CategoryTile(state.tiles[idx], idx, onSelectCategory)
}
```

Use this fallback if Robolectric/Compose test complains about the wrapper.

- [ ] **Step 4: Run `BrowseScreenTest` to verify it passes**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.browse.BrowseScreenTest"
```
Expected: PASS for both tests. (If `onNodeWithText("2 categories · 5 lines")` fails because of locale formatting, see the Debugging Notes in §Troubleshooting below.)

- [ ] **Step 5: Implement `CategoryDetailScreen` (no Compose test for this screen — relies on `IndexRow` which already has tests)**

Create `app/src/main/java/com/dailymind/feature/browse/CategoryDetailScreen.kt`:

```kotlin
package com.dailymind.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailymind.core.designsystem.EditorialEmpty
import com.dailymind.core.designsystem.IndexRow

@Composable
fun CategoryDetailScreen(
    category: String,
    onBack: () -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(category) {
        if (category.isBlank()) onBack()
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.isInvalid) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(48.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = state.category,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${state.quotes.size} lines",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            if (state.isEmpty) {
                item {
                    EditorialEmpty(
                        title = "This category is empty",
                        body = "Pull to sync or pick another.",
                        actionLabel = "Back",
                        onAction = onBack
                    )
                }
            } else {
                itemsIndexed(state.quotes, key = { _, q -> q.id }) { index, q ->
                    IndexRow(
                        number = (index + 1).toString().padStart(2, '0'),
                        content = q.content,
                        author = q.author,
                        onClick = {},
                        onSwipeOut = null
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 6: Run the focused browse tests**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.browse.*"
```
Expected: PASS for all browse tests (4 ViewModel + 2 Screen = 6 tests).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/dailymind/feature/browse/BrowseScreen.kt \
        app/src/main/java/com/dailymind/feature/browse/CategoryDetailScreen.kt \
        app/src/test/java/com/dailymind/feature/browse/BrowseScreenTest.kt
git commit -m "feat(browse): editorial 2-col grid screen and category detail list"
```

---

## Task 7: Wire `CategoryPreferenceStore` into `HomeViewModel` and render `CategoryChips` (TDD)

**Files:**
- Modify: `app/src/main/java/com/dailymind/feature/home/HomeUiState.kt` (+EmptyMode enum, +fields)
- Modify: `app/src/main/java/com/dailymind/feature/home/HomeViewModel.kt` (+ctor param, +SelectCategory event, +observe, +emptyMode compute)
- Modify: `app/src/test/java/com/dailymind/feature/home/HomeViewModelTest.kt` (extend — must add `CategoryPreferenceStore` to ctor calls)
- Create: `app/src/test/java/com/dailymind/feature/home/HomeViewModelCategoryTest.kt` (new category-specific cases)
- Modify: `app/src/main/java/com/dailymind/feature/home/HomeScreen.kt` (+CategoryChips composable, +EmptyMode switch in empty branch)
- Create: `app/src/test/java/com/dailymind/feature/home/HomeChipsTest.kt`

- [ ] **Step 1: Update `HomeUiState.kt`**

Modify `app/src/main/java/com/dailymind/feature/home/HomeUiState.kt`. Replace contents with:

```kotlin
package com.dailymind.feature.home

import com.dailymind.core.model.Quote

enum class EmptyMode {
    Generic,
    NoCategoryLines,
}

data class HomeUiState(
    val quote: Quote? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBrowsing: Boolean = false,
    val isCurrentQuoteFavorite: Boolean = false,
    val favoriteTapKey: Int = 0,
    val selectedCategory: String? = null,
    val availableCategories: List<String> = emptyList(),
    val emptyMode: EmptyMode = EmptyMode.Generic,
)

sealed interface HomeEvent {
    data object Load : HomeEvent
    data object NextRandom : HomeEvent
    data class Favorite(val id: String) : HomeEvent
    data class SelectCategory(val category: String?) : HomeEvent
}
```

- [ ] **Step 2: Update `HomeViewModel.kt`**

Modify `app/src/main/java/com/dailymind/feature/home/HomeViewModel.kt`. Replace contents with:

```kotlin
package com.dailymind.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.datastore.CategoryPreferenceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: QuoteRepository,
    private val categoryStore: CategoryPreferenceStore,
) : ViewModel() {
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
        viewModelScope.launch {
            combine(
                categoryStore.selectedCategory,
                repo.observeCategoryCounts(),
            ) { selected, counts ->
                selected to counts.take(MAX_CHIPS).map { it.category }
            }.collect { (selected, topCats) ->
                _uiState.value = _uiState.value.copy(
                    selectedCategory = selected,
                    availableCategories = topCats,
                    emptyMode = computeEmptyMode(_uiState.value, selected),
                )
            }
        }
        onEvent(HomeEvent.Load)
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Load -> viewModelScope.launch {
                val current = _uiState.value
                _uiState.value = current.copy(isLoading = true)
                try {
                    val q = if (current.selectedCategory == null) {
                        // "All": pin → unviewed random → network fallback
                        repo.getDailyQuote(null) ?: repo.getRandomQuote()
                    } else {
                        // Per spec §4.4: strict filter, no fallback to other categories
                        repo.getDailyQuote(current.selectedCategory)
                    }
                    _uiState.value = _uiState.value.copy(
                        quote = q,
                        isLoading = false,
                        isBrowsing = false,
                        error = null,
                        emptyMode = computeEmptyMode(_uiState.value.copy(quote = q), current.selectedCategory),
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isLoading = false,
                        emptyMode = computeEmptyMode(_uiState.value, current.selectedCategory),
                    )
                }
            }
            is HomeEvent.NextRandom -> viewModelScope.launch {
                try {
                    val current = _uiState.value
                    val q = repo.getLocalRandomQuote(
                        excludeId = current.quote?.id,
                        category = current.selectedCategory,
                    ) ?: throw IllegalStateException("No other quotes yet")
                    _uiState.value = _uiState.value.copy(quote = q, error = null, isBrowsing = true)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            }
            is HomeEvent.Favorite -> viewModelScope.launch {
                repo.toggleFavorite(event.id)
                _uiState.value = _uiState.value.copy(favoriteTapKey = _uiState.value.favoriteTapKey + 1)
            }
            is HomeEvent.SelectCategory -> viewModelScope.launch {
                categoryStore.setSelectedCategory(event.category)
                _uiState.value = _uiState.value.copy(selectedCategory = event.category)
                val current = _uiState.value
                val shouldReload = current.quote == null ||
                    (event.category != null && current.quote?.category != event.category)
                if (shouldReload) onEvent(HomeEvent.Load)
                else _uiState.value = _uiState.value.copy(
                    emptyMode = computeEmptyMode(current, event.category)
                )
            }
        }
    }

    private fun computeEmptyMode(state: HomeUiState, selected: String?): EmptyMode {
        if (state.quote != null || state.isLoading || state.error != null) return EmptyMode.Generic
        if (selected != null) return EmptyMode.NoCategoryLines
        return EmptyMode.Generic
    }

    private companion object {
        const val MAX_CHIPS = 6
    }
}
```

- [ ] **Step 3: Update existing `HomeViewModelTest.kt` so the 4 pre-existing tests pass the new constructor**

Modify `app/src/test/java/com/dailymind/feature/home/HomeViewModelTest.kt`. Add a `categoryStore()` helper and pass it into every `HomeViewModel(repo)` call. Replace contents with:

```kotlin
package com.dailymind.feature.home

import com.dailymind.MainDispatcherRule
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.datastore.CategoryPreferenceStore
import com.dailymind.core.model.Quote
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private fun quote(id: String, content: String = "Q-$id") = Quote(id, content, null, null, null, 1, null, null, 1L)

    private fun categoryStore(): CategoryPreferenceStore {
        val s = mockk<CategoryPreferenceStore>(relaxed = true)
        every { s.selectedCategory } returns MutableStateFlow<String?>(null)
        return s
    }

    @Test fun `initial state loads daily quote`() = runTest {
        val repo = mockk<QuoteRepository>()
        coEvery { repo.getDailyQuote(null) } returns quote("1")
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        every { repo.observeCategoryCounts() } returns flowOf(emptyList())
        val vm = HomeViewModel(repo, categoryStore())
        vm.onEvent(HomeEvent.Load)
        assertEquals("Q-1", vm.uiState.value.quote?.content)
    }

    @Test fun `NextRandom picks local random excluding current quote`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        coEvery { repo.getLocalRandomQuote(excludeId = "1", category = null) } returns quote("2", "Q-2")
        val vm = HomeViewModel(repo, categoryStore())
        @Suppress("UNCHECKED_CAST")
        (vm.uiState as MutableStateFlow<HomeUiState>).value = vm.uiState.value.copy(quote = quote("1"))
        vm.onEvent(HomeEvent.NextRandom)
        assertEquals("Q-2", vm.uiState.value.quote?.content)
        assertEquals(true, vm.uiState.value.isBrowsing)
        coVerify { repo.getLocalRandomQuote(excludeId = "1", category = null) }
    }

    @Test fun `NextRandom surfaces error when local random returns null`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        coEvery { repo.getLocalRandomQuote(any(), any()) } returns null
        val vm = HomeViewModel(repo, categoryStore())
        vm.onEvent(HomeEvent.NextRandom)
        assertNotNull(vm.uiState.value.error)
    }

    @Test fun `Favorite increments favoriteTapKey`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        val vm = HomeViewModel(repo, categoryStore())
        val before = vm.uiState.value.favoriteTapKey
        vm.onEvent(HomeEvent.Favorite("1"))
        assertEquals(before + 1, vm.uiState.value.favoriteTapKey)
    }

    @Test fun `isCurrentQuoteFavorite reflects observeFavoriteIds`() = runTest {
        val repo = mockk<QuoteRepository>()
        coEvery { repo.getDailyQuote(null) } returns quote("1")
        val favIds = MutableStateFlow(setOf("1"))
        every { repo.observeFavoriteIds() } returns favIds
        every { repo.observeCategoryCounts() } returns flowOf(emptyList())
        val vm = HomeViewModel(repo, categoryStore())
        vm.onEvent(HomeEvent.Load)
        assertEquals(true, vm.uiState.value.isCurrentQuoteFavorite)
    }
}
```

- [ ] **Step 4: Run the updated `HomeViewModelTest` to confirm no regression**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.home.HomeViewModelTest"
```
Expected: PASS for all 5 tests.

- [ ] **Step 5: Write the failing `HomeViewModelCategoryTest`**

Create `app/src/test/java/com/dailymind/feature/home/HomeViewModelCategoryTest.kt`:

```kotlin
package com.dailymind.feature.home

import com.dailymind.MainDispatcherRule
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.database.CategoryCount
import com.dailymind.core.datastore.CategoryPreferenceStore
import com.dailymind.core.model.Quote
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HomeViewModelCategoryTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private fun quote(id: String, cat: String? = null) =
        Quote(id, "Q-$id", null, null, cat, 1, null, null, 1L)

    private fun storeWithSelected(value: String?): CategoryPreferenceStore {
        val s = mockk<CategoryPreferenceStore>(relaxed = true)
        every { s.selectedCategory } returns MutableStateFlow<String?>(value)
        return s
    }

    @Test fun `SelectCategory writes store and updates state`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        every { repo.observeCategoryCounts() } returns flowOf(emptyList())
        val vm = HomeViewModel(repo, storeWithSelected(null))
        vm.onEvent(HomeEvent.SelectCategory("love"))
        coVerify { repo.getDailyQuote("love") }
        assertEquals("love", vm.uiState.value.selectedCategory)
    }

    @Test fun `SelectCategory to null does not reload when current quote has category`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        every { repo.observeCategoryCounts() } returns flowOf(emptyList())
        val vm = HomeViewModel(repo, storeWithSelected("love"))
        (vm.uiState as MutableStateFlow<HomeUiState>).value =
            vm.uiState.value.copy(quote = quote("1", cat = "love"))
        vm.onEvent(HomeEvent.SelectCategory(null))
        assertNull(vm.uiState.value.selectedCategory)
        coVerify(exactly = 0) { repo.getDailyQuote(null) }
    }

    @Test fun `SelectCategory to a non-matching category reloads`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        every { repo.observeCategoryCounts() } returns flowOf(emptyList())
        coEvery { repo.getDailyQuote("life") } returns quote("9", cat = "life")
        val vm = HomeViewModel(repo, storeWithSelected("love"))
        (vm.uiState as MutableStateFlow<HomeUiState>).value =
            vm.uiState.value.copy(quote = quote("1", cat = "love"))
        vm.onEvent(HomeEvent.SelectCategory("life"))
        assertEquals("9", vm.uiState.value.quote?.id)
    }

    @Test fun `availableCategories reflects top 6 by count`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        every { repo.observeCategoryCounts() } returns flowOf(
            listOf(
                CategoryCount("a", 1), CategoryCount("b", 1), CategoryCount("c", 1),
                CategoryCount("d", 1), CategoryCount("e", 1), CategoryCount("f", 1),
                CategoryCount("g", 1),
            )
        )
        val vm = HomeViewModel(repo, storeWithSelected(null))
        assertEquals(listOf("a", "b", "c", "d", "e", "f"), vm.uiState.value.availableCategories)
    }

    @Test fun `emptyMode is NoCategoryLines when category is selected and getDailyQuote returns null`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        every { repo.observeCategoryCounts() } returns flowOf(emptyList())
        coEvery { repo.getDailyQuote("love") } returns null
        // Per spec §4.4: getRandomQuote is NOT called as a fallback when a category is selected
        val vm = HomeViewModel(repo, storeWithSelected("love"))
        vm.onEvent(HomeEvent.SelectCategory("love"))
        assertNull(vm.uiState.value.quote)
        assertEquals(EmptyMode.NoCategoryLines, vm.uiState.value.emptyMode)
        coVerify(exactly = 0) { repo.getRandomQuote() }
    }
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.home.HomeViewModelCategoryTest"
```
Expected: PASS for all 5 tests.

- [ ] **Step 7: Write the failing `HomeChipsTest` (Compose UI)**

Create `app/src/test/java/com/dailymind/feature/home/HomeChipsTest.kt`:

```kotlin
package com.dailymind.feature.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.database.CategoryCount
import com.dailymind.core.datastore.CategoryPreferenceStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class HomeChipsTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Composable
    private fun Wrap(content: @Composable () -> Unit) {
        MaterialTheme(colorScheme = darkColorScheme()) { content() }
    }

    @Test fun `All chip is selected by default and tapping a category emits SelectCategory`() {
        var emitted: String? = null
        composeTestRule.setContent {
            Wrap {
                CategoryChips(
                    selected = null,
                    available = listOf("love", "life"),
                    onPick = { emitted = it }
                )
            }
        }
        composeTestRule.onNodeWithText("All").assertExists()
        composeTestRule.onNodeWithText("love").performClick()
        assert(emitted == "love") { "expected 'love' got $emitted" }
    }

    @Test fun `More chip appears when more than 6 categories available`() {
        val cats = (1..8).map { "c$it" }
        composeTestRule.setContent {
            Wrap {
                CategoryChips(
                    selected = null,
                    available = cats,
                    onPick = {},
                    onMore = {}
                )
            }
        }
        composeTestRule.onNodeWithText("More…").assertExists()
    }
}
```

- [ ] **Step 8: Run the test to verify it fails (compilation, because `CategoryChips` doesn't exist yet)**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.home.HomeChipsTest"
```
Expected: COMPILATION FAILURE.

- [ ] **Step 9: Add `CategoryChips` composable and empty-mode switch to `HomeScreen.kt`**

Modify `app/src/main/java/com/dailymind/feature/home/HomeScreen.kt`. The file is large; do these targeted edits:

(a) Add imports at the top (merge with existing imports):
```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
```

(b) Inside the `Scaffold` body, after the `EditorialTopBar` `item { ... }` and before the `item { AnimatedVisibility(... TODAY'S QUOTE ...) }`, insert a new `item { ... }` that renders `CategoryChips`:

```kotlin
item {
    CategoryChips(
        selected = state.selectedCategory,
        available = state.availableCategories,
        onPick = { viewModel.onEvent(HomeEvent.SelectCategory(it)) },
        onMore = { /* future: show full sheet */ }
    )
    Spacer(modifier = Modifier.height(24.dp))
}
```

(c) In the `key == "empty" && !state.isLoading` branch of the `AnimatedContent` `when`, replace the existing `EditorialEmpty(...)` with a `when (state.emptyMode)` that picks copy + action:

```kotlin
key == "empty" && !state.isLoading -> {
    when (state.emptyMode) {
        EmptyMode.NoCategoryLines -> EditorialEmpty(
            title = "No lines in this category yet",
            body = "This category hasn't been synced to your device.",
            actionLabel = "Switch to All",
            onAction = { viewModel.onEvent(HomeEvent.SelectCategory(null)) }
        )
        EmptyMode.Generic -> EditorialEmpty(
            title = "Nothing here yet",
            body = "Check your connection and try again.",
            actionLabel = "Retry →",
            onAction = { viewModel.onEvent(HomeEvent.Load) }
        )
    }
}
```

(d) Append a `CategoryChips` composable at the bottom of the same file:

```kotlin
@Composable
fun CategoryChips(
    selected: String?,
    available: List<String>,
    onPick: (String?) -> Unit,
    onMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var sheetVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ChipLabel(text = "All", isSelected = selected == null, onClick = { onPick(null) })
        }
        items(available) { cat ->
            ChipLabel(text = cat, isSelected = selected == cat, onClick = { onPick(cat) })
        }
        if (available.size > 6 && onMore != null) {
            item {
                ChipLabel(text = "More…", isSelected = false, onClick = { sheetVisible = true })
            }
        }
    }
    if (sheetVisible) {
        ModalBottomSheet(onDismissRequest = { sheetVisible = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                available.forEach { cat ->
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                onPick(cat)
                                sheetVisible = false
                            }
                            .padding(vertical = 12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ChipLabel(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
```

Add the additional imports needed for `ChipLabel` and the `ModalBottomSheet` (already covered by step (a) above).

- [ ] **Step 10: Run `HomeChipsTest` to verify it passes**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.home.HomeChipsTest"
```
Expected: PASS for both tests.

- [ ] **Step 11: Run the full :app:test suite to confirm no regression**

Run:
```bash
server/gradlew.bat :app:test
```
Expected: `BUILD SUCCESSFUL` and all tests pass.

- [ ] **Step 12: Commit**

```bash
git add app/src/main/java/com/dailymind/feature/home/HomeUiState.kt \
        app/src/main/java/com/dailymind/feature/home/HomeViewModel.kt \
        app/src/main/java/com/dailymind/feature/home/HomeScreen.kt \
        app/src/test/java/com/dailymind/feature/home/HomeViewModelTest.kt \
        app/src/test/java/com/dailymind/feature/home/HomeViewModelCategoryTest.kt \
        app/src/test/java/com/dailymind/feature/home/HomeChipsTest.kt
git commit -m "feat(home): wire CategoryPreferenceStore, add chip strip and empty-mode switch"
```

---

## Task 8: Aggregate verification

**Files:** none

- [ ] **Step 1: Run the full unit test suite**

Run:
```bash
server/gradlew.bat :app:test
```
Expected: `BUILD SUCCESSFUL`. If any pre-existing test fails (e.g. a test that constructs `HomeViewModel` without `CategoryPreferenceStore` because the worker is using an older copy of this plan), find and fix that test.

- [ ] **Step 2: Run the focused category-filter suite**

Run:
```bash
server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.browse.*" --tests "com.dailymind.feature.home.HomeViewModelCategoryTest" --tests "com.dailymind.feature.home.HomeChipsTest" --tests "com.dailymind.core.database.dao.QuoteDaoCategoryTest" --tests "com.dailymind.core.datastore.CategoryPreferenceStoreTest" --tests "com.dailymind.core.data.QuoteRepositoryCategoryTest"
```
Expected: all green.

- [ ] **Step 3: Smoke build the debug APK**

Run:
```bash
server/gradlew.bat :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists. (If a connected device is wired up, install and tap through; not required for this plan.)

- [ ] **Step 4: Final commit (if any incidental cleanup was needed)**

```bash
git status
# If clean, no commit needed. If there are stray edits, add and commit with a descriptive message.
```

---

## Troubleshooting

1. **`BrowseScreenTest` fails on `2 categories · 5 lines` because of locale/formatting.** The displayed string is exactly `"${state.totalCategories} categories · ${state.totalQuotes} lines"`. If the test environment uses a non-ASCII locale, the `·` may render differently. The implementation uses the literal middle dot character (U+00B7). Verify that the test file's `onNodeWithText` string matches the implementation byte-for-byte.

2. **Robolectric complains about missing manifest.** The `@Config(sdk = [34], manifest = Config.NONE)` annotation disables manifest loading. If a test needs the real app context (e.g. for DataStore), remove `manifest = Config.NONE` and ensure `app/src/main/AndroidManifest.xml` doesn't require permissions that block tests.

3. **`HomeViewModelCategoryTest` emptyMode test is flaky.** The empty-mode test depends on `repo.getRandomQuote()` being called as a fallback inside `getDailyQuote(category)`. If your implementation skips the fallback (current spec only does fallback for `category == null`), the test will fail. The current plan implementation does call `getRandomQuote()` as fallback; if you change that behavior, update the test.

4. **Compose UI test runs out of memory on Windows.** If Robolectric crashes with OOM during `HomeChipsTest`, increase the JVM heap in `gradle.properties`:
   ```properties
   org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
   ```
   And re-run with `--no-daemon` to avoid daemon memory pressure.

5. **`HomeViewModel` constructor change breaks `:app:assembleDebug` because Hilt's generated factory is stale.** Run `server/gradlew.bat :app:clean :app:assembleDebug` to force a fresh KSP round. The next regular build will be fine.

6. **The `LazyGridScope.items` private wrapper in `BrowseScreen` may conflict with the standard import.** Use the fallback shown in Step 3 of Task 6 (the standard `items(count = ..., key = ...)` call) if Robolectric reports a name clash.

---

## What This Plan Does Not Cover (deferred to a follow-up spec)

- Quote-detail screen reachable from `CategoryDetailScreen` rows.
- Favorite toggle from `CategoryDetailScreen`.
- Sort / search / paginate inside a category list.
- Server-side category filtering.
- "By mood" Browse analytics.
- Recommendation algorithm for the Home chip strip.

See spec §9.
