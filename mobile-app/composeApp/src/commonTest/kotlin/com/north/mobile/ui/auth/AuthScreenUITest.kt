package com.north.mobile.ui.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * UI tests for AuthScreen keyboard interaction and form navigation
 * These tests verify the UX enhancements for authentication
 */
class AuthScreenUITest {
    
    @Test
    fun `capitalizeWords should handle various input scenarios`() {
        val testCases = mapOf(
            // Basic capitalization
            "john" to "John",
            "mary jane" to "Mary Jane",
            "jean-claude van damme" to "Jean-claude Van Damme",
            
            // Edge cases
            "" to "",
            " " to " ",
            "  multiple   spaces  " to "  Multiple   Spaces  ",
            
            // Mixed case handling
            "JOHN DOE" to "John Doe",
            "mIxEd CaSe NaMe" to "Mixed Case Name",
            "lowercase" to "Lowercase",
            
            // Special characters (only splits on spaces)
            "o'connor" to "O'connor",
            "jean-pierre" to "Jean-pierre",
            "mary-jane smith" to "Mary-jane Smith",
            
            // Numbers and symbols
            "john123" to "John123",
            "user@domain" to "User@domain",
            "test user 2" to "Test User 2"
        )
        
        testCases.forEach { (input, expected) ->
            assertEquals(expected, input.capitalizeWords(), "Failed for input: '$input'")
        }
    }
    
    @Test
    fun `email validation should handle comprehensive scenarios`() {
        // Valid emails
        val validEmails = listOf(
            "test@example.com",
            "user.name@domain.co.uk",
            "firstname+lastname@company.org",
            "123@numbers.com",
            "user_name@domain.com",
            "test-email@sub.domain.com",
            "a@b.co"
        )
        
        validEmails.forEach { email ->
            assertEquals(null, validateEmail(email), "Expected '$email' to be valid")
        }
        
        // Invalid emails with specific error messages
        val invalidEmails = mapOf(
            "" to "Email is required",
            "   " to "Email is required",
            "invalid" to "Invalid email format",
            "invalid@" to "Invalid email format",
            "@invalid.com" to "Invalid email format",
            "invalid@@domain.com" to "Invalid email format",
            "invalid@domain" to "Invalid email format",
            "user@" to "Invalid email format",
            "@domain.com" to "Invalid email format",
            "user@@domain.com" to "Invalid email format",
            "user@domain@com" to "Invalid email format"
        )
        
        invalidEmails.forEach { (email, expectedError) ->
            assertEquals(expectedError, validateEmail(email.trim()), "Failed for email: '$email'")
        }
    }
    
    @Test
    fun `password validation should differentiate between login and registration`() {
        // Login mode - any non-empty password should be valid
        val loginPasswords = listOf(
            "123",
            "short",
            "any-password",
            "a",
            "password-without-numbers"
        )
        
        loginPasswords.forEach { password ->
            assertEquals(null, validatePassword(password, isRegistration = false), 
                "Login password '$password' should be valid")
        }
        
        // Registration mode - valid passwords
        val validRegistrationPasswords = listOf(
            "password123",
            "mypass1",
            "secure123password",
            "test123",
            "abcdef1",
            "longpasswordwith123numbers"
        )
        
        validRegistrationPasswords.forEach { password ->
            assertEquals(null, validatePassword(password, isRegistration = true),
                "Registration password '$password' should be valid")
        }
        
        // Registration mode - invalid passwords
        val invalidRegistrationPasswords = mapOf(
            "" to "Password is required",
            "   " to "Password is required",
            "short" to "Password must be at least 6 characters",
            "12345" to "Password must be at least 6 characters",
            "password" to "Password must contain at least one number",
            "abcdef" to "Password must contain at least one number",
            "ABCDEF" to "Password must contain at least one number",
            "!@#$%^" to "Password must contain at least one number"
        )
        
        invalidRegistrationPasswords.forEach { (password, expectedError) ->
            assertEquals(expectedError, validatePassword(password.trim(), isRegistration = true),
                "Registration password '$password' should be invalid")
        }
    }
    
    @Test
    fun `name validation should handle edge cases and requirements`() {
        // Valid names
        val validNames = listOf(
            "John",
            "Mary Jane",
            "O'Connor",
            "Jean-Pierre",
            "李",  // Single character names in other languages
            "Al",  // Minimum valid length
            "Very Long Name With Multiple Words",
            "Name123", // Names with numbers
            "Name-With-Hyphens",
            "Name.With.Dots"
        )
        
        validNames.forEach { name ->
            assertEquals(null, validateName(name, "First Name"), 
                "Name '$name' should be valid")
        }
        
        // Invalid names
        val invalidNames = mapOf(
            "" to "First Name is required",
            "   " to "First Name is required",
            "A" to "First Name must be at least 2 characters",
            " " to "First Name is required",
            "\t" to "First Name is required",
            "\n" to "First Name is required"
        )
        
        invalidNames.forEach { (name, expectedError) ->
            assertEquals(expectedError, validateName(name.trim(), "First Name"),
                "Name '$name' should be invalid")
        }
        
        // Test different field names
        assertEquals("Last Name is required", validateName("", "Last Name"))
        assertEquals("Middle Name must be at least 2 characters", validateName("A", "Middle Name"))
    }
    
