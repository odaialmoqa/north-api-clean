package com.north.mobile.integration

import com.north.mobile.data.analytics.*
import com.north.mobile.data.repository.*
import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

/**
 * Integration test for the complete recommendation system
 * Tests the interaction between recommendation engine, analytics service, and repositories
 */
class RecommendationIntegrationTest {
    
    private lateinit var userRepository: UserRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var analyticsService: FinancialAnalyticsService
    private lateinit var recommendationEngine: RecommendationEngine
    
    @BeforeTest
    fun setup() {
        // Use mock implementations for integration testing
        userRepository = MockUserRepository()
        accountRepository = MockAccountRepository()
        transactionRepository = MockTransactionRepository()
        analyticsService = FinancialAnalyticsServiceImpl(
            transactionRepository,
            accountRepository,
            userRepository
        )
        recommendationEngine = RecommendationEngineImpl(
            userRepository,
            accountRepository,
            transactionRepository,
            analyticsService
        )
    }
    
    @Test
    fun `complete recommendation workflow for Canadian user with RRSP and TFSA optimization`() = runTest {
        // Given - A Canadian user with income and contribution room
        val userId = "canadian_user_1"
        val userProfile = createCanadianUserProfile(
            userId = userId,
            age = 35,
            province = CanadianProvince.ON,
            grossAnnualIncome = Money.fromDollars(85000.0),
            rrspRoom = Money.fromDollars(15300.0), // 18% of income
            tfsaRoom = Money.fromDollars(6000.0),
            marginalTaxRate = 0.31, // Ontario marginal rate
            currentRRSPContributions = Money.fromDollars(8000.0),
            currentTFSAContributions = Money.fromDollars(2000.0)
        )
        
        // When - Generate comprehensive recommendations
        val recommendationsResult = recommendationEngine.generateFinancialPlanningRecommendations(userId, userProfile)
        
        // Then - Should receive tax optimization recommendations
        assertTrue(recommendationsResult.isSuccess)
        val recommendations = recommendationsResult.getOrThrow()
        
        // Verify RRSP recommendation
        val rrspRecommendation = recommendations.find { it.type == FinancialPlanningType.TAX_OPTIMIZATION && it.title.contains("RRSP") }
        assertNotNull(rrspRecommendation, "Should include RRSP optimization recommendation")
        assertEquals(Priority.HIGH, rrspRecommendation.priority)
        assertEquals(ImpactType.TAX_SAVINGS, rrspRecommendation.expectedImpact.impactType)
        assertTrue(rrspRecommendation.expectedImpact.financialImpact.amount > 0)
        
        // Verify TFSA recommendation
        val tfsaRecommendation = recommendations.find { it.type == FinancialPlanningType.TAX_OPTIMIZATION && it.title.contains("TFSA") }
        assertNotNull(tfsaRecommendation, "Should include TFSA optimization recommendation")
        assertEquals(Priority.MEDIUM, tfsaRecommendation.priority)
        
        // Test RRSP optimization details
        val rrspOptimizationResult = recommendationEngine.optimizeRRSPContributions(userProfile)
        assertTrue(rrspOptimizationResult.isSuccess)
        val rrspOptimization = rrspOptimizationResult.getOrThrow()
        
        assertTrue(rrspOptimization.recommendedContribution.amount > 0)
        assertTrue(rrspOptimization.recommendedContribution <= rrspOptimization.availableRoom)
        assertTrue(rrspOptimization.taxSavings.amount > 0)
        assertEquals(ContributionFrequency.MONTHLY, rrspOptimization.optimalTiming.frequency)
        assertTrue(rrspOptimization.reasoning.contains("tax rate"))
        
        // Test TFSA optimization details
        val tfsaOptimizationResult = recommendationEngine.optimizeTFSAContributions(userProfile)
        assertTrue(tfsaOptimizationResult.isSuccess)
        val tfsaOptimization = tfsaOptimizationResult.getOrThrow()
        
        assertTrue(tfsaOptimization.recommendedContribution.amount > 0)
        assertTrue(tfsaOptimization.recommendedContribution <= tfsaOptimization.availableRoom)
        assertTrue(tfsaOptimization.optimalAllocation.isNotEmpty())
        assertTrue(tfsaOptimization.reasoning.contains("tax-free"))
    }
    
