package com.north.mobile.data.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

/**
 * Memory management API service for North backend
 */
class MemoryApiService(private val apiClient: ApiClient) {
    
    /**
     * Get user's memory profile
     */
    suspend fun getMemoryProfile(token: String): Result<UserMemoryResponse> {
        return try {
            val response = apiClient.httpClient.get("/api/memory/profile") {
                authorize(token)
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val memory = response.body<UserMemoryResponse>()
                    Result.success(memory)
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(Exception("Authentication required"))
                }
                else -> {
                    Result.failure(Exception("Failed to fetch memory profile: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user's memory profile
     */
    suspend fun updateMemoryProfile(token: String, memoryData: String): Result<MemoryUpdateResponse> {
        return try {
            val response = apiClient.httpClient.post("/api/memory/profile") {
                authorize(token)
                setBody(memoryData)
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val updateResponse = response.body<MemoryUpdateResponse>()
                    Result.success(updateResponse)
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(Exception("Authentication required"))
                }
                else -> {
                    Result.failure(Exception("Failed to update memory profile: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add conversation message to memory
     */
    suspend fun addMessage(
        token: String,
        sessionId: String,
        message: String,
        isFromUser: Boolean,
        topics: List<String> = emptyList(),
        entities: List<String> = emptyList()
    ): Result<MemoryUpdateResponse> {
        return try {
            val request = AddMessageRequest(
                sessionId = sessionId,
                message = message,
                isFromUser = isFromUser,
                topics = topics,
                entities = entities
            )
            
            val response = apiClient.httpClient.post("/api/memory/message") {
                authorize(token)
                setBody(request)
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val updateResponse = response.body<MemoryUpdateResponse>()
                    Result.success(updateResponse)
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(Exception("Authentication required"))
                }
                else -> {
                    Result.failure(Exception("Failed to add message to memory: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get conversation history
     */
    suspend fun getConversationHistory(token: String, limit: Int = 10): Result<List<ConversationSessionResponse>> {
        return try {
            val response = apiClient.httpClient.get("/api/memory/conversations") {
                authorize(token)
                parameter("limit", limit)
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val conversations = response.body<List<ConversationSessionResponse>>()
                    Result.success(conversations)
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(Exception("Authentication required"))
                }
                else -> {
                    Result.failure(Exception("Failed to fetch conversation history: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send message to AI CFO with memory context
     */
    suspend fun sendChatMessageWithMemory(
        token: String,
        message: String,
        sessionId: String
    ): Result<ChatResponse> {
        return try {
            val request = ChatWithMemoryRequest(
                message = message,
                sessionId = sessionId
            )
            
            val response = apiClient.httpClient.post("/api/ai/chat-with-memory") {
                authorize(token)
                setBody(request)
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
                    Result.failure(Exception("AI chat with memory failed: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add insight to user's knowledge graph
     */
    suspend fun addInsight(
        token: String,
        insight: String,
        category: String,
        confidence: Double = 0.8,
        evidence: List<String> = emptyList(),
        actionable: Boolean = true
    ): Result<MemoryUpdateResponse> {
        return try {
            val request = AddInsightRequest(
                insight = insight,
                category = category,
                confidence = confidence,
                evidence = evidence,
                actionable = actionable
            )
            
            val response = apiClient.httpClient.post("/api/memory/insight") {
                authorize(token)
                setBody(request)
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val updateResponse = response.body<MemoryUpdateResponse>()
                    Result.success(updateResponse)
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(Exception("Authentication required"))
                }
                else -> {
                    Result.failure(Exception("Failed to add insight: ${response.status}"))
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

// Request/Response data classes
@Serializable
data class UserMemoryResponse(
    val userId: String,
    val personalInfo: Map<String, String> = emptyMap(),
    val financialProfile: Map<String, String> = emptyMap(),
    val conversationHistory: List<String> = emptyList(),
    val knowledgeGraph: Map<String, String> = emptyMap(),
    val preferences: Map<String, String> = emptyMap(),
    val lastUpdated: String
)

@Serializable
data class MemoryUpdateResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class AddMessageRequest(
    val sessionId: String,
    val message: String,
    val isFromUser: Boolean,
    val topics: List<String> = emptyList(),
    val entities: List<String> = emptyList()
)

@Serializable
data class ConversationSessionResponse(
    val sessionId: String,
    val startTime: String,
    val endTime: String? = null,
    val topics: List<String> = emptyList(),
    val insights: List<String> = emptyList(),
    val messages: List<MessageResponse> = emptyList()
)

@Serializable
data class MessageResponse(
    val message: String,
    val isFromUser: Boolean,
    val topics: List<String> = emptyList(),
    val entities: List<String> = emptyList(),
    val timestamp: String
)

@Serializable
data class ChatWithMemoryRequest(
    val message: String,
    val sessionId: String
)

@Serializable
data class AddInsightRequest(
    val insight: String,
    val category: String,
    val confidence: Double = 0.8,
    val evidence: List<String> = emptyList(),
    val actionable: Boolean = true
)