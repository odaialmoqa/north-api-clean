package com.north.mobile.data.sync

import com.north.mobile.data.plaid.*
import com.north.mobile.data.repository.AccountRepository
import com.north.mobile.data.repository.TransactionRepository
import com.north.mobile.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.time.Duration

// Mock PlaidService
class MockPlaidService : PlaidService {
    var shouldSucceed = true
    var error: PlaidServiceError? = null
    var balances = mapOf<String, Money>()
    var transactions = listOf<Transaction>()
    
    override suspend fun createLinkToken(userId: String): Result<PlaidLinkToken> {
        return if (shouldSucceed) {
            Result.success(PlaidLinkToken("token", Instant.DISTANT_FUTURE, "request123"))
        } else {
            Result.failure(error ?: PlaidServiceError.UnknownError("Mock error"))
        }
    }
    
    override suspend fun exchangePublicToken(publicToken: String): Result<PlaidAccessToken> {
        return if (shouldSucceed) {
            Result.success(PlaidAccessToken("access_token", "item123", "inst123", "Test Bank"))
        } else {
            Result.failure(error ?: PlaidServiceError.UnknownError("Mock error"))
        }
    }
    
    override suspend fun getAccounts(accessToken: String): Result<List<PlaidAccount>> {
        return if (shouldSucceed) {
            Result.success(emptyList())
        } else {
            Result.failure(error ?: PlaidServiceError.UnknownError("Mock error"))
        }
    }
    
    override suspend fun getBalances(accessToken: String): Result<List<PlaidAccount>> {
        return if (shouldSucceed) {
            val plaidAccounts = balances.map { (accountId, balance) ->
                PlaidAccount(
                    accountId = accountId,
                    itemId = "item123",
                    name = "Test Account",
                    type = PlaidAccountType.DEPOSITORY,
                    balances = PlaidBalances(
                        current = balance.dollars,
                        available = balance.dollars,
                        isoCurrencyCode = "CAD"
                    )
                )
            }
            Result.success(plaidAccounts)
        } else {
            Result.failure(error ?: PlaidServiceError.UnknownError("Mock error"))
        }
    }
    
    override suspend fun getTransactions(
        accessToken: String,
        startDate: LocalDate,
        endDate: LocalDate,
        accountIds: List<String>?
    ): Result<List<Transaction>> {
        return if (shouldSucceed) {
            Result.success(transactions)
        } else {
            Result.failure(error ?: PlaidServiceError.UnknownError("Mock error"))
        }
    }
    
    override suspend fun getItem(accessToken: String): Result<PlaidItem> {
        return if (shouldSucceed) {
            Result.success(PlaidItem("item123", "inst123", null, null, emptyList(), emptyList()))
        } else {
            Result.failure(error ?: PlaidServiceError.UnknownError("Mock error"))
        }
    }
    
    override suspend fun removeItem(accessToken: String): Result<Unit> {
        return if (shouldSucceed) Result.success(Unit) else Result.failure(error ?: PlaidServiceError.UnknownError("Mock error"))
    }
    
    override suspend fun createUpdateLinkToken(accessToken: String): Result<PlaidLinkToken> {
        return createLinkToken("user")
    }
    
    override suspend fun getCanadianInstitutions(): Result<List<FinancialInstitution>> {
        return Result.success(emptyList())
    }
    
    override suspend fun searchInstitutions(query: String): Result<List<FinancialInstitution>> {
        return Result.success(emptyList())
    }
}

// Mock AccountRepository
class MockAccountRepository : AccountRepository {
    var accounts = listOf<Account>()
    var balanceUpdates = mutableMapOf<String, Money>()
    var shouldFail = false
    
    override suspend fun insert(entity: Account): Result<Account> = Result.success(entity)
    override suspend fun update(entity: Account): Result<Account> = Result.success(entity)
    override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    
    override suspend fun findById(id: String): Result<Account?> {
        return Result.success(accounts.find { it.id == id })
    }
    
