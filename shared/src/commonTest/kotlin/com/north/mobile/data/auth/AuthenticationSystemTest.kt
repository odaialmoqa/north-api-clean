package com.north.mobile.data.auth

import kotlin.test.*

/**
 * Simple test to verify authentication system interfaces and basic functionality
 */
class AuthenticationSystemTest {
    
    @Test
    fun `AuthResult sealed class should have correct types`() {
        val success = AuthResult.Success
        val error = AuthResult.Error("Test error", AuthErrorType.UNKNOWN_ERROR)
        val cancelled = AuthResult.Cancelled
        val biometricNotAvailable = AuthResult.BiometricNotAvailable
        val biometricNotEnrolled = AuthResult.BiometricNotEnrolled
        val pinNotSetup = AuthResult.PINNotSetup
        
        assertTrue(success is AuthResult.Success)
        assertTrue(error is AuthResult.Error)
        assertTrue(cancelled is AuthResult.Cancelled)
        assertTrue(biometricNotAvailable is AuthResult.BiometricNotAvailable)
        assertTrue(biometricNotEnrolled is AuthResult.BiometricNotEnrolled)
        assertTrue(pinNotSetup is AuthResult.PINNotSetup)
        
        assertEquals("Test error", error.message)
        assertEquals(AuthErrorType.UNKNOWN_ERROR, error.errorType)
    }
    
    @Test
    fun `AuthErrorType enum should have all required types`() {
        val errorTypes = AuthErrorType.values()
        
        assertTrue(errorTypes.contains(AuthErrorType.BIOMETRIC_HARDWARE_NOT_AVAILABLE))
        assertTrue(errorTypes.contains(AuthErrorType.BIOMETRIC_NOT_ENROLLED))
        assertTrue(errorTypes.contains(AuthErrorType.BIOMETRIC_AUTHENTICATION_FAILED))
        assertTrue(errorTypes.contains(AuthErrorType.PIN_AUTHENTICATION_FAILED))
        assertTrue(errorTypes.contains(AuthErrorType.PIN_SETUP_FAILED))
        assertTrue(errorTypes.contains(AuthErrorType.AUTHENTICATION_CANCELLED))
        assertTrue(errorTypes.contains(AuthErrorType.UNKNOWN_ERROR))
    }
    
    @Test
    fun `AuthenticationState should have correct default values`() {
        val state = AuthenticationState()
        
        assertFalse(state.isAuthenticated)
        assertFalse(state.isBiometricAvailable)
        assertFalse(state.isBiometricEnrolled)
        assertFalse(state.isPINSetup)
        assertNull(state.lastAuthenticationTime)
    }
    
    @Test
    fun `AuthenticationState should allow updates`() {
        val state = AuthenticationState(
            isAuthenticated = true,
            isBiometricAvailable = true,
            isBiometricEnrolled = true,
            isPINSetup = true,
            lastAuthenticationTime = 123456789L
        )
        
        assertTrue(state.isAuthenticated)
        assertTrue(state.isBiometricAvailable)
        assertTrue(state.isBiometricEnrolled)
        assertTrue(state.isPINSetup)
        assertEquals(123456789L, state.lastAuthenticationTime)
    }
    
    @Test
    fun `TokenRefreshResult sealed class should have correct types`() {
        val success = TokenRefreshResult.Success("new_token", kotlinx.datetime.Clock.System.now())
        val expired = TokenRefreshResult.RefreshTokenExpired
        val error = TokenRefreshResult.Error("Test error")
        
        assertTrue(success is TokenRefreshResult.Success)
        assertTrue(expired is TokenRefreshResult.RefreshTokenExpired)
        assertTrue(error is TokenRefreshResult.Error)
        
        assertEquals("new_token", success.accessToken)
        assertEquals("Test error", error.message)
    }
    
    @Test
    fun `SessionState should have correct default values`() {
        val state = SessionState()
        
        assertFalse(state.hasValidSession)
        assertNull(state.accessToken)
        assertNull(state.tokenExpiresAt)
        assertFalse(state.isRefreshing)
    }
    
    @Test
    fun `SessionState should allow updates`() {
        val expiresAt = kotlinx.datetime.Clock.System.now()
        val state = SessionState(
            hasValidSession = true,
            accessToken = "test_token",
            tokenExpiresAt = expiresAt,
            isRefreshing = true
        )
        
        assertTrue(state.hasValidSession)
        assertEquals("test_token", state.accessToken)
        assertEquals(expiresAt, state.tokenExpiresAt)
        assertTrue(state.isRefreshing)
    }
    
    @Test
    fun `AuthTokens should be created correctly`() {
        val expiresAt = kotlinx.datetime.Clock.System.now()
        val tokens = AuthTokens(
            accessToken = "access_123",
            refreshToken = "refresh_456",
            expiresAt = expiresAt,
            tokenType = "Bearer"
        )
        
        assertEquals("access_123", tokens.accessToken)
        assertEquals("refresh_456", tokens.refreshToken)
        assertEquals(expiresAt, tokens.expiresAt)
        assertEquals("Bearer", tokens.tokenType)
    }
    
    @Test
    fun `AuthTokens should have default token type`() {
        val expiresAt = kotlinx.datetime.Clock.System.now()
        val tokens = AuthTokens(
            accessToken = "access_123",
            refreshToken = "refresh_456",
            expiresAt = expiresAt
        )
        
        assertEquals("Bearer", tokens.tokenType)
    }
    
    @Test
    fun `AuthenticationException should be throwable`() {
        val exception = AuthenticationException("Test message")
        assertEquals("Test message", exception.message)
        assertNull(exception.cause)
        
        val cause = RuntimeException("Cause")
        val exceptionWithCause = AuthenticationException("Test message", cause)
        assertEquals("Test message", exceptionWithCause.message)
        assertEquals(cause, exceptionWithCause.cause)
    }
    
    @Test
    fun `SessionException should be throwable`() {
        val exception = SessionException("Test message")
        assertEquals("Test message", exception.message)
        assertNull(exception.cause)
        
        val cause = RuntimeException("Cause")
        val exceptionWithCause = SessionException("Test message", cause)
        assertEquals("Test message", exceptionWithCause.message)
        assertEquals(cause, exceptionWithCause.cause)
    }
}