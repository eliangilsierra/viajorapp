package app.viajor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiProposalJsonTest {
    private val valid = """
        {"summary": "Add a viewpoint and rename lunch",
         "operations": [
           {"id": "o1", "type": "add", "item": {"title": "Miradouro", "lat": 38.71, "lon": -9.13, "start": "2027-03-01T17:00", "zone": "Europe/Lisbon"}},
           {"id": "o2", "type": "update", "target": "i2", "changes": {"title": "Seafood lunch", "notes": null, "lat": 38.7}},
           {"id": "o3", "type": "remove", "target": "i1"}
         ]}
    """.trimIndent()

    private val itinerary = Itinerary(
        listOf(
            ItineraryItem("i1", "Museum", lat = 38.69, lon = -9.21, startLocal = "2027-03-01T10:00", endLocal = "2027-03-01T12:00", zoneId = "Europe/Lisbon"),
            ItineraryItem("i2", "Lunch", notes = "near the river"),
        ),
    )

    @Test
    fun aValidAnswerBecomesAProposalThatThenPassesTheValidator() {
        val proposal = AiProposalJson.parse(valid)
        assertEquals("Add a viewpoint and rename lunch", proposal.summary)
        assertEquals(3, proposal.ops.size)
        val add = proposal.ops[0] as ProposalOp.Add
        assertEquals(NewItem("Miradouro", null, 38.71, -9.13, "2027-03-01T17:00", null, "Europe/Lisbon"), add.item)
        val update = proposal.ops[1] as ProposalOp.Update
        assertEquals(mapOf("title" to "Seafood lunch", "notes" to null, "lat" to "38.7"), update.changes)
        assertEquals(ProposalOp.Remove("o3", "i1"), proposal.ops[2])
        // The shape is accepted here, but update lat without lon is still caught by the validator.
        assertTrue(ProposalValidator.validate(proposal, itinerary).none { it.opId == "o1" })
    }

    @Test
    fun fencedBlocksAndSurroundingProseAreHandled() {
        val fenced = "Sure! Here is my suggestion:\n```json\n$valid\n```\nHope that helps."
        assertEquals(3, AiProposalJson.parse(fenced).ops.size)
        val prose = "I think this works. $valid Let me know."
        assertEquals(3, AiProposalJson.parse(prose).ops.size)
        val plainFence = "```\n$valid\n```"
        assertEquals(3, AiProposalJson.parse(plainFence).ops.size)
    }

    @Test
    fun malformedOrUnusableAnswersAreRejectedWithoutEchoingThem() {
        val secretish = "SENSITIVE-TEXT-123"
        val answers = listOf(
            "", "no json here $secretish", "{ not json $secretish }", "[1,2,3]", "{\"summary\": 5}",
            "{\"operations\": [{\"id\": \"o\", \"type\": \"teleport\"}]}",
            "{\"operations\": [{\"id\": \"o\", \"type\": \"add\"}]}",
            "{\"operations\": [{\"id\": \"o\", \"type\": \"update\", \"target\": \"i1\"}]}",
            "{\"operations\": [{\"id\": \"o\", \"type\": \"remove\"}]}",
            "{\"operations\": [{\"id\": \"o\", \"type\": \"update\", \"target\": \"i1\", \"changes\": {\"title\": [1]}}]}",
            "{\"operations\": [], \"surprise\": \"$secretish\"}",
        )
        for (answer in answers) {
            val e = assertFailsWith<AiParseException>(answer) { AiProposalJson.parse(answer) }
            assertFalse(secretish in e.message.orEmpty(), "message echoed content for: $answer")
        }
    }

    @Test
    fun oversizeAnswersAreRejected() {
        assertFailsWith<AiParseException> { AiProposalJson.parse("{" + "x".repeat(AiProposalJson.MAX_CHARS) + "}") }
    }

    @Test
    fun anEmptyOperationListParsesButTheValidatorRejectsIt() {
        val proposal = AiProposalJson.parse("{\"summary\": \"nothing to change\", \"operations\": []}")
        assertTrue(ProposalValidator.validate(proposal, itinerary).any { "no operations" in it.message })
    }

    @Test
    fun theSystemPromptStatesTheContractAndNoKeys() {
        assertTrue("single JSON object" in AiPrompt.SYSTEM)
        assertTrue("existing item id" in AiPrompt.SYSTEM)
        assertFalse(SecretRedactor().containsSecret(AiPrompt.SYSTEM))
    }

    @Test
    fun thePreviewListsExactlyWhatIsSent() {
        val preview = AiPrompt.build(itinerary, "Add something nice for the evening")
        assertEquals(listOf("Your request text", "Item titles", "Coordinates", "Times and time zones", "Item notes"), preview.dataSent)
        assertTrue("id=i1; title=Museum; lat=38.69; lon=-9.21; start=2027-03-01T10:00; end=2027-03-01T12:00; zone=Europe/Lisbon" in preview.user)
        assertTrue("notes=near the river" in preview.user)
        assertTrue("Request: Add something nice for the evening" in preview.user)
    }

    @Test
    fun anEmptyItineraryOnlySendsTheRequestText() {
        val preview = AiPrompt.build(Itinerary(emptyList()), "Plan three days in Porto")
        assertEquals(listOf("Your request text"), preview.dataSent)
        assertTrue("(none)" in preview.user)
    }

    @Test
    fun keyShapedTextInTheRequestIsMaskedAndNeverSent() {
        val key = "sk-" + "ab".repeat(15)
        val preview = AiPrompt.build(itinerary, "use my key $key please")
        assertFalse(key in preview.user)
        assertTrue("[REDACTED]" in preview.user)
        assertFalse(SecretRedactor().containsSecret(preview.user.replace("[REDACTED]", "x")))
    }

    @Test
    fun theRequestLengthIsCapped() {
        val preview = AiPrompt.build(Itinerary(emptyList()), "a".repeat(10_000))
        assertTrue(preview.user.length < 2_200)
    }
}
