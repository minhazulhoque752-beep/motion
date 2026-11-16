package app.penny

import app.penny.core.Money
import app.penny.domain.Category
import app.penny.domain.EntryDraft
import app.penny.domain.EntryType
import app.penny.domain.EntryValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryValidatorTest {
    private val today = d(2026, 11, 16)
    private val goals = setOf(1L, 2L)

    private fun draft(paise: Long?, type: EntryType = EntryType.DEPOSIT, goalId: Long? = null, date: java.time.LocalDate = today, note: String = "") =
        EntryDraft(paise, type, Category.GENERAL, note, date, goalId)

    @Test fun rejectsMissingZeroAndHugeAmounts() {
        assertEquals("Enter an amount", EntryValidator.validate(draft(null), emptyList(), null, today, goals))
        assertNotNull(EntryValidator.validate(draft(0), emptyList(), null, today, goals))
        assertNotNull(EntryValidator.validate(draft(-100), emptyList(), null, today, goals))
        assertNotNull(EntryValidator.validate(draft(Money.MAX_AMOUNT_PAISE + 1), emptyList(), null, today, goals))
        assertNull(EntryValidator.validate(draft(Money.MAX_AMOUNT_PAISE), emptyList(), null, today, goals))
        assertNull(EntryValidator.validate(draft(1), emptyList(), null, today, goals))
    }

    @Test fun rejectsFutureDatesButAllowsToday() {
        assertEquals("Date can't be in the future", EntryValidator.validate(draft(100, date = today.plusDays(1)), emptyList(), null, today, goals))
        assertNull(EntryValidator.validate(draft(100, date = today), emptyList(), null, today, goals))
        assertNull(EntryValidator.validate(draft(100, date = d(2020, 2, 29)), emptyList(), null, today, goals))
    }

    @Test fun rejectsLongNotesAndMissingGoals() {
        assertNotNull(EntryValidator.validate(draft(100, note = "x".repeat(121)), emptyList(), null, today, goals))
        assertEquals("That goal no longer exists", EntryValidator.validate(draft(100, goalId = 99), emptyList(), null, today, goals))
    }

    @Test fun withdrawalCannotExceedUnassignedBalance() {
        val existing = listOf(deposit(1_000, today), deposit(5_000, today, goalId = 1))
        assertNull(EntryValidator.validate(draft(1_000_00, EntryType.WITHDRAWAL), existing, null, today, goals))
        val err = EntryValidator.validate(draft(1_000_01, EntryType.WITHDRAWAL), existing, null, today, goals)
        assertNotNull(err)
        assertTrue(err!!.contains("₹1,000.00"))
    }

    @Test fun withdrawalFromGoalIsLimitedToThatGoal() {
        val existing = listOf(deposit(1_000, today), deposit(5_000, today, goalId = 1))
        assertNull(EntryValidator.validate(draft(5_000_00, EntryType.WITHDRAWAL, goalId = 1), existing, null, today, goals))
        assertEquals(
            "This goal only holds ₹5,000.00",
            EntryValidator.validate(draft(5_500_00, EntryType.WITHDRAWAL, goalId = 1), existing, null, today, goals),
        )
        assertEquals(
            "This goal has no savings to withdraw",
            EntryValidator.validate(draft(1, EntryType.WITHDRAWAL, goalId = 2), existing, null, today, goals),
        )
    }

    @Test fun withdrawalWithNothingSaved() {
        assertNotNull(EntryValidator.validate(draft(100, EntryType.WITHDRAWAL), emptyList(), null, today, goals))
    }

    @Test fun editingExcludesTheEntryBeingEdited() {
        val w = withdrawal(800, today)
        val existing = listOf(deposit(1_000, today), w)
        // Editing the ₹800 withdrawal up to ₹1,000 is fine: only ₹1,000 deposit counts.
        assertNull(EntryValidator.validate(draft(1_000_00, EntryType.WITHDRAWAL), existing, w.id, today, goals))
        assertNotNull(EntryValidator.validate(draft(1_000_01, EntryType.WITHDRAWAL), existing, w.id, today, goals))
        assertEquals(1_000_00L, EntryValidator.availableToWithdraw(existing, null, w.id))
        assertEquals(200_00L, EntryValidator.availableToWithdraw(existing, null, null))
    }

    @Test fun editingADepositCannotBreakALaterWithdrawal() {
        val dep = deposit(1_000, today)
        val existing = listOf(dep, withdrawal(700, today))
        assertNull(EntryValidator.validate(draft(700_00), existing, dep.id, today, goals))
        assertNotNull(EntryValidator.validate(draft(699_99), existing, dep.id, today, goals))
        // Moving the deposit into a goal would leave unassigned savings negative.
        assertNotNull(EntryValidator.validate(draft(1_000_00, goalId = 1), existing, dep.id, today, goals))
    }

    @Test fun deleteIsBlockedWhenItWouldLeaveNegativeBalance() {
        val dep = deposit(1_000, today)
        val other = deposit(200, today)
        val wd = withdrawal(900, today)
        val existing = listOf(dep, other, wd)
        assertNotNull(EntryValidator.validateDelete(dep.id, existing))
        assertNull(EntryValidator.validateDelete(other.id, existing))
        assertNull(EntryValidator.validateDelete(wd.id, existing))
    }

    @Test fun goalValidation() {
        assertNotNull(EntryValidator.validateGoal("  ", 100))
        assertNotNull(EntryValidator.validateGoal("x".repeat(41), 100))
        assertNotNull(EntryValidator.validateGoal("Phone", null))
        assertNotNull(EntryValidator.validateGoal("Phone", 0))
        assertNotNull(EntryValidator.validateGoal("Phone", Money.MAX_AMOUNT_PAISE + 1))
        assertNull(EntryValidator.validateGoal("New Phone", 30_000_00))
    }

    @Test fun ledgerConsistency() {
        assertTrue(EntryValidator.isConsistent(listOf(deposit(10, today), withdrawal(10, today))))
        assertFalse(EntryValidator.isConsistent(listOf(deposit(10, today), withdrawal(11, today))))
        assertFalse(EntryValidator.isConsistent(listOf(deposit(10, today, goalId = 1), withdrawal(5, today))))
    }
}
