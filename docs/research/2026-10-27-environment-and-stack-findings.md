# Environment and stack findings (2026-10-27)

Status: findings from reconnaissance and research that precede ADR-0001 to ADR-0006. Everything marked
**verified** was checked on this machine or against a primary source on the date above. Everything
marked **assumption** was not.

## 1. Repository

- **Verified.** The repository is the unbootstrapped `agent-team-project-template`: `AGENTS.md` and
  `README.md` still contain `{{project_name}}`, `scripts/bootstrap.sh` has not been run, and
  `.github/workflows/` holds the template `ci.yml`, `pr-conventions.yml` and `secret-scan.yml`.
  `.github/ci-templates/android.yml` is the ready-made Android CI.
- **Verified.** License: MIT, copyright "2026 Elian Gil Sierra" (already chosen by the owner; no new
  license decision is needed). Dependencies must therefore be license-compatible with MIT distribution
  (Apache-2.0, MIT, BSD are fine; GPL/AGPL libraries are not linked into the app).
- **Verified.** One commit on `develop`; `origin/main` exists. Branch rules in `AGENTS.md`: no direct
  pushes to `develop` or `main`; task branches are `<type>/<issue-number>-<description>`.
- **Gap.** No GitHub Issue exists yet, so the work branch is the local `feature/foundation-discovery`.
  It must be renamed after the Issue is created.

## 2. Development environment (Windows 11)

| Item | State |
| --- | --- |
| JDK | 17.0.10 only (`JAVA_HOME` unset). Android Gradle Plugin 9.x needs JDK 17+; fine. |
| Gradle | Not on `PATH`. `~/.gradle/wrapper/dists` caches 8.13, 9.0.0, 9.1.0, 9.2.1. Latest stable is 9.8.2. |
| Kotlin | Not installed standalone (comes from the Gradle plugin). |
| Android SDK | `%LOCALAPPDATA%\Android\Sdk`: platforms 35, 36, 36.1; build-tools 35.0.0 to 37.0.0; platform-tools, emulator. **No `cmdline-tools`, no `sdkmanager`.** `ANDROID_HOME` unset. |
| Emulator / AVD | `emulator` package present, `system-images/` empty, no AVD listed. `adb` not on `PATH` (it is in `platform-tools`). |
| iOS | **No Xcode, Swift or macOS.** iOS cannot be compiled or run here. |
| SVG tools | `resvg` and `svgo` installed. Python 3.12 with Pillow 12.2. No ImageMagick, Inkscape or rsvg-convert. |
| Other | Node, git, `gh`, Docker present. |

Consequences:

1. Android builds and JVM unit tests are possible locally once `local.properties` points at the SDK.
2. Android instrumented and UI tests need an emulator image (download) or a physical device.
3. **iOS compilation, simulator runs, Keychain behavior and App Store checks cannot be validated
   here.** They are reported as untested and left to macOS CI or the owner.

## 3. Current versions (checked against GitHub and Maven on 2026-10-27)

| Component | Latest stable | License | Notes |
| --- | --- | --- | --- |
| Kotlin | 2.4.21 | Apache-2.0 | |
| Compose Multiplatform | 1.12.2 | Apache-2.0 | iOS stable since 1.8.0; Android min API 21, iOS min 14 per JetBrains compatibility page. |
| Android Gradle Plugin | 9.4.1 (9.6.0-alpha01 exists) | Apache-2.0 | KMP libraries use `com.android.kotlin.multiplatform.library`; the Android app must be a separate module. |
| Gradle | 9.8.2 | Apache-2.0 | |
| SQLDelight | 2.4.1 | Apache-2.0 | Active (pushed 2026-10-25). |
| maplibre-compose | 0.20.0 | BSD-3-Clause | Android + iOS supported; API explicitly not yet stable; iOS 15.5+. 592 stars, active. |
| Ktor | 3.6.0 | Apache-2.0 | |
| Koin | 4.2.2 | Apache-2.0 | |
| kotlinx.coroutines | 1.11.0 | Apache-2.0 | |
| kotlinx.serialization | 1.12.0 | Apache-2.0 | |
| kotlinx-datetime | 0.8.0 | Apache-2.0 | |
| Turbine | 1.2.1 | Apache-2.0 | |

