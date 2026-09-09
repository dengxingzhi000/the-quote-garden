# Home Actions Bar + Swipe — Design Spec

**Date:** 2026-09-09
**Status:** Approved (brainstorming complete, awaiting spec review)
**Scope:** Home Save/Next 吸底固定 + 等宽双胶囊间距 + 左右滑动切换（左下右上）。
**Direction:** A — Scaffold bottomBar + 横滑手势 + 历史栈。

## 1. Problem

1. Save + Next 放在 `HomeScreen.kt:168-190` 的 `LazyColumn item` 里，随滚动滑走，用户读长 quote 时够不到操作。
2. Save 是 `OutlineTextAction` 胶囊（min 48dp、1dp边框、28dp圆角），Next 是 `TextAction` 纯文字（仅 vertical 8dp padding），同为 `weight(1f)` 但视觉重量失衡；`+ Save` / `Saved ✓` 切换时宽度抖动。
3. 切换只有点 `Next →` 一条路，无左右滑动，不符合阅读类 App 常规。

## 2. Goals

1. Save + Next 吸底常驻，滚动时始终可见，不遮挡 footnote。
2. 双按钮等宽对称：同款胶囊、同高 48dp、间距 12dp、文字居中，状态切换无抖动。
3. 市面常规手势：左滑下一条、右滑上一条（历史栈回退），离线可用，动画符合 editorial 规范（tween 200ms、位移 ≤8dp、无阴影无图标）。
4. 不碰数据层/网络/同步：复用 `getLocalRandomQuote`，只加 VM 内存历史栈。

## 3. Non-goals

- 预加载前后页的跟手 Pager（overkill，见备选 C）。
- 图标、手势提示动画、震动反馈、撤销 Snackbar。
- Favorite 列表、BottomNav、主题 token 改动。
- DAO / Retrofit / Worker / 导航改动。

## 4. Architecture

### 4.1 HomeUiState (`feature/home/HomeUiState.kt`)

```kotlin
data class HomeUiState(
    // ... existing fields unchanged ...
    val history: List<Quote> = emptyList(), // capped at 20, oldest dropped
    val historyIndex: Int = -1,             // -1 = empty, else valid index into history
)
sealed interface HomeEvent {
    // ... Load / Favorite / SelectCategory unchanged ...
    data object NextRandom : HomeEvent
    data object Prev : HomeEvent            // NEW: history back, no DAO call
}
```

### 4.2 HomeViewModel (`feature/home/HomeViewModel.kt`)

- `Load` 成功后：`history = listOf(q)`，`historyIndex = 0`（重置浏览位）。
- `NextRandom`：`repo.getLocalRandomQuote(excludeId, category)` → `history = (history.take(index+1) + q).takeLast(20)`，`historyIndex = history.lastIndex`。
- `Prev`：若 `historyIndex > 0` 则 `historyIndex--` 且 `quote = history[historyIndex]`；栈底则 no-op（UI 层回弹，不报错）。
- `isBrowsing` 语义不变：Load=false，Next/Prev=true。
- `isCurrentQuoteFavorite` 仍由 `observeFavoriteIds` 派生，切换 quote 时重算。

### 4.3 HomeScreen (`feature/home/HomeScreen.kt`)

- `Scaffold(bottomBar = { HomeActionsBar(...) })`，`containerColor = background`。
- 删除原 `LazyColumn item` action row（L168-190）；footnote 段上方加 `Spacer(96dp)` 防被 bottomBar 遮挡。
- Quote 内容区（现有 `AnimatedContent quoteContent`）外包横滑：`Modifier.swipeToNavigate(onNext, onPrev)`（`detectHorizontalDragGestures`，阈值 80px 或 fling 800px/s），`transitionSpec` 由纵向 `slideInVertically/slideOutVertically` 改为横向 `slideInHorizontally/slideOutHorizontally`（幅度 `it/12`，≤8dp 等效），时长保持 `tween(200)` + fade。
- 左滑 → `HomeEvent.NextRandom`，右滑 → `HomeEvent.Prev`。

### 4.4 Editorial components (`core/designsystem/EditorialComponents.kt`)

- 新增 `HomeActionsBar(saveLabel, isSaved, favoriteTapKey, onSave, onNext)`：
```kotlin
Surface(shadowElevation = 0.dp, tonalElevation = 0.dp, color = MaterialTheme.colorScheme.background) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlineTextAction(label, onSave, Modifier.weight(1f), isOn, pulseKey)
            OutlineTextAction("Next →", onNext, Modifier.weight(1f))
        }
    }
}
```
- Next 从 `TextAction` 换成 `OutlineTextAction` 同款（常亮 outline，不传 `isOn`），保证等宽对称；`TextAction` 本体不动（别处仍用）。
- 固定 `heightIn(min = 48dp)` + `contentAlignment = Center`，`+ Save` / `Saved ✓` 一字符差在 `weight(1f)` 内无抖动。

## 5. UI behavior

```
[TODAY'S QUOTE / QuoteBlock]  ← 左滑 Next，右滑 Prev（栈底回弹）
────────────────────────────────
[ + Save / Saved ✓ ]  [ Next → ]   ← 吸底常驻，双胶囊等宽，12dp间距
Offline-first · cached first
```

- 空/错态：`EditorialEmpty` / `EditorialError` 不变；空库 Next 仍报 `No other quotes yet`；Prev 永不报错。
- 明暗主题：沿用 `primary`/`outlineVariant`，无新 token。
- 无障碍：按钮保留 `Role.Button`（`OutlineTextAction` 已有 clickable 语义），滑动仅为快捷方式。

## 6. Tests

- `HomeViewModelTest` 新增：
  - Next 会 push 历史且 index 指向末尾。
  - Prev 回退且不调用 `getLocalRandomQuote`。
  - 栈底 Prev 为 no-op（quote 不变、无 error）。
  - 历史超 20 丢头。
  - Load 重置历史为单元素。
- 现有 `NextRandom` 单测沿用 `getLocalRandomQuote` stub，无需改网络 stub。
- 手动：360dp/410dp、明暗、1.0x/1.3x 字体、飞行模式滑动、吸底栏不遮 footnote、左滑连击无崩。

## 7. Boundaries

**Modify:** `feature/home/HomeScreen.kt`、`feature/home/HomeViewModel.kt`、`feature/home/HomeUiState.kt`、`core/designsystem/EditorialComponents.kt`、测试 `HomeViewModelTest.kt`。
**Do not modify:** DAO / Repository / 网络 / `DailySyncWorker` / 导航 / Theme / token 文件。

## 8. Risks & mitigations

| Risk | Mitigation |
|---|---|
| 横滑与 LazyColumn 纵滑冲突 | 只认横向 drag（`detectHorizontalDragGestures` 天然过滤纵滑）；阈值 80px 防误触 |
| 连击 Next 竞态 | VM 内 `viewModelScope.launch` 串行读 `_uiState.value`，与现有实现一致 |
| 历史栈内存 | 上限 20 个 Quote，内存可忽略 |

## 9. Self-review

- Placeholders: none — 文件路径、阈值、动画参数、历史上限全部具体。
- Consistency: bottomBar + 12dp + 双 OutlineTextAction 全文一致；Prev 无 DAO 调用与 §4.2 一致；动画 tween(200) 与 editorial 规范一致。
- Scope: 单一 Home 交互改进，不含数据层/导航，未膨胀。
- Ambiguity resolved: “固定”= 吸底 + 防抖两者；“间距”= 12dp 等宽双胶囊；“常规”= 左下右上带历史回退。
