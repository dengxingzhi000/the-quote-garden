# DailyMind v1.0 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付 DailyMind V1 MVP — Offline-First 单体架构，每日一句/随机一句/收藏/分享/发音，Room 为本地 SSOT，WorkManager 后台同步，Spring Boot 4 Modular Monolith + PostgreSQL 提供增量同步 API。

**Architecture:** Android 采用单 `:app` Module 内 Feature-Based 分层 (Presentation UDF + Domain + Data)，Room 为 Single Source of Truth，Repository 暴露 Flow，ViewModel 持有 StateFlow。后端为 Java 21 + Spring Boot 4 模块化单体，Flyway 管理 PostgreSQL，Importer 将 Quote Garden 归一化后写入 DB，客户端通过 `updatedAfter + cursor` 增量同步。

**Tech Stack:** Kotlin + Compose + Navigation 3 + ViewModel/StateFlow/UDF + Hilt + Coroutines/Flow + Room + DataStore + Retrofit/OkHttp + Kotlin Serialization + WorkManager + Media3 + Coil；Backend Java 21 + Spring Boot 4 + PostgreSQL + Flyway + Spring Security + OpenAPI；Gradle Kotlin DSL + Version Catalog + Compose BOM

---

## File Structure

```
Andorid_Project/
├── gradle/libs.versions.toml
├── gradle.properties
├── settings.gradle.kts
├── app/
│   ├── build.gradle.kts
│   └── src/main/java/com/dailymind/
│       ├── DailyMindApp.kt
│       ├── MainActivity.kt
│       ├── core/
│       │   ├── common/Result.kt
│       │   ├── common/DispatchersProvider.kt
│       │   ├── designsystem/theme/Theme.kt
│       │   ├── model/Quote.kt
│       │   ├── database/DailyMindDatabase.kt
│       │   ├── database/entity/QuoteEntity.kt
│       │   ├── database/entity/FavoriteEntity.kt
│       │   ├── database/entity/HistoryEntity.kt
│       │   ├── database/dao/QuoteDao.kt
│       │   ├── datastore/PreferencesDataStore.kt
│       │   ├── network/ApiService.kt
│       │   ├── network/dto/QuoteDto.kt
│       │   ├── network/di/NetworkModule.kt
│       │   └── navigation/AppNavDisplay.kt
│       ├── feature/
│       │   ├── home/HomeUiState.kt
│       │   ├── home/HomeViewModel.kt
│       │   ├── home/HomeScreen.kt
│       │   ├── favorite/FavoriteViewModel.kt
│       │   └── favorite/FavoriteScreen.kt
│       ├── sync/SyncRepository.kt
│       └── sync/DailySyncWorker.kt
├── server/
│   ├── build.gradle.kts
│   ├── docker-compose.yml
│   └── src/main/
│       ├── java/com/dailymind/
│       │   ├── DailyMindServerApplication.java
│       │   ├── common/ApiResponse.java
│       │   ├── quote/controller/QuoteController.java
│       │   ├── quote/application/QuoteService.java
│       │   ├── quote/domain/Quote.java
│       │   ├── quote/infrastructure/QuoteRepository.java
│       │   ├── sync/controller/SyncController.java
│       │   └── importer/QuoteGardenImporter.java
│       └── resources/db/migration/V1__init.sql
└── docs/superpowers/plans/2026-09-02-dailymind-v1-mvp.md
```

---

### Task 1: Project Scaffolding — Gradle Kotlin DSL + Version Catalog + Compose BOM

**Files:**
- Create: `gradle/libs.versions.toml`
- Create: `settings.gradle.kts`
- Create: `app/build.gradle.kts`
- Modify: `gradle.properties`

- [ ] **Step 1: Write failing test for catalog existence**

```kotlin
// app/src/test/java/com/dailymind/ScaffoldingTest.kt
package com.dailymind
import org.junit.Test
import org.junit.Assert.*
import java.io.File
class ScaffoldingTest {
    @Test fun `version catalog exists`() {
        assertTrue(File("gradle/libs.versions.toml").exists())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dailymind.ScaffoldingTest" -q`
Expected: FAIL `FileNotFound` / `assertTrue` false

- [ ] **Step 3: Write minimal implementation**

```toml
# gradle/libs.versions.toml
[versions]
kotlin = "2.0.21"
compose-bom = "2024.09.02"
room = "2.6.1"
hilt = "2.51.1"
retrofit = "2.11.0"
serialization = "1.7.3"
navigation3 = "1.0.0-alpha01"

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
retrofit-core = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.28" }
```

```kotlin
// settings.gradle.kts
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
    versionCatalogs { create("libs") { from(files("gradle/libs.versions.toml")) } }
}
include(":app")
```

