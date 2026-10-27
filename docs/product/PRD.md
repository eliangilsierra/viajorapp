# Viajorapp Product Requirements Document

Version 0.1 (2026-10-27). Status: draft for owner review. Nothing in this document is implemented yet;
implementation status is tracked only in [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md).

Tagline: **Plan smarter. Travel further.** Open-source, offline-first travel planning for Android and iOS.

## 1. Problem and product

Trip planning is scattered across spreadsheets, notes, map apps and booking emails. Planners that unify
it usually require an account, a connection and a subscription, and keep the user's plans on their
servers. Viajorapp keeps the whole plan on the device, works without connectivity, treats AI and sync
as optional add-ons the user controls, and is open source (MIT).

## 2. Personas

| Persona | Need | Key features |
| --- | --- | --- |
| **Marta, multi-city independent traveler** | Sequence several cities, days and transfers without losing track of time zones. | TRP-03, DAY-01, ACT-02, ACT-03, MAP-01 |
| **Ana and Luis, a couple** | Plan together, see each other's changes, resolve disagreements. | SYN-01, SYN-02, SYN-03 |
| **Joaquín, goes off-grid** | Itinerary and map available with no signal. | OFF behavior, OFM-01, RTE-01 |
| **Carla, has a spreadsheet** | Bring existing itinerary in without retyping or losing columns. | IMP-01, IMP-03, IMP-04 |
| **Sam, wants AI help without surrendering data** | Suggestions with their own key, shown before they change anything. | AI-01, AI-02, AI-03, PRV-01 |
| **Priya, limited data plan** | Nothing downloads unless she asks. | OFM-01, PRV-01, no background network |

## 3. Principles and non-goals

Principles, in priority order: offline-first; local data ownership; privacy by design; polished mobile
UX; reliable maps; transparent optional AI; optional sync; maintainability; real-device performance;
accessibility and i18n; minimal infrastructure and cost; separation of core and optional integrations.

Non-goals (1.0): booking or selling travel; a Viajorapp-run server or account system; real-time
availability, prices or opening hours; on-device turn-by-turn navigation; social feed; web or desktop
apps; analytics or telemetry (none by default).

Anti-goals: a feature that works only with a network; "synced" shown when it is not; AI that edits
silently; a route shown that is really a straight line.

## 4. Feature specifications

Priority: P0 essential for first useful release; P1 mature release; P2 later. Dependencies, offline
behavior, privacy, failure states and tests per feature are in [FEATURE_MATRIX.md](FEATURE_MATRIX.md).
"AC" = acceptance criteria; each AC must be covered by an automated test or a recorded manual check.

### 4.1 Trips

**TRP-01 Create and edit trips (P0).** Problem: start planning in seconds. Behavior: title, optional
description, start and end dates (or undated "someday" trip), cover image from local files, status
(`planning | upcoming | in progress | completed`, derived from dates unless overridden). AC: a trip can be
created with only a title; end date is not before start date; edits persist after process death; changing
dates warns if items fall outside the new range and offers to keep or move them.

**TRP-02 Duplicate, archive, delete (P0).** AC: duplicate copies days, items, places and notes with new
ids and none of the sync/provenance history; archive hides from the default list and is reversible;
delete is soft with an Undo for at least 10 seconds and a trash list for 30 days; "Delete forever"
requires confirmation and purges rows, attachments, offline packs and cached routes of that trip.

**TRP-03 Multiple destinations (P0).** AC: a trip has an ordered list of destinations with optional date
ranges and time zone; days inherit the destination zone; moving a day between destinations is supported.

**TRP-04 Templates and reusable activities (P1).** AC: save a day or activity as a template; insert a
template into any trip; templates carry no personal booking references unless chosen.

### 4.2 Itinerary

**DAY-01 Day-by-day list (P0).** The always-available, map-independent view of the itinerary. AC: one
section per day with local date and weekday; items in order; empty-day state with an add action; works at
200% font scale; fully screen-reader navigable.

**DAY-02 Timeline view (P1).** AC: items with start time and duration drawn on a time axis; untimed items
listed separately; travel legs between items drawn with their source (routed vs estimate).

