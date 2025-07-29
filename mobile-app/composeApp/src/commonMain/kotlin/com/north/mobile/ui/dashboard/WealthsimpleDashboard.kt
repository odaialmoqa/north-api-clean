package com.north.mobile.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.north.mobile.ui.components.NorthLogo
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WealthsimpleDashboard(
    onNavigateToChat: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp
            ) {
                val tabs = listOf(
                    Triple("Home", Icons.Outlined.Home, Icons.Filled.Home),
                    Triple("Insights", Icons.Outlined.TrendingUp, Icons.Filled.TrendingUp),
                    Triple("Goals", Icons.Outlined.Flag, Icons.Filled.Flag),
                    Triple("CFO", Icons.Outlined.Person, Icons.Filled.Person),
                    Triple("Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
                )
                
                tabs.forEachIndexed { index, (title, outlinedIcon, filledIcon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { 
                            selectedTab = index
                            when (index) {
                                1 -> onNavigateToInsights() // Insights tab
                                3 -> onNavigateToChat() // CFO tab
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) filledIcon else outlinedIcon,
                                contentDescription = title,
                                tint = if (selectedTab == index) Color(0xFF00D4AA) else Color(0xFF6B7280)
                            )
                        },
                        label = { 
                            Text(
                                title,
                                color = if (selectedTab == index) Color(0xFF00D4AA) else Color(0xFF6B7280),
                                fontSize = 12.sp
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> HomeContent(paddingValues, onNavigateToChat, onNavigateToInsights)
            1 -> InsightsContent(paddingValues, onNavigateToInsights)
            2 -> GoalsContent(paddingValues)
            3 -> ChatContent(paddingValues, onNavigateToChat)
            4 -> SettingsContent(paddingValues)
        }
    }
}

@Composable
fun HomeContent(
    paddingValues: PaddingValues, 
    onNavigateToChat: () -> Unit,
    onNavigateToInsights: () -> Unit
) {
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(paddingValues),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier.padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Good morning, Odai",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    "Here's your financial overview",
                    fontSize = 16.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
        
        // Bank Connection Card (prominent)
        item {
            BankConnectionCard()
        }
        
        // Dynamic Insights Card
        item {
            DynamicInsightsCard(onNavigateToInsights)
        }
        
        // AI-Generated Goals Card
        item {
            DynamicGoalsCard()
        }
        
        // Net Worth Card
        item {
            NetWorthCard()
        }
        
        // Monthly Spending Card
        item {
            MonthlySpendingCard()
        }
        
        // AI CFO Card
        item {
            AICFOCard(onNavigateToChat)
        }
        
        // Recent Activity
        item {
            RecentActivityCard()
        }
    }
}

@Composable
fun NetWorthCard() {
    var animatedValue by remember { mutableStateOf(0) }
    var hasAnimated by remember { mutableStateOf(false) }
    val targetValue = 127450
    
    LaunchedEffect(Unit) {
        if (!hasAnimated) {
            delay(400) // Shorter delay
            val animationDuration = 1500L // Faster animation
            val steps = 50
            val stepDelay = animationDuration / steps
            val stepValue = targetValue / steps
            
            repeat(steps) { step ->
                delay(stepDelay)
                animatedValue = (stepValue * (step + 1)).coerceAtMost(targetValue)
            }
            hasAnimated = true
        } else {
            animatedValue = targetValue // Show final value immediately if already animated
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "NET WORTH",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280),
                letterSpacing = 1.sp
            )
            
            Text(
                "$${animatedValue.toString().replace(Regex("(\\d)(?=(\\d{3})+(?!\\d))"), "$1,")}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "Up",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "+$4,280 this month",
                    fontSize = 14.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AnimatedCard(
    delayMs: Long,
    isVisible: Boolean,
    content: @Composable () -> Unit
) {
    var cardVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(delayMs)
            cardVisible = true
        }
    }
    
    AnimatedVisibility(
        visible = cardVisible,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            animationSpec = tween(300),
            initialOffsetY = { 20 }
        )
    ) {
        content()
    }
}

