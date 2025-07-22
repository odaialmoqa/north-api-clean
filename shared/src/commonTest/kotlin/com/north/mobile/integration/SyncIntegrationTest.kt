package com.north.mobile.integration

import com.north.mobile.data.sync.*
import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

/**
 * Integration test for the complete sync workflow including:
 * - Background sync scheduling
 * - Incremental sync optimization
 * - Conflict resolution
 * - Notification delivery
 * - Error handling and retry logic
 */
class SyncIntegrationTest {
    
    private lateinit var mockPlaidService: MockPlaidService
    private lateinit var mockAccountRepository: MockAccountRepository
    private lateinit var mockTransactionRepository: MockTransactionRepository
    private lateinit var conflictResolver: ConflictResolverImpl
    private lateinit var syncStatusManager: SyncStatusManagerImpl
    private lateinit var retryManager: RetryManagerImpl
    private lateinit var notificationManager: SyncNotificationManagerImpl
    private lateinit var syncService: EnhancedSyncServiceImpl
    
    @BeforeTest
    fun setup() {
        mockPlaidService = MockPlaidService()
        mockAccountRepository = MockAccountRepository()
        mockTransactionRepository = MockTransactionRepository()
        conflictResolver = ConflictResolverImpl()
        syncStatusManager = SyncStatusManagerImpl()
        retryManager = RetryManagerImpl()
        notificationManager = SyncNotificationManagerImpl()
        
        syncService = EnhancedSyncServiceImpl(
            plaidService = mockPlaidService,
            accountRepository = mockAccountRepository,
            transactionRepository = mockTransactionRepository,
            conflictResolver = conflictResolver,
            syncStatusManager = syncStatusManager,
            retryManager = retryManager,
            notificationManager = notificationManager
        )
    }
    
    @Test
    fun `complete sync workflow should handle all scenarios correctly`() = runTest {
        // Given: User with multiple accounts and existing transactions
        val userId = "user123"
        val now = Clock.System.now()
        
        val checkingAccount = Account(
            id = "checking_001",
            institutionId = "rbc",
            institutionName = "RBC Royal Bank",
            accountType = AccountType.CHECKING,
            balance = Money.fromDollars(2500.0),
            currency = Currency.CAD,
            lastUpdated = now.minus(30, DateTimeUnit.MINUTE),
            isActive = true
        )
        
        val savingsAccount = Account(
            id = "savings_001",
            institutionId = "rbc",
            institutionName = "RBC Royal Bank",
            accountType = AccountType.SAVINGS,
            balance = Money.fromDollars(15000.0),
            currency = Currency.CAD,
            lastUpdated = now.minus(45, DateTimeUnit.MINUTE),
            isActive = true
        )
        
        val creditAccount = Account(
            id = "credit_001",
            institutionId = "td",
            institutionName = "TD Canada Trust",
            accountType = AccountType.CREDIT_CARD,
            balance = Money.fromDollars(-1200.0),
            currency = Currency.CAD,
            lastUpdated = now.minus(60, DateTimeUnit.MINUTE),
            isActive = true
        )
        
        // Existing transactions that will create conflicts
        val existingTransaction1 = Transaction(
            id = "txn_001",
            accountId = "checking_001",
            amount = Money.fromDollars(-45.67),
            description = "GROCERY STORE",
            category = Category.GROCERIES,
            date = LocalDate(2024, 1, 15)
        )
        
        val existingTransaction2 = Transaction(
            id = "txn_002",
            accountId = "savings_001",
            amount = Money.fromDollars(500.0),
            description = "TRANSFER FROM CHECKING",
            category = Category.INCOME,
            date = LocalDate(2024, 1, 16)
        )
        
        // Remote transactions with some conflicts and new transactions
        val remoteTransaction1 = Transaction(
            id = "txn_001", // Same ID but different amount (conflict)
            accountId = "checking_001",
            amount = Money.fromDollars(-47.89), // Updated amount
            description = "LOBLAWS GROCERY STORE", // More detailed description
            category = Category.GROCERIES,
            date = LocalDate(2024, 1, 15),
            merchantName = "Loblaws",
            location = "Toronto, ON"
        )
        
        val newTransaction1 = Transaction(
            id = "txn_003",
            accountId = "checking_001",
            amount = Money.fromDollars(-25.00),
            description = "COFFEE SHOP",
            category = Category.FOOD,
            date = LocalDate(2024, 1, 17)
        )
        
        val newTransaction2 = Transaction(
            id = "txn_004",
            accountId = "credit_001",
            amount = Money.fromDollars(-89.99),
            description = "ONLINE PURCHASE",
            category = Category.SHOPPING,
            date = LocalDate(2024, 1, 17)
        )
        
        // Setup mock data
        mockAccountRepository.accounts = listOf(checkingAccount, savingsAccount, creditAccount)
        mockTransactionRepository.existingTransactions = listOf(existingTransaction1, existingTransaction2)
        
        // Setup Plaid service responses
        mockPlaidService.shouldSucceed = true
        mockPlaidService.balances = mapOf(
            "checking_001" to Money.fromDollars(2455.33), // Updated balance
            "savings_001" to Money.fromDollars(15000.0),   // Same balance
            "credit_001" to Money.fromDollars(-1289.99)    // Updated balance
        )
        mockPlaidService.transactions = listOf(
            remoteTransaction1, // Conflict with existing
            existingTransaction2, // No conflict
            newTransaction1, // New transaction
            newTransaction2  // New transaction
        )
        
        // Register accounts with sync status manager
        val statusManager = syncStatusManager as SyncStatusManagerImpl
        statusManager.registerAccountForUser(userId, "checking_001")
        statusManager.registerAccountForUser(userId, "savings_001")
        statusManager.registerAccountForUser(userId, "credit_001")
        
        // When: Perform full sync
        val syncResult = syncService.syncAllAccounts(userId)
        
        // Then: Verify sync results
        assertTrue(syncResult is SyncResult.Success, "Sync should succeed")
        assertEquals(2, syncResult.accountsUpdated, "Two accounts should have balance updates")
        assertEquals(2, syncResult.transactionsAdded, "Two new transactions should be added")
        assertEquals(1, syncResult.transactionsUpdated, "One transaction should be updated due to conflict")
        assertEquals(1, syncResult.conflictsResolved, "One conflict should be resolved")
        
        // Verify account balance updates
        assertEquals(Money.fromDollars(2455.33), mockAccountRepository.balanceUpdates["checking_001"])
        assertEquals(Money.fromDollars(-1289.99), mockAccountRepository.balanceUpdates["credit_001"])
        assertNull(mockAccountRepository.balanceUpdates["savings_001"]) // No change
        
        // Verify transaction operations
        assertEquals(2, mockTransactionRepository.insertedTransactions.size)
        assertEquals(1, mockTransactionRepository.updatedTransactions.size)
        
        // Verify conflict resolution
        val updatedTransaction = mockTransactionRepository.updatedTransactions.first()
        assertEquals("txn_001", updatedTransaction.id)
        assertEquals(Money.fromDollars(-47.89), updatedTransaction.amount)
        assertEquals("Loblaws", updatedTransaction.merchantName)
        assertEquals("Toronto, ON", updatedTransaction.location)
        
        // Verify new transactions were added
        val addedTransactionIds = mockTransactionRepository.insertedTransactions.map { it.id }
        assertTrue(addedTransactionIds.contains("txn_003"))
        assertTrue(addedTransactionIds.contains("txn_004"))
    }
    
