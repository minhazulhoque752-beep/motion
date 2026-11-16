package app.penny

import app.penny.core.DateRange
import app.penny.domain.Category
import app.penny.domain.SavingsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class SavingsCalculatorTest {
    private val today = d(2026, 11, 16) // a Monday

    @Test fun totalIsDepositsMinusWithdrawals() {
        val entries = listOf(
            deposit(10_000, d(2026, 10, 1)),
            deposit(5_000, d(2026, 11, 2)),
            withdrawal(2_500, d(2026, 11, 3)),
            deposit(13_000, d(2026, 11, 10)),
        )
        assertEquals(25_500_00L, SavingsCalculator.total(entries))
        assertEquals(0L, SavingsCalculator.total(emptyList()))
    }

    @Test fun totalKeepsPaiseExact() {
        val entries = (1..100).map { deposit(0, today, paise = 10) } // 100 × ₹0.10
        assertEquals(10_00L, SavingsCalculator.total(entries))
    }

    @Test fun monthlyNetSubtractsWithdrawalsOfThatMonthOnly() {
        val entries = listOf(
            deposit(1_000, d(2026, 10, 31)),
            deposit(5_000, d(2026, 11, 1)),
            withdrawal(1_200, d(2026, 11, 30)),
            deposit(700, d(2026, 12, 1)),
        )
        assertEquals(3_800_00L, SavingsCalculator.netForMonth(entries, YearMonth.of(2026, 11)))
        assertEquals(1_000_00L, SavingsCalculator.netForMonth(entries, YearMonth.of(2026, 10)))
        assertEquals(700_00L, SavingsCalculator.netForMonth(entries, YearMonth.of(2026, 12)))
    }

    @Test fun monthBoundariesHandleLeapYears() {
        val entries = listOf(deposit(100, d(2028, 2, 29)), deposit(50, d(2028, 3, 1)))
        assertEquals(100_00L, SavingsCalculator.netForMonth(entries, YearMonth.of(2028, 2)))
        assertEquals(50_00L, SavingsCalculator.netForMonth(entries, YearMonth.of(2028, 3)))
    }

    @Test fun weekRunsMondayToSunday() {
        val entries = listOf(
            deposit(100, d(2026, 11, 15)), // Sunday, previous week
            deposit(200, d(2026, 11, 16)), // Monday
            deposit(300, d(2026, 11, 22)), // Sunday, same week
            deposit(400, d(2026, 11, 23)), // next Monday
        )
        assertEquals(500_00L, SavingsCalculator.netForWeek(entries, d(2026, 11, 19)))
        assertEquals(500_00L, SavingsCalculator.netForWeek(entries, d(2026, 11, 22)))
        assertEquals(100_00L, SavingsCalculator.netForWeek(entries, d(2026, 11, 15)))
    }

    @Test fun weekAcrossYearBoundary() {
        // 31 Dec 2026 is a Thursday; its week starts Mon 28 Dec and ends Sun 3 Jan 2027.
        val entries = listOf(deposit(10, d(2026, 12, 28)), deposit(20, d(2027, 1, 3)), deposit(40, d(2027, 1, 4)))
        assertEquals(30_00L, SavingsCalculator.netForWeek(entries, d(2026, 12, 31)))
    }

    @Test fun dailyNetAndAverage() {
        val entries = listOf(
            deposit(300, today),
            withdrawal(100, today),
            deposit(1_000, d(2026, 11, 1)),
            deposit(9_999, d(2026, 10, 31)),
        )
        assertEquals(200_00L, SavingsCalculator.netForDay(entries, today))
        // (300 − 100 + 1000) over 16 days = ₹75 per day
        assertEquals(75_00L, SavingsCalculator.dailyAverageThisMonth(entries, today))
    }

    @Test fun netForRangeIsInclusive() {
        val entries = listOf(deposit(1, d(2026, 1, 1)), deposit(2, d(2026, 1, 31)), deposit(4, d(2026, 2, 1)))
        assertEquals(3_00L, SavingsCalculator.net(entries, DateRange(d(2026, 1, 1), d(2026, 1, 31))))
    }

    @Test fun goalProgressUsesOnlyAssignedEntries() {
        val phone = goal(1, "New Phone", 30_000)
        val fund = goal(2, "Emergency Fund", 50_000)
        val entries = listOf(
            deposit(10_000, d(2026, 9, 1), goalId = 1),
            deposit(2_000, d(2026, 10, 1), goalId = 1),
            withdrawal(1_000, d(2026, 10, 5), goalId = 1),
            deposit(25_000, d(2026, 10, 2), goalId = 2),
            deposit(3_000, d(2026, 10, 3)), // unassigned
        )
        val progress = SavingsCalculator.goalProgress(listOf(phone, fund), entries).associateBy { it.goal.id }
        assertEquals(11_000_00L, progress.getValue(1).allocatedPaise)
        assertEquals(19_000_00L, progress.getValue(1).remainingPaise)
        assertEquals(36.666, progress.getValue(1).percent, 0.001)
        assertEquals(3, progress.getValue(1).entryCount)
        assertEquals(50.0, progress.getValue(2).percent, 0.0001)
        assertEquals(3_000_00L, SavingsCalculator.unassigned(entries))
    }

    @Test fun noDoubleCountingBetweenGoalsAndTotal() {
        val goals = listOf(goal(1, "A", 1_000), goal(2, "B", 1_000))
        val entries = listOf(
            deposit(400, today, goalId = 1),
            deposit(600, today, goalId = 2),
            deposit(250, today),
            withdrawal(100, today, goalId = 2),
        )
        val allocated = SavingsCalculator.goalProgress(goals, entries).sumOf { it.allocatedPaise }
        assertEquals(SavingsCalculator.total(entries), allocated + SavingsCalculator.unassigned(entries))
    }

    @Test fun goalPercentEdgeCases() {
        assertEquals(0.0, SavingsCalculator.progressPercent(0, 100_00), 0.0)
        assertEquals(100.0, SavingsCalculator.progressPercent(100_00, 100_00), 0.0)
        assertEquals(150.0, SavingsCalculator.progressPercent(150_00, 100_00), 0.0)
        assertEquals(0.0, SavingsCalculator.progressPercent(50_00, 0), 0.0)
        val reached = SavingsCalculator.goalProgress(listOf(goal(1, "A", 100)), listOf(deposit(150, today, goalId = 1))).first()
        assertTrue(reached.isReached)
        assertEquals(0L, reached.remainingPaise)
        assertEquals(1f, reached.fraction)
    }

    @Test fun monthlySeriesIncludesEmptyMonthsOldestFirst() {
        val entries = listOf(deposit(100, d(2026, 9, 10)), deposit(300, d(2026, 11, 1)), withdrawal(50, d(2026, 11, 2)))
        val series = SavingsCalculator.monthlySeries(entries, YearMonth.of(2026, 11), 4)
        assertEquals(listOf(YearMonth.of(2026, 8), YearMonth.of(2026, 9), YearMonth.of(2026, 10), YearMonth.of(2026, 11)), series.map { it.month })
        assertEquals(listOf(0L, 100_00L, 0L, 250_00L), series.map { it.netPaise })
        assertEquals(50_00L, series.last().withdrawalsPaise)
    }

    @Test fun monthlySeriesAcrossYearBoundary() {
        val series = SavingsCalculator.monthlySeries(listOf(deposit(5, d(2025, 12, 31))), YearMonth.of(2026, 2), 3)
        assertEquals(listOf(YearMonth.of(2025, 12), YearMonth.of(2026, 1), YearMonth.of(2026, 2)), series.map { it.month })
        assertEquals(5_00L, series.first().netPaise)
    }

    @Test fun bestMonthIsHighestNet() {
        val entries = listOf(
            deposit(5_000, d(2026, 3, 1)),
            withdrawal(4_000, d(2026, 3, 2)),
            deposit(3_000, d(2026, 4, 1)),
            deposit(2_999, d(2026, 5, 1)),
        )
        val best = SavingsCalculator.bestMonth(entries)!!
        assertEquals(YearMonth.of(2026, 4), best.month)
        assertEquals(3_000_00L, best.netPaise)
        assertNull(SavingsCalculator.bestMonth(emptyList()))
    }

    @Test fun bestMonthTieGoesToMostRecent() {
        val entries = listOf(deposit(100, d(2026, 1, 1)), deposit(100, d(2026, 2, 1)))
        assertEquals(YearMonth.of(2026, 2), SavingsCalculator.bestMonth(entries)!!.month)
    }

    @Test fun monthComparison() {
        val entries = listOf(deposit(1_000, d(2026, 10, 5)), deposit(1_500, d(2026, 11, 5)))
        val cmp = SavingsCalculator.monthComparison(entries, YearMonth.of(2026, 11))
        assertEquals(1_500_00L, cmp.current.netPaise)
        assertEquals(1_000_00L, cmp.previous.netPaise)
        assertEquals(50.0, cmp.changePercent!!, 0.0001)
        val noPrev = SavingsCalculator.monthComparison(listOf(deposit(10, d(2026, 11, 1))), YearMonth.of(2026, 11))
        assertNull(noPrev.changePercent)
    }

    @Test fun categoryBreakdownIgnoresWithdrawals() {
        val entries = listOf(
            deposit(750, today, category = Category.TRAVEL),
            deposit(250, today, category = Category.EMERGENCY),
            withdrawal(500, today, category = Category.TRAVEL),
        )
        val shares = SavingsCalculator.categoryBreakdown(entries)
        assertEquals(listOf(Category.TRAVEL, Category.EMERGENCY), shares.map { it.category })
        assertEquals(0.75f, shares[0].fraction, 0.0001f)
        assertEquals(750_00L, shares[0].depositsPaise)
        assertTrue(SavingsCalculator.categoryBreakdown(emptyList()).isEmpty())
    }

    @Test fun monthlyNeededRoundsUpAndCountsCurrentMonth() {
        val g = goal(1, "Trip", 20_000, targetDate = d(2027, 1, 20))
        val p = SavingsCalculator.goalProgress(listOf(g), listOf(deposit(5_000, today, goalId = 1))).first()
        // Nov, Dec, Jan = 3 months for ₹15,000 → ₹5,000/month
        assertEquals(5_000_00L, SavingsCalculator.monthlyNeeded(p, today))
        val odd = SavingsCalculator.goalProgress(listOf(goal(2, "X", 100, d(2027, 1, 1))), emptyList()).first()
        assertEquals(33_34L, SavingsCalculator.monthlyNeeded(odd, today)) // 10000 paise / 3 rounded up
        val past = SavingsCalculator.goalProgress(listOf(goal(3, "Y", 100, d(2026, 1, 1))), emptyList()).first()
        assertNull(SavingsCalculator.monthlyNeeded(past, today))
        val noDate = SavingsCalculator.goalProgress(listOf(goal(4, "Z", 100)), emptyList()).first()
        assertNull(SavingsCalculator.monthlyNeeded(noDate, today))
    }

    @Test fun sortsNewestFirst() {
        val a = deposit(1, d(2026, 11, 1))
        val b = deposit(2, d(2026, 11, 5))
        val c = deposit(3, d(2026, 11, 5)) // created later, same day
        assertEquals(listOf(c, b, a), SavingsCalculator.sortNewestFirst(listOf(a, b, c)))
    }

    @Test fun searchMatchesNoteCategoryGoalAndAmount() {
        val e1 = deposit(1_500, today, goalId = 7, category = Category.TRAVEL, note = "Goa trip fund")
        val e2 = deposit(250, today, category = Category.SHOPPING, note = "")
        val e3 = withdrawal(40, today, note = "Bus")
        val all = listOf(e1, e2, e3)
        val titles = mapOf(7L to "Holiday")
        assertEquals(listOf(e1), SavingsCalculator.filter(all, null, "goa", titles))
        assertEquals(listOf(e2), SavingsCalculator.filter(all, null, "shop", titles))
        assertEquals(listOf(e1), SavingsCalculator.filter(all, null, "holiday", titles))
        assertEquals(listOf(e1), SavingsCalculator.filter(all, null, "1,500", titles))
        assertEquals(listOf(e3), SavingsCalculator.filter(all, null, "withdrawal", titles))
        assertEquals(all, SavingsCalculator.filter(all, null, "  ", titles))
    }

    @Test fun filterByRange() {
        val inRange = deposit(1, d(2026, 11, 10))
        val out = deposit(2, d(2026, 10, 10))
        assertEquals(listOf(inRange), SavingsCalculator.filter(listOf(inRange, out), DateRange(d(2026, 11, 1), d(2026, 11, 30)), "", emptyMap()))
    }
}
