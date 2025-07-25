package com.north.mobile.data.ai

import com.north.mobile.domain.model.*
import com.north.mobile.data.analytics.*
import kotlinx.datetime.*
import kotlin.math.*

/**
 * Generates personalized financial insights based on user data
 */
class InsightGenerator {
    
    fun generateSpendingInsights(
        analysis: SpendingAnalysis,
        context: UserFinancialContext
    ): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        
        // Analyze spending trends
        insights.addAll(generateTrendInsights(analysis, context))
        
        // Analyze category spending
        insights.addAll(generateCategoryInsights(analysis, context))
        
        // Analyze unusual spending
        insights.addAll(generateUnusualSpendingInsights(analysis, context))
        
        // Analyze positive behaviors
        insights.addAll(generatePositiveBehaviorInsights(analysis, context))
        
        return insights.sortedByDescending { it.confidence }
    }
    
    fun generateGoalInsights(
        goals: List<FinancialGoal>,
        context: UserFinancialContext
    ): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        
        if (goals.isEmpty()) {
            insights.add(
                AIInsight(
                    id = "no_goals",
                    type = AIInsightType.GOAL_PROGRESS,
                    title = "Set Your First Financial Goal",
                    description = "Setting specific financial goals can help you stay motivated and track progress. Consider starting with an emergency fund or savings goal.",
                    confidence = 0.9f,
                    impact = InsightImpact.HIGH,
                    category = null,
                    actionableSteps = listOf(
                        "Choose a specific savings target",
                        "Set a realistic timeline",
                        "Break it down into weekly targets"
                    ),
                    potentialSavings = null,
                    timeframe = "This week",
                    supportingData = emptyList()
                )
            )
            return insights
        }
        
        // Analyze goal progress
        val activeGoals = goals.filter { it.isActive }
        val completedGoals = goals.filter { it.isCompleted }
        val overdueGoals = goals.filter { it.isOverdue }
        
        // Goals on track
        val onTrackGoals = activeGoals.filter { goal ->
            val requiredWeeklyAmount = goal.weeklyTargetAmount
            val currentWeeklyRate = estimateWeeklySavingsRate(goal, context)
            currentWeeklyRate >= requiredWeeklyAmount
        }
        
        if (onTrackGoals.isNotEmpty()) {
            insights.add(
                AIInsight(
                    id = "goals_on_track",
                    type = AIInsightType.POSITIVE_BEHAVIOR,
                    title = "Great Goal Progress!",
                    description = "You're on track with ${onTrackGoals.size} of your goals. Keep up the excellent work!",
                    confidence = 0.95f,
                    impact = InsightImpact.MEDIUM,
                    category = null,
                    actionableSteps = listOf(
                        "Continue your current savings rate",
                        "Consider accelerating progress if possible",
                        "Celebrate your consistency"
                    ),
                    potentialSavings = null,
                    timeframe = "Ongoing",
                    supportingData = onTrackGoals.map { goal ->
                        DataPoint(goal.title, "${goal.progressPercentage.toInt()}% complete", "Goal Progress", "Current progress")
                    }
                )
            )
        }
        
        // Goals behind schedule
        val behindGoals = activeGoals.filter { goal ->
            val requiredWeeklyAmount = goal.weeklyTargetAmount
            val currentWeeklyRate = estimateWeeklySavingsRate(goal, context)
            currentWeeklyRate < requiredWeeklyAmount * 0.8 // 20% behind
        }
        
        if (behindGoals.isNotEmpty()) {
            val totalShortfall = behindGoals.sumOf { it.weeklyTargetAmount.amount - estimateWeeklySavingsRate(it, context).amount }
            
            insights.add(
                AIInsight(
                    id = "goals_behind",
                    type = AIInsightType.GOAL_PROGRESS,
                    title = "Goal Progress Needs Attention",
                    description = "You're behind on ${behindGoals.size} goals. Consider increasing your weekly contributions by ${Money.fromCents(totalShortfall).format()}.",
                    confidence = 0.85f,
                    impact = InsightImpact.HIGH,
                    category = null,
                    actionableSteps = listOf(
                        "Review your budget for additional savings opportunities",
                        "Consider extending goal timelines if needed",
                        "Automate savings to stay consistent"
                    ),
                    potentialSavings = null,
                    timeframe = "This week",
                    supportingData = behindGoals.map { goal ->
                        DataPoint(goal.title, "Need ${goal.weeklyTargetAmount.format()}/week", "Goal Requirements", "Weekly target amount")
                    }
                )
            )
        }
        
        // Overdue goals
        if (overdueGoals.isNotEmpty()) {
            insights.add(
                AIInsight(
                    id = "overdue_goals",
                    type = AIInsightType.GOAL_PROGRESS,
                    title = "Overdue Goals Need Review",
                    description = "You have ${overdueGoals.size} overdue goals. Consider adjusting timelines or target amounts to make them more achievable.",
                    confidence = 0.9f,
                    impact = InsightImpact.HIGH,
                    category = null,
                    actionableSteps = listOf(
                        "Review and adjust goal timelines",
                        "Consider reducing target amounts if needed",
                        "Focus on one goal at a time"
                    ),
                    potentialSavings = null,
                    timeframe = "This week",
                    supportingData = overdueGoals.map { goal ->
                        DataPoint(goal.title, "${abs(goal.daysRemaining)} days overdue", "Overdue Goals", "Days past target date")
                    }
                )
            )
        }
        
        return insights
    }
    
    fun generateBudgetInsights(
        budgets: List<Budget>,
        context: UserFinancialContext
    ): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        
        if (budgets.isEmpty()) {
            insights.add(
                AIInsight(
                    id = "no_budgets",
                    type = AIInsightType.BUDGET_ALERT,
                    title = "Create Your First Budget",
                    description = "Budgeting helps you control spending and reach your goals faster. Start with your top 3 spending categories.",
                    confidence = 0.9f,
                    impact = InsightImpact.HIGH,
                    category = null,
                    actionableSteps = listOf(
                        "Identify your top spending categories",
                        "Set realistic monthly limits",
                        "Track your progress weekly"
                    ),
                    potentialSavings = Money.fromDollars(200.0), // Estimated savings from budgeting
                    timeframe = "This week",
                    supportingData = emptyList()
                )
            )
            return insights
        }
        
        // Analyze budget performance
        val overBudgetCategories = budgets.filter { it.remaining.amount < 0 }
        val underBudgetCategories = budgets.filter { it.remaining.amount > it.amount.amount * 0.2 } // 20% under
        val onTrackCategories = budgets.filter { 
            it.remaining.amount >= 0 && it.remaining.amount <= it.amount.amount * 0.2 
        }
        
        // Over budget insights
        if (overBudgetCategories.isNotEmpty()) {
            val totalOverspend = overBudgetCategories.sumOf { abs(it.remaining.amount) }
            
            insights.add(
                AIInsight(
                    id = "over_budget",
                    type = AIInsightType.BUDGET_ALERT,
                    title = "Budget Overspend Alert",
                    description = "You're over budget in ${overBudgetCategories.size} categories by ${Money.fromCents(totalOverspend).format()} total.",
                    confidence = 0.95f,
                    impact = InsightImpact.HIGH,
                    category = overBudgetCategories.first().category,
                    actionableSteps = listOf(
                        "Reduce spending in over-budget categories",
                        "Consider reallocating budget from under-spent categories",
                        "Set up spending alerts to prevent future overspend"
                    ),
                    potentialSavings = Money.fromCents(totalOverspend),
                    timeframe = "Rest of month",
                    supportingData = overBudgetCategories.map { budget ->
                        DataPoint(budget.category.name, "${budget.spent.format()} / ${budget.amount.format()}", "Budget Status", "Spent vs budgeted")
                    }
                )
            )
        }
        
        // Under budget insights (positive)
        if (underBudgetCategories.isNotEmpty()) {
            val totalUnderSpend = underBudgetCategories.sumOf { it.remaining.amount }
            
            insights.add(
                AIInsight(
                    id = "under_budget",
                    type = AIInsightType.POSITIVE_BEHAVIOR,
                    title = "Excellent Budget Discipline!",
                    description = "You're under budget in ${underBudgetCategories.size} categories with ${Money.fromCents(totalUnderSpend).format()} remaining. Consider allocating this to savings or goals.",
                    confidence = 0.9f,
                    impact = InsightImpact.MEDIUM,
                    category = null,
                    actionableSteps = listOf(
                        "Transfer unused budget to savings",
                        "Accelerate goal contributions",
                        "Build emergency fund"
                    ),
                    potentialSavings = Money.fromCents(totalUnderSpend),
                    timeframe = "End of month",
                    supportingData = underBudgetCategories.map { budget ->
                        DataPoint(budget.category.name, budget.remaining.format(), "Budget Remaining", "Amount left in budget")
                    }
                )
            )
        }
        
        return insights
    }
    
    fun generateGamificationInsights(
        profile: GamificationProfile,
        context: UserFinancialContext
    ): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        
        // Streak insights
        val activeStreaks = profile.currentStreaks.filter { it.isActive }
        val longestStreak = activeStreaks.maxByOrNull { it.currentCount }
        
        if (longestStreak != null && longestStreak.currentCount >= 7) {
            insights.add(
                AIInsight(
                    id = "streak_achievement",
                    type = AIInsightType.POSITIVE_BEHAVIOR,
                    title = "Amazing Streak!",
                    description = "You have a ${longestStreak.currentCount}-day ${longestStreak.type.name.lowercase()} streak! Keep up the momentum.",
                    confidence = 1.0f,
                    impact = InsightImpact.MEDIUM,
                    category = null,
                    actionableSteps = listOf(
                        "Continue your daily financial habits",
                        "Set a new streak goal",
                        "Share your success with friends"
                    ),
                    potentialSavings = null,
                    timeframe = "Daily",
                    supportingData = listOf(
                        DataPoint("Current Streak", "${longestStreak.currentCount} days", "Gamification", "Consecutive days of positive behavior")
                    )
                )
            )
        }
        
        // Level progress insights
        val pointsToNextLevel = calculatePointsToNextLevel(profile.totalPoints, profile.level)
        if (pointsToNextLevel <= 100) { // Close to leveling up
            insights.add(
                AIInsight(
                    id = "level_up_soon",
                    type = AIInsightType.POSITIVE_BEHAVIOR,
                    title = "Level Up Soon!",
                    description = "You're only $pointsToNextLevel points away from reaching level ${profile.level + 1}!",
                    confidence = 1.0f,
                    impact = InsightImpact.LOW,
                    category = null,
                    actionableSteps = listOf(
                        "Complete a few micro-wins to level up",
                        "Check your balance to earn points",
                        "Categorize recent transactions"
                    ),
                    potentialSavings = null,
                    timeframe = "Today",
                    supportingData = listOf(
                        DataPoint("Points Needed", "$pointsToNextLevel", "Gamification", "Points needed for next level")
                    )
                )
            )
        }
        
        return insights
    }
    
    private fun generateTrendInsights(
        analysis: SpendingAnalysis,
        context: UserFinancialContext
    ): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        
        analysis.comparisonToPrevious?.let { comparison ->
            val changePercentage = comparison.totalSpentChangePercentage
            
            if (changePercentage > 20) { // Spending increased significantly
                insights.add(
                    AIInsight(
                        id = "spending_increase",
                        type = AIInsightType.SPENDING_PATTERN,
                        title = "Spending Increased",
                        description = "Your spending increased by ${changePercentage.toInt()}% compared to last period. Review your recent purchases to identify the cause.",
                        confidence = 0.9f,
                        impact = InsightImpact.HIGH,
                        category = null,
                        actionableSteps = listOf(
                            "Review large purchases from this period",
                            "Check if any expenses were one-time or recurring",
                            "Adjust budget if needed"
                        ),
                        potentialSavings = Money.fromCents((comparison.totalSpentChange.amount * 0.5).toLong()),
                        timeframe = "This month",
                        supportingData = listOf(
                            DataPoint("Spending Change", comparison.totalSpentChange.format(), "Trend Analysis", "Change from previous period")
                        )
                    )
                )
            } else if (changePercentage < -10) { // Spending decreased
                insights.add(
                    AIInsight(
                        id = "spending_decrease",
                        type = AIInsightType.POSITIVE_BEHAVIOR,
                        title = "Great Spending Control!",
                        description = "You reduced spending by ${abs(changePercentage).toInt()}% compared to last period. Excellent work!",
                        confidence = 0.95f,
                        impact = InsightImpact.MEDIUM,
                        category = null,
                        actionableSteps = listOf(
                            "Continue your current spending habits",
                            "Consider allocating savings to goals",
                            "Reward yourself for good discipline"
                        ),
                        potentialSavings = comparison.totalSpentChange.absoluteValue,
                        timeframe = "Ongoing",
                        supportingData = listOf(
                            DataPoint("Spending Reduction", comparison.totalSpentChange.absoluteValue.format(), "Trend Analysis", "Amount saved compared to previous period")
                        )
                    )
                )
            }
        }
        
        return insights
    }
    
    private fun generateCategoryInsights(
        analysis: SpendingAnalysis,
        context: UserFinancialContext
    ): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        
        // Find top spending category
        val topCategory = analysis.categoryBreakdown.maxByOrNull { it.totalAmount.amount }
        if (topCategory != null && topCategory.percentageOfTotal > 30) {
            insights.add(
                AIInsight(
                    id = "top_category_${topCategory.category.id}",
                    type = AIInsightType.SPENDING_PATTERN,
                    title = "High ${topCategory.category.name} Spending",
                    description = "${topCategory.category.name} represents ${topCategory.percentageOfTotal.toInt()}% of your spending (${topCategory.totalAmount.format()}). Consider if this aligns with your priorities.",
                    confidence = 0.8f,
                    impact = InsightImpact.MEDIUM,
                    category = topCategory.category,
                    actionableSteps = listOf(
                        "Review ${topCategory.category.name} transactions",
                        "Look for opportunities to reduce spending",
                        "Set a budget limit for this category"
                    ),
                    potentialSavings = Money.fromCents((topCategory.totalAmount.amount * 0.15).toLong()), // 15% potential reduction
                    timeframe = "This month",
                    supportingData = listOf(
                        DataPoint("Category Spending", topCategory.totalAmount.format(), "Category Analysis", "Total spent in category"),
                        DataPoint("Percentage of Total", "${topCategory.percentageOfTotal.toInt()}%", "Category Analysis", "Percentage of total spending")
                    )
                )
            )
        }
        
        // Find categories with increasing trends
        val increasingCategories = analysis.categoryBreakdown.filter { 
            it.trend == TrendDirection.INCREASING && it.comparedToPrevious != null && it.comparedToPrevious.amount > 0 
        }
        
        if (increasingCategories.isNotEmpty()) {
            val topIncreasing = increasingCategories.maxByOrNull { it.comparedToPrevious?.amount ?: 0 }
            if (topIncreasing != null) {
                insights.add(
                    AIInsight(
                        id = "increasing_${topIncreasing.category.id}",
                        type = AIInsightType.SPENDING_PATTERN,
                        title = "${topIncreasing.category.name} Spending Rising",
                        description = "Your ${topIncreasing.category.name} spending increased by ${topIncreasing.comparedToPrevious?.format()} this period.",
                        confidence = 0.85f,
                        impact = InsightImpact.MEDIUM,
                        category = topIncreasing.category,
                        actionableSteps = listOf(
                            "Monitor ${topIncreasing.category.name} spending closely",
                            "Set alerts for this category",
                            "Look for patterns in the increase"
                        ),
                        potentialSavings = topIncreasing.comparedToPrevious,
                        timeframe = "Next month",
                        supportingData = listOf(
                            DataPoint("Spending Increase", topIncreasing.comparedToPrevious?.format() ?: "N/A", "Trend Analysis", "Increase from previous period")
                        )
                    )
                )
            }
        }
        
        return insights
    }
    
    private fun generateUnusualSpendingInsights(
        analysis: SpendingAnalysis,
        context: UserFinancialContext
    ): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        
        // This would typically analyze transaction patterns for unusual activity
        // For now, we'll create a placeholder insight if there are any insights from the analysis
        if (analysis.insights.isNotEmpty()) {
            val unusualInsight = analysis.insights.find { it.type == InsightType.UNUSUAL_ACTIVITY }
            if (unusualInsight != null) {
                insights.add(
                    AIInsight(
                        id = "unusual_${unusualInsight.id}",
                        type = AIInsightType.UNUSUAL_ACTIVITY,
                        title = unusualInsight.title,
                        description = unusualInsight.description,
                        confidence = unusualInsight.confidence,
                        impact = unusualInsight.impact,
                        category = unusualInsight.category,
                        actionableSteps = unusualInsight.actionableRecommendations,
                        potentialSavings = unusualInsight.potentialSavings,
                        timeframe = "Recent",
                        supportingData = emptyList()
                    )
                )
            }
        }
        
        return insights
    }
    
    private fun generatePositiveBehaviorInsights(
        analysis: SpendingAnalysis,
        context: UserFinancialContext
    ): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        
        // Look for positive behaviors in the analysis insights
        val positiveInsights = analysis.insights.filter { it.type == InsightType.POSITIVE_BEHAVIOR }
        
        positiveInsights.forEach { insight ->
            insights.add(
                AIInsight(
                    id = "positive_${insight.id}",
                    type = AIInsightType.POSITIVE_BEHAVIOR,
                    title = insight.title,
                    description = insight.description,
                    confidence = insight.confidence,
                    impact = insight.impact,
                    category = insight.category,
                    actionableSteps = insight.actionableRecommendations,
                    potentialSavings = insight.potentialSavings,
                    timeframe = "Ongoing",
                    supportingData = emptyList()
                )
            )
        }
        
        return insights
    }
    
    private fun estimateWeeklySavingsRate(goal: FinancialGoal, context: UserFinancialContext): Money {
        // This would analyze recent transactions to estimate actual savings rate
        // For now, return a simple estimate based on goal progress
        val weeksElapsed = max(1, (Clock.System.now() - goal.createdAt).inWholeDays / 7)
        return if (weeksElapsed > 0) {
            goal.currentAmount / weeksElapsed.toDouble()
        } else {
            Money.zero()
        }
    }
    
    private fun calculatePointsToNextLevel(totalPoints: Int, currentLevel: Int): Int {
        // Simple leveling formula: level * 1000 points
        val pointsForNextLevel = (currentLevel + 1) * 1000
        return max(0, pointsForNextLevel - totalPoints)
    }
}