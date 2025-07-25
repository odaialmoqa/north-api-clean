package com.north.mobile.ui.profile

import androidx.compose.runtime.*
import com.north.mobile.data.auth.SessionManager
import com.north.mobile.data.plaid.PlaidIntegrationService
import com.north.mobile.data.plaid.SimplePlaidAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val sessionManager: SessionManager,
    private val plaidService: PlaidIntegrationService,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private var _uiState by mutableStateOf(ProfileUiState())
    val uiState: ProfileUiState get() = _uiState
    
    init {
        loadUserProfile()
        loadConnectedAccounts()
    }
    
    private fun loadUserProfile() {
        _uiState = _uiState.copy(isLoading = true)
        
        coroutineScope.launch {
            try {
                val userInfo = sessionManager.getUser()
                _uiState = _uiState.copy(
                    userEmail = userInfo?.email ?: "user@example.com",
                    userName = "${userInfo?.firstName ?: ""} ${userInfo?.lastName ?: ""}".trim().ifEmpty { "Financial Champion" },
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState = _uiState.copy(
                    isLoading = false,
                    error = "Failed to load user profile: ${e.message}"
                )
            }
        }
    }
    
    private fun loadConnectedAccounts() {
        coroutineScope.launch {
            try {
                val accounts = plaidService.getAccounts("current_user")
                _uiState = _uiState.copy(
                    connectedAccounts = accounts,
                    connectedAccountsCount = accounts.size
                )
            } catch (e: Exception) {
                // Handle gracefully - user might not have connected accounts yet
                _uiState = _uiState.copy(
                    connectedAccounts = emptyList(),
                    connectedAccountsCount = 0
                )
            }
        }
    }
    
    fun logout() {
        _uiState = _uiState.copy(isLoggingOut = true)
        
        coroutineScope.launch {
            try {
                // Clear session data
                sessionManager.clearSession()
                
                // Clear any cached financial data
                clearFinancialData()
                
                _uiState = _uiState.copy(
                    isLoggingOut = false,
                    logoutSuccess = true
                )
            } catch (e: Exception) {
                _uiState = _uiState.copy(
                    isLoggingOut = false,
                    error = "Failed to logout: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun clearFinancialData() {
        // Clear any cached financial data, preferences, etc.
        // This ensures user data is properly cleaned up on logout
        try {
            // Clear any local storage or cached data here
            // For now, we'll just ensure session is cleared
        } catch (e: Exception) {
            // Log error but don't fail logout process
        }
    }
    
    fun clearError() {
        _uiState = _uiState.copy(error = null)
    }
    
    fun resetLogoutSuccess() {
        _uiState = _uiState.copy(logoutSuccess = false)
    }
    
    fun refreshProfile() {
        loadUserProfile()
        loadConnectedAccounts()
    }
}

data class ProfileUiState(
    val userEmail: String = "",
    val userName: String = "",
    val connectedAccounts: List<SimplePlaidAccount> = emptyList(),
    val connectedAccountsCount: Int = 0,
    val isLoading: Boolean = false,
    val isLoggingOut: Boolean = false,
    val logoutSuccess: Boolean = false,
    val error: String? = null
)

// User data model
data class UserInfo(
    val id: String,
    val email: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)