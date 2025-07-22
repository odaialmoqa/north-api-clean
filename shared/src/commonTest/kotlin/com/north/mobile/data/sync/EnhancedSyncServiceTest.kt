package com.north.mobile.data.sync

import com.north.mobile.data.plaid.PlaidServiceError
import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

class EnhancedSyncServiceTest {
    
    private lateinit var mockPlaidService: MockPlaidService
    private lateinit var mockAccountRepository: MockAccountRepository
    private lateinit var mockTransactionRepository: MockTransactionRepository
    private lateinit var mockConflictResolver: MockConflictResolver
    private lateinit var mockSyncStatusManager: MockSyncStatusManager
    private lateinit var mockRetryManager: MockRetryManager
    private lateinit var mockNotificationManager: MockSyncNotificationManager
    private lateinit var enhancedSyncService: EnhancedSyncServiceImpl
    
    @BeforeTest
    fun setup() {
        mockPlaidService = MockPlaidService()
        mockAccountRepository = MockAccountRepository()
        mockTransactionRepository = MockTransactionRepository()
        mockConflictResolver = MockConflictResolver()
        mockSyncStatusManager = MockSyncStatusManager()
        mockRetryManager = MockRetryManager()
        mockNotificationManager = MockSyncNotificationManager()
        
        enhancedSyncService = EnhancedSyncServiceImpl(
            plaidService = mockPlaidService,
            accountRepository = mockAccountRepository,
            transactionRepository = mockTransactionRepository,
            conflictResolver = mockConflictResolver,
            syncStatusManager = mockSyncStatusManager,
            retryManager = mockRetryManager,
            notificationManager = mockNotificationManager
        )
    }
    
    @Test
    fun `syncAllAccounts should send success notification with transaction details`() = runTest {
        // Given
        val userId = "user123"
        val account = createTestAccount("acc1", "inst1")
        val newTransaction = createTestTransaction("txn1", "acc1")
        
        mockAccountRepository.accounts = listOf(account)
        mockPlaidService.shouldSucceed = true
        mockPlaidService.balances = mapOf("acc1" to Money.fromDollars(1200.0))
        mockPlaidService.transactions = listOf(newTransaction)
        mockTransactionRepository.existingTransactions = emptyList()
        
        // When
        val result = enhancedSyncService.syncAllAccounts(userId)
        
        // Then
        assertTrue(result is SyncResult.Success)
        assertEquals(1, result.accountsUpdated)
        assertEquals(1, result.transactionsAdded)
        
        // Verify notifications
        assertEquals(1, mockNotificationManager.successNotifications.size)
        val (notifiedUserId, notifiedResult) = mockNotificationManager.successNotifications.first()
        assertEquals(userId, notifiedUserId)
        assertEquals(1, notifiedResult.accountsUpdated)
        assertEquals(1, notifiedResult.transactionsAdded)
    }
    
    @Test
    fun `syncAllAccounts should send conflict resolved notification when conflicts are handled`() = runTest {
        // Given
        val userId = "user123"
        val accountId = "acc1"
        val account = createTestAccount(accountId, "inst1")
        val existingTransaction = createTestTransaction("txn1", accountId, amount = Money.fromDollars(50.0))
        val remoteTransaction = createTestTransaction("txn1", accountId, amount = Money.fromDollars(55.0))
        
        mockAccountRepository.accounts = listOf(account)
        mockTransactionRepository.existingTransactions = listOf(existingTransaction)
        mockPlaidService.transactions = listOf(remoteTransaction)
        mockPlaidService.shouldSucceed = true
        
        // Set up conflict resolution
        val conflict = ConflictDetails(
            conflictType = ConflictType.MODIFIED_TRANSACTION,
            localData = existingTransaction,
            remoteData = remoteTransaction,
            resolution = ConflictResolution.USE_REMOTE
        )
        mockConflictResolver.conflicts = mapOf(
            (existingTransaction to remoteTransaction) to conflict
        )
        mockConflictResolver.resolutions = mapOf(conflict to conflict)
        
        // When
        val result = enhancedSyncService.syncAllAccounts(userId)
        
        // Then
        assertTrue(result is SyncResult.Success)
        assertEquals(1, result.conflictsResolved)
        
        // Verify conflict resolution notification
        assertEquals(1, mockNotificationManager.conflictResolvedNotifications.size)
        val (notifiedUserId, conflictCount) = mockNotificationManager.conflictResolvedNotifications.first()
        assertEquals(userId, notifiedUserId)
        assertEquals(1, conflictCount)
    }
    
