package app.viajor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.viajor.ui.presenter.SettingsMessage
import app.viajor.ui.presenter.SettingsPresenter
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource
import viajorapp.ui.generated.resources.Res
import viajorapp.ui.generated.resources.action_back
import viajorapp.ui.generated.resources.action_cancel
import viajorapp.ui.generated.resources.action_delete_all
import viajorapp.ui.generated.resources.action_delete_all_confirm
import viajorapp.ui.generated.resources.action_export_backup
import viajorapp.ui.generated.resources.action_restore_backup
import viajorapp.ui.generated.resources.delete_all_body
import viajorapp.ui.generated.resources.delete_all_title
import viajorapp.ui.generated.resources.msg_cancelled
import viajorapp.ui.generated.resources.msg_erased
import viajorapp.ui.generated.resources.msg_exported
import viajorapp.ui.generated.resources.msg_failed
import viajorapp.ui.generated.resources.msg_restore_failed
import viajorapp.ui.generated.resources.msg_restored
import viajorapp.ui.generated.resources.settings_privacy_body
import viajorapp.ui.generated.resources.settings_privacy_title
import viajorapp.ui.generated.resources.settings_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(deps: AppDependencies, files: FileAccess, navigator: Navigator) {
    val scope = rememberCoroutineScope()
    val presenter = remember { SettingsPresenter(deps.backup, files, scope) { Clock.System.now().toEpochMilliseconds() } }
    val state by presenter.state.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = { TextButton(onClick = { navigator.back() }) { Text(stringResource(Res.string.action_back)) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.settings_privacy_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(Res.string.settings_privacy_body), style = MaterialTheme.typography.bodyMedium)

            Button(onClick = presenter::exportBackup, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.action_export_backup))
            }
            OutlinedButton(onClick = presenter::restoreBackup, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.action_restore_backup))
            }
            OutlinedButton(onClick = { confirmDelete = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.action_delete_all), color = MaterialTheme.colorScheme.error)
            }

            state.message?.let { message ->
                Text(message.text(), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(Res.string.delete_all_title)) },
            text = { Text(stringResource(Res.string.delete_all_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; presenter.deleteAllData() }) {
                    Text(stringResource(Res.string.action_delete_all_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(Res.string.action_cancel)) } },
        )
    }
}

@Composable
private fun SettingsMessage.text(): String = when (this) {
    SettingsMessage.Exported -> stringResource(Res.string.msg_exported)
    is SettingsMessage.Restored -> stringResource(Res.string.msg_restored, summary.trips, summary.activities)
    SettingsMessage.NotABackup -> stringResource(Res.string.msg_restore_failed)
    SettingsMessage.Erased -> stringResource(Res.string.msg_erased)
    SettingsMessage.Cancelled -> stringResource(Res.string.msg_cancelled)
    SettingsMessage.Failed -> stringResource(Res.string.msg_failed)
}
