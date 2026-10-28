# Release the iOS app

**Nothing about iOS has been built, run or validated.** The maintainers' environment is Windows; Kotlin/Native iOS builds need macOS and Xcode. The shared modules declare `iosArm64` and `iosSimulatorArm64` targets, but they have never been compiled, and the `iosApp/` Xcode project does not exist yet.

## What is needed before any iOS release

| Need | Who | Status |
| --- | --- | --- |
| A Mac with a current Xcode, or a macOS CI runner | Owner | Not available |
| Compile `:domain`, `:data`, `:designsystem`, `:ui` for iOS and fix what breaks | Developer on macOS | Not done |
| Create `iosApp/` (SwiftUI host for the Compose UI) and wire the SQLDelight native driver | Developer on macOS | Not started |
| Copy `design/brand/export/ios/AppIcon.appiconset` into the asset catalog and check it in Xcode | Developer on macOS | Files generated, never opened in Xcode |
| Keychain-backed secret store for the optional AI key (ADR-0005) | Developer on macOS | Not started |
| Apple Developer Program membership, signing certificates, provisioning | Owner | Not done; paid and subject to Apple's terms |
| TestFlight testing on real devices | Owner and testers | Not done |
| App Store listing, privacy nutrition labels, review | Owner | Not done |

## Steps once a Mac exists

1. Run the shared tests for the iOS targets and the app on a simulator; record the results in `docs/PROJECT_STATUS.md`.
2. Create `iosApp/` and build it in Xcode; run on a simulator, then on a device.
3. Upload to TestFlight; test offline use, map rendering and the data-deletion paths.
4. Fill App Store Connect (listing, privacy labels using [PRIVACY.md](../../PRIVACY.md)) and submit for review.

## Needs the owner's decision

Joining the Apple Developer Program (fee and legal terms), the bundle identifier, and providing macOS access or a runner. Until then no document or message from the project should claim iOS support.
