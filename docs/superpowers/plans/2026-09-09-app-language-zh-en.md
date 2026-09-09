# App Language (ZH / EN) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** App 界面中英雙語，首啟跟隨系統語言（英文系統→英文，其餘→中文），Me 頁可切換、即時生效、DataStore 持久化。

**Architecture:** `LanguageStore`（照抄 `ThemeStore` 的 DataStore 模式，`defaultLanguage()` 定預設規則）+ `AppStrings` data class（中英兩套實例，data class 保證欄位對齊）+ `LocalAppStrings` CompositionLocal（`MainActivity` 頂層提供）；各 Screen 把字面量換成 `appStrings().x`；ViewModel 保持無語言。

**Tech Stack:** Kotlin, Jetpack Compose (CompositionLocal), Hilt, DataStore Preferences, JUnit4 + MockK + kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-09-09-app-language-zh-en-design.md`

**JAVA_HOME note:** All Gradle commands need `$env:JAVA_HOME = "C:/Users/Deng/.jdks/temurin-21"` first (see AGENTS.md toolchain).

---

### Task 1: LanguageStore + KEY（TDD）

**Files:**
- Modify: `app/src/main/java/com/quotegarden/core/datastore/PreferencesDataStore.kt` (add 1 key line)
- Create: `app/src/main/java/com/quotegarden/core/datastore/LanguageStore.kt`
- Test: `app/src/test/java/com/quotegarden/core/datastore/LanguageStoreTest.kt` (create)

- [ ] **Step 1: 加 LANGUAGE key**

In `PreferencesDataStore.kt`, replace:

```kotlin
    val CATEGORY = stringPreferencesKey("selected_category")
```

with:

```kotlin
    val CATEGORY = stringPreferencesKey("selected_category")
    val LANGUAGE = stringPreferencesKey("language")
```

- [ ] **Step 2: 寫 failing test（完整文件，直接創建）**

Create `app/src/test/java/com/quotegarden/core/datastore/LanguageStoreTest.kt`:

```kotlin
package com.quotegarden.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageStoreTest {
    private fun makeStore(): Pair<DataStore<Preferences>, MutablePreferences> {
        val backing = mutablePreferencesOf()
        val flow = MutableStateFlow<Preferences>(backing)
        val store = mockk<DataStore<Preferences>>(relaxed = false)
        every { store.data } returns flow
        coEvery { store.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val current = flow.value
            val mutable = current.toMutablePreferences()
            val result = transform(mutable)
            flow.value = result
            result
        }
        return store to backing
    }

    @Test fun `defaultLanguage maps en to EN`() {
        val (store, _) = makeStore()
        assertEquals(AppLanguage.EN, LanguageStore(store).defaultLanguage(Locale("en")))
        assertEquals(AppLanguage.EN, LanguageStore(store).defaultLanguage(Locale("en", "US")))
    }

    @Test fun `defaultLanguage maps non-en to ZH`() {
        val (store, _) = makeStore()
        assertEquals(AppLanguage.ZH, LanguageStore(store).defaultLanguage(Locale("zh")))
        assertEquals(AppLanguage.ZH, LanguageStore(store).defaultLanguage(Locale("fr")))
    }

    @Test fun `language defaults to defaultLanguage when key absent`() = runTest {
        val (store, _) = makeStore()
        val s = LanguageStore(store)
        assertEquals(s.defaultLanguage(), s.language.first())
    }

    @Test fun `setLanguage roundtrips ZH then EN`() = runTest {
        val (store, _) = makeStore()
        val s = LanguageStore(store)
        s.setLanguage(AppLanguage.ZH)
        assertEquals(AppLanguage.ZH, s.language.first())
        s.setLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, s.language.first())
    }

    @Test fun `invalid stored value falls back to defaultLanguage`() = runTest {
        val (store, backing) = makeStore()
        backing.set(PreferencesKeys.LANGUAGE, "XX")
        val s = LanguageStore(store)
        assertEquals(s.defaultLanguage(), s.language.first())
    }
}
```

(The `makeStore` helper is copied verbatim from `CategoryPreferenceStoreTest.kt`.)

- [ ] **Step 3: 運行確認失敗**

Run: `server/gradlew.bat :app:testDebugUnitTest --tests "com.quotegarden.core.datastore.LanguageStoreTest"`
Expected: FAIL — `LanguageStore` / `AppLanguage` unresolved reference (class does not exist yet)

- [ ] **Step 4: 最小實現（創建 LanguageStore.kt）**

Create `app/src/main/java/com/quotegarden/core/datastore/LanguageStore.kt`:

```kotlin
package com.quotegarden.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppLanguage {
    ZH, EN;
}

