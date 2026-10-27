"""Run the visual-asset-quality-review gate sequence and write concepts/QA_REPORT.md.

Usage: python design/brand/tools/run_qa.py
Calls asset_review.py (from the installed visual-asset-quality-review skill) and
independent checks (viewBox, live text, external refs, safe-zone radius).
The manual-review notes live in qa_manual_notes.py so the report is regenerable.
"""
from __future__ import annotations

import datetime
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from PIL import Image

from brand_lib import contrast
from concepts import CONCEPTS
from qa_manual_notes import MANUAL, VERDICT

ROOT = Path(__file__).resolve().parents[1]
CDIR = ROOT / "concepts"
AR = Path.home() / ".claude/skills/visual-asset-quality-review/scripts/asset_review.py"
VT = Path.home() / ".claude/skills/vector-asset-production/scripts/svgtool.py"
SAFE_R = 66 / 108 * 1024 / 2

LOGO_SVGS = ["symbol.svg", "symbol-dark.svg", "symbol-mono.svg", "wordmark-lockup.svg", "wordmark-lockup-dark.svg", "wordmark-lockup-mono.svg", "app-icon.svg", "app-icon-foreground.svg"]


def run(*args) -> str:
    r = subprocess.run([sys.executable, *map(str, args)], capture_output=True, text=True, encoding="utf-8", errors="replace")
    return (r.stdout + r.stderr).strip()


def sha(p: Path) -> str:
    return hashlib.sha256(p.read_bytes()).hexdigest()[:10]


