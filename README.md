# The Quote Garden

Offline-first Android daily-quote app with a Spring Boot backend.

> Beta release: [v0.1.0-beta.1](https://github.com/dengxingzhi000/the-quote-garden/releases/tag/v0.1.0-beta.1)

## What it is

A quiet companion app that shows you one quote a day — and lets you browse more when you want. Everything works offline from first launch; the network is only used to fetch updates in the background.

- **Daily quote** — pinned per day, with a fresh one tomorrow
- **Random browse** — tap *Next* to see another; the chrome fades away so you can read
- **Favorites** — saved locally, no account, no cloud sync
- **Editorial design** — quiet typography, dark theme required

## Tech stack

**App**
- Kotlin 2.3.20, AGP 9.3.0, Gradle 9.7.1
- Jetpack Compose (BOM 2025.10.01), Navigation 3
- Hilt 2.60.1, Room 2.8.4, WorkManager
- Retrofit 2.11.0, kotlinx.serialization 1.8.1
- Min SDK 24, Target SDK 35, Core library desugaring enabled

**Server**
- Spring Boot 3.4.5
- PostgreSQL 18 with Flyway 11.20.3 migrations
- JPA, jsoup (HTML parsing)

## Repo layout

```
app/                  Android client (the only Gradle module in the root build)
server/               Spring Boot backend (standalone Gradle project)
design-system/        MASTER.md + per-page design overrides
docs/superpowers/     Plans and specs (read these before non-trivial work)
.github/              Workflows, issue/PR templates, Dependabot config
```

The root `settings.gradle.kts` includes only `:app`. The server has its own wrapper. From the repo root, use `server/gradlew.bat` (Windows) or `server/gradlew` (Unix) — gradle will pick up the root settings and the `:app` module.

## Build

### App (debug)

```
server\gradlew.bat :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### App (release / beta)

The release build is signed with a keystore configured via `local.properties`. Generate one with:

```
keytool -genkeypair -v -keystore keystore/dailymind-beta.jks \
  -alias dailymind-beta -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=The Quote Garden, OU=Mobile, O=DailyMind, L=City, ST=State, C=CN"
```

Then add to `local.properties`:

```
KEYSTORE_PATH=keystore/dailymind-beta.jks
KEYSTORE_PASSWORD=<store-password>
KEY_ALIAS=dailymind-beta
KEY_PASSWORD=<key-password>
```

Build:

```
server\gradlew.bat :app:assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Server

```
cd server
.\gradlew.bat test        # unit tests
.\gradlew.bat bootRun     # run locally against local Postgres
```

`server/src/main/resources/application.yml` defaults to a remote Postgres at `192.168.80.155:5432`. Override with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`. `SERVER_PORT` defaults to `8080`.

## Tests

```
server\gradlew.bat :app:test        # Android unit tests (38 tests, JUnit4 + MockK)
cd server && .\gradlew.bat test     # Server tests (JUnit Jupiter + AssertJ)
```

The server build requires `tasks.test { useJUnitPlatform() }` (already configured).

## Signing secrets

`local.properties` and `keystore/` are gitignored. Do not commit. For CI, inject the four properties (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) as repository secrets.

## Known issues

- 53 of the seeded Chinese translations are double-encoded in the upstream database (recoverable only from the original source). The English content is fine.
- WorkManager `OneTimeWorkRequest` may take 100ms–several seconds to dispatch on first launch, so the home screen can briefly show a loading state before the seed import completes.

## License

[MIT](LICENSE)