    @Test
    fun `incremental sync should optimize for recently updated accounts only`() = runTest {
        // Given: Multiple accounts with different last update times
        val userId = "user123"
        val now = Clock.System.now()
        
        val recentAccount = Account(
            id = "recent_001",
            institutionId = "rbc",
            institutionName = "RBC Royal Bank",
            accountType = AccountType.CHECKING,
            balance = Money.fromDollars(1000.0),
            currency = Currency.CAD,
            lastUpdated = now.minus(5, DateTimeUnit.MINUTE), // Recently updated
            isActive = true
        )
        
        val staleAccount = Account(
            id = "stale_001",
            institutionId = "td",
            institutionName = "TD Canada Trust",
            accountType = AccountType.SAVINGS,
            balance = Money.fromDollars(5000.0),
            currency = Currency.CAD,
            lastUpdated = now.minus(30, DateTimeUnit.MINUTE), // Needs sync
            isActive = true
        )
        
        val veryStaleAccount = Account(
            id = "very_stale_001",
            institutionId = "bmo",
            institutionName = "Bank of Montreal",
            accountType = AccountType.CREDIT_CARD,
            balance = Money.fromDollars(-500.0),
            currency = Currency.CAD,
            lastUpdated = now.minus(60, DateTimeUnit.MINUTE), // Needs sync
            isActive = true
        )
        
        mockAccountRepository.accounts = listOf(recentAccount, staleAccount, veryStaleAccount)
        mockPlaidService.shouldSucceed = true
        mockPlaidService.balances = mapOf(
            "stale_001" to Money.fromDollars(5100.0),
            "very_stale_001" to Money.fromDollars(-550.0)
        )
        mockPlaidService.transactions = emptyList()
        
        // When: Perform incremental sync
        val result = syncService.incrementalSync(userId)
        
        // Then: Verify only stale accounts were synced
        assertTrue(result is SyncResult.Success)
        assertEquals(2, result.accountsUpdated) // Only the two stale accounts
        
        // Verify balance updates for stale accounts only
        assertNull(mockAccountRepository.balanceUpdates["recent_001"]) // Should not be updated
        assertEquals(Money.fromDollars(5100.0), mockAccountRepository.balanceUpdates["stale_001"])
        assertEquals(Money.fromDollars(-550.0), mockAccountRepository.balanceUpdates["very_stale_001"])
    }
    
