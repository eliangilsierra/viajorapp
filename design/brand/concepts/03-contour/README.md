# Concept 03: Contour

*Plan smarter. Travel further.* Concept exploration for Viajorapp. Final brand approval belongs to the project owner; nothing here is a final production asset.

**Idea.** Topographic contour lines around a summit marker. Terrain you can read without a signal: the map is already in your pocket, and the line you walk is the line you chose.

## Rationale

Topographic contour lines around a summit marker. A map you can read without a signal, drawn as terrain rather than as streets. It leans towards hiking, slow travel and exploration, and away from booking.

How it carries the product pillars:

- **Offline-first**: contour lines are the language of paper maps and downloaded terrain.
- **Local data ownership and privacy**: the rings enclose the marker, a perimeter around what is yours.
- **Freedom**: the open terrain invites a route of your own.
- **Exploration**: summit and slope, the visual vocabulary of discovery.
- **Intelligent but optional planning**: the accent sky blue appears only on the summit marker and on optional suggestions.

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

Two smooth closed contours generated from low-order harmonics (8 cubic segments each, 40 unit stroke) and one rounded triangle (summit). Strokes should be outlined before Android VectorDrawable export.

The wordmark is hand-drawn geometry (circles, stems and one diagonal pair) built by `design/brand/tools/brand_lib.py`: x-height 100 units, stem 13, letter gap 20. It does not depend on any font file.

## Palette

Light mode tokens:

| Token | Hex |
| --- | --- |
| primary | `#1F5E45` |
| on-primary | `#FFFFFF` |
| accent | `#0F6FA8` |
| on-accent | `#FFFFFF` |
| bg | `#F4F1E8` |
| surface | `#FFFFFF` |
| text | `#14261E` |
| text-muted | `#475A50` |
| outline | `#76897C` |

Dark mode tokens:

| Token | Hex |
| --- | --- |
| primary | `#7FD1A6` |
| on-primary | `#0A2418` |
| accent | `#63BDF2` |
| on-accent | `#04202F` |
| bg | `#0C1A14` |
| surface | `#142A20` |
| text | `#E7F1EA` |
| text-muted | `#A8BDB0` |
| outline | `#5E9578` |

App icon: background `#1F5E45`, artwork `#F4F1E8`, accent `#8BD0F7`.

Contrast ratios (WCAG 2.x relative luminance; text needs 4.5:1, graphics and component boundaries 3:1):

**Light**

| Pair | Foreground | Background | Ratio | Needs | Result |
| --- | --- | --- | ---: | ---: | --- |
| text on bg | `#14261E` | `#F4F1E8` | 14.03:1 | 4.5:1 | pass |
| text-muted on bg | `#475A50` | `#F4F1E8` | 6.53:1 | 4.5:1 | pass |
| text on surface | `#14261E` | `#FFFFFF` | 15.84:1 | 4.5:1 | pass |
| on-primary on primary | `#FFFFFF` | `#1F5E45` | 7.65:1 | 4.5:1 | pass |
| on-accent on accent | `#FFFFFF` | `#0F6FA8` | 5.44:1 | 4.5:1 | pass |
| primary on bg (logo, graphics) | `#1F5E45` | `#F4F1E8` | 6.77:1 | 3.0:1 | pass |
| primary on surface (graphics) | `#1F5E45` | `#FFFFFF` | 7.65:1 | 3.0:1 | pass |
| accent on bg (logo, graphics) | `#0F6FA8` | `#F4F1E8` | 4.82:1 | 3.0:1 | pass |
| outline on bg (field borders) | `#76897C` | `#F4F1E8` | 3.30:1 | 3.0:1 | pass |

**Dark**

