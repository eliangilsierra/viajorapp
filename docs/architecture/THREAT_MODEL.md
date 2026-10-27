# Threat model (initial)

Scope: the planned application and its optional integrations as designed in ADR-0001 to ADR-0006. No
code exists yet; every mitigation below is a requirement to implement and verify, not a claim.

## Assets

1. Trip content (itineraries, places, reservation references): private and sometimes sensitive (home
   and hotel locations, travel dates imply an empty home).
2. Secrets: AI API keys, sync trip keys, WebDAV credentials.
3. Integrity of the user's plan (silent AI or sync overwrites are a harm).
4. Availability of the itinerary while offline.

## Trust boundaries

Device sandbox | optional tile/route/geocode/AI providers | user's storage provider for sync | other
participants | imported files | the build/distribution chain.

## Threats and mitigations

| # | Threat | Mitigation (to implement and test) | Residual risk |
| --- | --- | --- | --- |
| T1 | API key leaked via logs, export, backup, screenshot of state or crash dump | `SecretStore` only; redacting logger; export/backup schema has no secret fields; unit test searches outputs for a canary key; masked UI | Rooted devices can read Keystore-protected data in some scenarios |
| T2 | Malicious import file (zip bomb, huge CSV, formula/macro, path traversal, XML entity expansion) | Size, entry-count and decompressed-byte limits; no formula evaluation; no external entities; no file paths taken from content; fuzz tests | Resource exhaustion on very old devices |
| T3 | Prompt injection through imported text steering an AI provider | Imported text delimited and labeled data; output validated against schema; changes only as reviewable proposals; no tools given to the model | The proposal text itself may be misleading; the user reviews it |
| T4 | AI fabricates facts (hours, prices, availability) | Prompt forbids; provenance badges; such fields not shown as verified unless user-marked | Users may still trust suggestions |
| T5 | Tile/route/geocode providers learn location interest | Disclosure in Privacy center; user-initiated routing; "No base map" mode; no default routing endpoint | Tile requests reveal viewed area by design |
| T6 | Sync: storage provider reads or modifies files | Authenticated encryption per segment under per-trip key; tampered segments quarantined | Provider sees file names, sizes, timing; deletion/withholding by provider possible |
| T7 | Sync: revoked participant keeps data/key | Key rotation on revocation; documented that seen data cannot be recalled | Cooperative enforcement of roles (see ADR-0006) |
| T8 | Sync: stale or clock-skewed device overwrites newer data | HLC with drift detection; conflicts preserved, not auto-resolved | Wrong choice by the user in the resolver |
| T9 | Local attacker with device unlocked or backup extraction | OS sandbox and device encryption; Android backup rules exclude secrets; optional SQLCipher later | Database not encrypted by the app in 1.0 |
| T10 | Malicious custom tile/style/routing URL (SSRF-like to private hosts, cleartext) | https required unless user confirms a local/private address; no credentials in URLs; response size limits; no redirects to other schemes | A confirmed local host is the user's decision |
| T11 | Supply-chain compromise via dependencies or CI | Pinned versions and checksums (Gradle wrapper checksum, dependency verification planned); Dependabot; license check; secret scan in CI; no CI secrets needed for builds | Upstream compromise before detection |
| T12 | Deep links / intents triggering destructive actions | No destructive deep links; verified App Links only when added; intents validated | n/a |
| T13 | Accidental data loss (delete, bad import, bad migration) | Soft delete + undo + trash; import preview and single transaction; automatic pre-migration backup; backup/restore tests | User ignores warnings |

## Out of scope for 1.0

Compromised OS, hardware attacks, nation-state adversaries, anonymity from tile providers.