def main():
    out = []
    A = out.append
    results = {}
    A("# Visual asset QA report\n")
    A(f"Generated {datetime.date.today().isoformat()} by `design/brand/tools/run_qa.py` using `asset_review.py` (visual-asset-quality-review) and `svgtool.py` (vector-asset-production). Heuristic numbers are pointers; the sheets in each `preview/` folder were looked at, see section 8.\n")
    A("Scope: logo SVGs, app icons, palettes of the three concept directions. Mock UI SVGs are illustrations with live text by design and are excluded from the no-live-text rule.\n")

    # ---- 1 structure
    A("## 1. Render, structure and portability\n")
    A("| File | viewBox | live `<text>` | external refs | script / foreignObject | width/height attrs | resvg render |\n| --- | --- | --- | --- | --- | --- | --- |")
    fails = 0
    raw_rc = []
    for c in CONCEPTS:
        for name in LOGO_SVGS:
            p = CDIR / c["id"] / name
            s = p.read_text(encoding="utf-8")
            m = re.search(r'<svg[^>]*viewBox="([^"]+)"', s)
            vb = m.group(1) if m else "MISSING"
            txt = len(re.findall(r"<text\b", s))
            ext = len(re.findall(r'(?:href|src)\s*=\s*"(?!#)|url\(\s*[\'"]?(?!#)|@import|https?://(?!www\.w3\.org)', s))
            scr = len(re.findall(r"<script|<foreignObject", s))
            wh = bool(re.search(r'<svg[^>]*\s(?:width|height)=', s))
            rc = run(AR, "render-check", p)
            ok = rc.startswith("[ OK ]")
            bad = (vb == "MISSING") or txt or ext or scr or wh or not ok
            fails += bool(bad)
            A(f"| {c['id']}/{name} | `{vb}` | {txt} | {ext} | {scr} | {'yes' if wh else 'no'} | {'OK' if ok else 'FAIL'} |")
            raw_rc.append(rc.splitlines()[0] + " " + (rc.splitlines()[1].strip() if len(rc.splitlines()) > 1 else ""))
    results["structure_fails"] = fails
    A(f"\nFiles failing any structural check: **{fails}** of {len(CONCEPTS) * len(LOGO_SVGS)}.\n")
    vt = run(VT, "validate", *[CDIR / c["id"] / n for c in CONCEPTS for n in LOGO_SVGS])
    A("`svgtool.py validate` summary (last lines):\n\n```\n" + "\n".join(vt.splitlines()[-6:]) + "\n```\n")

    # ---- 2 legibility
    A("## 2. Small-size legibility (symbol on transparent, light | dark | one-colour)\n")
    A("| Concept | 16 px ink / detail-loss | 24 px | 32 px | 48 px | sheet |\n| --- | --- | --- | --- | --- | --- |")
    for c in CONCEPTS:
        d = CDIR / c["id"]
        sheet = d / "preview" / "legibility-sheet.png"
        t = run(AR, "legibility", d / "symbol.svg", "--sizes", 16, 24, 32, 48, "--out", sheet)
        cells = re.findall(r"(\d+)px\s+ink\s+([\d.]+)%\s+detail-loss\s+([\d.]+)\s+(\w+)", t)
        by = {int(a): (b, c_, e) for a, b, c_, e in cells}
        A(f"| {c['name']} | " + " | ".join(f"{by[s][0]}% / {by[s][1]} ({by[s][2]})" for s in (16, 24, 32, 48)) + f" | `{c['id']}/preview/legibility-sheet.png` |")
    A("")

    # ---- 3 alignment
    A("## 3. Centering and margins\n")
    A("| Concept | Asset | bbox offset x / y | ink-centroid offset x / y | margins L / T / R / B | verdict |\n| --- | --- | --- | --- | --- | --- |")
    for c in CONCEPTS:
        d = CDIR / c["id"]
        for name, extra in (("symbol.svg", []), ("app-icon-foreground.svg", ["--size", "1024"])):
            t = run(AR, "align", d / name, *extra)
            bb = re.search(r"bbox centre offset\s+x\s*([+-][\d.]+)%\s+y\s*([+-][\d.]+)%", t)
            ic = re.search(r"ink centroid offset x\s*([+-][\d.]+)%\s+y\s*([+-][\d.]+)%", t)
            mg = re.search(r"margins: L ([\d.]+)%\s+T ([\d.]+)%\s+R ([\d.]+)%\s+B ([\d.]+)%", t)
            v = t.strip().splitlines()[-1]
            A(f"| {c['name']} | {name} | {bb.group(1)}% / {bb.group(2)}% | {ic.group(1)}% / {ic.group(2)}% | {mg.group(1)} / {mg.group(2)} / {mg.group(3)} / {mg.group(4)} % | {v} |")
    A("\nSymbol margins are measured on the 512 canvas; foreground margins on the 1024 canvas.\n")

    # ---- 4 safe zone (independent)
    A("## 4. Adaptive-icon safe zone (independent measurement)\n")
    A(f"Android adaptive icons use a 108 dp canvas with a 66 dp safe circle: radius = 66/108 x 1024 / 2 = **{SAFE_R:.1f} px** at our 1024 viewBox (the brief's \"about 66 percent\" is 676 px diameter, a looser figure; the 66 dp rule is stricter, so it is the one enforced here).\n")
    A("| Concept | farthest ink pixel from centre | safe radius | margin | verdict |\n| --- | --- | --- | --- | --- |")
    for c in CONCEPTS:
        d = CDIR / c["id"]
        png = d / "preview" / "_fg.png"
        subprocess.run(["resvg", "-w", "1024", str(d / "app-icon-foreground.svg"), str(png)], check=True)
        im = Image.open(png).convert("RGBA")
        a = im.split()[3].load()
        best = 0.0
        for y in range(1024):
            for x in range(1024):
                if a[x, y] > 8:
                    r = ((x + 0.5 - 512) ** 2 + (y + 0.5 - 512) ** 2) ** 0.5
                    best = max(best, r)
        png.unlink()
        v = "PASS" if best <= SAFE_R else "FAIL"
        results[f"safe_{c['id']}"] = v
        A(f"| {c['name']} | {best:.1f} px | {SAFE_R:.1f} px | {SAFE_R - best:.1f} px | {v} |")
    A("")

    # ---- 5 contrast
    A("## 5. Contrast (WCAG 2.x), cross-checked with `asset_review.py contrast`\n")
    A("Full pair lists are in each concept's `palette.json`. Below, every pair is re-run through `asset_review.py` and compared with the generator's number.\n")
    A("| Concept | Mode | Pair | Ratio (generator) | Ratio (asset_review) | Need | Result |\n| --- | --- | --- | ---: | ---: | ---: | --- |")
    cfails = 0
    for c in CONCEPTS:
        rep = json.loads((CDIR / c["id"] / "palette.json").read_text(encoding="utf-8"))
        for mode in ("light", "dark", "app_icon"):
            for r in rep["contrast"][mode]:
                kind = "--ui" if r["kind"] == "graphics" else ""
                t = run(AR, "contrast", "--fg", r["fg"], "--bg", r["bg"], *( [kind] if kind else []))
                m = re.search(r":\s*([\d.]+):1", t)
                ar = float(m.group(1))
                same = abs(ar - r["ratio"]) < 0.015
                ok = r["passes"] and same
                cfails += not ok
                A(f"| {c['name']} | {mode} | {r['pair']} | {r['ratio']:.2f} | {ar:.2f} | {r['required']:.1f} | {'PASS' if ok else 'FAIL'} |")
    results["contrast_fails"] = cfails
    A(f"\nContrast failures: **{cfails}**.\n")

    # ---- 6 masks
    A("## 6. Adaptive-icon mask previews\n")
    for c in CONCEPTS:
        d = CDIR / c["id"]
        t = run(AR, "maskpreview", d / "preview" / "app-icon-1024.png", "--out", d / "preview" / "app-icon-masks.png")
        A(f"- {c['name']}: `{c['id']}/preview/app-icon-masks.png` generated. Tool output: `{' '.join(t.split())[:160]}`")
    A("")

    # ---- 7 completeness
    A("## 7. Deliverable completeness\n")
    need = ["symbol.svg", "symbol-mono.svg", "wordmark-lockup.svg", "app-icon.svg", "palette.json", "typography.md", "README.md", "mock-ui.svg", "preview/mock-ui.png"]
    for s_ in (16, 32, 48, 128, 512):
        for k in ("light", "dark", "mono"):
            need.append(f"preview/symbol-{k}-{s_}.png")
    files = []
    for c in CONCEPTS:
        for n in need:
            e = {"path": f"concepts/{c['id']}/{n}"}
            m_ = re.match(r"preview/symbol-\w+-(\d+)\.png", n)
            if m_:
                e["size"] = [int(m_.group(1))] * 2
            files.append(e)
        files.append({"path": f"concepts/{c['id']}/preview/app-icon-1024.png", "size": [1024, 1024], "alpha": False})
        files.append({"path": f"concepts/{c['id']}/preview/mock-ui.png", "size": [780, 1688]})
    files.append({"path": "gallery/index.html"})
    man = ROOT / ".qa-manifest.json"
    man.write_text(json.dumps({"files": files}), encoding="utf-8")
    t = run(AR, "completeness", man, "--root", ROOT)
    man.unlink()
    A("```\n" + "\n".join(t.splitlines()[-8:]) + "\n```\n")
    results["completeness_tail"] = t.splitlines()[-1] if t else ""

    # ---- 8 manual
    A("## 8. Manual review (sheets looked at)\n")
    for k, v in MANUAL.items():
        A(f"- **{k}:** {v}")
    A("")
    A("## 9. Files reviewed (sha256, first 10 hex)\n")
    A("| File | hash |\n| --- | --- |")
    for c in CONCEPTS:
        for n in ("symbol.svg", "symbol-mono.svg", "wordmark-lockup.svg", "app-icon.svg"):
            A(f"| {c['id']}/{n} | `{sha(CDIR / c['id'] / n)}` |")
    A("")
    A("## 10. Verdict\n")
    A(VERDICT.format(**results))
    A("\n## 11. Not verified\n")
    A("- Not tested on physical devices, real launchers (OEM masks differ) or iOS springboard.\n- Contrast uses WCAG 2.x, not APCA. Small-size numbers are heuristics.\n- Colour-blind simulation was reasoned from the palettes (no red/green-only encoding), not simulated with a tool.\n- Mock UI PNGs were rendered with system fallback fonts (Segoe UI, Georgia); the proposed open fonts are not installed or bundled.\n- No trademark or design-patent search was run; lookalike notes in each concept README are judgement only.\n")
    (CDIR / "QA_REPORT.md").write_text("\n".join(out), encoding="utf-8")
    print(json.dumps(results, indent=2))


if __name__ == "__main__":
    main()
