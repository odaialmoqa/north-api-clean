package com.north.mobile.data.finance

import com.north.mobile.data.plaid.SimplePlaidAccount
import com.north.mobile.data.plaid.SimplePlaidTransaction
import kotlin.math.abs

/**
 * Analyzes financial data from Plaid to generate insights and recommendations
 */
class FinancialDataAnalyzer {
    
    /**
     * Analyze spending patterns from transactions
     */
    fun analyzeSpendingPatterns(
        transactions: List<SimplePlaidTransaction>,
        timeRangeDays: Int = 30
    ): SpendingAnalysis {
        if (transactions.isEmpty()) {
            return SpendingAnalysis(
                totalSpent = 0.0,
                categories = emptyMap(),
                topCategory = null,
                averageDailySpend = 0.0,
                largestTransaction = null,
                recurringExpenses = emptyList(),
                unusualTransactions = emptyList()
            )
        }
        
        // Filter to recent transactions within timeRangeDays
        val recentTransactions = filterRecentTransactions(transactions, timeRangeDays)
        
        // Calculate total spent (negative amounts are expenses)
        val expenses = recentTransactions.filter { it.amount < 0 }
        val totalSpent = expenses.sumOf { abs(it.amount) }
        
        // Categorize spending
        val categorySpending = expenses.groupBy { 
            it.category.firstOrNull() ?: "Other" 
        }.mapValues { (_, txns) ->
            txns.sumOf { abs(it.amount) }
        }
        
        // Find top spending category
        val topCategory = categorySpending.maxByOrNull { it.value }?.let { (category, amount) ->
            CategorySpending(category, amount, amount / totalSpent * 100)
        }
        
        // Calculate average daily spend
        val averageDailySpend = if (timeRangeDays > 0) totalSpent / timeRangeDays else totalSpent
        
        // Find largest transaction
        val largestTransaction = expenses.maxByOrNull { abs(it.amount) }
        
        // Detect recurring expenses
        val recurringExpenses = detectRecurringExpenses(expenses)
        
        // Detect unusual transactions
        val unusualTransactions = detectUnusualTransactions(expenses, averageDailySpend)
        
        return SpendingAnalysis(
            totalSpent = totalSpent,
            categories = categorySpending,
            topCategory = topCategory,
            averageDailySpend = averageDailySpend,
            largestTransaction = largestTransaction,
            recurringExpenses = recurringExpenses,
            unusualTransactions = unusualTransactions
        )
    }
    
    /**
     * Analyze income patterns from transactions
     */
    fun analyzeIncomePatterns(
        transactions: List<SimplePlaidTransaction>,
        timeRangeDays: Int = 60
    ): IncomeAnalysis {
        if (transactions.isEmpty()) {
            return IncomeAnalysis(
                totalIncome = 0.0,
                averageMonthlyIncome = 0.0,
                incomeFrequency = IncomeFrequency.UNKNOWN,
                lastPayday = null,
                nextEstimatedPayday = null,
                regularIncomeSources = emptyList()
            )
        }
        
        // Filter to recent transactions within timeRangeDays
        val recentTransactions = filterRecentTransactions(transactions, timeRangeDays)
        
        // Calculate total income (positive amounts are income)
        val incomeTransactions = recentTransactions.filter { it.amount > 0 }
        val totalIncome = incomeTransactions.sumOf { it.amount }
        
        // Calculate average monthly income
        val monthsInRange = timeRangeDays / 30.0
        val averageMonthlyIncome = if (monthsInRange > 0) totalIncome / monthsInRange else totalIncome
        
        // Detect income frequency
        val (incomeFrequency, regularIncomeSources) = detectIncomeFrequency(incomeTransactions)
        
        // Find last payday
        val lastPayday = regularIncomeSources.maxOfOrNull { it.lastDate }
        
        // Estimate next payday
        val nextEstimatedPayday = estimateNextPayday(lastPayday, incomeFrequency)
        
        return IncomeAnalysis(
            totalIncome = totalIncome,
            averageMonthlyIncome = averageMonthlyIncome,
            incomeFrequency = incomeFrequency,
            lastPayday = lastPayday,
            nextEstimatedPayday = nextEstimatedPayday,
            regularIncomeSources = regularIncomeSources
        )
    }
    
