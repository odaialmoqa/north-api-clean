package com.north.mobile.data.privacy

import kotlinx.datetime.Instant

/**
 * Interface for audit logging to track data access and modifications for PIPEDA compliance
 */
interface AuditLogger {
    /**
     * Log data access event
     */
    suspend fun logDataAccess(event: DataAccessEvent): AuditResult
    
    /**
     * Log data modification event
     */
    suspend fun logDataModification(event: DataModificationEvent): AuditResult
    
    /**
     * Log privacy-related event (consent changes, exports, deletions)
     */
    suspend fun logPrivacyEvent(event: PrivacyEvent): AuditResult
    
    /**
     * Log security event
     */
    suspend fun logSecurityEvent(event: SecurityEvent): AuditResult
    
    /**
     * Query audit logs for a specific user
     */
    suspend fun getAuditLogs(
        userId: String,
        startDate: Instant? = null,
        endDate: Instant? = null,
        eventTypes: List<AuditEventType>? = null
    ): List<AuditLogEntry>
    
    /**
     * Query audit logs by event type
     */
    suspend fun getAuditLogsByType(
        eventType: AuditEventType,
        startDate: Instant,
        endDate: Instant
    ): List<AuditLogEntry>
    
    /**
     * Generate audit report for compliance
     */
    suspend fun generateAuditReport(
        startDate: Instant,
        endDate: Instant,
        userId: String? = null
    ): AuditReport
}

/**
 * Types of audit events
 */
enum class AuditEventType {
    DATA_ACCESS,
    DATA_MODIFICATION,
    CONSENT_GRANTED,
    CONSENT_WITHDRAWN,
    DATA_EXPORT_REQUESTED,
    DATA_EXPORT_DOWNLOADED,
    DATA_DELETION_REQUESTED,
    DATA_DELETION_COMPLETED,
    AUTHENTICATION_SUCCESS,
    AUTHENTICATION_FAILURE,
    ACCOUNT_LINKED,
    ACCOUNT_UNLINKED,
    PRIVACY_POLICY_ACCEPTED,
    SECURITY_INCIDENT
}

/**
 * Base audit log entry
 */
data class AuditLogEntry(
    val id: String,
    val userId: String?,
    val eventType: AuditEventType,
    val timestamp: Instant,
    val ipAddress: String?,
    val userAgent: String?,
    val sessionId: String?,
    val details: Map<String, Any>,
    val result: AuditEventResult,
    val riskLevel: RiskLevel = RiskLevel.LOW
)

/**
 * Data access event
 */
data class DataAccessEvent(
    val userId: String,
    val dataType: String,
    val resourceId: String?,
    val accessMethod: AccessMethod,
    val purpose: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val sessionId: String?
)

/**
 * Data modification event
 */
data class DataModificationEvent(
    val userId: String,
    val dataType: String,
    val resourceId: String?,
    val operation: ModificationOperation,
    val oldValue: String?,
    val newValue: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val sessionId: String?
)

/**
 * Privacy-related event
 */
data class PrivacyEvent(
    val userId: String,
    val eventType: AuditEventType,
    val details: Map<String, Any>,
    val ipAddress: String?,
    val userAgent: String?,
    val sessionId: String?
)

/**
 * Security event
 */
data class SecurityEvent(
    val userId: String?,
    val eventType: AuditEventType,
    val severity: SecuritySeverity,
    val details: Map<String, Any>,
    val ipAddress: String?,
    val userAgent: String?,
    val sessionId: String?
)

/**
 * Access methods
 */
enum class AccessMethod {
    API,
    DATABASE_DIRECT,
    EXPORT,
    SYNC,
    ANALYTICS,
    ADMIN_PANEL
}

/**
 * Modification operations
 */
enum class ModificationOperation {
    CREATE,
    UPDATE,
    DELETE,
    ENCRYPT,
    DECRYPT
}

/**
 * Event results
 */
enum class AuditEventResult {
    SUCCESS,
    FAILURE,
    PARTIAL_SUCCESS,
    UNAUTHORIZED,
    ERROR
}

/**
 * Risk levels for audit events
 */
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Security severity levels
 */
enum class SecuritySeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * Audit report for compliance
 */
data class AuditReport(
    val generatedAt: Instant,
    val startDate: Instant,
    val endDate: Instant,
    val userId: String?,
    val totalEvents: Int,
    val eventsByType: Map<AuditEventType, Int>,
    val riskEvents: List<AuditLogEntry>,
    val complianceMetrics: ComplianceMetrics,
    val recommendations: List<String>
)

/**
 * Compliance metrics
 */
data class ComplianceMetrics(
    val dataAccessEvents: Int,
    val consentChanges: Int,
    val dataExports: Int,
    val dataDeletions: Int,
    val securityIncidents: Int,
    val averageResponseTime: Long, // milliseconds
    val complianceScore: Float // 0.0 to 1.0
)

/**
 * Result of audit operations
 */
sealed class AuditResult {
    object Success : AuditResult()
    data class Error(val message: String, val cause: Throwable? = null) : AuditResult()
}