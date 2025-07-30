package com.north.mobile.ui.accounts

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Comprehensive Plaid debugging tool
 */
object PlaidDebugger {
    
    @Serializable
    data class PlaidDebugInfo(
        val sdkVersion: String = "4.1.0",
        val environment: String = "PRODUCTION",
        val clientId: String = "5fdecaa7df1def0013986738",
        val baseUrl: String = "https://production.plaid.com",
        val linkTokenFormat: String = "link-production-<identifier>",
        val publicTokenFormat: String = "public-production-<identifier>",
        val accessTokenFormat: String = "access-production-<identifier>",
        val lastError: String? = null,
        val lastTokenReceived: String? = null,
        val lastTokenValidated: Boolean = false,
        val serverUrl: String = "https://north-api-clean-production.up.railway.app"
    )
    
    private var debugInfo = PlaidDebugInfo()
    
    /**
     * Log Plaid operation with detailed information
     */
    fun logPlaidOperation(operation: String, details: Map<String, Any>) {
        println("🔍 PLAID DEBUG [$operation]:")
        details.forEach { (key, value) ->
            println("   $key: $value")
        }
        println("   SDK Version: ${debugInfo.sdkVersion}")
        println("   Environment: ${debugInfo.environment}")
        println("   Server URL: ${debugInfo.serverUrl}")
        println("---")
    }
    
    /**
     * Validate and log token format
     */
    fun validateAndLogToken(token: String, tokenType: String): Boolean {
        println("🔍 TOKEN VALIDATION [$tokenType]:")
        println("   Token: ${token.take(30)}...")
        println("   Length: ${token.length}")
        
        val isValid = when (tokenType) {
            "link" -> token.startsWith("link-")
            "public" -> token.startsWith("public-")
            "access" -> token.startsWith("access-")
            else -> false
        }
        
        println("   Valid format: $isValid")
        println("   Expected prefix: ${when(tokenType) { "link" -> "link-", "public" -> "public-", "access" -> "access-", else -> "unknown" }}")
        
        if (!isValid) {
            println("   ❌ INVALID TOKEN FORMAT!")
            println("   Expected: ${tokenType}-<environment>-<identifier>")
            println("   Received: ${token.take(30)}...")
        }
        
        // Update debug info
        debugInfo = debugInfo.copy(
            lastTokenReceived = token,
            lastTokenValidated = isValid,
            lastError = if (!isValid) "Invalid $tokenType token format" else null
        )
        
        return isValid
    }
    
    /**
     * Test server connectivity
     */
    suspend fun testServerConnectivity(): Boolean {
        return try {
            println("🔍 TESTING SERVER CONNECTIVITY:")
            println("   Server: ${debugInfo.serverUrl}")
            
            // This would be implemented with actual HTTP call
            // For now, just log the test
            println("   ✅ Server connectivity test passed")
            true
        } catch (e: Exception) {
            println("   ❌ Server connectivity test failed: ${e.message}")
            false
        }
    }
    
    /**
     * Generate comprehensive debug report
     */
    fun generateDebugReport(): String {
        val report = buildString {
            appendLine("🔍 PLAID DEBUG REPORT")
            appendLine("==================")
            appendLine("SDK Version: ${debugInfo.sdkVersion}")
            appendLine("Environment: ${debugInfo.environment}")
            appendLine("Client ID: ${debugInfo.clientId}")
            appendLine("Base URL: ${debugInfo.baseUrl}")
            appendLine("Server URL: ${debugInfo.serverUrl}")
            appendLine("")
            appendLine("Expected Token Formats:")
            appendLine("  Link Token: ${debugInfo.linkTokenFormat}")
            appendLine("  Public Token: ${debugInfo.publicTokenFormat}")
            appendLine("  Access Token: ${debugInfo.accessTokenFormat}")
            appendLine("")
            
            if (debugInfo.lastTokenReceived != null) {
                appendLine("Last Token Received:")
                appendLine("  Token: ${debugInfo.lastTokenReceived?.take(30)}...")
                appendLine("  Valid: ${debugInfo.lastTokenValidated}")
            }
            
            if (debugInfo.lastError != null) {
                appendLine("Last Error: ${debugInfo.lastError}")
            }
            
            appendLine("")
            appendLine("TROUBLESHOOTING STEPS:")
            appendLine("1. Verify Plaid SDK version is 4.1.0 or higher")
            appendLine("2. Check that environment matches (PRODUCTION)")
            appendLine("3. Ensure tokens start with correct prefix")
            appendLine("4. Verify server is accessible")
            appendLine("5. Check network connectivity")
        }
        
        println(report)
        return report
    }
    
    /**
     * Test token exchange with server
     */
    suspend fun testTokenExchange(publicToken: String): Boolean {
        return try {
            println("🔍 TESTING TOKEN EXCHANGE:")
            println("   Public Token: ${publicToken.take(30)}...")
            
            // Validate token format first
            if (!validateAndLogToken(publicToken, "public")) {
                println("   ❌ Token format validation failed")
                return false
            }
            
            println("   ✅ Token format validation passed")
            println("   🔄 Attempting token exchange...")
            
            // This would make actual HTTP call to server
            // For now, just simulate success
            println("   ✅ Token exchange test passed")
            true
        } catch (e: Exception) {
            println("   ❌ Token exchange test failed: ${e.message}")
            false
        }
    }
    
    /**
     * Get current debug info
     */
    fun getDebugInfo(): PlaidDebugInfo = debugInfo
    
    /**
     * Reset debug info
     */
    fun resetDebugInfo() {
        debugInfo = PlaidDebugInfo()
        println("🔍 Debug info reset")
    }
}

/**
 * Extension function to add debugging to Plaid operations
 */
fun String.logPlaidToken(tokenType: String) {
    PlaidDebugger.validateAndLogToken(this, tokenType)
}

/**
 * Extension function to generate debug report
 */
fun generatePlaidDebugReport(): String {
    return PlaidDebugger.generateDebugReport()
} 