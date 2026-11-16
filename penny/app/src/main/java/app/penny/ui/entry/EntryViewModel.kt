package app.penny.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.penny.AppContainer
import app.penny.core.Money
import app.penny.data.DeleteResult
import app.penny.domain.Category
import app.penny.domain.Entry
import app.penny.domain.EntryDraft
import app.penny.domain.EntryType
import app.penny.domain.EntryValidator
import app.penny.domain.Goal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EntryForm(
    val loading: Boolean = false,
    val notFound: Boolean = false,
    val amount: String = "",
    val type: EntryType = EntryType.DEPOSIT,
    val category: Category = Category.GENERAL,
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val goalId: Long? = null,
    val error: String? = null,
    val amountError: String? = null,
    val saving: Boolean = false,
    val done: Boolean = false,
)

data class EntryUiState(
    val form: EntryForm = EntryForm(),
    val goals: List<Goal> = emptyList(),
    /** Balance available to withdraw from the selected bucket (goal or unassigned). */
    val availablePaise: Long = 0,
    val today: LocalDate = LocalDate.now(),
)

/** Everything the entry form can ask for; implemented by [EntryViewModel]. */
interface EntryFormActions {
    fun setAmount(value: String)
    fun setType(type: EntryType)
    fun setCategory(category: Category)
    fun setNote(note: String)
    fun setGoal(goalId: Long?)
    fun save()
}

class EntryViewModel(
    private val container: AppContainer,
    val editingId: Long?,
    initialType: EntryType,
    initialGoalId: Long?,
) : ViewModel(), EntryFormActions {

    private val repo = container.repository
    private val today: LocalDate get() = LocalDate.now(container.clock)

    private val form = MutableStateFlow(
        EntryForm(loading = editingId != null, type = initialType, goalId = initialGoalId, date = today),
    )

    val state: StateFlow<EntryUiState> = combine(form, repo.goals, repo.entries) { f, goals, entries ->
        EntryUiState(
            form = f,
            goals = goals,
            availablePaise = EntryValidator.availableToWithdraw(entries, f.goalId, editingId),
            today = today,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntryUiState(form = form.value))

    init {
        if (editingId != null) {
            viewModelScope.launch {
                val e: Entry? = repo.getEntry(editingId)
                form.value = if (e == null) {
                    EntryForm(notFound = true)
                } else {
                    EntryForm(
                        amount = Money.toInputString(e.amountPaise),
                        type = e.type,
                        category = e.category,
                        note = e.note,
                        date = e.date,
                        goalId = e.goalId,
                    )
                }
            }
        }
    }

    override fun setAmount(value: String) = form.update { it.copy(amount = value, amountError = null, error = null) }
    override fun setType(type: EntryType) = form.update { it.copy(type = type, error = null) }
    override fun setCategory(category: Category) = form.update { it.copy(category = category) }
    override fun setNote(note: String) = form.update { it.copy(note = note.take(EntryValidator.MAX_NOTE_LENGTH)) }
    fun setDate(date: LocalDate) = form.update { it.copy(date = date, error = null) }
    override fun setGoal(goalId: Long?) = form.update { it.copy(goalId = goalId, error = null) }

    /** Saves once. Repeated taps while saving (or after success) are ignored, so an entry can't be duplicated. */
    override fun save() {
        val f = form.value
        if (f.saving || f.done || f.loading || f.notFound) return
        val paise = Money.parseToPaise(f.amount)
        if (paise == null || paise <= 0L) {
            form.update { it.copy(amountError = if (f.amount.isBlank()) "Enter an amount" else "Amount must be greater than zero") }
            return
        }
        form.update { it.copy(saving = true, error = null, amountError = null) }
        viewModelScope.launch {
            val draft = EntryDraft(paise, f.type, f.category, f.note, f.date, f.goalId)
            val error = try {
                repo.saveEntry(draft, editingId)
            } catch (e: Exception) {
                "Couldn't save. Please try again."
            }
            if (error != null) {
                form.update { it.copy(saving = false, error = error) }
            } else {
                form.update { it.copy(saving = false, done = true) }
                val amount = Money.formatInr(paise)
                container.messenger.show(
                    when {
                        editingId != null -> "Changes saved"
                        f.type == EntryType.WITHDRAWAL -> "Withdrawal of $amount recorded"
                        else -> "Saved $amount"
                    },
                )
            }
        }
    }

    fun delete() {
        val id = editingId ?: return
        if (form.value.saving || form.value.done) return
        form.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val result = repo.deleteEntry(id)) {
                is DeleteResult.Failed -> form.update { it.copy(saving = false, error = result.message) }
                is DeleteResult.Deleted -> {
                    form.update { it.copy(saving = false, done = true) }
                    container.messenger.show("Entry deleted", actionLabel = "Undo") {
                        container.appScope.launch {
                            repo.restoreEntry(result.entry)?.let { container.messenger.show(it) }
                        }
                    }
                }
            }
        }
    }
}
