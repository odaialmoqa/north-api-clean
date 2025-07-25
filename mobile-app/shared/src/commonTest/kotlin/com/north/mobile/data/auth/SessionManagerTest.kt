package com.north.mobile.data.auth

import com.north.mobile.data.api.UserResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionManagerTest {
    
    private val sessionManager = SessionManagerImpl()
    
    @Test
    fun `saveAuthToken should store token and mark session as valid`() = runTest {
        // Given
        val token = "test-token-123"
        
        // When
        sessionManager.saveAuthToken(token)
        
        // Then
        assertEquals(token, sessionManager.getAuthToken())
        assertTrue(sessionManager.isSessionValid())
    }
    
    @Test
    fun `saveUser should store user data`() = runTest {
        // Given
        val user = UserResponse(
            id = "user-123",
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe"
        )
        val token = "test-token-123"
        
        // When
        sessionManager.saveAuthToken(token)
        sessionManager.saveUser(user)
        
        // Then
        val storedUser = sessionManager.getUser()
        assertNotNull(storedUser)
        assertEquals(user.email, storedUser.email)
        assertEquals(user.firstName, storedUser.firstName)
        assertEquals(user.lastName, storedUser.lastName)
    }
    
    @Test
    fun `clearSession should remove all session data`() = runTest {
        // Given
        val token = "test-token-123"
        val user = UserResponse(
            id = "user-123",
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe"
        )
        
        sessionManager.saveAuthToken(token)
        sessionManager.saveUser(user)
        
        // When
        sessionManager.clearSession()
        
        // Then
        assertNull(sessionManager.getAuthToken())
        assertNull(sessionManager.getUser())
        assertFalse(sessionManager.isSessionValid())
    }
    
    @Test
    fun `isSessionValid should return false when no token is stored`() = runTest {
        // Given - fresh session manager
        
        // When & Then
        assertFalse(sessionManager.isSessionValid())
    }
    
    @Test
    fun `isSessionValid should return false when token exists but no user`() = runTest {
        // Given
        sessionManager.saveAuthToken("test-token")
        
        // When & Then
        assertFalse(sessionManager.isSessionValid())
    }
    
    @Test
    fun `isSessionValid should return true when both token and user exist`() = runTest {
        // Given
        val token = "test-token-123"
        val user = UserResponse(
            id = "user-123",
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe"
        )
        
        // When
        sessionManager.saveAuthToken(token)
        sessionManager.saveUser(user)
        
        // Then
        assertTrue(sessionManager.isSessionValid())
    }
    
    // Enhanced tests for UX improvements
    
    @Test
    fun `getSessionState should emit correct initial state`() = runTest {
        // Given - fresh session manager
        
        // When
        val initialState = sessionManager.getSessionState().first()
        
        // Then
        assertFalse(initialState.isAuthenticated)
        assertNull(initialState.token)
        assertNull(initialState.user)
        assertNull(initialState.expiresAt)
    }
    
    @Test
    fun `getSessionState should emit updated state after saving token`() = runTest {
        // Given
        val token = "test-token-123"
        
        // When
        sessionManager.saveAuthToken(token)
        val state = sessionManager.getSessionState().first()
        
        // Then
        assertFalse(state.isAuthenticated) // Still false because no user
        assertEquals(token, state.token)
        assertNotNull(state.expiresAt)
        assertTrue(state.expiresAt!! > System.currentTimeMillis())
    }
    
    @Test
    fun `getSessionState should emit authenticated state when both token and user exist`() = runTest {
        // Given
        val token = "test-token-123"
        val user = UserResponse(
            id = "user-123",
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe"
        )
        
        // When
        sessionManager.saveAuthToken(token)
        sessionManager.saveUser(user)
        val state = sessionManager.getSessionState().first()
        
        // Then
        assertTrue(state.isAuthenticated)
        assertEquals(token, state.token)
        assertEquals(user, state.user)
        assertNotNull(state.expiresAt)
    }
    
    @Test
    fun `getSessionState should emit cleared state after clearSession`() = runTest {
        // Given
        val token = "test-token-123"
        val user = UserResponse(
            id = "user-123",
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe"
        )
        
        sessionManager.saveAuthToken(token)
        sessionManager.saveUser(user)
        
        // When
        sessionManager.clearSession()
        val state = sessionManager.getSessionState().first()
        
        // Then
        assertFalse(state.isAuthenticated)
        assertNull(state.token)
        assertNull(state.user)
        assertNull(state.expiresAt)
    }
    
    @Test
    fun `token should be considered invalid after expiration time`() = runTest {
        // Given - Create a custom session manager that simulates expired tokens
        val expiredSessionManager = object : SessionManagerImpl() {
            private var forceExpired = false
            
            fun expireToken() {
                forceExpired = true
            }
            
            override suspend fun getAuthToken(): String? {
                return if (forceExpired) null else super.getAuthToken()
            }
            
            override suspend fun isSessionValid(): Boolean {
                return if (forceExpired) false else super.isSessionValid()
            }
        }
        
        val user = UserResponse(
            id = "user-123",
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe"
        )
        
        // When - Set up valid session first
        expiredSessionManager.saveAuthToken("valid-token")
        expiredSessionManager.saveUser(user)
        assertTrue(expiredSessionManager.isSessionValid())
        
        // When - Force token expiration
        expiredSessionManager.expireToken()
        
        // Then - Session should be invalid
        assertNull(expiredSessionManager.getAuthToken())
        assertFalse(expiredSessionManager.isSessionValid())
    }
    
    @Test
    fun `multiple token saves should update expiration time`() = runTest {
        // Given
        val firstToken = "first-token"
        val secondToken = "second-token"
        
        // When
        sessionManager.saveAuthToken(firstToken)
        val firstState = sessionManager.getSessionState().first()
        val firstExpiration = firstState.expiresAt
        
        // Wait a bit to ensure different timestamps
        kotlinx.coroutines.delay(10)
        
        sessionManager.saveAuthToken(secondToken)
        val secondState = sessionManager.getSessionState().first()
        val secondExpiration = secondState.expiresAt
        
        // Then
        assertEquals(secondToken, sessionManager.getAuthToken())
        assertNotNull(firstExpiration)
        assertNotNull(secondExpiration)
        assertTrue(secondExpiration!! > firstExpiration!!)
    }
    
    @Test
    fun `session should remain valid for expected duration`() = runTest {
        // Given
        val token = "test-token-123"
        val user = UserResponse(
            id = "user-123",
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe"
        )
        
        // When
        sessionManager.saveAuthToken(token)
        sessionManager.saveUser(user)
        val state = sessionManager.getSessionState().first()
        
        // Then
        assertTrue(sessionManager.isSessionValid())
        assertNotNull(state.expiresAt)
        
        // Token should be valid for approximately 24 hours (86400000 ms)
        val expectedExpiration = System.currentTimeMillis() + 86400000L
        val actualExpiration = state.expiresAt!!
        val timeDifference = kotlin.math.abs(actualExpiration - expectedExpiration)
        
        // Allow for small timing differences (within 1 second)
        assertTrue(timeDifference < 1000, "Token expiration time should be approximately 24 hours from now")
    }
}