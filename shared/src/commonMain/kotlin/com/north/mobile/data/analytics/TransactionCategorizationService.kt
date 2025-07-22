package com.north.mobile.data.analytics

import com.north.mobile.domain.model.Category
import com.north.mobile.domain.model.Transaction
import kotlinx.datetime.LocalDate

/**
 * Service for intelligent transaction categorization using machine learning
 */
interface TransactionCategorizationService {
    /**
     * Categorize a single transaction using ML models
     */
    suspend fun categorizeTransaction(transaction: Transaction): CategorizationResult
    
    /**
     * Batch categorize multiple transactions
     */
    suspend fun categorizeTransactions(transactions: List<Transaction>): List<CategorizationResult>
    
    /**
     * Learn from user feedback to improve categorization
     */
    suspend fun provideFeedback(transactionId: String, correctCategory: Category, confidence: Float = 1.0f)
    
    /**
     * Detect unusual spending patterns
     */
    suspend fun detectUnusualSpending(transactions: List<Transaction>): List<UnusualSpendingAlert>
    
    /**
     * Get categorization statistics for a user
     */
    suspend fun getCategorizationStats(): CategorizationStats
    
    /**
     * Retrain models with new data
     */
    suspend fun retrainModels()
}

/**
 * Result of transaction categorization
 */
data class CategorizationResult(
    val transactionId: String,
    val suggestedCategory: Category,
    val confidence: Float, // 0.0 to 1.0
    val alternativeCategories: List<CategorySuggestion> = emptyList(),
    val reasoning: String? = null
)

/**
 * Alternative category suggestion with confidence
 */
data class CategorySuggestion(
    val category: Category,
    val confidence: Float,
    val reasoning: String? = null
)

/**
 * Alert for unusual spending patterns
 */
data class UnusualSpendingAlert(
    val id: String,
    val transactionId: String,
    val alertType: UnusualSpendingType,
    val severity: AlertSeverity,
    val message: String,
    val suggestedAction: String? = null,
    val detectedAt: LocalDate
)

/**
 * Types of unusual spending patterns
 */
enum class UnusualSpendingType {
    AMOUNT_ANOMALY,      // Unusually high/low amount for category
    FREQUENCY_ANOMALY,   // Unusual frequency of transactions
    NEW_MERCHANT,        // First time at this merchant
    LOCATION_ANOMALY,    // Transaction in unusual location
    TIME_ANOMALY,        // Transaction at unusual time
    DUPLICATE_SUSPECTED  // Potential duplicate transaction
}

/**
 * Severity levels for alerts
 */
enum class AlertSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Statistics about categorization performance
 */
data class CategorizationStats(
    val totalTransactionsCategorized: Int,
    val averageConfidence: Float,
    val userFeedbackCount: Int,
    val accuracyRate: Float,
    val categoryDistribution: Map<String, Int>,
    val lastModelUpdate: LocalDate?
)