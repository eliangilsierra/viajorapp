package app.viajor.domain

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The file format of a full export or backup (requirements EXP-01, IMP-02). It mirrors the stored records,
 * including provenance and sync fields, and never contains API keys or other secrets (ADR-0002, ADR-0005).
 */
@Serializable
data class BackupDocument(
    val format: String = BackupCodec.FORMAT,
    val formatVersion: Int = BackupCodec.CURRENT_VERSION,
    val exportedAtMillis: Long? = null,
    val trips: List<BackupTrip> = emptyList(),
)

@Serializable
data class BackupTrip(
    val id: String,
    val title: String,
    val description: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val updatedAtHlc: String,
    val updatedBy: String,
    val days: List<BackupDay> = emptyList(),
    val places: List<BackupPlace> = emptyList(),
    val activities: List<BackupActivity> = emptyList(),
)

@Serializable
data class BackupDay(
    val id: String,
    val dayIndex: Int,
    val date: String? = null,
    val title: String? = null,
    val deletedAt: Long? = null,
    val updatedAtHlc: String,
    val updatedBy: String,
)

@Serializable
data class BackupPlace(
    val id: String,
    val name: String,
    val address: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val notes: String? = null,
    val source: String = "user",
    val deletedAt: Long? = null,
    val updatedAtHlc: String,
    val updatedBy: String,
)

@Serializable
data class BackupActivity(
    val id: String,
    val dayId: String? = null,
    val placeId: String? = null,
    val title: String,
    val notes: String? = null,
    val kind: String = "activity",
    val startLocal: String? = null,
    val endLocal: String? = null,
    val zoneId: String? = null,
    val sortKey: String,
    val completedAt: Long? = null,
    val source: String = "user",
    val deletedAt: Long? = null,
    val updatedAtHlc: String,
    val updatedBy: String,
)

/** Why a backup could not be written or read. Messages never repeat the file's content. */
class BackupException(message: String) : Exception(message)

data class BackupLimits(val maxChars: Int = 20_000_000, val maxTrips: Int = 1_000, val maxRecords: Int = 500_000)

object BackupCodec {
    const val FORMAT = "viajorapp-backup"
    const val CURRENT_VERSION = 1

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        // Strict on purpose: a file with unknown fields is from a different version or is not ours.
        ignoreUnknownKeys = false
    }

    private val localDateTime = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$")
    private val localDate = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val sources = setOf("user", "import", "ai", "geocoder")
    private val kinds = setOf("activity", "transport", "lodging", "meal", "note")

    /** Encodes [document] after validating it and checking that none of [secrets] or any key-shaped text is inside. */
    fun encode(document: BackupDocument, secrets: Collection<String> = emptyList()): String {
        validate(document, BackupLimits())
        val text = json.encodeToString(BackupDocument.serializer(), document)
        try {
            ExportGuard.assertClean(text, secrets)
        } catch (e: SecretLeakException) {
            throw BackupException("the backup contains a secret or key-shaped text; nothing was written")
        }
        return text
    }

    fun decode(text: String, limits: BackupLimits = BackupLimits()): BackupDocument {
        if (text.length > limits.maxChars) throw BackupException("the file is larger than ${limits.maxChars} characters")
        val document = try {
            json.decodeFromString(BackupDocument.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupException("the file is not a valid Viajorapp backup")
        } catch (e: IllegalArgumentException) {
            throw BackupException("the file is not a valid Viajorapp backup")
        }
        validate(document, limits)
        return document
    }

    private fun validate(doc: BackupDocument, limits: BackupLimits) {
        fun fail(message: String): Nothing = throw BackupException(message)
        if (doc.format != FORMAT) fail("this is not a Viajorapp backup")
        if (doc.formatVersion > CURRENT_VERSION) fail("this backup was made by a newer version (format ${doc.formatVersion}); update the app to read it")
        if (doc.formatVersion < 1) fail("unsupported backup format version ${doc.formatVersion}")
        if (doc.trips.size > limits.maxTrips) fail("more than ${limits.maxTrips} trips")
        val records = doc.trips.sumOf { 1 + it.days.size + it.places.size + it.activities.size }
        if (records > limits.maxRecords) fail("more than ${limits.maxRecords} records")

        val allIds = HashSet<String>()
        fun id(value: String, what: String) {
            if (value.isBlank()) fail("a $what has an empty id")
            if (!allIds.add(value)) fail("the id of a $what is used twice")
        }
        for (trip in doc.trips) {
            id(trip.id, "trip")
            if (trip.title.isBlank()) fail("a trip has an empty title")
            val s = trip.startDate
            val e = trip.endDate
            if (s != null && !localDate.matches(s) || e != null && !localDate.matches(e)) fail("a trip date is not YYYY-MM-DD")
            if (s != null && e != null && e < s) fail("a trip ends before it starts")

            val dayIds = HashSet<String>()
            val dayIndexes = HashSet<Int>()
            for (day in trip.days) {
                id(day.id, "day")
                dayIds += day.id
                if (day.dayIndex < 1 || !dayIndexes.add(day.dayIndex)) fail("a trip has an invalid or repeated day number")
                if (day.date != null && !localDate.matches(day.date)) fail("a day date is not YYYY-MM-DD")
            }
            val placeIds = HashSet<String>()
            for (place in trip.places) {
                id(place.id, "place")
                placeIds += place.id
                if (place.name.isBlank()) fail("a place has an empty name")
                if ((place.lat == null) != (place.lon == null)) fail("a place has only one of latitude and longitude")
                val lat = place.lat
                val lon = place.lon
                if (lat != null && lon != null && (lat !in -90.0..90.0 || lon !in -180.0..180.0)) fail("a place has coordinates out of range")
                if (place.source !in sources) fail("a place has an unknown source")
            }
            for (a in trip.activities) {
                id(a.id, "activity")
                if (a.title.isBlank()) fail("an activity has an empty title")
                if (a.dayId != null && a.dayId !in dayIds) fail("an activity points to a day that is not in its trip")
                if (a.placeId != null && a.placeId !in placeIds) fail("an activity points to a place that is not in its trip")
                if (a.kind !in kinds) fail("an activity has an unknown kind")
                if (a.source !in sources) fail("an activity has an unknown source")
                for (t in listOfNotNull(a.startLocal, a.endLocal)) if (!localDateTime.matches(t)) fail("an activity time is not YYYY-MM-DDTHH:MM")
                if (a.startLocal != null && a.endLocal != null && a.endLocal < a.startLocal) fail("an activity ends before it starts")
                if ((a.startLocal != null || a.endLocal != null) && a.zoneId.isNullOrBlank()) fail("an activity has a time but no time zone")
                if (a.sortKey.isBlank()) fail("an activity has an empty sort key")
            }
        }
    }
}
