# Home Actions Bar + Swipe Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Home Save/Next 吸底固定、等宽双胶囊 12dp 间距、左滑 Next / 右滑 Prev（历史栈回退）。

**Architecture:** `HomeUiState` 加 `history/historyIndex` + `HomeEvent.Prev`；`HomeViewModel` 维护上限 20 的内存栈；`EditorialComponents` 新增 `HomeActionsBar`（双 `OutlineTextAction`）；`HomeScreen` 用 `Scaffold(bottomBar)` + `detectHorizontalDragGestures` 横滑，删除原 LazyColumn 内 action row。

**Tech Stack:** Kotlin, Jetpack Compose (Material3, Scaffold, detectHorizontalDragGestures), Hilt ViewModel, JUnit4 + MockK + kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-09-09-home-actions-bar-swipe-design.md`

---

### Task 1: HomeUiState 加 history + Prev 事件

**Files:**
- Modify: `app/src/main/java/com/quotegarden/feature/home/HomeUiState.kt`
- Test: `app/src/test/java/com/quotegarden/feature/home/HomeViewModelTest.kt` (仅编译确认，无新增断言)

- [ ] **Step 1: 修改 HomeUiState.kt**

将整个 data class 替换为（新增最后 2 个字段，新增 Prev 事件）：

```kotlin
package com.quotegarden.feature.home

import com.quotegarden.core.model.Quote

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
    val history: List<Quote> = emptyList(),
    val historyIndex: Int = -1,
)

sealed interface HomeEvent {
    data object Load : HomeEvent
    data object NextRandom : HomeEvent
    data object Prev : HomeEvent
    data class Favorite(val id: String) : HomeEvent
    data class SelectCategory(val category: String?) : HomeEvent
}
```

- [ ] **Step 2: 编译确认不断**

Run: `server/gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL（Prev 未处理只是新分支，旧逻辑不变）

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quotegarden/feature/home/HomeUiState.kt
git commit -m "feat(home): add history stack fields and Prev event"
```

---

### Task 2: HomeViewModel 历史栈（TDD）

**Files:**
- Modify: `app/src/main/java/com/quotegarden/feature/home/HomeViewModel.kt`
- Test: `app/src/test/java/com/quotegarden/feature/home/HomeViewModelTest.kt`

- [ ] **Step 1: 写 failing test（Prev 栈底 no-op + Next push 历史）**

在 `HomeViewModelTest.kt` 末尾追加（完整方法，直接粘贴）：

```kotlin
@Test fun `Prev on empty history is no-op`() = runTest {
    val repo = mockk<QuoteRepository>(relaxed = true)
    every { repo.observeFavoriteIds() } returns flowOf(emptySet())
    every { repo.observeCategoryCounts() } returns flowOf(emptyList())
    val vm = HomeViewModel(repo, categoryStore())
    vm.onEvent(HomeEvent.Prev)
    assertNull(vm.uiState.value.quote)
    assertNull(vm.uiState.value.error)
}

