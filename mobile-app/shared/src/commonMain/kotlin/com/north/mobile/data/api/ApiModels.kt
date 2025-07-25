package com.north.mobile.data.api

import kotlinx.serialization.Serializable

// Authentication Models
@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val message: String,
    val user: UserResponse,
    val token: String
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String
)

// API Error Response
@Serializable
data class ApiError(
    val error: String,
    val message: String? = null
)

// Health Check Response
@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: String,
    val database: String,
    val db_time: String? = null
)

// API Info Response
@Serializable
data class ApiInfoResponse(
    val name: String,
    val version: String,
    val status: String
)

// Financial Data Models (to be expanded)
@Serializable
data class AccountData(
    val id: String,
    val name: String,
    val type: String,
    val balance: Double,
    val currency: String = "CAD"
)

@Serializable
data class TransactionData(
    val id: String,
    val accountId: String,
    val amount: Double,
    val description: String,
    val category: String,
    val date: String,
    val isRecurring: Boolean = false
)

@Serializable
data class FinancialGoalData(
    val id: String,
    val userId: String,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: String,
    val priority: String
)

// AI CFO Brain Models (Gemini-powered)
@Serializable
data class ChatRequest(
    val message: String
)

@Serializable
data class ChatResponse(
    val response: String
)

// Legacy Chat Models (for backward compatibility)
@Serializable
data class ChatContext(
    val userId: String,
    val conversationId: String? = null,
    val previousMessages: List<String> = emptyList()
)

// Financial Summary Response
@Serializable
data class FinancialSummaryResponse(
    val netWorth: Double,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val accounts: List<AccountData>
)

// Transactions Response
@Serializable
data class TransactionsResponse(
    val transactions: List<TransactionData>,
    val total: Int,
    val hasMore: Boolean
)

// Password Reset Models
@Serializable
data class PasswordResetRequest(
    val email: String
)

@Serializable
data class PasswordResetResponse(
    val message: String,
    val success: Boolean
)