package com.north.mobile.data.privacy

import kotlinx.datetime.Instant

/**
 * Interface for managing complete data deletion (right to be forgotten) as required by PIPEDA
 */
interface DataDeletionManager {
    /**
     * Request complete account and data deletion
     */
    suspend fun requestAccountDeletion(userId: String, reason: DeletionReason? = null): DeletionResult
    
    /**
     * Request deletion of specific data types
     */
    suspend fun requestPartialDeletion(userId: String, dataTypes: List<DataType>): DeletionResult
    
    /**
     * Get status of deletion request
     */
    suspend fun getDeletionStatus(requestId: String): DeletionStatus
    
    /**
     * Cancel pending deletion request (within grace period)
     */
    suspend fun cancelDeletion(requestId: String): Boolean
    
    /**
     * Get deletion history for audit purposes
     */
    suspend fun getDeletionHistory(userId: String): List<DeletionRequest>
    
    /**
     * Verify complete data deletion (for compliance audits)
     */
    suspend fun verifyDeletion(userId: String): DeletionVerification
}

/**
 * Types of data that can be deleted
 */
enum class DataType {
    PROFILE,
    ACCOUNTS,
    TRANSACTIONS,
    GOALS,
    GAMIFICATION,
    ANALYTICS,
    AUDIT_LOGS,
    CONSENTS,
    ALL
}

/**
 * Reasons for account deletion
 */
enum class DeletionReason {
    USER_REQUEST,
    ACCOUNT_CLOSURE,
    PRIVACY_CONCERN,
    SERVICE_DISSATISFACTION,
    OTHER
}

/**
 * Data deletion request
 */
data class DeletionRequest(
    val id: String,
    val userId: String,
    val dataTypes: List<DataType>,
    val reason: DeletionReason?,
    val requestedAt: Instant,
    val status: DeletionStatus,
    val scheduledFor: Instant, // Actual deletion date (after grace period)
    val completedAt: Instant? = null,
    val gracePeriodEnds: Instant, // When user can no longer cancel
    val verificationRequired: Boolean = true
)

/**
 * Deletion status
 */
enum class DeletionStatus {
    PENDING, // Within grace period
    SCHEDULED, // Grace period ended, deletion scheduled
    IN_PROGRESS, // Deletion is being processed
    COMPLETED, // Deletion completed
    CANCELLED, // User cancelled within grace period
    FAILED // Deletion failed, manual intervention required
}

/**
 * Result of deletion operations
 */
sealed class DeletionResult {
    data class Success(val requestId: String, val gracePeriodEnds: Instant) : DeletionResult()
    data class Error(val message: String, val cause: Throwable? = null) : DeletionResult()
}

/**
 * Verification that data has been completely deleted
 */
data class DeletionVerification(
    val userId: String,
    val verifiedAt: Instant,
    val deletedDataTypes: List<DataType>,
    val remainingData: List<DataType>, // Any data that couldn't be deleted (legal requirements)
    val verificationHash: String, // Hash of verification for audit trail
    val complianceNotes: String? = null
)

/**
 * Grace period configuration for different data types
 */
data class GracePeriodConfig(
    val dataType: DataType,
    val gracePeriodDays: Int,
    val canCancel: Boolean = true
) {
    companion object {
        val DEFAULT_CONFIG = listOf(
            GracePeriodConfig(DataType.PROFILE, 30),
            GracePeriodConfig(DataType.ACCOUNTS, 30),
            GracePeriodConfig(DataType.TRANSACTIONS, 7), // Shorter for financial data
            GracePeriodConfig(DataType.GOALS, 14),
            GracePeriodConfig(DataType.GAMIFICATION, 7),
            GracePeriodConfig(DataType.ANALYTICS, 1),
            GracePeriodConfig(DataType.AUDIT_LOGS, 0, canCancel = false), // Legal requirement
            GracePeriodConfig(DataType.CONSENTS, 0, canCancel = false), // Legal requirement
            GracePeriodConfig(DataType.ALL, 30)
        )
    }
}