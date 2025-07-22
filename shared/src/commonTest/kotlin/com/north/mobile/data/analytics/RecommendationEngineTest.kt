package com.north.mobile.data.analytics

import com.north.mobile.data.repository.AccountRepository
import com.north.mobile.data.repository.TransactionRepository
import com.north.mobile.data.repository.UserRepository
import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

class RecommendationEngineTest {
    
    private lateinit var mockUserRepository: MockUserRepository
    private lateinit var mockAccountRepository: MockAccountRepository
    private lateinit var mockTransactionRepository: MockTransactionRepository
    private lateinit var mockAnalyticsService: MockFinancialAnalyticsService
    private lateinit var recommendationEngine: RecommendationEngine
    
    @BeforeTest
    fun setup() {
        mockUserRepository = MockUserRepository()
        mockAccountRepository = MockAccountRepository()
        mockTransactionRepository = MockTransactionRepository()
        mockAnalyticsService = MockFinancialAnalyticsService()
        
        recommendationEngine = RecommendationEngineImpl(
            mockUserRepository,
            mockAccountRepository,
            mockTransactionRepository,
            mockAnalyticsService
        )
    }
    
    @Test
    fun `generateFinancialPlanningRecommendations should return comprehensive recommendations`() = runTest {
        // Given
        val userProfile = createTestUserProfile()
        
        // When
        val result = recommendationEngine.generateFinancialPlanningRecommendations("user1", userProfile)
        
        // Then
        assertTrue(result.isSuccess)
        val recommendations = result.getOrThrow()
        assertTrue(recommendations.isNotEmpty())
        
        // Should include tax optimization recommendations
        assertTrue(recommendations.any { it.type == FinancialPlanningType.TAX_OPTIMIZATION })
        
        // Should include emergency fund recommendation if needed
        assertTrue(recommendations.any { it.type == FinancialPlanningType.EMERGENCY_FUND })
        
        // Should be sorted by priority
        val priorities = recommendations.map { it.priority.ordinal }
        assertEquals(priorities, priorities.sortedDescending())
    }
    
    @Test
    fun `optimizeRRSPContributions should calculate optimal contribution based on tax savings`() = runTest {
        // Given
        val userProfile = createTestUserProfile(
            grossAnnualIncome = Money.fromDollars(80000.0),
            rrspRoom = Money.fromDollars(14400.0), // 18% of income
            marginalTaxRate = 0.30
        )
        
        // When
        val result = recommendationEngine.optimizeRRSPContributions(userProfile)
        
        // Then
        assertTrue(result.isSuccess)
        val recommendation = result.getOrThrow()
        
        assertTrue(recommendation.recommendedContribution.amount > 0)
        assertTrue(recommendation.recommendedContribution <= recommendation.availableRoom)
        assertEquals(recommendation.taxSavings, recommendation.recommendedContribution * 0.30)
        assertTrue(recommendation.reasoning.contains("tax rate"))
        assertTrue(recommendation.impactOnGoals.isNotEmpty())
    }
    
    @Test
    fun `optimizeTFSAContributions should recommend contribution within available room`() = runTest {
        // Given
        val userProfile = createTestUserProfile(
            tfsaRoom = Money.fromDollars(6000.0),
            riskTolerance = RiskTolerance.MODERATE
        )
        
        // When
        val result = recommendationEngine.optimizeTFSAContributions(userProfile)
        
        // Then
        assertTrue(result.isSuccess)
        val recommendation = result.getOrThrow()
        
        assertTrue(recommendation.recommendedContribution.amount > 0)
        assertTrue(recommendation.recommendedContribution <= recommendation.availableRoom)
        assertTrue(recommendation.optimalAllocation.isNotEmpty())
        assertTrue(recommendation.reasoning.contains("tax-free"))
        
        // Should have balanced allocation for moderate risk tolerance
        val allocation = recommendation.optimalAllocation
        assertTrue(allocation.any { it.assetClass == AssetClass.CANADIAN_EQUITY })
        assertTrue(allocation.any { it.assetClass == AssetClass.BONDS })
    }
    
    @Test
    fun `optimizeDebtPayoff should return no strategy when no debt exists`() = runTest {
        // Given
        val userProfile = createTestUserProfile(accounts = listOf(
            createTestAccount(accountType = AccountType.CHECKING, balance = Money.fromDollars(5000.0))
        ))
        
        // When
        val result = recommendationEngine.optimizeDebtPayoff(userProfile)
        
        // Then
        assertTrue(result.isSuccess)
        val strategy = result.getOrThrow()
        
        assertEquals(DebtPayoffMethod.MINIMUM_ONLY, strategy.strategy)
        assertTrue(strategy.payoffOrder.isEmpty())
        assertEquals(Money.zero(), strategy.totalInterestSaved)
        assertEquals("No debt to optimize", strategy.reasoning)
    }
    
