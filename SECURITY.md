# Security policy

## Supported versions

Viajorapp has no released version yet. Only the `develop` branch receives fixes.

## Reporting a vulnerability

Please do not open a public Issue with exploit details.

1. Use GitHub's private vulnerability reporting: the **Security** tab of the repository, then **Report a vulnerability**. If that option is not shown, open an Issue that says only that you have a security report and asks for a private way to send it; do not include details.
2. Describe what is affected, how to reproduce it, and what you think the impact is. Do not include real personal data or live credentials.

The maintainers aim to acknowledge a report within a few days. This is a volunteer project, so that is a goal, not a guarantee.

## What counts

Examples: a way to read another app's data, secrets (API keys) appearing in logs, exports or backups, an import file that executes code or exhausts memory, data sent to a server without the user's action, or a crypto mistake once sync exists.

## Current security posture, honestly

- There is no backend and no account system; nothing is sent anywhere by the app today.
- The local database is not encrypted by the app. It relies on the operating system's app sandbox and device encryption ([ADR-0002](docs/decisions/ADR-0002-local-persistence-sqldelight.md)).
- Encrypted sync and bring-your-own-key AI are designed ([ADR-0005](docs/decisions/ADR-0005-optional-ai-byok.md), [ADR-0006](docs/decisions/ADR-0006-optional-sync-and-collaboration.md)) but not implemented, so nothing here should be read as end-to-end encryption being available.
- The threat model is in [docs/architecture/THREAT_MODEL.md](docs/architecture/THREAT_MODEL.md).
- No independent security audit has been done.