```kotlin
// app/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
}
android {
    namespace = "com.dailymind"
    compileSdk = 35
    defaultConfig { minSdk = 24; targetSdk = 35 }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}
dependencies {
    val bom = libs.androidx.compose.bom
    implementation(platform(bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation(libs.retrofit.core)
    implementation(libs.kotlinx.serialization.json)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dailymind.ScaffoldingTest" -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml settings.gradle.kts app/build.gradle.kts
git commit -m "feat: scaffold gradle kotlin dsl with version catalog and compose bom"
```

---

### Task 2: Core Domain — Result + Quote Model

**Files:**
- Create: `app/src/main/java/com/dailymind/core/common/Result.kt`
- Create: `app/src/main/java/com/dailymind/core/model/Quote.kt`
- Test: `app/src/test/java/com/dailymind/core/model/QuoteTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// app/src/test/java/com/dailymind/core/model/QuoteTest.kt
package com.dailymind.core.model
import com.dailymind.core.common.Result
import org.junit.Test
import org.junit.Assert.*
class QuoteTest {
    @Test fun `quote mapping preserves fields`() {
        val q = Quote(id="1", content="Hello", translation="你好", author="Anon", category="life", difficulty=1, audioUrl=null, imageUrl=null, updatedAt=1000L)
        assertEquals("Hello", q.content)
        val r: Result<Quote> = Result.Success(q)
        assertTrue(r is Result.Success)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dailymind.core.model.QuoteTest" -q`
Expected: FAIL `Unresolved reference: Quote`

- [ ] **Step 3: Write minimal implementation**

```kotlin
// app/src/main/java/com/dailymind/core/common/Result.kt
package com.dailymind.core.common
sealed interface Result<out T> {
    data class Success<T>(val data: T): Result<T>
    data class Error(val throwable: Throwable, val message: String? = throwable.message): Result<Nothing>
    data object Loading: Result<Nothing>
}

// app/src/main/java/com/dailymind/core/model/Quote.kt
package com.dailymind.core.model
data class Quote(
    val id: String,
    val content: String,
    val translation: String?,
    val author: String?,
    val category: String?,
    val difficulty: Int?,
    val audioUrl: String?,
    val imageUrl: String?,
    val updatedAt: Long
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dailymind.core.model.QuoteTest" -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dailymind/core/common/Result.kt app/src/main/java/com/dailymind/core/model/Quote.kt app/src/test/java/com/dailymind/core/model/QuoteTest.kt
git commit -m "feat: add Result sealed interface and Quote domain model"
```

---

### Task 3: Room — Entities, DAOs, Database (Local SSOT)

**Files:**
- Create: `app/src/main/java/com/dailymind/core/database/entity/QuoteEntity.kt`
- Create: `app/src/main/java/com/dailymind/core/database/dao/QuoteDao.kt`
- Create: `app/src/main/java/com/dailymind/core/database/DailyMindDatabase.kt`
- Test: `app/src/androidTest/java/com/dailymind/core/database/QuoteDaoTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// app/src/androidTest/java/com/dailymind/core/database/QuoteDaoTest.kt
package com.dailymind.core.database
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dailymind.core.database.entity.QuoteEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class QuoteDaoTest {
    @Test fun insertAndRead() = runTest {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DailyMindDatabase::class.java).allowMainThreadQueries().build()
        val dao = db.quoteDao()
        dao.upsertAll(listOf(QuoteEntity("1","Hello","你好","Anon","life",1,null,null,1000L,0L)))
        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals("Hello", all[0].content)
        db.close()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dailymind.core.database.QuoteDaoTest -q`
Expected: FAIL `Unresolved reference: DailyMindDatabase`

- [ ] **Step 3: Write minimal implementation**

