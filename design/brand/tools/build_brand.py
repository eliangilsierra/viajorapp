"""Build all concept assets: SVGs, palettes, PNG previews, mock UI.

Usage:  python design/brand/tools/build_brand.py
Needs: Python 3.12 + Pillow, resvg on PATH. No network access is used.
"""
from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from PIL import Image

from brand_lib import (
    SYMBOLS, OPTICAL, Tf, app_icon_svg, contrast, f, lockup_svg, svg_doc, symbol_svg,
    wordmark_only_paths,
)
from concepts import CONCEPTS

ROOT = Path(__file__).resolve().parents[1]
CDIR = ROOT / "concepts"
SIZES = [16, 32, 48, 128, 512]
SAFE_RADIUS_1024 = 66 / 108 * 1024 / 2  # Android adaptive icon: 66dp safe circle in a 108dp canvas = 312.9 px
TARGET_RADIUS = 306  # aim a little inside the safe circle


def resvg(svg: Path, png: Path, w: int, h: int | None = None, bg: str | None = None):
    cmd = ["resvg", "-w", str(w)]
    if h:
        cmd += ["-h", str(h)]
    if bg:
        cmd += ["--background", bg]
    cmd += [str(svg), str(png)]
    subprocess.run(cmd, check=True)


def farthest_ink_radius(svg_text: str, tmp: Path) -> float:
    svg_path = tmp / "_ink.svg"
    svg_path.write_text(svg_text, encoding="utf-8")
    png = tmp / "_ink.png"
    resvg(svg_path, png, 1024)
    im = Image.open(png).convert("RGBA")
    a = im.split()[3]
    px = a.load()
    w, h = im.size
    best = 0.0
    for y in range(0, h):
        for x in range(0, w):
            if px[x, y] > 8:
                r = ((x + 0.5 - 512) ** 2 + (y + 0.5 - 512) ** 2) ** 0.5
                if r > best:
                    best = r
    svg_path.unlink()
    png.unlink()
    return best


def palette_report(c: dict) -> dict:
    def pairs(p):
        spec = [
            ("text on bg", p["text"], p["bg"], 4.5, "text"),
            ("text-muted on bg", p["text_muted"], p["bg"], 4.5, "text"),
            ("text on surface", p["text"], p["surface"], 4.5, "text"),
            ("on-primary on primary", p["on_primary"], p["primary"], 4.5, "text"),
            ("on-accent on accent", p["on_accent"], p["accent"], 4.5, "text"),
            ("primary on bg (logo, graphics)", p["primary"], p["bg"], 3.0, "graphics"),
            ("primary on surface (graphics)", p["primary"], p["surface"], 3.0, "graphics"),
            ("accent on bg (logo, graphics)", p["accent"], p["bg"], 3.0, "graphics"),
            ("outline on bg (field borders)", p["outline"], p["bg"], 3.0, "graphics"),
        ]
        return [
            dict(pair=n, fg=a, bg=b, ratio=round(contrast(a, b), 2), required=t, kind=k, passes=contrast(a, b) >= t)
            for n, a, b, t, k in spec
        ]

    ic = c["icon"]
    icon = [
        dict(pair="icon body on icon background", fg=ic["body"], bg=ic["bg"], ratio=round(contrast(ic["body"], ic["bg"]), 2), required=3.0, kind="graphics", passes=contrast(ic["body"], ic["bg"]) >= 3),
        dict(pair="icon accent on icon background", fg=ic["accent"], bg=ic["bg"], ratio=round(contrast(ic["accent"], ic["bg"]), 2), required=3.0, kind="graphics", passes=contrast(ic["accent"], ic["bg"]) >= 3),
    ]
    return dict(
        concept=c["id"],
        tokens=dict(light=c["light"], dark=c["dark"], app_icon=ic),
        contrast=dict(light=pairs(c["light"]), dark=pairs(c["dark"]), app_icon=icon),
        notes=[
            "Ratios are WCAG 2.x relative-luminance contrast, computed by design/brand/tools/brand_lib.py.",
            "text pairs need 4.5:1, graphics and UI-component boundaries need 3:1 (WCAG 1.4.3 / 1.4.11).",
            "Dividers that are purely decorative may use the outline token at reduced opacity.",
        ],
    )


