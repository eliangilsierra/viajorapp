# Store readiness checklist

Status as of the last update of this page. "Verified" means a command or inspection was run; everything else is open. Nothing has been submitted anywhere.

| Area | Item | Owner | Status |
| --- | --- | --- | --- |
| Build | Unsigned release APK and AAB build with R8 | Maintainers | Verified (see [Release the Android app](../how-to/release-android.md)) |
| Build | Release build runs on a real device | Maintainers | Not verified |
| Build | iOS build | Maintainers | Not possible without macOS |
| Product | At least the core trip and itinerary flow works | Maintainers | Not done (welcome screen only) |
| Product | Offline use verified with the network off | Maintainers | Not done |
| Quality | Unit tests pass | Maintainers | Verified for `:domain`, `:data`, `:designsystem` |
| Quality | Accessibility pass with a screen reader, large text, contrast | Maintainers | Contrast of the colour tokens tested; the rest not done |
| Quality | Start-up time and memory measured | Maintainers | Not done |
| Privacy | `PRIVACY.md` matches real behaviour | Maintainers | Matches today's code; re-check at every release |
| Privacy | Public privacy-policy URL | Owner | Not done |
| Privacy | Play data-safety form and App Store privacy labels | Owner | Not done |
| Security | Threat model reviewed against the code | Maintainers | Draft exists; not reviewed against features that do not exist yet |
| Security | Signing keys created and backed up | Owner | Not done ([guide](../how-to/signing-keys.md)) |
| Legal | Trademark search for the name and mark | Owner | Not done |
| Legal | Map tile provider terms and attribution shown in the UI | Maintainers | Attribution string exists; map not shown yet |
| Legal | Third-party licence notices bundled in the app | Maintainers | Not done |
| Brand | App icon (Android adaptive, iOS set) | Maintainers | Android wired; iOS files unopened in Xcode |
| Brand | Store graphics and screenshots | Maintainers | Not done |
| Accounts | Google Play Console account | Owner | Not done (fee, legal terms) |
| Accounts | Apple Developer Program | Owner | Not done (fee, legal terms) |
| Release | Version, changelog and release notes | Maintainers | Not done |
| Release | Release Pull Request `develop` to `main` | Owner | Not done |

A release should not be submitted while any item above that affects users or legal exposure is "Not done".
