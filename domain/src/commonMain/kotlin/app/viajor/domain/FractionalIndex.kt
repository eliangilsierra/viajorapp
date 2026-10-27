package app.viajor.domain

/**
 * Fractional ordering keys so a reorder rewrites exactly one row and concurrent reorders of different
 * items merge cleanly (see ADR-0002). Keys are non-empty strings over [DIGITS], never end in the zero
 * digit, and compare with ordinary string ordering. Port of the midpoint algorithm used by
 * "fractional indexing"; the integer prefix is omitted because every day has its own key space.
 */
object FractionalIndex {
    const val DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private const val ZERO = '0'

    /** Returns a key strictly between [before] and [after]; null means "no bound" on that side. */
    fun between(before: String?, after: String?): String {
        require(before == null || isValid(before)) { "Invalid key: $before" }
        require(after == null || isValid(after)) { "Invalid key: $after" }
        if (before != null && after != null) {
            require(before < after) { "before ($before) must sort before after ($after)" }
        }
        return midpoint(before.orEmpty(), after)
    }

    fun isValid(key: String): Boolean =
        key.isNotEmpty() && key.last() != ZERO && key.all { it in DIGITS }

    private fun midpoint(a: String, b: String?): String {
        if (b != null) {
            var n = 0
            while ((if (n < a.length) a[n] else ZERO) == b[n]) n++
            if (n > 0) return b.substring(0, n) + midpoint(a.substring(n.coerceAtMost(a.length)), b.substring(n))
        }
        val digitA = if (a.isNotEmpty()) DIGITS.indexOf(a[0]) else 0
        val digitB = if (b != null) DIGITS.indexOf(b[0]) else DIGITS.length
        return if (digitB - digitA > 1) {
            DIGITS[(digitA + digitB + 1) / 2].toString()
        } else if (b != null && b.length > 1) {
            b.substring(0, 1)
        } else {
            DIGITS[digitA] + midpoint(a.drop(1), null)
        }
    }
}
