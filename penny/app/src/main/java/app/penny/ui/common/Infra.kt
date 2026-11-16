package app.penny.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.penny.AppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** A transient message shown in the app-wide snackbar, optionally with an action such as Undo. */
data class UiMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

/** App-wide snackbar channel, so a screen can confirm an action even after navigating away (e.g. "Saved ₹500"). */
class Messenger {
    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    fun show(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        _messages.tryEmit(UiMessage(text, actionLabel, onAction))
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

/** Creates (or reuses) a ViewModel scoped to the current navigation destination, built from the [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> pennyViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = LocalAppContainer.current
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}

/** Emits the current date and again whenever it changes (e.g. at midnight), so "today" never goes stale. */
fun todayFlow(clock: Clock): Flow<LocalDate> = flow {
    while (true) {
        emit(LocalDate.now(clock))
        delay(30_000)
    }
}.distinctUntilChanged()

/** Emits the current time truncated to the hour, whenever the hour changes (drives greetings and "today"). */
fun hourFlow(clock: Clock): Flow<LocalDateTime> = flow {
    while (true) {
        emit(LocalDateTime.now(clock).truncatedTo(ChronoUnit.HOURS))
        delay(30_000)
    }
}.distinctUntilChanged()
