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
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Create Plaid launcher
    val plaidLauncher = remember(context) {
        PlaidLinkLauncher(context)
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
                    isConnecting = true
                    connectionStatus = "🔐 Creating secure link token..."
                    
                    coroutineScope.launch {
                        try {
                            val linkTokenResult = createLinkToken()
                            
                            if (linkTokenResult.isSuccess) {
                                val linkToken = linkTokenResult.getOrNull()
                                if (linkToken != null) {
                                    connectionStatus = "🚀 Opening Plaid Link..."
                                    
                                    plaidLauncher.launchPlaidLink(
                                        linkToken = linkToken,
                                        onSuccess = { publicToken ->
                                            connectionStatus = "✅ Successfully connected!\n\nYour bank account is now linked and ready to sync transactions."
                                            onSuccess(publicToken)
                                            isConnecting = false
                                        },
                                        onError = { error ->
                                            connectionStatus = "❌ Connection failed: $error"
                                            onError(error)
                                            isConnecting = false
                                        }
                                    )
                                } else {
                                    connectionStatus = "❌ Invalid link token received"
                                    onError("Invalid link token")
                                    isConnecting = false
                                }
                            } else {
                                val error = linkTokenResult.exceptionOrNull()?.message ?: "Unknown error"
                                connectionStatus = "❌ Failed to create link token: $error"
                                onError(error)
                                isConnecting = false
                            }
                            
                        } catch (e: Exception) {
                            connectionStatus = "❌ Connection failed: ${e.message}"
                            onError(e.message ?: "Unknown error")
                            isConnecting = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isConnecting
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
 * Create link token from backend API
 */
private suspend fun createLinkToken(): Result<String> {
    return try {
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
        
        if (response.status.isSuccess()) {
            val responseBody = response.body<JsonObject>()
            val linkToken = responseBody["link_token"]?.jsonPrimitive?.content
                ?: throw Exception("Missing link_token in response")
            Result.success(linkToken)
        } else {
            val errorBody = response.body<JsonObject>()
            val errorMessage = errorBody["error"]?.jsonPrimitive?.content ?: "Failed to create link token"
            Result.failure(Exception(errorMessage))
        }
    } catch (e: Exception) {
        Result.failure(e)
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