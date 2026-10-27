# ADR-0004: Routing provider interface and travel estimates

## Status

Accepted (2026-10-27). The owner approved it in conversation on 2026-10-27 and asked for the approval to be recorded; it takes effect when the foundation Pull Request (issue #1) is merged.

## Context

Users need travel time and walking routes between activities. Public demo routers are fair-use only and
unsuitable as a shipped default; an on-device routing engine with offline graph data is a large,
separate effort.

## Decision

- Define `RoutingProvider` in `:domain` returning a `TravelLeg` with a `source`:
  `ROUTED` (geometry, distance, duration, maneuvers from a real engine), `ESTIMATE`
  (great-circle distance and configurable walking speed, no geometry) or `UNAVAILABLE`.
- The UI marks the source on every leg: routed legs show the path; estimates are labeled "Estimated,
  straight line" and drawn dashed. Estimates are never styled or worded as routes.
- Shipped adapters: `StraightLineEstimator` (always available, fully offline) and a configurable
  Valhalla-compatible HTTP adapter (self-hosted or third-party endpoint entered by the user;
  OpenRouteService with the user's own key is a second adapter).
- No public endpoint is hard-coded as a default. Routing requests are user-initiated or explicitly
  enabled per trip, and the settings screen states which host receives the coordinates.
- Results are cached per (provider, profile, from, to) with a version so itineraries stay readable
  offline, and stale-cached routes are labeled with their age.
- Failures (timeout, quota, no route, provider down) degrade to `ESTIMATE` with a visible reason.
- **Offline turn-by-turn navigation is not promised.** It requires an on-device engine and regional
  graph data; it is documented as a future capability and tracked as P2.

## Alternatives

Hard-coding the Valhalla/OSRM demo (policy violation risk); bundling BRouter (GPL, incompatible with
MIT distribution as a linked dependency); Google Directions (paid, proprietary).

## Consequences

Out of the box the app gives honest estimates, not turn-by-turn. Real routes need one setup step by the
user. This is stated in the onboarding and the feature matrix.

## Security considerations

Coordinates leave the device only to the configured routing host, on user action. The endpoint URL must
be `https` unless the user confirms a local/private address.

## Operational considerations

Adapters are contract-tested against recorded fixtures; CI never calls a live router.
