package com.north.mobile.integration

import com.north.mobile.data.api.AuthApiService
import com.north.mobile.data.api.AuthResponse
import com.north.mobile.data.api.UserResponse
import com.north.mobile.data.auth.SessionManagerImpl
import com.north.mobile.data.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthenticationFlowTest {
    
    @Test
    fun `complete authentication flow should work end-to-end`() = runTest {
        // Given
        val mockAuthApiService = MockAuthApiService()
        val sessionManager = SessionManagerImpl()
        val authRepository = AuthRepository(mockAuthApiService, sessionManager)
        
        // Initially not authenticated
        assertFalse(authRepository.isUserAuthenticated())
        
        // When - Register user
        val registerResult = authRepository.register(
            email = "test@example.com",
            password = "password123",
            firstName = "John",
            lastName = "Doe"
        )
        
        // Then - Registration should succeed and user should be authenticated
        assertTrue(registerResult.isSuccess)
        assertTrue(authRepository.isUserAuthenticated())
        
        val user = registerResult.getOrNull()
        assertNotNull(user)
        assertEquals("test@example.com", user.email)
        assertEquals("John", user.firstName)
        assertEquals("Doe", user.lastName)
        
        // Session should be valid
        assertTrue(sessionManager.isSessionValid())
        assertNotNull(sessionManager.getAuthToken())
        assertNotNull(sessionManager.getUser())
        
        // When - Logout
        authRepository.logout()
        
        // Then - Should be logged out
        assertFalse(authRepository.isUserAuthenticated())
        assertFalse(sessionManager.isSessionValid())
    }
    
    @Test
    fun `session initialization should restore authentication state`() = runTest {
        // Given
        val mockAuthApiService = MockAuthApiService()
        val sessionManager = SessionManagerImpl()
        
        // Simulate existing session data
        sessionManager.saveAuthToken("existing-token")
        sessionManager.saveUser(UserResponse(
            id = "user-123",
            email = "existing@example.com",
            firstName = "Jane",
            lastName = "Smith"
        ))
        
        // When
        val authRepository = AuthRepository(mockAuthApiService, sessionManager)
        authRepository.initializeSession()
        
        // Then
        assertTrue(authRepository.isUserAuthenticated())
        
        val currentUser = authRepository.currentUser.value
        assertNotNull(currentUser)
        assertEquals("existing@example.com", currentUser.email)
        assertEquals("Jane", currentUser.firstName)
    }
    
    @Test
    fun `login flow should work with valid credentials`() = runTest {
        // Given
        val mockAuthApiService = MockAuthApiService()
        val sessionManager = SessionManagerImpl()
        val authRepository = AuthRepository(mockAuthApiService, sessionManager)
        
        // When
        val loginResult = authRepository.login(
            email = "test@example.com",
            password = "password123"
        )
        
        // Then
        assertTrue(loginResult.isSuccess)
        assertTrue(authRepository.isUserAuthenticated())
        assertTrue(sessionManager.isSessionValid())
    }
}

// Mock AuthApiService for testing
private class MockAuthApiService : AuthApiService(mockApiClient = null as Any?) {
    
    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<AuthResponse> {
        return Result.success(
            AuthResponse(
                message = "Registration successful",
                user = UserResponse(
                    id = "user-123",
                    email = email,
                    firstName = firstName,
                    lastName = lastName
                ),
                token = "mock-token-123"
            )
        )
    }
    
    override suspend fun login(email: String, password: String): Result<AuthResponse> {
        return Result.success(
            AuthResponse(
                message = "Login successful",
                user = UserResponse(
                    id = "user-123",
                    email = email,
                    firstName = "Test",
                    lastName = "User"
                ),
                token = "mock-token-123"
            )
        )
    }
}