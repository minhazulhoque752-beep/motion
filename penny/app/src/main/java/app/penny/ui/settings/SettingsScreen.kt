package app.penny.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.penny.BuildConfig
import app.penny.core.Money
import app.penny.data.ThemeMode
import app.penny.data.UserPrefs
import app.penny.ui.common.AmountInputDialog
import app.penny.ui.common.ConfirmDialog
import app.penny.ui.common.DetailTopBar
import app.penny.ui.common.Hairline
import app.penny.ui.common.ListRow
import app.penny.ui.common.LocalAppContainer
import app.penny.ui.common.Overline
import app.penny.ui.common.PennyMark
import app.penny.ui.common.PennyTimePickerDialog
import app.penny.ui.common.ScreenPadding
import app.penny.ui.common.Segmented
import app.penny.ui.common.TextInputDialog
import app.penny.ui.common.pennyTextFieldColors
import app.penny.ui.common.pennyViewModel
import app.penny.ui.icons.PennyIcons
import app.penny.ui.theme.PennyTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class SettingsDialog { NAME, TARGET, TIME, MESSAGE, CLEAR }

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenPrivacy: () -> Unit) {
    val vm = pennyViewModel { SettingsViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val messenger = LocalAppContainer.current.messenger
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
    val prefs = state.prefs
    val is24h = DateFormat.is24HourFormat(context)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(vm::exportCsv)
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::backup)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::loadBackup)
    }
    val openNotificationSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setReminderEnabled(true)
        else messenger.show("Notifications are blocked. Allow them to receive reminders.", "Settings") { openNotificationSettings() }
    }

    fun enableReminder() {
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else vm.setReminderEnabled(true)
    }

    val stamp = vm.today.toString()

    Column(Modifier.fillMaxSize()) {
        DetailTopBar("Settings", onBack = onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item { Group("Profile") }
            item {
                ListRow(
                    "Your name",
                    icon = PennyIcons.User,
                    subtitle = prefs.name.ifBlank { "Not set — used for your greeting" },
                    onClick = { dialog = SettingsDialog.NAME },
                )
            }

            item { Group("Savings") }
            item {
                ListRow(
                    "Monthly target",
                    icon = PennyIcons.Target,
                    subtitle = prefs.monthlyTargetPaise?.let { Money.formatInr(it) } ?: "Not set",
                    onClick = { dialog = SettingsDialog.TARGET },
                )
            }

            item { Group("Appearance") }
            item {
                Column(Modifier.padding(horizontal = ScreenPadding, vertical = 8.dp)) {
                    Segmented(
                        options = ThemeMode.entries.toList(),
                        selected = prefs.theme,
                        label = { it.label },
                        onSelect = vm::setTheme,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item { Group("Reminders") }
            item {
                ListRow(
                    "Daily reminder",
                    icon = PennyIcons.Bell,
                    subtitle = "A gentle nudge to record your savings",
                    onClick = { if (prefs.reminderEnabled) vm.setReminderEnabled(false) else enableReminder() },
                    trailing = {
                        Switch(
                            checked = prefs.reminderEnabled,
                            onCheckedChange = { on -> if (on) enableReminder() else vm.setReminderEnabled(false) },
                            colors = pennySwitchColors(),
                        )
                    },
                )
            }
            item {
                ListRow(
                    "Reminder time",
                    icon = PennyIcons.Clock,
                    subtitle = formatTime(prefs.reminderHour, prefs.reminderMinute, is24h),
                    enabled = prefs.reminderEnabled,
                    onClick = { dialog = SettingsDialog.TIME },
                )
            }
            item {
                ListRow(
                    "Skip if I've already saved today",
                    icon = PennyIcons.Skip,
                    enabled = prefs.reminderEnabled,
                    onClick = { vm.setReminderSkip(!prefs.reminderSkipIfSaved) },
                    trailing = {
                        Switch(
                            checked = prefs.reminderSkipIfSaved,
                            onCheckedChange = vm::setReminderSkip,
                            enabled = prefs.reminderEnabled,
                            colors = pennySwitchColors(),
                        )
                    },
                )
            }
            item {
                ListRow(
                    "Reminder message",
                    icon = PennyIcons.Message,
                    subtitle = prefs.reminderMessage,
                    enabled = prefs.reminderEnabled,
                    onClick = { dialog = SettingsDialog.MESSAGE },
                )
            }
            item {
                ListRow(
                    "Send a test notification",
                    icon = PennyIcons.Check,
                    enabled = prefs.reminderEnabled,
                    onClick = { vm.sendTestReminder() },
                )
            }

            item { Group("Your data") }
            item {
                ListRow(
                    "Export history as CSV",
                    icon = PennyIcons.Export,
                    subtitle = "${state.entryCount} entries · opens in any spreadsheet app",
                    enabled = !state.busy && state.entryCount > 0,
                    onClick = { exportLauncher.launch("penny-transactions-$stamp.csv") },
                )
            }
            item {
                ListRow(
                    "Back up data",
                    icon = PennyIcons.Backup,
                    subtitle = "Save entries and goals to a file you control",
                    enabled = !state.busy,
                    onClick = { backupLauncher.launch("penny-backup-$stamp.json") },
                )
            }
            item {
                ListRow(
                    "Restore from backup",
                    icon = PennyIcons.Restore,
                    subtitle = "Replace current data with a Penny backup file",
                    enabled = !state.busy,
                    onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                )
            }
            item {
                ListRow(
                    "Clear all data",
                    icon = PennyIcons.Delete,
                    titleColor = MaterialTheme.colorScheme.error,
                    subtitle = "Permanently delete every entry and goal",
                    enabled = !state.busy && (state.entryCount > 0 || state.goalCount > 0),
                    onClick = { dialog = SettingsDialog.CLEAR },
                )
            }

            item { Group("About") }
            item { ListRow("Privacy", icon = PennyIcons.Shield, subtitle = "Your data never leaves this device", onClick = onOpenPrivacy) }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ScreenPadding, vertical = 24.dp),
                ) {
                    PennyMark(40.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Penny ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "A calm, private ledger for your savings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    when (dialog) {
        SettingsDialog.NAME -> TextInputDialog(
            title = "Your name",
            initial = prefs.name,
            label = "Name",
            maxLength = UserPrefs.MAX_NAME_LENGTH,
            onConfirm = { vm.setName(it); dialog = null; null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.TARGET -> AmountInputDialog(
            title = "Monthly savings target",
            initialPaise = prefs.monthlyTargetPaise,
            allowClear = true,
            onConfirm = { vm.setMonthlyTarget(it); dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.TIME -> PennyTimePickerDialog(
            hour = prefs.reminderHour,
            minute = prefs.reminderMinute,
            is24Hour = is24h,
            onPick = { h, m -> vm.setReminderTime(h, m); dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.MESSAGE -> TextInputDialog(
            title = "Reminder message",
            initial = prefs.reminderMessage,
            label = "Message",
            maxLength = UserPrefs.MAX_MESSAGE_LENGTH,
            supporting = "Leave empty to use the default",
            onConfirm = { vm.setReminderMessage(it); dialog = null; null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.CLEAR -> ClearDataDialog(
            entryCount = state.entryCount,
            goalCount = state.goalCount,
            onConfirm = { vm.clearAll(); dialog = null },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }

    state.pendingRestore?.let { data ->
        ConfirmDialog(
            title = "Restore this backup?",
            text = "It contains ${data.entries.size} entries and ${data.goals.size} goals. " +
                "Your current ${state.entryCount} entries and ${state.goalCount} goals will be replaced. This can't be undone.",
            confirmLabel = "Replace & restore",
            destructive = true,
            onConfirm = vm::confirmRestore,
            onDismiss = vm::cancelRestore,
        )
    }
}

@Composable
private fun Group(title: String) {
    Column {
        Spacer(Modifier.height(20.dp))
        Overline(title, Modifier.padding(horizontal = ScreenPadding))
        Spacer(Modifier.height(4.dp))
        Hairline(Modifier.padding(horizontal = ScreenPadding))
    }
}

@Composable
private fun pennySwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = PennyTheme.colors.hairline,
)

private fun formatTime(hour: Int, minute: Int, is24h: Boolean): String =
    LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a", Locale.ENGLISH))

/** Clearing financial records is irreversible, so the user must type DELETE to confirm. */
@Composable
private fun ClearDataDialog(entryCount: Int, goalCount: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var typed by rememberSaveable { mutableStateOf("") }
    val ok = typed.trim().equals("DELETE", ignoreCase = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear all data?", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    "This permanently deletes $entryCount entries, $goalCount goals and your monthly target. " +
                        "Consider making a backup first. Type DELETE to confirm.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it.take(10) },
                    singleLine = true,
                    placeholder = { Text("DELETE") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    colors = pennyTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = ok) {
                Text("Delete everything", color = if (ok) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    )
}
