package app.viajor.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.viajor.data.db.ViajorDatabase

// NOT COMPILED OR RUN: there is no macOS build environment yet. Written from the SQLDelight 2.x native
// driver API; verify and fix on a Mac before relying on it.
actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver = NativeSqliteDriver(
        schema = ViajorDatabase.Schema,
        name = "viajor.db",
        onConfiguration = { config -> config.copy(extendedConfig = config.extendedConfig.copy(foreignKeyConstraints = true)) },
    )
}
