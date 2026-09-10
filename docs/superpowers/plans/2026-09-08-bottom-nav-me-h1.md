# 底部导航 Home-Me + H1 杂志刷新 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 加 editorial 文字式底部栏（Today｜Me），新建 Me 页（收藏+设置聚合），Home 按 H1 杂志方向提纯，删 Favorite 独立目的地。

**Architecture:** 先加纯新增件（ThemeStore、BottomBar/SettingRow，保证每步绿色）；再一个原子任务完成改名+MeScreen+导航重接线+删除（避免中间态编译断档）；然后 MainActivity 接主题、H1 点睛、统一验证。数据库 version 不变，无 migration。

**Tech Stack:** Kotlin + Compose BOM 2025.10.01 + Navigation 3 + Hilt 2.60.1 + Room 2.8.4 + DataStore Preferences + JUnit4 + MockK。

**Spec:** `docs/superpowers/specs/2026-09-08-dailymind-bottom-nav-editorial-design.md`

**TDD 说明：** 新逻辑（ThemeMode 映射、setThemeMode 委托）走红绿循环；composable 与接线以编译 + 全量单测 + 真机验证为准（本项目无 compose UI 测试基建，不新增）。

---

## File Structure

```
app/src/main/java/com/dailymind/core/datastore/
  ThemeStore.kt                          # Create: ThemeMode 枚举 + resolveDark + DataStore 读写
app/src/main/java/com/dailymind/core/designsystem/
  EditorialComponents.kt                 # Modify: 加 BottomTab + EditorialBottomBar + SettingRow
app/src/main/java/com/dailymind/feature/me/
  MeViewModel.kt                         # Create(git mv 自 favorite/): 改名 + 加 themeMode/setThemeMode
  MeScreen.kt                            # Create: SAVED 段 + SETTINGS 段 + 两个 dialog
app/src/main/java/com/dailymind/feature/favorite/
  FavoriteViewModel.kt                   # Delete(git mv 走) / FavoriteScreen.kt # Delete(git rm)
  （favorite/ 目录清空后自动消失）
app/src/main/java/com/dailymind/core/navigation/
  Route.kt                               # Modify: Favorite → Me
  AppNavDisplay.kt                       # Modify: 两 tab + single-top select + 底部栏
app/src/main/java/com/dailymind/feature/home/
  HomeScreen.kt                          # Modify: 删 onNavigateToFavorite 参数 + 两处 Saved → + H1 眉题变色
app/src/main/java/com/dailymind/
  MainActivity.kt                        # Modify: 注入 ThemeStore，darkTheme 跟设置走
app/src/test/java/com/dailymind/core/datastore/
  ThemeModeTest.kt                       # Create: 映射 3 测试
app/src/test/java/com/dailymind/feature/me/
  MeViewModelTest.kt                     # Create(git mv 自 favorite/): 改名 + 加 setTheme 测试
app/src/test/java/com/dailymind/core/navigation/
  RouteTest.kt                           # Modify: Favorite → Me
```

---

### Task 1: ThemeStore + ThemeMode（TDD 红绿）

**Files:**
- Create: `app/src/main/java/com/dailymind/core/datastore/ThemeStore.kt`
- Create: `app/src/test/java/com/dailymind/core/datastore/ThemeModeTest.kt`

- [ ] **Step 1: 写失败的测试（红）**

创建 `app/src/test/java/com/dailymind/core/datastore/ThemeModeTest.kt`，内容：

```kotlin
package com.dailymind.core.datastore

import org.junit.Assert.*
import org.junit.Test

class ThemeModeTest {
    @Test fun `LIGHT always resolves to false`() {
        assertFalse(ThemeMode.LIGHT.resolveDark(true))
        assertFalse(ThemeMode.LIGHT.resolveDark(false))
    }

    @Test fun `DARK always resolves to true`() {
        assertTrue(ThemeMode.DARK.resolveDark(true))
        assertTrue(ThemeMode.DARK.resolveDark(false))
    }

    @Test fun `SYSTEM mirrors system flag`() {
        assertTrue(ThemeMode.SYSTEM.resolveDark(true))
        assertFalse(ThemeMode.SYSTEM.resolveDark(false))
    }
}
```

