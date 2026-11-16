package app.penny.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.penny.AppContainer
import app.penny.core.Money
import app.penny.domain.Entry
import app.penny.domain.EntryValidator
import app.penny.domain.GoalProgress
import app.penny.domain.SavingsCalculator
import app.penny.ui.common.todayFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

// ---------- Goals list ----------

data class GoalsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val goals: List<GoalProgress> = emptyList(),
    val totalPaise: Long = 0,
    val allocatedPaise: Long = 0,
    val unassignedPaise: Long = 0,
    val today: LocalDate = LocalDate.now(),
)

class GoalsViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<GoalsUiState> = combine(
        container.repository.entries,
        container.repository.goals,
        todayFlow(container.clock),
    ) { entries, goals, today ->
        val progress = SavingsCalculator.goalProgress(goals, entries)
        GoalsUiState(
            loading = false,
            goals = progress,
            totalPaise = SavingsCalculator.total(entries),
            allocatedPaise = progress.sumOf { it.allocatedPaise },
            unassignedPaise = SavingsCalculator.unassigned(entries),
            today = today,
        )
    }
        .catch { emit(GoalsUiState(loading = false, error = "Couldn't load your goals.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())
}

// ---------- Goal detail ----------

data class GoalDetailUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val progress: GoalProgress? = null,
    val entries: List<Entry> = emptyList(),
    val monthlyNeededPaise: Long? = null,
    val today: LocalDate = LocalDate.now(),
)

class GoalDetailViewModel(container: AppContainer, goalId: Long) : ViewModel() {
    val state: StateFlow<GoalDetailUiState> = combine(
        container.repository.entries,
        container.repository.goals,
        todayFlow(container.clock),
    ) { entries, goals, today ->
        val goal = goals.firstOrNull { it.id == goalId }
        if (goal == null) {
            GoalDetailUiState(loading = false, missing = true, today = today)
        } else {
            val progress = SavingsCalculator.goalProgress(listOf(goal), entries).first()
            GoalDetailUiState(
                loading = false,
                progress = progress,
                entries = SavingsCalculator.sortNewestFirst(entries.filter { it.goalId == goalId }),
                monthlyNeededPaise = SavingsCalculator.monthlyNeeded(progress, today),
                today = today,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalDetailUiState())
}

// ---------- Goal editor ----------

data class GoalForm(
    val loading: Boolean = false,
    val missing: Boolean = false,
    val title: String = "",
    val target: String = "",
    val targetDate: LocalDate? = null,
    val error: String? = null,
    val saving: Boolean = false,
    val done: Boolean = false,
    val deleted: Boolean = false,
    /** Money currently assigned to this goal; shown when deleting so the user knows it stays in their total. */
    val allocatedPaise: Long = 0,
)

class GoalEditorViewModel(private val container: AppContainer, private val editingId: Long?) : ViewModel() {
    private val repo = container.repository
    private val _form = MutableStateFlow(GoalForm(loading = editingId != null))
    val form: StateFlow<GoalForm> = _form
    val today: LocalDate get() = LocalDate.now(container.clock)

    init {
        if (editingId != null) {
            viewModelScope.launch {
                val goal = repo.goals.first().firstOrNull { it.id == editingId }
                val allocated = repo.entries.first().filter { it.goalId == editingId }.sumOf { it.signedPaise }
                _form.value = if (goal == null) GoalForm(missing = true)
                else GoalForm(
                    title = goal.title,
                    target = Money.toInputString(goal.targetPaise),
                    targetDate = goal.targetDate,
                    allocatedPaise = allocated,
                )
            }
        }
    }

    fun setTitle(v: String) = _form.update { it.copy(title = v.take(EntryValidator.MAX_GOAL_TITLE_LENGTH), error = null) }
    fun setTarget(v: String) = _form.update { it.copy(target = v, error = null) }
    fun setTargetDate(d: LocalDate?) = _form.update { it.copy(targetDate = d, error = null) }

    fun save() {
        val f = _form.value
        if (f.saving || f.done || f.loading) return
        val paise = Money.parseToPaise(f.target)
        EntryValidator.validateGoal(f.title, paise)?.let { msg -> _form.update { it.copy(error = msg) }; return }
        _form.update { it.copy(saving = true) }
        viewModelScope.launch {
            val error = try {
                repo.saveGoal(editingId, f.title, paise, f.targetDate)
            } catch (e: Exception) {
                "Couldn't save the goal. Please try again."
            }
            if (error != null) {
                _form.update { it.copy(saving = false, error = error) }
            } else {
                _form.update { it.copy(saving = false, done = true) }
                container.messenger.show(if (editingId == null) "Goal “${f.title.trim()}” created" else "Goal updated")
            }
        }
    }

    fun delete() {
        val id = editingId ?: return
        if (_form.value.saving) return
        _form.update { it.copy(saving = true) }
        viewModelScope.launch {
            repo.deleteGoal(id)
            _form.update { it.copy(saving = false, deleted = true) }
            container.messenger.show("Goal deleted. Its savings are now unassigned.")
        }
    }
}
