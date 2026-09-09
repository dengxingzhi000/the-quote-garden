# Home Reader Layout Design

**Date:** 2026-09-09
**Status:** Draft — pending user review
**Motivation:** 橫滑只在引文文字小區生效、短內容無垂直可滾範圍，用戶感知「滑不動」。改為閱讀器式固定版式，中央引文區即最大手勢熱區。

## 1. Goal

Home 改為整頁不滾動的閱讀器版式：頂部固定、中央引文區固定佔滿剩餘空間、底部操作欄吸底。左右滑在整個中央區任意位置起滑都有效。

## 2. Non-goals

- ViewModel / history 棧 / Prev-Next 邏輯不動（已有單測）。
- 底部 `HomeActionsBar`（Save/Next）不動。
- 引文排版（`QuoteBlock` 分段、作者斜體）不動。
- 不引入分頁指示器、小圓點等新 chrome。

## 3. Layout

```
Scaffold(bottomBar = HomeActionsBar) { padding ->
  Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
    Header (fixed, 原 LazyColumn 第 1~3 個 item 照搬：64dp+topBar+48dp / chips+24dp /
            TODAY'S QUOTE + Loading 行)
    QuoteZone (Modifier.weight(1f).fillMaxWidth() + 橫滑 pointerInput)
      Box(contentAlignment = Center)
        Column(Modifier.verticalScroll(rememberScrollState())) {
          AnimatedContent(quote/error/empty, 橫向過渡保持) { QuoteBlock / EditorialError / EditorialEmpty }
        }
  }
}
```

- 短句：內容不足一屏 → 在中央區垂直居中。
- 長句：超出中央區 → 區內垂直滾動（`verticalScroll`），頁面整體仍不滾。
- 橫滑 `detectHorizontalDragGestures`（80dp 閾值，左 Next / 右 Prev）掛在中央區 `Box` 上，與內部垂直滾動方向正交、互不干擾。
- footnote（"Offline-first…"行 + 96dp spacer）刪除，保持閱讀器乾淨。
- Loading 行保留在 header 區（與改前一致，即時反饋）；Error / Empty 在中央區居中顯示。

## 4. Gesture / scroll rules

- 中央區任意點起滑：水平位移 ≥80dp 觸發切換；垂直拖動走內部滾動（僅長句有內容可滾）。
- 頁面級 `LazyColumn` 移除（無需整頁滾）；`CategoryChips` 內部 `LazyRow` 橫滾保留。
- 動畫：保持現有橫向 slide + fade 過渡（不做方向感知，Next/Prev 共用同一套）。

## 5. Testing

- 無新增可單測邏輯（純版式）；既有 `HomeViewModelTest` 9 tests 必須全過。
- `assembleDebug` 通過；無新增 warning（unused imports 清理）。
- 手動（實機）：短句居中；長句區內可滾且頁面不跟著滾；中央區任意位置左滑下一條、右滑回上一條；棧底右滑無反應不報錯；小屏 360dp + 1.3x 字體不斷行。

## 6. Manual verification

- 短引文垂直居中，無多餘空白堆積頂部。
- 長引文：區內滾動到底可見作者行；鬆手無回彈異常。
- 橫滑成功率：中央區上/中/下三處起滑皆有效。
- 旋轉/折疊屏：weight 自適應，無重疊遮擋。
