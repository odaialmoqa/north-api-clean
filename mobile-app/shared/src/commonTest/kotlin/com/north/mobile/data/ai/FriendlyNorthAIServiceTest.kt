package com.north.mobile.data.ai

import com.north.mobile.domain.model.*
import com.north.mobile.data.analytics.*
import com.north.mobile.data.goal.GoalService
import com.north.mobile.data.gamification.GamificationService
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

class FriendlyNorthAIServiceTest {
    
    private lateinit var friendlyAIService: FriendlyNorthAIService
    private lateinit var mockBaseAIService: MockNorthAIService
    private lateinit var mockFinancialAnalyticsService: MockFinancialAnalyticsService
    private lateinit var mockGoalService: MockGoalService
    private lateinit var mockGamificationService: MockGamificationService
    
    @BeforeTest
    fun setup() {
        mockBaseAIService = MockNorthAIService()
        mockFinancialAnalyticsService = MockFinancialAnalyticsService()
        mockGoalService = MockGoalService()
        mockGamificationService = MockGamificationService()
        
        friendlyAIService = FriendlyNorthAIServiceImpl(
            baseAIService = mockBaseAIService,
            financialAnalyticsService = mockFinancialAnalyticsService,
            goalService = mockGoalService,
            gamificationService = mockGamificationService
        )
    }
    
    @Test
    fun `processUserQuery should return friendly response with warm tone`() = runTest {
        // Given
        val query = "Can I afford a $100 dinner?"
        val context = createTestUserContext()
        
        // When
        val result = friendlyAIService.processUserQuery(query, context)
        
        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertTrue(response.message.isNotEmpty())
        assertTrue(response.emojis.isNotEmpty())
        assertEquals(ConversationTone.GENTLE_GUIDANCE, response.tone)
        assertTrue(response.encouragementLevel != EncouragementLevel.MINIMAL)
    }
    
    @Test
    fun `generatePersonalizedInsights should return friendly insights with celebration`() = runTest {
        // Given
        val context = createTestUserContext()
        
        // When
        val result = friendlyAIService.generatePersonalizedInsights(context)
        
        // Then
        assertTrue(result.isSuccess)
        val insights = result.getOrThrow()
        assertTrue(insights.isNotEmpty())
        
        val firstInsight = insights.first()
        assertTrue(firstInsight.message.isNotEmpty())
        assertTrue(firstInsight.emojis.isNotEmpty())
        assertTrue(firstInsight.encouragingContext.isNotEmpty())
        assertEquals(ConversationTone.ENCOURAGING, firstInsight.tone)
    }
    
    @Test
    fun `generateConversationStarters should return personalized starters`() = runTest {
        // Given
        val context = createTestUserContextWithGoals()
        
        // When
        val result = friendlyAIService.generateConversationStarters(context)
        
        // Then
        assertTrue(result.isSuccess)
        val starters = result.getOrThrow()
        assertTrue(starters.isNotEmpty())
        assertTrue(starters.size <= 3)
        
        val goalStarter = starters.find { it.category == ConversationCategory.GOAL_PROGRESS }
        assertNotNull(goalStarter)
        assertTrue(goalStarter.text.contains("goal"))
        assertTrue(goalStarter.emoji.isNotEmpty())
    }
    
    @Test
    fun `celebrateAchievement should return enthusiastic celebration message`() = runTest {
        // Given
        val achievement = Achievement(
            id = "test_achievement",
            type = AchievementType.GOAL_MILESTONE,
            title = "First Savings Goal",
            description = "Reached $1000 in savings",
            dateAchieved = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            value = Money.fromDollars(1000.0),
            category = null
        )
        val context = createTestUserContext()
        
        // When
        val result = friendlyAIService.celebrateAchievement(achievement, context)
        
        // Then
        assertTrue(result.isSuccess)
        val celebration = result.getOrThrow()
        assertTrue(celebration.message.contains("🎉") || celebration.message.contains("Incredible"))
        assertEquals(ConversationTone.CELEBRATORY, celebration.tone)
        assertTrue(celebration.emojis.isNotEmpty())
        assertTrue(celebration.celebrationElements.isNotEmpty())
        assertEquals(CelebrationType.MILESTONE_CELEBRATION, celebration.enthusiasmLevel)
    }
    
