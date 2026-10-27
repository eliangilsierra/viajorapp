# ADR-0002: Local persistence with SQLDelight

## Status

Accepted (2026-10-27). The owner approved it in conversation on 2026-10-27 and asked for the approval to be recorded; it takes effect when the foundation Pull Request (issue #1) is merged.

## Context

All user data must live on the device and survive restarts, interrupted writes and app updates.
Itineraries need ordering, filtering, full-text search and bounding-box queries, plus a change log for
synchronization.

## Decision

SQLDelight 2.4.1 on SQLite (Android driver, native driver on iOS).

- **Identifiers:** client-generated UUIDs (time-ordered v7 where the Kotlin `Uuid` API offers it),
  never autoincrement keys, so records can merge across devices.
- **Time:** items store a local date-time plus an IANA zone id (`startLocal`, `zoneId`) and an optional
  derived UTC instant. Trips store local dates. Overnight items are an item with an end date-time later
  than its start date. Daylight-saving gaps and overlaps are resolved by explicit domain functions.
- **Ordering:** fractional-index string keys per day (`sortKey`), so a reorder touches one row and
  concurrent reorders on different items merge cleanly.
- **Sync metadata on every synced table:** `updatedAtHlc`, `updatedBy` (device id), `deletedAt`
  tombstone, and a per-field version map for conflict-prone fields. Deleting is soft first, hard purge
  runs after a retention window or on explicit "delete forever".
- **Search:** FTS5 virtual table for place and activity text; coordinates are stored as
  `lat`/`lon` with an index on both for bounding-box queries (R-Tree only if measured to be needed).
- **Transactions:** every multi-entity change (duplicate trip, import commit, accept AI proposal,
  apply remote changes) runs in one transaction.
- **Migrations:** numbered `.sqm` files, a migration test that opens every historical schema fixture,
  and a pre-migration automatic backup copy.
- **Secrets are not stored here.** API keys and sync keys live in the platform secret store
  ([ADR-0005](ADR-0005-optional-ai-byok.md)).
- **Backups** are an explicit export file (JSON + attachments manifest), never containing secrets.

## Alternatives

Room KMP (viable, annotation-driven; weaker fit for explicit SQL migrations and FTS/spatial work);
Realm (deprecated direction); a document store (loses relational integrity).

## Consequences

SQL is hand-written and reviewed; schema changes are deliberate. The sync log tables add write cost,
which is acceptable at itinerary scale and checked by the performance tests.

## Security considerations

The database is not encrypted at rest by Viajorapp; it relies on OS app sandboxing and device
encryption. This is stated in the privacy documentation. SQLCipher is a possible P2 option with a
key held in the secret store.

## Operational considerations

Database file path and size are shown in Settings > Data. Corruption triggers a restore prompt from the
latest automatic backup.
