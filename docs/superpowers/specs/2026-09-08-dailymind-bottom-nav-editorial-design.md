# 底部导航 + Me 页 + Home 杂志混搭刷新设计

- 日期：2026-09-08
- 状态：待评审（§1–§4 已逐节确认：A 式文字栏 / Home｜Me 两项 / Me 收藏+设置聚合 / H1 杂志主导）
- 前置决策：editorial 规范曾两次排除 BottomNav（`2026-09-03-...-design.md` §Scope/§75/§108），
  本次经用户确认推翻——底部栏以 editorial 文字式实现，不引入 Material3 标准栏。
- 视觉 mockups（本地留档，未进仓库）：`.superpowers/brainstorm/208-1788852187/content/`
  `nav-context.html`、`bar-style.html`、`home-hybrid.html`

## 1. 目标

底部常驻两项 `Today｜Me`（editorial 文字式，选中 accent 下划线）；新建 Me 页（上收藏下设置）；
Home 按 H1 杂志方向提纯排印。整个改版只用现有设计语言，不引入新依赖。

## 2. 非目标

- 不做 Material3 标准 `NavigationBar`（已否决的 B 方案）。
- 不做 History / Profile 目的地（`Route.History`、`Route.Profile` 保持现状：已定义、无屏）。
- Me 设置段只要 Theme + About；不做清理缓存、同步状态、账号、通知。
- Home 不做 Apple Books 式卡片 + 横滑推荐（已否决的 H2 方向：需新增取 N 条随机的数据通路）。
- 不新增顶级路由：Theme 三选与 About 均用 dialog，App 只有 Home / Me 两个 entry。

## 3. 架构与组件

```
AppNavDisplay（Navigation3）
├── Route.Home  → HomeScreen（H1 排印刷新，删两处 Saved → 文字链）
└── Route.Me    → MeScreen（新建，单列 LazyColumn）
                    ├── SAVED 段：复用 IndexRow（含左滑 Unfavorite），空态复用 EditorialEmpty
                    └── SETTINGS 段：新建 SettingRow（右箭头行）
                                      ├── Theme → dialog 三选（System/Light/Dark，接 THEME key）
                                      └── About → dialog（版本 + 一句话）
```

- `Route.Favorite` 删除；`FavoriteScreen.kt` 删除；`FavoriteViewModel` 改名 `MeViewModel`
  （favorites flow + `Unfavorite` 事件原样保留，测试文件同步改名）。
- `DailyMindTheme` 改为观察 DataStore `THEME` 值（默认 System = 现状跟随系统）；
  `THEME` key 已定义但从未读写，本次是第一次接线。
- 底部栏是新 composable（建议放 `core/designsystem`，命名如 `EditorialBottomBar`），
  由 `AppNavDisplay` 持有选中态，常驻于两个 entry 之下（Scaffold bottomBar 或 NavDisplay 外层 Column）。
- Tab 切换 single-top：点已选中 tab 不压栈；切 tab 直接切，不堆 `Home→Me→Home`。
  现 `mutableStateListOf` 简单压栈语义要改（具体载体由实现计划定）。
- 返回键：Me → 回 Home；Home → 系统默认退出。
- 状态保持：tab 来回切不重载 Home quote/浏览态（Navigation3 entry 作用域默认行为，实现时验证）。

## 4. 数据流

- Me 的收藏段：`MeViewModel.favorites`（原 `observeFavorites`）→ `IndexRow` →
  `onSwipeOut` → `FavoriteEvent.Unfavorite`（改名后事件名由实现定，保持语义）→ `toggleFavorite`。
- Theme：读 `DataStore[THEME]` → `DailyMindTheme(darkTheme=…)`；
  写：dialog 选择 → `dataStore.edit`。读失败/值非法一律回落 System。
- About：静态文本（`versionName 0.1.0-beta.1`，实现时从 BuildConfig 取，禁止硬编码）。

## 5. 错误处理

| 情况 | 处理 |
|---|---|
| THEME 值缺失/非法 | 回落 System（跟随系统），不崩溃不报错 |
| Me 收藏为空 | SAVED 段复用 `EditorialEmpty`（现有组件），SETTINGS 段正常显示 |
| dialog 外点/返回键 | 正常 dismiss，不写值 |
| 深色/浅色主题 | 底部栏与 Me 全用 `MaterialTheme.colorScheme` token，沿用 DarkQuietLuxury / LightQuietLuxury，不写死颜色 |
| 360dp / 大字号 | tab 文字不截断（min 触区 48dp，`weight(1f)` 均分）；超长翻译沿用现有截断策略，不为本任务新开 |

## 6. 测试与验证

- 改名：`FavoriteViewModelTest` → `MeViewModelTest`（断言原样，类名同步），全绿。
- 新增：THEME 读写单测（参照现有 `PreferencesTest` 模式）；tab single-top 状态语义单测
 （若实现引入可测的状态载体；纯 NavDisplay 写法则以真机验证代替）。
- 回归：全量 `:app:test` 绿；`:app:assembleDebug` 过。
- 真机/模拟器验证：两 tab 切换、返回键行为、切 tab 状态保持、Theme 三选即时生效、
  About dialog、空收藏 Me 页、深浅主题各看一遍。

## 7. 范围说明

本 spec 自洽可单 plan 实施；若实现计划阶段发现 Home H1 刷新与导航+Me 耦合度低，
允许拆成两个 plan（导航+Me / Home 排印刷新）先后执行，以 writing-plans 阶段决定为准。
