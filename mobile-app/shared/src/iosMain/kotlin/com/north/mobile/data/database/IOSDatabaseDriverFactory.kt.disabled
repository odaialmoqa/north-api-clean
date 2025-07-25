package com.north.mobile.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.north.mobile.data.security.EncryptionManager
import com.north.mobile.database.NorthDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * iOS implementation of DatabaseDriverFactory with performance optimizations
 * Includes proper file location management and iOS-specific configurations
 */
class IOSDatabaseDriverFactory : DatabaseDriverFactory {
    
    override suspend fun createDriver(encryptionManager: EncryptionManager): SqlDriver {
        return try {
            val databasePath = getDatabasePath()
            
            NativeSqliteDriver(
                schema = NorthDatabase.Schema,
                name = databasePath,
                onConfiguration = { config ->
                    config.copy(
                        extendedConfig = config.extendedConfig.copy(
                            // iOS-specific optimizations
                            busyTimeout = 30_000L,
                            // Enable WAL mode for better concurrency
                            journalMode = "WAL",
                            // Optimize for iOS memory constraints
                            cacheSize = 2000,
                            // Enable foreign key constraints
                            foreignKeyConstraints = true,
                            // Optimize synchronization for iOS
                            synchronous = "NORMAL"
                        )
                    )
                },
                onUpgrade = { driver, oldVersion, newVersion ->
                    // Handle database migrations with proper error handling
                    try {
                        NorthDatabase.Schema.migrate(driver, oldVersion, newVersion)
                    } catch (e: Exception) {
                        throw DatabaseMigrationException("Migration failed from $oldVersion to $newVersion", e)
                    }
                }
            )
        } catch (e: Exception) {
            throw DatabaseException("Failed to create iOS database driver", e)
        }
    }
    
    private fun getDatabasePath(): String {
        // Get the Documents directory for the app
        val fileManager = NSFileManager.defaultManager
        val urls = fileManager.URLsForDirectory(
            directory = NSDocumentDirectory,
            inDomains = NSUserDomainMask
        )
        
        val documentsURL = urls.firstOrNull() as? NSURL
            ?: throw DatabaseException("Could not access Documents directory")
        
        val databaseURL = documentsURL.URLByAppendingPathComponent(DATABASE_NAME)
            ?: throw DatabaseException("Could not create database URL")
        
        return databaseURL.path ?: throw DatabaseException("Could not get database path")
    }
    
    companion object {
        private const val DATABASE_NAME = "north_database.db"
    }
}

class DatabaseMigrationException(message: String, cause: Throwable? = null) : Exception(message, cause)