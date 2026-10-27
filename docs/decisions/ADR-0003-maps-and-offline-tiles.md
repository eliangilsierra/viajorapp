# ADR-0003: Maps with MapLibre and an honest offline-map strategy

## Status

Accepted (2026-10-27). The owner approved it in conversation on 2026-10-27 and asked for the approval to be recorded; it takes effect when the foundation Pull Request (issue #1) is merged.

## Context

Maps are core. Offline itinerary data, offline map rendering and offline routing are three separate
capabilities. Provider terms are the binding constraint (see findings, section 4):
`tile.openstreetmap.org` forbids bulk and offline download; OpenFreeMap allows app-driven offline packs
only up to 3,000 tiles or 50 MB per download with 4 parallel requests and has no SLA.

## Decision

- Render with **maplibre-compose** (BSD-3-Clause; Android and iOS), behind a `MapSurface` interface in
  `:ui` so the library can be swapped.
- The map **style/tile source is configuration**, not code: a `TileSource` record (style URL, display
  name, attribution text, offline policy: `allowed | forbidden`, size limits). The app ships a small
  list of presets and lets the user add a custom style URL.
- **Offline itinerary availability is unconditional** (all data is local). It is shown as such.
- **Offline map regions** use maplibre-compose `OfflineStorage` packs, only for a source whose
  `offlinePolicy` is `allowed`. The UI shows estimated tile count and size before download, enforces
  the source's limits, supports pause/resume/cancel/delete, and records the pack metadata (source,
  bounds, zoom range, version, size, attribution) in the database.
- No default source is promised offline until a measurement spike shows what city-sized regions cost
  against the limits. If a region exceeds the limit the UI says so and offers smaller regions; it never
  silently truncates.
- The user-supplied PMTiles/MBTiles source is a P2 item for large regions.
- Attribution is always displayed on the map and in About.
- A distinct, stable `User-Agent` (`Viajorapp/<version>`) is sent on all tile and style requests.

## Alternatives

Google Maps / Mapbox (proprietary terms, keys, cost); Mapsforge-only (Android only); OSM standard tiles
for offline (forbidden); scraping any provider (forbidden).

## Consequences

The map works online with no key for the preset source. Offline map rendering is "supported where the
source permits and the region fits", which is narrower than "offline maps everywhere". The offline
capability matrix in the README states this.

## Security considerations

Tile and style requests reveal approximate viewed areas and IP to the tile provider. Settings > Privacy
lists the active provider; a "no network map" mode shows the itinerary without a base map.

## Operational considerations

Tile providers can change terms or disappear; presets are data, so they can be updated without a
rebuild of the core. Offline packs record which source and style version created them.