def typography_md(c: dict) -> str:
    d, b, m = c["fonts"]["display"], c["fonts"]["body"], c["fonts"]["mono"]
    return f"""# Typography direction: {c['name']}

All families are open-licensed and bundleable in a Compose Multiplatform app
(`composeResources/font`). Licence statements below are from upstream (Google Fonts
listings) and must be re-checked against the `OFL.txt` shipped with each font file
before bundling. No font file is included in this repository at the concept stage.

| Role | Family | Weights | Licence |
| --- | --- | --- | --- |
| Display / headings | {d[0]} | {d[1]} | {d[2]} |
| Body / UI | {b[0]} | {b[1]} | {b[2]} |
| Figures / data | {m[0]} | {m[1]} | {m[2]} |

## Scale (sp, mobile)

| Token | Size / line height | Family role | Use |
| --- | --- | --- | --- |
| display | 32 / 40 | display | trip title on the overview |
| headline | 24 / 32 | display | screen titles |
| title | 18 / 26 | display | card titles, day headings |
| body | 16 / 24 | body | default reading text (minimum for long text) |
| label | 14 / 20 | body | buttons, chips, navigation labels |
| caption | 12 / 16 | body | metadata, never the only carrier of meaning |

## Rules

- At most two families on screen. Use tabular figures wherever dates, prices, distances or times are aligned.
- Support Dynamic Type / font scale up to 200 percent; layouts reflow, nothing is clipped.
- Localised text (English and Spanish at 1.0, per PRD I18N-01) must be checked for the extended Latin glyph set before a family is accepted.
- The logotype is NOT set in these fonts: it is hand-drawn geometry converted to outlines, so the logo never depends on a font install or licence.
"""