**ACT-01 Activities with optional fields (P0).** AC: only a title is required; optional category, place,
start (date/time/zone), duration, notes, tags, URL, cost; saving never fabricates missing values.

**ACT-02 Reorder and move (P0).** AC: drag to reorder; every drag action also has a non-drag alternative
(move up/down, move to day) for accessibility; reorder persists and touches one row; cross-day moves keep
duration and, if timed, ask whether to keep the clock time.

**ACT-03 Conflict warnings and time handling (P0).** AC: overlapping timed items on the same day show a
warning (not a block); overnight items are supported; trips spanning time zones show each item in its own
local time; DST-gap and DST-overlap times are resolved by a documented rule and tested.

**ACT-04 Notes, tags, categories (P0).** AC: free-text notes (plain text), user tags, built-in
categories (sight, food, lodging, transport, shop, activity, custom).

**ACT-05 Completion status (P0).** AC: mark done/undone with one tap; done items render distinctly in
list and map; progress per day and per trip is computed from it.

**RSV-01 Reservation and reference info (P1).** AC: confirmation code, provider, contact, check-in/out,
attachment metadata (file name, type, size, local URI); a code is never auto-validated against a provider.

### 4.3 Places and maps

**PLC-01 Places and saved places (P0).** AC: place types (attraction, restaurant, lodging, station,
shop, activity, custom); name, address, coordinates (optional), notes; a saved place can be reused by
many activities; deleting a place used by activities asks what to do.

**PLC-02 Place search (P0 local, P1 online).** AC (P0): search saved places and activities by text, offline.
AC (P1): optional online geocoding provider, user-initiated (no search-as-you-type to a public service),
results clearly marked as from a provider, saved only on user action.

**MAP-01 Map with markers and list sync (P0).** AC: markers for the visible day and for saved places;
tapping a marker selects the item in the list and vice versa; re-center on destination; select a point to
create a place; the list shows everything the map shows (map is never the only access path); map failure
shows the list plus an explanation.

**MAP-02 Day route overlay (P1).** AC: line between a day's located items in order, styled by leg source;
planned vs completed styling is distinct without relying on color alone.

**MAP-03 Tile source configuration and attribution (P0).** AC: preset list plus custom style URL;
attribution always visible; a "no base map" mode; unreachable tiles show a clear non-blocking message.

**OFM-01 Offline map regions (P1).** AC: choose bounds and zoom range; show estimated tiles and size
and the source's limits before download; progress, pause, resume, cancel, delete; interrupted downloads
resume after restart; storage used is shown; sources with `offlinePolicy = forbidden` cannot start a
download.

**OFM-02 User-supplied tile package (P2).** Import a PMTiles/MBTiles file for large regions.

### 4.4 Routes

**RTE-01 Straight-line estimate (P0).** AC: distance by great-circle, duration by configurable walking
speed; always labeled "estimated, straight line"; works offline.

**RTE-02 Routed walking legs (P1).** AC: with a configured provider, a leg gets geometry, distance and
duration marked "routed"; failures fall back to the estimate with a reason; the host receiving
coordinates is shown in settings.

**RTE-03 Turn-by-turn instructions (P1).** Shown only when the provider returns maneuvers; not navigation.

**RTE-04 Offline routing (P2, not in 1.0).** Requires an on-device engine and regional data; not promised.

### 4.5 Import and export

**IMP-01 CSV import (P0).** AC: file picked locally; delimiter and encoding detection; column mapping with
auto-suggestions for common names (date, day, title, start, end, duration, place, address, lat, lon,
category, notes); multiple date formats with an explicit ambiguity prompt (DD/MM vs MM/DD); preview with
per-row warnings; duplicate detection; commit in one transaction; unrecognized columns are preserved as
notes or custom fields, never dropped silently; a rejected row never aborts the rest; the import report
is saved.

**IMP-02 JSON import (P0).** The versioned Viajorapp format (backup/export) plus a documented generic
schema; unknown versions are refused with an explanation, never partially applied.

**IMP-03 XLSX import (P1).** Same pipeline as IMP-01; reads cell values only (no formulas executed, no
macros, external links ignored); limits on size, sheet count and decompressed bytes.

**IMP-04 Plain-text import (P1).** Heuristic parser (date headings, time prefixes, bullets) with the same
preview; optional AI extraction (AI-03) is offered only if configured.