- [ ] **Step 2: 运行测试，确认 FAIL**

Run: `$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"; .\server\gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.core.datastore.ThemeModeTest"`
Expected: FAIL，含 `Unresolved reference 'ThemeMode'`

- [ ] **Step 3: 写最小实现（绿）**

创建 `app/src/main/java/com/dailymind/core/datastore/ThemeStore.kt`，内容：

```kotlin
package com.dailymind.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    fun resolveDark(isSystemDark: Boolean): Boolean = when (this) {
        LIGHT -> false
        DARK -> true
        SYSTEM -> isSystemDark
    }
}

@Singleton
class ThemeStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val mode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[PreferencesKeys.THEME] ?: "SYSTEM") }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.THEME] = mode.name }
    }
}
```

（说明：`PreferencesKeys.THEME` 已存在，无需动 `PreferencesDataStore.kt`；
`DataStore<Preferences>` 由现有 `DataStoreModule` 提供，无需新 module。）

- [ ] **Step 4: 运行测试，确认 PASS**

Run: 同 Step 2 命令
Expected: PASS（3/3）

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dailymind/core/datastore/ThemeStore.kt app/src/test/java/com/dailymind/core/datastore/ThemeModeTest.kt
git commit -m "feat(theme): ThemeStore with system-light-dark mode backed by DataStore"
```

---

### Task 2: EditorialBottomBar + SettingRow（纯新增）

**Files:**
- Modify: `app/src/main/java/com/dailymind/core/designsystem/EditorialComponents.kt`

- [ ] **Step 1: 文件顶部 import 区追加**

```kotlin
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.graphics.RectangleShape
```

（`Box`、`HorizontalDivider`、`TextButton`、`heightIn`、`background`、`clickable`、
`MutableInteractionSource`、`Role`、`padding`、`remember` 本文件已有，不重复加。）

- [ ] **Step 2: 文件末尾追加两个 composable**

```kotlin
data class BottomTab(val id: String, val label: String)

