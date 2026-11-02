# Implementation plan

Tracks phases, tasks and acceptance criteria. Current state is in [PROJECT_STATUS.md](PROJECT_STATUS.md).
A criterion is checked only when verified by a run or recorded check, with the evidence linked.
Priorities: P0/P1/P2 as in the [PRD](product/PRD.md). Risk column lists what could invalidate the task.

## Phase map

| # | Phase | Output | Status |
| --- | --- | --- | --- |
| 1 | Environment reconnaissance | [Findings](research/2026-10-27-environment-and-stack-findings.md) | Done |
| 2 | Product discovery | `docs/product/*` | Drafted, awaiting owner review |
| 3 | Brand concepts | `design/brand/**`, `docs/design/BRAND_STRATEGY.md` | Done: Waypoint chosen by the owner (2026-10-27) |
| 4 | Design system and UX prototype | tokens, components, interactive preview | In progress: Waypoint colour tokens, container mapping and Android icon merged; fonts, components and prototype not started |
| 5 | Architecture decisions | ADR-0001..0006 | Accepted (2026-10-27) |
| 6 | Project foundation and CI | Gradle project, version spike, CI | Android done and verified locally (F1-F4); CI workflow merged and green on `develop` for domain tests, lint and assemble (F5); data and designsystem tests not in CI; iOS unverified (F6) |
| 7 | Local data model and persistence | schema, repositories, migrations | Done for trips, days, places, activities and search (schema v3, verified migrations, repositories, backup service); sync log tables not started |
| 8 | Trips and itinerary | TRP-*, DAY-*, ACT-*, SRC-01 | In progress: trips, days, activities, completion, ideas and menu reordering work on the emulator; pickers, drag, tags, reservations, budget fields and the search screen not started |
| 9 | Maps and places | MAP-*, PLC-* | In progress: places with coordinates merged; no map screen yet |
| 10 | Routing and offline maps | RTE-*, OFM-* | In progress: straight-line estimate and offline region estimate merged; no routing provider or download |
| 11 | Import/export | IMP-*, EXP-*, PRV-01 | In progress: JSON backup export and restore and delete-all work on the emulator; CSV and text parsers merged without screens; XLSX not started |
| 12 | Optional AI | AI-* | In progress: proposal validation, answer parsing, prompt preview and secret redaction merged; no provider, key store or screens |
| 13 | Optional sync | SYN-* | In progress: clock and field merge merged; no change files, transport or encryption |
| 14 | Hardening | accessibility, performance, security | Not started |
| 15 | Docs and release prep | README, store checklists, signing guide | In progress: README, CONTRIBUTING, SECURITY, PRIVACY, CODE_OF_CONDUCT and the release, signing and store readiness guides merged; nothing executed |
| 16 | Final audit | release report | Not started |

## Phase 6 tasks (foundation)

| Task | Pri | Depends | Acceptance criteria | Files | Tests | Risks |
| --- | --- | --- | --- | --- | --- | --- |
| F1 Gradle wrapper and version catalog | P0 | none | `./gradlew help` succeeds with Gradle 9.8.2; versions in `gradle/libs.versions.toml` | `gradle/`, `settings.gradle.kts` | build | Toolchain download needs network |
| F2 Modules `:domain`, `:designsystem`, `:ui`, `:androidApp` | P0 | F1 | each module configures and compiles for Android | module dirs | `./gradlew assembleDebug` | AGP 9 KMP plugin differences |
| F3 Compatibility spike | P0 | F2 | Kotlin 2.4.21, CMP 1.12.2, AGP 9.4.1, maplibre-compose 0.20.0 and SQLDelight 2.4.1 compile together on Android; result recorded | spike report | build | Incompatible pins → adjust and record |
| F4 Domain rules with tests | P0 | F2 | ordering keys, overlap detection and estimate distance covered by passing JVM tests | `domain/src` | `:domain:allTests` | none |
| F5 CI | P0 | F2 | `.github/workflows/ci.yml` builds, tests and lints; secret scan stays on | `.github/workflows` | CI run (needs push, owner action) | Workflows are added by humans per template rules |
| F6 iOS target declared | P1 | F2 | iOS targets declared and ignored on non-macOS with a documented limitation | build files | n/a here | cannot be verified on Windows |

## Quality gate per phase

1. Relevant build succeeds. 2. Tests pass. 3. No known critical security issue. 4. Docs updated.
5. Untested platform-specific behavior is listed in PROJECT_STATUS.md. 6. Acceptance criteria are
checked only with evidence.
