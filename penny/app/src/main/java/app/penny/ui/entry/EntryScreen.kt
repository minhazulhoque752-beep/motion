package app.penny.ui.entry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import app.penny.domain.Category
import app.penny.domain.EntryType
import app.penny.domain.EntryValidator
import app.penny.ui.common.AmountField
import app.penny.ui.common.ConfirmDialog
import app.penny.ui.common.DetailTopBar
import app.penny.ui.common.EmptyState
import app.penny.ui.common.Hairline
import app.penny.ui.common.LoadingState
import app.penny.ui.common.Overline
import app.penny.ui.common.PennyDatePickerDialog
import app.penny.ui.common.PrimaryButton
import app.penny.ui.common.ScreenPadding
import app.penny.ui.common.SecondaryButton
import app.penny.ui.common.Segmented
import app.penny.ui.common.SelectPill
import app.penny.ui.common.pennyTextFieldColors
import app.penny.ui.common.pennyViewModel
import app.penny.ui.icons.PennyIcons
import app.penny.ui.theme.PennyTheme

@Composable
fun EntryScreen(
    editingId: Long?,
    initialType: EntryType,
    initialGoalId: Long?,
    onClose: () -> Unit,
) {
    val vm = pennyViewModel(key = "entry-$editingId-$initialType-$initialGoalId") {
        EntryViewModel(it, editingId, initialType, initialGoalId)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val form = state.form
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    // Close exactly once after a successful save/delete.
    LaunchedEffect(form.done) { if (form.done) onClose() }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(
            title = when {
                editingId != null -> "Edit entry"
                form.type == EntryType.WITHDRAWAL -> "Withdraw"
                else -> "Add savings"
            },
            onBack = onClose,
            backIcon = PennyIcons.Close,
            backDescription = "Cancel",
            actions = {
                if (editingId != null && !form.notFound && !form.loading) {
                    IconButton(onClick = { confirmDelete = true }, enabled = !form.saving) {
                        Icon(PennyIcons.Delete, contentDescription = "Delete entry", tint = MaterialTheme.colorScheme.error)
                    }
                }
            },
        )

        when {
            form.loading -> LoadingState()
            form.notFound -> EmptyState(
                PennyIcons.Info,
                "Entry not found",
                "It may have been deleted.",
                actionLabel = "Go back",
                onAction = onClose,
            )
            else -> EntryFormContent(
                state = state,
                isEditing = editingId != null,
                vm = vm,
                onPickDate = { pickDate = true },
                onCancel = onClose,
            )
        }
    }

    if (pickDate) {
        PennyDatePickerDialog(
            initial = form.date,
            maxDate = state.today,
            onPick = { vm.setDate(it); pickDate = false },
            onDismiss = { pickDate = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this entry?",
            text = "${Money.formatInr(Money.parseToPaise(form.amount) ?: 0)} on ${Periods.formatFull(form.date)} will be removed from your totals.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { confirmDelete = false; vm.delete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EntryFormContent(
    state: EntryUiState,
    isEditing: Boolean,
    vm: EntryFormActions,
    onPickDate: () -> Unit,
    onCancel: () -> Unit,
) {
    val form = state.form
    val isWithdrawal = form.type == EntryType.WITHDRAWAL
    val withdrawalColor = PennyTheme.colors.negative
    val savingColor = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxSize().imePadding()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding),
        ) {
            Spacer(Modifier.height(4.dp))
            Segmented(
                options = listOf(EntryType.DEPOSIT, EntryType.WITHDRAWAL),
                selected = form.type,
                label = { if (it == EntryType.DEPOSIT) "Saving" else "Withdrawal" },
                onSelect = vm::setType,
                selectedColor = { if (it == EntryType.WITHDRAWAL) withdrawalColor else savingColor },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(28.dp))
            Overline("Amount")
            Spacer(Modifier.height(8.dp))
            val goalName = form.goalId?.let { id -> state.goals.firstOrNull { it.id == id }?.title }
            AmountField(
                value = form.amount,
                onValueChange = vm::setAmount,
                error = form.amountError,
                hint = if (isWithdrawal) {
                    "Available ${if (goalName != null) "in $goalName" else "unassigned"}: ${Money.formatInr(state.availablePaise)}"
                } else null,
                autoFocus = !isEditing,
                onDone = vm::save,
            )

            Spacer(Modifier.height(20.dp))
            Overline("Date")
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClickLabel = "Change date", onClick = onPickDate)
                    .heightIn(min = 52.dp)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(PennyIcons.Calendar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    Periods.dayHeader(form.date, state.today).let { header ->
                        if (header == "Today" || header == "Yesterday") "$header · ${Periods.formatFull(form.date)}" else Periods.formatFull(form.date)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(PennyIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Hairline()

            Spacer(Modifier.height(20.dp))
            Overline("Category")
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { c ->
                    SelectPill(c.label, selected = form.category == c, onClick = { vm.setCategory(c) })
                }
            }

            Spacer(Modifier.height(24.dp))
            Overline(if (isWithdrawal) "Withdraw from" else "Assign to goal")
            Spacer(Modifier.height(10.dp))
            if (state.goals.isEmpty()) {
                Text(
                    if (isWithdrawal) "Unassigned savings" else "No goals yet — create one in the Goals tab to track progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectPill(if (isWithdrawal) "Unassigned" else "None", selected = form.goalId == null, onClick = { vm.setGoal(null) })
                    state.goals.forEach { g ->
                        SelectPill(g.title, selected = form.goalId == g.id, onClick = { vm.setGoal(g.id) })
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = form.note,
                onValueChange = vm::setNote,
                label = { Text("Note (optional)") },
                placeholder = { Text(if (isWithdrawal) "e.g. Laptop repair" else "e.g. Salary set-aside") },
                supportingText = { Text("${form.note.length} / ${EntryValidator.MAX_NOTE_LENGTH}") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                colors = pennyTextFieldColors(),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            if (form.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(form.error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
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
            SecondaryButton("Cancel", onCancel, Modifier.weight(1f), enabled = !form.saving)
            PrimaryButton(
                text = when {
                    isEditing -> "Save changes"
                    isWithdrawal -> "Record withdrawal"
                    else -> "Save"
                },
                onClick = vm::save,
                modifier = Modifier.weight(1.6f),
                loading = form.saving,
                enabled = !form.done,
            )
        }
    }
}
