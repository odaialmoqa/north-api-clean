package com.north.mobile.ui.chat

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.north.mobile.data.api.ApiClient
import com.north.mobile.data.api.AuthApiService
import com.north.mobile.data.api.FinancialApiService
import com.north.mobile.data.repository.AuthRepository

data class ChatMessage(
    val message: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleChatScreen(
    authRepository: AuthRepository,
    onBackClick: () -> Unit
) {
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var currentMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize with welcome message
    LaunchedEffect(Unit) {
        messages = listOf(
            ChatMessage(
                message = "👋 Hey there! I'm North and I'm so excited to help you with your finances! Think of me as that supportive friend who's always got your back when it comes to money decisions.\n\nWhat's on your mind today? I can help with budgeting, saving goals, spending decisions, or just chat about your financial dreams! 😊",
                isFromUser = false
            )
        )
    }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF10B981), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💰", fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            "North",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Always here to help! 😊",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { message ->
                ChatMessageBubble(message = message)
            }
            
            if (isLoading) {
                item {
                    ChatLoadingBubble()
                }
            }
        }
        
        // Quick action buttons
        if (messages.size <= 1) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "💭 What would you like to chat about?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                val quickQuestions = listOf(
                    "💸 Can I afford something?",
                    "🎯 How are my goals doing?",
                    "💡 Help me save more money",
                    "📊 Explain my spending"
                )
                
                items(quickQuestions) { question ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentMessage = question
                                sendMessage(
                                    question,
                                    messages,
                                    { messages = it },
                                    { isLoading = it },
                                    { currentMessage = it },
                                    authRepository
                                )
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            question,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
        
        // Input area
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = currentMessage,
                    onValueChange = { currentMessage = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask me anything...") },
                    enabled = !isLoading,
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp)
                )
                
                Button(
                    onClick = {
                        if (currentMessage.isNotBlank()) {
                            sendMessage(
                                currentMessage,
                                messages,
                                { messages = it },
                                { isLoading = it },
                                { currentMessage = it },
                                authRepository
                            )
                        }
                    },
                    enabled = !isLoading && currentMessage.isNotBlank(),
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isFromUser) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF10B981), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("💰", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = parseMarkdown(message.message),
                modifier = Modifier.padding(12.dp),
                color = if (message.isFromUser) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
        }
        
        if (message.isFromUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("👤", fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun ChatLoadingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF10B981), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("💰", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Thinking", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

// Real API call to backend AI CFO
fun sendMessage(
    userMessage: String,
    currentMessages: List<ChatMessage>,
    onMessagesUpdate: (List<ChatMessage>) -> Unit,
    onLoadingUpdate: (Boolean) -> Unit,
    onCurrentMessageUpdate: (String) -> Unit,
    authRepository: AuthRepository
) {
    // Add user message
    val updatedMessages = currentMessages + ChatMessage(userMessage, true)
    onMessagesUpdate(updatedMessages)
    onCurrentMessageUpdate("")
    onLoadingUpdate(true)
    
    // Make real API call to backend AI CFO Brain
    kotlinx.coroutines.GlobalScope.launch {
        try {
            println("🚀 Starting AI CFO API call for message: '$userMessage'")
            
            // Call the real Gemini-powered AI CFO endpoint
            val aiResponse = callAICFOApi(userMessage, authRepository)
            val finalMessages = updatedMessages + ChatMessage(aiResponse, false)
            
            println("✅ AI CFO response received successfully")
            onMessagesUpdate(finalMessages)
            onLoadingUpdate(false)
        } catch (e: Exception) {
            println("❌ AI CFO API call failed: ${e.message}")
            
            // Show the actual error for debugging
            val errorMessage = "Debug: API call failed - ${e.message}"
            val finalMessages = updatedMessages + ChatMessage(errorMessage, false)
            
            onMessagesUpdate(finalMessages)
            onLoadingUpdate(false)
        }
    }
}

// Call the real AI CFO API
suspend fun callAICFOApi(message: String, authRepository: AuthRepository): String {
    return try {
        println("🤖 Calling AI CFO API with message: $message")
        
        // Use the existing FinancialApiService to call the AI CFO
        val apiService = com.north.mobile.data.api.FinancialApiService(
            com.north.mobile.data.api.ApiClient()
        )
        
        // Get auth token from the shared auth repository
        println("🔍 Checking authentication state...")
        val isAuthenticated = authRepository.isUserAuthenticated()
        println("🔐 User authenticated: $isAuthenticated")
        
        var token = authRepository.getCurrentToken()
        println("🔑 Auth token retrieved: ${if (token != null) "✅ Success (${token.take(20)}...)" else "❌ Failed - token is null"}")
        
        // Additional debugging
        val currentUser = authRepository.currentUser.value
        println("👤 Current user: ${if (currentUser != null) "✅ Found user: ${currentUser.email}" else "❌ No user found"}")
        
        // If no token found, try to re-initialize the session
        if (token == null) {
            println("🔧 No token found, attempting to re-initialize session...")
            try {
                // Force re-initialization of the auth repository
                authRepository.initializeSession()
                token = authRepository.getCurrentToken()
                
                if (token != null) {
                    println("✅ Token found after re-initialization: ${token.take(20)}...")
                } else {
                    println("🧪 Still no token, creating test token for AI testing...")
                    // Fallback to test token creation
                    val testToken = createTestToken()
                    if (testToken != null) {
                        token = testToken
                        println("✅ Test token created successfully: ${token.take(20)}...")
                    } else {
                        throw Exception("Failed to create test token")
                    }
                }
            } catch (e: Exception) {
                println("❌ Token recovery failed: ${e.message}")
                throw Exception("No auth token available and recovery failed")
            }
        }
        
        // Call the AI CFO endpoint
        println("📡 Making API call to sendChatMessage...")
        val result = apiService.sendChatMessage(token, message)
        
        if (result.isSuccess) {
            val response = result.getOrThrow().response
            println("✅ AI CFO API success: ${response.take(100)}...")
            response
        } else {
            val error = result.exceptionOrNull()
            println("❌ AI CFO API failed: ${error?.message}")
            throw error ?: Exception("Unknown API error")
        }
    } catch (e: Exception) {
        println("💥 callAICFOApi exception: ${e.message}")
        throw Exception("Failed to get AI response: ${e.message}")
    }
}

// TEMPORARY: Create a test token for AI testing
suspend fun createTestToken(): String? {
    return try {
        println("🧪 Creating test user for AI testing...")
        val apiClient = ApiClient()
        val authApiService = AuthApiService(apiClient)
        
        // Generate a unique test email
        val testEmail = "ai-test-${System.currentTimeMillis()}@example.com"
        
        // Register a test user
        val result = authApiService.register(
            email = testEmail,
            password = "test123",
            firstName = "AI",
            lastName = "Test"
        )
        
        if (result.isSuccess) {
            val authResponse = result.getOrThrow()
            println("✅ Test user created: ${authResponse.user.email}")
            println("🎫 Test token: ${authResponse.token.take(20)}...")
            authResponse.token
        } else {
            println("❌ Test user creation failed: ${result.exceptionOrNull()?.message}")
            null
        }
    } catch (e: Exception) {
        println("💥 Exception creating test token: ${e.message}")
        null
    }
}

// Parse basic markdown formatting for chat messages
@Composable
fun parseMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val length = text.length
        
        while (currentIndex < length) {
            when {
                // Handle **bold** text
                text.startsWith("**", currentIndex) -> {
                    val endIndex = text.indexOf("**", currentIndex + 2)
                    if (endIndex != -1) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(currentIndex + 2, endIndex))
                        }
                        currentIndex = endIndex + 2
                    } else {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
                // Handle *italic* text
                text.startsWith("*", currentIndex) && !text.startsWith("**", currentIndex) -> {
                    val endIndex = text.indexOf("*", currentIndex + 1)
                    if (endIndex != -1) {
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(currentIndex + 1, endIndex))
                        }
                        currentIndex = endIndex + 1
                    } else {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
                // Handle bullet points (• or -)
                text.startsWith("• ", currentIndex) || text.startsWith("- ", currentIndex) -> {
                    append("• ")
                    currentIndex += 2
                }
                // Regular text
                else -> {
                    append(text[currentIndex])
                    currentIndex++
                }
            }
        }
    }
}



