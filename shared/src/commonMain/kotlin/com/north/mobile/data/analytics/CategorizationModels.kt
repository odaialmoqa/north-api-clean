package com.north.mobile.data.analytics

import com.north.mobile.domain.model.Category
import com.north.mobile.domain.model.Transaction
import kotlinx.datetime.LocalDate
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Features extracted from a transaction for ML processing
 */
data class TransactionFeatures(
    val amount: Float,
    val amountAbs: Float,
    val isDebit: Boolean,
    val descriptionLength: Int,
    val merchantName: String,
    val dayOfWeek: Int,
    val dayOfMonth: Int,
    val month: Int,
    val isRecurring: Boolean,
    val descriptionWords: List<String>,
    val merchantWords: List<String>,
    val hasCanadianKeywords: Boolean,
    val hasLocationInfo: Boolean
)

/**
 * Training example for ML model
 */
data class TrainingExample(
    val features: TransactionFeatures,
    val category: Category,
    val weight: Float = 1.0f
)

/**
 * Prediction result from ML model
 */
data class CategoryPrediction(
    val category: Category,
    val confidence: Float,
    val reasoning: String? = null
)

/**
 * User feedback for improving categorization
 */
data class UserFeedback(
    val transactionId: String,
    val correctCategory: Category,
    val userConfidence: Float,
    val timestamp: LocalDate
)

/**
 * Simple ML model for transaction categorization using rule-based and statistical approaches
 */
