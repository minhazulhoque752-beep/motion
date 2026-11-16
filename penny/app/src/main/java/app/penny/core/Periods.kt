package app.penny.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** The time windows used by History filters and Insights. Weeks run Monday to Sunday (ISO-8601). */
enum class PeriodType(val label: String) {
    ALL("All"),
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
}

data class DateRange(val start: LocalDate, val endInclusive: LocalDate) {
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(endInclusive)
}

object Periods {
    private val dayFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
    private val shortDayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val fullDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val headerFormat = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
    private val headerFormatWithYear = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
    private val greetingDate = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)

    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun range(type: PeriodType, anchor: LocalDate): DateRange? = when (type) {
        PeriodType.ALL -> null
        PeriodType.DAY -> DateRange(anchor, anchor)
        PeriodType.WEEK -> weekStart(anchor).let { DateRange(it, it.plusDays(6)) }
        PeriodType.MONTH -> YearMonth.from(anchor).let { DateRange(it.atDay(1), it.atEndOfMonth()) }
        PeriodType.YEAR -> DateRange(LocalDate.of(anchor.year, 1, 1), LocalDate.of(anchor.year, 12, 31))
    }

    /** Moves [anchor] by [steps] whole periods (negative = back in time). */
    fun shift(type: PeriodType, anchor: LocalDate, steps: Long): LocalDate = when (type) {
        PeriodType.ALL -> anchor
        PeriodType.DAY -> anchor.plusDays(steps)
        PeriodType.WEEK -> anchor.plusWeeks(steps)
        PeriodType.MONTH -> anchor.plusMonths(steps)
        PeriodType.YEAR -> anchor.plusYears(steps)
    }

    /** True when the period after [anchor]'s period would start in the future. */
    fun isCurrentOrFuture(type: PeriodType, anchor: LocalDate, today: LocalDate): Boolean {
        val r = range(type, anchor) ?: return true
        return !r.endInclusive.isBefore(today)
    }

    fun label(type: PeriodType, anchor: LocalDate, today: LocalDate): String = when (type) {
        PeriodType.ALL -> "All time"
        PeriodType.DAY -> when (anchor) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> anchor.format(dayFormat)
        }
        PeriodType.WEEK -> {
            val start = weekStart(anchor)
            if (start == weekStart(today)) "This week"
            else "${start.format(shortDayFormat)} – ${start.plusDays(6).format(shortDayFormat)} ${start.plusDays(6).year}"
        }
        PeriodType.MONTH -> if (YearMonth.from(anchor) == YearMonth.from(today)) "This month" else anchor.format(monthFormat)
        PeriodType.YEAR -> if (anchor.year == today.year) "This year" else anchor.year.toString()
    }

    /** Header used to group list items by day: "Today", "Yesterday", "Mon, 9 Nov" (year added if not this year). */
    fun dayHeader(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> if (date.year == today.year) date.format(headerFormat) else date.format(headerFormatWithYear)
    }

    fun formatFull(date: LocalDate): String = date.format(fullDate)

    fun formatMonth(month: YearMonth): String = month.format(monthFormat)

    fun formatMonthShort(month: YearMonth): String =
        month.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)

    fun greetingDate(date: LocalDate): String = date.format(greetingDate)

    fun daysBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)

    fun greeting(hour: Int): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Hello"
    }
}
