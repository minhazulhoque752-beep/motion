package app.penny.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.penny.ui.common.DetailTopBar
import app.penny.ui.common.Overline
import app.penny.ui.common.ScreenPadding

private val sections = listOf(
    "Stored only on this phone" to
        "Every entry, goal and setting lives in a private database inside Penny's app storage. " +
        "Penny has no account, no server and no internet permission, so your figures cannot be sent anywhere.",
    "No tracking" to
        "There are no analytics, advertising identifiers, crash reporters or third-party SDKs. Penny never sees how you use it.",
    "Excluded from cloud backup" to
        "Android's automatic cloud backup and device-to-device transfer are switched off for Penny. " +
        "Your data only leaves the app when you choose to export it.",
    "Files you control" to
        "CSV exports and backups are written to a location you pick. Treat them like any financial document: " +
        "anyone with the file can read its contents.",
    "Notifications" to
        "The daily reminder is scheduled on your device. Its text never includes your balances.",
    "Deleting your data" to
        "Settings › Clear all data permanently removes every entry and goal. Uninstalling Penny also deletes everything it stored.",
)

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        DetailTopBar("Privacy", onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding),
        ) {
            Text(
                "Your savings are your business.",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Penny is designed to work entirely offline. Here is exactly what that means.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            sections.forEach { (title, body) ->
                Spacer(Modifier.height(28.dp))
                Overline(title)
                Spacer(Modifier.height(8.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
