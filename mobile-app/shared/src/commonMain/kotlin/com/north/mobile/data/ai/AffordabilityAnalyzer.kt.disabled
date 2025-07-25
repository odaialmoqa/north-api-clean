package com.north.mobile.data.ai

import com.north.mobile.domain.model.*
import kotlinx.datetime.*
import kotlin.math.*

/**
 * Analyzes whether a user can afford specific expenses
 */
class AffordabilityAnalyzer {
    
    fun analyzeAffordability(
        expense: ExpenseRequest,
        context: UserFinancialContext
    ): AffordabilityResult {
        
        // Calculate available funds
        val availableFunds = calculateAvailableFunds(context)
        
        // Calculate expense impact
        val totalExpenseAmount = if (expense.isRecurring) {
            calculateRecurringExpenseImpact(expense)
        } else {
            expense.amount
        }
        
        // Basic affordability check
        val canAfford = availableFunds >= totalExpenseAmount
        
        // Analyze impact on goals
        val goalImpact = analyzeGoalImpact(expense, context)
        
        // Analyze impact on budget
        val budgetImpact = analyzeBudgetImpact(expense, context)
        
        // Generate alternatives
        val alternatives = generateAlternatives(expense, context)
        
        // Calculate confidence based on various factors
        val confidence = calculateAffordabilityConfidence(
            canAfford, 
            availableFunds, 
            totalExpenseAmount, 
            goalImpact, 
            budgetImpact
        )
        
        // Generate reasoning
        val reasoning = generateAffordabilityReasoning(
            canAfford,
            availableFunds,
            totalExpenseAmount,
            goalImpact,
            budgetImpact,
            context
        )
        
        // Generate recommendations
        val recommendations = generateAffordabilityRecommendations(
            canAfford,
            expense,
            goalImpact,
            budgetImpact,
            context
        )
        
        // Identify risk factors
        val riskFactors = identifyRiskFactors(expense, context, goalImpact, budgetImpact)
        
        return AffordabilityResult(
            canAfford = canAfford,
            confidence = confidence,
            impactOnGoals = goalImpact,
            impactOnBudget = budgetImpact,
            alternativeOptions = alternatives,
            reasoning = reasoning,
            recommendations = recommendations,
            riskFactors = riskFactors
        )
    }
    
    private fun calculateAvailableFunds(context: UserFinancialContext): Money {
        // Calculate available funds from checking and savings accounts
        val liquidAccounts = context.accounts.filter { 
            it.accountType == AccountType.CHECKING || it.accountType == AccountType.SAVINGS 
        }
        
        val totalLiquidFunds = liquidAccounts.sumOf { it.balance.amount }
        
        // Reserve emergency fund (if exists)
        val emergencyFundGoal = context.goals.find { it.goalType == GoalType.EMERGENCY_FUND }
        val emergencyReserve = emergencyFundGoal?.currentAmount?.amount ?: 0
        
        // Reserve minimum balance (safety buffer)
        val minimumBalance = 50000 // $500 minimum balance
        
        val availableAmount = totalLiquidFunds - emergencyReserve - minimumBalance
        
        return Money.fromCents(max(0, availableAmount))
    }
    
    private fun calculateRecurringExpenseImpact(expense: ExpenseRequest): Money {
        val multiplier = when (expense.frequency) {
            RecurringFrequency.WEEKLY -> 52.0
            RecurringFrequency.BIWEEKLY -> 26.0
            RecurringFrequency.MONTHLY -> 12.0
            RecurringFrequency.QUARTERLY -> 4.0
            RecurringFrequency.ANNUALLY -> 1.0
            null -> 1.0
        }
        
        return expense.amount * multiplier
    }
    
