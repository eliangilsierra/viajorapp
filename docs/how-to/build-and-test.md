# Build and test Viajorapp

How to build the Android app and run every check that exists. All commands run from the repository root. They were run on Windows 11 with JDK 17.

## Prerequisites

- JDK 17 (`java -version`).
- Android SDK with platform 37. The Android Gradle Plugin downloads missing platforms if licences are accepted.
- `local.properties` with `sdk.dir`, using forward slashes on Windows:

  ```properties
  sdk.dir=C:/Users/you/AppData/Local/Android/Sdk
  ```

The Gradle wrapper (9.8.2, checksum pinned) downloads Gradle on first use. On Linux or macOS make sure `gradlew` is executable.

## Run the checks

```bash
./gradlew :domain:allTests :data:allTests :designsystem:allTests
./gradlew :data:verifyCommonMainViajorDatabaseMigration
./gradlew :androidApp:lintDebug
./gradlew :androidApp:assembleDebug
```

Expected: every command ends with `BUILD SUCCESSFUL`. Lint reports one known warning (`targetSdk` 36 is not the latest). Test reports are under `<module>/build/reports/tests/`.

CI (`.github/workflows/ci.yml`) runs `:domain:allTests`, lint and `assembleDebug` only. The `:data` and `:designsystem` tests and the migration verification are not in CI yet, so run them locally before opening a PR.

## Troubleshooting

- `IOException` while compiling host tests on Windows: the `sdk.dir` line in `local.properties` has backslashes. Use forward slashes.
- `compileSdk` errors mentioning platform 37: accept the Android SDK licences and rerun so Gradle can download it.
- iOS: the iOS targets are declared, but Kotlin/Native iOS builds need macOS and Xcode. They have not been built or run by the maintainers.

## Not covered here

Running the app on an emulator or device, the release (R8) build, and anything on iOS have not been verified.