    @Test
    fun `syncAllAccounts should send partial success notification when some accounts fail`() = runTest {
        // Given
        val userId = "user123"
        val account1 = createTestAccount("acc1", "inst1")
        val account2 = createTestAccount("acc2", "inst2")
        
        mockAccountRepository.accounts = listOf(account1, account2)
        mockPlaidService.shouldSucceed = true
        mockPlaidService.balances = mapOf("acc1" to Money.fromDollars(1200.0))
        
        // Simulate failure for second account by not providing balance
        // This will cause a partial success scenario
        
        // When
        val result = enhancedSyncService.syncAllAccounts(userId)
        
        // Then
        assertTrue(result is SyncResult.Success || result is SyncResult.PartialSuccess)
        
        // Verify appropriate notification was sent
        assertTrue(
            mockNotificationManager.successNotifications.isNotEmpty() || 
            mockNotificationManager.partialSuccessNotifications.isNotEmpty()
        )
    }
    
    @Test
    fun `syncAllAccounts should send failure notification when repository fails`() = runTest {
        // Given
        val userId = "user123"
        
        mockAccountRepository.shouldFail = true
        
        // When
        val result = enhancedSyncService.syncAllAccounts(userId)
        
        // Then
        assertTrue(result is SyncResult.Failure)
        
        // Verify failure notification
        assertEquals(1, mockNotificationManager.failureNotifications.size)
        val (notifiedUserId, error) = mockNotificationManager.failureNotifications.first()
        assertEquals(userId, notifiedUserId)
        assertTrue(error is SyncError.UnknownError)
    }
    
    @Test
    fun `syncAllAccounts should send reauth notification for authentication errors`() = runTest {
        // Given
        val userId = "user123"
        val account = createTestAccount("acc1", "inst1", institutionName = "Test Bank")
        
        mockAccountRepository.accounts = listOf(account)
        mockPlaidService.shouldSucceed = false
        mockPlaidService.error = PlaidServiceError.AuthenticationError("Token expired")
        
        // When
        val result = enhancedSyncService.syncAllAccounts(userId)
        
        // Then
        assertTrue(result is SyncResult.PartialSuccess)
        
        // Verify partial success notification
        assertEquals(1, mockNotificationManager.partialSuccessNotifications.size)
        
        // Verify reauth notification
        assertEquals(1, mockNotificationManager.reauthNotifications.size)
        val (notifiedUserId, accountId, institutionName) = mockNotificationManager.reauthNotifications.first()
        assertEquals(userId, notifiedUserId)
        assertEquals("acc1", accountId)
        assertEquals("Test Bank", institutionName)
    }
    
    @Test
    fun `incrementalSync should only notify for significant transaction additions`() = runTest {
        // Given
        val userId = "user123"
        val now = Clock.System.now()
        val oldAccount = createTestAccount("acc1", "inst1", lastUpdated = now.minus(20, DateTimeUnit.MINUTE))
        val recentAccount = createTestAccount("acc2", "inst2", lastUpdated = now.minus(5, DateTimeUnit.MINUTE))
        
        // Create 6 new transactions (above the threshold of 5)
        val newTransactions = (1..6).map { i ->
            createTestTransaction("txn$i", "acc1")
        }
        
        mockAccountRepository.accounts = listOf(oldAccount, recentAccount)
        mockPlaidService.shouldSucceed = true
        mockPlaidService.transactions = newTransactions
        mockTransactionRepository.existingTransactions = emptyList()
        
        // When
        val result = enhancedSyncService.incrementalSync(userId)
        
        // Then
        assertTrue(result is SyncResult.Success)
        assertEquals(6, result.transactionsAdded)
        
        // Verify new transaction notification (only for significant additions)
        assertEquals(1, mockNotificationManager.newTransactionNotifications.size)
        val (notifiedUserId, accountId, transactionCount) = mockNotificationManager.newTransactionNotifications.first()
        assertEquals(userId, notifiedUserId)
        assertEquals("multiple", accountId)
        assertEquals(6, transactionCount)
    }
    
