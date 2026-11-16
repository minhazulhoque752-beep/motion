package app.penny.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

data class UserPrefs(
    val onboarded: Boolean = false,
    val name: String = "",
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val monthlyTargetPaise: Long? = null,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val reminderSkipIfSaved: Boolean = true,
    val reminderMessage: String = DEFAULT_REMINDER_MESSAGE,
) {
    companion object {
        const val DEFAULT_REMINDER_MESSAGE = "Set something aside today — every rupee counts."
        const val MAX_NAME_LENGTH = 24
        const val MAX_MESSAGE_LENGTH = 80
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "penny_prefs")

class PreferencesRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val onboarded = booleanPreferencesKey("onboarded")
        val name = stringPreferencesKey("name")
        val theme = stringPreferencesKey("theme")
        val monthlyTarget = longPreferencesKey("monthly_target_paise")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val reminderSkip = booleanPreferencesKey("reminder_skip_if_saved")
        val reminderMessage = stringPreferencesKey("reminder_message")
    }

    val prefs: Flow<UserPrefs> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            UserPrefs(
                onboarded = p[Keys.onboarded] ?: false,
                name = p[Keys.name].orEmpty(),
                theme = ThemeMode.entries.firstOrNull { it.name == p[Keys.theme] } ?: ThemeMode.SYSTEM,
                monthlyTargetPaise = p[Keys.monthlyTarget]?.takeIf { it > 0 },
                reminderEnabled = p[Keys.reminderEnabled] ?: false,
                reminderHour = (p[Keys.reminderHour] ?: 20).coerceIn(0, 23),
                reminderMinute = (p[Keys.reminderMinute] ?: 0).coerceIn(0, 59),
                reminderSkipIfSaved = p[Keys.reminderSkip] ?: true,
                reminderMessage = p[Keys.reminderMessage]?.takeIf { it.isNotBlank() } ?: UserPrefs.DEFAULT_REMINDER_MESSAGE,
            )
        }

    suspend fun current(): UserPrefs = prefs.first()

    suspend fun completeOnboarding(name: String) = store.edit {
        it[Keys.onboarded] = true
        it[Keys.name] = name.trim().take(UserPrefs.MAX_NAME_LENGTH)
    }

    suspend fun setName(name: String) = store.edit { it[Keys.name] = name.trim().take(UserPrefs.MAX_NAME_LENGTH) }

    suspend fun setTheme(mode: ThemeMode) = store.edit { it[Keys.theme] = mode.name }

    suspend fun setMonthlyTarget(paise: Long?) = store.edit {
        if (paise == null || paise <= 0) it.remove(Keys.monthlyTarget) else it[Keys.monthlyTarget] = paise
    }

    suspend fun setReminderEnabled(enabled: Boolean) = store.edit { it[Keys.reminderEnabled] = enabled }

    suspend fun setReminderTime(hour: Int, minute: Int) = store.edit {
        it[Keys.reminderHour] = hour.coerceIn(0, 23)
        it[Keys.reminderMinute] = minute.coerceIn(0, 59)
    }

    suspend fun setReminderSkipIfSaved(skip: Boolean) = store.edit { it[Keys.reminderSkip] = skip }

    suspend fun setReminderMessage(message: String) = store.edit {
        val m = message.trim().take(UserPrefs.MAX_MESSAGE_LENGTH)
        if (m.isEmpty()) it.remove(Keys.reminderMessage) else it[Keys.reminderMessage] = m
    }
}
