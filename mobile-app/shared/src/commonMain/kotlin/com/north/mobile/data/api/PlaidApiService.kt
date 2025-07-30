package com.north.mobile.data.api

import kotlinx.serialization.Serializable

@Serializable
data class LinkTokenRequest(
    val user_id: String? = null
)

@Serializable
data class LinkTokenResponse(
    val link_token: String,
    val expiration: String
)

@Serializable
data class ExchangeTokenRequest(
    val public_token: String
)

@Serializable
data class ExchangeTokenResponse(
    val success: Boolean,
    val accounts: List<PlaidAccount>,
    val access_token: String,
    val item_id: String
)

@Serializable
data class PlaidAccount(
    val id: String,
    val name: String,
    val type: String,
    val subtype: String?,
    val balance: Double,
    val institutionName: String,
    val lastSyncTime: Long,
    val connectionStatus: String
)

class PlaidApiService(private val apiClient: ApiClient) {
    
    suspend fun createLinkToken(): Result<LinkTokenResponse> {
        return try {
            val response = apiClient.post<LinkTokenResponse>("/api/plaid/create-link-token", LinkTokenRequest())
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to create link token: ${e.message}"))
        }
    }
    
    suspend fun exchangePublicToken(publicToken: String, authToken: String): Result<ExchangeTokenResponse> {
        return try {
            val response = apiClient.post<ExchangeTokenResponse>(
                "/api/plaid/exchange-public-token", 
                ExchangeTokenRequest(publicToken),
                authToken
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to exchange public token: ${e.message}"))
        }
    }
}