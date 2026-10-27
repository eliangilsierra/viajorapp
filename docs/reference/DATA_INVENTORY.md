# Data inventory

Planned state per the [PRD](../product/PRD.md) and ADRs. Nothing here is implemented yet; when a row is
implemented, the privacy audit must confirm it. "Leaves device" means the data is sent anywhere without a
user action that names the destination.

| Data | Stored where | Leaves device? | Receives it (if ever) | Retention | Export / delete |
| --- | --- | --- | --- | --- | --- |
| Trips, days, activities, notes, tags | App SQLite database (app-private storage) | No | Only a participant's storage folder if sync is enabled, as ciphertext | Until the user deletes it; soft-deleted items purged after 30 days | Backup/JSON/CSV export; delete trip; Delete all data |
| Places and coordinates | Same database | No (viewport tile requests reveal the viewed area, see below) | Routing host only on a user-requested route | Same | Same |
| Reservation references | Same database | No | Not included in AI requests unless the user ticks them | Same | Excluded from templates; included in backup only if chosen |
| Attachment metadata (name, type, size, local URI) | Database | No | None | Same | Same; files are not copied unless the user imports them |
| Preferences (theme, units, language) | Settings store | No | None | Until reset | Delete all data |
| Map tile and style requests | Not stored beyond the map library cache | **Yes, when a map is shown**: the viewed tile area, IP address and the `Viajorapp/<version>` User-Agent | The configured tile provider | Provider's policy; local cache cleared by Delete all data | "No base map" mode avoids it |
| Offline map packs | Map library storage; metadata in database | Download requests to the tile provider | The tile provider | Until the user deletes the region | Delete region / Delete all data |
| Routing cache | Database | Route requests only on user action | The configured routing host | Until trip deleted or cache cleared | Cache cleared with trip |
| AI provider settings (provider, base URL, model) | Settings store | No | None | Until changed | Delete all data |
| **AI API key** | Platform secret store (Android Keystore-backed, iOS Keychain), never the database | Only to its own provider on an AI action | The configured AI provider | Until the user deletes it | Replace/delete in Settings; excluded from backups and logs |
| AI requests (selected trip fields) | Not stored by Viajorapp except as an `AiProposal` result with provenance | **Only on user action**, after a disclosure screen | The configured AI provider | Provider's policy | Proposals deleted with the trip |
| Sync segments (encrypted changes) | The user's chosen folder | Yes, to whatever service syncs that folder | The user's storage provider (ciphertext) and participants | Until compacted or deleted | Disable sync; delete folder |
| Sync trip keys | Platform secret store | Shared with participants out of band | Invited participants | Until revoked or trip deleted | Key export warning; deleted with trip |
| Logs | Memory / local file with redaction | No | None | Cleared on request | Delete all data |
| Analytics / telemetry / crash reporting | **None collected** | No | None | n/a | n/a |

Rules enforced by tests (planned): no secret appears in any export or log; no network request occurs at
idle; Delete all data leaves no trip, secret, pack or cache behind.
