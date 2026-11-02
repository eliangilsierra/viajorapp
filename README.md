# Viajorapp

**Plan smarter. Travel further.** Open-source, offline-first travel planning for Android and iOS. Your trips stay on your device; maps, AI and sync are optional and off by default.

> **Status: early foundation (0.1).** The Android app builds and opens to a welcome screen. Trip, itinerary, map, import, AI and sync features are **not available yet**: some of their tested building blocks exist in the code but no screen uses them. iOS has never been compiled or run. See [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md) for exactly what works and what does not.

## What it is meant to be

- Trips, days and an itinerary stored on the device, usable without a connection or an account.
- No backend, no mandatory sign-in, no subscription, no analytics or telemetry.
- Optional, user-controlled extras: map tiles from a provider you can see, a bring-your-own-key AI assistant that only proposes changes you review, and optional encrypted sync through a folder you choose.
- Clear labels: a straight-line travel estimate is never shown as a routed path.

The product plan is in [docs/product/PRD.md](docs/product/PRD.md); the decisions behind the architecture are in [docs/decisions](docs/decisions/README.md).

## Getting started

Prerequisites: JDK 17 and the Android SDK (platform 37; the Android Gradle Plugin downloads it if the licences are accepted). On Windows, write the SDK path in `local.properties` with forward slashes.

```properties
sdk.dir=C:/Users/you/AppData/Local/Android/Sdk
```

Build and test (these commands were run on Windows 11 with JDK 17):

```bash
./gradlew :domain:allTests :data:allTests :designsystem:allTests
./gradlew :data:verifyCommonMainViajorDatabaseMigration
./gradlew :androidApp:lintDebug
./gradlew :androidApp:assembleDebug
```

The debug APK is written to `androidApp/build/outputs/apk/debug/`. More detail is in [docs/how-to/build-and-test.md](docs/how-to/build-and-test.md). The app has not been run on an emulator or device in the maintainers' environment yet. iOS targets are declared but need macOS and Xcode, which have not been available.

## Project layout

| Module | Contents |
| --- | --- |
| `domain/` | Pure Kotlin rules: ordering keys, time zones, trip days, CSV, sync merge, AI proposal validation, secret redaction, offline estimates, money |
| `data/` | SQLDelight schema, migrations and queries |
| `designsystem/` | Waypoint colour tokens and theme |
| `ui/` | Screens and the map surface |
| `androidApp/` | The Android application shell |
| `design/brand/` | Brand sources (SVG) and the concept gallery |
| `docs/` | Product, architecture decisions, research and guides |

## Contributing, security and privacy

- [CONTRIBUTING.md](CONTRIBUTING.md): how to propose and submit changes.
- [SECURITY.md](SECURITY.md): how to report a vulnerability.
- [PRIVACY.md](PRIVACY.md): what the app does with your data today and what is planned.
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)

## Documentation

Start at [docs/README.md](docs/README.md): tutorials, how-to guides, reference, explanation, architecture, decisions, UX design and research.

## Branches

| Branch | Purpose | How it changes |
| --- | --- | --- |
| `develop` | Integration of all development; the default branch | Squash-merged Pull Requests from `feature/`, `bugfix/`, `refactor/`, `chore/` and `docs/` branches |
| `main` | Released code only | A human merges the release Pull Request `develop` → `main` with a merge commit |

Both branches are protected: no direct pushes, no force pushes, no deletion, and the checks
`pr-conventions`, `ci` and `secret-scan` must pass.

## Licence

[MIT](LICENSE).
