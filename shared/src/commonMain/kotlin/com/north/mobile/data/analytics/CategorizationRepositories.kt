package com.north.mobile.data.analytics

import com.north.mobile.domain.model.Transaction

/**
 * Repository for storing and retrieving user feedback on categorization
 */
interface UserFeedbackRepository {
    suspend fun storeFeedback(feedback: UserFeedback)
    suspend fun getFeedback(transactionId: String): UserFeedback?
    suspend fun getAllFeedback(): List<UserFeedback>
    suspend fun getFeedbackByCategory(categoryId: String): List<UserFeedback>
    suspend fun deleteFeedback(transactionId: String)
}

/**
 * Provider for training data used to train ML models
 */
interface TrainingDataProvider {
    suspend fun getTrainingData(): List<TrainingExample>
    suspend fun addTrainingExample(example: TrainingExample)
    suspend fun updateTrainingData(examples: List<TrainingExample>)
}

/**
 * Provider for historical transaction data
 */
interface TransactionHistoryProvider {
    suspend fun getAllTransactions(): List<Transaction>
    suspend fun getTransaction(transactionId: String): Transaction?
    suspend fun getTransactionsByCategory(categoryId: String): List<Transaction>
    suspend fun getTransactionsByDateRange(startDate: kotlinx.datetime.LocalDate, endDate: kotlinx.datetime.LocalDate): List<Transaction>
    suspend fun getTransactionsByMerchant(merchantName: String): List<Transaction>
}

/**
 * Repository for managing custom categories
 */
interface CategoryRepository {
    suspend fun getAllCategories(): List<com.north.mobile.domain.model.Category>
    suspend fun getCustomCategories(): List<com.north.mobile.domain.model.Category>
    suspend fun createCustomCategory(category: com.north.mobile.domain.model.Category): com.north.mobile.domain.model.Category
    suspend fun updateCategory(category: com.north.mobile.domain.model.Category): com.north.mobile.domain.model.Category
    suspend fun deleteCategory(categoryId: String)
    suspend fun getCategoryById(categoryId: String): com.north.mobile.domain.model.Category?
}

/**
 * Repository for storing unusual spending alerts
 */
interface UnusualSpendingAlertRepository {
    suspend fun storeAlert(alert: UnusualSpendingAlert)
    suspend fun getAlerts(): List<UnusualSpendingAlert>
    suspend fun getUnreadAlerts(): List<UnusualSpendingAlert>
    suspend fun markAlertAsRead(alertId: String)
    suspend fun dismissAlert(alertId: String)
    suspend fun getAlertsByType(type: UnusualSpendingType): List<UnusualSpendingAlert>
}

/**
 * In-memory implementation of UserFeedbackRepository for testing/demo
 */
class InMemoryUserFeedbackRepository : UserFeedbackRepository {
    private val feedbackStorage = mutableMapOf<String, UserFeedback>()
    
    override suspend fun storeFeedback(feedback: UserFeedback) {
        feedbackStorage[feedback.transactionId] = feedback
    }
    
    override suspend fun getFeedback(transactionId: String): UserFeedback? {
        return feedbackStorage[transactionId]
    }
    
    override suspend fun getAllFeedback(): List<UserFeedback> {
        return feedbackStorage.values.toList()
    }
    
    override suspend fun getFeedbackByCategory(categoryId: String): List<UserFeedback> {
        return feedbackStorage.values.filter { it.correctCategory.id == categoryId }
    }
    
    override suspend fun deleteFeedback(transactionId: String) {
        feedbackStorage.remove(transactionId)
    }
}

/**
 * In-memory implementation of TrainingDataProvider
 */
class InMemoryTrainingDataProvider : TrainingDataProvider {
    private val trainingData = mutableListOf<TrainingExample>()
    
    init {
        // Initialize with some Canadian-specific training data
        initializeCanadianTrainingData()
    }
    
    override suspend fun getTrainingData(): List<TrainingExample> {
        return trainingData.toList()
    }
    
    override suspend fun addTrainingExample(example: TrainingExample) {
        trainingData.add(example)
    }
    
    override suspend fun updateTrainingData(examples: List<TrainingExample>) {
        trainingData.clear()
        trainingData.addAll(examples)
    }
    
    private fun initializeCanadianTrainingData() {
        // Add some sample Canadian merchant training data
        val canadianExamples = listOf(
            // Tim Hortons examples
            createTrainingExample("TIM HORTONS #1234", "tim hortons", 4.50f, com.north.mobile.domain.model.Category.RESTAURANTS),
            createTrainingExample("TIMS COFFEE", "tims", 3.25f, com.north.mobile.domain.model.Category.RESTAURANTS),
            
            // Grocery stores
            createTrainingExample("LOBLAWS SUPERMARKET", "loblaws", 85.30f, com.north.mobile.domain.model.Category.GROCERIES),
            createTrainingExample("METRO GROCERY", "metro", 67.45f, com.north.mobile.domain.model.Category.GROCERIES),
            createTrainingExample("SOBEYS STORE", "sobeys", 92.15f, com.north.mobile.domain.model.Category.GROCERIES),
            
            // Gas stations
            createTrainingExample("PETRO-CANADA GAS", "petro-canada", 55.00f, com.north.mobile.domain.model.Category.GAS),
            createTrainingExample("ESSO STATION", "esso", 48.75f, com.north.mobile.domain.model.Category.GAS),
            
            // Utilities
            createTrainingExample("HYDRO ONE PAYMENT", "hydro one", 125.50f, com.north.mobile.domain.model.Category.HYDRO),
            createTrainingExample("ROGERS COMMUNICATIONS", "rogers", 89.99f, com.north.mobile.domain.model.Category.INTERNET),
            createTrainingExample("BELL CANADA", "bell", 95.00f, com.north.mobile.domain.model.Category.INTERNET),
            
            // Shopping
            createTrainingExample("CANADIAN TIRE STORE", "canadian tire", 45.99f, com.north.mobile.domain.model.Category.SHOPPING),
            createTrainingExample("SHOPPERS DRUG MART", "shoppers", 23.45f, com.north.mobile.domain.model.Category.SHOPPING)
        )
        
        trainingData.addAll(canadianExamples)
    }
    