    @Test
    fun `checkAffordability should provide encouraging guidance when affordable`() = runTest {
        // Given
        val expense = ExpenseRequest(
            description = "New laptop",
            amount = Money.fromDollars(800.0),
            category = Category.ELECTRONICS,
            plannedDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
        )
        val context = createTestUserContext()
        
        // Configure mock to return affordable result
        mockBaseAIService.setAffordabilityResult(true, 0.9f)
        
        // When
        val result = friendlyAIService.checkAffordability(expense, context)
        
        // Then
        assertTrue(result.isSuccess)
        val affordabilityResult = result.getOrThrow()
        assertTrue(affordabilityResult.canAfford)
        assertTrue(affordabilityResult.encouragingMessage.contains("Great") || 
                  affordabilityResult.encouragingMessage.contains("afford"))
        assertEquals(CelebrationType.ENTHUSIASTIC, affordabilityResult.celebrationLevel)
        assertTrue(affordabilityResult.emojis.contains("🎉") || 
                  affordabilityResult.emojis.contains("✅"))
    }
    
    @Test
    fun `checkAffordability should provide supportive guidance when not affordable`() = runTest {
        // Given
        val expense = ExpenseRequest(
            description = "Expensive vacation",
            amount = Money.fromDollars(5000.0),
            category = Category.TRAVEL,
            plannedDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
        )
        val context = createTestUserContext()
        
        // Configure mock to return not affordable result
        mockBaseAIService.setAffordabilityResult(false, 0.8f)
        
        // When
        val result = friendlyAIService.checkAffordability(expense, context)
        
        // Then
        assertTrue(result.isSuccess)
        val affordabilityResult = result.getOrThrow()
        assertFalse(affordabilityResult.canAfford)
        assertEquals(ConversationTone.SUPPORTIVE, affordabilityResult.tone)
        assertTrue(affordabilityResult.supportiveReasoning.isNotEmpty())
        assertTrue(affordabilityResult.alternativeOptions.isNotEmpty())
    }
    
    @Test
    fun `explainTransaction should create engaging detective story`() = runTest {
        // Given
        val transactionId = "test_transaction_123"
        val context = createTestUserContextWithTransactions()
        
        // When
        val result = friendlyAIService.explainTransaction(transactionId, context)
        
        // Then
        assertTrue(result.isSuccess)
        val explanation = result.getOrThrow()
        assertTrue(explanation.detectiveStory.isNotEmpty())
        assertTrue(explanation.positiveSpins.isNotEmpty())
        assertTrue(explanation.encouragingConclusion.isNotEmpty())
        assertTrue(explanation.emojis.contains("🔍"))
    }
    
    // Helper methods for creating test data
    
    private fun createTestUserContext(): UserFinancialContext {
        return UserFinancialContext(
            userId = "test_user",
            accounts = listOf(
                Account(
                    id = "account_1",
                    userId = "test_user",
                    institutionId = "bank_1",
                    name = "Checking Account",
                    type = AccountType.CHECKING,
                    balance = Money.fromDollars(2500.0),
                    currency = Currency.CAD,
                    isActive = true,
                    lastSyncedAt = Clock.System.now()
                )
            ),
            recentTransactions = emptyList(),
            goals = emptyList(),
            budgets = emptyList(),
            userPreferences = UserPreferences(
                communicationStyle = CommunicationStyle.ENCOURAGING,
                riskTolerance = RiskTolerance.MODERATE,
                preferredCurrency = Currency.CAD,
                notificationPreferences = NotificationPreferences.IMPORTANT_ONLY,
                privacySettings = PrivacySettings.LIMITED_SHARING
            ),
            spendingAnalysis = null,
            netWorth = null,
            gamificationProfile = null
        )
    }
    
    private fun createTestUserContextWithGoals(): UserFinancialContext {
        val baseContext = createTestUserContext()
        return baseContext.copy(
            goals = listOf(
                FinancialGoal(
                    id = "goal_1",
                    userId = "test_user",
                    title = "Emergency Fund",
                    description = "Build emergency fund",
                    targetAmount = Money.fromDollars(5000.0),
                    currentAmount = Money.fromDollars(2000.0),
                    targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(365, DateTimeUnit.DAY),
                    goalType = GoalType.EMERGENCY_FUND,
                    isActive = true,
                    createdAt = Clock.System.now()
                )
            )
        )
    }
    
