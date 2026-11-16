package app.penny.domain

import java.time.LocalDate
import java.time.YearMonth

enum class EntryType { DEPOSIT, WITHDRAWAL }

enum class Category(val label: String) {
    GENERAL("General"),
    EMERGENCY("Emergency"),
    TRAVEL("Travel"),
    SHOPPING("Shopping"),
    OTHER("Other");

    companion object {
        fun fromName(name: String?): Category = entries.firstOrNull { it.name == name } ?: OTHER
    }
}

/** A single savings (deposit) or withdrawal record. [amountPaise] is always positive; [type] gives the direction. */
data class Entry(
    val id: Long,
    val amountPaise: Long,
    val type: EntryType,
    val category: Category,
    val note: String,
    val date: LocalDate,
    val goalId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    /** Positive for deposits, negative for withdrawals. */
    val signedPaise: Long get() = if (type == EntryType.DEPOSIT) amountPaise else -amountPaise
    val isWithdrawal: Boolean get() = type == EntryType.WITHDRAWAL
}

data class Goal(
    val id: Long,
    val title: String,
    val targetPaise: Long,
    val targetDate: LocalDate?,
    val createdAt: Long,
)

data class GoalProgress(
    val goal: Goal,
    /** Net amount allocated: deposits assigned to this goal minus withdrawals taken from it. */
    val allocatedPaise: Long,
    val entryCount: Int,
) {
    /** Exact percentage, may exceed 100 when a goal is over-funded. */
    val percent: Double get() = SavingsCalculator.progressPercent(allocatedPaise, goal.targetPaise)
    /** 0f..1f for progress bars. */
    val fraction: Float get() = (percent / 100.0).coerceIn(0.0, 1.0).toFloat()
    val remainingPaise: Long get() = (goal.targetPaise - allocatedPaise).coerceAtLeast(0)
    val isReached: Boolean get() = allocatedPaise >= goal.targetPaise
}

/** Inputs for creating or editing an entry, before validation. */
data class EntryDraft(
    val amountPaise: Long?,
    val type: EntryType,
    val category: Category,
    val note: String,
    val date: LocalDate,
    val goalId: Long?,
)

data class MonthPoint(
    val month: YearMonth,
    val depositsPaise: Long,
    val withdrawalsPaise: Long,
) {
    val netPaise: Long get() = depositsPaise - withdrawalsPaise
}

data class MonthComparison(
    val current: MonthPoint,
    val previous: MonthPoint,
) {
    /** Percentage change of net savings vs previous month, or null when there is nothing to compare against. */
    val changePercent: Double?
        get() = if (previous.netPaise <= 0L) null
        else (current.netPaise - previous.netPaise) * 100.0 / previous.netPaise
}

data class CategoryShare(val category: Category, val depositsPaise: Long, val fraction: Float)