    /**
     * Calculate savings rate and opportunities
     */
    fun analyzeSavingsRate(
        accounts: List<SimplePlaidAccount>,
        transactions: List<SimplePlaidTransaction>,
        timeRangeDays: Int = 30
    ): SavingsAnalysis {
        if (accounts.isEmpty()) {
            return SavingsAnalysis(
                totalBalance = 0.0,
                savingsBalance = 0.0,
                checkingBalance = 0.0,
                savingsRate = 0.0,
                monthlySavingsRate = 0.0,
                savingsOpportunities = emptyList()
            )
        }
        
        // Calculate balances by account type
        val totalBalance = accounts.sumOf { it.balance }
        val savingsBalance = accounts.filter { 
            it.type.lowercase() == "savings" || it.subtype?.lowercase() == "savings" 
        }.sumOf { it.balance }
        val checkingBalance = accounts.filter { 
            it.type.lowercase() == "checking" || it.subtype?.lowercase() == "checking" 
        }.sumOf { it.balance }
        
        // Calculate savings rate (savings as % of total)
        val savingsRate = if (totalBalance > 0) savingsBalance / totalBalance * 100 else 0.0
        
        // Calculate monthly savings rate from transactions
        val recentTransactions = filterRecentTransactions(transactions, timeRangeDays)
        val income = recentTransactions.filter { it.amount > 0 }.sumOf { it.amount }
        val expenses = recentTransactions.filter { it.amount < 0 }.sumOf { abs(it.amount) }
        val monthlySavingsRate = if (income > 0) (income - expenses) / income * 100 else 0.0
        
        // Identify savings opportunities
        val savingsOpportunities = identifySavingsOpportunities(
            recentTransactions,
            monthlySavingsRate
        )
        
        return SavingsAnalysis(
            totalBalance = totalBalance,
            savingsBalance = savingsBalance,
            checkingBalance = checkingBalance,
            savingsRate = savingsRate,
            monthlySavingsRate = monthlySavingsRate,
            savingsOpportunities = savingsOpportunities
        )
    }
    
    /**
     * Generate financial health score and insights
     */
    fun calculateFinancialHealthScore(
        accounts: List<SimplePlaidAccount>,
        transactions: List<SimplePlaidTransaction>,
        timeRangeDays: Int = 60
    ): FinancialHealthAnalysis {
        if (accounts.isEmpty()) {
            return FinancialHealthAnalysis(
                overallScore = 0,
                savingsScore = 0,
                spendingScore = 0,
                debtScore = 0,
                insights = emptyList(),
                recommendations = emptyList()
            )
        }
        
        // Calculate savings analysis
        val savingsAnalysis = analyzeSavingsRate(accounts, transactions, timeRangeDays)
        
        // Calculate spending analysis
        val spendingAnalysis = analyzeSpendingPatterns(transactions, timeRangeDays)
        
        // Calculate income analysis
        val incomeAnalysis = analyzeIncomePatterns(transactions, timeRangeDays)
        
        // Calculate savings score (0-100)
        val savingsScore = calculateSavingsScore(savingsAnalysis)
        
        // Calculate spending score (0-100)
        val spendingScore = calculateSpendingScore(spendingAnalysis, incomeAnalysis)
        
        // Calculate debt score (0-100)
        val debtScore = calculateDebtScore(accounts, transactions)
        
        // Calculate overall financial health score
        val overallScore = (savingsScore + spendingScore + debtScore) / 3
        
        // Generate insights
        val insights = generateFinancialInsights(
            savingsAnalysis,
            spendingAnalysis,
            incomeAnalysis,
            overallScore
        )
        
        // Generate recommendations
        val recommendations = generateFinancialRecommendations(
            savingsAnalysis,
            spendingAnalysis,
            incomeAnalysis,
            overallScore
        )
        
        return FinancialHealthAnalysis(
            overallScore = overallScore,
            savingsScore = savingsScore,
            spendingScore = spendingScore,
            debtScore = debtScore,
            insights = insights,
            recommendations = recommendations
        )
    }
    
    // Helper methods
    
    private fun filterRecentTransactions(
        transactions: List<SimplePlaidTransaction>,
        timeRangeDays: Int
    ): List<SimplePlaidTransaction> {
        return transactions.filter { isWithinLastDays(it.date, timeRangeDays) }
    }
    
