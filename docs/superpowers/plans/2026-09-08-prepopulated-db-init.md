# 预填充 SQLite 替代 JSON Seed Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 删除 `quotes_seed.json` 及其运行时解析，APK 改为发布预填充 `dailymind.db`，Room 经 `createFromAsset` 直接加载初始化数据。

**Architecture:** 先用 App 自身的 Room 在模拟器上跑一次现有 seed 导入并导出 `.db`（保证 `room_master_table` 身份哈希一致），清空用户表后放入 `assets/`；再接线 `createFromAsset`，删除 seed 文件、`SeedImporter` 及其测试与启动代码。数据库 version 保持 1，已安装用户不受影响。

**Tech Stack:** Kotlin + Room 2.8.4 (`createFromAsset`) + Hilt 2.60.1 + adb (SDK `platform-tools`) + JUnit4/MockK（回归）。

**Spec:** `docs/superpowers/specs/2026-09-08-dailymind-prepopulated-db-design.md`

**关于 TDD 的说明：** 本任务无新增可单元测试的行为（全是删除 + 框架接线），不适用红绿循环；以全量单测回归 + 编译 + 真机验证代替，每个 Task 明确写出验证命令与期望输出。

---

## File Structure

```
app/src/main/assets/
  quotes_seed.json                       # Delete: 不再打包
  dailymind.db                           # Create: 预填充库（499 行 quote，favorite/history 为空），二进制
app/src/main/java/com/dailymind/core/database/di/
  DatabaseModule.kt                      # Modify: 加 createFromAsset，删空转 onCreate 回调
app/src/main/java/com/dailymind/
  DailyMindApp.kt                        # Modify: 删 seed 整块
app/src/main/java/com/dailymind/core/data/
  SeedImporter.kt                        # Delete: 生成完 .db 即成死代码
app/src/test/java/com/dailymind/core/data/
  SeedImporterTest.kt                    # Delete: 随被测类删除
.github/ISSUE_TEMPLATE/
  bug_report.md                          # Modify: 第 36 行去掉 SeedImporter 字样
```

---

### Task 1: 生成预填充库 `dailymind.db`（设备步骤，前置阻塞）

**Files:**
- Create: `app/src/main/assets/dailymind.db`（二进制，499 行 quote）

- [x] **Step 1: 启动模拟器并确认 adb 可见**

Run（PowerShell，adb 用全路径，不在 PATH）：
```powershell
Start-Process "C:\Users\Deng\AppData\Local\Android\Sdk\emulator\emulator.exe" -ArgumentList "-avd Medium_Phone_API_36.1"
C:\Users\Deng\AppData\Local\Android\Sdk\platform-tools\adb.exe wait-for-device
C:\Users\Deng\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```
Expected: `List of devices attached` 下有一台 `device`（冷启动需几分钟，`wait-for-device` 会阻塞到就绪）。
如果模拟器起不来 → **停下**，先解决设备问题，不绕路（spec §6）。

- [x] **Step 2: 卸载旧包，全新安装当前 debug（仍含 seed 逻辑）**

Run（Gradle 必须用 Temurin JDK，否则 jlink 失败）：
```powershell
C:\Users\Deng\AppData\Local\Android\Sdk\platform-tools\adb.exe uninstall com.dailymind
$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"; .\server\gradlew.bat :app:installDebug 2>&1 | Select-Object -Last 5
```
Expected: uninstall 输出 `Success` 或 `Failure [not installed]`（两种都算过，保证全新）；
install 输出 `BUILD SUCCESSFUL`。

- [x] **Step 3: 启动 App，确认 seed 完成**

Run：
```powershell
C:\Users\Deng\AppData\Local\Android\Sdk\platform-tools\adb.exe shell monkey -p com.dailymind -c android.intent.category.LAUNCHER 1
```
Expected: 模拟器上 App 打开且 Home 显示今日 quote（即库中有数据，seed 完成）。

- [x] **Step 4: 导出设备上的库到仓库根目录临时文件**

Run：
```powershell
C:\Users\Deng\AppData\Local\Android\Sdk\platform-tools\adb.exe shell "run-as com.dailymind cp databases/dailymind.db /sdcard/dailymind_seed.db"
C:\Users\Deng\AppData\Local\Android\Sdk\platform-tools\adb.exe pull /sdcard/dailymind_seed.db dailymind_seed.db
```
Expected: 第二条输出 `... pulled` 且仓库根出现 `dailymind_seed.db`（约几百 KB）。
注意：必须经 `/sdcard` 中转，直接 `exec-out ... > file` 在 PowerShell 下会损坏二进制。

