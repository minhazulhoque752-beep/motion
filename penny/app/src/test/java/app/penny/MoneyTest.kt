package app.penny

import app.penny.core.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test fun formatsIndianGrouping() {
        assertEquals("₹0.00", Money.formatInr(0))
        assertEquals("₹5.00", Money.formatInr(500))
        assertEquals("₹999.99", Money.formatInr(99_999))
        assertEquals("₹1,000.00", Money.formatInr(1_000_00))
        assertEquals("₹25,500.00", Money.formatInr(25_500_00))
        assertEquals("₹1,00,000.00", Money.formatInr(1_00_000_00))
        assertEquals("₹12,34,567.89", Money.formatInr(12_34_567_89))
        assertEquals("₹1,00,00,000.00", Money.formatInr(1_00_00_000_00))
        assertEquals("₹10,00,00,000.00", Money.formatInr(10_00_00_000_00))
    }

    @Test fun formatsSignsAndPaise() {
        assertEquals("−₹500.00", Money.formatInr(-500_00))
        assertEquals("+₹500.00", Money.formatInr(500_00, signed = true))
        assertEquals("₹0.00", Money.formatInr(0, signed = true))
        assertEquals("₹0.05", Money.formatInr(5))
        assertEquals("−₹0.50", Money.formatInr(-50))
    }

    @Test fun formatsWithoutDecimalsRoundingHalfUp() {
        assertEquals("₹1,00,000", Money.formatInr(1_00_000_00, decimals = false))
        assertEquals("₹11", Money.formatInr(10_50, decimals = false))
        assertEquals("₹10", Money.formatInr(10_49, decimals = false))
    }

    @Test fun groupIndianEdgeCases() {
        assertEquals("0", Money.groupIndian(0))
        assertEquals("999", Money.groupIndian(999))
        assertEquals("1,000", Money.groupIndian(1000))
        assertEquals("10,000", Money.groupIndian(10_000))
        assertEquals("1,00,000", Money.groupIndian(100_000))
        assertEquals("10,00,000", Money.groupIndian(1_000_000))
        assertEquals("1,00,00,000", Money.groupIndian(10_000_000))
    }

    @Test fun parsesValidInput() {
        assertEquals(500_00L, Money.parseToPaise("500"))
        assertEquals(1_250_50L, Money.parseToPaise("1,250.5"))
        assertEquals(1_250_55L, Money.parseToPaise("1250.55"))
        assertEquals(75L, Money.parseToPaise("0.75"))
        assertEquals(50L, Money.parseToPaise(".5"))
        assertEquals(300_00L, Money.parseToPaise("₹ 300"))
        assertEquals(1_00_000_00L, Money.parseToPaise("1,00,000"))
        assertEquals(0L, Money.parseToPaise("0"))
        assertEquals(12_00L, Money.parseToPaise("12."))
    }

    @Test fun rejectsInvalidInput() {
        assertNull(Money.parseToPaise(""))
        assertNull(Money.parseToPaise("   "))
        assertNull(Money.parseToPaise("abc"))
        assertNull(Money.parseToPaise("-5"))
        assertNull(Money.parseToPaise("1.234"))
        assertNull(Money.parseToPaise("1.2.3"))
        assertNull(Money.parseToPaise("1e5"))
        assertNull(Money.parseToPaise("1234567890"))
    }

    @Test fun parsingAvoidsFloatingPointErrors() {
        // 0.1 + 0.2 style problems must not exist: everything is integer paise.
        val a = Money.parseToPaise("0.10")!!
        val b = Money.parseToPaise("0.20")!!
        assertEquals(30L, a + b)
        assertEquals("₹0.30", Money.formatInr(a + b))
    }

    @Test fun sanitizesKeyboardInput() {
        assertEquals("123", Money.sanitizeInput("123"))
        assertEquals("12.34", Money.sanitizeInput("12.345"))
        assertEquals("0.5", Money.sanitizeInput(".5"))
        assertEquals("7", Money.sanitizeInput("007"))
        assertEquals("0.07", Money.sanitizeInput("0.07"))
        assertEquals("1.2", Money.sanitizeInput("1.2.3"))
        assertEquals("1000", Money.sanitizeInput("1,000"))
        assertEquals("12345678", Money.sanitizeInput("123456789"))
        assertEquals("", Money.sanitizeInput("abc"))
        assertEquals("5", Money.sanitizeInput("-5"))
    }

    @Test fun inputStringRoundTrips() {
        assertEquals("300", Money.toInputString(300_00))
        assertEquals("1250.50", Money.toInputString(1_250_50))
        assertEquals("0.05", Money.toInputString(5))
        listOf(1L, 99L, 100L, 12_345_67L, Money.MAX_AMOUNT_PAISE).forEach {
            assertEquals(it, Money.parseToPaise(Money.toInputString(it)))
        }
    }

    @Test fun compactFormat() {
        assertEquals("₹950", Money.formatCompact(950_00))
        assertEquals("₹12.5K", Money.formatCompact(12_500_00))
        assertEquals("₹3K", Money.formatCompact(3_000_00))
        assertEquals("₹3.2L", Money.formatCompact(3_20_000_00))
        assertEquals("₹1.1Cr", Money.formatCompact(1_10_00_000_00))
        assertEquals("−₹2K", Money.formatCompact(-2_000_00))
    }

    @Test fun plainDecimal() {
        assertEquals("1250.50", Money.toPlainDecimal(1_250_50))
        assertEquals("-5.00", Money.toPlainDecimal(-500))
        assertEquals("0.00", Money.toPlainDecimal(0))
    }
}
