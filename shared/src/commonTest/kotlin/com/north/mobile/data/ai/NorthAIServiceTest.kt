package com.north.mobile.data.ai

import com.north.mobile.domain.model.*
import com.north.mobile.data.analytics.*
import com.north.mobile.data.goal.GoalService
import com.north.mobile.data.gamification.GamificationService
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

class NorthAIServiceTest {
    
    private lateinit var northAIService: NorthAIService
    private lateinit var mockFinancialAnalyticsService: MockFinancialAnalyticsService
    private lateinit var mockRecommendationEngine: MockRecommendationEngine
    private lateinit var mockGoalService: MockGoalService
    private lateinit var mockGamificationService: MockGamificationService
    private lateinit var mockTransactionCategorizationService: MockTransactionCategorizationService
    
    @BeforeTest
    fun setup() {
        mockFinancialAnalyticsService = MockFinancialAnalyticsService()
        mockRecommendationEngine = MockRecommendationEngine()
        mockGoalService = MockGoalService()
        mockGamificationService = MockGamificationService()
        mockTransactionCategorizationService = MockTransactionCategorizationService()
        
        northAIService = NorthAIServiceImpl(
            financialAnalyticsService = mockFinancialAnalyticsService,
            recommendationEngine = mockRecommendationEngine,
            goalService = mockGoalService,
            gamificationService = mockGamificationService,
            transactionCategorizationService = mockTransactionCategorizationService
        )
    }
    
    @Test
    fun `processUserQuery should handle affordability check query`() = runTest {
        // Given
        val query = "Can I afford a $200 dinner?"
        val context = createTestUserFinancialContext()
        
        // When
        val result = northAIService.processUserQuery(query, context)
        
        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertTrue(response.message.contains("afford"))
        assertTrue(response.confidence > 0.5f)
        assertTrue(response.supportingData.isNotEmpty())
    }
    
    @Test
    fun `processUserQuery should handle spending analysis query`() = runTest {
        // Given
        val query = "How much did I spend on food this month?"
        val context = createTestUserFinancialContext()
        
        // When
        val result = northAIService.processUserQuery(query, context)
        
        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertTrue(response.message.contains("spending"))
        assertTrue(response.confidence > 0.5f)
    }
    
    @Test
    fun `processUserQuery should handle goal progress query`() = runTest {
        // Given
        val query = "How am I doing with my savings goal?"
        val context = createTestUserFinancialContext()
        
        // When
        val result = northAIService.processUserQuery(query, context)
        
        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertTrue(response.message.contains("goal"))
        assertTrue(response.confidence > 0.5f)
    }
    
    @Test
    fun `generatePersonalizedInsights should return relevant insights`() = runTest {
        // Given
        val context = createTestUserFinancialContext()
        
        // When
        val result = northAIService.generatePersonalizedInsights(context)
        
        // Then
        assertTrue(result.isSuccess)
        val insights = result.getOrThrow()
        assertTrue(insights.isNotEmpty())
        insights.forEach { insight ->
            assertTrue(insight.confidence > 0.0f)
            assertTrue(insight.title.isNotBlank())
            assertTrue(insight.description.isNotBlank())
        }
    }
    
    @Test
    fun `checkAffordability should analyze expense affordability correctly`() = runTest {
        // Given
        val expense = ExpenseRequest(
            description = "New laptop",
            amount = Money.fromDollars(1500.0),
            category = Category.SHOPPING,
            plannedDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
        )
        val context = createTestUserFinancialContext()
        
        // When
        val result = northAIService.checkAffordability(expense, context)
        
        // Then
        assertTrue(result.isSuccess)
        val affordabilityResult = result.getOrThrow()
        assertTrue(affordabilityResult.confidence > 0.0f)
        assertTrue(affordabilityResult.reasoning.isNotBlank())
        assertNotNull(affordabilityResult.impactOnGoals)
        assertNotNull(affordabilityResult.impactOnBudget)
    }
    
    @Test
    fun `explainTransaction should provide detailed transaction explanation`() = runTest {
        // Given
        val context = createTestUserFinancialContext()
        val transactionId = context.recentTransactions.first().id
        
        // When
        val result = northAIService.explainTransaction(transactionId, context)
        
        // Then
        assertTrue(result.isSuccess)
        val explanation = result.getOrThrow()
        assertEquals(transactionId, explanation.transactionId)
        assertTrue(explanation.summary.isNotBlank())
        assertTrue(explanation.categoryExplanation.isNotBlank())
        assertTrue(explanation.recommendations.isNotEmpty())
    }
    
