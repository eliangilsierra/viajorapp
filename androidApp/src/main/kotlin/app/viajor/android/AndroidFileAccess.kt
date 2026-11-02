package app.viajor.android

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import app.viajor.ui.FileAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Reads and writes only the file the user picks in the system file picker (Storage Access Framework), so
 * the app needs no storage permission. Launchers must be registered while the activity is being created.
 */
class AndroidFileAccess(private val activity: ComponentActivity) : FileAccess {
    private var saveContinuation: ((Uri?) -> Unit)? = null
    private var pickContinuation: ((Uri?) -> Unit)? = null

    private val saveLauncher = activity.registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        saveContinuation?.invoke(uri)
        saveContinuation = null
    }
    private val pickLauncher = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pickContinuation?.invoke(uri)
        pickContinuation = null
    }

    override suspend fun saveText(suggestedName: String, mimeType: String, text: String): Boolean {
        val uri = suspendCancellableCoroutine<Uri?> { continuation ->
            saveContinuation = { continuation.resume(it) }
            continuation.invokeOnCancellation { saveContinuation = null }
            saveLauncher.launch(suggestedName)
        } ?: return false
        withContext(Dispatchers.IO) {
            val stream = activity.contentResolver.openOutputStream(uri, "wt") ?: error("could not open the chosen file")
            stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
        return true
    }

    override suspend fun pickText(mimeTypes: List<String>): String? {
        val uri = suspendCancellableCoroutine<Uri?> { continuation ->
            pickContinuation = { continuation.resume(it) }
            continuation.invokeOnCancellation { pickContinuation = null }
            pickLauncher.launch(mimeTypes.toTypedArray())
        } ?: return null
        return withContext(Dispatchers.IO) {
            val stream = activity.contentResolver.openInputStream(uri) ?: error("could not open the chosen file")
            stream.use { input ->
                // A backup is text; anything unreasonably large becomes an empty string, which the backup
                // reader rejects as not being a backup, instead of loading it all into memory.
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                    if (total > MAX_BYTES) return@use null
                    out.write(buffer, 0, n)
                }
                out.toString(Charsets.UTF_8.name())
            }
        } ?: ""
    }

    private companion object {
        const val MAX_BYTES = 25_000_000
    }
}
