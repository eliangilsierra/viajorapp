# Waypoint icon exports

Generated locally from the approved Waypoint concept; no remote service was used.

| Source | SHA-256 (first 16) |
| --- | --- |
| `../concepts/01-waypoint/app-icon.svg` | 0a8c6b62631040fd |
| `../concepts/01-waypoint/app-icon-foreground.svg` | b47134ae3bd8e10d |

- `android/ic_launcher-playstore.png`: 512 px store icon (for the owner's later store listing; nothing is submitted).
- `ios/AppIcon.appiconset`: 1024 px opaque icon plus `Contents.json`. Copy to `iosApp/iosApp/Assets.xcassets/` when the iOS app module exists. **Never opened in Xcode.**
- The Android resources used by the app live in `androidApp/src/main/res/mipmap-*`.

Checks run: the generator's `verify` gate (29 checks, 0 problems), safe-zone check (0% of visible pixels outside the safe circle), and visual inspection of the 512 px icon and the monochrome layer. Not checked: appearance on a real launcher or device, iOS dark and tinted variants (not produced), trademark clearance.