    @Test
    fun `suggestOptimizations should return actionable suggestions`() = runTest {
        // Given
        val context = createTestUserFinancialContext()
        
        // When
        val result = northAIService.suggestOptimizations(context)
        
        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrThrow()
        suggestions.forEach { suggestion ->
            assertTrue(suggestion.title.isNotBlank())
            assertTrue(suggestion.description.isNotBlank())
            assertTrue(suggestion.potentialSavings.amount > 0)
            assertTrue(suggestion.confidence > 0.0f)
            assertTrue(suggestion.steps.isNotEmpty())
        }
    }
    
    @Test
    fun `generateFollowUpQuestions should return contextual questions`() = runTest {
        // Given
        val context = createTestUserFinancialContext()
        
        // When
        val result = northAIService.generateFollowUpQuestions(context)
        
        // Then
        assertTrue(result.isSuccess)
        val questions = result.getOrThrow()
        assertTrue(questions.isNotEmpty())
        assertTrue(questions.size <= 4) // Should limit to 4 questions
        questions.forEach { question ->
            assertTrue(question.isNotBlank())
            assertTrue(question.endsWith("?"))
        }
    }
    
    @Test
    fun `analyzeUnusualSpending should detect spending anomalies`() = runTest {
        // Given
        val context = createTestUserFinancialContextWithUnusualSpending()
        val timeframe = DateRange(
            Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(30, DateTimeUnit.DAY),
            Clock.System.todayIn(TimeZone.currentSystemDefault())
        )
        
        // When
        val result = northAIService.analyzeUnusualSpending(context, timeframe)
        
        // Then
        assertTrue(result.isSuccess)
        val alerts = result.getOrThrow()
        alerts.forEach { alert ->
            assertTrue(alert.description.isNotBlank())
            assertTrue(alert.explanation.isNotBlank())
            assertTrue(alert.recommendations.isNotEmpty())
            assertTrue(alert.amount.amount > 0)
        }
    }
    
    private fun createTestUserFinancialContext(): UserFinancialContext {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        val accounts = listOf(
            Account(
                id = "acc1",
                institutionId = "inst1",
                institutionName = "Test Bank",
                accountType = AccountType.CHECKING,
                balance = Money.fromDollars(2500.0),
                lastUpdated = Clock.System.now()
            ),
            Account(
                id = "acc2",
                institutionId = "inst1",
                institutionName = "Test Bank",
                accountType = AccountType.SAVINGS,
                balance = Money.fromDollars(10000.0),
                lastUpdated = Clock.System.now()
            )
        )
        
        val transactions = listOf(
            Transaction(
                id = "trans1",
                accountId = "acc1",
                amount = Money.fromDollars(-50.0),
                description = "Grocery Store",
                category = Category.GROCERIES,
                date = today.minus(1, DateTimeUnit.DAY)
            ),
            Transaction(
                id = "trans2",
                accountId = "acc1",
                amount = Money.fromDollars(-25.0),
                description = "Coffee Shop",
                category = Category.RESTAURANTS,
                date = today.minus(2, DateTimeUnit.DAY)
            ),
            Transaction(
                id = "trans3",
                accountId = "acc1",
                amount = Money.fromDollars(-100.0),
                description = "Gas Station",
                category = Category.GAS,
                date = today.minus(3, DateTimeUnit.DAY)
            )
        )
        
        val goals = listOf(
            FinancialGoal(
                id = "goal1",
                userId = "user1",
                title = "Emergency Fund",
                targetAmount = Money.fromDollars(5000.0),
                currentAmount = Money.fromDollars(2000.0),
                targetDate = today.plus(6, DateTimeUnit.MONTH),
                priority = Priority.HIGH,
                goalType = GoalType.EMERGENCY_FUND
            )
        )
        
        val budgets = listOf(
            Budget(
                id = "budget1",
                userId = "user1",
                category = Category.GROCERIES,
                amount = Money.fromDollars(400.0),
                period = BudgetPeriod.MONTHLY,
                spent = Money.fromDollars(200.0),
                remaining = Money.fromDollars(200.0)
            ),
            Budget(
                id = "budget2",
                userId = "user1",
                category = Category.RESTAURANTS,
                amount = Money.fromDollars(200.0),
                period = BudgetPeriod.MONTHLY,
                spent = Money.fromDollars(150.0),
                remaining = Money.fromDollars(50.0)
            )
        )
        
        val spendingAnalysis = SpendingAnalysis(
            userId = "user1",
            period = DateRange(today.minus(30, DateTimeUnit.DAY), today),
            totalSpent = Money.fromDollars(1200.0),
            totalIncome = Money.fromDollars(4000.0),
            netCashFlow = Money.fromDollars(2800.0),
            categoryBreakdown = listOf(
                CategorySpending(
                    category = Category.GROCERIES,
                    totalAmount = Money.fromDollars(400.0),
                    transactionCount = 8,
                    averageAmount = Money.fromDollars(50.0),
                    percentageOfTotal = 33.3,
                    trend = TrendDirection.STABLE
                )
            ),
            trends = emptyList(),
            insights = emptyList(),
            comparisonToPrevious = null,
            generatedAt = Clock.System.now()
        )
        
        val gamificationProfile = GamificationProfile(
            level = 5,
            totalPoints = 2500,
            currentStreaks = listOf(
                Streak(
                    type = StreakType.DAILY_CHECK_IN,
                    currentCount = 7,
                    bestCount = 15,
                    lastUpdated = Clock.System.now(),
                    isActive = true
                )
            ),
            achievements = emptyList(),
            lastActivity = Clock.System.now()
        )
        
        return UserFinancialContext(
            userId = "user1",
            accounts = accounts,
            recentTransactions = transactions,
            goals = goals,
            budgets = budgets,
            userPreferences = UserPreferences(
                communicationStyle = CommunicationStyle.ENCOURAGING,
                riskTolerance = RiskTolerance.MODERATE,
                preferredCurrency = Currency.CAD,
                notificationPreferences = NotificationPreferences.IMPORTANT_ONLY,
                privacySettings = PrivacySettings.LIMITED_SHARING
            ),
            spendingAnalysis = spendingAnalysis,
            netWorth = null,
            gamificationProfile = gamificationProfile
        )
    }
    
