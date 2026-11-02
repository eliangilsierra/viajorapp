package app.viajor.ui

/**
 * The only door from the shared UI to the device's files. The user always picks the location through the
 * system file picker; the app never reads or writes anywhere else. Both calls return null/false when the
 * user cancels.
 */
interface FileAccess {
    /** Asks the user where to save [text] and writes it there. Returns false if cancelled. */
    suspend fun saveText(suggestedName: String, mimeType: String, text: String): Boolean

    /** Asks the user to pick a file and returns its text, or null if cancelled. */
    suspend fun pickText(mimeTypes: List<String>): String?
}
