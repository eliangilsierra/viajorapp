# Visual asset QA report

Generated 2026-10-27 by `design/brand/tools/run_qa.py` using `asset_review.py` (visual-asset-quality-review) and `svgtool.py` (vector-asset-production). Heuristic numbers are pointers; the sheets in each `preview/` folder were looked at, see section 8.

Scope: logo SVGs, app icons, palettes of the three concept directions. Mock UI SVGs are illustrations with live text by design and are excluded from the no-live-text rule.

## 1. Render, structure and portability

| File | viewBox | live `<text>` | external refs | script / foreignObject | width/height attrs | resvg render |
| --- | --- | --- | --- | --- | --- | --- |
| 01-waypoint/symbol.svg | `0 0 512 512` | 0 | 0 | 0 | no | OK |
| 01-waypoint/symbol-dark.svg | `0 0 512 512` | 0 | 0 | 0 | no | OK |
| 01-waypoint/symbol-mono.svg | `0 0 512 512` | 0 | 0 | 0 | no | OK |
| 01-waypoint/wordmark-lockup.svg | `0 0 637 160` | 0 | 0 | 0 | no | OK |
| 01-waypoint/wordmark-lockup-dark.svg | `0 0 637 160` | 0 | 0 | 0 | no | OK |
| 01-waypoint/wordmark-lockup-mono.svg | `0 0 637 160` | 0 | 0 | 0 | no | OK |
| 01-waypoint/app-icon.svg | `0 0 1024 1024` | 0 | 0 | 0 | no | OK |
| 01-waypoint/app-icon-foreground.svg | `0 0 1024 1024` | 0 | 0 | 0 | no | OK |
| 02-keyhole-road/symbol.svg | `0 0 512 512` | 0 | 0 | 0 | no | OK |
| 02-keyhole-road/symbol-dark.svg | `0 0 512 512` | 0 | 0 | 0 | no | OK |
| 02-keyhole-road/symbol-mono.svg | `0 0 512 512` | 0 | 0 | 0 | no | OK |
| 02-keyhole-road/wordmark-lockup.svg | `0 0 623 160` | 0 | 0 | 0 | no | OK |
| 02-keyhole-road/wordmark-lockup-dark.svg | `0 0 623 160` | 0 | 0 | 0 | no | OK |
| 02-keyhole-road/wordmark-lockup-mono.svg | `0 0 623 160` | 0 | 0 | 0 | no | OK |
| 02-keyhole-road/app-icon.svg | `0 0 1024 1024` | 0 | 0 | 0 | no | OK |
| 02-keyhole-road/app-icon-foreground.svg | `0 0 1024 1024` | 0 | 0 | 0 | no | OK |
| 03-contour/symbol.svg | `0 0 512 512` | 0 | 0 | 0 | no | OK |
| 03-contour/symbol-dark.svg | `0 0 512 512` | 0 | 0 | 0 | no | OK |
| 03-contour/symbol-mono.svg | `0 0 512 512` | 0 | 0 | 0 | no | OK |
| 03-contour/wordmark-lockup.svg | `0 0 655 160` | 0 | 0 | 0 | no | OK |
| 03-contour/wordmark-lockup-dark.svg | `0 0 655 160` | 0 | 0 | 0 | no | OK |
| 03-contour/wordmark-lockup-mono.svg | `0 0 655 160` | 0 | 0 | 0 | no | OK |
| 03-contour/app-icon.svg | `0 0 1024 1024` | 0 | 0 | 0 | no | OK |
| 03-contour/app-icon-foreground.svg | `0 0 1024 1024` | 0 | 0 | 0 | no | OK |

Files failing any structural check: **0** of 24.

`svgtool.py validate` summary (last lines):

```
[OK] D:\Git\viajorapp\design\brand\concepts\03-contour\symbol-mono.svg
[OK] D:\Git\viajorapp\design\brand\concepts\03-contour\wordmark-lockup.svg
[OK] D:\Git\viajorapp\design\brand\concepts\03-contour\wordmark-lockup-dark.svg
[OK] D:\Git\viajorapp\design\brand\concepts\03-contour\wordmark-lockup-mono.svg
[OK] D:\Git\viajorapp\design\brand\concepts\03-contour\app-icon.svg
[OK] D:\Git\viajorapp\design\brand\concepts\03-contour\app-icon-foreground.svg
```

