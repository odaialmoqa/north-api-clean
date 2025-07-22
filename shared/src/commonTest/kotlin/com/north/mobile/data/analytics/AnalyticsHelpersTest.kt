package com.north.mobile.data.analytics

import com.north.mobile.domain.model.*
import kotlinx.datetime.*
import kotlin.test.*

class AnalyticsHelpersTest {
    
    private val testDateRange = DateRange(
        LocalDate(2024, 1, 1),
        LocalDate(2024, 1, 31)
    )
    
    private val previousDateRange = DateRange(
        LocalDate(2023, 12, 1),
        LocalDate(2023, 12, 31)
    )
    
    @Test
    fun `calculatePreviousPeriodComparison should calculate correct spending changes`() {
        // Given
        val currentTransactions = listOf(
            createDebitTransaction("1", Money.fromDollars(100.0), Category.GROCERIES),
            createDebitTransaction("2", Money.fromDollars(50.0), Category.GAS),
            createCreditTransaction("3", Money.fromDollars(2000.0), Category.SALARY)
        )
        
        val previousTransactions = listOf(
            createDebitTransaction("4", Money.fromDollars(80.0), Category.GROCERIES),
            createDebitTransaction("5", Money.fromDollars(40.0), Category.GAS)
        )
        
        // When
        val result = AnalyticsHelpers.calculatePreviousPeriodComparison(
            currentTransactions, previousTransactions, testDateRange, previousDateRange
        )
        
        // Then
        assertEquals(Money.fromDollars(150.0), result.totalSpentChange) // Current: 150, Previous: 120
        assertEquals(Money.fromDollars(30.0), result.totalSpentChange) // 150 - 120 = 30
        assertEquals(25.0, result.totalSpentChangePercentage) // (30/120) * 100 = 25%
        assertTrue(result.categoryChanges.isNotEmpty())
        assertTrue(result.significantChanges.isNotEmpty())
    }
    
    @Test
    fun `calculatePreviousPeriodComparison should handle zero previous spending`() {
        // Given
        val currentTransactions = listOf(
            createDebitTransaction("1", Money.fromDollars(100.0), Category.GROCERIES)
        )
        val previousTransactions = emptyList<Transaction>()
        
        // When
        val result = AnalyticsHelpers.calculatePreviousPeriodComparison(
            currentTransactions, previousTransactions, testDateRange, previousDateRange
        )
        
        // Then
        assertEquals(Money.fromDollars(100.0), result.totalSpentChange)
        assertEquals(0.0, result.totalSpentChangePercentage) // Should handle division by zero
    }
    
    @Test
    fun `calculateCategoryChanges should track changes by category`() {
        // Given
        val currentTransactions = listOf(
            createDebitTransaction("1", Money.fromDollars(150.0), Category.GROCERIES),
            createDebitTransaction("2", Money.fromDollars(80.0), Category.GAS)
        )
        
        val previousTransactions = listOf(
            createDebitTransaction("3", Money.fromDollars(100.0), Category.GROCERIES),
            createDebitTransaction("4", Money.fromDollars(60.0), Category.GAS),
            createDebitTransaction("5", Money.fromDollars(30.0), Category.RESTAURANTS)
        )
        
        // When
        val result = AnalyticsHelpers.calculateCategoryChanges(currentTransactions, previousTransactions)
        
        // Then
        assertEquals(Money.fromDollars(50.0), result[Category.GROCERIES.id]) // 150 - 100
        assertEquals(Money.fromDollars(20.0), result[Category.GAS.id]) // 80 - 60
        assertEquals(Money.fromDollars(-30.0), result[Category.RESTAURANTS.id]) // 0 - 30
    }
    
