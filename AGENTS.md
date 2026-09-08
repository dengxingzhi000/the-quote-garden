# AGENTS.md — DailyMind

DailyMind is an offline-first Android daily-quote app with a Spring Boot backend. Two independent Gradle projects share this workspace.

## Project layout

- `app/` — Android client. **The only Gradle module included by the root `settings.gradle.kts`.** The root has no wrapper, so build it from the repo root with untracked `server/gradlew.bat`.
- `server/` — Spring Boot 3.4.5 backend. **Not** a Gradle subproject; it has its own `settings.gradle.kts`, `gradlew`, and wrapper. Build it from `server/` with `.\gradlew.bat ...`.
- `design-system/` — `MASTER.md` (default theme) plus per-page overrides in `pages/`.
- `docs/superpowers/` — Implementation plans and design specs (read these before significant work).

## Toolchain (pinned)

- JDK 21 Temurin, pinned via `gradle.properties` to `C:/Users/Deng/.jdks/temurin-21`.
- Gradle 9.7.1, AGP 9.3.0, Kotlin 2.3.20, KSP 2.3.11.
- Compose BOM 2025.10.01, Navigation 3, Hilt 2.60.1, Room 2.8.4, Retrofit 2.11.0, kotlinx.serialization 1.8.1.
- App: minSdk 24, targetSdk 35, compileSdk 36. Core library desugaring is enabled (required for `java.time` on API 24–25).
- Server: Spring Boot 3.4.5, Flyway 11.20.3 (`flyway-core` plus `flyway-database-postgresql`), PostgreSQL JDBC. The active remote database is PostgreSQL 18; the local Compose database remains PostgreSQL 16.

## Common commands

- The repo root has no wrapper; run Android commands as `server/gradlew.bat ...`. That server wrapper is untracked.
- App unit tests: `server/gradlew.bat :app:test`
- Focused Android unit test: `server/gradlew.bat :app:testDebugUnitTest --tests "com.dailymind.sync.DailySyncWorkerTest"`
- Do not pass `--tests` to aggregate `:app:test`; it rejects that option.
- Server tests from `server/`: `.\gradlew.bat test`
- Focused server test from `server/`: `.\gradlew.bat test --tests "com.dailymind.importer.NormalizerTest"`
- Server run/build from `server/`: `.\gradlew.bat bootRun`, `.\gradlew.bat build`
- Active server database: remote PostgreSQL 18 `quote_garden` at `192.168.80.152:5432`.
- Local database option: `cd server && docker compose up -d db` — PostgreSQL 16 on `localhost:5432`, db/user/pass all `dailymind`. It does not match the active remote default.
- To switch databases, set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`; `SERVER_PORT` defaults to `8080`.
- If `bootRun` reports port `8080` in use, check for an orphaned `DailyMindServerApplication` Java process before changing configuration.

## Architecture

**App (offline-first).** Room is the single source of truth. Repository exposes `Flow`; ViewModel holds `StateFlow` (UDF). WorkManager drives daily sync — `DailySyncWorker.enqueue` is called from `DailyMindApp.onCreate`. Hilt for DI. Retrofit + kotlinx.serialization for the network layer. `Application` uses `HiltWorkerFactory` to inject workers.

Package layout: `core/{common,data,database,datastore,designsystem,model,navigation,network}` + `feature/{home,favorite}` + `sync/`.

**Server (modular monolith).** Package layout: `quote/{controller,domain,infrastructure}`, `sync/{controller,dto}`, `importer/`. REST surface:

- `GET /api/v1/quotes/daily`
- `GET /api/v1/quotes/random`
- `GET /api/v1/sync/quotes?updatedAfter={epochMillis}&cursor={page}&limit={n}`

`updatedAfter` is epoch milliseconds and is required. `cursor` is the next page index as a stringified int, or null for the first page; the response's `nextCursor` field is null on the last page. An empty `quote` table makes `/daily` and `/random` throw; use the sync endpoint for a database health check. `QuoteGardenImporter.importFrom(url)` currently has no caller, so the remote database named `quote_garden` is the application database, not a live import source. Spring Data JPA is configured with `ddl-auto: validate`, so all schema changes must come through Flyway scripts under `server/src/main/resources/db/migration/`.

## Design system

`design-system/MASTER.md` defines a playful teal kids theme (Baloo 2 / Comic Neue). It is **overridden for Home, Favorite, and Theme** by `docs/superpowers/specs/2026-09-03-dailymind-editorial-redesign-design.md` (quiet-luxury editorial typography, dark scheme required). When touching those screens, follow the spec — do not reintroduce the teal palette.

Before designing a new screen, check `design-system/pages/<page>.md` first; if present, it overrides `MASTER.md`.

## Working-tree state

`git status` shows extensive uncommitted changes (editorial redesign in progress) plus untracked IDE/build artifacts: `local.properties`, `build/`, `app/build/`, `.gradle/`, `.idea/`, and on the server side, `server/build/`, `server/.gradle/`, `server/.idea/`, `server/gradle/`, `server/gradlew*`, `server/settings.gradle.kts`.

There is **no root `.gitignore`**, so these are not auto-excluded. Do not commit `local.properties`, `*.iml`, `build/`, `.gradle/`, or `.idea/` unless explicitly intended.

The untracked `server/src/main/resources/application.yml` currently holds the active remote-database credentials. Do not commit that file or paste its password into other tracked files.

The working-copy Android build currently fails while compiling `HomeScreen.kt` because delegated `state.quote` and `state.error` properties cannot be smart-cast. Server-side `test` and remote `bootRun` have been verified separately.

## Plans and specs

- `docs/superpowers/plans/2026-09-02-dailymind-v1-mvp.md` — v1 MVP implementation plan with file structure and a checkbox-tracked task list.
- `docs/superpowers/plans/2026-09-03-dailymind-editorial-redesign.md` — editorial redesign plan.
- `docs/superpowers/specs/2026-09-03-dailymind-editorial-redesign-design.md` — design spec that overrides MASTER.md for Home/Favorite/Theme.

When a request matches a plan, follow that plan's task list and update its `- [ ]` checkboxes as you go. The MVP plan's `./gradlew :server:test` commands are stale because the server is standalone; use the server wrapper instead. New code follows task-driven TDD per the MVP plan: failing test first, then implementation.

## Tests

- App: JUnit4 + MockK + kotlinx-coroutines-test + OkHttp MockWebServer + Room testing. Tests now cover core, feature, and sync code, not only the original smoke tests.
- Server: JUnit Jupiter + AssertJ. `tasks.test { useJUnitPlatform() }` is required; without it, Gradle reports no discoverable tests.
