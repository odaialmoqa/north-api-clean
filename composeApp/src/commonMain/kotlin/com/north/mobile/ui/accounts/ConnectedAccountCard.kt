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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.north.mobile.data.plaid.SimplePlaidAccount
import com.north.mobile.data.plaid.PlaidConnectionStatus
import java.text.NumberFormat
import java.util.*

@Composable
fun ConnectedAccountCard(
    account: SimplePlaidAccount,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    onViewDetails: () -> Unit,
    onSyncTransactions: () -> Unit,
    isLoading: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with account info and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        account.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        account.institutionName,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                
                ConnectionStatusBadge(account.connectionStatus)
            }
            
            // Balance and sync info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Balance",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        formatCurrency(account.balance),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (account.balance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Last sync",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        formatLastSync(account.lastSyncTime),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Action buttons based on connection status
            when (account.connectionStatus) {
                PlaidConnectionStatus.NEEDS_REAUTH -> {
                    Button(
                        onClick = onReconnect,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reconnect Account")
                    }
                }
                
                PlaidConnectionStatus.SYNC_ERROR -> {
                    OutlinedButton(
                        onClick = onSyncTransactions,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry Sync")
                    }
                }
                
                PlaidConnectionStatus.HEALTHY -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onViewDetails,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("View Details")
                        }
                        
                        OutlinedButton(
                            onClick = onSyncTransactions,
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                
                PlaidConnectionStatus.DISCONNECTED -> {
                    Text(
                        "Account disconnected",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
            
            // Disconnect option (only for connected accounts)
            if (account.connectionStatus != PlaidConnectionStatus.DISCONNECTED) {
                TextButton(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Disconnect Account", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusBadge(status: PlaidConnectionStatus) {
    val (color, text, icon) = when (status) {
        PlaidConnectionStatus.HEALTHY -> Triple(Color(0xFF10B981), "Connected", Icons.Default.CheckCircle)
        PlaidConnectionStatus.NEEDS_REAUTH -> Triple(Color(0xFFEF4444), "Needs Auth", Icons.Default.Warning)
        PlaidConnectionStatus.SYNC_ERROR -> Triple(Color(0xFFF59E0B), "Sync Error", Icons.Default.Error)
        PlaidConnectionStatus.DISCONNECTED -> Triple(Color(0xFF6B7280), "Disconnected", Icons.Default.Cancel)
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.CANADA)
    return formatter.format(amount)
}

private fun formatLastSync(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}