package com.north.mobile.integration

import com.north.mobile.data.ai.*
import com.north.mobile.domain.model.*
import com.north.mobile.data.analytics.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

/**
 * Integration test for North AI conversational interface
 * Tests the complete AI service functionality with realistic data
 */
class NorthAIIntegrationTest {
    
    @Test
    fun `AI service should process affordability queries correctly`() = runTest {
        // Given
        val queryProcessor = QueryProcessor()
        val query = "Can I afford a $500 weekend trip?"
        
        // When
        val processedQuery = queryProcessor.processQuery(query)
        
        // Then
        assertEquals(QueryIntent.AFFORDABILITY_CHECK, processedQuery.intent)
        assertEquals(Money.fromDollars(500.0), processedQuery.extractedAmount)
        assertTrue(processedQuery.confidence > 0.5f)
    }
    
    @Test
    fun `AI service should extract spending analysis intent`() = runTest {
        // Given
        val queryProcessor = QueryProcessor()
        val query = "How much did I spend on groceries last month?"
        
        // When
        val processedQuery = queryProcessor.processQuery(query)
        
        // Then
        assertEquals(QueryIntent.SPENDING_ANALYSIS, processedQuery.intent)
        assertEquals(Category.GROCERIES, processedQuery.extractedCategory)
        assertNotNull(processedQuery.extractedTimeframe)
        assertTrue(processedQuery.confidence > 0.5f)
    }
    
    @Test
    fun `AI service should generate personalized insights`() = runTest {
        // Given
        val insightGenerator = InsightGenerator()
        val context = createTestContext()
        
        // When
        val spendingInsights = insightGenerator.generateSpendingInsights(
            context.spendingAnalysis!!,
            context
        )
        val goalInsights = insightGenerator.generateGoalInsights(context.goals, context)
        val budgetInsights = insightGenerator.generateBudgetInsights(context.budgets, context)
        
        // Then
        assertTrue(spendingInsights.isNotEmpty() || goalInsights.isNotEmpty() || budgetInsights.isNotEmpty())
        
        val allInsights = spendingInsights + goalInsights + budgetInsights
        allInsights.forEach { insight ->
            assertTrue(insight.title.isNotBlank())
            assertTrue(insight.description.isNotBlank())
            assertTrue(insight.confidence > 0.0f)
            assertTrue(insight.actionableSteps.isNotEmpty())
        }
    }
    
    @Test
    fun `AI service should analyze affordability correctly`() = runTest {
        // Given
        val affordabilityAnalyzer = AffordabilityAnalyzer()
        val expense = ExpenseRequest(
            description = "New laptop",
            amount = Money.fromDollars(1200.0),
            category = Category.SHOPPING,
            plannedDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
        )
        val context = createTestContext()
        
        // When
        val result = affordabilityAnalyzer.analyzeAffordability(expense, context)
        
        // Then
        assertTrue(result.confidence > 0.0f)
        assertTrue(result.reasoning.isNotBlank())
        assertNotNull(result.impactOnGoals)
        assertNotNull(result.impactOnBudget)
        assertTrue(result.recommendations.isNotEmpty())
        assertTrue(result.alternativeOptions.isNotEmpty())
    }
    
    @Test
    fun `AI service should explain transactions with context`() = runTest {
        // Given
        val transactionExplainer = TransactionExplainer()
        val context = createTestContext()
        val transaction = context.recentTransactions.first()
        
        // When
        val explanation = transactionExplainer.explainTransaction(transaction, context)
        
        // Then
        assertEquals(transaction.id, explanation.transactionId)
        assertTrue(explanation.summary.isNotBlank())
        assertTrue(explanation.categoryExplanation.isNotBlank())
        assertTrue(explanation.spendingPatternContext.isNotBlank())
        assertTrue(explanation.budgetImpact.isNotBlank())
        assertTrue(explanation.goalImpact.isNotBlank())
        assertTrue(explanation.recommendations.isNotEmpty())
    }
    
