"""Concept definitions: palettes, type direction, wordmark style."""
from brand_lib import WordmarkStyle

CONCEPTS = [
    dict(
        id="01-waypoint",
        key="waypoint",
        name="Waypoint",
        idea="A V drawn as a single route: it starts at a hollow origin node, bends at the lowest point, and ends at a solid destination. The line is yours; nothing leaves the device.",
        style=WordmarkStyle(weight=17, gap=15),
        ui_radius=18,
        light=dict(
            primary="#0B5563", on_primary="#FFFFFF", accent="#B85C00", on_accent="#FFFFFF",
            bg="#F7F5EF", surface="#FFFFFF", text="#10272D", text_muted="#44585F", outline="#728A91",
        ),
        dark=dict(
            primary="#6CCBDB", on_primary="#06242B", accent="#FFC845", on_accent="#2A1B00",
            bg="#0B1A1E", surface="#12282E", text="#E8F1F2", text_muted="#A9BDC2", outline="#5F8791",
        ),
        icon=dict(bg="#0B5563", body="#F7F5EF", accent="#FFC845"),
        scale=1.18,
        fonts=dict(
            display=("Manrope", "ExtraBold / SemiBold", "SIL OFL 1.1"),
            body=("Inter", "Regular / Medium / SemiBold", "SIL OFL 1.1"),
            mono=("Inter", "tabular-nums feature for dates, prices, distances", "SIL OFL 1.1"),
        ),
        ui_fonts="'Manrope','Segoe UI',Arial,sans-serif",
    ),
    dict(
        id="02-keyhole-road",
        key="keyhole",
        name="Keyhole Road",
        idea="A keyhole whose slot is a road running to the horizon. Privacy and the open road in one silhouette: the data is locked to you, the journey is not.",
        style=WordmarkStyle(weight=22, gap=11),
        ui_radius=8,
        light=dict(
            primary="#1E2A78", on_primary="#FFFFFF", accent="#D2442A", on_accent="#FFFFFF",
            bg="#F6F6FB", surface="#FFFFFF", text="#15183A", text_muted="#4A4E73", outline="#7B80A6",
        ),
        dark=dict(
            primary="#AEB8FF", on_primary="#10143A", accent="#FF8A6B", on_accent="#3A0F05",
            bg="#0E1128", surface="#171B3D", text="#ECEEFF", text_muted="#B3B8DD", outline="#6F77C0",
        ),
        icon=dict(bg="#1E2A78", body="#F6F6FB", accent="#FF8A6B"),
        scale=1.12,
        fonts=dict(
            display=("Space Grotesk", "Bold / Medium", "SIL OFL 1.1"),
            body=("IBM Plex Sans", "Regular / Medium / SemiBold", "SIL OFL 1.1"),
            mono=("IBM Plex Mono", "coordinates, IDs, export formats", "SIL OFL 1.1"),
        ),
        ui_fonts="'Space Grotesk','Segoe UI',Arial,sans-serif",
    ),
    dict(
        id="03-contour",
        key="contour",
        name="Contour",
        idea="Topographic contour lines around a summit marker. Terrain you can read without a signal: the map is already in your pocket, and the line you walk is the line you chose.",
        style=WordmarkStyle(weight=13, gap=20),
        ui_radius=26,
        light=dict(
            primary="#1F5E45", on_primary="#FFFFFF", accent="#0F6FA8", on_accent="#FFFFFF",
            bg="#F4F1E8", surface="#FFFFFF", text="#14261E", text_muted="#475A50", outline="#76897C",
        ),
        dark=dict(
            primary="#7FD1A6", on_primary="#0A2418", accent="#63BDF2", on_accent="#04202F",
            bg="#0C1A14", surface="#142A20", text="#E7F1EA", text_muted="#A8BDB0", outline="#5E9578",
        ),
        icon=dict(bg="#1F5E45", body="#F4F1E8", accent="#8BD0F7"),
        scale=1.10,
        fonts=dict(
            display=("Fraunces", "SemiBold (soft optical size)", "SIL OFL 1.1"),
            body=("Source Sans 3", "Regular / SemiBold", "SIL OFL 1.1"),
            mono=("Source Sans 3", "tabular figures for elevation, distance, time", "SIL OFL 1.1"),
        ),
        ui_fonts="Georgia,'Segoe UI',serif",
    ),
]
