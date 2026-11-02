"""Convert the hand-authored brand SVGs to Android vector drawables (also read by Compose Multiplatform).

The brand SVGs use only <g>, <path>, <circle> with fill, stroke, stroke-width, stroke-linejoin and
stroke-linecap, so this converter supports exactly that and fails loudly on anything else. Run:

    python design/brand/tools/svg_to_vector.py

Outputs go to ui/src/commonMain/composeResources/drawable/.
"""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
CONCEPT = ROOT / "design/brand/concepts/01-waypoint"
OUT = ROOT / "ui/src/commonMain/composeResources/drawable"
NS = "{http://www.w3.org/2000/svg}"

SOURCES = {
    "brand_symbol_light": "symbol.svg",
    "brand_symbol_dark": "symbol-dark.svg",
    "brand_lockup_light": "wordmark-lockup.svg",
    "brand_lockup_dark": "wordmark-lockup-dark.svg",
}


def fmt(v):
    return ("%.2f" % float(v)).rstrip("0").rstrip(".")


def circle_path(cx, cy, r):
    cx, cy, r = float(cx), float(cy), float(r)
    return f"M{fmt(cx - r)},{fmt(cy)}a{fmt(r)},{fmt(r)} 0 1,0 {fmt(2 * r)},0a{fmt(r)},{fmt(r)} 0 1,0 {fmt(-2 * r)},0z"


def paint(attrs, name, default=None):
    v = attrs.get(name, default)
    if v in (None, "none"):
        return None
    if not (v.startswith("#") and len(v) == 7):
        raise ValueError(f"unsupported colour {v!r}")
    return "#FF" + v[1:].upper()


def convert(svg_path):
    root = ET.parse(svg_path).getroot()
    vb = [float(x) for x in root.attrib["viewBox"].split()]
    paths = []

    def walk(node):
        for child in node:
            tag = child.tag.replace(NS, "")
            if tag in ("title", "desc"):
                continue
            if tag == "g":
                walk(child)
                continue
            if tag == "path":
                data = child.attrib["d"]
            elif tag == "circle":
                data = circle_path(child.attrib["cx"], child.attrib["cy"], child.attrib["r"])
            else:
                raise ValueError(f"unsupported element {tag} in {svg_path.name}")
            a = child.attrib
            attrs = [f'        android:pathData="{data}"']
            fill = paint(a, "fill", "#000000")
            stroke = paint(a, "stroke")
            if fill:
                attrs.append(f'        android:fillColor="{fill}"')
            if stroke:
                attrs.append(f'        android:strokeColor="{stroke}"')
                attrs.append(f'        android:strokeWidth="{fmt(a.get("stroke-width", "1"))}"')
                attrs.append(f'        android:strokeLineJoin="{a.get("stroke-linejoin", "miter")}"')
                attrs.append(f'        android:strokeLineCap="{a.get("stroke-linecap", "butt")}"')
            paths.append("    <path\n" + "\n".join(attrs) + " />")

    walk(root)
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{fmt(vb[2])}dp"\n    android:height="{fmt(vb[3])}dp"\n'
        f'    android:viewportWidth="{fmt(vb[2])}"\n    android:viewportHeight="{fmt(vb[3])}">\n'
        + "\n".join(paths)
        + "\n</vector>\n"
    )


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, source in SOURCES.items():
        (OUT / f"{name}.xml").write_text(convert(CONCEPT / source), encoding="utf-8", newline="\n")
        print("wrote", name)


if __name__ == "__main__":
    sys.exit(main())
