# Browse Tile Full-Bleed Image Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Browse grid 瓦片从"角落小图标 + 暗色填充"改为"满铺分类插画 + 底部渐变 scrim + 文字叠底"，符合 editorial quiet-luxury 调性。

**Architecture:** 单一组件改造 — 重写 `BrowseScreen.kt::HeroCategoryTile`（去 `index` 参数、满铺 Image、垂直渐变 scrim、底部 Column 文字），删除旧的小图标/边框/编号/背景填充；调用点同步去掉 `idx`；清理 2 个不再用的 import。测试沿用 `BrowseScreenTest` 的 2 个用例，无需新增。

**Tech Stack:** Kotlin, Jetpack Compose (Box, Image, Brush.verticalGradient, ContentScale.Crop, clickable), Material3 typography (`headlineSmall`, `labelSmall`).

**Spec:** `docs/superpowers/specs/2026-09-10-browse-tile-fullbleed-image-design.md`

**Pre-condition (verified):** baseline `:app:testDebugUnitTest` 全绿（93/93 passed at end of conversation）。如开工时失败，先排查再继续。

---

### Task 1: 替换 HeroCategoryTile 函数体

**Files:**
- Modify: `app/src/main/java/com/quotegarden/feature/browse/BrowseScreen.kt:121-164`

- [x] **Step 1: 替换 HeroCategoryTile 函数体**

把 `BrowseScreen.kt` 第 121–164 行的 `@Composable private fun HeroCategoryTile(...)` 整段函数体替换为（直接覆盖，下面的代码块是完整新内容）：

```kotlin
@Composable
private fun HeroCategoryTile(
    item: CategoryCount,
    onClick: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick(item.category) }
    ) {
        Image(
            painter = painterResource(id = categoryDrawable(item.category)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )
        Column(
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

注意：函数签名从 `HeroCategoryTile(item, index, onClick)` 改成 `HeroCategoryTile(item, onClick)`。`index` 参数整个函数体不再使用，因此一并移除（Task 2 会改调用点）。

- [x] **Step 2: 编译确认函数体改完不报错**

Run（PowerShell，先设 `JAVA_HOME`）:

```powershell
$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"
server\gradlew.bat :app:compileDebugKotlin --console=plain
```

Expected: `BUILD SUCCESSFUL`。`HeroCategoryTile` 的旧 caller（`BrowseScreen.kt:110-112`）会先编译失败（参数数量不对），这是预期的，下一 Task 修。

---

### Task 2: 更新调用点（去掉 idx）

**Files:**
- Modify: `app/src/main/java/com/quotegarden/feature/browse/BrowseScreen.kt:104-113`

- [x] **Step 1: 替换 items 块**

把 `BrowseScreen.kt` 第 104–113 行（`LazyVerticalGrid(...) { items(...) { ... HeroCategoryTile(...) ... } }` 整个 `items` 块）替换为：

```kotlin
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(count = state.tiles.size, key = { state.tiles[it].category }) {
                            HeroCategoryTile(state.tiles[it], onSelectCategory)
                        }
                    }
                }
