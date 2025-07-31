package com.north.mobile.ui.accounts

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.plaid.link.Plaid
import com.plaid.link.PlaidHandler
import com.plaid.link.configuration.LinkTokenConfiguration
import com.plaid.link.result.LinkResult

actual class PlaidLinkLauncher actual constructor(context: Any) {
    private val context = context as Context
    
    actual fun launchPlaidLink(
        linkToken: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        android.util.Log.d("NorthApp", "🔧 DEBUG: Starting Plaid Link launch...")
        println("🔧 DEBUG: Starting Plaid Link launch...")
        
        try {
            android.util.Log.d("NorthApp", "🔧 DEBUG: Creating Plaid configuration...")
            println("🔧 DEBUG: Creating Plaid configuration...")
            
            // Create the link token configuration
            val configuration = LinkTokenConfiguration.Builder()
                .token(linkToken)
                .build()
            
            android.util.Log.d("NorthApp", "🔧 DEBUG: Creating Plaid handler...")
            println("🔧 DEBUG: Creating Plaid handler...")
            
            val activity = context.findActivity()
            if (activity == null) {
                android.util.Log.e("NorthApp", "❌ Activity context is null!")
                println("❌ Activity context is null!")
                onError("Activity context not available")
                return
            }
            
            android.util.Log.d("NorthApp", "🔧 DEBUG: Activity context obtained: ${activity.javaClass.simpleName}")
            println("🔧 DEBUG: Activity context obtained: ${activity.javaClass.simpleName}")
            
            // Create the Plaid handler with proper callback handling
            val plaidHandler = Plaid.create(
                application = context.applicationContext as android.app.Application,
                linkTokenConfiguration = configuration
            )
            
            // Set up the result handler
            val linkResultHandler = object : PlaidHandler.LinkResultHandler {
                override fun onLinkResult(linkResult: LinkResult) {
                    android.util.Log.d("NorthApp", "🔧 DEBUG: Plaid Link result received: ${linkResult.javaClass.simpleName}")
                    println("🔧 DEBUG: Plaid Link result received: ${linkResult.javaClass.simpleName}")
                    
                    when (linkResult) {
                        is LinkResult.Success -> {
                            android.util.Log.d("NorthApp", "✅ Plaid Link successful!")
                            println("✅ Plaid Link successful!")
                            println("🔍 Public token: ${linkResult.publicToken.take(20)}...")
                            println("🔍 Metadata: ${linkResult.metadata}")
                            
                            onSuccess(linkResult.publicToken)
                        }
                        is LinkResult.Cancelled -> {
                            android.util.Log.d("NorthApp", "ℹ️ Plaid Link cancelled")
                            println("ℹ️ Plaid Link cancelled")
                            onError("User cancelled")
                        }
                        is LinkResult.Failure -> {
                            android.util.Log.e("NorthApp", "❌ Plaid Link failed: ${linkResult.error}")
                            println("❌ Plaid Link failed: ${linkResult.error}")
                            onError("Plaid Link failed: ${linkResult.error.displayMessage ?: linkResult.error.errorMessage}")
                        }
                    }
                }
            }
            
            android.util.Log.d("NorthApp", "🔧 DEBUG: Opening Plaid Link with callback handler...")
            println("🔧 DEBUG: Opening Plaid Link with callback handler...")
            
            // Open Plaid Link with the result handler
            plaidHandler.open(activity, linkResultHandler)
            
            android.util.Log.d("NorthApp", "🔧 DEBUG: Plaid Link opened successfully!")
            println("🔧 DEBUG: Plaid Link opened successfully!")
            
        } catch (e: Exception) {
            android.util.Log.e("NorthApp", "❌ Exception in Plaid Link launch: ${e.message}")
            println("❌ Exception in Plaid Link launch: ${e.message}")
            e.printStackTrace()
            onError("Failed to launch Plaid: ${e.message}")
        }
    }
    
    private fun Context.findActivity(): Activity? {
        var context = this
        while (context is android.content.ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }
}

@Composable
actual fun getPlatformContext(): Any? {
    return LocalContext.current
}