@Singleton
class LanguageStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    fun defaultLanguage(locale: Locale = Locale.getDefault()): AppLanguage =
        if (locale.language == "en") AppLanguage.EN else AppLanguage.ZH

    val language: Flow<AppLanguage> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
            ?: defaultLanguage()
    }

    suspend fun setLanguage(lang: AppLanguage) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.LANGUAGE] = lang.name }
    }
}
```

- [ ] **Step 5: 運行確認通過**

Run: `server/gradlew.bat :app:testDebugUnitTest --tests "com.quotegarden.core.datastore.LanguageStoreTest"`
Expected: BUILD SUCCESSFUL, 5 tests pass

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/quotegarden/core/datastore/PreferencesDataStore.kt app/src/main/java/com/quotegarden/core/datastore/LanguageStore.kt app/src/test/java/com/quotegarden/core/datastore/LanguageStoreTest.kt
git commit -m "feat(i18n): LanguageStore with system-locale default"
```

---

### Task 2: AppStrings 中英實例 + JVM 測試（TDD）

**Files:**
- Create: `app/src/main/java/com/quotegarden/core/designsystem/AppStrings.kt`
- Test: `app/src/test/java/com/quotegarden/core/designsystem/AppStringsTest.kt` (create)

- [ ] **Step 1: 寫 failing test（完整文件，直接創建）**

Create `app/src/test/java/com/quotegarden/core/designsystem/AppStringsTest.kt`:

```kotlin
package com.quotegarden.core.designsystem

import com.quotegarden.core.datastore.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStringsTest {
    @Test fun `zh strings match spec`() {
        assertEquals("早上好", ZhStrings.greetingMorning)
        assertEquals("今日推荐", ZhStrings.todaysQuote)
        assertEquals("下一条 →", ZhStrings.next)
        assertEquals("今日", ZhStrings.tabToday)
        assertEquals("语言", ZhStrings.languageSetting)
    }

    @Test fun `en strings match spec`() {
        assertEquals("Good Morning", EnStrings.greetingMorning)
        assertEquals("TODAY'S QUOTE", EnStrings.todaysQuote)
        assertEquals("Next →", EnStrings.next)
        assertEquals("Today", EnStrings.tabToday)
        assertEquals("Language", EnStrings.languageSetting)
    }

    @Test fun `formatter fields produce non-blank output`() {
        assertTrue(ZhStrings.categoriesLines(6, 20).isNotBlank())
        assertTrue(EnStrings.categoriesLines(6, 20).isNotBlank())
        assertTrue(ZhStrings.linesCount(3).isNotBlank())
        assertTrue(EnStrings.linesCount(3).isNotBlank())
        assertTrue(ZhStrings.savedCount(2).isNotBlank())
        assertTrue(EnStrings.savedCount(2).isNotBlank())
        assertTrue(ZhStrings.aboutBody("1.0").contains("1.0"))
        assertTrue(EnStrings.aboutBody("1.0").contains("1.0"))
    }

    @Test fun `zh and en differ on key labels (no copy-paste)`() {
        assertNotEquals(ZhStrings.greetingMorning, EnStrings.greetingMorning)
        assertNotEquals(ZhStrings.save, EnStrings.save)
        assertNotEquals(ZhStrings.next, EnStrings.next)
        assertNotEquals(ZhStrings.tabBrowse, EnStrings.tabBrowse)
        assertNotEquals(ZhStrings.languageSetting, EnStrings.languageSetting)
    }

    @Test fun `stringsFor resolves by language`() {
        assertEquals(ZhStrings, stringsFor(AppLanguage.ZH))
        assertEquals(EnStrings, stringsFor(AppLanguage.EN))
    }
}
```

Note: `stringsFor`/`AppLanguage` import — `AppLanguage` lives in `com.quotegarden.core.datastore`; the import is already included in the test file block above.

- [ ] **Step 2: 運行確認失敗**

