package com.north.mobile.data.auth

import com.north.mobile.data.security.EncryptionManager
import com.north.mobile.data.security.EncryptedData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class AuthenticationManagerTest {
    
    private lateinit var mockEncryptionManager: MockEncryptionManager
    private lateinit var authManager: TestAuthenticationManager
    
    @BeforeTest
    fun setup() {
        mockEncryptionManager = MockEncryptionManager()
        authManager = TestAuthenticationManager(mockEncryptionManager)
    }
    
    @Test
    fun `setupPIN should succeed with valid PIN`() = runTest {
        val pin = "1234"
        val result = authManager.setupPIN(pin)
        
        assertTrue(result is AuthResult.Success)
        assertTrue(authManager.isPINSetup())
    }
    
    @Test
    fun `setupPIN should fail with short PIN`() = runTest {
        val pin = "123"
        val result = authManager.setupPIN(pin)
        
        assertTrue(result is AuthResult.Error)
        assertEquals(AuthErrorType.PIN_SETUP_FAILED, result.errorType)
        assertFalse(authManager.isPINSetup())
    }
    
    @Test
    fun `authenticateWithPIN should succeed with correct PIN`() = runTest {
        val pin = "1234"
        authManager.setupPIN(pin)
        
        val result = authManager.authenticateWithPIN(pin)
        
        assertTrue(result is AuthResult.Success)
    }
    
    @Test
    fun `authenticateWithPIN should fail with incorrect PIN`() = runTest {
        val correctPin = "1234"
        val incorrectPin = "5678"
        
        authManager.setupPIN(correctPin)
        val result = authManager.authenticateWithPIN(incorrectPin)
        
        assertTrue(result is AuthResult.Error)
        assertEquals(AuthErrorType.PIN_AUTHENTICATION_FAILED, result.errorType)
    }
    
    @Test
    fun `authenticateWithPIN should fail when PIN not setup`() = runTest {
        val result = authManager.authenticateWithPIN("1234")
        
        assertTrue(result is AuthResult.PINNotSetup)
    }
    
    @Test
    fun `clearAuthentication should clear PIN and authentication state`() = runTest {
        val pin = "1234"
        authManager.setupPIN(pin)
        authManager.setAuthenticated(true)
        
        val result = authManager.clearAuthentication()
        
        assertTrue(result is AuthResult.Success)
        assertFalse(authManager.isPINSetup())
        assertFalse(authManager.isAuthenticated())
    }
    
    @Test
    fun `authentication state should be updated correctly`() = runTest {
        val initialState = authManager.getAuthenticationState().first()
        assertFalse(initialState.isAuthenticated)
        assertFalse(initialState.isPINSetup)
        
        authManager.setupPIN("1234")
        authManager.setAuthenticated(true)
        
        val updatedState = authManager.getAuthenticationState().first()
        assertTrue(updatedState.isAuthenticated)
        assertTrue(updatedState.isPINSetup)
        assertNotNull(updatedState.lastAuthenticationTime)
    }
    
    @Test
    fun `isAuthenticationSetup should return true when PIN is setup`() = runTest {
        assertFalse(authManager.isAuthenticationSetup())
        
        authManager.setupPIN("1234")
        
        assertTrue(authManager.isAuthenticationSetup())
    }
    
    @Test
    fun `encryption failure should result in PIN setup failure`() = runTest {
        mockEncryptionManager.shouldFailEncryption = true
        
        val result = authManager.setupPIN("1234")
        
        assertTrue(result is AuthResult.Error)
        assertEquals(AuthErrorType.PIN_SETUP_FAILED, result.errorType)
    }
    
    @Test
    fun `decryption failure should result in PIN authentication failure`() = runTest {
        authManager.setupPIN("1234")
        mockEncryptionManager.shouldFailDecryption = true
        
        val result = authManager.authenticateWithPIN("1234")
        
        assertTrue(result is AuthResult.Error)
        assertEquals(AuthErrorType.PIN_AUTHENTICATION_FAILED, result.errorType)
    }
}

// Mock implementation for testing
private class MockEncryptionManager : EncryptionManager {
    var shouldFailEncryption = false
    var shouldFailDecryption = false
    private val storage = mutableMapOf<String, EncryptedData>()
    
    override suspend fun initialize(): Result<Unit> = Result.success(Unit)
    
    override suspend fun generateDatabaseKey(): Result<String> = Result.success("test_key")
    
    override suspend fun getDatabaseKey(): Result<String> = Result.success("test_key")
    
    override suspend fun encrypt(data: String, keyAlias: String): Result<EncryptedData> {
        return if (shouldFailEncryption) {
            Result.failure(Exception("Encryption failed"))
        } else {
            val encryptedData = EncryptedData(
                encryptedContent = data.encodeToByteArray(),
                iv = "test_iv".encodeToByteArray(),
                keyAlias = keyAlias
            )
            storage[keyAlias] = encryptedData
            Result.success(encryptedData)
        }
    }
    
    override suspend fun decrypt(encryptedData: EncryptedData, keyAlias: String): Result<String> {
        return if (shouldFailDecryption) {
            Result.failure(Exception("Decryption failed"))
        } else {
            Result.success(encryptedData.encryptedContent.decodeToString())
        }
    }
    
    override fun isEncryptionAvailable(): Boolean = true
    
    override suspend fun clearKeys(): Result<Unit> {
        storage.clear()
        return Result.success(Unit)
    }
}

// Test implementation of AuthenticationManager
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