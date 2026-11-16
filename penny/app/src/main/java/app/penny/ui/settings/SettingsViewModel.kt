package app.penny.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.penny.AppContainer
import app.penny.data.ThemeMode
import app.penny.data.UserPrefs
import app.penny.data.io.BackupCodec
import app.penny.data.io.BackupData
import app.penny.data.io.BackupFormatException
import app.penny.data.io.CsvExporter
import app.penny.notifications.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SettingsUiState(
    val loading: Boolean = true,
    val prefs: UserPrefs = UserPrefs(),
    val entryCount: Int = 0,
    val goalCount: Int = 0,
    val busy: Boolean = false,
    val pendingRestore: BackupData? = null,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.repository
    private val prefsRepo = container.preferences
    private val messenger = container.messenger

    private val local = MutableStateFlow(Pair<Boolean, BackupData?>(false, null))

    val state: StateFlow<SettingsUiState> = combine(
        prefsRepo.prefs,
        repo.entries,
        repo.goals,
        local,
    ) { prefs, entries, goals, (busy, pending) ->
        SettingsUiState(false, prefs, entries.size, goals.size, busy, pending)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    val today: LocalDate get() = LocalDate.now(container.clock)

    fun setName(name: String) = launch { prefsRepo.setName(name) }
    fun setTheme(mode: ThemeMode) = launch { prefsRepo.setTheme(mode) }
    fun setMonthlyTarget(paise: Long?) = launch { prefsRepo.setMonthlyTarget(paise) }

    fun setReminderEnabled(enabled: Boolean) = launch {
        prefsRepo.setReminderEnabled(enabled)
        val p = prefsRepo.current()
        if (enabled) {
            container.reminderScheduler.schedule(p.reminderHour, p.reminderMinute)
            messenger.show("Daily reminder on")
        } else {
            container.reminderScheduler.cancel()
            messenger.show("Daily reminder off")
        }
    }

    fun setReminderTime(hour: Int, minute: Int) = launch {
        prefsRepo.setReminderTime(hour, minute)
        if (prefsRepo.current().reminderEnabled) container.reminderScheduler.schedule(hour, minute)
    }

    fun setReminderSkip(skip: Boolean) = launch { prefsRepo.setReminderSkipIfSaved(skip) }
    fun setReminderMessage(message: String) = launch { prefsRepo.setReminderMessage(message) }

    fun exportCsv(uri: Uri) = busy {
        val (entries, goals) = repo.snapshot()
        container.documents.write(uri, CsvExporter.export(entries, goals))
        messenger.show(if (entries.size == 1) "Exported 1 entry" else "Exported ${entries.size} entries")
    }

    fun backup(uri: Uri) = busy {
        val (entries, goals) = repo.snapshot()
        container.documents.write(uri, BackupCodec.encode(entries, goals, container.clock.millis()))
        messenger.show("Backup saved")
    }

    /** Reads and validates a backup file, then asks the user to confirm before replacing anything. */
    fun loadBackup(uri: Uri) = busy {
        val data = try {
            BackupCodec.decode(container.documents.read(uri))
        } catch (e: BackupFormatException) {
            messenger.show(e.message ?: "This file isn't a Penny backup.")
            return@busy
        }
        local.update { it.copy(second = data) }
    }

    fun confirmRestore() {
        val data = local.value.second ?: return
        local.update { it.copy(second = null) }
        busy {
            repo.replaceAll(data)
            messenger.show("Restored ${data.entries.size} entries and ${data.goals.size} goals")
        }
    }

    fun cancelRestore() = local.update { it.copy(second = null) }

    fun clearAll() = busy {
        repo.clearAll()
        prefsRepo.setMonthlyTarget(null)
        messenger.show("All savings data cleared")
    }

    fun sendTestReminder() {
        if (!ReminderScheduler.canPostNotifications(container.appContext)) {
            messenger.show("Notifications are turned off for Penny in system settings")
            return
        }
        ReminderScheduler.showReminder(container.appContext, state.value.prefs.reminderMessage)
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    /** Runs a data operation once at a time, surfacing failures as a friendly message. */
    private fun busy(block: suspend () -> Unit) {
        if (local.value.first) return
        local.update { it.copy(first = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                messenger.show("Something went wrong: ${e.message ?: "please try again"}")
            } finally {
                local.update { it.copy(first = false) }
            }
        }
    }
}
