package com.north.mobile.data.database

import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Integration tests for database migration scenarios
 * Tests complete migration flows and data integrity
 */
class DatabaseMigrationTest {
    
    private lateinit var migrationManager: DatabaseMigrationManager
    
    @BeforeTest
    fun setup() {
        migrationManager = DatabaseMigrationManager()
    }
    
    @Test
    fun testMigrationManagerCreation() {
        assertNotNull(migrationManager)
    }
    
    @Test
    fun testCurrentVersion() {
        assertEquals(1L, DatabaseMigrationManager.CURRENT_VERSION)
    }
    
    @Test
    fun testBackupDatabaseWithMockDriver() {
        val mockDriver = MockSqlDriver()
        val result = migrationManager.backupDatabase(mockDriver)
        assertTrue(result.isSuccess)
        
        // Verify backup operations were executed
        val statements = mockDriver.getExecutedStatements()
        assertTrue(statements.any { it.contains("PRAGMA integrity_check") })
    }
    
    @Test
    fun testVerifyIntegrityWithMockDriver() {
        val mockDriver = MockSqlDriver()
        val result = migrationManager.verifyIntegrity(mockDriver)
        assertTrue(result.isSuccess)
        
        // Verify integrity check operations were executed
        val statements = mockDriver.getExecutedStatements()
        assertTrue(statements.any { it.contains("PRAGMA integrity_check") })
        assertTrue(statements.any { it.contains("PRAGMA foreign_key_check") })
    }
    
    @Test
    fun testMigrationFromVersion0To1() {
        val mockDriver = MockSqlDriver()
        
        // Test migration from version 0 to 1 (initial schema)
        assertDoesNotThrow {
            migrationManager.migrate(mockDriver, 0, 1)
        }
        
        // Version 0 to 1 should not execute any statements (handled by SQLDelight)
        val statements = mockDriver.getExecutedStatements()
        assertEquals(0, statements.size)
    }
    
    @Test
    fun testMigrationFromVersion1To2() {
        val mockDriver = MockSqlDriver()
        
        // Test migration from version 1 to 2
        assertDoesNotThrow {
            migrationManager.migrate(mockDriver, 1, 2)
        }
        
        // Verify migration statements were executed
        val statements = mockDriver.getExecutedStatements()
        assertTrue(statements.any { it.contains("ALTER TABLE User ADD COLUMN postalCode TEXT") })
        assertTrue(statements.any { it.contains("CREATE INDEX idx_user_postal_code ON User(postalCode)") })
    }
    
    @Test
    fun testMigrationFromVersion2To3() {
        val mockDriver = MockSqlDriver()
        
        // Test migration from version 2 to 3
        assertDoesNotThrow {
            migrationManager.migrate(mockDriver, 2, 3)
        }
        
        // Verify migration statements were executed
        val statements = mockDriver.getExecutedStatements()
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS MicroTask") })
        assertTrue(statements.any { it.contains("CREATE INDEX idx_microtask_goal_id ON MicroTask(goalId)") })
    }
    
    @Test
    fun testMultiVersionMigration() {
        val mockDriver = MockSqlDriver()
        
        // Test migration from version 0 to 3 (should execute all intermediate migrations)
        assertDoesNotThrow {
            migrationManager.migrate(mockDriver, 0, 3)
        }
        
        // Should execute migrations for versions 2 and 3
        val statements = mockDriver.getExecutedStatements()
        assertTrue(statements.any { it.contains("ALTER TABLE User ADD COLUMN postalCode TEXT") })
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS MicroTask") })
    }
    
    @Test
    fun testMigrationWithFailure() {
        val failingDriver = FailingMockSqlDriver()
        
        // Test that migration failure is handled properly
        assertFailsWith<DatabaseException> {
            migrationManager.migrate(failingDriver, 1, 2)
        }
    }
    
