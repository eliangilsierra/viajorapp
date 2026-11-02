# Project status

Last updated: 2026-11-02. Evidence for every "verified" statement is the named command, the CI run on
`develop`, or the emulator run described below.

## Summary

Early product (0.1, not released). Trips can be created, edited, archived and deleted; days, activities
(with times), ideas and places can be planned; straight-line travel estimates are shown between
activities, labelled as estimates. Everything is stored on the device with no account, server or
analytics. Backup export, restore and delete-all-data work through the system file picker. Verified on
an Android emulator only: **never run on a physical device, and nothing about iOS is verified.**

Not wired to any screen yet (tested building blocks only): CSV and text import, full-text search UI, sync
merge, AI proposal parsing and validation, secret redaction in logging, offline map region estimates,
money and budgets, routing providers, maps.

## Verified (Windows 11, JDK 17, on `develop` after the last merge)

| Check | Result |
| --- | --- |
| `./gradlew :domain:allTests` | 129 tests passed |
| `./gradlew :data:allTests :data:verifyCommonMainViajorDatabaseMigration` | 38 tests passed (repositories, search, backup service, migrations v1 to v2 to v3); migration verification passed |
| `./gradlew :designsystem:allTests :ui:allTests` | 3 and 17 tests passed (token contrast, presenters, string completeness) |
| `./gradlew :androidApp:lintDebug :androidApp:assembleDebug :androidApp:assembleRelease` | Passed. Lint reports informational warnings only (old `targetSdk`, `allowBackup`, legacy icon shape, KTX) |
| CI on `develop` (`ci`, `pr-conventions`, `secret-scan`) | Green on each merged PR |
| Android emulator (API 37 image, headless), debug and R8-minified release builds | Created a trip with dates, added activities with times and places, saw the labelled estimate (about 6.1 km between two Lisbon places), exported a backup through the system picker (valid JSON written), restored it, deleted all data; data persisted across a cold restart. Cold start 1.4 s release and 3.8 s debug on the emulator (not representative of real devices) |

Running on the emulator found a launch crash that no test caught (Compose resources were not packaged on
Android); it is fixed.

## Not verified or not possible here

- **iOS: nothing.** No macOS or Xcode. The iOS database driver is written but was never compiled.
- **No physical device**, TalkBack, font scaling, landscape or rotation (the back stack is not saved),
  the Spanish screens on screen, and performance on large trips.
- CI covers `:domain:allTests`, lint and `assembleDebug` only. `:data`, `:designsystem` and `:ui` tests and
  the migration verification run locally (workflow files are human-owned).
- The CSV formula guard was not tried in Excel, Calc or Sheets. The offline tile estimate uses an assumed
  25 kB per tile. Time-zone rules depend on the platform tz database.
- Signing, Play and App Store processes (guides exist, nothing was executed).

## Phase status

See [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md). Architecture: ADR-0001 to ADR-0006 accepted;
ADR-0007 (FTS4 instead of FTS5) proposed. Brand: Waypoint chosen and wired (colours, Android icon). Data:
schema v3 (trips, days, places, activities, full-text index) with verified migrations and repositories.
Features visible to a user: trips, itinerary, places with estimates, settings with backup and erase.

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

Decided by the owner:

1. ADR-0001 to ADR-0006 accepted; brand direction Waypoint; application ID
   `io.github.eliangilsierra.viajorapp` confirmed (2026-10-27).
2. Pull Requests #2 to #57 merged by or with the authorization of the owner, including
   `.gitleaksignore` with two fingerprints of made-up test values.

Still open:

- **ADR-0007** (FTS4 instead of FTS5 for Android 7 to 10 compatibility) is merged with status Proposed;
  the owner decides whether to accept it.
- Human decision on adding the `:data`, `:designsystem` and `:ui` tests and the migration verification to
  `.github/workflows/ci.yml`.
- macOS access or a macOS CI runner for iOS validation.
- Trademark search for the name and mark; signing keys; Play and Apple accounts (see the release guides).

## Known limitations of the current code

- Dates are typed as `YYYY-MM-DD` and times as `HH:MM` (no pickers). Reordering is by menu, not drag.
- Typography is not done: Manrope and Inter are not bundled until their OFL licences are checked.
- Places can be added and deleted, not edited. Activities on different days get no estimate.
- The map surface is compiled but shown on no screen; there is no routing provider and no offline map pack.
- Restore only adds trips (no merge or replace). Delete-all is not a secure wipe.
- The back stack is not saved across rotation or process death.

## Next tasks

1. Date and time pickers, edit places, move activities between days from the UI.
2. Import screens on top of `CsvReader`, `TextItineraryParser` and the backup service.
3. Search screen on top of the FTS index.
4. Bundle the Waypoint fonts after the licence check; accessibility pass with TalkBack on a device.
5. Maps screen with MapLibre and attribution; routing adapter behind `RoutingProvider`.
6. Optional AI: provider adapters, key storage in the platform secret store, disclosure screen.
7. Test on a physical device and on a Mac for iOS.