@Composable
fun EditorialBottomBar(
    tabs: List<BottomTab>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEach { tab ->
                val selected = tab.id == selectedId
                TextButton(
                    onClick = { onSelect(tab.id) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = RectangleShape
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else androidx.compose.ui.graphics.Color.Transparent
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingRow(
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClick = onClick, role = Role.Button, interactionSource = interaction, indication = null)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            value?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "→",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
    }
}
```

- [ ] **Step 3: 编译**

Run: `$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"; .\server\gradlew.bat :app:compileDebugKotlin 2>&1 | Select-Object -Last 5`
Expected: `BUILD SUCCESSFUL`（纯新增，无调用方）

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/dailymind/core/designsystem/EditorialComponents.kt
git commit -m "feat(designsystem): EditorialBottomBar text tabs plus SettingRow"
```

---

### Task 3（原子任务）: 改名 MeViewModel + 新建 MeScreen + 导航重接线 + 删除 FavoriteScreen

**Files:**
- git mv: `.../feature/favorite/FavoriteViewModel.kt` → `.../feature/me/MeViewModel.kt`
- git mv: `.../test/.../feature/favorite/FavoriteViewModelTest.kt` → `.../test/.../feature/me/MeViewModelTest.kt`
- Create: `app/src/main/java/com/dailymind/feature/me/MeScreen.kt`
- Modify: `app/src/main/java/com/dailymind/core/navigation/Route.kt`
- Modify: `app/src/main/java/com/dailymind/core/navigation/AppNavDisplay.kt`
- Modify: `app/src/main/java/com/dailymind/feature/home/HomeScreen.kt`（仅删 Saved → 两处 + 参数，不含 H1 变色）
- Modify: `app/src/test/java/com/dailymind/core/navigation/RouteTest.kt`
- Modify: `app/build.gradle.kts`（`buildFeatures` 加 `buildConfig = true`，AGP 9 默认不生成 BuildConfig）
- Delete(git rm): `app/src/main/java/com/dailymind/feature/favorite/FavoriteScreen.kt`

本任务必须同批次落地：改名会立刻破坏 `FavoriteScreen` 与 `AppNavDisplay` 的编译，
Me entry 又依赖尚不存在的 `MeScreen`，拆开提交必红。顺序按 Step 来。

- [ ] **Step 1: git mv 改名（不改内容）**

```bash
git mv app/src/main/java/com/dailymind/feature/favorite/FavoriteViewModel.kt app/src/main/java/com/dailymind/feature/me/MeViewModel.kt
git mv app/src/test/java/com/dailymind/feature/favorite/FavoriteViewModelTest.kt app/src/test/java/com/dailymind/feature/me/MeViewModelTest.kt
```

- [ ] **Step 2: MeViewModel 全文替换（改名 + 加主题委托）**

`app/src/main/java/com/dailymind/feature/me/MeViewModel.kt` 整个文件替换为：

```kotlin
package com.dailymind.feature.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.datastore.ThemeMode
import com.dailymind.core.datastore.ThemeStore
import com.dailymind.core.model.Quote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeViewModel @Inject constructor(
    private val repo: QuoteRepository,
    private val themeStore: ThemeStore
) : ViewModel() {
    val favorites: StateFlow<List<Quote>> = repo.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeMode: StateFlow<ThemeMode> = themeStore.mode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun onEvent(event: MeEvent) {
        when (event) {
            is MeEvent.Unfavorite -> viewModelScope.launch { repo.toggleFavorite(event.id) }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeStore.setMode(mode) }
    }
}

sealed interface MeEvent {
    data class Unfavorite(val id: String) : MeEvent
}
```

- [ ] **Step 3: MeViewModelTest 全文替换（改名 + 加 setTheme 测试）**

`app/src/test/java/com/dailymind/feature/me/MeViewModelTest.kt` 整个文件替换为：

```kotlin
package com.dailymind.feature.me

import com.dailymind.MainDispatcherRule
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.datastore.ThemeMode
import com.dailymind.core.datastore.ThemeStore
import com.dailymind.core.model.Quote
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private fun repoWithEmptyFavorites(): QuoteRepository {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavorites() } returns flowOf(emptyList())
        return repo
    }

    private fun themeStore(): ThemeStore {
        val store = mockk<ThemeStore>(relaxed = true)
        every { store.mode } returns flowOf(ThemeMode.SYSTEM)
        return store
    }

    @Test fun `emits favorites from repository`() = runTest {
        val repo = mockk<QuoteRepository>()
        every { repo.observeFavorites() } returns flowOf(
            listOf(Quote("1", "Hello", "你好", "Anon", "life", 1, null, null, 1000L))
        )
        val vm = MeViewModel(repo, themeStore())
        assertEquals("Hello", vm.favorites.first { it.isNotEmpty() }[0].content)
    }

    @Test fun `Unfavorite calls repo toggleFavorite`() = runTest {
        val repo = repoWithEmptyFavorites()
        val vm = MeViewModel(repo, themeStore())
        vm.onEvent(MeEvent.Unfavorite("42"))
        coVerify { repo.toggleFavorite("42") }
    }

    @Test fun `setThemeMode delegates to store`() = runTest {
        val store = themeStore()
        val vm = MeViewModel(repoWithEmptyFavorites(), store)
        vm.setThemeMode(ThemeMode.DARK)
        coVerify { store.setMode(ThemeMode.DARK) }
    }
}
```

注意：`Unfavorite` 测试沿用本仓库既有模式（与原 `FavoriteViewModelTest` 同构，原测试即如此通过，
`MainDispatcherRule` 下直接 `coVerify` 有效）。

- [ ] **Step 3.5: 开启 BuildConfig 生成（MeScreen 的版本号依赖它）**

`app/build.gradle.kts` 中：
```kotlin
    buildFeatures { compose = true }
```
→
```kotlin
    buildFeatures {
        compose = true
        buildConfig = true
    }
```

- [ ] **Step 4: 新建 MeScreen（含两个 dialog）**

创建 `app/src/main/java/com/dailymind/feature/me/MeScreen.kt`，全文：

```kotlin
package com.dailymind.feature.me

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailymind.BuildConfig
import com.dailymind.core.datastore.ThemeMode
import com.dailymind.core.designsystem.EditorialEmpty
import com.dailymind.core.designsystem.IndexRow
import com.dailymind.core.designsystem.SettingRow

