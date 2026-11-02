# Create and protect the Android signing keys

**Nothing in this guide has been done.** No key, keystore or Play account exists for this project. Creating them is the owner's decision and action; agents must not do it.

## Why this matters

Play uses two keys. The **upload key** signs the bundle you upload. **Play App Signing** holds the key that signs what users install. Losing the upload key is recoverable through Play support; leaking it lets someone upload a malicious update. Treat it like a password.

## Steps (owner)

1. Generate an upload keystore outside the repository, on a machine you trust. The values in angle brackets are yours to choose; use a long random password from a password manager.

   ```bash
   keytool -genkeypair -v -keystore <path-outside-repo>/viajorapp-upload.jks \
     -alias <alias> -keyalg RSA -keysize 4096 -validity 9125
   ```

2. Back up the keystore and its passwords in two separate places (for example an encrypted drive and a password manager). A keystore alone is useless without its passwords and vice versa.
3. Enrol the app in Play App Signing when creating the app in Play Console, and upload the **public certificate** only when Play asks for it.
4. Give Gradle the secrets without committing them. Planned, not implemented: read `storeFile`, `storePassword`, `keyAlias` and `keyPassword` from environment variables or from an untracked `keystore.properties`, and apply the `release` signing config only when they are present, so ordinary debug builds and CI keep working without keys. This needs a small code change in `androidApp/build.gradle.kts` made in a normal Pull Request.
5. If a CI job ever signs releases, store the values as encrypted repository secrets and review the workflow as a security change. Today no workflow signs anything.

## Rules

- `.gitignore` already excludes `*.jks`, `*.keystore` and `*.p12`; do not rely on it as the only protection.
- Never paste passwords into Issues, Pull Requests, logs or chat. Do not give them to an agent.
- If a key may have leaked, stop publishing, tell the maintainers, and follow Play's key-reset process.

## Verification status

Not verified: key creation, signing, Play enrolment. What was verified is only that an **unsigned** release build exists (see [Release the Android app](release-android.md)).
