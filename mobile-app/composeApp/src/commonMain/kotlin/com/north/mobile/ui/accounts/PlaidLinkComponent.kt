package com.north.mobile.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class PlaidLinkTokenRequest(
    val client_id: String,
    val secret: String,
    val client_name: String,
    val country_codes: List<String>,
    val language: String,
    val user: PlaidUser,
    val products: List<String>
)

@Serializable
data class PlaidUser(
    val client_user_id: String
)

@Serializable
data class PlaidLinkTokenResponse(
    val link_token: String,
    val expiration: String
)

/**
 * Validate public token format
 */
fun validatePublicTokenFormat(publicToken: String): Boolean {
    return try {
        // Check if token starts with "public-" and contains environment
        if (!publicToken.startsWith("public-")) {
            println("❌ Token format error: Token should start with 'public-'")
            return false
        }
        
        // Check if token contains environment (production, sandbox, development)
        val hasEnvironment = publicToken.contains("-production-") || 
                           publicToken.contains("-sandbox-") || 
                           publicToken.contains("-development-")
        
        if (!hasEnvironment) {
            println("❌ Token format error: Token should contain environment (production/sandbox/development)")
            return false
        }
        
        // Check minimum length
        if (publicToken.length < 20) {
            println("❌ Token format error: Token too short")
            return false
        }
        
        println("✅ Token format validation passed: ${publicToken.take(20)}...")
        true
    } catch (e: Exception) {
        println("❌ Token format validation failed: ${e.message}")
        false
    }
}

/**
 * Create a Plaid link token directly using Plaid API
 */
suspend fun createPlaidLinkTokenDirect(): String? {
    return try {
        println("Creating Plaid link token...")
        
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
        
        val request = PlaidLinkTokenRequest(
            client_id = "5fdecaa7df1def0013986738",
            secret = "084141a287c71fd8f75cdc71c796b1",
            client_name = "North",
            country_codes = listOf("US", "CA"),
            language = "en",
            user = PlaidUser(client_user_id = "test-user-123"),
            products = listOf("transactions")
        )
        
        println("Sending request to Plaid API...")
        
        val response = client.post("${com.north.mobile.config.PlaidConfig.baseUrl}/link/token/create") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        
        println("Received response from Plaid API, status: ${response.status}")
        
        if (response.status.value in 200..299) {
            val linkTokenResponse = response.body<PlaidLinkTokenResponse>()
            client.close()
            
            println("Successfully created link token: ${linkTokenResponse.link_token.take(20)}...")
            linkTokenResponse.link_token
        } else {
            val errorBody = response.body<String>()
            println("Plaid API error: ${response.status} - $errorBody")
            client.close()
            null
        }
    } catch (e: Exception) {
        println("Error creating Plaid link token: ${e.message}")
        e.printStackTrace()
        null
    }
}

/**
 * Composable component for Plaid Link integration
 */
