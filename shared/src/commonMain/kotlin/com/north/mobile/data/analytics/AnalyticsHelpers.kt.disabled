package com.north.mobile.data.analytics

import com.north.mobile.domain.model.*
import kotlinx.datetime.*
import kotlin.math.*

/**
 * Helper functions for financial analytics calculations
 */
object AnalyticsHelpers {
    
    fun calculatePreviousPeriodComparison(
        currentTransactions: List<Transaction>,
        previousTransactions: List<Transaction>,
        currentPeriod: DateRange,
        previousPeriod: DateRange
    ): PeriodComparison {
        val currentSpent = currentTransactions
            .filter { it.isDebit }
            .fold(Money.zero()) { acc, transaction -> acc + transaction.absoluteAmount }
        
        val previousSpent = previousTransactions
            .filter { it.isDebit }
            .fold(Money.zero()) { acc, transaction -> acc + transaction.absoluteAmount }
        
        val spentChange = currentSpent - previousSpent
        val spentChangePercentage = if (previousSpent.isZero) 0.0 
            else (spentChange.amount.toDouble() / previousSpent.amount.toDouble()) * 100
        
        val categoryChanges = calculateCategoryChanges(currentTransactions, previousTransactions)
        val significantChanges = identifySignificantChanges(categoryChanges)
        
        return PeriodComparison(
            currentPeriod = currentPeriod,
            previousPeriod = previousPeriod,
            totalSpentChange = spentChange,
            totalSpentChangePercentage = spentChangePercentage,
            categoryChanges = categoryChanges,
            significantChanges = significantChanges
        )
    }
    
    fun calculateCategoryChanges(
        currentTransactions: List<Transaction>,
        previousTransactions: List<Transaction>
    ): Map<String, Money> {
        val currentByCategory = currentTransactions
            .filter { it.isDebit }
            .groupBy { it.category.id }
            .mapValues { (_, transactions) ->
                transactions.fold(Money.zero()) { acc, transaction -> acc + transaction.absoluteAmount }
            }
        
        val previousByCategory = previousTransactions
            .filter { it.isDebit }
            .groupBy { it.category.id }
            .mapValues { (_, transactions) ->
                transactions.fold(Money.zero()) { acc, transaction -> acc + transaction.absoluteAmount }
            }
        
        val allCategories = (currentByCategory.keys + previousByCategory.keys).distinct()
        
        return allCategories.associateWith { categoryId ->
            val current = currentByCategory[categoryId] ?: Money.zero()
            val previous = previousByCategory[categoryId] ?: Money.zero()
            current - previous
        }
    }
    
    fun identifySignificantChanges(categoryChanges: Map<String, Money>): List<String> {
        return categoryChanges
            .filter { (_, change) -> abs(change.amount) > 5000 } // $50+ change
            .map { (categoryId, change) ->
                val direction = if (change.isPositive) "increased" else "decreased"
                "Spending in $categoryId $direction by ${change.absoluteValue.format()}"
            }
    }
    
    fun analyzeSpendingTrends(
        transactions: List<Transaction>,
        period: DateRange
    ): List<SpendingTrend> {
        val trends = mutableListOf<SpendingTrend>()
        
        // Analyze overall spending trend
        val weeklySpending = groupTransactionsByWeek(transactions.filter { it.isDebit })
        if (weeklySpending.size >= 2) {
            val trendDirection = calculateTrendDirection(weeklySpending.values.toList())
            val magnitude = calculateTrendMagnitude(weeklySpending.values.toList())
            
            trends.add(
                SpendingTrend(
                    category = null,
                    trendType = TrendType.SPENDING,
                    direction = trendDirection,
                    magnitude = magnitude,
                    confidence = 0.8f,
                    description = "Overall spending trend over the period",
                    timeframe = period
                )
            )
        }
        
        // Analyze category-specific trends
        val categorizedTransactions = transactions.filter { it.isDebit }.groupBy { it.category.id }
        categorizedTransactions.forEach { (_, categoryTransactions) ->
            if (categoryTransactions.size >= 3) {
                val weeklyAmounts = groupTransactionsByWeek(categoryTransactions)
                if (weeklyAmounts.size >= 2) {
                    val trendDirection = calculateTrendDirection(weeklyAmounts.values.toList())
                    val magnitude = calculateTrendMagnitude(weeklyAmounts.values.toList())
                    
                    trends.add(
                        SpendingTrend(
                            category = categoryTransactions.first().category,
                            trendType = TrendType.CATEGORY_SPENDING,
                            direction = trendDirection,
                            magnitude = magnitude,
                            confidence = 0.7f,
                            description = "Category spending trend",
                            timeframe = period
                        )
                    )
                }
            }
        }
        
        return trends
    }
    
