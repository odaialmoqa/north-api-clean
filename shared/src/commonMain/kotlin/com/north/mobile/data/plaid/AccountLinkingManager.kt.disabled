package com.north.mobile.data.plaid

import com.north.mobile.domain.model.Account
import com.north.mobile.domain.model.FinancialInstitution
import com.north.mobile.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate

interface AccountLinkingManager {
    /**
     * Current status of account linking
     */
    val linkingStatus: StateFlow<AccountLinkingStatus>
    
    /**
     * Get supported Canadian financial institutions
     */
    suspend fun getSupportedInstitutions(): Result<List<FinancialInstitution>>
    
    /**
     * Search institutions by name
     */
    suspend fun searchInstitutions(query: String): Result<List<FinancialInstitution>>
    
    /**
     * Start the account linking process
     */
    suspend fun startLinking(userId: String): Result<PlaidLinkToken>
    
    /**
     * Complete the linking process after user authentication
     */
    suspend fun completeLinking(publicToken: String): Result<List<Account>>
    
    /**
     * Get all linked accounts
     */
    suspend fun getLinkedAccounts(): Result<List<Account>>
    
    /**
     * Refresh account balances
     */
    suspend fun refreshBalances(): Result<List<Account>>
    
    /**
     * Get transactions for linked accounts
     */
    suspend fun getTransactions(
        startDate: LocalDate,
        endDate: LocalDate,
        accountIds: List<String>? = null
    ): Result<List<Transaction>>
    
    /**
     * Check connection status and handle re-authentication if needed
     */
    suspend fun checkConnectionStatus(): Result<List<ConnectionStatus>>
    
    /**
     * Start re-authentication for a specific item
     */
    suspend fun startReauthentication(itemId: String): Result<PlaidLinkToken>
    
    /**
     * Disconnect/remove a linked account
     */
    suspend fun disconnectAccount(itemId: String): Result<Unit>
    
    /**
     * Get connection health for all linked items
     */
    fun getConnectionHealth(): Flow<List<ConnectionHealth>>
}

data class ConnectionStatus(
    val itemId: String,
    val institutionName: String,
    val status: AccountLinkingStatus,
    val lastSuccessfulUpdate: kotlinx.datetime.Instant?,
    val error: PlaidError?
)

data class ConnectionHealth(
    val itemId: String,
    val institutionName: String,
    val isHealthy: Boolean,
    val lastUpdate: kotlinx.datetime.Instant?,
    val errorCount: Int,
    val requiresReauth: Boolean
)

