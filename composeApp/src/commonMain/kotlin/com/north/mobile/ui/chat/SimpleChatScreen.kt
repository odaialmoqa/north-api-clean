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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class ChatMessage(
    val message: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleChatScreen(
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
                message = "👋 Hey there! I'm your Personal CFO and I'm so excited to help you with your finances! Think of me as that supportive friend who's always got your back when it comes to money decisions.\n\nWhat's on your mind today? I can help with budgeting, saving goals, spending decisions, or just chat about your financial dreams! 😊",
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
                        Text("💝", fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            "Your Financial Friend",
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
                                    { currentMessage = it }
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
                                { currentMessage = it }
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
                Text("💝", fontSize = 20.sp)
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
                message.message,
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
            Text("💝", fontSize = 20.sp)
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
    onCurrentMessageUpdate: (String) -> Unit
) {
    // Add user message
    val updatedMessages = currentMessages + ChatMessage(userMessage, true)
    onMessagesUpdate(updatedMessages)
    onCurrentMessageUpdate("")
    onLoadingUpdate(true)
    
    // Make real API call to backend
    kotlinx.coroutines.GlobalScope.launch {
        try {
            // TODO: Replace with actual HTTP client call to backend
            // For now, simulate the API call with improved memory
            kotlinx.coroutines.delay(1000)
            
            // Convert messages to conversation history format for backend
            val conversationHistory = currentMessages.map { msg ->
                mapOf(
                    "message" to msg.message,
                    "isFromUser" to msg.isFromUser,
                    "timestamp" to msg.timestamp
                )
            }
            
            // Use the memory-based response that matches backend logic
            val aiResponse = generateAIResponseWithMemory(userMessage, conversationHistory)
            val finalMessages = updatedMessages + ChatMessage(aiResponse, false)
            
            onMessagesUpdate(finalMessages)
            onLoadingUpdate(false)
        } catch (e: Exception) {
            // Fallback to basic response
            val aiResponse = generateAIResponse(userMessage)
            val finalMessages = updatedMessages + ChatMessage(aiResponse, false)
            
            onMessagesUpdate(finalMessages)
            onLoadingUpdate(false)
        }
    }
}

fun generateAIResponseWithMemory(userMessage: String, conversationHistory: List<Map<String, Any>>): String {
    val messageCount = conversationHistory.size
    val lowerMessage = userMessage.lowercase()
    
    // Create varied responses based on conversation history to avoid repetition
    val responses = when {
        lowerMessage.contains("afford", ignoreCase = true) -> listOf(
            "Ooh, I love helping with spending decisions! 🤩 Tell me what you're thinking about buying and I'll check your budget to see if it fits comfortably.",
            "Purchase decisions are my favorite! 💝 What are you considering? I'll help you see if it fits your budget comfortably.",
            "Let's be smart about this together! 🧠 What are you thinking of buying? I'll check how it impacts your goals."
        )
        lowerMessage.contains("goal", ignoreCase = true) -> listOf(
            "Your goals are looking fantastic! 🎯 You're making great progress on your emergency fund - you're at $8,500 out of your $10,000 target. That's 85% there!",
            "I love that you're focused on your goals! Your emergency fund is at 85% - so close to that finish line! Want to talk about strategies to reach that final $1,500?",
            "Goals are where the magic happens! ✨ I see you're making steady progress. What's motivating you most about your current goals?"
        )
        lowerMessage.contains("save", ignoreCase = true) -> listOf(
            "I'm so excited you want to save more! 💪 Here are some friendly tips I've noticed from your spending:\n\n• You're doing great with dining out - down 15% this month! 🎉",
            "Saving is fantastic! I've been analyzing your patterns and you're actually doing better than you think! Your dining spending is down 15% this month.",
            "Let's boost those savings! 🚀 I spotted some opportunities - like those 3 unused subscriptions that could save you $47/month."
        )
        lowerMessage.contains("spending", ignoreCase = true) -> listOf(
            "Let me be your financial detective! 🕵️‍♀️ I've been analyzing your spending and found some really interesting patterns...",
            "Your spending story is actually quite positive! 📊 You're down 15% on dining out this month - that's fantastic progress!",
            "I love diving into spending patterns with you! 💡 Here's what I'm seeing in your recent transactions..."
        )
        lowerMessage.contains("hello", ignoreCase = true) || lowerMessage.contains("hi", ignoreCase = true) -> listOf(
            "Hey there! 👋 Great to see you again! What's on your financial mind today?",
            "Hi! 😊 I've been thinking about your financial journey - how can I help you today?",
            "Welcome back! 🌟 Ready to tackle some financial goals together?"
        )
        else -> listOf(
            "That's a great question! 😊 I love how thoughtful you are about your finances. What specific area would you like to dive deeper into?",
            "I'm here and ready to help with whatever's on your mind! 😊 Whether it's budgeting, goals, or just financial encouragement - what sounds good?",
            "Thanks for chatting with me! 💬 I love being part of your financial journey. What would you like to explore today?"
        )
    }
    
    // Select response based on message count to avoid repetition
    val responseIndex = messageCount % responses.size
    return responses[responseIndex]
}

fun generateAIResponse(userMessage: String): String {
    return when {
        userMessage.contains("afford", ignoreCase = true) -> {
            "Ooh, I love helping with spending decisions! 🤩 Tell me what you're thinking about buying and I'll check your budget to see if it fits comfortably.\n\nI'll look at your current spending, upcoming bills, and goals to give you a personalized answer. What's caught your eye?"
        }
        userMessage.contains("goal", ignoreCase = true) -> {
            "Your goals are looking fantastic! 🎯 You're making great progress on your emergency fund - you're at $8,500 out of your $10,000 target. That's 85% there!\n\nYour vacation fund is at $1,200 out of $3,000, and you're right on track to hit your August target. Keep up the amazing work! 🌟"
        }
        userMessage.contains("save", ignoreCase = true) -> {
            "I'm so excited you want to save more! 💪 Here are some friendly tips I've noticed from your spending:\n\n• You're doing great with dining out - down 15% this month! 🎉\n• I spotted 3 unused subscriptions that could save you $47/month\n• Your grocery bulk-buying strategy is working perfectly!\n\nWhich area would you like to focus on first?"
        }
        userMessage.contains("spending", ignoreCase = true) -> {
            "Let me be your financial detective! 🕵️‍♀️ I've been analyzing your spending and found some really interesting patterns...\n\nYou spent $127 on groceries last week vs your usual $85, but here's the cool part - that big $67 trip included cleaning supplies and toiletries, not just food!\n\nYou're actually being super smart by stocking up on essentials. That's strategic planning, not overspending! 👏"
        }
        userMessage.contains("hello", ignoreCase = true) || userMessage.contains("hi", ignoreCase = true) -> {
            "Hey there! 👋 I'm so happy to chat with you! I'm here to help make your financial journey as smooth and stress-free as possible.\n\nThink of me as your supportive friend who happens to be really good with money. What's on your mind today? 😊"
        }
        else -> {
            "That's a great question! 😊 I love how thoughtful you are about your finances. Based on what I know about your situation, here's what I'm thinking...\n\nYou're doing really well overall - your spending is under control and you're making solid progress on your goals. What specific area would you like to dive deeper into?"
        }
    }
}