    private fun createTestUserContextWithTransactions(): UserFinancialContext {
        val baseContext = createTestUserContext()
        return baseContext.copy(
            recentTransactions = listOf(
                Transaction(
                    id = "test_transaction_123",
                    accountId = "account_1",
                    amount = Money.fromDollars(-85.50),
                    description = "GROCERY STORE PURCHASE",
                    date = Clock.System.todayIn(TimeZone.currentSystemDefault()),
                    category = Category.GROCERIES,
                    merchant = "Local Grocery Store",
                    isRecurring = false,
                    tags = emptyList()
                )
            )
        )
    }
}

// Mock implementations for testing

private class MockNorthAIService : NorthAIService {
    private var affordabilityResult: Pair<Boolean, Float> = Pair(true, 0.8f)
    
    fun setAffordabilityResult(canAfford: Boolean, confidence: Float) {
        affordabilityResult = Pair(canAfford, confidence)
    }
    
    override suspend fun processUserQuery(query: String, context: UserFinancialContext): Result<AIResponse> {
        return Result.success(AIResponse(
            message = "Test response for: $query",
            confidence = 0.8f,
            supportingData = emptyList(),
            actionableRecommendations = emptyList(),
            followUpQuestions = listOf("What else would you like to know?")
        ))
    }
    
    override suspend fun generatePersonalizedInsights(context: UserFinancialContext): Result<List<AIInsight>> {
        return Result.success(listOf(
            AIInsight(
                id = "insight_1",
                type = AIInsightType.POSITIVE_BEHAVIOR,
                title = "Great Spending Habits",
                description = "You're doing well with your budget",
                confidence = 0.9f,
                impact = InsightImpact.POSITIVE,
                category = null,
                actionableSteps = listOf("Keep up the good work"),
                potentialSavings = Money.fromDollars(50.0),
                timeframe = "This month",
                supportingData = emptyList()
            )
        ))
    }
    
    override suspend fun checkAffordability(expense: ExpenseRequest, context: UserFinancialContext): Result<AffordabilityResult> {
        return Result.success(AffordabilityResult(
            canAfford = affordabilityResult.first,
            confidence = affordabilityResult.second,
            impactOnGoals = GoalImpactAnalysis(
                affectedGoals = emptyList(),
                overallImpact = GoalImpactSeverity.MINIMAL,
                delayInDays = 0,
                alternativeStrategies = emptyList()
            ),
            impactOnBudget = BudgetImpactAnalysis(
                categoryImpact = CategoryBudgetImpact(
                    category = expense.category,
                    budgetRemaining = Money.fromDollars(500.0),
                    wouldExceedBudget = false,
                    exceedAmount = null
                ),
                overallBudgetImpact = Money.fromDollars(0.0),
                remainingBudget = Money.fromDollars(1000.0),
                projectedOverspend = null,
                recommendations = emptyList()
            ),
            alternativeOptions = listOf(
                Alternative(
                    description = "Consider a less expensive option",
                    amount = expense.amount * 0.7,
                    pros = listOf("More affordable"),
                    cons = listOf("Less features"),
                    feasibility = 0.8f
                )
            ),
            reasoning = if (affordabilityResult.first) "You have sufficient funds" else "This would strain your budget",
            recommendations = emptyList(),
            riskFactors = emptyList()
        ))
    }
    
    override suspend fun explainTransaction(transactionId: String, context: UserFinancialContext): Result<TransactionExplanation> {
        return Result.success(TransactionExplanation(
            transactionId = transactionId,
            summary = "This was a grocery store purchase",
            categoryExplanation = "Categorized as groceries",
            spendingPatternContext = "Normal spending pattern",
            budgetImpact = "Within budget",
            goalImpact = "No impact on goals",
            unusualFactors = emptyList(),
            relatedTransactions = emptyList(),
            recommendations = listOf("Keep tracking your grocery spending")
        ))
    }
    
    // Implement remaining methods with basic mock responses
    override suspend fun analyzeSpendingPattern(category: String, timeframe: DateRange, context: UserFinancialContext): Result<SpendingAnalysis> = 
        Result.success(createMockSpendingAnalysis())
    
    override suspend fun suggestOptimizations(context: UserFinancialContext): Result<List<OptimizationSuggestion>> = 
        Result.success(emptyList())
    