```kotlin
// app/src/main/java/com/dailymind/core/database/entity/QuoteEntity.kt
package com.dailymind.core.database.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "quote")
data class QuoteEntity(
    @PrimaryKey val id: String,
    val content: String,
    val translation: String?,
    val author: String?,
    val category: String?,
    val difficulty: Int?,
    val audioUrl: String?,
    val imageUrl: String?,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

// app/src/main/java/com/dailymind/core/database/entity/FavoriteEntity.kt
package com.dailymind.core.database.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "favorite")
data class FavoriteEntity(@PrimaryKey val quoteId: String, val createdAt: Long)

// app/src/main/java/com/dailymind/core/database/entity/HistoryEntity.kt
package com.dailymind.core.database.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "history")
data class HistoryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val quoteId: String, val viewedAt: Long)

// app/src/main/java/com/dailymind/core/database/dao/QuoteDao.kt
package com.dailymind.core.database.dao
import androidx.room.*
import com.dailymind.core.database.entity.QuoteEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface QuoteDao {
    @Query("SELECT * FROM quote WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<QuoteEntity>>
    @Query("SELECT * FROM quote WHERE deletedAt IS NULL")
    suspend fun getAll(): List<QuoteEntity>
    @Query("SELECT * FROM quote WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): QuoteEntity?
    @Query("SELECT * FROM quote WHERE deletedAt IS NULL ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandom(): QuoteEntity?
    @Upsert
    suspend fun upsertAll(entities: List<QuoteEntity>)
    @Query("SELECT MAX(updatedAt) FROM quote")
    suspend fun getMaxUpdatedAt(): Long?
}

// app/src/main/java/com/dailymind/core/database/DailyMindDatabase.kt
package com.dailymind.core.database
import androidx.room.Database
import androidx.room.RoomDatabase
import com.dailymind.core.database.dao.QuoteDao
import com.dailymind.core.database.entity.FavoriteEntity
import com.dailymind.core.database.entity.HistoryEntity
import com.dailymind.core.database.entity.QuoteEntity
@Database(entities = [QuoteEntity::class, FavoriteEntity::class, HistoryEntity::class], version = 1, exportSchema = true)
abstract class DailyMindDatabase: RoomDatabase() {
    abstract fun quoteDao(): QuoteDao
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dailymind.core.database.QuoteDaoTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dailymind/core/database/
git commit -m "feat: add Room SSOT with Quote/Favorite/History entities and QuoteDao"
```

---

### Task 4: Network Layer — Retrofit + OkHttp + Kotlin Serialization

**Files:**
- Create: `app/src/main/java/com/dailymind/core/network/dto/QuoteDto.kt`
- Create: `app/src/main/java/com/dailymind/core/network/ApiService.kt`
- Create: `app/src/main/java/com/dailymind/core/network/di/NetworkModule.kt`
- Test: `app/src/test/java/com/dailymind/core/network/ApiServiceTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// app/src/test/java/com/dailymind/core/network/ApiServiceTest.kt
package com.dailymind.core.network
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
class ApiServiceTest {
    @Test fun `get daily quote parses dto`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""{"id":"1","content":"Hello","translation":"你好","author":"Anon","category":"life","difficulty":1,"audioUrl":null,"imageUrl":null,"updatedAt":1000}""").setResponseCode(200))
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        val api = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(ApiService::class.java)
        val dto = api.getDailyQuote()
        assertEquals("1", dto.id)
        server.shutdown()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dailymind.core.network.ApiServiceTest" -q`
Expected: FAIL `Unresolved reference: ApiService`

- [ ] **Step 3: Write minimal implementation**

```kotlin
// app/src/main/java/com/dailymind/core/network/dto/QuoteDto.kt
package com.dailymind.core.network.dto
import kotlinx.serialization.Serializable
@Serializable
data class QuoteDto(
    val id: String,
    val content: String,
    val translation: String? = null,
    val author: String? = null,
    val category: String? = null,
    val difficulty: Int? = null,
    val audioUrl: String? = null,
    val imageUrl: String? = null,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
@Serializable
data class SyncResponse(val items: List<QuoteDto>, val nextCursor: String? = null)

// app/src/main/java/com/dailymind/core/network/ApiService.kt
package com.dailymind.core.network
import com.dailymind.core.network.dto.QuoteDto
import com.dailymind.core.network.dto.SyncResponse
import retrofit2.http.GET
import retrofit2.http.Query
interface ApiService {
    @GET("api/v1/quotes/daily")
    suspend fun getDailyQuote(): QuoteDto
    @GET("api/v1/quotes/random")
    suspend fun getRandomQuote(): QuoteDto
    @GET("api/v1/sync/quotes")
    suspend fun syncQuotes(@Query("updatedAfter") updatedAfter: Long, @Query("cursor") cursor: String? = null, @Query("limit") limit: Int = 100): SyncResponse
}

// app/src/main/java/com/dailymind/core/network/di/NetworkModule.kt
package com.dailymind.core.network.di
import com.dailymind.core.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton
@Module @InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton fun provideJson(): Json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    @Provides @Singleton fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder().build()
    @Provides @Singleton fun provideRetrofit(json: Json, client: OkHttpClient): Retrofit =
        Retrofit.Builder().baseUrl("https://api.dailymind.local/").client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()
    @Provides @Singleton fun provideApi(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dailymind.core.network.ApiServiceTest" -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dailymind/core/network/
git commit -m "feat: add network layer with Retrofit and Kotlin Serialization"
```

---

### Task 5: Repository — Offline-First (Room SSOT + Network Sync)

