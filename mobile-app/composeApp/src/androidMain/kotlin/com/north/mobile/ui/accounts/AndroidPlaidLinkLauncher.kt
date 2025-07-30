package com.north.mobile.ui.accounts

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.plaid.link.Plaid
import com.plaid.link.configuration.LinkTokenConfiguration
import com.plaid.link.result.LinkResult

/**
 * Validate public token format for Android
 */
fun validateAndroidPublicTokenFormat(publicToken: String): Boolean {
    return try {
        // Check if token starts with "public-" and contains environment
        if (!publicToken.startsWith("public-")) {
            println("❌ Android Token format error: Token should start with 'public-'")
            println("   Received token: ${publicToken.take(30)}...")
            return false
        }
        
        // Check if token contains environment (production, sandbox, development)
        val hasEnvironment = publicToken.contains("-production-") || 
                           publicToken.contains("-sandbox-") || 
                           publicToken.contains("-development-")
        
        if (!hasEnvironment) {
            println("❌ Android Token format error: Token should contain environment (production/sandbox/development)")
            println("   Received token: ${publicToken.take(30)}...")
            return false
        }
        
        // Check minimum length
        if (publicToken.length < 20) {
            println("❌ Android Token format error: Token too short (${publicToken.length} chars)")
            return false
        }
        
        println("✅ Android Token format validation passed: ${publicToken.take(30)}...")
        true
    } catch (e: Exception) {
        println("❌ Android Token format validation failed: ${e.message}")
        false
    }
}

/**
 * Android-specific Plaid Link launcher using the actual Plaid SDK
 */
actual class PlaidLinkLauncher actual constructor(context: Any) {
    private val context = context as Context
    
    actual fun launchPlaidLink(
        linkToken: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Find the Activity from the context
            val activity = context.findActivity()
            if (activity == null) {
                onError("Unable to find Activity context")
                return
            }
            
            println("🚀 Launching Plaid Link with token: ${linkToken.take(20)}...")
            
            // Create Plaid Link configuration
            val config = LinkTokenConfiguration.Builder()
                .token(linkToken)
                .build()
            
            // Create Plaid handler with the correct API
            val plaidHandler = Plaid.create(
                application = context.applicationContext as android.app.Application,
                linkTokenConfiguration = config
            )
            
            // Launch Plaid Link with proper result handling using the modern callback API
            plaidHandler.open(activity) { result ->
                println("📱 Plaid Link result received: ${result.javaClass.simpleName}")
                
                when (result) {
                    is LinkResult.Success -> {
                        println("✅ Plaid Link success!")
                        println("📊 Public token: ${result.publicToken.take(30)}...")
                        println("📊 Metadata: ${result.metadata}")
                        
                        // Validate token format before calling onSuccess
                        if (validateAndroidPublicTokenFormat(result.publicToken)) {
                            println("✅ Token format validation passed, calling onSuccess")
                            onSuccess(result.publicToken)
                        } else {
                            println("❌ Token format validation failed, calling onError")
                            onError("Invalid token format received from Plaid SDK: ${result.publicToken.take(30)}...")
                        }
                    }
                    is LinkResult.Exit -> {
                        println("❌ Plaid Link exit")
                        if (result.error != null) {
                            println("❌ Error: ${result.error}")
                            onError("Plaid Link failed: ${result.error.displayMessage}")
                        } else {
                            println("ℹ️ User cancelled")
                            onError("User cancelled bank connection")
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            println("❌ Exception launching Plaid Link: ${e.message}")
            e.printStackTrace()
            onError("Failed to launch Plaid Link: ${e.message}")
        }
    }
}

private fun Context.findActivity(): Activity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }

/**
 * Composable function to create and remember a Plaid Link launcher
 */
@Composable
fun rememberPlaidLinkLauncher(
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
): PlaidLinkLauncher {
    val context = LocalContext.current
    return remember(context) {
        PlaidLinkLauncher(context)
    }
}