@Composable
fun MonthlySpendingCard() {
    var progressAnimated by remember { mutableStateOf(0f) }
    var hasAnimated by remember { mutableStateOf(false) }
    val targetProgress = 0.68f
    
    LaunchedEffect(Unit) {
        if (!hasAnimated) {
            delay(600) // Shorter delay
            animate(
                initialValue = 0f,
                targetValue = targetProgress,
                animationSpec = tween(1200, easing = EaseOutCubic) // Faster animation
            ) { value, _ ->
                progressAnimated = value
            }
            hasAnimated = true
        } else {
            progressAnimated = targetProgress // Show final value immediately if already animated
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFEF3C7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💳", fontSize = 16.sp)
                }
                Text(
                    "Monthly Spending",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "December Budget",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        "$2,380 / $3,500",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F2937)
                    )
                }
                
                LinearProgressIndicator(
                    progress = progressAnimated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFF59E0B),
                    trackColor = Color(0xFFF3F4F6)
                )
                
                Text(
                    "$1,120 remaining • On track for month-end",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpendingMetric("$847", "Avg weekly spend")
                SpendingMetric("-12%", "vs last month")
            }
        }
    }
}

@Composable
fun SpendingMetric(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937)
        )
        Text(
            label,
            fontSize = 12.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AICFOCard(onNavigateToChat: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(150)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        onNavigateToChat()
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(80.dp)
                            .background(Color(0xFF00D4AA), RoundedCornerShape(2.dp))
                    )
                    Text(
                        "\"Odai, I've noticed your dining expenses increased 18% this month. Based on your goals, I recommend setting a $400 dining budget to accelerate your Europe trip savings by 3 months.\"",
                        fontSize = 14.sp,
                        color = Color(0xFF374151),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RecentActivityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
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
                    imageVector = Icons.Default.BarChart,
                    contentDescription = "Activity",
                    tint = Color(0xFF6B46C1),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Recent Activity",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TransactionItem("Salary Deposit", "Today, 9:00 AM", "+$4,200.00", true)
                TransactionItem("Whole Foods", "Yesterday, 6:30 PM", "-$127.43", false)
                TransactionItem("Investment Transfer", "Dec 8, 2:15 PM", "-$800.00", false)
                TransactionItem("Coffee Project", "Dec 8, 8:45 AM", "-$6.75", false)
            }
        }
    }
}

@Composable
fun TransactionItem(title: String, time: String, amount: String, isPositive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            )
            Text(
                time,
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
        
        Text(
            amount,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPositive) Color(0xFF10B981) else Color(0xFF1F2937)
        )
    }
}

// Placeholder content for other tabs
@Composable
fun InsightsContent(paddingValues: PaddingValues, onNavigateToInsights: () -> Unit) {
    // This will trigger navigation to the actual insights screen
    LaunchedEffect(Unit) {
        onNavigateToInsights()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFF00D4AA))
    }
}

@Composable
fun GoalsContent(paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(paddingValues),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            GoalsCard()
        }
    }
}

@Composable
fun GoalsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFEE2E2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎯", fontSize = 16.sp)
                }
                Text(
                    "Financial Goals",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }
            
            AnimatedGoalItem("Emergency Fund", "$8,500", "$10,000", 0.85f, Color(0xFF10B981), 0)
            AnimatedGoalItem("Europe Trip", "$2,100", "$5,000", 0.42f, Color(0xFF6B46C1), 300)
        }
    }
}

@Composable
fun AnimatedGoalItem(title: String, current: String, target: String, targetProgress: Float, color: Color, delayMs: Long) {
    var progress by remember { mutableStateOf(0f) }
    var hasAnimated by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (!hasAnimated) {
            delay(800 + delayMs) // Shorter delay
            animate(
                initialValue = 0f,
                targetValue = targetProgress,
                animationSpec = tween(1000, easing = EaseOutCubic) // Faster animation
            ) { value, _ ->
                progress = value
            }
            hasAnimated = true
        } else {
            progress = targetProgress // Show final value immediately if already animated
        }
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            )
            Text(
                "$current / $target",
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            )
        }
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color(0xFFF3F4F6)
        )
        
        val remaining = target.replace("$", "").replace(",", "").toInt() - current.replace("$", "").replace(",", "").toInt()
        val months = (remaining / (current.replace("$", "").replace(",", "").toInt() / 12.0)).toInt()
        
        Text(
            "$${remaining.toString().replace(Regex("(\\d)(?=(\\d{3})+(?!\\d))"), "$1,")} to go • ${months} months at current rate",
            fontSize = 12.sp,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
fun GoalItem(title: String, current: String, target: String, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            )
            Text(
                "$current / $target",
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            )
        }
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color(0xFFF3F4F6)
        )
        
        val remaining = target.replace("$", "").replace(",", "").toInt() - current.replace("$", "").replace(",", "").toInt()
        val months = (remaining / (current.replace("$", "").replace(",", "").toInt() / 12.0)).toInt()
        
        Text(
            "$${remaining.toString().replace(Regex("(\\d)(?=(\\d{3})+(?!\\d))"), "$1,")} to go • ${months} months at current rate",
            fontSize = 12.sp,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
fun ChatContent(paddingValues: PaddingValues, onNavigateToChat: () -> Unit) {
    // This will trigger navigation to the actual chat screen
    LaunchedEffect(Unit) {
        onNavigateToChat()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFF00D4AA))
    }
}

