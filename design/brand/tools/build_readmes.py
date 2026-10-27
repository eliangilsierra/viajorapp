"""Write each concept's README.md (prose below, palette tables generated from palette.json)."""
from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from concepts import CONCEPTS
from scoring import CRITERIA, NOTES, SCORES, total

ROOT = Path(__file__).resolve().parents[1]
CDIR = ROOT / "concepts"

TEXT = {
    "01-waypoint": dict(
        rationale="""The V of Viajorapp is drawn as a route, not as a letter: a hollow ring marks where you start, the line bends once at the lowest point, and a solid disc marks where you are going. It says "your path, your pace" without a pin, a plane or a globe.

How it carries the product pillars:

- **Offline-first**: a route is something you have already drawn; nothing in the mark implies a connection, a signal or a cloud.
- **Local data ownership and privacy**: one continuous line from origin to destination, with no branching and no outbound arrow. The data takes one path and stays on it.
- **Freedom**: the line is open at both ends and asymmetrical; you choose the nodes.
- **Exploration**: origin and destination are different shapes (hollow, solid), a small story of leaving and arriving.
- **Intelligent but optional planning**: the accent colour sits only on the destination dot, so "suggestions" can reuse the accent without ever becoming the main mark.""",
        construction="Stroked polyline (64 units wide on the 512 canvas, round join at the apex), one ring (r 40, stroke 28) and one disc (r 46). Three primitives, two colours; in one colour the ring hole is the only detail.",
        small="At 16 px the V and the destination dot read clearly in colour, dark and one colour. The ring's hole is the first detail to close: at 16 px it reads as a dim node, from 24 px it is an open ring. In the one-colour column the ring hole is still visible at 24 px. See `preview/small-size-sheet.png` and `preview/legibility-sheet.png`.",
        lightdark="Two variants only: `symbol.svg` (primary teal and burnt amber on light) and `symbol-dark.svg` (cyan and amber on dark). The light variant disappears on a dark background by design (teal on near-black), so dark surfaces must use the dark variant. The app icon is fixed teal with a cream V and a bright amber dot, so it is the same in light and dark launchers.",
        adv=[
            "The simplest of the three marks: highest score for simplicity, scalability and reproducibility.",
            "Carries the initial V, so the icon and the name reinforce each other.",
            "Bold silhouette that fills the adaptive-icon safe zone and survives any mask.",
            "The accent has a job (destination, suggestion) so colour use stays disciplined.",
            "Trivial to rebuild as a Compose Path or a VectorDrawable.",
        ],
        dis=[
            "Lowest distinctiveness: V marks and route-with-nodes glyphs are common.",
            "The ring-and-dot ends can read as a generic \"route\" icon in transit and ride-hailing UIs.",
            "Amber accent on a light background is only 4.2:1, so it must not carry text.",
            "The hollow ring needs care in embroidery or one-pixel-stroke contexts (use the filled variant if needed).",
        ],
        confusion="Honest list of plausible lookalike categories (no trademark search was run): generic **route and waypoint glyphs** in navigation, transit and ride-hailing apps; **V monogram** marks in fintech, media and SaaS; **fitness route trackers** that draw a line between two dots. The V-as-route, with different start and end nodes, is the differentiator, and it is a thin one. Mitigation: keep the asymmetry (ring versus disc) and the teal and amber palette as part of the identity, and consider a custom V terminal later.",
    ),
    "02-keyhole-road": dict(
        rationale="""A keyhole whose slot is a road running to the horizon. The silhouette says "locked to you"; the dashed centre line says "go". It makes the privacy promise the visual hook and travel the payoff.

How it carries the product pillars:

- **Offline-first**: a keyhole is a physical object that works without power or network.
- **Local data ownership and privacy**: the keyhole is the privacy symbol; here you hold the key.
- **Freedom**: the road leaves the lock; the data is private, the journey is open.
- **Exploration**: the road in perspective, with dashes that shrink towards the horizon.
- **Intelligent but optional planning**: the accent coral appears only on small highlights (wordmark dots, optional suggestions), never on the lock.""",
        construction="One disc (r 104) joined to a rounded trapezoid road with three tapering dashes cut out with even-odd fill. Strictly one colour: the symbol is identical in colour and in one-colour use.",
        small="The keyhole silhouette is the most immediate mark at 16 px of all three concepts, in colour and in one colour. The road dashes close up at 16 px and are faint at 24 px, which is acceptable because they are the second reading, not the first. At 16 px some viewers read it as a person or a chess pawn; the road dashes from 32 px upward correct that reading.",
        lightdark="`symbol.svg` (indigo on light) and `symbol-dark.svg` (periwinkle on dark). The light variant disappears on dark backgrounds, so dark surfaces must use the dark variant. The app icon is indigo with a cream keyhole and indigo dashes.",
        adv=[
            "Instantly legible silhouette at the smallest sizes.",
            "Expresses the privacy pillar in the mark itself, which no conventional travel planner does.",
            "Strictly one colour; the same file works in colour, mono and embossing.",
            "Large flat shapes: robust in print, embroidery and low-resolution displays.",
        ],
        dis=[
            "Keyholes are the default symbol of security tools, so a first impression may be \"password manager\" rather than \"travel\".",
            "No visual link to the V of the name.",
            "Reads as a person or pawn for some viewers at 16 to 24 px.",
            "The road metaphor is only clear from 32 px upward.",
            "Asking users to trust a \"lock\" in a travel app can feel heavy if not balanced by warm colour and tone.",
        ],
        confusion="Plausible lookalike categories (no trademark search was run): **password managers, VPNs and security or privacy tools** (keyhole and lock marks are very common); **user-profile or avatar glyphs** (circle over trapezoid); **road and highway signage** icons in navigation. This is the highest category-confusion risk of the three, but in a different category to the one the product competes in. Mitigation: pair the mark with warm travel language, keep the road dashes in every size above 32 px, and never use a padlock shape.",
    ),
    "03-contour": dict(
        rationale="""Topographic contour lines around a summit marker. A map you can read without a signal, drawn as terrain rather than as streets. It leans towards hiking, slow travel and exploration, and away from booking.

How it carries the product pillars:

- **Offline-first**: contour lines are the language of paper maps and downloaded terrain.
- **Local data ownership and privacy**: the rings enclose the marker, a perimeter around what is yours.
- **Freedom**: the open terrain invites a route of your own.
- **Exploration**: summit and slope, the visual vocabulary of discovery.
- **Intelligent but optional planning**: the accent sky blue appears only on the summit marker and on optional suggestions.""",
        construction="Two smooth closed contours generated from low-order harmonics (8 cubic segments each, 40 unit stroke) and one rounded triangle (summit). Strokes should be outlined before Android VectorDrawable export.",
        small="Weakest at small sizes: at 16 px the two rings merge into a dense target and the summit marker disappears; it is faint at 24 px and clear from 32 px. Heuristic detail-loss at 16 px is more than double the other two concepts (see QA_REPORT.md). A simplified small-size cut (single ring plus marker) would be required for favicons.",
        lightdark="`symbol.svg` (forest green on light) and `symbol-dark.svg` (mint on dark). The light variant disappears on dark backgrounds, so dark surfaces must use the dark variant. The app icon is forest green with cream contours and a sky-blue summit.",
        adv=[
            "Evocative of exploration and offline maps; the least \"travel template\" of the three.",
            "Organic shape contrasts with the rectilinear UI and is immediately ownable as a texture and pattern.",
            "Contours can extend into backgrounds, empty states and print as a brand pattern.",
        ],
        dis=[
            "Worst small-size behaviour; needs a dedicated small-size cut.",
            "Parallel thin lines are harder for low-vision users and for embroidery or one-colour stamping.",
            "Unintended readings: **eye** (almond shape with a centred marker, uncomfortable for a privacy-first brand), **fingerprint**, **target** or **radar**.",
            "Hardest to redraw by hand: the organic curve is a parameterised construction.",
            "No link to the V of the name.",
        ],
        confusion="Plausible lookalike categories (no trademark search was run): **outdoor and hiking map apps** (contour motifs are common there); **fingerprint and biometric** icons (nested organic rings); **eye or vision** icons; **ripple and radar** glyphs. Distinctive among general trip planners, less so among outdoor-mapping tools. Mitigation: keep the triangle summit and the asymmetric offset to push the reading towards terrain, and test the eye reading with real users before choosing this direction.",
    ),
}