    @Test
    fun testBackupFailureHandling() {
        val failingDriver = FailingMockSqlDriver()
        
        val result = migrationManager.backupDatabase(failingDriver)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DatabaseException)
    }
    
    @Test
    fun testIntegrityCheckFailureHandling() {
        val failingDriver = FailingMockSqlDriver()
        
        val result = migrationManager.verifyIntegrity(failingDriver)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DatabaseException)
    }
    
    @Test
    fun testCompleteUpgradeScenario() = runTest {
        val mockDriver = MockSqlDriver()
        
        // Simulate complete upgrade from fresh install to current version
        val oldVersion = 0L
        val newVersion = DatabaseMigrationManager.CURRENT_VERSION
        
        // Step 1: Backup database
        val backupResult = migrationManager.backupDatabase(mockDriver)
        assertTrue(backupResult.isSuccess)
        
        // Step 2: Perform migration
        assertDoesNotThrow {
            migrationManager.migrate(mockDriver, oldVersion, newVersion)
        }
        
        // Step 3: Verify integrity
        val integrityResult = migrationManager.verifyIntegrity(mockDriver)
        assertTrue(integrityResult.isSuccess)
        
        // Verify all expected operations were performed
        val statements = mockDriver.getExecutedStatements()
        
        // Should have backup operations
        assertTrue(statements.any { it.contains("PRAGMA integrity_check") })
        
        // Should have migration operations (if any for current version)
        // Currently only version 1 is supported, so no migration statements expected
        
        // Should have integrity verification operations
        assertTrue(statements.any { it.contains("PRAGMA foreign_key_check") })
    }
    
    @Test
    fun testIncrementalMigrationScenario() = runTest {
        val mockDriver = MockSqlDriver()
        
        // Simulate incremental migration (e.g., app update from version 1 to 2)
        val oldVersion = 1L
        val newVersion = 2L
        
        // Step 1: Backup database
        val backupResult = migrationManager.backupDatabase(mockDriver)
        assertTrue(backupResult.isSuccess)
        
        // Step 2: Perform migration
        assertDoesNotThrow {
            migrationManager.migrate(mockDriver, oldVersion, newVersion)
        }
        
        // Step 3: Verify integrity
        val integrityResult = migrationManager.verifyIntegrity(mockDriver)
        assertTrue(integrityResult.isSuccess)
        
        // Verify migration-specific operations were performed
        val statements = mockDriver.getExecutedStatements()
        assertTrue(statements.any { it.contains("ALTER TABLE User ADD COLUMN postalCode TEXT") })
        assertTrue(statements.any { it.contains("CREATE INDEX idx_user_postal_code ON User(postalCode)") })
    }
    
    @Test
    fun testNoMigrationNeededScenario() {
        val mockDriver = MockSqlDriver()
        
        // Test scenario where no migration is needed (same version)
        val currentVersion = DatabaseMigrationManager.CURRENT_VERSION
        
        assertDoesNotThrow {
            migrationManager.migrate(mockDriver, currentVersion, currentVersion)
        }
        
        // Should not execute any migration statements
        val statements = mockDriver.getExecutedStatements()
        assertEquals(0, statements.size)
    }
    
    @Test
    fun testDowngradeScenario() {
        val mockDriver = MockSqlDriver()
        
        // Test scenario where app is downgraded (higher version to lower version)
        // This should not execute any migrations
        assertDoesNotThrow {
            migrationManager.migrate(mockDriver, 3, 1)
        }
        
        // Should not execute any migration statements for downgrades
        val statements = mockDriver.getExecutedStatements()
        assertEquals(0, statements.size)
    }
    
    @Test
    fun testMigrationWithDataPreservation() {
        val mockDriver = MockSqlDriverWithData()
        
        // Simulate migration that should preserve existing data
        assertDoesNotThrow {
            migrationManager.migrate(mockDriver, 1, 2)
        }
        
        // Verify that data preservation operations were performed
        val statements = mockDriver.getExecutedStatements()
        assertTrue(statements.any { it.contains("ALTER TABLE") }) // Should use ALTER TABLE to preserve data
        assertFalse(statements.any { it.contains("DROP TABLE") }) // Should not drop tables
    }
    
    @Test
    fun testMigrationRollbackOnFailure() {
        val transactionalDriver = TransactionalMockSqlDriver()
        
        // Simulate migration failure that should trigger rollback
        assertFailsWith<DatabaseException> {
            migrationManager.migrate(transactionalDriver, 1, 2)
        }
        
        // Verify that transaction was rolled back
        assertTrue(transactionalDriver.wasRolledBack)
        assertFalse(transactionalDriver.wasCommitted)
    }
}

/**
 * Mock SqlDriver for testing database operations
 */
open class MockSqlDriver : app.cash.sqldelight.db.SqlDriver {
    protected val executedStatements = mutableListOf<String>()
    
    override fun close() {
        // Mock implementation
    }
    
