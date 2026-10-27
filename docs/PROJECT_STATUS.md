# Project status

Last updated: 2026-10-27. Evidence for every "verified" statement is in
[the compatibility spike](research/2026-10-27-version-compatibility-spike.md) or the named command.

## Summary

Foundation phase. Product documents, architecture decisions and a building Android shell exist. **No
user-facing feature is implemented**: the app launches to a static welcome screen (never run on a device
or emulator in this environment). Work is committed locally on `feature/1-project-foundation` (issue #1); nothing is pushed.

## Verified (commands run on Windows 11, JDK 17)

| Check | Result |
| --- | --- |
| `./gradlew :domain:allTests` | 19 tests passed (ordering keys, overlap detection, distance estimate, tile-source rules) |
| `./gradlew :data:allTests` | 3 tests passed (SQLDelight schema v1 on in-memory SQLite) |
| `./gradlew :androidApp:assembleDebug` | Debug APK built (about 53 MB) |
| `./gradlew :androidApp:lintDebug` | Passed; 1 warning (`targetSdk` 36 is not the latest) |

## Not verified or not possible here

- **iOS: nothing.** No macOS or Xcode. iOS targets are declared but never compiled, linked or run.
- **No emulator/device run.** No AVD or system image installed; `adb` is not on `PATH`. UI, map
  rendering and startup time are unmeasured.
- Release build (R8), signing, Play and App Store requirements.
- CI has not run (it needs a push, which is the owner's action under the branch rules).

## Phase status

See [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md). Product docs: drafted. Architecture: six ADRs
Accepted by the owner on 2026-10-27 (effective when the Pull Request merges). Brand: **Waypoint chosen**,
see below. Design system: provisional tokens only until the Waypoint palette is wired in. Features: none.

## Brand work

Three concepts exist in `design/brand/concepts/` (01 Waypoint, 02 Keyhole Road, 03 Contour) with a
side-by-side [gallery](../design/brand/gallery/index.html) and [BRAND_STRATEGY.md](design/BRAND_STRATEGY.md).
The designer's weighted scores (its own judgement, not user research) are Waypoint 87.0, Keyhole Road
80.0, Contour 63.0, so Waypoint was the recommendation. The generated QA report
(`design/brand/concepts/QA_REPORT.md`) found no structural or contrast failures in the 24 logo SVGs.
Contour is weak at 16 px and can read as an eye or fingerprint. Not done: device testing, trademark
search, colour-blind simulation, VectorDrawable/iOS icon export. **Decision (2026-10-27): the owner chose concept 01, Waypoint**, for its design system. The
choice rests on their visual judgement, not on user research or a trademark search (still open).
Final assets, design tokens and the app icon are not yet wired into the app.

## Decisions and open items

Decided by the owner on 2026-10-27:

1. ADR-0001 to ADR-0006 accepted.
2. Brand direction: 01 Waypoint.
3. Application ID `io.github.eliangilsierra.viajorapp` confirmed.
4. GitHub issue [#1](https://github.com/eliangilsierra/viajorapp/issues/1) created for this work.

Still open:

- Human review of the `ci` change in `.github/workflows/ci.yml` (the template says humans add workflow
  files). The owner will validate it later.
- Provide macOS access or a macOS CI runner for iOS validation.
- Trademark search for the name and mark before any public release.

## Known limitations of the current code

- `Create a trip` and all feature entry points do not exist yet (no placeholder buttons are shown).
- Colors in `designsystem` are provisional until the brand is approved.
- The map surface is compiled but not displayed by any screen.
- Database is not wired to the UI; no repository layer yet.

## Next tasks

1. Turn the Waypoint palette and type into real design tokens; wire the app icon (adaptive icon layers).
2. Schema v2 with places, days, activities; migration test with a generated baseline schema.
3. Repository layer and view models for trips (TRP-01..03) with tests, then the day/itinerary UI.
4. Install an emulator image (or connect a device) to run the app and measure startup.
