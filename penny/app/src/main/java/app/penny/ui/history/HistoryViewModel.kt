package app.penny.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.penny.AppContainer
import app.penny.core.PeriodType
import app.penny.core.Periods
import app.penny.domain.Entry
import app.penny.domain.SavingsCalculator
import app.penny.ui.common.todayFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class HistoryFilter(
    val query: String = "",
    val period: PeriodType = PeriodType.ALL,
    /** Any date inside the selected period; null means "the current period". */
    val anchor: LocalDate? = null,
)

data class DayGroup(val date: LocalDate, val header: String, val entries: List<Entry>, val netPaise: Long)

data class HistoryUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val filter: HistoryFilter = HistoryFilter(),
    val periodLabel: String = "",
    val canGoForward: Boolean = false,
    val groups: List<DayGroup> = emptyList(),
    val matchCount: Int = 0,
    val savedPaise: Long = 0,
    val withdrawnPaise: Long = 0,
    val hasAnyEntries: Boolean = false,
    val goalTitles: Map<Long, String> = emptyMap(),
)

class HistoryViewModel(container: AppContainer) : ViewModel() {
    private val clock = container.clock
    private val filter = MutableStateFlow(HistoryFilter())

    val state: StateFlow<HistoryUiState> = combine(
        container.repository.entries,
        container.repository.goals,
        filter,
        todayFlow(container.clock),
    ) { entries, goals, f, today ->
        val anchor = f.anchor ?: today
        val range = Periods.range(f.period, anchor)
        val titles = goals.associate { it.id to it.title }
        val matches = SavingsCalculator.sortNewestFirst(SavingsCalculator.filter(entries, range, f.query, titles))
        HistoryUiState(
            loading = false,
            filter = f,
            periodLabel = Periods.label(f.period, anchor, today),
            canGoForward = !Periods.isCurrentOrFuture(f.period, anchor, today),
            groups = matches.groupBy { it.date }.map { (date, list) ->
                DayGroup(date, Periods.dayHeader(date, today), list, list.sumOf { it.signedPaise })
            },
            matchCount = matches.size,
            savedPaise = matches.filter { !it.isWithdrawal }.sumOf { it.amountPaise },
            withdrawnPaise = matches.filter { it.isWithdrawal }.sumOf { it.amountPaise },
            hasAnyEntries = entries.isNotEmpty(),
            goalTitles = titles,
        )
    }
        .catch { emit(HistoryUiState(loading = false, error = "Couldn't load your history.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setQuery(q: String) = filter.update { it.copy(query = q.take(60)) }

    fun setPeriod(p: PeriodType) = filter.update { it.copy(period = p, anchor = null) }

    fun step(steps: Long) = filter.update { f ->
        val base = f.anchor ?: LocalDate.now(clock)
        f.copy(anchor = Periods.shift(f.period, base, steps))
    }

    fun resetToCurrent() = filter.update { it.copy(anchor = null) }
}
