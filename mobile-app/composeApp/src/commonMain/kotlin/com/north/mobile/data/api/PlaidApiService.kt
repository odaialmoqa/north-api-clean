package com.north.mobile.data.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateLinkTokenRequest(
    val user_id: String? = null
)

@Serializable
data class CreateLinkTokenResponse(
    val link_token: String,
    val expiration: String
)

@Serializable
data class ExchangePublicTokenRequest(
    val public_token: String
)

@Serializable
data class ExchangePublicTokenResponse(
    val access_token: String,
    val item_id: String,
    val request_id: String
)

/**
 * API service for Plaid integration
 */
class PlaidApiService(
    private val apiClient: ApiClient,
    private val getAuthToken: () -> String?
) {
    
    /**
     * Create a link token for Plaid Link
     */
    suspend fun createLinkToken(): Result<CreateLinkTokenResponse> {
        return try {
            val authToken = getAuthToken() // Can be null for testing
            val response = apiClient.post<CreateLinkTokenResponse>(
                "/api/plaid/create-link-token",
                CreateLinkTokenRequest(),
                authToken // Pass null if no auth token available
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Exchange public token for access token
     */
    suspend fun exchangePublicToken(publicToken: String): Result<ExchangePublicTokenResponse> {
        return try {
            val authToken = getAuthToken() ?: throw Exception("No auth token available")
            val response = apiClient.post<ExchangePublicTokenResponse>(
                "/api/plaid/exchange-public-token",
                ExchangePublicTokenRequest(publicToken),
                authToken
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}