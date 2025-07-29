package com.north.mobile.data.repository

import com.north.mobile.data.api.InsightsApiService
import com.north.mobile.data.api.TransactionAnalysisService
import com.north.mobile.data.api.SpendingInsight
import com.north.mobile.data.api.DynamicGoal
import com.north.mobile.data.api.SpendingPattern
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InsightsRepository(
    private val insightsApiService: InsightsApiService,
    private val transactionAnalysisService: TransactionAnalysisService
) {
    private val _insights = MutableStateFlow<List<SpendingInsight>>(emptyList())
    val insights: StateFlow<List<SpendingInsight>> = _insights.asStateFlow()
    
    private val _goals = MutableStateFlow<List<DynamicGoal>>(emptyList())
    val goals: StateFlow<List<DynamicGoal>> = _goals.asStateFlow()
    
    private val _spendingPatterns = MutableStateFlow<List<SpendingPattern>>(emptyList())
    val spendingPatterns: StateFlow<List<SpendingPattern>> = _spendingPatterns.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    suspend fun analyzeTransactions(): Result<Unit> {
        return try {
            _isLoading.value = true
            _error.value = null
            
            val result = transactionAnalysisService.analyzeTransactions()
            
            if (result.isSuccess) {
                // Refresh all data after analysis
                refreshAllData()
                Result.success(Unit)
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Transaction analysis failed"
                Result.failure(result.exceptionOrNull() ?: Exception("Transaction analysis failed"))
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    suspend fun refreshAllData() {
        try {
            _isLoading.value = true
            _error.value = null
            
            // Load insights
            val insightsResult = insightsApiService.getInsights()
            if (insightsResult.isSuccess) {
                _insights.value = insightsResult.getOrThrow()
            }
            
            // Load goals
            val goalsResult = insightsApiService.getGoals()
            if (goalsResult.isSuccess) {
                _goals.value = goalsResult.getOrThrow()
            }
            
            // Load spending patterns
            val patternsResult = insightsApiService.getSpendingPatterns()
            if (patternsResult.isSuccess) {
                _spendingPatterns.value = patternsResult.getOrThrow()
            }
            
        } catch (e: Exception) {
            _error.value = e.message ?: "Failed to refresh data"
        } finally {
            _isLoading.value = false
        }
    }
    
    suspend fun markInsightAsRead(insightId: String): Result<Unit> {
        return try {
            val result = insightsApiService.markInsightAsRead(insightId)
            
            if (result.isSuccess) {
                // Update local state
                _insights.value = _insights.value.map { insight ->
                    if (insight.id == insightId) {
                        insight.copy(is_read = true)
                    } else {
                        insight
                    }
                }
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateGoalProgress(goalId: String, amount: Double): Result<Unit> {
        return try {
            val result = insightsApiService.updateGoalProgress(goalId, amount)
            
            if (result.isSuccess) {
                // Update local state
                _goals.value = _goals.value.map { goal ->
                    if (goal.id == goalId) {
                        val newCurrentAmount = goal.currentAmount + amount
                        val newProgressPercentage = ((newCurrentAmount / goal.targetAmount) * 100).toInt()
                        val newRemainingAmount = goal.targetAmount - newCurrentAmount
                        
                        goal.copy(
                            currentAmount = newCurrentAmount,
                            progressPercentage = newProgressPercentage,
                            remainingAmount = newRemainingAmount
                        )
                    } else {
                        goal
                    }
                }
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getUnreadInsights(): List<SpendingInsight> {
        return _insights.value.filter { !it.is_read }
    }
    
    fun getHighPriorityGoals(): List<DynamicGoal> {
        return _goals.value.filter { it.priority >= 8 }.sortedByDescending { it.priority }
    }
    
    fun getTopSpendingCategories(limit: Int = 5): List<SpendingPattern> {
        return _spendingPatterns.value
            .sortedByDescending { it.total_amount }
            .take(limit)
    }
    
    fun getIncreasingSpendingCategories(): List<SpendingPattern> {
        return _spendingPatterns.value.filter { 
            it.trend_direction == "increasing" && (it.trend_percentage ?: 0.0) > 10.0 
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}

// Extension function to copy SpendingInsight (since it's a data class from API)
private fun SpendingInsight.copy(
    id: String = this.id,
    insight_type: String = this.insight_type,
    title: String = this.title,
    description: String = this.description,
    category: String? = this.category,
    amount: Double? = this.amount,
    confidence_score: Double = this.confidence_score,
    action_items: List<String> = this.action_items,
    is_read: Boolean = this.is_read,
    created_at: String = this.created_at
): SpendingInsight {
    return SpendingInsight(
        id = id,
        insight_type = insight_type,
        title = title,
        description = description,
        category = category,
        amount = amount,
        confidence_score = confidence_score,
        action_items = action_items,
        is_read = is_read,
        created_at = created_at
    )
}

// Extension function to copy DynamicGoal
private fun DynamicGoal.copy(
    id: String = this.id,
    goalType: String = this.goalType,
    title: String = this.title,
    description: String = this.description,
    targetAmount: Double = this.targetAmount,
    currentAmount: Double = this.currentAmount,
    targetDate: String? = this.targetDate,
    category: String? = this.category,
    priority: Int = this.priority,
    status: String = this.status,
    aiGenerated: Boolean = this.aiGenerated,
    progressPercentage: Int = this.progressPercentage,
    remainingAmount: Double = this.remainingAmount,
    createdAt: String = this.createdAt
): DynamicGoal {
    return DynamicGoal(
        id = id,
        goalType = goalType,
        title = title,
        description = description,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        targetDate = targetDate,
        category = category,
        priority = priority,
        status = status,
        aiGenerated = aiGenerated,
        progressPercentage = progressPercentage,
        remainingAmount = remainingAmount,
        createdAt = createdAt
    )
}