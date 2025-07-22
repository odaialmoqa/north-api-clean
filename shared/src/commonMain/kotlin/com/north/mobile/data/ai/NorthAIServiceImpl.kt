package com.north.mobile.data.ai

import com.north.mobile.domain.model.*
import com.north.mobile.data.analytics.*
import com.north.mobile.data.goal.GoalService
import com.north.mobile.data.gamification.GamificationService
import kotlinx.datetime.*
import kotlin.math.*

/**
 * Implementation of North AI conversational interface
 * Provides intelligent financial assistance through natural language processing
 */
class NorthAIServiceImpl(
    private val financialAnalyticsService: FinancialAnalyticsService,
    private val recommendationEngine: RecommendationEngine,
    private val goalService: GoalService,
    private val gamificationService: GamificationService,
    private val transactionCategorizationService: TransactionCategorizationService
) : NorthAIService {
    
    private val queryProcessor = QueryProcessor()
    private val insightGenerator = InsightGenerator()
    private val affordabilityAnalyzer = AffordabilityAnalyzer()
    private val transactionExplainer = TransactionExplainer()
    
    override suspend fun processUserQuery(
        query: String,
        context: UserFinancialContext
    ): Result<AIResponse> {
        return try {
            val processedQuery = queryProcessor.processQuery(query)
            val response = when (processedQuery.intent) {
                QueryIntent.AFFORDABILITY_CHECK -> handleAffordabilityQuery(processedQuery, context)
                QueryIntent.SPENDING_ANALYSIS -> handleSpendingAnalysisQuery(processedQuery, context)
                QueryIntent.GOAL_PROGRESS -> handleGoalProgressQuery(processedQuery, context)
                QueryIntent.BUDGET_STATUS -> handleBudgetStatusQuery(processedQuery, context)
                QueryIntent.TRANSACTION_EXPLANATION -> handleTransactionExplanationQuery(processedQuery, context)
                QueryIntent.SAVINGS_ADVICE -> handleSavingsAdviceQuery(processedQuery, context)
                QueryIntent.GENERAL_INSIGHT -> handleGeneralInsightQuery(processedQuery, context)
                QueryIntent.OPTIMIZATION_SUGGESTION -> handleOptimizationQuery(processedQuery, context)
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun generatePersonalizedInsights(
        context: UserFinancialContext
    ): Result<List<AIInsight>> {
        return try {
            val insights = mutableListOf<AIInsight>()
            
            // Generate spending insights
            context.spendingAnalysis?.let { analysis ->
                insights.addAll(insightGenerator.generateSpendingInsights(analysis, context))
            }
            
            // Generate goal insights
            insights.addAll(insightGenerator.generateGoalInsights(context.goals, context))
            
            // Generate budget insights
            insights.addAll(insightGenerator.generateBudgetInsights(context.budgets, context))
            
            // Generate gamification insights
            context.gamificationProfile?.let { profile ->
                insights.addAll(insightGenerator.generateGamificationInsights(profile, context))
            }
            
            // Sort by impact and confidence
            val sortedInsights = insights.sortedWith(
                compareByDescending<AIInsight> { it.impact.ordinal }
                    .thenByDescending { it.confidence }
            ).take(10) // Limit to top 10 insights
            
            Result.success(sortedInsights)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun analyzeSpendingPattern(
        category: String,
        timeframe: DateRange,
        context: UserFinancialContext
    ): Result<SpendingAnalysis> {
        return try {
            val categoryTransactions = context.recentTransactions.filter { 
                it.category.name.equals(category, ignoreCase = true) &&
                it.date >= timeframe.startDate && it.date <= timeframe.endDate
            }
            
            val totalSpent = categoryTransactions.fold(Money.zero()) { acc, transaction ->
                acc + transaction.amount.absoluteValue
            }
            
            val analysis = SpendingAnalysis(
                userId = context.userId,
                period = timeframe,
                totalSpent = totalSpent,
                totalIncome = Money.zero(), // Would need income data
                netCashFlow = totalSpent * -1,
                categoryBreakdown = listOf(
                    CategorySpending(
                        category = Category(category, category),
                        totalAmount = totalSpent,
                        transactionCount = categoryTransactions.size,
                        averageAmount = if (categoryTransactions.isNotEmpty()) 
                            totalSpent / categoryTransactions.size.toDouble() else Money.zero(),
                        percentageOfTotal = 100.0,
                        trend = calculateTrend(categoryTransactions)
                    )
                ),
                trends = emptyList(),
                insights = emptyList(),
                comparisonToPrevious = null,
                generatedAt = Clock.System.now()
            )
            
            Result.success(analysis)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun checkAffordability(
        expense: ExpenseRequest,
        context: UserFinancialContext
    ): Result<AffordabilityResult> {
        return try {
            val result = affordabilityAnalyzer.analyzeAffordability(expense, context)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun explainTransaction(
        transactionId: String,
        context: UserFinancialContext
    ): Result<TransactionExplanation> {
        return try {
            val transaction = context.recentTransactions.find { it.id == transactionId }
                ?: return Result.failure(IllegalArgumentException("Transaction not found"))
            
            val explanation = transactionExplainer.explainTransaction(transaction, context)
            Result.success(explanation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun suggestOptimizations(
        context: UserFinancialContext
    ): Result<List<OptimizationSuggestion>> {
        return try {
            val suggestions = mutableListOf<OptimizationSuggestion>()
            
            // Analyze subscriptions
            suggestions.addAll(analyzeSubscriptionOptimizations(context))
            
            // Analyze spending patterns
            suggestions.addAll(analyzeSpendingOptimizations(context))
            
            // Analyze savings opportunities
            suggestions.addAll(analyzeSavingsOptimizations(context))
            
            // Sort by potential savings and confidence
            val sortedSuggestions = suggestions.sortedWith(
                compareByDescending<OptimizationSuggestion> { it.potentialSavings.amount }
                    .thenByDescending { it.confidence }
            ).take(5)
            
            Result.success(sortedSuggestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun generateFollowUpQuestions(
        context: UserFinancialContext,
        previousQuery: String?
    ): Result<List<String>> {
        return try {
            val questions = mutableListOf<String>()
            
            // Context-based questions
            if (context.goals.isNotEmpty()) {
                questions.add("How can I accelerate my ${context.goals.first().title} goal?")
                questions.add("What's the best way to balance my financial goals?")
            }
            
            if (context.budgets.isNotEmpty()) {
                questions.add("Am I on track with my budget this month?")
                questions.add("Where can I cut spending to stay within budget?")
            }
            
            // General financial questions
            questions.addAll(listOf(
                "What should I focus on financially this month?",
                "How much can I afford to spend on entertainment?",
                "When will I reach my savings goal?",
                "What are my biggest spending categories?",
                "How can I optimize my RRSP contributions?",
                "Should I pay off debt or save more?"
            ))
            
            // Randomize and limit
            Result.success(questions.shuffled().take(4))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun analyzeUnusualSpending(
        context: UserFinancialContext,
        timeframe: DateRange
    ): Result<List<com.north.mobile.data.ai.UnusualSpendingAlert>> {
        return try {
            val alerts = mutableListOf<com.north.mobile.data.ai.UnusualSpendingAlert>()
            val recentTransactions = context.recentTransactions.filter { 
                it.date >= timeframe.startDate && it.date <= timeframe.endDate 
            }
            
            // Analyze large purchases
            alerts.addAll(detectLargePurchases(recentTransactions, context))
            
            // Analyze frequency spikes
            alerts.addAll(detectFrequencySpikes(recentTransactions, context))
            
            // Analyze new merchants
            alerts.addAll(detectNewMerchants(recentTransactions, context))
            
            Result.success(alerts.sortedByDescending { it.amount.amount })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Private helper methods
    
    private suspend fun handleAffordabilityQuery(
        query: ProcessedQuery,
        context: UserFinancialContext
    ): AIResponse {
        val amount = query.extractedAmount ?: Money.fromDollars(100.0)
        val category = query.extractedCategory ?: Category.UNCATEGORIZED
        
        val expense = ExpenseRequest(
            description = query.originalQuery,
            amount = amount,
            category = category,
            plannedDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
        )
        
        val affordabilityResult = affordabilityAnalyzer.analyzeAffordability(expense, context)
        
        val message = if (affordabilityResult.canAfford) {
            "Yes, you can afford this ${amount.format()} expense! ${affordabilityResult.reasoning}"
        } else {
            "This ${amount.format()} expense might strain your budget. ${affordabilityResult.reasoning}"
        }
        
        return AIResponse(
            message = message,
            confidence = affordabilityResult.confidence,
            supportingData = listOf(
                DataPoint("Budget Impact", affordabilityResult.impactOnBudget.overallBudgetImpact.format(), "Budget Analysis", "Shows impact on monthly budget"),
                DataPoint("Goal Impact", "${affordabilityResult.impactOnGoals.delayInDays} days delay", "Goal Analysis", "Potential delay to financial goals")
            ),
            actionableRecommendations = affordabilityResult.recommendations.map { 
                Recommendation("rec_${it.hashCode()}", RecommendationType.BUDGET_ADJUSTMENT, "Budget Adjustment", it, Priority.MEDIUM, "Review", null)
            },
            followUpQuestions = listOf(
                "What are some alternatives to this expense?",
                "How can I adjust my budget to afford this?",
                "What would be the impact on my goals?"
            )
        )
    }
    
    private suspend fun handleSpendingAnalysisQuery(
        query: ProcessedQuery,
        context: UserFinancialContext
    ): AIResponse {
        val category = query.extractedCategory?.name ?: "all categories"
        val timeframe = query.extractedTimeframe ?: DateRange(
            Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(30, DateTimeUnit.DAY),
            Clock.System.todayIn(TimeZone.currentSystemDefault())
        )
        
        val analysis = context.spendingAnalysis ?: return AIResponse(
            message = "I need more transaction data to provide spending analysis.",
            confidence = 0.5f,
            supportingData = emptyList(),
            actionableRecommendations = emptyList(),
            followUpQuestions = listOf("Can you link more accounts to get better insights?")
        )
        
        val message = buildString {
            append("Here's your spending analysis for $category: ")
            append("You spent ${analysis.totalSpent.format()} in the last ${timeframe.durationInDays} days. ")
            
            val topCategory = analysis.categoryBreakdown.maxByOrNull { it.totalAmount.amount }
            topCategory?.let {
                append("Your biggest expense was ${it.category.name} at ${it.totalAmount.format()}. ")
            }
            
            if (analysis.comparisonToPrevious != null) {
                val change = analysis.comparisonToPrevious.totalSpentChangePercentage
                if (change > 0) {
                    append("This is ${abs(change).toInt()}% more than last period.")
                } else {
                    append("This is ${abs(change).toInt()}% less than last period - great job!")
                }
            }
        }
        
        return AIResponse(
            message = message,
            confidence = 0.9f,
            supportingData = analysis.categoryBreakdown.take(3).map { 
                DataPoint(it.category.name, it.totalAmount.format(), "Spending Analysis", "Category spending breakdown")
            },
            actionableRecommendations = analysis.insights.map { insight ->
                Recommendation(
                    insight.id, 
                    RecommendationType.SPENDING_REDUCTION, 
                    insight.title, 
                    insight.description, 
                    Priority.MEDIUM, 
                    "Review spending", 
                    insight.potentialSavings
                )
            },
            followUpQuestions = listOf(
                "How can I reduce spending in my top categories?",
                "What's my spending trend over the last few months?",
                "Are there any unusual purchases I should know about?"
            )
        )
    }
    
    private suspend fun handleGoalProgressQuery(
        query: ProcessedQuery,
        context: UserFinancialContext
    ): AIResponse {
        if (context.goals.isEmpty()) {
            return AIResponse(
                message = "You don't have any financial goals set up yet. Would you like me to help you create one?",
                confidence = 1.0f,
                supportingData = emptyList(),
                actionableRecommendations = listOf(
                    Recommendation("create_goal", RecommendationType.GOAL_ADJUSTMENT, "Create Goal", "Set up your first financial goal", Priority.HIGH, "Create Goal", null)
                ),
                followUpQuestions = listOf(
                    "What financial goal would you like to work towards?",
                    "How much would you like to save?",
                    "What's your target timeline?"
                )
            )
        }
        
        val activeGoals = context.goals.filter { it.isActive }
        val totalProgress = activeGoals.map { it.progressPercentage }.average()
        
        val message = buildString {
            append("Here's your goal progress: ")
            append("You have ${activeGoals.size} active goals with an average progress of ${totalProgress.toInt()}%. ")
            
            val closestGoal = activeGoals.minByOrNull { it.daysRemaining }
            closestGoal?.let {
                append("Your closest goal is '${it.title}' at ${it.progressPercentage.toInt()}% complete. ")
                append("You need ${it.weeklyTargetAmount.format()} per week to stay on track.")
            }
        }
        
        return AIResponse(
            message = message,
            confidence = 0.95f,
            supportingData = activeGoals.take(3).map { goal ->
                DataPoint(goal.title, "${goal.progressPercentage.toInt()}%", "Goal Progress", "Current progress towards goal")
            },
            actionableRecommendations = listOf(
                Recommendation("adjust_contributions", RecommendationType.GOAL_ADJUSTMENT, "Adjust Contributions", "Consider increasing your weekly contributions", Priority.MEDIUM, "Review Goals", null)
            ),
            followUpQuestions = listOf(
                "How can I accelerate my goal progress?",
                "Should I adjust my goal timeline?",
                "What's the best way to balance multiple goals?"
            )
        )
    }
    
    private suspend fun handleBudgetStatusQuery(
        query: ProcessedQuery,
        context: UserFinancialContext
    ): AIResponse {
        if (context.budgets.isEmpty()) {
            return AIResponse(
                message = "You don't have any budgets set up. Creating budgets can help you track and control your spending.",
                confidence = 1.0f,
                supportingData = emptyList(),
                actionableRecommendations = listOf(
                    Recommendation("create_budget", RecommendationType.BUDGET_ADJUSTMENT, "Create Budget", "Set up your first budget", Priority.HIGH, "Create Budget", null)
                ),
                followUpQuestions = listOf(
                    "What categories should we budget for?",
                    "How much do you want to spend on dining out?",
                    "What's a reasonable entertainment budget?"
                )
            )
        }
        
        val totalBudget = context.budgets.sumOf { it.amount.amount }
        val totalSpent = context.budgets.sumOf { it.spent.amount }
        val totalRemaining = context.budgets.sumOf { it.remaining.amount }
        val percentageUsed = (totalSpent.toDouble() / totalBudget.toDouble() * 100).toInt()
        
        val overBudgetCategories = context.budgets.filter { it.remaining.amount < 0 }
        
        val message = buildString {
            append("Your budget status: You've used $percentageUsed% of your total budget. ")
            
            if (overBudgetCategories.isNotEmpty()) {
                append("You're over budget in ${overBudgetCategories.size} categories: ")
                append(overBudgetCategories.joinToString(", ") { it.category.name })
                append(". ")
            } else {
                append("You're staying within budget - great job! ")
            }
            
            append("You have ${Money.fromCents(totalRemaining).format()} remaining this month.")
        }
        
        return AIResponse(
            message = message,
            confidence = 0.9f,
            supportingData = context.budgets.take(5).map { budget ->
                DataPoint(budget.category.name, "${budget.spent.format()} / ${budget.amount.format()}", "Budget Status", "Spent vs budgeted amount")
            },
            actionableRecommendations = if (overBudgetCategories.isNotEmpty()) {
                listOf(Recommendation("adjust_spending", RecommendationType.BUDGET_ADJUSTMENT, "Adjust Spending", "Consider reducing spending in over-budget categories", Priority.HIGH, "Review Budget", null))
            } else {
                listOf(Recommendation("maintain_discipline", RecommendationType.BUDGET_ADJUSTMENT, "Keep It Up", "Continue your excellent budget discipline", Priority.LOW, "Continue", null))
            },
            followUpQuestions = listOf(
                "How can I get back on budget?",
                "Should I adjust my budget amounts?",
                "What are my biggest budget challenges?"
            )
        )
    }
    
    private suspend fun handleTransactionExplanationQuery(
        query: ProcessedQuery,
        context: UserFinancialContext
    ): AIResponse {
        val recentLargeTransactions = context.recentTransactions
            .filter { it.amount.absoluteValue.amount > 5000 } // > $50
            .sortedByDescending { it.amount.absoluteValue.amount }
            .take(3)
        
        if (recentLargeTransactions.isEmpty()) {
            return AIResponse(
                message = "I don't see any notable transactions to explain. Your recent spending looks normal!",
                confidence = 0.8f,
                supportingData = emptyList(),
                actionableRecommendations = emptyList(),
                followUpQuestions = listOf(
                    "Would you like me to analyze your spending patterns?",
                    "Are there any specific transactions you're curious about?"
                )
            )
        }
        
        val transaction = recentLargeTransactions.first()
        val explanation = transactionExplainer.explainTransaction(transaction, context)
        
        return AIResponse(
            message = explanation.summary,
            confidence = 0.85f,
            supportingData = listOf(
                DataPoint("Amount", transaction.amount.format(), "Transaction Details", "Transaction amount"),
                DataPoint("Category", transaction.category.name, "Transaction Details", "Spending category"),
                DataPoint("Date", transaction.date.toString(), "Transaction Details", "Transaction date")
            ),
            actionableRecommendations = explanation.recommendations.map { rec ->
                Recommendation("trans_rec_${rec.hashCode()}", RecommendationType.SPENDING_REDUCTION, "Transaction Insight", rec, Priority.MEDIUM, "Review", null)
            },
            followUpQuestions = listOf(
                "Are there similar transactions I should be aware of?",
                "How does this affect my budget?",
                "Should I categorize this differently?"
            )
        )
    }
    
    private suspend fun handleSavingsAdviceQuery(
        query: ProcessedQuery,
        context: UserFinancialContext
    ): AIResponse {
        val savingsGoals = context.goals.filter { it.goalType == GoalType.GENERAL_SAVINGS || it.goalType == GoalType.EMERGENCY_FUND }
        val totalSavingsTarget = savingsGoals.sumOf { it.targetAmount.amount }
        val totalSavingsCurrent = savingsGoals.sumOf { it.currentAmount.amount }
        
        val message = if (savingsGoals.isNotEmpty()) {
            val progressPercentage = if (totalSavingsTarget > 0) {
                (totalSavingsCurrent.toDouble() / totalSavingsTarget.toDouble() * 100).toInt()
            } else 0
            
            "Your savings progress: You've saved ${Money.fromCents(totalSavingsCurrent).format()} " +
            "towards your ${Money.fromCents(totalSavingsTarget).format()} target ($progressPercentage% complete). " +
            "Based on your spending patterns, I recommend saving ${calculateRecommendedSavings(context).format()} per month."
        } else {
            "You don't have specific savings goals set up. Based on your income and expenses, " +
            "I recommend starting with an emergency fund of ${calculateEmergencyFundTarget(context).format()} " +
            "and saving ${calculateRecommendedSavings(context).format()} per month."
        }
        
        return AIResponse(
            message = message,
            confidence = 0.8f,
            supportingData = listOf(
                DataPoint("Recommended Monthly Savings", calculateRecommendedSavings(context).format(), "Savings Analysis", "Optimal monthly savings amount"),
                DataPoint("Emergency Fund Target", calculateEmergencyFundTarget(context).format(), "Savings Analysis", "Recommended emergency fund size")
            ),
            actionableRecommendations = listOf(
                Recommendation("automate_savings", RecommendationType.SAVINGS_OPPORTUNITY, "Automate Savings", "Set up automatic transfers to your savings account", Priority.HIGH, "Set Up Auto-Save", null),
                Recommendation("high_yield_account", RecommendationType.SAVINGS_OPPORTUNITY, "High-Yield Account", "Consider moving savings to a high-yield account", Priority.MEDIUM, "Research Accounts", null)
            ),
            followUpQuestions = listOf(
                "How can I save more each month?",
                "What's the best savings account for me?",
                "Should I prioritize RRSP or TFSA contributions?"
            )
        )
    }
    
    private suspend fun handleGeneralInsightQuery(
        query: ProcessedQuery,
        context: UserFinancialContext
    ): AIResponse {
        val insights = generatePersonalizedInsights(context).getOrNull() ?: emptyList()
        val topInsight = insights.firstOrNull()
        
        val message = if (topInsight != null) {
            "Here's what I noticed about your finances: ${topInsight.description} " +
            if (topInsight.potentialSavings != null) {
                "This could save you ${topInsight.potentialSavings.format()}."
            } else {
                "This could help improve your financial health."
            }
        } else {
            "Your finances look healthy overall! Keep up the good work with your spending habits and goal progress."
        }
        
        return AIResponse(
            message = message,
            confidence = 0.75f,
            supportingData = insights.take(3).map { insight ->
                DataPoint(insight.title, insight.description, "Financial Insight", "AI-generated insight")
            },
            actionableRecommendations = insights.take(2).flatMap { insight ->
                insight.actionableSteps.map { step ->
                    Recommendation("insight_${step.hashCode()}", RecommendationType.SAVINGS_OPPORTUNITY, insight.title, step, Priority.MEDIUM, "Take Action", insight.potentialSavings)
                }
            },
            followUpQuestions = listOf(
                "What should I focus on this month?",
                "How can I improve my financial health?",
                "What are my biggest opportunities?"
            )
        )
    }
    
    private suspend fun handleOptimizationQuery(
        query: ProcessedQuery,
        context: UserFinancialContext
    ): AIResponse {
        val optimizations = suggestOptimizations(context).getOrNull() ?: emptyList()
        val topOptimization = optimizations.firstOrNull()
        
        val message = if (topOptimization != null) {
            "I found some optimization opportunities: ${topOptimization.description} " +
            "This could save you ${topOptimization.potentialSavings.format()} with ${topOptimization.effort.name.lowercase()} effort."
        } else {
            "Your finances are already well-optimized! I don't see any major opportunities for improvement right now."
        }
        
        return AIResponse(
            message = message,
            confidence = 0.8f,
            supportingData = optimizations.take(3).map { opt ->
                DataPoint(opt.title, opt.potentialSavings.format(), "Optimization", "Potential savings opportunity")
            },
            actionableRecommendations = optimizations.take(2).map { opt ->
                Recommendation("opt_${opt.id}", RecommendationType.SAVINGS_OPPORTUNITY, opt.title, opt.description, Priority.MEDIUM, "Optimize", opt.potentialSavings)
            },
            followUpQuestions = listOf(
                "How can I reduce my monthly expenses?",
                "What subscriptions should I cancel?",
                "Where am I overspending?"
            )
        )
    }
    
    // Helper methods for calculations and analysis
    
    private fun calculateTrend(transactions: List<Transaction>): TrendDirection {
        if (transactions.size < 2) return TrendDirection.STABLE
        
        val sortedTransactions = transactions.sortedBy { it.date }
        val firstHalf = sortedTransactions.take(sortedTransactions.size / 2)
        val secondHalf = sortedTransactions.drop(sortedTransactions.size / 2)
        
        val firstHalfAvg = firstHalf.map { it.amount.absoluteValue.amount }.average()
        val secondHalfAvg = secondHalf.map { it.amount.absoluteValue.amount }.average()
        
        return when {
            secondHalfAvg > firstHalfAvg * 1.1 -> TrendDirection.INCREASING
            secondHalfAvg < firstHalfAvg * 0.9 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }
    
    private fun calculateRecommendedSavings(context: UserFinancialContext): Money {
        // Simple rule: 20% of income or $500, whichever is lower
        val monthlyIncome = estimateMonthlyIncome(context)
        val recommendedAmount = min(monthlyIncome.amount * 0.2, 50000) // $500 max
        return Money.fromCents(recommendedAmount.toLong())
    }
    
    private fun calculateEmergencyFundTarget(context: UserFinancialContext): Money {
        // 3 months of expenses
        val monthlyExpenses = estimateMonthlyExpenses(context)
        return monthlyExpenses * 3.0
    }
    
    private fun estimateMonthlyIncome(context: UserFinancialContext): Money {
        // Estimate from recent credit transactions (income)
        val incomeTransactions = context.recentTransactions.filter { 
            it.amount.isPositive && it.category.id == Category.INCOME.id 
        }
        
        return if (incomeTransactions.isNotEmpty()) {
            val totalIncome = incomeTransactions.sumOf { it.amount.amount }
            Money.fromCents(totalIncome / incomeTransactions.size * 30) // Daily average * 30
        } else {
            Money.fromDollars(4000.0) // Default estimate
        }
    }
    
    private fun estimateMonthlyExpenses(context: UserFinancialContext): Money {
        val expenseTransactions = context.recentTransactions.filter { it.amount.isNegative }
        
        return if (expenseTransactions.isNotEmpty()) {
            val totalExpenses = expenseTransactions.sumOf { it.amount.absoluteValue.amount }
            Money.fromCents(totalExpenses / expenseTransactions.size * 30) // Daily average * 30
        } else {
            Money.fromDollars(3000.0) // Default estimate
        }
    }
    
    private fun analyzeSubscriptionOptimizations(context: UserFinancialContext): List<OptimizationSuggestion> {
        val subscriptionKeywords = listOf("netflix", "spotify", "amazon", "subscription", "monthly", "annual")
        val subscriptionTransactions = context.recentTransactions.filter { transaction ->
            subscriptionKeywords.any { keyword ->
                transaction.description.contains(keyword, ignoreCase = true) ||
                transaction.merchantName?.contains(keyword, ignoreCase = true) == true
            }
        }
        
        if (subscriptionTransactions.isEmpty()) return emptyList()
        
        val totalSubscriptionCost = subscriptionTransactions.sumOf { it.amount.absoluteValue.amount }
        val potentialSavings = Money.fromCents(totalSubscriptionCost / 3) // Assume 1/3 could be cancelled
        
        return listOf(
            OptimizationSuggestion(
                id = "subscription_audit",
                type = OptimizationType.SUBSCRIPTION_OPTIMIZATION,
                title = "Review Subscriptions",
                description = "You have ${subscriptionTransactions.size} subscription services. Consider cancelling unused ones.",
                potentialSavings = potentialSavings,
                effort = EffortLevel.LOW,
                timeToImplement = "30 minutes",
                steps = listOf(
                    "Review all subscription services",
                    "Cancel unused subscriptions",
                    "Consider annual plans for frequently used services"
                ),
                riskLevel = RiskLevel.LOW,
                confidence = 0.8f
            )
        )
    }
    
    private fun analyzeSpendingOptimizations(context: UserFinancialContext): List<OptimizationSuggestion> {
        val suggestions = mutableListOf<OptimizationSuggestion>()
        
        // Analyze dining out
        val diningTransactions = context.recentTransactions.filter { 
            it.category.id == Category.RESTAURANTS.id 
        }
        
        if (diningTransactions.isNotEmpty()) {
            val totalDining = diningTransactions.sumOf { it.amount.absoluteValue.amount }
            val potentialSavings = Money.fromCents(totalDining / 4) // 25% reduction
            
            suggestions.add(
                OptimizationSuggestion(
                    id = "dining_reduction",
                    type = OptimizationType.SPENDING_REDUCTION,
                    title = "Reduce Dining Out",
                    description = "You spent ${Money.fromCents(totalDining).format()} on dining out. Consider cooking more meals at home.",
                    potentialSavings = potentialSavings,
                    effort = EffortLevel.MEDIUM,
                    timeToImplement = "Ongoing",
                    steps = listOf(
                        "Plan meals for the week",
                        "Cook at home 2-3 more times per week",
                        "Pack lunch for work"
                    ),
                    riskLevel = RiskLevel.LOW,
                    confidence = 0.7f
                )
            )
        }
        
        return suggestions
    }
    
    private fun analyzeSavingsOptimizations(context: UserFinancialContext): List<OptimizationSuggestion> {
        val suggestions = mutableListOf<OptimizationSuggestion>()
        
        // Check if user has high-yield savings
        val savingsAccounts = context.accounts.filter { it.accountType == AccountType.SAVINGS }
        
        if (savingsAccounts.isNotEmpty()) {
            val totalSavings = savingsAccounts.sumOf { it.balance.amount }
            val potentialSavings = Money.fromCents((totalSavings * 0.02).toLong()) // 2% additional interest
            
            suggestions.add(
                OptimizationSuggestion(
                    id = "high_yield_savings",
                    type = OptimizationType.SAVINGS_INCREASE,
                    title = "High-Yield Savings Account",
                    description = "Consider moving your savings to a high-yield account for better returns.",
                    potentialSavings = potentialSavings,
                    effort = EffortLevel.LOW,
                    timeToImplement = "1 hour",
                    steps = listOf(
                        "Research high-yield savings accounts",
                        "Compare interest rates",
                        "Transfer funds to higher-yield account"
                    ),
                    riskLevel = RiskLevel.LOW,
                    confidence = 0.9f
                )
            )
        }
        
        return suggestions
    }
    
    private fun detectLargePurchases(
        transactions: List<Transaction>,
        context: UserFinancialContext
    ): List<com.north.mobile.data.ai.UnusualSpendingAlert> {
        val averageTransaction = transactions.map { it.amount.absoluteValue.amount }.average()
        val threshold = averageTransaction * 3 // 3x average
        
        return transactions.filter { it.amount.absoluteValue.amount > threshold }
            .map { transaction ->
                com.north.mobile.data.ai.UnusualSpendingAlert(
                    id = "large_${transaction.id}",
                    type = UnusualSpendingType.LARGE_PURCHASE,
                    description = "Large purchase detected: ${transaction.displayDescription}",
                    amount = transaction.amount.absoluteValue,
                    category = transaction.category,
                    transactions = listOf(transaction),
                    explanation = "This transaction is ${(transaction.amount.absoluteValue.amount / averageTransaction).toInt()}x your average transaction amount.",
                    severity = if (transaction.amount.absoluteValue.amount > averageTransaction * 5) com.north.mobile.data.analytics.AlertSeverity.HIGH else com.north.mobile.data.analytics.AlertSeverity.MEDIUM,
                    recommendations = listOf(
                        "Verify this transaction is correct",
                        "Consider if this purchase aligns with your budget",
                        "Update your budget if this was planned"
                    )
                )
            }
    }
    
    private fun detectFrequencySpikes(
        transactions: List<Transaction>,
        context: UserFinancialContext
    ): List<com.north.mobile.data.ai.UnusualSpendingAlert> {
        // Group by merchant and detect frequency spikes
        val merchantGroups = transactions.groupBy { it.merchantName ?: it.description }
        
        return merchantGroups.filter { (_, transactions) -> transactions.size > 5 }
            .map { (merchant, transactions) ->
                com.north.mobile.data.ai.UnusualSpendingAlert(
                    id = "freq_${merchant.hashCode()}",
                    type = UnusualSpendingType.FREQUENCY_SPIKE,
                    description = "Frequent transactions at $merchant",
                    amount = Money.fromCents(transactions.sumOf { it.amount.absoluteValue.amount }),
                    category = transactions.first().category,
                    transactions = transactions,
                    explanation = "You made ${transactions.size} transactions at $merchant, which is higher than usual.",
                    severity = com.north.mobile.data.analytics.AlertSeverity.MEDIUM,
                    recommendations = listOf(
                        "Review if all transactions are necessary",
                        "Consider setting a spending limit for this merchant",
                        "Look for patterns in your spending behavior"
                    )
                )
            }
    }
    
    private fun detectNewMerchants(
        transactions: List<Transaction>,
        context: UserFinancialContext
    ): List<com.north.mobile.data.ai.UnusualSpendingAlert> {
        // This would require historical data to determine "new" merchants
        // For now, return empty list as we don't have historical context
        return emptyList()
    }
}.0) // Default estimate
        }
    }
    
    private fun estimateMonthlyExpenses(context: UserFinancialContext): Money {
        val expenseTransactions = context.recentTransactions.filter { it.amount.isNegative }
        
        return if (expenseTransactions.isNotEmpty()) {
            val totalExpenses = expenseTransactions.sumOf { it.amount.absoluteValue.amount }
            Money.fromCents(totalExpenses / expenseTransactions.size * 30) // Daily average * 30
        } else {
            Money.fromDollars(3000.0) // Default estimate
        }
    }
    
    private fun analyzeSubscriptionOptimizations(context: UserFinancialContext): List<OptimizationSuggestion> {
        val subscriptionKeywords = listOf("netflix", "spotify", "amazon", "subscription", "monthly", "annual")
        val subscriptionTransactions = context.recentTransactions.filter { transaction ->
            subscriptionKeywords.any { keyword ->
                transaction.description.contains(keyword, ignoreCase = true) ||
                transaction.merchantName?.contains(keyword, ignoreCase = true) == true
            }
        }
        
        if (subscriptionTransactions.isEmpty()) return emptyList()
        
        val totalSubscriptionCost = subscriptionTransactions.sumOf { it.amount.absoluteValue.amount }
        val potentialSavings = Money.fromCents(totalSubscriptionCost / 3) // Assume 1/3 could be cancelled
        
        return listOf(
            OptimizationSuggestion(
                id = "subscription_audit",
                type = OptimizationType.SUBSCRIPTION_OPTIMIZATION,
                title = "Review Subscriptions",
                description = "You have ${subscriptionTransactions.size} subscription services. Consider cancelling unused ones.",
                potentialSavings = potentialSavings,
                effort = EffortLevel.LOW,
                timeToImplement = "30 minutes",
                steps = listOf(
                    "Review all subscription services",
                    "Cancel unused subscriptions",
                    "Consider annual plans for frequently used services"
                ),
                riskLevel = RiskLevel.LOW,
                confidence = 0.8f
            )
        )
    }
    
    private fun analyzeSpendingOptimizations(context: UserFinancialContext): List<OptimizationSuggestion> {
        val suggestions = mutableListOf<OptimizationSuggestion>()
        
        // Analyze dining out
        val diningTransactions = context.recentTransactions.filter { 
            it.category.id == Category.RESTAURANTS.id 
        }
        
        if (diningTransactions.isNotEmpty()) {
            val totalDining = diningTransactions.sumOf { it.amount.absoluteValue.amount }
            val potentialSavings = Money.fromCents(totalDining / 4) // 25% reduction
            
            suggestions.add(
                OptimizationSuggestion(
                    id = "dining_reduction",
                    type = OptimizationType.SPENDING_REDUCTION,
                    title = "Reduce Dining Out",
                    description = "You spent ${Money.fromCents(totalDining).format()} on dining out. Consider cooking more meals at home.",
                    potentialSavings = potentialSavings,
                    effort = EffortLevel.MEDIUM,
                    timeToImplement = "Ongoing",
                    steps = listOf(
                        "Plan meals for the week",
                        "Cook at home 2-3 more times per week",
                        "Pack lunch for work"
                    ),
                    riskLevel = RiskLevel.LOW,
                    confidence = 0.7f
                )
            )
        }
        
        return suggestions
    }
    
    private fun analyzeSavingsOptimizations(context: UserFinancialContext): List<OptimizationSuggestion> {
        val suggestions = mutableListOf<OptimizationSuggestion>()
        
        // Check if user has high-yield savings
        val savingsAccounts = context.accounts.filter { it.accountType == AccountType.SAVINGS }
        
        if (savingsAccounts.isNotEmpty()) {
            val totalSavings = savingsAccounts.sumOf { it.balance.amount }
            val potentialSavings = Money.fromCents((totalSavings * 0.02).toLong()) // 2% additional interest
            
            suggestions.add(
                OptimizationSuggestion(
                    id = "high_yield_savings",
                    type = OptimizationType.SAVINGS_INCREASE,
                    title = "High-Yield Savings Account",
                    description = "Consider moving your savings to a high-yield account for better returns.",
                    potentialSavings = potentialSavings,
                    effort = EffortLevel.LOW,
                    timeToImplement = "1 hour",
                    steps = listOf(
                        "Research high-yield savings accounts",
                        "Compare interest rates",
                        "Transfer funds to higher-yield account"
                    ),
                    riskLevel = RiskLevel.LOW,
                    confidence = 0.9f
                )
            )
        }
        
        return suggestions
    }
    
    private fun detectLargePurchases(
        transactions: List<Transaction>,
        context: UserFinancialContext
    ): List<com.north.mobile.data.ai.UnusualSpendingAlert> {
        val averageTransaction = transactions.map { it.amount.absoluteValue.amount }.average()
        val threshold = averageTransaction * 3 // 3x average
        
        return transactions.filter { it.amount.absoluteValue.amount > threshold }
            .map { transaction ->
                UnusualSpendingAlert(
                    id = "large_${transaction.id}",
                    type = UnusualSpendingType.LARGE_PURCHASE,
                    description = "Large purchase detected: ${transaction.displayDescription}",
                    amount = transaction.amount.absoluteValue,
                    category = transaction.category,
                    transactions = listOf(transaction),
                    explanation = "This transaction is ${(transaction.amount.absoluteValue.amount / averageTransaction).toInt()}x your average transaction amount.",
                    severity = if (transaction.amount.absoluteValue.amount > averageTransaction * 5) AlertSeverity.HIGH else AlertSeverity.MEDIUM,
                    recommendations = listOf(
                        "Verify this transaction is correct",
                        "Consider if this purchase aligns with your budget",
                        "Update your budget if this was planned"
                    )
                )
            }
    }
    
    private fun detectFrequencySpikes(
        transactions: List<Transaction>,
        context: UserFinancialContext
    ): List<com.north.mobile.data.ai.UnusualSpendingAlert> {
        // Group by merchant and detect frequency spikes
        val merchantGroups = transactions.groupBy { it.merchantName ?: it.description }
        
        return merchantGroups.filter { (_, transactions) -> transactions.size > 5 }
            .map { (merchant, transactions) ->
                UnusualSpendingAlert(
                    id = "freq_${merchant.hashCode()}",
                    type = UnusualSpendingType.FREQUENCY_SPIKE,
                    description = "Frequent transactions at $merchant",
                    amount = Money.fromCents(transactions.sumOf { it.amount.absoluteValue.amount }),
                    category = transactions.first().category,
                    transactions = transactions,
                    explanation = "You made ${transactions.size} transactions at $merchant, which is higher than usual.",
                    severity = AlertSeverity.MEDIUM,
                    recommendations = listOf(
                        "Review if all transactions are necessary",
                        "Consider setting a spending limit for this merchant",
                        "Look for patterns in your spending behavior"
                    )
                )
            }
    }
    
    private fun detectNewMerchants(
        transactions: List<Transaction>,
        context: UserFinancialContext
    ): List<com.north.mobile.data.ai.UnusualSpendingAlert> {
        // This would require historical data to determine "new" merchants
        // For now, return empty list as we don't have historical context
        return emptyList()
    }
}