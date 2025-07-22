package com.north.mobile.data.auth

import com.north.mobile.data.security.EncryptionManager
import com.north.mobile.data.security.EncryptedData
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.*
import kotlin.time.Duration.Companion.hours

class SessionManagerTest {
    
    private lateinit var mockEncryptionManager: MockEncryptionManager
    private lateinit var settings: MapSettings
    private lateinit var sessionManager: SessionManagerImpl
    
    @BeforeTest
    fun setup() {
        mockEncryptionManager = MockEncryptionManager()
        settings = MapSettings()
        sessionManager = SessionManagerImpl(settings, mockEncryptionManager)
    }
    
    @Test
    fun `storeTokens should encrypt and store tokens successfully`() = runTest {
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"
        val expiresAt = Clock.System.now().plus(1.hours)
        
        val result = sessionManager.storeTokens(accessToken, refreshToken, expiresAt)
        
        assertTrue(result.isSuccess)
        
        val storedAccessToken = sessionManager.getAccessToken().getOrNull()
        assertEquals(accessToken, storedAccessToken)
        
        val storedRefreshToken = sessionManager.getRefreshToken().getOrNull()
        assertEquals(refreshToken, storedRefreshToken)
    }
    
    @Test
    fun `getAccessToken should return null when no tokens stored`() = runTest {
        val result = sessionManager.getAccessToken()
        
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun `isTokenValid should return true for valid token`() = runTest {
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"
        val expiresAt = Clock.System.now().plus(1.hours)
        
        sessionManager.storeTokens(accessToken, refreshToken, expiresAt)
        
        assertTrue(sessionManager.isTokenValid())
    }
    
    @Test
    fun `isTokenValid should return false for expired token`() = runTest {
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"
        val expiresAt = Clock.System.now().minus(1.hours) // Expired
        
        sessionManager.storeTokens(accessToken, refreshToken, expiresAt)
        
        assertFalse(sessionManager.isTokenValid())
    }
    
    @Test
    fun `hasValidSession should return true when token is valid`() = runTest {
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"
        val expiresAt = Clock.System.now().plus(1.hours)
        
        sessionManager.storeTokens(accessToken, refreshToken, expiresAt)
        
        assertTrue(sessionManager.hasValidSession())
    }
    
    @Test
    fun `hasValidSession should return false when token is expired`() = runTest {
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"
        val expiresAt = Clock.System.now().minus(1.hours)
        
        sessionManager.storeTokens(accessToken, refreshToken, expiresAt)
        
        assertFalse(sessionManager.hasValidSession())
    }
    
    @Test
    fun `clearTokens should remove all stored tokens`() = runTest {
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"
        val expiresAt = Clock.System.now().plus(1.hours)
        
        sessionManager.storeTokens(accessToken, refreshToken, expiresAt)
        
        val clearResult = sessionManager.clearTokens()
        assertTrue(clearResult.isSuccess)
        
        val accessTokenResult = sessionManager.getAccessToken()
        assertTrue(accessTokenResult.isSuccess)
        assertNull(accessTokenResult.getOrNull())
        
        val refreshTokenResult = sessionManager.getRefreshToken()
        assertTrue(refreshTokenResult.isSuccess)
        assertNull(refreshTokenResult.getOrNull())
        
        assertFalse(sessionManager.hasValidSession())
    }
    
    @Test
    fun `getTokenExpirationTime should return correct expiration time`() = runTest {
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"
        val expiresAt = Clock.System.now().plus(1.hours)
        
        sessionManager.storeTokens(accessToken, refreshToken, expiresAt)
        
        val retrievedExpirationTime = sessionManager.getTokenExpirationTime()
        assertEquals(expiresAt, retrievedExpirationTime)
    }
    
    @Test
    fun `getTokenExpirationTime should return null when no tokens stored`() = runTest {
        val expirationTime = sessionManager.getTokenExpirationTime()
        assertNull(expirationTime)
    }
    
    @Test
    fun `refreshToken should return success with new token`() = runTest {
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"
        val expiresAt = Clock.System.now().plus(1.hours)
        
        sessionManager.storeTokens(accessToken, refreshToken, expiresAt)
        
        val refreshResult = sessionManager.refreshToken()
        assertTrue(refreshResult.isSuccess)
        
        val result = refreshResult.getOrThrow()
        assertTrue(result is TokenRefreshResult.Success)
        
        val successResult = result as TokenRefreshResult.Success
        assertNotEquals(accessToken, successResult.accessToken)
        assertTrue(successResult.expiresAt > Clock.System.now())
    }
    
    @Test
    fun `refreshToken should return RefreshTokenExpired when no refresh token`() = runTest {
        val refreshResult = sessionManager.refreshToken()
        assertTrue(refreshResult.isSuccess)
        
        val result = refreshResult.getOrThrow()
        assertTrue(result is TokenRefreshResult.RefreshTokenExpired)
    }
    
    @Test
    fun `session state should be updated correctly`() = runTest {
        val initialState = sessionManager.getSessionState().first()
        assertFalse(initialState.hasValidSession)
        assertNull(initialState.accessToken)
        assertNull(initialState.tokenExpiresAt)
        
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"
        val expiresAt = Clock.System.now().plus(1.hours)
        
        sessionManager.storeTokens(accessToken, refreshToken, expiresAt)
        
        val updatedState = sessionManager.getSessionState().first()
        assertTrue(updatedState.hasValidSession)
        assertEquals(accessToken, updatedState.accessToken)
        assertEquals(expiresAt, updatedState.tokenExpiresAt)
    }
    
    @Test
    fun `encryption failure should result in store failure`() = runTest {
        mockEncryptionManager.shouldFailEncryption = true
        
        val result = sessionManager.storeTokens("token", "refresh", Clock.System.now().plus(1.hours))
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SessionException)
    }
    
    @Test
    fun `decryption failure should result in get failure`() = runTest {
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"
        val expiresAt = Clock.System.now().plus(1.hours)
        
        sessionManager.storeTokens(accessToken, refreshToken, expiresAt)
        mockEncryptionManager.shouldFailDecryption = true
        
        val result = sessionManager.getAccessToken()
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SessionException)
    }
}

// Mock implementation for testing
private class MockEncryptionManager : EncryptionManager {
    var shouldFailEncryption = false
    var shouldFailDecryption = false
    private val storage = mutableMapOf<String, String>()
    
    override suspend fun initialize(): Result<Unit> = Result.success(Unit)
    
    override suspend fun generateDatabaseKey(): Result<String> = Result.success("test_key")
    
    override suspend fun getDatabaseKey(): Result<String> = Result.success("test_key")
    
    override suspend fun encrypt(data: String, keyAlias: String): Result<EncryptedData> {
        return if (shouldFailEncryption) {
            Result.failure(Exception("Encryption failed"))
        } else {
            storage[keyAlias] = data
            val encryptedData = EncryptedData(
                encryptedContent = data.encodeToByteArray(),
                iv = "test_iv".encodeToByteArray(),
                keyAlias = keyAlias
            )
            Result.success(encryptedData)
        }
    }
    
    override suspend fun decrypt(encryptedData: EncryptedData, keyAlias: String): Result<String> {
        return if (shouldFailDecryption) {
            Result.failure(Exception("Decryption failed"))
        } else {
            val data = storage[keyAlias] ?: return Result.failure(Exception("Data not found"))
            Result.success(data)
        }
    }
    
    override fun isEncryptionAvailable(): Boolean = true
    
    override suspend fun clearKeys(): Result<Unit> {
        storage.clear()
        return Result.success(Unit)
    }
}