"""Build design/brand/gallery/index.html: self-contained (all assets are base64 data
URIs, no network, no external files). Run after build_brand.py."""
from __future__ import annotations

import base64
import html
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from concepts import CONCEPTS
from scoring import CRITERIA, NOTES, SCORES, total

ROOT = Path(__file__).resolve().parents[1]
CDIR = ROOT / "concepts"
RECOMMENDED = "01-waypoint"


def uri(path: Path) -> str:
    mime = "image/svg+xml" if path.suffix == ".svg" else "image/png"
    return f"data:{mime};base64," + base64.b64encode(path.read_bytes()).decode()


def img(path: Path, alt: str, cls: str = "", **attrs) -> str:
    a = "".join(f' {k.replace("_", "-")}="{v}"' for k, v in attrs.items())
    return f'<img class="{cls}" src="{uri(path)}" alt="{html.escape(alt)}"{a}>'


def pair(light: Path, dark: Path, alt: str, cls: str = "", **attrs) -> str:
    return img(light, alt, f"{cls} on-light".strip(), **attrs) + img(dark, alt, f"{cls} on-dark".strip(), **attrs)


def swatches(p: dict) -> str:
    keys = [("primary", "primary"), ("accent", "accent"), ("bg", "bg"), ("surface", "surface"), ("text", "text"), ("text_muted", "text-muted"), ("outline", "outline")]
    out = []
    for k, label in keys:
        out.append(
            f'<div class="sw"><span class="chip" style="background:{p[k]}"></span><span class="swl">{label}<br><code>{p[k]}</code></span></div>'
        )
    return '<div class="sws">' + "".join(out) + "</div>"


def contrast_table(rep: dict, mode: str) -> str:
    rows = ""
    for r in rep["contrast"][mode]:
        cls = "ok" if r["passes"] else "bad"
        mark = "pass" if r["passes"] else "fail"
        rows += (
            f'<tr><td>{html.escape(r["pair"])}</td><td><span class="dot" style="background:{r["fg"]}"></span>'
            f'<span class="dot" style="background:{r["bg"]}"></span></td>'
            f'<td class="num">{r["ratio"]:.2f}:1</td><td class="num">{r["required"]:.1f}</td><td class="{cls}">{mark}</td></tr>'
        )
    return f'<table class="ct"><thead><tr><th>{mode} pair</th><th></th><th>ratio</th><th>need</th><th></th></tr></thead><tbody>{rows}</tbody></table>'


def card(c: dict) -> str:
    d = CDIR / c["id"]
    rep = json.loads((d / "palette.json").read_text(encoding="utf-8"))
    L, D = c["light"], c["dark"]
    sc = SCORES[c["id"]]
    rec = c["id"] == RECOMMENDED
    prev = d / "preview"
    # small-size row: png previews at 16, 24(not rendered -> use svg scaled), 32, 48
    small = ""
    for n in (16, 24, 32, 48):
        small += (
            f'<span class="sm">{pair(d / "symbol.svg", d / "symbol-dark.svg", c["name"] + " symbol", width=n, height=n)}'
            f'<small>{n}</small></span>'
        )
    mono = "".join(
        f'<span class="sm mono">{img(d / "symbol-mono.svg", c["name"] + " one-colour symbol", width=n, height=n)}<small>{n}</small></span>'
        for n in (16, 24, 32, 48)
    )
    icon = d / "app-icon.svg"
    masks = "".join(
        f'<figure><div class="mask {m}">{img(icon, c["name"] + " app icon", "ic")}</div><figcaption>{lab}</figcaption></figure>'
        for m, lab in [("circle", "Android circle"), ("squircle", "Android squircle"), ("round", "Android rounded"), ("ios", "iOS squircle")]
    )
    return f"""
<article class="card" style="--cbg-l:{L['bg']};--cbg-d:{D['bg']};--csf-l:{L['surface']};--csf-d:{D['surface']}" id="{c['id']}">
  <header>
    <p class="num-badge">{c['id'][:2]}</p>
    <h2>{c['name']}{' <span class="rec">Recommended</span>' if rec else ''}</h2>
    <p class="idea">{html.escape(c['idea'])}</p>
    <p class="score">Weighted score <strong>{total(sc)}</strong> / 100</p>
  </header>

  <h3>Symbol and lockup</h3>
  <div class="row">
    <div class="tile sq">{pair(d / 'symbol.svg', d / 'symbol-dark.svg', c['name'] + ' symbol')}</div>
    <div class="tile sq mono-tile">{img(d / 'symbol-mono.svg', c['name'] + ' one-colour symbol')}</div>
  </div>
  <div class="tile wide">{pair(d / 'wordmark-lockup.svg', d / 'wordmark-lockup-dark.svg', c['name'] + ' horizontal lockup')}</div>

  <h3>App icon masks</h3>
  <div class="masks">{masks}</div>
  <p class="hint">Icon artwork sits inside the 66 dp adaptive-icon safe circle (farthest ink {round(rep['app_icon_probe_radius_at_scale_1'] * rep['app_icon_scale'])} of 313 px at 1024).</p>

  <h3>Small sizes</h3>
  <div class="smalls tile">{small}</div>
  <div class="smalls tile mono-tile">{mono}</div>

  <h3>Palette (light and dark)</h3>
  <div class="palbox pal-l" style="background:{L['bg']}">{swatches(L)}</div>
  <div class="palbox pal-d" style="background:{D['bg']};color:{D['text']}">{swatches(D)}</div>
  <details><summary>Contrast ratios (WCAG 2.x)</summary>{contrast_table(rep, 'light')}{contrast_table(rep, 'dark')}</details>

  <h3>Mock UI <small>(concept illustration, not a product screenshot)</small></h3>
  <div class="mock">{pair(d / 'preview' / 'mock-ui.png', d / 'preview' / 'mock-ui-dark.png', c['name'] + ' mock trip overview', 'ui')}</div>

  <h3>Type direction</h3>
  <p class="type">Headings <strong>{c['fonts']['display'][0]}</strong> &middot; body <strong>{c['fonts']['body'][0]}</strong> &middot; {c['fonts']['display'][2]}. The logotype is drawn outlines, not a font.</p>
</article>
"""