@Composable
fun MeScreen(
    vm: MeViewModel = hiltViewModel(),
    onExplore: () -> Unit = {}
) {
    val list by vm.favorites.collectAsStateWithLifecycle()
    val mode by vm.themeMode.collectAsStateWithLifecycle()
    var showTheme by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    if (showTheme) {
        ThemeDialog(
            current = mode,
            onPick = { vm.setThemeMode(it); showTheme = false },
            onDismiss = { showTheme = false }
        )
    }
    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(64.dp))
                Text(
                    text = "Me",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${list.size} saved · DailyMind ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "SAVED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (list.isEmpty()) {
                item {
                    EditorialEmpty(
                        title = "No saved quotes yet",
                        body = "Keep the lines that stay with you.",
                        actionLabel = "Explore →",
                        onAction = onExplore
                    )
                }
            } else {
                itemsIndexed(list, key = { _, q -> q.id }) { index, q ->
                    IndexRow(
                        number = (index + 1).toString().padStart(2, '0'),
                        content = q.content,
                        author = q.author,
                        onClick = {},
                        onSwipeOut = { vm.onEvent(MeEvent.Unfavorite(q.id)) },
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                SettingRow(label = "Theme", value = mode.label(), onClick = { showTheme = true })
                SettingRow(label = "About", value = null, onClick = { showAbout = true })
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

@Composable
private fun ThemeDialog(
    current: ThemeMode,
    onPick: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(text = "Theme", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                ThemeMode.entries.forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onPick(m) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = m == current, onClick = { onPick(m) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = m.label(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        title = { Text(text = "DailyMind", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Text(
                text = "Offline-first daily quotes. Version ${BuildConfig.VERSION_NAME}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}
```

（说明：`headlineSmall` 是 M3 Typography 默认项，本项目未覆盖，取值默认；
`ThemeMode.entries` 需 Kotlin 1.9+，本项目 2.3.20。）

- [ ] **Step 5: Route.kt 删除 Favorite、加 Me**

`app/src/main/java/com/dailymind/core/navigation/Route.kt` 整个文件替换为：

```kotlin
package com.dailymind.core.navigation

sealed interface Route {
    data object Home : Route
    data object Me : Route
    data object History : Route
    data object Profile : Route
}
```

（`History`/`Profile` 按 spec 非目标原样保留。）

- [ ] **Step 6: AppNavDisplay 全文替换（两 tab + single-top + 底部栏）**

`app/src/main/java/com/dailymind/core/navigation/AppNavDisplay.kt` 整个文件替换为：

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
import com.dailymind.feature.home.HomeScreen
import com.dailymind.feature.me.MeScreen

private val Tabs = listOf(BottomTab("home", "Today"), BottomTab("me", "Me"))

private fun Route.tabId(): String = when (this) {
    is Route.Home -> "home"
    is Route.Me -> "me"
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
                    else -> NavEntry(route) { }
                }
            },
            modifier = Modifier.weight(1f)
        )
        EditorialBottomBar(
            tabs = Tabs,
            selectedId = backStack.lastOrNull()?.tabId() ?: "home",
            onSelect = { id -> select(if (id == "me") Route.Me else Route.Home) }
        )
    }
}
```

single-top 语义说明：`select` 先 `remove(route)` 再 `add`（每次 select 都先清同路由，
栈里永无重复），最多形如 `[Home]`、`[Me]`、`[Home, Me]`、`[Me, Home]` 四种；
Me 按返回 → 弹回 Home。
`onBack` 的 `size > 1` 守卫 + Home 退出行为由 Task 6 真机验证。

- [ ] **Step 7: HomeScreen 删 Saved → 两处 + 参数（不含 H1 变色）**

三处精确替换（`app/src/main/java/com/dailymind/feature/home/HomeScreen.kt`）：

(a) 参数删除：
```kotlin
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToFavorite: () -> Unit = {}
) {
```
→
```kotlin
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
```

(b) 浏览态分支：
```kotlin
                    if (browsing) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            TextAction(
                                label = "Saved →",
                                onClick = onNavigateToFavorite,
                                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd)
                            )
                        }
                    } else {
```
→
```kotlin
                    if (browsing) {
                        Spacer(modifier = Modifier.height(48.dp))
                    } else {
```
（`TextAction` 原最小高度 48.dp，`Spacer(48.dp)` 等高占位，节奏不变。）

(c) 常态 TopBar：
```kotlin
                        EditorialTopBar(
                            date = date,
                            greeting = greeting,
                            action = {
                                TextAction(label = "Saved →", onClick = onNavigateToFavorite)
                            }
                        )
```
→
```kotlin
                        EditorialTopBar(
                            date = date,
                            greeting = greeting
                        )
```

(d) 删 `Box` import（文件中仅 (b) 一处使用 `Box`）：
```kotlin
import androidx.compose.foundation.layout.Box
```
整行删除。（`TextAction` import 保留，`Next →` 仍在用。）

- [ ] **Step 8: RouteTest 更新 + 删 FavoriteScreen**

`app/src/test/java/com/dailymind/core/navigation/RouteTest.kt` 第 8 行：
```kotlin
        assertNotEquals(Route.Home, Route.Favorite)
```
→
```kotlin
        assertNotEquals(Route.Home, Route.Me)
```

```bash
git rm app/src/main/java/com/dailymind/feature/favorite/FavoriteScreen.kt
```

- [ ] **Step 9: 编译 + 目标测试**

Run: `$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"; .\server\gradlew.bat :app:compileDebugKotlin 2>&1 | Select-Object -Last 5`
Expected: `BUILD SUCCESSFUL`

Run: `$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"; .\server\gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.me.MeViewModelTest" --tests "com.dailymind.core.navigation.RouteTest" 2>&1 | Select-Object -Last 5`
Expected: `BUILD SUCCESSFUL`（MeViewModelTest 3/3，RouteTest 1/1）

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/dailymind/feature/me/MeViewModel.kt app/src/main/java/com/dailymind/feature/me/MeScreen.kt app/src/main/java/com/dailymind/core/navigation/Route.kt app/src/main/java/com/dailymind/core/navigation/AppNavDisplay.kt app/src/main/java/com/dailymind/feature/home/HomeScreen.kt app/src/test/java/com/dailymind/feature/me/MeViewModelTest.kt app/src/test/java/com/dailymind/core/navigation/RouteTest.kt
git commit -m "feat(nav): two-tab Home-Me shell, Me aggregates favorites and settings"
```
（注：两个 git mv 与一个 git rm 的暂存已在 Step 1/8 完成，无需再 add。）

---

### Task 4: MainActivity 接主题设置

**Files:**
- Modify: `app/src/main/java/com/dailymind/MainActivity.kt`

- [ ] **Step 1: 整个文件替换为**

```kotlin
package com.dailymind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailymind.core.datastore.ThemeMode
import com.dailymind.core.datastore.ThemeStore
import com.dailymind.core.designsystem.theme.DailyMindTheme
import com.dailymind.core.navigation.AppNavDisplay
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var themeStore: ThemeStore

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val mode by themeStore.mode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            DailyMindTheme(darkTheme = mode.resolveDark(isSystemInDarkTheme())) {
                AppNavDisplay()
            }
        }
    }
}
```

（`@AndroidEntryPoint` 支持字段注入；`ThemeStore` 是 `@Singleton` 构造注入，
`DataStore` 由现有 `DataStoreModule` 提供，无需新 module。）

- [ ] **Step 2: 编译**

Run: `$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"; .\server\gradlew.bat :app:compileDebugKotlin 2>&1 | Select-Object -Last 5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/dailymind/MainActivity.kt
git commit -m "feat(theme): drive dark mode from stored theme preference"
```

---

### Task 5: Home H1 点睛（眉题变 accent）

**Files:**
- Modify: `app/src/main/java/com/dailymind/feature/home/HomeScreen.kt`

背景：现状 Home 与 H1 mock 的差距经核对只剩眉题颜色（问候已是 40sp serif displayLarge，
quote 已是 26sp serif headlineLarge，间距节奏保留）。本任务只做这一处精确改动。

- [ ] **Step 1: 眉题颜色改 accent**

```kotlin
                        Text(
                            text = "TODAY'S QUOTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
```
→
```kotlin
                        Text(
                            text = "TODAY'S QUOTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
```

- [ ] **Step 2: 编译**

Run: `$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"; .\server\gradlew.bat :app:compileDebugKotlin 2>&1 | Select-Object -Last 5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/dailymind/feature/home/HomeScreen.kt
git commit -m "feat(home): H1 magazine eyebrow in accent color"
```

---

### Task 6: 全量验证 + 真机清单

- [ ] **Step 1: 全量单测**

Run: `$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"; .\server\gradlew.bat :app:test 2>&1 | Select-Object -Last 5`
Expected: `BUILD SUCCESSFUL`，共 44 个测试（原 40 −删除的 SeedImporterTest 早已不在内；
本次 +1 `MeViewModelTest.setThemeMode`、+3 `ThemeModeTest`），各 suite 0 failures/errors
（以 `app\build\test-results\testDebugUnitTest\*.xml` 为准）。

- [ ] **Step 2: assembleDebug**

Run: `$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"; .\server\gradlew.bat :app:assembleDebug 2>&1 | Select-Object -Last 3`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 真机/模拟器清单（卸载重装后逐项）**

- 底部栏常驻两项 Today｜Me，选中下划线跟随，深浅主题各看一遍；
- 切 tab 来回：Home quote/浏览态不重载；重复点已选 tab 不叠屏；
- 返回键：Me → 回 Home；Home → 退 App（验证 `onBack` 的 `size > 1` 守卫行为；
  若 Home 按返回无反应， fallback：`onBack` 改为无条件 `removeLastOrNull` 并加
  `if (backStack.isEmpty()) (context as Activity).finish()`——只在验证失败时执行，
  届时单独 commit 说明）；
- Me 空收藏显示 EditorialEmpty，Explore → 跳回 Home；
- Me 左滑移除、Theme 三选即时生效（切 Dark/Light/System 各看一次）、About dialog；
- 360dp / 大字号下 tab 不截断。

- [ ] **Step 4: 收尾**

Run: `git status --short`
Expected: 无 tracked 修改残留。有顺手修复则单独 commit，否则不提交空 commit。

---

## Self-review checklist

1. Spec coverage:
   - §1 底部栏 Today｜Me + A 式文字 → Task 2（组件）+ Task 3 Step 6（接线）
   - §2 MeScreen/SettingRow → Task 3 Step 4；改名 MeViewModel → Step 2；删 FavoriteScreen/Route.Favorite → Step 5/8；
     删 Saved → → Step 7；dialog 无路由 → Step 4 内联 dialog
   - §3 single-top/返回/状态保持 → Step 6（含 fallback 规则）+ Task 6 Step 3 验证
   - §4 Theme 三选 + THEME key → Task 1 + Task 3 Step 2（委托）+ Task 4（接线）；
     About dialog → Step 4；清理缓存等不在内 → 全计划无此代码
   - §5 错误处理 → THEME 回落（Task 1 `getOrDefault`）、空收藏 EditorialEmpty（Step 4）、
     token 化颜色（各组件全用 colorScheme）、48/56dp 触区（组件内写死）
   - §6 测试 → Task 1 红绿、Task 3 Step 9（3+1）、Task 6（44 全绿 + assemble + 真机）
   - §2 非目标（History/Profile/M3 栏/H2/新路由）→ 全计划无此代码；Route.History/Profile 原样保留
2. Placeholders — 无。全部代码块完整可落；数量 44 = 40+1+3 可验算；
   `headlineSmall`/`ThemeMode.entries` 均为现成 API（Kotlin 2.3.20 支持 entries）；
   `BuildConfig.VERSION_NAME` 需本计划 Step 3.5 先开启 `buildConfig = true`
   （AGP 9 app 模块默认不生成，已验证：`generateDebugBuildConfig` 执行后编译通过）。
3. Type consistency — `BottomTab(id,label)` 两处一致；`MeEvent.Unfavorite(id)`；
   `ThemeMode.{SYSTEM,LIGHT,DARK}+resolveDark`；`themeStore.mode: Flow<ThemeMode>`；
   `setThemeMode(ThemeMode)`；`EditorialBottomBar(tabs,selectedId,onSelect)`；
   `SettingRow(label,value,onClick)`；`MeScreen(vm,onExplore)`；`mode.label()` 私有仅 MeScreen 用。
