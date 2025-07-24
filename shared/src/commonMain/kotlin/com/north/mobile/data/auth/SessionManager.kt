package com.north.mobile.data.auth

import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing user authentication sessions
 */
interface SessionManager {
    /**
     * Save authentication token securely
     */
    suspend fun saveAuthToken(token: String)
    
    /**
     * Get stored authentication token
     */
    suspend fun getAuthToken(): String?
    
    /**
     * Save user information
     */
    suspend fun saveUser(user: com.north.mobile.data.api.UserResponse)
    
    /**
     * Get stored user information
     */
    suspend fun getUser(): com.north.mobile.data.api.UserResponse?
    
    /**
     * Check if current session is valid
     */
    suspend fun isSessionValid(): Boolean
    
    /**
     * Clear all session data (logout)
     */
    suspend fun clearSession()
    
    /**
     * Get session state as a flow
     */
    fun getSessionState(): Flow<SessionState>
}

/**
 * Data class representing the current session state
 */
data class SessionState(
    val isAuthenticated: Boolean = false,
    val token: String? = null,
    val user: com.north.mobile.data.api.UserResponse? = null,
    val expiresAt: Long? = null
)