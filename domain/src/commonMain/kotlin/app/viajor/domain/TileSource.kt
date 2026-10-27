package app.viajor.domain

enum class OfflinePolicy { ALLOWED, FORBIDDEN }

/** Offline limits a tile provider imposes per download (ADR-0003); null means "not published". */
data class OfflineLimits(val maxTiles: Int?, val maxBytes: Long?, val maxParallelRequests: Int?)

/** Map style/tile source as data so providers can change without code changes (ADR-0003). */
data class TileSource(
    val id: String,
    val displayName: String,
    val styleUrl: String,
    val attribution: String,
    val offlinePolicy: OfflinePolicy,
    val offlineLimits: OfflineLimits? = null,
) {
    init {
        require(styleUrl.startsWith("https://")) { "Tile source style URL must use https" }
        require(offlinePolicy == OfflinePolicy.ALLOWED || offlineLimits == null) {
            "Limits only apply to sources that allow offline use"
        }
    }
}

object TileSourcePresets {
    /** Terms checked 2026-10-27: offline downloads allowed up to 3,000 tiles / 50 MB, 4 parallel requests. */
    val openFreeMap = TileSource(
        id = "openfreemap-liberty",
        displayName = "OpenFreeMap",
        styleUrl = "https://tiles.openfreemap.org/styles/liberty",
        attribution = "OpenFreeMap © OpenMapTiles Data from OpenStreetMap",
        offlinePolicy = OfflinePolicy.ALLOWED,
        offlineLimits = OfflineLimits(maxTiles = 3_000, maxBytes = 50L * 1024 * 1024, maxParallelRequests = 4),
    )
}
