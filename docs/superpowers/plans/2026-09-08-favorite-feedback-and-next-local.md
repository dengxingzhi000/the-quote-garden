# Favorite Feedback + Next Local Random Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Make Home favorite button feel responsive (state-aware label + brief underline pulse, tap-toggle on/off), make Home `Next →` resolve instantly from local Room data, and make FavoriteScreen rows removable via swipe-left.

**Architecture:** Add one DAO query (`getRandomExcluding`), two repository methods (`getLocalRandomQuote`, `observeFavoriteIds`), one HomeViewModel combinator (favorite ids + state). Extend two design-system composables (`OutlineTextAction` gains `isOn`/`pulseKey`; `IndexRow` gains `onSwipeOut`). New `FavoriteEvent.Unfavorite`. Background `DailySyncWorker` already keeps Room fresh.

**Tech Stack:** Kotlin + Compose BOM 2025.10.01 + Material3 `SwipeToDismissBox` + Room 2.8.4 + Hilt 2.60.1 + JUnit4 + MockK + kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-09-08-dailymind-favorite-feedback-and-next-local-design.md`

---

## File Structure

```
app/src/main/java/com/dailymind/core/database/dao/
  QuoteDao.kt                              # Modify: 加 getRandomExcluding 查询
app/src/main/java/com/dailymind/core/data/
  QuoteRepository.kt                       # Modify: 加 getLocalRandomQuote + observeFavoriteIds
  QuoteRepositoryImpl.kt                   # Modify: 实现新方法
app/src/main/java/com/dailymind/feature/home/
  HomeUiState.kt                           # Modify: 加 isCurrentQuoteFavorite + favoriteTapKey
  HomeViewModel.kt                         # Modify: combine + NextRandom 走本地
  HomeScreen.kt                            # Modify: label 切换 + 传 isOn/pulseKey
app/src/main/java/com/dailymind/feature/favorite/
  FavoriteViewModel.kt                     # Modify: 加 Unfavorite 事件
  FavoriteScreen.kt                        # Modify: rows 接入 onSwipeOut
app/src/main/java/com/dailymind/core/designsystem/
  EditorialComponents.kt                   # Modify: OutlineTextAction 加 isOn/pulseKey，IndexRow 加 onSwipeOut
app/src/test/java/com/dailymind/core/data/
  QuoteRepositoryTest.kt                   # Modify: 新增 3 个本地随机 / favoriteIds 测试
app/src/test/java/com/dailymind/feature/home/
  HomeViewModelTest.kt                     # Modify: 适配新 API + 新断言
app/src/test/java/com/dailymind/feature/favorite/
  FavoriteViewModelTest.kt                 # Modify: 新增 Unfavorite 测试
