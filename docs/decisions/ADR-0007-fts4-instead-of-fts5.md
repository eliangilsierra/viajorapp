# ADR-0007: FTS4 instead of FTS5 for full-text search

## Status

Proposed (2026-10-28). Amends the search bullet of [ADR-0002](ADR-0002-local-persistence-sqldelight.md); the owner decides.

## Context

ADR-0002 chose an FTS5 virtual table for searching places and activities. The Android app supports API 24 and up and uses the SQLite that ships with the operating system. FTS5 is not guaranteed to be compiled into that SQLite on the older versions in range (Android 7 to 10), while FTS3 and FTS4 have been available since Android's earliest SQLite. A missing module would make the schema migration fail when the app opens, so the user could not open their trips at all.

## Decision

Use an FTS4 virtual table (`search_index`, tokenizer `unicode61`, so case and accents are ignored) maintained by triggers on `place` and `activity`. Search text is turned into lower-case prefix terms by `SearchQuery`, so user input cannot inject query syntax. Revisit FTS5 when the minimum supported Android version guarantees it, or if the project bundles its own SQLite.

## Alternatives

- FTS5 as in ADR-0002: better ranking (bm25) but a startup risk on older devices.
- Bundle a SQLite build with FTS5: adds a native dependency and APK size for a small feature.
- `LIKE` queries: no index, no accent folding, poor at scale.

## Consequences

Ranking is simple (kind, then text) instead of bm25. Soft-deleted rows stay in the index and are filtered by the query. The index duplicates text, which costs some disk space at itinerary scale. Moving to FTS5 later is a migration that rebuilds the index.

## Security considerations

Query text is reduced to letters and digits before it reaches `MATCH`, and the SQL uses bound parameters. The index lives in the same local database and has the same protections (OS sandbox, no app-level encryption).

## Operational considerations

If the platform SQLite lacked FTS4 the migration would fail; FTS4 has been present on Android since API 11 and in the JDBC driver used by tests.