def scoring_table() -> str:
    ids = [c["id"] for c in CONCEPTS]
    names = {c["id"]: c["name"] for c in CONCEPTS}
    th = "".join(f'<th class="{"recc" if i == RECOMMENDED else ""}">{names[i]}</th>' for i in ids)
    rows = ""
    for k, label, w, desc in CRITERIA:
        cells = "".join(
            f'<td class="num {"recc" if i == RECOMMENDED else ""}" title="{html.escape(NOTES[i][k])}">{SCORES[i][k]}<span class="x">/5</span></td>'
            for i in ids
        )
        rows += f'<tr><th scope="row">{label}<small>{html.escape(desc)}</small></th><td class="num">{w}</td>{cells}</tr>'
    tot = "".join(f'<td class="num tot {"recc" if i == RECOMMENDED else ""}">{total(SCORES[i])}</td>' for i in ids)
    notes = ""
    for k, label, w, _ in CRITERIA:
        notes += f"<li><strong>{label}:</strong> " + " ".join(f"<em>{names[i]}</em>: {html.escape(NOTES[i][k])}" for i in ids) + "</li>"
    return f"""
<section class="scoring" id="scoring">
  <h2>Scoring</h2>
  <p>Scores 1 to 5 against the rubric in <code>docs/design/BRAND_STRATEGY.md</code>, weighted to 100. Hover a score for the reason.</p>
  <div class="scroll"><table class="scoretbl"><thead><tr><th>Criterion</th><th>Weight</th>{th}</tr></thead>
  <tbody>{rows}<tr><th scope="row">Weighted total</th><td class="num">100</td>{tot}</tr></tbody></table></div>
  <details><summary>Reasons per criterion</summary><ul class="reasons">{notes}</ul></details>
</section>"""