    override suspend fun generateFollowUpQuestions(context: UserFinancialContext, previousQuery: String?): Result<List<String>> = 
        Result.success(listOf("What else can I help with?"))
    
    override suspend fun analyzeUnusualSpending(context: UserFinancialContext, timeframe: DateRange): Result<List<UnusualSpendingAlert>> = 
        Result.success(emptyList())
    
    private fun createMockSpendingAnalysis(): SpendingAnalysis {
        return SpendingAnalysis(
            userId = "test_user",
            period = DateRange(
                Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(30, DateTimeUnit.DAY),
                Clock.System.todayIn(TimeZone.currentSystemDefault())
            ),
            totalSpent = Money.fromDollars(1500.0),
            totalIncome = Money.fromDollars(3000.0),
            netCashFlow = Money.fromDollars(1500.0),
            categoryBreakdown = emptyList(),
            trends = emptyList(),
            insights = emptyList(),
            comparisonToPrevious = null,
            generatedAt = Clock.System.now()
        )
    }
}

private class MockFinancialAnalyticsService : FinancialAnalyticsService {
    override suspend fun analyzeSpending(userId: String, timeframe: DateRange): Result<SpendingAnalysis> = 
        Result.success(SpendingAnalysis(
            userId = userId,
            period = timeframe,
            totalSpent = Money.fromDollars(1000.0),
            totalIncome = Money.fromDollars(2000.0),
            netCashFlow = Money.fromDollars(1000.0),
            categoryBreakdown = emptyList(),
            trends = emptyList(),
            insights = emptyList(),
            comparisonToPrevious = null,
            generatedAt = Clock.System.now()
        ))
    
    override suspend fun generateInsights(userId: String): Result<List<FinancialInsight>> = Result.success(emptyList())
    override suspend fun calculateNetWorth(userId: String): Result<NetWorthSummary> = Result.success(NetWorthSummary(Money.fromDollars(10000.0), emptyList(), Clock.System.now()))
    override suspend fun analyzeGoalProgress(userId: String): Result<List<GoalProgressAnalysis>> = Result.success(emptyList())
    override suspend fun generateRecommendations(userId: String): Result<List<FinancialRecommendation>> = Result.success(emptyList())
}

private class MockGoalService : GoalService {
    override suspend fun createGoal(goal: FinancialGoal): Result<FinancialGoal> = Result.success(goal)
    override suspend fun updateGoal(goal: FinancialGoal): Result<FinancialGoal> = Result.success(goal)
    override suspend fun deleteGoal(goalId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getGoal(goalId: String): Result<FinancialGoal?> = Result.success(null)
    override suspend fun getUserGoals(userId: String): Result<List<FinancialGoal>> = Result.success(emptyList())
    override suspend fun updateGoalProgress(goalId: String, amount: Money): Result<FinancialGoal> = Result.success(
        FinancialGoal(
            id = goalId,
            userId = "test",
            title = "Test Goal",
            description = "Test",
            targetAmount = Money.fromDollars(1000.0),
            currentAmount = amount,
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            goalType = GoalType.GENERAL_SAVINGS,
            isActive = true,
            createdAt = Clock.System.now()
        )
    )
}

private class MockGamificationService : GamificationService {
    override suspend fun getUserProfile(userId: String): Result<GamificationProfile> = Result.success(
        GamificationProfile(
            userId = userId,
            level = 1,
            totalPoints = 100,
            currentStreak = 5,
            longestStreak = 10,
            achievements = emptyList(),
            badges = emptyList(),
            weeklyGoalsMet = 2,
            monthlyGoalsMet = 8
        )
    )
    
    override suspend fun awardPoints(userId: String, points: Int, reason: String): Result<GamificationProfile> = getUserProfile(userId)
    override suspend fun updateStreak(userId: String, action: String): Result<StreakUpdate> = Result.success(
        StreakUpdate(
            userId = userId,
            currentStreak = 6,
            isNewRecord = false,
            pointsAwarded = 10,
            achievementsUnlocked = emptyList()
        )
    )
    override suspend fun checkAchievements(userId: String): Result<List<Achievement>> = Result.success(emptyList())
    override suspend fun celebrateAchievement(userId: String, achievementId: String): Result<CelebrationResponse> = Result.success(
        CelebrationResponse(
            message = "Congratulations!",
            pointsAwarded = 50,
            badgeUnlocked = null,
            nextMilestone = "Keep going!"
        )
    )
}