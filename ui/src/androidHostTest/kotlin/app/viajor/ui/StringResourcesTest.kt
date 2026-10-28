package app.viajor.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Requirement I18N-01: English and Spanish must stay in step, including printf-style placeholders. */
class StringResourcesTest {
    private val resourcesDir = File("src/commonMain/composeResources")

    private fun parse(xml: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream())
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).associate { i ->
            val n = nodes.item(i)
            n.attributes.getNamedItem("name").nodeValue to n.textContent
        }
    }

    private fun placeholders(text: String): List<String> =
        Regex("%(\\d+\\$)?[sdf]").findAll(text).map { it.value }.sorted().toList()

    /** Problems found comparing [other] against [base]; empty means the two are in step. */
    private fun problems(language: String, base: Map<String, String>, other: Map<String, String>): List<String> {
        val issues = mutableListOf<String>()
        for (key in base.keys - other.keys) issues += "$language is missing '$key'"
        for (key in other.keys - base.keys) issues += "$language has '$key' which English does not"
        for (key in base.keys intersect other.keys) {
            if (other.getValue(key).isBlank()) issues += "$language '$key' is empty"
            if (placeholders(base.getValue(key)) != placeholders(other.getValue(key))) issues += "$language '$key' has different placeholders"
        }
        return issues
    }

    @Test
    fun spanishMatchesEnglishKeysAndPlaceholders() {
        val english = parse(File(resourcesDir, "values/strings.xml").readText())
        val spanish = parse(File(resourcesDir, "values-es/strings.xml").readText())
        assertTrue(english.isNotEmpty(), "English strings were not found at ${resourcesDir.absolutePath}")
        assertEquals(emptyList(), problems("es", english, spanish))
    }

    @Test
    fun theCheckCanFail() {
        val base = mapOf("a" to "Hello %1\$s", "b" to "Two", "c" to "Three")
        val other = mapOf("a" to "Hola", "b" to " ", "d" to "Extra")
        val issues = problems("es", base, other)
        assertTrue(issues.any { "missing 'c'" in it })
        assertTrue(issues.any { "'d'" in it })
        assertTrue(issues.any { "'b' is empty" in it })
        assertTrue(issues.any { "'a' has different placeholders" in it })
        assertEquals(emptyList(), problems("es", base, base))
    }
}
