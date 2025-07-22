package com.north.mobile.data.privacy

import com.north.mobile.data.repository.Repository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlin.test.*

class ConsentManagerTest {
    
    private lateinit var mockRepository: MockPrivacyRepository
    private lateinit var mockAuditLogger: MockAuditLogger
    private lateinit var consentManager: ConsentManagerImpl
    
    @BeforeTest
    fun setup() {
        mockRepository = MockPrivacyRepository()
        mockAuditLogger = MockAuditLogger()
        consentManager = ConsentManagerImpl(mockRepository, mockAuditLogger)
    }
    
    @Test
    fun `recordConsent should store consent record and log event`() = runTest {
        // Given
        val consent = ConsentRecord(
            id = "consent_1",
            userId = "user_1",
            purpose = ConsentPurpose.ACCOUNT_AGGREGATION,
            granted = true,
            timestamp = Clock.System.now(),
            ipAddress = "192.168.1.1",
            userAgent = "TestAgent",
            version = "2025.1.0"
        )
        
        // When
        val result = consentManager.recordConsent(consent)
        
        // Then
        assertTrue(result is ConsentResult.Success)
        assertTrue(mockRepository.consentRecords.contains(consent))
        assertEquals(1, mockAuditLogger.privacyEvents.size)
        assertEquals(AuditEventType.CONSENT_GRANTED, mockAuditLogger.privacyEvents.first().eventType)
    }
    
    @Test
    fun `withdrawConsent should create withdrawal records for all purposes`() = runTest {
        // Given
        val userId = "user_1"
        val purposes = listOf(ConsentPurpose.ACCOUNT_AGGREGATION, ConsentPurpose.FINANCIAL_INSIGHTS)
        
        // When
        val result = consentManager.withdrawConsent(userId, purposes)
        
        // Then
        assertTrue(result is ConsentResult.Success)
        assertEquals(2, mockRepository.consentRecords.size)
        assertTrue(mockRepository.consentRecords.all { !it.granted })
        assertEquals(2, mockAuditLogger.privacyEvents.size)
        assertTrue(mockAuditLogger.privacyEvents.all { it.eventType == AuditEventType.CONSENT_WITHDRAWN })
    }
    
    @Test
    fun `hasConsent should return true for granted non-expired consent`() = runTest {
        // Given
        val userId = "user_1"
        val purpose = ConsentPurpose.ACCOUNT_AGGREGATION
        val consent = ConsentRecord(
            id = "consent_1",
            userId = userId,
            purpose = purpose,
            granted = true,
            timestamp = Clock.System.now(),
            ipAddress = null,
            userAgent = null,
            version = "2025.1.0",
            expiryDate = Clock.System.now().plus(1, DateTimeUnit.YEAR)
        )
        mockRepository.consentRecords.add(consent)
        
        // When
        val hasConsent = consentManager.hasConsent(userId, purpose)
        
        // Then
        assertTrue(hasConsent)
    }
    
    @Test
    fun `hasConsent should return false for expired consent`() = runTest {
        // Given
        val userId = "user_1"
        val purpose = ConsentPurpose.ACCOUNT_AGGREGATION
        val consent = ConsentRecord(
            id = "consent_1",
            userId = userId,
            purpose = purpose,
            granted = true,
            timestamp = Clock.System.now(),
            ipAddress = null,
            userAgent = null,
            version = "2025.1.0",
            expiryDate = Clock.System.now().plus(-1, DateTimeUnit.DAY) // Expired
        )
        mockRepository.consentRecords.add(consent)
        
        // When
        val hasConsent = consentManager.hasConsent(userId, purpose)
        
        // Then
        assertFalse(hasConsent)
    }
    
    @Test
    fun `getConsentStatus should return current consent status`() = runTest {
        // Given
        val userId = "user_1"
        val consent1 = ConsentRecord(
            id = "consent_1",
            userId = userId,
            purpose = ConsentPurpose.ACCOUNT_AGGREGATION,
            granted = true,
            timestamp = Clock.System.now(),
            ipAddress = null,
            userAgent = null,
            version = "2025.1.0"
        )
        val consent2 = ConsentRecord(
            id = "consent_2",
            userId = userId,
            purpose = ConsentPurpose.FINANCIAL_INSIGHTS,
            granted = false,
            timestamp = Clock.System.now(),
            ipAddress = null,
            userAgent = null,
            version = "2025.1.0"
        )
        mockRepository.consentRecords.addAll(listOf(consent1, consent2))
        
        // When
        val status = consentManager.getConsentStatus(userId)
        
        // Then
        assertEquals(userId, status.userId)
        assertEquals(2, status.consents.size)
        assertTrue(status.consents[ConsentPurpose.ACCOUNT_AGGREGATION]?.granted == true)
        assertTrue(status.consents[ConsentPurpose.FINANCIAL_INSIGHTS]?.granted == false)
    }
    
    @Test
    fun `updateConsentPreferences should store preferences and log event`() = runTest {
        // Given
        val preferences = ConsentPreferences(
            userId = "user_1",
            purposes = mapOf(
                ConsentPurpose.ACCOUNT_AGGREGATION to true,
                ConsentPurpose.FINANCIAL_INSIGHTS to false
            ),
            marketingOptIn = true,
            analyticsOptIn = false,
            dataRetentionPeriod = DataRetentionPeriod.EXTENDED
        )
        
        // When
        val result = consentManager.updateConsentPreferences("user_1", preferences)
        
        // Then
        assertTrue(result is ConsentResult.Success)
        assertEquals(2, mockRepository.consentRecords.size)
        assertTrue(mockRepository.consentPreferences.contains(preferences))
        assertEquals(1, mockAuditLogger.privacyEvents.size)
    }
}