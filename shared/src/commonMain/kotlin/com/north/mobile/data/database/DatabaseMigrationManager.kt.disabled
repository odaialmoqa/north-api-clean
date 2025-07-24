package com.north.mobile.data.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Manages database schema migrations for app updates
 */
class DatabaseMigrationManager {
    
    /**
     * Perform database migration from old version to new version
     */
    fun migrate(driver: SqlDriver, oldVersion: Long, newVersion: Long) {
        when {
            oldVersion < 1 && newVersion >= 1 -> {
                // Initial schema creation is handled by SQLDelight
                // No migration needed for version 1
            }
            oldVersion < 2 && newVersion >= 2 -> {
                migrateToVersion2(driver)
            }
            oldVersion < 3 && newVersion >= 3 -> {
                migrateToVersion3(driver)
            }
            // Add more migration cases as needed
        }
    }
    
    /**
     * Example migration to version 2 - adds new columns or tables
     */
    private fun migrateToVersion2(driver: SqlDriver) {
        try {
            // Example: Add new column to User table
            driver.execute(
                identifier = null,
                sql = "ALTER TABLE User ADD COLUMN postalCode TEXT",
                parameters = 0
            )
            
            // Example: Add new index
            driver.execute(
                identifier = null,
                sql = "CREATE INDEX idx_user_postal_code ON User(postalCode)",
                parameters = 0
            )
        } catch (e: Exception) {
            throw DatabaseException("Failed to migrate to version 2", e)
        }
    }
    
    /**
     * Example migration to version 3 - adds gamification enhancements
     */
    private fun migrateToVersion3(driver: SqlDriver) {
        try {
            // Example: Add new gamification features
            driver.execute(
                identifier = null,
                sql = """
                    CREATE TABLE IF NOT EXISTS MicroTask (
                        id TEXT NOT NULL PRIMARY KEY,
                        goalId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        targetAmount INTEGER,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY (goalId) REFERENCES FinancialGoal(id) ON DELETE CASCADE
                    )
                """.trimIndent(),
                parameters = 0
            )
            
            driver.execute(
                identifier = null,
                sql = "CREATE INDEX idx_microtask_goal_id ON MicroTask(goalId)",
                parameters = 0
            )
        } catch (e: Exception) {
            throw DatabaseException("Failed to migrate to version 3", e)
        }
    }
    
    /**
     * Backup database before migration
     */
    fun backupDatabase(driver: SqlDriver): Result<Unit> {
        return try {
            // In a real implementation, you would create a backup
            // This is a placeholder for the backup logic
            driver.execute(
                identifier = null,
                sql = "PRAGMA integrity_check",
                parameters = 0
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DatabaseException("Failed to backup database", e))
        }
    }
    
    /**
     * Verify database integrity after migration
     */
    fun verifyIntegrity(driver: SqlDriver): Result<Unit> {
        return try {
            driver.execute(
                identifier = null,
                sql = "PRAGMA integrity_check",
                parameters = 0
            )
            
            driver.execute(
                identifier = null,
                sql = "PRAGMA foreign_key_check",
                parameters = 0
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DatabaseException("Database integrity check failed", e))
        }
    }
    
    companion object {
        const val CURRENT_VERSION = 1L
    }
}