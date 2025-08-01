package com.north.mobile.data.plaid

import com.north.mobile.data.api.ApiClient
import kotlinx.serialization.Serializable

// Models are defined in PlaidModels.kt to avoid duplication

// Simplified data classes for integration service
@Serializable
data class SimplePlaidAccount(
    val id: String,
    val name: String,
    val type: String,
    val subtype: String?,
    val balance: Double,
    val institutionName: String,
    val lastSyncTime: Long,
    val connectionStatus: PlaidConnectionStatus
)

@Serializable
data class SimplePlaidTransaction(
    val id: String,
    val accountId: String,
    val amount: Double,
    val description: String,
    val category: List<String>,
    val date: String,
    val merchantName: String?
)

enum class PlaidConnectionStatus {
    HEALTHY,
    NEEDS_REAUTH,
    SYNC_ERROR,
    DISCONNECTED
}

@Serializable
data class SimplePlaidLinkResult(
    val success: Boolean,
    val publicToken: String?,
    val error: String?
)

@Serializable
data class SimpleLinkTokenResult(
    val success: Boolean,
    val linkToken: String?,
    val error: String?
)

@Serializable
data class SimpleAccountConnectionResult(
    val success: Boolean,
    val accounts: List<SimplePlaidAccount>,
    val error: String?
)

@Serializable
data class SimpleTransactionSyncResult(
    val success: Boolean,
    val transactions: List<SimplePlaidTransaction>,
    val error: String?
)

interface PlaidIntegrationService {
    suspend fun initializePlaidLink(): SimpleLinkTokenResult
    suspend fun exchangePublicToken(publicToken: String): SimpleAccountConnectionResult
    suspend fun getAccounts(userId: String): List<SimplePlaidAccount>
    suspend fun syncTransactions(accountId: String): SimpleTransactionSyncResult
    suspend fun disconnectAccount(accountId: String): Boolean
    suspend fun refreshAccountConnection(accountId: String): SimpleAccountConnectionResult
}

// Implementation that connects to your North API server
class PlaidIntegrationServiceImpl(
    private val apiClient: ApiClient,
    private val getAuthToken: () -> String?
) : PlaidIntegrationService {
    
    override suspend fun initializePlaidLink(): SimpleLinkTokenResult {
        return try {
            val token = getAuthToken() ?: throw Exception("No auth token available")
            val response = apiClient.post<LinkTokenResponse>("/api/plaid/create-link-token", emptyMap<String, Any>(), token)
            SimpleLinkTokenResult(
                success = true,
                linkToken = response.link_token,
                error = null
            )
        } catch (e: Exception) {
            SimpleLinkTokenResult(
                success = false,
                linkToken = null,
                error = e.message ?: "Failed to create link token"
            )
        }
    }
    
    override suspend fun exchangePublicToken(publicToken: String): SimpleAccountConnectionResult {
        return try {
            val token = getAuthToken() ?: throw Exception("No auth token available")
            println("🔑 Auth token for exchange request: ${token.take(20)}...")
            println("📡 Exchanging public token: ${publicToken.take(20)}...")
            
            val response = apiClient.post<ExchangeTokenResponse>(
                "/api/plaid/exchange-public-token",
                mapOf("public_token" to publicToken),
                token
            )
            println("📡 Exchange response: success=${response.success}")
            
            if (response.success) {
                val accounts = response.accounts.map { account ->
                    SimplePlaidAccount(
                        id = account.id,
                        name = account.name,
                        type = account.type,
                        subtype = account.subtype,
                        balance = account.balance,
                        institutionName = account.institutionName,
                        lastSyncTime = account.lastSyncTime,
                        connectionStatus = try {
                            PlaidConnectionStatus.valueOf(account.connectionStatus)
                        } catch (e: Exception) {
                            PlaidConnectionStatus.HEALTHY
                        }
                    )
                }
                
                SimpleAccountConnectionResult(
                    success = true,
                    accounts = accounts,
                    error = null
                )
            } else {
                SimpleAccountConnectionResult(
                    success = false,
                    accounts = emptyList(),
                    error = "Failed to exchange token"
                )
            }
        } catch (e: Exception) {
            SimpleAccountConnectionResult(
                success = false,
                accounts = emptyList(),
                error = e.message ?: "Failed to exchange public token"
            )
        }
    }
    
    override suspend fun getAccounts(userId: String): List<SimplePlaidAccount> {
        return try {
            val token = getAuthToken() ?: throw Exception("No auth token available")
            val response = apiClient.get<AccountsResponse>("/api/plaid/accounts", token)
            response.accounts.map { account ->
                SimplePlaidAccount(
                    id = account.id,
                    name = account.name,
                    type = account.type,
                    subtype = account.subtype,
                    balance = account.balance,
                    institutionName = account.institutionName,
                    lastSyncTime = account.lastSyncTime,
                    connectionStatus = try {
                        PlaidConnectionStatus.valueOf(account.connectionStatus)
                    } catch (e: Exception) {
                        PlaidConnectionStatus.HEALTHY
                    }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun syncTransactions(accountId: String): SimpleTransactionSyncResult {
        return try {
            val token = getAuthToken() ?: throw Exception("No auth token available")
            val response = apiClient.post<TransactionSyncResponse>(
                "/api/plaid/sync-transactions",
                mapOf("accountId" to accountId),
                token
            )
            
            if (response.success) {
                val transactions = response.transactions.map { txn ->
                    SimplePlaidTransaction(
                        id = txn.id,
                        accountId = txn.accountId,
                        amount = txn.amount,
                        description = txn.description,
                        category = txn.category,
                        date = txn.date,
                        merchantName = txn.merchantName
                    )
                }
                
                SimpleTransactionSyncResult(
                    success = true,
                    transactions = transactions,
                    error = null
                )
            } else {
                SimpleTransactionSyncResult(
                    success = false,
                    transactions = emptyList(),
                    error = "Failed to sync transactions"
                )
            }
        } catch (e: Exception) {
            SimpleTransactionSyncResult(
                success = false,
                transactions = emptyList(),
                error = e.message ?: "Failed to sync transactions"
            )
        }
    }
    
    override suspend fun disconnectAccount(accountId: String): Boolean {
        return try {
            val token = getAuthToken() ?: throw Exception("No auth token available")
            val response = apiClient.post<DisconnectResponse>(
                "/api/plaid/disconnect-account",
                mapOf("accountId" to accountId),
                token
            )
            response.success
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun refreshAccountConnection(accountId: String): SimpleAccountConnectionResult {
        // For now, just re-fetch accounts
        return SimpleAccountConnectionResult(
            success = true,
            accounts = getAccounts("current_user"),
            error = null
        )
    }
}

// Note: API Response models are defined in PlaidModels.kt to avoid duplication