    private fun createTestUserFinancialContextWithUnusualSpending(): UserFinancialContext {
        val context = createTestUserFinancialContext()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        // Add some unusual transactions
        val unusualTransactions = listOf(
            Transaction(
                id = "unusual1",
                accountId = "acc1",
                amount = Money.fromDollars(-500.0), // Large purchase
                description = "Electronics Store",
                category = Category.SHOPPING,
                date = today.minus(1, DateTimeUnit.DAY)
            ),
            Transaction(
                id = "unusual2",
                accountId = "acc1",
                amount = Money.fromDollars(-15.0),
                description = "Coffee Shop",
                category = Category.RESTAURANTS,
                date = today.minus(1, DateTimeUnit.DAY)
            ),
            Transaction(
                id = "unusual3",
                accountId = "acc1",
                amount = Money.fromDollars(-15.0),
                description = "Coffee Shop",
                category = Category.RESTAURANTS,
                date = today.minus(2, DateTimeUnit.DAY)
            ),
            Transaction(
                id = "unusual4",
                accountId = "acc1",
                amount = Money.fromDollars(-15.0),
                description = "Coffee Shop",
                category = Category.RESTAURANTS,
                date = today.minus(3, DateTimeUnit.DAY)
            ),
            Transaction(
                id = "unusual5",
                accountId = "acc1",
                amount = Money.fromDollars(-15.0),
                description = "Coffee Shop",
                category = Category.RESTAURANTS,
                date = today.minus(4, DateTimeUnit.DAY)
            ),
            Transaction(
                id = "unusual6",
                accountId = "acc1",
                amount = Money.fromDollars(-15.0),
                description = "Coffee Shop",
                category = Category.RESTAURANTS,
                date = today.minus(5, DateTimeUnit.DAY)
            ),
            Transaction(
                id = "unusual7",
                accountId = "acc1",
                amount = Money.fromDollars(-15.0),
                description = "Coffee Shop",
                category = Category.RESTAURANTS,
                date = today.minus(6, DateTimeUnit.DAY)
            )
        )
        
        return context.copy(
            recentTransactions = context.recentTransactions + unusualTransactions
        )
    }
}

// Mock implementations for testing

