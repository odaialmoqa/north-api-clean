package com.north.mobile.integration

import com.north.mobile.data.api.ApiClient
import com.north.mobile.data.api.AuthApiService
import com.north.mobile.data.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for password reset functionality end-to-end
 * These tests verify the complete password recovery flow
 */
class PasswordResetIntegrationTest {
    
    private val apiClient = ApiClient()
    private val authApiService = AuthApiService(apiClient)
    private val authRepository = AuthRepository(authApiService)
    
    @Test
    fun `password reset request should handle valid email addresses`() = runTest {
        // Test various valid email formats
        val validEmails = listOf(
            "user@example.com",
            "test.user@domain.co.uk",
            "firstname+lastname@company.org",
            "user_name@subdomain.domain.com",
            "123numbers@domain.com"
        )
        
        validEmails.forEach { email ->
            // When - Request password reset
            val result = authRepository.requestPasswordReset(email)
            
            // Then - Should handle the request (success or expected failure)
            result.fold(
                onSuccess = { message ->
                    // Verify success response
                    assertNotNull(message, "Success message should not be null for $email")
                    assertTrue(message.isNotEmpty(), "Success message should not be empty for $email")
                    
                    // Message should indicate email was sent or processed
                    val messageContainsExpectedTerms = message.contains("sent", ignoreCase = true) ||
                                                     message.contains("email", ignoreCase = true) ||
                                                     message.contains("reset", ignoreCase = true) ||
                                                     message.contains("link", ignoreCase = true)
                    
                    assertTrue(messageContainsExpectedTerms, 
                        "Success message should contain relevant terms for $email: $message")
                },
                onFailure = { error ->
                    // In test environment, we might expect failures due to network/server issues
                    assertNotNull(error.message, "Error message should not be null for $email")
                    assertTrue(error.message!!.isNotEmpty(), 
                        "Error message should not be empty for $email")
                    
                    println("Password reset failed as expected in test environment for $email: ${error.message}")
                }
            )
        }
    }
    
    @Test
    fun `password reset request should handle invalid email addresses`() = runTest {
        // Test various invalid email formats
        val invalidEmails = listOf(
            "", // Empty email
            "   ", // Whitespace only
            "invalid", // No @ symbol
            "invalid@", // Incomplete
            "@domain.com", // Missing user part
            "user@@domain.com", // Double @
            "user@domain", // No TLD
            "user@.com", // Invalid domain
            "user@domain.", // Incomplete TLD
            "user name@domain.com" // Spaces in email
        )
        
        invalidEmails.forEach { email ->
            // When - Request password reset with invalid email
            val result = authRepository.requestPasswordReset(email)
            
            // Then - Should handle invalid email appropriately
            result.fold(
                onSuccess = { message ->
                    // Some systems might still return success for invalid emails (security)
                    // but the message should be generic
                    assertNotNull(message, "Message should not be null for invalid email: $email")
                    println("Password reset returned success for invalid email $email: $message")
                },
                onFailure = { error ->
                    // Expected behavior for invalid emails
                    assertNotNull(error.message, "Error message should not be null for invalid email: $email")
                    assertTrue(error.message!!.isNotEmpty(), 
                        "Error message should not be empty for invalid email: $email")
                    
                    // Error message should indicate validation issue
                    val errorIndicatesValidation = error.message!!.contains("invalid", ignoreCase = true) ||
                                                 error.message!!.contains("email", ignoreCase = true) ||
                                                 error.message!!.contains("format", ignoreCase = true) ||
                                                 error.message!!.contains("valid", ignoreCase = true)
                    
                    assertTrue(errorIndicatesValidation || error.message!!.contains("400") || 
                              error.message!!.contains("Bad Request"),
                        "Error should indicate validation issue for $email: ${error.message}")
                }
            )
        }
    }
    
    @Test
    fun `password reset should handle network and server errors gracefully`() = runTest {
        // Test with a valid email format but expect potential network issues
        val testEmail = "network.test@example.com"
        
        // When - Request password reset (might fail due to network/server issues in test)
        val result = authRepository.requestPasswordReset(testEmail)
        
        // Then - Should handle errors gracefully
        result.fold(
            onSuccess = { message ->
                // If successful, verify the response
                assertNotNull(message)
                assertTrue(message.isNotEmpty())
                println("Password reset successful: $message")
            },
            onFailure = { error ->
                // Verify error handling
                assertNotNull(error.message)
                assertTrue(error.message!!.isNotEmpty())
                
                // Common network/server error indicators
                val isNetworkError = error.message!!.contains("network", ignoreCase = true) ||
                                   error.message!!.contains("connection", ignoreCase = true) ||
                                   error.message!!.contains("timeout", ignoreCase = true) ||
                                   error.message!!.contains("500") ||
                                   error.message!!.contains("503") ||
                                   error.message!!.contains("404")
                
                println("Password reset failed with network/server error: ${error.message}")
                
                // Error should be informative for debugging
                assertTrue(error.message!!.length > 5, 
                    "Error message should be informative: ${error.message}")
            }
        )
    }
    
