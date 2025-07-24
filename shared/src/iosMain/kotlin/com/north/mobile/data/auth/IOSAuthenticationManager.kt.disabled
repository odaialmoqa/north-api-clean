package com.north.mobile.data.auth

import com.north.mobile.data.security.EncryptionManager
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.LocalAuthentication.*
import platform.Security.*
import kotlin.coroutines.resume

/**
 * iOS implementation of AuthenticationManager using LocalAuthentication framework
 */
class IOSAuthenticationManager(
    private val encryptionManager: EncryptionManager
) : AuthenticationManager {
    
    private val _authenticationState = MutableStateFlow(AuthenticationState())
    private val authenticationState = _authenticationState.asStateFlow()
    
    private val laContext = LAContext()
    
    override fun isBiometricAvailable(): Boolean {
        return memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val result = laContext.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error.ptr)
            result
        }
    }
    
    override suspend fun isBiometricEnrolled(): Boolean {
        return isBiometricAvailable() // On iOS, if biometric is available, it's enrolled
    }
    
    override suspend fun authenticateWithBiometric(): AuthResult {
        if (!isBiometricAvailable()) {
            return AuthResult.BiometricNotAvailable
        }
        
        return suspendCancellableCoroutine { continuation ->
            laContext.evaluatePolicy(
                policy = LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = "Authenticate to access your financial data"
            ) { success, error ->
                if (success) {
                    continuation.resume(AuthResult.Success)
                } else {
                    val nsError = error
                    val result = when (nsError?.code) {
                        LAErrorUserCancel -> AuthResult.Cancelled
                        LAErrorBiometryNotAvailable -> AuthResult.BiometricNotAvailable
                        LAErrorBiometryNotEnrolled -> AuthResult.BiometricNotEnrolled
                        else -> AuthResult.Error(
                            nsError?.localizedDescription ?: "Biometric authentication failed",
                            AuthErrorType.BIOMETRIC_AUTHENTICATION_FAILED
                        )
                    }
                    continuation.resume(result)
                }
            }
        }
    }
    
    override suspend fun setupPIN(pin: String): AuthResult {
        return try {
            if (pin.length < 4) {
                return AuthResult.Error("PIN must be at least 4 digits", AuthErrorType.PIN_SETUP_FAILED)
            }
            
            // Hash the PIN before storing
            val hashedPIN = hashPIN(pin)
            val encryptResult = encryptionManager.encrypt(hashedPIN, PIN_KEY_ALIAS)
            
            if (encryptResult.isSuccess) {
                // Store encrypted PIN hash in keychain
                val encryptedData = encryptResult.getOrThrow()
                val storeResult = storePINInKeychain(encryptedData)
                if (storeResult.isSuccess) {
                    updateAuthenticationState()
                    AuthResult.Success
                } else {
                    AuthResult.Error("Failed to store PIN", AuthErrorType.PIN_SETUP_FAILED)
                }
            } else {
                AuthResult.Error("Failed to encrypt PIN", AuthErrorType.PIN_SETUP_FAILED)
            }
        } catch (e: Exception) {
            AuthResult.Error("Failed to setup PIN: ${e.message}", AuthErrorType.PIN_SETUP_FAILED)
        }
    }
    
    override suspend fun authenticateWithPIN(pin: String): AuthResult {
        return try {
            if (!isPINSetup()) {
                return AuthResult.PINNotSetup
            }
            
            val storedPINData = getPINFromKeychain().getOrNull() ?: return AuthResult.Error(
                "PIN data not found",
                AuthErrorType.PIN_AUTHENTICATION_FAILED
            )
            
            val decryptResult = encryptionManager.decrypt(storedPINData, PIN_KEY_ALIAS)
            if (decryptResult.isFailure) {
                return AuthResult.Error(
                    "Failed to decrypt PIN data",
                    AuthErrorType.PIN_AUTHENTICATION_FAILED
                )
            }
            
            val storedHashedPIN = decryptResult.getOrThrow()
            val inputHashedPIN = hashPIN(pin)
            
            if (storedHashedPIN == inputHashedPIN) {
                AuthResult.Success
            } else {
                AuthResult.Error("Invalid PIN", AuthErrorType.PIN_AUTHENTICATION_FAILED)
            }
        } catch (e: Exception) {
            AuthResult.Error("PIN authentication failed: ${e.message}", AuthErrorType.PIN_AUTHENTICATION_FAILED)
        }
    }
    
    override suspend fun isPINSetup(): Boolean {
        return getPINFromKeychain().isSuccess
    }
    
    override suspend fun isAuthenticationSetup(): Boolean {
        return isBiometricEnrolled() || isPINSetup()
    }
    
    override suspend fun clearAuthentication(): AuthResult {
        return try {
            // Clear PIN data from keychain
            clearPINFromKeychain()
            
            // Clear authentication state
            setAuthenticated(false)
            updateAuthenticationState()
            
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error("Failed to clear authentication: ${e.message}", AuthErrorType.UNKNOWN_ERROR)
        }
    }
    
    override fun getAuthenticationState(): Flow<AuthenticationState> {
        return authenticationState
    }
    
    override suspend fun isAuthenticated(): Boolean {
        return _authenticationState.value.isAuthenticated
    }
    
    override suspend fun setAuthenticated(authenticated: Boolean) {
        val currentState = _authenticationState.value
        _authenticationState.value = currentState.copy(
            isAuthenticated = authenticated,
            lastAuthenticationTime = if (authenticated) NSDate().timeIntervalSince1970.toLong() * 1000 else null
        )
    }
    
    private suspend fun updateAuthenticationState() {
        val currentState = _authenticationState.value
        _authenticationState.value = currentState.copy(
            isBiometricAvailable = isBiometricAvailable(),
            isBiometricEnrolled = isBiometricEnrolled(),
            isPINSetup = isPINSetup()
        )
    }
    
    private fun hashPIN(pin: String): String {
        // Simple hash function - in production, use a proper password hashing library
        return pin.hashCode().toString()
    }
    
    private fun storePINInKeychain(encryptedData: com.north.mobile.data.security.EncryptedData): Result<Unit> {
        return memScoped {
            try {
                // Serialize encrypted data to JSON-like string
                val dataString = "${encryptedData.encryptedContent.joinToString(",")};${encryptedData.iv.joinToString(",")};${encryptedData.keyAlias}"
                val data = dataString.encodeToByteArray()
                val cfData = CFDataCreate(null, data.refTo(0), data.size.toLong())
                
                val query = CFDictionaryCreateMutable(null, 0, null, null)
                CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
                CFDictionarySetValue(query, kSecAttrService, CFStringCreateWithCString(null, SERVICE_NAME, kCFStringEncodingUTF8))
                CFDictionarySetValue(query, kSecAttrAccount, CFStringCreateWithCString(null, PIN_ACCOUNT, kCFStringEncodingUTF8))
                CFDictionarySetValue(query, kSecValueData, cfData)
                CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
                
                val status = SecItemAdd(query, null)
                
                CFRelease(query)
                CFRelease(cfData)
                
                if (status == errSecSuccess) {
                    Result.success(Unit)
                } else if (status == errSecDuplicateItem) {
                    // Update existing item
                    updatePINInKeychain(encryptedData)
                } else {
                    Result.failure(Exception("Failed to store PIN in keychain, status: $status"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun updatePINInKeychain(encryptedData: com.north.mobile.data.security.EncryptedData): Result<Unit> {
        return memScoped {
            try {
                val dataString = "${encryptedData.encryptedContent.joinToString(",")};${encryptedData.iv.joinToString(",")};${encryptedData.keyAlias}"
                val data = dataString.encodeToByteArray()
                val cfData = CFDataCreate(null, data.refTo(0), data.size.toLong())
                
                val query = CFDictionaryCreateMutable(null, 0, null, null)
                CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
                CFDictionarySetValue(query, kSecAttrService, CFStringCreateWithCString(null, SERVICE_NAME, kCFStringEncodingUTF8))
                CFDictionarySetValue(query, kSecAttrAccount, CFStringCreateWithCString(null, PIN_ACCOUNT, kCFStringEncodingUTF8))
                
                val attributes = CFDictionaryCreateMutable(null, 0, null, null)
                CFDictionarySetValue(attributes, kSecValueData, cfData)
                
                val status = SecItemUpdate(query, attributes)
                
                CFRelease(query)
                CFRelease(attributes)
                CFRelease(cfData)
                
                if (status == errSecSuccess) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to update PIN in keychain, status: $status"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun getPINFromKeychain(): Result<com.north.mobile.data.security.EncryptedData> {
        return memScoped {
            try {
                val query = CFDictionaryCreateMutable(null, 0, null, null)
                CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
                CFDictionarySetValue(query, kSecAttrService, CFStringCreateWithCString(null, SERVICE_NAME, kCFStringEncodingUTF8))
                CFDictionarySetValue(query, kSecAttrAccount, CFStringCreateWithCString(null, PIN_ACCOUNT, kCFStringEncodingUTF8))
                CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
                CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
                
                val result = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(query, result.ptr)
                
                CFRelease(query)
                
                if (status == errSecSuccess) {
                    val data = result.value as CFDataRef
                    val length = CFDataGetLength(data).toInt()
                    val bytes = CFDataGetBytePtr(data)
                    val dataString = bytes?.readBytes(length)?.decodeToString()
                    
                    if (dataString != null) {
                        val parts = dataString.split(";")
                        if (parts.size == 3) {
                            val encryptedContent = parts[0].split(",").map { it.toByte() }.toByteArray()
                            val iv = parts[1].split(",").map { it.toByte() }.toByteArray()
                            val keyAlias = parts[2]
                            
                            val encryptedData = com.north.mobile.data.security.EncryptedData(encryptedContent, iv, keyAlias)
                            Result.success(encryptedData)
                        } else {
                            Result.failure(Exception("Invalid PIN data format"))
                        }
                    } else {
                        Result.failure(Exception("Failed to decode PIN data"))
                    }
                } else {
                    Result.failure(Exception("PIN not found in keychain, status: $status"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun clearPINFromKeychain(): Result<Unit> {
        return memScoped {
            try {
                val query = CFDictionaryCreateMutable(null, 0, null, null)
                CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
                CFDictionarySetValue(query, kSecAttrService, CFStringCreateWithCString(null, SERVICE_NAME, kCFStringEncodingUTF8))
                CFDictionarySetValue(query, kSecAttrAccount, CFStringCreateWithCString(null, PIN_ACCOUNT, kCFStringEncodingUTF8))
                
                val status = SecItemDelete(query)
                CFRelease(query)
                
                if (status == errSecSuccess || status == errSecItemNotFound) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to clear PIN from keychain, status: $status"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    companion object {
        private const val SERVICE_NAME = "com.north.mobile.auth"
        private const val PIN_ACCOUNT = "user_pin"
        private const val PIN_KEY_ALIAS = "north_pin_key"
    }
}