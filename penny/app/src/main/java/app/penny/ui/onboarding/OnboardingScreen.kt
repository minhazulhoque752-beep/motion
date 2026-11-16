package app.penny.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.penny.data.UserPrefs
import app.penny.ui.common.LocalAppContainer
import app.penny.ui.common.MoneyText
import app.penny.ui.common.Overline
import app.penny.ui.common.PennyCard
import app.penny.ui.common.PennyMark
import app.penny.ui.common.PrimaryButton
import app.penny.ui.common.ProgressLine
import app.penny.ui.common.pennyTextFieldColors
import app.penny.ui.home.BalanceText
import app.penny.ui.theme.PennyText
import app.penny.ui.theme.PennyTheme
import kotlinx.coroutines.launch

/**
 * Sample figures used ONLY to preview the dashboard during onboarding. They are clearly labelled and are never
 * written to the database — the user's ledger always starts empty.
 */
private object SamplePreview {
    const val TOTAL_PAISE = 25_500_00L
    const val MONTH_PAISE = 5_000_00L
    const val ENTRIES = 18
    const val TARGET_PAISE = 8_000_00L
}

@Composable
fun OnboardingScreen() {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var starting by rememberSaveable { mutableStateOf(false) }

    fun start() {
        if (starting) return
        starting = true
        scope.launch { container.preferences.completeOnboarding(name) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(36.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PennyMark(44.dp)
                Spacer(Modifier.width(12.dp))
                Text("Penny", style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(36.dp))
            Text("Save with intention.", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Text(
                "Record what you put aside, set goals worth saving for, and see your progress clearly — all privately, on this phone.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))
            SamplePreviewCard()

            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(UserPrefs.MAX_NAME_LENGTH) },
                label = { Text("What should we call you? (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { start() }),
                colors = pennyTextFieldColors(),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Start with an empty ledger", ::start, Modifier.fillMaxWidth(), loading = starting)
            Spacer(Modifier.height(12.dp))
            Text(
                "No account. No internet. Your data stays on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SamplePreviewCard() {
    PennyCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline("Dashboard preview", Modifier.weight(1f))
            Text(
                "SAMPLE DATA",
                style = PennyText.overline,
                color = PennyTheme.colors.onSampleBadge,
                modifier = Modifier
                    .background(PennyTheme.colors.sampleBadge, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Overline("Total savings")
        Spacer(Modifier.height(4.dp))
        BalanceText(SamplePreview.TOTAL_PAISE, MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1f)) {
                Overline("This month")
                MoneyText(SamplePreview.MONTH_PAISE, style = PennyText.amount)
            }
            Column(Modifier.weight(1f)) {
                Overline("Savings entries")
                Text(SamplePreview.ENTRIES.toString(), style = PennyText.amount)
            }
        }
        Spacer(Modifier.height(16.dp))
        ProgressLine(SamplePreview.MONTH_PAISE.toFloat() / SamplePreview.TARGET_PAISE)
        Spacer(Modifier.height(10.dp))
        Text(
            "Illustration only — none of these figures are added to your account.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