def tbl(rep, mode):
    rows = "| Pair | Foreground | Background | Ratio | Needs | Result |\n| --- | --- | --- | ---: | ---: | --- |\n"
    for r in rep["contrast"][mode]:
        rows += f"| {r['pair']} | `{r['fg']}` | `{r['bg']}` | {r['ratio']:.2f}:1 | {r['required']:.1f}:1 | {'pass' if r['passes'] else 'fail'} |\n"
    return rows


def tokens(p):
    return "| Token | Hex |\n| --- | --- |\n" + "".join(f"| {k.replace('_', '-')} | `{v}` |\n" for k, v in p.items())


def main():
    for c in CONCEPTS:
        d = CDIR / c["id"]
        rep = json.loads((d / "palette.json").read_text(encoding="utf-8"))
        t = TEXT[c["id"]]
        sc = SCORES[c["id"]]
        score_rows = "".join(f"| {label} | {w} | {sc[k]} | {NOTES[c['id']][k]} |\n" for k, label, w, _ in CRITERIA)
        ic = rep["tokens"]["app_icon"]
        md = f"""# Concept {c['id'][:2]}: {c['name']}

*Plan smarter. Travel further.* Concept exploration for Viajorapp. Final brand approval belongs to the project owner; nothing here is a final production asset.

**Idea.** {c['idea']}

## Rationale

{t['rationale']}

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

{t['construction']}

The wordmark is hand-drawn geometry (circles, stems and one diagonal pair) built by `design/brand/tools/brand_lib.py`: x-height 100 units, stem {c['style'].weight}, letter gap {c['style'].gap}. It does not depend on any font file.

## Palette

Light mode tokens:

{tokens(c['light'])}
Dark mode tokens:

{tokens(c['dark'])}
App icon: background `{ic['bg']}`, artwork `{ic['body']}`{(", accent `" + ic['accent'] + "`") if c['key'] != 'keyhole' else ""}.

Contrast ratios (WCAG 2.x relative luminance; text needs 4.5:1, graphics and component boundaries 3:1):

**Light**

{tbl(rep, 'light')}
**Dark**

{tbl(rep, 'dark')}
**App icon**

| Pair | Foreground | Background | Ratio | Needs | Result |
| --- | --- | --- | ---: | ---: | --- |
""" + "".join(f"| {r['pair']} | `{r['fg']}` | `{r['bg']}` | {r['ratio']:.2f}:1 | {r['required']:.1f}:1 | {'pass' if r['passes'] else 'fail'} |\n" for r in rep["contrast"]["app_icon"]) + f"""
## Typography direction

Headings **{c['fonts']['display'][0]}**, body **{c['fonts']['body'][0]}**, figures **{c['fonts']['mono'][0]}**. All SIL OFL 1.1 per upstream; re-check the licence file when bundling. Full scale and rules in [`typography.md`](typography.md).

## Small-size legibility

{t['small']}

## Light and dark suitability

{t['lightdark']}

## Advantages

""" + "".join(f"- {a}\n" for a in t["adv"]) + """
## Disadvantages

""" + "".join(f"- {a}\n" for a in t["dis"]) + f"""
## Distinctiveness and confusion risk

{t['confusion']}

## Score

Weighted score **{total(sc)} / 100** (rubric in `docs/design/BRAND_STRATEGY.md`).

| Criterion | Weight | Score (1-5) | Reason |
| --- | ---: | ---: | --- |
{score_rows}
## Reproducing

`python design/brand/tools/build_brand.py <tmp-dir>` regenerates every file in this folder from code (stdlib + Pillow, resvg on PATH, no network).
"""
        (d / "README.md").write_text(md, encoding="utf-8")
        print("wrote", d / "README.md")


if __name__ == "__main__":
    main()