class MockFinancialAnalyticsService : FinancialAnalyticsService {
    override suspend fun generateSpendingAnalysis(userId: String, period: DateRange, includeComparison: Boolean): Result<SpendingAnalysis> {
        return Result.success(SpendingAnalysis(
            userId = userId,
            period = period,
            totalSpent = Money.fromDollars(1000.0),
            totalIncome = Money.fromDollars(4000.0),
            netCashFlow = Money.fromDollars(3000.0),
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
            totalAssets = Money.fromDollars(50000.0),
            totalLiabilities = Money.fromDollars(10000.0),
            netWorth = Money.fromDollars(40000.0),
            assetBreakdown = emptyList(),
            liabilityBreakdown = emptyList(),
            monthlyChange = null,
            yearlyChange = null,
            trend = TrendDirection.INCREASING,
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
            totalBudget = Money.fromDollars(2000.0),
            totalSpent = Money.fromDollars(1500.0),
            remainingBudget = Money.fromDollars(500.0),
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
            grossIncome = Money.fromDollars(60000.0),
            estimatedTaxes = TaxBreakdown(
                federalTax = Money.fromDollars(8000.0),
                provincialTax = Money.fromDollars(4000.0),
                cpp = Money.fromDollars(3000.0),
                ei = Money.fromDollars(1000.0),
                totalTax = Money.fromDollars(16000.0),
                afterTaxIncome = Money.fromDollars(44000.0),
                marginalTaxRate = 0.31,
                averageTaxRate = 0.27
            ),
            rrspContributions = RRSPAnalysis(
                currentContributions = Money.fromDollars(5000.0),
                contributionRoom = Money.fromDollars(10000.0),
                maxContribution = Money.fromDollars(15000.0),
                taxSavings = Money.fromDollars(1550.0),
                recommendedContribution = Money.fromDollars(8000.0),
                carryForwardRoom = Money.fromDollars(2000.0)
            ),
            tfsaContributions = TFSAAnalysis(
                currentContributions = Money.fromDollars(3000.0),
                contributionRoom = Money.fromDollars(6000.0),
                maxContribution = Money.fromDollars(9000.0),
                recommendedContribution = Money.fromDollars(6000.0),
                withdrawalRoom = Money.fromDollars(0.0)
            ),
            taxOptimizationRecommendations = emptyList()
        ))
    }
    
    override suspend fun calculateRRSPRecommendations(userId: String): Result<RRSPAnalysis> {
        return Result.success(RRSPAnalysis(
            currentContributions = Money.fromDollars(5000.0),
            contributionRoom = Money.fromDollars(10000.0),
            maxContribution = Money.fromDollars(15000.0),
            taxSavings = Money.fromDollars(1550.0),
            recommendedContribution = Money.fromDollars(8000.0),
            carryForwardRoom = Money.fromDollars(2000.0)
        ))
    }
    
    override suspend fun calculateTFSARecommendations(userId: String): Result<TFSAAnalysis> {
        return Result.success(TFSAAnalysis(
            currentContributions = Money.fromDollars(3000.0),
            contributionRoom = Money.fromDollars(6000.0),
            maxContribution = Money.fromDollars(9000.0),
            recommendedContribution = Money.fromDollars(6000.0),
            withdrawalRoom = Money.fromDollars(0.0)
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
            totalRecommendations = 10,
            completedRecommendations = 7,
            completionRate = 0.7,
            averageTimeToComplete = 5.0,
            totalPotentialSavings = Money.fromDollars(1000.0),
            actualizedSavings = Money.fromDollars(700.0),
            topPerformingTypes = emptyList()
        ))
    }
}

class MockRecommendationEngine : RecommendationEngine {
    override suspend fun generateFinancialPlanningRecommendations(userId: String, userProfile: UserFinancialProfile): Result<List<FinancialPlanningRecommendation>> {
        return Result.success(emptyList())
    }
    
