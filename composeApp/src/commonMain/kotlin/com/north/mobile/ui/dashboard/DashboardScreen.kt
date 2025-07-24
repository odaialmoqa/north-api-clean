package com.north.mobile.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.north.mobile.data.api.ChatMessage
import com.north.mobile.ui.accounts.PlaidLinkButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class DashboardTab(
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    Home("Home", Icons.Outlined.Home, Icons.Filled.Home),
    Goals("Goals", Icons.Outlined.Star, Icons.Filled.Star),
    Accounts("Accounts", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle),
    Insights("Insights", Icons.Outlined.Info, Icons.Filled.Info),
    CFO("CFO", Icons.Outlined.Email, Icons.Filled.Email)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.Home) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // North logo (star/diamond shape)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFD700),
                                            Color(0xFFFFA000)
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "⭐",
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                        
                        Text(
                            "North",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2563EB),
                    titleContentColor = Color.White
                ),
                actions = {
                    // Points indicator
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Points",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "1,247",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                DashboardTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == tab) tab.selectedIcon else tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith 
                fadeOut(animationSpec = tween(300))
            },
            label = "tabContent"
        ) { tab ->
            when (tab) {
                DashboardTab.Home -> HomeTabContent(paddingValues, onNavigateToChat)
                DashboardTab.Goals -> GoalsTabContent(paddingValues)
                DashboardTab.Accounts -> AccountsTabContent(paddingValues)
                DashboardTab.Insights -> InsightsTabContent(paddingValues)
                DashboardTab.CFO -> CFOTabContent(paddingValues, onNavigateToChat)
            }
        }
    }
}

@Composable
fun HomeTabContent(paddingValues: PaddingValues, onNavigateToChat: () -> Unit = {}) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { PersonalCFOStartCard(onNavigateToChat) }
        item { DailyChallengeCard() }
        item { GamificationProgressCard() }
        item { ActiveStreaksCard() }
        item { FinancialOverviewCard() }
        item { QuickActionsCard() }
        item { RecentAchievementsCard() }
        item { MicroWinsCard() }
    }
}

@Composable
fun DailyChallengeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "TODAY'S CHALLENGE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    "Save $5 on Coffee",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Make coffee at home instead of buying it",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color(0xFFFFD700),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "+50 PTS",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        "4 hours left",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("☕", fontSize = 24.sp)
            }
        }
    }
}

@Composable
fun GamificationProgressCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Level 5 Financial Explorer",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "1,247 / 1,600 points to Level 6",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFD700), Color(0xFFFFA000))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "5",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            LinearProgressIndicator(
                progress = 0.78f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF4CAF50),
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
            
            Text(
                "353 points until next level unlock!",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ActiveStreaksCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Active Streaks",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Streaks",
                    tint = Color(0xFFFF5722)
                )
            }
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(3) { index ->
                    val streaks = listOf(
                        Triple("Daily Check-in", "7", "🔥"),
                        Triple("Under Budget", "12", "💰"),
                        Triple("Goal Progress", "5", "🎯")
                    )
                    val (title, count, emoji) = streaks[index]
                    StreakCard(title = title, count = count, emoji = emoji)
                }
            }
        }
    }
}

@Composable
fun StreakCard(title: String, count: String, emoji: String) {
    Card(
        modifier = Modifier.width(120.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(emoji, fontSize = 24.sp)
            Text(
                count,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                title,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun FinancialOverviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Financial Overview",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CircularProgressMetric("Savings", 0.65f, Color(0xFF4CAF50), "$1,407")
                CircularProgressMetric("Budget", 0.42f, Color(0xFF2196F3), "58% left")
                CircularProgressMetric("Goals", 0.78f, Color(0xFFFF9800), "2/3 met")
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FinancialMetric("Total Balance", "$23,251.19", "+2.3%", true)
                FinancialMetric("Monthly Savings", "$1,407.85", "+12.5%", true)
            }
        }
    }
}