    @Test
    fun `optimizeDebtPayoff should recommend avalanche strategy for high debt amounts`() = runTest {
        // Given
        val debtAccounts = listOf(
            createTestAccount(
                accountType = AccountType.CREDIT_CARD,
                balance = Money.fromDollars(-30000.0)
            ),
            createTestAccount(
                accountType = AccountType.CREDIT_CARD,
                balance = Money.fromDollars(-20000.0)
            )
        )
        val userProfile = createTestUserProfile(
            accounts = debtAccounts,
            grossAnnualIncome = Money.fromDollars(60000.0)
        )
        
        // When
        val result = recommendationEngine.optimizeDebtPayoff(userProfile)
        
        // Then
        assertTrue(result.isSuccess)
        val strategy = result.getOrThrow()
        
        assertEquals(DebtPayoffMethod.AVALANCHE, strategy.strategy)
        assertTrue(strategy.payoffOrder.isNotEmpty())
        assertTrue(strategy.totalInterestSaved.amount > 0)
        
        // Should prioritize highest interest rate debt first
        val firstDebt = strategy.payoffOrder.first()
        assertEquals(19.99, firstDebt.interestRate)
    }
    
    @Test
    fun `optimizeSavingsStrategy should recommend appropriate savings rate based on age and debt`() = runTest {
        // Given - young user with low debt
        val userProfile = createTestUserProfile(
            age = 25,
            grossAnnualIncome = Money.fromDollars(60000.0),
            accounts = listOf(
                createTestAccount(balance = Money.fromDollars(5000.0))
            )
        )
        
        // When
        val result = recommendationEngine.optimizeSavingsStrategy(userProfile)
        
        // Then
        assertTrue(result.isSuccess)
        val recommendation = result.getOrThrow()
        
        // Young users should have higher recommended savings rate
        assertTrue(recommendation.recommendedSavingsRate > 0.20)
        assertTrue(recommendation.emergencyFundTarget.amount > 0)
        assertTrue(recommendation.optimalAllocation.isNotEmpty())
        assertTrue(recommendation.reasoning.contains("savings rate"))
    }
    