    fun groupTransactionsByWeek(transactions: List<Transaction>): Map<Int, Money> {
        return transactions.groupBy { transaction ->
            // Simple week calculation - could be improved
            transaction.date.toEpochDays().toInt() / 7
        }.mapValues { (_, weekTransactions) ->
            weekTransactions.fold(Money.zero()) { acc, transaction -> acc + transaction.absoluteAmount }
        }
    }
    
    fun calculateTrendDirection(amounts: List<Money>): TrendDirection {
        if (amounts.size < 2) return TrendDirection.STABLE
        
        val first = amounts.first().amount.toDouble()
        val last = amounts.last().amount.toDouble()
        val change = if (first != 0.0) (last - first) / first else 0.0
        
        return when {
            change > 0.1 -> TrendDirection.INCREASING
            change < -0.1 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }
    
    fun calculateTrendMagnitude(amounts: List<Money>): Double {
        if (amounts.size < 2) return 0.0
        
        val first = amounts.first().amount.toDouble()
        val last = amounts.last().amount.toDouble()
        
        return if (first != 0.0) abs((last - first) / first) * 100 else 0.0
    }
    
    fun generateSpendingInsights(
        transactions: List<Transaction>,
        categoryBreakdown: List<CategorySpending>
    ): List<SpendingInsight> {
        val insights = mutableListOf<SpendingInsight>()
        
        // Find highest spending category
        val topCategory = categoryBreakdown.maxByOrNull { it.totalAmount.amount }
        if (topCategory != null && topCategory.percentageOfTotal > 30) {
            insights.add(
                SpendingInsight(
                    id = "top_category_${topCategory.category.id}",
                    type = InsightType.SPENDING_PATTERN,
                    title = "High spending in ${topCategory.category.name}",
                    description = "You spent ${topCategory.totalAmount.format()} on ${topCategory.category.name}, which is ${topCategory.percentageOfTotal.toInt()}% of your total spending.",
                    impact = InsightImpact.MEDIUM,
                    actionableRecommendations = listOf(
                        "Review your ${topCategory.category.name} expenses",
                        "Set a budget limit for this category",
                        "Look for ways to reduce spending in this area"
                    ),
                    potentialSavings = topCategory.totalAmount * 0.1, // 10% potential savings
                    category = topCategory.category,
                    confidence = 0.9f
                )
            )
        }
        
        // Find categories with increasing trends
        categoryBreakdown.filter { it.trend == TrendDirection.INCREASING }.forEach { category ->
            insights.add(
                SpendingInsight(
                    id = "increasing_trend_${category.category.id}",
                    type = InsightType.SPENDING_PATTERN,
                    title = "Increasing spending in ${category.category.name}",
                    description = "Your spending in ${category.category.name} has been increasing recently.",
                    impact = InsightImpact.MEDIUM,
                    actionableRecommendations = listOf(
                        "Monitor your ${category.category.name} spending more closely",
                        "Consider setting alerts for this category"
                    ),
                    potentialSavings = null,
                    category = category.category,
                    confidence = 0.7f
                )
            )
        }
        
        // Positive behavior insights
        categoryBreakdown.filter { it.trend == TrendDirection.DECREASING }.forEach { category ->
            insights.add(
                SpendingInsight(
                    id = "positive_trend_${category.category.id}",
                    type = InsightType.POSITIVE_BEHAVIOR,
                    title = "Great job reducing ${category.category.name} spending!",
                    description = "You've successfully reduced your spending in ${category.category.name}. Keep it up!",
                    impact = InsightImpact.LOW,
                    actionableRecommendations = listOf(
                        "Continue your current approach",
                        "Consider applying similar strategies to other categories"
                    ),
                    potentialSavings = category.comparedToPrevious?.absoluteValue,
                    category = category.category,
                    confidence = 0.8f
                )
            )
        }
        
        return insights
    }
    
    fun calculateAssetBreakdown(assets: List<Account>): List<AssetCategory> {
        val totalAssets = assets.fold(Money.zero()) { acc, account -> acc + account.balance }
        
        return assets.groupBy { account ->
            when (account.accountType) {
                AccountType.CHECKING -> AssetType.CHECKING
                AccountType.SAVINGS -> AssetType.SAVINGS
                AccountType.INVESTMENT -> AssetType.INVESTMENT
                else -> AssetType.OTHER
            }
        }.map { (assetType, accounts) ->
            val amount = accounts.fold(Money.zero()) { acc, account -> acc + account.balance }
            val percentage = if (totalAssets.isZero) 0.0 else (amount.amount.toDouble() / totalAssets.amount.toDouble()) * 100
            
            AssetCategory(
                type = assetType,
                amount = amount,
                accounts = accounts.map { it.id },
                percentageOfTotal = percentage,
                monthlyChange = null // Would need historical data
            )
        }
    }
    
    fun calculateLiabilityBreakdown(liabilities: List<Account>): List<LiabilityCategory> {
        val totalLiabilities = liabilities.fold(Money.zero()) { acc, account -> 
            acc + account.balance.absoluteValue 
        }
        
        return liabilities.groupBy { account ->
            when (account.accountType) {
                AccountType.CREDIT_CARD -> LiabilityType.CREDIT_CARD
                AccountType.MORTGAGE -> LiabilityType.MORTGAGE
                AccountType.LOAN -> LiabilityType.LOAN
                else -> LiabilityType.OTHER_DEBT
            }
        }.map { (liabilityType, accounts) ->
            val amount = accounts.fold(Money.zero()) { acc, account -> acc + account.balance.absoluteValue }
            val percentage = if (totalLiabilities.isZero) 0.0 else (amount.amount.toDouble() / totalLiabilities.amount.toDouble()) * 100
            
            LiabilityCategory(
                type = liabilityType,
                amount = amount,
                accounts = accounts.map { it.id },
                percentageOfTotal = percentage,
                monthlyChange = null, // Would need historical data
                interestRate = null // Would need to be stored in account data
            )
        }
    }
    
    fun determineTrend(change: Money?): TrendDirection {
        return when {
            change == null -> TrendDirection.STABLE
            change.isPositive -> TrendDirection.INCREASING
            change.isNegative -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }
    
    fun calculateNetWorthProjections(
        currentNetWorth: Money,
        monthlyChange: Money?,
        months: Int = 12
    ): List<NetWorthProjection> {
        val projections = mutableListOf<NetWorthProjection>()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val change = monthlyChange ?: Money.zero()
        
        for (i in 1..months) {
            val projectedDate = today.plus(i, DateTimeUnit.MONTH)
            val projectedAmount = currentNetWorth + (change * i.toDouble())
            val confidence = max(0.1f, 1.0f - (i * 0.05f)) // Decreasing confidence over time
            
            projections.add(
                NetWorthProjection(
                    date = projectedDate,
                    projectedNetWorth = projectedAmount,
                    confidence = confidence
                )
            )
        }
        
        return projections
    }
    
    fun getCurrentMonthRange(): DateRange {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val startOfMonth = LocalDate(today.year, today.month, 1)
        val endOfMonth = startOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        return DateRange(startOfMonth, endOfMonth)
    }
}