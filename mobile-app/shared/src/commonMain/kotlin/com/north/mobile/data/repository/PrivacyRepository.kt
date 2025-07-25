package com.north.mobile.data.repository

import com.north.mobile.data.privacy.*
import kotlinx.datetime.Instant

/**
 * Repository interface for privacy-related data operations
 */
interface PrivacyRepository {
    
    // Consent Management
    suspend fun insertConsentRecord(consent: ConsentRecord)
    suspend fun getLatestConsentsForUser(userId: String): List<ConsentRecord>
    suspend fun getLatestConsentForPurpose(userId: String, purpose: ConsentPurpose): ConsentRecord?
    suspend fun getConsentHistory(userId: String): List<ConsentRecord>
    suspend fun updateConsentPreferences(preferences: ConsentPreferences)
    suspend fun markConsentsAsDeleted(userId: String)
    suspend fun hasUserConsents(userId: String): Boolean
    
    // Data Export Management
    suspend fun insertDataExportRequest(request: DataExportRequest)
    suspend fun getDataExportRequest(exportId: String): DataExportRequest?
    suspend fun updateExportStatus(exportId: String, status: ExportStatus)
    suspend fun updateExportRequest(request: DataExportRequest)
    suspend fun getDataExportHistory(userId: String): List<DataExportRequest>
    suspend fun storeExportData(exportId: String, data: ByteArray)
    suspend fun getExportData(exportId: String): ByteArray
    
    // Data Deletion Management
    suspend fun insertDeletionRequest(request: DeletionRequest)
    suspend fun getDeletionRequest(requestId: String): DeletionRequest?
    suspend fun updateDeletionStatus(requestId: String, status: DeletionStatus)
    suspend fun updateDeletionRequest(request: DeletionRequest)
    suspend fun getDeletionHistory(userId: String): List<DeletionRequest>
    
    // User Data Operations for Export/Deletion
    suspend fun getUserProfile(userId: String): UserProfileExport
    suspend fun getUserAccounts(userId: String): List<AccountExport>
    suspend fun getUserTransactions(userId: String): List<TransactionExport>
    suspend fun getUserGoals(userId: String): List<GoalExport>
    suspend fun getUserGamification(userId: String): GamificationExport
    
    // Data Existence Checks
    suspend fun hasUserProfile(userId: String): Boolean
    suspend fun hasUserAccounts(userId: String): Boolean
    suspend fun hasUserTransactions(userId: String): Boolean
    suspend fun hasUserGoals(userId: String): Boolean
    suspend fun hasUserGamification(userId: String): Boolean
    suspend fun hasUserAnalytics(userId: String): Boolean
    suspend fun hasUserAuditLogs(userId: String): Boolean
    
    // Data Deletion Operations
    suspend fun deleteUserProfile(userId: String)
    suspend fun deleteUserAccounts(userId: String)
    suspend fun deleteUserTransactions(userId: String)
    suspend fun deleteUserGoals(userId: String)
    suspend fun deleteUserGamification(userId: String)
    suspend fun deleteUserAnalytics(userId: String)
    suspend fun deleteUserAuditLogs(userId: String, excludeCompliance: Boolean = false)
    
    // Audit Logging
    suspend fun insertAuditLogEntry(entry: AuditLogEntry)
    suspend fun getAuditLogs(
        userId: String,
        startDate: Instant? = null,
        endDate: Instant? = null,
        eventTypes: List<AuditEventType>? = null
    ): List<AuditLogEntry>
    suspend fun getAuditLogsByType(
        eventType: AuditEventType,
        startDate: Instant,
        endDate: Instant
    ): List<AuditLogEntry>
    suspend fun getAllAuditLogs(startDate: Instant, endDate: Instant): List<AuditLogEntry>
}