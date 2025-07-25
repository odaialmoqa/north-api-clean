package com.north.mobile.data.plaid

import com.north.mobile.domain.model.Account
import com.north.mobile.domain.model.FinancialInstitution
import com.north.mobile.domain.model.Transaction

interface PlaidService {
    /**
     * Create a link token for initiating the Plaid Link flow
     */
    suspend fun createLinkToken(userId: String): Result<PlaidLinkToken>
    
    /**
     * Exchange public token for access token after successful linking
     */
    suspend fun exchangePublicToken(publicToken: String): Result<PlaidAccessToken>
    
    /**
     * Get accounts for a linked item
     */
    suspend fun getAccounts(accessToken: String): Result<List<PlaidAccount>>
    
    /**
     * Get account balances
     */
    suspend fun getBalances(accessToken: String): Result<List<PlaidAccount>>
    
    /**
     * Get transactions for an account
     */
    suspend fun getTransactions(
        accessToken: String,
        startDate: kotlinx.datetime.LocalDate,
        endDate: kotlinx.datetime.LocalDate,
        accountIds: List<String>? = null
    ): Result<List<Transaction>>
    
    /**
     * Get item information (connection status, errors, etc.)
     */
    suspend fun getItem(accessToken: String): Result<PlaidItem>
    
    /**
     * Remove/disconnect an item
     */
    suspend fun removeItem(accessToken: String): Result<Unit>
    
    /**
     * Create update mode link token for re-authentication
     */
    suspend fun createUpdateLinkToken(accessToken: String): Result<PlaidLinkToken>
    
    /**
     * Get supported Canadian financial institutions
     */
    suspend fun getCanadianInstitutions(): Result<List<FinancialInstitution>>
    
    /**
     * Search institutions by name
     */
    suspend fun searchInstitutions(query: String): Result<List<FinancialInstitution>>
}

sealed class PlaidServiceError : Exception() {
    data class NetworkError(override val message: String, override val cause: Throwable? = null) : PlaidServiceError()
    data class AuthenticationError(override val message: String) : PlaidServiceError()
    data class InvalidRequestError(override val message: String) : PlaidServiceError()
    data class ItemError(val plaidError: PlaidError) : PlaidServiceError()
    data class RateLimitError(override val message: String) : PlaidServiceError()
    data class ApiError(override val message: String, val errorCode: String) : PlaidServiceError()
    data class UnknownError(override val message: String, override val cause: Throwable? = null) : PlaidServiceError()
}