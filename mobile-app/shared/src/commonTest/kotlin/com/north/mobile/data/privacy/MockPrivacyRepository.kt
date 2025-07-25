package com.north.mobile.data.privacy

import com.north.mobile.data.repository.PrivacyRepository
import kotlinx.datetime.Instant

class MockPrivacyRepository : PrivacyRepository {
    
    val consentRecords = mutableListOf<ConsentRecord>()
    val consentPreferences = mutableListOf<ConsentPreferences>()
    val dataExportRequests = mutableListOf<DataExportRequest>()
    val exportData = mutableMapOf<String, ByteArray>()
    val deletionRequests = mutableListOf<DeletionRequest>()
    val auditLogEntries = mutableListOf<AuditLogEntry>()
    
    // User data for export/deletion
    val userProfiles = mutableMapOf<String, UserProfileExport>()
    val userAccounts = mutableMapOf<String, List<AccountExport>>()
    val userTransactions = mutableMapOf<String, List<TransactionExport>>()
    val userGoals = mutableMapOf<String, List<GoalExport>>()
    val userGamification = mutableMapOf<String, GamificationExport>()
    
    // Data existence flags
    val dataExistence = mutableMapOf<String, MutableMap<String, Boolean>>()
    
    override suspend fun insertConsentRecord(consent: ConsentRecord) {
        consentRecords.add(consent)
    }
    
    override suspend fun getLatestConsentsForUser(userId: String): List<ConsentRecord> {
        return consentRecords
            .filter { it.userId == userId }
            .groupBy { it.purpose }
            .mapValues { it.value.maxByOrNull { record -> record.timestamp } }
            .values
            .filterNotNull()
    }
    
    override suspend fun getLatestConsentForPurpose(userId: String, purpose: ConsentPurpose): ConsentRecord? {
        return consentRecords
            .filter { it.userId == userId && it.purpose == purpose }
            .maxByOrNull { it.timestamp }
    }
    
    override suspend fun getConsentHistory(userId: String): List<ConsentRecord> {
        return consentRecords.filter { it.userId == userId }.sortedByDescending { it.timestamp }
    }
    
    override suspend fun updateConsentPreferences(preferences: ConsentPreferences) {
        consentPreferences.removeAll { it.userId == preferences.userId }
        consentPreferences.add(preferences)
    }
    
    override suspend fun markConsentsAsDeleted(userId: String) {
        consentRecords.replaceAll { consent ->
            if (consent.userId == userId) {
                consent.copy(granted = false, version = consent.version + "_DELETED")
            } else {
                consent
            }
        }
    }
    
    override suspend fun hasUserConsents(userId: String): Boolean {
        return consentRecords.any { it.userId == userId }
    }
    
    override suspend fun insertDataExportRequest(request: DataExportRequest) {
        dataExportRequests.add(request)
    }
    
    override suspend fun getDataExportRequest(exportId: String): DataExportRequest? {
        return dataExportRequests.find { it.id == exportId }
    }
    
    override suspend fun updateExportStatus(exportId: String, status: ExportStatus) {
        val index = dataExportRequests.indexOfFirst { it.id == exportId }
        if (index >= 0) {
            dataExportRequests[index] = dataExportRequests[index].copy(status = status)
        }
    }
    
    override suspend fun updateExportRequest(request: DataExportRequest) {
        val index = dataExportRequests.indexOfFirst { it.id == request.id }
        if (index >= 0) {
            dataExportRequests[index] = request
        }
    }
    
    override suspend fun getDataExportHistory(userId: String): List<DataExportRequest> {
        return dataExportRequests.filter { it.userId == userId }.sortedByDescending { it.requestedAt }
    }
    
    override suspend fun storeExportData(exportId: String, data: ByteArray) {
        exportData[exportId] = data
    }
    
    override suspend fun getExportData(exportId: String): ByteArray {
        return exportData[exportId] ?: throw IllegalArgumentException("Export data not found")
    }
    
    override suspend fun insertDeletionRequest(request: DeletionRequest) {
        deletionRequests.add(request)
    }
    
    override suspend fun getDeletionRequest(requestId: String): DeletionRequest? {
        return deletionRequests.find { it.id == requestId }
    }
    
    override suspend fun updateDeletionStatus(requestId: String, status: DeletionStatus) {
        val index = deletionRequests.indexOfFirst { it.id == requestId }
        if (index >= 0) {
            deletionRequests[index] = deletionRequests[index].copy(status = status)
        }
    }
    
    override suspend fun updateDeletionRequest(request: DeletionRequest) {
        val index = deletionRequests.indexOfFirst { it.id == request.id }
        if (index >= 0) {
            deletionRequests[index] = request
        }
    }
    
    override suspend fun getDeletionHistory(userId: String): List<DeletionRequest> {
        return deletionRequests.filter { it.userId == userId }.sortedByDescending { it.requestedAt }
    }
    
    override suspend fun getUserProfile(userId: String): UserProfileExport {
        return userProfiles[userId] ?: UserProfileExport(
            id = userId,
            email = "test@example.com",
            createdAt = kotlinx.datetime.Clock.System.now(),
            lastLoginAt = kotlinx.datetime.Clock.System.now(),
            preferences = emptyMap()
        )
    }
    
