package app.penny.domain

import app.penny.core.DateRange
import app.penny.core.Money
import app.penny.core.Periods
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Every number shown in Penny is computed here, from the raw entries stored in Room.
 * Nothing is cached or duplicated, so totals can never drift out of sync with the ledger.
 */
object SavingsCalculator {

    /** Total savings = all deposits − all withdrawals. */
    fun total(entries: List<Entry>): Long = entries.sumOf { it.signedPaise }

    /** Net savings (deposits − withdrawals) for entries dated inside [range]. */
    fun net(entries: List<Entry>, range: DateRange): Long =
        entries.filter { it.date in range }.sumOf { it.signedPaise }

    fun netForMonth(entries: List<Entry>, month: YearMonth): Long =
        net(entries, DateRange(month.atDay(1), month.atEndOfMonth()))

    fun netForWeek(entries: List<Entry>, today: LocalDate): Long {
        val start = Periods.weekStart(today)
        return net(entries, DateRange(start, start.plusDays(6)))
    }

    fun netForDay(entries: List<Entry>, day: LocalDate): Long = net(entries, DateRange(day, day))

    /** Average net saved per day this month, counting days elapsed so far (including today). */
    fun dailyAverageThisMonth(entries: List<Entry>, today: LocalDate): Long {
        val month = YearMonth.from(today)
        val days = today.dayOfMonth.toLong()
        return netForMonth(entries, month) / days
    }

    /** Goal progress in percent: allocated / target × 100. */
    fun progressPercent(allocatedPaise: Long, targetPaise: Long): Double =
        if (targetPaise <= 0L) 0.0 else allocatedPaise.coerceAtLeast(0) * 100.0 / targetPaise

    /** Net allocation per goal. Each entry belongs to at most one goal, so nothing is double-counted. */
    fun goalProgress(goals: List<Goal>, entries: List<Entry>): List<GoalProgress> {
        val byGoal = entries.filter { it.goalId != null }.groupBy { it.goalId }
        return goals.map { goal ->
            val list = byGoal[goal.id].orEmpty()
            GoalProgress(goal, list.sumOf { it.signedPaise }, list.size)
        }
    }

    /** Savings that are not assigned to any goal. */
    fun unassigned(entries: List<Entry>): Long = entries.filter { it.goalId == null }.sumOf { it.signedPaise }

    /**
     * The balance held in each "bucket": one per goal, plus `null` for unassigned savings.
     * Bucket balances must never go negative — see [EntryValidator].
     */
    fun buckets(entries: List<Entry>): Map<Long?, Long> =
        entries.groupBy { it.goalId }.mapValues { (_, list) -> list.sumOf { it.signedPaise } }

    /** Monthly totals for the [count] months ending with [endMonth] (oldest first), including empty months. */
    fun monthlySeries(entries: List<Entry>, endMonth: YearMonth, count: Int): List<MonthPoint> {
        val byMonth = entries.groupBy { YearMonth.from(it.date) }
        return (count - 1 downTo 0).map { back ->
            val m = endMonth.minusMonths(back.toLong())
            monthPoint(m, byMonth[m].orEmpty())
        }
    }

    fun monthComparison(entries: List<Entry>, current: YearMonth): MonthComparison {
        val byMonth = entries.groupBy { YearMonth.from(it.date) }
        val prev = current.minusMonths(1)
        return MonthComparison(monthPoint(current, byMonth[current].orEmpty()), monthPoint(prev, byMonth[prev].orEmpty()))
    }

    /** The month with the highest net savings. Ties go to the most recent month. Null when there are no entries. */
    fun bestMonth(entries: List<Entry>): MonthPoint? =
        entries.groupBy { YearMonth.from(it.date) }
            .map { (m, list) -> monthPoint(m, list) }
            .maxWithOrNull(compareBy<MonthPoint> { it.netPaise }.thenBy { it.month })

    /** Share of deposits per category (withdrawals are excluded). Sorted largest first; empty categories omitted. */
    fun categoryBreakdown(entries: List<Entry>): List<CategoryShare> {
        val deposits = entries.filter { !it.isWithdrawal }
        val total = deposits.sumOf { it.amountPaise }
        if (total == 0L) return emptyList()
        return deposits.groupBy { it.category }
            .map { (c, list) -> list.sumOf { it.amountPaise }.let { CategoryShare(c, it, it.toFloat() / total) } }
            .sortedByDescending { it.depositsPaise }
    }

    /**
     * How much needs to be saved per month to reach [progress]'s target by its target date.
     * Returns null when the goal has no date, is already reached, or the date has passed.
     */
    fun monthlyNeeded(progress: GoalProgress, today: LocalDate): Long? {
        val date = progress.goal.targetDate ?: return null
        if (progress.isReached || date.isBefore(today)) return null
        val months = ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(date)) + 1
        val remaining = progress.remainingPaise
        return (remaining + months - 1) / months // round up so the goal is actually met
    }

    /** Newest first: by date, then by creation time, then id. */
    fun sortNewestFirst(entries: List<Entry>): List<Entry> =
        entries.sortedWith(compareByDescending<Entry> { it.date }.thenByDescending { it.createdAt }.thenByDescending { it.id })

    /**
     * Filters for the History screen. [query] matches note, category, goal title or amount (e.g. "500", "1,500").
     */
    fun filter(entries: List<Entry>, range: DateRange?, query: String, goalTitles: Map<Long, String>): List<Entry> {
        val q = query.trim().lowercase()
        val qDigits = q.replace(",", "").replace("₹", "").trim()
        return entries.filter { e ->
            (range == null || e.date in range) && (q.isEmpty() || matches(e, q, qDigits, goalTitles))
        }
    }

    private fun matches(e: Entry, q: String, qDigits: String, goalTitles: Map<Long, String>): Boolean {
        if (e.note.lowercase().contains(q)) return true
        if (e.category.label.lowercase().contains(q)) return true
        if (e.goalId != null && goalTitles[e.goalId]?.lowercase()?.contains(q) == true) return true
        if (q == "withdrawal" || q == "withdrawals") return e.isWithdrawal
        if (q == "saving" || q == "savings" || q == "deposit") return !e.isWithdrawal
        if (qDigits.isNotEmpty() && qDigits.all { it.isDigit() || it == '.' }) {
            return Money.toPlainDecimal(e.amountPaise).contains(qDigits) || Money.toInputString(e.amountPaise).contains(qDigits)
        }
        return false
    }

    private fun monthPoint(month: YearMonth, list: List<Entry>) = MonthPoint(
        month = month,
        depositsPaise = list.filter { !it.isWithdrawal }.sumOf { it.amountPaise },
        withdrawalsPaise = list.filter { it.isWithdrawal }.sumOf { it.amountPaise },
    )
}
