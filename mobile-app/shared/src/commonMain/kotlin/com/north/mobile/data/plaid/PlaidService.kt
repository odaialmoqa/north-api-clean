package com.north.mobile.data.plaid

import com.north.mobile.data.api.ApiClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Service for Plaid integration with North backend
 */
class PlaidService(private val apiClient: ApiClient) {
    
    /**
     * Create a Plaid Link token for account connection
     */
    suspend fun createLinkToken(): Result<LinkTokenResponse> {
        return try {
            val response = apiClient.httpClient.post("/api/plaid/create-link-token") {
                setBody(emptyMap<String, Any>())
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val linkTokenResponse = response.body<LinkTokenResponse>()
                    Result.success(linkTokenResponse)
                }
                else -> {
                    val errorText = response.body<String>()
                    Result.failure(Exception("Failed to create link token: ${response.status} - $errorText"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error creating link token: ${e.message}"))
        }
    }
    
    /**
     * Exchange public token for access token
     */
    suspend fun exchangePublicToken(publicToken: String, authToken: String): Result<ExchangeTokenResponse> {
        return try {
            val request = ExchangeTokenRequest(public_token = publicToken)
            
            val response = apiClient.httpClient.post("/api/plaid/exchange-public-token") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $authToken")
                }
                setBody(request)
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val exchangeResponse = response.body<ExchangeTokenResponse>()
                    Result.success(exchangeResponse)
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(Exception("Authentication required - please log in"))
                }
                else -> {
                    val errorText = try {
                        response.body<String>()
                    } catch (e: Exception) {
                        "Unknown error"
                    }
                    Result.failure(Exception("Failed to exchange token: ${response.status} - $errorText"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error exchanging token: ${e.message}"))
        }
    }
    
    /**
     * Get connected accounts
     */
    suspend fun getAccounts(authToken: String): Result<PlaidAccountsResponse> {
        return try {
            val response = apiClient.httpClient.get("/api/plaid/accounts") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $authToken")
                }
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val accountsResponse = response.body<PlaidAccountsResponse>()
                    Result.success(accountsResponse)
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(Exception("Authentication required"))
                }
                else -> {
                    val errorText = response.body<String>()
                    Result.failure(Exception("Failed to get accounts: ${response.status} - $errorText"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error getting accounts: ${e.message}"))
        }
    }
    
    /**
     * Get transactions
     */
    suspend fun getTransactions(authToken: String, limit: Int = 50, offset: Int = 0): Result<PlaidTransactionsResponse> {
        return try {
            val response = apiClient.httpClient.get("/api/transactions") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $authToken")
                }
                parameter("limit", limit)
                parameter("offset", offset)
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val transactionsResponse = response.body<PlaidTransactionsResponse>()
                    Result.success(transactionsResponse)
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(Exception("Authentication required"))
                }
                else -> {
                    val errorText = response.body<String>()
                    Result.failure(Exception("Failed to get transactions: ${response.status} - $errorText"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error getting transactions: ${e.message}"))
        }
    }
    
    /**
     * Sync transactions for a specific account
     */
    suspend fun syncTransactions(authToken: String, accountId: String): Result<Unit> {
        return try {
            val request = mapOf("accountId" to accountId)
            
            val response = apiClient.httpClient.post("/api/plaid/sync-transactions") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $authToken")
                }
                setBody(request)
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    Result.success(Unit)
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(Exception("Authentication required"))
                }
                else -> {
                    val errorText = response.body<String>()
                    Result.failure(Exception("Failed to sync transactions: ${response.status} - $errorText"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error syncing transactions: ${e.message}"))
        }
    }
}