package com.north.mobile.data.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Implementation of SessionManager with basic persistence
 * Uses in-memory storage with simple persistence simulation
 */
class SessionManagerImpl : SessionManager {
    
    private val _sessionState = MutableStateFlow(SessionState())
    
    // In-memory storage with persistence simulation
    private var storedToken: String? = null
    private var storedUser: com.north.mobile.data.api.UserResponse? = null
    private var tokenExpiresAt: Long? = null
    
    // Simple persistence using static storage (survives app lifecycle)
    companion object {
        private var persistentToken: String? = null
        private var persistentUser: com.north.mobile.data.api.UserResponse? = null
        private var persistentExpiresAt: Long? = null
    }
    
    override suspend fun saveAuthToken(token: String) {
        storedToken = token
        persistentToken = token
        // Set token to expire in 24 hours (86400000 ms)
        tokenExpiresAt = System.currentTimeMillis() + 86400000L
        persistentExpiresAt = tokenExpiresAt
        println("💾 Saved auth token: ${token.take(20)}... (expires at: $tokenExpiresAt)")
        updateSessionState()
    }
    
    override suspend fun getAuthToken(): String? {
        return if (isTokenValid()) storedToken else null
    }
    
    override suspend fun saveUser(user: com.north.mobile.data.api.UserResponse) {
        storedUser = user
        persistentUser = user
        println("👤 Saved user: ${user.email}")
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
        persistentToken = null
        persistentUser = null
        persistentExpiresAt = null
        println("🗑️ Cleared session")
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
        // Load from persistent storage on initialization
        storedToken = persistentToken
        storedUser = persistentUser
        tokenExpiresAt = persistentExpiresAt
        
        println("🔄 SessionManager initialized")
        println("📱 Loaded token: ${if (storedToken != null) "✅ Found (${storedToken!!.take(20)}...)" else "❌ None"}")
        println("👤 Loaded user: ${if (storedUser != null) "✅ Found (${storedUser!!.email})" else "❌ None"}")
        println("⏰ Token expires: ${if (tokenExpiresAt != null) "✅ At $tokenExpiresAt" else "❌ Never"}")
        
        // Initialize session state
        updateSessionState()
    }
}