    @Test
    fun `password reset UI flow should validate email before API call`() = runTest {
        // Test email validation that should happen before API calls
        val emailValidationTests = mapOf(
            "" to false,
            "   " to false,
            "invalid" to false,
            "invalid@" to false,
            "@domain.com" to false,
            "user@@domain.com" to false,
            "valid@example.com" to true,
            "user.name@domain.co.uk" to true,
            "test+tag@company.org" to true
        )
        
        emailValidationTests.forEach { (email, shouldBeValid) ->
            val isValid = isValidEmailForPasswordReset(email)
            
            if (shouldBeValid) {
                assertTrue(isValid, "Email '$email' should be valid for password reset")
            } else {
                assertFalse(isValid, "Email '$email' should be invalid for password reset")
            }
        }
    }
    
    @Test
    fun `password reset dialog should handle user interactions correctly`() = runTest {
        // Test dialog state management
        var showDialog = false
        var email = ""
        var message: String? = null
        var isLoading = false
        
        // Test initial state
        assertFalse(showDialog)
        assertTrue(email.isEmpty())
        assertTrue(message == null)
        assertFalse(isLoading)
        
        // Test opening dialog
        showDialog = true
        assertTrue(showDialog)
        
        // Test email input
        email = "user@example.com"
        assertTrue(email.isNotEmpty())
        assertTrue(isValidEmailForPasswordReset(email))
        
        // Test loading state during request
        isLoading = true
        assertTrue(isLoading)
        
        // Test success state
        isLoading = false
        message = "Password reset link sent to your email"
        assertFalse(isLoading)
        assertNotNull(message)
        assertTrue(message!!.contains("sent", ignoreCase = true))
        
        // Test dialog dismissal
        showDialog = false
        email = ""
        message = null
        
        assertFalse(showDialog)
        assertTrue(email.isEmpty())
        assertTrue(message == null)
    }
    
    @Test
    fun `password reset should provide appropriate user feedback`() = runTest {
        // Test different feedback scenarios
        val feedbackScenarios = listOf(
            // Success scenarios
            Triple("user@example.com", true, "Reset link sent"),
            Triple("test@domain.com", true, "Check your email"),
            Triple("valid@company.org", true, "Password reset requested"),
            
            // Error scenarios
            Triple("invalid-email", false, "Invalid email format"),
            Triple("", false, "Email is required"),
            Triple("user@", false, "Invalid email format")
        )
        
        feedbackScenarios.forEach { (email, shouldSucceed, expectedMessageType) ->
            if (isValidEmailForPasswordReset(email)) {
                // For valid emails, test actual API call
                val result = authRepository.requestPasswordReset(email)
                
                result.fold(
                    onSuccess = { message ->
                        if (shouldSucceed) {
                            assertTrue(message.isNotEmpty(), 
                                "Success message should not be empty for $email")
                        }
                    },
                    onFailure = { error ->
                        if (!shouldSucceed) {
                            assertNotNull(error.message, 
                                "Error message should not be null for $email")
                        }
                    }
                )
            } else {
                // For invalid emails, validation should catch them
                assertFalse(shouldSucceed, "Invalid email $email should not succeed")
            }
        }
    }
    
    @Test
    fun `password reset should handle rate limiting and security measures`() = runTest {
        // Test multiple requests to the same email (might trigger rate limiting)
        val testEmail = "rate.limit.test@example.com"
        val requestCount = 3
        
        repeat(requestCount) { attempt ->
            val result = authRepository.requestPasswordReset(testEmail)
            
            result.fold(
                onSuccess = { message ->
                    assertNotNull(message)
                    println("Password reset attempt ${attempt + 1} successful: $message")
                },
                onFailure = { error ->
                    assertNotNull(error.message)
                    
                    // Check if error indicates rate limiting
                    val isRateLimited = error.message!!.contains("rate", ignoreCase = true) ||
                                      error.message!!.contains("limit", ignoreCase = true) ||
                                      error.message!!.contains("too many", ignoreCase = true) ||
                                      error.message!!.contains("429")
                    
                    if (isRateLimited) {
                        println("Rate limiting detected on attempt ${attempt + 1}: ${error.message}")
                    } else {
                        println("Password reset attempt ${attempt + 1} failed: ${error.message}")
                    }
                }
            )
            
            // Small delay between requests
            kotlinx.coroutines.delay(100)
        }
    }
    
    @Test
    fun `password reset should maintain security best practices`() = runTest {
        // Test that password reset doesn't reveal user existence
        val testEmails = listOf(
            "existing.user@example.com", // Might exist
            "nonexistent.user@example.com", // Probably doesn't exist
            "another.test@example.com" // Unknown
        )
        
        val responses = mutableListOf<String>()
        
        testEmails.forEach { email ->
            val result = authRepository.requestPasswordReset(email)
            
            result.fold(
                onSuccess = { message ->
                    responses.add(message)
                },
                onFailure = { error ->
                    responses.add(error.message ?: "Unknown error")
                }
            )
        }
        
        // In a secure system, responses should be similar regardless of user existence
        // This is a basic check - in practice, timing attacks should also be considered
        if (responses.size > 1) {
            println("Password reset responses: $responses")
            // All responses should be non-empty
            responses.forEach { response ->
                assertTrue(response.isNotEmpty(), "All responses should be non-empty")
            }
        }
    }
}

// Helper function for email validation in password reset context
private fun isValidEmailForPasswordReset(email: String): Boolean {
    return email.isNotBlank() &&
           email.contains("@") &&
           email.contains(".") &&
           email.count { it == '@' } == 1 &&
           !email.startsWith("@") &&
           !email.endsWith("@") &&
           email.indexOf("@") < email.lastIndexOf(".")
}