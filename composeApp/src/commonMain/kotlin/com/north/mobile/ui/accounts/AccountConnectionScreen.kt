package com.north.mobile.ui.accounts

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountConnectionScreen(
    viewModel: AccountConnectionViewModel,
    onBackClick: () -> Unit,
    onConnectionComplete: () -> Unit = {},
    onLaunchPlaidLink: ((linkToken: String, onResult: (publicToken: String?) -> Unit) -> Unit)? = null
) {
    val uiState = viewModel.uiState
    
    // Handle connection success
    LaunchedEffect(uiState.connectionSuccess) {
        if (uiState.connectionSuccess) {
            onConnectionComplete()
            viewModel.resetConnectionState()
        }
    }
    
    // Show error snackbar if needed
    val errorMessage = uiState.connectionError ?: uiState.loadError
    errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Show error message
            viewModel.clearError()
        }
    }
    
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
            when (uiState.connectionStep) {
                ConnectionStep.NOT_STARTED -> {
                    ConnectionStartScreen(
                        onStartConnection = { viewModel.startAccountConnection() },
                        onLaunchPlaidLink = onLaunchPlaidLink
                    )
                }
                ConnectionStep.INITIALIZING -> {
                    ConnectionLoadingScreen("Initializing connection...")
                }
                ConnectionStep.SELECTING_INSTITUTION -> {
                    ConnectionLoadingScreen("Select your bank...")
                }
                ConnectionStep.AUTHENTICATING -> {
                    ConnectionLoadingScreen("Authenticating with your bank...")
                }
                ConnectionStep.EXCHANGING_TOKEN -> {
                    ConnectionLoadingScreen("Securely connecting your account...")
                }
                ConnectionStep.COMPLETED -> {
                    ConnectionSuccessScreen(onContinue = onConnectionComplete)
                }
                ConnectionStep.ERROR -> {
                    ConnectionErrorScreen(
                        errorMessage = uiState.connectionError ?: "An error occurred during connection",
                        onRetry = { viewModel.startAccountConnection() },
                        onCancel = onBackClick
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionStartScreen(
    onStartConnection: () -> Unit,
    onLaunchPlaidLink: ((linkToken: String, onResult: (publicToken: String?) -> Unit) -> Unit)? = null
) {
    var isLaunchingPlaid by remember { mutableStateOf(false) }
    var plaidError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
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
        
        SecurityFeaturesList()
        
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
        if (onLaunchPlaidLink != null) {
            Button(
                onClick = {
                    isLaunchingPlaid = true
                    plaidError = null
                    scope.launch {
                        try {
                            // Simulate fetching a link token from the backend
                            // In real code, this should be done via the ViewModel or a callback
                            // For now, just call the callback with a placeholder
                            onLaunchPlaidLink("link-sandbox-placeholder") { publicToken ->
                                isLaunchingPlaid = false
                                if (publicToken == null) {
                                    plaidError = "Plaid Link was cancelled."
                                } else {
                                    // Optionally, trigger ViewModel to exchange the token here
                                }
                            }
                        } catch (e: Exception) {
                            isLaunchingPlaid = false
                            plaidError = e.message ?: "Failed to launch Plaid."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLaunchingPlaid
            ) {
                Text(if (isLaunchingPlaid) "Launching Plaid..." else "Connect with Plaid", fontSize = 16.sp)
            }
            if (plaidError != null) {
                Text(
                    text = plaidError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        TextButton(onClick = {}) {
            Text("How does this work?", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SecurityFeaturesList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SecurityFeatureItem(
            icon = Icons.Default.Lock,
            title = "Bank-level security",
            description = "Your credentials are never stored on our servers"
        )
        
        SecurityFeatureItem(
            icon = Icons.Default.RemoveRedEye,
            title = "Read-only access",
            description = "We can't move money or make changes to your account"
        )
        
        SecurityFeatureItem(
            icon = Icons.Default.Lock,
            title = "Data encryption",
            description = "Your data is encrypted with 256-bit encryption"
        )
    }
}

@Composable
fun SecurityFeatureItem(
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
fun ConnectionLoadingScreen(message: String) {
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
fun ConnectionSuccessScreen(onContinue: () -> Unit) {
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
fun ConnectionErrorScreen(
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