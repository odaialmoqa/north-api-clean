package com.north.mobile.data.ai

import com.north.mobile.domain.model.*
import kotlinx.datetime.*
import kotlin.math.*

/**
 * Explains transactions with context and insights
 */
class TransactionExplainer {
    
    fun explainTransaction(
        transaction: Transaction,
        context: UserFinancialContext
    ): TransactionExplanation {
        
        val summary = generateTransactionSummary(transaction, context)
        val categoryExplanation = generateCategoryExplanation(transaction, context)
        val spendingPatternContext = generateSpendingPatternContext(transaction, context)
        val budgetImpact = generateBudgetImpact(transaction, context)
        val goalImpact = generateGoalImpact(transaction, context)
        val unusualFactors = identifyUnusualFactors(transaction, context)
        val relatedTransactions = findRelatedTransactions(transaction, context)
        val recommendations = generateRecommendations(transaction, context, unusualFactors)
        
        return TransactionExplanation(
            transactionId = transaction.id,
            summary = summary,
            categoryExplanation = categoryExplanation,
            spendingPatternContext = spendingPatternContext,
            budgetImpact = budgetImpact,
            goalImpact = goalImpact,
            unusualFactors = unusualFactors,
            relatedTransactions = relatedTransactions,
            recommendations = recommendations
        )
    }
    
    private fun generateTransactionSummary(
        transaction: Transaction,
        context: UserFinancialContext
    ): String {
        val amount = transaction.amount.absoluteValue
        val merchant = transaction.merchantName ?: transaction.description
        val date = transaction.date
        
        return buildString {
            append("On ${formatDate(date)}, you spent ${amount.format()} at $merchant. ")
            
            if (transaction.isRecurring) {
                append("This appears to be a recurring transaction. ")
            }
            
            // Compare to average transaction
            val averageTransaction = calculateAverageTransaction(context)
            val comparison = amount.amount.toDouble() / averageTransaction.amount.toDouble()
            
            when {
                comparison > 3.0 -> append("This is a large purchase - ${comparison.toInt()}x your average transaction. ")
                comparison > 1.5 -> append("This is above your typical spending amount. ")
                comparison < 0.5 -> append("This is a smaller purchase compared to your usual spending. ")
                else -> append("This is a typical transaction amount for you. ")
            }
        }
    }
    
    private fun generateCategoryExplanation(
        transaction: Transaction,
        context: UserFinancialContext
    ): String {
        val category = transaction.category
        
        // Find similar transactions in the same category
        val categoryTransactions = context.recentTransactions.filter { 
            it.category.id == category.id && it.id != transaction.id 
        }
        
        val categoryTotal = categoryTransactions.sumOf { it.amount.absoluteValue.amount }
        val categoryAverage = if (categoryTransactions.isNotEmpty()) {
            Money.fromCents(categoryTotal / categoryTransactions.size)
        } else {
            Money.zero()
        }
        
        return buildString {
            append("This transaction was categorized as ${category.name}. ")
            
            if (categoryTransactions.isNotEmpty()) {
                append("You've made ${categoryTransactions.size} other ${category.name} transactions recently, ")
                append("with an average of ${categoryAverage.format()} per transaction. ")
                
                val thisVsAverage = transaction.amount.absoluteValue.amount.toDouble() / categoryAverage.amount.toDouble()
                when {
                    thisVsAverage > 2.0 -> append("This transaction is significantly higher than your usual ${category.name} spending. ")
                    thisVsAverage > 1.3 -> append("This is above your typical ${category.name} spending. ")
                    thisVsAverage < 0.7 -> append("This is below your typical ${category.name} spending. ")
                    else -> append("This is consistent with your usual ${category.name} spending. ")
                }
            } else {
                append("This is your first recent transaction in the ${category.name} category. ")
            }
        }
    }
    