Run: `server/gradlew.bat :app:testDebugUnitTest --tests "com.quotegarden.core.designsystem.AppStringsTest"`
Expected: FAIL — `ZhStrings` / `AppStrings` unresolved reference

- [ ] **Step 3: 最小實現（創建 AppStrings.kt，完整內容）**

Create `app/src/main/java/com/quotegarden/core/designsystem/AppStrings.kt` with exactly this content (data class + CompositionLocal + accessor + ZH/EN instances per spec §3/§7):

```kotlin
package com.quotegarden.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import com.quotegarden.core.datastore.AppLanguage

data class AppStrings(
    val retry: String,
    val close: String,
    val errorTitle: String,
    val tabToday: String,
    val tabBrowse: String,
    val tabMe: String,
    val greetingMorning: String,
    val greetingAfternoon: String,
    val greetingEvening: String,
    val todaysQuote: String,
    val loading: String,
    val save: String,
    val saved: String,
    val next: String,
    val chipAll: String,
    val chipMore: String,
    val emptyGenericTitle: String,
    val emptyGenericBody: String,
    val emptyNoCategoryTitle: String,
    val emptyNoCategoryBody: String,
    val switchToAll: String,
    val footnoteOffline: String,
    val noOtherQuotes: String,
    val browseTitle: String,
    val categoriesLines: (Int, Int) -> String,
    val linesCount: (Int) -> String,
    val goHome: String,
    val browseEmptyTitle: String,
    val browseEmptyBody: String,
    val backArrow: String,
    val back: String,
    val detailEmptyTitle: String,
    val detailEmptyBody: String,
    val meTitle: String,
    val savedCount: (Int) -> String,
    val savedHeading: String,
    val settingsHeading: String,
    val languageSetting: String,
    val themeSetting: String,
    val contactUs: String,
    val about: String,
    val noSavedTitle: String,
    val noSavedBody: String,
    val explore: String,
    val themeDialogTitle: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val aboutBody: (String) -> String,
    val contactTitle: String,
    val contactBody: String,
    val contactHint: String,
    val contactQrDesc: String,
)

val ZhStrings = AppStrings(
    retry = "重试",
    close = "关闭",
    errorTitle = "这里静悄悄",
    tabToday = "今日",
    tabBrowse = "逛逛",
    tabMe = "我的",
    greetingMorning = "早上好",
    greetingAfternoon = "下午好",
    greetingEvening = "晚上好",
    todaysQuote = "今日推荐",
    loading = "加载中…",
    save = "+ 保存",
    saved = "已保存 ✓",
    next = "下一条 →",
    chipAll = "全部",
    chipMore = "更多…",
    emptyGenericTitle = "还没有内容",
    emptyGenericBody = "检查网络后重试。",
    emptyNoCategoryTitle = "该分类暂无内容",
    emptyNoCategoryBody = "该分类尚未同步到本机。",
    switchToAll = "切换到全部",
    footnoteOffline = "离线优先 · 本地缓存，后台静默同步",
    noOtherQuotes = "暂无其他句子",
    browseTitle = "按心情",
    categoriesLines = { c, q -> "共 $c 个分类 · $q 条" },
    linesCount = { n -> "$n 条" },
    goHome = "去首页",
    browseEmptyTitle = "暂无分类",
    browseEmptyBody = "去首页同步试试。",
    backArrow = "<- 返回",
    back = "返回",
    detailEmptyTitle = "该分类是空的",
    detailEmptyBody = "同步后重试，或换个分类。",
    meTitle = "我的",
    savedCount = { n -> "已存 $n 条" },
    savedHeading = "已保存",
    settingsHeading = "设置",
    languageSetting = "语言",
    themeSetting = "主题",
    contactUs = "联系我们",
    about = "关于",
    noSavedTitle = "还没有收藏",
    noSavedBody = "留下打动你的句子。",
    explore = "去逛逛 →",
    themeDialogTitle = "主题",
    themeSystem = "跟随系统",
    themeLight = "浅色",
    themeDark = "深色",
    aboutBody = { v -> "离线优先的每日句子。版本 $v。" },
    contactTitle = "联系我们",
    contactBody = "用微信扫描二维码联系 Quote Garden 团队。",
    contactHint = "长按图片保存。",
    contactQrDesc = "微信二维码",
)

val EnStrings = AppStrings(
    retry = "Retry",
    close = "Close",
    errorTitle = "Something went quiet",
    tabToday = "Today",
    tabBrowse = "Browse",
    tabMe = "Me",
    greetingMorning = "Good Morning",
    greetingAfternoon = "Good Afternoon",
    greetingEvening = "Good Evening",
    todaysQuote = "TODAY'S QUOTE",
    loading = "Loading...",
    save = "+ Save",
    saved = "Saved ✓",
    next = "Next →",
    chipAll = "All",
    chipMore = "More…",
    emptyGenericTitle = "Nothing here yet",
    emptyGenericBody = "Check your connection and try again.",
    emptyNoCategoryTitle = "No lines in this category yet",
    emptyNoCategoryBody = "This category hasn't been synced to your device.",
    switchToAll = "Switch to All",
    footnoteOffline = "Offline-first - cached first, syncs quietly",
    noOtherQuotes = "No other quotes yet",
    browseTitle = "By mood",
    categoriesLines = { c, q -> "$c categories - $q lines" },
    linesCount = { n -> "$n lines" },
    goHome = "Go to Home",
    browseEmptyTitle = "Nothing to browse yet",
    browseEmptyBody = "Pull to sync on Home.",
    backArrow = "<- Back",
    back = "Back",
    detailEmptyTitle = "This category is empty",
    detailEmptyBody = "Pull to sync or pick another.",
    meTitle = "Me",
    savedCount = { n -> "$n saved" },
    savedHeading = "SAVED",
    settingsHeading = "SETTINGS",
    languageSetting = "Language",
    themeSetting = "Theme",
    contactUs = "Contact us",
    about = "About",
    noSavedTitle = "No saved quotes yet",
    noSavedBody = "Keep the lines that stay with you.",
    explore = "Explore →",
    themeDialogTitle = "Theme",
    themeSystem = "System",
    themeLight = "Light",
    themeDark = "Dark",
    aboutBody = { v -> "Offline-first daily quotes. Version $v." },
    contactTitle = "Contact us",
    contactBody = "Scan the QR code with WeChat to reach the Quote Garden team.",
    contactHint = "Long-press the image to save it.",
    contactQrDesc = "WeChat QR code",
)

fun stringsFor(lang: AppLanguage): AppStrings = when (lang) {
    AppLanguage.ZH -> ZhStrings
    AppLanguage.EN -> EnStrings
}

val LocalAppStrings = compositionLocalOf { EnStrings }

@Composable
@ReadOnlyComposable
fun appStrings(): AppStrings = LocalAppStrings.current
```

