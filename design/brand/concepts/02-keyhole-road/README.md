# Concept 02: Keyhole Road

*Plan smarter. Travel further.* Concept exploration for Viajorapp. Final brand approval belongs to the project owner; nothing here is a final production asset.

**Idea.** A keyhole whose slot is a road running to the horizon. Privacy and the open road in one silhouette: the data is locked to you, the journey is not.

## Rationale

A keyhole whose slot is a road running to the horizon. The silhouette says "locked to you"; the dashed centre line says "go". It makes the privacy promise the visual hook and travel the payoff.

How it carries the product pillars:

- **Offline-first**: a keyhole is a physical object that works without power or network.
- **Local data ownership and privacy**: the keyhole is the privacy symbol; here you hold the key.
- **Freedom**: the road leaves the lock; the data is private, the journey is open.
- **Exploration**: the road in perspective, with dashes that shrink towards the horizon.
- **Intelligent but optional planning**: the accent coral appears only on small highlights (wordmark dots, optional suggestions), never on the lock.

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

One disc (r 104) joined to a rounded trapezoid road with three tapering dashes cut out with even-odd fill. Strictly one colour: the symbol is identical in colour and in one-colour use.

The wordmark is hand-drawn geometry (circles, stems and one diagonal pair) built by `design/brand/tools/brand_lib.py`: x-height 100 units, stem 22, letter gap 11. It does not depend on any font file.

## Palette

Light mode tokens:

| Token | Hex |
| --- | --- |
| primary | `#1E2A78` |
| on-primary | `#FFFFFF` |
| accent | `#D2442A` |
| on-accent | `#FFFFFF` |
| bg | `#F6F6FB` |
| surface | `#FFFFFF` |
| text | `#15183A` |
| text-muted | `#4A4E73` |
| outline | `#7B80A6` |

Dark mode tokens:

| Token | Hex |
| --- | --- |
| primary | `#AEB8FF` |
| on-primary | `#10143A` |
| accent | `#FF8A6B` |
| on-accent | `#3A0F05` |
| bg | `#0E1128` |
| surface | `#171B3D` |
| text | `#ECEEFF` |
| text-muted | `#B3B8DD` |
| outline | `#6F77C0` |

App icon: background `#1E2A78`, artwork `#F6F6FB`.

Contrast ratios (WCAG 2.x relative luminance; text needs 4.5:1, graphics and component boundaries 3:1):

**Light**

| Pair | Foreground | Background | Ratio | Needs | Result |
| --- | --- | --- | ---: | ---: | --- |
| text on bg | `#15183A` | `#F6F6FB` | 15.93:1 | 4.5:1 | pass |
| text-muted on bg | `#4A4E73` | `#F6F6FB` | 7.42:1 | 4.5:1 | pass |
| text on surface | `#15183A` | `#FFFFFF` | 17.16:1 | 4.5:1 | pass |
| on-primary on primary | `#FFFFFF` | `#1E2A78` | 12.67:1 | 4.5:1 | pass |
| on-accent on accent | `#FFFFFF` | `#D2442A` | 4.56:1 | 4.5:1 | pass |
| primary on bg (logo, graphics) | `#1E2A78` | `#F6F6FB` | 11.76:1 | 3.0:1 | pass |
| primary on surface (graphics) | `#1E2A78` | `#FFFFFF` | 12.67:1 | 3.0:1 | pass |
| accent on bg (logo, graphics) | `#D2442A` | `#F6F6FB` | 4.24:1 | 3.0:1 | pass |
| outline on bg (field borders) | `#7B80A6` | `#F6F6FB` | 3.56:1 | 3.0:1 | pass |

**Dark**

| Pair | Foreground | Background | Ratio | Needs | Result |
| --- | --- | --- | ---: | ---: | --- |
| text on bg | `#ECEEFF` | `#0E1128` | 16.15:1 | 4.5:1 | pass |
| text-muted on bg | `#B3B8DD` | `#0E1128` | 9.58:1 | 4.5:1 | pass |
| text on surface | `#ECEEFF` | `#171B3D` | 14.47:1 | 4.5:1 | pass |
| on-primary on primary | `#10143A` | `#AEB8FF` | 9.38:1 | 4.5:1 | pass |
| on-accent on accent | `#3A0F05` | `#FF8A6B` | 7.28:1 | 4.5:1 | pass |
| primary on bg (logo, graphics) | `#AEB8FF` | `#0E1128` | 9.83:1 | 3.0:1 | pass |
| primary on surface (graphics) | `#AEB8FF` | `#171B3D` | 8.81:1 | 3.0:1 | pass |
| accent on bg (logo, graphics) | `#FF8A6B` | `#0E1128` | 8.06:1 | 3.0:1 | pass |
| outline on bg (field borders) | `#6F77C0` | `#0E1128` | 4.49:1 | 3.0:1 | pass |

