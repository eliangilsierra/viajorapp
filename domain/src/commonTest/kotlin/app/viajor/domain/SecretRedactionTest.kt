package app.viajor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecretRedactionTest {
    private val key = "sk-ant-api03-ABCDEFGHIJKLMNOP1234567890"

    @Test
    fun aRegisteredKeyNeverSurvivesRedaction() {
        val r = SecretRedactor().apply { register("hunter2-secret") }
        val text = "calling with hunter2-secret now; again hunter2-secret"
        val out = r.redact(text)
        assertFalse("hunter2-secret" in out)
        assertEquals("calling with [REDACTED] now; again [REDACTED]", out)
    }

    @Test
    fun percentEncodedFormsOfARegisteredKeyAreRedactedToo() {
        val r = SecretRedactor().apply { register("p@ss_word!xyz") }
        assertFalse("p%40ss_word%21xyz" in r.redact("GET /x?v=p%40ss_word%21xyz"))
    }

    @Test
    fun headersAreMaskedEvenWhenTheKeyWasNeverRegistered() {
        val r = SecretRedactor()
        assertEquals("Authorization: [REDACTED]", r.redact("Authorization: Bearer abcdefgh12345678"))
        assertEquals("x-api-key: [REDACTED]", r.redact("x-api-key: ZXCVBNM12345"))
        assertEquals("{\"api-key\": \"[REDACTED]\"}", r.redact("{\"api-key\": \"qwertyuiop1234\"}"))
        assertEquals("AUTHORIZATION=[REDACTED]", r.redact("AUTHORIZATION=Basic dXNlcjpwYXNz"))
    }

    @Test
    fun keyShapesAndQueryParametersAreMasked() {
        val r = SecretRedactor()
        val out = r.redact("using $key and https://api.example.com/v1?api_key=SECRETVALUE123&q=paris")
        assertFalse(key in out)
        assertFalse("SECRETVALUE123" in out)
        assertTrue("q=paris" in out)
        assertEquals("token [REDACTED]", r.redact("token sk-abcdefghijklmnopqrst"))
    }

    @Test
    fun ordinaryTextIsUntouched() {
        val r = SecretRedactor().apply { register("hunter2-secret") }
        val text = "Visit the museum at 10:00. Sketch a key idea: token economy of the market? q=paris&page=2"
        assertEquals(text, r.redact(text))
        assertFalse(r.containsSecret(text))
    }

    @Test
    fun shortOrBlankSecretsCannotBeRegisteredSoTheyCannotBlankText() {
        val r = SecretRedactor()
        assertFailsWith<IllegalArgumentException> { r.register("") }
        assertFailsWith<IllegalArgumentException> { r.register("abc") }
        assertFailsWith<IllegalArgumentException> { r.register("has a space inside") }
        assertEquals("a b c", r.redact("a b c"))
    }

    @Test
    fun longerSecretsAreRedactedBeforeTheirPrefixes() {
        val r = SecretRedactor().apply {
            register("abcdefgh")
            register("abcdefgh-extended")
        }
        assertEquals("[REDACTED] and [REDACTED]", r.redact("abcdefgh-extended and abcdefgh"))
    }

    @Test
    fun exportGuardFailsWithAMessageThatDoesNotRepeatTheSecret() {
        val secret = "my-very-private-token"
        val e = assertFailsWith<SecretLeakException> { ExportGuard.assertClean("{\"note\": \"$secret\"}", listOf(secret)) }
        assertFalse(secret in e.message.orEmpty())
        assertFailsWith<SecretLeakException> { ExportGuard.assertClean("key sk-abcdefghijklmnopqrstuvwx") }
        ExportGuard.assertClean("{\"trip\": \"Lisbon\"}", listOf(secret)) // clean export passes
    }
}
