package com.north.mobile.data.analytics

import com.north.mobile.domain.model.Category
import com.north.mobile.domain.model.Money
import com.north.mobile.domain.model.Transaction
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TransactionCategorizationServiceTest {
    
    private fun createTestService(): TransactionCategorizationServiceImpl {
        val trainingDataProvider = InMemoryTrainingDataProvider()
        val userFeedbackRepository = InMemoryUserFeedbackRepository()
        val transactionHistoryProvider = InMemoryTransactionHistoryProvider()
        
        return TransactionCategorizationServiceImpl(
            trainingDataProvider,
            userFeedbackRepository,
            transactionHistoryProvider
        )
    }
    
    private fun createTestTransaction(
        id: String = "test-tx-1",
        description: String = "Test Transaction",
        merchantName: String? = null,
        amount: Double = -50.0,
        date: LocalDate = LocalDate(2024, 6, 15)
    ): Transaction {
        return Transaction(
            id = id,
            accountId = "test-account",
            amount = Money.fromDollars(amount),
            description = description,
            category = Category.UNCATEGORIZED,
            date = date,
            merchantName = merchantName
        )
    }
    
    @Test
    fun testCategorizationOfTimHortonsTransaction() = runBlocking {
        val service = createTestService()
        val transaction = createTestTransaction(
            description = "TIM HORTONS #1234",
            merchantName = "Tim Hortons",
            amount = -4.50
        )
        
        val result = service.categorizeTransaction(transaction)
        
        assertNotNull(result)
        assertEquals(transaction.id, result.transactionId)
        assertEquals(Category.RESTAURANTS.id, result.suggestedCategory.id)
        assertTrue(result.confidence > 0.8f)
        assertNotNull(result.reasoning)
    }
    
    @Test
    fun testCategorizationOfGroceryTransaction() = runBlocking {
        val service = createTestService()
        val transaction = createTestTransaction(
            description = "LOBLAWS SUPERMARKET",
            merchantName = "Loblaws",
            amount = -85.30
        )
        
        val result = service.categorizeTransaction(transaction)
        
        assertNotNull(result)
        assertEquals(Category.GROCERIES.id, result.suggestedCategory.id)
        assertTrue(result.confidence > 0.8f)
    }
    
    @Test
    fun testCategorizationOfGasStationTransaction() = runBlocking {
        val service = createTestService()
        val transaction = createTestTransaction(
            description = "PETRO-CANADA GAS STATION",
            merchantName = "Petro-Canada",
            amount = -55.00
        )
        
        val result = service.categorizeTransaction(transaction)
        
        assertNotNull(result)
        assertEquals(Category.GAS.id, result.suggestedCategory.id)
        assertTrue(result.confidence > 0.7f)
    }
    
    @Test
    fun testCategorizationOfUtilityBill() = runBlocking {
        val service = createTestService()
        val transaction = createTestTransaction(
            description = "HYDRO ONE PAYMENT",
            merchantName = "Hydro One",
            amount = -125.50
        )
        
        val result = service.categorizeTransaction(transaction)
        
        assertNotNull(result)
        assertTrue(
            result.suggestedCategory.id == Category.HYDRO.id || 
            result.suggestedCategory.id == Category.BILLS.id
        )
        assertTrue(result.confidence > 0.7f)
    }
    
    @Test
    fun testCategorizationOfIncomeTransaction() = runBlocking {
        val service = createTestService()
        val transaction = createTestTransaction(
            description = "PAYROLL DEPOSIT",
            amount = 2500.0 // Positive amount for income
        )
        
        val result = service.categorizeTransaction(transaction)
        
        assertNotNull(result)
        assertEquals(Category.SALARY.id, result.suggestedCategory.id)
        assertTrue(result.confidence > 0.8f)
    }
    
    @Test
    fun testBatchCategorization() = runBlocking {
        val service = createTestService()
        val transactions = listOf(
            createTestTransaction("tx1", "TIM HORTONS", "Tim Hortons", -4.50),
            createTestTransaction("tx2", "LOBLAWS", "Loblaws", -85.30),
            createTestTransaction("tx3", "ESSO STATION", "Esso", -48.75)
        )
        
        val results = service.categorizeTransactions(transactions)
        
        assertEquals(3, results.size)
        assertEquals("tx1", results[0].transactionId)
        assertEquals("tx2", results[1].transactionId)
        assertEquals("tx3", results[2].transactionId)
        
        // Verify categories
        assertEquals(Category.RESTAURANTS.id, results[0].suggestedCategory.id)
        assertEquals(Category.GROCERIES.id, results[1].suggestedCategory.id)
        assertEquals(Category.GAS.id, results[2].suggestedCategory.id)
    }
    
    @Test
    fun testUserFeedbackLearning() = runBlocking {
        val service = createTestService()
        val transaction = createTestTransaction(
            id = "feedback-tx",
            description = "UNKNOWN MERCHANT",
            amount = -25.0
        )
        
        // First categorization
        val initialResult = service.categorizeTransaction(transaction)
        val initialCategory = initialResult.suggestedCategory
        
        // Provide user feedback
        service.provideFeedback("feedback-tx", Category.ENTERTAINMENT, 1.0f)
        
        // Get stats to verify feedback was recorded
        val stats = service.getCategorizationStats()
        assertEquals(1, stats.userFeedbackCount)
    }
    
    @Test
    fun testUnusualSpendingDetection() = runBlocking {
        val service = createTestService()
        
        // Create a mix of normal and unusual transactions
        val transactions = listOf(
            // Normal grocery transactions
            createTestTransaction("normal1", "LOBLAWS", "Loblaws", -85.0),
            createTestTransaction("normal2", "METRO", "Metro", -92.0),
            createTestTransaction("normal3", "SOBEYS", "Sobeys", -78.0),
            
            // Unusual high amount grocery transaction
            createTestTransaction("unusual1", "LOBLAWS", "Loblaws", -350.0),
            
            // Potential duplicate transactions
            createTestTransaction("dup1", "TIM HORTONS", "Tim Hortons", -4.50, LocalDate(2024, 6, 15)),
            createTestTransaction("dup2", "TIM HORTONS", "Tim Hortons", -4.50, LocalDate(2024, 6, 15)),
            
            // New merchant
            createTestTransaction("new1", "UNKNOWN RESTAURANT", "Unknown Restaurant", -45.0)
        )
        
        val alerts = service.detectUnusualSpending(transactions)
        
        assertTrue(alerts.isNotEmpty())
        
        // Should detect amount anomaly
        val amountAnomalies = alerts.filter { it.alertType == UnusualSpendingType.AMOUNT_ANOMALY }
        assertTrue(amountAnomalies.isNotEmpty())
        
        // Should detect potential duplicates
        val duplicateAlerts = alerts.filter { it.alertType == UnusualSpendingType.DUPLICATE_SUSPECTED }
        assertTrue(duplicateAlerts.isNotEmpty())
        
        // Should detect new merchant
        val newMerchantAlerts = alerts.filter { it.alertType == UnusualSpendingType.NEW_MERCHANT }
        assertTrue(newMerchantAlerts.isNotEmpty())
    }
    
    @Test
    fun testCategorizationStats() = runBlocking {
        val service = createTestService()
        
        // Perform some categorizations
        val transactions = listOf(
            createTestTransaction("stats1", "TIM HORTONS", "Tim Hortons", -4.50),
            createTestTransaction("stats2", "LOBLAWS", "Loblaws", -85.30),
            createTestTransaction("stats3", "ESSO", "Esso", -48.75)
        )
        
        transactions.forEach { service.categorizeTransaction(it) }
        
        // Provide some feedback
        service.provideFeedback("stats1", Category.RESTAURANTS, 1.0f)
        service.provideFeedback("stats2", Category.GROCERIES, 0.9f)
        
        val stats = service.getCategorizationStats()
        
        assertEquals(3, stats.totalTransactionsCategorized)
        assertEquals(2, stats.userFeedbackCount)
        assertTrue(stats.averageConfidence > 0.0f)
        assertTrue(stats.categoryDistribution.isNotEmpty())
    }
    
    @Test
    fun testAlternativeCategorySuggestions() = runBlocking {
        val service = createTestService()
        val transaction = createTestTransaction(
            description = "COFFEE SHOP PURCHASE",
            merchantName = "Local Coffee Shop",
            amount = -6.50
        )
        
        val result = service.categorizeTransaction(transaction)
        
        assertNotNull(result)
        assertTrue(result.alternativeCategories.isNotEmpty())
        
        // Alternative suggestions should have different categories
        val categoryIds = result.alternativeCategories.map { it.category.id }.toSet()
        assertTrue(categoryIds.size == result.alternativeCategories.size) // All unique
        
        // All alternatives should have confidence scores
        result.alternativeCategories.forEach { alternative ->
            assertTrue(alternative.confidence > 0.0f)
            assertTrue(alternative.confidence <= 1.0f)
        }
    }
    
    @Test
    fun testCanadianMerchantPatternMatching() = runBlocking {
        val service = createTestService()
        
        val canadianMerchants = listOf(
            "CANADIAN TIRE" to Category.SHOPPING,
            "SHOPPERS DRUG MART" to Category.SHOPPING,
            "ROGERS COMMUNICATIONS" to Category.INTERNET,
            "BELL CANADA" to Category.INTERNET,
            "TORONTO HYDRO" to Category.HYDRO
        )
        
        canadianMerchants.forEach { (merchantName, expectedCategory) ->
            val transaction = createTestTransaction(
                description = merchantName,
                merchantName = merchantName,
                amount = -50.0
            )
            
            val result = service.categorizeTransaction(transaction)
            
            // Should match the expected category or a parent category
            assertTrue(
                result.suggestedCategory.id == expectedCategory.id ||
                result.suggestedCategory.parentCategoryId == expectedCategory.id ||
                (expectedCategory.parentCategoryId != null && 
                 result.suggestedCategory.id == expectedCategory.parentCategoryId)
            )
            assertTrue(result.confidence > 0.7f)
        }
    }
    
    @Test
    fun testModelRetraining() = runBlocking {
        val service = createTestService()
        
        // Get initial stats
        val initialStats = service.getCategorizationStats()
        
        // Retrain models
        service.retrainModels()
        
        // Stats should still be accessible after retraining
        val newStats = service.getCategorizationStats()
        assertNotNull(newStats)
    }
}