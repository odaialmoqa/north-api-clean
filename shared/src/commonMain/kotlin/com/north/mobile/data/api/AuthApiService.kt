package com.north.mobile.data.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Authentication API service for North backend
 */
class AuthApiService(private val apiClient: ApiClient) {
    
    /**
     * Register a new user
     */
    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<AuthResponse> {
        return try {
            val response = apiClient.httpClient.post("/api/auth/register") {
                setBody(RegisterRequest(email, password, firstName, lastName))
            }
            
            when (response.status) {
                HttpStatusCode.Created -> {
                    val authResponse = response.body<AuthResponse>()
                    Result.success(authResponse)
                }
                HttpStatusCode.Conflict -> {
                    Result.failure(Exception("User already exists"))
                }
                HttpStatusCode.BadRequest -> {
                    val error = response.body<ApiError>()
                    Result.failure(Exception(error.error))
                }
                else -> {
                    Result.failure(Exception("Registration failed: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Login user
     */
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = apiClient.httpClient.post("/api/auth/login") {
                setBody(LoginRequest(email, password))
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val authResponse = response.body<AuthResponse>()
                    Result.success(authResponse)
                }
                HttpStatusCode.Unauthorized -> {
                    Result.failure(Exception("Invalid credentials"))
                }
                HttpStatusCode.BadRequest -> {
                    val error = response.body<ApiError>()
                    Result.failure(Exception(error.error))
                }
                else -> {
                    Result.failure(Exception("Login failed: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check API health
     */
    suspend fun checkHealth(): Result<HealthResponse> {
        return try {
            val response = apiClient.httpClient.get("/health")
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val healthResponse = response.body<HealthResponse>()
                    Result.success(healthResponse)
                }
                else -> {
                    val healthResponse = response.body<HealthResponse>()
                    Result.failure(Exception("API unhealthy: ${healthResponse.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Request password reset
     */
    suspend fun requestPasswordReset(email: String): Result<PasswordResetResponse> {
        return try {
            val response = apiClient.httpClient.post("/api/auth/forgot-password") {
                setBody(PasswordResetRequest(email))
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val resetResponse = response.body<PasswordResetResponse>()
                    Result.success(resetResponse)
                }
                HttpStatusCode.NotFound -> {
                    Result.failure(Exception("Email address not found"))
                }
                HttpStatusCode.BadRequest -> {
                    val error = response.body<ApiError>()
                    Result.failure(Exception(error.error))
                }
                else -> {
                    Result.failure(Exception("Password reset failed: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get API info
     */
    suspend fun getApiInfo(): Result<ApiInfoResponse> {
        return try {
            val response = apiClient.httpClient.get("/api")
            val apiInfo = response.body<ApiInfoResponse>()
            Result.success(apiInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}