class CategorizationModel private constructor(
    private val rules: List<CategorizationRule>,
    private val statisticalModel: StatisticalModel,
    private val canadianMerchantPatterns: Map<String, Category>
) {
    var totalPredictions: Int = 0
        private set
    var averageConfidence: Float = 0.0f
        private set
    var lastUpdated: LocalDate? = null
        private set
    
    fun predict(features: TransactionFeatures): CategoryPrediction {
        totalPredictions++
        
        // First, try rule-based categorization
        val ruleBasedResult = applyRules(features)
        if (ruleBasedResult != null && ruleBasedResult.confidence > 0.8f) {
            updateAverageConfidence(ruleBasedResult.confidence)
            return ruleBasedResult
        }
        
        // Try Canadian merchant pattern matching
        val merchantResult = matchCanadianMerchant(features)
        if (merchantResult != null && merchantResult.confidence > 0.7f) {
            updateAverageConfidence(merchantResult.confidence)
            return merchantResult
        }
        
        // Fall back to statistical model
        val statisticalResult = statisticalModel.predict(features)
        updateAverageConfidence(statisticalResult.confidence)
        return statisticalResult
    }
    
    fun getAlternativePredictions(features: TransactionFeatures, topK: Int = 3): List<CategoryPrediction> {
        val allPredictions = mutableListOf<CategoryPrediction>()
        
        // Get rule-based predictions
        rules.forEach { rule ->
            val prediction = rule.evaluate(features)
            if (prediction != null) {
                allPredictions.add(prediction)
            }
        }
        
        // Get statistical predictions
        allPredictions.addAll(statisticalModel.getAllPredictions(features))
        
        // Sort by confidence and return top K
        return allPredictions
            .distinctBy { it.category.id }
            .sortedByDescending { it.confidence }
            .take(topK)
    }
    
    fun updateWithFeedback(transactionId: String, correctCategory: Category, confidence: Float) {
        // Online learning - update model weights based on feedback
        // This is a simplified implementation
        lastUpdated = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
    }
    
    private fun applyRules(features: TransactionFeatures): CategoryPrediction? {
        for (rule in rules) {
            val prediction = rule.evaluate(features)
            if (prediction != null) {
                return prediction
            }
        }
        return null
    }
    
    private fun matchCanadianMerchant(features: TransactionFeatures): CategoryPrediction? {
        val merchantName = features.merchantName.lowercase()
        
        for ((pattern, category) in canadianMerchantPatterns) {
            if (merchantName.contains(pattern)) {
                return CategoryPrediction(
                    category = category,
                    confidence = 0.85f,
                    reasoning = "Matched Canadian merchant pattern: $pattern"
                )
            }
        }
        
        return null
    }
    
    private fun updateAverageConfidence(newConfidence: Float) {
        averageConfidence = if (totalPredictions == 1) {
            newConfidence
        } else {
            (averageConfidence * (totalPredictions - 1) + newConfidence) / totalPredictions
        }
    }
    
    companion object {
        fun train(trainingData: List<TrainingExample>): CategorizationModel {
            val rules = createCategorizationRules()
            val statisticalModel = StatisticalModel.train(trainingData)
            val merchantPatterns = createCanadianMerchantPatterns()
            
            return CategorizationModel(rules, statisticalModel, merchantPatterns)
        }
        
        private fun createCategorizationRules(): List<CategorizationRule> {
            return listOf(
                // Food & Dining rules
                CategorizationRule(
                    condition = { features ->
                        features.descriptionWords.any { word ->
                            listOf("restaurant", "cafe", "coffee", "pizza", "burger", "food").any { 
                                word.contains(it) 
                            }
                        } || features.merchantWords.any { word ->
                            listOf("mcdonald", "subway", "starbucks", "tim horton").any { 
                                word.contains(it) 
                            }
                        }
                    },
                    category = Category.RESTAURANTS,
                    confidence = 0.9f,
                    reasoning = "Matched restaurant keywords"
                ),
                
                // Grocery rules
                CategorizationRule(
                    condition = { features ->
                        features.descriptionWords.any { word ->
                            listOf("grocery", "supermarket", "loblaws", "metro", "sobeys").any { 
                                word.contains(it) 
                            }
                        }
                    },
                    category = Category.GROCERIES,
                    confidence = 0.9f,
                    reasoning = "Matched grocery store keywords"
                ),
                
                // Gas station rules
                CategorizationRule(
                    condition = { features ->
                        features.descriptionWords.any { word ->
                            listOf("gas", "fuel", "petro", "esso", "shell", "station").any { 
                                word.contains(it) 
                            }
                        }
                    },
                    category = Category.GAS,
                    confidence = 0.85f,
                    reasoning = "Matched gas station keywords"
                ),
                
                // Utility bills rules
                CategorizationRule(
                    condition = { features ->
                        features.descriptionWords.any { word ->
                            listOf("hydro", "electric", "utility", "rogers", "bell", "telus").any { 
                                word.contains(it) 
                            }
                        }
                    },
                    category = Category.BILLS,
                    confidence = 0.9f,
                    reasoning = "Matched utility bill keywords"
                ),
                
                // Income rules
                CategorizationRule(
                    condition = { features ->
                        !features.isDebit && features.descriptionWords.any { word ->
                            listOf("salary", "payroll", "deposit", "income", "pay").any { 
                                word.contains(it) 
                            }
                        }
                    },
                    category = Category.SALARY,
                    confidence = 0.95f,
                    reasoning = "Matched salary/income keywords"
                )
            )
        }
        
        private fun createCanadianMerchantPatterns(): Map<String, Category> {
            return mapOf(
                // Food & Dining
                "tim hortons" to Category.RESTAURANTS,
                "tims" to Category.RESTAURANTS,
                "harvey's" to Category.RESTAURANTS,
                "swiss chalet" to Category.RESTAURANTS,
                "boston pizza" to Category.RESTAURANTS,
                
                // Groceries
                "loblaws" to Category.GROCERIES,
                "metro" to Category.GROCERIES,
                "sobeys" to Category.GROCERIES,
                "food basics" to Category.GROCERIES,
                "no frills" to Category.GROCERIES,
                "freshco" to Category.GROCERIES,
                
                // Gas
                "petro-canada" to Category.GAS,
                "esso" to Category.GAS,
                "shell canada" to Category.GAS,
                "husky" to Category.GAS,
                
                // Shopping
                "canadian tire" to Category.SHOPPING,
                "shoppers drug mart" to Category.SHOPPING,
                "the bay" to Category.SHOPPING,
                "winners" to Category.SHOPPING,
                
                // Utilities
                "hydro one" to Category.HYDRO,
                "toronto hydro" to Category.HYDRO,
                "rogers" to Category.INTERNET,
                "bell canada" to Category.INTERNET,
                "telus" to Category.INTERNET
            )
        }
    }
}