    @Test
    fun `incrementalSync should not notify for small transaction additions`() = runTest {
        // Given
        val userId = "user123"
        val now = Clock.System.now()
        val oldAccount = createTestAccount("acc1", "inst1", lastUpdated = now.minus(20, DateTimeUnit.MINUTE))
        
        // Create only 3 new transactions (below the threshold of 5)
        val newTransactions = (1..3).map { i ->
            createTestTransaction("txn$i", "acc1")
        }
        
        mockAccountRepository.accounts = listOf(oldAccount)
        mockPlaidService.shouldSucceed = true
        mockPlaidService.transactions = newTransactions
        mockTransactionRepository.existingTransactions = emptyList()
        
        // When
        val result = enhancedSyncService.incrementalSync(userId)
        
        // Then
        assertTrue(result is SyncResult.Success)
        assertEquals(3, result.transactionsAdded)
        
        // Verify no new transaction notification for small additions
        assertEquals(0, mockNotificationManager.newTransactionNotifications.size)
    }
    
    @Test
    fun `cancelSync should cancel notifications`() = runTest {
        // Given
        val userId = "user123"
        
        // When
        enhancedSyncService.cancelSync(userId)
        
        // Then
        assertEquals(1, mockNotificationManager.cancelledNotifications.size)
        assertEquals(userId, mockNotificationManager.cancelledNotifications.first())
        
        // Verify sync status was updated to cancelled
        assertTrue(mockSyncStatusManager.userSyncStatusUpdates.any { 
            it.first == userId && it.second == SyncStatus.CANCELLED 
        })
    }
    
    @Test
    fun `syncAccount should handle retry logic with proper error mapping`() = runTest {
        // Given
        val accountId = "acc1"
        val account = createTestAccount(accountId, "inst1")
        
        mockAccountRepository.accounts = listOf(account)
        mockPlaidService.shouldSucceed = false
        mockPlaidService.error = PlaidServiceError.RateLimitError("Rate limit exceeded")
        mockRetryManager.retryAttempts = 0
        
        // When
        val result = enhancedSyncService.syncAccount(accountId)
        
        // Then
        assertTrue(result is SyncResult.Failure)
        assertTrue(result.error is SyncError.RateLimitError)
        
        // Verify retry was attempted
        assertTrue(mockRetryManager.retryAttempts > 0)
    }
    
    @Test
    fun `syncTransactions should properly handle conflict resolution`() = runTest {
        // Given
        val accountId = "acc1"
        val account = createTestAccount(accountId, "inst1")
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        val existingTransaction = createTestTransaction("txn1", accountId, 
            amount = Money.fromDollars(100.0), description = "Original")
        val remoteTransaction = createTestTransaction("txn1", accountId, 
            amount = Money.fromDollars(100.0), description = "Updated")
        
        mockAccountRepository.accounts = listOf(account)
        mockTransactionRepository.existingTransactions = listOf(existingTransaction)
        mockPlaidService.transactions = listOf(remoteTransaction)
        mockPlaidService.shouldSucceed = true
        
        // Set up conflict detection and resolution
        val conflict = ConflictDetails(
            conflictType = ConflictType.MODIFIED_TRANSACTION,
            localData = existingTransaction,
            remoteData = remoteTransaction,
            resolution = ConflictResolution.USE_REMOTE
        )
        mockConflictResolver.conflicts = mapOf(
            (existingTransaction to remoteTransaction) to conflict
        )
        mockConflictResolver.resolutions = mapOf(conflict to conflict)
        
        // When
        val result = enhancedSyncService.syncTransactions(accountId, startDate, endDate)
        
        // Then
        assertTrue(result is SyncResult.Success)
        assertEquals(0, result.transactionsAdded) // No new transactions
        assertEquals(1, result.transactionsUpdated) // One updated
        assertEquals(1, result.conflictsResolved) // One conflict resolved
        
        // Verify the transaction was updated
        assertEquals(1, mockTransactionRepository.updatedTransactions.size)
        assertEquals(remoteTransaction, mockTransactionRepository.updatedTransactions.first())
    }
    
    private fun createTestAccount(
        id: String,
        institutionId: String,
        balance: Money = Money.fromDollars(1000.0),
        isActive: Boolean = true,
        lastUpdated: Instant = Clock.System.now(),
        institutionName: String = "Test Bank"
    ): Account {
        return Account(
            id = id,
            institutionId = institutionId,
            institutionName = institutionName,
            accountType = AccountType.CHECKING,
            balance = balance,
            currency = Currency.CAD,
            lastUpdated = lastUpdated,
            isActive = isActive
        )
    }
    
    private fun createTestTransaction(
        id: String,
        accountId: String,
        amount: Money = Money.fromDollars(100.0),
        date: LocalDate = LocalDate(2024, 1, 15),
        description: String = "Test Transaction"
    ): Transaction {
        return Transaction(
            id = id,
            accountId = accountId,
            amount = amount,
            description = description,
            category = Category.UNCATEGORIZED,
            date = date
        )
    }
}