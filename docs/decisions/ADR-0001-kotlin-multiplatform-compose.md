# ADR-0001: Kotlin Multiplatform with Compose Multiplatform

## Status

Accepted (2026-10-27). The owner approved it in conversation on 2026-10-27 and asked for the approval to be recorded; it takes effect when the foundation Pull Request (issue #1) is merged.

## Context

Viajorapp targets Android and iOS, must work offline, and is developed in an environment built around
Kotlin and KMP tooling. See [findings](../research/2026-10-27-environment-and-stack-findings.md).
Compose Multiplatform for iOS has been stable since 1.8.0; the current release is 1.12.2 with Kotlin
2.4.21 and AGP 9.4.1. No Mac is available in the current environment.

## Decision

Use Kotlin Multiplatform for domain, data and integrations, and Compose Multiplatform for the UI on both
platforms. Platform-specific code (secret storage, file pickers, connectivity, share sheets, PDF
printing) sits behind small interfaces in a `platform` module with `expect`/`actual` or Koin-provided
implementations.

Module layout (seven modules, deliberately few):

| Module | Content | Depends on |
| --- | --- | --- |
| `:domain` | Entities, value types, rules (ordering, overlap detection, time handling), repository and provider interfaces. Pure Kotlin, no Android or iOS API. | none |
| `:data` | SQLDelight schema and migrations, repositories, import/export, backup, sync engine. | `:domain` |
| `:designsystem` | Tokens, theme, brand resources, base components. | none |
| `:platform` | `expect`/`actual`: secret store, file access, connectivity, clock/time zone, share. | `:domain` |
| `:integrations` | AI provider adapters, routing and geocoding adapters, tile-source config (Ktor). | `:domain`, `:platform` |
| `:ui` | Feature screens, navigation, view models, grouped by feature package. | all of the above |
| `:androidApp` and `iosApp/` | Thin application shells. | `:ui` |

`:androidApp` is separate because AGP 9 does not allow the Android application plugin and the
Kotlin Multiplatform plugin in one module.

Dependency injection: Koin, constructor injection only in `:data`/`:integrations`, wiring in `:ui`.

State: ViewModel + `StateFlow` UI state, one-shot effects as a separate flow.

## Alternatives

Flutter, React Native and native twin apps were considered and rejected (see the findings, section 7).

## Consequences

- One implementation of itinerary rules, import, sync and UI for both platforms.
- iOS correctness depends on macOS-only tooling that is unavailable here. Until a macOS build runs,
  iOS is "compiles in theory, unverified", and every release report must say so.
- maplibre-compose is pre-1.0 with an unstable API: map code is isolated behind a `MapSurface`
  abstraction in `:ui` so it can be replaced.
- Version pins live in `gradle/libs.versions.toml`; a compile spike must prove the combination before
  the first release (Kotlin 2.4.21, CMP 1.12.2, AGP 9.4.1, Gradle 9.8.2, maplibre-compose 0.20.0).

## Security considerations

Shared code must not log secrets or trip content; the logging wrapper strips values by default.

## Operational considerations

CI runs JVM/Android unit tests on Linux. A macOS job is needed for iOS; it is a manual follow-up
because it needs a macOS runner and signing assets.