    @Test
    fun `Query processor should extract amounts correctly`() = runTest {
        // Given
        val queryProcessor = QueryProcessor()
        val testCases = mapOf(
            "Can I afford $1,234.56?" to Money.fromDollars(1234.56),
            "I want to spend 500 dollars" to Money.fromDollars(500.0),
            "Is 50 affordable?" to Money.fromDollars(50.0)
        )
        
        // When & Then
        testCases.forEach { (query, expectedAmount) ->
            val processedQuery = queryProcessor.processQuery(query)
            assertEquals(expectedAmount, processedQuery.extractedAmount, "Failed for query: $query")
        }
    }
    
    @Test
    fun `Query processor should extract categories correctly`() = runTest {
        // Given
        val queryProcessor = QueryProcessor()
        val testCases = mapOf(
            "How much did I spend on food?" to Category.FOOD,
            "My grocery spending this month" to Category.GROCERIES,
            "Restaurant expenses" to Category.RESTAURANTS,
            "Transportation costs" to Category.TRANSPORT,
            "Gas spending" to Category.GAS
        )
        
        // When & Then
        testCases.forEach { (query, expectedCategory) ->
            val processedQuery = queryProcessor.processQuery(query)
            assertEquals(expectedCategory, processedQuery.extractedCategory, "Failed for query: $query")
        }
    }
    
    @Test
    fun `Query processor should extract timeframes correctly`() = runTest {
        // Given
        val queryProcessor = QueryProcessor()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        val testCases = listOf(
            "spending today",
            "expenses this week",
            "last month's budget",
            "last 30 days spending"
        )
        
        // When & Then
        testCases.forEach { query ->
            val processedQuery = queryProcessor.processQuery(query)
            assertNotNull(processedQuery.extractedTimeframe, "Failed to extract timeframe for: $query")
        }
    }
    
    @Test
    fun `Insight generator should create goal insights for users without goals`() = runTest {
        // Given
        val insightGenerator = InsightGenerator()
        val contextWithoutGoals = createTestContext().copy(goals = emptyList())
        
        // When
        val insights = insightGenerator.generateGoalInsights(emptyList(), contextWithoutGoals)
        
        // Then
        assertTrue(insights.isNotEmpty())
        val noGoalsInsight = insights.find { it.id == "no_goals" }
        assertNotNull(noGoalsInsight)
        assertEquals(AIInsightType.GOAL_PROGRESS, noGoalsInsight.type)
        assertTrue(noGoalsInsight.actionableSteps.isNotEmpty())
    }
    
    @Test
    fun `Insight generator should create budget insights for users without budgets`() = runTest {
        // Given
        val insightGenerator = InsightGenerator()
        val contextWithoutBudgets = createTestContext().copy(budgets = emptyList())
        
        // When
        val insights = insightGenerator.generateBudgetInsights(emptyList(), contextWithoutBudgets)
        
        // Then
        assertTrue(insights.isNotEmpty())
        val noBudgetsInsight = insights.find { it.id == "no_budgets" }
        assertNotNull(noBudgetsInsight)
        assertEquals(AIInsightType.BUDGET_ALERT, noBudgetsInsight.type)
        assertTrue(noBudgetsInsight.actionableSteps.isNotEmpty())
        assertNotNull(noBudgetsInsight.potentialSavings)
    }
    
    @Test
    fun `Affordability analyzer should handle recurring expenses`() = runTest {
        // Given
        val affordabilityAnalyzer = AffordabilityAnalyzer()
        val recurringExpense = ExpenseRequest(
            description = "Netflix subscription",
            amount = Money.fromDollars(15.99),
            category = Category.ENTERTAINMENT,
            isRecurring = true,
            frequency = RecurringFrequency.MONTHLY,
            plannedDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
        )
        val context = createTestContext()
        
        // When
        val result = affordabilityAnalyzer.analyzeAffordability(recurringExpense, context)
        
        // Then
        assertTrue(result.confidence > 0.0f)
        assertTrue(result.reasoning.contains("recurring") || result.reasoning.contains("monthly"))
        assertTrue(result.recommendations.isNotEmpty())
    }
    