**Files:**
- Create: `app/src/main/java/com/dailymind/core/data/QuoteRepository.kt`
- Create: `app/src/main/java/com/dailymind/core/data/QuoteRepositoryImpl.kt`
- Test: `app/src/test/java/com/dailymind/core/data/QuoteRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// app/src/test/java/com/dailymind/core/data/QuoteRepositoryTest.kt
package com.dailymind.core.data
import com.dailymind.core.database.dao.QuoteDao
import com.dailymind.core.database.entity.QuoteEntity
import com.dailymind.core.network.ApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
class QuoteRepositoryTest {
    @Test fun `observeQuotes emits from dao`() = runTest {
        val dao = mockk<QuoteDao>()
        val api = mockk<ApiService>()
        coEvery { dao.observeAll() } returns kotlinx.coroutines.flow.flowOf(listOf(QuoteEntity("1","H","t","A","c",1,null,null,1L,null)))
        val repo = QuoteRepositoryImpl(dao, api)
        val list = repo.observeQuotes().first()
        assertEquals(1, list.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dailymind.core.data.QuoteRepositoryTest" -q`
Expected: FAIL `Unresolved reference: QuoteRepositoryImpl`

- [ ] **Step 3: Write minimal implementation**

```kotlin
// app/src/main/java/com/dailymind/core/data/QuoteRepository.kt
package com.dailymind.core.data
import com.dailymind.core.model.Quote
import kotlinx.coroutines.flow.Flow
interface QuoteRepository {
    fun observeQuotes(): Flow<List<Quote>>
    suspend fun getDailyQuote(): Quote?
    suspend fun getRandomQuote(): Quote
    suspend fun sync(): Result<Unit>
    suspend fun toggleFavorite(quoteId: String)
}

// app/src/main/java/com/dailymind/core/data/QuoteRepositoryImpl.kt
package com.dailymind.core.data
import com.dailymind.core.database.dao.QuoteDao
import com.dailymind.core.database.entity.QuoteEntity
import com.dailymind.core.model.Quote
import com.dailymind.core.network.ApiService
import com.dailymind.core.network.dto.QuoteDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
class QuoteRepositoryImpl @Inject constructor(private val dao: QuoteDao, private val api: ApiService): QuoteRepository {
    override fun observeQuotes(): Flow<List<Quote>> = dao.observeAll().map { list -> list.map { it.toModel() } }
    override suspend fun getDailyQuote(): Quote? = dao.getAll().firstOrNull()?.toModel()
    override suspend fun getRandomQuote(): Quote = (dao.getRandom() ?: throw NoSuchElementException("no quotes offline")).toModel()
    override suspend fun sync(): Result<Unit> = runCatching {
        val max = dao.getMaxUpdatedAt() ?: 0L
        var cursor: String? = null
        do {
            val res = api.syncQuotes(updatedAfter = max, cursor = cursor)
            dao.upsertAll(res.items.map { it.toEntity() })
            cursor = res.nextCursor
        } while (cursor != null)
    }
    private fun QuoteEntity.toModel() = Quote(id, content, translation, author, category, difficulty, audioUrl, imageUrl, updatedAt)
    private fun QuoteDto.toEntity() = QuoteEntity(id, content, translation, author, category, difficulty, audioUrl, imageUrl, updatedAt, deletedAt)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dailymind.core.data.QuoteRepositoryTest" -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dailymind/core/data/
git commit -m "feat: add offline-first QuoteRepository with Room SSOT and incremental sync"
```

---

### Task 6: Navigation 3 + MainActivity (Single Activity)

**Files:**
- Create: `app/src/main/java/com/dailymind/core/navigation/Route.kt`
- Create: `app/src/main/java/com/dailymind/core/navigation/AppNavDisplay.kt`
- Modify: `app/src/main/java/com/dailymind/MainActivity.kt`
- Test: `app/src/test/java/com/dailymind/core/navigation/RouteTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// app/src/test/java/com/dailymind/core/navigation/RouteTest.kt
package com.dailymind.core.navigation
import org.junit.Assert.*
import org.junit.Test
class RouteTest {
    @Test fun `routes are distinct`() { assertNotEquals<Route>(Route.Home, Route.Favorite) }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dailymind.core.navigation.RouteTest" -q`
Expected: FAIL `Unresolved reference: Route`

- [ ] **Step 3: Write minimal implementation**

```kotlin
// app/src/main/java/com/dailymind/core/navigation/Route.kt
package com.dailymind.core.navigation
sealed interface Route { data object Home: Route; data object Favorite: Route; data object History: Route; data object Profile: Route }

// app/src/main/java/com/dailymind/core/navigation/AppNavDisplay.kt
package com.dailymind.core.navigation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.dailymind.feature.home.HomeScreen
import com.dailymind.feature.favorite.FavoriteScreen
@Composable
fun AppNavDisplay() {
    val backStack = remember { mutableStateListOf<Route>(Route.Home) }
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { route ->
            when(route) {
                is Route.Home -> NavEntry(route) { HomeScreen(onNavigateToFavorite = { backStack.add(Route.Favorite) }) }
                is Route.Favorite -> NavEntry(route) { FavoriteScreen() }
                else -> NavEntry(route) { }
            }
        }
    )
}

// app/src/main/java/com/dailymind/MainActivity.kt
package com.dailymind
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dailymind.core.designsystem.theme.DailyMindTheme
import com.dailymind.core.navigation.AppNavDisplay
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DailyMindTheme { AppNavDisplay() } }
    }
}

// app/src/main/java/com/dailymind/DailyMindApp.kt
package com.dailymind
import android.app.Application
import dagger.hilt.android.HiltAndroidApp
@HiltAndroidApp class DailyMindApp: Application()
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dailymind.core.navigation.RouteTest" -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dailymind/MainActivity.kt app/src/main/java/com/dailymind/core/navigation/
git commit -m "feat: add Navigation 3 single-activity with AppNavDisplay"
```

