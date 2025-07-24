package com.north.mobile.data.analytics

import com.north.mobile.domain.model.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Comprehensive spending analysis for a user
 */
@Serializable
data class SpendingAnalysis(
    val userId: String,
    val period: DateRange,
    val totalSpent: Money,
    val totalIncome: Money,
    val netCashFlow: Money,
    val categoryBreakdown: List<CategorySpending>,
    val trends: List<SpendingTrend>,
    val insights: List<SpendingInsight>,
    val comparisonToPrevious: PeriodComparison?,
    val generatedAt: Instant
)

/**
 * Spending breakdown by category
 */
@Serializable
data class CategorySpending(
    val category: Category,
    val totalAmount: Money,
    val transactionCount: Int,
    val averageAmount: Money,
    val percentageOfTotal: Double,
    val trend: TrendDirection,
    val comparedToPrevious: Money? = null
)

/**
 * Spending trend over time
 */
@Serializable
data class SpendingTrend(
    val category: Category?,
    val trendType: TrendType,
    val direction: TrendDirection,
    val magnitude: Double, // Percentage change
    val confidence: Float,
    val description: String,
    val timeframe: DateRange
)

/**
 * Personalized spending insight
 */
@Serializable
data class SpendingInsight(
    val id: String,
    val type: InsightType,
    val title: String,
    val description: String,
    val impact: InsightImpact,
    val actionableRecommendations: List<String>,
    val potentialSavings: Money?,
    val category: Category?,
    val confidence: Float
)

/**
 * Net worth calculation and tracking
 */
@Serializable
data class NetWorthSummary(
    val userId: String,
    val calculatedAt: Instant,
    val totalAssets: Money,
    val totalLiabilities: Money,
    val netWorth: Money,
    val assetBreakdown: List<AssetCategory>,
    val liabilityBreakdown: List<LiabilityCategory>,
    val monthlyChange: Money?,
    val yearlyChange: Money?,
    val trend: TrendDirection,
    val projectedNetWorth: List<NetWorthProjection>
)

/**
 * Asset category breakdown
 */
@Serializable
data class AssetCategory(
    val type: AssetType,
    val amount: Money,
    val accounts: List<String>, // Account IDs
    val percentageOfTotal: Double,
    val monthlyChange: Money?
)

/**
 * Liability category breakdown
 */
@Serializable
data class LiabilityCategory(
    val type: LiabilityType,
    val amount: Money,
    val accounts: List<String>, // Account IDs
    val percentageOfTotal: Double,
    val monthlyChange: Money?,
    val interestRate: Double?
)

/**
 * Net worth projection
 */
@Serializable
data class NetWorthProjection(
    val date: LocalDate,
    val projectedNetWorth: Money,
    val confidence: Float
)

/**
 * Budget vs actual comparison
 */
@Serializable
data class BudgetAnalysis(
    val userId: String,
    val period: DateRange,
    val totalBudget: Money,
    val totalSpent: Money,
    val remainingBudget: Money,
    val categoryComparisons: List<CategoryBudgetComparison>,
    val overallPerformance: BudgetPerformance,
    val alerts: List<BudgetAlert>,
    val recommendations: List<BudgetRecommendation>
)

/**
 * Budget comparison for a specific category
 */
@Serializable
data class CategoryBudgetComparison(
    val category: Category,
    val budgetAmount: Money,
    val actualAmount: Money,
    val variance: Money,
    val variancePercentage: Double,
    val performance: BudgetPerformance,
    val daysRemaining: Int,
    val projectedSpend: Money
)

/**
 * Budget alert for overspending or underspending
 */
@Serializable
data class BudgetAlert(
    val id: String,
    val type: BudgetAlertType,
    val category: Category?,
    val severity: AlertSeverity,
    val message: String,
    val amount: Money,
    val threshold: Double,
    val actionRequired: Boolean
)

/**
 * Budget recommendation
 */
@Serializable
data class BudgetRecommendation(
    val id: String,
    val type: RecommendationType,
    val category: Category?,
    val title: String,
    val description: String,
    val suggestedAmount: Money?,
    val potentialSavings: Money?,
    val priority: Priority,
    val confidence: Float
)

/**
 * Canadian-specific financial calculations
 */
@Serializable
data class CanadianTaxAnalysis(
    val userId: String,
    val taxYear: Int,
    val province: CanadianProvince,
    val grossIncome: Money,
    val estimatedTaxes: TaxBreakdown,
    val rrspContributions: RRSPAnalysis,
    val tfsaContributions: TFSAAnalysis,
    val taxOptimizationRecommendations: List<TaxRecommendation>
)