@Composable
fun SettingsContent(paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(paddingValues),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier.padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Settings",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    "Manage your account and preferences",
                    fontSize = 16.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
        
        // Bank Connection Section
        item {
            SettingsBankConnectionCard()
        }
        
        // Account Settings
        item {
            SettingsSection(
                title = "Account",
                items = listOf(
                    SettingsItem("Connected Accounts", Icons.Default.AccountBalance, "Manage your linked bank accounts"),
                    SettingsItem("Privacy Settings", Icons.Default.Lock, "Control your data and privacy"),
                    SettingsItem("Data Management", Icons.Default.Storage, "Export or delete your data")
                )
            )
        }
        
        // App Settings
        item {
            SettingsSection(
                title = "App",
                items = listOf(
                    SettingsItem("Notifications", Icons.Default.Notifications, "Manage alerts and reminders"),
                    SettingsItem("Help & Support", Icons.Default.Help, "Get help or contact support"),
                    SettingsItem("About", Icons.Default.Info, "App version and legal info")
                )
            )
        }
        
        // Logout
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Handle logout */ }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Sign Out",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFEF4444)
                        )
                        Text(
                            "Sign out of your North account",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BankConnectionCard() {
    var connectionStatus by remember { mutableStateOf<String?>(null) }
    var isConnected by remember { mutableStateOf(false) }
    
    if (!isConnected) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF00D4AA)),
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
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "Bank",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            "Connect Your Bank",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Get personalized insights",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                
                Text(
                    "Securely connect your Canadian bank accounts to unlock AI-powered financial advice and real-time spending insights.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                
                // Connection status
                connectionStatus?.let { status ->
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            status,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                // Clean button that calls the Plaid service
                BankConnectButton(
                    onSuccess = { publicToken ->
                        connectionStatus = "🎉 Successfully connected! Your bank account is now linked."
                        isConnected = true
                        println("✅ Bank connection successful! Public token: $publicToken")
                    },
                    onError = { error ->
                        connectionStatus = "❌ Connection failed: $error"
                        println("❌ Bank connection failed: $error")
                    },
                    onStatusUpdate = { status ->
                        connectionStatus = status
                    }
                )
                
                // Security info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Bank-level security • Read-only access • Canadian data protection",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsBankConnectionCard() {
    var isConnected by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = "Bank Accounts",
                    tint = Color(0xFF00D4AA),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        "Bank Accounts",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        if (isConnected) "Connected" else "0 accounts connected",
                        fontSize = 14.sp,
                        color = if (isConnected) Color(0xFF00D4AA) else Color(0xFF6B7280)
                    )
                }
            }
            
            if (!isConnected) {
                Text(
                    "Connect your bank accounts to get personalized insights and AI-powered financial advice.",
                    color = Color(0xFF6B7280),
                    fontSize = 14.sp
                )
                
                BankConnectButton(
                    onSuccess = { publicToken ->
                        isConnected = true
                        println("✅ Bank connected from Settings! Public token: $publicToken")
                    },
                    onError = { error ->
                        println("❌ Bank connection failed from Settings: $error")
                    },
                    onStatusUpdate = { /* No status display in settings */ },
                    buttonColor = Color(0xFF00D4AA),
                    textColor = Color.White
                )
            } else {
                Text(
                    "✅ Your bank account is successfully connected and syncing transactions.",
                    color = Color(0xFF00D4AA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    items: List<SettingsItem>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* Handle item click */ }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                item.description,
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Navigate",
                            tint = Color(0xFF6B7280).copy(alpha = 0.6f)
                        )
                    }
                    
                    if (index < items.size - 1) {
                        Divider(
                            color = Color(0xFFF3F4F6),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BankConnectButton(
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
    onStatusUpdate: (String) -> Unit,
    buttonColor: Color = Color.White,
    textColor: Color = Color(0xFF00D4AA)
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isConnecting by remember { mutableStateOf(false) }
    
    Button(
        onClick = {
            isConnecting = true
            onStatusUpdate("🔐 Creating secure connection...")
            
            coroutineScope.launch {
                connectBankAccount(
                    context = context,
                    onSuccess = { publicToken ->
                        onSuccess(publicToken)
                        isConnecting = false
                    },
                    onError = { error ->
                        onError(error)
                        isConnecting = false
                    },
                    onStatusUpdate = onStatusUpdate
                )
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                    color = textColor
                )
                Text(
                    "Connecting...",
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else {
            Text(
                "Connect Bank Account",
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// Consolidated bank connection logic
private suspend fun connectBankAccount(
    context: android.content.Context,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
    onStatusUpdate: (String) -> Unit
) {
    try {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
        
        val response = client.post("https://north-api-clean-production.up.railway.app/api/plaid/create-link-token") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("user_id", "north-user-${System.currentTimeMillis()}")
            })
        }
        
        if (response.status.isSuccess()) {
            val responseBody = response.body<JsonObject>()
            val linkToken = responseBody["link_token"]?.jsonPrimitive?.content
            
            if (linkToken != null) {
                onStatusUpdate("🚀 Opening secure connection...")
                
                val plaidLauncher = com.north.mobile.ui.accounts.PlaidLinkLauncher(context)
                plaidLauncher.launchPlaidLink(
                    linkToken = linkToken,
                    onSuccess = onSuccess,
                    onError = onError
                )
            } else {
                onError("Failed to create secure connection")
            }
        } else {
            onError("Connection failed. Please try again.")
        }
    } catch (e: Exception) {
        onError("Connection failed: ${e.message}")
    }
}

@Composable
fun DynamicInsightsCard() {
    var insights by remember { mutableStateOf<List<com.north.mobile.data.api.SpendingInsight>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        try {
            val apiClient = com.north.mobile.data.api.ApiClient()
            val insightsService = com.north.mobile.data.api.InsightsApiService(apiClient)
            val result = insightsService.getInsights()
            
            if (result.isSuccess) {
                insights = result.getOrNull() ?: emptyList()
            }
        } catch (e: Exception) {
            println("Error loading insights: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Insights",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "💡 Smart Insights",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }
            
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFF59E0B)
                    )
                    Text(
                        "Analyzing your spending...",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            } else if (insights.isEmpty()) {
                Text(
                    "Connect your bank account to get personalized insights about your spending patterns.",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            } else {
                // Show top 2 insights
                insights.take(2).forEach { insight ->
                    Surface(
                        color = when (insight.insight_type) {
                            "spending_pattern" -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                            "budget_alert" -> Color(0xFFEF4444).copy(alpha = 0.1f)
                            "saving_opportunity" -> Color(0xFF10B981).copy(alpha = 0.1f)
                            else -> Color(0xFF6B7280).copy(alpha = 0.1f)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                insight.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                insight.description,
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                            if (insight.amount != null) {
                                Text(
                                    "$${insight.amount.toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (insight.insight_type) {
                                        "saving_opportunity" -> Color(0xFF10B981)
                                        "budget_alert" -> Color(0xFFEF4444)
                                        else -> Color(0xFFF59E0B)
                                    }
                                )
                            }
                        }
                    }
                }
                
                if (insights.size > 2) {
                    Text(
                        "+${insights.size - 2} more insights available",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicGoalsCard() {
    var goals by remember { mutableStateOf<List<com.north.mobile.data.api.DynamicGoal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        try {
            val apiClient = com.north.mobile.data.api.ApiClient()
            val insightsService = com.north.mobile.data.api.InsightsApiService(apiClient)
            val result = insightsService.getGoals()
            
            if (result.isSuccess) {
                goals = result.getOrNull() ?: emptyList()
            }
        } catch (e: Exception) {
            println("Error loading goals: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Goals",
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "🎯 Smart Goals",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }
            
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF8B5CF6)
                    )
                    Text(
                        "Creating personalized goals...",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            } else if (goals.isEmpty()) {
                Text(
                    "Connect your bank account to get AI-generated financial goals based on your spending patterns.",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            } else {
                // Show top 2 goals
                goals.take(2).forEach { goal ->
                    Surface(
                        color = Color(0xFF8B5CF6).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                goal.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                goal.description,
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                            
                            // Progress bar
                            val progress = if (goal.target_amount > 0) {
                                (goal.current_amount / goal.target_amount).coerceIn(0.0, 1.0)
                            } else 0.0
                            
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "$${goal.current_amount.toInt()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8B5CF6)
                                    )
                                    Text(
                                        "$${goal.target_amount.toInt()}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                                
                                LinearProgressIndicator(
                                    progress = progress.toFloat(),
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = Color(0xFF8B5CF6),
                                    trackColor = Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
                
                if (goals.size > 2) {
                    Text(
                        "+${goals.size - 2} more goals available",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

data class SettingsItem(
    val title: String,
    val icon: ImageVector,
    val description: String
)

@Composable
fun DynamicInsightsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFDCFDF7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Insights",
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    "AI Insights",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }
            
            // Sample insights - these would come from the InsightsRepository
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InsightItem(
                    title = "Dining Spending Alert",
                    description = "Your dining expenses increased 18% this month ($450 vs $380 last month)",
                    type = "alert",
                    confidence = 0.92
                )
                
                InsightItem(
                    title = "Savings Opportunity",
                    description = "You could save $150/month by cooking 2 more meals at home",
                    type = "opportunity",
                    confidence = 0.85
                )
                
                InsightItem(
                    title = "Goal Progress",
                    description = "At current rate, you'll reach your Europe trip goal 2 months early",
                    type = "goal",
                    confidence = 0.78
                )
            }
            
            TextButton(
                onClick = onNavigateToInsights,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "View All Insights",
                    color = Color(0xFF00D4AA),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun InsightItem(
    title: String,
    description: String,
    type: String,
    confidence: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = when (type) {
                "alert" -> Icons.Default.Warning
                "opportunity" -> Icons.Default.TrendingUp
                "goal" -> Icons.Default.Flag
                else -> Icons.Default.Info
            },
            contentDescription = null,
            tint = when (type) {
                "alert" -> Color(0xFFEF4444)
                "opportunity" -> Color(0xFF10B981)
                "goal" -> Color(0xFF6B46C1)
                else -> Color(0xFF6B7280)
            },
            modifier = Modifier.size(16.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            )
            Text(
                description,
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
                lineHeight = 18.sp
            )
        }
        
        Text(
            "${(confidence * 100).toInt()}%",
            fontSize = 12.sp,
            color = Color(0xFF6B7280),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DynamicGoalsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFEE2E2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Goals",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    "AI-Generated Goals",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }
            
            // Sample AI-generated goals - these would come from the InsightsRepository
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DynamicGoalItem(
                    title = "Reduce Dining Budget",
                    description = "Save $150/month by setting a $400 dining limit",
                    targetAmount = 400.0,
                    currentAmount = 450.0,
                    isReduction = true,
                    priority = 9
                )
                
                DynamicGoalItem(
                    title = "Emergency Fund Boost",
                    description = "Complete your emergency fund in 2 months",
                    targetAmount = 10000.0,
                    currentAmount = 8500.0,
                    isReduction = false,
                    priority = 8
                )
                
                DynamicGoalItem(
                    title = "Grocery Optimization",
                    description = "Maintain excellent grocery discipline",
                    targetAmount = 300.0,
                    currentAmount = 295.0,
                    isReduction = false,
                    priority = 6
                )
            }
            
            TextButton(
                onClick = { /* Navigate to goals management */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Manage All Goals",
                    color = Color(0xFF00D4AA),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun DynamicGoalItem(
    title: String,
    description: String,
    targetAmount: Double,
    currentAmount: Double,
    isReduction: Boolean,
    priority: Int
) {
    val progress = if (isReduction) {
        // For reduction goals, progress is inverse (lower spending = better)
        ((targetAmount / currentAmount).coerceAtMost(1.0)).toFloat()
    } else {
        // For savings goals, normal progress
        (currentAmount / targetAmount).toFloat()
    }
    
    val progressColor = when {
        progress >= 0.8f -> Color(0xFF10B981)
        progress >= 0.5f -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F2937)
                )
                Text(
                    description,
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
            }
            
            // Priority indicator
            Surface(
                color = when {
                    priority >= 8 -> Color(0xFFDC2626).copy(alpha = 0.1f)
                    priority >= 6 -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                    else -> Color(0xFF6B7280).copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    "P$priority",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        priority >= 8 -> Color(0xFFDC2626)
                        priority >= 6 -> Color(0xFFF59E0B)
                        else -> Color(0xFF6B7280)
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = Color(0xFFF3F4F6)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (isReduction) "Target: $${targetAmount.toInt()}" else "$${currentAmount.toInt()} / $${targetAmount.toInt()}",
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
            Text(
                "${(progress * 100).toInt()}%",
                fontSize = 12.sp,
                color = progressColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}