**EXP-01 Backup and restore (P0).** AC: one file containing all trips and settings that are not secrets;
restore into an empty or existing app with merge/replace choice and preview; round trip equals the
original (tested); keys, tokens and sync secrets are never included.

**EXP-02 Itinerary export (P0).** JSON, CSV and Markdown via the system share sheet. **EXP-03 PDF
export (P2).** Readable pagination.

### 4.6 Search, settings, privacy

**SRC-01 Search and filter (P0).** AC: by trip, destination, date, text, category, tag, completion; local
and instant on a 1,000-item trip (measured).

**SET-01 Settings (P0).** Theme (system/light/dark), units, language, default walking speed, tile source,
routing provider, AI provider, data management.

**PRV-01 Privacy center (P0).** AC: shows what is stored, which optional providers are configured and what
each would receive, last network activity per provider, export all data, delete all data (including
secrets, packs and caches) with confirmation. The app makes no network request until a feature that needs
one is used.

### 4.7 AI (optional)

**AI-01 Provider and key setup (P1).** AC: choose provider/base URL/model, paste key, test connection,
replace, delete; key masked and kept only in the secret store.

**AI-02 Proposals with review (P1).** AC: draft itinerary, suggest activities, reorder a day, alternative
plans; each returns a validated proposal shown as a diff; accept all/part/edit/reject; original preserved
until acceptance; cancel at any time; malformed/timeout/rate-limit/quota errors have specific messages.

**AI-03 Extraction and review (P1).** Extract structured activities from text, summarize an import, list
missing information, feasibility check using only data in the trip. Facts the model cannot know
(hours, prices, availability) are never shown as verified.

### 4.8 Sync and collaboration (optional)

**SYN-01 Own-device sync (P1)**, **SYN-02 Conflict resolution (P1)**, **SYN-03 Shared trips (P2)**: see
[ADR-0006](../decisions/ADR-0006-optional-sync-and-collaboration.md). AC highlights: local-only remains a
complete mode; status is truthful; offline edits followed by reconnection merge deterministically;
conflicts are shown with both values and never silently resolved; revocation is documented with its
limits.

### 4.9 Cross-cutting

**ONB-01 Onboarding (P0)**: no account screen; explains local ownership; first action is "Create a trip";
all permissions requested in context. **A11Y-01 (P0)**: labels, 48 dp targets, 4.5:1 text contrast, 200%
text scale, focus order, non-color status cues, list alternatives to the map. **I18N-01 (P0)**: all
strings externalized, English and Spanish at 1.0, locale-aware dates/units, right-to-left layout verified.
**BUD-01 Budget (P2).**

## 5. Non-functional requirements (targets to be measured, not yet verified)

| Area | Target | Verification |
| --- | --- | --- |
| Cold start to interactive trip list | under 2 s on a mid-range device | macrobenchmark, recorded |
| Open a 300-item trip | under 300 ms to first frame | benchmark |
| Search over 1,000 items | under 100 ms | instrumented test |
| Import 5,000 rows | no UI jank, cancellable, memory bounded | test with generated file |
| Network requests at idle | zero | traffic check in audit |
| Telemetry | none | audit |

## 6. Offline behavior (summary)

| Situation | Behavior |
| --- | --- |
| First launch, no network | Fully usable; no step needs a network. |
| Edit trips | Always local, instant. |
| Map | Base map needs network or a downloaded region; markers/list always work. |
| Routing | Estimate offline; routed legs only when cached or online. |
| AI | Disabled with an explanation; nothing queued silently. |
| Sync | Pending changes accumulate; status says so. |
| Interrupted download | Resumes or can be cancelled; partial packs are never shown as ready. |

## 7. Open questions for the owner

1. Approve the recommended brand concept (see `docs/design/BRAND_STRATEGY.md` once available).
2. Which languages beyond English and Spanish should 1.0 ship?
3. Is the Apache/MIT-only dependency policy acceptable (excludes GPL tools such as BRouter)?
4. Is there access to a Mac or macOS CI runner for iOS validation?
5. Application ID / bundle ID domain (for example `io.github.eliangilsierra.viajorapp`).