    @Test
    fun `trackRecommendationEffectiveness should handle completed recommendations`() = runTest {
        // Given
        val outcome = RecommendationOutcome(
            recommendationId = "test_rec_1",
            action = RecommendationAction.IMPLEMENTED,
            actualImpact = Money.fromDollars(1500.0),
            timeToComplete = 30,
            userFeedback = UserFeedback(
                rating = 5,
                comment = "Very helpful",
                helpfulness = 5,
                clarity = 4,
                suggestions = null
            ),
            completedAt = Clock.System.now()
        )
        
        // When
        val result = recommendationEngine.trackRecommendationEffectiveness("test_rec_1", outcome)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `getRecommendationExplanation should provide detailed explanation`() = runTest {
        // Given
        val recommendationId = "test_rec_1"
        
        // When
        val result = recommendationEngine.getRecommendationExplanation(recommendationId)
        
        // Then
        assertTrue(result.isSuccess)
        val explanation = result.getOrThrow()
        
        assertEquals(recommendationId, explanation.recommendationId)
        assertTrue(explanation.summary.isNotEmpty())
        assertTrue(explanation.detailedReasoning.isNotEmpty())
        assertTrue(explanation.assumptions.isNotEmpty())
        assertTrue(explanation.calculations.isNotEmpty())
        assertTrue(explanation.sources.isNotEmpty())
        assertTrue(explanation.riskFactors.isNotEmpty())
        assertTrue(explanation.alternativeApproaches.isNotEmpty())
    }
    
    @Test
    fun `recommendations should consider Canadian tax implications`() = runTest {
        // Given
        val userProfile = createTestUserProfile(
            province = CanadianProvince.ON,
            grossAnnualIncome = Money.fromDollars(75000.0),
            rrspRoom = Money.fromDollars(13500.0),
            tfsaRoom = Money.fromDollars(6000.0),
            marginalTaxRate = 0.31 // Ontario marginal rate
        )
        
        // When
        val result = recommendationEngine.generateFinancialPlanningRecommendations("user1", userProfile)
        
        // Then
        assertTrue(result.isSuccess)
        val recommendations = result.getOrThrow()
        
        // Should include RRSP recommendation with tax savings calculation
        val rrspRec = recommendations.find { it.title.contains("RRSP") }
        assertNotNull(rrspRec)
        assertEquals(ImpactType.TAX_SAVINGS, rrspRec.expectedImpact.impactType)
        
        // Should include TFSA recommendation
        val tfsaRec = recommendations.find { it.title.contains("TFSA") }
        assertNotNull(tfsaRec)
    }
    
    @Test
    fun `debt payoff strategies should include proper reasoning and alternatives`() = runTest {
        // Given
        val debtAccounts = listOf(
            createTestAccount(
                accountType = AccountType.CREDIT_CARD,
                balance = Money.fromDollars(-5000.0)
            ),
            createTestAccount(
                accountType = AccountType.CREDIT_CARD,
                balance = Money.fromDollars(-2000.0)
            )
        )
        val userProfile = createTestUserProfile(
            accounts = debtAccounts,
            age = 28 // Young user should get hybrid strategy
        )
        
        // When
        val result = recommendationEngine.optimizeDebtPayoff(userProfile)
        
        // Then
        assertTrue(result.isSuccess)
        val strategy = result.getOrThrow()
        
        assertTrue(strategy.reasoning.isNotEmpty())
        assertTrue(strategy.alternativeStrategies.isNotEmpty())
        
        // Should have alternative strategies with pros and cons
        val alternative = strategy.alternativeStrategies.first()
        assertTrue(alternative.pros.isNotEmpty())
        assertTrue(alternative.cons.isNotEmpty())
        assertTrue(alternative.description.isNotEmpty())
    }
    
    // Helper methods for creating test data
    
    private fun createTestUserProfile(
        userId: String = "user1",
        age: Int = 30,
        province: CanadianProvince = CanadianProvince.ON,
        grossAnnualIncome: Money = Money.fromDollars(70000.0),
        accounts: List<Account> = listOf(createTestAccount()),
        transactions: List<Transaction> = listOf(createTestTransaction()),
        goals: List<FinancialGoal> = listOf(createTestGoal()),
        currentRRSPContributions: Money = Money.fromDollars(5000.0),
        currentTFSAContributions: Money = Money.fromDollars(3000.0),
        rrspRoom: Money = Money.fromDollars(12600.0),
        tfsaRoom: Money = Money.fromDollars(6000.0),
        marginalTaxRate: Double = 0.30,
        riskTolerance: RiskTolerance = RiskTolerance.MODERATE,
        timeHorizon: TimeHorizon = TimeHorizon.LONG_TERM
    ): UserFinancialProfile {
        return UserFinancialProfile(
            userId = userId,
            age = age,
            province = province,
            grossAnnualIncome = grossAnnualIncome,
            netWorth = createTestNetWorthSummary(),
            accounts = accounts,
            transactions = transactions,
            goals = goals,
            currentRRSPContributions = currentRRSPContributions,
            currentTFSAContributions = currentTFSAContributions,
            rrspRoom = rrspRoom,
            tfsaRoom = tfsaRoom,
            marginalTaxRate = marginalTaxRate,
            riskTolerance = riskTolerance,
            timeHorizon = timeHorizon,
            spendingAnalysis = createTestSpendingAnalysis(),
            budgetAnalysis = createTestBudgetAnalysis()
        )
    }
    
    private fun createTestAccount(
        id: String = "account1",
        accountType: AccountType = AccountType.CHECKING,
        balance: Money = Money.fromDollars(5000.0),
        institutionName: String = "Test Bank"
    ): Account {
        return Account(
            id = id,
            institutionId = "institution1",
            institutionName = institutionName,
            accountType = accountType,
            balance = balance,
            currency = Currency.CAD,
            lastUpdated = Clock.System.now(),
            isActive = true
        )
    }
    
    private fun createTestTransaction(
        id: String = "transaction1",
        amount: Money = Money.fromDollars(-100.0)
    ): Transaction {
        return Transaction(
            id = id,
            accountId = "account1",
            amount = amount,
            description = "Test Transaction",
            category = Category(id = "groceries", name = "Groceries"),
            date = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            isRecurring = false
        )
    }
    
    private fun createTestGoal(
        id: String = "goal1",
        title: String = "Emergency Fund",
        targetAmount: Money = Money.fromDollars(10000.0),
        currentAmount: Money = Money.fromDollars(5000.0)
    ): FinancialGoal {
        return FinancialGoal(
            id = id,
            userId = "user1",
            title = title,
            description = "Build emergency fund",
            targetAmount = targetAmount,
            currentAmount = currentAmount,
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(365, DateTimeUnit.DAY),
            priority = Priority.HIGH,
            goalType = GoalType.EMERGENCY_FUND,
            createdAt = Clock.System.now()
        )
    }
    
    private fun createTestNetWorthSummary(): NetWorthSummary {
        return NetWorthSummary(
            userId = "user1",
            calculatedAt = Clock.System.now(),
            totalAssets = Money.fromDollars(50000.0),
            totalLiabilities = Money.fromDollars(20000.0),
            netWorth = Money.fromDollars(30000.0),
            assetBreakdown = listOf(),
            liabilityBreakdown = listOf(),
            monthlyChange = Money.fromDollars(500.0),
            yearlyChange = Money.fromDollars(6000.0),
            trend = TrendDirection.INCREASING,
            projectedNetWorth = listOf()
        )
    }
    
    private fun createTestSpendingAnalysis(): SpendingAnalysis {
        return SpendingAnalysis(
            userId = "user1",
            period = DateRange(
                Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(30, DateTimeUnit.DAY),
                Clock.System.todayIn(TimeZone.currentSystemDefault())
            ),
            totalSpent = Money.fromDollars(3000.0),
            totalIncome = Money.fromDollars(5000.0),
            netCashFlow = Money.fromDollars(2000.0),
            categoryBreakdown = listOf(
                CategorySpending(
                    category = Category(id = "groceries", name = "Groceries"),
                    totalAmount = Money.fromDollars(500.0),
                    transactionCount = 10,
                    averageAmount = Money.fromDollars(50.0),
                    percentageOfTotal = 16.7,
                    trend = TrendDirection.INCREASING
                )
            ),
            trends = listOf(),
            insights = listOf(),
            comparisonToPrevious = null,
            generatedAt = Clock.System.now()
        )
    }
    
    private fun createTestBudgetAnalysis(): BudgetAnalysis {
        return BudgetAnalysis(
            userId = "user1",
            period = DateRange(
                Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(30, DateTimeUnit.DAY),
                Clock.System.todayIn(TimeZone.currentSystemDefault())
            ),
            totalBudget = Money.fromDollars(3500.0),
            totalSpent = Money.fromDollars(3000.0),
            remainingBudget = Money.fromDollars(500.0),
            categoryComparisons = listOf(),
            overallPerformance = BudgetPerformance.UNDER_BUDGET,
            alerts = listOf(),
            recommendations = listOf()
        )
    }
}

// Mock implementations for testing

class MockUserRepository : UserRepository {
    override suspend fun findById(id: String): Result<User?> {
        return Result.success(null)
    }
    
