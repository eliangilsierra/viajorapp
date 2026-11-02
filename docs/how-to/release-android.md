# Release the Android app

This guide prepares a release. **Nothing has been published, signed or submitted**, and publishing needs decisions only the owner can make (see the end of this page).

## What has been verified

On Windows 11 with JDK 17, from `develop`:

```bash
./gradlew :androidApp:assembleRelease :androidApp:lintRelease
./gradlew :androidApp:bundleRelease
```

Both finished with `BUILD SUCCESSFUL`. Results:

| Artifact | Size | Notes |
| --- | --- | --- |
| `androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk` | about 40 MB | R8 minification is on; unsigned, so it cannot be installed as is |
| `androidApp/build/outputs/bundle/release/androidApp-release.aab` | about 19.5 MB | What Play expects; Play delivers per-device splits |

The package contains native libraries for four ABIs (arm64-v8a, armeabi-v7a, x86, x86_64) because of MapLibre. Size is tracked, not optimised.

**Not verified:** that the minified build **runs**. No emulator or device was available, and R8 can remove code that is only reached by reflection (MapLibre is a likely place). Install the release build on a real device and open every screen before trusting it.

## Steps to publish (owner)

1. Create the signing keys: [Create and protect the Android signing keys](signing-keys.md).
2. Decide the final application ID. `io.github.eliangilsierra.viajorapp` is confirmed for now, but it cannot be changed after the first Play upload.
3. Set `versionName` and `versionCode` in `androidApp/build.gradle.kts` (currently `0.1.0` and `1`). `versionCode` must rise with every upload.
4. Build the signed bundle (after the signing change is merged): `./gradlew :androidApp:bundleRelease`.
5. In Play Console create the app and upload the bundle to **internal testing** first; test on real devices.
6. Complete the store listing, content rating, data-safety form and target audience. Use [PRIVACY.md](../../PRIVACY.md) as the source for the privacy answers, and host a public privacy policy URL, because Play requires one.
7. Review the current Play policy and target-API requirements on the day you publish; they change.
8. Promote to production only after the checklist in [Store readiness checklist](../reference/store-readiness-checklist.md) is complete.

## Needs the owner's decision

- Opening a Play Console developer account (a fee and legal terms apply).
- Accepting Play's developer agreements and distribution terms.
- The app name, listing text and a hosted privacy policy URL.
- Whether to publish at all while no feature is usable (version 0.1 only shows a welcome screen).
