package app.viajor.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/**
 * A geographic box in degrees. When [west] is greater than [east] the box crosses the antimeridian
 * (for example 170 to -170 is a 20 degree wide box over the Pacific).
 */
data class BoundingBox(val west: Double, val south: Double, val east: Double, val north: Double) {
    init {
        require(west in -180.0..180.0 && east in -180.0..180.0) { "longitude must be between -180 and 180" }
        require(south in -90.0..90.0 && north in -90.0..90.0) { "latitude must be between -90 and 90" }
        require(south <= north) { "south must not be above north" }
    }

    val crossesAntimeridian: Boolean get() = west > east
}

data class ZoomRange(val min: Int, val max: Int) {
    init {
        require(min in 0..MAX_ZOOM && max in 0..MAX_ZOOM && min <= max) { "zoom must satisfy 0 <= min <= max <= $MAX_ZOOM" }
    }

    companion object {
        const val MAX_ZOOM = 22
    }
}

/** Web Mercator tile ("slippy map") arithmetic. */
object SlippyTiles {
    /** The Web Mercator projection is undefined beyond this latitude. */
    const val MAX_LATITUDE = 85.0511287798

    fun tilesPerAxis(zoom: Int): Long = 1L shl zoom

    fun tileX(lon: Double, zoom: Int): Long {
        val n = tilesPerAxis(zoom)
        return floor((lon + 180.0) / 360.0 * n).toLong().coerceIn(0, n - 1)
    }

    fun tileY(lat: Double, zoom: Int): Long {
        val n = tilesPerAxis(zoom)
        val clamped = lat.coerceIn(-MAX_LATITUDE, MAX_LATITUDE)
        val rad = clamped * PI / 180.0
        val y = (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * n
        return floor(y).toLong().coerceIn(0, n - 1)
    }

    /** Number of tiles covering [box] at one [zoom] level. */
    fun count(box: BoundingBox, zoom: Int): Long {
        val rows = tileY(box.south, zoom) - tileY(box.north, zoom) + 1
        val columns = if (box.crossesAntimeridian) {
            (tileX(180.0, zoom) - tileX(box.west, zoom) + 1) + (tileX(box.east, zoom) - tileX(-180.0, zoom) + 1)
        } else {
            tileX(box.east, zoom) - tileX(box.west, zoom) + 1
        }
        return rows * columns
    }

    fun count(box: BoundingBox, zooms: ZoomRange): Long = (zooms.min..zooms.max).sumOf { count(box, it) }
}

/** What downloading a region for offline use would cost, before anything is downloaded. Always an estimate. */
sealed interface OfflineEstimate {
    /** The source's terms do not allow offline downloads; no count is offered. */
    data class NotAllowed(val reason: String) : OfflineEstimate

    /** Offline use is allowed but the provider publishes no limit, so fitting cannot be promised. */
    data class LimitsUnknown(val tileCount: Long, val estimatedBytes: Long) : OfflineEstimate

    data class Fits(val tileCount: Long, val estimatedBytes: Long) : OfflineEstimate

    data class TooLarge(val tileCount: Long, val estimatedBytes: Long, val overTiles: Long, val overBytes: Long) : OfflineEstimate

    companion object {
        /** An assumption, not a measurement: vector tiles vary widely with area and zoom. Callers may pass their own. */
        const val ASSUMED_BYTES_PER_TILE = 25_000L

        fun forRegion(
            box: BoundingBox,
            zooms: ZoomRange,
            source: TileSource,
            bytesPerTile: Long = ASSUMED_BYTES_PER_TILE,
        ): OfflineEstimate {
            require(bytesPerTile > 0) { "bytes per tile must be positive" }
            if (source.offlinePolicy != OfflinePolicy.ALLOWED) {
                return NotAllowed("${source.displayName} does not allow offline downloads")
            }
            val tiles = SlippyTiles.count(box, zooms)
            val bytes = tiles * bytesPerTile
            val limits = source.offlineLimits
            val maxTiles = limits?.maxTiles?.toLong()
            val maxBytes = limits?.maxBytes
            if (maxTiles == null && maxBytes == null) return LimitsUnknown(tiles, bytes)
            val overTiles = if (maxTiles != null) (tiles - maxTiles).coerceAtLeast(0) else 0
            val overBytes = if (maxBytes != null) (bytes - maxBytes).coerceAtLeast(0) else 0
            return if (overTiles > 0 || overBytes > 0) TooLarge(tiles, bytes, overTiles, overBytes) else Fits(tiles, bytes)
        }
    }
}