    override suspend fun save(user: User): Result<User> {
        return Result.success(user)
    }
    
    override suspend fun delete(id: String): Result<Unit> {
        return Result.success(Unit)
    }
}

class MockAccountRepository : AccountRepository {
    override suspend fun findByUserId(userId: String): Result<List<Account>> {
        return Result.success(emptyList())
    }
    
    override suspend fun findById(id: String): Result<Account?> {
        return Result.success(null)
    }
    
    override suspend fun save(account: Account): Result<Account> {
        return Result.success(account)
    }
    
    override suspend fun delete(id: String): Result<Unit> {
        return Result.success(Unit)
    }
}

class MockTransactionRepository : TransactionRepository {
    override suspend fun findByAccountId(accountId: String): Result<List<Transaction>> {
        return Result.success(emptyList())
    }
    
    override suspend fun findByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<Transaction>> {
        return Result.success(emptyList())
    }
    
    override suspend fun findById(id: String): Result<Transaction?> {
        return Result.success(null)
    }
    
    override suspend fun save(transaction: Transaction): Result<Transaction> {
        return Result.success(transaction)
    }
    
    override suspend fun delete(id: String): Result<Unit> {
        return Result.success(Unit)
    }
}

class MockFinancialAnalyticsService : FinancialAnalyticsService {
    override suspend fun generateSpendingAnalysis(userId: String, period: DateRange, includeComparison: Boolean): Result<SpendingAnalysis> {
        return Result.success(SpendingAnalysis(
            userId = userId,
            period = period,
            totalSpent = Money.zero(),
            totalIncome = Money.zero(),
            netCashFlow = Money.zero(),
            categoryBreakdown = emptyList(),
            trends = emptyList(),
            insights = emptyList(),
            comparisonToPrevious = null,
            generatedAt = Clock.System.now()
        ))
    }
    
    override suspend fun analyzeSpendingTrends(userId: String, periods: List<DateRange>): Result<List<SpendingTrend>> {
        return Result.success(emptyList())
    }
    
