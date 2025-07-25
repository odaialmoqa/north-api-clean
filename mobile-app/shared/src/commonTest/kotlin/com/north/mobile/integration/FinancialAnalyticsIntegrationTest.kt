package com.north.mobile.integration

import com.north.mobile.data.analytics.*
import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

class FinancialAnalyticsIntegrationTest {
    
    @Test
    fun `complete financial analytics workflow should work end-to-end`() = runTest {
        // Given - A user with realistic financial data
        val userId = "integration_user"
        val mockTransactionRepo = MockTransactionRepository()
        val mockAccountRepo = MockAccountRepository()
        val mockUserRepo = MockUserRepository()
        
        val analyticsService = FinancialAnalyticsServiceImpl(
            mockTransactionRepo,
            mockAccountRepo,
            mockUserRepo
        )
        
        // Set up realistic user data
        val user = User(
            id = userId,
            email = "john.doe@example.com",
            profile = UserProfile(
                firstName = "John",
                lastName = "Doe",
                postalCode = "M5V 3A8"
            ),
            preferences = UserPreferences(currency = Currency.CAD),
            gamificationData = GamificationProfile(
                level = 5,
                totalPoints = 1250,
                currentStreaks = emptyList(),
                achievements = emptyList(),
                lastActivity = Clock.System.now()
            )
        )
        mockUserRepo.setUser(user)
        
        // Set up realistic accounts
        val accounts = listOf(
            Account(
                id = "chequing_001",
                institutionId = "rbc",
                institutionName = "RBC Royal Bank",
                accountType = AccountType.CHECKING,
                balance = Money.fromDollars(2450.75),
                lastUpdated = Clock.System.now()
            ),
            Account(
                id = "savings_001",
                institutionId = "tangerine",
                institutionName = "Tangerine",
                accountType = AccountType.SAVINGS,
                balance = Money.fromDollars(15800.00),
                lastUpdated = Clock.System.now()
            ),
            Account(
                id = "credit_001",
                institutionId = "rbc",
                institutionName = "RBC Royal Bank",
                accountType = AccountType.CREDIT_CARD,
                balance = Money.fromDollars(-1250.50),
                lastUpdated = Clock.System.now()
            )
        )
        mockAccountRepo.setAccounts(accounts)
        
        // Set up realistic transactions for the current month
        val currentMonth = AnalyticsHelpers.getCurrentMonthRange()
        val transactions = createRealisticTransactions(currentMonth)
        mockTransactionRepo.setTransactions(transactions)
        
        // When - Generate comprehensive financial analysis
        
        // 1. Spending Analysis
        val spendingResult = analyticsService.generateSpendingAnalysis(userId, currentMonth, true)
        assertTrue(spendingResult.isSuccess, "Spending analysis should succeed")
        val spendingAnalysis = spendingResult.getOrThrow()
        
        // 2. Net Worth Calculation
        val netWorthResult = analyticsService.calculateNetWorth(userId)
        assertTrue(netWorthResult.isSuccess, "Net worth calculation should succeed")
        val netWorth = netWorthResult.getOrThrow()
        
        // 3. Budget Analysis
        val budgetResult = analyticsService.analyzeBudgetPerformance(userId, currentMonth)
        assertTrue(budgetResult.isSuccess, "Budget analysis should succeed")
        val budgetAnalysis = budgetResult.getOrThrow()
        
        // 4. Canadian Tax Analysis
        val taxResult = analyticsService.analyzeCanadianTaxes(userId, 2024)
        assertTrue(taxResult.isSuccess, "Tax analysis should succeed")
        val taxAnalysis = taxResult.getOrThrow()
        
        // 5. Personalized Recommendations
        val recommendationsResult = analyticsService.generatePersonalizedRecommendations(userId)
        assertTrue(recommendationsResult.isSuccess, "Recommendations should succeed")
        val recommendations = recommendationsResult.getOrThrow()
        
        // Then - Verify comprehensive analysis results
        
        // Verify spending analysis
        assertTrue(spendingAnalysis.totalSpent.isPositive, "Should have positive spending")
        assertTrue(spendingAnalysis.totalIncome.isPositive, "Should have positive income")
        assertTrue(spendingAnalysis.categoryBreakdown.isNotEmpty(), "Should have category breakdown")
        assertTrue(spendingAnalysis.insights.isNotEmpty(), "Should have spending insights")
        
        // Verify net worth calculation
        assertEquals(Money.fromDollars(18250.25), netWorth.totalAssets, "Assets should match account balances")
        assertEquals(Money.fromDollars(1250.50), netWorth.totalLiabilities, "Liabilities should match credit card balance")
        assertEquals(Money.fromDollars(16999.75), netWorth.netWorth, "Net worth should be assets minus liabilities")
        assertTrue(netWorth.assetBreakdown.isNotEmpty(), "Should have asset breakdown")
        assertTrue(netWorth.liabilityBreakdown.isNotEmpty(), "Should have liability breakdown")
        
        // Verify budget analysis
        assertTrue(budgetAnalysis.totalSpent.isPositive, "Should have spending data")
        assertTrue(budgetAnalysis.categoryComparisons.isNotEmpty(), "Should have category comparisons")
        
        // Verify tax analysis
        assertTrue(taxAnalysis.grossIncome.isPositive, "Should have income data")
        assertTrue(taxAnalysis.estimatedTaxes.totalTax.isPositive, "Should calculate taxes")
        assertTrue(taxAnalysis.rrspContributions.contributionRoom.isPositive, "Should have RRSP room")
        assertTrue(taxAnalysis.tfsaContributions.contributionRoom.isPositive, "Should have TFSA room")
        assertTrue(taxAnalysis.taxOptimizationRecommendations.isNotEmpty(), "Should have tax recommendations")
        
        // Verify personalized recommendations
        assertTrue(recommendations.isNotEmpty(), "Should have personalized recommendations")
        assertTrue(recommendations.any { it.type == RecommendationType.SPENDING_REDUCTION }, "Should have spending recommendations")
        assertTrue(recommendations.any { it.type == RecommendationType.TAX_OPTIMIZATION }, "Should have tax recommendations")
        
        // Verify Canadian-specific calculations
        assertEquals(CanadianProvince.ON, taxAnalysis.province, "Should default to Ontario")
        assertTrue(taxAnalysis.estimatedTaxes.cpp.isPositive, "Should calculate CPP")
        assertTrue(taxAnalysis.estimatedTaxes.ei.isPositive, "Should calculate EI")
        assertTrue(taxAnalysis.estimatedTaxes.federalTax.isPositive, "Should calculate federal tax")
        assertTrue(taxAnalysis.estimatedTaxes.provincialTax.isPositive, "Should calculate provincial tax")
        
        // Verify insights quality
        val highSpendingInsights = spendingAnalysis.insights.filter { it.type == InsightType.SPENDING_PATTERN }
        assertTrue(highSpendingInsights.isNotEmpty(), "Should identify spending patterns")
        
        val savingsOpportunities = spendingAnalysis.insights.filter { it.type == InsightType.SAVINGS_OPPORTUNITY }
        // May or may not have savings opportunities depending on spending patterns
        
        // Verify recommendation priorities
        val highPriorityRecommendations = recommendations.filter { it.priority == Priority.HIGH }
        val mediumPriorityRecommendations = recommendations.filter { it.priority == Priority.MEDIUM }
        assertTrue(highPriorityRecommendations.isNotEmpty() || mediumPriorityRecommendations.isNotEmpty(), 
                  "Should have actionable recommendations")
        
        // Verify trend analysis
        assertTrue(spendingAnalysis.trends.isNotEmpty(), "Should analyze spending trends")
        
        // Verify Canadian financial product recommendations
        val rrspRecommendations = recommendations.filter { it.title.contains("RRSP", ignoreCase = true) }
        val tfsaRecommendations = recommendations.filter { it.title.contains("TFSA", ignoreCase = true) }
        assertTrue(rrspRecommendations.isNotEmpty() || tfsaRecommendations.isNotEmpty(), 
                  "Should recommend Canadian investment products")
    }
    
