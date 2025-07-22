package com.north.mobile.data.analytics

import com.north.mobile.domain.model.*
import kotlinx.datetime.*
import kotlin.math.*

/**
 * Core recommendation engine for automated financial planning
 * Implements Canadian-specific tax optimization and financial planning strategies
 */
interface RecommendationEngine {
    
    /**
     * Generate comprehensive financial planning recommendations
     */
    suspend fun generateFinancialPlanningRecommendations(
        userId: String,
        userProfile: UserFinancialProfile
    ): Result<List<FinancialPlanningRecommendation>>
    
    /**
     * Optimize RRSP contributions based on income and tax situation
     */
    suspend fun optimizeRRSPContributions(
        userProfile: UserFinancialProfile
    ): Result<RRSPOptimizationRecommendation>
    
    /**
     * Optimize TFSA contributions based on available room and goals
     */
    suspend fun optimizeTFSAContributions(
        userProfile: UserFinancialProfile
    ): Result<TFSAOptimizationRecommendation>
    
    /**
     * Generate debt payoff optimization strategy
     */
    suspend fun optimizeDebtPayoff(
        userProfile: UserFinancialProfile
    ): Result<DebtPayoffStrategy>
    
    /**
     * Generate savings optimization recommendations
     */
    suspend fun optimizeSavingsStrategy(
        userProfile: UserFinancialProfile
    ): Result<SavingsOptimizationRecommendation>
    
    /**
     * Track recommendation effectiveness and update strategies
     */
    suspend fun trackRecommendationEffectiveness(
        recommendationId: String,
        outcome: RecommendationOutcome
    ): Result<Unit>
    
    /**
     * Get explanation for a specific recommendation
     */
    suspend fun getRecommendationExplanation(
        recommendationId: String
    ): Result<RecommendationExplanation>
}

/**
 * User's complete financial profile for recommendation generation
 */
data class UserFinancialProfile(
    val userId: String,
    val age: Int,
    val province: CanadianProvince,
    val grossAnnualIncome: Money,
    val netWorth: NetWorthSummary,
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val goals: List<FinancialGoal>,
    val currentRRSPContributions: Money,
    val currentTFSAContributions: Money,
    val rrspRoom: Money,
    val tfsaRoom: Money,
    val marginalTaxRate: Double,
    val riskTolerance: RiskTolerance,
    val timeHorizon: TimeHorizon,
    val spendingAnalysis: SpendingAnalysis,
    val budgetAnalysis: BudgetAnalysis
)

/**
 * Comprehensive financial planning recommendation
 */
data class FinancialPlanningRecommendation(
    val id: String,
    val userId: String,
    val type: FinancialPlanningType,
    val priority: Priority,
    val title: String,
    val description: String,
    val reasoning: RecommendationReasoning,
    val actionSteps: List<ActionStep>,
    val expectedImpact: ExpectedImpact,
    val timeframe: RecommendationTimeframe,
    val prerequisites: List<String>,
    val risks: List<String>,
    val alternatives: List<AlternativeRecommendation>,
    val trackingMetrics: List<TrackingMetric>,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val isCompleted: Boolean = false
)

/**
 * RRSP contribution optimization recommendation
 */
data class RRSPOptimizationRecommendation(
    val recommendedContribution: Money,
    val currentContribution: Money,
    val availableRoom: Money,
    val taxSavings: Money,
    val optimalTiming: ContributionTiming,
    val reasoning: String,
    val impactOnGoals: List<GoalImpact>,
    val alternativeStrategies: List<String>
)

/**
 * TFSA contribution optimization recommendation
 */
data class TFSAOptimizationRecommendation(
    val recommendedContribution: Money,
    val currentContribution: Money,
    val availableRoom: Money,
    val optimalAllocation: List<AllocationRecommendation>,
    val reasoning: String,
    val impactOnGoals: List<GoalImpact>,
    val alternativeStrategies: List<String>
)

/**
 * Debt payoff optimization strategy
 */
data class DebtPayoffStrategy(
    val strategy: DebtPayoffMethod,
    val payoffOrder: List<DebtPayoffPlan>,
    val totalInterestSaved: Money,
    val payoffTimeframe: Int, // months
    val monthlyPaymentPlan: Money,
    val reasoning: String,
    val alternativeStrategies: List<AlternativeDebtStrategy>
)

/**
 * Savings optimization recommendation
 */
data class SavingsOptimizationRecommendation(
    val recommendedSavingsRate: Double,
    val currentSavingsRate: Double,
    val optimalAllocation: List<SavingsAllocation>,
    val emergencyFundTarget: Money,
    val currentEmergencyFund: Money,
    val reasoning: String,
    val impactOnGoals: List<GoalImpact>
)

/**
 * Recommendation outcome tracking
 */
data class RecommendationOutcome(
    val recommendationId: String,
    val action: RecommendationAction,
    val actualImpact: Money?,
    val timeToComplete: Int?, // days
    val userFeedback: UserFeedback?,
    val completedAt: Instant
)

/**
 * Detailed explanation for a recommendation
 */
data class RecommendationExplanation(
    val recommendationId: String,
    val summary: String,
    val detailedReasoning: String,
    val assumptions: List<String>,
    val calculations: List<CalculationStep>,
    val sources: List<String>,
    val riskFactors: List<String>,
    val alternativeApproaches: List<String>
)

/**
 * Reasoning behind a recommendation
 */
