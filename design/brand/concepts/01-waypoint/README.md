# Concept 01: Waypoint

*Plan smarter. Travel further.* Concept exploration for Viajorapp. Final brand approval belongs to the project owner; nothing here is a final production asset.

**Idea.** A V drawn as a single route: it starts at a hollow origin node, bends at the lowest point, and ends at a solid destination. The line is yours; nothing leaves the device.

## Rationale

The V of Viajorapp is drawn as a route, not as a letter: a hollow ring marks where you start, the line bends once at the lowest point, and a solid disc marks where you are going. It says "your path, your pace" without a pin, a plane or a globe.

How it carries the product pillars:

- **Offline-first**: a route is something you have already drawn; nothing in the mark implies a connection, a signal or a cloud.
- **Local data ownership and privacy**: one continuous line from origin to destination, with no branching and no outbound arrow. The data takes one path and stays on it.
- **Freedom**: the line is open at both ends and asymmetrical; you choose the nodes.
- **Exploration**: origin and destination are different shapes (hollow, solid), a small story of leaving and arriving.
- **Intelligent but optional planning**: the accent colour sits only on the destination dot, so "suggestions" can reuse the accent without ever becoming the main mark.

## Files

| File | Purpose |
| --- | --- |
| `symbol.svg` | Primary symbol, 512 viewBox, light-background colours |
| `symbol-dark.svg` | Same symbol for dark backgrounds |
| `symbol-mono.svg` | One-colour symbol (#000000), for stamps, print, embossing |
| `wordmark-lockup.svg` (+ `-dark`, `-mono`) | Horizontal lockup, wordmark converted to outlines (no live text) |
| `app-icon.svg` | 1024 viewBox, full-bleed background, artwork inside the Android 66 dp safe circle |
| `app-icon-foreground.svg` | The same artwork on transparent (adaptive-icon foreground layer) |
| `palette.json` | Tokens (light, dark, app icon) and computed WCAG contrast ratios |
| `typography.md` | Open-licensed type direction |
| `mock-ui.svg`, `mock-ui-dark.svg` | Concept illustration of a trip overview (not a product screenshot) |
| `preview/` | PNG renders at 16, 32, 48, 128, 512 px (light, dark, mono), small-size sheets, mask previews, mock UI PNGs |

## Construction

Stroked polyline (64 units wide on the 512 canvas, round join at the apex), one ring (r 40, stroke 28) and one disc (r 46). Three primitives, two colours; in one colour the ring hole is the only detail.

The wordmark is hand-drawn geometry (circles, stems and one diagonal pair) built by `design/brand/tools/brand_lib.py`: x-height 100 units, stem 17, letter gap 15. It does not depend on any font file.

## Palette

Light mode tokens:

| Token | Hex |
| --- | --- |
| primary | `#0B5563` |
| on-primary | `#FFFFFF` |
| accent | `#B85C00` |
| on-accent | `#FFFFFF` |
| bg | `#F7F5EF` |
| surface | `#FFFFFF` |
| text | `#10272D` |
| text-muted | `#44585F` |
| outline | `#728A91` |

Dark mode tokens:

| Token | Hex |
| --- | --- |
| primary | `#6CCBDB` |
| on-primary | `#06242B` |
| accent | `#FFC845` |
| on-accent | `#2A1B00` |
| bg | `#0B1A1E` |
| surface | `#12282E` |
| text | `#E8F1F2` |
| text-muted | `#A9BDC2` |
| outline | `#5F8791` |

App icon: background `#0B5563`, artwork `#F7F5EF`, accent `#FFC845`.

Contrast ratios (WCAG 2.x relative luminance; text needs 4.5:1, graphics and component boundaries 3:1):

**Light**

| Pair | Foreground | Background | Ratio | Needs | Result |
| --- | --- | --- | ---: | ---: | --- |
| text on bg | `#10272D` | `#F7F5EF` | 14.27:1 | 4.5:1 | pass |
| text-muted on bg | `#44585F` | `#F7F5EF` | 6.86:1 | 4.5:1 | pass |
| text on surface | `#10272D` | `#FFFFFF` | 15.55:1 | 4.5:1 | pass |
| on-primary on primary | `#FFFFFF` | `#0B5563` | 8.42:1 | 4.5:1 | pass |
| on-accent on accent | `#FFFFFF` | `#B85C00` | 4.60:1 | 4.5:1 | pass |
| primary on bg (logo, graphics) | `#0B5563` | `#F7F5EF` | 7.72:1 | 3.0:1 | pass |
| primary on surface (graphics) | `#0B5563` | `#FFFFFF` | 8.42:1 | 3.0:1 | pass |
| accent on bg (logo, graphics) | `#B85C00` | `#F7F5EF` | 4.22:1 | 3.0:1 | pass |
| outline on bg (field borders) | `#728A91` | `#F7F5EF` | 3.34:1 | 3.0:1 | pass |

**Dark**

| Pair | Foreground | Background | Ratio | Needs | Result |
| --- | --- | --- | ---: | ---: | --- |
| text on bg | `#E8F1F2` | `#0B1A1E` | 15.49:1 | 4.5:1 | pass |
| text-muted on bg | `#A9BDC2` | `#0B1A1E` | 9.10:1 | 4.5:1 | pass |
| text on surface | `#E8F1F2` | `#12282E` | 13.37:1 | 4.5:1 | pass |
| on-primary on primary | `#06242B` | `#6CCBDB` | 8.65:1 | 4.5:1 | pass |
| on-accent on accent | `#2A1B00` | `#FFC845` | 10.83:1 | 4.5:1 | pass |
| primary on bg (logo, graphics) | `#6CCBDB` | `#0B1A1E` | 9.49:1 | 3.0:1 | pass |
| primary on surface (graphics) | `#6CCBDB` | `#12282E` | 8.19:1 | 3.0:1 | pass |
| accent on bg (logo, graphics) | `#FFC845` | `#0B1A1E` | 11.52:1 | 3.0:1 | pass |
| outline on bg (field borders) | `#5F8791` | `#0B1A1E` | 4.54:1 | 3.0:1 | pass |

**App icon**

| Pair | Foreground | Background | Ratio | Needs | Result |
| --- | --- | --- | ---: | ---: | --- |
| icon body on icon background | `#F7F5EF` | `#0B5563` | 7.72:1 | 3.0:1 | pass |
| icon accent on icon background | `#FFC845` | `#0B5563` | 5.45:1 | 3.0:1 | pass |

## Typography direction

Headings **Manrope**, body **Inter**, figures **Inter**. All SIL OFL 1.1 per upstream; re-check the licence file when bundling. Full scale and rules in [`typography.md`](typography.md).

## Small-size legibility

At 16 px the V and the destination dot read clearly in colour, dark and one colour. The ring's hole is the first detail to close: at 16 px it reads as a dim node, from 24 px it is an open ring. In the one-colour column the ring hole is still visible at 24 px. See `preview/small-size-sheet.png` and `preview/legibility-sheet.png`.

## Light and dark suitability

Two variants only: `symbol.svg` (primary teal and burnt amber on light) and `symbol-dark.svg` (cyan and amber on dark). The light variant disappears on a dark background by design (teal on near-black), so dark surfaces must use the dark variant. The app icon is fixed teal with a cream V and a bright amber dot, so it is the same in light and dark launchers.

## Advantages

- The simplest of the three marks: highest score for simplicity, scalability and reproducibility.
- Carries the initial V, so the icon and the name reinforce each other.
- Bold silhouette that fills the adaptive-icon safe zone and survives any mask.
- The accent has a job (destination, suggestion) so colour use stays disciplined.
- Trivial to rebuild as a Compose Path or a VectorDrawable.

## Disadvantages

- Lowest distinctiveness: V marks and route-with-nodes glyphs are common.
- The ring-and-dot ends can read as a generic "route" icon in transit and ride-hailing UIs.
- Amber accent on a light background is only 4.2:1, so it must not carry text.
- The hollow ring needs care in embroidery or one-pixel-stroke contexts (use the filled variant if needed).

## Distinctiveness and confusion risk

Honest list of plausible lookalike categories (no trademark search was run): generic **route and waypoint glyphs** in navigation, transit and ride-hailing apps; **V monogram** marks in fintech, media and SaaS; **fitness route trackers** that draw a line between two dots. The V-as-route, with different start and end nodes, is the differentiator, and it is a thin one. Mitigation: keep the asymmetry (ring versus disc) and the teal and amber palette as part of the identity, and consider a custom V terminal later.

## Score

Weighted score **87.0 / 100** (rubric in `docs/design/BRAND_STRATEGY.md`).

| Criterion | Weight | Score (1-5) | Reason |
| --- | ---: | ---: | --- |
| Recognition | 15 | 4 | V initial plus route-with-two-nodes is nameable in one sentence. |
| Simplicity | 10 | 5 | Three primitives: stroked polyline, ring, disc. |
| Scalability | 15 | 5 | Holds at 16 px in colour and mono; origin ring hole is the first detail to close. |
| Distinctiveness | 15 | 3 | Route and node motifs are common in mapping and travel; the V-as-route is the differentiator. Lowest score of the three. |
| Accessibility | 10 | 4 | Primary 7.7:1 and accent 4.2:1 on the light background; shape, not colour, carries the meaning. |
| Reproducibility | 10 | 5 | Directly expressible as one Compose Path plus two circles. |
| Mobile suitability | 15 | 5 | Bold V fills the safe circle (farthest ink 306 px of the 313 px safe radius); centred under circle, squircle and rounded masks. |
| Cross-surface consistency | 10 | 4 | Works as favicon, store icon, README header and one-colour stamp; amber accent is the only fragile part in print. |

## Reproducing

`python design/brand/tools/build_brand.py <tmp-dir>` regenerates every file in this folder from code (stdlib + Pillow, resvg on PATH, no network).