    private fun createTrainingExample(
        description: String,
        merchantName: String,
        amount: Float,
        category: com.north.mobile.domain.model.Category
    ): TrainingExample {
        val features = TransactionFeatures(
            amount = -amount, // Negative for expenses
            amountAbs = amount,
            isDebit = true,
            descriptionLength = description.length,
            merchantName = merchantName.lowercase(),
            dayOfWeek = 2, // Tuesday
            dayOfMonth = 15,
            month = 6, // June
            isRecurring = false,
            descriptionWords = description.lowercase().split(" "),
            merchantWords = merchantName.lowercase().split(" "),
            hasCanadianKeywords = true,
            hasLocationInfo = false
        )
        
        return TrainingExample(features, category, 1.0f)
    }
}

/**
 * In-memory implementation of TransactionHistoryProvider
 */
class InMemoryTransactionHistoryProvider : TransactionHistoryProvider {
    private val transactions = mutableListOf<Transaction>()
    
    override suspend fun getAllTransactions(): List<Transaction> {
        return transactions.toList()
    }
    
    override suspend fun getTransaction(transactionId: String): Transaction? {
        return transactions.find { it.id == transactionId }
    }
    
    override suspend fun getTransactionsByCategory(categoryId: String): List<Transaction> {
        return transactions.filter { it.category.id == categoryId }
    }
    
    override suspend fun getTransactionsByDateRange(
        startDate: kotlinx.datetime.LocalDate,
        endDate: kotlinx.datetime.LocalDate
    ): List<Transaction> {
        return transactions.filter { it.date >= startDate && it.date <= endDate }
    }
    
    override suspend fun getTransactionsByMerchant(merchantName: String): List<Transaction> {
        return transactions.filter { 
            it.merchantName?.lowercase()?.contains(merchantName.lowercase()) == true 
        }
    }
    
    fun addTransaction(transaction: Transaction) {
        transactions.add(transaction)
    }
    
    fun addTransactions(newTransactions: List<Transaction>) {
        transactions.addAll(newTransactions)
    }
}

/**
 * In-memory implementation of CategoryRepository
 */
class InMemoryCategoryRepository : CategoryRepository {
    private val customCategories = mutableMapOf<String, com.north.mobile.domain.model.Category>()
    
    override suspend fun getAllCategories(): List<com.north.mobile.domain.model.Category> {
        return com.north.mobile.domain.model.Category.getDefaultCategories() + customCategories.values
    }
    
    override suspend fun getCustomCategories(): List<com.north.mobile.domain.model.Category> {
        return customCategories.values.toList()
    }
    
    override suspend fun createCustomCategory(category: com.north.mobile.domain.model.Category): com.north.mobile.domain.model.Category {
        val customCategory = category.copy(isCustom = true)
        customCategories[customCategory.id] = customCategory
        return customCategory
    }
    
    override suspend fun updateCategory(category: com.north.mobile.domain.model.Category): com.north.mobile.domain.model.Category {
        if (category.isCustom) {
            customCategories[category.id] = category
        }
        return category
    }
    
    override suspend fun deleteCategory(categoryId: String) {
        customCategories.remove(categoryId)
    }
    
    override suspend fun getCategoryById(categoryId: String): com.north.mobile.domain.model.Category? {
        return customCategories[categoryId] 
            ?: com.north.mobile.domain.model.Category.getDefaultCategories().find { it.id == categoryId }
    }
}

/**
 * In-memory implementation of UnusualSpendingAlertRepository
 */
class InMemoryUnusualSpendingAlertRepository : UnusualSpendingAlertRepository {
    private val alerts = mutableMapOf<String, UnusualSpendingAlert>()
    private val readAlerts = mutableSetOf<String>()
    private val dismissedAlerts = mutableSetOf<String>()
    
    override suspend fun storeAlert(alert: UnusualSpendingAlert) {
        alerts[alert.id] = alert
    }
    
    override suspend fun getAlerts(): List<UnusualSpendingAlert> {
        return alerts.values.filter { it.id !in dismissedAlerts }.toList()
    }
    
    override suspend fun getUnreadAlerts(): List<UnusualSpendingAlert> {
        return alerts.values.filter { 
            it.id !in readAlerts && it.id !in dismissedAlerts 
        }.toList()
    }
    
    override suspend fun markAlertAsRead(alertId: String) {
        readAlerts.add(alertId)
    }
    
    override suspend fun dismissAlert(alertId: String) {
        dismissedAlerts.add(alertId)
    }
    
    override suspend fun getAlertsByType(type: UnusualSpendingType): List<UnusualSpendingAlert> {
        return alerts.values.filter { 
            it.alertType == type && it.id !in dismissedAlerts 
        }.toList()
    }
}