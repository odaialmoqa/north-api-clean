package com.north.mobile.data.sync

import com.north.mobile.data.plaid.PlaidService
import com.north.mobile.data.plaid.PlaidServiceError
import com.north.mobile.data.repository.AccountRepository
import com.north.mobile.data.repository.TransactionRepository
import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

class SyncServiceTest {
    
    private lateinit var mockPlaidService: MockPlaidService
    private lateinit var mockAccountRepository: MockAccountRepository
    private lateinit var mockTransactionRepository: MockTransactionRepository
    private lateinit var mockConflictResolver: MockConflictResolver
    private lateinit var mockSyncStatusManager: MockSyncStatusManager
    private lateinit var mockRetryManager: MockRetryManager
    private lateinit var mockNotificationManager: MockSyncNotificationManager
    private lateinit var syncService: SyncServiceImpl
    
    @BeforeTest
    fun setup() {
        mockPlaidService = MockPlaidService()
        mockAccountRepository = MockAccountRepository()
        mockTransactionRepository = MockTransactionRepository()
        mockConflictResolver = MockConflictResolver()
        mockSyncStatusManager = MockSyncStatusManager()
        mockRetryManager = MockRetryManager()
        mockNotificationManager = MockSyncNotificationManager()
        
        syncService = SyncServiceImpl(
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
    fun `syncAllAccounts should sync all active accounts for user`() = runTest {
        // Given
        val userId = "user123"
        val account1 = createTestAccount("acc1", "inst1")
        val account2 = createTestAccount("acc2", "inst2")
        val inactiveAccount = createTestAccount("acc3", "inst3", isActive = false)
        
        mockAccountRepository.accounts = listOf(account1, account2, inactiveAccount)
        mockPlaidService.shouldSucceed = true
        
        // When
        val result = syncService.syncAllAccounts(userId)
        
        // Then
        assertTrue(result is SyncResult.Success)
        assertEquals(2, result.accountsUpdated) // Only active accounts
        assertTrue(mockSyncStatusManager.userSyncStatusUpdates.contains(userId to SyncStatus.SYNCING))
        assertTrue(mockSyncStatusManager.userSyncStatusUpdates.contains(userId to SyncStatus.SUCCESS))
    }
    
    @Test
    fun `syncAccount should update account balance when changed`() = runTest {
        // Given
        val accountId = "acc1"
        val oldBalance = Money.fromDollars(1000.0)
        val newBalance = Money.fromDollars(1200.0)
        val account = createTestAccount(accountId, "inst1", balance = oldBalance)
        
        mockAccountRepository.accounts = listOf(account)
        mockPlaidService.balances = mapOf(accountId to newBalance)
        mockPlaidService.shouldSucceed = true
        
        // When
        val result = syncService.syncAccount(accountId)
        
        // Then
        assertTrue(result is SyncResult.Success)
        assertEquals(1, result.accountsUpdated)
        assertTrue(mockAccountRepository.balanceUpdates.containsKey(accountId))
        assertEquals(newBalance, mockAccountRepository.balanceUpdates[accountId])
    }
    
    @Test
    fun `syncTransactions should add new transactions`() = runTest {
        // Given
        val accountId = "acc1"
        val account = createTestAccount(accountId, "inst1")
        val newTransaction = createTestTransaction("txn1", accountId)
        
        mockAccountRepository.accounts = listOf(account)
        mockTransactionRepository.existingTransactions = emptyList()
        mockPlaidService.transactions = listOf(newTransaction)
        mockPlaidService.shouldSucceed = true
        
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        // When
        val result = syncService.syncTransactions(accountId, startDate, endDate)
        
        // Then
        assertTrue(result is SyncResult.Success)
        assertEquals(1, result.transactionsAdded)
        assertEquals(0, result.transactionsUpdated)
        assertTrue(mockTransactionRepository.insertedTransactions.contains(newTransaction))
    }
    
    @Test
    fun `syncTransactions should resolve conflicts for existing transactions`() = runTest {
        // Given
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
        
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        // When
        val result = syncService.syncTransactions(accountId, startDate, endDate)
        
        // Then
        assertTrue(result is SyncResult.Success)
        assertEquals(0, result.transactionsAdded)
        assertEquals(1, result.transactionsUpdated)
        assertEquals(1, result.conflictsResolved)
        assertTrue(mockTransactionRepository.updatedTransactions.contains(remoteTransaction))
    }
    
    @Test
    fun `incrementalSync should only sync accounts that need syncing`() = runTest {
        // Given
        val userId = "user123"
        val now = Clock.System.now()
        val recentlyUpdated = createTestAccount("acc1", "inst1", lastUpdated = now.minus(5, DateTimeUnit.MINUTE))
        val needsSync = createTestAccount("acc2", "inst2", lastUpdated = now.minus(20, DateTimeUnit.MINUTE))
        
        mockAccountRepository.accounts = listOf(recentlyUpdated, needsSync)
        mockPlaidService.shouldSucceed = true
        
        // When
        val result = syncService.incrementalSync(userId)
        
        // Then
        assertTrue(result is SyncResult.Success)
        assertEquals(1, result.accountsUpdated) // Only one account needed syncing
    }
    
    @Test
    fun `syncAccount should handle authentication errors`() = runTest {
        // Given
        val accountId = "acc1"
        val account = createTestAccount(accountId, "inst1")
        
        mockAccountRepository.accounts = listOf(account)
        mockPlaidService.shouldSucceed = false
        mockPlaidService.error = PlaidServiceError.AuthenticationError("Invalid token")
        
        // When
        val result = syncService.syncAccount(accountId)
        
        // Then
        assertTrue(result is SyncResult.Failure)
        assertTrue(result.error is SyncError.AuthenticationError)
        assertTrue(mockSyncStatusManager.accountSyncStatusUpdates.contains(accountId to SyncStatus.ERROR))
    }
    
    @Test
    fun `syncAccount should handle network errors with retry`() = runTest {
        // Given
        val accountId = "acc1"
        val account = createTestAccount(accountId, "inst1")
        
        mockAccountRepository.accounts = listOf(account)
        mockPlaidService.shouldSucceed = false
        mockPlaidService.error = PlaidServiceError.NetworkError("Connection timeout")
        mockRetryManager.shouldRetry = true
        
        // When
        val result = syncService.syncAccount(accountId)
        
        // Then
        assertTrue(result is SyncResult.Failure)
        assertTrue(result.error is SyncError.NetworkError)
        assertTrue(mockRetryManager.retryAttempts > 0)
    }
    
    @Test
    fun `syncAllAccounts should send success notification when sync completes successfully`() = runTest {
        // Given
        val userId = "user123"
        val account = createTestAccount("acc1", "inst1")
        
        mockAccountRepository.accounts = listOf(account)
        mockPlaidService.shouldSucceed = true
        mockPlaidService.balances = mapOf("acc1" to Money.fromDollars(1200.0))
        
        // When
        val result = syncService.syncAllAccounts(userId)
        
        // Then
        assertTrue(result is SyncResult.Success)
        assertEquals(1, mockNotificationManager.successNotifications.size)
        assertEquals(userId, mockNotificationManager.successNotifications.first().first)
    }
    
    @Test
    fun `syncAllAccounts should send failure notification when sync fails`() = runTest {
        // Given
        val userId = "user123"
        
        mockAccountRepository.accounts = emptyList()
        mockAccountRepository.shouldFail = true
        
        // When
        val result = syncService.syncAllAccounts(userId)
        
        // Then
        assertTrue(result is SyncResult.Failure)
        assertEquals(1, mockNotificationManager.failureNotifications.size)
        assertEquals(userId, mockNotificationManager.failureNotifications.first().first)
    }
    
    @Test
    fun `syncAllAccounts should send conflict resolved notification when conflicts are resolved`() = runTest {
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
        val result = syncService.syncAllAccounts(userId)
        
        // Then
        assertTrue(result is SyncResult.Success)
        assertEquals(1, result.conflictsResolved)
        assertEquals(1, mockNotificationManager.conflictResolvedNotifications.size)
        assertEquals(userId, mockNotificationManager.conflictResolvedNotifications.first().first)
        assertEquals(1, mockNotificationManager.conflictResolvedNotifications.first().second)
    }
    
    private fun createTestAccount(
        id: String,
        institutionId: String,
        balance: Money = Money.fromDollars(1000.0),
        isActive: Boolean = true,
        lastUpdated: Instant = Clock.System.now()
    ): Account {
        return Account(
            id = id,
            institutionId = institutionId,
            institutionName = "Test Bank",
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
        date: LocalDate = LocalDate(2024, 1, 15)
    ): Transaction {
        return Transaction(
            id = id,
            accountId = accountId,
            amount = amount,
            description = "Test Transaction",
            category = Category.UNCATEGORIZED,
            date = date
        )
    }
}