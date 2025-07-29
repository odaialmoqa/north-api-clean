package com.north.mobile.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class SpendingInsight(
    val id: String,
    val insight_type: String,
    val title: String,
    val description: String,
    val category: String?,
    val amount: Double?,
    val confidence_score: Double,
    val action_items: List<String>,
    val is_read: Boolean,
    val created_at: String
)

@Serializable
data class DynamicGoal(
    val id: String,
    val goalType: String,
    val title: String,
    val description: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: String?,
    val category: String?,
    val priority: Int,
    val status: String,
    val aiGenerated: Boolean,
    val progressPercentage: Int,
    val remainingAmount: Double,
    val createdAt: String
)

@Serializable
data class SpendingPattern(
    val category: String,
    val period_start: String,
    val period_end: String,
    val total_amount: Double,
    val transaction_count: Int,
    val average_transaction: Double,
    val trend_direction: String?,
    val trend_percentage: Double?
)

@Serializable
data class InsightsResponse(
    val success: Boolean,
    val insights: List<SpendingInsight>
)

@Serializable
data class PatternsResponse(
    val success: Boolean,
    val patterns: List<SpendingPattern>
)

@Serializable
data class GoalProgressRequest(
    val amount: Double
)

class InsightsApiService(private val apiClient: ApiClient) {
    
    suspend fun getInsights(): Result<List<SpendingInsight>> {
        return try {
            val response = apiClient.httpClient.get("/api/insights") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${apiClient.getAuthToken()}")
                }
            }
            
            if (response.status.isSuccess()) {
                val insightsResponse = response.body<InsightsResponse>()
                Result.success(insightsResponse.insights)
            } else {
                Result.failure(Exception("Failed to get insights: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getGoals(): Result<List<DynamicGoal>> {
        return try {
            val response = apiClient.httpClient.get("/api/goals") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${apiClient.getAuthToken()}")
                }
            }
            
            if (response.status.isSuccess()) {
                // Handle both array response and goals response format
                val responseText = response.body<String>()
                val goals = if (responseText.trim().startsWith("[")) {
                    // Direct array response
                    response.body<List<DynamicGoal>>()
                } else {
                    // Wrapped response - for future compatibility
                    response.body<List<DynamicGoal>>()
                }
                Result.success(goals)
            } else {
                Result.failure(Exception("Failed to get goals: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSpendingPatterns(): Result<List<SpendingPattern>> {
        return try {
            val response = apiClient.httpClient.get("/api/spending-patterns") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${apiClient.getAuthToken()}")
                }
            }
            
            if (response.status.isSuccess()) {
                val patternsResponse = response.body<PatternsResponse>()
                Result.success(patternsResponse.patterns)
            } else {
                Result.failure(Exception("Failed to get spending patterns: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun markInsightAsRead(insightId: String): Result<Unit> {
        return try {
            val response = apiClient.httpClient.post("/api/insights/$insightId/read") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${apiClient.getAuthToken()}")
                }
            }
            
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark insight as read: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateGoalProgress(goalId: String, amount: Double): Result<Unit> {
        return try {
            val response = apiClient.httpClient.post("/api/goals/$goalId/progress") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${apiClient.getAuthToken()}")
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                setBody(GoalProgressRequest(amount))
            }
            
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update goal progress: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}