## 2. Small-size legibility (symbol on transparent, light | dark | one-colour)

| Concept | 16 px ink / detail-loss | 24 px | 32 px | 48 px | sheet |
| --- | --- | --- | --- | --- | --- |
| Waypoint | 26.2% / 0.043 (OK) | 23.8% / 0.027 (OK) | 22.3% / 0.018 (OK) | 21.3% / 0.011 (OK) | `01-waypoint/preview/legibility-sheet.png` |
| Keyhole Road | 31.2% / 0.041 (OK) | 29.2% / 0.027 (OK) | 28.3% / 0.020 (OK) | 27.0% / 0.012 (OK) | `02-keyhole-road/preview/legibility-sheet.png` |
| Contour | 48.4% / 0.096 (OK) | 42.5% / 0.056 (OK) | 39.3% / 0.043 (OK) | 36.2% / 0.026 (OK) | `03-contour/preview/legibility-sheet.png` |

## 3. Centering and margins

| Concept | Asset | bbox offset x / y | ink-centroid offset x / y | margins L / T / R / B | verdict |
| --- | --- | --- | --- | --- | --- |
| Waypoint | symbol.svg | -0.4% / +1.2% | +0.4% / -1.9% | 12.9 / 16.2 / 13.7 / 13.9 % | OK |
| Waypoint | app-icon-foreground.svg | -0.3% / +0.8% | +0.3% / -1.2% | 25.8 / 28.0 / 26.4 / 26.5 % | OK |
| Keyhole Road | symbol.svg | +0.0% / -1.0% | -0.0% / +0.6% | 25.2 / 12.1 / 25.2 / 14.1 % | OK |
| Keyhole Road | app-icon-foreground.svg | +0.0% / -0.7% | -0.0% / +0.4% | 32.1 / 22.8 / 32.1 / 24.1 % | OK |
| Contour | symbol.svg | -0.1% / +0.0% | -1.1% / -1.6% | 8.4 / 13.5 / 8.6 / 13.5 % | OK |
| Contour | app-icon-foreground.svg | -0.1% / +0.0% | -0.7% / -1.0% | 22.9 / 26.2 / 23.0 / 26.2 % | OK |

Symbol margins are measured on the 512 canvas; foreground margins on the 1024 canvas.

## 4. Adaptive-icon safe zone (independent measurement)

