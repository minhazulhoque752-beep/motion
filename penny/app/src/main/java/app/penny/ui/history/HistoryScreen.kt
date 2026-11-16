package app.penny.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.penny.core.Money
import app.penny.core.PeriodType
import app.penny.ui.common.EmptyState
import app.penny.ui.common.EntryRow
import app.penny.ui.common.ErrorState
import app.penny.ui.common.Hairline
import app.penny.ui.common.LoadingState
import app.penny.ui.common.MoneyText
import app.penny.ui.common.Overline
import app.penny.ui.common.ScreenHeader
import app.penny.ui.common.ScreenPadding
import app.penny.ui.common.SelectPill
import app.penny.ui.common.pennyTextFieldColors
import app.penny.ui.common.pennyViewModel
import app.penny.ui.icons.PennyIcons
import app.penny.ui.theme.PennyTheme

@Composable
fun HistoryScreen(onOpenEntry: (Long) -> Unit, onAdd: () -> Unit) {
    val vm = pennyViewModel { HistoryViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    HistoryContent(
        state = state,
        onQuery = vm::setQuery,
        onPeriod = vm::setPeriod,
        onStep = vm::step,
        onOpenEntry = onOpenEntry,
        onAdd = onAdd,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HistoryContent(
    state: HistoryUiState,
    onQuery: (String) -> Unit,
    onPeriod: (PeriodType) -> Unit,
    onStep: (Long) -> Unit,
    onOpenEntry: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            "History",
            actions = {
                IconButton(onClick = onAdd) { Icon(PennyIcons.Plus, contentDescription = "Add entry") }
            },
        )

        if (state.loading) {
            LoadingState(); return@Column
        }
        state.error?.let { ErrorState(it); return@Column }
        if (!state.hasAnyEntries) {
            EmptyState(
                PennyIcons.History,
                "No entries yet",
                "Everything you save or withdraw will be listed here, newest first.",
                actionLabel = "Add savings",
                onAction = onAdd,
            )
            return@Column
        }

        OutlinedTextField(
            value = state.filter.query,
            onValueChange = onQuery,
            placeholder = { Text("Search notes, categories, goals, amounts") },
            leadingIcon = { Icon(PennyIcons.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = if (state.filter.query.isNotEmpty()) {
                { IconButton(onClick = { onQuery("") }) { Icon(PennyIcons.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp)) } }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            colors = pennyTextFieldColors(),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding, vertical = 4.dp),
        )

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PeriodType.entries.forEach { p ->
                SelectPill(p.label, selected = state.filter.period == p, onClick = { onPeriod(p) })
            }
        }

        if (state.filter.period != PeriodType.ALL) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onStep(-1) }) { Icon(PennyIcons.ChevronLeft, contentDescription = "Previous ${state.filter.period.label.lowercase()}") }
                Text(
                    state.periodLabel,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onStep(1) }, enabled = state.canGoForward) {
                    Icon(PennyIcons.ChevronRight, contentDescription = "Next ${state.filter.period.label.lowercase()}")
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Overline(if (state.matchCount == 1) "1 entry" else "${state.matchCount} entries", Modifier.weight(1f))
            Text(
                buildString {
                    append("Saved ${Money.formatInr(state.savedPaise)}")
                    if (state.withdrawnPaise > 0) append("  ·  Out ${Money.formatInr(state.withdrawnPaise)}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Hairline()

        if (state.groups.isEmpty()) {
            EmptyState(
                PennyIcons.Search,
                "Nothing here",
                if (state.filter.query.isNotBlank()) "No entries match “${state.filter.query}” in this period." else "No entries in this period.",
            )
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            state.groups.forEach { group ->
                stickyHeader(key = "h-${group.date}") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = ScreenPadding, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Overline(group.header, Modifier.weight(1f))
                        MoneyText(
                            group.netPaise,
                            signed = true,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (group.netPaise < 0) PennyTheme.colors.negative else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(group.entries, key = { it.id }) { entry ->
                    EntryRow(
                        entry = entry,
                        goalTitle = entry.goalId?.let { state.goalTitles[it] },
                        onClick = { onOpenEntry(entry.id) },
                    )
                    Hairline(inset = ScreenPadding + 50.dp)
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
