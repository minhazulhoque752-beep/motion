package app.penny.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.penny.PennyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Fires once a day: shows the reminder (unless already saved today, if the user chose that) and schedules the next one. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as PennyApplication).container
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefs = container.preferences.current()
                if (!prefs.reminderEnabled) return@launch
                container.reminderScheduler.schedule(prefs.reminderHour, prefs.reminderMinute)
                val savedToday = container.repository.hasEntryOn(LocalDate.now(container.clock))
                if (!(prefs.reminderSkipIfSaved && savedToday)) {
                    ReminderScheduler.showReminder(context, prefs.reminderMessage)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms are cleared on reboot, app update and clock changes; this puts the daily reminder back. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as PennyApplication).container
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefs = container.preferences.current()
                if (prefs.reminderEnabled) {
                    container.reminderScheduler.schedule(prefs.reminderHour, prefs.reminderMinute)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