```

---

### Task 1: QuoteDao 新增 `getRandomExcluding`

**Files:**
- Modify: `app/src/main/java/com/dailymind/core/database/dao/QuoteDao.kt`

- [x] **Step 1: 加查询（保持现有接口不变）**

打开 `app/src/main/java/com/dailymind/core/database/dao/QuoteDao.kt`，在 `getRandom()` 之后追加：

```kotlin
    @Query("SELECT * FROM quote WHERE deletedAt IS NULL AND id != :excludeId ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomExcluding(excludeId: String): QuoteEntity?
```

- [x] **Step 2: 编译**

Run: `cd app/.. && server/gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL（仅接口扩展，无调用方，先不破窗）

- [x] **Step 3: Commit**

```bash
git add app/src/main/java/com/dailymind/core/database/dao/QuoteDao.kt
git commit -m "feat(dao): add getRandomExcluding query for Next excluding current"
```

---

### Task 2: Repository 加 getLocalRandomQuote + observeFavoriteIds（TDD 红绿）

**Files:**
- Modify: `app/src/main/java/com/dailymind/core/data/QuoteRepository.kt`
- Modify: `app/src/main/java/com/dailymind/core/data/QuoteRepositoryImpl.kt`
- Modify: `app/src/test/java/com/dailymind/core/data/QuoteRepositoryTest.kt`

- [x] **Step 1: 写失败的测试（红）**

打开 `app/src/test/java/com/dailymind/core/data/QuoteRepositoryTest.kt`，在最后一个 `@Test` 之后追加：

```kotlin
    @Test fun `getLocalRandomQuote excludes specified id`() = runTest {
        val dao = mockk<QuoteDao>()
        val q1 = QuoteEntity("1", "A", "甲", "X", "c", 1, null, null, 1L, null)
        val q2 = QuoteEntity("2", "B", "乙", "Y", "c", 1, null, null, 2L, null)
        coEvery { dao.getRandomExcluding("1") } returns q2
        val q = repo(dao = dao).getLocalRandomQuote(excludeId = "1")!!
        assertEquals("2", q.id)
        coVerify { dao.getRandomExcluding("1") }
    }

    @Test fun `getLocalRandomQuote returns null on empty db`() = runTest {
        val dao = mockk<QuoteDao>()
        coEvery { dao.getRandomExcluding("any") } returns null
        assertNull(repo(dao = dao).getLocalRandomQuote(excludeId = "any"))
    }

    @Test fun `getLocalRandomQuote calls getRandom when excludeId null`() = runTest {
        val dao = mockk<QuoteDao>()
        coEvery { dao.getRandom() } returns entity
        val q = repo(dao = dao).getLocalRandomQuote(excludeId = null)!!
        assertEquals("9", q.id)
        coVerify(exactly = 0) { dao.getRandomExcluding(any()) }
    }

    @Test fun `observeFavoriteIds emits id set from favorites flow`() = runTest {
        val favorites = mockk<FavoriteDao>()
        coEvery { favorites.observeFavorites() } returns flowOf(
            listOf(entity, entity.copy(id = "8", content = "Other"))
        )
        val ids = repo(favorites = favorites).observeFavoriteIds().first()
        assertEquals(setOf("9", "8"), ids)
    }
```

需要的额外 import（文件顶部）：

```kotlin
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertNull
```

- [x] **Step 2: 运行测试，确认 4 个新测试都 FAIL**

Run: `cd app/.. && server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.core.data.QuoteRepositoryTest"`
Expected: 4 failures with "Unresolved reference: getLocalRandomQuote" / "Unresolved reference: observeFavoriteIds"

- [x] **Step 3: 在接口上加方法**

打开 `app/src/main/java/com/dailymind/core/data/QuoteRepository.kt`，在 `getRandomQuote(): Quote` 之后追加：

```kotlin
    suspend fun getLocalRandomQuote(excludeId: String?): Quote?
    fun observeFavoriteIds(): Flow<Set<String>>
```

- [x] **Step 4: 在 impl 里实现**

打开 `app/src/main/java/com/dailymind/core/data/QuoteRepositoryImpl.kt`：

文件顶部追加 import：
```kotlin
import kotlinx.coroutines.flow.map
```

`override fun getRandomQuote(): Quote = ...` 块**之前**插入：

```kotlin
    override suspend fun getLocalRandomQuote(excludeId: String?): Quote? =
        if (excludeId == null) dao.getRandom()?.toModel()
        else dao.getRandomExcluding(excludeId)?.toModel()

    override fun observeFavoriteIds(): Flow<Set<String>> =
        favorites.observeFavorites().map { list -> list.map { it.id }.toSet() }
```

- [x] **Step 5: 运行测试，4 个新测试 PASS**

Run: `cd app/.. && server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.core.data.QuoteRepositoryTest"`
Expected: PASS（原有 11 个 + 4 个新 = 15 个测试全绿）

- [x] **Step 6: Commit**

```bash
git add app/src/main/java/com/dailymind/core/data/QuoteRepository.kt \
        app/src/main/java/com/dailymind/core/data/QuoteRepositoryImpl.kt \
        app/src/test/java/com/dailymind/core/data/QuoteRepositoryTest.kt
git commit -m "feat(repo): getLocalRandomQuote excludes id, observeFavoriteIds emits id set"
```

---

### Task 3: HomeViewModel — NextRandom 走本地 + 收藏状态观测（TDD）

**Files:**
- Modify: `app/src/main/java/com/dailymind/feature/home/HomeUiState.kt`
- Modify: `app/src/main/java/com/dailymind/feature/home/HomeViewModel.kt`
- Modify: `app/src/test/java/com/dailymind/feature/home/HomeViewModelTest.kt`

- [x] **Step 1: 扩 HomeUiState**

整个文件替换为：

```kotlin
package com.dailymind.feature.home

import com.dailymind.core.model.Quote

data class HomeUiState(
    val quote: Quote? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBrowsing: Boolean = false,
    val isCurrentQuoteFavorite: Boolean = false,
    val favoriteTapKey: Int = 0,
)

sealed interface HomeEvent {
    data object Load : HomeEvent
    data object NextRandom : HomeEvent
    data class Favorite(val id: String) : HomeEvent
}
```

- [x] **Step 2: 写失败测试**

打开 `app/src/test/java/com/dailymind/feature/home/HomeViewModelTest.kt`，整个文件替换为：

```kotlin
package com.dailymind.feature.home

import com.dailymind.MainDispatcherRule
import com.dailymind.core.data.QuoteRepository
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

    @Test fun `initial state loads daily quote`() = runTest {
        val repo = mockk<QuoteRepository>()
        coEvery { repo.getDailyQuote() } returns quote("1")
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        val vm = HomeViewModel(repo)
        vm.onEvent(HomeEvent.Load)
        assertEquals("Q-1", vm.uiState.value.quote?.content)
    }

    @Test fun `NextRandom picks local random excluding current quote`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        coEvery { repo.getLocalRandomQuote(excludeId = "1") } returns quote("2", "Q-2")
        val vm = HomeViewModel(repo)
        vm.uiState.value = vm.uiState.value.copy(quote = quote("1"))
        vm.onEvent(HomeEvent.NextRandom)
        assertEquals("Q-2", vm.uiState.value.quote?.content)
        assertEquals(true, vm.uiState.value.isBrowsing)
        coVerify { repo.getLocalRandomQuote(excludeId = "1") }
    }

    @Test fun `NextRandom surfaces error when local random returns null`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        coEvery { repo.getLocalRandomQuote(excludeId = null) } returns null
        val vm = HomeViewModel(repo)
        vm.onEvent(HomeEvent.NextRandom)
        assertNotNull(vm.uiState.value.error)
    }

    @Test fun `Favorite increments favoriteTapKey`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        val vm = HomeViewModel(repo)
        val before = vm.uiState.value.favoriteTapKey
        vm.onEvent(HomeEvent.Favorite("1"))
        assertEquals(before + 1, vm.uiState.value.favoriteTapKey)
    }

    @Test fun `isCurrentQuoteFavorite reflects observeFavoriteIds`() = runTest {
        val repo = mockk<QuoteRepository>()
        coEvery { repo.getDailyQuote() } returns quote("1")
        val favIds = MutableStateFlow(setOf("1"))
        every { repo.observeFavoriteIds() } returns favIds
        val vm = HomeViewModel(repo)
        vm.onEvent(HomeEvent.Load)
        assertEquals(true, vm.uiState.value.isCurrentQuoteFavorite)
    }
}
```

- [x] **Step 3: 运行测试，确认至少 NextRandom 类 2 个 FAIL（impl 还没换）**

Run: `cd app/.. && server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.home.HomeViewModelTest"`
Expected: 失败信息含 "Unresolved reference: getLocalRandomQuote" 或方法签名不匹配

- [x] **Step 4: 改 HomeViewModel**

整个文件替换为：

```kotlin
package com.dailymind.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailymind.core.data.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: QuoteRepository
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
        onEvent(HomeEvent.Load)
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Load -> viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                try {
                    val q = repo.getDailyQuote() ?: repo.getRandomQuote()
                    _uiState.value = _uiState.value.copy(quote = q, isLoading = false, isBrowsing = false)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
            }
            is HomeEvent.NextRandom -> viewModelScope.launch {
                try {
                    val current = _uiState.value.quote?.id
                    val q = repo.getLocalRandomQuote(excludeId = current)
                        ?: throw IllegalStateException("No other quotes yet")
                    _uiState.value = _uiState.value.copy(quote = q, error = null, isBrowsing = true)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            }
            is HomeEvent.Favorite -> viewModelScope.launch {
                repo.toggleFavorite(event.id)
                _uiState.value = _uiState.value.copy(favoriteTapKey = _uiState.value.favoriteTapKey + 1)
            }
        }
    }
}
```

- [x] **Step 5: 运行测试，全部绿**

Run: `cd app/.. && server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.home.HomeViewModelTest"`
Expected: PASS（5/5）

- [x] **Step 6: Commit**

```bash
git add app/src/main/java/com/dailymind/feature/home/HomeUiState.kt \
        app/src/main/java/com/dailymind/feature/home/HomeViewModel.kt \
        app/src/test/java/com/dailymind/feature/home/HomeViewModelTest.kt
git commit -m "feat(home): NextRandom uses local random; observe favorite state"
```

---

### Task 4: OutlineTextAction 加 isOn + pulseKey（带下划线脉冲）

**Files:**
- Modify: `app/src/main/java/com/dailymind/core/designsystem/EditorialComponents.kt`

- [x] **Step 1: 替换 OutlineTextAction**

打开 `app/src/main/java/com/dailymind/core/designsystem/EditorialComponents.kt`，将 `OutlineTextAction` 整个替换为：

```kotlin
@Composable
fun OutlineTextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOn: Boolean = false,
    pulseKey: Any = Unit,
) {
    val border = if (isOn) BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                 else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    val pulse = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(pulseKey) {
        if (pulseKey != Unit) {
            pulse.animateTo(1f, androidx.compose.animation.core.tween(200))
            pulse.animateTo(0f, androidx.compose.animation.core.tween(200))
        }
    }
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        border = border,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(2.dp))
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(1.dp)
                    .androidx.compose.ui.draw.alpha(pulse.value)
                    .androidx.compose.foundation.background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
```

文件顶部 import 区追加：
```kotlin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
```

- [x] **Step 2: 编译（注意 `androidx.compose.ui.draw.alpha` 与 `androidx.compose.foundation.background` 等链式需要正确顺序，alpha 在 background 之后才生效）**

Run: `cd app/.. && server/gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

如果出现"qualified reference alpha not allowed"之类的链式问题，调整为：

```kotlin
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .alpha(pulse.value)
            )
