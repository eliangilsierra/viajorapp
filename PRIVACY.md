# Privacy

This document describes what Viajorapp does with data **today** and what is planned. It is not a legal policy; the maintainers have not had one reviewed.

## Today (version 0.1, foundation)

- No account, no sign-in, no backend run by the project.
- No analytics, crash reporting or telemetry.
- The app opens a welcome screen only. It stores no trip data yet and makes no network requests.
- The Android manifest declares the `INTERNET` permission for future map tiles. Nothing uses it today.
- Android automatic cloud backup is switched off (`allowBackup=false`).

## Planned, and what each will mean for your data

| Feature | What leaves your device | When |
| --- | --- | --- |
| Trips, days, places, activities | Nothing. Stored in a local database on the device, which the app does not encrypt (the operating system protects it). | Planned |
| Map tiles | Requests for the map area you view go to the tile provider you choose, which can therefore see roughly where you look. The provider and its terms are shown in the app. | Planned |
| Travel estimates | Nothing for straight-line estimates. A routing provider, if you configure one, receives the coordinates of the legs you ask about. | Planned |
| Import and export | Files you choose, handled on the device. Exports never include API keys. | Planned |
| AI assistant (optional) | Only the trip content you choose to send, after a disclosure screen, to the provider **you** configure with **your own key**. Keys are kept in the platform secret store and never in the database, exports or logs. Results are proposals you review. | Planned |
| Sync (optional) | Encrypted change files written to a folder you choose; the project runs no server. | Planned |

Details of every data item are in [docs/reference/DATA_INVENTORY.md](docs/reference/DATA_INVENTORY.md).

## Deleting your data

Today there is nothing to delete. When trips exist, deleting a trip and "delete everything" will remove the local data; copies you exported or synced to your own folder stay where you put them.

## Questions

Open an Issue. For security problems use [SECURITY.md](SECURITY.md).
