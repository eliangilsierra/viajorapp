# ADR-0005: Optional AI with bring-your-own-key and reviewable proposals

## Status

Accepted (2026-10-27). The owner approved it in conversation on 2026-10-27 and asked for the approval to be recorded; it takes effect when the foundation Pull Request (issue #1) is merged.

## Context

AI must be optional, transparent and subordinate to the user. No Viajorapp server may exist, and keys
must never enter ordinary data.

## Decision

- `AiProvider` interface in `:domain`: `generate(request): AiResult`, with typed failures
  (`Unauthorized`, `RateLimited`, `QuotaExhausted`, `Timeout`, `Unavailable`, `MalformedResponse`,
  `Cancelled`). Adapters in `:integrations`: an Anthropic Messages adapter, an OpenAI-compatible
  chat-completions adapter (covers many hosted and local servers such as Ollama or LM Studio through a
  user-entered base URL). Adding a provider means adding one adapter and one registry entry.
- **Keys** live in a `SecretStore` (Android Keystore-backed encrypted preferences; iOS Keychain).
  Never in SQLDelight, exports, backups, logs, crash output or Compose state that is saved.
- AI runs only on a user action ("Suggest", "Reorganize"). A pre-send screen shows the provider host and
  exactly which trip fields will be sent; trip data not in that list is not sent.
- **Proposals, not edits.** The model must return JSON matching a versioned schema; the response is
  validated against the domain model (dates inside the trip, durations positive, referenced ids exist).
  The result is stored as an `AiProposal` (provenance: provider, model, prompt version, time, input
  summary) and rendered as a diff against the current itinerary. The user accepts all, accepts parts,
  edits or rejects. Acceptance is one transaction; the original stays recoverable (undo).
- Items carry a `provenance` enum: `USER`, `IMPORTED`, `VERIFIED`, `AI_SUGGESTED`. The UI shows it.
- The prompt instructs the model not to state opening hours, prices or availability as fact; the app
  displays such fields only if the user marked them verified.
- Tests use recorded fixtures and a fake provider, never a live key.

## Alternatives

A hosted proxy (forbidden: mandatory backend, key custody); on-device models only (not mature enough
across both platforms; remains an optional adapter via OpenAI-compatible local servers).

## Consequences

AI quality depends on the user's provider and model. All non-AI features are unaffected when AI is off
or unreachable.

## Security considerations

Prompt-injection through imported text is possible: imported text is delimited and the model output is
still validated and reviewed before any change. Keys are masked in the UI, replaceable and deletable.

## Operational considerations

Provider APIs change; adapters pin an API version header where the provider supports it and are covered
by contract fixtures.
