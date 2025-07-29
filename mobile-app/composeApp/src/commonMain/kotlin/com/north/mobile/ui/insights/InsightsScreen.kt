package com.north.mobile.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.north.mobile.data.api.SpendingInsight
import com.north.mobile.data.api.DynamicGoal
import com.north.mobile.data.api.SpendingPattern
import com.north.mobile.data.repository.InsightsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    insightsRepository: InsightsRepository,
    onNavigateBack: () -> Unit = {}
) {
    val insights by insightsRepository.insights.collectAsState()
    val goals by insightsRepository.goals.collectAsState()
    val spendingPatterns by insightsRepository.spendingPatterns.collectAsState()
    val isLoading by insightsRepository.isLoading.collectAsState()
    val error by insightsRepository.error.collectAsState()
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        insightsRepository.refreshAllData()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Text(
                text = "Financial Insights",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = {
                    scope.launch {
                        insightsRepository.analyzeTransactions()
                    }
                }
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Analyze Transactions",
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4CAF50))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error handling
                error?.let { errorMessage ->
                    item {
                        ErrorCard(
                            message = errorMessage,
                            onDismiss = { insightsRepository.clearError() }
                        )
                    }
                }
                
                // Quick Actions
                item {
                    QuickActionsCard(
                        onAnalyzeTransactions = {
                            scope.launch {
                                insightsRepository.analyzeTransactions()
                            }
                        },
                        onRefreshData = {
                            scope.launch {
                                insightsRepository.refreshAllData()
                            }
                        }
                    )
                }
                
                // Goals Section
                if (goals.isNotEmpty()) {
                    item {
                        Text(
                            text = "Your Goals",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(goals.take(3)) { goal ->
                        GoalCard(
                            goal = goal,
                            onUpdateProgress = { amount ->
                                scope.launch {
                                    insightsRepository.updateGoalProgress(goal.id, amount)
                                }
                            }
                        )
                    }
                }
                
                // Insights Section
                if (insights.isNotEmpty()) {
                    item {
                        Text(
                            text = "AI Insights",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(insights) { insight ->
                        InsightCard(
                            insight = insight,
                            onMarkAsRead = {
                                scope.launch {
                                    insightsRepository.markInsightAsRead(insight.id)
                                }
                            }
                        )
                    }
                }
                
                // Spending Patterns Section
                if (spendingPatterns.isNotEmpty()) {
                    item {
                        Text(
                            text = "Spending Patterns",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(spendingPatterns.take(5)) { pattern ->
                        SpendingPatternCard(pattern = pattern)
                    }
                }
                
                // Empty state
                if (insights.isEmpty() && goals.isEmpty() && spendingPatterns.isEmpty()) {
                    item {
                        EmptyStateCard()
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsCard(
    onAnalyzeTransactions: () -> Unit,
    onRefreshData: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAnalyzeTransactions,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze")
                }
                
                OutlinedButton(
                    onClick = onRefreshData,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh")
                }
            }
        }
    }
}

@Composable
fun GoalCard(
    goal: DynamicGoal,
    onUpdateProgress: (Double) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = goal.description,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                
                if (goal.aiGenerated) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI Generated",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = goal.progressPercentage / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFF2A2F3E)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$${goal.currentAmount.toInt()} / $${goal.targetAmount.toInt()}",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "${goal.progressPercentage}%",
                    color = Color(0xFF4CAF50),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (goal.remainingAmount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$${goal.remainingAmount.toInt()} remaining",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun InsightCard(
    insight: SpendingInsight,
    onMarkAsRead: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (insight.is_read) Color(0xFF1A1F2E) else Color(0xFF2A2F3E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when (insight.insight_type) {
                            "spending_alert" -> Icons.Default.Warning
                            "opportunity" -> Icons.Default.TrendingUp
                            "trend" -> Icons.Default.Timeline
                            "goal_suggestion" -> Icons.Default.Flag
                            else -> Icons.Default.Lightbulb
                        },
                        contentDescription = null,
                        tint = when (insight.insight_type) {
                            "spending_alert" -> Color(0xFFFF9800)
                            "opportunity" -> Color(0xFF4CAF50)
                            "trend" -> Color(0xFF2196F3)
                            "goal_suggestion" -> Color(0xFF9C27B0)
                            else -> Color(0xFFFFEB3B)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = insight.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (!insight.is_read) {
                    IconButton(onClick = onMarkAsRead) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Mark as read",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = insight.description,
                color = Color.Gray,
                fontSize = 14.sp
            )
            
            if (insight.action_items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Recommended Actions:",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                insight.action_items.forEach { action ->
                    Text(
                        text = "• $action",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
            
            if (insight.amount != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Amount: $${insight.amount.toInt()}",
                    color = Color(0xFF4CAF50),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SpendingPatternCard(pattern: SpendingPattern) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pattern.category,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${pattern.transaction_count} transactions",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${pattern.total_amount.toInt()}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                pattern.trend_direction?.let { trend ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when (trend) {
                                "increasing" -> Icons.Default.TrendingUp
                                "decreasing" -> Icons.Default.TrendingDown
                                else -> Icons.Default.TrendingFlat
                            },
                            contentDescription = null,
                            tint = when (trend) {
                                "increasing" -> Color(0xFFFF5722)
                                "decreasing" -> Color(0xFF4CAF50)
                                else -> Color.Gray
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = "${pattern.trend_percentage?.toInt() ?: 0}%",
                            color = when (trend) {
                                "increasing" -> Color(0xFFFF5722)
                                "decreasing" -> Color(0xFF4CAF50)
                                else -> Color.Gray
                            },
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3F1A1A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Analytics,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No insights yet",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Connect your bank account and analyze transactions to get personalized insights",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}