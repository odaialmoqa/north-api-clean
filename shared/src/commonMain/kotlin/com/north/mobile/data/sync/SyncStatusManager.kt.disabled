package com.north.mobile.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Manages sync status for accounts and users
 */
interface SyncStatusManager {
    /**
     * Update sync status for a user (affects all their accounts)
     */
    suspend fun updateUserSyncStatus(userId: String, status: SyncStatus)
    
    /**
     * Update sync status for a specific account
     */
    suspend fun updateAccountSyncStatus(accountId: String, status: SyncStatus, error: SyncError? = null)
    
    /**
     * Update sync progress for a specific account
     */
    suspend fun updateAccountProgress(accountId: String, progress: SyncProgress)
    
    /**
     * Get sync status for all accounts of a user
     */
    fun getUserSyncStatus(userId: String): Flow<List<AccountSyncStatus>>
    
    /**
     * Get sync status for a specific account
     */
    fun getAccountSyncStatus(accountId: String): Flow<AccountSyncStatus>
    
    /**
     * Record successful sync completion
     */
    suspend fun recordSyncCompletion(accountId: String, result: SyncResult)
    
    /**
     * Schedule next sync time for an account
     */
    suspend fun scheduleNextSync(accountId: String, nextSyncTime: Instant)
}

class SyncStatusManagerImpl : SyncStatusManager {
    
    private val accountStatusMap = MutableStateFlow<Map<String, AccountSyncStatus>>(emptyMap())
    private val userAccountsMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    
    override suspend fun updateUserSyncStatus(userId: String, status: SyncStatus) {
        val accountIds = userAccountsMap.value[userId] ?: emptyList()
        
        val currentStatuses = accountStatusMap.value.toMutableMap()
        
        for (accountId in accountIds) {
            val currentStatus = currentStatuses[accountId] ?: createDefaultStatus(accountId)
            currentStatuses[accountId] = currentStatus.copy(
                status = status,
                error = if (status == SyncStatus.ERROR) currentStatus.error else null
            )
        }
        
        accountStatusMap.value = currentStatuses
    }
    
    override suspend fun updateAccountSyncStatus(accountId: String, status: SyncStatus, error: SyncError?) {
        val currentStatuses = accountStatusMap.value.toMutableMap()
        val currentStatus = currentStatuses[accountId] ?: createDefaultStatus(accountId)
        
        currentStatuses[accountId] = currentStatus.copy(
            status = status,
            error = error,
            lastSyncTime = if (status == SyncStatus.SUCCESS) Clock.System.now() else currentStatus.lastSyncTime
        )
        
        accountStatusMap.value = currentStatuses
    }
    
    override suspend fun updateAccountProgress(accountId: String, progress: SyncProgress) {
        val currentStatuses = accountStatusMap.value.toMutableMap()
        val currentStatus = currentStatuses[accountId] ?: createDefaultStatus(accountId)
        
        currentStatuses[accountId] = currentStatus.copy(
            progress = progress,
            status = SyncStatus.SYNCING
        )
        
        accountStatusMap.value = currentStatuses
    }
    
    override fun getUserSyncStatus(userId: String): Flow<List<AccountSyncStatus>> {
        return accountStatusMap.map { statusMap ->
            val accountIds = userAccountsMap.value[userId] ?: emptyList()
            accountIds.mapNotNull { accountId ->
                statusMap[accountId]
            }
        }
    }
    
    override fun getAccountSyncStatus(accountId: String): Flow<AccountSyncStatus> {
        return accountStatusMap.map { statusMap ->
            statusMap[accountId] ?: createDefaultStatus(accountId)
        }
    }
    
    override suspend fun recordSyncCompletion(accountId: String, result: SyncResult) {
        val currentStatuses = accountStatusMap.value.toMutableMap()
        val currentStatus = currentStatuses[accountId] ?: createDefaultStatus(accountId)
        
        val status = when (result) {
            is SyncResult.Success -> SyncStatus.SUCCESS
            is SyncResult.PartialSuccess -> SyncStatus.ERROR
            is SyncResult.Failure -> SyncStatus.ERROR
        }
        
        val error = when (result) {
            is SyncResult.PartialSuccess -> result.errors.firstOrNull()
            is SyncResult.Failure -> result.error
            else -> null
        }
        
        currentStatuses[accountId] = currentStatus.copy(
            status = status,
            lastSyncTime = Clock.System.now(),
            error = error,
            progress = null // Clear progress after completion
        )
        
        accountStatusMap.value = currentStatuses
    }
    
    override suspend fun scheduleNextSync(accountId: String, nextSyncTime: Instant) {
        val currentStatuses = accountStatusMap.value.toMutableMap()
        val currentStatus = currentStatuses[accountId] ?: createDefaultStatus(accountId)
        
        currentStatuses[accountId] = currentStatus.copy(
            nextSyncTime = nextSyncTime
        )
        
        accountStatusMap.value = currentStatuses
    }
    
    /**
     * Register an account with a user (for tracking purposes)
     */
    suspend fun registerAccountForUser(userId: String, accountId: String) {
        val currentUserAccounts = userAccountsMap.value.toMutableMap()
        val userAccounts = currentUserAccounts[userId]?.toMutableList() ?: mutableListOf()
        
        if (!userAccounts.contains(accountId)) {
            userAccounts.add(accountId)
            currentUserAccounts[userId] = userAccounts
            userAccountsMap.value = currentUserAccounts
        }
        
        // Initialize account status if not exists
        val currentStatuses = accountStatusMap.value.toMutableMap()
        if (!currentStatuses.containsKey(accountId)) {
            currentStatuses[accountId] = createDefaultStatus(accountId)
            accountStatusMap.value = currentStatuses
        }
    }
    
    /**
     * Unregister an account from a user
     */
    suspend fun unregisterAccountForUser(userId: String, accountId: String) {
        val currentUserAccounts = userAccountsMap.value.toMutableMap()
        val userAccounts = currentUserAccounts[userId]?.toMutableList()
        
        if (userAccounts != null) {
            userAccounts.remove(accountId)
            currentUserAccounts[userId] = userAccounts
            userAccountsMap.value = currentUserAccounts
        }
        
        // Remove account status
        val currentStatuses = accountStatusMap.value.toMutableMap()
        currentStatuses.remove(accountId)
        accountStatusMap.value = currentStatuses
    }
    
    private fun createDefaultStatus(accountId: String): AccountSyncStatus {
        return AccountSyncStatus(
            accountId = accountId,
            status = SyncStatus.IDLE,
            lastSyncTime = null,
            nextSyncTime = null,
            error = null,
            progress = null
        )
    }
}