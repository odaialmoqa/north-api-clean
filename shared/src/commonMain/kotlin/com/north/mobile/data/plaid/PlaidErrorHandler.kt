package com.north.mobile.data.plaid

object PlaidErrorHandler {
    
    /**
     * Convert Plaid errors to user-friendly messages
     */
    fun getDisplayMessage(error: PlaidError): String {
        return error.displayMessage ?: when (error.errorCode) {
            // Authentication errors
            "ITEM_LOGIN_REQUIRED" -> "Please re-authenticate with your bank to continue syncing your accounts."
            "INVALID_CREDENTIALS" -> "The login credentials you provided are incorrect. Please try again."
            "INVALID_MFA" -> "The verification code you entered is incorrect. Please try again."
            "INVALID_SEND_METHOD" -> "Unable to send verification code. Please try a different method."
            "ITEM_LOCKED" -> "Your account has been locked by your bank. Please contact your bank to unlock it."
            "USER_SETUP_REQUIRED" -> "Additional setup is required with your bank. Please log into your bank's website first."
            
            // Institution errors
            "INSTITUTION_DOWN" -> "Your bank's systems are temporarily unavailable. Please try again later."
            "INSTITUTION_NOT_RESPONDING" -> "Your bank is not responding. Please try again in a few minutes."
            "INSTITUTION_NO_LONGER_SUPPORTED" -> "This bank is no longer supported. Please contact support for alternatives."
            
            // Rate limiting
            "RATE_LIMIT_EXCEEDED" -> "Too many requests. Please wait a moment and try again."
            
            // Product access
            "INSUFFICIENT_CREDENTIALS" -> "Unable to access your account information. Please verify your login credentials."
            "INVALID_PRODUCT" -> "This feature is not available for your account type."
            "UNAUTHORIZED_ENVIRONMENT" -> "This service is not available in your region."
            
            // API errors
            "INVALID_REQUEST" -> "There was an error with your request. Please try again."
            "INVALID_INPUT" -> "Invalid information provided. Please check your input and try again."
            "MISSING_FIELDS" -> "Required information is missing. Please complete all fields."
            "UNKNOWN_FIELDS" -> "Invalid information provided. Please try again."
            "INVALID_FIELD" -> "One or more fields contain invalid information."
            
            // Item errors
            "ITEM_NOT_FOUND" -> "Account connection not found. Please reconnect your account."
            "ACCESS_NOT_GRANTED" -> "Access to your account was not granted. Please try connecting again."
            "CONSENT_REVOKED" -> "Access to your account has been revoked. Please reconnect to continue."
            "ITEM_NO_ERROR" -> "Connection is healthy."
            
            // Network/connectivity
            "INTERNAL_SERVER_ERROR" -> "Our servers are experiencing issues. Please try again later."
            "PLANNED_MAINTENANCE" -> "We're performing scheduled maintenance. Please try again later."
            
            // Default fallback
            else -> error.errorMessage.takeIf { it.isNotBlank() } 
                ?: "An unexpected error occurred. Please try again or contact support if the problem persists."
        }
    }
    
    /**
     * Determine if an error is recoverable (user can retry)
     */
    fun isRecoverable(error: PlaidError): Boolean {
        return when (error.errorCode) {
            // Recoverable - user can retry
            "INVALID_CREDENTIALS",
            "INVALID_MFA",
            "INVALID_SEND_METHOD",
            "INSTITUTION_DOWN",
            "INSTITUTION_NOT_RESPONDING",
            "RATE_LIMIT_EXCEEDED",
            "INTERNAL_SERVER_ERROR",
            "PLANNED_MAINTENANCE" -> true
            
            // Not recoverable - requires different action
            "ITEM_LOGIN_REQUIRED",
            "ITEM_LOCKED",
            "USER_SETUP_REQUIRED",
            "INSTITUTION_NO_LONGER_SUPPORTED",
            "UNAUTHORIZED_ENVIRONMENT",
            "ITEM_NOT_FOUND",
            "ACCESS_NOT_GRANTED",
            "CONSENT_REVOKED" -> false
            
            // Default to recoverable for unknown errors
            else -> true
        }
    }
    
    /**
     * Determine if an error requires re-authentication
     */
    fun requiresReauth(error: PlaidError): Boolean {
        return when (error.errorCode) {
            "ITEM_LOGIN_REQUIRED",
            "INVALID_CREDENTIALS",
            "CONSENT_REVOKED" -> true
            else -> false
        }
    }
    
    /**
     * Get suggested action for the user
     */
    fun getSuggestedAction(error: PlaidError): String {
        return when (error.errorCode) {
            "ITEM_LOGIN_REQUIRED" -> "Tap 'Reconnect' to re-authenticate with your bank."
            "INVALID_CREDENTIALS" -> "Double-check your username and password, then try again."
            "INVALID_MFA" -> "Enter the correct verification code from your bank."
            "ITEM_LOCKED" -> "Contact your bank to unlock your account, then try reconnecting."
            "USER_SETUP_REQUIRED" -> "Log into your bank's website first, then try connecting again."
            "INSTITUTION_DOWN" -> "Wait a few minutes and try again."
            "RATE_LIMIT_EXCEEDED" -> "Wait a moment before trying again."
            "INSTITUTION_NO_LONGER_SUPPORTED" -> "Contact support for help connecting this bank."
            else -> "Try again, or contact support if the problem continues."
        }
    }
    
    /**
     * Categorize error for analytics/monitoring
     */
    fun getErrorCategory(error: PlaidError): ErrorCategory {
        return when (error.errorType) {
            "INVALID_REQUEST" -> ErrorCategory.CLIENT_ERROR
            "INVALID_INPUT" -> ErrorCategory.CLIENT_ERROR
            "INSTITUTION_ERROR" -> ErrorCategory.INSTITUTION_ERROR
            "API_ERROR" -> ErrorCategory.SERVER_ERROR
            "ITEM_ERROR" -> when (error.errorCode) {
                "ITEM_LOGIN_REQUIRED", "INVALID_CREDENTIALS" -> ErrorCategory.AUTH_ERROR
                "CONSENT_REVOKED", "ACCESS_NOT_GRANTED" -> ErrorCategory.PERMISSION_ERROR
                else -> ErrorCategory.ITEM_ERROR
            }
            "RATE_LIMIT_EXCEEDED" -> ErrorCategory.RATE_LIMIT_ERROR
            else -> ErrorCategory.UNKNOWN_ERROR
        }
    }
}

enum class ErrorCategory {
    CLIENT_ERROR,
    SERVER_ERROR,
    AUTH_ERROR,
    PERMISSION_ERROR,
    INSTITUTION_ERROR,
    ITEM_ERROR,
    RATE_LIMIT_ERROR,
    UNKNOWN_ERROR
}