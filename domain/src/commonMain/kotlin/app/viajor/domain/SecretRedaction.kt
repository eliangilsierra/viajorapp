package app.viajor.domain

/** Raised when text that must be free of secrets (an export, a log line) is not. Never carries the secret. */
class SecretLeakException(message: String) : Exception(message)

/**
 * Removes secrets from text before it is logged or exported (ADR-0005: keys are never logged, exported or
 * backed up). Registered values are matched exactly, also in their percent-encoded form; unregistered ones
 * are caught by their shape (`sk-` keys, bearer tokens, key headers and query parameters). Registered
 * values are held in memory only for the life of this object.
 */
class SecretRedactor {
    private val secrets = mutableListOf<String>()

    /** Registers a secret value. Short values are refused: redacting "abc" would blank ordinary text. */
    fun register(secret: String) {
        require(secret.length >= MIN_SECRET_LENGTH) { "secret is shorter than $MIN_SECRET_LENGTH characters and cannot be redacted safely" }
        require(secret.none { it.isWhitespace() }) { "secret contains whitespace" }
        if (secret !in secrets) {
            secrets += secret
            secrets.sortByDescending { it.length }
        }
    }

    fun redact(text: String): String {
        var out = text
        for (secret in secrets) {
            out = out.replace(secret, MASK)
            val encoded = percentEncode(secret)
            if (encoded != secret) out = out.replace(encoded, MASK)
        }
        out = HEADER.replace(out) { "${it.groupValues[1]}${it.groupValues[2]}$MASK" }
        out = QUERY.replace(out) { "${it.groupValues[1]}$MASK" }
        out = BEARER.replace(out) { "${it.groupValues[1]}$MASK" }
        out = SK_KEY.replace(out, MASK)
        return out
    }

    /** True if [text] contains a registered secret or something shaped like one. */
    fun containsSecret(text: String): Boolean = redact(text) != text

    companion object {
        const val MASK = "[REDACTED]"
        const val MIN_SECRET_LENGTH = 8

        private val SK_KEY = Regex("\\bsk-[A-Za-z0-9_\\-]{16,}")
        private val BEARER = Regex("(?i)(\\bBearer\\s+)(?!\\[REDACTED\\])[A-Za-z0-9._~+/=\\-]{8,}")
        private val HEADER = Regex(
            "(?i)(\\b(?:authorization|proxy-authorization|x-api-key|api-key|apikey|x-goog-api-key)\\b[\"']?)(\\s*[:=]\\s*[\"']?)(?!\\[REDACTED\\])(?:Bearer\\s+|Basic\\s+)?[^\\s\"',;&]{4,}",
        )
        private val QUERY = Regex("(?i)([?&](?:key|api_key|apikey|token|access_token|auth)=)(?!\\[REDACTED\\])[^&\\s\"']+")

        private fun percentEncode(value: String): String = buildString {
            for (b in value.encodeToByteArray()) {
                val c = b.toInt() and 0xFF
                val ch = c.toChar()
                if (ch.isLetterOrDigit() && c < 128 || ch in "-._~") append(ch)
                else append('%').append(c.toString(16).uppercase().padStart(2, '0'))
            }
        }
    }
}

object ExportGuard {
    /**
     * Fails if [text] contains any of [secrets] or a secret-shaped value. The message names only how many
     * problems were found, never the text, so the failure itself cannot leak.
     */
    fun assertClean(text: String, secrets: Collection<String> = emptyList()) {
        val redactor = SecretRedactor()
        secrets.filter { it.length >= SecretRedactor.MIN_SECRET_LENGTH && it.none(Char::isWhitespace) }.forEach(redactor::register)
        if (redactor.containsSecret(text)) {
            throw SecretLeakException("export contains a secret or a secret-shaped value; nothing was written")
        }
    }
}
