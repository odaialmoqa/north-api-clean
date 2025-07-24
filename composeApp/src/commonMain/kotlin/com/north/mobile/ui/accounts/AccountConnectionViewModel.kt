package com.north.mobile.ui.accounts

import androidx.compose.runtime.*
import com.north.mobile.data.auth.SessionManager
import com.north.mobile.data.plaid.PlaidIntegrationService
import com.north.mobile.data.plaid.SimplePlaidAccount
import com.north.mobile.data.plaid.SimplePlaidLinkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AccountConnectionViewModel(
    private val sessionManager: SessionManager,
    private val plaidService: PlaidIntegrationService,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private var _uiState by mutableStateOf(AccountConnectionState())
    val uiState: AccountConnectionState get() = _uiState
    
    init {
        loadConnectedAccounts()
    }
    
    fun startAccountConnection() {
        _uiState = _uiState.copy(
            isConnecting = true,
            connectionStep = ConnectionStep.INITIALIZING
        )
        
        coroutineScope.launch {
            try {
                // Initialize Plaid Link
                _uiState = _uiState.copy(connectionStep = ConnectionStep.INITIALIZING)
                val linkResult = plaidService.initializePlaidLink()
                
                val publicToken = linkResult.publicToken
                if (linkResult.success && publicToken != null) {
                    // Successfully got public token, now exchange it
                    exchangePublicToken(publicToken)
                } else {
                    // Failed to get public token
                    _uiState = _uiState.copy(
                        isConnecting = false,
                        connectionStep = ConnectionStep.ERROR,
                        connectionSuccess = false,
                        connectionError = linkResult.error ?: "Failed to initialize connection"
                    )
                }
            } catch (e: Exception) {
                // Handle any exceptions
                _uiState = _uiState.copy(
                    isConnecting = false,
                    connectionStep = ConnectionStep.ERROR,
                    connectionSuccess = false,
                    connectionError = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }
    
    fun exchangePublicToken(publicToken: String) {
        _uiState = _uiState.copy(
            isConnecting = true,
            connectionStep = ConnectionStep.EXCHANGING_TOKEN
        )
        
        coroutineScope.launch {
            try {
                val connectionResult = plaidService.exchangePublicToken(publicToken)
                
                if (connectionResult.success) {
                    // Successfully connected accounts
                    _uiState = _uiState.copy(
                        isConnecting = false,
                        connectionStep = ConnectionStep.COMPLETED,
                        connectedAccounts = connectionResult.accounts,
                        connectionSuccess = true,
                        connectionError = null
                    )
                    
                    // Refresh the accounts list
                    loadConnectedAccounts()
                } else {
                    // Failed to exchange token
                    _uiState = _uiState.copy(
                        isConnecting = false,
                        connectionStep = ConnectionStep.ERROR,
                        connectionSuccess = false,
                        connectionError = connectionResult.error ?: "Failed to connect account"
                    )
                }
            } catch (e: Exception) {
                // Handle any exceptions
                _uiState = _uiState.copy(
                    isConnecting = false,
                    connectionStep = ConnectionStep.ERROR,
                    connectionSuccess = false,
                    connectionError = e.message ?: "Failed to exchange token: ${e.message}"
                )
            }
        }
    }
    
    fun disconnectAccount(accountId: String) {
        _uiState = _uiState.copy(isDeletingAccount = accountId)
        
        coroutineScope.launch {
            try {
                val success = plaidService.disconnectAccount(accountId)
                
                if (success) {
                    // Successfully disconnected account
                    _uiState = _uiState.copy(
                        isDeletingAccount = null,
                        disconnectSuccess = true,
                        disconnectError = null
                    )
                    
                    // Refresh the accounts list
                    loadConnectedAccounts()
                } else {
                    // Failed to disconnect account
                    _uiState = _uiState.copy(
                        isDeletingAccount = null,
                        disconnectSuccess = false,
                        disconnectError = "Failed to disconnect account"
                    )
                }
            } catch (e: Exception) {
                // Handle any exceptions
                _uiState = _uiState.copy(
                    isDeletingAccount = null,
                    disconnectSuccess = false,
                    disconnectError = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }
    
    fun refreshAccount(accountId: String) {
        _uiState = _uiState.copy(isRefreshingAccount = accountId)
        
        coroutineScope.launch {
            try {
                val result = plaidService.refreshAccountConnection(accountId)
                
                if (result.success) {
                    // Successfully refreshed account
                    _uiState = _uiState.copy(
                        isRefreshingAccount = null,
                        refreshSuccess = true,
                        refreshError = null
                    )
                    
                    // Refresh the accounts list
                    loadConnectedAccounts()
                } else {
                    // Failed to refresh account
                    _uiState = _uiState.copy(
                        isRefreshingAccount = null,
                        refreshSuccess = false,
                        refreshError = result.error ?: "Failed to refresh account"
                    )
                }
            } catch (e: Exception) {
                // Handle any exceptions
                _uiState = _uiState.copy(
                    isRefreshingAccount = null,
                    refreshSuccess = false,
                    refreshError = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }
    
    fun loadConnectedAccounts() {
        _uiState = _uiState.copy(isLoadingAccounts = true)
        
        coroutineScope.launch {
            try {
                val accounts = plaidService.getAccounts("current_user")
                _uiState = _uiState.copy(
                    connectedAccounts = accounts,
                    isLoadingAccounts = false,
                    loadError = null
                )
            } catch (e: Exception) {
                _uiState = _uiState.copy(
                    isLoadingAccounts = false,
                    loadError = e.message ?: "Failed to load accounts"
                )
            }
        }
    }
    
    fun resetConnectionState() {
        _uiState = _uiState.copy(
            connectionSuccess = false,
            connectionError = null,
            disconnectSuccess = false,
            disconnectError = null,
            refreshSuccess = false,
            refreshError = null
        )
    }
    
    fun clearError() {
        _uiState = _uiState.copy(
            connectionError = null,
            disconnectError = null,
            refreshError = null,
            loadError = null
        )
    }
}

data class AccountConnectionState(
    val isConnecting: Boolean = false,
    val connectionStep: ConnectionStep = ConnectionStep.NOT_STARTED,
    val connectionSuccess: Boolean = false,
    val connectionError: String? = null,
    
    val isDeletingAccount: String? = null,
    val disconnectSuccess: Boolean = false,
    val disconnectError: String? = null,
    
    val isRefreshingAccount: String? = null,
    val refreshSuccess: Boolean = false,
    val refreshError: String? = null,
    
    val isLoadingAccounts: Boolean = false,
    val connectedAccounts: List<SimplePlaidAccount> = emptyList(),
    val loadError: String? = null
)

enum class ConnectionStep {
    NOT_STARTED,
    INITIALIZING,
    SELECTING_INSTITUTION,
    AUTHENTICATING,
    EXCHANGING_TOKEN,
    COMPLETED,
    ERROR
}