    override suspend fun optimizeRRSPContributions(userProfile: UserFinancialProfile): Result<RRSPOptimizationRecommendation> {
        return Result.success(RRSPOptimizationRecommendation(
            recommendedContribution = Money.fromDollars(8000.0),
            currentContribution = Money.fromDollars(5000.0),
            availableRoom = Money.fromDollars(10000.0),
            taxSavings = Money.fromDollars(2480.0),
            optimalTiming = ContributionTiming(ContributionFrequency.MONTHLY, listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), "Spread contributions throughout the year"),
            reasoning = "Maximize tax savings while maintaining cash flow",
            impactOnGoals = emptyList(),
            alternativeStrategies = emptyList()
        ))
    }
    
    override suspend fun optimizeTFSAContributions(userProfile: UserFinancialProfile): Result<TFSAOptimizationRecommendation> {
        return Result.success(TFSAOptimizationRecommendation(
            recommendedContribution = Money.fromDollars(6000.0),
            currentContribution = Money.fromDollars(3000.0),
            availableRoom = Money.fromDollars(6000.0),
            optimalAllocation = emptyList(),
            reasoning = "Maximize tax-free growth potential",
            impactOnGoals = emptyList(),
            alternativeStrategies = emptyList()
        ))
    }
    
    override suspend fun optimizeDebtPayoff(userProfile: UserFinancialProfile): Result<DebtPayoffStrategy> {
        return Result.success(DebtPayoffStrategy(
            strategy = DebtPayoffMethod.AVALANCHE,
            payoffOrder = emptyList(),
            totalInterestSaved = Money.fromDollars(500.0),
            payoffTimeframe = 24,
            monthlyPaymentPlan = Money.fromDollars(300.0),
            reasoning = "Minimize total interest paid",
            alternativeStrategies = emptyList()
        ))
    }
    
    override suspend fun optimizeSavingsStrategy(userProfile: UserFinancialProfile): Result<SavingsOptimizationRecommendation> {
        return Result.success(SavingsOptimizationRecommendation(
            recommendedSavingsRate = 0.20,
            currentSavingsRate = 0.15,
            optimalAllocation = emptyList(),
            emergencyFundTarget = Money.fromDollars(15000.0),
            currentEmergencyFund = Money.fromDollars(5000.0),
            reasoning = "Build emergency fund and increase savings rate",
            impactOnGoals = emptyList()
        ))
    }
    
    override suspend fun trackRecommendationEffectiveness(recommendationId: String, outcome: RecommendationOutcome): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun getRecommendationExplanation(recommendationId: String): Result<RecommendationExplanation> {
        return Result.success(RecommendationExplanation(
            recommendationId = recommendationId,
            summary = "Test recommendation explanation",
            detailedReasoning = "Detailed reasoning for the recommendation",
            assumptions = emptyList(),
            calculations = emptyList(),
            sources = emptyList(),
            riskFactors = emptyList(),
            alternativeApproaches = emptyList()
        ))
    }
}

class MockGoalService : GoalService {
    override suspend fun createGoal(goal: FinancialGoal): Result<FinancialGoal> = Result.success(goal)
    override suspend fun updateGoal(goal: FinancialGoal): Result<FinancialGoal> = Result.success(goal)
    override suspend fun deleteGoal(goalId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getGoal(goalId: String): Result<FinancialGoal?> = Result.success(null)
    override suspend fun getUserGoals(userId: String): Result<List<FinancialGoal>> = Result.success(emptyList())
    override suspend fun getActiveGoals(userId: String): Result<List<FinancialGoal>> = Result.success(emptyList())
    override suspend fun updateGoalProgress(goalId: String, amount: Money): Result<FinancialGoal> = Result.failure(NotImplementedError())
    override suspend fun getGoalProgress(goalId: String): Result<com.north.mobile.data.goal.GoalProgress> = Result.failure(NotImplementedError())
    override suspend fun getGoalProjection(goalId: String): Result<com.north.mobile.data.goal.GoalProjection> = Result.failure(NotImplementedError())
    override suspend fun createMicroTask(goalId: String, microTask: MicroTask): Result<MicroTask> = Result.success(microTask)
    override suspend fun completeMicroTask(microTaskId: String): Result<MicroTask> = Result.failure(NotImplementedError())
    override suspend fun generateMicroTasks(goalId: String): Result<List<MicroTask>> = Result.success(emptyList())
    override suspend fun getMicroTasks(goalId: String): Result<List<MicroTask>> = Result.success(emptyList())
    override suspend fun detectGoalConflicts(userId: String): Result<List<com.north.mobile.data.goal.GoalConflict>> = Result.success(emptyList())
    override suspend fun prioritizeGoals(userId: String, goalIds: List<String>): Result<List<FinancialGoal>> = Result.success(emptyList())
    override suspend fun resolveGoalConflict(conflictId: String, resolution: com.north.mobile.data.goal.ConflictResolution): Result<Unit> = Result.success(Unit)
    override suspend fun checkGoalAchievements(userId: String): Result<List<com.north.mobile.data.goal.GoalAchievement>> = Result.success(emptyList())
    override suspend fun celebrateGoalAchievement(goalId: String): Result<com.north.mobile.data.goal.GoalCelebration> = Result.failure(NotImplementedError())
    override suspend fun getNextStepSuggestions(goalId: String): Result<List<com.north.mobile.data.goal.NextStepSuggestion>> = Result.success(emptyList())
    override suspend fun getGoalInsights(userId: String): Result<com.north.mobile.data.goal.GoalInsights> = Result.failure(NotImplementedError())
    override suspend fun getGoalRecommendations(userId: String): Result<List<com.north.mobile.data.goal.GoalRecommendation>> = Result.success(emptyList())
    override fun observeGoalProgress(goalId: String): kotlinx.coroutines.flow.Flow<com.north.mobile.data.goal.GoalProgress> = kotlinx.coroutines.flow.emptyFlow()
    override fun observeUserGoals(userId: String): kotlinx.coroutines.flow.Flow<List<FinancialGoal>> = kotlinx.coroutines.flow.emptyFlow()
}

class MockGamificationService : GamificationService {
    override suspend fun awardPoints(userId: String, action: UserAction, points: Int?, description: String?): Result<com.north.mobile.data.gamification.PointsResult> {
        return Result.success(com.north.mobile.data.gamification.PointsResult(
            pointsAwarded = points ?: 10,
            totalPoints = 1000,
            newLevel = 5,
            leveledUp = false
        ))
    }
    
