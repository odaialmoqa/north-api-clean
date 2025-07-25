package com.north.mobile.ui.profile

import com.north.mobile.data.auth.SessionManager
import com.north.mobile.data.auth.SessionState
import com.north.mobile.data.api.UserResponse
import com.north.mobile.data.plaid.PlaidIntegrationService
import com.north.mobile.data.plaid.SimplePlaidAccount
import com.north.mobile.data.plaid.PlaidConnectionStatus
import com.north.mobile.data.plaid.SimpleTransactionSyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileManagementTest {
    
    // Mock SessionManager for testing
    private class MockSessionManager : SessionManager {
        private var authToken: String? = null
        private var user: UserResponse? = null
        private var sessionValid = false
        private val sessionStateFlow = MutableStateFlow(SessionState())
        
        override suspend fun saveAuthToken(token: String) {
            authToken = token
            updateSessionState()
        }
        
        override suspend fun getAuthToken(): String? = authToken
        
        override suspend fun saveUser(user: UserResponse) {
            this.user = user
            updateSessionState()
        }
        
        override suspend fun getUser(): UserResponse? = user
        
        override suspend fun isSessionValid(): Boolean = sessionValid && authToken != null && user != null
        
        override suspend fun clearSession() {
            authToken = null
            user = null
            sessionValid = false
            updateSessionState()
        }
        
        override fun getSessionState(): Flow<SessionState> = sessionStateFlow
        
        private fun updateSessionState() {
            sessionValid = authToken != null && user != null
            sessionStateFlow.value = SessionState(
                isAuthenticated = sessionValid,
                token = authToken,
                user = user,
                expiresAt = if (authToken != null) System.currentTimeMillis() + 86400000L else null
            )
        }
        
        // Helper method for testing
        fun setInitialSession(token: String, userResponse: UserResponse) {
            authToken = token
            user = userResponse
            sessionValid = true
            updateSessionState()
        }
    }
    
    // Mock PlaidIntegrationService for testing
    private class MockPlaidService : PlaidIntegrationService {
        private val mockAccounts = listOf(
            SimplePlaidAccount(
                id = "account_1",
                name = "Test Checking",
                type = "checking",
                subtype = "checking",
                balance = 1500.0,
                institutionName = "Test Bank",
                lastSyncTime = System.currentTimeMillis(),
                connectionStatus = PlaidConnectionStatus.HEALTHY
            ),
            SimplePlaidAccount(
                id = "account_2",
                name = "Test Savings",
                type = "savings",
                subtype = "savings",
                balance = 5000.0,
                institutionName = "Test Bank",
                lastSyncTime = System.currentTimeMillis(),
                connectionStatus = PlaidConnectionStatus.HEALTHY
            )
        )
        
        override suspend fun initializePlaidLink() = throw NotImplementedError()
        override suspend fun exchangePublicToken(publicToken: String) = throw NotImplementedError()
        override suspend fun getAccounts(userId: String) = mockAccounts
        override suspend fun syncTransactions(accountId: String) = SimpleTransactionSyncResult(true, emptyList(), null)
        override suspend fun disconnectAccount(accountId: String) = true
        override suspend fun refreshAccountConnection(accountId: String) = throw NotImplementedError()
    }
    
    @Test
    fun testProfileViewModel_initialization() = runTest {
        val mockSessionManager = MockSessionManager()
        val mockPlaidService = MockPlaidService()
        
        // Set up initial session
        mockSessionManager.setInitialSession(
            "test_token",
            UserResponse(
                id = "user_123",
                email = "test@example.com",
                name = "Test User"
            )
        )
        
        val viewModel = ProfileViewModel(mockSessionManager, mockPlaidService)
        
        // Wait for initialization
        kotlinx.coroutines.delay(100)
        
        // Verify initial state
        assertEquals("test@example.com", viewModel.uiState.userEmail)
        assertEquals("Test User", viewModel.uiState.userName)
        assertEquals(2, viewModel.uiState.connectedAccountsCount)
        assertFalse(viewModel.uiState.isLoading)
    }
    
    @Test
    fun testProfileViewModel_logout() = runTest {
        val mockSessionManager = MockSessionManager()
        val mockPlaidService = MockPlaidService()
        
        // Set up initial session
        mockSessionManager.setInitialSession(
            "test_token",
            UserResponse(
                id = "user_123",
                email = "test@example.com",
                name = "Test User"
            )
        )
        
        val viewModel = ProfileViewModel(mockSessionManager, mockPlaidService)
        
        // Verify session is initially valid
        assertTrue(mockSessionManager.isSessionValid())
        
        // Perform logout
        viewModel.logout()
        
        // Wait for logout to complete
        kotlinx.coroutines.delay(100)
        
        // Verify session is cleared
        assertFalse(mockSessionManager.isSessionValid())
        assertTrue(viewModel.uiState.logoutSuccess)
        assertFalse(viewModel.uiState.isLoggingOut)
    }
    
    @Test
    fun testProfileViewModel_refreshProfile() = runTest {
        val mockSessionManager = MockSessionManager()
        val mockPlaidService = MockPlaidService()
        
        // Set up initial session
        mockSessionManager.setInitialSession(
            "test_token",
            UserResponse(
                id = "user_123",
                email = "test@example.com",
                name = "Test User"
            )
        )
        
        val viewModel = ProfileViewModel(mockSessionManager, mockPlaidService)
        
        // Wait for initial load
        kotlinx.coroutines.delay(100)
        
        // Verify initial state
        assertEquals(2, viewModel.uiState.connectedAccountsCount)
        
        // Refresh profile
        viewModel.refreshProfile()
        
        // Wait for refresh to complete
        kotlinx.coroutines.delay(100)
        
        // Verify data is still correct after refresh
        assertEquals("test@example.com", viewModel.uiState.userEmail)
        assertEquals("Test User", viewModel.uiState.userName)
        assertEquals(2, viewModel.uiState.connectedAccountsCount)
    }
    
    @Test
    fun testProfileViewModel_errorHandling() = runTest {
        val mockSessionManager = MockSessionManager()
        val mockPlaidService = object : PlaidIntegrationService {
            override suspend fun initializePlaidLink() = throw NotImplementedError()
            override suspend fun exchangePublicToken(publicToken: String) = throw NotImplementedError()
            override suspend fun getAccounts(userId: String): List<SimplePlaidAccount> {
                throw Exception("Network error")
            }
            override suspend fun syncTransactions(accountId: String) = throw NotImplementedError()
            override suspend fun disconnectAccount(accountId: String) = true
            override suspend fun refreshAccountConnection(accountId: String) = throw NotImplementedError()
        }
        
        // Set up initial session
        mockSessionManager.setInitialSession(
            "test_token",
            UserResponse(
                id = "user_123",
                email = "test@example.com",
                name = "Test User"
            )
        )
        
        val viewModel = ProfileViewModel(mockSessionManager, mockPlaidService)
        
        // Wait for initialization
        kotlinx.coroutines.delay(100)
        
        // Verify that error is handled gracefully
        assertEquals(0, viewModel.uiState.connectedAccountsCount) // Should default to 0 on error
        assertEquals("test@example.com", viewModel.uiState.userEmail) // User data should still load
    }
    
    @Test
    fun testProfileUiState_defaultValues() {
        val uiState = ProfileUiState()
        
        assertEquals("", uiState.userEmail)
        assertEquals("", uiState.userName)
        assertEquals(0, uiState.connectedAccountsCount)
        assertTrue(uiState.connectedAccounts.isEmpty())
        assertFalse(uiState.isLoading)
        assertFalse(uiState.isLoggingOut)
        assertFalse(uiState.logoutSuccess)
        assertEquals(null, uiState.error)
    }
    
    @Test
    fun testUserInfo_dataClass() {
        val userInfo = UserInfo(
            id = "user_123",
            email = "test@example.com",
            name = "Test User",
            createdAt = 1234567890L,
            lastLoginAt = 1234567891L
        )
        
        assertEquals("user_123", userInfo.id)
        assertEquals("test@example.com", userInfo.email)
        assertEquals("Test User", userInfo.name)
        assertEquals(1234567890L, userInfo.createdAt)
        assertEquals(1234567891L, userInfo.lastLoginAt)
    }
    
    @Test
    fun testSessionManager_clearSession() = runTest {
        val mockSessionManager = MockSessionManager()
        
        // Set up session
        mockSessionManager.setInitialSession(
            "test_token",
            UserResponse(
                id = "user_123",
                email = "test@example.com",
                name = "Test User"
            )
        )
        
        // Verify session is valid
        assertTrue(mockSessionManager.isSessionValid())
        assertEquals("test_token", mockSessionManager.getAuthToken())
        assertEquals("test@example.com", mockSessionManager.getUser()?.email)
        
        // Clear session
        mockSessionManager.clearSession()
        
        // Verify session is cleared
        assertFalse(mockSessionManager.isSessionValid())
        assertEquals(null, mockSessionManager.getAuthToken())
        assertEquals(null, mockSessionManager.getUser())
    }
}

// Mock UserResponse for testing
data class UserResponse(
    val id: String,
    val email: String,
    val name: String
)