- [ ] **Step 4: 運行確認通過**

Run: `server/gradlew.bat :app:testDebugUnitTest --tests "com.quotegarden.core.designsystem.AppStringsTest"`
Expected: BUILD SUCCESSFUL, 5 tests pass

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/quotegarden/core/designsystem/AppStrings.kt app/src/test/java/com/quotegarden/core/designsystem/AppStringsTest.kt
git commit -m "feat(i18n): AppStrings ZH-EN instances with CompositionLocal"
```

---

### Task 3: 頂層接線（MainActivity + 底部 Tab）

**Files:**
- Modify: `app/src/main/java/com/quotegarden/MainActivity.kt`
- Modify: `app/src/main/java/com/quotegarden/core/navigation/AppNavDisplay.kt`

- [ ] **Step 1: MainActivity 注入 LanguageStore 並提供 strings**

Replace the whole `MainActivity.kt` content with:

```kotlin
package com.quotegarden

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotegarden.core.datastore.LanguageStore
import com.quotegarden.core.datastore.ThemeMode
import com.quotegarden.core.datastore.ThemeStore
import com.quotegarden.core.designsystem.LocalAppStrings
import com.quotegarden.core.designsystem.stringsFor
import com.quotegarden.core.designsystem.theme.QuoteGardenTheme
import com.quotegarden.core.navigation.AppNavDisplay
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var themeStore: ThemeStore
    @Inject lateinit var languageStore: LanguageStore

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val mode by themeStore.mode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val lang by languageStore.language.collectAsStateWithLifecycle(
                initialValue = languageStore.defaultLanguage()
            )
            CompositionLocalProvider(LocalAppStrings provides stringsFor(lang)) {
                QuoteGardenTheme(darkTheme = mode.resolveDark(isSystemInDarkTheme())) {
                    AppNavDisplay()
                }
            }
        }
    }
}
```

(`initialValue = languageStore.defaultLanguage()` avoids an EN flash on ZH phones before DataStore emits.)

- [ ] **Step 2: AppNavDisplay 的 Tabs 移入 composable 並取 strings**

In `AppNavDisplay.kt`, delete the top-level block:

```kotlin
private val Tabs = listOf(
    BottomTab("home", "Today"),
    BottomTab("browse", "Browse"),
    BottomTab("me", "Me"),
)
```

and at the start of `AppNavDisplay()` insert after `val backStack = ...` line:

```kotlin
    val strings = appStrings()
    val tabs = listOf(
        BottomTab("home", strings.tabToday),
        BottomTab("browse", strings.tabBrowse),
        BottomTab("me", strings.tabMe),
    )