    @Test
    fun `debt payoff optimization for user with multiple high-interest debts`() = runTest {
        // Given - User with multiple credit card debts
        val userId = "debt_user_1"
        val debtAccounts = listOf(
            createDebtAccount("cc1", "Visa", Money.fromDollars(-8500.0), 19.99),
            createDebtAccount("cc2", "MasterCard", Money.fromDollars(-4200.0), 22.99),
            createDebtAccount("cc3", "Store Card", Money.fromDollars(-1800.0), 26.99)
        )
        
        val userProfile = createCanadianUserProfile(
            userId = userId,
            accounts = debtAccounts + listOf(createCheckingAccount()),
            grossAnnualIncome = Money.fromDollars(65000.0),
            age = 28 // Young user should get hybrid strategy
        )
        
        // When - Optimize debt payoff strategy
        val debtStrategyResult = recommendationEngine.optimizeDebtPayoff(userProfile)
        
        // Then - Should receive optimal debt payoff strategy
        assertTrue(debtStrategyResult.isSuccess)
        val strategy = debtStrategyResult.getOrThrow()
        
        // Young user with moderate debt should get hybrid strategy
        assertEquals(DebtPayoffMethod.HYBRID, strategy.strategy)
        assertEquals(3, strategy.payoffOrder.size)
        assertTrue(strategy.totalInterestSaved.amount > 0)
        assertTrue(strategy.payoffTimeframe > 0)
        assertTrue(strategy.reasoning.contains("hybrid"))
        
        // Should prioritize smallest debt first (hybrid approach)
        val firstDebt = strategy.payoffOrder.first()
        assertEquals(1800.0, firstDebt.currentBalance.amount) // Smallest balance
        
        // Should have alternative strategies
        assertTrue(strategy.alternativeStrategies.isNotEmpty())
        val avalancheAlternative = strategy.alternativeStrategies.find { it.method == DebtPayoffMethod.AVALANCHE }
        assertNotNull(avalancheAlternative)
        assertTrue(avalancheAlternative.pros.contains("Saves the most money"))
        
        // Generate comprehensive recommendations should include debt reduction
        val recommendationsResult = recommendationEngine.generateFinancialPlanningRecommendations(userId, userProfile)
        assertTrue(recommendationsResult.isSuccess)
        val recommendations = recommendationsResult.getOrThrow()
        
        val debtRecommendation = recommendations.find { it.type == FinancialPlanningType.DEBT_REDUCTION }
        assertNotNull(debtRecommendation, "Should include debt reduction recommendation")
        assertEquals(Priority.CRITICAL, debtRecommendation.priority)
    }
    
    @Test
    fun `savings optimization for user with insufficient emergency fund`() = runTest {
        // Given - User with low emergency fund
        val userId = "savings_user_1"
        val accounts = listOf(
            createCheckingAccount(balance = Money.fromDollars(3000.0)),
            createSavingsAccount("emergency", Money.fromDollars(2000.0)), // Insufficient emergency fund
            createSavingsAccount("general", Money.fromDollars(5000.0))
        )
        
        val userProfile = createCanadianUserProfile(
            userId = userId,
            accounts = accounts,
            grossAnnualIncome = Money.fromDollars(70000.0),
            age = 32,
            riskTolerance = RiskTolerance.MODERATE
        )
        
        // When - Optimize savings strategy
        val savingsResult = recommendationEngine.optimizeSavingsStrategy(userProfile)
        
        // Then - Should recommend building emergency fund
        assertTrue(savingsResult.isSuccess)
        val savingsOptimization = savingsResult.getOrThrow()
        
        assertTrue(savingsOptimization.emergencyFundTarget.amount > savingsOptimization.currentEmergencyFund.amount)
        assertTrue(savingsOptimization.recommendedSavingsRate > savingsOptimization.currentSavingsRate)
        assertTrue(savingsOptimization.optimalAllocation.isNotEmpty())
        
        // Should prioritize emergency fund in allocation
        val emergencyAllocation = savingsOptimization.optimalAllocation.find { 
            it.accountType == SavingsAccountType.EMERGENCY_FUND 
        }
        assertNotNull(emergencyAllocation)
        assertTrue(emergencyAllocation.percentage > 0)
        
        // Generate comprehensive recommendations should include emergency fund
        val recommendationsResult = recommendationEngine.generateFinancialPlanningRecommendations(userId, userProfile)
        assertTrue(recommendationsResult.isSuccess)
        val recommendations = recommendationsResult.getOrThrow()
        
        val emergencyRecommendation = recommendations.find { it.type == FinancialPlanningType.EMERGENCY_FUND }
        assertNotNull(emergencyRecommendation, "Should include emergency fund recommendation")
        assertEquals(Priority.HIGH, emergencyRecommendation.priority)
        assertTrue(emergencyRecommendation.description.contains("emergency fund"))
    }
    
