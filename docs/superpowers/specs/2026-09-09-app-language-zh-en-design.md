# App Language (ZH / EN) Design

**Date:** 2026-09-09
**Status:** Draft — pending user review
**Scope:** App UI chrome only (buttons, labels, titles, dialogs, tabs). Quote content/author/category untouched.

## 1. Goal

Support two UI languages: Simplified Chinese (ZH) and English (EN).
First launch follows the system locale (system language starts with `en` → EN, otherwise → ZH).
User can switch in Me → Language; switch applies instantly without restart and persists via DataStore.

## 2. Non-goals

- Quote `content` / `translation` / `author` / `category` display logic unchanged (always `content`).
- No `strings.xml` migration; no `AppCompatDelegate` per-app locales; no Activity recreate.
- No plural rules beyond existing style (`N lines`, `N saved` in both languages).
- No third language scaffolding (adding one later = new `AppStrings` instance + enum entry).

## 3. Architecture

New `core/designsystem/AppStrings.kt`:

```kotlin
data class AppStrings(
    // common
    val retry: String,
    val close: String,
    val errorTitle: String,    // "Something went quiet" / "这里静悄悄"
    val back: String,
    // bottom tabs
    val tabToday: String,
    val tabBrowse: String,
    val tabMe: String,
    // home
    val greetingMorning: String,
    val greetingAfternoon: String,
    val greetingEvening: String,
    val todaysQuote: String,
    val loading: String,
    val save: String,          // "+ Save" / "+ 保存"
    val saved: String,         // "Saved ✓" / "已保存 ✓"
    val next: String,          // "Next →" / "下一条 →"
    val chipAll: String,
    val chipMore: String,      // "More…" / "更多…"
    val emptyGenericTitle: String,
    val emptyGenericBody: String,
    val emptyNoCategoryTitle: String,
    val emptyNoCategoryBody: String,
    val switchToAll: String,
    val footnoteOffline: String,
    val noOtherQuotes: String, // replaces ViewModel "No other quotes yet"
    // browse
    val browseTitle: String,   // "By mood" / "按心情"
    val categoriesLines: (Int, Int) -> String, // "N categories - M lines"
    val linesCount: (Int) -> String,           // "N lines"
    val goHome: String,
    val browseEmptyTitle: String, // "Nothing to browse yet" / "暂无分类"
    val browseEmptyBody: String,  // "Pull to sync on Home." / "去首页同步试试。"
    // category detail
    val backArrow: String,     // "<- Back" / "<- 返回"
    val back: String,          // "Back" / "返回"
    val detailEmptyTitle: String, // "This category is empty" / "该分类是空的"
    val detailEmptyBody: String,  // "Pull to sync or pick another." / "同步后重试，或换个分类。"
    // me
    val meTitle: String,         // "Me" / "我的"    val savedCount: (Int) -> String, // "N saved" / "已存 N 条"
    val savedHeading: String,    // "SAVED" / "已保存"
    val settingsHeading: String, // "SETTINGS" / "设置"
    val languageSetting: String, // "Language" / "语言"
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
    val aboutBody: (String) -> String, // version string
    // contact dialog
    val contactTitle: String,
    val contactBody: String,   // WeChat QR line, translated per §7 table
    val contactHint: String,   // long-press hint
    val contactQrDesc: String, // accessibility: "WeChat QR code" / "微信二维码"
)

val ZhStrings = AppStrings(...)
val EnStrings = AppStrings(...)

val LocalAppStrings = compositionLocalOf { EnStrings }
fun appStrings(): AppStrings = LocalAppStrings.current
```

New `core/datastore/LanguageStore.kt` (mirrors `ThemeStore`):