    @Test
    fun `identifySignificantChanges should identify large changes`() {
        // Given
        val categoryChanges = mapOf(
            Category.GROCERIES.id to Money.fromDollars(75.0), // $75 increase - significant
            Category.GAS.id to Money.fromDollars(25.0), // $25 increase - not significant
            Category.RESTAURANTS.id to Money.fromDollars(-60.0) // $60 decrease - significant
        )
        
        // When
        val result = AnalyticsHelpers.identifySignificantChanges(categoryChanges)
        
        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.contains("increased") && it.contains(Category.GROCERIES.id) })
        assertTrue(result.any { it.contains("decreased") && it.contains(Category.RESTAURANTS.id) })
        assertFalse(result.any { it.contains(Category.GAS.id) })
    }
    
    @Test
    fun `analyzeSpendingTrends should identify overall spending trends`() {
        // Given - Transactions with increasing weekly spending
        val transactions = listOf(
            createDebitTransaction("1", Money.fromDollars(100.0), Category.GROCERIES, LocalDate(2024, 1, 1)),
            createDebitTransaction("2", Money.fromDollars(120.0), Category.GROCERIES, LocalDate(2024, 1, 8)),
            createDebitTransaction("3", Money.fromDollars(140.0), Category.GROCERIES, LocalDate(2024, 1, 15)),
            createDebitTransaction("4", Money.fromDollars(160.0), Category.GROCERIES, LocalDate(2024, 1, 22))
        )
        
        // When
        val result = AnalyticsHelpers.analyzeSpendingTrends(transactions, testDateRange)
        
        // Then
        assertTrue(result.isNotEmpty())
        val overallTrend = result.find { it.category == null }
        assertNotNull(overallTrend)
        assertEquals(TrendType.SPENDING, overallTrend.trendType)
        assertTrue(overallTrend.confidence > 0.0f)
    }
    
    @Test
    fun `groupTransactionsByWeek should group transactions correctly`() {
        // Given
        val transactions = listOf(
            createDebitTransaction("1", Money.fromDollars(100.0), Category.GROCERIES, LocalDate(2024, 1, 1)),
            createDebitTransaction("2", Money.fromDollars(50.0), Category.GROCERIES, LocalDate(2024, 1, 2)),
            createDebitTransaction("3", Money.fromDollars(75.0), Category.GROCERIES, LocalDate(2024, 1, 8)),
            createDebitTransaction("4", Money.fromDollars(25.0), Category.GROCERIES, LocalDate(2024, 1, 9))
        )
        
        // When
        val result = AnalyticsHelpers.groupTransactionsByWeek(transactions)
        
        // Then
        assertTrue(result.isNotEmpty())
        // Should have at least 2 different weeks
        assertTrue(result.size >= 2)
        // All amounts should be positive
        result.values.forEach { amount ->
            assertTrue(amount.isPositive)
        }
    }
    
    @Test
    fun `calculateTrendDirection should identify increasing trend`() {
        // Given - Increasing amounts
        val amounts = listOf(
            Money.fromDollars(100.0),
            Money.fromDollars(120.0),
            Money.fromDollars(140.0)
        )
        
        // When
        val result = AnalyticsHelpers.calculateTrendDirection(amounts)
        
        // Then
        assertEquals(TrendDirection.INCREASING, result)
    }
    
    @Test
    fun `calculateTrendDirection should identify decreasing trend`() {
        // Given - Decreasing amounts
        val amounts = listOf(
            Money.fromDollars(150.0),
            Money.fromDollars(120.0),
            Money.fromDollars(90.0)
        )
        
        // When
        val result = AnalyticsHelpers.calculateTrendDirection(amounts)
        
        // Then
        assertEquals(TrendDirection.DECREASING, result)
    }
    
    @Test
    fun `calculateTrendDirection should identify stable trend`() {
        // Given - Stable amounts (small change)
        val amounts = listOf(
            Money.fromDollars(100.0),
            Money.fromDollars(105.0),
            Money.fromDollars(102.0)
        )
        
        // When
        val result = AnalyticsHelpers.calculateTrendDirection(amounts)
        
        // Then
        assertEquals(TrendDirection.STABLE, result)
    }
    
    @Test
    fun `calculateTrendMagnitude should calculate correct magnitude`() {
        // Given - 50% increase
        val amounts = listOf(
            Money.fromDollars(100.0),
            Money.fromDollars(150.0)
        )
        
        // When
        val result = AnalyticsHelpers.calculateTrendMagnitude(amounts)
        
        // Then
        assertEquals(50.0, result)
    }
    
    @Test
    fun `calculateTrendMagnitude should handle zero starting amount`() {
        // Given - Starting from zero
        val amounts = listOf(
            Money.fromDollars(0.0),
            Money.fromDollars(100.0)
        )
        
        // When
        val result = AnalyticsHelpers.calculateTrendMagnitude(amounts)
        
        // Then
        assertEquals(0.0, result) // Should handle division by zero
    }
    
    @Test
    fun `generateSpendingInsights should identify high spending categories`() {
        // Given
        val transactions = listOf(
            createDebitTransaction("1", Money.fromDollars(800.0), Category.RESTAURANTS),
            createDebitTransaction("2", Money.fromDollars(200.0), Category.GROCERIES)
        )
        
        val categoryBreakdown = listOf(
            CategorySpending(
                category = Category.RESTAURANTS,
                totalAmount = Money.fromDollars(800.0),
                transactionCount = 1,
                averageAmount = Money.fromDollars(800.0),
                percentageOfTotal = 80.0,
                trend = TrendDirection.STABLE,
                comparedToPrevious = null
            ),
            CategorySpending(
                category = Category.GROCERIES,
                totalAmount = Money.fromDollars(200.0),
                transactionCount = 1,
                averageAmount = Money.fromDollars(200.0),
                percentageOfTotal = 20.0,
                trend = TrendDirection.STABLE,
                comparedToPrevious = null
            )
        )
        
        // When
        val result = AnalyticsHelpers.generateSpendingInsights(transactions, categoryBreakdown)
        
        // Then
        assertTrue(result.isNotEmpty())
        val highSpendingInsight = result.find { 
            it.type == InsightType.SPENDING_PATTERN && it.title.contains("High spending")
        }
        assertNotNull(highSpendingInsight)
        assertEquals(Category.RESTAURANTS, highSpendingInsight.category)
        assertEquals(InsightImpact.MEDIUM, highSpendingInsight.impact)
        assertTrue(highSpendingInsight.actionableRecommendations.isNotEmpty())
        assertNotNull(highSpendingInsight.potentialSavings)
    }
    
    @Test
    fun `generateSpendingInsights should identify increasing trends`() {
        // Given
        val transactions = listOf(
            createDebitTransaction("1", Money.fromDollars(500.0), Category.GROCERIES)
        )
        
        val categoryBreakdown = listOf(
            CategorySpending(
                category = Category.GROCERIES,
                totalAmount = Money.fromDollars(500.0),
                transactionCount = 1,
                averageAmount = Money.fromDollars(500.0),
                percentageOfTotal = 100.0,
                trend = TrendDirection.INCREASING,
                comparedToPrevious = Money.fromDollars(100.0)
            )
        )
        
        // When
        val result = AnalyticsHelpers.generateSpendingInsights(transactions, categoryBreakdown)
        
        // Then
        val increasingTrendInsight = result.find { 
            it.type == InsightType.SPENDING_PATTERN && it.title.contains("Increasing spending")
        }
        assertNotNull(increasingTrendInsight)
        assertEquals(Category.GROCERIES, increasingTrendInsight.category)
        assertEquals(InsightImpact.MEDIUM, increasingTrendInsight.impact)
    }
    
    @Test
    fun `generateSpendingInsights should identify positive behaviors`() {
        // Given
        val transactions = listOf(
            createDebitTransaction("1", Money.fromDollars(300.0), Category.RESTAURANTS)
        )
        
        val categoryBreakdown = listOf(
            CategorySpending(
                category = Category.RESTAURANTS,
                totalAmount = Money.fromDollars(300.0),
                transactionCount = 1,
                averageAmount = Money.fromDollars(300.0),
                percentageOfTotal = 100.0,
                trend = TrendDirection.DECREASING,
                comparedToPrevious = Money.fromDollars(-100.0)
            )
        )
        
        // When
        val result = AnalyticsHelpers.generateSpendingInsights(transactions, categoryBreakdown)
        
        // Then
        val positiveInsight = result.find { 
            it.type == InsightType.POSITIVE_BEHAVIOR
        }
        assertNotNull(positiveInsight)
        assertEquals(Category.RESTAURANTS, positiveInsight.category)
        assertEquals(InsightImpact.LOW, positiveInsight.impact)
        assertNotNull(positiveInsight.potentialSavings)
    }
    
    @Test
    fun `calculateAssetBreakdown should categorize assets correctly`() {
        // Given
        val assets = listOf(
            createAccount("1", AccountType.CHECKING, Money.fromDollars(5000.0)),
            createAccount("2", AccountType.SAVINGS, Money.fromDollars(15000.0)),
            createAccount("3", AccountType.INVESTMENT, Money.fromDollars(30000.0))
        )
        
        // When
        val result = AnalyticsHelpers.calculateAssetBreakdown(assets)
        
        // Then
        assertEquals(3, result.size)
        
        val checkingCategory = result.find { it.type == AssetType.CHECKING }
        assertNotNull(checkingCategory)
        assertEquals(Money.fromDollars(5000.0), checkingCategory.amount)
        assertEquals(10.0, checkingCategory.percentageOfTotal) // 5000/50000 * 100
        
        val savingsCategory = result.find { it.type == AssetType.SAVINGS }
        assertNotNull(savingsCategory)
        assertEquals(Money.fromDollars(15000.0), savingsCategory.amount)
        assertEquals(30.0, savingsCategory.percentageOfTotal)
        
        val investmentCategory = result.find { it.type == AssetType.INVESTMENT }
        assertNotNull(investmentCategory)
        assertEquals(Money.fromDollars(30000.0), investmentCategory.amount)
        assertEquals(60.0, investmentCategory.percentageOfTotal)
    }
    
    @Test
    fun `calculateLiabilityBreakdown should categorize liabilities correctly`() {
        // Given
        val liabilities = listOf(
            createAccount("1", AccountType.CREDIT_CARD, Money.fromDollars(-2000.0)),
            createAccount("2", AccountType.MORTGAGE, Money.fromDollars(-200000.0)),
            createAccount("3", AccountType.LOAN, Money.fromDollars(-10000.0))
        )
        
        // When
        val result = AnalyticsHelpers.calculateLiabilityBreakdown(liabilities)
        
        // Then
        assertEquals(3, result.size)
        
        val creditCardCategory = result.find { it.type == LiabilityType.CREDIT_CARD }
        assertNotNull(creditCardCategory)
        assertEquals(Money.fromDollars(2000.0), creditCardCategory.amount) // Should be positive
        
        val mortgageCategory = result.find { it.type == LiabilityType.MORTGAGE }
        assertNotNull(mortgageCategory)
        assertEquals(Money.fromDollars(200000.0), mortgageCategory.amount)
        
        val loanCategory = result.find { it.type == LiabilityType.LOAN }
        assertNotNull(loanCategory)
        assertEquals(Money.fromDollars(10000.0), loanCategory.amount)
    }
    
    @Test
    fun `determineTrend should handle null change`() {
        // When
        val result = AnalyticsHelpers.determineTrend(null)
        
        // Then
        assertEquals(TrendDirection.STABLE, result)
    }
    
    @Test
    fun `determineTrend should identify positive and negative changes`() {
        // When/Then
        assertEquals(TrendDirection.INCREASING, AnalyticsHelpers.determineTrend(Money.fromDollars(100.0)))
        assertEquals(TrendDirection.DECREASING, AnalyticsHelpers.determineTrend(Money.fromDollars(-100.0)))
        assertEquals(TrendDirection.STABLE, AnalyticsHelpers.determineTrend(Money.zero()))
    }
    
    @Test
    fun `calculateNetWorthProjections should create reasonable projections`() {
        // Given
        val currentNetWorth = Money.fromDollars(50000.0)
        val monthlyChange = Money.fromDollars(1000.0)
        val months = 6
        
        // When
        val result = AnalyticsHelpers.calculateNetWorthProjections(currentNetWorth, monthlyChange, months)
        
        // Then
        assertEquals(months, result.size)
        
        // First projection should be current + 1 month
        assertEquals(Money.fromDollars(51000.0), result[0].projectedNetWorth)
        
        // Last projection should be current + 6 months
        assertEquals(Money.fromDollars(56000.0), result[5].projectedNetWorth)
        
        // Confidence should decrease over time
        assertTrue(result[0].confidence > result[5].confidence)
        
        // All projections should have positive confidence
        result.forEach { projection ->
            assertTrue(projection.confidence > 0.0f)
        }
    }
    
    @Test
    fun `calculateNetWorthProjections should handle negative monthly change`() {
        // Given
        val currentNetWorth = Money.fromDollars(50000.0)
        val monthlyChange = Money.fromDollars(-500.0)
        val months = 3
        
        // When
        val result = AnalyticsHelpers.calculateNetWorthProjections(currentNetWorth, monthlyChange, months)
        
        // Then
        assertEquals(Money.fromDollars(49500.0), result[0].projectedNetWorth)
        assertEquals(Money.fromDollars(49000.0), result[1].projectedNetWorth)
        assertEquals(Money.fromDollars(48500.0), result[2].projectedNetWorth)
    }
    
    @Test
    fun `getCurrentMonthRange should return current month range`() {
        // When
        val result = AnalyticsHelpers.getCurrentMonthRange()
        
        // Then
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        assertEquals(today.year, result.startDate.year)
        assertEquals(today.month, result.startDate.month)
        assertEquals(1, result.startDate.dayOfMonth)
        
        // End date should be last day of the month
        assertTrue(result.endDate.dayOfMonth >= 28) // At least 28 days in any month
        assertEquals(today.month, result.endDate.month)
    }
    
    // Helper methods
    
    private fun createDebitTransaction(
        id: String,
        amount: Money,
        category: Category,
        date: LocalDate = LocalDate(2024, 1, 15)
    ): Transaction {
        return Transaction(
            id = id,
            accountId = "account1",
            amount = -amount, // Negative for debit
            description = "Test transaction",
            category = category,
            date = date,
            merchantName = "Test Merchant",
            subcategory = null,
            isRecurring = false,
            isVerified = true,
            notes = null
        )
    }
    
    private fun createCreditTransaction(
        id: String,
        amount: Money,
        category: Category,
        date: LocalDate = LocalDate(2024, 1, 15)
    ): Transaction {
        return Transaction(
            id = id,
            accountId = "account1",
            amount = amount, // Positive for credit
            description = "Test transaction",
            category = category,
            date = date,
            merchantName = "Test Merchant",
            subcategory = null,
            isRecurring = false,
            isVerified = true,
            notes = null
        )
    }
    
    private fun createAccount(
        id: String,
        type: AccountType,
        balance: Money
    ): Account {
        return Account(
            id = id,
            institutionId = "inst1",
            institutionName = "Test Bank",
            accountType = type,
            balance = balance,
            lastUpdated = Clock.System.now()
        )
    }
}