package com.north.mobile.integration

import com.north.mobile.data.privacy.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Integration test for PIPEDA compliance and privacy controls
 */
class PrivacyIntegrationTest {
    
    private lateinit var mockRepository: MockPrivacyRepository
    private lateinit var mockAuditLogger: MockAuditLogger
    private lateinit var consentManager: ConsentManagerImpl
    private lateinit var dataExportManager: DataExportManagerImpl
    private lateinit var dataDeletionManager: DataDeletionManagerImpl
    private lateinit var auditLogger: AuditLoggerImpl
    
    @BeforeTest
    fun setup() {
        mockRepository = MockPrivacyRepository()
        mockAuditLogger = MockAuditLogger()
        consentManager = ConsentManagerImpl(mockRepository, mockAuditLogger)
        dataExportManager = DataExportManagerImpl(mockRepository, mockAuditLogger)
        dataDeletionManager = DataDeletionManagerImpl(mockRepository, mockAuditLogger)
        auditLogger = AuditLoggerImpl(mockRepository)
    }
    
    @Test
    fun `complete privacy workflow - consent, export, and deletion`() = runTest {
        val userId = "test_user_1"
        
        // Step 1: Record user consent
        val consent = ConsentRecord(
            id = "consent_1",
            userId = userId,
            purpose = ConsentPurpose.ACCOUNT_AGGREGATION,
            granted = true,
            timestamp = Clock.System.now(),
            ipAddress = "192.168.1.1",
            userAgent = "TestAgent",
            version = "2025.1.0"
        )
        
        val consentResult = consentManager.recordConsent(consent)
        assertTrue(consentResult is ConsentResult.Success)
        assertTrue(consentManager.hasConsent(userId, ConsentPurpose.ACCOUNT_AGGREGATION))
        
        // Step 2: Request data export
        val exportResult = dataExportManager.requestDataExport(userId, ExportFormat.JSON)
        assertTrue(exportResult is DataExportResult.Success)
        
        val exportId = (exportResult as DataExportResult.Success).exportId
        val exportStatus = dataExportManager.getExportStatus(exportId)
        assertTrue(exportStatus in listOf(ExportStatus.PENDING, ExportStatus.PROCESSING))
        
        // Step 3: Request data deletion
        val deletionResult = dataDeletionManager.requestAccountDeletion(userId, DeletionReason.USER_REQUEST)
        assertTrue(deletionResult is DeletionResult.Success)
        
        val deletionRequestId = (deletionResult as DeletionResult.Success).requestId
        val deletionStatus = dataDeletionManager.getDeletionStatus(deletionRequestId)
        assertEquals(DeletionStatus.PENDING, deletionStatus)
        
        // Step 4: Verify audit logging
        val auditLogs = auditLogger.getAuditLogs(userId)
        assertTrue(auditLogs.isNotEmpty())
        
        // Verify consent event was logged
        assertTrue(mockAuditLogger.privacyEvents.any { it.eventType == AuditEventType.CONSENT_GRANTED })
        
        // Verify export request was logged
        assertTrue(mockAuditLogger.privacyEvents.any { it.eventType == AuditEventType.DATA_EXPORT_REQUESTED })
        
        // Verify deletion request was logged
        assertTrue(mockAuditLogger.privacyEvents.any { it.eventType == AuditEventType.DATA_DELETION_REQUESTED })
    }
    