    private fun analyzeGoalImpact(
        expense: ExpenseRequest,
        context: UserFinancialContext
    ): GoalImpactAnalysis {
        val activeGoals = context.goals.filter { it.isActive }
        val affectedGoals = mutableListOf<GoalImpact>()
        
        var totalDelayDays = 0
        var overallSeverity = GoalImpactSeverity.NONE
        
        for (goal in activeGoals) {
            val currentWeeklyTarget = goal.weeklyTargetAmount
            val expenseWeeklyImpact = if (expense.isRecurring) {
                when (expense.frequency) {
                    RecurringFrequency.WEEKLY -> expense.amount
                    RecurringFrequency.BIWEEKLY -> expense.amount / 2.0
                    RecurringFrequency.MONTHLY -> expense.amount / 4.0
                    RecurringFrequency.QUARTERLY -> expense.amount / 12.0
                    RecurringFrequency.ANNUALLY -> expense.amount / 52.0
                    null -> Money.zero()
                }
            } else {
                expense.amount / 4.0 // Spread one-time expense over 4 weeks
            }
            
            // Calculate delay in days
            val delayWeeks = if (currentWeeklyTarget.amount > 0) {
                expenseWeeklyImpact.amount.toDouble() / currentWeeklyTarget.amount.toDouble()
            } else {
                0.0
            }
            
            val delayDays = (delayWeeks * 7).toInt()
            totalDelayDays = max(totalDelayDays, delayDays)
            
            val impactSeverity = when {
                delayDays == 0 -> GoalImpactSeverity.NONE
                delayDays <= 7 -> GoalImpactSeverity.MINIMAL
                delayDays <= 30 -> GoalImpactSeverity.MODERATE
                delayDays <= 90 -> GoalImpactSeverity.SIGNIFICANT
                else -> GoalImpactSeverity.SEVERE
            }
            
            if (impactSeverity.ordinal > overallSeverity.ordinal) {
                overallSeverity = impactSeverity
            }
            
            affectedGoals.add(
                GoalImpact(
                    goalId = goal.id,
                    goalName = goal.title,
                    impactType = if (delayDays > 0) GoalImpactType.DELAYS else GoalImpactType.NEUTRAL,
                    timeImpact = delayDays,
                    amountImpact = expenseWeeklyImpact
                )
            )
        }
        
        val alternativeStrategies = generateGoalAlternativeStrategies(overallSeverity, expense)
        
        return GoalImpactAnalysis(
            affectedGoals = affectedGoals,
            overallImpact = overallSeverity,
            delayInDays = totalDelayDays,
            alternativeStrategies = alternativeStrategies
        )
    }
    
    private fun analyzeBudgetImpact(
        expense: ExpenseRequest,
        context: UserFinancialContext
    ): BudgetImpactAnalysis {
        val relevantBudget = context.budgets.find { it.category.id == expense.category.id }
        
        val categoryImpact = if (relevantBudget != null) {
            val wouldExceed = expense.amount > relevantBudget.remaining
            val exceedAmount = if (wouldExceed) {
                expense.amount - relevantBudget.remaining
            } else {
                null
            }
            
            CategoryBudgetImpact(
                category = expense.category,
                budgetRemaining = relevantBudget.remaining,
                wouldExceedBudget = wouldExceed,
                exceedAmount = exceedAmount
            )
        } else {
            CategoryBudgetImpact(
                category = expense.category,
                budgetRemaining = Money.zero(),
                wouldExceedBudget = true,
                exceedAmount = expense.amount
            )
        }
        
        val totalBudgetRemaining = context.budgets.sumOf { it.remaining.amount }
        val overallBudgetImpact = expense.amount
        val projectedOverspend = if (expense.amount.amount > totalBudgetRemaining) {
            expense.amount - Money.fromCents(totalBudgetRemaining)
        } else {
            null
        }
        
        val recommendations = generateBudgetRecommendations(categoryImpact, context)
        
        return BudgetImpactAnalysis(
            categoryImpact = categoryImpact,
            overallBudgetImpact = overallBudgetImpact,
            remainingBudget = Money.fromCents(totalBudgetRemaining),
            projectedOverspend = projectedOverspend,
            recommendations = recommendations
        )
    }
    