    private fun generateSpendingPatternContext(
        transaction: Transaction,
        context: UserFinancialContext
    ): String {
        val dayOfWeek = transaction.date.dayOfWeek
        val timeOfMonth = transaction.date.dayOfMonth
        
        // Analyze spending patterns by day of week
        val dayOfWeekTransactions = context.recentTransactions.filter { 
            it.date.dayOfWeek == dayOfWeek && it.id != transaction.id 
        }
        
        // Analyze spending patterns by time of month
        val timeOfMonthPattern = when (timeOfMonth) {
            in 1..7 -> "beginning"
            in 8..23 -> "middle"
            else -> "end"
        }
        
        return buildString {
            append("This transaction occurred on a ${dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}. ")
            
            if (dayOfWeekTransactions.isNotEmpty()) {
                val avgDaySpending = dayOfWeekTransactions.map { it.amount.absoluteValue.amount }.average()
                val thisVsDay = transaction.amount.absoluteValue.amount / avgDaySpending
                
                when {
                    thisVsDay > 1.5 -> append("You typically spend less on ${dayOfWeek.name.lowercase()}s. ")
                    thisVsDay < 0.7 -> append("This is consistent with your lighter spending on ${dayOfWeek.name.lowercase()}s. ")
                    else -> append("This aligns with your typical ${dayOfWeek.name.lowercase()} spending pattern. ")
                }
            }
            
            append("It occurred during the $timeOfMonthPattern of the month, ")
            
            when (timeOfMonthPattern) {
                "beginning" -> append("which is often when people make larger purchases after payday. ")
                "middle" -> append("a typical time for regular expenses. ")
                "end" -> append("when budgets are often tighter. ")
            }
        }
    }
    
    private fun generateBudgetImpact(
        transaction: Transaction,
        context: UserFinancialContext
    ): String {
        val relevantBudget = context.budgets.find { it.category.id == transaction.category.id }
        
        return if (relevantBudget != null) {
            val percentageOfBudget = (transaction.amount.absoluteValue.amount.toDouble() / relevantBudget.amount.amount.toDouble()) * 100
            val remainingAfter = relevantBudget.remaining - transaction.amount.absoluteValue
            
            buildString {
                append("This transaction used ${percentageOfBudget.toInt()}% of your ${transaction.category.name} budget. ")
                
                if (remainingAfter.amount >= 0) {
                    append("You have ${remainingAfter.format()} remaining in this category. ")
                } else {
                    append("This put you ${remainingAfter.absoluteValue.format()} over budget in this category. ")
                }
                
                when {
                    percentageOfBudget > 50 -> append("This is a significant portion of your budget for this category. ")
                    percentageOfBudget > 25 -> append("This is a moderate impact on your category budget. ")
                    else -> append("This has a minimal impact on your category budget. ")
                }
            }
        } else {
            "You don't have a budget set for ${transaction.category.name}, so I can't assess the budget impact. Consider setting up a budget for better spending control."
        }
    }
    
    private fun generateGoalImpact(
        transaction: Transaction,
        context: UserFinancialContext
    ): String {
        val activeGoals = context.goals.filter { it.isActive }
        
        if (activeGoals.isEmpty()) {
            return "You don't have active financial goals set up, so this transaction doesn't impact any specific targets."
        }
        
        // Calculate impact on savings rate
        val totalWeeklyGoalTarget = activeGoals.sumOf { it.weeklyTargetAmount.amount }
        val transactionWeeklyImpact = transaction.amount.absoluteValue.amount / 4.0 // Spread over 4 weeks
        
        val impactPercentage = if (totalWeeklyGoalTarget > 0) {
            (transactionWeeklyImpact / totalWeeklyGoalTarget) * 100
        } else {
            0.0
        }
        
        return buildString {
            when {
                impactPercentage > 25 -> {
                    append("This transaction significantly impacts your goal progress. ")
                    append("It represents ${impactPercentage.toInt()}% of your weekly goal contributions. ")
                    append("Consider if this purchase aligns with your financial priorities. ")
                }
                impactPercentage > 10 -> {
                    append("This transaction has a moderate impact on your goal progress. ")
                    append("It's equivalent to ${impactPercentage.toInt()}% of your weekly goal savings. ")
                }
                impactPercentage > 0 -> {
                    append("This transaction has a minimal impact on your goal progress. ")
                    append("Your goals remain on track. ")
                }
                else -> {
                    append("This transaction doesn't significantly impact your goal timeline. ")
                }
            }
            
            // Mention specific goals if severely impacted
            if (impactPercentage > 25) {
                val mostImpactedGoal = activeGoals.maxByOrNull { it.weeklyTargetAmount.amount }
                mostImpactedGoal?.let { goal ->
                    append("Your '${goal.title}' goal may be most affected. ")
                }
            }
        }
    }
    