    override suspend fun generateSpendingInsights(userId: String): Result<List<SpendingInsight>> {
        return Result.success(emptyList())
    }
    
    override suspend fun calculateNetWorth(userId: String): Result<NetWorthSummary> {
        return Result.success(NetWorthSummary(
            userId = userId,
            calculatedAt = Clock.System.now(),
            totalAssets = Money.zero(),
            totalLiabilities = Money.zero(),
            netWorth = Money.zero(),
            assetBreakdown = emptyList(),
            liabilityBreakdown = emptyList(),
            monthlyChange = null,
            yearlyChange = null,
            trend = TrendDirection.STABLE,
            projectedNetWorth = emptyList()
        ))
    }
    
    override suspend fun trackNetWorthHistory(userId: String, period: DateRange): Result<List<NetWorthSummary>> {
        return Result.success(emptyList())
    }
    
    override suspend fun projectNetWorth(userId: String, projectionMonths: Int): Result<List<NetWorthProjection>> {
        return Result.success(emptyList())
    }
    
    override suspend fun analyzeBudgetPerformance(userId: String, period: DateRange): Result<BudgetAnalysis> {
        return Result.success(BudgetAnalysis(
            userId = userId,
            period = period,
            totalBudget = Money.zero(),
            totalSpent = Money.zero(),
            remainingBudget = Money.zero(),
            categoryComparisons = emptyList(),
            overallPerformance = BudgetPerformance.ON_TRACK,
            alerts = emptyList(),
            recommendations = emptyList()
        ))
    }
    
    override suspend fun generateBudgetAlerts(userId: String): Result<List<BudgetAlert>> {
        return Result.success(emptyList())
    }
    
    override suspend fun recommendBudgetAdjustments(userId: String): Result<List<BudgetRecommendation>> {
        return Result.success(emptyList())
    }
    
    override suspend fun analyzeCanadianTaxes(userId: String, taxYear: Int): Result<CanadianTaxAnalysis> {
        return Result.success(CanadianTaxAnalysis(
            userId = userId,
            taxYear = taxYear,
            province = CanadianProvince.ON,
            grossIncome = Money.zero(),
            estimatedTaxes = TaxBreakdown(
                federalTax = Money.zero(),
                provincialTax = Money.zero(),
                cpp = Money.zero(),
                ei = Money.zero(),
                totalTax = Money.zero(),
                afterTaxIncome = Money.zero(),
                marginalTaxRate = 0.0,
                averageTaxRate = 0.0
            ),
            rrspContributions = RRSPAnalysis(
                currentContributions = Money.zero(),
                contributionRoom = Money.zero(),
                maxContribution = Money.zero(),
                taxSavings = Money.zero(),
                recommendedContribution = Money.zero(),
                carryForwardRoom = Money.zero()
            ),
            tfsaContributions = TFSAAnalysis(
                currentContributions = Money.zero(),
                contributionRoom = Money.zero(),
                maxContribution = Money.zero(),
                recommendedContribution = Money.zero(),
                withdrawalRoom = Money.zero()
            ),
            taxOptimizationRecommendations = emptyList()
        ))
    }
    
    override suspend fun calculateRRSPRecommendations(userId: String): Result<RRSPAnalysis> {
        return Result.success(RRSPAnalysis(
            currentContributions = Money.zero(),
            contributionRoom = Money.zero(),
            maxContribution = Money.zero(),
            taxSavings = Money.zero(),
            recommendedContribution = Money.zero(),
            carryForwardRoom = Money.zero()
        ))
    }
    
    override suspend fun calculateTFSARecommendations(userId: String): Result<TFSAAnalysis> {
        return Result.success(TFSAAnalysis(
            currentContributions = Money.zero(),
            contributionRoom = Money.zero(),
            maxContribution = Money.zero(),
            recommendedContribution = Money.zero(),
            withdrawalRoom = Money.zero()
        ))
    }
    
    override suspend fun generatePersonalizedRecommendations(userId: String): Result<List<PersonalizedRecommendation>> {
        return Result.success(emptyList())
    }
    
    override suspend fun markRecommendationCompleted(recommendationId: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun getRecommendationMetrics(userId: String): Result<RecommendationMetrics> {
        return Result.success(RecommendationMetrics(
            totalRecommendations = 0,
            completedRecommendations = 0,
            completionRate = 0.0,
            averageTimeToComplete = 0.0,
            totalPotentialSavings = Money.zero(),
            actualizedSavings = Money.zero(),
            topPerformingTypes = emptyList()
        ))
    }
}