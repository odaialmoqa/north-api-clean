package com.north.mobile.data.privacy

import com.north.mobile.data.repository.PrivacyRepository
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.days

/**
 * Implementation of ConsentManager for PIPEDA compliance
 */
class ConsentManagerImpl(
    private val repository: PrivacyRepository,
    private val auditLogger: AuditLogger
) : ConsentManager {
    
    override suspend fun recordConsent(consent: ConsentRecord): ConsentResult {
        return try {
            // Store consent record
            repository.insertConsentRecord(consent)
            
            // Log the consent event
            auditLogger.logPrivacyEvent(
                PrivacyEvent(
                    userId = consent.userId,
                    eventType = if (consent.granted) AuditEventType.CONSENT_GRANTED else AuditEventType.CONSENT_WITHDRAWN,
                    details = mapOf(
                        "purpose" to consent.purpose.name,
                        "version" to consent.version,
                        "expiryDate" to (consent.expiryDate?.toString() ?: "none")
                    ),
                    ipAddress = consent.ipAddress,
                    userAgent = consent.userAgent,
                    sessionId = null
                )
            )
            
            ConsentResult.Success
        } catch (e: Exception) {
            ConsentResult.Error("Failed to record consent", e)
        }
    }
    
    override suspend fun withdrawConsent(userId: String, purposes: List<ConsentPurpose>): ConsentResult {
        return try {
            val timestamp = Clock.System.now()
            
            purposes.forEach { purpose ->
                val withdrawalRecord = ConsentRecord(
                    id = generateId(),
                    userId = userId,
                    purpose = purpose,
                    granted = false,
                    timestamp = timestamp,
                    ipAddress = null,
                    userAgent = null,
                    version = getCurrentPrivacyPolicyVersion()
                )
                
                repository.insertConsentRecord(withdrawalRecord)
                
                auditLogger.logPrivacyEvent(
                    PrivacyEvent(
                        userId = userId,
                        eventType = AuditEventType.CONSENT_WITHDRAWN,
                        details = mapOf("purpose" to purpose.name),
                        ipAddress = null,
                        userAgent = null,
                        sessionId = null
                    )
                )
            }
            
            ConsentResult.Success
        } catch (e: Exception) {
            ConsentResult.Error("Failed to withdraw consent", e)
        }
    }
    
    override suspend fun getConsentStatus(userId: String): ConsentStatus {
        val consents = repository.getLatestConsentsForUser(userId)
        val consentMap = consents.associateBy { it.purpose }
        
        return ConsentStatus(
            userId = userId,
            consents = consentMap,
            lastUpdated = consents.maxOfOrNull { it.timestamp } ?: Clock.System.now(),
            privacyPolicyVersion = getCurrentPrivacyPolicyVersion()
        )
    }
    
    override suspend fun hasConsent(userId: String, purpose: ConsentPurpose): Boolean {
        val latestConsent = repository.getLatestConsentForPurpose(userId, purpose)
        return latestConsent?.granted == true && !isConsentExpired(latestConsent)
    }
    
    override suspend fun updateConsentPreferences(userId: String, preferences: ConsentPreferences): ConsentResult {
        return try {
            val timestamp = Clock.System.now()
            
            preferences.purposes.forEach { (purpose, granted) ->
                val consentRecord = ConsentRecord(
                    id = generateId(),
                    userId = userId,
                    purpose = purpose,
                    granted = granted,
                    timestamp = timestamp,
                    ipAddress = null,
                    userAgent = null,
                    version = getCurrentPrivacyPolicyVersion(),
                    expiryDate = calculateExpiryDate(purpose, preferences.dataRetentionPeriod)
                )
                
                repository.insertConsentRecord(consentRecord)
            }
            
            // Store preferences
            repository.updateConsentPreferences(preferences)
            
            auditLogger.logPrivacyEvent(
                PrivacyEvent(
                    userId = userId,
                    eventType = AuditEventType.CONSENT_GRANTED,
                    details = mapOf(
                        "preferences_updated" to preferences.purposes.size,
                        "marketing_opt_in" to preferences.marketingOptIn,
                        "analytics_opt_in" to preferences.analyticsOptIn,
                        "retention_period" to preferences.dataRetentionPeriod.name
                    ),
                    ipAddress = null,
                    userAgent = null,
                    sessionId = null
                )
            )
            
            ConsentResult.Success
        } catch (e: Exception) {
            ConsentResult.Error("Failed to update consent preferences", e)
        }
    }
    
    override suspend fun getConsentHistory(userId: String): List<ConsentRecord> {
        return repository.getConsentHistory(userId)
    }
    
    private fun isConsentExpired(consent: ConsentRecord): Boolean {
        return consent.expiryDate?.let { it < Clock.System.now() } ?: false
    }
    
    private fun calculateExpiryDate(purpose: ConsentPurpose, retentionPeriod: DataRetentionPeriod): Instant {
        return Clock.System.now() + (retentionPeriod.years * 365).days
    }
    
    private fun getCurrentPrivacyPolicyVersion(): String {
        return "2025.1.0" // This should be configurable
    }
    
    private fun generateId(): String {
        return "consent_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
}