```

（注意：此时文件顶部 import 不需要再分别导入 `background` 和 `alpha`，因为上面 import 块已经引入；这里链式是 inline 的 lambda 调用 `Modifier.x`，注意顺序是 `fillMaxWidth → height → background → alpha`）

- [x] **Step 3: Commit**

```bash
git add app/src/main/java/com/dailymind/core/designsystem/EditorialComponents.kt
git commit -m "feat(designsystem): OutlineTextAction supports isOn + underline pulse"
```

---

### Task 5: IndexRow 加 onSwipeOut（Material3 SwipeToDismissBox）

**Files:**
- Modify: `app/src/main/java/com/dailymind/core/designsystem/EditorialComponents.kt`

- [x] **Step 1: 替换 IndexRow**

打开 `EditorialComponents.kt`，将 `IndexRow` 整个替换为：

```kotlin
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun IndexRow(
    number: String,
    content: String,
    author: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeOut: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) {
                onSwipeOut?.invoke()
                true
            } else false
        },
        positionalThreshold = { totalWidth -> totalWidth * 0.5f }
    )
    androidx.compose.material3.SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = onSwipeOut != null,
        backgroundContent = {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .androidx.compose.foundation.background(MaterialTheme.colorScheme.outlineVariant),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Remove",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 24.dp)
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onClick, role = Role.Button, interactionSource = interaction, indication = null)
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    author?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "— $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
}
```

顶部 import 块追加：
```kotlin
import androidx.compose.foundation.background
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
```

- [x] **Step 2: 编译**

Run: `cd app/.. && server/gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