/**
 * Rule-based categorization rule
 */
data class CategorizationRule(
    val condition: (TransactionFeatures) -> Boolean,
    val category: Category,
    val confidence: Float,
    val reasoning: String
) {
    fun evaluate(features: TransactionFeatures): CategoryPrediction? {
        return if (condition(features)) {
            CategoryPrediction(category, confidence, reasoning)
        } else {
            null
        }
    }
}

/**
 * Simple statistical model for categorization
 */
class StatisticalModel private constructor(
    private val categoryProbabilities: Map<String, Float>,
    private val featureWeights: Map<String, Map<String, Float>>
) {
    
    fun predict(features: TransactionFeatures): CategoryPrediction {
        val scores = calculateCategoryScores(features)
        val bestCategory = scores.maxByOrNull { it.value }
        
        return if (bestCategory != null) {
            val category = Category.getDefaultCategories().find { it.id == bestCategory.key }
                ?: Category.UNCATEGORIZED
            
            CategoryPrediction(
                category = category,
                confidence = sigmoid(bestCategory.value),
                reasoning = "Statistical model prediction"
            )
        } else {
            CategoryPrediction(
                category = Category.UNCATEGORIZED,
                confidence = 0.5f,
                reasoning = "No strong statistical signal"
            )
        }
    }
    
    fun getAllPredictions(features: TransactionFeatures): List<CategoryPrediction> {
        val scores = calculateCategoryScores(features)
        
        return scores.map { (categoryId, score) ->
            val category = Category.getDefaultCategories().find { it.id == categoryId }
                ?: Category.UNCATEGORIZED
            
            CategoryPrediction(
                category = category,
                confidence = sigmoid(score),
                reasoning = "Statistical model prediction"
            )
        }.sortedByDescending { it.confidence }
    }
    
    private fun calculateCategoryScores(features: TransactionFeatures): Map<String, Float> {
        val scores = mutableMapOf<String, Float>()
        
        categoryProbabilities.forEach { (categoryId, baseProbability) ->
            var score = ln(baseProbability)
            
            // Add feature contributions
            featureWeights[categoryId]?.forEach { (feature, weight) ->
                when (feature) {
                    "amount_range" -> {
                        val amountScore = getAmountScore(features.amountAbs, categoryId)
                        score += weight * amountScore
                    }
                    "is_debit" -> {
                        if (features.isDebit) score += weight
                    }
                    "day_of_week" -> {
                        score += weight * getDayOfWeekScore(features.dayOfWeek, categoryId)
                    }
                    "has_canadian_keywords" -> {
                        if (features.hasCanadianKeywords) score += weight
                    }
                }
            }
            
            scores[categoryId] = score
        }
        
        return scores
    }
    
    private fun getAmountScore(amount: Float, categoryId: String): Float {
        // Simple amount-based scoring
        return when (categoryId) {
            "groceries" -> if (amount in 20f..200f) 1.0f else 0.0f
            "gas" -> if (amount in 30f..100f) 1.0f else 0.0f
            "restaurants" -> if (amount in 10f..80f) 1.0f else 0.0f
            "bills" -> if (amount in 50f..300f) 1.0f else 0.0f
            else -> 0.0f
        }
    }
    
    private fun getDayOfWeekScore(dayOfWeek: Int, categoryId: String): Float {
        // Weekend vs weekday patterns
        val isWeekend = dayOfWeek == 5 || dayOfWeek == 6 // Saturday or Sunday
        
        return when (categoryId) {
            "restaurants" -> if (isWeekend) 0.2f else 0.0f
            "groceries" -> if (isWeekend) 0.1f else 0.0f
            "entertainment" -> if (isWeekend) 0.3f else 0.0f
            else -> 0.0f
        }
    }
    
    private fun sigmoid(x: Float): Float {
        return 1.0f / (1.0f + exp(-x))
    }
    
    companion object {
        fun train(trainingData: List<TrainingExample>): StatisticalModel {
            // Calculate category probabilities
            val categoryProbabilities = calculateCategoryProbabilities(trainingData)
            
            // Calculate feature weights (simplified)
            val featureWeights = calculateFeatureWeights(trainingData)
            
            return StatisticalModel(categoryProbabilities, featureWeights)
        }
        
        private fun calculateCategoryProbabilities(trainingData: List<TrainingExample>): Map<String, Float> {
            val categoryCounts = trainingData.groupBy { it.category.id }
                .mapValues { it.value.size.toFloat() }
            
            val total = categoryCounts.values.sum()
            
            return categoryCounts.mapValues { (_, count) ->
                max(0.001f, count / total) // Smoothing
            }
        }
        
        private fun calculateFeatureWeights(trainingData: List<TrainingExample>): Map<String, Map<String, Float>> {
            // Simplified feature weight calculation
            val weights = mutableMapOf<String, MutableMap<String, Float>>()
            
            Category.getDefaultCategories().forEach { category ->
                weights[category.id] = mutableMapOf(
                    "amount_range" to Random.nextFloat() * 0.5f + 0.5f,
                    "is_debit" to Random.nextFloat() * 0.3f + 0.2f,
                    "day_of_week" to Random.nextFloat() * 0.2f + 0.1f,
                    "has_canadian_keywords" to Random.nextFloat() * 0.4f + 0.6f
                )
            }
            
            return weights
        }
    }
}

