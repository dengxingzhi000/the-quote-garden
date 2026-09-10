# Browse Tile Full-Bleed Image — Design Spec

**Date:** 2026-09-10
**Status:** Approved (brainstorming complete, awaiting spec review)
**Scope:** Browse grid tile visual refresh — replace small corner icon with full-bleed category image + bottom scrim + bottom-aligned text.
**Direction:** A — Spotify-style image-first tile.

## 1. Problem

`BrowseScreen.kt:127-163` 的 `HeroCategoryTile` 现在是：

- 1:1 方格
- 左上角 `#01` 编号
- 右上角 56dp 小图标（`ContentScale.Fit`，整张图只在角落露 56dp 直径）
- 左下角标题 + 数量
- 1dp 边框 + 35% 黑填充背景

新做的 13 张分类插画（`drawable/` 下 1.2–1.9 MB）目前只露了一个角，看不出 editorial 调性。Browse 整页因此偏向"13 个深色方块"，跟 `2026-09-03` 的 quiet-luxury editorial spec 不贴合。

## 2. Goals

1. Browse tile 视觉以分类插画为主体（image first, text second），符合 editorial design spec 方向。
2. 标题/数量始终可读（在 scrim 上），无障碍对比度满足。
3. 改动仅限 `HeroCategoryTile`，不污染 `CategoryDetailScreen`、`HomeScreen::CategoryChips`、数据层、网络层。

## 3. Non-goals

- 不重画插画源文件。128×128 维持现状，`ContentScale.Crop` 会切到主体不在中心的图——这是已接受风险。
- 不动 `CategoryDetailScreen.kt` 头部 40dp 小图标。
- 不动 `HomeScreen.kt::CategoryChips`。
- 不加 Coil / 异步图片加载库。13 张 1–2 MB PNG 解码的内存压力比之前 56dp 小图高 4–9 倍；本次接受，不在 spec 内优化。
- 不改 `LazyVerticalGrid` 的 `columns = GridCells.Fixed(2)`、spacing、key。
- 不改 `categoryDrawable(...)` 的路由（`CategoryIcon.kt` 不动）。

## 4. Design

### 4.1 New `HeroCategoryTile` structure

```kotlin
@Composable
private fun HeroCategoryTile(
    item: CategoryCount,
    onClick: (String) -> Unit,        // 签名去掉 index: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick(item.category) }
    ) {
        Image(                                          // 满铺背景
            painter = painterResource(id = categoryDrawable(item.category)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(                                            // 渐变 scrim
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f    to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )
        Column(                                         // 文字叠底部
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = item.category,
                style = MaterialTheme.typography.headlineSmall,
                color = OnDark
            )
            Text(
                text = "${item.count} lines",
                style = MaterialTheme.typography.labelSmall,
                color = OnDarkMuted
            )
        }
    }
}
```

### 4.2 Changes vs current

| Element | Before | After |
|---|---|---|
| Image | 56dp fitted, top-end corner | full-bleed, `Crop`, behind scrim |
| Scrim | none | vertical gradient 55% transparent → 100% black @ 85% alpha |
| Background fill | `Color.Black.copy(alpha=0.35f)` | removed (scrim replaces) |
| Border | 1dp `OnDarkFaint` outline | removed (dark hero bg + scrim give separation) |
| `#01` index | `labelSmall` `OnDarkMuted`, top-start | removed |
| Title | `headlineSmall` `OnDark`, bottom-start, 16dp padding | unchanged |
| Count | `labelSmall` `OnDarkMuted`, bottom-start | unchanged |
| Click target | whole tile | unchanged |
| Aspect ratio | 1:1 | unchanged |

### 4.3 Call-site change

`BrowseScreen.kt:110-112` 从

```kotlin
items(count = state.tiles.size, key = { state.tiles[it].category }) { idx ->
    HeroCategoryTile(state.tiles[idx], idx, onSelectCategory)
}
```

改为

```kotlin
items(count = state.tiles.size, key = { state.tiles[it].category }) {
    HeroCategoryTile(state.tiles[it], onSelectCategory)
}
```

`idx` 不再需要传。

### 4.4 Tokens

复用文件顶部已有常量：`OnDark`（title）、`OnDarkMuted`（count）。`HeroScrim` 仍只用于外层 hero 渐变；tile 内的 scrim 用 `Color.Black.copy(alpha=0.85f)` 直写，不引入新颜色。Typography 用 `headlineSmall` + `labelSmall`，与改之前一致。

### 4.5 Imports

从 `BrowseScreen.kt` import 区删除：
- `androidx.compose.foundation.border`（不再用 1dp 边框）
- `androidx.compose.foundation.layout.size`（图标 .size(56.dp) 删了）

新增：
- 无（`Image`、`Box`、`Column`、`AspectRatio`、`Brush`、`ContentScale`、`clickable` 都已在文件里 import 过）

## 5. Testing

`BrowseScreenTest.kt` 现有 2 个测试不变：

- `renders category tiles and invokes onSelectCategory on tap`：用 `onNodeWithText(...)` 断言 category 名 + "X categories - Y lines" 头部。改完后 category 名仍存在（叠在 scrim 上的 `Text` 节点相同），头部不动 → 通过。
- `empty counts renders empty state`：empty 分支没碰 → 通过。

不新增测试：tile 的视觉表现（满铺图、scrim、对比度）属于 Compose 渲染层，单元测试 ROI 低；现有 UI 测试已覆盖"tile 存在 + 可点 + 点击回调"这条契约。

`BrowseViewModelTest.kt` 不动。

## 6. Risks

1. **Crop 切到主体** — 128×128 源图主体不在中心的会被裁。已接受（见 §3）。
2. **内存压力** — 13 张 1–2 MB PNG 解码到 GPU。`LazyVerticalGrid` 默认 viewport 内可见 4–6 张。如果实测掉帧，下一步考虑 Coil + `crossfade(true)`，但不在本次 spec。
3. **`.assertExists()` 通过但视觉不一样** — 现有测试只验证节点存在，无法捕捉 scrim 缺失等回归。可接受：本 spec 内视觉回归由人工 preview 兜底。

## 7. Rollout

1. 修改 `BrowseScreen.kt` 的 `HeroCategoryTile` 函数体 + call-site。
2. 跑 `:app:testDebugUnitTest` 全量，确认两个 `BrowseScreenTest` 通过、其余 91 个测试无回归。
3. 不发新版本 — 本次只是视觉调整，沿用现有 release 通道。