    override suspend fun getGamificationProfile(userId: String): Result<GamificationProfile> {
        return Result.success(GamificationProfile(
            level = 5,
            totalPoints = 1000,
            currentStreaks = emptyList(),
            achievements = emptyList(),
            lastActivity = Clock.System.now()
        ))
    }
    
    override suspend fun updateStreak(userId: String, streakType: StreakType): Result<Streak> {
        return Result.success(Streak(
            type = streakType,
            currentCount = 5,
            bestCount = 10,
            lastUpdated = Clock.System.now(),
            isActive = true
        ))
    }
    
    override suspend fun checkLevelUp(userId: String): Result<com.north.mobile.data.gamification.LevelUpResult?> {
        return Result.success(null)
    }
    
    override suspend fun unlockAchievement(userId: String, achievementType: com.north.mobile.data.gamification.AchievementType): Result<Achievement> {
        return Result.success(Achievement(
            id = "ach1",
            type = achievementType,
            title = "Test Achievement",
            description = "Test achievement description",
            unlockedAt = Clock.System.now(),
            pointsAwarded = 100
        ))
    }
    
    override suspend fun getAvailableMicroWins(userId: String): Result<List<MicroWin>> {
        return Result.success(emptyList())
    }
    
    override suspend fun getPointsHistory(userId: String, limit: Int): Result<List<com.north.mobile.data.gamification.PointsHistoryEntry>> {
        return Result.success(emptyList())
    }
    
    override fun getPointsRequiredForNextLevel(currentLevel: Int): Int = currentLevel * 1000
    override fun getLevelFromPoints(totalPoints: Int): Int = totalPoints / 1000
}

class MockTransactionCategorizationService : TransactionCategorizationService {
    override suspend fun categorizeTransaction(transaction: Transaction): CategorizationResult {
        return CategorizationResult(
            transactionId = transaction.id,
            suggestedCategory = transaction.category,
            confidence = 0.9f
        )
    }
    
    override suspend fun categorizeTransactions(transactions: List<Transaction>): List<CategorizationResult> {
        return transactions.map { categorizeTransaction(it) }
    }
    
    override suspend fun provideFeedback(transactionId: String, correctCategory: Category, confidence: Float) {
        // Mock implementation
    }
    
    override suspend fun detectUnusualSpending(transactions: List<Transaction>): List<com.north.mobile.data.analytics.UnusualSpendingAlert> {
        return emptyList()
    }
    
    override suspend fun getCategorizationStats(): CategorizationStats {
        return CategorizationStats(
            totalTransactionsCategorized = 100,
            averageConfidence = 0.85f,
            userFeedbackCount = 10,
            accuracyRate = 0.90f,
            categoryDistribution = emptyMap(),
            lastModelUpdate = null
        )
    }
    
    override suspend fun retrainModels() {
        // Mock implementation
    }
}