package com.north.mobile.data.repository

import com.north.mobile.data.api.AuthApiService
import com.north.mobile.data.api.AuthResponse
import com.north.mobile.data.auth.SessionManager
import com.north.mobile.data.auth.SessionManagerImpl
import com.north.mobile.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for authentication and user management
 */
class AuthRepository(
    private val authApiService: AuthApiService,
    private val sessionManager: SessionManager = SessionManagerImpl()
) {
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    /**
     * Register a new user
     */
    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<User> {
        return try {
            val result = authApiService.register(email, password, firstName, lastName)
            
            result.fold(
                onSuccess = { authResponse ->
                    handleAuthSuccess(authResponse)
                    Result.success(mapToUser(authResponse))
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Login user
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = authApiService.login(email, password)
            
            result.fold(
                onSuccess = { authResponse ->
                    handleAuthSuccess(authResponse)
                    Result.success(mapToUser(authResponse))
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Logout user
     */
    suspend fun logout() {
        sessionManager.clearSession()
        _currentUser.value = null
        _authToken.value = null
        _isAuthenticated.value = false
    }
    
    /**
     * Check if user is authenticated
     */
    suspend fun isUserAuthenticated(): Boolean {
        return sessionManager.isSessionValid()
    }
    
    /**
     * Get current auth token
     */
    suspend fun getCurrentToken(): String? {
        return sessionManager.getAuthToken()
    }
    
    /**
     * Initialize session from stored data
     */
    suspend fun initializeSession() {
        val storedUser = sessionManager.getUser()
        val storedToken = sessionManager.getAuthToken()
        
        if (storedUser != null && storedToken != null) {
            _currentUser.value = User(
                id = storedUser.id,
                email = storedUser.email,
                firstName = storedUser.firstName,
                lastName = storedUser.lastName,
                isActive = true
            )
            _authToken.value = storedToken
            _isAuthenticated.value = true
        }
    }
    
    /**
     * Request password reset
     */
    suspend fun requestPasswordReset(email: String): Result<String> {
        return try {
            val result = authApiService.requestPasswordReset(email)
            result.fold(
                onSuccess = { resetResponse ->
                    Result.success(resetResponse.message)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check API health
     */
    suspend fun checkApiHealth(): Result<String> {
        return try {
            val result = authApiService.checkHealth()
            result.fold(
                onSuccess = { healthResponse ->
                    Result.success("API is ${healthResponse.status} - Database: ${healthResponse.database}")
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun handleAuthSuccess(authResponse: AuthResponse) {
        _authToken.value = authResponse.token
        _currentUser.value = mapToUser(authResponse)
        _isAuthenticated.value = true
        
        // Store session data for persistence
        sessionManager.saveAuthToken(authResponse.token)
        sessionManager.saveUser(authResponse.user)
    }
    
    private fun mapToUser(authResponse: AuthResponse): User {
        return User(
            id = authResponse.user.id,
            email = authResponse.user.email,
            firstName = authResponse.user.firstName,
            lastName = authResponse.user.lastName,
            isActive = true
        )
    }
}