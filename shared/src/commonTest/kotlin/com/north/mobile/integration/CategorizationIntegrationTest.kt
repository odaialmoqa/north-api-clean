package com.north.mobile.integration

import com.north.mobile.data.analytics.*
import com.north.mobile.domain.model.Category
import com.north.mobile.domain.model.Money
import com.north.mobile.domain.model.Transaction
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test for the complete transaction categorization system
 */
class CategorizationIntegrationTest {
    
    private fun createIntegratedSystem(): Triple<
        TransactionCategorizationServiceImpl,
        CategoryManagementServiceImpl,
        InMemoryTransactionHistoryProvider
    > {
        val trainingDataProvider = InMemoryTrainingDataProvider()
        val userFeedbackRepository = InMemoryUserFeedbackRepository()
        val transactionHistoryProvider = InMemoryTransactionHistoryProvider()
        val categoryRepository = InMemoryCategoryRepository()
        
        val categorizationService = TransactionCategorizationServiceImpl(
            trainingDataProvider,
            userFeedbackRepository,
            transactionHistoryProvider
        )
        
        val categoryManagementService = CategoryManagementServiceImpl(
            categoryRepository,
            transactionHistoryProvider
        )
        
        return Triple(categorizationService, categoryManagementService, transactionHistoryProvider)
    }
    
    private fun createCanadianTransactions(): List<Transaction> {
        return listOf(
            // Tim Hortons transactions
            Transaction(
                id = "tx1",
                accountId = "account1",
                amount = Money.fromDollars(-4.50),
                description = "TIM HORTONS #1234",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 6, 1),
                merchantName = "Tim Hortons"
            ),
            Transaction(
                id = "tx2",
                accountId = "account1",
                amount = Money.fromDollars(-3.25),
                description = "TIMS COFFEE",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 6, 2),
                merchantName = "Tims"
            ),
            
