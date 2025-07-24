package com.north.mobile.data.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Implementation of SessionManager using simple in-memory storage
 * TODO: Replace with secure platform-specific storage in production
 */
class SessionManagerImpl : SessionManager {
    
    private val _sessionState = MutableStateFlow(SessionState())
    
    // Simple in-memory storage - replace with secure storage
    private var storedToken: String? = null
    private var storedUser: com.north.mobile.data.api.UserResponse? = null
    private var tokenExpiresAt: Long? = null
    
    override suspend fun saveAuthToken(token: String) {
        storedToken = token
        // Set token to expire in 24 hours (86400000 ms)
        tokenExpiresAt = System.currentTimeMillis() + 86400000L
        updateSessionState()
    }
    
    override suspend fun getAuthToken(): String? {
        return if (isTokenValid()) storedToken else null
    }
    
    override suspend fun saveUser(user: com.north.mobile.data.api.UserResponse) {
        storedUser = user
        updateSessionState()
    }
    
    override suspend fun getUser(): com.north.mobile.data.api.UserResponse? {
        return if (isSessionValid()) storedUser else null
    }
    
    override suspend fun isSessionValid(): Boolean {
        return isTokenValid() && storedUser != null
    }
    
    override suspend fun clearSession() {
        storedToken = null
        storedUser = null
        tokenExpiresAt = null
        updateSessionState()
    }
    
    override fun getSessionState(): Flow<SessionState> {
        return _sessionState.asStateFlow()
    }
    
    private fun isTokenValid(): Boolean {
        val expiresAt = tokenExpiresAt ?: return false
        return System.currentTimeMillis() < expiresAt && storedToken != null
    }
    
    private fun updateSessionState() {
        val sessionValid = isTokenValid() && storedUser != null
        _sessionState.value = SessionState(
            isAuthenticated = sessionValid,
            token = if (isTokenValid()) storedToken else null,
            user = if (sessionValid) storedUser else null,
            expiresAt = tokenExpiresAt
        )
    }
    
    init {
        // Initialize session state
        updateSessionState()
    }
}