# Requirements traceability

Status vocabulary: **Not implemented**, **Partially implemented**, **Implemented**, **Tested**,
**Blocked**. A requirement is **Tested** only when the named automated test or recorded manual check has
run and passed. Updated 2026-10-27: only domain rules and a schema spike exist; no user-facing feature is implemented. Rows with code are Partially implemented and name what is covered.

| ID | Journeys | ADR | Planned tests | Code | Status |
| --- | --- | --- | --- | --- | --- |
| ONB-01 | J1 | 0001 | UI onboarding; M | none | Not implemented |
| TRP-01 | J2 | 0002 | `TripRepositoryTest`, UI create trip | `data/.../Trip.sq` (schema v1 and queries only) | Partially implemented |
| TRP-02 | J12 | 0002 | `TripLifecycleTest`, E2E delete | none | Not implemented |
| TRP-03 | J2, J4 | 0002 | `DestinationTest` | none | Not implemented |
| TRP-04 | J4 | 0002 | `TemplateTest` | none | Not implemented |
| DAY-01 | J2, J4 | 0001 | UI day list, a11y | none | Not implemented |
| DAY-02 | J6 | 0004 | UI timeline | none | Not implemented |
| ACT-01 | J4 | 0002 | `ActivityValidationTest` | none | Not implemented |
| ACT-02 | J6 | 0002 | `OrderingTest`, UI drag/alt | `domain/.../FractionalIndex.kt` (ordering keys only; no UI or persistence) | Partially implemented |
| ACT-03 | J4 | 0002 | `TimeRulesPropertyTest` | `domain/.../Overlap.kt` (overlap detection only; time-zone rules not started) | Partially implemented |
| ACT-04 | J4 | 0002 | `TagRepositoryTest` | none | Not implemented |
| ACT-05 | J5 | 0002 | `CompletionTest` | none | Not implemented |
| RSV-01 | J4 | 0002 | `ReservationTest` | none | Not implemented |
| PLC-01 | J5 | 0002 | `PlaceRepositoryTest` | none | Not implemented |
| PLC-02 | J5 | 0002, 0003 | `SearchTest`, geocoder fake | none | Not implemented |
| MAP-01 | J5 | 0003 | map/list sync UI test; M | none | Not implemented |
| MAP-02 | J6, J8 | 0003, 0004 | overlay UI | none | Not implemented |
| MAP-03 | J5, J7 | 0003 | `TileSourceTest` | `domain/.../TileSource.kt`, `ui/.../MapSurface.kt` (model and surface; not wired to a screen or run on a device) | Partially implemented |
| OFM-01 | J7 | 0003 | `OfflinePackTest`, M on device | none | Not implemented |
| OFM-02 | J7 | 0003 | `TilePackageImportTest` | none | Not implemented |
| RTE-01 | J8 | 0004 | `StraightLineEstimatorPropertyTest` | `domain/.../TravelEstimate.kt` (calculation only; not shown in UI) | Partially implemented |
| RTE-02 | J8 | 0004 | `ValhallaAdapterContractTest` | none | Not implemented |
| RTE-03 | J8 | 0004 | `ManeuverMappingTest` | none | Not implemented |
| RTE-04 | J7, J8 | 0004 | not planned | none | Not implemented |
| IMP-01 | J3 | 0002 | `CsvImportTest`, E2E | none | Not implemented |
| IMP-02 | J3 | 0002 | `JsonImportTest` | none | Not implemented |
| IMP-03 | J3 | 0002 | `XlsxImportTest`, zip-bomb test | none | Not implemented |
| IMP-04 | J3 | 0002, 0005 | `TextImportTest` | none | Not implemented |
| EXP-01 | J12 | 0002 | `BackupRoundTripTest`, E2E | none | Not implemented |
| EXP-02 | J12 | 0002 | `ExportFormatTest` | none | Not implemented |
| EXP-03 | J12 | none | M | none | Not implemented |
| SRC-01 | J4 | 0002 | `SearchPerfTest` | none | Not implemented |
| SET-01 | all | 0001 | UI settings | none | Not implemented |
| PRV-01 | J1, J9, J12 | 0002, 0005 | `DeleteAllTest`, network-idle audit | none | Not implemented |
| AI-01 | J9 | 0005 | `SecretStoreTest`, `KeyLeakTest` | none | Not implemented |
| AI-02 | J9 | 0005 | `ProposalValidationTest`, E2E | none | Not implemented |
| AI-03 | J3, J9 | 0005 | `ExtractionTest` | none | Not implemented |
| SYN-01 | J10 | 0006 | `SyncRoundTripTest` | none | Not implemented |
| SYN-02 | J11 | 0006 | `ConflictResolutionTest`, E2E | none | Not implemented |
| SYN-03 | J10 | 0006 | `RoleAndRevocationTest`, threat-model review | none | Not implemented |
| BUD-01 | J4 | 0002 | `BudgetTest` | none | Not implemented |
| A11Y-01 | all | 0001 | Compose accessibility checks; M with screen reader | none | Not implemented |
| I18N-01 | all | 0001 | `StringsCompleteTest`, RTL M | none | Not implemented |