    private fun generateAlternatives(
        expense: ExpenseRequest,
        context: UserFinancialContext
    ): List<Alternative> {
        val alternatives = mutableListOf<Alternative>()
        
        // Lower cost alternative
        if (expense.amount.amount > 5000) { // > $50
            val lowerCostAmount = expense.amount * 0.7 // 30% less
            alternatives.add(
                Alternative(
                    description = "Consider a lower-cost option for ${expense.description}",
                    amount = lowerCostAmount,
                    pros = listOf(
                        "Reduces financial impact",
                        "Still meets basic needs",
                        "Leaves room for other priorities"
                    ),
                    cons = listOf(
                        "May have fewer features",
                        "Might not be ideal choice"
                    ),
                    feasibility = 0.8f
                )
            )
        }
        
        // Delayed purchase
        if (!expense.isRecurring) {
            val futureDate = expense.plannedDate.plus(30, DateTimeUnit.DAY)
            alternatives.add(
                Alternative(
                    description = "Delay purchase until ${futureDate}",
                    amount = expense.amount,
                    pros = listOf(
                        "More time to save",
                        "Better budget planning",
                        "Avoid impulse buying"
                    ),
                    cons = listOf(
                        "May miss current deals",
                        "Delayed gratification required"
                    ),
                    feasibility = 0.9f
                )
            )
        }
        
        // Payment plan alternative
        if (expense.amount.amount > 20000) { // > $200
            val monthlyPayment = expense.amount / 3.0 // 3-month plan
            alternatives.add(
                Alternative(
                    description = "Split into 3 monthly payments of ${monthlyPayment.format()}",
                    amount = monthlyPayment,
                    pros = listOf(
                        "Easier on monthly budget",
                        "Immediate access to purchase",
                        "Manageable payments"
                    ),
                    cons = listOf(
                        "May incur interest charges",
                        "Ongoing payment commitment"
                    ),
                    feasibility = 0.7f
                )
            )
        }
        
        return alternatives.sortedByDescending { it.feasibility }
    }
    
    private fun calculateAffordabilityConfidence(
        canAfford: Boolean,
        availableFunds: Money,
        expenseAmount: Money,
        goalImpact: GoalImpactAnalysis,
        budgetImpact: BudgetImpactAnalysis
    ): Float {
        var confidence = if (canAfford) 0.7f else 0.3f
        
        // Adjust based on available funds buffer
        val buffer = availableFunds.amount.toDouble() / expenseAmount.amount.toDouble()
        when {
            buffer > 3.0 -> confidence += 0.2f
            buffer > 2.0 -> confidence += 0.1f
            buffer < 1.2 -> confidence -= 0.2f
        }
        
        // Adjust based on goal impact
        when (goalImpact.overallImpact) {
            GoalImpactSeverity.NONE -> confidence += 0.1f
            GoalImpactSeverity.MINIMAL -> confidence += 0.05f
            GoalImpactSeverity.MODERATE -> confidence -= 0.05f
            GoalImpactSeverity.SIGNIFICANT -> confidence -= 0.1f
            GoalImpactSeverity.SEVERE -> confidence -= 0.2f
        }
        
        // Adjust based on budget impact
        if (budgetImpact.categoryImpact.wouldExceedBudget) {
            confidence -= 0.1f
        }
        
        return confidence.coerceIn(0.1f, 0.95f)
    }
    
    private fun generateAffordabilityReasoning(
        canAfford: Boolean,
        availableFunds: Money,
        expenseAmount: Money,
        goalImpact: GoalImpactAnalysis,
        budgetImpact: BudgetImpactAnalysis,
        context: UserFinancialContext
    ): String {
        return buildString {
            if (canAfford) {
                append("You have ${availableFunds.format()} available, which covers the ${expenseAmount.format()} expense. ")
            } else {
                append("You have ${availableFunds.format()} available, but need ${expenseAmount.format()}. ")
            }
            
            when (goalImpact.overallImpact) {
                GoalImpactSeverity.NONE -> append("This won't impact your financial goals. ")
                GoalImpactSeverity.MINIMAL -> append("This may delay your goals by a few days. ")
                GoalImpactSeverity.MODERATE -> append("This could delay your goals by ${goalImpact.delayInDays} days. ")
                GoalImpactSeverity.SIGNIFICANT -> append("This would significantly delay your goals by ${goalImpact.delayInDays} days. ")
                GoalImpactSeverity.SEVERE -> append("This would severely impact your goals, delaying them by ${goalImpact.delayInDays} days. ")
            }
            
            if (budgetImpact.categoryImpact.wouldExceedBudget) {
                budgetImpact.categoryImpact.exceedAmount?.let { exceed ->
                    append("This would exceed your ${budgetImpact.categoryImpact.category.name} budget by ${exceed.format()}. ")
                }
            } else {
                append("This fits within your current budget. ")
            }
        }
    }
    
