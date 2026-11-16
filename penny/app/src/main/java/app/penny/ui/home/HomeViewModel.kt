package app.penny.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.penny.AppContainer
import app.penny.core.Periods
import app.penny.domain.Entry
import app.penny.domain.SavingsCalculator
import app.penny.ui.common.hourFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val greeting: String = "",
    val name: String = "",
    val dateLabel: String = "",
    val today: LocalDate = LocalDate.now(),
    val totalPaise: Long = 0,
    val monthPaise: Long = 0,
    val monthName: String = "",
    val entryCount: Int = 0,
    val monthlyTargetPaise: Long? = null,
    val daysLeftInMonth: Int = 0,
    val recent: List<Entry> = emptyList(),
    val goalTitles: Map<Long, String> = emptyMap(),
    val goalCount: Int = 0,
    val goalsAllocatedPaise: Long = 0,
    val goalsTargetPaise: Long = 0,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        container.repository.entries,
        container.repository.goals,
        container.preferences.prefs,
        hourFlow(container.clock),
    ) { entries, goals, prefs, now ->
        val today = now.toLocalDate()
        val month = YearMonth.from(today)
        val progress = SavingsCalculator.goalProgress(goals, entries)
        HomeUiState(
            loading = false,
            greeting = Periods.greeting(now.hour),
            name = prefs.name,
            dateLabel = Periods.greetingDate(today),
            today = today,
            totalPaise = SavingsCalculator.total(entries),
            monthPaise = SavingsCalculator.netForMonth(entries, month),
            monthName = month.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH),
            entryCount = entries.size,
            monthlyTargetPaise = prefs.monthlyTargetPaise,
            daysLeftInMonth = month.lengthOfMonth() - today.dayOfMonth,
            recent = SavingsCalculator.sortNewestFirst(entries).take(5),
            goalTitles = goals.associate { it.id to it.title },
            goalCount = goals.size,
            goalsAllocatedPaise = progress.sumOf { it.allocatedPaise.coerceAtMost(it.goal.targetPaise) },
            goalsTargetPaise = goals.sumOf { it.targetPaise },
        )
    }
        .catch { emit(HomeUiState(loading = false, error = "Couldn't load your savings. Please restart Penny.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setMonthlyTarget(paise: Long?) {
        viewModelScope.launch { container.preferences.setMonthlyTarget(paise) }
    }
}
