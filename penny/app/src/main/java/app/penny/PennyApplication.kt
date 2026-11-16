package app.penny

import android.app.Application
import android.content.Context
import app.penny.core.DeviceClock
import app.penny.data.PreferencesRepository
import app.penny.data.SavingsRepository
import app.penny.data.db.PennyDatabase
import app.penny.data.io.DocumentIo
import app.penny.notifications.ReminderScheduler
import app.penny.ui.common.Messenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock

class PennyApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderScheduler.ensureChannel(this)
    }
}

/** Manual dependency container — small, explicit, and free of reflection. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val clock: Clock = DeviceClock()
    val database: PennyDatabase by lazy { PennyDatabase.build(context) }
    val repository: SavingsRepository by lazy { SavingsRepository(database, clock) }
    val preferences = PreferencesRepository(context)
    val reminderScheduler = ReminderScheduler(context.applicationContext, clock)
    val documents = DocumentIo(context.applicationContext)
    val messenger = Messenger()

    /** For work that must outlive a screen, e.g. an Undo tapped after the screen has closed. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}