    override suspend fun getUserAccounts(userId: String): List<AccountExport> {
        return userAccounts[userId] ?: emptyList()
    }
    
    override suspend fun getUserTransactions(userId: String): List<TransactionExport> {
        return userTransactions[userId] ?: emptyList()
    }
    
    override suspend fun getUserGoals(userId: String): List<GoalExport> {
        return userGoals[userId] ?: emptyList()
    }
    
    override suspend fun getUserGamification(userId: String): GamificationExport {
        return userGamification[userId] ?: GamificationExport(
            level = 1,
            totalPoints = 0,
            achievements = emptyList(),
            streaks = emptyMap(),
            lastActivity = kotlinx.datetime.Clock.System.now()
        )
    }
    
    override suspend fun hasUserProfile(userId: String): Boolean {
        return dataExistence[userId]?.get("profile") ?: false
    }
    
    override suspend fun hasUserAccounts(userId: String): Boolean {
        return dataExistence[userId]?.get("accounts") ?: false
    }
    
    override suspend fun hasUserTransactions(userId: String): Boolean {
        return dataExistence[userId]?.get("transactions") ?: false
    }
    
    override suspend fun hasUserGoals(userId: String): Boolean {
        return dataExistence[userId]?.get("goals") ?: false
    }
    
    override suspend fun hasUserGamification(userId: String): Boolean {
        return dataExistence[userId]?.get("gamification") ?: false
    }
    
    override suspend fun hasUserAnalytics(userId: String): Boolean {
        return dataExistence[userId]?.get("analytics") ?: false
    }
    
    override suspend fun hasUserAuditLogs(userId: String): Boolean {
        return auditLogEntries.any { it.userId == userId }
    }
    
    override suspend fun deleteUserProfile(userId: String) {
        userProfiles.remove(userId)
        setDataExistence(userId, "profile", false)
    }
    
    override suspend fun deleteUserAccounts(userId: String) {
        userAccounts.remove(userId)
        setDataExistence(userId, "accounts", false)
    }
    
    override suspend fun deleteUserTransactions(userId: String) {
        userTransactions.remove(userId)
        setDataExistence(userId, "transactions", false)
    }
    
    override suspend fun deleteUserGoals(userId: String) {
        userGoals.remove(userId)
        setDataExistence(userId, "goals", false)
    }
    
    override suspend fun deleteUserGamification(userId: String) {
        userGamification.remove(userId)
        setDataExistence(userId, "gamification", false)
    }
    
    override suspend fun deleteUserAnalytics(userId: String) {
        auditLogEntries.removeAll { it.userId == userId && it.eventType.name.contains("ANALYTICS") }
        setDataExistence(userId, "analytics", false)
    }
    
    override suspend fun deleteUserAuditLogs(userId: String, excludeCompliance: Boolean) {
        if (excludeCompliance) {
            auditLogEntries.removeAll { 
                it.userId == userId && it.eventType !in listOf(
                    AuditEventType.CONSENT_GRANTED,
                    AuditEventType.CONSENT_WITHDRAWN,
                    AuditEventType.DATA_DELETION_REQUESTED,
                    AuditEventType.DATA_DELETION_COMPLETED
                )
            }
        } else {
            auditLogEntries.removeAll { it.userId == userId }
        }
    }
    
    override suspend fun insertAuditLogEntry(entry: AuditLogEntry) {
        auditLogEntries.add(entry)
    }
    
    override suspend fun getAuditLogs(
        userId: String,
        startDate: Instant?,
        endDate: Instant?,
        eventTypes: List<AuditEventType>?
    ): List<AuditLogEntry> {
        return auditLogEntries
            .filter { it.userId == userId }
            .filter { startDate == null || it.timestamp >= startDate }
            .filter { endDate == null || it.timestamp <= endDate }
            .filter { eventTypes == null || it.eventType in eventTypes }
            .sortedByDescending { it.timestamp }
    }
    
    override suspend fun getAuditLogsByType(
        eventType: AuditEventType,
        startDate: Instant,
        endDate: Instant
    ): List<AuditLogEntry> {
        return auditLogEntries
            .filter { it.eventType == eventType }
            .filter { it.timestamp >= startDate && it.timestamp <= endDate }
            .sortedByDescending { it.timestamp }
    }
    
    override suspend fun getAllAuditLogs(startDate: Instant, endDate: Instant): List<AuditLogEntry> {
        return auditLogEntries
            .filter { it.timestamp >= startDate && it.timestamp <= endDate }
            .sortedByDescending { it.timestamp }
    }
    
    private fun setDataExistence(userId: String, dataType: String, exists: Boolean) {
        dataExistence.getOrPut(userId) { mutableMapOf() }[dataType] = exists
    }
    
    // Helper methods for testing
    fun setUserDataExists(userId: String, dataType: String, exists: Boolean) {
        setDataExistence(userId, dataType, exists)
    }
}