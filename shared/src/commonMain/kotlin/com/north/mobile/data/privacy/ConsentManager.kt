package com.north.mobile.data.privacy

import kotlinx.datetime.Instant

/**
 * Interface for managing user consent and privacy preferences according to PIPEDA requirements
 */
interface ConsentManager {
    /**
     * Record user consent for specific data collection purposes
     */
    suspend fun recordConsent(consent: ConsentRecord): ConsentResult
    
    /**
     * Withdraw consent for specific purposes
     */
    suspend fun withdrawConsent(userId: String, purposes: List<ConsentPurpose>): ConsentResult
    
    /**
     * Get current consent status for a user
     */
    suspend fun getConsentStatus(userId: String): ConsentStatus
    
    /**
     * Check if user has consented to specific purpose
     */
    suspend fun hasConsent(userId: String, purpose: ConsentPurpose): Boolean
    
    /**
     * Update consent preferences
     */
    suspend fun updateConsentPreferences(userId: String, preferences: ConsentPreferences): ConsentResult
    
    /**
     * Get consent history for audit purposes
     */
    suspend fun getConsentHistory(userId: String): List<ConsentRecord>
}

/**
 * Data consent purposes as defined by PIPEDA
 */
enum class ConsentPurpose {
    ACCOUNT_AGGREGATION,
    TRANSACTION_ANALYSIS,
    FINANCIAL_INSIGHTS,
    GOAL_TRACKING,
    GAMIFICATION,
    PUSH_NOTIFICATIONS,
    ANALYTICS,
    MARKETING_COMMUNICATIONS,
    THIRD_PARTY_INTEGRATIONS
}

/**
 * Consent record for tracking user permissions
 */
data class ConsentRecord(
    val id: String,
    val userId: String,
    val purpose: ConsentPurpose,
    val granted: Boolean,
    val timestamp: Instant,
    val ipAddress: String?,
    val userAgent: String?,
    val version: String, // Privacy policy version
    val expiryDate: Instant? = null
)

/**
 * Current consent status for a user
 */
data class ConsentStatus(
    val userId: String,
    val consents: Map<ConsentPurpose, ConsentRecord>,
    val lastUpdated: Instant,
    val privacyPolicyVersion: String
)

/**
 * User consent preferences
 */
data class ConsentPreferences(
    val userId: String,
    val purposes: Map<ConsentPurpose, Boolean>,
    val marketingOptIn: Boolean = false,
    val analyticsOptIn: Boolean = true,
    val dataRetentionPeriod: DataRetentionPeriod = DataRetentionPeriod.STANDARD
)

/**
 * Data retention periods as per PIPEDA guidelines
 */
enum class DataRetentionPeriod(val years: Int) {
    MINIMUM(1),
    STANDARD(7), // Standard for financial records in Canada
    EXTENDED(10)
}

/**
 * Result of consent operations
 */
sealed class ConsentResult {
    object Success : ConsentResult()
    data class Error(val message: String, val cause: Throwable? = null) : ConsentResult()
}