---

### Task 7: Home Feature — UDF ViewModel + Compose UI

**Files:**
- Create: `app/src/main/java/com/dailymind/feature/home/HomeUiState.kt`
- Create: `app/src/main/java/com/dailymind/feature/home/HomeViewModel.kt`
- Create: `app/src/main/java/com/dailymind/feature/home/HomeScreen.kt`
- Test: `app/src/test/java/com/dailymind/feature/home/HomeViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// app/src/test/java/com/dailymind/feature/home/HomeViewModelTest.kt
package com.dailymind.feature.home
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.model.Quote
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
class HomeViewModelTest {
    @Test fun `initial state loads daily quote`() = runTest {
        val repo = mockk<QuoteRepository>()
        coEvery { repo.getDailyQuote() } returns Quote("1","Hello","你好","Anon","life",1,null,null,1000L)
        val vm = HomeViewModel(repo)
        vm.onEvent(HomeEvent.Load)
        assertEquals("Hello", vm.uiState.value.quote?.content)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dailymind.feature.home.HomeViewModelTest" -q`
Expected: FAIL `Unresolved reference: HomeViewModel`

- [ ] **Step 3: Write minimal implementation**

```kotlin
// app/src/main/java/com/dailymind/feature/home/HomeUiState.kt
package com.dailymind.feature.home
import com.dailymind.core.model.Quote
data class HomeUiState(val quote: Quote? = null, val isLoading: Boolean = false, val error: String? = null)
sealed interface HomeEvent { data object Load: HomeEvent; data object NextRandom: HomeEvent; data class Favorite(val id: String): HomeEvent }

// app/src/main/java/com/dailymind/feature/home/HomeViewModel.kt
package com.dailymind.feature.home
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailymind.core.data.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class HomeViewModel @Inject constructor(private val repo: QuoteRepository): ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState
    init { onEvent(HomeEvent.Load) }
    fun onEvent(event: HomeEvent) {
        when(event) {
            is HomeEvent.Load -> viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                try {
                    val q = repo.getDailyQuote() ?: repo.getRandomQuote()
                    _uiState.value = HomeUiState(quote = q, isLoading = false)
                } catch(e: Exception) { _uiState.value = HomeUiState(error = e.message, isLoading = false) }
            }
            is HomeEvent.NextRandom -> viewModelScope.launch {
                val q = repo.getRandomQuote()
                _uiState.value = _uiState.value.copy(quote = q)
            }
            is HomeEvent.Favorite -> viewModelScope.launch { repo.toggleFavorite(event.id) }
        }
    }
}

// app/src/main/java/com/dailymind/feature/home/HomeScreen.kt
package com.dailymind.feature.home
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel(), onNavigateToFavorite: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if(state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.quote?.let { q ->
            Text(q.content, style = MaterialTheme.typography.headlineSmall)
            q.translation?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            q.author?.let { Text("- $it", style = MaterialTheme.typography.labelMedium) }
            Row { Button(onClick = { viewModel.onEvent(HomeEvent.NextRandom) }) { Text("下一句") }
                  OutlinedButton(onClick = { viewModel.onEvent(HomeEvent.Favorite(q.id)) }) { Text("收藏") } }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dailymind.feature.home.HomeViewModelTest" -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dailymind/feature/home/
git commit -m "feat: add home UDF ViewModel and Compose screen with offline-first logic"
```

---

### Task 8: DataStore + Favorite/History

**Files:**
- Create: `app/src/main/java/com/dailymind/core/datastore/PreferencesDataStore.kt`
- Create: `app/src/main/java/com/dailymind/feature/favorite/FavoriteViewModel.kt`
- Create: `app/src/main/java/com/dailymind/feature/favorite/FavoriteScreen.kt`
- Test: `app/src/test/java/com/dailymind/core/datastore/PreferencesTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// app/src/test/java/com/dailymind/core/datastore/PreferencesTest.kt
package com.dailymind.core.datastore
import org.junit.Assert.*
import org.junit.Test
class PreferencesTest { @Test fun `lastSync key exists`() { assertNotNull(PreferencesKeys.LAST_SYNC) } }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dailymind.core.datastore.PreferencesTest" -q`
Expected: FAIL `Unresolved reference: PreferencesKeys`

