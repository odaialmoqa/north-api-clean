package com.north.mobile.data.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.north.mobile.data.security.EncryptionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of AuthenticationManager using BiometricPrompt
 */
class AndroidAuthenticationManager(
    private val context: Context,
    private val encryptionManager: EncryptionManager
) : AuthenticationManager {
    
    private val _authenticationState = MutableStateFlow(AuthenticationState())
    private val authenticationState = _authenticationState.asStateFlow()
    
    private val biometricManager = BiometricManager.from(context)
    
    // Current activity reference for biometric prompt
    private var currentActivity: FragmentActivity? = null
    
    fun setCurrentActivity(activity: FragmentActivity?) {
        currentActivity = activity
    }
    
    override fun isBiometricAvailable(): Boolean {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    override suspend fun isBiometricEnrolled(): Boolean {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
            else -> false
        }
    }
    
    override suspend fun authenticateWithBiometric(): AuthResult {
        val activity = currentActivity ?: return AuthResult.Error(
            "No activity available for biometric authentication",
            AuthErrorType.UNKNOWN_ERROR
        )
        
        if (!isBiometricAvailable()) {
            return AuthResult.BiometricNotAvailable
        }
        
        if (!isBiometricEnrolled()) {
            return AuthResult.BiometricNotEnrolled
        }
        
        return suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        val result = when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_CANCELED -> AuthResult.Cancelled
                            BiometricPrompt.ERROR_HW_NOT_PRESENT,
                            BiometricPrompt.ERROR_HW_UNAVAILABLE -> AuthResult.BiometricNotAvailable
                            BiometricPrompt.ERROR_NO_BIOMETRICS -> AuthResult.BiometricNotEnrolled
                            else -> AuthResult.Error(
                                errString.toString(),
                                AuthErrorType.BIOMETRIC_AUTHENTICATION_FAILED
                            )
                        }
                        continuation.resume(result)
                    }
                    
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        continuation.resume(AuthResult.Success)
                    }
                    
                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        // Don't resume here, let user try again
                    }
                })
            
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate to North")
                .setSubtitle("Use your biometric credential to access your financial data")
                .setNegativeButtonText("Cancel")
                .build()
            
            biometricPrompt.authenticate(promptInfo)
            
            continuation.invokeOnCancellation {
                biometricPrompt.cancelAuthentication()
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
                // Store encrypted PIN hash
                val encryptedData = encryptResult.getOrThrow()
                storePINData(encryptedData)
                updateAuthenticationState()
                AuthResult.Success
            } else {
                AuthResult.Error("Failed to setup PIN", AuthErrorType.PIN_SETUP_FAILED)
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
            
            val storedPINData = getPINData() ?: return AuthResult.Error(
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
        return getPINData() != null
    }
    
    override suspend fun isAuthenticationSetup(): Boolean {
        return isBiometricEnrolled() || isPINSetup()
    }
    
    override suspend fun clearAuthentication(): AuthResult {
        return try {
            // Clear PIN data
            clearPINData()
            
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
            lastAuthenticationTime = if (authenticated) System.currentTimeMillis() else null
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
        // Simple hash function - in production, use a proper password hashing library like Argon2
        return pin.hashCode().toString()
    }
    
    private fun storePINData(encryptedData: com.north.mobile.data.security.EncryptedData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(PIN_ENCRYPTED_CONTENT_KEY, encryptedData.encryptedContent.joinToString(",") { it.toString() })
            .putString(PIN_IV_KEY, encryptedData.iv.joinToString(",") { it.toString() })
            .putString(PIN_KEY_ALIAS_KEY, encryptedData.keyAlias)
            .apply()
    }
    
    private fun getPINData(): com.north.mobile.data.security.EncryptedData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedContentStr = prefs.getString(PIN_ENCRYPTED_CONTENT_KEY, null) ?: return null
        val ivStr = prefs.getString(PIN_IV_KEY, null) ?: return null
        val keyAlias = prefs.getString(PIN_KEY_ALIAS_KEY, null) ?: return null
        
        return try {
            val encryptedContent = encryptedContentStr.split(",").map { it.toByte() }.toByteArray()
            val iv = ivStr.split(",").map { it.toByte() }.toByteArray()
            com.north.mobile.data.security.EncryptedData(encryptedContent, iv, keyAlias)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun clearPINData() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(PIN_ENCRYPTED_CONTENT_KEY)
            .remove(PIN_IV_KEY)
            .remove(PIN_KEY_ALIAS_KEY)
            .apply()
    }
    
    companion object {
        private const val PREFS_NAME = "north_auth_prefs"
        private const val PIN_KEY_ALIAS = "north_pin_key"
        private const val PIN_ENCRYPTED_CONTENT_KEY = "pin_encrypted_content"
        private const val PIN_IV_KEY = "pin_iv"
        private const val PIN_KEY_ALIAS_KEY = "pin_key_alias"
    }
}