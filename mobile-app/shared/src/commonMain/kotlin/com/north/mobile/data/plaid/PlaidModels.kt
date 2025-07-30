package com.north.mobile.data.plaid

import kotlinx.serialization.Serializable

/**
 * Plaid API models for North mobile app
 */

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
    val access_token: String,
    val item_id: String,
    val request_id: String
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