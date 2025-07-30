package com.north.mobile.data.user

import com.north.mobile.data.api.ApiClient
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val fullName: String = "$firstName $lastName"
)

@Serializable
data class UserProfileResponse(
    val success: Boolean,
    val user: UserProfile?,
    val error: String?
)

interface UserService {
    suspend fun getCurrentUser(): UserProfile?
    suspend fun updateProfile(firstName: String, lastName: String): Boolean
}

class UserServiceImpl(
    private val apiClient: ApiClient,
    private val getAuthToken: () -> String?
) : UserService {
    
    override suspend fun getCurrentUser(): UserProfile? {
        return try {
            val token = getAuthToken() ?: return null
            val response = apiClient.get<UserProfileResponse>("/api/user/profile", token)
            response.user
        } catch (e: Exception) {
            println("Failed to get user profile: ${e.message}")
            null
        }
    }
    
    override suspend fun updateProfile(firstName: String, lastName: String): Boolean {
        return try {
            val token = getAuthToken() ?: return false
            val response = apiClient.post<UserProfileResponse>(
                "/api/user/profile",
                mapOf(
                    "firstName" to firstName,
                    "lastName" to lastName
                ),
                token
            )
            response.success
        } catch (e: Exception) {
            println("Failed to update user profile: ${e.message}")
            false
        }
    }
}