    @Test
    fun `recommendation tracking and effectiveness measurement`() = runTest {
        // Given - A completed recommendation
        val recommendationId = "test_recommendation_123"
        val outcome = RecommendationOutcome(
            recommendationId = recommendationId,
            action = RecommendationAction.IMPLEMENTED,
            actualImpact = Money.fromDollars(2400.0), // Annual tax savings
            timeToComplete = 14, // 2 weeks to implement
            userFeedback = UserFeedback(
                rating = 5,
                comment = "Excellent recommendation, saved me a lot in taxes!",
                helpfulness = 5,
                clarity = 4,
                suggestions = "Could provide more specific steps for implementation"
            ),
            completedAt = Clock.System.now()
        )
        
        // When - Track recommendation effectiveness
        val trackingResult = recommendationEngine.trackRecommendationEffectiveness(recommendationId, outcome)
        
        // Then - Should successfully track the outcome
        assertTrue(trackingResult.isSuccess)
        
        // When - Get recommendation explanation
        val explanationResult = recommendationEngine.getRecommendationExplanation(recommendationId)
        
        // Then - Should provide detailed explanation
        assertTrue(explanationResult.isSuccess)
        val explanation = explanationResult.getOrThrow()
        
        assertEquals(recommendationId, explanation.recommendationId)
        assertTrue(explanation.summary.isNotEmpty())
        assertTrue(explanation.detailedReasoning.isNotEmpty())
        assertTrue(explanation.assumptions.isNotEmpty())
        assertTrue(explanation.calculations.isNotEmpty())
        assertTrue(explanation.sources.isNotEmpty())
        assertTrue(explanation.riskFactors.isNotEmpty())
        assertTrue(explanation.alternativeApproaches.isNotEmpty())
        
        // Calculations should include tax savings formula
        val taxCalculation = explanation.calculations.find { it.description.contains("tax savings") }
        assertNotNull(taxCalculation)
        assertTrue(taxCalculation.formula.contains("×"))
        assertTrue(taxCalculation.inputs.isNotEmpty())
        assertTrue(taxCalculation.result.isNotEmpty())
    }
    