    override fun currentTransaction(): app.cash.sqldelight.db.SqlDriver.Transaction? {
        return null
    }
    
    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (app.cash.sqldelight.db.SqlPreparedStatement.() -> Unit)?
    ): app.cash.sqldelight.db.QueryResult<Long> {
        executedStatements.add(sql)
        return app.cash.sqldelight.db.QueryResult.Value(0L)
    }
    
    override fun executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (app.cash.sqldelight.db.SqlCursor) -> app.cash.sqldelight.db.QueryResult<*>,
        parameters: Int,
        binders: (app.cash.sqldelight.db.SqlPreparedStatement.() -> Unit)?
    ): app.cash.sqldelight.db.QueryResult<*> {
        executedStatements.add(sql)
        return app.cash.sqldelight.db.QueryResult.Value(emptyList<Any>())
    }
    
    override fun newTransaction(): app.cash.sqldelight.db.SqlDriver.Transaction {
        return MockTransaction()
    }
    
    fun getExecutedStatements(): List<String> = executedStatements.toList()
}

/**
 * Mock SqlDriver that fails on execute operations
 */
class FailingMockSqlDriver : MockSqlDriver() {
    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (app.cash.sqldelight.db.SqlPreparedStatement.() -> Unit)?
    ): app.cash.sqldelight.db.QueryResult<Long> {
        throw Exception("Database operation failed")
    }
    
    override fun executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (app.cash.sqldelight.db.SqlCursor) -> app.cash.sqldelight.db.QueryResult<*>,
        parameters: Int,
        binders: (app.cash.sqldelight.db.SqlPreparedStatement.() -> Unit)?
    ): app.cash.sqldelight.db.QueryResult<*> {
        throw Exception("Database query failed")
    }
}

/**
 * Mock SqlDriver that simulates having existing data
 */
class MockSqlDriverWithData : MockSqlDriver() {
    private val userData = mutableMapOf<String, Any>()
    
    init {
        // Simulate existing user data
        userData["user1"] = mapOf(
            "id" to "user1",
            "email" to "test@example.com",
            "firstName" to "John",
            "lastName" to "Doe"
        )
    }
    
    override fun executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (app.cash.sqldelight.db.SqlCursor) -> app.cash.sqldelight.db.QueryResult<*>,
        parameters: Int,
        binders: (app.cash.sqldelight.db.SqlPreparedStatement.() -> Unit)?
    ): app.cash.sqldelight.db.QueryResult<*> {
        executedStatements.add(sql)
        
        // Simulate returning existing data for SELECT queries
        if (sql.contains("SELECT") && sql.contains("User")) {
            return app.cash.sqldelight.db.QueryResult.Value(userData.values.toList())
        }
        
        return app.cash.sqldelight.db.QueryResult.Value(emptyList<Any>())
    }
}

/**
 * Mock SqlDriver that supports transactions for rollback testing
 */
class TransactionalMockSqlDriver : MockSqlDriver() {
    var wasCommitted = false
    var wasRolledBack = false
    
    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (app.cash.sqldelight.db.SqlPreparedStatement.() -> Unit)?
    ): app.cash.sqldelight.db.QueryResult<Long> {
        executedStatements.add(sql)
        
        // Simulate failure on specific migration statement
        if (sql.contains("ALTER TABLE User ADD COLUMN postalCode TEXT")) {
            throw Exception("Migration failed")
        }
        
        return app.cash.sqldelight.db.QueryResult.Value(0L)
    }
    
    override fun newTransaction(): app.cash.sqldelight.db.SqlDriver.Transaction {
        return TransactionalMockTransaction(this)
    }
}

/**
 * Mock Transaction for testing rollback scenarios
 */
class MockTransaction : app.cash.sqldelight.db.SqlDriver.Transaction {
    override fun endTransaction(successful: Boolean) {
        // Mock implementation
    }
}

/**
 * Mock Transaction that tracks commit/rollback state
 */
class TransactionalMockTransaction(
    private val driver: TransactionalMockSqlDriver
) : app.cash.sqldelight.db.SqlDriver.Transaction {
    override fun endTransaction(successful: Boolean) {
        if (successful) {
            driver.wasCommitted = true
        } else {
            driver.wasRolledBack = true
        }
    }
}

/**
 * Custom exception for database operations
 */
class DatabaseException(message: String, cause: Throwable? = null) : Exception(message, cause)

// Helper function to assert no exception is thrown
fun assertDoesNotThrow(block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        fail("Expected no exception, but got: ${e.message}")
    }
}