package app.penny.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.penny.core.Money
import app.penny.domain.EntryType
import app.penny.ui.common.AmountInputDialog
import app.penny.ui.common.EmptyState
import app.penny.ui.common.EntryRow
import app.penny.ui.common.ErrorState
import app.penny.ui.common.Hairline
import app.penny.ui.common.LoadingState
import app.penny.ui.common.MoneyText
import app.penny.ui.common.Overline
import app.penny.ui.common.PennyCard
import app.penny.ui.common.PrimaryButton
import app.penny.ui.common.ProgressLine
import app.penny.ui.common.ScreenPadding
import app.penny.ui.common.SecondaryButton
import app.penny.ui.common.SectionHeader
import app.penny.ui.common.pennyViewModel
import app.penny.ui.icons.PennyIcons
import app.penny.ui.theme.PennyText
import app.penny.ui.theme.PennyTheme
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    onAdd: (EntryType) -> Unit,
    onOpenEntry: (Long) -> Unit,
    onSeeAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGoals: () -> Unit,
) {
    val vm = pennyViewModel { HomeViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    var editTarget by rememberSaveable { mutableStateOf(false) }

    when {
        state.loading -> LoadingState()
        state.error != null -> ErrorState(state.error!!)
        else -> HomeContent(
            state = state,
            onAdd = onAdd,
            onOpenEntry = onOpenEntry,
            onSeeAll = onSeeAll,
            onOpenSettings = onOpenSettings,
            onOpenGoals = onOpenGoals,
            onEditTarget = { editTarget = true },
        )
    }

    if (editTarget) {
        AmountInputDialog(
            title = "Monthly savings target",
            initialPaise = state.monthlyTargetPaise,
            allowClear = true,
            supporting = "How much you aim to save each month. Leave empty to remove.",
            onConfirm = { vm.setMonthlyTarget(it); editTarget = false },
            onDismiss = { editTarget = false },
        )
    }
}

@Composable
internal fun HomeContent(
    state: HomeUiState,
    onAdd: (EntryType) -> Unit,
    onOpenEntry: (Long) -> Unit,
    onSeeAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGoals: () -> Unit,
    onEditTarget: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = ScreenPadding, end = 8.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Overline(state.dateLabel)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (state.name.isBlank()) state.greeting else "${state.greeting}, ${state.name}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(PennyIcons.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        item(key = "balance") {
            Column(Modifier.padding(horizontal = ScreenPadding).padding(top = 32.dp)) {
                Overline("Total savings")
                Spacer(Modifier.height(6.dp))
                BalanceText(state.totalPaise, MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Stat(label = "This month", modifier = Modifier.weight(1f)) {
                        MoneyText(
                            state.monthPaise,
                            style = PennyText.amountLarge,
                            color = if (state.monthPaise < 0) PennyTheme.colors.negative else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    VerticalDivider(Modifier.fillMaxHeight().padding(horizontal = 16.dp), color = PennyTheme.colors.hairline)
                    Stat(label = "Entries", modifier = Modifier.weight(1f)) {
                        Text(state.entryCount.toString(), style = PennyText.amountLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton("Add savings", { onAdd(EntryType.DEPOSIT) }, Modifier.weight(1.4f), icon = PennyIcons.Plus)
                    SecondaryButton(
                        "Withdraw",
                        { onAdd(EntryType.WITHDRAWAL) },
                        Modifier.weight(1f),
                        enabled = state.totalPaise > 0,
                        icon = PennyIcons.Minus,
                    )
                }
            }
        }

        item(key = "progress") {
            Column(Modifier.padding(horizontal = ScreenPadding).padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MonthlyTargetCard(state, onEditTarget)
                if (state.goalCount > 0) GoalsSummaryCard(state, onOpenGoals)
            }
        }

        item(key = "recentHeader") {
            Spacer(Modifier.height(20.dp))
            SectionHeader(
                "Recent activity",
                actionLabel = if (state.entryCount > 0) "See all" else null,
                onAction = onSeeAll,
            )
        }

        if (state.recent.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = PennyIcons.Wallet,
                    title = "Your ledger is empty",
                    body = "Record your first saving and Penny will start tracking your progress.",
                    actionLabel = "Add your first saving",
                    onAction = { onAdd(EntryType.DEPOSIT) },
                )
            }
        } else {
            items(state.recent, key = { it.id }) { entry ->
                EntryRow(
                    entry = entry,
                    goalTitle = entry.goalId?.let { state.goalTitles[it] },
                    subtitle = app.penny.core.Periods.dayHeader(entry.date, state.today),
                    onClick = { onOpenEntry(entry.id) },
                )
                Hairline(inset = ScreenPadding + 50.dp)
            }
        }
    }
}

@Composable
private fun Stat(label: String, modifier: Modifier = Modifier, value: @Composable () -> Unit) {
    Column(modifier) {
        Overline(label)
        Spacer(Modifier.height(4.dp))
        value()
    }
}

/** Rupees in the display face, paise smaller and quieter — an editorial treatment for the headline figure. */
@Composable
fun BalanceText(paise: Long, style: TextStyle, modifier: Modifier = Modifier) {
    val formatted = Money.formatInr(paise)
    val dot = formatted.lastIndexOf('.')
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val text = buildAnnotatedString {
        append(formatted.substring(0, dot))
        withStyle(SpanStyle(color = muted, fontSize = style.fontSize * 0.55f)) { append(formatted.substring(dot)) }
    }
    Text(
        text,
        style = style,
        color = if (paise < 0) PennyTheme.colors.negative else MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        modifier = modifier.semantics { contentDescription = formatted },
    )
}

@Composable
private fun MonthlyTargetCard(state: HomeUiState, onEditTarget: () -> Unit) {
    val target = state.monthlyTargetPaise
    PennyCard(onClick = onEditTarget) {
        if (target == null) {
            Overline("Savings progress")
            Spacer(Modifier.height(8.dp))
            Text("Set a monthly target", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick an amount to aim for each month and Penny will show how close you are.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onEditTarget, contentPadding = PaddingValues(0.dp)) { Text("Set target") }
        } else {
            val saved = state.monthPaise.coerceAtLeast(0)
            val fraction = (saved.toDouble() / target).toFloat()
            val pct = (saved * 100.0 / target).roundToInt()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Overline("${state.monthName} target", Modifier.weight(1f))
                Text("$pct%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                MoneyText(saved, style = PennyText.amountLarge)
                Spacer(Modifier.width(6.dp))
                Text(
                    "of ${Money.formatInr(target)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            ProgressLine(fraction)
            Spacer(Modifier.height(10.dp))
            val remaining = target - saved
            Text(
                if (remaining <= 0) "Target reached for ${state.monthName}. Well done."
                else "${Money.formatInr(remaining)} to go · ${dayCount(state.daysLeftInMonth)} left",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun dayCount(days: Int) = when (days) {
    0 -> "last day"
    1 -> "1 day"
    else -> "$days days"
}

@Composable
private fun GoalsSummaryCard(state: HomeUiState, onOpenGoals: () -> Unit) {
    val fraction = if (state.goalsTargetPaise > 0) (state.goalsAllocatedPaise.toDouble() / state.goalsTargetPaise).toFloat() else 0f
    PennyCard(onClick = onOpenGoals) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline(if (state.goalCount == 1) "1 goal" else "${state.goalCount} goals", Modifier.weight(1f))
            Text("${(fraction * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Icon(PennyIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 2.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "${Money.formatInr(state.goalsAllocatedPaise)} of ${Money.formatInr(state.goalsTargetPaise)} across your goals",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        ProgressLine(fraction)
    }
}