    @Test
    fun `goal acceleration recommendations for off-track financial goals`() = runTest {
        // Given - User with off-track financial goals
        val userId = "goal_user_1"
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val offTrackGoal = FinancialGoal(
            id = "vacation_goal",
            userId = userId,
            title = "European Vacation",
            description = "Save for 3-week European vacation",
            targetAmount = Money.fromDollars(8000.0),
            currentAmount = Money.fromDollars(2000.0), // Only 25% progress
            targetDate = today.plus(180, DateTimeUnit.DAY), // 6 months from now
            priority = Priority.MEDIUM,
            category = GoalCategory.VACATION,
            isCompleted = false,
            createdAt = today.minus(120, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.currentSystemDefault()), // Started 4 months ago
            updatedAt = Clock.System.now()
        )
        
        val userProfile = createCanadianUserProfile(
            userId = userId,
            goals = listOf(offTrackGoal),
            grossAnnualIncome = Money.fromDollars(75000.0)
        )
        
        // When - Generate recommendations
        val recommendationsResult = recommendationEngine.generateFinancialPlanningRecommendations(userId, userProfile)
        
        // Then - Should include goal acceleration recommendation
        assertTrue(recommendationsResult.isSuccess)
        val recommendations = recommendationsResult.getOrThrow()
        
        val goalAcceleration = recommendations.find { it.type == FinancialPlanningType.GOAL_ACCELERATION }
        assertNotNull(goalAcceleration, "Should include goal acceleration recommendation")
        assertTrue(goalAcceleration.title.contains("European Vacation"))
        assertTrue(goalAcceleration.description.contains("back on track"))
        assertEquals(Priority.MEDIUM, goalAcceleration.priority)
        
        // Should have actionable steps
        assertTrue(goalAcceleration.actionSteps.isNotEmpty())
        val reviewStep = goalAcceleration.actionSteps.find { it.description.contains("Review goal timeline") }
        assertNotNull(reviewStep)
        
        val increaseStep = goalAcceleration.actionSteps.find { it.description.contains("Increase monthly contributions") }
        assertNotNull(increaseStep)
    }
    
    @Test
    fun `comprehensive recommendations prioritization and sorting`() = runTest {
        // Given - User with multiple financial needs
        val userId = "complex_user_1"
        val userProfile = createCanadianUserProfile(
            userId = userId,
            age = 40,
            grossAnnualIncome = Money.fromDollars(95000.0),
            accounts = listOf(
                createCheckingAccount(balance = Money.fromDollars(2000.0)),
                createSavingsAccount("emergency", Money.fromDollars(3000.0)), // Insufficient
                createDebtAccount("cc1", "Credit Card", Money.fromDollars(-12000.0), 21.99)
            ),
            rrspRoom = Money.fromDollars(17100.0),
            tfsaRoom = Money.fromDollars(6000.0),
            marginalTaxRate = 0.33,
            goals = listOf(
                createOffTrackGoal("retirement", Money.fromDollars(500000.0), Money.fromDollars(50000.0))
            )
        )
        
        // When - Generate comprehensive recommendations
        val recommendationsResult = recommendationEngine.generateFinancialPlanningRecommendations(userId, userProfile)
        
        // Then - Should receive prioritized recommendations
        assertTrue(recommendationsResult.isSuccess)
        val recommendations = recommendationsResult.getOrThrow()
        
        assertTrue(recommendations.size >= 4) // Should have multiple recommendations
        
        // Should be sorted by priority (CRITICAL > HIGH > MEDIUM > LOW)
        val priorities = recommendations.map { it.priority }
        val sortedPriorities = priorities.sortedByDescending { it.ordinal }
        assertEquals(sortedPriorities, priorities, "Recommendations should be sorted by priority")
        
        // High-interest debt should be highest priority
        val debtRecommendation = recommendations.find { it.type == FinancialPlanningType.DEBT_REDUCTION }
        assertNotNull(debtRecommendation)
        assertEquals(Priority.CRITICAL, debtRecommendation.priority)
        
        // Emergency fund should be high priority
        val emergencyRecommendation = recommendations.find { it.type == FinancialPlanningType.EMERGENCY_FUND }
        assertNotNull(emergencyRecommendation)
        assertEquals(Priority.HIGH, emergencyRecommendation.priority)
        
        // Tax optimization should be present
        val taxRecommendation = recommendations.find { it.type == FinancialPlanningType.TAX_OPTIMIZATION }
        assertNotNull(taxRecommendation)
        
        // Each recommendation should have complete information
        for (recommendation in recommendations) {
            assertTrue(recommendation.title.isNotEmpty())
            assertTrue(recommendation.description.isNotEmpty())
            assertTrue(recommendation.reasoning.primaryFactors.isNotEmpty())
            assertTrue(recommendation.actionSteps.isNotEmpty())
            assertTrue(recommendation.expectedImpact.financialImpact.amount >= 0)
            assertTrue(recommendation.reasoning.confidence > 0)
        }
    }
    
    // Helper methods for creating test data
    