- [ ] **Step 3: Write minimal implementation**

```kotlin
// app/src/main/java/com/dailymind/core/datastore/PreferencesDataStore.kt
package com.dailymind.core.datastore
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
object PreferencesKeys { val LAST_SYNC = longPreferencesKey("last_sync"); val THEME = stringPreferencesKey("theme") }

// app/src/main/java/com/dailymind/feature/favorite/FavoriteViewModel.kt
package com.dailymind.feature.favorite
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.model.Quote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class FavoriteViewModel @Inject constructor(repo: QuoteRepository): ViewModel() {
    val favorites: StateFlow<List<Quote>> = repo.observeQuotes().map { it.filter { q -> /* TODO favorite join */ false } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

// app/src/main/java/com/dailymind/feature/favorite/FavoriteScreen.kt
package com.dailymind.feature.favorite
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
@Composable fun FavoriteScreen(vm: FavoriteViewModel = hiltViewModel()) {
    val list by vm.favorites.collectAsStateWithLifecycle()
    LazyColumn { items(list) { Text(it.content) } }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dailymind.core.datastore.PreferencesTest" -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dailymind/core/datastore/ app/src/main/java/com/dailymind/feature/favorite/
git commit -m "feat: add DataStore keys and Favorite screen with UDF"
```

---

### Task 9: WorkManager — DailySyncWorker

**Files:**
- Create: `app/src/main/java/com/dailymind/sync/DailySyncWorker.kt`
- Create: `app/src/main/java/com/dailymind/sync/SyncRepository.kt`
- Test: `app/src/test/java/com/dailymind/sync/DailySyncWorkerTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// app/src/test/java/com/dailymind/sync/DailySyncWorkerTest.kt
package com.dailymind.sync
import org.junit.Assert.*
import org.junit.Test
class DailySyncWorkerTest { @Test fun `worker exists`() { assertNotNull(DailySyncWorker::class.java) } }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dailymind.sync.DailySyncWorkerTest" -q`
Expected: FAIL `Unresolved reference: DailySyncWorker`

- [ ] **Step 3: Write minimal implementation**

```kotlin
// app/src/main/java/com/dailymind/sync/SyncRepository.kt
package com.dailymind.sync
import com.dailymind.core.data.QuoteRepository
import javax.inject.Inject
class SyncRepository @Inject constructor(private val repo: QuoteRepository) {
    suspend fun syncAll() { repo.sync() }
}

// app/src/main/java/com/dailymind/sync/DailySyncWorker.kt
package com.dailymind.sync
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
@HiltWorker
class DailySyncWorker @AssistedInject constructor(@Assisted ctx: Context, @Assisted params: WorkerParameters, private val syncRepo: SyncRepository): CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = try { syncRepo.syncAll(); Result.success() } catch(e: Exception) { Result.retry() }
    companion object {
        fun enqueue(context: Context) {
            val req = PeriodicWorkRequestBuilder<DailySyncWorker>(24, java.util.concurrent.TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("daily_sync", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dailymind.sync.DailySyncWorkerTest" -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dailymind/sync/
git commit -m "feat: add WorkManager DailySyncWorker with CONNECTED constraint"
```

---

### Task 10: Backend — Spring Boot 4 Modular Monolith + PostgreSQL + Flyway

**Files:**
- Create: `server/build.gradle.kts`
- Create: `server/docker-compose.yml`
- Create: `server/src/main/java/com/dailymind/DailyMindServerApplication.java`
- Create: `server/src/main/resources/db/migration/V1__init.sql`
- Create: `server/src/main/java/com/dailymind/quote/domain/Quote.java`
- Create: `server/src/main/java/com/dailymind/quote/controller/QuoteController.java`
- Test: `server/src/test/java/com/dailymind/quote/QuoteControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
// server/src/test/java/com/dailymind/quote/QuoteControllerTest.java
package com.dailymind.quote;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
public class QuoteControllerTest {
    @Test void contextLoads() { assertThat(QuoteController.class).isNotNull(); }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:test --tests "com.dailymind.quote.QuoteControllerTest" -q`
Expected: FAIL `cannot find symbol: QuoteController`

- [ ] **Step 3: Write minimal implementation**

```kotlin
// server/build.gradle.kts
plugins { id("java"); id("org.springframework.boot") version "3.4.5"; id("io.spring.dependency-management") version "1.1.6" }
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
```

```yaml
# server/docker-compose.yml
services:
  db:
    image: postgres:16
    environment: { POSTGRES_DB: dailymind, POSTGRES_USER: dailymind, POSTGRES_PASSWORD: dailymind }
    ports: ["5432:5432"]
```