**App icon**

| Pair | Foreground | Background | Ratio | Needs | Result |
| --- | --- | --- | ---: | ---: | --- |
| icon body on icon background | `#F6F6FB` | `#1E2A78` | 11.76:1 | 3.0:1 | pass |
| icon accent on icon background | `#FF8A6B` | `#1E2A78` | 5.49:1 | 3.0:1 | pass |

## Typography direction

Headings **Space Grotesk**, body **IBM Plex Sans**, figures **IBM Plex Mono**. All SIL OFL 1.1 per upstream; re-check the licence file when bundling. Full scale and rules in [`typography.md`](typography.md).

## Small-size legibility

The keyhole silhouette is the most immediate mark at 16 px of all three concepts, in colour and in one colour. The road dashes close up at 16 px and are faint at 24 px, which is acceptable because they are the second reading, not the first. At 16 px some viewers read it as a person or a chess pawn; the road dashes from 32 px upward correct that reading.

## Light and dark suitability

`symbol.svg` (indigo on light) and `symbol-dark.svg` (periwinkle on dark). The light variant disappears on dark backgrounds, so dark surfaces must use the dark variant. The app icon is indigo with a cream keyhole and indigo dashes.

## Advantages

- Instantly legible silhouette at the smallest sizes.
- Expresses the privacy pillar in the mark itself, which no conventional travel planner does.
- Strictly one colour; the same file works in colour, mono and embossing.
- Large flat shapes: robust in print, embroidery and low-resolution displays.

## Disadvantages

- Keyholes are the default symbol of security tools, so a first impression may be "password manager" rather than "travel".
- No visual link to the V of the name.
- Reads as a person or pawn for some viewers at 16 to 24 px.
- The road metaphor is only clear from 32 px upward.
- Asking users to trust a "lock" in a travel app can feel heavy if not balanced by warm colour and tone.

## Distinctiveness and confusion risk

Plausible lookalike categories (no trademark search was run): **password managers, VPNs and security or privacy tools** (keyhole and lock marks are very common); **user-profile or avatar glyphs** (circle over trapezoid); **road and highway signage** icons in navigation. This is the highest category-confusion risk of the three, but in a different category to the one the product competes in. Mitigation: pair the mark with warm travel language, keep the road dashes in every size above 32 px, and never use a padlock shape.

## Score

Weighted score **80.0 / 100** (rubric in `docs/design/BRAND_STRATEGY.md`).

| Criterion | Weight | Score (1-5) | Reason |
| --- | ---: | ---: | --- |
| Recognition | 15 | 4 | Keyhole silhouette is read instantly; the road slot is a second, slower read. |
| Simplicity | 10 | 4 | One disc, one rounded trapezoid, three slots. |
| Scalability | 15 | 4 | Keyhole silhouette survives 16 px; road dashes close up at 16 px and are faint at 24 px (acceptable, they are secondary). |
| Distinctiveness | 15 | 4 | Keyhole-plus-road is unusual in travel, but keyholes are heavy security/password-manager territory. |
| Accessibility | 10 | 4 | Primary 11.8:1 on the light background; the symbol is strictly one colour (dashes are cut-outs), so no accent-contrast risk. |
| Reproducibility | 10 | 4 | Rounded polygon with even-odd holes; needs corner-rounding helper in Compose. |
| Mobile suitability | 15 | 4 | Tall centred silhouette sits well in the safe circle; reads as a person or pawn for some viewers. |
| Cross-surface consistency | 10 | 4 | Strong in one colour; relies on the privacy metaphor, with no link to the V of the name. |

## Reproducing

`python design/brand/tools/build_brand.py <tmp-dir>` regenerates every file in this folder from code (stdlib + Pillow, resvg on PATH, no network).