    @Test
    fun `consent withdrawal should be properly tracked`() = runTest {
        val userId = "test_user_2"
        
        // Grant consent first
        val consent = ConsentRecord(
            id = "consent_1",
            userId = userId,
            purpose = ConsentPurpose.FINANCIAL_INSIGHTS,
            granted = true,
            timestamp = Clock.System.now(),
            ipAddress = null,
            userAgent = null,
            version = "2025.1.0"
        )
        
        consentManager.recordConsent(consent)
        assertTrue(consentManager.hasConsent(userId, ConsentPurpose.FINANCIAL_INSIGHTS))
        
        // Withdraw consent
        val withdrawResult = consentManager.withdrawConsent(userId, listOf(ConsentPurpose.FINANCIAL_INSIGHTS))
        assertTrue(withdrawResult is ConsentResult.Success)
        
        // Verify consent is withdrawn
        assertFalse(consentManager.hasConsent(userId, ConsentPurpose.FINANCIAL_INSIGHTS))
        
        // Verify withdrawal was logged
        assertTrue(mockAuditLogger.privacyEvents.any { it.eventType == AuditEventType.CONSENT_WITHDRAWN })
    }
    
    @Test
    fun `data deletion grace period should be respected`() = runTest {
        val userId = "test_user_3"
        
        // Request deletion
        val deletionResult = dataDeletionManager.requestPartialDeletion(
            userId, 
            listOf(DataType.PROFILE, DataType.ACCOUNTS)
        )
        assertTrue(deletionResult is DeletionResult.Success)
        
        val requestId = (deletionResult as DeletionResult.Success).requestId
        val gracePeriodEnds = deletionResult.gracePeriodEnds
        
        // Should be in pending status during grace period
        assertEquals(DeletionStatus.PENDING, dataDeletionManager.getDeletionStatus(requestId))
        
        // Should be able to cancel during grace period
        assertTrue(dataDeletionManager.cancelDeletion(requestId))
        
        // Status should be cancelled
        assertEquals(DeletionStatus.CANCELLED, dataDeletionManager.getDeletionStatus(requestId))
    }
    
    @Test
    fun `audit report generation should work correctly`() = runTest {
        val userId = "test_user_4"
        val startDate = Clock.System.now()
        
        // Generate some audit events
        auditLogger.logDataAccess(DataAccessEvent(
            userId = userId,
            dataType = "financial_data",
            resourceId = "account_1",
            accessMethod = AccessMethod.API,
            purpose = "dashboard_display",
            ipAddress = "192.168.1.1",
            userAgent = "TestAgent",
            sessionId = "session_1"
        ))
        
        auditLogger.logPrivacyEvent(PrivacyEvent(
            userId = userId,
            eventType = AuditEventType.CONSENT_GRANTED,
            details = mapOf("purpose" to "ACCOUNT_AGGREGATION"),
            ipAddress = "192.168.1.1",
            userAgent = "TestAgent",
            sessionId = "session_1"
        ))
        
        val endDate = Clock.System.now()
        
        // Generate audit report
        val report = auditLogger.generateAuditReport(startDate, endDate, userId)
        
        assertNotNull(report)
        assertEquals(userId, report.userId)
        assertTrue(report.totalEvents >= 2)
        assertTrue(report.eventsByType.containsKey(AuditEventType.DATA_ACCESS))
        assertTrue(report.eventsByType.containsKey(AuditEventType.CONSENT_GRANTED))
        assertTrue(report.complianceMetrics.complianceScore > 0.0f)
        assertTrue(report.recommendations.isNotEmpty())
    }
    
    @Test
    fun `data verification after deletion should work correctly`() = runTest {
        val userId = "test_user_5"
        
        // Set up some mock data existence
        mockRepository.setUserDataExists(userId, "profile", true)
        mockRepository.setUserDataExists(userId, "accounts", true)
        mockRepository.setUserDataExists(userId, "transactions", false)
        
        // Verify deletion status
        val verification = dataDeletionManager.verifyDeletion(userId)
        
        assertNotNull(verification)
        assertEquals(userId, verification.userId)
        assertTrue(verification.deletedDataTypes.contains(DataType.TRANSACTIONS))
        assertTrue(verification.remainingData.contains(DataType.PROFILE))
        assertTrue(verification.remainingData.contains(DataType.ACCOUNTS))
        assertNotNull(verification.verificationHash)
    }
}