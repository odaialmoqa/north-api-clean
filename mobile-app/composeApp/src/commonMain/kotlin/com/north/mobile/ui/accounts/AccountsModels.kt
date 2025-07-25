package com.north.mobile.ui.accounts

import com.north.mobile.data.plaid.SimplePlaidAccount
import com.north.mobile.data.plaid.SimplePlaidTransaction

/**
 * Connection health status for account management
 */
enum class ConnectionHealth {
    Unknown,
    Excellent,    // All accounts healthy, recent sync
    Good,         // Most accounts healthy
    Warning,      // Some accounts need attention
    Critical      // Multiple accounts have issues
}

/**
 * Account sync status for individual accounts
 */
data class AccountSyncStatus(
    val accountId: String,
    val isLoading: Boolean = false,
    val lastSyncAttempt: Long? = null,
    val syncError: String? = null
)

/**
 * Enhanced account state with additional metadata
 */
data class EnhancedAccountState(
    val account: SimplePlaidAccount,
    val transactions: List<SimplePlaidTransaction> = emptyList(),
    val syncStatus: AccountSyncStatus = AccountSyncStatus(account.id),
    val isExpanded: Boolean = false
)

/**
 * Privacy settings for account data sharing
 */
data class AccountPrivacySettings(
    val accountId: String,
    val aiInsightsEnabled: Boolean = true,
    val dataExportAllowed: Boolean = true,
    val retentionPeriodDays: Int = 365
)

/**
 * Account management actions
 */
sealed class AccountAction {
    object Refresh : AccountAction()
    data class Connect(val onSuccess: (String) -> Unit, val onError: (String) -> Unit) : AccountAction()
    data class Disconnect(val accountId: String) : AccountAction()
    data class Reconnect(val accountId: String) : AccountAction()
    data class SyncTransactions(val accountId: String) : AccountAction()
    data class ViewDetails(val accountId: String) : AccountAction()
    data class ManagePrivacy(val accountId: String) : AccountAction()
    data class ExportData(val accountId: String) : AccountAction()
    data class DeleteData(val accountId: String) : AccountAction()
}