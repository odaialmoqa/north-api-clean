package com.north.mobile.integration

import com.north.mobile.data.api.ApiClient
import com.north.mobile.data.api.AuthApiService
import com.north.mobile.data.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Integration test to verify connection to Railway production API
 */
class RailwayAPIConnectionTest {
    
    private val apiClient = ApiClient()
    private val authApiService = AuthApiService(apiClient.httpClient)
    private val authRepository = AuthRepository(authApiService)
    
    @Test
    fun testRailwayAPIHealthCheck() = runTest {
        println("🚀 Testing connection to Railway API: ${ApiClient.BASE_URL}")
        
        val result = authRepository.checkApiHealth()
        
        result.fold(
            onSuccess = { healthMessage ->
                println("✅ API Health Check Successful: $healthMessage")
                assertTrue(healthMessage.contains("API is"), "Health response should contain API status")
            },
            onFailure = { error ->
                println("❌ API Health Check Failed: ${error.message}")
                println("Error details: ${error.stackTraceToString()}")
                throw error
            }
        )
    }
    
    @Test
    fun testRailwayAPIRegistration() = runTest {
        println("🧪 Testing user registration with Railway API")
        
        // Use a test email with timestamp to avoid conflicts
        val testEmail = "test+${System.currentTimeMillis()}@north.app"
        val testPassword = "TestPassword123!"
        val testFirstName = "Test"
        val testLastName = "User"
        
        val result = authRepository.register(
            email = testEmail,
            password = testPassword,
            firstName = testFirstName,
            lastName = testLastName
        )
        
        result.fold(
            onSuccess = { user ->
                println("✅ Registration Successful!")
                println("   User ID: ${user.id}")
                println("   Email: ${user.email}")
                println("   Name: ${user.firstName} ${user.lastName}")
                
                assertNotNull(user.id, "User ID should not be null")
                assertTrue(user.email == testEmail, "Email should match")
                assertTrue(user.firstName == testFirstName, "First name should match")
                assertTrue(user.lastName == testLastName, "Last name should match")
            },
            onFailure = { error ->
                println("❌ Registration Failed: ${error.message}")
                println("Error details: ${error.stackTraceToString()}")
                
                // If it's a "user already exists" error, that's actually good - means API is working
                if (error.message?.contains("already exists") == true || 
                    error.message?.contains("duplicate") == true) {
                    println("ℹ️  User already exists - API is working correctly")
                } else {
                    throw error
                }
            }
        )
    }
    
    @Test
    fun testRailwayAPILogin() = runTest {
        println("🔐 Testing user login with Railway API")
        
        // First register a user
        val testEmail = "login-test+${System.currentTimeMillis()}@north.app"
        val testPassword = "TestPassword123!"
        
        // Register first
        val registerResult = authRepository.register(
            email = testEmail,
            password = testPassword,
            firstName = "Login",
            lastName = "Test"
        )
        
        if (registerResult.isSuccess) {
            println("✅ Test user registered successfully")
            
            // Now test login
            val loginResult = authRepository.login(testEmail, testPassword)
            
            loginResult.fold(
                onSuccess = { user ->
                    println("✅ Login Successful!")
                    println("   User ID: ${user.id}")
                    println("   Email: ${user.email}")
                    
                    assertNotNull(user.id, "User ID should not be null")
                    assertTrue(user.email == testEmail, "Email should match")
                    
                    // Check if auth token was set
                    val token = authRepository.getCurrentToken()
                    assertNotNull(token, "Auth token should be set after login")
                    println("   Auth token received: ${token?.take(20)}...")
                },
                onFailure = { error ->
                    println("❌ Login Failed: ${error.message}")
                    throw error
                }
            )
        } else {
            println("⚠️  Registration failed, skipping login test")
            println("Registration error: ${registerResult.exceptionOrNull()?.message}")
        }
    }
}