```

Change `tabs = Tabs,` to `tabs = tabs,` in the `EditorialBottomBar` call. Add import `import com.quotegarden.core.designsystem.appStrings`.

- [ ] **Step 3: 編譯確認**

Run: `server/gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/quotegarden/MainActivity.kt app/src/main/java/com/quotegarden/core/navigation/AppNavDisplay.kt
git commit -m "feat(i18n): provide AppStrings at root and localize bottom tabs"
```

---

### Task 4: HomeScreen 字串替換 + 錯誤映射

**Files:**
- Modify: `app/src/main/java/com/quotegarden/feature/home/HomeScreen.kt`

Apply these replacements in order (each oldString is unique in the file):

- [ ] **Step 1: 問候語 + bottomBar + 節日標題**

1a. Greetings — replace:

```kotlin
    val greeting = when (now.hour) {
        in 5..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }
```

with:

```kotlin
    val strings = appStrings()
    val greeting = when (now.hour) {
        in 5..11 -> strings.greetingMorning
        in 12..17 -> strings.greetingAfternoon
        else -> strings.greetingEvening
    }
```

1b. bottomBar saveLabel — replace:

```kotlin
                    saveLabel = if (state.isCurrentQuoteFavorite) "Saved ✓" else "+ Save",
```

with:

```kotlin
                    saveLabel = if (state.isCurrentQuoteFavorite) strings.saved else strings.save,