class AccountLinkingManagerImpl(
    private val plaidService: PlaidService,
    private val accountRepository: com.north.mobile.data.repository.AccountRepository,
    private val secureStorage: com.north.mobile.data.security.EncryptionManager
) : AccountLinkingManager {
    
    private val _linkingStatus = MutableStateFlow<AccountLinkingStatus>(AccountLinkingStatus.NotStarted)
    override val linkingStatus: StateFlow<AccountLinkingStatus> = _linkingStatus.asStateFlow()
    
    private val accessTokens = mutableMapOf<String, String>() // itemId -> accessToken
    
    override suspend fun getSupportedInstitutions(): Result<List<FinancialInstitution>> {
        return plaidService.getCanadianInstitutions()
    }
    
    override suspend fun searchInstitutions(query: String): Result<List<FinancialInstitution>> {
        return plaidService.searchInstitutions(query)
    }
    
    override suspend fun startLinking(userId: String): Result<PlaidLinkToken> {
        _linkingStatus.value = AccountLinkingStatus.InProgress
        
        return plaidService.createLinkToken(userId).onFailure { error ->
            _linkingStatus.value = when (error) {
                is PlaidServiceError.ItemError -> AccountLinkingStatus.Failed(error.plaidError)
                else -> AccountLinkingStatus.Failed(
                    PlaidError(
                        errorType = "UNKNOWN_ERROR",
                        errorCode = "LINK_TOKEN_CREATION_FAILED",
                        errorMessage = error.message ?: "Failed to create link token",
                        displayMessage = "Unable to start account linking. Please try again.",
                        requestId = null
                    )
                )
            }
        }
    }
    
    override suspend fun completeLinking(publicToken: String): Result<List<Account>> {
        return try {
            // Exchange public token for access token
            val accessTokenResult = plaidService.exchangePublicToken(publicToken)
            if (accessTokenResult.isFailure) {
                val error = accessTokenResult.exceptionOrNull()
                _linkingStatus.value = when (error) {
                    is PlaidServiceError.ItemError -> AccountLinkingStatus.Failed(error.plaidError)
                    else -> AccountLinkingStatus.Failed(
                        PlaidError(
                            errorType = "TOKEN_EXCHANGE_ERROR",
                            errorCode = "PUBLIC_TOKEN_EXCHANGE_FAILED",
                            errorMessage = error?.message ?: "Failed to exchange token",
                            displayMessage = "Unable to complete account linking. Please try again.",
                            requestId = null
                        )
                    )
                }
                return Result.failure(error ?: Exception("Token exchange failed"))
            }
            
            val accessToken = accessTokenResult.getOrThrow()
            
            // Store access token securely
            storeAccessToken(accessToken.itemId, accessToken.accessToken)
            
            // Get accounts
            val accountsResult = plaidService.getAccounts(accessToken.accessToken)
            if (accountsResult.isFailure) {
                val error = accountsResult.exceptionOrNull()
                _linkingStatus.value = AccountLinkingStatus.Failed(
                    PlaidError(
                        errorType = "ACCOUNT_FETCH_ERROR",
                        errorCode = "ACCOUNTS_GET_FAILED",
                        errorMessage = error?.message ?: "Failed to fetch accounts",
                        displayMessage = "Unable to retrieve account information. Please try again.",
                        requestId = null
                    )
                )
                return Result.failure(error ?: Exception("Failed to fetch accounts"))
            }
            
            val plaidAccounts = accountsResult.getOrThrow()
            val accounts = plaidAccounts.map { plaidAccount ->
                plaidAccount.toAccount(accessToken.institutionId, accessToken.institutionName)
            }
            
            // Save accounts to local database
            accounts.forEach { account ->
                accountRepository.saveAccount(account)
            }
            
            _linkingStatus.value = AccountLinkingStatus.Connected(
                itemId = accessToken.itemId,
                accountCount = accounts.size
            )
            
            Result.success(accounts)
        } catch (e: Exception) {
            _linkingStatus.value = AccountLinkingStatus.Failed(
                PlaidError(
                    errorType = "UNKNOWN_ERROR",
                    errorCode = "LINKING_COMPLETION_FAILED",
                    errorMessage = e.message ?: "Unknown error occurred",
                    displayMessage = "Unable to complete account linking. Please try again.",
                    requestId = null
                )
            )
            Result.failure(e)
        }
    }
    
    override suspend fun getLinkedAccounts(): Result<List<Account>> {
        return try {
            val accounts = accountRepository.getAllAccounts()
            Result.success(accounts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun refreshBalances(): Result<List<Account>> {
        return try {
            val updatedAccounts = mutableListOf<Account>()
            
            for ((itemId, accessToken) in accessTokens) {
                val balancesResult = plaidService.getBalances(accessToken)
                if (balancesResult.isSuccess) {
                    val plaidAccounts = balancesResult.getOrThrow()
                    val accounts = plaidAccounts.map { plaidAccount ->
                        // Get institution info from stored data or Plaid
                        val existingAccount = accountRepository.getAccountById(plaidAccount.accountId)
                        plaidAccount.toAccount(
                            existingAccount?.institutionId ?: "unknown",
                            existingAccount?.institutionName ?: "Unknown Bank"
                        )
                    }
                    
                    // Update accounts in database
                    accounts.forEach { account ->
                        accountRepository.updateAccount(account)
                    }
                    
                    updatedAccounts.addAll(accounts)
                }
            }
            
            Result.success(updatedAccounts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getTransactions(
        startDate: LocalDate,
        endDate: LocalDate,
        accountIds: List<String>?
    ): Result<List<Transaction>> {
        return try {
            val allTransactions = mutableListOf<Transaction>()
            
            for ((itemId, accessToken) in accessTokens) {
                val transactionsResult = plaidService.getTransactions(
                    accessToken = accessToken,
                    startDate = startDate,
                    endDate = endDate,
                    accountIds = accountIds
                )
                
                if (transactionsResult.isSuccess) {
                    allTransactions.addAll(transactionsResult.getOrThrow())
                }
            }
            
            Result.success(allTransactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun checkConnectionStatus(): Result<List<ConnectionStatus>> {
        return try {
            val statuses = mutableListOf<ConnectionStatus>()
            
            for ((itemId, accessToken) in accessTokens) {
                val itemResult = plaidService.getItem(accessToken)
                if (itemResult.isSuccess) {
                    val item = itemResult.getOrThrow()
                    val institutionName = com.north.mobile.domain.model.CanadianInstitutions
                        .getById(item.institutionId)?.displayName ?: "Unknown Bank"
                    
                    val status = when {
                        item.error != null -> {
                            if (item.error.errorType == "ITEM_LOGIN_REQUIRED") {
                                AccountLinkingStatus.RequiresReauth(itemId)
                            } else {
                                AccountLinkingStatus.Failed(item.error)
                            }
                        }
                        else -> AccountLinkingStatus.Connected(itemId, 0) // Account count would need to be fetched
                    }
                    
                    statuses.add(ConnectionStatus(
                        itemId = itemId,
                        institutionName = institutionName,
                        status = status,
                        lastSuccessfulUpdate = kotlinx.datetime.Clock.System.now(), // Would track this properly
                        error = item.error
                    ))
                }
            }
            
            Result.success(statuses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun startReauthentication(itemId: String): Result<PlaidLinkToken> {
        val accessToken = accessTokens[itemId]
            ?: return Result.failure(Exception("Access token not found for item: $itemId"))
        
        return plaidService.createUpdateLinkToken(accessToken)
    }
    
    override suspend fun disconnectAccount(itemId: String): Result<Unit> {
        val accessToken = accessTokens[itemId]
            ?: return Result.failure(Exception("Access token not found for item: $itemId"))
        
        return plaidService.removeItem(accessToken).onSuccess {
            // Remove from local storage
            accessTokens.remove(itemId)
            removeStoredAccessToken(itemId)
            
            // Remove accounts from local database
            // This would need to be implemented in the repository
            // accountRepository.removeAccountsByItemId(itemId)
            
            _linkingStatus.value = AccountLinkingStatus.Disconnected
        }
    }
    
    override fun getConnectionHealth(): Flow<List<ConnectionHealth>> {
        // This would return a flow that monitors connection health
        // For now, return empty flow - would need proper implementation
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }
    
    private suspend fun storeAccessToken(itemId: String, accessToken: String) {
        try {
            val encryptResult = secureStorage.encrypt(accessToken, "plaid_token_$itemId")
            encryptResult.fold(
                onSuccess = { encryptedData ->
                    // Store encrypted token - in real implementation this would go to secure storage
                    // For now, store in memory (not secure, just for demo)
                    accessTokens[itemId] = accessToken
                },
                onFailure = { error ->
                    throw Exception("Failed to encrypt access token", error)
                }
            )
        } catch (e: Exception) {
            throw Exception("Failed to store access token securely", e)
        }
    }
    
    private suspend fun removeStoredAccessToken(itemId: String) {
        try {
            // Remove from secure storage
            // This would need proper implementation
            accessTokens.remove(itemId)
        } catch (e: Exception) {
            // Log error but don't throw - removal should be best effort
        }
    }
}