package com.north.mobile.data.ai

import com.north.mobile.domain.model.*
import com.north.mobile.data.analytics.*
import kotlinx.datetime.LocalDate

/**
 * North AI conversational interface for intelligent financial assistance
 * Provides natural language processing and contextual financial insights
 */
interface NorthAIService {
    
    /**
     * Process a user's natural language query and provide contextual response
     * @param query The user's question or request
     * @param context Current user financial context
     * @return AI response with insights and recommendations
     */
    suspend fun processUserQuery(
        query: String, 
        context: UserFinancialContext
    ): Result<AIResponse>
    
    /**
     * Generate personalized financial insights based on user data
     * @param context User's financial context
     * @return List of personalized insights
     */
    suspend fun generatePersonalizedInsights(
        context: UserFinancialContext
    ): Result<List<AIInsight>>
    
    /**
     * Analyze spending patterns for a specific category and timeframe
     * @param category Spending category to analyze
     * @param timeframe Date range for analysis
     * @param context User's financial context
     * @return Detailed spending pattern analysis
     */
    suspend fun analyzeSpendingPattern(
        category: String,
        timeframe: DateRange,
        context: UserFinancialContext
    ): Result<SpendingAnalysis>
    
    /**
     * Check if user can afford a specific expense
     * @param expense Details of the requested expense
     * @param context User's financial context
     * @return Affordability analysis with recommendations
     */
    suspend fun checkAffordability(
        expense: ExpenseRequest,
        context: UserFinancialContext
    ): Result<AffordabilityResult>
    
    /**
     * Explain a specific transaction with context and insights
     * @param transactionId ID of the transaction to explain
     * @param context User's financial context
     * @return Detailed transaction explanation
     */
    suspend fun explainTransaction(
        transactionId: String,
        context: UserFinancialContext
    ): Result<TransactionExplanation>
    
    /**
     * Suggest financial optimizations based on user data
     * @param context User's financial context
     * @return List of optimization suggestions
     */
    suspend fun suggestOptimizations(
        context: UserFinancialContext
    ): Result<List<OptimizationSuggestion>>
    
    /**
     * Generate follow-up questions to help users explore their finances
     * @param context User's financial context
     * @param previousQuery Optional previous query for context
     * @return List of suggested follow-up questions
     */
    suspend fun generateFollowUpQuestions(
        context: UserFinancialContext,
        previousQuery: String? = null
    ): Result<List<String>>
    
    /**
     * Analyze unusual spending and provide explanations
     * @param context User's financial context
     * @param timeframe Period to analyze for unusual activity
     * @return List of unusual spending alerts with explanations
     */
    suspend fun analyzeUnusualSpending(
        context: UserFinancialContext,
        timeframe: DateRange
    ): Result<List<UnusualSpendingAlert>>
}

/**
 * User's complete financial context for AI processing
 */
data class UserFinancialContext(
    val userId: String,
    val accounts: List<Account>,
    val recentTransactions: List<Transaction>,
    val goals: List<FinancialGoal>,
    val budgets: List<Budget>,
    val userPreferences: UserPreferences,
    val spendingAnalysis: SpendingAnalysis?,
    val netWorth: NetWorthSummary?,
    val gamificationProfile: GamificationProfile?
)

/**
 * AI response to user queries
 */
data class AIResponse(
    val message: String,
    val confidence: Float,
    val supportingData: List<DataPoint>,
    val actionableRecommendations: List<Recommendation>,
    val followUpQuestions: List<String>,
    val visualizations: List<DataVisualization> = emptyList(),
    val relatedInsights: List<AIInsight> = emptyList()
)

/**
 * AI-generated financial insight
 */
data class AIInsight(
    val id: String,
    val type: AIInsightType,
    val title: String,
    val description: String,
    val confidence: Float,
    val impact: InsightImpact,
    val category: Category?,
    val actionableSteps: List<String>,
    val potentialSavings: Money?,
    val timeframe: String,
    val supportingData: List<DataPoint>
)

/**
 * Request for expense affordability check
 */
data class ExpenseRequest(
    val description: String,
    val amount: Money,
    val category: Category,
    val isRecurring: Boolean = false,
    val frequency: RecurringFrequency? = null,
    val plannedDate: LocalDate
)

/**
 * Result of affordability analysis
 */
data class AffordabilityResult(
    val canAfford: Boolean,
    val confidence: Float,
    val impactOnGoals: GoalImpactAnalysis,
    val impactOnBudget: BudgetImpactAnalysis,
    val alternativeOptions: List<Alternative>,
    val reasoning: String,
    val recommendations: List<String>,
    val riskFactors: List<String>
)

/**
 * Detailed explanation of a transaction
 */
