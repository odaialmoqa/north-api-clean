package com.north.mobile.data.finance

import com.north.mobile.data.plaid.SimplePlaidTransaction

// Data classes for financial analysis

data class SpendingAnalysis(
    val totalSpent: Double,
    val categories: Map<String, Double>,
    val topCategory: CategorySpending?,
    val averageDailySpend: Double,
    val largestTransaction: SimplePlaidTransaction?,
    val recurringExpenses: List<RecurringExpense>,
    val unusualTransactions: List<SimplePlaidTransaction>
)

data class CategorySpending(
    val category: String,
    val amount: Double,
    val percentOfTotal: Double
)

data class RecurringExpense(
    val merchant: String,
    val averageAmount: Double,
    val frequency: IncomeFrequency,
    val lastDate: String,
    val category: String
)

data class IncomeAnalysis(
    val totalIncome: Double,
    val averageMonthlyIncome: Double,
    val incomeFrequency: IncomeFrequency,
    val lastPayday: String?,
    val nextEstimatedPayday: String?,
    val regularIncomeSources: List<IncomeSource>
)

data class IncomeSource(
    val source: String,
    val averageAmount: Double,
    val frequency: IncomeFrequency,
    val lastDate: String
)

enum class IncomeFrequency {
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    UNKNOWN
}

data class SavingsAnalysis(
    val totalBalance: Double,
    val savingsBalance: Double,
    val checkingBalance: Double,
    val savingsRate: Double, // Savings as % of total balance
    val monthlySavingsRate: Double, // (Income - Expenses) / Income
    val savingsOpportunities: List<SavingsOpportunity>
)

data class SavingsOpportunity(
    val category: String,
    val currentSpending: Double,
    val potentialSavings: Double,
    val percentOfTotal: Double,
    val tips: List<String>
)

data class FinancialHealthAnalysis(
    val overallScore: Int, // 0-100
    val savingsScore: Int, // 0-100
    val spendingScore: Int, // 0-100
    val debtScore: Int, // 0-100
    val insights: List<FinancialInsight>,
    val recommendations: List<FinancialRecommendation>
)

data class FinancialInsight(
    val title: String,
    val description: String,
    val type: InsightType,
    val score: Int, // 0-100
    val emoji: String
)

enum class InsightType {
    OVERALL_HEALTH,
    SAVINGS,
    SPENDING,
    DEBT,
    INCOME,
    RECURRING_EXPENSES
}

data class FinancialRecommendation(
    val title: String,
    val description: String,
    val priority: RecommendationPriority,
    val actionable: Boolean,
    val type: RecommendationType,
    val emoji: String,
    val tips: List<String> = emptyList()
)

enum class RecommendationPriority {
    HIGH,
    MEDIUM,
    LOW
}

enum class RecommendationType {
    SAVINGS,
    SPENDING,
    DEBT,
    INCOME,
    INVESTMENT
}

data class FinancialGoal(
    val id: String,
    val name: String,
    val type: GoalType,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: Long,
    val priority: GoalPriority,
    val description: String,
    val createdAt: Long,
    val emoji: String
)

enum class GoalType {
    EMERGENCY_FUND,
    SAVINGS_RATE,
    DEBT_REDUCTION,
    MAJOR_PURCHASE,
    VACATION,
    EDUCATION,
    RETIREMENT,
    HOME_PURCHASE,
    CUSTOM
}

enum class GoalPriority {
    HIGH,
    MEDIUM,
    LOW
}