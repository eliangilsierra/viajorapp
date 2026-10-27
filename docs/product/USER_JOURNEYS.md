# User journeys

Each journey lists the happy path, the offline behavior, failure states and the requirements it
exercises ([PRD](PRD.md)). Journeys marked E2E are the backbone of the end-to-end test plan.

## J1. First launch and onboarding (E2E)

1. Open the app. Three short screens explain: data stays on this device; no account needed; optional
   map, AI and sync features.
2. No permission prompt appears. The empty home offers **Create a trip** and **Import an itinerary**.
- Offline: identical. Failure: none expected; if storage init fails, an error with **Retry** and **Export
  logs (no trip data)** is shown.
- Requirements: ONB-01, PRV-01, A11Y-01, I18N-01.

## J2. Create a trip manually (E2E)

1. Tap **Create a trip**, enter a title (the only required field), optionally dates and a destination.
2. Land on the trip overview with day sections (or an "undated" list) and an **Add activity** action.
- Failure: end before start shows an inline message and keeps the input. App killed mid-edit: draft
  restored.
- Requirements: TRP-01, TRP-03, DAY-01.

## J3. Import an existing itinerary (E2E)

1. **Import**, pick a CSV/XLSX/JSON/text file from the system picker.
2. Review detected columns, correct the mapping, resolve date-format ambiguity.
3. Preview rows with warnings (missing date, duplicate, unparsable time). Choose to import valid rows
   and keep rejected rows in a report.
4. Commit; open the new trip. Unrecognized columns appear as notes.
- Offline: fully local. Failure: oversized or malformed file is refused with a reason; nothing is
  partially written.
- Requirements: IMP-01..04, EXP-01.

## J4. Plan a multi-day trip (E2E)

1. Add destinations with dates; days appear.
2. Add activities per day (title first; place, time, duration optional).
3. Overlap warnings appear on conflicting timed items; the user resolves or ignores.
- Requirements: TRP-03, ACT-01, ACT-03, ACT-04.

## J5. Explore places on a map

1. Open the map tab: markers for the day. Tap a marker to see the item; tap the item in the list to
   focus the map.
2. Long-press a point to create a place; search saved places by text.
- Offline without tiles: a notice explains the base map is unavailable; markers and the list remain.
- Requirements: MAP-01, MAP-03, PLC-01, PLC-02.

## J6. Organize by day and time (E2E)

1. Drag an item to a new position, or use **Move to** day/up/down (alternative to dragging).
2. Set start time and duration; the timeline view shows gaps and travel legs.
- Requirements: ACT-02, ACT-03, DAY-02.

## J7. Download a trip for offline use

1. Itinerary is already offline. For the map, open **Offline maps**, select region and zoom range.
2. The app shows tile count, size and the source's limits, then downloads with progress; pause, resume,
   cancel and delete are available.
3. Airplane-mode check inside the app lists which parts work offline: itinerary yes, map region yes/no,
   routing estimates yes, routed legs only if cached.
- Failure: limit exceeded → suggest smaller regions; storage full → clear message; app killed → resume.
- Requirements: OFM-01, RTE-01.

## J8. Walking routes and directions

1. Select a leg between two located items: it shows an **Estimated, straight line** value.
2. If a routing provider is configured, **Get route** requests geometry and shows **Routed** with
   distance, duration and instructions.
- Failure: provider down or quota → estimate remains with the reason.
- Requirements: RTE-01..03.

## J9. Ask AI to plan or reorganize (E2E with fake provider)

1. In Settings, add a provider and key (stored securely); test the connection.
2. In a day, choose **Suggest improvements**; a screen lists the provider host and exact data to send.
3. Receive a proposal; review the diff; accept parts, edit, or reject. Undo is available after accepting.
- Offline: action disabled with an explanation. Failure: malformed response, timeout, rate limit,
  quota, cancellation each have a distinct message; the itinerary is untouched.
- Requirements: AI-01..03, PRV-01.

## J10. Share a trip with a partner

1. Owner enables sync for the trip, picks a shared folder, generates an invite (QR/passphrase).
2. Partner scans, selects the same folder, and receives the trip.
- Status shows pending/synced truthfully. Failure: wrong key → refused with a clear message.
- Requirements: SYN-01, SYN-03.

## J11. Resolve synchronization conflicts (E2E with fake transport)

1. Two devices edit the same field offline, then reconnect.
2. The item shows a conflict badge; the resolver shows both values, author, time; the user picks or edits.
3. A delete versus edit conflict offers **Keep** or **Delete**.
- Requirements: SYN-02.

## J12. Export, back up and delete personal data (E2E)

1. **Settings → Data**: export backup (no secrets), export itinerary (JSON/CSV/Markdown).
2. Restore from a backup with a preview and merge/replace choice.
3. Delete a trip (Undo, trash) or **Delete all data** (confirmation; also removes secrets, packs,
   caches).
- Requirements: EXP-01, EXP-02, TRP-02, PRV-01.
