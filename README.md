<div align="center">

# The Quote Garden

### An offline-first Android app that shows you one quote a day — plus a Spring Boot backend.

**Kotlin + Jetpack Compose client, offline-first from first launch.
The network is only used to fetch updates in the background.**

[![Release](https://img.shields.io/github/v/release/dengxingzhi000/the-quote-garden?labelColor=black&style=flat-square)](https://github.com/dengxingzhi000/the-quote-garden/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white&style=flat-square)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/license-MIT-green?style=flat-square)](./LICENSE)

[Features](#features) · [Build](#build) · [Tests](#tests) · [Structure](#repo-layout) · [Known issues](#known-issues)

<code>server\gradlew.bat :app:assembleDebug</code>

</div>

> [!IMPORTANT]
> **Use `server\gradlew.bat` to build the Android app.** There is no Gradle wrapper
> at the repository root — only `server/` has one. The root
> `settings.gradle.kts` includes just `:app`, so running the _server's_ wrapper
> from the _root_ directory is what makes Gradle resolve `:app`. Confusing, but
> correct.
>
> The server is a separate Gradle project: `cd server` first, then use its own
> wrapper for server tasks.

> [!NOTE]
> `server/src/main/resources/application.yml` points at a **remote Postgres at
> `192.168.80.155:5432`**. That is a private LAN address baked into the default
> config. Override `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` /
> `SPRING_DATASOURCE_PASSWORD` to point anywhere else.

---

A quiet companion app: one quote a day, pinned, with a fresh one tomorrow. Tap
_Next_ for a random one and the chrome fades away so you can actually read. Favorites
stay local — no account, no cloud sync, and **everything works offline from first
launch**.

## Features

- **Daily quote** — pinned per day, with a fresh one tomorrow
- **Random browse** — tap _Next_ to see another; the chrome fades away so you can read
- **Favorites** — saved locally, no account, no cloud sync
- **Editorial design** — quiet typography, dark theme required

## Tech stack

**App**

- Kotlin 2.3.20, AGP 9.3.0, Gradle 9.7.1
- Jetpack Compose (BOM 2025.10.01), Navigation 3
- Hilt 2.60.1, Room 2.8.4, WorkManager
- Retrofit 2.11.0, kotlinx.serialization 1.8.1
- Min SDK 24, Target SDK 35, core library desugaring enabled

**Server**

- Spring Boot 3.4.5
- PostgreSQL 18 with Flyway 11.20.3 migrations
- JPA, jsoup (HTML parsing)

## Repo layout

```text
app/                  Android client (the only Gradle module in the root build)
server/               Spring Boot backend (standalone Gradle project — has the wrapper)
design-system/        MASTER.md + per-page design overrides
docs/superpowers/     Plans and specs (read these before non-trivial work)
tools/                Helper scripts
.github/              Workflows (Android CI), issue/PR templates, Dependabot config
render.yaml           Render.com deployment config for the server
```

The root `settings.gradle.kts` includes only `:app`; the server is independent and
has its own wrapper. From the repo root use `server/gradlew.bat` (Windows) or
`server/gradlew` (Unix) — Gradle picks up the root settings and resolves `:app`.
For server work, `cd server` first.

## Build

### App (debug)

```bash
server\gradlew.bat :app:assembleDebug      # Windows
server/gradlew :app:assembleDebug          # Unix
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### App (release / beta)

The release build is signed with a keystore configured via `local.properties`.
Generate one with:

```bash
keytool -genkeypair -v -keystore keystore/dailymind-beta.jks \
  -alias dailymind-beta -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=The Quote Garden, OU=Mobile, O=DailyMind, L=City, ST=State, C=CN"
```

Then add to `local.properties`:

```properties
KEYSTORE_PATH=keystore/dailymind-beta.jks
KEYSTORE_PASSWORD=<store-password>
KEY_ALIAS=dailymind-beta
KEY_PASSWORD=<key-password>
```

```bash
server\gradlew.bat :app:assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Server

```bash
cd server
./gradlew.bat test        # unit tests
./gradlew.bat bootRun     # run locally against local Postgres
```

`SERVER_PORT` defaults to `8080`. The datasource is overridden with
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD`.

## Tests

```bash
server\gradlew.bat :app:test        # Android unit tests (38 tests, JUnit4 + MockK)
cd server && ./gradlew.bat test     # Server tests (JUnit Jupiter + AssertJ)
```

The server build requires `tasks.test { useJUnitPlatform() }` (already configured).

> [!NOTE]
> CI covers the **Android app only** — `.github/workflows/android-ci.yml` is the
> sole workflow in the repository, so server tests are not run automatically.

## Signing secrets

`local.properties` and `keystore/` are gitignored. **Do not commit them.** For CI,
inject the four properties (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`) as repository secrets.

## Known issues

- **53 of the seeded Chinese translations are double-encoded** in the upstream
  database (recoverable only from the original source). The English content is fine.
- **WorkManager `OneTimeWorkRequest` may take 100ms–several seconds to dispatch on
  first launch**, so the home screen can briefly show a loading state before the
  seed import completes.
- The default datasource points at `192.168.80.155:5432` — see the note at the top.

## License

[MIT](./LICENSE)