CSS = """
:root{--bg:#F5F6F7;--surface:#fff;--text:#14181B;--muted:#4C565C;--line:#C9D0D4;--tile:#fff;--link:#0B5563;--ok:#0B6B3A;--bad:#A4262C;--shadow:0 1px 2px rgba(0,0,0,.08)}
:root[data-theme=dark]{--bg:#0F1315;--surface:#171C1F;--text:#ECF0F2;--muted:#A7B2B8;--line:#33403F;--tile:#1E2427;--link:#7FD0DE;--ok:#6FD39A;--bad:#FF8A90;--shadow:none}
@media (prefers-color-scheme:dark){:root:not([data-theme=light]){--bg:#0F1315;--surface:#171C1F;--text:#ECF0F2;--muted:#A7B2B8;--line:#33403F;--tile:#1E2427;--link:#7FD0DE;--ok:#6FD39A;--bad:#FF8A90;--shadow:none}}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--text);font:16px/1.5 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
a{color:var(--link)}
code{font-family:ui-monospace,Consolas,monospace;font-size:.85em}
.on-dark{display:none}
:root[data-theme=dark] .on-light{display:none}
:root[data-theme=dark] .on-dark{display:inline-block}
@media (prefers-color-scheme:dark){:root:not([data-theme=light]) .on-light{display:none}:root:not([data-theme=light]) .on-dark{display:inline-block}}
.top{position:sticky;top:0;z-index:5;background:var(--surface);border-bottom:1px solid var(--line);padding:10px 16px;display:flex;gap:12px;align-items:center;justify-content:space-between;flex-wrap:wrap}
.top strong{font-size:1.05rem}
.top nav a{margin-right:12px;font-size:.9rem}
button.tg{font:inherit;font-size:.9rem;padding:8px 14px;min-height:44px;border-radius:22px;border:1px solid var(--line);background:var(--bg);color:var(--text);cursor:pointer}
button.tg:focus-visible,a:focus-visible,summary:focus-visible{outline:3px solid var(--link);outline-offset:2px}
main{max-width:1280px;margin:0 auto;padding:16px}
.intro h1{margin:.2em 0 .1em;font-size:1.8rem}
.intro p{max-width:70ch;color:var(--muted)}
.banner{background:var(--surface);border:1px solid var(--line);border-left:6px solid var(--link);border-radius:10px;padding:14px 16px;margin:16px 0;box-shadow:var(--shadow)}
.banner h2{margin:.1em 0}
.grid{display:grid;gap:16px;grid-template-columns:repeat(auto-fit,minmax(min(100%,340px),1fr))}
.card{background:var(--surface);border:1px solid var(--line);border-radius:14px;padding:16px;box-shadow:var(--shadow);--cbg:var(--cbg-l);--csf:var(--csf-l)}
:root[data-theme=dark] .card{--cbg:var(--cbg-d);--csf:var(--csf-d)}
@media (prefers-color-scheme:dark){:root:not([data-theme=light]) .card{--cbg:var(--cbg-d);--csf:var(--csf-d)}}
.card header .num-badge{margin:0;color:var(--muted);font-weight:700;letter-spacing:.08em}
.card h2{margin:.1em 0 .3em;font-size:1.4rem}
.card h3{margin:1.2em 0 .5em;font-size:.95rem;text-transform:uppercase;letter-spacing:.06em;color:var(--muted)}
.card h3 small{text-transform:none;letter-spacing:0;font-weight:400}
.rec{font-size:.7rem;vertical-align:middle;background:var(--link);color:var(--surface);padding:3px 8px;border-radius:12px;letter-spacing:.04em}
.idea{color:var(--muted);margin:.2em 0}
.score strong{font-size:1.2rem}
.row{display:grid;grid-template-columns:1fr 1fr;gap:10px}
.tile{background:var(--cbg);border:1px solid var(--line);border-radius:10px;display:flex;align-items:center;justify-content:center;padding:14px}
.tile.sq{aspect-ratio:1/1}
.tile.sq img{width:78%;height:78%}
.tile.wide{margin-top:10px;min-height:96px}
.tile.wide img{width:100%;max-height:120px}
.mono-tile{background:#fff}
:root[data-theme=dark] .mono-tile{background:#0B0D0E}
:root[data-theme=dark] .mono-tile img{filter:invert(1)}
@media (prefers-color-scheme:dark){:root:not([data-theme=light]) .mono-tile{background:#0B0D0E}:root:not([data-theme=light]) .mono-tile img{filter:invert(1)}}
.masks{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;text-align:center}
.masks figure{margin:0}
.masks figcaption{font-size:.72rem;color:var(--muted);margin-top:4px}
.mask{width:100%;aspect-ratio:1/1;overflow:hidden;background:#888;margin:0 auto;max-width:96px}
.mask.circle{border-radius:50%}
.mask.squircle{border-radius:36%}
.mask.round{border-radius:22%}
.mask.ios{border-radius:22.37%}
.mask img{width:100%;height:100%;display:block}
.hint{font-size:.8rem;color:var(--muted)}
.smalls{gap:18px;justify-content:flex-start;margin-top:8px;padding:10px 14px}
.sm{display:inline-flex;flex-direction:column;align-items:center;gap:4px}
.sm small{font-size:.7rem;color:var(--muted)}
.mono-tile .sm small{color:#555}
:root[data-theme=dark] .mono-tile .sm small{color:#C4CCD0}
@media (prefers-color-scheme:dark){:root:not([data-theme=light]) .mono-tile .sm small{color:#C4CCD0}}
.palbox{border:1px solid var(--line);border-radius:10px;padding:10px;margin-top:8px;color:#14181B}
.sws{display:grid;grid-template-columns:repeat(auto-fill,minmax(98px,1fr));gap:8px}
.sw{display:flex;align-items:center;gap:8px;font-size:.72rem;line-height:1.25}
.chip{width:30px;height:30px;border-radius:8px;border:1px solid rgba(128,128,128,.5);flex:none}
.pal-d .swl{color:inherit}
details{margin-top:10px}
summary{cursor:pointer;font-size:.9rem;min-height:32px}
.ct{width:100%;border-collapse:collapse;font-size:.78rem;margin:8px 0}
.ct th,.ct td{padding:3px 6px;border-bottom:1px solid var(--line);text-align:left}
.ct .num,.scoretbl .num{text-align:right;font-variant-numeric:tabular-nums}
.ct .ok{color:var(--ok);font-weight:700}.ct .bad{color:var(--bad);font-weight:700}
.dot{display:inline-block;width:12px;height:12px;border-radius:50%;border:1px solid rgba(128,128,128,.6);margin-right:3px;vertical-align:middle}
.mock{display:flex;justify-content:center}
.mock img.ui{width:100%;max-width:300px;border-radius:22px;border:1px solid var(--line)}
.type{font-size:.9rem;color:var(--muted)}
.scoring{margin-top:28px;background:var(--surface);border:1px solid var(--line);border-radius:14px;padding:16px}
.scroll{overflow-x:auto}
.scoretbl{border-collapse:collapse;width:100%;min-width:560px}
.scoretbl th,.scoretbl td{padding:8px 10px;border-bottom:1px solid var(--line)}
.scoretbl thead th{text-align:center}
.scoretbl thead th:first-child{text-align:left}
.scoretbl tbody th{text-align:left;font-weight:600}
.scoretbl th small{display:block;font-weight:400;color:var(--muted);font-size:.75rem}
.scoretbl .x{color:var(--muted);font-size:.75rem}
.scoretbl .recc{background:rgba(11,85,99,.12)}
:root[data-theme=dark] .scoretbl .recc{background:rgba(108,203,219,.14)}
@media (prefers-color-scheme:dark){:root:not([data-theme=light]) .scoretbl .recc{background:rgba(108,203,219,.14)}}
.scoretbl .tot{font-size:1.15rem;font-weight:800}
.reasons{padding-left:18px;font-size:.88rem}.reasons li{margin:.5em 0}
footer{color:var(--muted);font-size:.85rem;margin:28px 0 12px}
@media (max-width:520px){.masks{grid-template-columns:repeat(2,1fr)}.row{gap:8px}}
@media (prefers-reduced-motion:no-preference){.card{transition:box-shadow .2s}}
"""