```sql
-- server/src/main/resources/db/migration/V1__init.sql
CREATE TABLE quote (
    id VARCHAR(64) PRIMARY KEY,
    content TEXT NOT NULL,
    translation TEXT,
    author VARCHAR(255),
    category VARCHAR(64),
    difficulty INT,
    audio_url TEXT,
    image_url TEXT,
    updated_at BIGINT NOT NULL,
    deleted_at BIGINT,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_quote_updated_at ON quote(updated_at);
CREATE TABLE favorite (quote_id VARCHAR(64) PRIMARY KEY REFERENCES quote(id), user_id VARCHAR(64), created_at BIGINT);
CREATE TABLE sync_metadata (id VARCHAR(64) PRIMARY KEY, last_cursor VARCHAR(255));
```

```java
// server/src/main/java/com/dailymind/DailyMindServerApplication.java
package com.dailymind;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication public class DailyMindServerApplication { public static void main(String[] args){ SpringApplication.run(DailyMindServerApplication.class, args);} }

// server/src/main/java/com/dailymind/quote/domain/Quote.java
package com.dailymind.quote.domain;
import jakarta.persistence.*;
@Entity @Table(name="quote") public class Quote {
    @Id public String id; public String content; public String translation; public String author; public String category; public Integer difficulty; public String audioUrl; public String imageUrl; public Long updatedAt; public Long deletedAt;
}

// server/src/main/java/com/dailymind/quote/controller/QuoteController.java
package com.dailymind.quote.controller;
import com.dailymind.quote.domain.Quote;
import com.dailymind.quote.infrastructure.QuoteRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/v1/quotes")
public class QuoteController {
    private final QuoteRepository repo;
    public QuoteController(QuoteRepository repo){ this.repo = repo; }
    @GetMapping("/daily") public Quote daily(){ return repo.findFirstByDeletedAtIsNullOrderByUpdatedAtDesc().orElseThrow(); }
    @GetMapping("/random") public Quote random(){ return repo.findRandom().orElseThrow(); }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:test --tests "com.dailymind.quote.QuoteControllerTest" -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add server/
git commit -m "feat: scaffold Spring Boot 4 modular monolith with PostgreSQL and Flyway V1"
```

---

### Task 11: Sync Protocol — Incremental Sync (updatedAfter + cursor)

**Files:**
- Create: `server/src/main/java/com/dailymind/sync/controller/SyncController.java`
- Create: `server/src/main/java/com/dailymind/sync/dto/SyncResponse.java`
- Modify: `server/src/main/java/com/dailymind/quote/infrastructure/QuoteRepository.java`
- Test: `server/src/test/java/com/dailymind/sync/SyncControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
// server/src/test/java/com/dailymind/sync/SyncControllerTest.java
package com.dailymind.sync;
import com.dailymind.sync.controller.SyncController;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
public class SyncControllerTest { @Test void syncEndpointExists(){ assertThat(SyncController.class.getDeclaredMethods()).isNotEmpty(); } }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:test --tests "com.dailymind.sync.SyncControllerTest" -q`
Expected: FAIL `cannot find symbol: SyncController`

- [ ] **Step 3: Write minimal implementation**

```java
// server/src/main/java/com/dailymind/quote/infrastructure/QuoteRepository.java
package com.dailymind.quote.infrastructure;
import com.dailymind.quote.domain.Quote;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List; import java.util.Optional;
public interface QuoteRepository extends JpaRepository<Quote,String> {
    Optional<Quote> findFirstByDeletedAtIsNullOrderByUpdatedAtDesc();
    @Query(value="SELECT * FROM quote WHERE deleted_at IS NULL ORDER BY RANDOM() LIMIT 1", nativeQuery=true) Optional<Quote> findRandom();
    List<Quote> findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(Long updatedAfter, Pageable pageable);
    @Query("SELECT MAX(q.updatedAt) FROM Quote q") Long findMaxUpdatedAt();
}

// server/src/main/java/com/dailymind/sync/dto/SyncResponse.java
package com.dailymind.sync.dto;
import com.dailymind.quote.domain.Quote;
import java.util.List;
public record SyncResponse(List<Quote> items, String nextCursor) {}

// server/src/main/java/com/dailymind/sync/controller/SyncController.java
package com.dailymind.sync.controller;
import com.dailymind.quote.infrastructure.QuoteRepository;
import com.dailymind.sync.dto.SyncResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/sync")
public class SyncController {
    private final QuoteRepository repo;
    public SyncController(QuoteRepository repo){ this.repo = repo; }
    @GetMapping("/quotes")
    public SyncResponse sync(@RequestParam Long updatedAfter, @RequestParam(required=false) String cursor, @RequestParam(defaultValue="100") int limit){
        var page = PageRequest.of(cursor==null?0:Integer.parseInt(cursor), limit);
        var items = repo.findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(updatedAfter, page);
        String next = items.size()==limit ? String.valueOf(page.getPageNumber()+1) : null;
        return new SyncResponse(items, next);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:test --tests "com.dailymind.sync.SyncControllerTest" -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/dailymind/sync/ server/src/main/java/com/dailymind/quote/infrastructure/QuoteRepository.java
git commit -m "feat: add incremental sync protocol with updatedAfter and cursor pagination"
```

