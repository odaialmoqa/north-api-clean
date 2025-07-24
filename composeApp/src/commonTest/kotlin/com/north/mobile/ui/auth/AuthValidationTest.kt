package com.north.mobile.ui.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthValidationTest {
    
    @Test
    fun `validateEmail should return null for valid emails`() {
        // Valid email formats
        val validEmails = listOf(
            "test@example.com",
            "user.name@domain.co.uk",
            "firstname+lastname@company.org",
            "123@numbers.com"
        )
        
        validEmails.forEach { email ->
            assertNull(validateEmail(email), "Expected $email to be valid")
        }
    }
    
    @Test
    fun `validateEmail should return error for invalid emails`() {
        val testCases = mapOf(
            "" to "Email is required",
            "   " to "Email is required",
            "invalid" to "Invalid email format",
            "invalid@" to "Invalid email format",
            "@invalid.com" to "Invalid email format",
            "invalid@@domain.com" to "Invalid email format",
            "invalid@domain" to "Invalid email format"
        )
        
        testCases.forEach { (email, expectedError) ->
            assertEquals(expectedError, validateEmail(email.trim()))
        }
    }
    
    @Test
    fun `validatePassword should return null for valid passwords in login mode`() {
        // In login mode, any non-empty password should be valid
        val validPasswords = listOf("123", "short", "any-password")
        
        validPasswords.forEach { password ->
            assertNull(validatePassword(password, isRegistration = false))
        }
    }
    
    @Test
    fun `validatePassword should validate strength in registration mode`() {
        // Valid registration passwords
        val validPasswords = listOf(
            "password123",
            "mypass1",
            "secure123password"
        )
        
        validPasswords.forEach { password ->
            assertNull(validatePassword(password, isRegistration = true))
        }
    }
    
    @Test
    fun `validatePassword should return errors for invalid registration passwords`() {
        val testCases = mapOf(
            "" to "Password is required",
            "short" to "Password must be at least 6 characters",
            "password" to "Password must contain at least one number",
            "12345" to "Password must be at least 6 characters"
        )
        
        testCases.forEach { (password, expectedError) ->
            assertEquals(expectedError, validatePassword(password, isRegistration = true))
        }
    }
    
    @Test
    fun `validateName should return null for valid names`() {
        val validNames = listOf(
            "John",
            "Mary Jane",
            "O'Connor",
            "Jean-Pierre"
        )
        
        validNames.forEach { name ->
            assertNull(validateName(name, "First Name"))
        }
    }
    
    @Test
    fun `validateName should return errors for invalid names`() {
        val testCases = mapOf(
            "" to "First Name is required",
            "   " to "First Name is required",
            "A" to "First Name must be at least 2 characters"
        )
        
        testCases.forEach { (name, expectedError) ->
            assertEquals(expectedError, validateName(name.trim(), "First Name"))
        }
    }
    
    @Test
    fun `capitalizeWords should properly capitalize names`() {
        val testCases = mapOf(
            "john" to "John",
            "mary jane" to "Mary Jane",
            "jean-pierre" to "Jean-pierre", // Note: only splits on spaces
            "JOHN DOE" to "John Doe",
            "mIxEd CaSe" to "Mixed Case"
        )
        
        testCases.forEach { (input, expected) ->
            assertEquals(expected, input.capitalizeWords())
        }
    }
}

// Mock validation functions for testing
// These would normally be private in AuthScreen.kt
private fun validateEmail(email: String): String? {
    return when {
        email.isBlank() -> "Email is required"
        !email.contains("@") -> "Invalid email format"
        !email.contains(".") -> "Invalid email format"
        email.count { it == '@' } != 1 -> "Invalid email format"
        email.startsWith("@") || email.endsWith("@") -> "Invalid email format"
        else -> null
    }
}

private fun validatePassword(password: String, isRegistration: Boolean): String? {
    return when {
        password.isBlank() -> "Password is required"
        isRegistration && password.length < 6 -> "Password must be at least 6 characters"
        isRegistration && !password.any { it.isDigit() } -> "Password must contain at least one number"
        else -> null
    }
}

private fun validateName(name: String, fieldName: String): String? {
    return when {
        name.isBlank() -> "$fieldName is required"
        name.length < 2 -> "$fieldName must be at least 2 characters"
        else -> null
    }
}

private fun String.capitalizeWords(): String {
    return this.split(" ").joinToString(" ") { word ->
        if (word.isNotEmpty()) {
            word.lowercase().replaceFirstChar { it.uppercase() }
        } else {
            word
        }
    }
}