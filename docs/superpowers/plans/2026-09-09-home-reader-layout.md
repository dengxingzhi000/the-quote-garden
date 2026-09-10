# Home Reader Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Home 改為閱讀器式固定版式——頂部固定、中央引文區佔滿剩餘空間並成為橫滑熱區，整頁不再滾動。

**Architecture:** `HomeScreen.kt` 單檔重構：`LazyColumn` → `Column`；header（問候/chips/標題/Loading）直接放 Column；引文區改為 `Box(weight(1f), contentAlignment=Center)` 掛橫滑 `pointerInput`，內層 `Column(verticalScroll)` 處理長句；刪除 footnote item；Scaffold bottomBar、ViewModel、動畫、过渡一律不動。

**Tech Stack:** Kotlin, Jetpack Compose (Column, Box weight, verticalScroll, detectHorizontalDragGestures), existing Hilt ViewModel (untouched).

**Spec:** `docs/superpowers/specs/2026-09-09-home-reader-layout-design.md`

**JAVA_HOME note:** All Gradle commands need `$env:JAVA_HOME = "C:/Users/Deng/.jdks/temurin-21"` first.

---

### Task 1: HomeScreen 重構為閱讀器版式

**Files:**
- Modify: `app/src/main/java/com/quotegarden/feature/home/HomeScreen.kt` (imports + Scaffold content body; CategoryChips/ChipLabel 不動)

- [ ] **Step 1: 改 imports（刪 LazyColumn，加 scroll）**

Replace:

```kotlin
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
```

with:

```kotlin
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
```

(`LazyRow` + `items` 留給 CategoryChips 用；`Arrangement` 留給 LazyRow 的 spacedBy；其餘 imports 全部仍有用，不動。)

- [ ] **Step 2: 替換 Scaffold content（LazyColumn → Column + 固定中央區）**

Replace the entire block from:

```kotlin
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(64.dp))
```

through the matching closing of the LazyColumn (the footnote `item` block ending with):

```kotlin
            item {
                AnimatedVisibility(visible = !state.isBrowsing) {
                    Column {
                        Spacer(modifier = Modifier.height(48.dp))
                        Text(
                            text = "Offline-first - cached first, syncs quietly",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Spacer(modifier = Modifier.height(96.dp))
                    }
                }
            }
        }
    }
```

with:

```kotlin
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            AnimatedContent(
                targetState = state.isBrowsing,
                transitionSpec = {
                    (fadeIn(tween(200)) togetherWith fadeOut(tween(200)))
                },
                label = "topBar"
            ) { browsing ->
                if (browsing) {
                    Spacer(modifier = Modifier.height(48.dp))
                } else {
                    EditorialTopBar(
                        date = date,
                        greeting = greeting
                    )
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
            CategoryChips(
                selected = state.selectedCategory,
                available = state.availableCategories,
                onPick = { viewModel.onEvent(HomeEvent.SelectCategory(it)) },
                onMore = { /* future: show full sheet */ }
            )
            Spacer(modifier = Modifier.height(24.dp))
            AnimatedVisibility(visible = state.quote != null && !state.isBrowsing) {
                Column {
                    Text(
                        text = "TODAY'S QUOTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            AnimatedVisibility(visible = state.isLoading) {
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(state.quote?.id) {
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
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    AnimatedContent(
                        targetState = state.quote?.id ?: state.error ?: "empty",
                        transitionSpec = {
                            (fadeIn(tween(200)) +
                                    slideInHorizontally(tween(200)) { it / 12 }) togetherWith
                                (fadeOut(tween(200)) +
                                        slideOutHorizontally(tween(200)) { -it / 12 })
                        },
                        label = "quoteContent"
                    ) { key ->
                        val quote = state.quote
                        val error = state.error
                        when {
                            quote != null && key == quote.id -> {
                                QuoteBlock(quote = quote)
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                            error != null && key == error -> {
                                EditorialError(
                                    message = error,
                                    onRetry = { viewModel.onEvent(HomeEvent.Load) }
                                )
                            }
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
                        }
                    }
                }
            }
        }
    }
```

Key points preserved verbatim from the old code: all strings, transitionSpecs, swipe threshold logic, empty/error branches. Changed only: `LazyColumn`/`item {}` wrappers removed, swipe `pointerInput` moved from `AnimatedContent` to the outer weight-1f `Box` (whole zone is the hotspot), inner `verticalScroll` column added, footnote item deleted.

- [ ] **Step 3: 編譯確認**

Run: `server/gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL. If the compiler reports an unused import (e.g. `Arrangement` if LazyRow usage changes — it doesn't), remove only the flagged line and re-run.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/quotegarden/feature/home/HomeScreen.kt
git commit -m "feat(home): reader layout with full-zone swipe hotspot"
```

---

### Task 2: 迴歸驗證（邏輯零改動確認）

**Files:** none modified (verification only)

- [ ] **Step 1: 跑 HomeViewModel focused tests**

Run: `server/gradlew.bat :app:testDebugUnitTest --tests "com.quotegarden.feature.home.HomeViewModelTest"`
Expected: BUILD SUCCESSFUL, 9 tests pass (this task touches no ViewModel logic; any failure means an accidental edit — inspect `git diff` before proceeding)

- [ ] **Step 2: 全量迴歸**

Run: `server/gradlew.bat :app:test`
Expected: same baseline as 2026-09-09 — all pass except the 2 known pre-existing `BrowseViewModelTest` failures (WhileSubscribed without subscriber, unrelated, user-accepted). No new failures.

---

## Manual verification checklist (實機，用戶執行)

- 短引文垂直居中，無多餘空白堆積頂部。
- 長引文：中央區內可滾到底（作者行可見）；頁面整體不跟著滾。
- 中央區上/中/下三處起滑：左滑下一條、右滑回上一條皆有效；棧底右滑無反應不報錯。
- 360dp + 1.3x 字體：不斷行、不遮擋；旋轉自適應。
