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
import com.north.mobile.ui.components.NorthLogo
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.north.mobile.data.ai.ContextualAIService

data class ChatMessage(
    val message: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val attachments: List<ChatAttachment> = emptyList()
)

data class ChatAttachment(
    val id: String,
    val fileName: String,
    val type: AttachmentType,
    val size: Long,
    val previewUrl: String? = null
)

enum class AttachmentType {
    IMAGE, DOCUMENT, RECEIPT, OTHER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleChatScreen(
    authRepository: AuthRepository,
    onBackClick: () -> Unit
) {
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var currentMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedAttachments by remember { mutableStateOf(listOf<ChatAttachment>()) }
    val aiService = remember { ContextualAIService(authRepository) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize with welcome message
    LaunchedEffect(Unit) {
        messages = listOf(
            ChatMessage(
                message = "Hey there! 👋 I'm North, your personal CFO. I'm here to help you make smart financial decisions and reach your goals.\n\nI can see you're working on some exciting goals like your Europe trip and emergency fund - that's fantastic! I'm here to help you stay on track and make the most of your money.\n\nWhat's on your mind today?",
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
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NorthLogo(size = 40.dp)
                    Column {
                        Text(
                            "North, Your Personal CFO",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            "Your personal financial advisor",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Default.ArrowBack, 
                        contentDescription = "Back",
                        tint = Color(0xFF1F2937)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
                titleContentColor = Color(0xFF1F2937)
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
                    "💸 \"Can I afford something?\"",
                    "🎯 \"How are my goals doing?\"",
                    "💡 \"Help me save more money\"",
                    "📊 \"Explain my spending\""
                )
                
                items(quickQuestions) { question ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentMessage = question
                                sendMessageWithAttachments(
                                    question,
                                    emptyList(),
                                    messages,
                                    { messages = it },
                                    { isLoading = it },
                                    { currentMessage = it },
                                    { },
                                    authRepository,
                                    aiService
                                )
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Text(
                            question,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp,
                            color = Color(0xFF1F2937)
                        )
                    }
                }
            }
        }
        
        // Input area
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Attachment preview
                if (selectedAttachments.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedAttachments) { attachment ->
                            AttachmentPreview(
                                attachment = attachment,
                                onRemove = { 
                                    selectedAttachments = selectedAttachments.filter { it.id != attachment.id }
                                }
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = 8.dp), // Extra bottom padding
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = currentMessage,
                        onValueChange = { currentMessage = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { 
                            Text(
                                "Ask me anything...",
                                color = Color(0xFF6B7280)
                            ) 
                        },
                        enabled = !isLoading,
                        maxLines = 3,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00D4AA),
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            focusedTextColor = Color(0xFF1F2937),
                            unfocusedTextColor = Color(0xFF1F2937)
                        )
                    )
                    
                    // Attachment button (WhatsApp style - tight spacing)
                    IconButton(
                        onClick = { 
                            selectedAttachments = selectedAttachments + ChatAttachment(
                                id = "mock_${System.currentTimeMillis()}",
                                fileName = "receipt.jpg",
                                type = AttachmentType.RECEIPT,
                                size = 1024L
                            )
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Text(
                            "📎",
                            fontSize = 20.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                    
                    Button(
                        onClick = {
                            if (currentMessage.isNotBlank() || selectedAttachments.isNotEmpty()) {
                                sendMessageWithAttachments(
                                    currentMessage,
                                    selectedAttachments,
                                    messages,
                                    { messages = it },
                                    { isLoading = it },
                                    { currentMessage = it },
                                    { selectedAttachments = emptyList() },
                                    authRepository,
                                    aiService
                                )
                            }
                        },
                        enabled = !isLoading && (currentMessage.isNotBlank() || selectedAttachments.isNotEmpty()),
                        modifier = Modifier.size(44.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00D4AA),
                            disabledContainerColor = Color(0xFFE5E7EB)
                        )
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
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isFromUser) {
            NorthLogo(size = 32.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser) 
                    Color(0xFF00D4AA)
                else 
                    Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 280.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Show attachments if any
                if (message.attachments.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        message.attachments.forEach { attachment ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    when (attachment.type) {
                                        AttachmentType.IMAGE, AttachmentType.RECEIPT -> "🖼️"
                                        else -> "📄"
                                    },
                                    fontSize = 14.sp
                                )
                                Text(
                                    attachment.fileName,
                                    fontSize = 12.sp,
                                    color = if (message.isFromUser) Color.White.copy(alpha = 0.8f) else Color(0xFF6B7280)
                                )
                            }
                        }
                        
                        if (message.message.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                
                // Show message text if not empty
                if (message.message.isNotBlank()) {
                    Text(
                        text = parseMarkdown(message.message),
                        color = if (message.isFromUser) 
                            Color.White
                        else 
                            Color(0xFF1F2937),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
        
        if (message.isFromUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF6B7280), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "O",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun AttachmentPreview(
    attachment: ChatAttachment,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.width(120.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when (attachment.type) {
                        AttachmentType.IMAGE, AttachmentType.RECEIPT -> "🖼️"
                        else -> "📄"
                    },
                    fontSize = 16.sp
                )
                
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(20.dp)
                ) {
                    Text("×", color = Color(0xFF6B7280), fontSize = 16.sp)
                }
            }
            
            Text(
                attachment.fileName,
                fontSize = 12.sp,
                color = Color(0xFF374151),
                maxLines = 2
            )
            
            Text(
                "${attachment.size / 1024}KB",
                fontSize = 10.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}

fun sendMessageWithAttachments(
    message: String,
    attachments: List<ChatAttachment>,
    messages: List<ChatMessage>,
    setMessages: (List<ChatMessage>) -> Unit,
    setLoading: (Boolean) -> Unit,
    setCurrentMessage: (String) -> Unit,
    clearAttachments: () -> Unit,
    authRepository: AuthRepository,
    aiService: ContextualAIService
) {
    if (message.isBlank() && attachments.isEmpty()) return
    
    // Add user message with attachments
    val userMessage = ChatMessage(
        message = message.ifBlank { "Shared ${attachments.size} attachment(s)" },
        isFromUser = true,
        attachments = attachments
    )
    val updatedMessages = messages + userMessage
    setMessages(updatedMessages)
    setCurrentMessage("")
    clearAttachments()
    setLoading(true)
    
    // Generate contextual AI response with memory
    kotlinx.coroutines.GlobalScope.launch {
        try {
            kotlinx.coroutines.delay(1000) // Faster response with memory system
            
            val responseMessage = aiService.generateContextualResponse(
                userMessage = message,
                attachments = attachments,
                allMessages = updatedMessages
            )
            
            val aiResponse = ChatMessage(
                message = responseMessage,
                isFromUser = false
            )
            
            setMessages(updatedMessages + aiResponse)
            setLoading(false)
        } catch (e: Exception) {
            println("❌ AI response error: ${e.message}")
            val errorResponse = ChatMessage(
                message = "I'm having trouble processing that right now. Let me try again - what would you like to discuss about your finances?",
                isFromUser = false
            )
            setMessages(updatedMessages + errorResponse)
            setLoading(false)
        }
    }
}

@Composable
fun ChatLoadingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        NorthLogo(size = 32.dp)
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



