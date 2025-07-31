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
import com.north.mobile.ui.accounts.PlaidCallbackManager

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
        
        Log.d("NorthApp", "🔧 MainActivity.onActivityResult called: requestCode=$requestCode, resultCode=$resultCode")
        
        // Handle Plaid Link result (requestCode is typically 1001 for Plaid)
        if (requestCode == 1001 || resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val publicToken = data.getStringExtra("public_token")
                val error = data.getStringExtra("error")
                val metadata = data.getParcelableExtra<android.os.Bundle>("metadata")
                
                Log.d("NorthApp", "🔧 Plaid Link result:")
                Log.d("NorthApp", "  - public_token: ${publicToken?.take(20)}...")
                Log.d("NorthApp", "  - error: $error")
                Log.d("NorthApp", "  - metadata: $metadata")
                
                if (publicToken != null) {
                    Log.d("NorthApp", "✅ Plaid Link successful! Calling PlaidCallbackManager...")
                    PlaidCallbackManager.handleSuccess(publicToken)
                } else if (error != null) {
                    Log.e("NorthApp", "❌ Plaid Link error: $error")
                    PlaidCallbackManager.handleError(error)
                } else {
                    Log.d("NorthApp", "ℹ️ Plaid Link cancelled")
                    PlaidCallbackManager.handleError("User cancelled")
                }
            } else {
                Log.d("NorthApp", "ℹ️ Plaid Link cancelled or failed (no data)")
                PlaidCallbackManager.handleError("Link cancelled or failed")
            }
        } else {
            Log.d("NorthApp", "ℹ️ Non-Plaid activity result or cancelled")
            // Only call error callback if this might be a Plaid result
            if (requestCode == 1001) {
                PlaidCallbackManager.handleError("Link cancelled or failed")
            }
        }
    }
}