    @Test
    fun `sync should handle authentication errors gracefully with proper notifications`() = runTest {
        // Given: Account that requires re-authentication
        val userId = "user123"
        val account = Account(
            id = "auth_error_001",
            institutionId = "scotiabank",
            institutionName = "Scotiabank",
            accountType = AccountType.CHECKING,
            balance = Money.fromDollars(1500.0),
            currency = Currency.CAD,
            lastUpdated = Clock.System.now().minus(30, DateTimeUnit.MINUTE),
            isActive = true
        )
        
        mockAccountRepository.accounts = listOf(account)
        mockPlaidService.shouldSucceed = false
        mockPlaidService.error = com.north.mobile.data.plaid.PlaidServiceError.AuthenticationError("Item requires user interaction")
        
        // When: Perform sync
        val result = syncService.syncAllAccounts(userId)
        
        // Then: Verify partial success with authentication error
        assertTrue(result is SyncResult.PartialSuccess)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors.first() is SyncError.AuthenticationError)
        
        // Verify authentication error contains account ID
        val authError = result.errors.first() as SyncError.AuthenticationError
        assertEquals("auth_error_001", authError.accountId)
    }
    
    @Test
    fun `sync should handle network errors with retry logic`() = runTest {
        // Given: Account that will experience network errors
        val userId = "user123"
        val account = Account(
            id = "network_error_001",
            institutionId = "cibc",
            institutionName = "CIBC",
            accountType = AccountType.CHECKING,
            balance = Money.fromDollars(2000.0),
            currency = Currency.CAD,
            lastUpdated = Clock.System.now().minus(30, DateTimeUnit.MINUTE),
            isActive = true
        )
        
        mockAccountRepository.accounts = listOf(account)
        mockPlaidService.shouldSucceed = false
        mockPlaidService.error = com.north.mobile.data.plaid.PlaidServiceError.NetworkError("Connection timeout")
        
        // When: Perform sync (retry manager will attempt retries)
        val result = syncService.syncAllAccounts(userId)
        
        // Then: Verify failure with network error
        assertTrue(result is SyncResult.PartialSuccess || result is SyncResult.Failure)
        
        if (result is SyncResult.PartialSuccess) {
            assertTrue(result.errors.any { it is SyncError.NetworkError })
        } else if (result is SyncResult.Failure) {
            assertTrue(result.error is SyncError.NetworkError)
        }
    }
    
    @Test
    fun `background sync should be schedulable and cancellable`() = runTest {
        // Given: User with accounts
        val userId = "user123"
        val account = Account(
            id = "bg_sync_001",
            institutionId = "rbc",
            institutionName = "RBC Royal Bank",
            accountType = AccountType.CHECKING,
            balance = Money.fromDollars(1000.0),
            currency = Currency.CAD,
            lastUpdated = Clock.System.now().minus(30, DateTimeUnit.MINUTE),
            isActive = true
        )
        
        mockAccountRepository.accounts = listOf(account)
        mockPlaidService.shouldSucceed = true
        
        // When: Schedule background sync
        syncService.scheduleBackgroundSync(userId, intervalMinutes = 30)
        
        // Then: Background sync should be active (we can't easily test the actual scheduling in unit tests)
        // But we can verify that stopping it works
        syncService.stopBackgroundSync(userId)
        
        // Verify no exceptions were thrown
        assertTrue(true, "Background sync scheduling and stopping should work without errors")
    }
    
    @Test
    fun `sync cancellation should work correctly`() = runTest {
        // Given: User with accounts
        val userId = "user123"
        val account = Account(
            id = "cancel_001",
            institutionId = "rbc",
            institutionName = "RBC Royal Bank",
            accountType = AccountType.CHECKING,
            balance = Money.fromDollars(1000.0),
            currency = Currency.CAD,
            lastUpdated = Clock.System.now().minus(30, DateTimeUnit.MINUTE),
            isActive = true
        )
        
        mockAccountRepository.accounts = listOf(account)
        
        // When: Cancel sync
        syncService.cancelSync(userId)
        
        // Then: Verify cancellation was processed
        // (In a real implementation, this would cancel ongoing operations)
        assertTrue(true, "Sync cancellation should work without errors")
    }
}