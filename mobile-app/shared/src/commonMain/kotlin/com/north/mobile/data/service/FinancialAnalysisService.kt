package com.north.mobile.data.service

import com.north.mobile.data.repository.InsightsRepository
import com.north.mobile.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Service to coordinate financial analysis and insights generation
 */
class FinancialAnalysisService(
    private val insightsRepository: InsightsRepository,
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    
    /**
     * Trigger comprehensive financial analysis
     * This should be called after Plaid account connection
     */
    fun triggerAnalysis() {
        scope.launch {
            try {
                // Ensure user is authenticated
                if (authRepository.getCurrentToken() == null) {
                    println("⚠️ No auth token available for analysis")
                    return@launch
                }
                
                println("🔍 Starting financial analysis...")
                
                // Analyze transactions and generate insights
                val analysisResult = insightsRepository.analyzeTransactions()
                
                if (analysisResult.isSuccess) {
                    println("✅ Financial analysis completed successfully")
                    
                    // Wait a moment for backend processing
                    delay(2000)
                    
                    // Refresh all data to get the latest insights and goals
                    insightsRepository.refreshAllData()
                    
                    println("✅ Data refreshed with new insights and goals")
                } else {
                    println("❌ Financial analysis failed: ${analysisResult.exceptionOrNull()?.message}")
                }
                
            } catch (e: Exception) {
                println("❌ Error during financial analysis: ${e.message}")
            }
        }
    }
    
    /**
     * Refresh insights and goals data
     */
    fun refreshData() {
        scope.launch {
            try {
                insightsRepository.refreshAllData()
                println("✅ Financial data refreshed")
            } catch (e: Exception) {
                println("❌ Error refreshing data: ${e.message}")
            }
        }
    }
    
    /**
     * Get summary of current financial state
     */
    suspend fun getFinancialSummary(): FinancialSummary {
        return try {
            val insights = insightsRepository.insights.value
            val goals = insightsRepository.goals.value
            val patterns = insightsRepository.spendingPatterns.value
            
            FinancialSummary(
                totalInsights = insights.size,
                unreadInsights = insights.count { !it.is_read },
                activeGoals = goals.size,
                highPriorityGoals = goals.count { it.priority >= 8 },
                topSpendingCategory = patterns.maxByOrNull { it.total_amount }?.category ?: "Unknown",
                totalMonthlySpending = patterns.sumOf { it.total_amount },
                hasIncreasingSpending = patterns.any { 
                    it.trend_direction == "increasing" && (it.trend_percentage ?: 0.0) > 10.0 
                }
            )
        } catch (e: Exception) {
            println("❌ Error getting financial summary: ${e.message}")
            FinancialSummary()
        }
    }
}

data class FinancialSummary(
    val totalInsights: Int = 0,
    val unreadInsights: Int = 0,
    val activeGoals: Int = 0,
    val highPriorityGoals: Int = 0,
    val topSpendingCategory: String = "Unknown",
    val totalMonthlySpending: Double = 0.0,
    val hasIncreasingSpending: Boolean = false
)