@Test fun `NextRandom pushes history and Prev goes back without DAO call`() = runTest {
    val repo = mockk<QuoteRepository>(relaxed = true)
    every { repo.observeFavoriteIds() } returns flowOf(emptySet())
    every { repo.observeCategoryCounts() } returns flowOf(emptyList())
    coEvery { repo.getLocalRandomQuote(excludeId = "1", category = null) } returns quote("2", "Q-2")
    val vm = HomeViewModel(repo, categoryStore())
    @Suppress("UNCHECKED_CAST")
    (vm.uiState as MutableStateFlow<HomeUiState>).value = vm.uiState.value.copy(
        quote = quote("1"),
        history = listOf(quote("1")),
        historyIndex = 0,
    )
    vm.onEvent(HomeEvent.NextRandom)
    assertEquals("Q-2", vm.uiState.value.quote?.content)
    assertEquals(1, vm.uiState.value.historyIndex)
    assertEquals(2, vm.uiState.value.history.size)
    vm.onEvent(HomeEvent.Prev)
    assertEquals("Q-1", vm.uiState.value.quote?.content)
    assertEquals(0, vm.uiState.value.historyIndex)
    coVerify(exactly = 1) { repo.getLocalRandomQuote(any(), any()) }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `server/gradlew.bat :app:testDebugUnitTest --tests "com.quotegarden.feature.home.HomeViewModelTest"`
Expected: FAIL — `Prev` 分支缺失导致第二个 test 的 Prev 断言失败（quote 仍为 Q-2）

- [ ] **Step 3: 最小实现（替换 HomeViewModel.kt 三处）**

(a) `Load` 成功分支：在现有 `_uiState.update { state -> ... }` 内，`quote = q` 处追加历史重置。找到：

```kotlin
val next = state.copy(
    quote = q,
    isLoading = false,
    isBrowsing = false,
    error = null,
    isCurrentQuoteFavorite = q?.id in currentFavIds,
)
```

替换为：

```kotlin
val next = state.copy(
    quote = q,
    isLoading = false,
    isBrowsing = false,
    error = null,
    isCurrentQuoteFavorite = q?.id in currentFavIds,
    history = if (q != null) listOf(q) else state.history,
    historyIndex = if (q != null) 0 else state.historyIndex,
)
```

(b) `NextRandom` 成功分支：找到：

```kotlin
_uiState.update { state ->
    state.copy(
        quote = q,
        error = null,
        isBrowsing = true,
        isCurrentQuoteFavorite = q.id in currentFavIds,
    )
}
```

替换为：

```kotlin
_uiState.update { state ->
    val base = state.history.take((state.historyIndex + 1).coerceAtLeast(0))
    val capped = (base + q).takeLast(20)
    state.copy(
        quote = q,
        error = null,
        isBrowsing = true,
        isCurrentQuoteFavorite = q.id in currentFavIds,
        history = capped,
        historyIndex = capped.lastIndex,
    )
}
```

(c) 新增 `Prev` 分支：在 `is HomeEvent.NextRandom -> ...` 块之后、`is HomeEvent.Favorite` 之前插入：

```kotlin
is HomeEvent.Prev -> {
    val current = _uiState.value
    if (current.historyIndex > 0 && current.history.isNotEmpty()) {
        val prevIndex = current.historyIndex - 1
        val prev = current.history.getOrNull(prevIndex)
        if (prev != null) {
            _uiState.update { state ->
                state.copy(
                    quote = prev,
                    error = null,
                    isBrowsing = true,
                    isCurrentQuoteFavorite = prev.id in currentFavIds,
                    historyIndex = prevIndex,
                )
            }
        }
    }
}
```

- [ ] **Step 4: 运行确认通过**

Run: `server/gradlew.bat :app:testDebugUnitTest --tests "com.quotegarden.feature.home.HomeViewModelTest"`
Expected: BUILD SUCCESSFUL，8 tests pass

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/quotegarden/feature/home/HomeViewModel.kt app/src/test/java/com/quotegarden/feature/home/HomeViewModelTest.kt
git commit -m "feat(home): history stack with Prev back navigation"
```

---

### Task 3: EditorialComponents 新增 HomeActionsBar

**Files:**
- Modify: `app/src/main/java/com/quotegarden/core/designsystem/EditorialComponents.kt`

- [ ] **Step 1: 追加 HomeActionsBar（文件末尾 SettingRow 之后粘贴）**

```kotlin
@Composable
fun HomeActionsBar(
    saveLabel: String,
    isSaved: Boolean,
    favoriteTapKey: Int,
    onSave: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Surface(
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlineTextAction(
                    label = saveLabel,
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    isOn = isSaved,
                    pulseKey = favoriteTapKey,
                )
                OutlineTextAction(
                    label = "Next →",
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
```

注意：`Arrangement`、`fillMaxWidth`、`navigationBarsPadding`、`padding`、`HorizontalDivider`、`MaterialTheme`、`Surface` 均已在该文件 import，无需新增 import。

- [ ] **Step 2: 编译确认**

Run: `server/gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/quotegarden/core/designsystem/EditorialComponents.kt
git commit -m "feat(designsystem): add HomeActionsBar equal-weight capsules"
```

---

### Task 4: HomeScreen 吸底 + 横滑（替换 action row）

**Files:**
- Modify: `app/src/main/java/com/quotegarden/feature/home/HomeScreen.kt`

- [ ] **Step 1: 改 imports（增 4 行）**

在现有 import 块追加：

```kotlin
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.quotegarden.core.designsystem.HomeActionsBar
```

删除不再使用的 2 行（编译若报错再删，先保留验证）：

```kotlin
// import com.quotegarden.core.designsystem.OutlineTextAction  // 待删除
// import com.quotegarden.core.designsystem.TextAction          // 待删除
```

实际操作：先不删，Step 3 后按编译器 unused-import 警告清理。

- [ ] **Step 2: Scaffold 加 bottomBar + 横滑 + 删旧 row**

(a) 找到：

```kotlin
Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
```

替换为：

```kotlin
val swipeThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
        if (state.quote != null) {
            val qb = state.quote
            HomeActionsBar(
                saveLabel = if (state.isCurrentQuoteFavorite) "Saved ✓" else "+ Save",
                isSaved = state.isCurrentQuoteFavorite,
                favoriteTapKey = state.favoriteTapKey,
                onSave = { qb?.let { viewModel.onEvent(HomeEvent.Favorite(it.id)) } },
                onNext = { viewModel.onEvent(HomeEvent.NextRandom) },
            )
        }
    }
) { padding ->
```

注意：`state.quote` 不做 smart-cast，用局部 `qb` + `?.let`（规避已知 smart-cast 编译失败）。

(b) 找到 quote `AnimatedContent` 块外层 `item { AnimatedContent(...) }`，在其 `modifier` 加横滑。找到：

```kotlin
item {
    AnimatedContent(
        targetState = state.quote?.id ?: state.error ?: "empty",
```

替换为：

```kotlin
item {
    AnimatedContent(
        targetState = state.quote?.id ?: state.error ?: "empty",
```

并在该 `item` 的 `Modifier` 链加 pointerInput — 更稳的做法是包一层 Box：找到该 item 块的：

```kotlin
) { key ->
    val quote = state.quote
    val error = state.error
```

在其所属 `AnimatedContent` 的 modifier 参数追加（AnimatedContent 支持 modifier 参数，当前调用没有传，需加上）：

```kotlin
AnimatedContent(
    targetState = state.quote?.id ?: state.error ?: "empty",
    modifier = Modifier.pointerInput(state.quote?.id) {
        var totalX = 0f
        detectHorizontalDragGestures(
            onDragEnd = {
                when {
                    totalX <= -swipeThresholdPx -> viewModel.onEvent(HomeEvent.NextRandom)
                    totalX >= swipeThresholdPx -> viewModel.onEvent(HomeEvent.Prev)
                }
                totalX = 0f
            }
        ) { _, dragAmount ->
            totalX += dragAmount
        }
    },
```

(c) 删除旧 action row 整块（原 L168-190）：

```kotlin
item {
    if (state.quote != null) {
        val q = state.quote!!
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlineTextAction(...)
            TextAction(...)
        }
    }
}
```

整块删除，替换为：

```kotlin
item {
    Spacer(modifier = Modifier.height(32.dp))
}
```

(d) footnote 防遮挡：找到：

```kotlin
AnimatedVisibility(visible = !state.isBrowsing) {
    Column {
        Spacer(modifier = Modifier.height(48.dp))
```

在该 Column 末尾（现有 `Spacer(modifier = Modifier.height(32.dp))` 之后）追加：

```kotlin
Spacer(modifier = Modifier.height(96.dp))
```

(e) 横滑动画改横向：找到：

```kotlin
(fadeIn(tween(200)) +
        slideInVertically(tween(200)) { it / 12 }) togetherWith
    (fadeOut(tween(200)) +
            slideOutVertically(tween(200)) { -it / 12 })
```

替换为：

```kotlin
(fadeIn(tween(200)) +
        slideInHorizontally(tween(200)) { it / 12 }) togetherWith
    (fadeOut(tween(200)) +
            slideOutHorizontally(tween(200)) { -it / 12 })
```

并将 import 中 `slideInVertically/slideOutVertically` 改为 `slideInHorizontally/slideOutHorizontally`。

- [ ] **Step 3: 编译并清理 unused imports**

Run: `server/gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL；若报 `OutlineTextAction`/`TextAction` unused，删除对应 import 行后再跑一次。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/quotegarden/feature/home/HomeScreen.kt
git commit -m "feat(home): sticky actions bar with horizontal swipe"
```

---

### Task 5: 补历史上限/Load重置单测 + 全量回归

**Files:**
- Modify: `app/src/test/java/com/quotegarden/feature/home/HomeViewModelTest.kt`

- [ ] **Step 1: 追加 2 个 tests**

```kotlin
@Test fun `history capped at 20 dropping oldest`() = runTest {
    val repo = mockk<QuoteRepository>(relaxed = true)
    every { repo.observeFavoriteIds() } returns flowOf(emptySet())
    every { repo.observeCategoryCounts() } returns flowOf(emptyList())
    coEvery { repo.getLocalRandomQuote(any(), any()) } answers {
        val exclude = firstArg<String?>()
        quote("n-$exclude", "Q-n-$exclude")
    }
    val vm = HomeViewModel(repo, categoryStore())
    @Suppress("UNCHECKED_CAST")
    (vm.uiState as MutableStateFlow<HomeUiState>).value = vm.uiState.value.copy(
        quote = quote("0"),
        history = listOf(quote("0")),
        historyIndex = 0,
    )
    repeat(25) { vm.onEvent(HomeEvent.NextRandom) }
    assertEquals(20, vm.uiState.value.history.size)
    assertEquals(19, vm.uiState.value.historyIndex)
}

@Test fun `Load resets history to single element`() = runTest {
    val repo = mockk<QuoteRepository>()
    coEvery { repo.getDailyQuote(null) } returns quote("9", "Q-9")
    every { repo.observeFavoriteIds() } returns flowOf(emptySet())
    every { repo.observeCategoryCounts() } returns flowOf(emptyList())
    val vm = HomeViewModel(repo, categoryStore())
    vm.onEvent(HomeEvent.Load)
    assertEquals(1, vm.uiState.value.history.size)
    assertEquals(0, vm.uiState.value.historyIndex)
    assertEquals("Q-9", vm.uiState.value.quote?.content)
}
```

- [ ] **Step 2: 运行 focused tests**

Run: `server/gradlew.bat :app:testDebugUnitTest --tests "com.quotegarden.feature.home.HomeViewModelTest"`
Expected: BUILD SUCCESSFUL，10 tests pass

- [ ] **Step 3: 全量回归**

Run: `server/gradlew.bat :app:test`
Expected: BUILD SUCCESSFUL（注意：不要给 `:app:test` 加 `--tests`，Gradle 会拒收）

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/com/quotegarden/feature/home/HomeViewModelTest.kt
git commit -m "test(home): history cap and load reset coverage"
```

---

## Manual verification checklist

- 明/暗主题：双胶囊可读，Saved ✓ primary 边框可见。
- 360dp + 410dp：双按钮等宽，12dp 间距，文字居中不断行。
- 1.3x 字体：`Saved ✓` / `+ Save` / `Next →` 不截断。
- 滚动：长 quote 上滑时吸底栏常驻，footnote 不被遮挡（96dp spacer）。
- 手势：左滑下一条、右滑上一条、栈底右滑回弹无报错、飞行模式可用。
- 空库：`Next →` 报 `No other quotes yet`，`Prev` 无操作。