    private fun createCanadianUserProfile(
        userId: String,
        age: Int = 35,
        province: CanadianProvince = CanadianProvince.ON,
        grossAnnualIncome: Money = Money.fromDollars(75000.0),
        accounts: List<Account> = listOf(createCheckingAccount()),
        goals: List<FinancialGoal> = emptyList(),
        rrspRoom: Money = Money.fromDollars(13500.0),
        tfsaRoom: Money = Money.fromDollars(6000.0),
        currentRRSPContributions: Money = Money.fromDollars(5000.0),
        currentTFSAContributions: Money = Money.fromDollars(3000.0),
        marginalTaxRate: Double = 0.30,
        riskTolerance: RiskTolerance = RiskTolerance.MODERATE
    ): UserFinancialProfile {
        val transactions = createTestTransactions(userId)
        val spendingAnalysis = createSpendingAnalysis(userId, transactions)
        val budgetAnalysis = createBudgetAnalysis(userId)
        
        return UserFinancialProfile(
            userId = userId,
            age = age,
            province = province,
            grossAnnualIncome = grossAnnualIncome,
            netWorth = createNetWorthSummary(userId, accounts),
            accounts = accounts,
            transactions = transactions,
            goals = goals,
            currentRRSPContributions = currentRRSPContributions,
            currentTFSAContributions = currentTFSAContributions,
            rrspRoom = rrspRoom,
            tfsaRoom = tfsaRoom,
            marginalTaxRate = marginalTaxRate,
            riskTolerance = riskTolerance,
            timeHorizon = TimeHorizon.LONG_TERM,
            spendingAnalysis = spendingAnalysis,
            budgetAnalysis = budgetAnalysis
        )
    }
    
    private fun createDebtAccount(id: String, name: String, balance: Money, interestRate: Double): Account {
        return Account(
            id = id,
            userId = "user1",
            institutionId = "bank1",
            accountType = AccountType.CREDIT_CARD,
            name = name,
            balance = balance,
            currency = Currency.CAD,
            isActive = true,
            lastUpdated = Clock.System.now(),
            interestRate = interestRate,
            isDebt = true,
            minimumPayment = Money.fromDollars(balance.amount.absoluteValue * 0.03) // 3% minimum payment
        )
    }
    
    private fun createCheckingAccount(balance: Money = Money.fromDollars(5000.0)): Account {
        return Account(
            id = "checking1",
            userId = "user1",
            institutionId = "bank1",
            accountType = AccountType.CHECKING,
            name = "Main Checking",
            balance = balance,
            currency = Currency.CAD,
            isActive = true,
            lastUpdated = Clock.System.now(),
            interestRate = 0.0,
            isDebt = false,
            minimumPayment = null
        )
    }
    
    private fun createSavingsAccount(name: String, balance: Money): Account {
        return Account(
            id = "${name}_savings",
            userId = "user1",
            institutionId = "bank1",
            accountType = AccountType.SAVINGS,
            name = "$name Savings",
            balance = balance,
            currency = Currency.CAD,
            isActive = true,
            lastUpdated = Clock.System.now(),
            interestRate = 2.5,
            isDebt = false,
            minimumPayment = null
        )
    }
    
    private fun createOffTrackGoal(id: String, targetAmount: Money, currentAmount: Money): FinancialGoal {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return FinancialGoal(
            id = id,
            userId = "user1",
            title = "Retirement Savings",
            description = "Build retirement nest egg",
            targetAmount = targetAmount,
            currentAmount = currentAmount,
            targetDate = today.plus(10 * 365, DateTimeUnit.DAY), // 10 years
            priority = Priority.HIGH,
            category = GoalCategory.RETIREMENT,
            isCompleted = false,
            createdAt = today.minus(2 * 365, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.currentSystemDefault()), // Started 2 years ago
            updatedAt = Clock.System.now()
        )
    }
    
