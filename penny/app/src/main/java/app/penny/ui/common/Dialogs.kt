package app.penny.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.penny.core.Money
import app.penny.ui.theme.PennyTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
fun pennyTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = PennyTheme.colors.hairline,
    focusedContainerColor = PennyTheme.colors.raised,
    unfocusedContainerColor = PennyTheme.colors.raised,
    cursorColor = MaterialTheme.colorScheme.primary,
)

/**
 * Text entry dialog. [onConfirm] returns an error message to keep the dialog open, or null to accept.
 */
@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    label: String,
    onConfirm: (String) -> String?,
    onDismiss: () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLength: Int = 80,
    supporting: String? = null,
    prefix: String? = null,
    sanitize: (String) -> String = { it },
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = sanitize(it).take(maxLength); error = null },
                label = { Text(label) },
                prefix = prefix?.let { p -> { Text(p) } },
                singleLine = true,
                isError = error != null,
                supportingText = (error ?: supporting)?.let { t -> { Text(t) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { error = onConfirm(value) }),
                colors = pennyTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { error = onConfirm(value) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    )
}

/** Amount dialog in rupees, e.g. for the monthly target. Returns paise, or null to clear when [allowClear]. */
@Composable
fun AmountInputDialog(
    title: String,
    initialPaise: Long?,
    onConfirm: (Long?) -> Unit,
    onDismiss: () -> Unit,
    allowClear: Boolean = false,
    supporting: String? = null,
) {
    TextInputDialog(
        title = title,
        initial = initialPaise?.let(Money::toInputString).orEmpty(),
        label = "Amount",
        prefix = "₹ ",
        keyboardType = KeyboardType.Decimal,
        sanitize = Money::sanitizeInput,
        supporting = supporting ?: if (allowClear) "Leave empty to remove" else null,
        onConfirm = { text ->
            if (text.isBlank() && allowClear) {
                onConfirm(null); null
            } else {
                val paise = Money.parseToPaise(text)
                when {
                    paise == null -> "Enter a valid amount"
                    paise <= 0 -> "Amount must be greater than zero"
                    paise > Money.MAX_AMOUNT_PAISE -> "That's more than ${Money.formatInr(Money.MAX_AMOUNT_PAISE, decimals = false)}"
                    else -> { onConfirm(paise); null }
                }
            }
        },
        onDismiss = onDismiss,
    )
}

/**
 * Large editorial amount input: "₹" + digits, numeric keyboard, Indian-format preview underneath.
 */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
    autoFocus: Boolean = false,
    onDone: () -> Unit = {},
) {
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { focus.requestFocus() }
    val colors = MaterialTheme.colorScheme
    val big: TextStyle = MaterialTheme.typography.displayMedium.copy(color = colors.onSurface)
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("₹", style = big.copy(color = colors.onSurfaceVariant))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(Money.sanitizeInput(it)) },
                textStyle = big,
                singleLine = true,
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text("0", style = big.copy(color = colors.onSurfaceVariant.copy(alpha = 0.45f)))
                    inner()
                },
            )
        }
        Spacer(Modifier.height(10.dp))
        Hairline(Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        val parsed = Money.parseToPaise(value)
        val helper = error ?: hint ?: parsed?.takeIf { it > 0 }?.let { Money.formatInr(it) }
        Text(
            helper ?: " ",
            style = MaterialTheme.typography.bodySmall,
            color = if (error != null) colors.error else colors.onSurfaceVariant,
        )
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.utcMillisToDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * Material date picker. The picker works in UTC midnight millis; conversion to [LocalDate] is done in UTC so the
 * chosen calendar day never shifts with the device time zone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PennyDatePickerDialog(
    initial: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
) {
    val selectable = remember(minDate, maxDate) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val d = utcTimeMillis.utcMillisToDate()
                return (minDate == null || !d.isBefore(minDate)) && (maxDate == null || !d.isAfter(maxDate))
            }

            override fun isSelectableYear(year: Int): Boolean =
                (minDate == null || year >= minDate.year) && (maxDate == null || year <= maxDate.year)
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toUtcMillis(),
        selectableDates = selectable,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onPick(it.utcMillisToDate()) } ?: onDismiss() },
            ) { Text("Done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        DatePicker(
            state = state,
            showModeToggle = true,
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                todayDateBorderColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PennyTimePickerDialog(
    hour: Int,
    minute: Int,
    onPick: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
    is24Hour: Boolean,
) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = is24Hour)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(16.dp)
                .border(1.dp, PennyTheme.colors.hairline, MaterialTheme.shapes.extraLarge),
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Overline("Reminder time", Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectorColor = MaterialTheme.colorScheme.primary,
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    TextButton(onClick = { onPick(state.hour, state.minute) }) { Text("Set time") }
                }
            }
        }
    }
}

val DefaultFieldShape = RoundedCornerShape(10.dp)
