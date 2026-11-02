# Requirements traceability

Status vocabulary: **Not implemented**, **Partially implemented**, **Implemented**, **Tested**,
**Blocked**. A requirement is **Tested** only when the named automated test or recorded manual check has
run and passed. Updated 2026-11-02 (MVP). **Tested** here means the named automated tests passed and the feature was also exercised by hand on an Android emulator (not a physical device, never on iOS). **Implemented** means code and automated tests exist but it was not exercised by hand. Rows marked Partially implemented name what is covered.

| ID | Journeys | ADR | Planned tests | Code | Status |
| --- | --- | --- | --- | --- | --- |
| ONB-01 | J1 | 0001 | UI onboarding; M | none | Not implemented |
| TRP-01 | J2 | 0002 | `TripRepositoryTest`, UI create trip | `TripRepository`, `TripListScreen`, `TripEditorScreen` with date pickers; tests `RepositoriesTest`, `PresentersTest`; emulator runs (create, list, persist, date picker opens on the stored date) | Tested |
| TRP-02 | J12 | 0002 | `TripLifecycleTest`, E2E delete | `TripRepository` (delete, restore, archive) and list screen undo; repository and presenter tests; delete and archive not exercised by hand | Implemented |
| TRP-03 | J2, J4 | 0002 | `DestinationTest` | none | Not implemented |
| TRP-04 | J4 | 0002 | `TemplateTest` | none | Not implemented |
| DAY-01 | J2, J4 | 0001 | UI day list, a11y | `DayPlanner`, `SqlTripRepository` days, `ItineraryScreen`; tests `TripDaysTest`, `RepositoriesTest`; emulator run (days listed per date) | Tested |
| DAY-02 | J6 | 0004 | UI timeline | none | Not implemented |
| ACT-01 | J4 | 0002 | `ActivityValidationTest` | `SqlItineraryRepository`, activity dialog; tests `RepositoriesTest`, `ItineraryQueriesTest`; emulator run (activity with times) | Tested |
| ACT-02 | J6 | 0002 | `OrderingTest`, UI drag/alt | `FractionalIndex`, `moveActivity`, menu up/down and Move to another day; repository and presenter tests; menu moves not exercised by hand; no drag | Implemented |
| ACT-03 | J4 | 0002 | `TimeRulesPropertyTest` | `Overlap`, `ZonedTimes`; times stored with zone via the time picker; overlap warnings not shown in the UI | Partially implemented |
| ACT-04 | J4 | 0002 | `TagRepositoryTest` | none | Not implemented |
| ACT-05 | J5 | 0002 | `CompletionTest` | completion checkbox, `setCompleted`; repository and presenter tests; not exercised by hand | Implemented |
| RSV-01 | J4 | 0002 | `ReservationTest` | none | Not implemented |
| PLC-01 | J5 | 0002 | `PlaceRepositoryTest` | `SqlItineraryRepository` places, `PlaceUi.kt`; tests `RepositoriesTest`, `PlacesPresenterTest`; emulator run (two places with coordinates) | Tested |
| PLC-02 | J5 | 0002, 0003 | `SearchTest`, geocoder fake | none | Not implemented |
| MAP-01 | J5 | 0003 | map/list sync UI test; M | `MapScreen`, `MapSurface` (MapLibre, markers); emulator run (map centred on two places, tiles loaded, markers visible); no place picking from the map | Tested |
| MAP-02 | J6, J8 | 0003, 0004 | overlay UI | none | Not implemented |
| MAP-03 | J5, J7 | 0003 | `TileSourceTest` | `TileSource`, `TileSourcePresets`, attribution shown on the map screen; `TileSourceTest`; emulator run | Tested |
| OFM-01 | J7 | 0003 | `OfflinePackTest`, M on device | `domain/.../OfflineEstimate.kt` (tile arithmetic and a pre-download size estimate only; nothing is downloaded or stored) | Partially implemented |
| OFM-02 | J7 | 0003 | `TilePackageImportTest` | none | Not implemented |
| RTE-01 | J8 | 0004 | `StraightLineEstimatorPropertyTest` | `TravelEstimate`, `LegResolver`, `EstimateLine`; tests `TravelEstimateTest`, `RoutingTest`; emulator run (6.1 km labelled as an estimate) | Tested |
| RTE-02 | J8 | 0004 | `ValhallaAdapterContractTest` | none | Not implemented |
| RTE-03 | J8 | 0004 | `ManeuverMappingTest` | none | Not implemented |
| RTE-04 | J7, J8 | 0004 | not planned | none | Not implemented |
| IMP-01 | J3 | 0002 | `CsvImportTest`, E2E | `CsvReader`, `ImportPresenter`, `ImportScreen` (CSV with header columns title, day, start, end, notes); `CsvTest`, `ImportPresenterTest`; CSV file import not exercised by hand | Implemented |
| IMP-02 | J3 | 0002 | `JsonImportTest` | `BackupCodec.decode`, `SqlBackupService.restore`, Settings > Restore; tests `BackupTest`, `BackupServiceTest`, `SettingsPresenterTest`; emulator run | Tested |
| IMP-03 | J3 | 0002 | `XlsxImportTest`, zip-bomb test | none | Not implemented |
| IMP-04 | J3 | 0002, 0005 | `TextImportTest` | `TextItineraryParser`, `ImportScreen` (paste, review, import); tests `TextItineraryParserTest`, `ImportPresenterTest`; emulator run (review of a pasted line) | Tested |
| EXP-01 | J12 | 0002 | `BackupRoundTripTest`, E2E | `SqlBackupService.export`, `BackupCodec.encode`, Settings > Export; tests `BackupServiceTest`, `SettingsPresenterTest`; emulator run (valid JSON written) | Tested |
| EXP-02 | J12 | 0002 | `ExportFormatTest` | `PlanExport` (Markdown and CSV), Export menu; `PlanExportTest`, round-trip test with the importer; not exercised by hand | Implemented |
| EXP-03 | J12 | none | M | none | Not implemented |
| SRC-01 | J4 | 0002 | `SearchPerfTest` | `SqlSearchRepository` (FTS4, ADR-0007), `SearchScreen`; tests `SearchTest`, `SearchPresenterTest`, `MigrationTest`; emulator run (prefix search found an activity) | Tested |
| SET-01 | all | 0001 | UI settings | `SettingsScreen` (data section only: backup, restore, delete-all); emulator run | Partially implemented |
| PRV-01 | J1, J9, J12 | 0002, 0005 | `DeleteAllTest`, network-idle audit | `SqlBackupService.deleteAllData`, confirmation dialog; tests; emulator run; the network-idle audit and the privacy copy review are not done | Partially implemented |
| AI-01 | J9 | 0005 | `SecretStoreTest`, `KeyLeakTest` | `domain/.../SecretRedaction.kt` (redactor and export guard only; no platform secret store, logging wiring or settings screen) | Partially implemented |
| AI-02 | J9 | 0005 | `ProposalValidationTest`, E2E | `AiProposal.kt`, `AiProposalJson.kt` (model, validation, diff, apply, answer parsing, prompt preview); no provider, key storage or review screen | Partially implemented |
| AI-03 | J3, J9 | 0005 | `ExtractionTest` | none | Not implemented |
| SYN-01 | J10 | 0006 | `SyncRoundTripTest` | none | Not implemented |
| SYN-02 | J11 | 0006 | `ConflictResolutionTest`, E2E | `domain/.../SyncMerge.kt` (HLC and three-way field merge as pure functions; no change files, transport, encryption or UI) | Partially implemented |
| SYN-03 | J10 | 0006 | `RoleAndRevocationTest`, threat-model review | none | Not implemented |
| BUD-01 | J4 | 0002 | `BudgetTest` | `domain/.../Money.kt` (exact money type and per-currency totals; no cost fields in the schema, repository or UI) | Partially implemented |
| A11Y-01 | all | 0001 | Compose accessibility checks; M with screen reader | none | Not implemented |
| I18N-01 | all | 0001 | `StringsCompleteTest`, RTL M | none | Not implemented |
