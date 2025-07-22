package com.north.mobile.data.privacy

import com.north.mobile.data.repository.PrivacyRepository
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.days

/**
 * Implementation of DataDeletionManager for PIPEDA right to be forgotten compliance
 */
class DataDeletionManagerImpl(
    private val repository: PrivacyRepository,
    private val auditLogger: AuditLogger
) : DataDeletionManager {
    
    private val gracePeriodConfig = GracePeriodConfig.DEFAULT_CONFIG.associateBy { it.dataType }
    
    override suspend fun requestAccountDeletion(userId: String, reason: DeletionReason?): DeletionResult {
        return requestPartialDeletion(userId, listOf(DataType.ALL))
    }
    
    override suspend fun requestPartialDeletion(userId: String, dataTypes: List<DataType>): DeletionResult {
        return try {
            val requestId = generateDeletionId()
            val now = Clock.System.now()
            
            // Calculate grace period (use longest grace period for multiple data types)
            val maxGracePeriod = dataTypes.mapNotNull { gracePeriodConfig[it]?.gracePeriodDays }.maxOrNull() ?: 30
            val gracePeriodEnds = now + maxGracePeriod.days
            val scheduledFor = gracePeriodEnds + 1.days
            
            val deletionRequest = DeletionRequest(
                id = requestId,
                userId = userId,
                dataTypes = dataTypes,
                reason = null,
                requestedAt = now,
                status = DeletionStatus.PENDING,
                scheduledFor = scheduledFor,
                gracePeriodEnds = gracePeriodEnds,
                verificationRequired = dataTypes.contains(DataType.ALL) || dataTypes.size > 1
            )
            
            repository.insertDeletionRequest(deletionRequest)
            
            // Log the deletion request
            auditLogger.logPrivacyEvent(
                PrivacyEvent(
                    userId = userId,
                    eventType = AuditEventType.DATA_DELETION_REQUESTED,
                    details = mapOf(
                        "request_id" to requestId,
                        "data_types" to dataTypes.map { it.name },
                        "grace_period_ends" to gracePeriodEnds.toString(),
                        "scheduled_for" to scheduledFor.toString()
                    ),
                    ipAddress = null,
                    userAgent = null,
                    sessionId = null
                )
            )
            
            // Schedule the actual deletion
            scheduleDeletion(deletionRequest)
            
            DeletionResult.Success(requestId, gracePeriodEnds)
        } catch (e: Exception) {
            DeletionResult.Error("Failed to request data deletion", e)
        }
    }
    
    override suspend fun getDeletionStatus(requestId: String): DeletionStatus {
        return repository.getDeletionRequest(requestId)?.status ?: DeletionStatus.FAILED
    }
    
    override suspend fun cancelDeletion(requestId: String): Boolean {
        return try {
            val deletionRequest = repository.getDeletionRequest(requestId)
                ?: return false
            
            val now = Clock.System.now()
            
            // Check if still within grace period
            if (now < deletionRequest.gracePeriodEnds && deletionRequest.status == DeletionStatus.PENDING) {
                repository.updateDeletionStatus(requestId, DeletionStatus.CANCELLED)
                
                auditLogger.logPrivacyEvent(
                    PrivacyEvent(
                        userId = deletionRequest.userId,
                        eventType = AuditEventType.DATA_DELETION_REQUESTED,
                        details = mapOf(
                            "request_id" to requestId,
                            "action" to "cancelled",
                            "cancelled_at" to now.toString()
                        ),
                        ipAddress = null,
                        userAgent = null,
                        sessionId = null
                    )
                )
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getDeletionHistory(userId: String): List<DeletionRequest> {
        return repository.getDeletionHistory(userId)
    }
    
    override suspend fun verifyDeletion(userId: String): DeletionVerification {
        val now = Clock.System.now()
        val deletedDataTypes = mutableListOf<DataType>()
        val remainingData = mutableListOf<DataType>()
        
        // Check each data type
        DataType.values().forEach { dataType ->
            if (dataType == DataType.ALL) return@forEach
            
            val hasData = when (dataType) {
                DataType.PROFILE -> repository.hasUserProfile(userId)
                DataType.ACCOUNTS -> repository.hasUserAccounts(userId)
                DataType.TRANSACTIONS -> repository.hasUserTransactions(userId)
                DataType.GOALS -> repository.hasUserGoals(userId)
                DataType.GAMIFICATION -> repository.hasUserGamification(userId)
                DataType.ANALYTICS -> repository.hasUserAnalytics(userId)
                DataType.AUDIT_LOGS -> repository.hasUserAuditLogs(userId)
                DataType.CONSENTS -> repository.hasUserConsents(userId)
                DataType.ALL -> false
            }
            
            if (hasData) {
                remainingData.add(dataType)
            } else {
                deletedDataTypes.add(dataType)
            }
        }
        
        val verificationHash = generateVerificationHash(userId, deletedDataTypes, remainingData, now)
        
        val complianceNotes = if (remainingData.contains(DataType.AUDIT_LOGS) || remainingData.contains(DataType.CONSENTS)) {
            "Some data retained for legal compliance requirements (PIPEDA audit trail)"
        } else null
        
        return DeletionVerification(
            userId = userId,
            verifiedAt = now,
            deletedDataTypes = deletedDataTypes,
            remainingData = remainingData,
            verificationHash = verificationHash,
            complianceNotes = complianceNotes
        )
    }
    
    private suspend fun scheduleDeletion(deletionRequest: DeletionRequest) {
        // In a real implementation, this would schedule a background job
        // For now, we'll simulate the scheduling
        
        val now = Clock.System.now()
        if (now >= deletionRequest.gracePeriodEnds) {
            processDeletion(deletionRequest)
        } else {
            // Schedule for later processing
            repository.updateDeletionStatus(deletionRequest.id, DeletionStatus.SCHEDULED)
        }
    }
    
    private suspend fun processDeletion(deletionRequest: DeletionRequest) {
        try {
            repository.updateDeletionStatus(deletionRequest.id, DeletionStatus.IN_PROGRESS)
            
            val userId = deletionRequest.userId
            val dataTypes = deletionRequest.dataTypes
            
            // Delete data based on requested types
            dataTypes.forEach { dataType ->
                when (dataType) {
                    DataType.ALL -> deleteAllUserData(userId)
                    DataType.PROFILE -> repository.deleteUserProfile(userId)
                    DataType.ACCOUNTS -> repository.deleteUserAccounts(userId)
                    DataType.TRANSACTIONS -> repository.deleteUserTransactions(userId)
                    DataType.GOALS -> repository.deleteUserGoals(userId)
                    DataType.GAMIFICATION -> repository.deleteUserGamification(userId)
                    DataType.ANALYTICS -> repository.deleteUserAnalytics(userId)
                    DataType.AUDIT_LOGS -> {
                        // Only delete non-compliance related audit logs
                        repository.deleteUserAuditLogs(userId, excludeCompliance = true)
                    }
                    DataType.CONSENTS -> {
                        // Mark consents as deleted but keep for compliance
                        repository.markConsentsAsDeleted(userId)
                    }
                }
            }
            
            // Update deletion request
            repository.updateDeletionRequest(
                deletionRequest.copy(
                    status = DeletionStatus.COMPLETED,
                    completedAt = Clock.System.now()
                )
            )
            
            // Log completion
            auditLogger.logPrivacyEvent(
                PrivacyEvent(
                    userId = userId,
                    eventType = AuditEventType.DATA_DELETION_COMPLETED,
                    details = mapOf(
                        "request_id" to deletionRequest.id,
                        "data_types" to dataTypes.map { it.name },
                        "completed_at" to Clock.System.now().toString()
                    ),
                    ipAddress = null,
                    userAgent = null,
                    sessionId = null
                )
            )
            
        } catch (e: Exception) {
            repository.updateDeletionStatus(deletionRequest.id, DeletionStatus.FAILED)
            
            auditLogger.logPrivacyEvent(
                PrivacyEvent(
                    userId = deletionRequest.userId,
                    eventType = AuditEventType.DATA_DELETION_COMPLETED,
                    details = mapOf(
                        "request_id" to deletionRequest.id,
                        "status" to "failed",
                        "error" to e.message.orEmpty()
                    ),
                    ipAddress = null,
                    userAgent = null,
                    sessionId = null
                )
            )
        }
    }
    
    private suspend fun deleteAllUserData(userId: String) {
        repository.deleteUserProfile(userId)
        repository.deleteUserAccounts(userId)
        repository.deleteUserTransactions(userId)
        repository.deleteUserGoals(userId)
        repository.deleteUserGamification(userId)
        repository.deleteUserAnalytics(userId)
        repository.deleteUserAuditLogs(userId, excludeCompliance = true)
        repository.markConsentsAsDeleted(userId)
    }
    
    private fun generateVerificationHash(
        userId: String,
        deletedDataTypes: List<DataType>,
        remainingData: List<DataType>,
        timestamp: kotlinx.datetime.Instant
    ): String {
        val content = "$userId:${deletedDataTypes.sorted()}:${remainingData.sorted()}:$timestamp"
        return content.hashCode().toString()
    }
    
    private fun generateDeletionId(): String {
        return "deletion_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
}