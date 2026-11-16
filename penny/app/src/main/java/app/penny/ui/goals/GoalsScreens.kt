package app.penny.ui.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.penny.core.Money
import app.penny.core.Periods
import app.penny.domain.EntryType
import app.penny.domain.GoalProgress
import app.penny.ui.common.AmountField
import app.penny.ui.common.ConfirmDialog
import app.penny.ui.common.DetailTopBar
import app.penny.ui.common.EmptyState
import app.penny.ui.common.EntryRow
import app.penny.ui.common.ErrorState
import app.penny.ui.common.Hairline
import app.penny.ui.common.LoadingState
import app.penny.ui.common.MoneyText
import app.penny.ui.common.Overline
import app.penny.ui.common.PennyCard
import app.penny.ui.common.PennyDatePickerDialog
import app.penny.ui.common.PrimaryButton
import app.penny.ui.common.ProgressLine
import app.penny.ui.common.ScreenHeader
import app.penny.ui.common.ScreenPadding
import app.penny.ui.common.SecondaryButton
import app.penny.ui.common.SectionHeader
import app.penny.ui.common.SelectPill
import app.penny.ui.common.pennyTextFieldColors
import app.penny.ui.common.pennyViewModel
import app.penny.ui.home.BalanceText
import app.penny.ui.icons.PennyIcons
import app.penny.ui.theme.PennyText
import java.time.LocalDate
import kotlin.math.roundToInt

private fun percentLabel(p: GoalProgress): String {
    val pct = p.percent
    return when {
        pct >= 100 -> "${pct.roundToInt()}%"
        pct > 0 && pct < 1 -> "<1%"
        else -> "${pct.toInt()}%" // round down so a goal never shows 100% before it is reached
    }
}

private fun deadlineLabel(date: LocalDate, today: LocalDate): String {
    val days = Periods.daysBetween(today, date)
    return when {
        days < 0 -> "Target date passed · ${Periods.formatFull(date)}"
        days == 0L -> "Due today"
        days == 1L -> "By ${Periods.formatFull(date)} · 1 day left"
        else -> "By ${Periods.formatFull(date)} · $days days left"
    }
}

// ---------------- List ----------------

