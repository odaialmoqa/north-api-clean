package com.north.mobile.data.auth

import com.north.mobile.data.security.EncryptionManager
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementation of SessionManager that handles JWT tokens and session state
 */
class SessionManagerImpl(
    private val settings: Settings,
    private val encryptionManager: EncryptionManager
) : SessionManager {
    
    private val _sessionState = MutableStateFlow(SessionState())
    private val sessionState = _sessionState.asStateFlow()
    
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun storeTokens(accessToken: String, refreshToken: String, expiresAt: Instant): Result<Unit> {
        return try {
            val tokens = AuthTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAt = expiresAt
            )
            
            val tokensJson = json.encodeToString(tokens)
            val encryptResult = encryptionManager.encrypt(tokensJson, TOKEN_KEY_ALIAS)
            
            if (encryptResult.isSuccess) {
                val encryptedData = encryptResult.getOrThrow()
                
                // Store encrypted token data
                settings.putString(TOKEN_ENCRYPTED_CONTENT_KEY, encryptedData.encryptedContent.joinToString(",") { it.toString() })
                settings.putString(TOKEN_IV_KEY, encryptedData.iv.joinToString(",") { it.toString() })
                settings.putString(TOKEN_KEY_ALIAS_KEY, encryptedData.keyAlias)
                
                // Update session state
                updateSessionState(tokens)
                
                Result.success(Unit)
            } else {
                Result.failure(SessionException("Failed to encrypt tokens"))
            }
        } catch (e: Exception) {
            Result.failure(SessionException("Failed to store tokens", e))
        }
    }
    
    override suspend fun getAccessToken(): Result<String?> {
        return try {
            val tokens = getStoredTokens().getOrNull()
            Result.success(tokens?.accessToken)
        } catch (e: Exception) {
            Result.failure(SessionException("Failed to get access token", e))
        }
    }
    
    override suspend fun getRefreshToken(): Result<String?> {
        return try {
            val tokens = getStoredTokens().getOrNull()
            Result.success(tokens?.refreshToken)
        } catch (e: Exception) {
            Result.failure(SessionException("Failed to get refresh token", e))
        }
    }
    
    override suspend fun isTokenValid(): Boolean {
        return try {
            val tokens = getStoredTokens().getOrNull() ?: return false
            val now = Clock.System.now()
            tokens.expiresAt > now
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun refreshToken(): Result<TokenRefreshResult> {
        return try {
            val refreshToken = getRefreshToken().getOrNull()
                ?: return Result.success(TokenRefreshResult.RefreshTokenExpired)
            
            // TODO: Implement actual API call to refresh token
            // For now, return a mock result
            val newExpiresAt = Clock.System.now().plus(kotlin.time.Duration.parse("1h"))
            val newAccessToken = "new_access_token_${Clock.System.now().toEpochMilliseconds()}"
            
            // Store the new tokens
            storeTokens(newAccessToken, refreshToken, newExpiresAt)
            
            Result.success(TokenRefreshResult.Success(newAccessToken, newExpiresAt))
        } catch (e: Exception) {
            Result.success(TokenRefreshResult.Error("Failed to refresh token: ${e.message}"))
        }
    }
    
    override suspend fun clearTokens(): Result<Unit> {
        return try {
            // Clear stored token data
            settings.remove(TOKEN_ENCRYPTED_CONTENT_KEY)
            settings.remove(TOKEN_IV_KEY)
            settings.remove(TOKEN_KEY_ALIAS_KEY)
            
            // Update session state
            _sessionState.value = SessionState()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(SessionException("Failed to clear tokens", e))
        }
    }
    
    override fun getSessionState(): Flow<SessionState> {
        return sessionState
    }
    
    override suspend fun hasValidSession(): Boolean {
        return isTokenValid()
    }
    
    override suspend fun getTokenExpirationTime(): Instant? {
        return try {
            getStoredTokens().getOrNull()?.expiresAt
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun getStoredTokens(): Result<AuthTokens> {
        return try {
            val encryptedContentStr = settings.getStringOrNull(TOKEN_ENCRYPTED_CONTENT_KEY)
                ?: return Result.failure(SessionException("No tokens stored"))
            val ivStr = settings.getStringOrNull(TOKEN_IV_KEY)
                ?: return Result.failure(SessionException("No IV stored"))
            val keyAlias = settings.getStringOrNull(TOKEN_KEY_ALIAS_KEY)
                ?: return Result.failure(SessionException("No key alias stored"))
            
            val encryptedContent = encryptedContentStr.split(",").map { it.toByte() }.toByteArray()
            val iv = ivStr.split(",").map { it.toByte() }.toByteArray()
            
            val encryptedData = com.north.mobile.data.security.EncryptedData(encryptedContent, iv, keyAlias)
            val decryptResult = encryptionManager.decrypt(encryptedData, keyAlias)
            
            if (decryptResult.isSuccess) {
                val tokensJson = decryptResult.getOrThrow()
                val tokens = json.decodeFromString<AuthTokens>(tokensJson)
                Result.success(tokens)
            } else {
                Result.failure(SessionException("Failed to decrypt tokens"))
            }
        } catch (e: Exception) {
            Result.failure(SessionException("Failed to get stored tokens", e))
        }
    }
    
    private fun updateSessionState(tokens: AuthTokens) {
        val now = Clock.System.now()
        _sessionState.value = SessionState(
            hasValidSession = tokens.expiresAt > now,
            accessToken = tokens.accessToken,
            tokenExpiresAt = tokens.expiresAt,
            isRefreshing = false
        )
    }
    
    companion object {
        private const val TOKEN_KEY_ALIAS = "north_session_tokens"
        private const val TOKEN_ENCRYPTED_CONTENT_KEY = "session_tokens_encrypted_content"
        private const val TOKEN_IV_KEY = "session_tokens_iv"
        private const val TOKEN_KEY_ALIAS_KEY = "session_tokens_key_alias"
    }
}