/**
 * Anomaly detection model for unusual spending patterns
 */
class AnomalyDetectionModel private constructor(
    private val categoryBaselines: Map<String, CategoryBaseline>
) {
    
    fun detectAnomalies(transactions: List<Transaction>): List<Transaction> {
        val anomalies = mutableListOf<Transaction>()
        
        transactions.forEach { transaction ->
            val baseline = categoryBaselines[transaction.category.id]
            if (baseline != null && isAnomaly(transaction, baseline)) {
                anomalies.add(transaction)
            }
        }
        
        return anomalies
    }
    
    private fun isAnomaly(transaction: Transaction, baseline: CategoryBaseline): Boolean {
        val amount = transaction.absoluteAmount.cents.toFloat()
        val zScore = (amount - baseline.meanAmount) / baseline.stdDevAmount
        
        return kotlin.math.abs(zScore) > 2.0f // 2 standard deviations
    }
    
    companion object {
        fun train(historicalTransactions: List<Transaction>): AnomalyDetectionModel {
            val categoryBaselines = calculateCategoryBaselines(historicalTransactions)
            return AnomalyDetectionModel(categoryBaselines)
        }
        
        private fun calculateCategoryBaselines(transactions: List<Transaction>): Map<String, CategoryBaseline> {
            return transactions.groupBy { it.category.id }
                .mapValues { (_, categoryTransactions) ->
                    val amounts = categoryTransactions.map { it.absoluteAmount.cents.toFloat() }
                    val mean = amounts.average().toFloat()
                    val variance = amounts.map { (it - mean) * (it - mean) }.average().toFloat()
                    val stdDev = kotlin.math.sqrt(variance)
                    
                    CategoryBaseline(
                        meanAmount = mean,
                        stdDevAmount = stdDev,
                        transactionCount = categoryTransactions.size,
                        frequencyPerWeek = categoryTransactions.size / 52.0f // Assuming 1 year of data
                    )
                }
        }
    }
}

/**
 * Baseline statistics for a category
 */
data class CategoryBaseline(
    val meanAmount: Float,
    val stdDevAmount: Float,
    val transactionCount: Int,
    val frequencyPerWeek: Float
)