@Composable
fun PlaidLinkButton(
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = "Connect with Plaid",
    buttonColor: Color = Color.White,
    textColor: Color = Color(0xFF00D4AA)
) {
    var isConnecting by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // Get context using platform-specific implementation
    val context = getPlatformContext()
    
    // Create Plaid launcher with proper error handling
    val plaidLauncher = remember(context) {
        try {
            if (context != null) {
                println("🔧 Creating PlaidLinkLauncher with context: ${context.javaClass.simpleName}")
                PlaidLinkLauncher(context)
            } else {
                println("❌ Context is null - cannot create PlaidLinkLauncher")
                null
            }
        } catch (e: Exception) {
            println("❌ Failed to create PlaidLinkLauncher: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2563EB)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Bank",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        "Connect Your Bank Account",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Powered by Plaid",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            
            Text(
                "Securely connect your Canadian bank accounts to get personalized financial insights and advice.",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp
            )
            
            // Show connection status if any
            connectionStatus?.let { status ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        status,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            Button(
                onClick = {
                    println("🔘 Plaid button clicked")
                    
                    if (plaidLauncher == null) {
                        println("❌ PlaidLauncher is null")
                        onError("Plaid SDK not available")
                        return@Button
                    }
                    
                    isConnecting = true
                    connectionStatus = "🔐 Creating secure link token..."
                    
                    coroutineScope.launch {
                        try {
                            println("🔄 Starting link token creation...")
                            val linkTokenResult = createLinkToken()
                            
                            if (linkTokenResult.isSuccess) {
                                val linkToken = linkTokenResult.getOrNull()
                                if (linkToken != null) {
                                    connectionStatus = "🚀 Opening Plaid Link..."
                                    println("✅ Link token created: ${linkToken.take(20)}...")
                                    
                                    // Log the link token for debugging
                                    try {
                                        linkToken.logPlaidToken("link")
                                    } catch (e: Exception) {
                                        println("⚠️ Failed to log token: ${e.message}")
                                    }
                                    
                                    println("🚀 Launching Plaid Link...")
                                    plaidLauncher.launchPlaidLink(
                                        linkToken = linkToken,
                                        onSuccess = { publicToken ->
                                            println("✅ Plaid Link success: ${publicToken.take(20)}...")
                                            try {
                                                // Log the public token for debugging
                                                publicToken.logPlaidToken("public")
                                            } catch (e: Exception) {
                                                println("⚠️ Failed to log public token: ${e.message}")
                                            }
                                            
                                            // Validate token format before proceeding
                                            if (validatePublicTokenFormat(publicToken)) {
                                                connectionStatus = "✅ Successfully connected!\n\nYour bank account is now linked and ready to sync transactions."
                                                onSuccess(publicToken)
                                                isConnecting = false
                                            } else {
                                                connectionStatus = "❌ Invalid token format received from Plaid"
                                                onError("Invalid token format: Token should start with 'public-' and contain environment")
                                                isConnecting = false
                                            }
                                        },
                                        onError = { error ->
                                            println("❌ Plaid Link error: $error")
                                            try {
                                                // Log the error for debugging
                                                PlaidDebugger.logPlaidOperation("ERROR", mapOf(
                                                    "error" to error,
                                                    "linkToken" to linkToken.take(20)
                                                ))
                                            } catch (e: Exception) {
                                                println("⚠️ Failed to log error: ${e.message}")
                                            }
                                            connectionStatus = "❌ Connection failed: $error"
                                            onError(error)
                                            isConnecting = false
                                        }
                                    )
                                } else {
                                    println("❌ Link token is null")
                                    connectionStatus = "❌ Invalid link token received"
                                    onError("Invalid link token")
                                    isConnecting = false
                                }
                            } else {
                                val error = linkTokenResult.exceptionOrNull()?.message ?: "Unknown error"
                                println("❌ Link token creation failed: $error")
                                connectionStatus = "❌ Failed to create link token: $error"
                                onError(error)
                                isConnecting = false
                            }
                            
                        } catch (e: Exception) {
                            println("❌ Plaid connection failed: ${e.message}")
                            e.printStackTrace()
                            connectionStatus = "❌ Connection failed: ${e.message}"
                            onError(e.message ?: "Unknown error")
                            isConnecting = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isConnecting && plaidLauncher != null
            ) {
                if (isConnecting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF2563EB)
                        )
                        Text(
                            "Connecting...",
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        "Connect with Plaid",
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Security info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🔒", fontSize = 16.sp)
                Text(
                    "Bank-level security • Read-only access • Canadian data protection",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Create link token from backend API with improved error handling
 */
private suspend fun createLinkToken(): Result<String> {
    return try {
        println("🔗 Creating link token from backend...")
        
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
        
        // Call your backend API to create the link token
        val response = client.post("https://north-api-clean-production.up.railway.app/api/plaid/create-link-token") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("user_id", "north-user-${System.currentTimeMillis()}")
            })
        }
        
        println("📡 Backend response status: ${response.status}")
        
        if (response.status.isSuccess()) {
            val responseBody = response.body<JsonObject>()
            val linkToken = responseBody["link_token"]?.jsonPrimitive?.content
                ?: throw Exception("Missing link_token in response")
            
            println("✅ Link token created successfully: ${linkToken.take(20)}...")
            Result.success(linkToken)
        } else {
            val errorBody = response.body<JsonObject>()
            val errorMessage = errorBody["error"]?.jsonPrimitive?.content ?: "Failed to create link token"
            println("❌ Backend error: $errorMessage")
            Result.failure(Exception(errorMessage))
        }
    } catch (e: Exception) {
        println("❌ Exception creating link token: ${e.message}")
        e.printStackTrace()
        Result.failure(e)
    }
}

/**
 * Test Plaid SDK availability
 */
fun testPlaidSDKAvailability(): Boolean {
    return try {
        // This is a simple test to check if Plaid SDK classes are available
        println("🔍 Testing Plaid SDK availability...")
        
        // Try to access Plaid SDK classes (this will fail if SDK is not properly included)
        val plaidClass = Class.forName("com.plaid.link.Plaid")
        println("✅ Plaid SDK is available")
        true
    } catch (e: Exception) {
        println("❌ Plaid SDK not available: ${e.message}")
        false
    }
}

/**
 * Expect/actual pattern for platform-specific Plaid Link implementation
 */
expect class PlaidLinkLauncher(context: Any) {
    fun launchPlaidLink(
        linkToken: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    )
}

/**
 * Platform-specific context getter
 */
@Composable
expect fun getPlatformContext(): Any?