    @Test
    fun `analytics should handle edge cases gracefully`() = runTest {
        // Given - User with minimal data
        val userId = "minimal_user"
        val mockTransactionRepo = MockTransactionRepository()
        val mockAccountRepo = MockAccountRepository()
        val mockUserRepo = MockUserRepository()
        
        val analyticsService = FinancialAnalyticsServiceImpl(
            mockTransactionRepo,
            mockAccountRepo,
            mockUserRepo
        )
        
        val user = User(
            id = userId,
            email = "minimal@example.com",
            profile = UserProfile(firstName = "Min", lastName = "User"),
            preferences = UserPreferences(),
            gamificationData = GamificationProfile(
                level = 1,
                totalPoints = 0,
                currentStreaks = emptyList(),
                achievements = emptyList(),
                lastActivity = Clock.System.now()
            )
        )
        mockUserRepo.setUser(user)
        
        // Empty accounts and transactions
        mockAccountRepo.setAccounts(emptyList())
        mockTransactionRepo.setTransactions(emptyList())
        
        // When - Generate analysis with minimal data
        val currentMonth = AnalyticsHelpers.getCurrentMonthRange()
        
        val spendingResult = analyticsService.generateSpendingAnalysis(userId, currentMonth)
        val netWorthResult = analyticsService.calculateNetWorth(userId)
        val budgetResult = analyticsService.analyzeBudgetPerformance(userId, currentMonth)
        
        // Then - Should handle gracefully without errors
        assertTrue(spendingResult.isSuccess, "Should handle empty transactions")
        assertTrue(netWorthResult.isSuccess, "Should handle empty accounts")
        assertTrue(budgetResult.isSuccess, "Should handle empty budget data")
        
        val spendingAnalysis = spendingResult.getOrThrow()
        assertEquals(Money.zero(), spendingAnalysis.totalSpent, "Should show zero spending")
        assertEquals(Money.zero(), spendingAnalysis.totalIncome, "Should show zero income")
        assertTrue(spendingAnalysis.categoryBreakdown.isEmpty(), "Should have empty category breakdown")
        
        val netWorth = netWorthResult.getOrThrow()
        assertEquals(Money.zero(), netWorth.totalAssets, "Should show zero assets")
        assertEquals(Money.zero(), netWorth.totalLiabilities, "Should show zero liabilities")
        assertEquals(Money.zero(), netWorth.netWorth, "Should show zero net worth")
    }
    