```

变化点（仅一处）：`items(count = ...) { idx -> HeroCategoryTile(state.tiles[idx], idx, onSelectCategory) }` → `items(count = ...) { HeroCategoryTile(state.tiles[it], onSelectCategory) }`。`it` 直接是索引，名字不再绑定到 `idx`。

- [x] **Step 2: 编译确认**

Run:

```powershell
$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"
server\gradlew.bat :app:compileDebugKotlin --console=plain
```

Expected: `BUILD SUCCESSFUL`。如果还报 `HeroCategoryTile` 参数不对，回去检查 Step 1 是否漏改。

---

### Task 3: 清理不再用的 import

**Files:**
- Modify: `app/src/main/java/com/quotegarden/feature/browse/BrowseScreen.kt:1-37`（import 区）

- [x] **Step 1: 删除两个未使用的 import**

`HeroCategoryTile` 改完后，整个 `BrowseScreen.kt` 不再使用：
- `androidx.compose.foundation.border`（旧 tile 用 1dp 边框，已删）
- `androidx.compose.foundation.layout.size`（旧 tile 用 `.size(56.dp)` 给图标，已删）

定位到文件顶部 import 区，删除这两行：

```kotlin
import androidx.compose.foundation.border
```

```kotlin
import androidx.compose.foundation.layout.size
```

- [x] **Step 2: 编译确认**

Run:

```powershell
$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"
server\gradlew.bat :app:compileDebugKotlin --console=plain
```

Expected: `BUILD SUCCESSFUL` 且无 unused import warning（如果 Kotlin 编译器默认 warn unused import）。如果编译器提示某个 import 还是 unused，保留（说明还有别处用）。注：实际 `size` 仍被 HeroEmptyBlock 使用，已保留；仅删除 `border`。

---

### Task 4: 跑全量单测，确认无回归

**Files:** 无

- [x] **Step 1: 跑 `:app:testDebugUnitTest`**

Run:

```powershell
$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"
server\gradlew.bat :app:testDebugUnitTest --console=plain
```

Expected: `BUILD SUCCESSFUL`，93/93 tests pass。特别确认两个：
- `BrowseScreenTest::renders category tiles and invokes onSelectCategory on tap`（用 `onNodeWithText` 断言 category 名 — 改完后 Text 节点仍存在 → pass）
- `BrowseScreenTest::empty counts renders empty state`（empty 分支没动 → pass）

如果失败：
- 检查 `HomeScreen.kt:33` 上次修过的 `matchParentSize` import（不应受影响）
- 检查 `HomeChipsTest.kt` 上次修过的 bounds 检查（不应受影响）
- 失败信息里如果提到 `HeroCategoryTile` 参数或 `idx`，回 Task 1/2 检查

- [x] **Step 2: 汇总聚合测试报告**

Run（可选，确认 0 failures）:

```powershell
$tests = 0; $failures = 0; $errors = 0
Get-ChildItem app\build\test-results\testDebugUnitTest -Filter "*.xml" | ForEach-Object {
    $x = [xml](Get-Content $_.FullName -Raw)
    $tests += [int]$x.testsuite.tests
    $failures += [int]$x.testsuite.failures
    $errors += [int]$x.testsuite.errors
}
Write-Host "TOTAL: tests=$tests failures=$failures errors=$errors"
```

Expected: `TOTAL: tests=93 failures=0 errors=0`（如有 Skip 计数不算 failure）。

---

### Task 5: 人工 preview（spec §6 risk 兜底）

**Files:** 无

- [ ] **Step 1: 在 Android Studio 装 APK 到模拟器/真机**

构建并安装 debug APK：

```powershell
$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"
server\gradlew.bat :app:installDebug --console=plain
```

或者用 Android Studio 的 Run 按钮。

- [ ] **Step 2: 打开 Browse tab，逐 tile 检查 checklist**

逐项 ✓ / ✗：

| 检查项 | 期望 |
|---|---|
| tile 整体被分类插画填满 | ✓（无黑边、无白边） |
| 文字（category 名 + "N lines"）落在底部，左对齐 | ✓ |
| 文字在深色 scrim 上，可读 | ✓（即使最浅的 autumn/birds 也读得清） |
| 左上角不再有 `#01` 编号 | ✓ |
| 右上角不再有 56dp 小图标 | ✓ |
| tile 之间仍 12dp spacing | ✓ |
| 整张 Browse 滚动到底不卡顿（13 张 1–2 MB PNG） | 预期 OK；如果明显掉帧，记录到后续优化 backlog（出 spec） |
| 点击 tile 仍跳 CategoryDetail | ✓ |

任一项 ✗：回到 Task 1 检查 scrim stops / padding / image contentScale。如"主体被 Crop 切掉" → 记录该 slug 到未来"重画插画"backlog，不在本次 spec 修。

- [ ] **Step 3: 验证 CategoryDetailScreen 头部 40dp 小图标未受影响**

点进任意一个 tile → 详情页头部右侧仍是 40dp 小图标（非满铺）。

Expected: 详情页头部外观完全不变（spec §3 non-goal 明确不动）。

---

## Rollout

本 plan 不涉及构建版本号 / release 通道改动（spec §7 已注明）。改完代码、测试通过、人工 preview OK 后，沿用现有 release 流程。

## Commit（可选，等用户明确要求再执行）

如果用户要求本批改动一次性提交（参考 AGENTS.md「NEVER commit changes unless the user explicitly asks you to」）：

```powershell
git add app\src\main\java\com\quotegarden\feature\browse\BrowseScreen.kt
git commit -m "feat(browse): tile shows full-bleed category image + bottom scrim"
```

不带 `git push`。
