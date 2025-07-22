package com.north.mobile.data.analytics

import com.north.mobile.domain.model.Category
import com.north.mobile.domain.model.Transaction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Implementation of transaction categorization service using ML-based approaches
 */
class TransactionCategorizationServiceImpl(
    private val trainingDataProvider: TrainingDataProvider,
    private val userFeedbackRepository: UserFeedbackRepository,
    private val transactionHistoryProvider: TransactionHistoryProvider
) : TransactionCategorizationService {
    
    private val modelMutex = Mutex()
    private var categorizationModel: CategorizationModel? = null
    private var anomalyDetectionModel: AnomalyDetectionModel? = null
    
    override suspend fun categorizeTransaction(transaction: Transaction): CategorizationResult {
        val model = getOrCreateModel()
        
        // Extract features from transaction
        val features = extractFeatures(transaction)
        
        // Get prediction from model
        val prediction = model.predict(features)
        
        // Get alternative suggestions
        val alternatives = model.getAlternativePredictions(features, topK = 3)
            .filter { it.category.id != prediction.category.id }
            .map { CategorySuggestion(it.category, it.confidence, it.reasoning) }
        
        return CategorizationResult(
            transactionId = transaction.id,
            suggestedCategory = prediction.category,
            confidence = prediction.confidence,
            alternativeCategories = alternatives,
            reasoning = prediction.reasoning
        )
    }
    
    override suspend fun categorizeTransactions(transactions: List<Transaction>): List<CategorizationResult> {
        return transactions.map { categorizeTransaction(it) }
    }
    
    override suspend fun provideFeedback(
        transactionId: String, 
        correctCategory: Category, 
        confidence: Float
    ) {
        // Store user feedback
        userFeedbackRepository.storeFeedback(
            UserFeedback(
                transactionId = transactionId,
                correctCategory = correctCategory,
                userConfidence = confidence,
                timestamp = Clock.System.todayIn(TimeZone.currentSystemDefault())
            )
        )
        
        // Update model with feedback (online learning)
        modelMutex.withLock {
            categorizationModel?.updateWithFeedback(transactionId, correctCategory, confidence)
        }
    }
    
    override suspend fun detectUnusualSpending(transactions: List<Transaction>): List<UnusualSpendingAlert> {
        val anomalyModel = getOrCreateAnomalyModel()
        val alerts = mutableListOf<UnusualSpendingAlert>()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        // Group transactions by category for analysis
        val transactionsByCategory = transactions.groupBy { it.category.id }
        
        transactionsByCategory.forEach { (categoryId, categoryTransactions) ->
            // Detect amount anomalies
            val amountAnomalies = detectAmountAnomalies(categoryTransactions)
            alerts.addAll(amountAnomalies.map { transaction ->
                UnusualSpendingAlert(
                    id = "amount_${transaction.id}",
                    transactionId = transaction.id,
                    alertType = UnusualSpendingType.AMOUNT_ANOMALY,
                    severity = calculateAmountAnomalySeverity(transaction, categoryTransactions),
                    message = "Unusual ${if (transaction.amount.isNegative) "expense" else "income"} amount for ${transaction.category.name}",
                    suggestedAction = "Please verify this transaction is correct",
                    detectedAt = today
                )
            })
            
            // Detect frequency anomalies
            val frequencyAnomalies = detectFrequencyAnomalies(categoryTransactions)
            alerts.addAll(frequencyAnomalies)
        }
        
        // Detect new merchants
        val newMerchantAlerts = detectNewMerchants(transactions)
        alerts.addAll(newMerchantAlerts)
        
        // Detect potential duplicates
        val duplicateAlerts = detectPotentialDuplicates(transactions)
        alerts.addAll(duplicateAlerts)
        
        return alerts.sortedByDescending { it.severity }
    }
    
    override suspend fun getCategorizationStats(): CategorizationStats {
        val feedback = userFeedbackRepository.getAllFeedback()
        val model = categorizationModel
        
        return CategorizationStats(
            totalTransactionsCategorized = model?.totalPredictions ?: 0,
            averageConfidence = model?.averageConfidence ?: 0.0f,
            userFeedbackCount = feedback.size,
            accuracyRate = calculateAccuracyRate(feedback),
            categoryDistribution = getCategoryDistribution(),
            lastModelUpdate = model?.lastUpdated
        )
    }
    
    override suspend fun retrainModels() {
        modelMutex.withLock {
            val trainingData = trainingDataProvider.getTrainingData()
            val userFeedback = userFeedbackRepository.getAllFeedback()
            
            // Combine training data with user feedback
            val combinedData = combineTrainingData(trainingData, userFeedback)
            
            // Retrain categorization model
            categorizationModel = CategorizationModel.train(combinedData)
            
            // Retrain anomaly detection model
            val historicalTransactions = transactionHistoryProvider.getAllTransactions()
            anomalyDetectionModel = AnomalyDetectionModel.train(historicalTransactions)
        }
    }
    
    private suspend fun getOrCreateModel(): CategorizationModel {
        return modelMutex.withLock {
            categorizationModel ?: run {
                val trainingData = trainingDataProvider.getTrainingData()
                val model = CategorizationModel.train(trainingData)
                categorizationModel = model
                model
            }
        }
    }
    
    private suspend fun getOrCreateAnomalyModel(): AnomalyDetectionModel {
        return modelMutex.withLock {
            anomalyDetectionModel ?: run {
                val historicalTransactions = transactionHistoryProvider.getAllTransactions()
                val model = AnomalyDetectionModel.train(historicalTransactions)
                anomalyDetectionModel = model
                model
            }
        }
    }
    
    private fun extractFeatures(transaction: Transaction): TransactionFeatures {
        val description = transaction.description.lowercase()
        val merchantName = transaction.merchantName?.lowercase() ?: ""
        
        return TransactionFeatures(
            amount = transaction.amount.cents.toFloat(),
            amountAbs = transaction.absoluteAmount.cents.toFloat(),
            isDebit = transaction.isDebit,
            descriptionLength = description.length,
            merchantName = merchantName,
            dayOfWeek = transaction.date.dayOfWeek.ordinal,
            dayOfMonth = transaction.date.dayOfMonth,
            month = transaction.date.monthNumber,
            isRecurring = transaction.isRecurring,
            // Text features
            descriptionWords = description.split(" ").filter { it.isNotBlank() },
            merchantWords = merchantName.split(" ").filter { it.isNotBlank() },
            // Canadian-specific patterns
            hasCanadianKeywords = containsCanadianKeywords(description, merchantName),
            hasLocationInfo = transaction.location != null
        )
    }
    
    private fun containsCanadianKeywords(description: String, merchantName: String): Boolean {
        val canadianKeywords = setOf(
            "tim hortons", "tims", "canadian tire", "loblaws", "metro", "sobeys",
            "shoppers drug mart", "rbc", "td bank", "bmo", "scotiabank", "cibc",
            "hydro", "rogers", "bell", "telus", "petro-canada", "esso", "shell canada"
        )
        
        val text = "$description $merchantName".lowercase()
        return canadianKeywords.any { keyword -> text.contains(keyword) }
    }
    
    private fun detectAmountAnomalies(transactions: List<Transaction>): List<Transaction> {
        if (transactions.size < 3) return emptyList()
        
        val amounts = transactions.map { it.absoluteAmount.cents.toFloat() }
        val mean = amounts.average().toFloat()
        val stdDev = calculateStandardDeviation(amounts, mean)
        
        // Consider transactions beyond 2 standard deviations as anomalies
        val threshold = 2.0f * stdDev
        
        return transactions.filter { transaction ->
            abs(transaction.absoluteAmount.cents.toFloat() - mean) > threshold
        }
    }
    
    private fun calculateStandardDeviation(values: List<Float>, mean: Float): Float {
        if (values.size <= 1) return 0f
        
        val variance = values.map { (it - mean) * (it - mean) }.average().toFloat()
        return kotlin.math.sqrt(variance)
    }
    
    private fun calculateAmountAnomalySeverity(
        transaction: Transaction, 
        categoryTransactions: List<Transaction>
    ): AlertSeverity {
        val amounts = categoryTransactions.map { it.absoluteAmount.cents.toFloat() }
        val mean = amounts.average().toFloat()
        val deviation = abs(transaction.absoluteAmount.cents.toFloat() - mean) / mean
        
        return when {
            deviation > 5.0f -> AlertSeverity.CRITICAL
            deviation > 3.0f -> AlertSeverity.HIGH
            deviation > 2.0f -> AlertSeverity.MEDIUM
            else -> AlertSeverity.LOW
        }
    }
    
    private fun detectFrequencyAnomalies(transactions: List<Transaction>): List<UnusualSpendingAlert> {
        // Group by merchant and detect unusual frequency
        val merchantFrequency = transactions
            .filter { it.merchantName != null }
            .groupBy { it.merchantName!! }
            .filter { it.value.size > 1 }
        
        val alerts = mutableListOf<UnusualSpendingAlert>()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        merchantFrequency.forEach { (merchant, merchantTransactions) ->
            // Check for multiple transactions on the same day
            val transactionsByDate = merchantTransactions.groupBy { it.date }
            transactionsByDate.forEach { (date, dayTransactions) ->
                if (dayTransactions.size > 2) {
                    dayTransactions.forEach { transaction ->
                        alerts.add(
                            UnusualSpendingAlert(
                                id = "freq_${transaction.id}",
                                transactionId = transaction.id,
                                alertType = UnusualSpendingType.FREQUENCY_ANOMALY,
                                severity = AlertSeverity.MEDIUM,
                                message = "Multiple transactions at $merchant on the same day",
                                suggestedAction = "Check for potential duplicate charges",
                                detectedAt = today
                            )
                        )
                    }
                }
            }
        }
        
        return alerts
    }
    
    private fun detectNewMerchants(transactions: List<Transaction>): List<UnusualSpendingAlert> {
        // This would typically check against historical data
        // For now, we'll simulate by checking for merchants with only one transaction
        val merchantCounts = transactions
            .filter { it.merchantName != null }
            .groupBy { it.merchantName!! }
            .mapValues { it.value.size }
        
        val newMerchants = merchantCounts.filter { it.value == 1 }
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        return transactions
            .filter { it.merchantName != null && newMerchants.containsKey(it.merchantName!!) }
            .map { transaction ->
                UnusualSpendingAlert(
                    id = "new_merchant_${transaction.id}",
                    transactionId = transaction.id,
                    alertType = UnusualSpendingType.NEW_MERCHANT,
                    severity = AlertSeverity.LOW,
                    message = "First transaction at ${transaction.merchantName}",
                    suggestedAction = "Verify this is a legitimate transaction",
                    detectedAt = today
                )
            }
    }
    
    private fun detectPotentialDuplicates(transactions: List<Transaction>): List<UnusualSpendingAlert> {
        val alerts = mutableListOf<UnusualSpendingAlert>()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        // Check for transactions with same amount, merchant, and date
        for (i in transactions.indices) {
            for (j in i + 1 until transactions.size) {
                val t1 = transactions[i]
                val t2 = transactions[j]
                
                if (t1.amount == t2.amount &&
                    t1.merchantName == t2.merchantName &&
                    t1.date == t2.date &&
                    t1.merchantName != null) {
                    
                    alerts.add(
                        UnusualSpendingAlert(
                            id = "duplicate_${t1.id}_${t2.id}",
                            transactionId = t1.id,
                            alertType = UnusualSpendingType.DUPLICATE_SUSPECTED,
                            severity = AlertSeverity.HIGH,
                            message = "Potential duplicate transaction at ${t1.merchantName}",
                            suggestedAction = "Check if this is a duplicate charge",
                            detectedAt = today
                        )
                    )
                }
            }
        }
        
        return alerts
    }
    
    private fun calculateAccuracyRate(feedback: List<UserFeedback>): Float {
        if (feedback.isEmpty()) return 0.0f
        
        // This would compare original predictions with user corrections
        // For now, return a simulated accuracy based on feedback confidence
        val avgConfidence = feedback.map { it.userConfidence }.average().toFloat()
        return min(1.0f, max(0.0f, avgConfidence))
    }
    
    private fun getCategoryDistribution(): Map<String, Int> {
        // This would typically come from actual transaction data
        // For now, return a simulated distribution
        return mapOf(
            "food" to 150,
            "transport" to 80,
            "shopping" to 120,
            "bills" to 45,
            "entertainment" to 60,
            "healthcare" to 25,
            "uncategorized" to 30
        )
    }
    
    private fun combineTrainingData(
        trainingData: List<TrainingExample>,
        userFeedback: List<UserFeedback>
    ): List<TrainingExample> {
        // Convert user feedback to training examples and combine
        val feedbackExamples = userFeedback.mapNotNull { feedback ->
            transactionHistoryProvider.getTransaction(feedback.transactionId)?.let { transaction ->
                TrainingExample(
                    features = extractFeatures(transaction),
                    category = feedback.correctCategory,
                    weight = feedback.userConfidence
                )
            }
        }
        
        return trainingData + feedbackExamples
    }
}