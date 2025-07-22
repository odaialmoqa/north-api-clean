package com.north.mobile.data.auth

import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing user authentication including biometric and PIN-based authentication
 */
interface AuthenticationManager {
    /**
     * Check if biometric authentication is available on this device
     */
    fun isBiometricAvailable(): Boolean
    
    /**
     * Check if biometric authentication is enrolled (user has set up biometrics)
     */
    suspend fun isBiometricEnrolled(): Boolean
    
    /**
     * Authenticate using biometric authentication (Touch ID, Face ID, fingerprint)
     */
    suspend fun authenticateWithBiometric(): AuthResult
    
    /**
     * Set up PIN-based authentication
     */
    suspend fun setupPIN(pin: String): AuthResult
    
    /**
     * Authenticate using PIN
     */
    suspend fun authenticateWithPIN(pin: String): AuthResult
    
    /**
     * Check if PIN is set up
     */
    suspend fun isPINSetup(): Boolean
    
    /**
     * Check if any authentication method is available
     */
    suspend fun isAuthenticationSetup(): Boolean
    
    /**
     * Clear all authentication data (for logout)
     */
    suspend fun clearAuthentication(): AuthResult
    
    /**
     * Get the current authentication state
     */
    fun getAuthenticationState(): Flow<AuthenticationState>
    
    /**
     * Check if user is currently authenticated
     */
    suspend fun isAuthenticated(): Boolean
    
    /**
     * Set authentication status (after successful auth)
     */
    suspend fun setAuthenticated(authenticated: Boolean)
}

/**
 * Result of authentication operations
 */
sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String, val errorType: AuthErrorType) : AuthResult()
    object Cancelled : AuthResult()
    object BiometricNotAvailable : AuthResult()
    object BiometricNotEnrolled : AuthResult()
    object PINNotSetup : AuthResult()
}

/**
 * Types of authentication errors
 */
enum class AuthErrorType {
    BIOMETRIC_HARDWARE_NOT_AVAILABLE,
    BIOMETRIC_NOT_ENROLLED,
    BIOMETRIC_AUTHENTICATION_FAILED,
    PIN_AUTHENTICATION_FAILED,
    PIN_SETUP_FAILED,
    AUTHENTICATION_CANCELLED,
    UNKNOWN_ERROR
}

/**
 * Current authentication state
 */
data class AuthenticationState(
    val isAuthenticated: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnrolled: Boolean = false,
    val isPINSetup: Boolean = false,
    val lastAuthenticationTime: Long? = null
)

/**
 * Exception thrown when authentication operations fail
 */
class AuthenticationException(message: String, cause: Throwable? = null) : Exception(message, cause)