    private fun isWithinLastDays(dateStr: String, days: Int): Boolean {
        try {
            val parts = dateStr.split("-").map { it.toInt() }
            if (parts.size != 3) return false
            
            val transactionDate = java.util.Calendar.getInstance().apply {
                set(parts[0], parts[1] - 1, parts[2]) // Year, Month (0-based), Day
            }
            
            val cutoffDate = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, -days)
            }
            
            return transactionDate.after(cutoffDate)
        } catch (e: Exception) {
            return false
        }
    }
    
    private fun detectRecurringExpenses(
        expenses: List<SimplePlaidTransaction>
    ): List<RecurringExpense> {
        // Group by merchant name or description
        val merchantGroups = expenses.groupBy { it.merchantName ?: it.description }
        
        return merchantGroups.mapNotNull { (merchant, transactions) ->
            // Only consider merchants with multiple transactions
            if (transactions.size < 2) return@mapNotNull null
            
            // Check if amounts are similar
            val amounts = transactions.map { abs(it.amount) }
            val averageAmount = amounts.average()
            val amountVariance = amounts.map { abs(it - averageAmount) }.average()
            
            // If amounts are consistent and there are multiple transactions, likely recurring
            if (amountVariance < averageAmount * 0.1 && transactions.size >= 2) {
                RecurringExpense(
                    merchant = merchant,
                    averageAmount = averageAmount,
                    frequency = estimateFrequency(transactions.map { it.date }),
                    lastDate = transactions.maxOf { it.date },
                    category = transactions.first().category.firstOrNull() ?: "Other"
                )
            } else null
        }
    }
    
    private fun detectUnusualTransactions(
        expenses: List<SimplePlaidTransaction>,
        averageDailySpend: Double
    ): List<SimplePlaidTransaction> {
        // Transactions significantly larger than average daily spend
        return expenses.filter { abs(it.amount) > averageDailySpend * 3 }
    }
    
    private fun detectIncomeFrequency(
        incomeTransactions: List<SimplePlaidTransaction>
    ): Pair<IncomeFrequency, List<IncomeSource>> {
        if (incomeTransactions.isEmpty()) {
            return Pair(IncomeFrequency.UNKNOWN, emptyList())
        }
        
        // Group by source (merchant or description)
        val sourceGroups = incomeTransactions.groupBy { it.merchantName ?: it.description }
        
        val incomeSources = sourceGroups.mapNotNull { (source, transactions) ->
            if (transactions.size < 2) return@mapNotNull null
            
            // Check if amounts are similar
            val amounts = transactions.map { it.amount }
            val averageAmount = amounts.average()
            val amountVariance = amounts.map { abs(it - averageAmount) }.average()
            
            // If amounts are consistent, likely regular income
            if (amountVariance < averageAmount * 0.1) {
                val frequency = estimateFrequency(transactions.map { it.date })
                
                IncomeSource(
                    source = source,
                    averageAmount = averageAmount,
                    frequency = frequency,
                    lastDate = transactions.maxOf { it.date }
                )
            } else null
        }
        
        // Determine overall income frequency based on most common source frequency
        val overallFrequency = if (incomeSources.isNotEmpty()) {
            incomeSources.groupBy { it.frequency }
                .maxByOrNull { it.value.size }?.key ?: IncomeFrequency.UNKNOWN
        } else {
            IncomeFrequency.UNKNOWN
        }
        
        return Pair(overallFrequency, incomeSources)
    }
    
    private fun estimateFrequency(dates: List<String>): IncomeFrequency {
        if (dates.size < 2) return IncomeFrequency.UNKNOWN
        
        try {
            // Parse dates and sort them
            val sortedDates = dates.map { dateStr ->
                val parts = dateStr.split("-").map { it.toInt() }
                java.util.Calendar.getInstance().apply {
                    set(parts[0], parts[1] - 1, parts[2]) // Year, Month (0-based), Day
                }.timeInMillis
            }.sorted()
            
            // Calculate average interval in days
            var totalInterval = 0L
            for (i in 1 until sortedDates.size) {
                totalInterval += sortedDates[i] - sortedDates[i-1]
            }
            val avgIntervalDays = totalInterval / (sortedDates.size - 1) / (24 * 60 * 60 * 1000)
            
            return when {
                avgIntervalDays <= 8 -> IncomeFrequency.WEEKLY
                avgIntervalDays <= 16 -> IncomeFrequency.BIWEEKLY
                avgIntervalDays <= 35 -> IncomeFrequency.MONTHLY
                else -> IncomeFrequency.UNKNOWN
            }
        } catch (e: Exception) {
            return IncomeFrequency.UNKNOWN
        }
    }
    
    private fun estimateNextPayday(lastPayday: String?, frequency: IncomeFrequency): String? {
        if (lastPayday == null) return null
        
        try {
            val parts = lastPayday.split("-").map { it.toInt() }
            val calendar = java.util.Calendar.getInstance().apply {
                set(parts[0], parts[1] - 1, parts[2]) // Year, Month (0-based), Day
            }
            
            when (frequency) {
                IncomeFrequency.WEEKLY -> calendar.add(java.util.Calendar.DAY_OF_YEAR, 7)
                IncomeFrequency.BIWEEKLY -> calendar.add(java.util.Calendar.DAY_OF_YEAR, 14)
                IncomeFrequency.MONTHLY -> calendar.add(java.util.Calendar.MONTH, 1)
                else -> return null
            }
            
            return "${calendar.get(java.util.Calendar.YEAR)}-${calendar.get(java.util.Calendar.MONTH) + 1}-${calendar.get(java.util.Calendar.DAY_OF_MONTH)}"
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun identifySavingsOpportunities(
        transactions: List<SimplePlaidTransaction>,
        currentSavingsRate: Double
    ): List<SavingsOpportunity> {
        val opportunities = mutableListOf<SavingsOpportunity>()
        
        // Group expenses by category
        val categoryExpenses = transactions.filter { it.amount < 0 }
            .groupBy { it.category.firstOrNull() ?: "Other" }
            .mapValues { (_, txns) -> txns.sumOf { abs(it.amount) } }
        
        // Find high-spending categories
        val totalExpenses = categoryExpenses.values.sum()
        
        categoryExpenses.forEach { (category, amount) ->
            val percentOfSpending = amount / totalExpenses * 100
            
            // Categories that make up a significant portion of spending
            if (percentOfSpending > 15 && amount > 100) {
                val potentialSavings = when (category) {
                    "Food and Drink" -> amount * 0.2 // 20% potential savings
                    "Shopping" -> amount * 0.25 // 25% potential savings
                    "Entertainment" -> amount * 0.3 // 30% potential savings
                    else -> amount * 0.15 // 15% potential savings for other categories
                }
                
                opportunities.add(
                    SavingsOpportunity(
                        category = category,
                        currentSpending = amount,
                        potentialSavings = potentialSavings,
                        percentOfTotal = percentOfSpending,
                        tips = getSavingsTipsForCategory(category)
                    )
                )
            }
        }
        
        return opportunities
    }
    
    private fun getSavingsTipsForCategory(category: String): List<String> {
        return when (category) {
            "Food and Drink" -> listOf(
                "Meal prep on weekends to reduce takeout",
                "Use grocery store loyalty programs",
                "Limit dining out to once per week"
            )
            "Shopping" -> listOf(
                "Wait 24 hours before making non-essential purchases",
                "Look for second-hand options for clothing",
                "Use price comparison tools before buying"
            )
            "Entertainment" -> listOf(
                "Share subscription services with family",
                "Look for free community events",
                "Use library services for books and media"
            )
            else -> listOf(
                "Review your spending in this category for potential cuts",
                "Look for more affordable alternatives",
                "Set a monthly budget for this category"
            )
        }
    }
    
    private fun calculateSavingsScore(savingsAnalysis: SavingsAnalysis): Int {
        var score = 0
        
        // Score based on savings rate (0-40 points)
        score += when {
            savingsAnalysis.savingsRate >= 30 -> 40
            savingsAnalysis.savingsRate >= 20 -> 30
            savingsAnalysis.savingsRate >= 10 -> 20
            savingsAnalysis.savingsRate >= 5 -> 10
            else -> 0
        }
        
        // Score based on monthly savings rate (0-40 points)
        score += when {
            savingsAnalysis.monthlySavingsRate >= 25 -> 40
            savingsAnalysis.monthlySavingsRate >= 15 -> 30
            savingsAnalysis.monthlySavingsRate >= 10 -> 20
            savingsAnalysis.monthlySavingsRate >= 5 -> 10
            else -> 0
        }
        
        // Score based on absolute savings amount (0-20 points)
        score += when {
            savingsAnalysis.savingsBalance >= 10000 -> 20
            savingsAnalysis.savingsBalance >= 5000 -> 15
            savingsAnalysis.savingsBalance >= 1000 -> 10
            savingsAnalysis.savingsBalance >= 500 -> 5
            else -> 0
        }
        
        return score.coerceIn(0, 100)
    }
    
    private fun calculateSpendingScore(
        spendingAnalysis: SpendingAnalysis,
        incomeAnalysis: IncomeAnalysis
    ): Int {
        var score = 50 // Start with neutral score
        
        // If no income data, use basic scoring
        if (incomeAnalysis.averageMonthlyIncome <= 0) {
            return score
        }
        
        // Calculate monthly spending ratio (spending / income)
        val monthlySpending = spendingAnalysis.totalSpent
        val spendingRatio = monthlySpending / incomeAnalysis.averageMonthlyIncome
        
        // Score based on spending ratio (±50 points)
        score += when {
            spendingRatio <= 0.5 -> 50 // Spending less than 50% of income
            spendingRatio <= 0.7 -> 40
            spendingRatio <= 0.8 -> 30
            spendingRatio <= 0.9 -> 20
            spendingRatio <= 1.0 -> 10
            spendingRatio <= 1.1 -> 0
            spendingRatio <= 1.2 -> -10
            spendingRatio <= 1.3 -> -20
            spendingRatio <= 1.5 -> -30
            else -> -40 // Spending much more than income
        }
        
        return score.coerceIn(0, 100)
    }
    
    private fun calculateDebtScore(accounts: List<SimplePlaidAccount>, transactions: List<SimplePlaidTransaction>): Int {
        // Start with perfect score and deduct based on debt factors
        var score = 100
        
        // Calculate total assets and debts
        val totalAssets = accounts.filter { it.balance > 0 }.sumOf { it.balance }
        val totalDebts = accounts.filter { it.balance < 0 }.sumOf { abs(it.balance) }
        
        // No debt is perfect
        if (totalDebts <= 0) return 100
        
        // Calculate debt-to-asset ratio
        val debtToAssetRatio = if (totalAssets > 0) totalDebts / totalAssets else Double.MAX_VALUE
        
        // Score based on debt-to-asset ratio (0-50 points)
        score -= when {
            debtToAssetRatio >= 2.0 -> 50
            debtToAssetRatio >= 1.5 -> 40
            debtToAssetRatio >= 1.0 -> 30
            debtToAssetRatio >= 0.75 -> 20
            debtToAssetRatio >= 0.5 -> 10
            debtToAssetRatio >= 0.25 -> 5
            else -> 0
        }
        
        return score.coerceIn(0, 100)
    }
    
    private fun generateFinancialInsights(
        savingsAnalysis: SavingsAnalysis,
        spendingAnalysis: SpendingAnalysis,
        incomeAnalysis: IncomeAnalysis,
        overallScore: Int
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()
        
        // Overall financial health insight
        insights.add(
            FinancialInsight(
                title = "Financial Health",
                description = when {
                    overallScore >= 80 -> "Your financial health is excellent! You're making great choices."
                    overallScore >= 60 -> "Your financial health is good. Some small improvements could help."
                    overallScore >= 40 -> "Your financial health needs attention in a few key areas."
                    else -> "Your financial health needs significant improvement. Let's work on this together."
                },
                type = InsightType.OVERALL_HEALTH,
                score = overallScore,
                emoji = when {
                    overallScore >= 80 -> "🌟"
                    overallScore >= 60 -> "👍"
                    overallScore >= 40 -> "⚠️"
                    else -> "❗"
                }
            )
        )
        
        return insights
    }
    
    private fun generateFinancialRecommendations(
        savingsAnalysis: SavingsAnalysis,
        spendingAnalysis: SpendingAnalysis,
        incomeAnalysis: IncomeAnalysis,
        overallScore: Int
    ): List<FinancialRecommendation> {
        val recommendations = mutableListOf<FinancialRecommendation>()
        
        // Emergency fund recommendation
        if (savingsAnalysis.savingsBalance < 1000) {
            recommendations.add(
                FinancialRecommendation(
                    title = "Start an Emergency Fund",
                    description = "Work toward saving $1,000 as a starter emergency fund.",
                    priority = RecommendationPriority.HIGH,
                    actionable = true,
                    type = RecommendationType.SAVINGS,
                    emoji = "🛡️"
                )
            )
        }
        
        return recommendations
    }
}