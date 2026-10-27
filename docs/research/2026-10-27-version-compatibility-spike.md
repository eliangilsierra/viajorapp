# Version compatibility spike (2026-10-27)

Goal (ADR-0001, task F3): prove that the pinned toolchain compiles together on Android before the
versions are treated as fixed.

## Result

**Passed on Android and the JVM. iOS not verified (no macOS).**

| Component | Version | Evidence |
| --- | --- | --- |
| Gradle (wrapper, checksum pinned) | 9.8.2 | `./gradlew` runs |
| Android Gradle Plugin | 9.4.1 | `:androidApp:assembleDebug` succeeded |
| Kotlin | 2.4.21 | all modules compile |
| Compose Multiplatform | 1.12.2 (material3 1.9.0, resources 1.12.2) | `:ui:compileAndroidMain`, debug APK |
| maplibre-compose | 0.20.0 + OpenGL Android runtime | compiles and packages; **not run on a device** |
| SQLDelight | 2.4.1 | `:data:allTests`: 3 tests, 0 failures |
| JDK | 17.0.10 | host JDK used |

Commands run from `D:\Git\viajorapp`:

```bash
./gradlew :domain:allTests :data:allTests    # 19 + 3 tests passed
./gradlew :androidApp:assembleDebug           # androidApp-debug.apk, about 53 MB
./gradlew :androidApp:lintDebug               # passed with 1 warning (OldTargetApi: targetSdk 36 < 37)
```

## Findings that changed the plan

1. **`compileSdk` must be 37.** Compose 1.12.1 and maplibre-compose 0.20.0 declare AAR metadata
   requiring it. The local SDK only had platforms up to 36.1; AGP downloaded platform `android-37.0`
   into `%LOCALAPPDATA%\Android\Sdk` (licenses were already accepted). CI runners download it the same
   way. `targetSdk` stays 36 until 37 behavior changes are reviewed (lint warns).
2. **`local.properties` must use forward slashes** (`sdk.dir=C:/Users/...`) on Windows; single
   backslashes are dropped by the properties format and break the Android host-test build with a
   confusing `IOException`.
3. **Repository content filters.** `settings.gradle.kts` restricts the Google repository to
   `com.android`, `com.google` and `androidx` groups, and fails on project-level repositories, to limit
   dependency-confusion risk.
4. **The debug APK is large (about 53 MB)** because MapLibre ships native libraries for several ABIs.
   Release builds should use App Bundles (per-ABI splits); size is tracked, not yet optimized.
5. **Hand-checked API:** `MaplibreMap` takes a `MapState` from `rememberMapState(baseStyle = ...)`;
   there is no `baseStyle` parameter on the composable. Imports are `org.maplibre.compose.map.*` and
   `org.maplibre.compose.style.BaseStyle`.

## Not verified

- Running the app on an emulator or device (no AVD or system image installed; `adb` not on `PATH`).
- Any iOS compilation, simulator run, framework linking or Keychain behavior.
- `verifyMigrations` has no baseline schema yet; `generateSchema` output is added with schema v2.
- Release (R8-minified) build.