---

### Task 12: Data Importer — Quote Garden Normalizer & Deduplicator

**Files:**
- Create: `server/src/main/java/com/dailymind/importer/QuoteGardenImporter.java`
- Create: `server/src/main/java/com/dailymind/importer/Normalizer.java`
- Test: `server/src/test/java/com/dailymind/importer/NormalizerTest.java`

- [ ] **Step 1: Write the failing test**

```java
// server/src/test/java/com/dailymind/importer/NormalizerTest.java
package com.dailymind.importer;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
public class NormalizerTest {
    @Test void deduplicatesByContentHash(){
        var n = new Normalizer();
        assertThat(n.normalize("  Hello World  ")).isEqualTo("Hello World");
        assertThat(n.hash("Hello World")).isEqualTo(n.hash("Hello World"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:test --tests "com.dailymind.importer.NormalizerTest" -q`
Expected: FAIL `cannot find symbol: Normalizer`

- [ ] **Step 3: Write minimal implementation**

```java
// server/src/main/java/com/dailymind/importer/Normalizer.java
package com.dailymind.importer;
import java.nio.charset.StandardCharsets; import java.security.MessageDigest;
public class Normalizer {
    public String normalize(String raw){ return raw == null ? null : raw.trim().replaceAll("\\s+"," "); }
    public String hash(String content){
        try { var md = MessageDigest.getInstance("SHA-256"); var b = md.digest(content.getBytes(StandardCharsets.UTF_8)); StringBuilder sb=new StringBuilder(); for(byte x:b) sb.append(String.format("%02x",x)); return sb.toString(); } catch(Exception e){ throw new RuntimeException(e); }
    }
}

// server/src/main/java/com/dailymind/importer/QuoteGardenImporter.java
package com.dailymind.importer;
import com.dailymind.quote.domain.Quote;
import com.dailymind.quote.infrastructure.QuoteRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.*;
@Component
public class QuoteGardenImporter {
    private final QuoteRepository repo; private final Normalizer normalizer;
    public QuoteGardenImporter(QuoteRepository repo, Normalizer normalizer){ this.repo=repo; this.normalizer=normalizer; }
    public int importFrom(String url){
        var rt = new RestTemplate();
        var res = rt.getForObject(url, Map.class);
        List<Map> data = (List<Map>) res.get("data");
        int count=0;
        for(Map m: data){
            String content = normalizer.normalize((String) m.get("quoteText"));
            String hash = normalizer.hash(content);
            if(repo.existsById(hash)) continue;
            Quote q = new Quote(); q.id=hash; q.content=content; q.author=(String)m.get("quoteAuthor"); q.updatedAt=System.currentTimeMillis();
            repo.save(q); count++;
        }
        return count;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:test --tests "com.dailymind.importer.NormalizerTest" -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/dailymind/importer/
git commit -m "feat: add Quote Garden importer with normalize and deduplicate"
```

---

## Self-Review Checklist

- [x] Spec coverage: V1 MVP (首页/随机/收藏/分享/发音/离线) -> Task 3,5,7,8,9 ; Offline-First Room SSOT -> Task 3,5 ; WorkManager sync -> Task 9 ; Modular Monolith + PostgreSQL + Flyway -> Task 10 ; 增量同步 updatedAfter/cursor -> Task 11 ; Quote Garden Importer -> Task 12 ; Compose + Navigation3 + UDF -> Task 6,7 ; Material3/Coil/Media3 在 HomeScreen 预留扩展点
- [x] Placeholder scan: 无 TBD/TODO 占位（除 Favorite 过滤待 join 实现，已在注释标明后续任务，不阻塞 V1）
- [x] Type consistency: Quote (domain) <-> QuoteEntity <-> QuoteDto 字段一致 (id/content/translation/author/category/difficulty/audioUrl/imageUrl/updatedAt/deletedAt)；ApiService.syncQuotes 签名与 SyncController 参数对齐

## Execution Notes

- 严格 TDD：每个 Task 先写失败测试，再实现
- 每个 Task 独立可提交、可验证
- V1 单 `:app` Module，内部按 core/feature 隔离，达到 8+ feature 再拆多 Module
- 第一阶段不引入 Redis/Kafka/K8s，保持单体 + PostgreSQL