data class RecommendationReasoning(
    val primaryFactors: List<String>,
    val dataPoints: List<DataPoint>,
    val assumptions: List<String>,
    val confidence: Float,
    val methodology: String
)

/**
 * Expected impact of implementing a recommendation
 */
data class ExpectedImpact(
    val financialImpact: Money,
    val timeToRealize: Int, // months
    val confidence: Float,
    val impactType: ImpactType,
    val goalProgress: List<GoalImpact>
)

/**
 * Action step for implementing a recommendation
 */
data class ActionStep(
    val order: Int,
    val description: String,
    val estimatedTime: Int, // minutes
    val difficulty: Difficulty,
    val resources: List<String>,
    val isCompleted: Boolean = false
)

/**
 * Alternative recommendation option
 */
data class AlternativeRecommendation(
    val title: String,
    val description: String,
    val expectedImpact: Money,
    val tradeoffs: List<String>
)

/**
 * Metric for tracking recommendation success
 */
data class TrackingMetric(
    val name: String,
    val currentValue: Double,
    val targetValue: Double,
    val unit: String,
    val trackingFrequency: TrackingFrequency
)

/**
 * Impact on a specific financial goal
 */
data class GoalImpact(
    val goalId: String,
    val goalName: String,
    val impactType: GoalImpactType,
    val timeImpact: Int, // days (positive = faster, negative = slower)
    val amountImpact: Money
)

/**
 * Debt payoff plan for a specific debt
 */
data class DebtPayoffPlan(
    val accountId: String,
    val accountName: String,
    val currentBalance: Money,
    val interestRate: Double,
    val minimumPayment: Money,
    val recommendedPayment: Money,
    val payoffOrder: Int,
    val estimatedPayoffMonths: Int
)

/**
 * Alternative debt payoff strategy
 */
data class AlternativeDebtStrategy(
    val method: DebtPayoffMethod,
    val description: String,
    val totalInterestSaved: Money,
    val payoffTimeframe: Int,
    val pros: List<String>,
    val cons: List<String>
)

/**
 * Savings allocation recommendation
 */
data class SavingsAllocation(
    val accountType: SavingsAccountType,
    val percentage: Double,
    val amount: Money,
    val reasoning: String,
    val expectedReturn: Double
)

/**
 * Allocation recommendation for investments
 */
data class AllocationRecommendation(
    val assetClass: AssetClass,
    val percentage: Double,
    val reasoning: String,
    val riskLevel: RiskLevel
)

/**
 * Contribution timing recommendation
 */
data class ContributionTiming(
    val frequency: ContributionFrequency,
    val optimalMonths: List<Int>,
    val reasoning: String
)

/**
 * Calculation step for transparency
 */
data class CalculationStep(
    val step: Int,
    val description: String,
    val formula: String,
    val inputs: Map<String, String>,
    val result: String
)

/**
 * Data point supporting a recommendation
 */
data class DataPoint(
    val name: String,
    val value: String,
    val source: String,
    val relevance: String
)

/**
 * User feedback on recommendation
 */
data class UserFeedback(
    val rating: Int, // 1-5
    val comment: String?,
    val helpfulness: Int, // 1-5
    val clarity: Int, // 1-5
    val suggestions: String?
)

// Enums for various types and classifications

enum class FinancialPlanningType {
    TAX_OPTIMIZATION,
    DEBT_REDUCTION,
    SAVINGS_OPTIMIZATION,
    INVESTMENT_ALLOCATION,
    RETIREMENT_PLANNING,
    EMERGENCY_FUND,
    GOAL_ACCELERATION,
    CASH_FLOW_OPTIMIZATION
}

enum class RiskTolerance {
    CONSERVATIVE,
    MODERATE,
    AGGRESSIVE
}

enum class TimeHorizon {
    SHORT_TERM, // < 2 years
    MEDIUM_TERM, // 2-10 years
    LONG_TERM // > 10 years
}

enum class RecommendationTimeframe {
    IMMEDIATE, // < 1 week
    SHORT_TERM, // 1 week - 1 month
    MEDIUM_TERM, // 1-6 months
    LONG_TERM // > 6 months
}

enum class RecommendationAction {
    IMPLEMENTED,
    PARTIALLY_IMPLEMENTED,
    REJECTED,
    DEFERRED,
    MODIFIED
}

enum class ImpactType {
    SAVINGS,
    INCOME_INCREASE,
    DEBT_REDUCTION,
    TAX_SAVINGS,
    GOAL_ACCELERATION
}

enum class Difficulty {
    EASY,
    MODERATE,
    DIFFICULT,
    EXPERT_REQUIRED
}

enum class TrackingFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    ANNUALLY
}

enum class GoalImpactType {
    ACCELERATES,
    DELAYS,
    NEUTRAL,
    CONFLICTS
}

enum class DebtPayoffMethod {
    AVALANCHE, // Highest interest first
    SNOWBALL, // Smallest balance first
    HYBRID, // Combination approach
    MINIMUM_ONLY
}

enum class SavingsAccountType {
    HIGH_YIELD_SAVINGS,
    RRSP,
    TFSA,
    INVESTMENT_ACCOUNT,
    EMERGENCY_FUND,
    GIC
}

enum class AssetClass {
    CANADIAN_EQUITY,
    US_EQUITY,
    INTERNATIONAL_EQUITY,
    BONDS,
    REAL_ESTATE,
    COMMODITIES,
    CASH
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class ContributionFrequency {
    MONTHLY,
    QUARTERLY,
    ANNUALLY,
    LUMP_SUM
}