            // Grocery transactions
            Transaction(
                id = "tx3",
                accountId = "account1",
                amount = Money.fromDollars(-85.30),
                description = "LOBLAWS SUPERMARKET",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 6, 3),
                merchantName = "Loblaws"
            ),
            Transaction(
                id = "tx4",
                accountId = "account1",
                amount = Money.fromDollars(-67.45),
                description = "METRO GROCERY",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 6, 4),
                merchantName = "Metro"
            ),
            Transaction(
                id = "tx5",
                accountId = "account1",
                amount = Money.fromDollars(-92.15),
                description = "SOBEYS STORE",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 6, 5),
                merchantName = "Sobeys"
            ),
            
            // Gas station transactions
            Transaction(
                id = "tx6",
                accountId = "account1",
                amount = Money.fromDollars(-55.00),
                description = "PETRO-CANADA GAS",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 6, 6),
                merchantName = "Petro-Canada"
            ),
            Transaction(
                id = "tx7",
                accountId = "account1",
                amount = Money.fromDollars(-48.75),
                description = "ESSO STATION",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 6, 7),
                merchantName = "Esso"
            ),
            
            // Utility bills
            Transaction(
                id = "tx8",
                accountId = "account1",
                amount = Money.fromDollars(-125.50),
                description = "HYDRO ONE PAYMENT",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 6, 8),
                merchantName = "Hydro One"
            ),
            Transaction(
                id = "tx9",
                accountId = "account1",
                amount = Money.fromDollars(-89.99),
                description = "ROGERS COMMUNICATIONS",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 6, 9),
                merchantName = "Rogers"
            ),
            
            // Shopping
            Transaction(
                id = "tx10",
                accountId = "account1",
                amount = Money.fromDollars(-45.99),
                description = "CANADIAN TIRE STORE",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 6, 10),
                merchantName = "Canadian Tire"
            ),
            
            // Income
            Transaction(
                id = "tx11",
                accountId = "account1",
                amount = Money.fromDollars(2500.00),
                description = "PAYROLL DEPOSIT",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 6, 15),
                merchantName = null
            ),
            
            // Unusual transactions for anomaly detection
            Transaction(
                id = "tx12",
                accountId = "account1",
                amount = Money.fromDollars(-350.00), // Unusually high grocery amount
                description = "LOBLAWS SUPERMARKET",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 6, 20),
                merchantName = "Loblaws"
            ),
            
            // Potential duplicate
            Transaction(
                id = "tx13",
                accountId = "account1",
                amount = Money.fromDollars(-4.50),
                description = "TIM HORTONS #1234",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 6, 1), // Same date as tx1
                merchantName = "Tim Hortons"
            )
        )
    }
    
    @Test
    fun testCompleteCategorizationWorkflow() = runBlocking {
        val (categorizationService, categoryManagementService, transactionProvider) = createIntegratedSystem()
        val transactions = createCanadianTransactions()
        
        // Add transactions to the provider
        transactionProvider.addTransactions(transactions)
        
        // Step 1: Categorize all transactions
        val categorizationResults = categorizationService.categorizeTransactions(transactions)
        
        // Verify all transactions were categorized
        assertEquals(transactions.size, categorizationResults.size)
        
        // Verify Canadian merchant patterns are recognized
        val timHortonsResults = categorizationResults.filter { 
            it.transactionId in listOf("tx1", "tx2", "tx13") 
        }
        timHortonsResults.forEach { result ->
            assertEquals(Category.RESTAURANTS.id, result.suggestedCategory.id)
            assertTrue(result.confidence > 0.8f)
        }
        
        val groceryResults = categorizationResults.filter { 
            it.transactionId in listOf("tx3", "tx4", "tx5", "tx12") 
        }
        groceryResults.forEach { result ->
            assertEquals(Category.GROCERIES.id, result.suggestedCategory.id)
            assertTrue(result.confidence > 0.8f)
        }
        
        val gasResults = categorizationResults.filter { 
            it.transactionId in listOf("tx6", "tx7") 
        }
        gasResults.forEach { result ->
            assertEquals(Category.GAS.id, result.suggestedCategory.id)
            assertTrue(result.confidence > 0.7f)
        }
        
        // Step 2: Test user feedback learning
        categorizationService.provideFeedback("tx10", Category.SHOPPING, 1.0f)
        categorizationService.provideFeedback("tx8", Category.HYDRO, 0.9f)
        
        // Step 3: Detect unusual spending patterns
        val unusualSpendingAlerts = categorizationService.detectUnusualSpending(transactions)
        
        assertTrue(unusualSpendingAlerts.isNotEmpty())
        
        // Should detect the unusually high grocery transaction
        val amountAnomalies = unusualSpendingAlerts.filter { 
            it.alertType == UnusualSpendingType.AMOUNT_ANOMALY 
        }
        assertTrue(amountAnomalies.any { it.transactionId == "tx12" })
        
        // Should detect potential duplicate Tim Hortons transactions
        val duplicateAlerts = unusualSpendingAlerts.filter { 
            it.alertType == UnusualSpendingType.DUPLICATE_SUSPECTED 
        }
        assertTrue(duplicateAlerts.isNotEmpty())
        
        // Step 4: Test category management
        val allCategories = categoryManagementService.getAllCategories()
        assertTrue(allCategories.isNotEmpty())
        
        // Create a custom category
        val customCategoryResult = categoryManagementService.createCustomCategory(
            name = "Coffee Shops",
            parentCategoryId = Category.RESTAURANTS.id,
            color = "#8B4513"
        )
        assertTrue(customCategoryResult is CategoryManagementResult.Success)
        
        // Get category usage statistics
        val usageStats = categoryManagementService.getCategoryUsageStats()
        assertTrue(usageStats.isNotEmpty())
        
        // Verify grocery category has high usage
        val groceryStats = usageStats.find { it.category.id == Category.GROCERIES.id }
        assertNotNull(groceryStats)
        assertTrue(groceryStats.transactionCount >= 4) // tx3, tx4, tx5, tx12
        
        // Step 5: Get categorization statistics
        val categorizationStats = categorizationService.getCategorizationStats()
        assertEquals(transactions.size, categorizationStats.totalTransactionsCategorized)
        assertEquals(2, categorizationStats.userFeedbackCount) // tx10 and tx8 feedback
        assertTrue(categorizationStats.averageConfidence > 0.0f)
        
        // Step 6: Test category suggestions
        val categorySuggestions = categoryManagementService.suggestCategoryImprovements()
        // Should suggest creating subcategories for heavily used categories
        assertTrue(categorySuggestions.any { 
            it.type == CategorySuggestionType.CREATE_SUBCATEGORY 
        })
    }
    
    @Test
    fun testCanadianSpecificCategorization() = runBlocking {
        val (categorizationService, _, transactionProvider) = createIntegratedSystem()
        
        // Test Canadian-specific merchants and patterns
        val canadianTransactions = listOf(
            Transaction(
                id = "cdn1",
                accountId = "account1",
                amount = Money.fromDollars(-23.45),
                description = "SHOPPERS DRUG MART",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 6, 1),
                merchantName = "Shoppers Drug Mart"
            ),
            Transaction(
                id = "cdn2",
                accountId = "account1",
                amount = Money.fromDollars(-95.00),
                description = "BELL CANADA",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 6, 2),
                merchantName = "Bell Canada"
            ),
            Transaction(
                id = "cdn3",
                accountId = "account1",
                amount = Money.fromDollars(-67.50),
                description = "TELUS MOBILITY",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 6, 3),
                merchantName = "Telus"
            )
        )
        
        transactionProvider.addTransactions(canadianTransactions)
        
        val results = categorizationService.categorizeTransactions(canadianTransactions)
        
        // Verify Canadian merchant categorization
        val shoppersResult = results.find { it.transactionId == "cdn1" }
        assertNotNull(shoppersResult)
        assertEquals(Category.SHOPPING.id, shoppersResult.suggestedCategory.id)
        
        val bellResult = results.find { it.transactionId == "cdn2" }
        assertNotNull(bellResult)
        assertEquals(Category.INTERNET.id, bellResult.suggestedCategory.id)
        
        val telusResult = results.find { it.transactionId == "cdn3" }
        assertNotNull(telusResult)
        assertEquals(Category.INTERNET.id, telusResult.suggestedCategory.id)
        
        // All should have high confidence due to Canadian merchant patterns
        results.forEach { result ->
            assertTrue(result.confidence > 0.7f)
        }
    }
    
    @Test
    fun testAnomalyDetectionAccuracy() = runBlocking {
        val (categorizationService, _, transactionProvider) = createIntegratedSystem()
        
        // Create transactions with known anomalies
        val testTransactions = listOf(
            // Normal grocery spending pattern
            Transaction("normal1", "account1", Money.fromDollars(-80.0), "LOBLAWS", Category.GROCERIES, LocalDate(2024, 6, 1), "Loblaws"),
            Transaction("normal2", "account1", Money.fromDollars(-85.0), "METRO", Category.GROCERIES, LocalDate(2024, 6, 2), "Metro"),
            Transaction("normal3", "account1", Money.fromDollars(-90.0), "SOBEYS", Category.GROCERIES, LocalDate(2024, 6, 3), "Sobeys"),
            
            // Anomaly: Unusually high grocery amount
            Transaction("anomaly1", "account1", Money.fromDollars(-400.0), "LOBLAWS", Category.GROCERIES, LocalDate(2024, 6, 4), "Loblaws"),
            
            // Anomaly: Multiple transactions same day/merchant
            Transaction("freq1", "account1", Money.fromDollars(-25.0), "TIM HORTONS", Category.RESTAURANTS, LocalDate(2024, 6, 5), "Tim Hortons"),
            Transaction("freq2", "account1", Money.fromDollars(-15.0), "TIM HORTONS", Category.RESTAURANTS, LocalDate(2024, 6, 5), "Tim Hortons"),
            Transaction("freq3", "account1", Money.fromDollars(-8.0), "TIM HORTONS", Category.RESTAURANTS, LocalDate(2024, 6, 5), "Tim Hortons"),
            
            // Anomaly: Exact duplicate
            Transaction("dup1", "account1", Money.fromDollars(-55.0), "ESSO", Category.GAS, LocalDate(2024, 6, 6), "Esso"),
            Transaction("dup2", "account1", Money.fromDollars(-55.0), "ESSO", Category.GAS, LocalDate(2024, 6, 6), "Esso")
        )
        
        transactionProvider.addTransactions(testTransactions)
        
        val alerts = categorizationService.detectUnusualSpending(testTransactions)
        
        // Should detect amount anomaly
        val amountAnomalies = alerts.filter { it.alertType == UnusualSpendingType.AMOUNT_ANOMALY }
        assertTrue(amountAnomalies.any { it.transactionId == "anomaly1" })
        
        // Should detect frequency anomaly
        val frequencyAnomalies = alerts.filter { it.alertType == UnusualSpendingType.FREQUENCY_ANOMALY }
        assertTrue(frequencyAnomalies.isNotEmpty())
        
        // Should detect duplicate
        val duplicateAnomalies = alerts.filter { it.alertType == UnusualSpendingType.DUPLICATE_SUSPECTED }
        assertTrue(duplicateAnomalies.isNotEmpty())
        
        // Verify alert severity is appropriate
        val highSeverityAlerts = alerts.filter { it.severity == AlertSeverity.HIGH }
        assertTrue(highSeverityAlerts.isNotEmpty())
    }
    
    @Test
    fun testModelLearningAndImprovement() = runBlocking {
        val (categorizationService, _, transactionProvider) = createIntegratedSystem()
        
        // Create a transaction that might be ambiguous
        val ambiguousTransaction = Transaction(
            id = "ambiguous1",
            accountId = "account1",
            amount = Money.fromDollars(-25.0),
            description = "UNKNOWN MERCHANT PURCHASE",
            category = Category.UNCATEGORIZED,
            date = LocalDate(2024, 6, 1),
            merchantName = "Unknown Merchant"
        )
        
        transactionProvider.addTransaction(ambiguousTransaction)
        
        // Initial categorization
        val initialResult = categorizationService.categorizeTransaction(ambiguousTransaction)
        val initialCategory = initialResult.suggestedCategory
        
        // Provide user feedback
        categorizationService.provideFeedback("ambiguous1", Category.ENTERTAINMENT, 1.0f)
        
        // Verify feedback was recorded
        val stats = categorizationService.getCategorizationStats()
        assertTrue(stats.userFeedbackCount > 0)
        
        // Test model retraining
        categorizationService.retrainModels()
        
        // Verify system still works after retraining
        val postRetrainingResult = categorizationService.categorizeTransaction(ambiguousTransaction)
        assertNotNull(postRetrainingResult)
        assertTrue(postRetrainingResult.confidence > 0.0f)
    }
}