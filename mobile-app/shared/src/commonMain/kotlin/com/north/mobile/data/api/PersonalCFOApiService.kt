package com.north.mobile.data.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val message: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class CFOChatRequest(
    val message: String,
    val userId: String,
    val conversationHistory: List<ChatMessage> = emptyList()
)

@Serializable
data class CFOChatResponse(
    val response: String,
    val suggestedReplies: List<String>? = null,
    val goalCreated: String? = null,
    val actionRequired: String? = null
)

interface PersonalCFOApiService {
    suspend fun sendMessage(request: CFOChatRequest): CFOChatResponse
    suspend fun startConversation(userId: String): CFOChatResponse
}

class PersonalCFOApiServiceImpl(
    private val apiClient: ApiClient
) : PersonalCFOApiService {
    
    override suspend fun sendMessage(request: CFOChatRequest): CFOChatResponse {
        return try {
            apiClient.httpClient.post("/api/cfo/chat") {
                setBody(request)
            }.body()
        } catch (e: Exception) {
            // Fallback response for testing
            CFOChatResponse(
                response = "I'm here to help you with your finances! Tell me about your current financial situation and goals.",
                suggestedReplies = listOf(
                    "I want to save for a vacation",
                    "Help me create a budget",
                    "I need to pay off debt"
                )
            )
        }
    }
    
    override suspend fun startConversation(userId: String): CFOChatResponse {
        return try {
            apiClient.httpClient.post("/api/cfo/start") {
                setBody(mapOf("userId" to userId))
            }.body()
        } catch (e: Exception) {
            // Fallback response for testing
            CFOChatResponse(
                response = "Hi there! I'm your Personal CFO and I'm excited to help you achieve your financial goals. Let's start by getting to know each other - what's your name and what brings you to North today?",
                suggestedReplies = listOf(
                    "I want to get better with money",
                    "I need help saving",
                    "I want to plan for the future"
                )
            )
        }
    }
}