data class TransactionExplanation(
    val transactionId: String,
    val summary: String,
    val categoryExplanation: String,
    val spendingPatternContext: String,
    val budgetImpact: String,
    val goalImpact: String,
    val unusualFactors: List<String>,
    val relatedTransactions: List<Transaction>,
    val recommendations: List<String>
)

/**
 * Financial optimization suggestion
 */
data class OptimizationSuggestion(
    val id: String,
    val type: OptimizationType,
    val title: String,
    val description: String,
    val potentialSavings: Money,
    val effort: EffortLevel,
    val timeToImplement: String,
    val steps: List<String>,
    val riskLevel: RiskLevel,
    val confidence: Float
)

/**
 * Alert for unusual spending activity
 */
data class UnusualSpendingAlert(
    val id: String,
    val type: UnusualSpendingType,
    val description: String,
    val amount: Money,
    val category: Category,
    val transactions: List<Transaction>,
    val explanation: String,
    val severity: AlertSeverity,
    val recommendations: List<String>
)

/**
 * Impact analysis on financial goals
 */
data class GoalImpactAnalysis(
    val affectedGoals: List<GoalImpact>,
    val overallImpact: GoalImpactSeverity,
    val delayInDays: Int,
    val alternativeStrategies: List<String>
)

/**
 * Impact analysis on budget
 */
data class BudgetImpactAnalysis(
    val categoryImpact: CategoryBudgetImpact,
    val overallBudgetImpact: Money,
    val remainingBudget: Money,
    val projectedOverspend: Money?,
    val recommendations: List<String>
)

/**
 * Budget impact for specific category
 */
data class CategoryBudgetImpact(
    val category: Category,
    val budgetRemaining: Money,
    val wouldExceedBudget: Boolean,
    val exceedAmount: Money?
)

/**
 * Alternative option for expense
 */
data class Alternative(
    val description: String,
    val amount: Money,
    val pros: List<String>,
    val cons: List<String>,
    val feasibility: Float
)

/**
 * Data visualization for AI responses
 */
data class DataVisualization(
    val type: VisualizationType,
    val title: String,
    val data: Map<String, Any>,
    val description: String
)

/**
 * User preferences for AI interactions
 */
data class UserPreferences(
    val communicationStyle: CommunicationStyle,
    val riskTolerance: RiskTolerance,
    val preferredCurrency: Currency,
    val notificationPreferences: NotificationPreferences,
    val privacySettings: PrivacySettings
)

/**
 * Budget information for AI context
 */
data class Budget(
    val id: String,
    val userId: String,
    val category: Category,
    val amount: Money,
    val period: BudgetPeriod,
    val spent: Money,
    val remaining: Money
)

/**
 * Recommendation from AI
 */
data class Recommendation(
    val id: String,
    val type: RecommendationType,
    val title: String,
    val description: String,
    val priority: Priority,
    val actionText: String,
    val estimatedImpact: Money?
)

// Enums for various types and classifications

enum class AIInsightType {
    SPENDING_PATTERN,
    SAVINGS_OPPORTUNITY,
    GOAL_PROGRESS,
    BUDGET_ALERT,
    TAX_OPTIMIZATION,
    UNUSUAL_ACTIVITY,
    POSITIVE_BEHAVIOR,
    RECOMMENDATION
}

enum class RecurringFrequency {
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    QUARTERLY,
    ANNUALLY
}

enum class OptimizationType {
    SUBSCRIPTION_OPTIMIZATION,
    SPENDING_REDUCTION,
    SAVINGS_INCREASE,
    DEBT_PAYOFF,
    TAX_OPTIMIZATION,
    GOAL_ACCELERATION,
    BUDGET_REALLOCATION
}

enum class EffortLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class UnusualSpendingType {
    LARGE_PURCHASE,
    FREQUENCY_SPIKE,
    NEW_MERCHANT,
    CATEGORY_ANOMALY,
    TIME_ANOMALY,
    LOCATION_ANOMALY
}

enum class GoalImpactSeverity {
    NONE,
    MINIMAL,
    MODERATE,
    SIGNIFICANT,
    SEVERE
}

enum class VisualizationType {
    BAR_CHART,
    PIE_CHART,
    LINE_CHART,
    PROGRESS_BAR,
    COMPARISON_TABLE
}

enum class CommunicationStyle {
    CASUAL,
    PROFESSIONAL,
    ENCOURAGING,
    DIRECT
}

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    ANNUALLY
}

enum class NotificationPreferences {
    ALL,
    IMPORTANT_ONLY,
    MINIMAL,
    NONE
}

enum class PrivacySettings {
    FULL_SHARING,
    LIMITED_SHARING,
    MINIMAL_SHARING
}