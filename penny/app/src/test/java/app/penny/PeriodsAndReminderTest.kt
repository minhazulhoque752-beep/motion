package app.penny

import app.penny.core.PeriodType
import app.penny.core.Periods
import app.penny.core.ReminderTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class PeriodsAndReminderTest {
    private val today = d(2026, 11, 16) // Monday

    @Test fun ranges() {
        assertNull(Periods.range(PeriodType.ALL, today))
        assertEquals(today, Periods.range(PeriodType.DAY, today)!!.start)
        val week = Periods.range(PeriodType.WEEK, d(2026, 11, 19))!!
        assertEquals(d(2026, 11, 16), week.start)
        assertEquals(d(2026, 11, 22), week.endInclusive)
        val feb = Periods.range(PeriodType.MONTH, d(2028, 2, 10))!!
        assertEquals(d(2028, 2, 29), feb.endInclusive)
        val year = Periods.range(PeriodType.YEAR, today)!!
        assertEquals(d(2026, 1, 1), year.start)
        assertEquals(d(2026, 12, 31), year.endInclusive)
    }

    @Test fun shiftingAndForwardLimit() {
        assertEquals(d(2026, 10, 16), Periods.shift(PeriodType.MONTH, today, -1))
        assertEquals(d(2026, 11, 9), Periods.shift(PeriodType.WEEK, today, -1))
        // Month-end clamps rather than overflowing: 31 Mar − 1 month = 28 Feb
        assertEquals(d(2026, 2, 28), Periods.shift(PeriodType.MONTH, d(2026, 3, 31), -1))
        assertTrue(Periods.isCurrentOrFuture(PeriodType.MONTH, today, today))
        assertFalse(Periods.isCurrentOrFuture(PeriodType.MONTH, d(2026, 10, 1), today))
    }

    @Test fun labels() {
        assertEquals("Today", Periods.label(PeriodType.DAY, today, today))
        assertEquals("Yesterday", Periods.label(PeriodType.DAY, today.minusDays(1), today))
        assertEquals("This month", Periods.label(PeriodType.MONTH, today, today))
        assertEquals("October 2026", Periods.label(PeriodType.MONTH, d(2026, 10, 3), today))
        assertEquals("2025", Periods.label(PeriodType.YEAR, d(2025, 6, 1), today))
        assertEquals("Mon, 9 Nov", Periods.dayHeader(d(2026, 11, 9), today))
        assertEquals("Wed, 31 Dec 2025", Periods.dayHeader(d(2025, 12, 31), today))
    }

    @Test fun greetings() {
        assertEquals("Good morning", Periods.greeting(7))
        assertEquals("Good afternoon", Periods.greeting(13))
        assertEquals("Good evening", Periods.greeting(19))
        assertEquals("Hello", Periods.greeting(2))
    }

    private val ist = ZoneId.of("Asia/Kolkata")

    @Test fun reminderLaterToday() {
        val now = ZonedDateTime.of(2026, 11, 16, 18, 30, 0, 0, ist)
        val expected = ZonedDateTime.of(2026, 11, 16, 20, 0, 0, 0, ist).toInstant().toEpochMilli()
        assertEquals(expected, ReminderTime.nextTriggerMillis(now, 20, 0))
    }

    @Test fun reminderTomorrowWhenTimeHasPassedOrIsNow() {
        val now = ZonedDateTime.of(2026, 11, 16, 20, 0, 0, 0, ist)
        val expected = ZonedDateTime.of(2026, 11, 17, 20, 0, 0, 0, ist).toInstant().toEpochMilli()
        assertEquals(expected, ReminderTime.nextTriggerMillis(now, 20, 0))
    }

    @Test fun reminderAcrossMonthAndYearEnd() {
        val now = ZonedDateTime.of(2026, 12, 31, 23, 0, 0, 0, ist)
        val expected = ZonedDateTime.of(2027, 1, 1, 9, 15, 0, 0, ist).toInstant().toEpochMilli()
        assertEquals(expected, ReminderTime.nextTriggerMillis(now, 9, 15))
    }

    @Test fun reminderOnDaylightSavingGap() {
        // 2:30 AM doesn't exist on 8 Mar 2026 in New York; it resolves to a valid instant after "now".
        val ny = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 3, 7, 22, 0, 0, 0, ny)
        val next = ReminderTime.nextTriggerMillis(now, 2, 30)
        assertTrue(next > now.toInstant().toEpochMilli())
        assertTrue(next - now.toInstant().toEpochMilli() <= 6 * 3_600_000L)
    }
}
