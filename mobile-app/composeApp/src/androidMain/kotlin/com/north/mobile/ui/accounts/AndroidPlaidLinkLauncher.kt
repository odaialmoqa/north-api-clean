package com.north.mobile.ui.accounts

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.plaid.link.Plaid
import com.plaid.link.configuration.LinkTokenConfiguration

// Global callback storage for Plaid results
object PlaidCallbackManager {
    private var currentOnSuccess: ((String) -> Unit)? = null
    private var currentOnError: ((String) -> Unit)? = null
    
    fun setCallbacks(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        currentOnSuccess = onSuccess
        currentOnError = onError
    }
    
    fun handleSuccess(publicToken: String) {
        android.util.Log.d("NorthApp", "🎉 PlaidCallbackManager.handleSuccess called with token: ${publicToken.take(20)}...")
        currentOnSuccess?.invoke(publicToken)
        clearCallbacks()
    }
    
    fun handleError(error: String) {
        android.util.Log.e("NorthApp", "❌ PlaidCallbackManager.handleError called: $error")
        currentOnError?.invoke(error)
        clearCallbacks()
    }
    
    private fun clearCallbacks() {
        currentOnSuccess = null
        currentOnError = null
    }
}

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
            // Store callbacks for MainActivity to use
            PlaidCallbackManager.setCallbacks(onSuccess, onError)
            
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
            
            // Create the Plaid handler
            val plaidHandler = Plaid.create(
                application = context.applicationContext as android.app.Application,
                linkTokenConfiguration = configuration
            )
            
            android.util.Log.d("NorthApp", "🔧 DEBUG: Opening Plaid Link...")
            println("🔧 DEBUG: Opening Plaid Link...")
            
            // Open Plaid Link - this will trigger onActivityResult in MainActivity
            val success = plaidHandler.open(activity)
            
            android.util.Log.d("NorthApp", "🔧 DEBUG: Plaid.open() returned: $success")
            println("🔧 DEBUG: Plaid.open() returned: $success")
            
            if (success) {
                android.util.Log.d("NorthApp", "🔧 DEBUG: Plaid Link opened successfully!")
                println("🔧 DEBUG: Plaid Link opened successfully!")
                // Result will be handled by MainActivity's onActivityResult
            } else {
                android.util.Log.e("NorthApp", "❌ Failed to open Plaid Link")
                println("❌ Failed to open Plaid Link")
                onError("Failed to open Plaid Link")
            }
            
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