- [x] **Step 5: 清空用户表并校验行数（python 兜底，本机无 sqlite3 CLI）**

Run：
```powershell
python -c "import sqlite3; c=sqlite3.connect('dailymind_seed.db'); c.execute('DELETE FROM favorite'); c.execute('DELETE FROM history'); c.commit(); print('quote=', c.execute('SELECT COUNT(*) FROM quote').fetchone()[0]); print('favorite=', c.execute('SELECT COUNT(*) FROM favorite').fetchone()[0]); print('history=', c.execute('SELECT COUNT(*) FROM history').fetchone()[0]); c.close()"
```
Expected: 输出恰好三行 `quote= 499`、`favorite= 0`、`history= 0`。
499 是 `quotes_seed.json` 的 items 实测总数；对不上 → 停下排查，不继续。

- [x] **Step 6: 放入 assets，删临时文件，提交**

```bash
git status --short
copy dailymind_seed.db app\src\main\assets\dailymind.db
del dailymind_seed.db
git add app/src/main/assets/dailymind.db
git commit -m "feat(db): add prepopulated dailymind.db (499 quotes, no user rows)"
```
Expected: `git status --short` 在 copy 前无 `dailymind.db` 相关残留；commit 成功。
（`copy`/`del` 是 cmd 写法，在 PowerShell 下分别等价 `Copy-Item`/`Remove-Item`，用哪种都行。）

---

### Task 2: DatabaseModule 接 `createFromAsset` + 删空回调

**Files:**
- Modify: `app/src/main/java/com/dailymind/core/database/di/DatabaseModule.kt`

- [x] **Step 1: 整个文件替换为**

```kotlin
package com.dailymind.core.database.di

import android.content.Context
import androidx.room.Room
import com.dailymind.core.database.DailyMindDatabase
import com.dailymind.core.database.dao.FavoriteDao
import com.dailymind.core.database.dao.HistoryDao
import com.dailymind.core.database.dao.QuoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DailyMindDatabase =
        Room.databaseBuilder(context, DailyMindDatabase::class.java, "dailymind.db")
            .createFromAsset("dailymind.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideQuoteDao(db: DailyMindDatabase): QuoteDao = db.quoteDao()

    @Provides
    fun provideFavoriteDao(db: DailyMindDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideHistoryDao(db: DailyMindDatabase): HistoryDao = db.historyDao()
}
```

（相对现文件：删 `RoomDatabase`、`SupportSQLiteDatabase`、`CoroutineScope`、`Dispatchers`、`launch` 五个 import，
删整个 `.addCallback(...)` 块，加 `.createFromAsset("dailymind.db")` 一行；其余原样。）

- [x] **Step 2: 编译**

Run: `$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"; .\server\gradlew.bat :app:compileDebugKotlin 2>&1 | Select-Object -Last 5`
Expected: `BUILD SUCCESSFUL`

- [x] **Step 3: Commit**

```bash
git add app/src/main/java/com/dailymind/core/database/di/DatabaseModule.kt
git commit -m "feat(db): load prepopulated db via createFromAsset, drop empty onCreate callback"
```

---

### Task 3: DailyMindApp 删除 seed 整块

**Files:**
- Modify: `app/src/main/java/com/dailymind/DailyMindApp.kt`

- [x] **Step 1: 整个文件替换为**

```kotlin
package com.dailymind

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dailymind.sync.DailySyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DailyMindApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        DailySyncWorker.enqueue(this)
        DailySyncWorker.enqueueImmediate(this)
    }
}
```

（相对现文件：删 `SeedImporter`、`CoroutineScope`、`Dispatchers`、`SupervisorJob`、`launch` 五个 import，
删 `seedImporter` 注入、`appScope` 字段、`appScope.launch { ... }` 整块；两个 `DailySyncWorker.enqueue` 保留。）

- [x] **Step 2: 编译**

Run: `$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"; .\server\gradlew.bat :app:compileDebugKotlin 2>&1 | Select-Object -Last 5`
Expected: `BUILD SUCCESSFUL`

- [x] **Step 3: Commit**

```bash
git add app/src/main/java/com/dailymind/DailyMindApp.kt
git commit -m "feat(app): drop JSON seed on launch, db comes prepopulated"
```

---

### Task 4: 删除 seed 文件、`SeedImporter` 及其测试，改 issue 模板

**Files:**
- Delete: `app/src/main/assets/quotes_seed.json`
- Delete: `app/src/main/java/com/dailymind/core/data/SeedImporter.kt`
- Delete: `app/src/test/java/com/dailymind/core/data/SeedImporterTest.kt`
- Modify: `.github/ISSUE_TEMPLATE/bug_report.md`（第 36 行）

