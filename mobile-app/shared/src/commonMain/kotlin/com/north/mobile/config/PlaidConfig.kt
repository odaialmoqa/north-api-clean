package com.north.mobile.config

import com.north.mobile.data.plaid.PlaidEnvironment

object PlaidConfig {
    // Production configuration
    const val CLIENT_ID = "5fdecaa7df1def0013986738"
    const val PRODUCTION_SECRET = "370ff905f8cafc934b6b1da256e729" // Replace with actual secret
    const val SANDBOX_SECRET = "084141a287c71fd8f75cdc71c796b1" // Keep for testing
    
    // Current environment - change this to switch between sandbox and production
    val CURRENT_ENVIRONMENT = PlaidEnvironment.PRODUCTION
    
    val currentSecret: String
        get() = when (CURRENT_ENVIRONMENT) {
            PlaidEnvironment.PRODUCTION -> PRODUCTION_SECRET
            PlaidEnvironment.SANDBOX -> SANDBOX_SECRET
            PlaidEnvironment.DEVELOPMENT -> SANDBOX_SECRET // Use sandbox for development
        }
    
    val baseUrl: String
        get() = when (CURRENT_ENVIRONMENT) {
            PlaidEnvironment.PRODUCTION -> "https://production.plaid.com"
            PlaidEnvironment.SANDBOX -> "https://sandbox.plaid.com"
            PlaidEnvironment.DEVELOPMENT -> "https://development.plaid.com"
        }
}