package com.north.mobile.data.api

/**
 * API Configuration for North Mobile App
 */
object ApiConfig {
    // Update this URL to your deployed server
    const val BASE_URL = "https://north-api-clean-production.up.railway.app/api/"
    
    // For local development, use:
    // const val BASE_URL = "http://10.0.2.2:3000/api/" // Android emulator
    // const val BASE_URL = "http://localhost:3000/api/" // iOS simulator
    
    // API Endpoints
    object Endpoints {
        const val REGISTER = "auth/register"
        const val LOGIN = "auth/login"
        const val FORGOT_PASSWORD = "auth/forgot-password"
        
        // AI CFO Endpoints
        const val AI_CHAT = "ai/chat"
        const val AI_ONBOARDING_START = "ai/onboarding/start"
        
        // Financial Data
        const val FINANCIAL_SUMMARY = "financial/summary"
        const val GOALS = "goals"
        const val TRANSACTIONS = "transactions"
        
        // Health
        const val HEALTH = "../health" // Note: health is not under /api/
    }
    
    // Request timeouts
    const val CONNECT_TIMEOUT = 30L // seconds
    const val READ_TIMEOUT = 30L // seconds
    const val WRITE_TIMEOUT = 30L // seconds
    
    // Headers
    object Headers {
        const val AUTHORIZATION = "Authorization"
        const val CONTENT_TYPE = "Content-Type"
        const val ACCEPT = "Accept"
        
        const val APPLICATION_JSON = "application/json"
    }
}

/**
 * Environment-specific configuration
 */
object Environment {
    const val DEVELOPMENT = "development"
    const val PRODUCTION = "production"
    
    // Set this based on your build configuration
    const val CURRENT = DEVELOPMENT
    
    val baseUrl: String
        get() = when (CURRENT) {
            DEVELOPMENT -> "http://10.0.2.2:3000/api/" // Local development
            PRODUCTION -> "https://north-api-clean-production.up.railway.app/api/" // Production
            else -> ApiConfig.BASE_URL
        }
}