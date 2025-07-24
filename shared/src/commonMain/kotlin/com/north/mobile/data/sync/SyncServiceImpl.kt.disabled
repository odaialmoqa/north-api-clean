package com.north.mobile.data.sync

import com.north.mobile.data.plaid.PlaidService
import com.north.mobile.data.plaid.PlaidServiceError
import com.north.mobile.data.repository.AccountRepository
import com.north.mobile.data.repository.TransactionRepository
import com.north.mobile.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SyncServiceImpl(
    private val plaidService: PlaidService,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val conflictResolver: ConflictResolver,
    private val syncStatusManager: SyncStatusManager,
    private val retryManager: RetryManager,
    private val notificationManager: SyncNotificationManager,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : SyncService {
    
    private val activeSyncJobs = mutableMapOf<String, Job>()
    private val backgroundSyncJobs = mutableMapOf<String, Job>()
    
    override suspend fun syncAllAccounts(userId: String): SyncResult {
        val startTime = Clock.System.now()
        
        return try {
            syncStatusManager.updateUserSyncStatus(userId, SyncStatus.SYNCING)
            
            // Get all user accounts
            val accountsResult = accountRepository.findByUserId(userId)
            if (accountsResult.isFailure) {
                return SyncResult.Failure(
                    SyncError.UnknownError("Failed to fetch user accounts", accountsResult.exceptionOrNull()),
                    Clock.System.now().minus(startTime).inWholeMilliseconds
                )
            }
            
            val accounts = accountsResult.getOrThrow()
            val syncResults = mutableListOf<SyncResult>()
            
            // Sync each account
            for (account in accounts) {
                if (!account.isActive) continue
                
                val accountSyncResult = syncAccount(account.id)
                syncResults.add(accountSyncResult)
            }
            
            // Aggregate results
            val totalAccountsUpdated = syncResults.sumOf { 
                when (it) {
                    is SyncResult.Success -> it.accountsUpdated
                    is SyncResult.PartialSuccess -> it.accountsUpdated
                    else -> 0
                }
            }
            
            val totalTransactionsAdded = syncResults.sumOf {
                when (it) {
                    is SyncResult.Success -> it.transactionsAdded
                    is SyncResult.PartialSuccess -> it.transactionsAdded
                    else -> 0
                }
            }
            
            val totalTransactionsUpdated = syncResults.sumOf {
                when (it) {
                    is SyncResult.Success -> it.transactionsUpdated
                    is SyncResult.PartialSuccess -> it.transactionsUpdated
                    else -> 0
                }
            }
            
            val totalConflictsResolved = syncResults.sumOf {
                when (it) {
                    is SyncResult.Success -> it.conflictsResolved
                    is SyncResult.PartialSuccess -> it.conflictsResolved
                    else -> 0
                }
            }
            
            val errors = syncResults.filterIsInstance<SyncResult.PartialSuccess>()
                .flatMap { it.errors } + 
                syncResults.filterIsInstance<SyncResult.Failure>()
                .map { it.error }
            
            val syncDuration = Clock.System.now().minus(startTime).inWholeMilliseconds
            
            val result = if (errors.isEmpty()) {
                SyncResult.Success(
                    accountsUpdated = totalAccountsUpdated,
                    transactionsAdded = totalTransactionsAdded,
                    transactionsUpdated = totalTransactionsUpdated,
                    conflictsResolved = totalConflictsResolved,
                    syncDuration = syncDuration
                )
            } else {
                SyncResult.PartialSuccess(
                    accountsUpdated = totalAccountsUpdated,
                    transactionsAdded = totalTransactionsAdded,
                    transactionsUpdated = totalTransactionsUpdated,
                    conflictsResolved = totalConflictsResolved,
                    errors = errors,
                    syncDuration = syncDuration
                )
            }
            
            syncStatusManager.updateUserSyncStatus(
                userId, 
                if (errors.isEmpty()) SyncStatus.SUCCESS else SyncStatus.ERROR
            )
            
            result
            
        } catch (e: Exception) {
            syncStatusManager.updateUserSyncStatus(userId, SyncStatus.ERROR)
            val error = SyncError.UnknownError("Sync failed", e)
            notificationManager.notifySyncFailure(userId, error)
            SyncResult.Failure(
                error,
                Clock.System.now().minus(startTime).inWholeMilliseconds
            )
        }
    }
    
    override suspend fun syncAccount(accountId: String): SyncResult {
        val startTime = Clock.System.now()
        
        return try {
            syncStatusManager.updateAccountSyncStatus(accountId, SyncStatus.SYNCING)
            
            // Get account details
            val accountResult = accountRepository.findById(accountId)
            if (accountResult.isFailure) {
                return SyncResult.Failure(
                    SyncError.UnknownError("Failed to fetch account", accountResult.exceptionOrNull()),
                    Clock.System.now().minus(startTime).inWholeMilliseconds
                )
            }
            
            val account = accountResult.getOrThrow()
                ?: return SyncResult.Failure(
                    SyncError.UnknownError("Account not found"),
                    Clock.System.now().minus(startTime).inWholeMilliseconds
                )
            
            // Get access token for this account's institution
            val accessToken = getAccessTokenForAccount(account)
                ?: return SyncResult.Failure(
                    SyncError.AuthenticationError("No access token found", accountId),
                    Clock.System.now().minus(startTime).inWholeMilliseconds
                )
            
            var accountsUpdated = 0
            var transactionsAdded = 0
            var transactionsUpdated = 0
            var conflictsResolved = 0
            val errors = mutableListOf<SyncError>()
            
            // Update progress
            syncStatusManager.updateAccountProgress(accountId, SyncProgress("Syncing account balance", 1, 3))
            
            // Sync account balance
            try {
                val balanceResult = plaidService.getBalances(accessToken)
                if (balanceResult.isSuccess) {
                    val plaidAccounts = balanceResult.getOrThrow()
                    val matchingAccount = plaidAccounts.find { it.accountId == accountId }
                    
                    if (matchingAccount != null) {
                        val newBalance = Money.fromDollars(matchingAccount.balances.current ?: 0.0)
                        if (newBalance != account.balance) {
                            accountRepository.updateBalance(accountId, newBalance)
                            accountsUpdated++
                        }
                    }
                }
            } catch (e: Exception) {
                errors.add(SyncError.NetworkError("Failed to sync account balance", e))
            }
            
            // Update progress
            syncStatusManager.updateAccountProgress(accountId, SyncProgress("Syncing transactions", 2, 3))
            
            // Sync transactions (last 30 days for incremental sync)
            try {
                val endDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val startDate = endDate.minus(30, DateTimeUnit.DAY)
                
                val transactionResult = syncTransactions(accountId, startDate, endDate)
                when (transactionResult) {
                    is SyncResult.Success -> {
                        transactionsAdded += transactionResult.transactionsAdded
                        transactionsUpdated += transactionResult.transactionsUpdated
                        conflictsResolved += transactionResult.conflictsResolved
                    }
                    is SyncResult.PartialSuccess -> {
                        transactionsAdded += transactionResult.transactionsAdded
                        transactionsUpdated += transactionResult.transactionsUpdated
                        conflictsResolved += transactionResult.conflictsResolved
                        errors.addAll(transactionResult.errors)
                    }
                    is SyncResult.Failure -> {
                        errors.add(transactionResult.error)
                    }
                }
            } catch (e: Exception) {
                errors.add(SyncError.NetworkError("Failed to sync transactions", e))
            }
            
            // Update progress
            syncStatusManager.updateAccountProgress(accountId, SyncProgress("Finalizing sync", 3, 3))
            
            val syncDuration = Clock.System.now().minus(startTime).inWholeMilliseconds
            
            val result = if (errors.isEmpty()) {
                SyncResult.Success(
                    accountsUpdated = accountsUpdated,
                    transactionsAdded = transactionsAdded,
                    transactionsUpdated = transactionsUpdated,
                    conflictsResolved = conflictsResolved,
                    syncDuration = syncDuration
                )
            } else {
                SyncResult.PartialSuccess(
                    accountsUpdated = accountsUpdated,
                    transactionsAdded = transactionsAdded,
                    transactionsUpdated = transactionsUpdated,
                    conflictsResolved = conflictsResolved,
                    errors = errors,
                    syncDuration = syncDuration
                )
            }
            
            syncStatusManager.updateAccountSyncStatus(
                accountId,
                if (errors.isEmpty()) SyncStatus.SUCCESS else SyncStatus.ERROR
            )
            
            result
            
        } catch (e: Exception) {
            syncStatusManager.updateAccountSyncStatus(accountId, SyncStatus.ERROR)
            SyncResult.Failure(
                SyncError.UnknownError("Account sync failed", e),
                Clock.System.now().minus(startTime).inWholeMilliseconds
            )
        }
    }
    
    override suspend fun syncTransactions(
        accountId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): SyncResult {
        val startTime = Clock.System.now()
        
        return try {
            // Get account details
            val accountResult = accountRepository.findById(accountId)
            if (accountResult.isFailure) {
                return SyncResult.Failure(
                    SyncError.UnknownError("Failed to fetch account", accountResult.exceptionOrNull()),
                    Clock.System.now().minus(startTime).inWholeMilliseconds
                )
            }
            
            val account = accountResult.getOrThrow()
                ?: return SyncResult.Failure(
                    SyncError.UnknownError("Account not found"),
                    Clock.System.now().minus(startTime).inWholeMilliseconds
                )
            
            // Get access token
            val accessToken = getAccessTokenForAccount(account)
                ?: return SyncResult.Failure(
                    SyncError.AuthenticationError("No access token found", accountId),
                    Clock.System.now().minus(startTime).inWholeMilliseconds
                )
            
            // Fetch transactions from Plaid
            val transactionResult = retryManager.withRetry {
                plaidService.getTransactions(accessToken, startDate, endDate, listOf(accountId))
            }
            
            if (transactionResult.isFailure) {
                val error = when (val exception = transactionResult.exceptionOrNull()) {
                    is PlaidServiceError.RateLimitError -> SyncError.RateLimitError(exception.message)
                    is PlaidServiceError.AuthenticationError -> SyncError.AuthenticationError(exception.message, accountId)
                    is PlaidServiceError.NetworkError -> SyncError.NetworkError(exception.message, exception.cause)
                    else -> SyncError.UnknownError("Failed to fetch transactions", exception)
                }
                
                return SyncResult.Failure(
                    error,
                    Clock.System.now().minus(startTime).inWholeMilliseconds
                )
            }
            
            val remoteTransactions = transactionResult.getOrThrow()
            
            // Get existing transactions for conflict detection
            val existingTransactionsResult = transactionRepository.findByAccountAndDateRange(accountId, startDate, endDate)
            val existingTransactions = existingTransactionsResult.getOrElse { emptyList() }
            
            var transactionsAdded = 0
            var transactionsUpdated = 0
            var conflictsResolved = 0
            val errors = mutableListOf<SyncError>()
            
            // Process each remote transaction
            for (remoteTransaction in remoteTransactions) {
                try {
                    val existingTransaction = existingTransactions.find { it.id == remoteTransaction.id }
                    
                    if (existingTransaction == null) {
                        // New transaction - add it
                        val insertResult = transactionRepository.insert(remoteTransaction)
                        if (insertResult.isSuccess) {
                            transactionsAdded++
                        } else {
                            errors.add(SyncError.ValidationError(
                                "Failed to insert transaction",
                                mapOf("transactionId" to remoteTransaction.id)
                            ))
                        }
                    } else {
                        // Existing transaction - check for conflicts
                        val conflict = conflictResolver.detectTransactionConflict(existingTransaction, remoteTransaction)
                        
                        if (conflict != null) {
                            val resolution = conflictResolver.resolveTransactionConflict(conflict)
                            
                            when (resolution.resolution) {
                                ConflictResolution.USE_REMOTE -> {
                                    val updateResult = transactionRepository.update(remoteTransaction)
                                    if (updateResult.isSuccess) {
                                        transactionsUpdated++
                                        conflictsResolved++
                                    } else {
                                        errors.add(SyncError.ValidationError(
                                            "Failed to update transaction",
                                            mapOf("transactionId" to remoteTransaction.id)
                                        ))
                                    }
                                }
                                ConflictResolution.USE_LOCAL -> {
                                    // Keep local version, no action needed
                                    conflictsResolved++
                                }
                                ConflictResolution.MERGE -> {
                                    // Implement merge logic if needed
                                    conflictsResolved++
                                }
                                ConflictResolution.MANUAL_REVIEW_REQUIRED -> {
                                    errors.add(SyncError.DataConflictError(
                                        "Manual review required for transaction conflict",
                                        resolution
                                    ))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    errors.add(SyncError.UnknownError("Failed to process transaction ${remoteTransaction.id}", e))
                }
            }
            
            val syncDuration = Clock.System.now().minus(startTime).inWholeMilliseconds
            
            if (errors.isEmpty()) {
                SyncResult.Success(
                    accountsUpdated = 0,
                    transactionsAdded = transactionsAdded,
                    transactionsUpdated = transactionsUpdated,
                    conflictsResolved = conflictsResolved,
                    syncDuration = syncDuration
                )
            } else {
                SyncResult.PartialSuccess(
                    accountsUpdated = 0,
                    transactionsAdded = transactionsAdded,
                    transactionsUpdated = transactionsUpdated,
                    conflictsResolved = conflictsResolved,
                    errors = errors,
                    syncDuration = syncDuration
                )
            }
            
        } catch (e: Exception) {
            SyncResult.Failure(
                SyncError.UnknownError("Transaction sync failed", e),
                Clock.System.now().minus(startTime).inWholeMilliseconds
            )
        }
    }
    
    override suspend fun incrementalSync(userId: String): SyncResult {
        val startTime = Clock.System.now()
        
        return try {
            syncStatusManager.updateUserSyncStatus(userId, SyncStatus.SYNCING)
            
            // Get accounts that need syncing (based on last sync time)
            val accountsResult = accountRepository.findByUserId(userId)
            if (accountsResult.isFailure) {
                return SyncResult.Failure(
                    SyncError.UnknownError("Failed to fetch user accounts", accountsResult.exceptionOrNull()),
                    Clock.System.now().minus(startTime).inWholeMilliseconds
                )
            }
            
            val accounts = accountsResult.getOrThrow()
            val now = Clock.System.now()
            val syncThreshold = now.minus(15.minutes) // Only sync if last sync was more than 15 minutes ago
            
            val accountsToSync = accounts.filter { account ->
                account.isActive && (account.lastUpdated < syncThreshold)
            }
            
            if (accountsToSync.isEmpty()) {
                return SyncResult.Success(
                    accountsUpdated = 0,
                    transactionsAdded = 0,
                    transactionsUpdated = 0,
                    conflictsResolved = 0,
                    syncDuration = Clock.System.now().minus(startTime).inWholeMilliseconds
                )
            }
            
            val syncResults = mutableListOf<SyncResult>()
            
            // Sync only accounts that need it
            for (account in accountsToSync) {
                val accountSyncResult = syncAccount(account.id)
                syncResults.add(accountSyncResult)
            }
            
            // Aggregate results (same logic as syncAllAccounts)
            val totalAccountsUpdated = syncResults.sumOf { 
                when (it) {
                    is SyncResult.Success -> it.accountsUpdated
                    is SyncResult.PartialSuccess -> it.accountsUpdated
                    else -> 0
                }
            }
            
            val totalTransactionsAdded = syncResults.sumOf {
                when (it) {
                    is SyncResult.Success -> it.transactionsAdded
                    is SyncResult.PartialSuccess -> it.transactionsAdded
                    else -> 0
                }
            }
            
            val totalTransactionsUpdated = syncResults.sumOf {
                when (it) {
                    is SyncResult.Success -> it.transactionsUpdated
                    is SyncResult.PartialSuccess -> it.transactionsUpdated
                    else -> 0
                }
            }
            
            val totalConflictsResolved = syncResults.sumOf {
                when (it) {
                    is SyncResult.Success -> it.conflictsResolved
                    is SyncResult.PartialSuccess -> it.conflictsResolved
                    else -> 0
                }
            }
            
            val errors = syncResults.filterIsInstance<SyncResult.PartialSuccess>()
                .flatMap { it.errors } + 
                syncResults.filterIsInstance<SyncResult.Failure>()
                .map { it.error }
            
            val syncDuration = Clock.System.now().minus(startTime).inWholeMilliseconds
            
            val result = if (errors.isEmpty()) {
                SyncResult.Success(
                    accountsUpdated = totalAccountsUpdated,
                    transactionsAdded = totalTransactionsAdded,
                    transactionsUpdated = totalTransactionsUpdated,
                    conflictsResolved = totalConflictsResolved,
                    syncDuration = syncDuration
                )
            } else {
                SyncResult.PartialSuccess(
                    accountsUpdated = totalAccountsUpdated,
                    transactionsAdded = totalTransactionsAdded,
                    transactionsUpdated = totalTransactionsUpdated,
                    conflictsResolved = totalConflictsResolved,
                    errors = errors,
                    syncDuration = syncDuration
                )
            }
            
            syncStatusManager.updateUserSyncStatus(
                userId, 
                if (errors.isEmpty()) SyncStatus.SUCCESS else SyncStatus.ERROR
            )
            
            result
            
        } catch (e: Exception) {
            syncStatusManager.updateUserSyncStatus(userId, SyncStatus.ERROR)
            SyncResult.Failure(
                SyncError.UnknownError("Incremental sync failed", e),
                Clock.System.now().minus(startTime).inWholeMilliseconds
            )
        }
    }
    
    override fun getSyncStatus(userId: String): Flow<List<AccountSyncStatus>> {
        return syncStatusManager.getUserSyncStatus(userId)
    }
    
    override fun getAccountSyncStatus(accountId: String): Flow<AccountSyncStatus> {
        return syncStatusManager.getAccountSyncStatus(accountId)
    }
    
    override suspend fun cancelSync(userId: String) {
        activeSyncJobs[userId]?.cancel()
        activeSyncJobs.remove(userId)
        syncStatusManager.updateUserSyncStatus(userId, SyncStatus.CANCELLED)
    }
    
    override suspend fun scheduleBackgroundSync(userId: String, intervalMinutes: Long) {
        // Cancel existing background sync if any
        stopBackgroundSync(userId)
        
        val job = coroutineScope.launch {
            while (isActive) {
                try {
                    delay(intervalMinutes.minutes)
                    incrementalSync(userId)
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    // Log error but continue background sync
                    println("Background sync error for user $userId: ${e.message}")
                }
            }
        }
        
        backgroundSyncJobs[userId] = job
    }
    
    override suspend fun stopBackgroundSync(userId: String) {
        backgroundSyncJobs[userId]?.cancel()
        backgroundSyncJobs.remove(userId)
    }
    
    private suspend fun getAccessTokenForAccount(account: Account): String? {
        // This would typically fetch the access token from secure storage
        // For now, return a placeholder - this should be implemented based on your auth system
        return "placeholder_access_token_${account.institutionId}"
    }
}