package com.north.mobile.integration

import com.north.mobile.data.auth.*
import com.north.mobile.data.security.EncryptionManager
import com.north.mobile.data.security.EncryptedData
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.hours

/**
 * Integration tests for the complete authentication system
 */
class AuthenticationIntegrationTest {
    
    private lateinit var encryptionManager: MockEncryptionManager
    private lateinit var authManager: TestAuthenticationManager
    private lateinit var sessionManager: SessionManagerImpl
    private lateinit var settings: MapSettings
    
    @BeforeTest
    fun setup() {
        encryptionManager = MockEncryptionManager()
        settings = MapSettings()
        authManager = TestAuthenticationManager(encryptionManager)
        sessionManager = SessionManagerImpl(settings, encryptionManager)
    }
    
    @Test
    fun `complete authentication flow with PIN and session management`() = runTest {
        // Step 1: Setup PIN authentication
        val pin = "1234"
        val setupResult = authManager.setupPIN(pin)
        assertTrue(setupResult is AuthResult.Success)
        assertTrue(authManager.isPINSetup())
        
        // Step 2: Authenticate with PIN
        val authResult = authManager.authenticateWithPIN(pin)
        assertTrue(authResult is AuthResult.Success)
        
        // Step 3: Set authenticated state
        authManager.setAuthenticated(true)
        assertTrue(authManager.isAuthenticated())
        
        // Step 4: Store session tokens
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"
        val expiresAt = Clock.System.now().plus(1.hours)
        
        val storeResult = sessionManager.storeTokens(accessToken, refreshToken, expiresAt)
        assertTrue(storeResult.isSuccess)
        
        // Step 5: Verify session is valid
        assertTrue(sessionManager.hasValidSession())
        assertEquals(accessToken, sessionManager.getAccessToken().getOrNull())
        
        // Step 6: Check authentication state
        val authState = authManager.getAuthenticationState().first()
        assertTrue(authState.isAuthenticated)
        assertTrue(authState.isPINSetup)
        assertNotNull(authState.lastAuthenticationTime)
        
        // Step 7: Check session state
        val sessionState = sessionManager.getSessionState().first()
        assertTrue(sessionState.hasValidSession)
        assertEquals(accessToken, sessionState.accessToken)
        assertEquals(expiresAt, sessionState.tokenExpiresAt)
    }
    
    @Test
    fun `authentication failure should not create session`() = runTest {
        // Step 1: Setup PIN
        val correctPin = "1234"
        val incorrectPin = "5678"
        
        authManager.setupPIN(correctPin)
        
        // Step 2: Try to authenticate with wrong PIN
        val authResult = authManager.authenticateWithPIN(incorrectPin)
        assertTrue(authResult is AuthResult.Error)
        
        // Step 3: Verify user is not authenticated
        assertFalse(authManager.isAuthenticated())
        
        // Step 4: Verify no session tokens should be stored
        assertNull(sessionManager.getAccessToken().getOrNull())
        assertFalse(sessionManager.hasValidSession())
    }
    
    @Test
    fun `logout should clear both authentication and session`() = runTest {
        // Step 1: Setup authentication and session
        val pin = "1234"
        authManager.setupPIN(pin)
        authManager.authenticateWithPIN(pin)
        authManager.setAuthenticated(true)
        
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"
        val expiresAt = Clock.System.now().plus(1.hours)
        sessionManager.storeTokens(accessToken, refreshToken, expiresAt)
        
        // Verify setup
        assertTrue(authManager.isAuthenticated())
        assertTrue(sessionManager.hasValidSession())
        
        // Step 2: Clear authentication
        val clearAuthResult = authManager.clearAuthentication()
        assertTrue(clearAuthResult is AuthResult.Success)
        
        // Step 3: Clear session
        val clearSessionResult = sessionManager.clearTokens()
        assertTrue(clearSessionResult.isSuccess)
        
        // Step 4: Verify everything is cleared
        assertFalse(authManager.isAuthenticated())
        assertFalse(authManager.isPINSetup())
        assertFalse(sessionManager.hasValidSession())
        assertNull(sessionManager.getAccessToken().getOrNull())
        
        val authState = authManager.getAuthenticationState().first()
        assertFalse(authState.isAuthenticated)
        assertFalse(authState.isPINSetup)
        assertNull(authState.lastAuthenticationTime)
        
        val sessionState = sessionManager.getSessionState().first()
        assertFalse(sessionState.hasValidSession)
        assertNull(sessionState.accessToken)
        assertNull(sessionState.tokenExpiresAt)
    }
    
    @Test
    fun `token refresh should work with valid refresh token`() = runTest {
        // Step 1: Setup initial session
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"
        val expiresAt = Clock.System.now().plus(1.hours)
        
        sessionManager.storeTokens(accessToken, refreshToken, expiresAt)
        
        // Step 2: Refresh token
        val refreshResult = sessionManager.refreshToken()
        assertTrue(refreshResult.isSuccess)
        
        val result = refreshResult.getOrThrow()
        assertTrue(result is TokenRefreshResult.Success)
        
        val successResult = result as TokenRefreshResult.Success
        assertNotEquals(accessToken, successResult.accessToken)
        
        // Step 3: Verify new token is stored
        val newAccessToken = sessionManager.getAccessToken().getOrNull()
        assertEquals(successResult.accessToken, newAccessToken)
        assertTrue(sessionManager.hasValidSession())
    }
    