# ----------------------------------------------------------------------------
# mock UI
# ----------------------------------------------------------------------------
def mock_ui_svg(c: dict, mode: str) -> str:
    p = c[mode]
    R = c["ui_radius"]
    ff = c["ui_fonts"]
    key = c["key"]
    uid = f"{key}{mode[0]}"
    col = dict(body=p["primary"], accent=p["accent"])
    out = []
    A = out.append
    W, H = 390, 844
    A(f'<rect width="{W}" height="{H}" fill="{p["bg"]}"/>')
    # status bar placeholders
    A(f'<text x="22" y="30" font-family="{ff}" font-size="13" font-weight="600" fill="{p["text"]}">9:41</text>')
    A(f'<rect x="318" y="20" width="22" height="10" rx="3" fill="{p["text_muted"]}"/>')
    A(f'<rect x="346" y="20" width="22" height="10" rx="3" fill="none" stroke="{p["text_muted"]}" stroke-width="1.5"/>')
    # app bar: symbol + wordmark
    T = Tf(34 / 512, 20 + OPTICAL[key][0] * 34 / 512, 56 + OPTICAL[key][1] * 34 / 512)
    A(SYMBOLS[key](T, col))
    wm, wmw = wordmark_only_paths(c["style"], 13, 66, 85, col, p["text"])
    A(wm)
    # offline chip
    A(f'<rect x="244" y="58" width="126" height="28" rx="14" fill="{p["surface"]}" stroke="{p["outline"]}" stroke-width="1"/>')
    A(f'<circle cx="261" cy="72" r="5" fill="{p["accent"]}"/>')
    A(f'<text x="272" y="76" font-family="{ff}" font-size="12" font-weight="600" fill="{p["text"]}">Offline ready</text>')
    # hero card
    A(f'<clipPath id="{uid}hc"><rect x="16" y="108" width="358" height="176" rx="{R}"/></clipPath>')
    A(f'<rect x="16" y="108" width="358" height="176" rx="{R}" fill="{p["primary"]}"/>')
    T2 = Tf(300 / 512, 160 + OPTICAL[key][0] * 300 / 512, 96 + OPTICAL[key][1] * 300 / 512)
    A(f'<g clip-path="url(#{uid}hc)" opacity="0.16">{SYMBOLS[key](T2, dict(body=p["on_primary"], accent=p["on_primary"]))}</g>')
    A(f'<text x="36" y="150" font-family="{ff}" font-size="12" font-weight="600" letter-spacing="0.6" fill="{p["on_primary"]}" opacity="0.85">NEXT TRIP</text>')
    A(f'<text x="36" y="188" font-family="{ff}" font-size="28" font-weight="700" fill="{p["on_primary"]}">Portugal coast</text>')
    A(f'<text x="36" y="214" font-family="{ff}" font-size="14" fill="{p["on_primary"]}">8 days  ·  3 stops  ·  412 km</text>')
    A(f'<rect x="36" y="238" width="178" height="30" rx="15" fill="{p["on_primary"]}" opacity="0.16"/>')
    A(f'<path d="M50 253l5 5 9-10" fill="none" stroke="{p["on_primary"]}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>')
    A(f'<text x="72" y="258" font-family="{ff}" font-size="12.5" font-weight="600" fill="{p["on_primary"]}">Stored on this device</text>')
    # section title
    A(f'<text x="20" y="320" font-family="{ff}" font-size="18" font-weight="700" fill="{p["text"]}">Itinerary</text>')
    A(f'<text x="370" y="320" text-anchor="end" font-family="{ff}" font-size="13" font-weight="600" fill="{p["primary"]}">Edit</text>')
    days = [("1", "Lisbon", "Alfama walk, tram 28, evening fado"), ("2", "Sintra", "Train 08:12, palace, coast road"), ("3", "Porto", "Ribeira, river cruise, wine cellar")]
    y = 336
    for n, t, s in days:
        A(f'<rect x="16" y="{y}" width="358" height="76" rx="{max(R-4, 6)}" fill="{p["surface"]}" stroke="{p["outline"]}" stroke-opacity="0.45" stroke-width="1"/>')
        A(f'<circle cx="50" cy="{y+38}" r="18" fill="{p["primary"]}"/>')
        A(f'<text x="50" y="{y+44}" text-anchor="middle" font-family="{ff}" font-size="16" font-weight="700" fill="{p["on_primary"]}">{n}</text>')
        A(f'<text x="80" y="{y+33}" font-family="{ff}" font-size="16" font-weight="700" fill="{p["text"]}">{t}</text>')
        A(f'<text x="80" y="{y+54}" font-family="{ff}" font-size="12.5" fill="{p["text_muted"]}">{s}</text>')
        A(f'<path d="M346 {y+30}l8 8-8 8" fill="none" stroke="{p["text_muted"]}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>')
        y += 88
    # optional suggestion
    A(f'<rect x="16" y="{y+4}" width="358" height="92" rx="{max(R-4, 6)}" fill="{p["bg"]}" stroke="{p["accent"]}" stroke-width="1.6" stroke-dasharray="6 5"/>')
    A(f'<text x="34" y="{y+30}" font-family="{ff}" font-size="11.5" font-weight="700" letter-spacing="0.5" fill="{p["accent"]}">SUGGESTION  ·  OPTIONAL  ·  ON DEVICE</text>')
    A(f'<text x="34" y="{y+52}" font-family="{ff}" font-size="14" fill="{p["text"]}">Add a sunset viewpoint near Day 2?</text>')
    A(f'<rect x="34" y="{y+62}" width="76" height="24" rx="12" fill="{p["accent"]}"/>')
    A(f'<text x="72" y="{y+78}" text-anchor="middle" font-family="{ff}" font-size="12" font-weight="700" fill="{p["on_accent"]}">Add</text>')
    A(f'<text x="128" y="{y+78}" font-family="{ff}" font-size="12" font-weight="700" fill="{p["text_muted"]}">Skip</text>')
    # bottom nav
    A(f'<rect x="0" y="760" width="{W}" height="84" fill="{p["surface"]}"/>')
    A(f'<rect x="0" y="760" width="{W}" height="1" fill="{p["outline"]}" opacity="0.5"/>')
    navs = [("Trips", 49), ("Map", 146), ("Saved", 243), ("Settings", 340)]
    for i, (lab, cx) in enumerate(navs):
        act = i == 0
        colr = p["primary"] if act else p["text_muted"]
        if act:
            A(f'<rect x="{cx-26}" y="768" width="52" height="30" rx="15" fill="{p["primary"]}" opacity="0.18"/>')
        if i == 0:
            A(f'<circle cx="{cx-7}" cy="789" r="3.4" fill="none" stroke="{colr}" stroke-width="2"/><path d="M{cx-5} 786l8-8" stroke="{colr}" stroke-width="2.2" stroke-linecap="round"/><circle cx="{cx+8}" cy="777" r="3.4" fill="{colr}"/>')
        elif i == 1:
            A(f'<path d="M{cx-11} 781l7-3 8 3 7-3v18l-7 3-8-3-7 3z" fill="none" stroke="{colr}" stroke-width="2" stroke-linejoin="round"/>')
        elif i == 2:
            A(f'<circle cx="{cx}" cy="783" r="10" fill="none" stroke="{colr}" stroke-width="2"/><path d="M{cx} 778v9m-4-4l4 4 4-4" fill="none" stroke="{colr}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>')
        else:
            A(f'<path d="M{cx-10} 778h20M{cx-10} 784h20M{cx-10} 790h20" stroke="{colr}" stroke-width="2" stroke-linecap="round"/><circle cx="{cx-4}" cy="778" r="2.6" fill="{p["surface"]}" stroke="{colr}" stroke-width="2"/><circle cx="{cx+5}" cy="784" r="2.6" fill="{p["surface"]}" stroke="{colr}" stroke-width="2"/><circle cx="{cx-3}" cy="790" r="2.6" fill="{p["surface"]}" stroke="{colr}" stroke-width="2"/>')
        A(f'<text x="{cx}" y="816" text-anchor="middle" font-family="{ff}" font-size="11.5" font-weight="{700 if act else 500}" fill="{colr}">{lab}</text>')
    A(f'<text x="195" y="837" text-anchor="middle" font-family="{ff}" font-size="9" fill="{p["text_muted"]}">Concept illustration, not a product screenshot</text>')
    return svg_doc(
        f"0 0 {W} {H}",
        f"Viajorapp concept {c['name']}: mock trip overview ({mode})",
        "Concept illustration of a mobile trip overview screen in the concept palette. Not a product screenshot. Uses live text with fallback system fonts.",
        "".join(out),
    )