    override suspend fun findAll(): Result<List<Account>> = Result.success(accounts)
    
    override suspend fun findByUserId(userId: String): Result<List<Account>> {
        return if (shouldFail) {
            Result.failure(Exception("Mock repository failure"))
        } else {
            Result.success(accounts)
        }
    }
    
    override suspend fun findByInstitution(userId: String, institutionId: String): Result<List<Account>> {
        return Result.success(accounts.filter { it.institutionId == institutionId })
    }
    
    override suspend fun updateBalance(accountId: String, balance: Money): Result<Unit> {
        balanceUpdates[accountId] = balance
        return Result.success(Unit)
    }
    
    override suspend fun deactivateAccount(accountId: String): Result<Unit> = Result.success(Unit)
    override suspend fun saveAccount(account: Account): Result<Account> = Result.success(account)
    override suspend fun updateAccount(account: Account): Result<Account> = Result.success(account)
    override suspend fun getAllAccounts(): List<Account> = accounts
    override suspend fun getAccountById(accountId: String): Account? = accounts.find { it.id == accountId }
}

// Mock TransactionRepository
class MockTransactionRepository : TransactionRepository {
    var existingTransactions = listOf<Transaction>()
    var insertedTransactions = mutableListOf<Transaction>()
    var updatedTransactions = mutableListOf<Transaction>()
    
    override suspend fun insert(entity: Transaction): Result<Transaction> {
        insertedTransactions.add(entity)
        return Result.success(entity)
    }
    
    override suspend fun update(entity: Transaction): Result<Transaction> {
        updatedTransactions.add(entity)
        return Result.success(entity)
    }
    
    override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    
    override suspend fun findById(id: String): Result<Transaction?> {
        return Result.success(existingTransactions.find { it.id == id })
    }
    
    override suspend fun findAll(): Result<List<Transaction>> = Result.success(existingTransactions)
    
    override suspend fun findByAccountId(accountId: String): Result<List<Transaction>> {
        return Result.success(existingTransactions.filter { it.accountId == accountId })
    }
    