    private fun createRealisticTransactions(period: DateRange): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        var transactionId = 1
        
        // Monthly salary
        transactions.add(
            Transaction(
                id = "salary_${transactionId++}",
                accountId = "chequing_001",
                amount = Money.fromDollars(4500.00),
                description = "Salary Deposit",
                category = Category.SALARY,
                date = period.startDate.plus(1, DateTimeUnit.DAY),
                merchantName = "Employer Corp"
            )
        )
        
        // Grocery shopping (weekly)
        for (week in 0..3) {
            transactions.add(
                Transaction(
                    id = "grocery_${transactionId++}",
                    accountId = "chequing_001",
                    amount = Money.fromDollars(-120.50 - (week * 10)), // Varying amounts
                    description = "Grocery Purchase",
                    category = Category.GROCERIES,
                    date = period.startDate.plus(week * 7 + 2, DateTimeUnit.DAY),
                    merchantName = "Loblaws"
                )
            )
        }
        
        // Restaurant visits
        transactions.addAll(listOf(
            Transaction(
                id = "restaurant_${transactionId++}",
                accountId = "chequing_001",
                amount = Money.fromDollars(-45.75),
                description = "Dinner",
                category = Category.RESTAURANTS,
                date = period.startDate.plus(5, DateTimeUnit.DAY),
                merchantName = "Tim Hortons"
            ),
            Transaction(
                id = "restaurant_${transactionId++}",
                accountId = "chequing_001",
                amount = Money.fromDollars(-85.20),
                description = "Weekend Dinner",
                category = Category.RESTAURANTS,
                date = period.startDate.plus(12, DateTimeUnit.DAY),
                merchantName = "Swiss Chalet"
            )
        ))
        
        // Gas purchases
        transactions.addAll(listOf(
            Transaction(
                id = "gas_${transactionId++}",
                accountId = "chequing_001",
                amount = Money.fromDollars(-65.00),
                description = "Gas Purchase",
                category = Category.GAS,
                date = period.startDate.plus(7, DateTimeUnit.DAY),
                merchantName = "Petro-Canada"
            ),
            Transaction(
                id = "gas_${transactionId++}",
                accountId = "chequing_001",
                amount = Money.fromDollars(-72.50),
                description = "Gas Purchase",
                category = Category.GAS,
                date = period.startDate.plus(21, DateTimeUnit.DAY),
                merchantName = "Esso"
            )
        ))
        
        // Utility bills
        transactions.addAll(listOf(
            Transaction(
                id = "hydro_${transactionId++}",
                accountId = "chequing_001",
                amount = Money.fromDollars(-125.75),
                description = "Hydro Bill",
                category = Category.HYDRO,
                date = period.startDate.plus(15, DateTimeUnit.DAY),
                merchantName = "Toronto Hydro"
            ),
            Transaction(
                id = "internet_${transactionId++}",
                accountId = "chequing_001",
                amount = Money.fromDollars(-89.99),
                description = "Internet Bill",
                category = Category.INTERNET,
                date = period.startDate.plus(10, DateTimeUnit.DAY),
                merchantName = "Rogers"
            )
        ))
        
        // Shopping
        transactions.add(
            Transaction(
                id = "shopping_${transactionId++}",
                accountId = "credit_001",
                amount = Money.fromDollars(-156.80),
                description = "Clothing Purchase",
                category = Category.SHOPPING,
                date = period.startDate.plus(18, DateTimeUnit.DAY),
                merchantName = "The Bay"
            )
        )
        
        // RRSP contribution
        transactions.add(
            Transaction(
                id = "rrsp_${transactionId++}",
                accountId = "chequing_001",
                amount = Money.fromDollars(-500.00),
                description = "RRSP Contribution",
                category = Category.RRSP,
                date = period.startDate.plus(1, DateTimeUnit.DAY),
                merchantName = "RBC Investments"
            )
        )
        
        return transactions
    }
}