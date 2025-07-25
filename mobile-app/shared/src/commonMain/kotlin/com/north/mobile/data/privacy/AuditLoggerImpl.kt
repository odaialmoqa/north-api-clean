package com.north.mobile.data.privacy

import com.north.mobile.data.repository.PrivacyRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Implementation of AuditLogger for PIPEDA compliance audit trail
 */
class AuditLoggerImpl(
    private val repository: PrivacyRepository
) : AuditLogger {
    
    override suspend fun logDataAccess(event: DataAccessEvent): AuditResult {
        return try {
            val auditEntry = AuditLogEntry(
                id = generateAuditId(),
                userId = event.userId,
                eventType = AuditEventType.DATA_ACCESS,
                timestamp = Clock.System.now(),
                ipAddress = event.ipAddress,
                userAgent = event.userAgent,
                sessionId = event.sessionId,
                details = mapOf(
                    "data_type" to event.dataType,
                    "resource_id" to (event.resourceId ?: ""),
                    "access_method" to event.accessMethod.name,
                    "purpose" to (event.purpose ?: "")
                ),
                result = AuditEventResult.SUCCESS,
                riskLevel = determineRiskLevel(event)
            )
            
            repository.insertAuditLogEntry(auditEntry)
            AuditResult.Success
        } catch (e: Exception) {
            AuditResult.Error("Failed to log data access event", e)
        }
    }
    
    override suspend fun logDataModification(event: DataModificationEvent): AuditResult {
        return try {
            val auditEntry = AuditLogEntry(
                id = generateAuditId(),
                userId = event.userId,
                eventType = AuditEventType.DATA_MODIFICATION,
                timestamp = Clock.System.now(),
                ipAddress = event.ipAddress,
                userAgent = event.userAgent,
                sessionId = event.sessionId,
                details = mapOf(
                    "data_type" to event.dataType,
                    "resource_id" to (event.resourceId ?: ""),
                    "operation" to event.operation.name,
                    "old_value" to (event.oldValue ?: ""),
                    "new_value" to (event.newValue ?: "")
                ),
                result = AuditEventResult.SUCCESS,
                riskLevel = determineRiskLevel(event)
            )
            
            repository.insertAuditLogEntry(auditEntry)
            AuditResult.Success
        } catch (e: Exception) {
            AuditResult.Error("Failed to log data modification event", e)
        }
    }
    
    override suspend fun logPrivacyEvent(event: PrivacyEvent): AuditResult {
        return try {
            val auditEntry = AuditLogEntry(
                id = generateAuditId(),
                userId = event.userId,
                eventType = event.eventType,
                timestamp = Clock.System.now(),
                ipAddress = event.ipAddress,
                userAgent = event.userAgent,
                sessionId = event.sessionId,
                details = event.details,
                result = AuditEventResult.SUCCESS,
                riskLevel = determinePrivacyEventRiskLevel(event.eventType)
            )
            
            repository.insertAuditLogEntry(auditEntry)
            AuditResult.Success
        } catch (e: Exception) {
            AuditResult.Error("Failed to log privacy event", e)
        }
    }
    
    override suspend fun logSecurityEvent(event: SecurityEvent): AuditResult {
        return try {
            val auditEntry = AuditLogEntry(
                id = generateAuditId(),
                userId = event.userId,
                eventType = event.eventType,
                timestamp = Clock.System.now(),
                ipAddress = event.ipAddress,
                userAgent = event.userAgent,
                sessionId = event.sessionId,
                details = event.details + mapOf("severity" to event.severity.name),
                result = AuditEventResult.SUCCESS,
                riskLevel = mapSecuritySeverityToRiskLevel(event.severity)
            )
            
            repository.insertAuditLogEntry(auditEntry)
            AuditResult.Success
        } catch (e: Exception) {
            AuditResult.Error("Failed to log security event", e)
        }
    }
    
    override suspend fun getAuditLogs(
        userId: String,
        startDate: Instant?,
        endDate: Instant?,
        eventTypes: List<AuditEventType>?
    ): List<AuditLogEntry> {
        return repository.getAuditLogs(userId, startDate, endDate, eventTypes)
    }
    
    override suspend fun getAuditLogsByType(
        eventType: AuditEventType,
        startDate: Instant,
        endDate: Instant
    ): List<AuditLogEntry> {
        return repository.getAuditLogsByType(eventType, startDate, endDate)
    }
    
    override suspend fun generateAuditReport(
        startDate: Instant,
        endDate: Instant,
        userId: String?
    ): AuditReport {
        val auditLogs = if (userId != null) {
            repository.getAuditLogs(userId, startDate, endDate, null)
        } else {
            repository.getAllAuditLogs(startDate, endDate)
        }
        
        val eventsByType = auditLogs.groupBy { it.eventType }.mapValues { it.value.size }
        val riskEvents = auditLogs.filter { it.riskLevel in listOf(RiskLevel.HIGH, RiskLevel.CRITICAL) }
        
        val complianceMetrics = calculateComplianceMetrics(auditLogs)
        val recommendations = generateRecommendations(auditLogs, complianceMetrics)
        
        return AuditReport(
            generatedAt = Clock.System.now(),
            startDate = startDate,
            endDate = endDate,
            userId = userId,
            totalEvents = auditLogs.size,
            eventsByType = eventsByType,
            riskEvents = riskEvents,
            complianceMetrics = complianceMetrics,
            recommendations = recommendations
        )
    }
    
    private fun determineRiskLevel(event: DataAccessEvent): RiskLevel {
        return when {
            event.accessMethod == AccessMethod.ADMIN_PANEL -> RiskLevel.MEDIUM
            event.dataType.contains("financial", ignoreCase = true) -> RiskLevel.MEDIUM
            event.dataType.contains("personal", ignoreCase = true) -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }
    
    private fun determineRiskLevel(event: DataModificationEvent): RiskLevel {
        return when {
            event.operation == ModificationOperation.DELETE -> RiskLevel.HIGH
            event.dataType.contains("financial", ignoreCase = true) -> RiskLevel.MEDIUM
            event.dataType.contains("personal", ignoreCase = true) -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }
    
    private fun determinePrivacyEventRiskLevel(eventType: AuditEventType): RiskLevel {
        return when (eventType) {
            AuditEventType.DATA_DELETION_REQUESTED,
            AuditEventType.DATA_DELETION_COMPLETED -> RiskLevel.HIGH
            AuditEventType.CONSENT_WITHDRAWN,
            AuditEventType.DATA_EXPORT_REQUESTED -> RiskLevel.MEDIUM
            AuditEventType.CONSENT_GRANTED,
            AuditEventType.PRIVACY_POLICY_ACCEPTED -> RiskLevel.LOW
            else -> RiskLevel.LOW
        }
    }
    
    private fun mapSecuritySeverityToRiskLevel(severity: SecuritySeverity): RiskLevel {
        return when (severity) {
            SecuritySeverity.INFO -> RiskLevel.LOW
            SecuritySeverity.WARNING -> RiskLevel.MEDIUM
            SecuritySeverity.ERROR -> RiskLevel.HIGH
            SecuritySeverity.CRITICAL -> RiskLevel.CRITICAL
        }
    }
    
    private fun calculateComplianceMetrics(auditLogs: List<AuditLogEntry>): ComplianceMetrics {
        val dataAccessEvents = auditLogs.count { it.eventType == AuditEventType.DATA_ACCESS }
        val consentChanges = auditLogs.count { 
            it.eventType in listOf(AuditEventType.CONSENT_GRANTED, AuditEventType.CONSENT_WITHDRAWN) 
        }
        val dataExports = auditLogs.count { 
            it.eventType in listOf(AuditEventType.DATA_EXPORT_REQUESTED, AuditEventType.DATA_EXPORT_DOWNLOADED) 
        }
        val dataDeletions = auditLogs.count { 
            it.eventType in listOf(AuditEventType.DATA_DELETION_REQUESTED, AuditEventType.DATA_DELETION_COMPLETED) 
        }
        val securityIncidents = auditLogs.count { it.eventType == AuditEventType.SECURITY_INCIDENT }
        
        // Calculate average response time (simplified)
        val averageResponseTime = 500L // milliseconds - would be calculated from actual response times
        
        // Calculate compliance score based on various factors
        val complianceScore = calculateComplianceScore(auditLogs)
        
        return ComplianceMetrics(
            dataAccessEvents = dataAccessEvents,
            consentChanges = consentChanges,
            dataExports = dataExports,
            dataDeletions = dataDeletions,
            securityIncidents = securityIncidents,
            averageResponseTime = averageResponseTime,
            complianceScore = complianceScore
        )
    }
    
    private fun calculateComplianceScore(auditLogs: List<AuditLogEntry>): Float {
        if (auditLogs.isEmpty()) return 1.0f
        
        val totalEvents = auditLogs.size
        val successfulEvents = auditLogs.count { it.result == AuditEventResult.SUCCESS }
        val highRiskEvents = auditLogs.count { it.riskLevel in listOf(RiskLevel.HIGH, RiskLevel.CRITICAL) }
        
        val successRate = successfulEvents.toFloat() / totalEvents
        val riskPenalty = (highRiskEvents.toFloat() / totalEvents) * 0.2f
        
        return (successRate - riskPenalty).coerceIn(0.0f, 1.0f)
    }
    
    private fun generateRecommendations(auditLogs: List<AuditLogEntry>, metrics: ComplianceMetrics): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (metrics.complianceScore < 0.8f) {
            recommendations.add("Review and improve data handling processes to increase compliance score")
        }
        
        if (metrics.securityIncidents > 0) {
            recommendations.add("Investigate and address security incidents to prevent future occurrences")
        }
        
        val highRiskEvents = auditLogs.count { it.riskLevel == RiskLevel.HIGH }
        if (highRiskEvents > auditLogs.size * 0.1) {
            recommendations.add("High number of high-risk events detected. Review access controls and procedures")
        }
        
        if (metrics.dataExports > metrics.dataDeletions * 2) {
            recommendations.add("Consider implementing data retention policies to reduce data export requests")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Compliance metrics are within acceptable ranges. Continue monitoring.")
        }
        
        return recommendations
    }
    
    private fun generateAuditId(): String {
        return "audit_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
}