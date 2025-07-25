package com.north.mobile.data.sync

import com.north.mobile.domain.model.Account
import com.north.mobile.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Service responsible for synchronizing account data from external providers
 */
interface SyncService {
    /**
     * Sync all accounts for a user
     */
    suspend fun syncAllAccounts(userId: String): SyncResult
    
    /**
     * Sync specific account
     */
    suspend fun syncAccount(accountId: String): SyncResult
    
    /**
     * Sync transactions for an account within a date range
     */
    suspend fun syncTransactions(
        accountId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): SyncResult
    
    /**
     * Perform incremental sync (only new/changed data)
     */
    suspend fun incrementalSync(userId: String): SyncResult
    
    /**
     * Get sync status for all accounts
     */
    fun getSyncStatus(userId: String): Flow<List<AccountSyncStatus>>
    
    /**
     * Get sync status for specific account
     */
    fun getAccountSyncStatus(accountId: String): Flow<AccountSyncStatus>
    
    /**
     * Cancel ongoing sync operations
     */
    suspend fun cancelSync(userId: String)
    
    /**
     * Schedule background sync
     */
    suspend fun scheduleBackgroundSync(userId: String, intervalMinutes: Long = 60)
    
    /**
     * Stop background sync
     */
    suspend fun stopBackgroundSync(userId: String)
}

/**
 * Result of a sync operation
 */
sealed class SyncResult {
    data class Success(
        val accountsUpdated: Int,
        val transactionsAdded: Int,
        val transactionsUpdated: Int,
        val conflictsResolved: Int,
        val syncDuration: Long // milliseconds
    ) : SyncResult()
    
    data class PartialSuccess(
        val accountsUpdated: Int,
        val transactionsAdded: Int,
        val transactionsUpdated: Int,
        val conflictsResolved: Int,
        val errors: List<SyncError>,
        val syncDuration: Long
    ) : SyncResult()
    
    data class Failure(
        val error: SyncError,
        val syncDuration: Long
    ) : SyncResult()
}

/**
 * Sync status for an account
 */
data class AccountSyncStatus(
    val accountId: String,
    val status: SyncStatus,
    val lastSyncTime: Instant?,
    val nextSyncTime: Instant?,
    val error: SyncError? = null,
    val progress: SyncProgress? = null
)

/**
 * Overall sync status
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    CANCELLED
}

/**
 * Sync progress information
 */
data class SyncProgress(
    val currentStep: String,
    val completedSteps: Int,
    val totalSteps: Int,
    val percentage: Float = if (totalSteps > 0) (completedSteps.toFloat() / totalSteps) * 100 else 0f
)

/**
 * Sync error information
 */
sealed class SyncError : Exception() {
    data class NetworkError(override val message: String, override val cause: Throwable? = null) : SyncError()
    data class AuthenticationError(override val message: String, val accountId: String) : SyncError()
    data class RateLimitError(override val message: String, val retryAfter: Long? = null) : SyncError()
    data class DataConflictError(override val message: String, val conflictDetails: ConflictDetails) : SyncError()
    data class ValidationError(override val message: String, val fieldErrors: Map<String, String>) : SyncError()
    data class UnknownError(override val message: String, override val cause: Throwable? = null) : SyncError()
}

/**
 * Details about data conflicts during sync
 */
data class ConflictDetails(
    val conflictType: ConflictType,
    val localData: Any,
    val remoteData: Any,
    val resolution: ConflictResolution
)

enum class ConflictType {
    DUPLICATE_TRANSACTION,
    MODIFIED_TRANSACTION,
    BALANCE_MISMATCH,
    ACCOUNT_STATUS_CHANGE
}

enum class ConflictResolution {
    USE_REMOTE,
    USE_LOCAL,
    MERGE,
    MANUAL_REVIEW_REQUIRED
}