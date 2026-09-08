# DailyMind v1.0 MVP Implementation Plan

> Reference plan that the team uses for the offline-first MVP slice.
> For the active per-screen redesign work, see `2026-09-03-dailymind-editorial-redesign.md` instead.

## Goal

Ship a single daily-quote + favourite experience backed by a synchronising
backend, offline-first, with a backend modular monolith in Spring Boot 4 +
PostgreSQL.

## Architecture

- Android `:app` module, feature-based packages (`core/*`, `feature/*`, `sync/*`).
- Room is the single source of truth. The repository exposes `Flow`s; the
  ViewModels hold `StateFlow` (UDF). WorkManager drives the daily sync;
  `DailySyncWorker.enqueue` is called from `DailyMindApp.onCreate`.
- Hilt DI across the app, including `HiltWorkerFactory` for `WorkManager`.
- Retrofit + kotlinx.serialization for the network layer.
- Server is a Spring Boot 4 modular monolith with Flyway-managed
  PostgreSQL. Schema changes go through `server/src/main/resources/db/migration/`
  scripts (`ddl-auto: validate`).

## REST surface

- `GET /api/v1/quotes/daily`
- `GET /api/v1/quotes/random`
- `GET /api/v1/sync/quotes?updatedAfter={epochMillis}&cursor={page}&limit={n}`

`updatedAfter` is required (epoch millis). `cursor` is the next page index
as a stringified int, or `null` for the first page; the response's
`nextCursor` is `null` on the last page.

## Tech stack

Kotlin + Compose + Navigation 3 + ViewModel/StateFlow/UDF + Hilt + Coroutines/Flow + Room + DataStore + Retrofit/OkHttp + Kotlin Serialization + WorkManager + Media3 + Coil. Backend: Java 21 + Spring Boot 4 + PostgreSQL + Flyway + Spring Security + OpenAPI. Gradle Kotlin DSL + Version Catalog + Compose BOM.

## File layout (`:app`)

```
app/
  build.gradle.kts
  src/main/java/com/dailymind/
    DailyMindApp.kt
    MainActivity.kt
    core/
      common/Result.kt
      common/DispatchersProvider.kt
      designsystem/theme/Theme.kt
      model/Quote.kt
      database/DailyMindDatabase.kt
      database/entity/QuoteEntity.kt
      database/entity/FavoriteEntity.kt
      database/entity/HistoryEntity.kt
      database/dao/QuoteDao.kt
      database/dao/HistoryDao.kt
      database/di/DatabaseModule.kt
      datastore/PreferencesDataStore.kt
      datastore/di/DataStoreModule.kt
      network/ApiService.kt
      network/dto/QuoteDto.kt
      network/di/NetworkModule.kt
      navigation/AppNavDisplay.kt
    feature/
      home/HomeUiState.kt
      home/HomeViewModel.kt
      home/HomeScreen.kt
      favorite/FavoriteViewModel.kt
      favorite/FavoriteScreen.kt
    sync/SyncRepository.kt
    sync/DailySyncWorker.kt
```

## File layout (server)

```
server/
  build.gradle.kts
  docker-compose.yml
  src/main/
    java/com/dailymind/
      DailyMindServerApplication.java
      common/ApiResponse.java
      quote/controller/QuoteController.java
      quote/application/QuoteService.java
      quote/domain/Quote.java
      quote/infrastructure/QuoteRepository.java
      sync/controller/SyncController.java
      importer/QuoteGardenImporter.java
```

## Tasks

- [x] `core/common` Result + Dispatchers
- [x] `core/model` Quote + Room entities + DAOs
- [x] `core/database/DailyMindDatabase` + Hilt module
- [x] `core/datastore` PreferencesDataStore + DataStoreModule
- [x] `core/network` Retrofit ApiService + DTO + NetworkModule (kotlinx.serialization)
- [x] `core/navigation` AppNavDisplay + Route sealed
- [x] `feature/home` HomeUiState / ViewModel / Screen (Compose)
- [x] `feature/favorite` FavoriteViewModel / Screen
- [x] `sync/DailySyncWorker` + WorkManager wiring from `DailyMindApp.onCreate`
- [x] `server` Spring Boot 4 project with Flyway, `/quotes/{daily,random}` and `/sync/quotes`
- [x] Tests: app JUnit4/MockK, server JUnit Jupiter