```kotlin
enum class AppLanguage { ZH, EN }

@Singleton
class LanguageStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    // Hilt cannot inject default constructor params, so the system-locale rule
    // lives in a plain method with a default argument instead of a lambda dep.
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

`PreferencesKeys.LANGUAGE = stringPreferencesKey("language")` alongside existing keys.

## 4. Wiring

- `MainActivity`: inject `LanguageStore`, `collectAsStateWithLifecycle(initialValue = …)` (initial must apply the same system-locale rule to avoid EN flash on ZH phones — expose `LanguageStore.defaultLanguage()` or compute once), wrap content:
  `CompositionLocalProvider(LocalAppStrings provides stringsFor(lang)) { QuoteGardenTheme { … } }`.
- `AppNavDisplay`: move `Tabs` list inside the composable, labels from `appStrings()`.
- Each screen replaces literals with `appStrings().x`. `HomeViewModel` keeps throwing `IllegalStateException("No other quotes yet")` unchanged; HomeScreen maps exactly that message to `strings.noOtherQuotes`, all other error messages render raw. ViewModels stay language-free.
- Language labels in the dialog show natively: `中文` / `English` (not translated).

## 5. Me settings UI

- `SettingRow(label = strings.languageSetting, value = 中文/English, onClick = showLanguage)`.
- `LanguageDialog` mirrors `ThemeDialog` (AlertDialog + RadioButton rows).
- `MeViewModel`: add `val language: StateFlow<AppLanguage>` (WhileSubscribed, initial = system default) + `fun setLanguage(lang)`. Constructor gains `LanguageStore` — update `MeViewModelTest` accordingly.

## 6. Testing

- `LanguageStoreTest`: set/get roundtrip ZH↔EN; invalid stored value falls back to `defaultLanguage()`; `defaultLanguage(Locale("en"))` is EN, `Locale("zh")`/`Locale("fr")` are ZH. DataStore faked with the `MutableStateFlow` + `updateData` mock pattern from `CategoryPreferenceStoreTest`.
- `MeViewModelTest`: construct with mocked `LanguageStore`, existing tests keep passing.
- Data class guarantees ZH/EN field parity at compile time — no parity test needed.
- Full regression `:app:test` (excluding the 2 known pre-existing `BrowseViewModelTest` failures).

## 7. String table (EN → ZH)

| Key | EN | ZH |
|---|---|---|
| retry | Retry | 重试 |
| errorTitle | Something went quiet | 这里静悄悄 |
| close | Close | 关闭 |
| back | Back | 返回 |
| tabToday / tabBrowse / tabMe | Today / Browse / Me | 今日 / 逛逛 / 我的 |
| greetingMorning/Afternoon/Evening | Good Morning / Good Afternoon / Good Evening | 早上好 / 下午好 / 晚上好 |
| todaysQuote | TODAY'S QUOTE | 今日推荐 |
| loading | Loading... | 加载中… |
| save / saved / next | + Save / Saved ✓ / Next → | + 保存 / 已保存 ✓ / 下一条 → |
| chipAll / chipMore | All / More… | 全部 / 更多… |
| emptyGenericTitle/Body | Nothing here yet / Check your connection and try again. | 还没有内容 / 检查网络后重试。 |
| emptyNoCategoryTitle/Body | No lines in this category yet / This category hasn't been synced to your device. | 该分类暂无内容 / 该分类尚未同步到本机。 |
| switchToAll | Switch to All | 切换到全部 |
| footnoteOffline | Offline-first - cached first, syncs quietly | 离线优先 · 本地缓存，后台静默同步 |
| noOtherQuotes | No other quotes yet | 暂无其他句子 |
| browseTitle | By mood | 按心情 |
| categoriesLines | "$c categories - $q lines" | "共 $c 个分类 · $q 条" |
| linesCount | "$n lines" | "$n 条" |
| goHome | Go to Home | 回首页 |
| backArrow / back | <- Back / Back | <- 返回 / 返回 |
| browseEmptyTitle/Body | Nothing to browse yet / Pull to sync on Home. | 暂无分类 / 去首页同步试试。 |
| detailEmptyTitle/Body | This category is empty / Pull to sync or pick another. | 该分类是空的 / 同步后重试，或换个分类。 |
| meTitle | Me | 我的 |
| savedCount | "$n saved" | "已存 $n 条" |
| savedHeading / settingsHeading | SAVED / SETTINGS | 已保存 / 设置 |
| languageSetting | Language | 语言 |
| themeSetting / contactUs / about | Theme / Contact us / About | 主题 / 联系我们 / 关于 |
| noSavedTitle/Body/explore | No saved quotes yet / Keep the lines that stay with you. / Explore → | 还没有收藏 / 留下打动你的句子。 / 去逛逛 → |
| themeDialogTitle/System/Light/Dark | Theme / System / Light / Dark | 主题 / 跟随系统 / 浅色 / 深色 |
| aboutBody | Offline-first daily quotes. Version X. | 离线优先的每日句子。版本 X。 |
| contactTitle/Body/Hint | Contact us / Scan the QR code with WeChat to reach the Quote Garden team. / Long-press the image to save it. | 联系我们 / 用微信扫描二维码联系 Quote Garden 团队。 / 长按图片保存。 |
| themeMode value labels | System / Light / Dark | 跟随系统 / 浅色 / 深色 |

Note: "Quote Garden" brand name stays untranslated everywhere.

## 8. Manual verification

- 系統中文首啟 → 中文；系統英文首啟 → 英文；切換到 Me → Language 改英文 → 全 App 即時變英文，殺進程重進保持。
- 360dp 小屏：`下一条 →` / `已保存 ✓` 不截斷；1.3x 字體不斷行。
- 引文正文、作者、分類名不受語言切換影響。
