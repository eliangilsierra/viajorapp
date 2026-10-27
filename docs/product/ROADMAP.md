# Roadmap

Dates are intentionally absent: the roadmap is ordered by dependency and verified acceptance, not
calendar. A feature is on a release only when its row in
[REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md) says Tested.

| Release | Theme | Contents | Exit criteria |
| --- | --- | --- | --- |
| 0.1 | Foundation | Gradle/KMP project builds on Android; CI; design tokens and theme; empty app shell; schema v1 with migrations; version spike | CI green; app launches on Android; unit tests pass; spike report |
| 0.2 | Trips and itinerary | TRP-01..03, DAY-01, ACT-01..05, PLC-01, SRC-01, SET-01, ONB-01, I18N-01 (en, es) | E2E J1, J2, J4, J6 pass; restart persistence verified; a11y checks pass |
| 0.3 | Maps and estimates | MAP-01, MAP-03, PLC-02 local, RTE-01 | Map/list sync tests; map failure fallback verified |
| 0.4 | Import/export/backup | IMP-01, IMP-02, EXP-01, EXP-02, PRV-01 | E2E J3, J12; round-trip test |
| 0.5 | Offline maps and routes | OFM-01, RTE-02, RTE-03, MAP-02 | Region spike results documented; interrupted-download test |
| 0.6 | Optional AI | AI-01..03, IMP-03, IMP-04 | E2E J9 with fake provider; secret-leak tests |
| 0.7 | Optional sync | SYN-01, SYN-02 | E2E J11 with fake transport; crypto spike accepted |
| 1.0 | Release candidate | Hardening, performance budgets, accessibility audit, docs, store-readiness checklist | Release audit passes; iOS validated on macOS or reported unverified |
| 1.x / 2.0 | Later | SYN-03, TRP-04, RSV-01, DAY-02, EXP-03, OFM-02, BUD-01, RTE-04 | Per feature |

Order may change if a documented dependency requires it (for example the crypto spike gating sync).
