package com.north.mobile.ui.accounts

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedAccountsScreen(
    viewModel: AccountConnectionViewModel,
    onBackClick: () -> Unit,
    onAddAccount: () -> Unit = {},
    onAccountDetails: (String) -> Unit = {}
) {
    val uiState = viewModel.uiState
    
    // Handle disconnect success
    LaunchedEffect(uiState.disconnectSuccess) {
        if (uiState.disconnectSuccess) {
            // Show success message
            viewModel.resetConnectionState()
        }
    }
    
    // Show error snackbar if needed
    val errorMessage = uiState.disconnectError ?: uiState.loadError
    errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Show error message
            viewModel.clearError()
        }
    }
    
    // Load accounts when screen is shown
    LaunchedEffect(Unit) {
        viewModel.loadConnectedAccounts()
    }
    
    // Convert Plaid accounts to UI model
    val connectedAccounts = uiState.connectedAccounts.map { account ->
        ConnectedAccountInfo(
            id = account.id,
            name = account.name,
            institutionName = account.institutionName,
            accountType = account.type,
            balance = "$${String.format("%.2f", account.balance)}",
            lastSynced = "Recently",
            status = when (account.connectionStatus) {
                com.north.mobile.data.plaid.PlaidConnectionStatus.HEALTHY -> AccountStatus.HEALTHY
                com.north.mobile.data.plaid.PlaidConnectionStatus.SYNC_ERROR -> AccountStatus.ERROR
                com.north.mobile.data.plaid.PlaidConnectionStatus.NEEDS_REAUTH -> AccountStatus.NEEDS_ATTENTION
                com.north.mobile.data.plaid.PlaidConnectionStatus.DISCONNECTED -> AccountStatus.ERROR
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connected Accounts") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddAccount) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Account"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview card
            item {
                AccountsOverviewCard(connectedAccounts)
            }
            
            // Connected accounts list
            item {
                Text(
                    "Your Accounts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(connectedAccounts) { account ->
                ConnectedAccountCard(
                    account = account,
                    onAccountClick = { onAccountDetails(account.id) },
                    onDisconnectClick = { viewModel.disconnectAccount(account.id) },
                    isDisconnecting = uiState.isDeletingAccount == account.id
                )
            }
            
            // Add account card
            item {
                AddAccountCard(onClick = onAddAccount)
            }
            
            // Security info
            item {
                SecurityInfoCard()
            }
        }
    }
}

@Composable
fun AccountsOverviewCard(accounts: List<ConnectedAccountInfo>) {
    val totalBalance = accounts.sumOf { 
        it.balance.replace("$", "").replace(",", "").replace("-", "").toDoubleOrNull() ?: 0.0 
    }
    val accountCount = accounts.size
    val healthyAccounts = accounts.count { it.status == AccountStatus.HEALTHY }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF10B981).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = "Accounts",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(32.dp)
                )
                
                Column {
                    Text(
                        "Account Overview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        "All your financial accounts in one place",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OverviewStatItem("Total Accounts", "$accountCount", Icons.Default.AccountCircle)
                OverviewStatItem("Healthy", "$healthyAccounts", Icons.Default.CheckCircle)
                OverviewStatItem("Last Sync", "2 min ago", Icons.Default.Refresh)
            }
        }
    }
}

@Composable
fun OverviewStatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(20.dp)
        )
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10B981)
        )
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ConnectedAccountCard(
    account: ConnectedAccountInfo,
    onAccountClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    isDisconnecting: Boolean = false
) {
    var showDisconnectDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAccountClick() },
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = when (account.accountType) {
                            "Checking" -> Icons.Default.CreditCard
                            "Savings" -> Icons.Default.AccountCircle
                            "Credit" -> Icons.Default.CreditCard
                            else -> Icons.Default.AccountCircle
                        },
                        contentDescription = account.accountType,
                        tint = when (account.status) {
                            AccountStatus.HEALTHY -> Color(0xFF10B981)
                            AccountStatus.NEEDS_ATTENTION -> Color(0xFFF59E0B)
                            AccountStatus.ERROR -> Color(0xFFEF4444)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Column {
                        Text(
                            account.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${account.institutionName} • ${account.accountType}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        account.balance,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (account.balance.startsWith("-")) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Updated ${account.lastSynced}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when (account.status) {
                        AccountStatus.HEALTHY -> Color(0xFF10B981).copy(alpha = 0.1f)
                        AccountStatus.NEEDS_ATTENTION -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                        AccountStatus.ERROR -> Color(0xFFEF4444).copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = when (account.status) {
                                AccountStatus.HEALTHY -> Icons.Default.CheckCircle
                                AccountStatus.NEEDS_ATTENTION -> Icons.Default.Warning
                                AccountStatus.ERROR -> Icons.Default.Error
                            },
                            contentDescription = account.status.name,
                            tint = when (account.status) {
                                AccountStatus.HEALTHY -> Color(0xFF10B981)
                                AccountStatus.NEEDS_ATTENTION -> Color(0xFFF59E0B)
                                AccountStatus.ERROR -> Color(0xFFEF4444)
                            },
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            when (account.status) {
                                AccountStatus.HEALTHY -> "Connected"
                                AccountStatus.NEEDS_ATTENTION -> "Needs Attention"
                                AccountStatus.ERROR -> "Connection Error"
                            },
                            fontSize = 12.sp,
                            color = when (account.status) {
                                AccountStatus.HEALTHY -> Color(0xFF10B981)
                                AccountStatus.NEEDS_ATTENTION -> Color(0xFDF59E0B)
                                AccountStatus.ERROR -> Color(0xFFEF4444)
                            }
                        )
                    }
                }
                
                TextButton(
                    onClick = { showDisconnectDialog = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    enabled = !isDisconnecting
                ) {
                    if (isDisconnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disconnecting...", fontSize = 12.sp)
                    } else {
                        Text("Disconnect", fontSize = 12.sp)
                    }
                }
            }
        }
    }
    
    if (showDisconnectDialog) {
        DisconnectAccountDialog(
            accountName = account.name,
            onConfirm = {
                showDisconnectDialog = false
                onDisconnectClick()
            },
            onDismiss = { showDisconnectDialog = false }
        )
    }
}

@Composable
fun AddAccountCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3B82F6).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Account",
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(32.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Connect Another Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3B82F6)
                )
                Text(
                    "Add more accounts to get a complete financial picture",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = Color(0xFF3B82F6).copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun SecurityInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Security",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(24.dp)
            )
            
            Column {
                Text(
                    "Bank-Level Security",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Your data is encrypted and protected with the same security standards used by banks.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun DisconnectAccountDialog(
    accountName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Disconnect Account",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = { 
            Text("Are you sure you want to disconnect \"$accountName\"? This will remove all transaction data and insights for this account.") 
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "Disconnect",
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

// Data models
data class ConnectedAccountInfo(
    val id: String,
    val name: String,
    val institutionName: String,
    val accountType: String,
    val balance: String,
    val lastSynced: String,
    val status: AccountStatus
)

enum class AccountStatus {
    HEALTHY,
    NEEDS_ATTENTION,
    ERROR
}