package com.north.mobile.data.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Interface for managing user sessions and JWT tokens
 */
interface SessionManager {
    /**
     * Store authentication tokens after successful login
     */
    suspend fun storeTokens(accessToken: String, refreshToken: String, expiresAt: Instant): Result<Unit>
    
    /**
     * Get the current access token
     */
    suspend fun getAccessToken(): Result<String?>
    
    /**
     * Get the current refresh token
     */
    suspend fun getRefreshToken(): Result<String?>
    
    /**
     * Check if the current access token is valid (not expired)
     */
    suspend fun isTokenValid(): Boolean
    
    /**
     * Refresh the access token using the refresh token
     */
    suspend fun refreshToken(): Result<TokenRefreshResult>
    
    /**
     * Clear all stored tokens (for logout)
     */
    suspend fun clearTokens(): Result<Unit>
    
    /**
     * Get the current session state
     */
    fun getSessionState(): Flow<SessionState>
    
    /**
     * Check if user has a valid session
     */
    suspend fun hasValidSession(): Boolean
    
    /**
     * Get token expiration time
     */
    suspend fun getTokenExpirationTime(): Instant?
}

/**
 * Result of token refresh operation
 */
sealed class TokenRefreshResult {
    data class Success(val accessToken: String, val expiresAt: Instant) : TokenRefreshResult()
    object RefreshTokenExpired : TokenRefreshResult()
    data class Error(val message: String) : TokenRefreshResult()
}

/**
 * Current session state
 */
data class SessionState(
    val hasValidSession: Boolean = false,
    val accessToken: String? = null,
    val tokenExpiresAt: Instant? = null,
    val isRefreshing: Boolean = false
)

/**
 * JWT token data
 */
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
    val tokenType: String = "Bearer"
)

/**
 * Exception thrown when session operations fail
 */
class SessionException(message: String, cause: Throwable? = null) : Exception(message, cause)