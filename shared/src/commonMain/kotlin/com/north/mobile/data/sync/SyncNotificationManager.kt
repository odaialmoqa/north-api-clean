package com.north.mobile.data.sync

import kotlinx.datetime.Instant

/**
 * Manages notifications for sync events and status updates
 */
interface SyncNotificationManager {
    /**
     * Send notification when sync completes successfully
     */
    suspend fun notifySyncSuccess(userId: String, result: SyncResult.Success)
    
    /**
     * Send notification when sync fails
     */
    suspend fun notifySyncFailure(userId: String, error: SyncError)
    
    /**
     * Send notification when sync completes with partial success
     */
    suspend fun notifySyncPartialSuccess(userId: String, result: SyncResult.PartialSuccess)
    
    /**
     * Send notification when account requires re-authentication
     */
    suspend fun notifyReauthRequired(userId: String, accountId: String, institutionName: String)
    
    /**
     * Send notification when new transactions are detected
     */
    suspend fun notifyNewTransactions(userId: String, accountId: String, transactionCount: Int)
    
    /**
     * Send notification when conflicts are resolved
     */
    suspend fun notifyConflictsResolved(userId: String, conflictCount: Int)
    
    /**
     * Send notification when sync is taking longer than expected
     */
    suspend fun notifySyncDelayed(userId: String, accountId: String)
    
    /**
     * Cancel all pending notifications for a user
     */
    suspend fun cancelNotifications(userId: String)
}

class SyncNotificationManagerImpl : SyncNotificationManager {
    
    override suspend fun notifySyncSuccess(userId: String, result: SyncResult.Success) {
        if (result.transactionsAdded > 0 || result.accountsUpdated > 0) {
            val message = buildString {
                append("Sync completed successfully")
                if (result.transactionsAdded > 0) {
                    append(" • ${result.transactionsAdded} new transactions")
                }
                if (result.accountsUpdated > 0) {
                    append(" • ${result.accountsUpdated} accounts updated")
                }
            }
            
            sendNotification(
                userId = userId,
                title = "Accounts Updated",
                message = message,
                type = NotificationType.SYNC_SUCCESS
            )
        }
    }
    
    override suspend fun notifySyncFailure(userId: String, error: SyncError) {
        val (title, message) = when (error) {
            is SyncError.NetworkError -> {
                "Connection Issue" to "Unable to sync your accounts. Please check your internet connection."
            }
            is SyncError.AuthenticationError -> {
                "Account Access Issue" to "Please re-authenticate your account to continue syncing."
            }
            is SyncError.RateLimitError -> {
                "Sync Temporarily Unavailable" to "Too many requests. Sync will resume automatically."
            }
            else -> {
                "Sync Error" to "There was an issue syncing your accounts. We'll try again shortly."
            }
        }
        
        sendNotification(
            userId = userId,
            title = title,
            message = message,
            type = NotificationType.SYNC_ERROR
        )
    }
    
    override suspend fun notifySyncPartialSuccess(userId: String, result: SyncResult.PartialSuccess) {
        val message = buildString {
            append("Sync completed with some issues")
            if (result.transactionsAdded > 0) {
                append(" • ${result.transactionsAdded} new transactions")
            }
            if (result.errors.isNotEmpty()) {
                append(" • ${result.errors.size} accounts had issues")
            }
        }
        
        sendNotification(
            userId = userId,
            title = "Partial Sync Complete",
            message = message,
            type = NotificationType.SYNC_PARTIAL
        )
    }
    
    override suspend fun notifyReauthRequired(userId: String, accountId: String, institutionName: String) {
        sendNotification(
            userId = userId,
            title = "Account Needs Attention",
            message = "Please re-authenticate your $institutionName account to continue syncing.",
            type = NotificationType.REAUTH_REQUIRED,
            actionData = mapOf("accountId" to accountId)
        )
    }
    
    override suspend fun notifyNewTransactions(userId: String, accountId: String, transactionCount: Int) {
        val message = if (transactionCount == 1) {
            "1 new transaction found"
        } else {
            "$transactionCount new transactions found"
        }
        
        sendNotification(
            userId = userId,
            title = "New Transactions",
            message = message,
            type = NotificationType.NEW_TRANSACTIONS,
            actionData = mapOf("accountId" to accountId)
        )
    }
    
    override suspend fun notifyConflictsResolved(userId: String, conflictCount: Int) {
        val message = if (conflictCount == 1) {
            "1 transaction conflict was automatically resolved"
        } else {
            "$conflictCount transaction conflicts were automatically resolved"
        }
        
        sendNotification(
            userId = userId,
            title = "Conflicts Resolved",
            message = message,
            type = NotificationType.CONFLICTS_RESOLVED
        )
    }
    
    override suspend fun notifySyncDelayed(userId: String, accountId: String) {
        sendNotification(
            userId = userId,
            title = "Sync Taking Longer",
            message = "Account sync is taking longer than usual. We're still working on it.",
            type = NotificationType.SYNC_DELAYED,
            actionData = mapOf("accountId" to accountId)
        )
    }
    
    override suspend fun cancelNotifications(userId: String) {
        // Implementation would cancel all pending notifications for the user
        // This would typically interact with the platform's notification system
    }
    
    private suspend fun sendNotification(
        userId: String,
        title: String,
        message: String,
        type: NotificationType,
        actionData: Map<String, String> = emptyMap()
    ) {
        // This would typically send a push notification or in-app notification
        // For now, we'll just log it - this should be implemented based on your notification system
        println("Notification for user $userId: $title - $message")
        
        // In a real implementation, this would:
        // 1. Check user notification preferences
        // 2. Send push notification via FCM/APNS
        // 3. Store in-app notification for when user opens app
        // 4. Track notification delivery and engagement
    }
}

enum class NotificationType {
    SYNC_SUCCESS,
    SYNC_ERROR,
    SYNC_PARTIAL,
    REAUTH_REQUIRED,
    NEW_TRANSACTIONS,
    CONFLICTS_RESOLVED,
    SYNC_DELAYED
}

data class SyncNotification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Instant,
    val isRead: Boolean = false,
    val actionData: Map<String, String> = emptyMap()
)