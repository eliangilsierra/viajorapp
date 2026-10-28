# Contributing to Viajorapp

Thank you for helping. This project is small and still forming, so the rules below are about keeping changes reviewable and honest.

## Before you start

1. Open or pick an Issue. One task Issue is one branch and one Pull Request.
2. For anything that changes the architecture, privacy behaviour or data format, read the [ADRs](docs/decisions/README.md) first; a change that contradicts an accepted ADR needs a new ADR.
3. Ask before adding a dependency, a network call, analytics of any kind, or anything that needs an account or a paid service. The project does not accept these as shortcuts.

## Branches, commits and Pull Requests

- Branch from `develop`: `<feature|bugfix|refactor|chore|docs>/<issue-number>-<short-description>`.
- Conventional Commits for every commit and every PR title: `<type>(<scope>): <description>` with type `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore` or `revert`.
- The PR targets `develop`, uses the template, and has one `Closes #<issue>`.
- Never push to `develop` or `main`. A human code owner reviews and squash-merges.
- Keep a PR small and say what you did **not** verify (a device, iOS, a release build).

## Build and test

See [docs/how-to/build-and-test.md](docs/how-to/build-and-test.md). A change is not ready until the tests that cover it pass and lint passes.

- New behaviour needs a test; a bug fix needs a test that failed before the fix.
- Tests use fakes and recorded fixtures. They must not call live network services or paid APIs.
- Do not commit secrets, keys, keystores, `local.properties` or personal data. Test values that look like keys should be assembled in the test so secret scanners do not flag them.

## Code style

- Kotlin, following the existing code. Shared logic lives in `domain/` as pure functions; platform and storage code stays out of it.
- Screens use theme tokens from `designsystem/`, never colour literals, and user-visible text lives in the string resources for English and Spanish.
- Be honest in the UI: do not present an estimate as a routed path, or an unavailable feature as available.

## Reporting problems

Bugs and ideas go in Issues. Security problems follow [SECURITY.md](SECURITY.md), not a public Issue.

By contributing you agree that your contribution is licensed under the project's [MIT licence](LICENSE) and that you follow the [code of conduct](CODE_OF_CONDUCT.md).
