package app.penny

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.penny.data.UserPrefs
import app.penny.ui.PennyAppRoot
import app.penny.ui.common.LocalAppContainer
import app.penny.ui.theme.PennyTheme
import app.penny.ui.theme.isDarkTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val openAddRequested = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        val container = (application as PennyApplication).container
        val prefsState: StateFlow<UserPrefs?> = container.preferences.prefs
            .stateIn(lifecycleScope, SharingStarted.Eagerly, null)

        // Hold the branded splash until preferences are loaded, so the first frame is the right screen.
        splash.setKeepOnScreenCondition { prefsState.value == null }

        if (savedInstanceState == null) handleIntent(intent)

        // Alarms are cleared if the app is force-stopped; make sure the daily reminder is in place.
        lifecycleScope.launch {
            val prefs = container.preferences.current()
            if (prefs.reminderEnabled) container.reminderScheduler.schedule(prefs.reminderHour, prefs.reminderMinute)
        }

        setContent {
            val prefs by prefsState.collectAsStateWithLifecycle()
            val openAdd by openAddRequested.collectAsStateWithLifecycle()
            val current = prefs ?: return@setContent
            val dark = isDarkTheme(current.theme)

            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose { }
            }

            CompositionLocalProvider(LocalAppContainer provides container) {
                PennyTheme(darkTheme = dark) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        PennyAppRoot(
                            prefs = current,
                            openAddRequested = openAdd,
                            onOpenAddHandled = { openAddRequested.value = false },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_ADD, false) == true) {
            openAddRequested.value = true
            intent.removeExtra(EXTRA_OPEN_ADD)
        }
    }

    companion object {
        const val EXTRA_OPEN_ADD = "app.penny.extra.OPEN_ADD"
    }
}
