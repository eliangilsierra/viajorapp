"""Evaluation rubric and designer scores (1-5). Scores are a judgement by the brand
designer after viewing renders and running the QA checks; they are an input to the
project owner's decision, not a decision."""

CRITERIA = [
    # key, label, weight, what a 5 means
    ("recognition", "Recognition", 15, "Memorable, nameable in one sentence, recalled after one sighting"),
    ("simplicity", "Simplicity", 10, "Few shapes, one idea, no decoration"),
    ("scalability", "Scalability", 15, "Identifiable at 16 px and in one colour; holds up large"),
    ("distinctiveness", "Distinctiveness", 15, "Unlike travel/map/security category defaults; low confusion risk"),
    ("accessibility", "Accessibility", 10, "Contrast pairs pass; no red/green reliance; shape carries meaning"),
    ("reproducibility", "Reproducibility", 10, "Rebuildable from primitives; trivial to convert to VectorDrawable / Compose Path"),
    ("mobile", "Mobile suitability", 15, "Fills the adaptive-icon safe zone, centred under circle/squircle masks"),
    ("cross_surface", "Cross-surface consistency", 10, "Same mark and tokens work on app, store, web, README, print, embroidery"),
]

SCORES = {
    "01-waypoint": dict(
        recognition=4, simplicity=5, scalability=5, distinctiveness=3,
        accessibility=4, reproducibility=5, mobile=5, cross_surface=4,
    ),
    "02-keyhole-road": dict(
        recognition=4, simplicity=4, scalability=4, distinctiveness=4,
        accessibility=4, reproducibility=4, mobile=4, cross_surface=4,
    ),
    "03-contour": dict(
        recognition=3, simplicity=3, scalability=3, distinctiveness=4,
        accessibility=3, reproducibility=3, mobile=3, cross_surface=3,
    ),
}

NOTES = {
    "01-waypoint": dict(
        recognition="V initial plus route-with-two-nodes is nameable in one sentence.",
        simplicity="Three primitives: stroked polyline, ring, disc.",
        scalability="Holds at 16 px in colour and mono; origin ring hole is the first detail to close.",
        distinctiveness="Route and node motifs are common in mapping and travel; the V-as-route is the differentiator. Lowest score of the three.",
        accessibility="Primary 7.7:1 and accent 4.2:1 on the light background; shape, not colour, carries the meaning.",
        reproducibility="Directly expressible as one Compose Path plus two circles.",
        mobile="Bold V fills the safe circle (farthest ink 306 px of the 313 px safe radius); centred under circle, squircle and rounded masks.",
        cross_surface="Works as favicon, store icon, README header and one-colour stamp; amber accent is the only fragile part in print.",
    ),
    "02-keyhole-road": dict(
        recognition="Keyhole silhouette is read instantly; the road slot is a second, slower read.",
        simplicity="One disc, one rounded trapezoid, three slots.",
        scalability="Keyhole silhouette survives 16 px; road dashes close up at 16 px and are faint at 24 px (acceptable, they are secondary).",
        distinctiveness="Keyhole-plus-road is unusual in travel, but keyholes are heavy security/password-manager territory.",
        accessibility="Primary 11.8:1 on the light background; the symbol is strictly one colour (dashes are cut-outs), so no accent-contrast risk.",
        reproducibility="Rounded polygon with even-odd holes; needs corner-rounding helper in Compose.",
        mobile="Tall centred silhouette sits well in the safe circle; reads as a person or pawn for some viewers.",
        cross_surface="Strong in one colour; relies on the privacy metaphor, with no link to the V of the name.",
    ),
    "03-contour": dict(
        recognition="Contour lines suggest terrain, but also fingerprint or eye for some viewers.",
        simplicity="Two organic rings plus a marker; the organic curves need more thought to redraw.",
        scalability="Rings merge at 16 px and the marker disappears; needs a simplified small-size cut.",
        distinctiveness="Topographic motif is distinctive among general travel planners but common in outdoor and mapping apps.",
        accessibility="Contrast passes (6.8:1 primary); thin parallel lines are harder for low-vision users at small sizes.",
        reproducibility="Blob curves are parameterised in code, but not trivial to hand-rebuild in a vector editor.",
        mobile="Large organic shape fills the mask well but has no strong silhouette.",
        cross_surface="Fine lines suffer in embroidery, favicon and low-resolution print.",
    ),
}


def total(scores: dict) -> float:
    return round(sum(scores[k] * w for k, _, w, _ in CRITERIA) / 5, 1)


def table_md() -> str:
    ids = list(SCORES)
    head = "| Criterion | Weight | " + " | ".join(ids) + " |\n"
    head += "| --- | ---: | " + " | ".join(["---:"] * len(ids)) + " |\n"
    rows = ""
    for k, label, w, _ in CRITERIA:
        rows += f"| {label} | {w} | " + " | ".join(str(SCORES[i][k]) for i in ids) + " |\n"
    rows += "| **Weighted total (0-100)** | 100 | " + " | ".join(f"**{total(SCORES[i])}**" for i in ids) + " |\n"
    return head + rows


if __name__ == "__main__":
    print(table_md())
