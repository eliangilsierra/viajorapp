# ADR-0006: Optional synchronization through encrypted change files

## Status

Accepted (2026-10-27). The owner approved it in conversation on 2026-10-27 and asked for the approval to be recorded; it takes effect when the foundation Pull Request (issue #1) is merged.

## Context

Local-only must be a complete mode. Sync between a user's devices and travel companions must not need a
Viajorapp backend or an account. Sharing a file is not safe concurrent collaboration by itself.

## Decision

- **Model:** per-record, per-field versioned changes with a Hybrid Logical Clock (HLC) and a device id,
  plus tombstones. Every local write also appends to a change log table.
- **Transport:** a storage-agnostic `SyncTransport` interface. First adapter: a folder chosen by the
  user (Android Storage Access Framework tree URI; iOS document picker with a security-scoped
  bookmark), which works with any sync app or cloud drive the user already uses. Second adapter
  (later): WebDAV with the user's credentials. No Viajorapp-operated endpoint.
- **Layout:** each device writes its own append-only, immutable segment files
  (`<tripId>/<deviceId>/<seq>.vjsync`). Devices never write another device's files, so file-sync tools
  cannot produce write conflicts on the same file.
- **Encryption:** segments are encrypted with an authenticated cipher (XChaCha20-Poly1305 or AES-GCM)
  under a per-trip key. The key is generated on the owner's device, stored in the `SecretStore`, and
  shared with a participant out of band (QR code or passphrase-wrapped invite). The storage provider
  sees only ciphertext, file names and sizes. The crypto library is chosen in a spike (libsodium
  bindings versus platform crypto behind an interface); this ADR does not claim end-to-end encryption
  until that implementation and its key-management model exist and are tested.
- **Roles:** owner, editor, viewer. The owner holds the trip key and a signed participant list; a
  viewer receives the key but the app ignores their segments (**limitation:** this is cooperative
  enforcement, not cryptographic; a malicious key holder can write files; documented in the threat
  model). Revocation rotates the trip key and re-encrypts forward; data already seen by a revoked
  participant cannot be recalled.
- **Merge rules:** different fields of one record merge automatically. Same field, different values
  from concurrent writers, or edit versus delete, is a **conflict**: both values are kept in a
  `conflict` table, the item shows a conflict badge, and the user chooses a value (or edits). Order
  changes use fractional keys; equal keys tie-break by item id. A stale device (clock far off or long
  offline) is detected by HLC drift and its changes are held for review instead of overwriting.
- **Status in the UI:** per trip: `Local only`, `Pending (n changes)`, `Synced at <time>`,
  `Needs attention (conflicts / failures)`. The UI never shows "synced" without a verified round trip.

## Alternatives

A hosted sync server (forbidden); CRDT libraries (heavy, hard to explain conflicts to users);
last-write-wins on whole files (silent data loss); iCloud/Google Drive APIs directly (platform-locked,
account coupling).

## Consequences

Sync is P1 for a single user's devices and P2 for multi-person trips. The first release can ship
without it; the data model carries the metadata from the first migration so it can be added later
without rewriting history.

## Security considerations

See the threat model (planned in `docs/architecture/THREAT_MODEL.md`). Key loss means unrecoverable
shared data; the UI warns and offers an export of the key.

## Operational considerations

Segment compaction and garbage collection run per trip. Corrupt or unauthenticated segments are
quarantined, never applied.