@Composable
fun CircularProgressMetric(label: String, value: Float, color: Color, amount: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            CircularProgressIndicator(
                progress = 1f,
                modifier = Modifier.fillMaxSize(),
                color = Color.LightGray.copy(alpha = 0.3f),
                strokeWidth = 8.dp,
            )
            CircularProgressIndicator(
                progress = value,
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 8.dp,
            )
            Text(
                text = "${(value * 100).toInt()}%",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(
            text = amount,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun FinancialMetric(label: String, value: String, change: String, isPositive: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            change,
            fontSize = 12.sp,
            color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFEF4444)
        )
    }
}

@Composable
fun QuickActionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(4) { index ->
                    val actions = listOf(
                        Triple("Check Balance", Icons.Default.AccountCircle, Color(0xFF2196F3)),
                        Triple("Add Goal", Icons.Default.Add, Color(0xFF4CAF50)),
                        Triple("Categorize", Icons.Default.Star, Color(0xFFFF9800)),
                        Triple("Review Insights", Icons.Default.Info, Color(0xFF9C27B0))
                    )
                    val (title, icon, color) = actions[index]
                    QuickActionButton(title, icon, color) { }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            title,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp)
        )
    }
}

@Composable
fun RecentAchievementsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Achievements", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = {}) { Text("View All") }
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    val achievements = listOf(
                        Triple("🏆", "Budget Master", "Stayed under budget for 3 months"),
                        Triple("🎯", "Goal Setter", "Created your first financial goal"),
                        Triple("💰", "Saver", "Saved your first $100")
                    )
                    val (emoji, title, description) = achievements[index]
                    AchievementItem(emoji, title, description, when (index) { 0 -> 100; 1 -> 50; else -> 25 })
                }
            }
        }
    }
}

@Composable
fun AchievementItem(emoji: String, title: String, description: String, points: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 24.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "+$points",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun MicroWinsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Available Micro-Wins", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    val microWins = listOf(
                        Triple("Check Your Balance", "Stay on top of your finances", 5),
                        Triple("Categorize 3 Transactions", "Help us understand your spending", 30),
                        Triple("Review Your Insights", "Discover new ways to save", 10)
                    )
                    val (title, description, points) = microWins[index]
                    MicroWinItem(title, description, points) { }
                }
            }
        }
    }
}

@Composable
fun MicroWinItem(title: String, description: String, points: Int, onComplete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = Color(0xFFFFD700),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    "+$points",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Button(
                onClick = onComplete,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("Complete", fontSize = 12.sp)
            }
        }
    }
}