如果出现"background qualified use not allowed"，把链式中的 `.androidx.compose.foundation.background(...)` 替换为 `.background(...)`（依赖顶部 import）。

- [x] **Step 3: Commit**

```bash
git add app/src/main/java/com/dailymind/core/designsystem/EditorialComponents.kt
git commit -m "feat(designsystem): IndexRow supports swipe-left to remove"
```

---

### Task 6: HomeScreen 接 isOn + pulseKey + label 切换

**Files:**
- Modify: `app/src/main/java/com/dailymind/feature/home/HomeScreen.kt`

- [x] **Step 1: 替换底部 action row**

打开 `app/src/main/java/com/dailymind/feature/home/HomeScreen.kt`，定位到 `// 底部 actions row` 块（line 150-170 附近，整个 `item { if (state.quote != null) ... }`），替换为：

```kotlin
            item {
                if (state.quote != null) {
                    val q = state.quote!!
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlineTextAction(
                            label = if (state.isCurrentQuoteFavorite) "Saved ✓" else "+ Save",
                            onClick = { viewModel.onEvent(HomeEvent.Favorite(q.id)) },
                            isOn = state.isCurrentQuoteFavorite,
                            pulseKey = state.favoriteTapKey,
                            modifier = Modifier.weight(1f)
                        )
                        TextAction(
                            label = "Next →",
                            onClick = { viewModel.onEvent(HomeEvent.NextRandom) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
```