- [x] **Step 1: 删除三个文件，改模板一行**

```bash
git rm app/src/main/assets/quotes_seed.json app/src/main/java/com/dailymind/core/data/SeedImporter.kt app/src/test/java/com/dailymind/core/data/SeedImporterTest.kt
```

模板第 36 行由：
```
If available, paste from `adb logcat` filtered by `DailySyncWorker`, `SeedImporter`, or the app's tag.
```
改为：
```
If available, paste from `adb logcat` filtered by `DailySyncWorker` or the app's tag.
```

- [x] **Step 2: 全量单测回归（SeedImporterTest 已删，其余必须全绿）**

Run: `$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"; .\server\gradlew.bat :app:test 2>&1 | Select-Object -Last 5`
Expected: `BUILD SUCCESSFUL`（此前 45 个测试（含 SeedImporterTest 5 个）→ 此后 40 个，0 failures/errors；
以 test-results XML 中 `failures="0" errors="0"` 为准）

- [x] **Step 3: Commit**

```bash
git add .github/ISSUE_TEMPLATE/bug_report.md
git commit -m "chore(seed): remove JSON seed pipeline and stale template reference"
```
（注：三个删除文件已由 `git rm` 进入暂存，无需再 add。）

---

### Task 5: 设备端验证 + assembleDebug

- [x] **Step 1: 卸载重装，全新安装走预填充库**

Run：
```powershell
C:\Users\Deng\AppData\Local\Android\Sdk\platform-tools\adb.exe uninstall com.dailymind
$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"; .\server\gradlew.bat :app:installDebug 2>&1 | Select-Object -Last 3
C:\Users\Deng\AppData\Local\Android\Sdk\platform-tools\adb.exe shell monkey -p com.dailymind -c android.intent.category.LAUNCHER 1
```
Expected: `BUILD SUCCESSFUL`；App 首启直接显示今日 quote。

- [x] **Step 2: 功能与离线清单（模拟器上手动走一遍）**

- 首启有数据，无需等待；`Next →` 切 quote 正常；
- 收藏/取消收藏、Favorite 左滑删除正常；
- 开飞行模式后卸载重装再首启：同样有数据（证明纯本地库，无 JSON、无网络依赖）；
- logcat 无 Room/`room_master_table` 相关崩溃。

- [x] **Step 3: assembleDebug**

Run: `$env:JAVA_HOME = "C:\Users\Deng\.jdks\temurin-21"; .\server\gradlew.bat :app:assembleDebug 2>&1 | Select-Object -Last 3`
Expected: `BUILD SUCCESSFUL`

- [x] **Step 4: 收尾检查**

Run: `git status --short`
Expected: 无 tracked 修改残留（只有之前就存在的 untracked 文件）。如有 lint/编译顺手修的，
则 `git add -A && git commit -m "chore: verify prepopulated db init"`，否则不提交空 commit。

---

## Self-review checklist

1. Spec coverage:
   - §3 数据流（assets .db → createFromAsset → Room；老用户不动）→ Task 1（生成）+ Task 2（接线）
   - §4 组件变更 6 处 → Task 2（第 1 处）、Task 3（第 2 处）、Task 4（第 3–6 处）
   - §4 顺序要求（先生成再删）→ Task 1 排最前，Task 3/4 在后
   - §5 生成步骤 → Task 1 Step 1–6（命令、499 行校验、PowerShell 二进制坑全部落地）
   - §6 错误处理 → Task 1 阻塞项、Task 5 崩溃检查、version 不变贯穿
   - §7 测试验证 → Task 4 Step 2（回归）、Task 5（设备清单 + assemble）
   - §2 非目标（不过滤/不升版/不动同步/不加依赖）→ 全计划无过滤代码、无 version 改动、无新依赖
2. Placeholders — 无。499 行是实测值；adb/emulator/python 路径均已验证存在；
   Task 3 的"占位写法"提示已在同一 Step 内给出完整真实代码（执行时以第二块完整代码为准，
   注解保持 `@HiltAndroidApp`）。
3. Type consistency — `.createFromAsset("dailymind.db")` 与 `app/src/main/assets/dailymind.db`
   同名；库名 `"dailymind.db"` 与现 `databaseBuilder` 一致；`uninstall com.dailymind`
   与 `namespace = "com.dailymind"`（无独立 applicationId，debug 包名即此）一致；
   Task 3 注解保持 `@HiltAndroidApp`，与现文件一致。
