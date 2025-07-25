package com.north.mobile.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleAccountConnectionScreen(
    onBackClick: () -> Unit,
    onConnectionComplete: () -> Unit = {}
) {
    var connectionStep by remember { mutableStateOf(SimpleConnectionStep.NOT_STARTED) }
    var isConnecting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect Account") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (connectionStep) {
                SimpleConnectionStep.NOT_STARTED -> {
                    SimpleConnectionStartScreen(
                        onStartConnection = {
                            isConnecting = true
                            connectionStep = SimpleConnectionStep.CONNECTING
                            // Simulate connection process
                            coroutineScope.launch {
                                delay(3000)
                                connectionStep = SimpleConnectionStep.COMPLETED
                                isConnecting = false
                            }
                        }
                    )
                }
                SimpleConnectionStep.CONNECTING -> {
                    SimpleConnectionLoadingScreen("Connecting to your bank...")
                }
                SimpleConnectionStep.COMPLETED -> {
                    SimpleConnectionSuccessScreen(onContinue = onConnectionComplete)
                }
                SimpleConnectionStep.ERROR -> {
                    SimpleConnectionErrorScreen(
                        errorMessage = "Failed to connect account",
                        onRetry = { connectionStep = SimpleConnectionStep.NOT_STARTED },
                        onCancel = onBackClick
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleConnectionStartScreen(onStartConnection: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Connect Bank",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            "Connect Your Bank Account",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            "Securely connect your bank accounts to get personalized financial insights and recommendations.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SimpleSecurityFeaturesList()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onStartConnection,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Connect My Bank", fontSize = 16.sp)
        }
        
        TextButton(onClick = {}) {
            Text("How does this work?", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SimpleSecurityFeaturesList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SimpleSecurityFeatureItem(
            icon = Icons.Default.Lock,
            title = "Bank-level security",
            description = "Your credentials are never stored on our servers"
        )
        
        SimpleSecurityFeatureItem(
            icon = Icons.Default.Info,
            title = "Read-only access",
            description = "We can't move money or make changes to your account"
        )
        
        SimpleSecurityFeatureItem(
            icon = Icons.Default.Lock,
            title = "Data encryption",
            description = "Your data is encrypted with 256-bit encryption"
        )
    }
}

@Composable
fun SimpleSecurityFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Column {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SimpleConnectionLoadingScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            strokeWidth = 6.dp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            message,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "This may take a moment. Please don't close the app.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun SimpleConnectionSuccessScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            modifier = Modifier.size(100.dp),
            tint = Color(0xFF10B981)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "Account Connected!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Your account has been successfully connected. We're now analyzing your financial data to provide personalized insights.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Continue", fontSize = 16.sp)
        }
    }
}

@Composable
fun SimpleConnectionErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFEF4444)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "Connection Error",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            errorMessage,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Try Again", fontSize = 16.sp)
        }
        
        TextButton(onClick = onCancel) {
            Text("Cancel", color = MaterialTheme.colorScheme.primary)
        }
    }
}

enum class SimpleConnectionStep {
    NOT_STARTED,
    CONNECTING,
    COMPLETED,
    ERROR
}