**Assumption.** Exact transitive compatibility (for example maplibre-compose 0.20.0 against Compose
Multiplatform 1.12.2 and Kotlin 2.4.21) is only proven by a successful build. ADR-0001 therefore
requires a compile spike before the versions are frozen in `gradle/libs.versions.toml`.

## 4. Maps, tiles and offline data

- **maplibre-compose** wraps MapLibre Native on Android and iOS and exposes an offline API
  (`OfflineStorage`, tile-pyramid and shape pack definitions, progress, pause/resume/delete).
  Its documentation says nothing about provider terms, so licensing is decided separately.
- **OpenStreetMap standard tile servers (`tile.openstreetmap.org`) forbid bulk download, prefetching
  and "save for later" or city-download features.** They cannot back offline maps. They are acceptable
  only for viewport-driven online display with an identifying User-Agent, and Viajorapp will not
  default to them.
- **OpenFreeMap** (free, no key) explicitly allows app-driven offline downloads, limited to 3,000 tiles
  or 50 MB per download, at most 4 parallel requests, with a unique User-Agent. It has no SLA and may be
  discontinued. Larger regions need the operator's permission.
- Consequence: a city-sized offline region at useful zoom levels will often exceed 3,000 tiles. The
  honest options are (a) several small packs, (b) user-supplied PMTiles/MBTiles or a user-hosted tile
  source (planned P2), or (c) a paid or self-hosted vector tile provider the user configures. No
  default offline provider is promised until a region-size spike is measured.
- Attribution (OpenStreetMap contributors, plus the tile/style provider) must be visible on every map.

## 5. Routing

- Public demo routing servers (OSRM, Valhalla at `valhalla.openstreetmap.de`, Nominatim) are
  fair-use demos with rate limits and are not suitable as a shipped default.
- Viajorapp will not ship a hard-coded public routing endpoint. Routing is a provider interface; the
  user configures a Valhalla-compatible, OpenRouteService (own key) or self-hosted endpoint.
- Without a configured provider the app shows a **straight-line estimate** labeled as such (distance
  by great-circle, duration by a configurable walking speed). Estimates are never shown as routes.
- Offline routing (an on-device engine plus regional graph data) is **out of scope for 1.0** and is
  documented as a future capability. BRouter is GPL and Valhalla's mobile builds are large native
  dependencies; both need their own evaluation before any promise.

## 6. Other decisions that needed evidence

- **Persistence:** SQLDelight 2.x. SQL-first, mature on Android/iOS, and allows FTS5 and plain SQL for
  spatial bounding-box queries. Room has KMP support, but a SQL-first schema with explicit migrations
  fits the sync and import requirements better.
- **XLSX:** no maintained, permissively licensed Kotlin Multiplatform reader was confirmed. The plan is
  a small reader built on Okio's `ZipFileSystem` plus a streaming XML parser, restricted to reading cell
  values (no formulas executed), with size and entry limits (zip-bomb protection).
- **Secrets:** Android Keystore-backed encrypted storage and iOS Keychain behind a `SecretStore`
  interface. **iOS half is unverifiable here.**
- **Sync:** see ADR-0006; no Viajorapp server.

## 7. Rejected alternatives

| Alternative | Why rejected |
| --- | --- |
| Flutter | Would discard the Kotlin/KMP skills and tooling already in the environment; no material advantage for this app. |
| React Native | Offline spatial and sync-heavy logic would live in JS; weaker fit with the Kotlin toolchain here. |
| Native twin apps (Kotlin + Swift) | Doubles the work for a solo/open-source project; KMP shares the domain, data and UI. |
| Google Maps / Mapbox SDKs | Proprietary terms, API keys and recurring cost conflict with the principles. |
| Firebase / Supabase backend | Mandatory third-party cloud, contradicts local ownership. |
| Room KMP | Viable; SQLDelight preferred for explicit SQL migrations and FTS/spatial queries. |
| CRDT library (Automerge etc.) | Heavy for this data shape; a field-level versioned log with explicit conflicts is simpler and testable. |