    private fun createTestTransactions(userId: String): List<Transaction> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return listOf(
            Transaction(
                id = "tx1",
                accountId = "checking1",
                amount = Money.fromDollars(-500.0),
                description = "Grocery Store",
                category = Category(id = "groceries", name = "Groceries", type = CategoryType.EXPENSE),
                date = today.minus(5, DateTimeUnit.DAY),
                isRecurring = false,
                isDebit = true,
                absoluteAmount = Money.fromDollars(500.0)
            ),
            Transaction(
                id = "tx2",
                accountId = "checking1",
                amount = Money.fromDollars(-1200.0),
                description = "Rent Payment",
                category = Category(id = "housing", name = "Housing", type = CategoryType.EXPENSE),
                date = today.minus(1, DateTimeUnit.DAY),
                isRecurring = true,
                isDebit = true,
                absoluteAmount = Money.fromDollars(1200.0)
            ),
            Transaction(
                id = "tx3",
                accountId = "checking1",
                amount = Money.fromDollars(3500.0),
                description = "Salary Deposit",
                category = Category(id = "salary", name = "Salary", type = CategoryType.INCOME),
                date = today.minus(15, DateTimeUnit.DAY),
                isRecurring = true,
                isDebit = false,
                absoluteAmount = Money.fromDollars(3500.0)
            )
        )
    }
    
    private fun createSpendingAnalysis(userId: String, transactions: List<Transaction>): SpendingAnalysis {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val period = DateRange(
            today.minus(30, DateTimeUnit.DAY),
            today
        )
        
        val totalSpent = transactions.filter { it.isDebit }.fold(Money.zero()) { acc, tx -> acc + tx.absoluteAmount }
        val totalIncome = transactions.filter { !it.isDebit }.fold(Money.zero()) { acc, tx -> acc + tx.absoluteAmount }
        
        return SpendingAnalysis(
            userId = userId,
            period = period,
            totalSpent = totalSpent,
            totalIncome = totalIncome,
            netCashFlow = totalIncome - totalSpent,
            categoryBreakdown = listOf(
                CategorySpending(
                    category = Category(id = "groceries", name = "Groceries", type = CategoryType.EXPENSE),
                    totalAmount = Money.fromDollars(500.0),
                    transactionCount = 1,
                    averageAmount = Money.fromDollars(500.0),
                    percentageOfTotal = 29.4, // 500/1700 * 100
                    trend = TrendDirection.INCREASING
                ),
                CategorySpending(
                    category = Category(id = "housing", name = "Housing", type = CategoryType.EXPENSE),
                    totalAmount = Money.fromDollars(1200.0),
                    transactionCount = 1,
                    averageAmount = Money.fromDollars(1200.0),
                    percentageOfTotal = 70.6, // 1200/1700 * 100
                    trend = TrendDirection.STABLE
                )
            ),
            trends = emptyList(),
            insights = emptyList(),
            comparisonToPrevious = null,
            generatedAt = Clock.System.now()
        )
    }
    
    private fun createBudgetAnalysis(userId: String): BudgetAnalysis {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val period = DateRange(
            today.minus(30, DateTimeUnit.DAY),
            today
        )
        
        return BudgetAnalysis(
            userId = userId,
            period = period,
            totalBudget = Money.fromDollars(2000.0),
            totalSpent = Money.fromDollars(1700.0),
            remainingBudget = Money.fromDollars(300.0),
            categoryComparisons = emptyList(),
            overallPerformance = BudgetPerformance.UNDER_BUDGET,
            alerts = emptyList(),
            recommendations = emptyList()
        )
    }
    
    private fun createNetWorthSummary(userId: String, accounts: List<Account>): NetWorthSummary {
        val totalAssets = accounts.filter { !it.isDebt }.fold(Money.zero()) { acc, account -> acc + account.balance }
        val totalLiabilities = accounts.filter { it.isDebt }.fold(Money.zero()) { acc, account -> acc + account.balance.absoluteValue }
        
        return NetWorthSummary(
            userId = userId,
            calculatedAt = Clock.System.now(),
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            netWorth = totalAssets - totalLiabilities,
            assetBreakdown = emptyList(),
            liabilityBreakdown = emptyList(),
            monthlyChange = Money.fromDollars(500.0),
            yearlyChange = Money.fromDollars(6000.0),
            trend = TrendDirection.INCREASING,
            projectedNetWorth = emptyList()
        )
    }
}