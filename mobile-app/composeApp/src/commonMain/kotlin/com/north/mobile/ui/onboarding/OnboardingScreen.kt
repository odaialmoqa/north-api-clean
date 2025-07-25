package com.north.mobile.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Celebration
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to North",
            description = "Your personal finance app that helps you manage your money with confidence and ease.",
            icon = Icons.Default.Celebration,
            iconTint = Color(0xFF10B981)
        ),
        OnboardingPage(
            title = "Meet Your Personal CFO",
            description = "Your friendly financial advisor that helps you make smart decisions about your money.",
            icon = Icons.Default.Assistant,
            iconTint = Color(0xFF3B82F6)
        ),
        OnboardingPage(
            title = "Connect Your Accounts",
            description = "Securely link your bank accounts to get a complete picture of your finances.",
            icon = Icons.Default.AccountBalance,
            iconTint = Color(0xFF8B5CF6)
        )
    )
    
    Scaffold(
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Page content
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon illustration
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(pages[currentPage].iconTint.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = pages[currentPage].icon,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = pages[currentPage].iconTint
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Title
                Text(
                    text = pages[currentPage].title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Description
                Text(
                    text = pages[currentPage].description,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    lineHeight = 24.sp
                )
            }
            
            // Bottom navigation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    repeat(pages.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (currentPage == index) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (currentPage == index) {
                                        pages[currentPage].iconTint
                                    } else {
                                        Color.LightGray
                                    }
                                )
                        )
                    }
                }
                
                // Navigation buttons
                if (currentPage < pages.size - 1) {
                    Button(
                        onClick = { currentPage++ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pages[currentPage].iconTint
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Back/Skip button
                if (currentPage > 0) {
                    TextButton(
                        onClick = { currentPage-- },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Back", color = Color.Gray)
                    }
                } else {
                    TextButton(
                        onClick = onComplete,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Skip", color = Color.Gray)
                    }
                }
            }
        }
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconTint: Color
)