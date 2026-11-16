package app.penny.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.penny.AppContainer
import app.penny.domain.CategoryShare
import app.penny.domain.MonthComparison
import app.penny.domain.MonthPoint
import app.penny.domain.SavingsCalculator
import app.penny.ui.common.todayFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class InsightsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val hasEntries: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val totalPaise: Long = 0,
    val monthPaise: Long = 0,
    val weekPaise: Long = 0,
    val todayPaise: Long = 0,
    val dailyAveragePaise: Long = 0,
    val months: Int = 6,
    val series: List<MonthPoint> = emptyList(),
    val comparison: MonthComparison? = null,
    val best: MonthPoint? = null,
    val categories: List<CategoryShare> = emptyList(),
)

class InsightsViewModel(container: AppContainer) : ViewModel() {
    private val months = MutableStateFlow(6)

    val state: StateFlow<InsightsUiState> = combine(
        container.repository.entries,
        months,
        todayFlow(container.clock),
    ) { entries, count, today ->
        val month = YearMonth.from(today)
        InsightsUiState(
            loading = false,
            hasEntries = entries.isNotEmpty(),
            today = today,
            totalPaise = SavingsCalculator.total(entries),
            monthPaise = SavingsCalculator.netForMonth(entries, month),
            weekPaise = SavingsCalculator.netForWeek(entries, today),
            todayPaise = SavingsCalculator.netForDay(entries, today),
            dailyAveragePaise = SavingsCalculator.dailyAverageThisMonth(entries, today),
            months = count,
            series = SavingsCalculator.monthlySeries(entries, month, count),
            comparison = SavingsCalculator.monthComparison(entries, month),
            best = SavingsCalculator.bestMonth(entries),
            categories = SavingsCalculator.categoryBreakdown(entries),
        )
    }
        .catch { emit(InsightsUiState(loading = false, error = "Couldn't calculate insights.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    fun setMonths(count: Int) {
        months.value = count
    }
}