    private fun identifyUnusualFactors(
        transaction: Transaction,
        context: UserFinancialContext
    ): List<String> {
        val unusualFactors = mutableListOf<String>()
        
        // Check if amount is unusual
        val averageTransaction = calculateAverageTransaction(context)
        val amountRatio = transaction.amount.absoluteValue.amount.toDouble() / averageTransaction.amount.toDouble()
        
        if (amountRatio > 3.0) {
            unusualFactors.add("Transaction amount is ${amountRatio.toInt()}x larger than your average")
        }
        
        // Check if merchant is new
        val merchantName = transaction.merchantName ?: transaction.description
        val previousMerchantTransactions = context.recentTransactions.filter { 
            (it.merchantName == merchantName || it.description == merchantName) && it.id != transaction.id 
        }
        
        if (previousMerchantTransactions.isEmpty()) {
            unusualFactors.add("First time transaction at this merchant")
        }
        
        // Check if category spending is unusual
        val categoryTransactions = context.recentTransactions.filter { 
            it.category.id == transaction.category.id && it.id != transaction.id 
        }
        
        if (categoryTransactions.isNotEmpty()) {
            val categoryAverage = categoryTransactions.map { it.amount.absoluteValue.amount }.average()
            val categoryRatio = transaction.amount.absoluteValue.amount / categoryAverage
            
            if (categoryRatio > 2.0) {
                unusualFactors.add("Much higher than typical ${transaction.category.name} spending")
            }
        }
        
        // Check if timing is unusual (weekend vs weekday patterns)
        val isWeekend = transaction.date.dayOfWeek == DayOfWeek.SATURDAY || transaction.date.dayOfWeek == DayOfWeek.SUNDAY
        val weekendTransactions = context.recentTransactions.filter { 
            val isTransactionWeekend = it.date.dayOfWeek == DayOfWeek.SATURDAY || it.date.dayOfWeek == DayOfWeek.SUNDAY
            isTransactionWeekend == isWeekend && it.id != transaction.id
        }
        
        if (weekendTransactions.isNotEmpty()) {
            val weekendAverage = weekendTransactions.map { it.amount.absoluteValue.amount }.average()
            val timingRatio = transaction.amount.absoluteValue.amount / weekendAverage
            
            if (timingRatio > 2.0) {
                val dayType = if (isWeekend) "weekend" else "weekday"
                unusualFactors.add("Higher than typical $dayType spending")
            }
        }
        
        return unusualFactors
    }
    
