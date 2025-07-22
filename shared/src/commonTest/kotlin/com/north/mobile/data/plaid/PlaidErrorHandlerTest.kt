package com.north.mobile.data.plaid

import kotlin.test.*

class PlaidErrorHandlerTest {
    
    @Test
    fun testGetDisplayMessage_ItemLoginRequired() {
        val error = PlaidError(
            errorType = "ITEM_ERROR",
            errorCode = "ITEM_LOGIN_REQUIRED",
            errorMessage = "the login details of this item have changed",
            displayMessage = null,
            requestId = "test-request-id"
        )
        
        val displayMessage = PlaidErrorHandler.getDisplayMessage(error)
        assertEquals(
            "Please re-authenticate with your bank to continue syncing your accounts.",
            displayMessage
        )
    }
    
    @Test
    fun testGetDisplayMessage_InvalidCredentials() {
        val error = PlaidError(
            errorType = "ITEM_ERROR",
            errorCode = "INVALID_CREDENTIALS",
            errorMessage = "the provided credentials were not correct",
            displayMessage = null,
            requestId = "test-request-id"
        )
        
        val displayMessage = PlaidErrorHandler.getDisplayMessage(error)
        assertEquals(
            "The login credentials you provided are incorrect. Please try again.",
            displayMessage
        )
    }
    
    @Test
    fun testGetDisplayMessage_InstitutionDown() {
        val error = PlaidError(
            errorType = "INSTITUTION_ERROR",
            errorCode = "INSTITUTION_DOWN",
            errorMessage = "the institution is currently down",
            displayMessage = null,
            requestId = "test-request-id"
        )
        
        val displayMessage = PlaidErrorHandler.getDisplayMessage(error)
        assertEquals(
            "Your bank's systems are temporarily unavailable. Please try again later.",
            displayMessage
        )
    }
    
    @Test
    fun testGetDisplayMessage_UsesProvidedDisplayMessage() {
        val error = PlaidError(
            errorType = "ITEM_ERROR",
            errorCode = "SOME_ERROR",
            errorMessage = "technical error message",
            displayMessage = "Custom user-friendly message",
            requestId = "test-request-id"
        )
        
        val displayMessage = PlaidErrorHandler.getDisplayMessage(error)
        assertEquals("Custom user-friendly message", displayMessage)
    }
    
    @Test
    fun testIsRecoverable_RecoverableErrors() {
        val recoverableErrors = listOf(
            "INVALID_CREDENTIALS",
            "INVALID_MFA",
            "INSTITUTION_DOWN",
            "RATE_LIMIT_EXCEEDED",
            "INTERNAL_SERVER_ERROR"
        )
        
        recoverableErrors.forEach { errorCode ->
            val error = PlaidError(
                errorType = "TEST_ERROR",
                errorCode = errorCode,
                errorMessage = "test message",
                displayMessage = null,
                requestId = null
            )
            
            assertTrue(
                PlaidErrorHandler.isRecoverable(error),
                "Error $errorCode should be recoverable"
            )
        }
    }
    
    @Test
    fun testIsRecoverable_NonRecoverableErrors() {
        val nonRecoverableErrors = listOf(
            "ITEM_LOGIN_REQUIRED",
            "ITEM_LOCKED",
            "USER_SETUP_REQUIRED",
            "INSTITUTION_NO_LONGER_SUPPORTED",
            "UNAUTHORIZED_ENVIRONMENT"
        )
        
        nonRecoverableErrors.forEach { errorCode ->
            val error = PlaidError(
                errorType = "TEST_ERROR",
                errorCode = errorCode,
                errorMessage = "test message",
                displayMessage = null,
                requestId = null
            )
            
            assertFalse(
                PlaidErrorHandler.isRecoverable(error),
                "Error $errorCode should not be recoverable"
            )
        }
    }
    
    @Test
    fun testRequiresReauth() {
        val reauthErrors = listOf(
            "ITEM_LOGIN_REQUIRED",
            "INVALID_CREDENTIALS",
            "CONSENT_REVOKED"
        )
        
        reauthErrors.forEach { errorCode ->
            val error = PlaidError(
                errorType = "TEST_ERROR",
                errorCode = errorCode,
                errorMessage = "test message",
                displayMessage = null,
                requestId = null
            )
            
            assertTrue(
                PlaidErrorHandler.requiresReauth(error),
                "Error $errorCode should require re-authentication"
            )
        }
        
        // Test non-reauth error
        val nonReauthError = PlaidError(
            errorType = "INSTITUTION_ERROR",
            errorCode = "INSTITUTION_DOWN",
            errorMessage = "test message",
            displayMessage = null,
            requestId = null
        )
        
        assertFalse(PlaidErrorHandler.requiresReauth(nonReauthError))
    }
    
    @Test
    fun testGetSuggestedAction() {
        val error = PlaidError(
            errorType = "ITEM_ERROR",
            errorCode = "ITEM_LOGIN_REQUIRED",
            errorMessage = "login required",
            displayMessage = null,
            requestId = null
        )
        
        val suggestedAction = PlaidErrorHandler.getSuggestedAction(error)
        assertEquals("Tap 'Reconnect' to re-authenticate with your bank.", suggestedAction)
    }
    
    @Test
    fun testGetErrorCategory() {
        val testCases = mapOf(
            "INVALID_REQUEST" to ErrorCategory.CLIENT_ERROR,
            "INVALID_INPUT" to ErrorCategory.CLIENT_ERROR,
            "INSTITUTION_ERROR" to ErrorCategory.INSTITUTION_ERROR,
            "API_ERROR" to ErrorCategory.SERVER_ERROR,
            "RATE_LIMIT_EXCEEDED" to ErrorCategory.RATE_LIMIT_ERROR
        )
        
        testCases.forEach { (errorType, expectedCategory) ->
            val error = PlaidError(
                errorType = errorType,
                errorCode = "TEST_CODE",
                errorMessage = "test message",
                displayMessage = null,
                requestId = null
            )
            
            assertEquals(
                expectedCategory,
                PlaidErrorHandler.getErrorCategory(error),
                "Error type $errorType should map to category $expectedCategory"
            )
        }
    }
    
    @Test
    fun testGetErrorCategory_ItemErrors() {
        val authError = PlaidError(
            errorType = "ITEM_ERROR",
            errorCode = "ITEM_LOGIN_REQUIRED",
            errorMessage = "test message",
            displayMessage = null,
            requestId = null
        )
        
        assertEquals(ErrorCategory.AUTH_ERROR, PlaidErrorHandler.getErrorCategory(authError))
        
        val permissionError = PlaidError(
            errorType = "ITEM_ERROR",
            errorCode = "CONSENT_REVOKED",
            errorMessage = "test message",
            displayMessage = null,
            requestId = null
        )
        
        assertEquals(ErrorCategory.PERMISSION_ERROR, PlaidErrorHandler.getErrorCategory(permissionError))
        
        val genericItemError = PlaidError(
            errorType = "ITEM_ERROR",
            errorCode = "SOME_OTHER_ERROR",
            errorMessage = "test message",
            displayMessage = null,
            requestId = null
        )
        
        assertEquals(ErrorCategory.ITEM_ERROR, PlaidErrorHandler.getErrorCategory(genericItemError))
    }
}