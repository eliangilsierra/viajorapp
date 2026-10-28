package app.viajor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class OfflineEstimateTest {
    private val world = BoundingBox(-180.0, -90.0, 180.0, 90.0)

    @Test
    fun theWholeWorldIsFourToTheZoomLevelSquaredTiles() {
        assertEquals(1, SlippyTiles.count(world, 0))
        assertEquals(4, SlippyTiles.count(world, 1))
        assertEquals(16, SlippyTiles.count(world, 2))
        assertEquals(1 + 4 + 16, SlippyTiles.count(world, ZoomRange(0, 2)))
    }

    @Test
    fun aBoxAroundTheEquatorAndMeridianCoversFourTilesAtZoomTwo() {
        // At zoom 2 the columns split at -90, 0, 90 and the rows split at about +-66.5 and 0.
        assertEquals(4, SlippyTiles.count(BoundingBox(-50.0, -10.0, 50.0, 10.0), 2))
        assertEquals(1, SlippyTiles.count(BoundingBox(10.0, 10.0, 20.0, 20.0), 2))
    }

    @Test
    fun aBoxCrossingTheAntimeridianCountsBothSides() {
        val pacific = BoundingBox(170.0, -10.0, -170.0, 10.0)
        assertEquals(true, pacific.crossesAntimeridian)
        assertEquals(4, SlippyTiles.count(pacific, 2)) // one column at x=3, one at x=0, two rows
    }

    @Test
    fun polarBoxesAreClampedToTheMercatorLimit() {
        assertEquals(4, SlippyTiles.count(BoundingBox(-180.0, 80.0, 180.0, 90.0), 2))
        assertEquals(4, SlippyTiles.count(BoundingBox(-180.0, -90.0, 180.0, -80.0), 2))
        assertEquals(1, SlippyTiles.count(BoundingBox(10.0, 90.0, 10.0, 90.0), 5))
    }

    @Test
    fun invalidBoxesAndZoomsAreRejected() {
        assertFailsWith<IllegalArgumentException> { BoundingBox(0.0, 10.0, 1.0, 5.0) }
        assertFailsWith<IllegalArgumentException> { BoundingBox(-181.0, 0.0, 0.0, 1.0) }
        assertFailsWith<IllegalArgumentException> { ZoomRange(5, 3) }
        assertFailsWith<IllegalArgumentException> { ZoomRange(0, 23) }
    }

    @Test
    fun aSourceThatForbidsOfflineGivesNoCount() {
        val source = TileSource("osm", "OSM standard", "https://example.org/style", "© OSM", OfflinePolicy.FORBIDDEN)
        val result = OfflineEstimate.forRegion(world, ZoomRange(0, 2), source)
        assertIs<OfflineEstimate.NotAllowed>(result)
    }

    @Test
    fun aSmallRegionFitsAndAnOversizedOneReportsTheOverage() {
        val city = BoundingBox(-9.25, 38.68, -9.08, 38.80)
        val fits = OfflineEstimate.forRegion(city, ZoomRange(10, 12), TileSourcePresets.openFreeMap)
        assertIs<OfflineEstimate.Fits>(fits)

        val big = OfflineEstimate.forRegion(world, ZoomRange(0, 6), TileSourcePresets.openFreeMap)
        val tooLarge = assertIs<OfflineEstimate.TooLarge>(big)
        // 1 + 4 + ... + 4096 = 5461 tiles against a limit of 3000.
        assertEquals(5461, tooLarge.tileCount)
        assertEquals(5461L - 3000, tooLarge.overTiles)
    }

    @Test
    fun theByteLimitAlsoApplies() {
        val city = BoundingBox(-9.25, 38.68, -9.08, 38.80)
        val result = OfflineEstimate.forRegion(city, ZoomRange(10, 12), TileSourcePresets.openFreeMap, bytesPerTile = 50L * 1024 * 1024)
        val tooLarge = assertIs<OfflineEstimate.TooLarge>(result)
        assertEquals(0, tooLarge.overTiles)
        assertEquals(true, tooLarge.overBytes > 0)
    }

    @Test
    fun allowedSourcesWithoutPublishedLimitsAreNeverPromisedToFit() {
        val source = TileSource("x", "Example", "https://example.org/style", "© x", OfflinePolicy.ALLOWED, OfflineLimits(null, null, null))
        val result = OfflineEstimate.forRegion(BoundingBox(0.0, 0.0, 1.0, 1.0), ZoomRange(5, 6), source)
        assertIs<OfflineEstimate.LimitsUnknown>(result)
    }
}
