package com.north.mobile.data.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Financial data API service for North backend
 */
class FinancialApiService(private val apiClient: ApiClient) {
    
    /**
     * Get user's financial summary
     */
    suspend fun getFinancialSummary(token: String): Result<FinancialSummaryResponse> {
        return try {
            val response = apiClient.httpClient.get("/api/financial/summary") {
                authorize(token)
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val summary = response.body<FinancialSummaryResponse>()
                    Result.success(summary)
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(Exception("Authentication required"))
                }
                else -> {
                    Result.failure(Exception("Failed to fetch financial summary: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user's goals
     */
    suspend fun getGoals(token: String): Result<List<FinancialGoalData>> {
        return try {
            val response = apiClient.httpClient.get("/api/goals") {
                authorize(token)
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val goals = response.body<List<FinancialGoalData>>()
                    Result.success(goals)
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(Exception("Authentication required"))
                }
                else -> {
                    Result.failure(Exception("Failed to fetch goals: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user's transactions
     */
    suspend fun getTransactions(
        token: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<TransactionsResponse> {
        return try {
            val response = apiClient.httpClient.get("/api/transactions") {
                authorize(token)
                parameter("limit", limit)
                parameter("offset", offset)
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val transactions = response.body<TransactionsResponse>()
                    Result.success(transactions)
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(Exception("Authentication required"))
                }
                else -> {
                    Result.failure(Exception("Failed to fetch transactions: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send message to AI CFO Brain (powered by Gemini LLM)
     * Analyzes user's real transaction data to provide intelligent financial insights
     */
    suspend fun sendChatMessage(
        token: String,
        message: String
    ): Result<ChatResponse> {
        return try {
            val response = apiClient.httpClient.post("/api/ai/chat") {
                authorize(token)
                setBody(ChatRequest(message))
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val chatResponse = response.body<ChatResponse>()
                    Result.success(chatResponse)
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(Exception("Authentication required"))
                }
                else -> {
                    Result.failure(Exception("AI chat failed: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun HttpRequestBuilder.authorize(token: String) {
        apiClient.run { authorize(token) }
    }
}