```

1c. bottomBar onNext — `HomeActionsBar` label lives in `EditorialComponents.kt` (Task 6 handles it); HomeScreen passes no label for Next. Skip.

1d. Section label — replace `text = "TODAY'S QUOTE",` with `text = strings.todaysQuote,`.

1e. Loading — replace `text = "Loading...",` with `text = strings.loading,`.

- [ ] **Step 2: chips、空態、footnote、錯誤映射**

2a. Chips — replace `ChipLabel(text = "All",` with `ChipLabel(text = strings.chipAll,` and `ChipLabel(text = "More\u2026",` with `ChipLabel(text = strings.chipMore,`.

2b. Empty states — replace the whole `key == "empty"` branch:

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
                                    actionLabel = "Retry",
                                    onAction = { viewModel.onEvent(HomeEvent.Load) }
                                )
                            }
                        }
```

with:

```kotlin
                        key == "empty" && !state.isLoading -> {
                            when (state.emptyMode) {
                                EmptyMode.NoCategoryLines -> EditorialEmpty(
                                    title = strings.emptyNoCategoryTitle,
                                    body = strings.emptyNoCategoryBody,
                                    actionLabel = strings.switchToAll,
                                    onAction = { viewModel.onEvent(HomeEvent.SelectCategory(null)) }
                                )
                                EmptyMode.Generic -> EditorialEmpty(
                                    title = strings.emptyGenericTitle,
                                    body = strings.emptyGenericBody,
                                    actionLabel = strings.retry,
                                    onAction = { viewModel.onEvent(HomeEvent.Load) }
                                )
                            }
                        }
```

2c. Error mapping — replace:

```kotlin
                        error != null && key == error -> {
                            EditorialError(
                                message = error,
                                onRetry = { viewModel.onEvent(HomeEvent.Load) }
                            )
                        }
```

with:

```kotlin
                        error != null && key == error -> {
                            EditorialError(
                                message = if (error == "No other quotes yet") strings.noOtherQuotes else error,
                                onRetry = { viewModel.onEvent(HomeEvent.Load) }
                            )
                        }
```

(`error` is a local `val`, smart-cast safe. `HomeViewModel` stays language-free.)

2d. Footnote — replace `text = "Offline-first - cached first, syncs quietly",` with `text = strings.footnoteOffline,`.

2e. Add import `import com.quotegarden.core.designsystem.appStrings`.

- [ ] **Step 3: 編譯確認**

Run: `server/gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/quotegarden/feature/home/HomeScreen.kt
git commit -m "feat(i18n): localize HomeScreen strings"
```

---

### Task 5: Browse + CategoryDetail 字串替換

**Files:**
- Modify: `app/src/main/java/com/quotegarden/feature/browse/BrowseScreen.kt`
- Modify: `app/src/main/java/com/quotegarden/feature/browse/CategoryDetailScreen.kt`

- [ ] **Step 1: BrowseScreen（5 處 + import）**

In `BrowseScreen()`, after `val state by ...` insert `val strings = appStrings()` and replace:
- `greeting = "By mood"` → `greeting = strings.browseTitle`
- `text = "${state.totalCategories} categories - ${state.totalQuotes} lines",` → `text = strings.categoriesLines(state.totalCategories, state.totalQuotes),`
- `title = "Nothing to browse yet",` → `title = strings.browseEmptyTitle,`
- `body = "Pull to sync on Home.",` → `body = strings.browseEmptyBody,`
- `actionLabel = "Go to Home",` → `actionLabel = strings.goHome,`

In `CategoryTile`, replace `text = "${item.count} lines",` with `text = strings.linesCount(item.count),` — this requires `strings` in scope: add `val strings = appStrings()` at the top of the private `CategoryTile` composable too.

Add import `import com.quotegarden.core.designsystem.appStrings`.

(Category names `item.category` stay untranslated per spec non-goals.)

- [ ] **Step 2: CategoryDetailScreen（5 處 + import）**

In `CategoryDetailScreen()`, after `val state by ...` insert `val strings = appStrings()` and replace:
- `text = "<- Back",` → `text = strings.backArrow,`
- `text = "${state.quotes.size} lines",` → `text = strings.linesCount(state.quotes.size),`
- `title = "This category is empty",` → `title = strings.detailEmptyTitle,`
- `body = "Pull to sync or pick another.",` → `body = strings.detailEmptyBody,`
- `actionLabel = "Back",` → `actionLabel = strings.back,`

Add import `import com.quotegarden.core.designsystem.appStrings`.

(`state.category` and quote content/author stay untranslated.)

- [ ] **Step 3: 編譯確認**

Run: `server/gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/quotegarden/feature/browse/BrowseScreen.kt app/src/main/java/com/quotegarden/feature/browse/CategoryDetailScreen.kt
git commit -m "feat(i18n): localize Browse and CategoryDetail strings"
```

---

### Task 6: Me + ContactUs + EditorialError + MeViewModel

**Files:**
- Modify: `app/src/main/java/com/quotegarden/core/designsystem/EditorialComponents.kt` (EditorialError x2 + HomeActionsBar Next label)
- Modify: `app/src/main/java/com/quotegarden/feature/me/MeScreen.kt`
- Modify: `app/src/main/java/com/quotegarden/feature/me/ContactUsDialog.kt`
- Modify: `app/src/main/java/com/quotegarden/feature/me/MeViewModel.kt`

- [ ] **Step 1: EditorialError + HomeActionsBar 取 strings**

In `EditorialComponents.kt`, in `EditorialError` replace:
- `text = "Something went quiet",` → `text = LocalAppStrings.current.errorTitle,`
- `TextAction(label = "Retry", onClick = onRetry)` → `TextAction(label = LocalAppStrings.current.retry, onRetry)`

In `HomeActionsBar`, replace `label = "Next →",` with `label = LocalAppStrings.current.next,`.

(Designsystem file reads the CompositionLocal directly — no signature changes, no new imports needed: same file, same package.)

- [ ] **Step 2: MeViewModel 加 language**

Replace the whole `MeViewModel.kt` content with:

```kotlin
package com.quotegarden.feature.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotegarden.core.data.QuoteRepository
import com.quotegarden.core.datastore.AppLanguage
import com.quotegarden.core.datastore.LanguageStore
import com.quotegarden.core.datastore.ThemeMode
import com.quotegarden.core.datastore.ThemeStore
import com.quotegarden.core.model.Quote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeViewModel @Inject constructor(
    private val repo: QuoteRepository,
    private val themeStore: ThemeStore,
    private val languageStore: LanguageStore,
) : ViewModel() {
    val favorites: StateFlow<List<Quote>> = repo.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeMode: StateFlow<ThemeMode> = themeStore.mode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val language: StateFlow<AppLanguage> = languageStore.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), languageStore.defaultLanguage())

    fun onEvent(event: MeEvent) {
        when (event) {
            is MeEvent.Unfavorite -> viewModelScope.launch { repo.toggleFavorite(event.id) }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeStore.setMode(mode) }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch { languageStore.setLanguage(lang) }
    }
}

sealed interface MeEvent {
    data class Unfavorite(val id: String) : MeEvent
}
```

- [ ] **Step 3: MeScreen 字串 + Language 設定項**

In `MeScreen.kt`:
- After `val mode by ...` add:
```kotlin
    val strings = appStrings()
    val lang by vm.language.collectAsStateWithLifecycle()
    var showLanguage by remember { mutableStateOf(false) }
```
- After the `if (showTheme) { ... }` block add:
```kotlin
    if (showLanguage) {
        LanguageDialog(
            current = lang,
            onPick = { vm.setLanguage(it); showLanguage = false },
            onDismiss = { showLanguage = false }
        )
    }
```
- Replace literals:
  - `greeting = "Me"` → `greeting = strings.meTitle`
  - `"${list.size} saved - Quote Garden ${BuildConfig.VERSION_NAME}"` → `"${strings.savedCount(list.size)} - Quote Garden ${BuildConfig.VERSION_NAME}"`
  - `text = "SAVED",` → `text = strings.savedHeading,`
  - `title = "No saved quotes yet",` → `title = strings.noSavedTitle,`
  - `body = "Keep the lines that stay with you.",` → `body = strings.noSavedBody,`
  - `actionLabel = "Explore \u2192",` → `actionLabel = strings.explore,`
  - `text = "SETTINGS",` → `text = strings.settingsHeading,`
  - `SettingRow(label = "Theme", value = mode.label(), onClick = { showTheme = true })` → `SettingRow(label = strings.themeSetting, value = mode.label(strings), onClick = { showTheme = true })`
  - After the Theme SettingRow insert: `SettingRow(label = strings.languageSetting, value = lang.nativeLabel(), onClick = { showLanguage = true })`
  - `SettingRow(label = "Contact us", value = null, onClick = { showContact = true })` → `SettingRow(label = strings.contactUs, value = null, onClick = { showContact = true })`
  - `SettingRow(label = "About", value = null, onClick = { showAbout = true })` → `SettingRow(label = strings.about, value = null, onClick = { showAbout = true })`
  - `ThemeDialog(current = mode, ...)` stays; inside `ThemeDialog`, `title = { Text(text = "Theme", ...) }` → `title = { Text(text = appStrings().themeDialogTitle, ...) }` and `text = m.label(),` → `text = m.label(appStrings()),`
  - AboutDialog: `text = "Close",` → `text = appStrings().close,`; `title = { Text(text = "Quote Garden", ...) }` unchanged (brand); `text = "Offline-first daily quotes. Version ${BuildConfig.VERSION_NAME}.",` → `text = appStrings().aboutBody(BuildConfig.VERSION_NAME),`
- Replace the `ThemeMode.label()` helper with language-aware versions plus native language names — replace:
```kotlin
private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
```
with:
```kotlin
private fun ThemeMode.label(strings: AppStrings): String = when (this) {
    ThemeMode.SYSTEM -> strings.themeSystem
    ThemeMode.LIGHT -> strings.themeLight
    ThemeMode.DARK -> strings.themeDark
}

private fun AppLanguage.nativeLabel(): String = when (this) {
    AppLanguage.ZH -> "中文"
    AppLanguage.EN -> "English"
}
```
- Append a `LanguageDialog` after `ThemeDialog` (mirror structure, native labels):
```kotlin
@Composable
private fun LanguageDialog(current: AppLanguage, onPick: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(text = appStrings().languageSetting, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                AppLanguage.entries.forEach { l ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onPick(l) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = l == current, onClick = { onPick(l) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = l.nativeLabel(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    )
}
```
- Add imports: `com.quotegarden.core.datastore.AppLanguage`, `com.quotegarden.core.designsystem.AppStrings`, `com.quotegarden.core.designsystem.appStrings`.

- [ ] **Step 4: ContactUsDialog 字串**

Replace in `ContactUsDialog.kt`:
- `Text(text = "Close", ...)` → `Text(text = appStrings().close, ...)`
- `title = { Text(text = "Contact us", ...) }` → `title = { Text(text = appStrings().contactTitle, ...) }`
- `text = "Scan the QR code with WeChat to reach the Quote Garden team.",` → `text = appStrings().contactBody,`
- `contentDescription = "WeChat QR code",` → `contentDescription = appStrings().contactQrDesc,`
- `text = "Long-press the image to save it.",` → `text = appStrings().contactHint,`
- Add import `import com.quotegarden.core.designsystem.appStrings`.

- [ ] **Step 5: 編譯確認**

Run: `server/gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/quotegarden/core/designsystem/EditorialComponents.kt app/src/main/java/com/quotegarden/feature/me/MeScreen.kt app/src/main/java/com/quotegarden/feature/me/ContactUsDialog.kt app/src/main/java/com/quotegarden/feature/me/MeViewModel.kt
git commit -m "feat(i18n): localize Me screens and add Language setting"
```

---

### Task 7: MeViewModelTest 更新 + 全量迴歸

**Files:**
- Modify: `app/src/test/java/com/quotegarden/feature/me/MeViewModelTest.kt`

- [ ] **Step 1: 更新測試（三處：helper + 構造調用 + 新斷言）**

1a. Add imports `com.quotegarden.core.datastore.AppLanguage` and `com.quotegarden.core.datastore.LanguageStore`, plus helper after `themeStore()`:

```kotlin
    private fun languageStore(): LanguageStore {
        val store = mockk<LanguageStore>(relaxed = true)
        every { store.language } returns flowOf(AppLanguage.ZH)
        return store
    }
```

1b. Update all three existing constructions `MeViewModel(repo, themeStore())` → `MeViewModel(repo, themeStore(), languageStore())`. There are exactly 3 occurrences: in `emits favorites from repository`, `Unfavorite calls repo toggleFavorite`, `setThemeMode delegates to store`. For the last one the call is `MeViewModel(repoWithEmptyFavorites(), store)` → `MeViewModel(repoWithEmptyFavorites(), store, languageStore())`.

1c. Append new test at end of class:

```kotlin
    @Test fun `setLanguage delegates to store`() = runTest {
        val store = languageStore()
        val vm = MeViewModel(repoWithEmptyFavorites(), themeStore(), store)
        vm.setLanguage(AppLanguage.EN)
        coVerify { store.setLanguage(AppLanguage.EN) }
    }
```

- [ ] **Step 2: 運行 focused tests**

Run: `server/gradlew.bat :app:testDebugUnitTest --tests "com.quotegarden.feature.me.MeViewModelTest"`
Expected: BUILD SUCCESSFUL, 4 tests pass

- [ ] **Step 3: 全量迴歸**

Run: `server/gradlew.bat :app:test`
Expected: BUILD SUCCESSFUL except the 2 known pre-existing `BrowseViewModelTest` failures (`WhileSubscribed` without subscriber — unrelated to this plan, accepted by user on 2026-09-09). All i18n tests (LanguageStoreTest 5, AppStringsTest 5, MeViewModelTest 4, HomeViewModelTest 9) must pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/com/quotegarden/feature/me/MeViewModelTest.kt
git commit -m "test(i18n): cover MeViewModel language delegation"
```

---

## Manual verification checklist

- 系統中文首啟 → 中文；系統英文首啟 → 英文；無閃爍（initialValue 即預設語言）。
- Me → Language 切英文 → 全 App 即時變英文免重啟；殺進程重進保持英文；切回中文同理。
- 360dp + 410dp：`下一条 →` / `已保存 ✓` / Tab `今日/逛逛/我的` 不截斷；1.3x 字體不斷行。
- 引文正文、作者、分類名不受語言切換影響。
- 空庫：`Next` 報 `暂无其他句子`（中文）/ `No other quotes yet`（英文）。
- Contact us 二維碼描述、About 版本號雙語正確；品牌名 Quote Garden 兩語言一致。
