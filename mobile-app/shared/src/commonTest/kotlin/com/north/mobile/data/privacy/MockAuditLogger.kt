package com.north.mobile.data.privacy

import kotlinx.datetime.Instant

class MockAuditLogger : AuditLogger {
    
    val dataAccessEvents = mutableListOf<DataAccessEvent>()
    val dataModificationEvents = mutableListOf<DataModificationEvent>()
    val privacyEvents = mutableListOf<PrivacyEvent>()
    val securityEvents = mutableListOf<SecurityEvent>()
    val auditLogEntries = mutableListOf<AuditLogEntry>()
    
    override suspend fun logDataAccess(event: DataAccessEvent): AuditResult {
        dataAccessEvents.add(event)
        return AuditResult.Success
    }
    
    override suspend fun logDataModification(event: DataModificationEvent): AuditResult {
        dataModificationEvents.add(event)
        return AuditResult.Success
    }
    
    override suspend fun logPrivacyEvent(event: PrivacyEvent): AuditResult {
        privacyEvents.add(event)
        return AuditResult.Success
    }
    
    override suspend fun logSecurityEvent(event: SecurityEvent): AuditResult {
        securityEvents.add(event)
        return AuditResult.Success
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
    
    override suspend fun generateAuditReport(
        startDate: Instant,
        endDate: Instant,
        userId: String?
    ): AuditReport {
        val logs = if (userId != null) {
            getAuditLogs(userId, startDate, endDate, null)
        } else {
            auditLogEntries.filter { it.timestamp >= startDate && it.timestamp <= endDate }
        }
        
        return AuditReport(
            generatedAt = kotlinx.datetime.Clock.System.now(),
            startDate = startDate,
            endDate = endDate,
            userId = userId,
            totalEvents = logs.size,
            eventsByType = logs.groupBy { it.eventType }.mapValues { it.value.size },
            riskEvents = logs.filter { it.riskLevel in listOf(RiskLevel.HIGH, RiskLevel.CRITICAL) },
            complianceMetrics = ComplianceMetrics(
                dataAccessEvents = logs.count { it.eventType == AuditEventType.DATA_ACCESS },
                consentChanges = logs.count { it.eventType in listOf(AuditEventType.CONSENT_GRANTED, AuditEventType.CONSENT_WITHDRAWN) },
                dataExports = logs.count { it.eventType in listOf(AuditEventType.DATA_EXPORT_REQUESTED, AuditEventType.DATA_EXPORT_DOWNLOADED) },
                dataDeletions = logs.count { it.eventType in listOf(AuditEventType.DATA_DELETION_REQUESTED, AuditEventType.DATA_DELETION_COMPLETED) },
                securityIncidents = logs.count { it.eventType == AuditEventType.SECURITY_INCIDENT },
                averageResponseTime = 500L,
                complianceScore = 0.95f
            ),
            recommendations = listOf("Continue monitoring compliance metrics")
        )
    }
}