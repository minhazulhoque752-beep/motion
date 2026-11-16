package app.penny.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import app.penny.MainActivity
import app.penny.R
import app.penny.core.ReminderTime
import java.time.Clock
import java.time.ZonedDateTime

/**
 * Schedules the daily reminder with [AlarmManager]. Uses an inexact, doze-friendly alarm (no special
 * exact-alarm permission needed); after each reminder fires the next day's alarm is scheduled.
 */
class ReminderScheduler(private val context: Context, private val clock: Clock) {

    fun schedule(hour: Int, minute: Int) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val trigger = ReminderTime.nextTriggerMillis(ZonedDateTime.now(clock), hour, minute)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, reminderIntent())
    }

    fun cancel() {
        context.getSystemService(AlarmManager::class.java)?.cancel(reminderIntent())
    }

    private fun reminderIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val ACTION_REMIND = "app.penny.action.REMIND"
        const val CHANNEL_ID = "daily_reminder"
        private const val REQUEST_CODE = 4100
        private const val NOTIFICATION_ID = 4101

        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.reminder_channel_description) }
            nm.createNotificationChannel(channel)
        }

        fun canPostNotifications(context: Context): Boolean {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return false
            val permitted = Build.VERSION.SDK_INT < 33 ||
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            return permitted && nm.areNotificationsEnabled()
        }

        fun showReminder(context: Context, message: String) {
            if (!canPostNotifications(context)) return
            ensureChannel(context)
            val open = PendingIntent.getActivity(
                context,
                REQUEST_CODE + 1,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_OPEN_ADD, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_penny)
                .setColor(0xFF2F5D46.toInt())
                .setContentTitle("Time to save")
                .setContentText(message)
                .setStyle(android.app.Notification.BigTextStyle().bigText(message))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            context.getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
        }
    }
}