JS = """
(function(){
  var root=document.documentElement,btn=document.getElementById('theme'),modes=['auto','light','dark'];
  function get(){try{return localStorage.getItem('viajorapp-gallery-theme')||'auto'}catch(e){return 'auto'}}
  function set(m){try{localStorage.setItem('viajorapp-gallery-theme',m)}catch(e){}}
  function apply(m){
    if(m==='auto'){root.removeAttribute('data-theme')}else{root.setAttribute('data-theme',m)}
    btn.textContent='Theme: '+m+' (click to change)';
  }
  var cur=get();apply(cur);
  btn.addEventListener('click',function(){cur=modes[(modes.indexOf(cur)+1)%3];set(cur);apply(cur)});
})();
"""


def main():
    cards = "".join(card(c) for c in CONCEPTS)
    rec = next(c for c in CONCEPTS if c["id"] == RECOMMENDED)
    html_doc = f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="light dark">
<title>Viajorapp brand concepts</title>
<style>{CSS}</style>
</head>
<body>
<div class="top">
  <div><strong>Viajorapp</strong> brand concepts</div>
  <nav aria-label="Sections"><a href="#concepts">Concepts</a><a href="#scoring">Scoring</a></nav>
  <button class="tg" id="theme" type="button">Theme: auto</button>
</div>
<main>
  <section class="intro">
    <h1>Three directions for Viajorapp</h1>
    <p>Plan smarter. Travel further. Concept exploration only: every mark is hand-authored vector geometry rendered locally, with no image generator and no network access. Mock screens are concept illustrations, not product screenshots.</p>
  </section>
  <aside class="banner" aria-label="Recommendation">
    <h2>Recommendation: {rec['name']} ({total(SCORES[RECOMMENDED])} / 100)</h2>
    <p>Highest weighted score: it is the simplest mark, survives 16 px in colour and one colour, fills the adaptive-icon safe zone and carries the V of the name. Its weakest point is distinctiveness (route and node motifs are common in mapping), scored 3 of 5. <strong>This is a recommendation, not a decision:</strong> final brand approval belongs to the project owner, and the choice is reversible because every asset is generated behind design tokens.</p>
  </aside>
  <section id="concepts" class="grid" aria-label="Concepts">{cards}</section>
  {scoring_table()}
  <footer>
    <p>Fonts named in the type direction are open-licensed (SIL OFL 1.1 per upstream) and are not bundled here; the mock UIs fall back to system fonts. No trademark search was performed; confusion risks are the designer's judgement. Source: <code>design/brand/tools/</code>.</p>
  </footer>
</main>
<script>{JS}</script>
</body>
</html>
"""
    out = ROOT / "gallery"
    out.mkdir(exist_ok=True)
    (out / "index.html").write_text(html_doc, encoding="utf-8")
    print("gallery", (out / "index.html").stat().st_size, "bytes")


if __name__ == "__main__":
    main()