    private fun createTestContext(): UserFinancialContext {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        return UserFinancialContext(
            userId = "test_user",
            accounts = listOf(
                Account(
                    id = "acc1",
                    institutionId = "inst1",
                    institutionName = "Test Bank",
                    accountType = AccountType.CHECKING,
                    balance = Money.fromDollars(3000.0),
                    lastUpdated = Clock.System.now()
                ),
                Account(
                    id = "acc2",
                    institutionId = "inst1",
                    institutionName = "Test Bank",
                    accountType = AccountType.SAVINGS,
                    balance = Money.fromDollars(15000.0),
                    lastUpdated = Clock.System.now()
                )
            ),
            recentTransactions = listOf(
                Transaction(
                    id = "trans1",
                    accountId = "acc1",
                    amount = Money.fromDollars(-75.50),
                    description = "Grocery Store",
                    category = Category.GROCERIES,
                    date = today.minus(1, DateTimeUnit.DAY),
                    merchantName = "Metro"
                ),
                Transaction(
                    id = "trans2",
                    accountId = "acc1",
                    amount = Money.fromDollars(-45.00),
                    description = "Restaurant",
                    category = Category.RESTAURANTS,
                    date = today.minus(2, DateTimeUnit.DAY),
                    merchantName = "The Keg"
                ),
                Transaction(
                    id = "trans3",
                    accountId = "acc1",
                    amount = Money.fromDollars(-85.00),
                    description = "Gas Station",
                    category = Category.GAS,
                    date = today.minus(3, DateTimeUnit.DAY),
                    merchantName = "Petro-Canada"
                )
            ),
            goals = listOf(
                FinancialGoal(
                    id = "goal1",
                    userId = "test_user",
                    title = "Emergency Fund",
                    targetAmount = Money.fromDollars(10000.0),
                    currentAmount = Money.fromDollars(4000.0),
                    targetDate = today.plus(8, DateTimeUnit.MONTH),
                    priority = Priority.HIGH,
                    goalType = GoalType.EMERGENCY_FUND
                ),
                FinancialGoal(
                    id = "goal2",
                    userId = "test_user",
                    title = "Vacation Fund",
                    targetAmount = Money.fromDollars(3000.0),
                    currentAmount = Money.fromDollars(800.0),
                    targetDate = today.plus(6, DateTimeUnit.MONTH),
                    priority = Priority.MEDIUM,
                    goalType = GoalType.VACATION
                )
            ),
            budgets = listOf(
                Budget(
                    id = "budget1",
                    userId = "test_user",
                    category = Category.GROCERIES,
                    amount = Money.fromDollars(500.0),
                    period = BudgetPeriod.MONTHLY,
                    spent = Money.fromDollars(300.0),
                    remaining = Money.fromDollars(200.0)
                ),
                Budget(
                    id = "budget2",
                    userId = "test_user",
                    category = Category.RESTAURANTS,
                    amount = Money.fromDollars(300.0),
                    period = BudgetPeriod.MONTHLY,
                    spent = Money.fromDollars(280.0),
                    remaining = Money.fromDollars(20.0)
                ),
                Budget(
                    id = "budget3",
                    userId = "test_user",
                    category = Category.ENTERTAINMENT,
                    amount = Money.fromDollars(150.0),
                    period = BudgetPeriod.MONTHLY,
                    spent = Money.fromDollars(50.0),
                    remaining = Money.fromDollars(100.0)
                )
            ),
            userPreferences = UserPreferences(
                communicationStyle = CommunicationStyle.ENCOURAGING,
                riskTolerance = RiskTolerance.MODERATE,
                preferredCurrency = Currency.CAD,
                notificationPreferences = NotificationPreferences.IMPORTANT_ONLY,
                privacySettings = PrivacySettings.LIMITED_SHARING
            ),
            spendingAnalysis = SpendingAnalysis(
                userId = "test_user",
                period = DateRange(today.minus(30, DateTimeUnit.DAY), today),
                totalSpent = Money.fromDollars(1800.0),
                totalIncome = Money.fromDollars(5000.0),
                netCashFlow = Money.fromDollars(3200.0),
                categoryBreakdown = listOf(
                    CategorySpending(
                        category = Category.GROCERIES,
                        totalAmount = Money.fromDollars(600.0),
                        transactionCount = 12,
                        averageAmount = Money.fromDollars(50.0),
                        percentageOfTotal = 33.3,
                        trend = TrendDirection.STABLE,
                        comparedToPrevious = Money.fromDollars(-20.0)
                    ),
                    CategorySpending(
                        category = Category.RESTAURANTS,
                        totalAmount = Money.fromDollars(400.0),
                        transactionCount = 8,
                        averageAmount = Money.fromDollars(50.0),
                        percentageOfTotal = 22.2,
                        trend = TrendDirection.INCREASING,
                        comparedToPrevious = Money.fromDollars(50.0)
                    )
                ),
                trends = emptyList(),
                insights = listOf(
                    SpendingInsight(
                        id = "insight1",
                        type = InsightType.POSITIVE_BEHAVIOR,
                        title = "Great Grocery Savings!",
                        description = "You reduced grocery spending by $20 this month",
                        impact = InsightImpact.MEDIUM,
                        actionableRecommendations = listOf("Keep up the good work"),
                        potentialSavings = Money.fromDollars(20.0),
                        category = Category.GROCERIES,
                        confidence = 0.9f
                    )
                ),
                comparisonToPrevious = PeriodComparison(
                    currentPeriod = DateRange(today.minus(30, DateTimeUnit.DAY), today),
                    previousPeriod = DateRange(today.minus(60, DateTimeUnit.DAY), today.minus(30, DateTimeUnit.DAY)),
                    totalSpentChange = Money.fromDollars(30.0),
                    totalSpentChangePercentage = 1.7,
                    categoryChanges = mapOf("groceries" to Money.fromDollars(-20.0)),
                    significantChanges = listOf("Reduced grocery spending")
                ),
                generatedAt = Clock.System.now()
            ),
            netWorth = NetWorthSummary(
                userId = "test_user",
                calculatedAt = Clock.System.now(),
                totalAssets = Money.fromDollars(18000.0),
                totalLiabilities = Money.fromDollars(2000.0),
                netWorth = Money.fromDollars(16000.0),
                assetBreakdown = emptyList(),
                liabilityBreakdown = emptyList(),
                monthlyChange = Money.fromDollars(500.0),
                yearlyChange = Money.fromDollars(6000.0),
                trend = TrendDirection.INCREASING,
                projectedNetWorth = emptyList()
            ),
            gamificationProfile = GamificationProfile(
                level = 7,
                totalPoints = 3500,
                currentStreaks = listOf(
                    Streak(
                        id = "streak1",
                        type = StreakType.DAILY_CHECK_IN,
                        currentCount = 12,
                        bestCount = 20,
                        lastActivityDate = today,
                        isActive = true
                    ),
                    Streak(
                        id = "streak2",
                        type = StreakType.WEEKLY_BUDGET_ADHERENCE,
                        currentCount = 5,
                        bestCount = 8,
                        lastActivityDate = today,
                        isActive = true
                    )
                ),
                achievements = listOf(
                    Achievement(
                        id = "ach1",
                        title = "Goal Setter",
                        description = "Created your first financial goal",
                        badgeIcon = "🎯",
                        pointsAwarded = 100,
                        unlockedAt = Clock.System.now().minus(30, DateTimeUnit.DAY),
                        category = AchievementCategory.GOAL_ACHIEVEMENT
                    )
                ),
                lastActivity = Clock.System.now()
            )
        )
    }
}