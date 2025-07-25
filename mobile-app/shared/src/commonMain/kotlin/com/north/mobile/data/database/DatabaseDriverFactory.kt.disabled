package com.north.mobile.data.database

import app.cash.sqldelight.db.SqlDriver
import com.north.mobile.data.security.EncryptionManager

/**
 * Factory interface for creating encrypted database drivers
 */
interface DatabaseDriverFactory {
    /**
     * Create an encrypted SQLite driver
     */
    suspend fun createDriver(encryptionManager: EncryptionManager): SqlDriver
}

/**
 * Exception thrown when database operations fail
 */
class DatabaseException(message: String, cause: Throwable? = null) : Exception(message, cause)