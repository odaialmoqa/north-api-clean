package com.north.mobile.ui.accounts

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
import com.north.mobile.data.plaid.SimplePlaidTransaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsScreen(
    accountId: String,
    viewModel: AccountConnectionViewModel,
    onBackClick: () -> Unit
) {
    val uiState = viewModel.uiState
    
    // Find the selected account
    val account = uiState.connectedAccounts.find { it.id == accountId }
    
    // Handle refresh success
    LaunchedEffect(uiState.refreshSuccess) {
        if (uiState.refreshSuccess) {
            // Show success message
            viewModel.resetConnectionState()
        }
    }
    
    // Show error snackbar if needed
    val errorMessage = uiState.refreshError ?: uiState.loadError
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

    // Move mockTransactions here
    val mockTransactions = remember {
        listOf(
            SimplePlaidTransaction(
                id = "txn_1",
                accountId = account?.id ?: "",
                amount = -45.67,
                date = "2023-04-15",
                description = "Grocery Store Purchase",
                merchantName = "Whole Foods",
                category = listOf("Food and Drink", "Groceries")
            ),
            SimplePlaidTransaction(
                id = "txn_2",
                accountId = account?.id ?: "",
                amount = -12.50,
                date = "2023-04-14",
                description = "Coffee Shop",
                merchantName = "Starbucks",
                category = listOf("Food and Drink", "Coffee Shops")
            ),
            SimplePlaidTransaction(
                id = "txn_3",
                accountId = account?.id ?: "",
                amount = -89.99,
                date = "2023-04-13",
                description = "Monthly Subscription",
                merchantName = "Netflix",
                category = listOf("Entertainment", "Subscription")
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.name ?: "Account Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            if (account != null) {
                                viewModel.refreshAccount(account.id)
                            }
                        },
                        enabled = account != null && uiState.isRefreshingAccount != account.id
                    ) {
                        if (uiState.isRefreshingAccount == account?.id) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (account == null) {
            // Account not found
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoadingAccounts) {
                    CircularProgressIndicator()
                } else {
                    Text("Account not found")
                }
            }
        } else {
            // Account found, show details
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Account balance card
                item {
                    AccountBalanceCard(account)
                }
                
                // Account details
                item {
                    AccountInfoCard(account)
                }
                
                // Recent transactions
                item {
                    Text(
                        "Recent Transactions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(mockTransactions) { transaction ->
                    TransactionItem(transaction)
                }
                
                // Account management actions
                item {
                    AccountActionsCard(
                        onDisconnect = { viewModel.disconnectAccount(account.id) },
                        isDisconnecting = uiState.isDeletingAccount == account.id
                    )
                }
            }
        }
    }
}

@Composable
fun AccountBalanceCard(account: com.north.mobile.data.plaid.SimplePlaidAccount) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Current Balance",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            
            Text(
                "$${String.format("%.2f", account.balance)}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Last Updated",
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Updated recently",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AccountInfoCard(account: com.north.mobile.data.plaid.SimplePlaidAccount) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Account Information",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            AccountInfoRow("Account Name", account.name)
            AccountInfoRow("Account Type", "${account.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} ${account.subtype?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: ""}")
            AccountInfoRow("Institution", account.institutionName)
            AccountInfoRow("Status", account.connectionStatus.name)
        }
    }
}

@Composable
fun AccountInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TransactionItem(transaction: SimplePlaidTransaction) {
    val isExpense = transaction.amount < 0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category icon
            Surface(
                color = getCategoryColor(transaction.category.firstOrNull() ?: "").copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getCategoryIcon(transaction.category.firstOrNull() ?: ""),
                        contentDescription = transaction.category.firstOrNull() ?: "",
                        tint = getCategoryColor(transaction.category.firstOrNull() ?: ""),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Transaction details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.merchantName ?: transaction.description,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    "${transaction.date} • ${transaction.category.firstOrNull() ?: "Uncategorized"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Amount
            Text(
                if (isExpense) "-$${String.format("%.2f", Math.abs(transaction.amount))}" 
                else "+$${String.format("%.2f", transaction.amount)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isExpense) Color(0xFFEF4444) else Color(0xFF10B981)
            )
        }
    }
}

@Composable
fun AccountActionsCard(
    onDisconnect: () -> Unit,
    isDisconnecting: Boolean
) {
    var showDisconnectDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Account Management",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444)
            )
            
            Button(
                onClick = { showDisconnectDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444)
                ),
                enabled = !isDisconnecting
            ) {
                if (isDisconnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnecting...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Disconnect",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect Account")
                }
            }
        }
    }
    
    if (showDisconnectDialog) {
        DisconnectAccountDialog(
            accountName = "this account",
            onConfirm = {
                showDisconnectDialog = false
                onDisconnect()
            },
            onDismiss = { showDisconnectDialog = false }
        )
    }
}

// Helper functions
fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

@Composable
fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category.lowercase()) {
        "food and drink" -> Icons.Default.Restaurant
        "shopping" -> Icons.Default.ShoppingCart
        "entertainment" -> Icons.Default.Movie
        "travel" -> Icons.Default.Flight
        "transportation" -> Icons.Default.DirectionsCar
        "health" -> Icons.Default.LocalHospital
        "deposit" -> Icons.Default.AccountCircle
        "transfer" -> Icons.Default.SwapHoriz
        else -> Icons.Default.Receipt
    }
}

@Composable
fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "food and drink" -> Color(0xFFF59E0B) // Amber
        "shopping" -> Color(0xFF3B82F6) // Blue
        "entertainment" -> Color(0xFF8B5CF6) // Purple
        "travel" -> Color(0xFF10B981) // Emerald
        "transportation" -> Color(0xFF6366F1) // Indigo
        "health" -> Color(0xFFEF4444) // Red
        "deposit" -> Color(0xFF10B981) // Emerald
        "transfer" -> Color(0xFF6366F1) // Indigo
        else -> Color(0xFF64748B) // Slate
    }
}