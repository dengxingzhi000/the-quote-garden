# Beta Release Build Infrastructure — Design Spec

**Date:** 2026-09-07
**Status:** Approved (verbal)
**Goal:** Ship the first beta (`0.1.0-beta.1`) of *The Quote Garden* Android app as a signed, minified APK ready for distribution.

## Constraints
- Existing debug build pipeline stays intact; no regression in unit tests.
- Keystore and passwords must not be committed to git.
- `local.properties` is the agreed secrets sink (already gitignored per AGENTS.md).
- App icon (PNG in `mipmap-xxxhdpi/ic_launcher.png`), app name (`The Quote Garden`), seed import, browsing-mode toggle — all already in place from prior work. No behavior changes.

## Architecture

### Signing
- New `beta` keystore generated via `keytool`:
  - File: `keystore/dailymind-beta.jks` (project root)
  - Alias: `dailymind-beta`
  - Algorithm: RSA 2048, validity 10 000 days
- Secrets stored in `local.properties`:
  - `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- `app/build.gradle.kts` reads them via `providers.gradleProperty()` — no secrets in any committed file.
- `keystore/` added to `.gitignore`.

### Build config (`app/build.gradle.kts`)
```kotlin
defaultConfig {
    minSdk = 24; targetSdk = 35
    versionCode = 1
    versionName = "0.1.0-beta.1"
}
signingConfigs {
    create("beta") { /* read 4 props */ }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("beta")
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```

### ProGuard / R8 rules (`app/proguard-rules.pro`)
- Keep `kotlinx.serialization` `@Serializable` companions (already covered by bundled rules in `kotlinx-serialization-json`, but explicit `-keepclassmembers @kotlinx.serialization.Serializable class * { ... }` for safety).
- Keep Hilt-generated classes (default rules cover).
- Keep Room generated impls (default rules cover; verify no `@TypeConverter` reflection breakage).
- Keep `WorkManager` initializer and `HiltWorkerFactory`.

### Output
- `app/build/outputs/apk/release/app-release.apk` — V2-signed, R8-minified, resource-shrunk.

## Data Flow (unchanged)
1. App launch → `DailyMindApp.onCreate` enqueues 2 WorkManager jobs + seed import.
2. HomeScreen reads from local Room (seeded + server-synced).

## Error Handling
- Keystore missing or password wrong → `assembleRelease` fails loudly with a clear Gradle error (build-time, not runtime).
- `local.properties` missing entries → same.

## Testing
- Unit tests run on `debug` variant — no change.
- After `assembleRelease`, manually verify with `apksigner verify` and `aapt dump badging`.

## Out of Scope
- Play Store upload / Play App Signing.
- Per-channel signing (only one keystore for now).
- Crash reporting (Sentry / Firebase Crashlytics) — follow-up.
- Beta distribution platform (Firebase App Distribution, etc.) — follow-up.

## Decisions
- Build type strategy: modify existing `release` (user choice A).
- Signing: generate new keystore (user choice A).
- Version: `0.1.0-beta.1` / code 1 (user choice A).