# ----------------------------------------------------------------------------
# build
# ----------------------------------------------------------------------------
def build_concept(c: dict, tmp: Path) -> dict:
    d = CDIR / c["id"]
    prev = d / "preview"
    prev.mkdir(parents=True, exist_ok=True)
    key = c["key"]
    L, D = c["light"], c["dark"]
    title = f"Viajorapp concept {c['name']}"
    light_col = dict(body=L["primary"], accent=L["accent"])
    dark_col = dict(body=D["primary"], accent=D["accent"])
    mono_col = dict(body="#000000", accent="#000000")

    (d / "symbol.svg").write_text(symbol_svg(key, light_col, f"{title}: symbol"), encoding="utf-8")
    (d / "symbol-dark.svg").write_text(symbol_svg(key, dark_col, f"{title}: symbol for dark backgrounds"), encoding="utf-8")
    (d / "symbol-mono.svg").write_text(symbol_svg(key, mono_col, f"{title}: one-colour symbol"), encoding="utf-8")
    (d / "wordmark-lockup.svg").write_text(lockup_svg(key, c["style"], light_col, L["text"], f"{title}: horizontal lockup"), encoding="utf-8")
    (d / "wordmark-lockup-dark.svg").write_text(lockup_svg(key, c["style"], dark_col, D["text"], f"{title}: horizontal lockup for dark backgrounds"), encoding="utf-8")
    (d / "wordmark-lockup-mono.svg").write_text(lockup_svg(key, c["style"], mono_col, "#000000", f"{title}: one-colour horizontal lockup"), encoding="utf-8")

    # app icon: auto-scale so the farthest ink pixel sits inside the adaptive-icon safe circle
    ic = c["icon"]
    icon_col = dict(body=ic["body"], accent=ic["accent"])
    probe = app_icon_svg(key, "#000000", icon_col, 1.0, "probe").replace('<rect width="1024" height="1024" fill="#000000"/>', "")
    r1 = farthest_ink_radius(probe, tmp)
    scale = round(TARGET_RADIUS / r1, 3)
    icon_svg = app_icon_svg(key, ic["bg"], icon_col, scale, f"{title}: app icon")
    (d / "app-icon.svg").write_text(icon_svg, encoding="utf-8")
    (d / "app-icon-foreground.svg").write_text(
        app_icon_svg(key, None, icon_col, scale, f"{title}: adaptive icon foreground layer"), encoding="utf-8"
    )

    # palette + typography
    rep = palette_report(c)
    rep["app_icon_scale"] = scale
    rep["app_icon_probe_radius_at_scale_1"] = round(r1, 1)
    (d / "palette.json").write_text(json.dumps(rep, indent=2) + "\n", encoding="utf-8")
    (d / "typography.md").write_text(typography_md(c), encoding="utf-8")

    # mock UI
    for mode in ("light", "dark"):
        name = "mock-ui.svg" if mode == "light" else "mock-ui-dark.svg"
        (d / name).write_text(mock_ui_svg(c, mode), encoding="utf-8")
        resvg(d / name, prev / name.replace(".svg", ".png"), 780)

    # PNG previews
    for n in SIZES:
        resvg(d / "symbol.svg", prev / f"symbol-light-{n}.png", n, n, L["bg"])
        resvg(d / "symbol-dark.svg", prev / f"symbol-dark-{n}.png", n, n, D["bg"])
        resvg(d / "symbol-mono.svg", prev / f"symbol-mono-{n}.png", n, n, "#FFFFFF")
        resvg(d / "app-icon.svg", prev / f"app-icon-{n}.png", n, n)
    resvg(d / "wordmark-lockup.svg", prev / "wordmark-lockup-light-512.png", 512, None, L["bg"])
    resvg(d / "wordmark-lockup-dark.svg", prev / "wordmark-lockup-dark-512.png", 512, None, D["bg"])
    resvg(d / "app-icon.svg", prev / "app-icon-1024.png", 1024)
    # app icons must be opaque (iOS marketing icon: no alpha); resvg always writes RGBA
    for n in SIZES + [1024]:
        q = prev / f"app-icon-{n}.png"
        Image.open(q).convert("RGB").save(q)

    # small-size contact sheet: rows light/dark/mono at 16, 24, 32, 48 (1x and 4x nearest)
    sizes = [16, 24, 32, 48]
    rows = [("symbol.svg", L["bg"]), ("symbol-dark.svg", D["bg"]), ("symbol-mono.svg", "#FFFFFF")]
    pad = 12
    cell_w = sum(s * 5 + pad for s in sizes) + pad
    sheet = Image.new("RGB", (cell_w, 3 * (48 * 4 + pad * 2)), "#888888")
    yy = 0
    for svgname, bg in rows:
        xx = pad
        for s in sizes:
            tmp_png = tmp / f"_s{s}.png"
            resvg(d / svgname, tmp_png, s, s, bg)
            im = Image.open(tmp_png).convert("RGB")
            sheet.paste(im, (xx, yy + pad))
            big = im.resize((s * 4, s * 4), Image.NEAREST)
            sheet.paste(big, (xx + s + 4, yy + pad))
            xx += s * 5 + pad
        yy += 48 * 4 + pad * 2
    sheet.save(prev / "small-size-sheet.png")
    for t in tmp.glob("_s*.png"):
        t.unlink()
    return dict(scale=scale, r1=r1)


def main():
    tmp = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / ".tmp"
    tmp.mkdir(parents=True, exist_ok=True)
    info = {}
    for c in CONCEPTS:
        info[c["id"]] = build_concept(c, tmp)
        print(c["id"], info[c["id"]])


if __name__ == "__main__":
    main()
