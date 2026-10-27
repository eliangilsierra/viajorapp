# Viajorapp brand (concept stage)

Brand strategy: [`docs/design/BRAND_STRATEGY.md`](../../docs/design/BRAND_STRATEGY.md).

| Path | What |
| --- | --- |
| `concepts/01-waypoint`, `02-keyhole-road`, `03-contour` | Three concepts: symbol, mono, dark, lockups, app icon, palette, typography, mock UI, `preview/` PNGs, README |
| `concepts/QA_REPORT.md` | Quality-review results with numbers |
| `gallery/index.html` | Self-contained side-by-side comparison (open locally; no network) |
| `tools/` | Python generators and QA runner; everything above is reproducible from here |

Rebuild (Python 3.12 with Pillow, `resvg` on PATH, no network):

```
python design/brand/tools/build_brand.py <scratch-dir>
python design/brand/tools/build_readmes.py
python design/brand/tools/build_gallery.py
python design/brand/tools/run_qa.py
```

These are concept assets, not final production assets. Final brand approval belongs to the project owner.
