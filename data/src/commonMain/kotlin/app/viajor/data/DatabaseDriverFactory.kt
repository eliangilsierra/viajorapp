package app.viajor.data

import app.cash.sqldelight.db.SqlDriver

/**
 * Creates the platform SQLite driver. Every driver must run with foreign keys switched on (SQLite has them
 * off by default) because the schema relies on them (ADR-0002).
 */
expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}