    private fun generateAffordabilityRecommendations(
        canAfford: Boolean,
        expense: ExpenseRequest,
        goalImpact: GoalImpactAnalysis,
        budgetImpact: BudgetImpactAnalysis,
        context: UserFinancialContext
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (!canAfford) {
            recommendations.add("Consider saving for this expense over the next few weeks")
            recommendations.add("Look for lower-cost alternatives")
            recommendations.add("Review your budget to find areas to cut spending")
        } else {
            if (goalImpact.overallImpact != GoalImpactSeverity.NONE) {
                recommendations.add("Consider the impact on your financial goals before purchasing")
                recommendations.add("You might want to increase your savings rate to compensate")
            }
            
            if (budgetImpact.categoryImpact.wouldExceedBudget) {
                recommendations.add("This would exceed your budget - consider adjusting other spending")
                recommendations.add("You could reallocate budget from other categories")
            }
            
            if (canAfford && goalImpact.overallImpact == GoalImpactSeverity.NONE && !budgetImpact.categoryImpact.wouldExceedBudget) {
                recommendations.add("This purchase looks financially sound - go for it!")
            }
        }
        
        return recommendations
    }
    
    private fun identifyRiskFactors(
        expense: ExpenseRequest,
        context: UserFinancialContext,
        goalImpact: GoalImpactAnalysis,
        budgetImpact: BudgetImpactAnalysis
    ): List<String> {
        val riskFactors = mutableListOf<String>()
        
        // Low emergency fund
        val emergencyFund = context.goals.find { it.goalType == GoalType.EMERGENCY_FUND }
        if (emergencyFund == null || emergencyFund.progressPercentage < 50) {
            riskFactors.add("Low emergency fund - unexpected expenses could be problematic")
        }
        
        // High debt levels
        val debtAccounts = context.accounts.filter { it.isDebt }
        val totalDebt = debtAccounts.sumOf { it.balance.amount }
        if (totalDebt > 100000) { // > $1000 debt
            riskFactors.add("High debt levels - focus on debt reduction might be priority")
        }
        
        // Recurring expense risk
        if (expense.isRecurring) {
            riskFactors.add("Recurring expense will impact budget every period")
        }
        
        // Goal timeline risk
        if (goalImpact.delayInDays > 30) {
            riskFactors.add("Significant delay to financial goals")
        }
        
        // Budget overspend risk
        if (budgetImpact.projectedOverspend != null) {
            riskFactors.add("Would cause budget overspend of ${budgetImpact.projectedOverspend.format()}")
        }
        
        return riskFactors
    }
    
    private fun generateGoalAlternativeStrategies(
        severity: GoalImpactSeverity,
        expense: ExpenseRequest
    ): List<String> {
        return when (severity) {
            GoalImpactSeverity.NONE -> emptyList()
            GoalImpactSeverity.MINIMAL -> listOf(
                "Continue with current savings plan",
                "Consider a small increase in weekly contributions"
            )
            GoalImpactSeverity.MODERATE -> listOf(
                "Increase weekly savings by 10-15%",
                "Look for additional income sources",
                "Reduce spending in other categories"
            )
            GoalImpactSeverity.SIGNIFICANT -> listOf(
                "Significantly increase savings rate",
                "Consider extending goal timeline",
                "Postpone non-essential expenses"
            )
            GoalImpactSeverity.SEVERE -> listOf(
                "Reconsider this expense timing",
                "Extend goal timelines",
                "Focus on highest priority goals only"
            )
        }
    }
    
    private fun generateBudgetRecommendations(
        categoryImpact: CategoryBudgetImpact,
        context: UserFinancialContext
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (categoryImpact.wouldExceedBudget) {
            recommendations.add("Increase ${categoryImpact.category.name} budget allocation")
            recommendations.add("Reduce spending in other categories to compensate")
            recommendations.add("Consider if this is a one-time or recurring budget adjustment")
        } else {
            recommendations.add("This fits within your current budget allocation")
        }
        
        return recommendations
    }
}