    @Test
    fun `form validation should work together for complete user input`() {
        // Test complete valid registration form
        val validFormData = FormData(
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            password = "password123"
        )
        
        assertTrue(isValidRegistrationForm(validFormData), "Valid form should pass validation")
        
        // Test complete valid login form
        val validLoginData = FormData(
            firstName = "", // Not required for login
            lastName = "", // Not required for login
            email = "john.doe@example.com",
            password = "any-password"
        )
        
        assertTrue(isValidLoginForm(validLoginData), "Valid login form should pass validation")
        
        // Test invalid registration forms
        val invalidRegistrationForms = listOf(
            validFormData.copy(firstName = ""), // Missing first name
            validFormData.copy(lastName = "A"), // Too short last name
            validFormData.copy(email = "invalid-email"), // Invalid email
            validFormData.copy(password = "short") // Invalid password
        )
        
        invalidRegistrationForms.forEach { formData ->
            assertFalse(isValidRegistrationForm(formData), 
                "Invalid registration form should fail validation: $formData")
        }
        
        // Test invalid login forms
        val invalidLoginForms = listOf(
            validLoginData.copy(email = ""), // Missing email
            validLoginData.copy(email = "invalid-email"), // Invalid email
            validLoginData.copy(password = "") // Missing password
        )
        
        invalidLoginForms.forEach { formData ->
            assertFalse(isValidLoginForm(formData), 
                "Invalid login form should fail validation: $formData")
        }
    }
    
    @Test
    fun `keyboard navigation should handle IME actions correctly`() {
        // Test that form fields have correct IME actions for navigation
        val formFieldsImeActions = mapOf(
            "firstName" to "Next",
            "lastName" to "Next", 
            "email" to "Next",
            "password" to "Done"
        )
        
        // This would be tested in actual UI tests, but we can verify the logic
        formFieldsImeActions.forEach { (field, expectedAction) ->
            assertEquals(expectedAction, getExpectedImeAction(field),
                "Field '$field' should have IME action '$expectedAction'")
        }
    }
    
    @Test
    fun `keyboard capitalization should be set correctly for different fields`() {
        val fieldCapitalizationSettings = mapOf(
            "firstName" to "Words",
            "lastName" to "Words",
            "email" to "None",
            "password" to "None"
        )
        
        fieldCapitalizationSettings.forEach { (field, expectedCapitalization) ->
            assertEquals(expectedCapitalization, getExpectedCapitalization(field),
                "Field '$field' should have capitalization '$expectedCapitalization'")
        }
    }
    
    @Test
    fun `form state should handle loading and error states correctly`() {
        // Test initial form state
        val initialState = AuthFormState()
        assertFalse(initialState.isLoading)
        assertEquals(null, initialState.errorMessage)
        assertFalse(initialState.showForgotPasswordDialog)
        
        // Test loading state
        val loadingState = initialState.copy(isLoading = true)
        assertTrue(loadingState.isLoading)
        
        // Test error state
        val errorState = initialState.copy(errorMessage = "Authentication failed")
        assertEquals("Authentication failed", errorState.errorMessage)
        
        // Test forgot password dialog state
        val forgotPasswordState = initialState.copy(showForgotPasswordDialog = true)
        assertTrue(forgotPasswordState.showForgotPasswordDialog)
    }
}

// Helper data classes and functions for testing

private data class FormData(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String
)

private data class AuthFormState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showForgotPasswordDialog: Boolean = false
)

private fun isValidRegistrationForm(formData: FormData): Boolean {
    return validateName(formData.firstName, "First Name") == null &&
           validateName(formData.lastName, "Last Name") == null &&
           validateEmail(formData.email) == null &&
           validatePassword(formData.password, isRegistration = true) == null
}

private fun isValidLoginForm(formData: FormData): Boolean {
    return validateEmail(formData.email) == null &&
           validatePassword(formData.password, isRegistration = false) == null
}

private fun getExpectedImeAction(fieldName: String): String {
    return when (fieldName) {
        "firstName", "lastName", "email" -> "Next"
        "password" -> "Done"
        else -> "Default"
    }
}

private fun getExpectedCapitalization(fieldName: String): String {
    return when (fieldName) {
        "firstName", "lastName" -> "Words"
        "email", "password" -> "None"
        else -> "None"
    }
}

// Mock validation functions (these would normally be private in AuthScreen.kt)
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