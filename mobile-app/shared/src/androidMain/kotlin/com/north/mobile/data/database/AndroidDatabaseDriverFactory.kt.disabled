package com.north.mobile.data.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.north.mobile.data.security.EncryptionManager
import com.north.mobile.database.NorthDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

/**
 * Android implementation of DatabaseDriverFactory using SQLCipher for encryption
 */
class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {
    
    override suspend fun createDriver(encryptionManager: EncryptionManager): SqlDriver {
        return try {
            // Get the database encryption key
            val databaseKey = encryptionManager.getDatabaseKey().getOrThrow()
            
            // Initialize SQLCipher
            SQLiteDatabase.loadLibs(context)
            
            // Create encrypted database factory
            val supportFactory = SupportFactory(databaseKey.toByteArray())
            
            // Create the driver with encryption
            AndroidSqliteDriver(
                schema = NorthDatabase.Schema,
                context = context,
                name = DATABASE_NAME,
                factory = supportFactory
            ).also { driver ->
                // Enable foreign key constraints
                driver.execute(null, "PRAGMA foreign_keys=ON", 0)
                // Set secure delete mode
                driver.execute(null, "PRAGMA secure_delete=ON", 0)
                // Set journal mode to WAL for better performance
                driver.execute(null, "PRAGMA journal_mode=WAL", 0)
            }
        } catch (e: Exception) {
            throw DatabaseException("Failed to create encrypted database driver", e)
        }
    }
    
    companion object {
        private const val DATABASE_NAME = "north_database.db"
    }
}