// Placeholder tabs - simplified for now
@Composable
fun GoalsTabContent(paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎯 Goals Tab", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Goal management interface coming soon",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun AccountsTabContent(paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                "Your Accounts",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Plaid connection card
        item {
            PlaidConnectionCard()
        }
        
        // Info about security
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security",
                        tint = Color(0xFF4CAF50)
                    )
                    Column {
                        Text(
                            "Bank-level Security",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Your data is encrypted and never stored on our servers",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaidConnectionCard() {
    PlaidLinkButton(
        onSuccess = { publicToken ->
            println("Plaid Link successful! Public token: $publicToken")
            // Here you would typically:
            // 1. Send the public token to your backend
            // 2. Exchange it for an access token
            // 3. Fetch account data
            // 4. Update the UI to show connected accounts
        },
        onError = { error ->
            println("Plaid Link error: $error")
            // Handle error - show user-friendly message
        }
    )
}

@Composable
fun InsightsTabContent(paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1).copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📊 Insights Tab", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Analytics and insights display coming soon",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun CFOTabContent(paddingValues: PaddingValues, onNavigateToChat: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // CFO Hero Image
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFF10B981), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("💝", fontSize = 60.sp)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title
        Text(
            "Your Personal CFO",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        Text(
            "I'm here to help you with your finances! Let's chat about your goals, spending, or any financial questions you have.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Start Conversation Button
        Button(
            onClick = onNavigateToChat,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            modifier = Modifier.fillMaxWidth(0.8f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Start Conversation with Your CFO",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
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
            // CFO avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF10B981), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("👨‍💼", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser) 
                    Color(0xFF2563EB) 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 16.dp
            )
        ) {
            Text(
                message.message,
                modifier = Modifier.padding(12.dp),
                color = if (message.isFromUser) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
        
        if (message.isFromUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // User avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF2563EB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
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
            Text("👨‍💼", fontSize = 20.sp)
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

@Composable
fun ChatInputArea(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type your message...") },
                enabled = enabled,
                maxLines = 3
            )
            
            Button(
                onClick = onSendMessage,
                enabled = enabled && message.isNotBlank(),
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp)
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

// Simple response generation for testing - replace with actual API call
fun generateCFOResponse(userMessage: String, conversationHistory: List<ChatMessage>): String {
    val responses = when {
        userMessage.contains("name", ignoreCase = true) -> listOf(
            "Nice to meet you! I'm excited to be your financial coach. Tell me, what's your biggest financial goal right now?",
            "Great! Now that we're acquainted, what would you like to work on first - saving, budgeting, or planning for something specific?"
        )
        userMessage.contains("save", ignoreCase = true) || userMessage.contains("saving", ignoreCase = true) -> listOf(
            "Saving is fantastic! What are you hoping to save for? A vacation, emergency fund, or something else?",
            "I love that you want to save! Let's figure out a realistic amount. How much do you think you could comfortably set aside each month?"
        )
        userMessage.contains("budget", ignoreCase = true) -> listOf(
            "Budgeting is key to financial success! Let's start simple - do you know roughly how much you spend each month?",
            "Great choice! A good budget is like a roadmap for your money. What's your biggest spending category right now?"
        )
        userMessage.contains("debt", ignoreCase = true) -> listOf(
            "I understand dealing with debt can be stressful. Let's create a plan to tackle it together. What type of debt is your biggest concern?",
            "Debt payoff is totally achievable! The key is having the right strategy. Are you dealing with credit cards, student loans, or something else?"
        )
        userMessage.contains("vacation", ignoreCase = true) || userMessage.contains("trip", ignoreCase = true) -> listOf(
            "A vacation sounds amazing! Where are you thinking of going and when? This will help me calculate how much you need to save.",
            "I love vacation goals - they're so motivating! What's your dream destination and rough budget for the trip?"
        )
        else -> listOf(
            "That's interesting! Tell me more about your financial situation so I can give you the best advice.",
            "I'm here to help with whatever financial goals you have. What's the most important thing you'd like to achieve?",
            "Thanks for sharing! What aspect of your finances would you like to focus on first?"
        )
    }
    return responses.random()
}

@Composable
fun AICFOWelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981)),
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
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👨‍💼", fontSize = 28.sp)
                }
                
                Column {
                    Text(
                        "👋 Hey! I'm your Personal CFO!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Coming Soon",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            
            Text(
                "Let's have a conversation to get to know you well. I'll create personalized financial goals and help you get financially fit in no time!",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Text(
                "Instead of filling out boring forms, we'll just chat! I'll learn about your lifestyle, interests, and financial priorities to create a plan that actually works for you.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Button(
                onClick = { /* Coming soon */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            ) {
                Text(
                    "Let's Get Started! (Coming Soon)",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PersonalCFOStartCard(onNavigateToChat: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981)),
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
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👨‍💼", fontSize = 28.sp)
                }
                
                Column {
                    Text(
                        "Meet Your Personal CFO!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Ready to chat?",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            
            Text(
                "I'm here to help you achieve your financial goals through friendly conversation. No boring forms - just tell me about your life and I'll create a personalized plan for you!",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Button(
                onClick = onNavigateToChat,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Start Our Conversation",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}