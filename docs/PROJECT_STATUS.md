# Project status

Last updated: 2026-11-02. Evidence for every "verified" statement is the named command, the CI run on
the Pull Request, or the emulator run described below.

## Summary

**The Android MVP works end to end on an emulator.** A person can create, edit, archive and delete trips;
plan days and activities (dates and times from pickers, completion, reorder and move between days, ideas
that are not on a day); keep places with coordinates and see a labelled straight-line estimate between
activities; see the places on a map with the tile provider's attribution; search every trip; import a plan
from pasted text or a CSV file after a review step; export a plan as Markdown or CSV; and back up, restore
and erase everything. All data stays on the device with no account, server or analytics, and the app
survives rotation.

**Not done and not verified:** a physical device, iOS (never compiled), TalkBack and font scaling,
optional AI and optional sync (only tested building blocks exist), offline map packs, routing providers.
See the lists below.

## Verified (Windows 11, JDK 17; the whole chain of PRs #65 to #69 merged in a scratch branch)

| Check | Result |
| --- | --- |
| `./gradlew :domain:allTests` | 133 tests passed |
| `./gradlew :data:allTests :data:verifyCommonMainViajorDatabaseMigration` | 38 tests passed (repositories, search, backup service, migrations v1 to v2 to v3); migration verification passed |
| `./gradlew :designsystem:allTests :ui:allTests` | 3 and 30 tests passed (token contrast, presenters, import, search, navigation, strings) |
| `./gradlew :androidApp:lintDebug :androidApp:assembleDebug :androidApp:assembleRelease` | Passed; lint reports informational warnings only. Release APK about 42 MB |
| CI (`ci`, `pr-conventions`, `secret-scan`) | Green on every merged Pull Request; it runs only `:domain:allTests`, lint and `assembleDebug` |
| Android emulator (API 37 image, headless, software GL), debug build | Restored a seeded trip through the system file picker; itinerary with times, places and the labelled estimate (7.1 km, 84 min on foot between two Lisbon places); the map opened centred on Lisbon with two markers over OpenFreeMap tiles and visible attribution; search found "cafe" in "Cafe da manha"; the import screen reviewed a pasted line and offered it for import; the trip editor opened the Material date picker on the stored date |
| Earlier emulator runs (debug and R8 release builds) | Created trips, added activities and places, exported and restored a backup through the system picker, deleted all data, data persisted across a cold restart; release cold start 1.4 s (emulator, not representative) |

Running on the emulator found two defects that no test caught, both fixed: a launch crash (Compose
resources not packaged on Android) and a map that stayed on the world view (camera fixed before the places
arrived). Lint caught an API 33 call in the file picker.

## Not verified or not possible here

- **iOS: nothing.** No macOS or Xcode. The iOS database driver is written but never compiled.
- **No physical device**, TalkBack, font scaling, landscape layout, the Spanish screens on screen, and
  performance on large trips. Only a software-rendered emulator was used.
- Not exercised by hand (covered by automated tests only): moving an activity to another day, completing,
  reordering, deleting with undo, editing a place, exporting Markdown or CSV, importing a CSV file, the
  time picker, behaviour with no network.
- CI covers `:domain:allTests`, lint and `assembleDebug` only; the other module tests and the migration
  verification run locally (workflow files are human-owned).
- The CSV formula guard was not tried in Excel, Calc or Sheets. The offline tile estimate and travel
  estimates use assumed speeds and sizes. Time-zone rules depend on the platform tz database.
- Signing, Play and App Store processes (guides exist, nothing was executed).

## Phase status

See [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md). Architecture: ADR-0001 to ADR-0006 accepted;
ADR-0007 (FTS4 instead of FTS5) proposed. Brand: Waypoint chosen and wired. Data: schema v3 with verified
migrations and repositories. Features in the app: trips, itinerary, places and estimates, map, search,
import, export, backup and erase. Optional AI and sync: building blocks only.

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
2. The first batches of Pull Requests (#2 to #57) were merged by or with the authorization of the owner,
   including `.gitleaksignore` with two fingerprints of made-up test values.

Still open:

- **ADR-0007** (FTS4 instead of FTS5 for Android 7 to 10 compatibility) is merged with status Proposed;
  the owner decides whether to accept it.
- Review and merge of the MVP chain: #65, #66, #67, #68, #69 (in that order, each includes the earlier
  ones) and this documentation PR.
- Human decision on adding the `:data`, `:designsystem` and `:ui` tests and the migration verification to
  `.github/workflows/ci.yml`.
- macOS access or a macOS CI runner for iOS validation; a physical device for the first real run.
- Trademark search for the name and mark; signing keys; Play and Apple accounts (see the release guides).

## Known limitations of the current code

- Reordering is by menu (up, down, move to another day), not drag. Estimates are shown only between
  consecutive activities of the same day. The map has no place picking and shows a world view when no place
  has coordinates.
- Typography is not done: Manrope and Inter are not bundled until their OFL licences are checked.
- Places can be added, edited and deleted; deleting a place leaves activities that referenced it without it.
- Restore only adds trips (no merge or replace). Delete-all is not a secure wipe.
- Import supports pasted text and CSV with the columns title, day, start, end, notes; not XLSX.
- The base map needs a connection; there are no offline map packs and no routing provider.

## Next tasks

1. Run on a physical device and fix what appears; TalkBack and font-scale pass.
2. Optional AI: provider adapters, key storage in the platform secret store, disclosure and review screens
   on top of the proposal validator and prompt preview.
3. Offline map packs (after the region-size spike) and a routing adapter behind `RoutingProvider`.
4. Bundle the Waypoint fonts after the licence check; drag reordering.
5. iOS: compile the shared modules and build the host app on a Mac.
6. Optional sync (crypto spike first).