    @Test
    fun `biometric authentication should work when available`() = runTest {
        // Step 1: Check biometric availability
        assertTrue(authManager.isBiometricAvailable())
        assertTrue(authManager.isBiometricEnrolled())
        
        // Step 2: Authenticate with biometric
        val authResult = authManager.authenticateWithBiometric()
        assertTrue(authResult is AuthResult.Success)
        
        // Step 3: Set authenticated state
        authManager.setAuthenticated(true)
        assertTrue(authManager.isAuthenticated())
        
        // Step 4: Verify authentication setup
        assertTrue(authManager.isAuthenticationSetup())
    }
    
    @Test
    fun `encryption failure should not compromise security`() = runTest {
        // Step 1: Setup PIN successfully
        val pin = "1234"
        val setupResult = authManager.setupPIN(pin)
        assertTrue(setupResult is AuthResult.Success)
        
        // Step 2: Simulate encryption failure for session tokens
        encryptionManager.shouldFailEncryption = true
        
        val storeResult = sessionManager.storeTokens(
            "access_token",
            "refresh_token",
            Clock.System.now().plus(1.hours)
        )
        
        // Step 3: Verify session storage fails
        assertTrue(storeResult.isFailure)
        assertFalse(sessionManager.hasValidSession())
        
        // Step 4: Verify authentication still works (different encryption path)
        encryptionManager.shouldFailEncryption = false
        val authResult = authManager.authenticateWithPIN(pin)
        assertTrue(authResult is AuthResult.Success)
    }
    
    @Test
    fun `concurrent authentication attempts should be handled safely`() = runTest {
        // This test would be more meaningful in a real concurrent environment
        // For now, we'll test sequential operations that simulate concurrent access
        
        val pin = "1234"
        authManager.setupPIN(pin)
        
        // Simulate multiple authentication attempts
        val results = mutableListOf<AuthResult>()
        repeat(5) {
            results.add(authManager.authenticateWithPIN(pin))
        }
        
        // All should succeed
        results.forEach { result ->
            assertTrue(result is AuthResult.Success)
        }
        
        // Test with wrong PIN
        val wrongResults = mutableListOf<AuthResult>()
        repeat(3) {
            wrongResults.add(authManager.authenticateWithPIN("wrong"))
        }
        
        // All should fail
        wrongResults.forEach { result ->
            assertTrue(result is AuthResult.Error)
        }
    }
}

// Mock implementations for integration testing
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

private class TestAuthenticationManager(
    private val encryptionManager: EncryptionManager
) : AuthenticationManager {
    
    private var pinData: EncryptedData? = null
    private var authState = AuthenticationState()
    
    override fun isBiometricAvailable(): Boolean = true
    
    override suspend fun isBiometricEnrolled(): Boolean = true
    
    override suspend fun authenticateWithBiometric(): AuthResult = AuthResult.Success
    
    override suspend fun setupPIN(pin: String): AuthResult {
        if (pin.length < 4) {
            return AuthResult.Error("PIN must be at least 4 digits", AuthErrorType.PIN_SETUP_FAILED)
        }
        
        val hashedPIN = pin.hashCode().toString()
        val encryptResult = encryptionManager.encrypt(hashedPIN, "test_pin_key")
        
        return if (encryptResult.isSuccess) {
            pinData = encryptResult.getOrThrow()
            authState = authState.copy(isPINSetup = true)
            AuthResult.Success
        } else {
            AuthResult.Error("Failed to setup PIN", AuthErrorType.PIN_SETUP_FAILED)
        }
    }
    
    override suspend fun authenticateWithPIN(pin: String): AuthResult {
        if (pinData == null) {
            return AuthResult.PINNotSetup
        }
        
        val decryptResult = encryptionManager.decrypt(pinData!!, "test_pin_key")
        if (decryptResult.isFailure) {
            return AuthResult.Error("Failed to decrypt PIN", AuthErrorType.PIN_AUTHENTICATION_FAILED)
        }
        
        val storedHashedPIN = decryptResult.getOrThrow()
        val inputHashedPIN = pin.hashCode().toString()
        
        return if (storedHashedPIN == inputHashedPIN) {
            AuthResult.Success
        } else {
            AuthResult.Error("Invalid PIN", AuthErrorType.PIN_AUTHENTICATION_FAILED)
        }
    }
    
    override suspend fun isPINSetup(): Boolean = pinData != null
    
    override suspend fun isAuthenticationSetup(): Boolean = isPINSetup() || isBiometricEnrolled()
    
    override suspend fun clearAuthentication(): AuthResult {
        pinData = null
        authState = authState.copy(
            isAuthenticated = false,
            isPINSetup = false,
            lastAuthenticationTime = null
        )
        return AuthResult.Success
    }
    
    override fun getAuthenticationState() = kotlinx.coroutines.flow.flowOf(authState)
    
    override suspend fun isAuthenticated(): Boolean = authState.isAuthenticated
    
    override suspend fun setAuthenticated(authenticated: Boolean) {
        authState = authState.copy(
            isAuthenticated = authenticated,
            lastAuthenticationTime = if (authenticated) System.currentTimeMillis() else null
        )
    }
}