- [x] **Step 2: 编译**

Run: `cd app/.. && server/gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [x] **Step 3: Commit**

```bash
git add app/src/main/java/com/dailymind/feature/home/HomeScreen.kt
git commit -m "feat(home): favorite button state-aware + underline pulse"
```

---

### Task 7: FavoriteViewModel 加 Unfavorite 事件（TDD）

**Files:**
- Modify: `app/src/main/java/com/dailymind/feature/favorite/FavoriteViewModel.kt`
- Modify: `app/src/test/java/com/dailymind/feature/favorite/FavoriteViewModelTest.kt`

- [x] **Step 1: 写失败测试**

打开 `app/src/test/java/com/dailymind/feature/favorite/FavoriteViewModelTest.kt`，追加：

```kotlin
    @Test fun `Unfavorite calls repo toggleFavorite`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavorites() } returns flowOf(emptyList())
        val vm = FavoriteViewModel(repo)
        vm.onEvent(FavoriteEvent.Unfavorite("42"))
        io.mockk.coVerify { repo.toggleFavorite("42") }
    }
```

文件顶部 import 追加：
```kotlin
import io.mockk.coVerify
```

- [x] **Step 2: 运行测试，确认 FAIL（FavoriteEvent 还没加）**

Run: `cd app/.. && server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.favorite.FavoriteViewModelTest"`
Expected: FAIL 含 "Unresolved reference: Unfavorite"

- [x] **Step 3: 改 FavoriteViewModel**

整个文件替换为：

```kotlin
package com.dailymind.feature.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.model.Quote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val repo: QuoteRepository
) : ViewModel() {
    val favorites: StateFlow<List<Quote>> = repo.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onEvent(event: FavoriteEvent) {
        when (event) {
            is FavoriteEvent.Unfavorite -> viewModelScope.launch { repo.toggleFavorite(event.id) }
        }
    }
}

sealed interface FavoriteEvent {
    data class Unfavorite(val id: String) : FavoriteEvent
}
```