/**
 * Tax breakdown for Canadian users
 */
@Serializable
data class TaxBreakdown(
    val federalTax: Money,
    val provincialTax: Money,
    val cpp: Money, // Canada Pension Plan
    val ei: Money,  // Employment Insurance
    val totalTax: Money,
    val afterTaxIncome: Money,
    val marginalTaxRate: Double,
    val averageTaxRate: Double
)

/**
 * RRSP analysis and recommendations
 */
@Serializable
data class RRSPAnalysis(
    val currentContributions: Money,
    val contributionRoom: Money,
    val maxContribution: Money,
    val taxSavings: Money,
    val recommendedContribution: Money,
    val carryForwardRoom: Money
)

/**
 * TFSA analysis and recommendations
 */
@Serializable
data class TFSAAnalysis(
    val currentContributions: Money,
    val contributionRoom: Money,
    val maxContribution: Money,
    val recommendedContribution: Money,
    val withdrawalRoom: Money
)

/**
 * Tax optimization recommendation
 */
@Serializable
data class TaxRecommendation(
    val id: String,
    val type: TaxRecommendationType,
    val title: String,
    val description: String,
    val potentialSavings: Money,
    val priority: Priority,
    val deadline: LocalDate?
)

/**
 * Personalized financial recommendation
 */
@Serializable
data class PersonalizedRecommendation(
    val id: String,
    val userId: String,
    val type: RecommendationType,
    val title: String,
    val description: String,
    val reasoning: String,
    val priority: Priority,
    val category: Category?,
    val potentialImpact: Money?,
    val confidence: Float,
    val actionSteps: List<String>,
    val deadline: LocalDate?,
    val createdAt: Instant,
    val isCompleted: Boolean = false
)

/**
 * Date range for analysis
 */
@Serializable
data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    val durationInDays: Long
        get() = (endDate.toEpochDays() - startDate.toEpochDays()).toLong()
    
    val durationInWeeks: Double
        get() = durationInDays / 7.0
    
    val durationInMonths: Double
        get() = durationInDays / 30.0
}

/**
 * Period comparison between current and previous periods
 */
@Serializable
data class PeriodComparison(
    val currentPeriod: DateRange,
    val previousPeriod: DateRange,
    val totalSpentChange: Money,
    val totalSpentChangePercentage: Double,
    val categoryChanges: Map<String, Money>,
    val significantChanges: List<String>
)

// Enums for various types and classifications

@Serializable
enum class TrendDirection {
    INCREASING,
    DECREASING,
    STABLE,
    VOLATILE
}

@Serializable
enum class TrendType {
    SPENDING,
    INCOME,
    CATEGORY_SPENDING,
    FREQUENCY,
    AMOUNT_PER_TRANSACTION
}

@Serializable
enum class InsightType {
    SPENDING_PATTERN,
    SAVINGS_OPPORTUNITY,
    BUDGET_ALERT,
    GOAL_PROGRESS,
    TAX_OPTIMIZATION,
    UNUSUAL_ACTIVITY,
    POSITIVE_BEHAVIOR
}

@Serializable
enum class InsightImpact {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Serializable
enum class AssetType {
    CHECKING,
    SAVINGS,
    INVESTMENT,
    REAL_ESTATE,
    OTHER
}

@Serializable
enum class LiabilityType {
    CREDIT_CARD,
    MORTGAGE,
    LOAN,
    OTHER_DEBT
}

@Serializable
enum class BudgetPerformance {
    UNDER_BUDGET,
    ON_TRACK,
    OVER_BUDGET,
    SIGNIFICANTLY_OVER
}

@Serializable
enum class BudgetAlertType {
    OVERSPENDING,
    APPROACHING_LIMIT,
    UNDERSPENDING,
    BUDGET_EXCEEDED
}

@Serializable
enum class RecommendationType {
    BUDGET_ADJUSTMENT,
    SAVINGS_OPPORTUNITY,
    DEBT_REDUCTION,
    INVESTMENT_SUGGESTION,
    TAX_OPTIMIZATION,
    GOAL_ADJUSTMENT,
    SPENDING_REDUCTION
}

@Serializable
enum class CanadianProvince {
    AB, BC, MB, NB, NL, NS, NT, NU, ON, PE, QC, SK, YT
}

@Serializable
enum class TaxRecommendationType {
    RRSP_CONTRIBUTION,
    TFSA_CONTRIBUTION,
    TAX_LOSS_HARVESTING,
    INCOME_SPLITTING,
    DEDUCTION_OPTIMIZATION,
    CREDIT_OPTIMIZATION
}