    override suspend fun findByAccountAndDateRange(
        accountId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<Transaction>> {
        return Result.success(existingTransactions.filter { 
            it.accountId == accountId && it.date >= startDate && it.date <= endDate 
        })
    }
    
    override suspend fun findByCategory(categoryId: String): Result<List<Transaction>> {
        return Result.success(existingTransactions.filter { it.category.id == categoryId })
    }
    
    override suspend fun findByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<Transaction>> {
        return Result.success(existingTransactions.filter { it.date >= startDate && it.date <= endDate })
    }
    
    override suspend fun updateCategory(transactionId: String, category: Category): Result<Unit> = Result.success(Unit)
    override suspend fun markAsRecurring(transactionId: String, isRecurring: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun findDuplicates(transaction: Transaction): Result<List<Transaction>> = Result.success(emptyList())
}

// Mock ConflictResolver
class MockConflictResolver : ConflictResolver {
    var conflicts = mapOf<Pair<Transaction, Transaction>, ConflictDetails>()
    var resolutions = mapOf<ConflictDetails, ConflictDetails>()
    
    override fun detectTransactionConflict(local: Transaction, remote: Transaction): ConflictDetails? {
        return conflicts[local to remote]
    }
    
    override fun detectAccountConflict(local: Account, remote: Account): ConflictDetails? = null
    
    override fun resolveTransactionConflict(conflict: ConflictDetails): ConflictDetails {
        return resolutions[conflict] ?: conflict
    }
    
    override fun resolveAccountConflict(conflict: ConflictDetails): ConflictDetails = conflict
}

// Mock SyncStatusManager
class MockSyncStatusManager : SyncStatusManager {
    var userSyncStatusUpdates = mutableListOf<Pair<String, SyncStatus>>()
    var accountSyncStatusUpdates = mutableListOf<Pair<String, SyncStatus>>()
    var progressUpdates = mutableListOf<Pair<String, SyncProgress>>()
    
    override suspend fun updateUserSyncStatus(userId: String, status: SyncStatus) {
        userSyncStatusUpdates.add(userId to status)
    }
    
    override suspend fun updateAccountSyncStatus(accountId: String, status: SyncStatus, error: SyncError?) {
        accountSyncStatusUpdates.add(accountId to status)
    }
    
    override suspend fun updateAccountProgress(accountId: String, progress: SyncProgress) {
        progressUpdates.add(accountId to progress)
    }
    
    override fun getUserSyncStatus(userId: String): Flow<List<AccountSyncStatus>> {
        return flowOf(emptyList())
    }
    
    override fun getAccountSyncStatus(accountId: String): Flow<AccountSyncStatus> {
        return flowOf(AccountSyncStatus(accountId, SyncStatus.IDLE, null, null))
    }
    
    override suspend fun recordSyncCompletion(accountId: String, result: SyncResult) {}
    override suspend fun scheduleNextSync(accountId: String, nextSyncTime: Instant) {}
}

// Mock RetryManager
class MockRetryManager : RetryManager {
    var shouldRetry = false
    var retryAttempts = 0
    
    override suspend fun <T> withRetry(
        maxAttempts: Int,
        initialDelay: Duration,
        maxDelay: Duration,
        backoffMultiplier: Double,
        jitterRange: Double,
        retryCondition: (Throwable) -> Boolean,
        operation: suspend () -> Result<T>
    ): Result<T> {
        retryAttempts++
        return operation()
    }
    
    override fun shouldRetry(error: Throwable, attempt: Int, maxAttempts: Int): Boolean = shouldRetry
    
    override fun calculateDelay(
        attempt: Int,
        initialDelay: Duration,
        maxDelay: Duration,
        backoffMultiplier: Double,
        jitterRange: Double
    ): Duration = Duration.ZERO
}

// Mock SyncNotificationManager
class MockSyncNotificationManager : SyncNotificationManager {
    var successNotifications = mutableListOf<Pair<String, SyncResult.Success>>()
    var failureNotifications = mutableListOf<Pair<String, SyncError>>()
    var partialSuccessNotifications = mutableListOf<Pair<String, SyncResult.PartialSuccess>>()
    var reauthNotifications = mutableListOf<Triple<String, String, String>>()
    var newTransactionNotifications = mutableListOf<Triple<String, String, Int>>()
    var conflictResolvedNotifications = mutableListOf<Pair<String, Int>>()
    var delayedNotifications = mutableListOf<Pair<String, String>>()
    var cancelledNotifications = mutableListOf<String>()
    
    override suspend fun notifySyncSuccess(userId: String, result: SyncResult.Success) {
        successNotifications.add(userId to result)
    }
    
    override suspend fun notifySyncFailure(userId: String, error: SyncError) {
        failureNotifications.add(userId to error)
    }
    
    override suspend fun notifySyncPartialSuccess(userId: String, result: SyncResult.PartialSuccess) {
        partialSuccessNotifications.add(userId to result)
    }
    
    override suspend fun notifyReauthRequired(userId: String, accountId: String, institutionName: String) {
        reauthNotifications.add(Triple(userId, accountId, institutionName))
    }
    
    override suspend fun notifyNewTransactions(userId: String, accountId: String, transactionCount: Int) {
        newTransactionNotifications.add(Triple(userId, accountId, transactionCount))
    }
    
    override suspend fun notifyConflictsResolved(userId: String, conflictCount: Int) {
        conflictResolvedNotifications.add(userId to conflictCount)
    }
    
    override suspend fun notifySyncDelayed(userId: String, accountId: String) {
        delayedNotifications.add(userId to accountId)
    }
    
    override suspend fun cancelNotifications(userId: String) {
        cancelledNotifications.add(userId)
    }
}