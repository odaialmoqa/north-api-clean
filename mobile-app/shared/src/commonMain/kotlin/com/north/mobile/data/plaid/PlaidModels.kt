package com.north.mobile.data.plaid

import kotlinx.serialization.Serializable
import com.north.mobile.data.api.ApiClient

/**
 * Plaid API models for North mobile app
 */

@Serializable
data class LinkTokenRequest(
    val client_name: String,
    val country_codes: List<String>,
    val language: String,
    val user: LinkTokenUser,
    val products: List<String>
)

@Serializable
data class LinkTokenUser(
    val client_user_id: String
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
    val accounts: List<AccountData>,
    val access_token: String = "",
    val item_id: String = "",
    val institution_name: String = "",
    val transactions_synced: Boolean = false,
    val insights_generated: Boolean = false
)

@Serializable
data class AccountData(
    val id: String,
    val name: String,
    val type: String,
    val subtype: String?,
    val balance: Double,
    val institutionName: String,
    val lastSyncTime: Long,
    val connectionStatus: String
)

@Serializable
data class AccountsResponse(
    val accounts: List<AccountData>
)

@Serializable
data class TransactionSyncResponse(
    val success: Boolean,
    val transactions: List<TransactionData>
)

@Serializable
data class TransactionData(
    val id: String,
    val accountId: String,
    val amount: Double,
    val description: String,
    val category: List<String>,
    val date: String,
    val merchantName: String?
)

@Serializable
data class DisconnectResponse(
    val success: Boolean,
    val message: String = ""
)

@Serializable
data class PlaidAccount(
    val id: String,
    val name: String,
    val type: String,
    val subtype: String,
    val balance: Double,
    val institutionName: String,
    val lastSyncTime: Long,
    val connectionStatus: String
)

@Serializable
data class PlaidAccountsResponse(
    val accounts: List<PlaidAccount>
)

@Serializable
data class PlaidTransaction(
    val id: String,
    val accountId: String,
    val amount: Double,
    val description: String,
    val category: String,
    val date: String,
    val isRecurring: Boolean
)

@Serializable
data class PlaidTransactionsResponse(
    val transactions: List<PlaidTransaction>,
    val total: Int,
    val hasMore: Boolean
)

@Serializable
data class PlaidError(
    val error: String,
    val message: String? = null
)

// New models for transaction processing
@Serializable
data class Transaction(
    val transaction_id: String,
    val account_id: String,
    val amount: Double,
    val date: String,
    val name: String,
    val merchant_name: String?,
    val category: List<String>?,
    val pending: Boolean
)

@Serializable
data class TransactionsResponse(
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val total_transactions: Int,
    val request_id: String
)

@Serializable
data class Account(
    val account_id: String,
    val balances: AccountBalance,
    val mask: String,
    val name: String,
    val official_name: String?,
    val type: String,
    val subtype: String
)

@Serializable
data class AccountBalance(
    val available: Double?,
    val current: Double,
    val limit: Double?
)

// Service class for handling Plaid token exchange and transactions
class PlaidTokenService(
    private val apiClient: ApiClient,
    private val sessionManager: com.north.mobile.data.auth.SessionManager
) {
    
    suspend fun exchangePublicToken(publicToken: String): ExchangeTokenResponse {
        val authToken = sessionManager.getAuthToken() 
            ?: throw Exception("No auth token available - please log in")
            
        return apiClient.post(
            endpoint = "/api/plaid/exchange-public-token",
            body = ExchangeTokenRequest(public_token = publicToken),
            token = authToken
        )
    }
    
    suspend fun getTransactions(accessToken: String, startDate: String, endDate: String): TransactionsResponse {
        val authToken = sessionManager.getAuthToken() 
            ?: throw Exception("No auth token available - please log in")
            
        return apiClient.post(
            endpoint = "/api/plaid/transactions",
            body = mapOf(
                "access_token" to accessToken,
                "start_date" to startDate,
                "end_date" to endDate
            ),
            token = authToken
        )
    }
    
    suspend fun getAccounts(accessToken: String): List<Account> {
        val authToken = sessionManager.getAuthToken() 
            ?: throw Exception("No auth token available - please log in")
            
        val response: Map<String, Any> = apiClient.post(
            endpoint = "/api/plaid/accounts",
            body = mapOf("access_token" to accessToken),
            token = authToken
        )
        
        // Parse the accounts from the response
        return emptyList() // TODO: Implement proper parsing
    }
}