package com.north.mobile.data.analytics

import com.north.mobile.domain.model.*
import kotlinx.datetime.LocalDate

/**
 * Core financial analytics service for generating insights and recommendations
 */
interface FinancialAnalyticsService {
    
    // Spending Analysis
    /**
     * Generate comprehensive spending analysis for a user
     */
    suspend fun generateSpendingAnalysis(
        userId: String,
        period: DateRange,
        includeComparison: Boolean = true
    ): Result<SpendingAnalysis>
    
    /**
     * Analyze spending trends over multiple periods
     */
    suspend fun analyzeSpendingTrends(
        userId: String,
        periods: List<DateRange>
    ): Result<List<SpendingTrend>>
    
    /**
     * Generate spending insights and recommendations
     */
    suspend fun generateSpendingInsights(userId: String): Result<List<SpendingInsight>>
    
    // Net Worth Calculation
    /**
     * Calculate current net worth for a user
     */
    suspend fun calculateNetWorth(userId: String): Result<NetWorthSummary>
    
    /**
     * Track net worth changes over time
     */
    suspend fun trackNetWorthHistory(
        userId: String,
        period: DateRange
    ): Result<List<NetWorthSummary>>
    
    /**
     * Project future net worth based on current trends
     */
    suspend fun projectNetWorth(
        userId: String,
        projectionMonths: Int = 12
    ): Result<List<NetWorthProjection>>
    
    // Budget Analysis
    /**
     * Compare actual spending against budgets
     */
    suspend fun analyzeBudgetPerformance(
        userId: String,
        period: DateRange
    ): Result<BudgetAnalysis>
    
    /**
     * Generate budget alerts for overspending
     */
    suspend fun generateBudgetAlerts(userId: String): Result<List<BudgetAlert>>
    
    /**
     * Recommend budget adjustments
     */
    suspend fun recommendBudgetAdjustments(userId: String): Result<List<BudgetRecommendation>>
    
    // Canadian Tax Analysis
    /**
     * Analyze Canadian tax implications and optimization opportunities
     */
    suspend fun analyzeCanadianTaxes(
        userId: String,
        taxYear: Int = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault()).year
    ): Result<CanadianTaxAnalysis>
    
    /**
     * Calculate RRSP contribution recommendations
     */
    suspend fun calculateRRSPRecommendations(userId: String): Result<RRSPAnalysis>
    
    /**
     * Calculate TFSA contribution recommendations
     */
    suspend fun calculateTFSARecommendations(userId: String): Result<TFSAAnalysis>
    
    // Personalized Recommendations
    /**
     * Generate personalized financial recommendations
     */
    suspend fun generatePersonalizedRecommendations(userId: String): Result<List<PersonalizedRecommendation>>
    
    /**
     * Update recommendation status when user takes action
     */
    suspend fun markRecommendationCompleted(recommendationId: String): Result<Unit>
    
    /**
     * Get recommendation effectiveness metrics
     */
    suspend fun getRecommendationMetrics(userId: String): Result<RecommendationMetrics>
}

/**
 * Metrics for tracking recommendation effectiveness
 */
data class RecommendationMetrics(
    val totalRecommendations: Int,
    val completedRecommendations: Int,
    val completionRate: Double,
    val averageTimeToComplete: Double, // in days
    val totalPotentialSavings: Money,
    val actualizedSavings: Money,
    val topPerformingTypes: List<RecommendationType>
)