    private fun findRelatedTransactions(
        transaction: Transaction,
        context: UserFinancialContext
    ): List<Transaction> {
        val relatedTransactions = mutableListOf<Transaction>()
        
        // Find transactions at the same merchant
        val merchantName = transaction.merchantName ?: transaction.description
        val merchantTransactions = context.recentTransactions.filter { 
            (it.merchantName == merchantName || it.description == merchantName) && 
            it.id != transaction.id 
        }.take(3)
        
        relatedTransactions.addAll(merchantTransactions)
        
        // Find transactions in the same category on the same day
        val sameDayCategory = context.recentTransactions.filter { 
            it.category.id == transaction.category.id && 
            it.date == transaction.date && 
            it.id != transaction.id 
        }.take(2)
        
        relatedTransactions.addAll(sameDayCategory)
        
        // Find similar amount transactions
        val similarAmount = context.recentTransactions.filter { 
            val amountDiff = abs(it.amount.absoluteValue.amount - transaction.amount.absoluteValue.amount)
            amountDiff < transaction.amount.absoluteValue.amount * 0.1 && // Within 10%
            it.id != transaction.id
        }.take(2)
        
        relatedTransactions.addAll(similarAmount)
        
        return relatedTransactions.distinctBy { it.id }.take(5)
    }
    
    private fun generateRecommendations(
        transaction: Transaction,
        context: UserFinancialContext,
        unusualFactors: List<String>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Recommendations based on unusual factors
        if (unusualFactors.any { it.contains("larger than") || it.contains("higher than") }) {
            recommendations.add("Review if this large expense was planned and necessary")
            recommendations.add("Consider if this should be a one-time purchase or if you need to adjust your budget")
        }
        
        if (unusualFactors.any { it.contains("First time") }) {
            recommendations.add("Verify this new merchant charge is legitimate")
            recommendations.add("Consider adding this merchant to your regular spending if it will be recurring")
        }
        
        // Budget-based recommendations
        val relevantBudget = context.budgets.find { it.category.id == transaction.category.id }
        if (relevantBudget != null && relevantBudget.remaining.amount < 0) {
            recommendations.add("You're over budget in ${transaction.category.name} - consider reducing spending in this category")
            recommendations.add("Look for ways to reallocate budget from other categories")
        }
        
        // Goal-based recommendations
        val activeGoals = context.goals.filter { it.isActive }
        if (activeGoals.isNotEmpty()) {
            val totalWeeklyGoalTarget = activeGoals.sumOf { it.weeklyTargetAmount.amount }
            val transactionWeeklyImpact = transaction.amount.absoluteValue.amount / 4.0
            
            if (transactionWeeklyImpact > totalWeeklyGoalTarget * 0.25) {
                recommendations.add("This expense impacts your goal progress - consider increasing your savings rate to compensate")
                recommendations.add("Evaluate if this purchase aligns with your financial priorities")
            }
        }
        
        // Category-specific recommendations
        when (transaction.category.id) {
            Category.RESTAURANTS.id -> {
                recommendations.add("Consider cooking at home more often to reduce dining expenses")
            }
            Category.SHOPPING.id -> {
                recommendations.add("Review if this purchase was planned or impulse buying")
                recommendations.add("Consider implementing a 24-hour waiting period for non-essential purchases")
            }
            Category.ENTERTAINMENT.id -> {
                recommendations.add("Look for free or lower-cost entertainment alternatives")
            }
        }
        
        // Positive reinforcement for good spending
        if (unusualFactors.isEmpty() && relevantBudget?.remaining?.amount ?: 0 > 0) {
            recommendations.add("This looks like responsible spending within your budget - well done!")
        }
        
        return recommendations.take(4) // Limit to top 4 recommendations
    }
    
    private fun calculateAverageTransaction(context: UserFinancialContext): Money {
        val transactions = context.recentTransactions.filter { it.amount.isNegative } // Only expenses
        
        return if (transactions.isNotEmpty()) {
            val totalAmount = transactions.sumOf { it.amount.absoluteValue.amount }
            Money.fromCents(totalAmount / transactions.size)
        } else {
            Money.fromDollars(50.0) // Default average
        }
    }
    
    private fun formatDate(date: LocalDate): String {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val daysAgo = today.toEpochDays() - date.toEpochDays()
        
        return when (daysAgo.toInt()) {
            0 -> "today"
            1 -> "yesterday"
            in 2..7 -> "$daysAgo days ago"
            else -> date.toString()
        }
    }
}