package app.penny.domain

import app.penny.core.Money
import java.time.LocalDate

/**
 * Validates changes to the ledger.
 *
 * Savings live in "buckets": one per goal plus one for unassigned savings. A withdrawal must come out of a
 * specific bucket, and no change (add, edit, delete) may leave any bucket below zero. This guarantees that
 * total savings never go negative and that goal progress always reflects money that is really there.
 */
object EntryValidator {

    const val MAX_NOTE_LENGTH = 120
    const val MAX_GOAL_TITLE_LENGTH = 40

    /** Returns an error message, or null when [draft] can be saved. [editingId] is the entry being edited, if any. */
    fun validate(
        draft: EntryDraft,
        existing: List<Entry>,
        editingId: Long?,
        today: LocalDate,
        goalIds: Set<Long>,
    ): String? {
        val amount = draft.amountPaise ?: return "Enter an amount"
        if (amount <= 0L) return "Amount must be greater than zero"
        if (amount > Money.MAX_AMOUNT_PAISE) return "Amount can't be more than ${Money.formatInr(Money.MAX_AMOUNT_PAISE, decimals = false)}"
        if (draft.date.isAfter(today)) return "Date can't be in the future"
        if (draft.note.length > MAX_NOTE_LENGTH) return "Note is too long"
        if (draft.goalId != null && draft.goalId !in goalIds) return "That goal no longer exists"

        val others = existing.filter { it.id != editingId }
        val candidate = Entry(
            id = editingId ?: -1L, amountPaise = amount, type = draft.type, category = draft.category,
            note = draft.note, date = draft.date, goalId = draft.goalId, createdAt = 0L, updatedAt = 0L,
        )
        val buckets = SavingsCalculator.buckets(others + candidate)
        val broken = buckets.entries.firstOrNull { it.value < 0 } ?: return null

        // Explain the problem in terms of the bucket the user is touching.
        val available = SavingsCalculator.buckets(others)[draft.goalId] ?: 0L
        return if (draft.type == EntryType.WITHDRAWAL && broken.key == draft.goalId) {
            when {
                available <= 0L && draft.goalId == null -> "There are no unassigned savings to withdraw. Choose the goal you're withdrawing from."
                available <= 0L -> "This goal has no savings to withdraw"
                draft.goalId == null -> "Only ${Money.formatInr(available)} is unassigned. Choose a goal to withdraw more."
                else -> "This goal only holds ${Money.formatInr(available)}"
            }
        } else {
            "This change would leave a negative balance because of an existing withdrawal. Edit that withdrawal first."
        }
    }

    /** Returns an error message if deleting [entryId] would leave a bucket negative, else null. */
    fun validateDelete(entryId: Long, existing: List<Entry>): String? {
        val remaining = existing.filter { it.id != entryId }
        return if (SavingsCalculator.buckets(remaining).values.any { it < 0 }) {
            "This saving covers a later withdrawal. Delete or edit that withdrawal first."
        } else null
    }

    /** Amount that can be withdrawn from [goalId] (null = unassigned), ignoring the entry being edited. */
    fun availableToWithdraw(existing: List<Entry>, goalId: Long?, editingId: Long?): Long =
        (SavingsCalculator.buckets(existing.filter { it.id != editingId })[goalId] ?: 0L).coerceAtLeast(0)

    fun validateGoal(title: String, targetPaise: Long?): String? {
        val t = title.trim()
        return when {
            t.isEmpty() -> "Give your goal a name"
            t.length > MAX_GOAL_TITLE_LENGTH -> "Keep the name under $MAX_GOAL_TITLE_LENGTH characters"
            targetPaise == null -> "Enter a target amount"
            targetPaise <= 0L -> "Target must be greater than zero"
            targetPaise > Money.MAX_AMOUNT_PAISE -> "Target can't be more than ${Money.formatInr(Money.MAX_AMOUNT_PAISE, decimals = false)}"
            else -> null
        }
    }

    /** Whole-ledger consistency check, used when restoring a backup. */
    fun isConsistent(entries: List<Entry>): Boolean =
        entries.all { it.amountPaise in 1..Money.MAX_AMOUNT_PAISE } &&
            SavingsCalculator.buckets(entries).values.all { it >= 0 }
}
