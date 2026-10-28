# Project status

Last updated: 2026-10-27. Evidence for every "verified" statement is in
[the compatibility spike](research/2026-10-27-version-compatibility-spike.md) or the named command.

## Summary

Foundation phase. Product documents, architecture decisions and a building Android shell exist. **No
user-facing feature is implemented**: the app launches to a static welcome screen (never run on a device
or emulator in this environment). Everything below is merged into `develop` (PRs #2, #5, #7, #16 to #23). Tested building blocks exist in `:domain` and `:data` (time zones, trip days, CSV, sync merge, AI proposal validation, secret redaction, offline tile estimate, money, schema v2), but **none of them is wired to a screen yet**.

## Verified (commands run on Windows 11, JDK 17)

| Check | Result |
| --- | --- |
| `./gradlew :domain:allTests` | 94 tests passed (ordering keys, overlap, distance estimate, tile sources, time zones and DST, trip days, CSV, sync merge, AI proposals, secret redaction, offline estimate, money) |
| `./gradlew :data:allTests :data:verifyCommonMainViajorDatabaseMigration` | 11 tests passed (schema v2 constraints, ordering, soft delete, v1 to v2 upgrade of a populated database); migration verification passed |
| `./gradlew :androidApp:assembleDebug` | Debug APK built (about 53 MB) |
| `./gradlew :androidApp:lintDebug` | Passed; 1 warning (`targetSdk` 36 is not the latest) |
| `./gradlew :designsystem:allTests` | 3 tests passed (Waypoint token contrast pairs, WCAG ratio) |
| CI on `develop` (`ci`, `secret-scan`) | Both succeeded at the merge of #23 |
| `app_icons.py verify` | 29 checks, 0 problems; the debug APK contains the adaptive icon resources |

## Not verified or not possible here

- **iOS: nothing.** No macOS or Xcode. iOS targets are declared but never compiled, linked or run.
- **No emulator/device run.** No AVD or system image installed; `adb` is not on `PATH`. UI, map
  rendering and startup time are unmeasured.
- Release build (R8), signing, Play and App Store requirements.
- CI covers only `:domain:allTests`, `:androidApp:lintDebug` and `:androidApp:assembleDebug`. It does not run the `:data` and `:designsystem` tests or the migration verification (workflow files are human-owned); those were run locally.
- The CSV formula guard was not tried in Excel, Calc or Sheets. The offline tile estimate uses an assumed 25 kB per tile, not a measurement.
- Time-zone rules depend on the platform's tz database.

## Phase status

See [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md). Product docs: drafted. Architecture: six ADRs
Accepted and merged. Brand: **Waypoint chosen**, see below. Design system: Waypoint color tokens and
Android icon merged; fonts and components not started. Data: schema v2 merged, no repositories yet.
Features: none visible to a user.

## Brand work

Three concepts exist in `design/brand/concepts/` (01 Waypoint, 02 Keyhole Road, 03 Contour) with a
side-by-side [gallery](../design/brand/gallery/index.html) and [BRAND_STRATEGY.md](design/BRAND_STRATEGY.md).
The designer's weighted scores (its own judgement, not user research) are Waypoint 87.0, Keyhole Road
80.0, Contour 63.0, so Waypoint was the recommendation. The generated QA report
(`design/brand/concepts/QA_REPORT.md`) found no structural or contrast failures in the 24 logo SVGs.
Contour is weak at 16 px and can read as an eye or fingerprint. Not done: device testing, trademark
search, colour-blind simulation, VectorDrawable/iOS icon export. **Decision (2026-10-27): the owner chose concept 01, Waypoint**, for its design system. The
choice rests on their visual judgement, not on user research or a trademark search (still open).
Design tokens and the Android app icon are wired in (issue #3, merged); the iOS icon files exist but were never opened in Xcode. Fonts are not wired.

## Decisions and open items

Decided by the owner on 2026-10-27:

1. ADR-0001 to ADR-0006 accepted.
2. Brand direction: 01 Waypoint.
3. Application ID `io.github.eliangilsierra.viajorapp` confirmed.
4. GitHub issue [#1](https://github.com/eliangilsierra/viajorapp/issues/1) created for this work.
5. First batch of Pull Requests merged by the owner, including `.gitleaksignore` with two fingerprints of made-up test values from the secret-redaction tests.

Still open:

- Human decision on adding `:data:allTests`, `:designsystem:allTests` and `:data:verifyCommonMainViajorDatabaseMigration` to `.github/workflows/ci.yml` (workflow files are human-owned).
- Provide macOS access or a macOS CI runner for iOS validation.
- Trademark search for the name and mark before any public release.

## Known limitations of the current code

- `Create a trip` and all feature entry points do not exist yet (no placeholder buttons are shown).
- `designsystem` colors are the Waypoint tokens (light and dark) and their contrast pairs are tested. Typography is not done: Manrope and Inter are not bundled until their OFL files are checked. Only some Material3 roles are mapped.
- The map surface is compiled but not displayed by any screen.
- The database is not wired to the UI; there is no repository layer, no driver setup (production drivers must enable `PRAGMA foreign_keys=ON`) and no cost column for `Money`.
- The domain building blocks are not used by any screen, import flow, sync transport or AI provider.

## Next tasks

1. Repository layer and view models for trips and the day/itinerary (TRP-01..03, DAY-01, ACT-01), using the merged schema v2, `DayPlanner`, `TripValidation` and `ZonedTimes`; production driver setup.
2. Trip list and itinerary UI with the Waypoint theme.
3. Bundle the Waypoint fonts after checking their OFL licences; replace remaining Material defaults.
4. Import: CSV mapping UI on top of `CsvReader`; JSON backup with `ExportGuard`.
5. Install an emulator image (or connect a device) to run the app and measure startup.
