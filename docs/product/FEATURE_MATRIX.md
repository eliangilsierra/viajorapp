# Feature matrix

Priorities: P0 essential first release, P1 mature release, P2 later. Behavior and acceptance criteria are
in the [PRD](PRD.md). "Offline" describes the behavior with no network. Test types: U unit, I integration
(JVM/Android, fakes), UI instrumented/Compose, E2E flow, M manual recorded check.

| ID | Feature | Pri | Depends on | Offline behavior | Privacy | Failure states | Tests |
| --- | --- | --- | --- | --- | --- | --- | --- |
| ONB-01 | Onboarding, no account | P0 | none | Same | None collected | Storage init error → retry | UI, M |
| TRP-01 | Create/edit trips | P0 | DB | Full | Local | Invalid dates; draft restore | U, I, UI |
| TRP-02 | Duplicate/archive/delete/trash | P0 | TRP-01 | Full | Hard delete purges all copies | Undo expiry | U, I, E2E |
| TRP-03 | Multiple destinations | P0 | TRP-01 | Full | Local | Zone missing → device zone, flagged | U, I |
| TRP-04 | Templates | P1 | TRP-01 | Full | Strip bookings by default | Template schema drift | U, I |
| DAY-01 | Day list | P0 | TRP-01 | Full | Local | Empty day state | UI, A11Y |
| DAY-02 | Timeline | P1 | DAY-01, ACT-03 | Full | Local | Untimed items listed apart | UI |
| ACT-01 | Activities, optional fields | P0 | DAY-01 | Full | Local | Validation messages inline | U, UI |
| ACT-02 | Reorder / move | P0 | ACT-01 | Full | Local | Concurrent reorder (sync) | U, I, UI, E2E |
| ACT-03 | Overlap and time-zone rules | P0 | ACT-01 | Full | Local | DST gap/overlap, overnight | U (property tests) |
| ACT-04 | Notes, tags, categories | P0 | ACT-01 | Full | Local | Tag rename merge | U, I |
| ACT-05 | Completion status | P0 | ACT-01 | Full | Local | None | U, UI |
| RSV-01 | Reservations/references | P1 | ACT-01 | Full | Sensitive, not exported unless chosen | Missing attachment file | U, I |
| PLC-01 | Places, saved places | P0 | DB | Full | Coordinates are local | Delete in use | U, I |
| PLC-02 | Place search | P0 local, P1 online | PLC-01 | Local yes; online no | Online: query sent to provider on tap | Provider error/quota | U, I (fake) |
| MAP-01 | Map, markers, list sync | P0 | PLC-01, MAP-03 | Markers yes; base map only if cached/downloaded | Tile provider sees viewed area | Tiles unreachable; GPU/style error | UI, M |
| MAP-02 | Route overlay, completed styling | P1 | MAP-01, RTE-01 | Estimates yes | Local | No located items | UI |
| MAP-03 | Tile source config, attribution | P0 | none | Config local | Active provider shown | Invalid style URL | U, UI |
| OFM-01 | Offline map regions | P1 | MAP-03 | Core purpose | Provider sees download | Limit exceeded; storage full; interrupted | I, M |
| OFM-02 | User tile package | P2 | OFM-01 | Full | Local | Corrupt/unsupported file | I |
| RTE-01 | Straight-line estimate | P0 | PLC-01 | Full | Local | Missing coordinates → none shown | U (property) |
| RTE-02 | Routed walking legs | P1 | RTE-01 | Cached only | Coordinates to configured host | Timeout, quota, no route | I (recorded fixtures) |
| RTE-03 | Instructions | P1 | RTE-02 | Cached only | Same | Provider omits maneuvers | I |
| RTE-04 | Offline routing | P2 | engine + data | n/a for 1.0 | n/a | n/a | not planned |
| IMP-01 | CSV import | P0 | DB | Full | File stays local | Bad encoding/dates; huge file | U, I, E2E |
| IMP-02 | JSON import | P0 | DB | Full | Local | Unknown version | U, I |
| IMP-03 | XLSX import | P1 | IMP-01 | Full | Local | Zip bomb; formulas ignored | U, I |
| IMP-04 | Plain-text import | P1 | IMP-01 | Heuristic yes; AI no | AI sends text to provider | Unparsable lines kept in notes | U, I |
| EXP-01 | Backup/restore | P0 | DB | Full | No secrets in file | Corrupt file; version mismatch | U, I, E2E |
| EXP-02 | JSON/CSV/Markdown export | P0 | DB | Full | User-initiated share | Share target missing | U, I |
| EXP-03 | PDF export | P2 | EXP-02 | Full | Local | Pagination | M |
| SRC-01 | Search/filter | P0 | DB (FTS5) | Full | Local | Empty result state | U, I, perf |
| SET-01 | Settings | P0 | none | Full | Local | None | UI |
| PRV-01 | Privacy center, delete all | P0 | all stores | Full | Core purpose | Partial deletion failure → report | I, E2E, M |
| AI-01 | BYOK setup | P1 | SecretStore | Disabled offline | Key never leaves device except to provider | Bad key, unreachable | U, I (fake) |
| AI-02 | Proposals + diff review | P1 | AI-01, ACT-* | Disabled | Preview of data sent | Malformed, timeout, quota, cancel | U, I, E2E |
| AI-03 | Extraction / gaps / feasibility | P1 | AI-01 | Disabled | Same | Hallucinated facts rejected | U, I |
| SYN-01 | Own-device folder sync | P1 | DB metadata, crypto | Pending queue | Ciphertext only on storage | Folder missing, corrupt segment | I, E2E |
| SYN-02 | Conflict resolution | P1 | SYN-01 | Local resolve | Local | Delete vs edit; stale device | U, I, E2E |
| SYN-03 | Shared trips, roles | P2 | SYN-01 | Same | Key custody documented | Revocation limits | I, threat-model review |
| BUD-01 | Budget | P2 | ACT-01 | Full | Local | Currency formatting | U |
| A11Y-01 | Accessibility | P0 | all UI | n/a | n/a | n/a | UI, M |
| I18N-01 | Localization | P0 | all UI | n/a | n/a | Missing string fallback | U, M |
