"""Generate the app's line icons as Android vector drawables (read by Compose Multiplatform).

The icons share the brand's construction: 24 unit grid, 2 unit round strokes, a ring and a disc where the
symbol has them. Run: python design/brand/tools/make_icons.py
"""
from pathlib import Path

OUT = Path(__file__).resolve().parents[3] / "ui/src/commonMain/composeResources/drawable"

STROKE = "#FF000000"


def stroke(d):
    return (
        f'    <path android:pathData="{d}" android:strokeColor="{STROKE}" android:strokeWidth="2"'
        ' android:strokeLineCap="round" android:strokeLineJoin="round" />'
    )


def disc(cx, cy, r):
    return (
        f'    <path android:pathData="M{cx - r},{cy}a{r},{r} 0 1,0 {2 * r},0a{r},{r} 0 1,0 {-2 * r},0z"'
        f' android:fillColor="{STROKE}" />'
    )


ICONS = {
    "ic_route": [
        stroke("M3.5,6a2.5,2.5 0 1,0 5,0a2.5,2.5 0 1,0 -5,0z"),
        stroke("M7.2,8.2L12,18L17.6,7.6"),
        disc(18, 6, 2),
    ],
    "ic_map": [stroke("M3,6L9,4L15,6L21,4L21,18L15,20L9,18L3,20Z"), stroke("M9,4L9,18"), stroke("M15,6L15,20")],
    "ic_search": [stroke("M5,11a6,6 0 1,0 12,0a6,6 0 1,0 -12,0z"), stroke("M15.5,15.5L20,20")],
    "ic_sliders": [
        stroke("M4,7L20,7"), stroke("M4,12L20,12"), stroke("M4,17L20,17"),
        disc(9, 7, 2.2), disc(15, 12, 2.2), disc(9, 17, 2.2),
    ],
    "ic_chevron": [stroke("M9,6L15,12L9,18")],
    "ic_more": [disc(12, 5, 1.7), disc(12, 12, 1.7), disc(12, 19, 1.7)],
    "ic_add": [stroke("M12,5L12,19"), stroke("M5,12L19,12")],
}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, parts in ICONS.items():
        xml = (
            '<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n'
            + "\n".join(parts)
            + "\n</vector>\n"
        )
        (OUT / f"{name}.xml").write_text(xml, encoding="utf-8", newline="\n")
        print("wrote", name)


if __name__ == "__main__":
    main()
