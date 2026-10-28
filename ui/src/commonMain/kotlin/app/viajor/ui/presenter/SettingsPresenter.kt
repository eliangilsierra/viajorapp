package app.viajor.ui.presenter

import app.viajor.data.BackupService
import app.viajor.data.RestoreSummary
import app.viajor.domain.BackupCodec
import app.viajor.domain.BackupException
import app.viajor.ui.FileAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

/** What the screen tells the person after an action. It carries numbers only, never file content. */
sealed interface SettingsMessage {
    data object Exported : SettingsMessage

    data class Restored(val summary: RestoreSummary) : SettingsMessage

    data object NotABackup : SettingsMessage

    data object Erased : SettingsMessage

    data object Cancelled : SettingsMessage

    data object Failed : SettingsMessage
}

data class SettingsState(val busy: Boolean = false, val message: SettingsMessage? = null)

class SettingsPresenter(
    private val backup: BackupService,
    private val files: FileAccess,
    private val scope: CoroutineScope,
    private val nowMillis: () -> Long,
) {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun exportBackup() = guarded {
        val now = nowMillis()
        val text = BackupCodec.encode(backup.export(now))
        val day = Instant.fromEpochMilliseconds(now).toLocalDateTime(TimeZone.UTC).date
        val saved = files.saveText("viajorapp-backup-$day.json", "application/json", text)
        if (saved) SettingsMessage.Exported else SettingsMessage.Cancelled
    }

    fun restoreBackup() = guarded {
        val text = files.pickText(listOf("application/json", "text/plain", "application/octet-stream"))
        if (text == null) {
            SettingsMessage.Cancelled
        } else {
            try {
                SettingsMessage.Restored(backup.restore(BackupCodec.decode(text)))
            } catch (e: BackupException) {
                SettingsMessage.NotABackup
            }
        }
    }

    fun deleteAllData() = guarded {
        backup.deleteAllData()
        SettingsMessage.Erased
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun guarded(block: suspend () -> SettingsMessage) = scope.launch {
        _state.update { it.copy(busy = true, message = null) }
        val message = try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SettingsMessage.Failed
        }
        _state.update { it.copy(busy = false, message = message) }
    }
}
