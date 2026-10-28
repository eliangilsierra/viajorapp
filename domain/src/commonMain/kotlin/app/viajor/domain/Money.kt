package app.viajor.domain

/** An ISO 4217 currency and how many digits its minor unit has (JPY 0, USD 2, KWD 3). */
class Currency private constructor(val code: String, val minorDigits: Int) : Comparable<Currency> {
    override fun compareTo(other: Currency): Int = code.compareTo(other.code)
    override fun equals(other: Any?): Boolean = other is Currency && other.code == code
    override fun hashCode(): Int = code.hashCode()
    override fun toString(): String = code

    companion object {
        private val zero = "JPY KRW VND CLP ISK UGX PYG XAF XOF"
        private val three = "KWD BHD OMR JOD TND LYD IQD"
        private val two = "USD EUR GBP CHF CAD AUD NZD CNY INR MXN BRL ARS COP PEN UYU SEK NOK DKK PLN CZK HUF TRY ZAR THB IDR PHP MYR SGD HKD TWD AED SAR ILS EGP MAD RON BGN HRK RSD UAH KES NGN GHS"

        private val known: Map<String, Currency> =
            zero.split(' ').associateWith { Currency(it, 0) } +
                three.split(' ').associateWith { Currency(it, 3) } +
                two.split(' ').associateWith { Currency(it, 2) }

        /** A known currency by code; unknown codes fail loudly rather than guessing the number of decimals. */
        fun of(code: String): Currency = known[code.uppercase()]
            ?: throw IllegalArgumentException("unsupported currency code: $code")
    }
}

/** An exact amount in the minor unit of [currency]. Floating point is never used for money. */
data class Money(val minor: Long, val currency: Currency) {
    operator fun plus(other: Money): Money {
        requireSame(other)
        return Money(addExact(minor, other.minor), currency)
    }

    operator fun minus(other: Money): Money {
        requireSame(other)
        require(other.minor != Long.MIN_VALUE) { "amount overflow" }
        return Money(addExact(minor, -other.minor), currency)
    }

    operator fun unaryMinus(): Money {
        require(minor != Long.MIN_VALUE) { "amount overflow" }
        return Money(-minor, currency)
    }

    /** A plain decimal such as `12.50` or `-1200` (no grouping, no currency symbol; formatting is the UI's job). */
    fun toDecimalString(): String {
        val negative = minor < 0
        val digits = minor.toString().removePrefix("-")
        val d = currency.minorDigits
        val body = if (d == 0) digits else {
            val padded = digits.padStart(d + 1, '0')
            padded.dropLast(d) + "." + padded.takeLast(d)
        }
        return if (negative) "-$body" else body
    }

    /**
     * Converts with a rate the user supplied: one major unit of this currency equals [rate] major units of
     * [target]. Rounding is half to even. Nothing fetches or assumes a rate.
     */
    fun convert(rate: String, target: Currency): Money {
        val parsed = parseDecimal(rate)
        require(!parsed.negative && parsed.digits.any { it != '0' }) { "rate must be positive" }
        val numerator = mulExact(mulExact(minor, parsed.digits.toLongOrNull() ?: throw ArithmeticException("rate is too large")), pow10(target.minorDigits))
        val denominator = mulExact(pow10(parsed.scale), pow10(currency.minorDigits))
        return Money(divideHalfEven(numerator, denominator), target)
    }

    private fun requireSame(other: Money) =
        require(currency == other.currency) { "cannot combine ${currency.code} and ${other.currency.code} without a conversion" }

    companion object {
        /** Exact parse of a plain decimal; more fractional digits than the currency has is an error unless they are zeros. */
        fun parse(text: String, currency: Currency): Money {
            val p = parseDecimal(text.trim())
            val d = currency.minorDigits
            var fraction = p.fraction
            if (fraction.length > d) {
                require(fraction.drop(d).all { it == '0' }) { "too many decimals for ${currency.code}: $text" }
                fraction = fraction.take(d)
            }
            val minorText = p.whole + fraction.padEnd(d, '0')
            val value = (if (p.negative) "-" else "") + minorText
            val minor = value.toLongOrNull() ?: throw ArithmeticException("amount is too large: $text")
            return Money(minor, currency)
        }

        private class Decimal(val negative: Boolean, val whole: String, val fraction: String) {
            val scale: Int get() = fraction.length
            val digits: String get() = (whole + fraction).trimStart('0').ifEmpty { "0" }
        }

        private val decimal = Regex("^(-)?(\\d+)(?:\\.(\\d+))?$")

        private fun parseDecimal(text: String): Decimal {
            val m = decimal.matchEntire(text) ?: throw IllegalArgumentException("not a plain decimal number: $text")
            return Decimal(m.groupValues[1] == "-", m.groupValues[2], m.groupValues[3])
        }

        internal fun addExact(a: Long, b: Long): Long {
            val r = a + b
            if (((a xor r) and (b xor r)) < 0) throw ArithmeticException("amount overflow")
            return r
        }

        internal fun mulExact(a: Long, b: Long): Long {
            if (a == 0L || b == 0L) return 0
            val r = a * b
            if (r / b != a || (a == Long.MIN_VALUE && b == -1L) || (b == Long.MIN_VALUE && a == -1L)) throw ArithmeticException("amount overflow")
            return r
        }

        private fun pow10(n: Int): Long {
            require(n in 0..18) { "scale out of range" }
            var r = 1L
            repeat(n) { r *= 10 }
            return r
        }

        /** Half-to-even division: ties go to the even neighbour, so repeated rounding has no bias. */
        internal fun divideHalfEven(numerator: Long, denominator: Long): Long {
            val q = numerator / denominator
            val r = numerator % denominator
            if (r == 0L) return q
            val twiceR = mulExact(if (r < 0) -r else r, 2)
            val absDen = if (denominator < 0) -denominator else denominator
            val sign = if ((numerator < 0) != (denominator < 0)) -1L else 1L
            return when {
                twiceR > absDen -> q + sign
                twiceR < absDen -> q
                q % 2 == 0L -> q
                else -> q + sign
            }
        }
    }
}

object Budget {
    /** One total per currency, ordered by currency code. Currencies are never mixed or converted implicitly. */
    fun totals(items: List<Money>): List<Money> =
        items.groupBy { it.currency }
            .entries.sortedBy { it.key }
            .map { (currency, group) -> group.fold(Money(0, currency)) { sum, m -> sum + m } }
}