- [x] **Step 4: 运行测试，PASS（2/2）**

Run: `cd app/.. && server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.feature.favorite.FavoriteViewModelTest"`
Expected: PASS

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/com/dailymind/feature/favorite/FavoriteViewModel.kt \
        app/src/test/java/com/dailymind/feature/favorite/FavoriteViewModelTest.kt
git commit -m "feat(favorite): Unfavorite event wired to repo.toggleFavorite"
```

---

### Task 8: FavoriteScreen rows 接入 onSwipeOut

**Files:**
- Modify: `app/src/main/java/com/dailymind/feature/favorite/FavoriteScreen.kt`

- [x] **Step 1: 替换 IndexRow 调用**

打开 `app/src/main/java/com/dailymind/feature/favorite/FavoriteScreen.kt`，定位到 `else { itemsIndexed(...) { index, q -> IndexRow(...) } }` 块（line 67-74），替换为：

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

- [x] **Step 2: 编译**

Run: `cd app/.. && server/gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [x] **Step 3: Commit**

```bash
git add app/src/main/java/com/dailymind/feature/favorite/FavoriteScreen.kt
git commit -m "feat(favorite): rows swipe-left to unfavorite"
```

---

### Task 9: 全量测试 + assembleDebug

- [x] **Step 1: 全量 unit test**

Run: `cd app/.. && server/gradlew.bat :app:test`
Expected: BUILD SUCCESSFUL（包含 `HomeViewModelTest` 5/5、`QuoteRepositoryTest` 15/15、`FavoriteViewModelTest` 2/2 + 其他既有测试）

- [x] **Step 2: assembleDebug**

Run: `cd app/.. && server/gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

如果仍有 spec 提到的 HomeScreen smart-cast 编译错误（见 AGENTS.md "working-tree state"），那与本任务无关，独立修复。

- [x] **Step 3: 手动验证清单**

按 spec §7.3 走一遍：
- 浅色 + 深色主题：`Saved ✓` 与 accent 边框可见
- 飞行模式：Next 仍能从本地取
- 空库：`Next →` 显示 `EditorialError`
- 慢网络：Home 首屏不再等 `/api/v1/quotes/random`
- FavoriteScreen 左滑 50% 触发 Remove
- 360dp / 410dp / 1.3x 字号：标签不截断，下划线脉冲可见

- [x] **Step 4: 最终 commit（如有 manifest 变更或 lint 修复）**

```bash
git status
# 如果有需要提交的，运行：
git add -A
git commit -m "chore: verified favorite feedback + local random + swipe remove"
```

---

## Self-review checklist

1. Spec coverage — each requirement mapped to a task:
    - §3.1 favorite state-aware label + pulse → Task 4 + Task 6
    - §3.1 favorite toggle on Home (clicking Saved ✓ removes) → Task 6 (no logic gate, the same `Favorite` event toggles)
    - §3.2 Next uses local random excluding current → Task 1 (query) + Task 2 (repo) + Task 3 (VM)
    - §3.2 background sync already in place → not a task (no change)
    - §3.3 FavoriteScreen swipe-left to remove → Task 5 (component) + Task 7 (VM event) + Task 8 (wire)
    - §3.3 text-only Remove background → Task 5
    - §3.4 Room empty edge → Task 3 step "NextRandom surfaces error"
    - §7 tests → Task 2/3/7 step 1 (TDD red) + step verify (green)

2. Placeholders — none. All steps include actual code or exact commands.

3. Type consistency — `getLocalRandomQuote(excludeId: String?)` matches interface, impl, ViewModel, and test signatures. `observeFavoriteIds(): Flow<Set<String>>` matches all. `FavoriteEvent.Unfavorite(id: String)` matches. `OutlineTextAction` parameter names `isOn`/`pulseKey` consistent. `IndexRow` parameter `onSwipeOut` consistent. `HomeUiState` field names `isCurrentQuoteFavorite`/`favoriteTapKey` consistent across state, VM, screen.