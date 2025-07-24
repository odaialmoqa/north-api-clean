package com.north.mobile.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.CleaningServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    onBackClick: () -> Unit,
    onExportData: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onSyncData: () -> Unit = {}
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isClearingCache by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Management") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Data overview
            item {
                DataOverviewCard()
            }
            
            // Data export section
            item {
                DataSection(
                    title = "Export & Backup",
                    items = listOf(
                        DataActionItem(
                            title = "Export All Data",
                            description = "Download a complete copy of your financial data",
                            icon = Icons.Default.Download,
                            onClick = {
                                isExporting = true
                                onExportData()
                                // Reset loading state after some time (in real app, this would be handled by the ViewModel)
                                kotlinx.coroutines.GlobalScope.launch {
                                    kotlinx.coroutines.delay(3000)
                                    isExporting = false
                                }
                            },
                            isLoading = isExporting
                        ),
                        DataActionItem(
                            title = "Sync Financial Data",
                            description = "Refresh your account and transaction data",
                            icon = Icons.Default.Sync,
                            onClick = {
                                isSyncing = true
                                onSyncData()
                                kotlinx.coroutines.GlobalScope.launch {
                                    kotlinx.coroutines.delay(2000)
                                    isSyncing = false
                                }
                            },
                            isLoading = isSyncing
                        )
                    )
                )
            }
            
            // Storage management
            item {
                DataSection(
                    title = "Storage Management",
                    items = listOf(
                        DataActionItem(
                            title = "Clear Cache",
                            description = "Free up space by clearing temporary data",
                            icon = Icons.Default.CleaningServices,
                            onClick = {
                                isClearingCache = true
                                onClearCache()
                                kotlinx.coroutines.GlobalScope.launch {
                                    kotlinx.coroutines.delay(1000)
                                    isClearingCache = false
                                }
                            },
                            isLoading = isClearingCache
                        )
                    )
                )
            }
            
            // Data retention info
            item {
                DataRetentionCard()
            }
            
            // Danger zone
            item {
                DangerZoneSection(
                    onDeleteAccount = { showDeleteConfirmation = true }
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        DeleteAccountConfirmationDialog(
            onConfirm = {
                showDeleteConfirmation = false
                onDeleteAccount()
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }
}

@Composable
fun DataOverviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3B82F6).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Data",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(32.dp)
                )
                
                Column {
                    Text(
                        "Your Data Overview",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6)
                    )
                    Text(
                        "Manage your financial data and privacy settings",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Data stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DataStatItem("Accounts", "3", Icons.Default.AccountBalance)
                DataStatItem("Transactions", "1,247", Icons.Default.Receipt)
                DataStatItem("Goals", "5", Icons.Default.Flag)
            }
        }
    }
}

@Composable
fun DataStatItem(label: String, value: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF3B82F6),
            modifier = Modifier.size(20.dp)
        )
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3B82F6)
        )
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun DataRetentionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF59E0B).copy(alpha = 0.1f)
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
                imageVector = Icons.Default.Schedule,
                contentDescription = "Data Retention",
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(24.dp)
            )
            
            Column {
                Text(
                    "Data Retention Policy",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )
                Text(
                    "We keep your data for 7 years as required by financial regulations. You can request deletion at any time.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun DangerZoneSection(onDeleteAccount: () -> Unit) {
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
                "Danger Zone",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDeleteAccount() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Delete Account",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(24.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Delete Account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFEF4444)
                    )
                    Text(
                        "Permanently delete your account and all associated data",
                        fontSize = 14.sp,
                        color = Color(0xFFEF4444).copy(alpha = 0.7f)
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Navigate",
                    tint = Color(0xFFEF4444).copy(alpha = 0.6f)
                )
            }
        }
    }
}

data class DataActionItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isLoading: Boolean = false
)

@Composable
fun DataSection(
    title: String,
    items: List<DataActionItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            items.forEach { item ->
                DataActionRow(item)
            }
        }
    }
}

@Composable
fun DataActionRow(item: DataActionItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.isLoading) { 
                if (!item.isLoading) item.onClick() 
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (item.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (item.isLoading) 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                item.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        if (!item.isLoading) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun DeleteAccountConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Delete Account",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444)
            ) 
        },
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This action cannot be undone. Deleting your account will:")
                Text("• Remove all your financial data")
                Text("• Disconnect all linked accounts")
                Text("• Cancel any active goals or plans")
                Text("• Delete your profile permanently")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "Delete Forever",
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