Android adaptive icons use a 108 dp canvas with a 66 dp safe circle: radius = 66/108 x 1024 / 2 = **312.9 px** at our 1024 viewBox (the brief's "about 66 percent" is 676 px diameter, a looser figure; the 66 dp rule is stricter, so it is the one enforced here).

| Concept | farthest ink pixel from centre | safe radius | margin | verdict |
| --- | --- | --- | --- | --- |
| Waypoint | 305.7 px | 312.9 px | 7.2 px | PASS |
| Keyhole Road | 305.9 px | 312.9 px | 7.0 px | PASS |
| Contour | 305.6 px | 312.9 px | 7.3 px | PASS |

## 5. Contrast (WCAG 2.x), cross-checked with `asset_review.py contrast`

Full pair lists are in each concept's `palette.json`. Below, every pair is re-run through `asset_review.py` and compared with the generator's number.

| Concept | Mode | Pair | Ratio (generator) | Ratio (asset_review) | Need | Result |
| --- | --- | --- | ---: | ---: | ---: | --- |
| Waypoint | light | text on bg | 14.27 | 14.27 | 4.5 | PASS |
| Waypoint | light | text-muted on bg | 6.86 | 6.86 | 4.5 | PASS |
| Waypoint | light | text on surface | 15.55 | 15.55 | 4.5 | PASS |
| Waypoint | light | on-primary on primary | 8.42 | 8.42 | 4.5 | PASS |
| Waypoint | light | on-accent on accent | 4.60 | 4.60 | 4.5 | PASS |
| Waypoint | light | primary on bg (logo, graphics) | 7.72 | 7.72 | 3.0 | PASS |
| Waypoint | light | primary on surface (graphics) | 8.42 | 8.42 | 3.0 | PASS |
| Waypoint | light | accent on bg (logo, graphics) | 4.22 | 4.22 | 3.0 | PASS |
| Waypoint | light | outline on bg (field borders) | 3.34 | 3.34 | 3.0 | PASS |
| Waypoint | dark | text on bg | 15.49 | 15.49 | 4.5 | PASS |
| Waypoint | dark | text-muted on bg | 9.10 | 9.10 | 4.5 | PASS |
| Waypoint | dark | text on surface | 13.37 | 13.37 | 4.5 | PASS |
| Waypoint | dark | on-primary on primary | 8.65 | 8.65 | 4.5 | PASS |
| Waypoint | dark | on-accent on accent | 10.83 | 10.83 | 4.5 | PASS |
| Waypoint | dark | primary on bg (logo, graphics) | 9.49 | 9.49 | 3.0 | PASS |
| Waypoint | dark | primary on surface (graphics) | 8.19 | 8.19 | 3.0 | PASS |
| Waypoint | dark | accent on bg (logo, graphics) | 11.52 | 11.52 | 3.0 | PASS |
| Waypoint | dark | outline on bg (field borders) | 4.54 | 4.54 | 3.0 | PASS |
| Waypoint | app_icon | icon body on icon background | 7.72 | 7.72 | 3.0 | PASS |
| Waypoint | app_icon | icon accent on icon background | 5.45 | 5.45 | 3.0 | PASS |
| Keyhole Road | light | text on bg | 15.93 | 15.93 | 4.5 | PASS |
| Keyhole Road | light | text-muted on bg | 7.42 | 7.42 | 4.5 | PASS |
| Keyhole Road | light | text on surface | 17.16 | 17.16 | 4.5 | PASS |
| Keyhole Road | light | on-primary on primary | 12.67 | 12.67 | 4.5 | PASS |
| Keyhole Road | light | on-accent on accent | 4.56 | 4.56 | 4.5 | PASS |
| Keyhole Road | light | primary on bg (logo, graphics) | 11.76 | 11.76 | 3.0 | PASS |
| Keyhole Road | light | primary on surface (graphics) | 12.67 | 12.67 | 3.0 | PASS |
| Keyhole Road | light | accent on bg (logo, graphics) | 4.24 | 4.24 | 3.0 | PASS |
| Keyhole Road | light | outline on bg (field borders) | 3.56 | 3.56 | 3.0 | PASS |
| Keyhole Road | dark | text on bg | 16.15 | 16.15 | 4.5 | PASS |
| Keyhole Road | dark | text-muted on bg | 9.58 | 9.58 | 4.5 | PASS |
| Keyhole Road | dark | text on surface | 14.47 | 14.47 | 4.5 | PASS |
| Keyhole Road | dark | on-primary on primary | 9.38 | 9.38 | 4.5 | PASS |
| Keyhole Road | dark | on-accent on accent | 7.28 | 7.28 | 4.5 | PASS |
| Keyhole Road | dark | primary on bg (logo, graphics) | 9.83 | 9.83 | 3.0 | PASS |
| Keyhole Road | dark | primary on surface (graphics) | 8.81 | 8.81 | 3.0 | PASS |
| Keyhole Road | dark | accent on bg (logo, graphics) | 8.06 | 8.06 | 3.0 | PASS |
| Keyhole Road | dark | outline on bg (field borders) | 4.49 | 4.49 | 3.0 | PASS |
| Keyhole Road | app_icon | icon body on icon background | 11.76 | 11.76 | 3.0 | PASS |
| Keyhole Road | app_icon | icon accent on icon background | 5.49 | 5.49 | 3.0 | PASS |
| Contour | light | text on bg | 14.03 | 14.03 | 4.5 | PASS |
| Contour | light | text-muted on bg | 6.53 | 6.53 | 4.5 | PASS |
| Contour | light | text on surface | 15.84 | 15.84 | 4.5 | PASS |
| Contour | light | on-primary on primary | 7.65 | 7.65 | 4.5 | PASS |
| Contour | light | on-accent on accent | 5.44 | 5.44 | 4.5 | PASS |
| Contour | light | primary on bg (logo, graphics) | 6.77 | 6.77 | 3.0 | PASS |
| Contour | light | primary on surface (graphics) | 7.65 | 7.65 | 3.0 | PASS |
| Contour | light | accent on bg (logo, graphics) | 4.82 | 4.82 | 3.0 | PASS |
| Contour | light | outline on bg (field borders) | 3.30 | 3.30 | 3.0 | PASS |
| Contour | dark | text on bg | 15.48 | 15.48 | 4.5 | PASS |
| Contour | dark | text-muted on bg | 9.01 | 9.01 | 4.5 | PASS |
| Contour | dark | text on surface | 13.15 | 13.15 | 4.5 | PASS |
| Contour | dark | on-primary on primary | 9.05 | 9.05 | 4.5 | PASS |
| Contour | dark | on-accent on accent | 8.06 | 8.06 | 4.5 | PASS |
| Contour | dark | primary on bg (logo, graphics) | 9.86 | 9.86 | 3.0 | PASS |
| Contour | dark | primary on surface (graphics) | 8.38 | 8.38 | 3.0 | PASS |
| Contour | dark | accent on bg (logo, graphics) | 8.60 | 8.60 | 3.0 | PASS |
| Contour | dark | outline on bg (field borders) | 5.15 | 5.15 | 3.0 | PASS |
| Contour | app_icon | icon body on icon background | 6.77 | 6.77 | 3.0 | PASS |
| Contour | app_icon | icon accent on icon background | 4.54 | 4.54 | 3.0 | PASS |

Contrast failures: **0**.

## 6. Adaptive-icon mask previews

- Waypoint: `01-waypoint/preview/app-icon-masks.png` generated. Tool output: `mask preview: D:\Git\viajorapp\design\brand\concepts\01-waypoint\preview\app-icon-masks.png (circle | squircle | rounded | square, on light then dark wallpaper)`
- Keyhole Road: `02-keyhole-road/preview/app-icon-masks.png` generated. Tool output: `mask preview: D:\Git\viajorapp\design\brand\concepts\02-keyhole-road\preview\app-icon-masks.png (circle | squircle | rounded | square, on light then dark wallpa`
- Contour: `03-contour/preview/app-icon-masks.png` generated. Tool output: `mask preview: D:\Git\viajorapp\design\brand\concepts\03-contour\preview\app-icon-masks.png (circle | squircle | rounded | square, on light then dark wallpaper)`

## 7. Deliverable completeness

```
79 expected, 0 problem(s)
```

## 8. Manual review (sheets looked at)

- **One idea per mark:** Waypoint: a V drawn as a route from a hollow origin to a solid destination. Keyhole Road: a keyhole whose slot is a road. Contour: terrain contours around a summit. Each states in one sentence.
- **Legibility sheets (16 / 24 / 32 / 48 px, light | dark | one-colour):** Waypoint: V and destination dot read at 16 px in all three columns; the ring hole is a dim dot at 16 px and open from 24 px. Keyhole Road: silhouette reads at 16 px; road dashes close at 16 px, faint at 24 px, clear at 32 px. Contour: at 16 px the two rings merge into a dense target and the summit marker is lost; the marker is faint at 24 px and clear from 32 px. Contour's detail-loss at 16 px (0.096) is more than double the other two (0.043 and 0.041).
- **Dark column of the tool sheet:** The tool paints the light-variant symbol on a near-black tile, where teal, indigo and forest green nearly vanish. This is expected: dark surfaces use `symbol-dark.svg`. The dark variants on their dark backgrounds are shown in `preview/small-size-sheet.png` (row 2) and read correctly at every size. Not a defect, but the light variant must never be placed on a dark surface (misuse rule for the guidelines).
- **Mask previews (circle, squircle, rounded, square, on light and dark wallpaper):** None of the three icons is cut by any mask and all stay centred. Contour's outer ring and Waypoint's nodes are the elements closest to the circle edge (about 7 px inside the 312.9 px safe radius at 1024).
- **Craft (looked at 512 px renders):** No slivers or notches. Minor: Waypoint's arm meets the ring with a slightly lumpy shoulder at the lower right of the ring; Contour has a gentle kink at the right-hand side of both rings from the harmonic construction; Keyhole Road has two small concave corners where the disc meets the road (normal for keyholes). None is blocking at concept stage; all would be refined in the production phase.
- **Unintended readings:** Waypoint rotated 180 degrees reads as a peak with two nodes (harmless). Keyhole Road reads as a person or chess pawn to some viewers at 16 to 24 px. Contour can read as an eye, a fingerprint or a target; the eye reading is a real risk for a privacy-first brand and is recorded in the concept README.
- **Colour:** All three work in one colour and reversed (symbol-mono). No red and green pair carries meaning: Waypoint uses teal and amber (different hue and luminance, and the meaning is carried by ring versus disc, not colour), Keyhole Road is strictly single colour, Contour uses green and blue that differ in luminance and shape (triangle versus rings). Colour-blind behaviour reasoned from the palette, not simulated.
- **Typography:** Wordmark text is outlined geometry for all three (0 live `<text>` elements in any logo SVG, section 1), spelled `Viajorapp`. Letter spacing and the tuck of the j hook were tuned by eye on the 512 px lockup renders. The letterforms are simple geometric constructions, not a type design: kerning for other languages and glyph coverage are out of scope until a brand typeface or commissioned logotype is chosen.
- **Brand fit:** Waypoint: practical, clear, friendly. Keyhole Road: protective, assured, slightly technical. Contour: quiet, outdoorsy, craft. None uses an airplane, globe, plain pin or compass.
- **Gallery in a browser:** design/brand/gallery/index.html was opened over a local HTTP server on 127.0.0.1 (the Browser pane refused file:// URLs) and checked at 1300 px and 375 px wide, in dark and light themes, with the theme toggle. One defect found and fixed: a CSS class name collision (`.score`) gave a paragraph a 560 px min-width and caused horizontal scroll; after the fix scrollWidth equals clientWidth at both widths. The page makes no network requests (0 non-data image sources, 0 external script or link elements).
- **Fixes made during the review loop:** (1) app-icon PNG previews had an alpha channel; flattened to RGB so the 1024 preview satisfies the iOS opaque-icon rule. (2) Optical offsets of all three symbols tuned until bbox offset is within 2 percent and ink-centroid within 3 percent. (3) Keyhole Road: the original red-sun and indigo-road version read as a person on a road; replaced by a single-colour keyhole with cut-out dashes. (4) Contour: first version (open arcs) read as a spiral and the closed-ring-with-dot version read as an eye; replaced with offset closed rings and a triangular summit. (5) Palette `border` tokens (1.3:1) replaced by `outline` tokens that meet 3:1. (6) App icon artwork rescaled to sit inside the true 66 dp safe circle (312.9 px), not only the looser 66 percent diameter.

## 9. Files reviewed (sha256, first 10 hex)

| File | hash |
| --- | --- |
| 01-waypoint/symbol.svg | `799d7d453f` |
| 01-waypoint/symbol-mono.svg | `4219e59b35` |
| 01-waypoint/wordmark-lockup.svg | `bc31cc2065` |
| 01-waypoint/app-icon.svg | `0a8c6b6263` |
| 02-keyhole-road/symbol.svg | `8604e8c36c` |
| 02-keyhole-road/symbol-mono.svg | `c5942b418e` |
| 02-keyhole-road/wordmark-lockup.svg | `9338381df2` |
| 02-keyhole-road/app-icon.svg | `f19b69d4e6` |
| 03-contour/symbol.svg | `f5ee5bff91` |
| 03-contour/symbol-mono.svg | `911fec07d2` |
| 03-contour/wordmark-lockup.svg | `5974f432dc` |
| 03-contour/app-icon.svg | `da444b53ae` |

## 10. Verdict

**PASS WITH FIXES** for the concept stage (all three concepts), with one open weakness.

Worst failure: **Contour at 16 px** (rings merge, summit marker lost, highest detail-loss). It needs a dedicated small-size cut before it could be chosen; Waypoint and Keyhole Road have no small-size failure.

Automated results from this run: structural failures 0 of 24 logo SVGs; contrast failures 0; safe-zone checks: Waypoint PASS, Keyhole Road PASS, Contour PASS; completeness: 79 expected, 0 problem(s).

These are concept assets, not production assets: they have not been minified with svgo, converted to Android VectorDrawable or iOS asset catalogues, or tested on a device.

## 11. Not verified

- Not tested on physical devices, real launchers (OEM masks differ) or iOS springboard.
- Contrast uses WCAG 2.x, not APCA. Small-size numbers are heuristics.
- Colour-blind simulation was reasoned from the palettes (no red/green-only encoding), not simulated with a tool.
- Mock UI PNGs were rendered with system fallback fonts (Segoe UI, Georgia); the proposed open fonts are not installed or bundled.
- No trademark or design-patent search was run; lookalike notes in each concept README are judgement only.
