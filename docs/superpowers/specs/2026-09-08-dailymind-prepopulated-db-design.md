# 预填充 SQLite 替代 JSON Seed 设计

- 日期：2026-09-08
- 状态：待评审（用户已确认 §1–§3 方向）
- 相关计划：`docs/superpowers/plans/2026-09-08-favorite-feedback-and-next-local.md`（已完成，不动）

## 1. 背景与目标

App 首次启动靠 `DailyMindApp.onCreate` 读取包内 `assets/quotes_seed.json`（187KB），经 `SeedImporter` 解析后写入 Room。
实测链路确认：seed 成功后所有读取都走 Room，但**每次冷启动**仍会把整个 asset 读进内存成 String，
再 `dao.getAll()` 全表加载只为判断空不空，造成可观浪费。

目标：删掉 JSON seed 文件及其运行时解析，改为随 APK 发布一个预填充 SQLite 库，
Room 经 `createFromAsset` 直接加载。数据内容原样不动，不做翻译过滤。

## 2. 非目标

- 不做翻译过滤：`translation` 为空的条目原样保留。
- 不升级数据库版本：`DailyMindDatabase` 保持 version 1，已安装用户的数据不受影响。
- 不改同步链路：`DailySyncWorker` / `QuoteRepository.sync` 原样保留。
- 不引入新的运行时依赖。

## 3. 架构与数据流

```
构建期（一次性，可重跑）：
  quotes_seed.json --(现有 SeedImporter 在设备上跑一次)--> dailymind.db
      --> 清空 favorite/history --> app/src/main/assets/dailymind.db

运行期（全新安装）：
  APK assets/dailymind.db --(Room createFromAsset 拷库)--> /data/data/.../dailymind.db
      --> 首屏读取全走 Room DAO（与现状 seed 成功后一致）

运行期（已安装用户）：
  version 1 未变，createFromAsset 不触碰已存在的库，行为零变化。
```

关键约束：预填充库必须由 App 自身的 Room 生成，`room_master_table` 身份哈希才与当前
schema 一致；手工造库（sqlite3 建表）必然哈希对不上，启动即崩溃。本方案杜绝手工造库。

## 4. 组件变更（共 6 处）

1. `app/src/main/java/com/dailymind/core/database/di/DatabaseModule.kt`
   - 加 `.createFromAsset("dailymind.db")`。
   - 删空转的 `onCreate` 回调（含其 `CoroutineScope` import）。
2. `app/src/main/java/com/dailymind/DailyMindApp.kt`
   - 删读 `quotes_seed.json` + `importIfEmpty` 整块、`seedImporter` 注入；
     `appScope` 若无其他使用者一并删除（以删除后编译通过为准）。
3. 删 `app/src/main/assets/quotes_seed.json`（不再打包）。
4. 删 `app/src/main/java/com/dailymind/core/data/SeedImporter.kt`。
5. 删 `app/src/test/java/com/dailymind/core/data/SeedImporterTest.kt`（随被测类删除）。
6. `.github/ISSUE_TEMPLATE/bug_report.md` 第 36 行：logcat 过滤提示里的 `SeedImporter` 字样删掉。

顺序要求：先执行 §5 生成 `dailymind.db` 并放入 assets，**再**删 2–5（生成步骤依赖现有的
`SeedImporter` 代码）。

## 5. 预填充库生成步骤（一次性，可重跑）

前置：模拟器或真机一台，`adb devices` 可见；Gradle 需用 Temurin JDK
（`$env:JAVA_HOME="C:\Users\Deng\.jdks\temurin-21"`），否则 `compileDebugJavaWithJavac` 的
jlink 步骤失败（与本任务无关的环境要求）。

```bat
:: 1. 全新安装当前 debug 包（此时仍含 seed 逻辑），启动 App 看到今日 quote 即表示 seed 完成
.\server\gradlew.bat :app:installDebug

:: 2. 把设备上的库拷出来（经 /sdcard 中转，避开 PowerShell 重定向损坏二进制）
adb shell "run-as com.dailymind cp databases/dailymind.db /sdcard/dailymind_seed.db"
adb pull /sdcard/dailymind_seed.db

:: 3. 清空用户表并压缩（本机有 sqlite3；没有则用 DB Browser 做等价操作）
sqlite3 dailymind_seed.db "DELETE FROM favorite; DELETE FROM history; VACUUM;"

:: 4. 校验：quote 行数 == quotes_seed.json 的 items 数；favorite/history 为 0
sqlite3 dailymind_seed.db "SELECT COUNT(*) FROM quote; SELECT COUNT(*) FROM favorite; SELECT COUNT(*) FROM history;"

:: 5. 放入 assets（文件名与 DatabaseModule.createFromAsset 参数一致）
copy dailymind_seed.db app\src\main\assets\dailymind.db
```

以后 seed 数据要更新：重跑本节即可，并在 commit message 注明数据来源版本。

## 6. 错误处理

| 风险 | 对策 |
|---|---|
| `room_master_table` 哈希 mismatch 启动崩溃 | 只用 §5 流程（自身 Room 生成）；以后改实体/升 Room 版本必须重跑 §5，version 升级的 migration 到时再定 |
| asset 缺失或文件名与 `createFromAsset` 参数不一致 | 接线后立即做卸载重装验证（§7），首次创建即暴露 |
| 已安装用户数据丢失 | version 保持 1，`createFromAsset` 只在建库时拷贝，已存在库不受影响；`fallbackToDestructiveMigration` 保留但无版本变更不会触发 |
| 设备不可用导致无法生成 | §5 是前置阻塞项，无设备则停下先解决，不手写造库凑合 |
| Release 包 minify | 预填充库是二进制拷贝，不受 R8/ProGuard 影响；`isShrinkResources` 只处理 res，不动 assets |

## 7. 测试与验证

- 删除 `SeedImporterTest`；其余单测（`QuoteRepositoryTest`、`HomeViewModelTest` 等）不依赖
  seed 文件，必须全绿：`.\server\gradlew.bat :app:test` BUILD SUCCESSFUL。
- `.\server\gradlew.bat :app:compileDebugKotlin` 与 `:app:assembleDebug` 通过。
- 真机/模拟器卸载重装验证清单：
  - 首启 Home 有数据，无需等待、无 JSON 解析日志；
  - 飞行模式首启同样有数据（纯本地库）；
  - `Next →`、收藏/取消收藏、Favorite 左滑删除均正常（走新本地库）；
  - logcat 无 Room `room_master_table` 相关崩溃。

## 8. 风险与回滚

- 最大风险是 §5 生成环境（设备/adb）不可用 → 停下，不绕路。
- 回滚：本任务所有改动集中在 6 处文件 + 1 个 asset；任一步失败，
  `git checkout -- <file>` / 删 `assets/dailymind.db` 即可回到 JSON seed 状态（需同步恢复
  `SeedImporter` 相关删除）。
