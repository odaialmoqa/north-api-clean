package com.north.mobile.data.privacy

import kotlinx.datetime.Instant

/**
 * Interface for managing user data export functionality as required by PIPEDA
 */
interface DataExportManager {
    /**
     * Request complete data export for a user
     */
    suspend fun requestDataExport(userId: String, format: ExportFormat = ExportFormat.JSON): DataExportResult
    
    /**
     * Get status of data export request
     */
    suspend fun getExportStatus(exportId: String): ExportStatus
    
    /**
     * Download completed data export
     */
    suspend fun downloadExport(exportId: String): ByteArray
    
    /**
     * Cancel pending data export request
     */
    suspend fun cancelExport(exportId: String): Boolean
    
    /**
     * Get list of user's export requests
     */
    suspend fun getExportHistory(userId: String): List<DataExportRequest>
}

/**
 * Supported export formats
 */
enum class ExportFormat {
    JSON,
    CSV,
    PDF
}

/**
 * Data export request
 */
data class DataExportRequest(
    val id: String,
    val userId: String,
    val format: ExportFormat,
    val requestedAt: Instant,
    val status: ExportStatus,
    val completedAt: Instant? = null,
    val downloadUrl: String? = null,
    val expiresAt: Instant? = null, // Download link expiry
    val fileSize: Long? = null
)

/**
 * Export status
 */
enum class ExportStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    EXPIRED,
    CANCELLED
}

/**
 * Result of data export operations
 */
sealed class DataExportResult {
    data class Success(val exportId: String) : DataExportResult()
    data class Error(val message: String, val cause: Throwable? = null) : DataExportResult()
}

/**
 * Complete user data export structure
 */
data class UserDataExport(
    val userId: String,
    val exportedAt: Instant,
    val privacyPolicyVersion: String,
    val profile: UserProfileExport,
    val accounts: List<AccountExport>,
    val transactions: List<TransactionExport>,
    val goals: List<GoalExport>,
    val gamification: GamificationExport,
    val consents: List<ConsentRecord>,
    val auditLog: List<AuditLogEntry>
)

/**
 * User profile export data
 */
data class UserProfileExport(
    val id: String,
    val email: String,
    val createdAt: Instant,
    val lastLoginAt: Instant?,
    val preferences: Map<String, Any>
)

/**
 * Account export data (anonymized sensitive info)
 */
data class AccountExport(
    val id: String,
    val institutionName: String,
    val accountType: String,
    val currency: String,
    val createdAt: Instant,
    val lastSyncAt: Instant?
)

/**
 * Transaction export data
 */
data class TransactionExport(
    val id: String,
    val accountId: String,
    val amount: String,
    val description: String,
    val category: String,
    val date: String,
    val createdAt: Instant
)

/**
 * Goal export data
 */
data class GoalExport(
    val id: String,
    val title: String,
    val targetAmount: String,
    val currentAmount: String,
    val targetDate: String,
    val createdAt: Instant,
    val status: String
)

/**
 * Gamification export data
 */
data class GamificationExport(
    val level: Int,
    val totalPoints: Int,
    val achievements: List<String>,
    val streaks: Map<String, Int>,
    val lastActivity: Instant
)