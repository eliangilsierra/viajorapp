package app.viajor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoneyTest {
    private val usd = Currency.of("USD")
    private val eur = Currency.of("EUR")
    private val jpy = Currency.of("JPY")
    private val kwd = Currency.of("KWD")

    @Test
    fun parsingIsExactForZeroTwoAndThreeDecimalCurrencies() {
        assertEquals(1250, Money.parse("12.50", usd).minor)
        assertEquals(1200, Money.parse("12", usd).minor)
        assertEquals(1250, Money.parse("12.500", usd).minor) // trailing zeros are not extra precision
        assertEquals(1500, Money.parse("1500", jpy).minor)
        assertEquals(12_345, Money.parse("12.345", kwd).minor)
        assertEquals(-5, Money.parse("-0.05", usd).minor)
        assertEquals(10, Money.parse("0.1", usd).minor)
    }

    @Test
    fun inputThatWouldNeedRoundingOrIsNotPlainIsRejected() {
        assertFailsWith<IllegalArgumentException> { Money.parse("12.505", usd) }
        assertFailsWith<IllegalArgumentException> { Money.parse("12.5", jpy) }
        assertFailsWith<IllegalArgumentException> { Money.parse("1,234.50", usd) }
        assertFailsWith<IllegalArgumentException> { Money.parse("abc", usd) }
        assertFailsWith<IllegalArgumentException> { Money.parse("", usd) }
        assertFailsWith<IllegalArgumentException> { Currency.of("XXX") }
    }

    @Test
    fun decimalStringsRoundTrip() {
        for ((text, currency) in listOf("12.50" to usd, "0.05" to usd, "-0.05" to usd, "1500" to jpy, "-1500" to jpy, "0.001" to kwd, "-12.345" to kwd, "0.00" to usd)) {
            assertEquals(text, Money.parse(text, currency).toDecimalString())
        }
    }

    @Test
    fun mixedCurrenciesAreNeverCombinedImplicitly() {
        assertFailsWith<IllegalArgumentException> { Money(100, usd) + Money(100, eur) }
        val totals = Budget.totals(listOf(Money(100, usd), Money(250, eur), Money(50, usd), Money(1000, jpy)))
        assertEquals(listOf(Money(250, eur), Money(1000, jpy), Money(150, usd)), totals)
        assertEquals(emptyList(), Budget.totals(emptyList()))
    }

    @Test
    fun arithmeticAndNegation() {
        assertEquals(Money(75, usd), Money(100, usd) - Money(25, usd))
        assertEquals(Money(-100, usd), -Money(100, usd))
    }

    @Test
    fun overflowThrowsInsteadOfWrapping() {
        assertFailsWith<ArithmeticException> { Money(Long.MAX_VALUE, usd) + Money(1, usd) }
        assertFailsWith<ArithmeticException> { Money(Long.MIN_VALUE, usd) - Money(1, usd) }
        assertFailsWith<IllegalArgumentException> { -Money(Long.MIN_VALUE, usd) }
        assertFailsWith<ArithmeticException> { Money.parse("99999999999999999999", usd) }
        assertFailsWith<ArithmeticException> { Money(Long.MAX_VALUE / 2, usd).convert("1000000", eur) }
    }

    @Test
    fun conversionUsesTheSuppliedRateAndRoundsHalfToEven() {
        // 10.00 USD at 0.9 = 9.00 EUR.
        assertEquals(Money(900, eur), Money(1000, usd).convert("0.9", eur))
        // 100 JPY (a zero-decimal currency) at 0.006 EUR per JPY = 0.60 EUR.
        assertEquals(Money(60, eur), Money(100, jpy).convert("0.006", eur))
        // 1.00 EUR at 150 JPY per EUR = 150 JPY.
        assertEquals(Money(150, jpy), Money(100, eur).convert("150", jpy))
        // Ties go to the even neighbour: 0.05 USD * 50 = 2.5 JPY -> 2, and 0.07 USD * 50 = 3.5 JPY -> 4.
        assertEquals(Money(2, jpy), Money(5, usd).convert("50", jpy))
        assertEquals(Money(4, jpy), Money(7, usd).convert("50", jpy))
    }

    @Test
    fun halfEvenTiesGoToTheEvenNeighbour() {
        assertEquals(2, Money.divideHalfEven(5, 2)) // 2.5 -> 2
        assertEquals(4, Money.divideHalfEven(7, 2)) // 3.5 -> 4
        assertEquals(-2, Money.divideHalfEven(-5, 2)) // -2.5 -> -2
        assertEquals(-4, Money.divideHalfEven(-7, 2)) // -3.5 -> -4
        assertEquals(3, Money.divideHalfEven(26, 10)) // 2.6 -> 3
        assertEquals(2, Money.divideHalfEven(24, 10)) // 2.4 -> 2
    }

    @Test
    fun nonPositiveRatesAreRejected() {
        assertFailsWith<IllegalArgumentException> { Money(100, usd).convert("0", eur) }
        assertFailsWith<IllegalArgumentException> { Money(100, usd).convert("-1.2", eur) }
        assertFailsWith<IllegalArgumentException> { Money(100, usd).convert("abc", eur) }
    }
}
