package app.viajor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AiProposalTest {
    private val museum = ItineraryItem("i1", "Museum", lat = 38.69, lon = -9.21, startLocal = "2027-03-01T10:00", endLocal = "2027-03-01T12:00", zoneId = "Europe/Lisbon")
    private val lunch = ItineraryItem("i2", "Lunch", provenance = Provenance.USER)
    private val itinerary = Itinerary(listOf(museum, lunch))

    private fun issues(vararg ops: ProposalOp, summary: String = "Ideas") =
        ProposalValidator.validate(AiProposal(summary, ops.toList()), itinerary)

    @Test
    fun aValidProposalPassesAndProducesOneDiffEntryPerOperation() {
        val proposal = AiProposal(
            "Add a viewpoint, rename lunch, drop the museum",
            listOf(
                ProposalOp.Add("a", NewItem("Viewpoint", lat = 38.71, lon = -9.13)),
                ProposalOp.Update("u", "i2", mapOf("title" to "Seafood lunch")),
                ProposalOp.Remove("r", "i1"),
            ),
        )
        assertEquals(emptyList(), ProposalValidator.validate(proposal, itinerary))
        val diff = ProposalDiff.of(proposal, itinerary)
        assertEquals(listOf(DiffEntry.Kind.ADD, DiffEntry.Kind.UPDATE, DiffEntry.Kind.REMOVE), diff.map { it.kind })
        assertEquals(mapOf("title" to "Lunch"), diff[1].before)
        assertEquals(mapOf("title" to "Seafood lunch"), diff[1].after)
        assertEquals(mapOf("title" to "Museum"), diff[2].before)
    }

    @Test
    fun missingTargetsAreRejected() {
        assertTrue(issues(ProposalOp.Update("u", "nope", mapOf("title" to "x"))).any { "does not exist" in it.message })
        assertTrue(issues(ProposalOp.Remove("r", "nope")).any { "does not exist" in it.message })
    }

    @Test
    fun badCoordinatesTimesAndSizesAreRejectedWithSpecificMessages() {
        assertTrue(issues(ProposalOp.Add("a", NewItem("X", lat = 91.0, lon = 0.0))).any { "latitude" in it.message })
        assertTrue(issues(ProposalOp.Add("a", NewItem("X", lat = 10.0))).any { "together" in it.message })
        assertTrue(issues(ProposalOp.Add("a", NewItem("X", startLocal = "tomorrow", zoneId = "Europe/Lisbon"))).any { "start time" in it.message })
        assertTrue(issues(ProposalOp.Add("a", NewItem("X", startLocal = "2027-03-01T10:00"))).any { "time zone" in it.message })
        assertTrue(issues(ProposalOp.Add("a", NewItem("X", startLocal = "2027-03-01T10:00", endLocal = "2027-03-01T09:00", zoneId = "Europe/Lisbon"))).any { "before start" in it.message })
        assertTrue(issues(ProposalOp.Add("a", NewItem("x".repeat(201)))).any { "title is longer" in it.message })
        assertTrue(issues(ProposalOp.Add("a", NewItem("X", notes = "n".repeat(2_001)))).any { "notes" in it.message })
        assertTrue(issues(ProposalOp.Add("a", NewItem(" "))).any { "title is empty" in it.message })
        assertTrue(issues(ProposalOp.Update("u", "i1", mapOf("lat" to "not-a-number"))).any { "latitude" in it.message })
    }

    @Test
    fun unknownFieldsDuplicateIdsAndConflictingTargetsAreRejected() {
        assertTrue(issues(ProposalOp.Update("u", "i1", mapOf("provenance" to "USER"))).any { "not allowed" in it.message })
        assertTrue(issues(ProposalOp.Update("u", "i1", emptyMap())).any { "changes nothing" in it.message })
        assertTrue(
            issues(ProposalOp.Remove("same", "i1"), ProposalOp.Remove("same", "i2")).any { "repeated" in it.message },
        )
        assertTrue(
            issues(ProposalOp.Remove("r1", "i1"), ProposalOp.Update("u1", "i1", mapOf("title" to "x"))).any { "more than one operation" in it.message },
        )
        assertTrue(issues().any { "no operations" in it.message })
    }

    @Test
    fun tooManyOperationsAndLongSummariesAreRejected() {
        val many = List(51) { ProposalOp.Add("a$it", NewItem("Item $it")) }
        assertTrue(ProposalValidator.validate(AiProposal("s", many), itinerary).any { "more than 50" in it.message })
        assertTrue(issues(ProposalOp.Add("a", NewItem("X")), summary = "s".repeat(501)).any { "summary" in it.message })
    }

    @Test
    fun onlyAcceptedOperationsApplyAndTheirItemsAreMarkedAi() {
        val proposal = AiProposal(
            "Mixed",
            listOf(
                ProposalOp.Add("a", NewItem("Viewpoint")),
                ProposalOp.Update("u", "i2", mapOf("title" to "Seafood lunch", "notes" to "book ahead")),
                ProposalOp.Remove("r", "i1"),
            ),
        )
        val result = ProposalApply.apply(itinerary, proposal, accepted = setOf("a", "u")) { "new-$it" }
        assertEquals(listOf("i1", "i2", "new-a"), result.items.map { it.id })
        val updated = result.items.first { it.id == "i2" }
        assertEquals("Seafood lunch", updated.title)
        assertEquals("book ahead", updated.notes)
        assertEquals(Provenance.AI, updated.provenance)
        assertEquals(Provenance.AI, result.items.last().provenance)
        assertEquals(Provenance.USER, result.items.first { it.id == "i1" }.provenance)
    }

    @Test
    fun rejectingEverythingLeavesTheItineraryExactlyAsItWas() {
        val proposal = AiProposal("x", listOf(ProposalOp.Remove("r", "i1"), ProposalOp.Add("a", NewItem("New"))))
        assertEquals(itinerary, ProposalApply.apply(itinerary, proposal, accepted = emptySet()) { it })
    }

    @Test
    fun anInvalidProposalOrUnknownAcceptanceAppliesNothing() {
        val bad = AiProposal("x", listOf(ProposalOp.Add("a", NewItem("Fine")), ProposalOp.Remove("r", "ghost")))
        assertFailsWith<IllegalArgumentException> { ProposalApply.apply(itinerary, bad, accepted = setOf("a")) { it } }
        val good = AiProposal("x", listOf(ProposalOp.Add("a", NewItem("Fine"))))
        assertFailsWith<IllegalArgumentException> { ProposalApply.apply(itinerary, good, accepted = setOf("zzz")) { it } }
    }

    @Test
    fun updatingTimesRevalidatesTheWholeItem() {
        // Moving the end before the existing start must be caught even though only one field changes.
        assertTrue(issues(ProposalOp.Update("u", "i1", mapOf("endLocal" to "2027-03-01T08:00"))).any { "before start" in it.message })
        assertEquals(emptyList(), issues(ProposalOp.Update("u", "i1", mapOf("endLocal" to "2027-03-01T13:00"))))
    }
}
