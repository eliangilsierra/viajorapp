package app.viajor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupTest {
    private fun doc() = BackupDocument(
        exportedAtMillis = 1_800_000_000_000,
        trips = listOf(
            BackupTrip(
                id = "t1", title = "Lisbon", description = "Spring trip", startDate = "2027-03-01", endDate = "2027-03-03",
                updatedAtHlc = "000000000000001-00000-a", updatedBy = "a",
                days = listOf(
                    BackupDay("d1", 1, "2027-03-01", null, null, "h", "a"),
                    BackupDay("d2", 2, "2027-03-02", "Sintra", null, "h", "a"),
                ),
                places = listOf(BackupPlace("p1", "Alfama", "Rua X", 38.71, -9.13, "old town", "geocoder", null, "h", "a")),
                activities = listOf(
                    BackupActivity("a1", "d1", "p1", "Walk", "bring water", "activity", "2027-03-01T10:00", "2027-03-01T12:00", "Europe/Lisbon", "V", 5L, "user", null, "h", "a"),
                    BackupActivity("a2", null, null, "Idea: fado night", null, "activity", null, null, null, "W", null, "ai", null, "h", "a"),
                ),
            ),
        ),
    )

    private fun fail(message: String, text: String): BackupException {
        val e = assertFailsWith<BackupException>(message) { BackupCodec.decode(text) }
        return e
    }

    @Test
    fun aPopulatedDocumentRoundTripsExactly() {
        val original = doc()
        val text = BackupCodec.encode(original)
        assertEquals(original, BackupCodec.decode(text))
        assertTrue("\"formatVersion\": 1" in text)
        assertTrue("\"source\": \"ai\"" in text)
    }

    @Test
    fun anEmptyBackupIsValid() {
        assertEquals(BackupDocument(), BackupCodec.decode(BackupCodec.encode(BackupDocument())))
    }

    @Test
    fun aNewerFormatVersionIsRejectedWithAClearMessage() {
        val text = BackupCodec.encode(doc()).replace("\"formatVersion\": 1", "\"formatVersion\": 2")
        val e = fail("newer", text)
        assertTrue("newer version" in e.message.orEmpty())
        fail("zero", BackupCodec.encode(doc()).replace("\"formatVersion\": 1", "\"formatVersion\": 0"))
    }

    @Test
    fun aFileThatIsNotABackupIsRejected() {
        fail("other format", BackupCodec.encode(doc()).replace("viajorapp-backup", "something-else"))
        fail("not json", "this is not json")
        fail("empty", "")
        fail("wrong shape", "[1, 2, 3]")
    }

    @Test
    fun unknownFieldsAreRejectedNotIgnored() {
        val text = BackupCodec.encode(doc()).replace("\"format\":", "\"surprise\": true,\n  \"format\":")
        fail("unknown key", text)
    }

    @Test
    fun anExportContainingASecretIsNeverWritten() {
        val secret = "my-very-private-token"
        val withSecret = doc().let { d -> d.copy(trips = listOf(d.trips.single().copy(description = "note $secret"))) }
        val e = assertFailsWith<BackupException> { BackupCodec.encode(withSecret, listOf(secret)) }
        assertFalse(secret in e.message.orEmpty())
        val shaped = doc().let { d -> d.copy(trips = listOf(d.trips.single().copy(description = "key " + "sk-" + "x".repeat(30)))) }
        assertFailsWith<BackupException> { BackupCodec.encode(shaped) }
    }

    @Test
    fun brokenReferencesDuplicatesAndBadValuesAreRejectedOnDecode() {
        val good = doc()
        val base = BackupCodec.encode(good)
        fail("dangling day", base.replace("\"dayId\": \"d1\"", "\"dayId\": \"nope\""))
        fail("dangling place", base.replace("\"placeId\": \"p1\"", "\"placeId\": \"nope\""))
        fail("duplicate id", base.replace("\"id\": \"d2\"", "\"id\": \"d1\""))
        fail("bad date", base.replace("\"startDate\": \"2027-03-01\"", "\"startDate\": \"1 March\""))
        fail("end before start", base.replace("\"endDate\": \"2027-03-03\"", "\"endDate\": \"2027-02-01\""))
        fail("bad lat", base.replace("\"lat\": 38.71", "\"lat\": 91.0"))
        fail("bad kind", base.replace("\"kind\": \"activity\"", "\"kind\": \"spaceship\""))
        fail("time without zone", base.replace("\"zoneId\": \"Europe/Lisbon\"", "\"zoneId\": null"))
        fail("empty title", base.replace("\"title\": \"Lisbon\"", "\"title\": \" \""))
    }

    @Test
    fun oversizeInputAndTooManyRecordsAreRejected() {
        val text = BackupCodec.encode(doc())
        assertFailsWith<BackupException> { BackupCodec.decode(text, BackupLimits(maxChars = 100)) }
        assertFailsWith<BackupException> { BackupCodec.decode(text, BackupLimits(maxTrips = 0)) }
        assertFailsWith<BackupException> { BackupCodec.decode(text, BackupLimits(maxRecords = 3)) }
    }

    @Test
    fun errorMessagesDoNotRepeatFileContent() {
        val text = BackupCodec.encode(doc()).replace("\"title\": \"Lisbon\"", "\"title\": \"\"")
        val e = fail("empty title", text)
        assertFalse("Spring trip" in e.message.orEmpty())
    }
}