| Pair | Foreground | Background | Ratio | Needs | Result |
| --- | --- | --- | ---: | ---: | --- |
| text on bg | `#E7F1EA` | `#0C1A14` | 15.48:1 | 4.5:1 | pass |
| text-muted on bg | `#A8BDB0` | `#0C1A14` | 9.01:1 | 4.5:1 | pass |
| text on surface | `#E7F1EA` | `#142A20` | 13.15:1 | 4.5:1 | pass |
| on-primary on primary | `#0A2418` | `#7FD1A6` | 9.05:1 | 4.5:1 | pass |
| on-accent on accent | `#04202F` | `#63BDF2` | 8.06:1 | 4.5:1 | pass |
| primary on bg (logo, graphics) | `#7FD1A6` | `#0C1A14` | 9.86:1 | 3.0:1 | pass |
| primary on surface (graphics) | `#7FD1A6` | `#142A20` | 8.38:1 | 3.0:1 | pass |
| accent on bg (logo, graphics) | `#63BDF2` | `#0C1A14` | 8.60:1 | 3.0:1 | pass |
| outline on bg (field borders) | `#5E9578` | `#0C1A14` | 5.15:1 | 3.0:1 | pass |

**App icon**

| Pair | Foreground | Background | Ratio | Needs | Result |
| --- | --- | --- | ---: | ---: | --- |
| icon body on icon background | `#F4F1E8` | `#1F5E45` | 6.77:1 | 3.0:1 | pass |
| icon accent on icon background | `#8BD0F7` | `#1F5E45` | 4.54:1 | 3.0:1 | pass |

## Typography direction

Headings **Fraunces**, body **Source Sans 3**, figures **Source Sans 3**. All SIL OFL 1.1 per upstream; re-check the licence file when bundling. Full scale and rules in [`typography.md`](typography.md).

## Small-size legibility

Weakest at small sizes: at 16 px the two rings merge into a dense target and the summit marker disappears; it is faint at 24 px and clear from 32 px. Heuristic detail-loss at 16 px is more than double the other two concepts (see QA_REPORT.md). A simplified small-size cut (single ring plus marker) would be required for favicons.

## Light and dark suitability

`symbol.svg` (forest green on light) and `symbol-dark.svg` (mint on dark). The light variant disappears on dark backgrounds, so dark surfaces must use the dark variant. The app icon is forest green with cream contours and a sky-blue summit.

## Advantages

- Evocative of exploration and offline maps; the least "travel template" of the three.
- Organic shape contrasts with the rectilinear UI and is immediately ownable as a texture and pattern.
- Contours can extend into backgrounds, empty states and print as a brand pattern.

## Disadvantages

- Worst small-size behaviour; needs a dedicated small-size cut.
- Parallel thin lines are harder for low-vision users and for embroidery or one-colour stamping.
- Unintended readings: **eye** (almond shape with a centred marker, uncomfortable for a privacy-first brand), **fingerprint**, **target** or **radar**.
- Hardest to redraw by hand: the organic curve is a parameterised construction.
- No link to the V of the name.

## Distinctiveness and confusion risk

Plausible lookalike categories (no trademark search was run): **outdoor and hiking map apps** (contour motifs are common there); **fingerprint and biometric** icons (nested organic rings); **eye or vision** icons; **ripple and radar** glyphs. Distinctive among general trip planners, less so among outdoor-mapping tools. Mitigation: keep the triangle summit and the asymmetric offset to push the reading towards terrain, and test the eye reading with real users before choosing this direction.

## Score

Weighted score **63.0 / 100** (rubric in `docs/design/BRAND_STRATEGY.md`).

| Criterion | Weight | Score (1-5) | Reason |
| --- | ---: | ---: | --- |
| Recognition | 15 | 3 | Contour lines suggest terrain, but also fingerprint or eye for some viewers. |
| Simplicity | 10 | 3 | Two organic rings plus a marker; the organic curves need more thought to redraw. |
| Scalability | 15 | 3 | Rings merge at 16 px and the marker disappears; needs a simplified small-size cut. |
| Distinctiveness | 15 | 4 | Topographic motif is distinctive among general travel planners but common in outdoor and mapping apps. |
| Accessibility | 10 | 3 | Contrast passes (6.8:1 primary); thin parallel lines are harder for low-vision users at small sizes. |
| Reproducibility | 10 | 3 | Blob curves are parameterised in code, but not trivial to hand-rebuild in a vector editor. |
| Mobile suitability | 15 | 3 | Large organic shape fills the mask well but has no strong silhouette. |
| Cross-surface consistency | 10 | 3 | Fine lines suffer in embroidery, favicon and low-resolution print. |

## Reproducing

`python design/brand/tools/build_brand.py <tmp-dir>` regenerates every file in this folder from code (stdlib + Pillow, resvg on PATH, no network).
