package com.north.mobile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.north.mobile.ui.theme.NorthAppTheme
import com.north.mobile.ui.main.NorthApp

// Global callback for Plaid Link results
object PlaidLinkResultHandler {
    private var onSuccess: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    
    fun setCallbacks(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        this.onSuccess = onSuccess
        this.onError = onError
    }
    
    fun handleSuccess(publicToken: String) {
        onSuccess?.invoke(publicToken)
        clearCallbacks()
    }
    
    fun handleError(error: String) {
        onError?.invoke(error)
        clearCallbacks()
    }
    
    private fun clearCallbacks() {
        onSuccess = null
        onError = null
    }
}

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            NorthAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NorthApp()
                }
            }
        }
    }
    
    // Handle Plaid Link result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        Log.d("NorthApp", "🔧 onActivityResult called: requestCode=$requestCode, resultCode=$resultCode")
        
        // Handle Plaid Link result
        if (requestCode == 1001) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val publicToken = data.getStringExtra("public_token")
                val error = data.getStringExtra("error")
                val metadata = data.getParcelableExtra<android.os.Bundle>("metadata")
                
                Log.d("NorthApp", "🔧 Plaid Link result:")
                Log.d("NorthApp", "  - public_token: ${publicToken?.take(20)}...")
                Log.d("NorthApp", "  - error: $error")
                Log.d("NorthApp", "  - metadata: $metadata")
                
                if (publicToken != null) {
                    Log.d("NorthApp", "✅ Plaid Link successful! Calling success callback...")
                    PlaidLinkResultHandler.handleSuccess(publicToken)
                } else if (error != null) {
                    Log.e("NorthApp", "❌ Plaid Link error: $error")
                    PlaidLinkResultHandler.handleError(error)
                } else {
                    Log.d("NorthApp", "ℹ️ Plaid Link cancelled")
                    PlaidLinkResultHandler.handleError("User cancelled")
                }
            } else {
                Log.d("NorthApp", "ℹ️ Plaid Link cancelled or failed")
                PlaidLinkResultHandler.handleError("Link cancelled or failed")
            }
        }
    }
}