@Composable
fun GoalsScreen(onOpenGoal: (Long) -> Unit, onCreateGoal: () -> Unit) {
    val vm = pennyViewModel { GoalsViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            "Goals",
            actions = { IconButton(onClick = onCreateGoal) { Icon(PennyIcons.Plus, contentDescription = "New goal") } },
        )
        when {
            state.loading -> LoadingState()
            state.error != null -> ErrorState(state.error!!)
            state.goals.isEmpty() -> EmptyState(
                PennyIcons.Goals,
                "Save with a purpose",
                "Create a goal — a new phone, an emergency fund, a trip — then assign savings to it to watch it fill up.",
                actionLabel = "Create a goal",
                onAction = onCreateGoal,
            )
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item(key = "summary") {
                    PennyCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ScreenPadding, vertical = 8.dp),
                    ) {
                        Row {
                            Column(Modifier.weight(1f)) {
                                Overline("In goals")
                                Spacer(Modifier.height(4.dp))
                                MoneyText(state.allocatedPaise, style = PennyText.amountLarge)
                            }
                            Column(Modifier.weight(1f)) {
                                Overline("Unassigned")
                                Spacer(Modifier.height(4.dp))
                                MoneyText(state.unassignedPaise, style = PennyText.amountLarge)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Each saving counts towards at most one goal, so these add up to your total of ${Money.formatInr(state.totalPaise)}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item(key = "header") { SectionHeader("Your goals") }
                items(state.goals, key = { it.goal.id }) { p ->
                    GoalCard(p, state.today, onClick = { onOpenGoal(p.goal.id) })
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
internal fun GoalCard(p: GoalProgress, today: LocalDate, onClick: () -> Unit) {
    PennyCard(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p.goal.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1)
            Text(
                percentLabel(p),
                style = MaterialTheme.typography.labelLarge,
                color = if (p.isReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        p.goal.targetDate?.let {
            Spacer(Modifier.height(2.dp))
            Text(deadlineLabel(it, today), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(14.dp))
        ProgressLine(p.fraction)
        Spacer(Modifier.height(10.dp))
        Row {
            Text(
                "${Money.formatInr(p.allocatedPaise)} of ${Money.formatInr(p.goal.targetPaise)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (p.isReached) "Reached" else "${Money.formatInr(p.remainingPaise)} to go",
                style = MaterialTheme.typography.bodySmall,
                color = if (p.isReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------- Detail ----------------

@Composable
fun GoalDetailScreen(
    goalId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddToGoal: (EntryType) -> Unit,
    onOpenEntry: (Long) -> Unit,
) {
    val vm = pennyViewModel(key = "goal-$goalId") { GoalDetailViewModel(it, goalId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val p = state.progress

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(
            title = p?.goal?.title ?: "Goal",
            onBack = onBack,
            actions = {
                if (p != null) IconButton(onClick = onEdit) { Icon(PennyIcons.Edit, contentDescription = "Edit goal") }
            },
        )
        when {
            state.loading -> LoadingState()
            state.missing || p == null -> EmptyState(PennyIcons.Goals, "Goal not found", "It may have been deleted.", actionLabel = "Go back", onAction = onBack)
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item(key = "hero") {
                    Column(Modifier.padding(horizontal = ScreenPadding, vertical = 12.dp)) {
                        Overline("Saved towards goal")
                        Spacer(Modifier.height(6.dp))
                        BalanceText(p.allocatedPaise, MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "of ${Money.formatInr(p.goal.targetPaise)} · ${percentLabel(p)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(18.dp))
                        ProgressLine(p.fraction, height = 6.dp)
                        Spacer(Modifier.height(20.dp))
                        Row {
                            DetailStat("Remaining", Modifier.weight(1f)) { MoneyText(p.remainingPaise, style = PennyText.amount) }
                            DetailStat("Entries", Modifier.weight(1f)) { Text(p.entryCount.toString(), style = PennyText.amount) }
                        }
                        p.goal.targetDate?.let { date ->
                            Spacer(Modifier.height(16.dp))
                            DetailStat("Target date") {
                                Text(deadlineLabel(date, state.today), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        state.monthlyNeededPaise?.let { needed ->
                            Spacer(Modifier.height(16.dp))
                            PennyCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                                Text(
                                    "Save ${Money.formatInr(needed)} a month to reach this goal on time.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        if (p.isReached) {
                            Spacer(Modifier.height(16.dp))
                            Text("Goal reached. Well done.", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PrimaryButton("Add to goal", { onAddToGoal(EntryType.DEPOSIT) }, Modifier.weight(1.4f), icon = PennyIcons.Plus)
                            SecondaryButton(
                                "Withdraw",
                                { onAddToGoal(EntryType.WITHDRAWAL) },
                                Modifier.weight(1f),
                                enabled = p.allocatedPaise > 0,
                                icon = PennyIcons.Minus,
                            )
                        }
                    }
                }
                item(key = "entriesHeader") { SectionHeader("Entries for this goal") }
                if (state.entries.isEmpty()) {
                    item(key = "noEntries") {
                        Text(
                            "No savings assigned yet. Use “Add to goal”, or pick this goal when adding savings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 8.dp),
                        )
                    }
                } else {
                    items(state.entries, key = { it.id }) { e ->
                        EntryRow(e, goalTitle = null, subtitle = Periods.dayHeader(e.date, state.today), onClick = { onOpenEntry(e.id) })
                        Hairline(inset = ScreenPadding + 50.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailStat(label: String, modifier: Modifier = Modifier, value: @Composable () -> Unit) {
    Column(modifier) {
        Overline(label)
        Spacer(Modifier.height(4.dp))
        value()
    }
}

// ---------------- Editor ----------------

private val suggestions = listOf("New Phone", "Emergency Fund", "Travel", "Laptop", "Wedding", "Education")

@Composable
fun GoalEditorScreen(editingId: Long?, onClose: () -> Unit, onDeleted: () -> Unit) {
    val vm = pennyViewModel(key = "goal-editor-$editingId") { GoalEditorViewModel(it, editingId) }
    val form by vm.form.collectAsStateWithLifecycle()
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(form.done) { if (form.done) onClose() }
    LaunchedEffect(form.deleted) { if (form.deleted) onDeleted() }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(
            title = if (editingId == null) "New goal" else "Edit goal",
            onBack = onClose,
            backIcon = PennyIcons.Close,
            backDescription = "Cancel",
            actions = {
                if (editingId != null && !form.loading && !form.missing) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(PennyIcons.Delete, contentDescription = "Delete goal", tint = MaterialTheme.colorScheme.error)
                    }
                }
            },
        )
        when {
            form.loading -> LoadingState()
            form.missing -> EmptyState(PennyIcons.Goals, "Goal not found", "It may have been deleted.", actionLabel = "Go back", onAction = onClose)
            else -> Column(Modifier.fillMaxSize().imePadding()) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = ScreenPadding),
                ) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = form.title,
                        onValueChange = vm::setTitle,
                        label = { Text("Goal name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        colors = pennyTextFieldColors(),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (editingId == null) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            suggestions.forEach { s -> SelectPill(s, selected = form.title == s, onClick = { vm.setTitle(s) }) }
                        }
                    }

                    Spacer(Modifier.height(28.dp))
                    Overline("Target amount")
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = form.target, onValueChange = vm::setTarget, onDone = vm::save)

                    Spacer(Modifier.height(20.dp))
                    Overline("Target date (optional)")
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button, onClick = { pickDate = true })
                            .heightIn(min = 52.dp)
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(PennyIcons.Calendar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            form.targetDate?.let(Periods::formatFull) ?: "No target date",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (form.targetDate == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (form.targetDate != null) {
                            TextButton(onClick = { vm.setTargetDate(null) }) { Text("Clear") }
                        } else {
                            Icon(PennyIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                    Hairline()

                    form.error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(24.dp))
                }
                Hairline()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ScreenPadding, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SecondaryButton("Cancel", onClose, Modifier.weight(1f), enabled = !form.saving)
                    PrimaryButton(
                        if (editingId == null) "Create goal" else "Save changes",
                        vm::save,
                        Modifier.weight(1.6f),
                        loading = form.saving,
                        enabled = !form.done,
                    )
                }
            }
        }
    }

    if (pickDate) {
        PennyDatePickerDialog(
            initial = form.targetDate ?: vm.today.plusMonths(6),
            minDate = vm.today,
            onPick = { vm.setTargetDate(it); pickDate = false },
            onDismiss = { pickDate = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this goal?",
            text = if (form.allocatedPaise > 0) {
                "The ${Money.formatInr(form.allocatedPaise)} assigned to it stays in your total savings as unassigned money. Entries are not deleted."
            } else {
                "This goal will be removed. Your entries are not affected."
            },
            confirmLabel = "Delete goal",
            destructive = true,
            onConfirm = { confirmDelete = false; vm.delete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
