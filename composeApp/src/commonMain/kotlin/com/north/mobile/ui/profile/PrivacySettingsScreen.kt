package com.north.mobile.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onBackClick: () -> Unit,
    onDataExport: () -> Unit = {},
    onDataDeletion: () -> Unit = {},
    onConsentManagement: () -> Unit = {}
) {
    var biometricEnabled by remember { mutableStateOf(true) }
    var dataAnalyticsEnabled by remember { mutableStateOf(true) }
    var marketingEnabled by remember { mutableStateOf(false) }
    var locationTrackingEnabled by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Settings") },
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
            // Privacy overview
            item {
                PrivacyOverviewCard()
            }
            
            // Authentication settings
            item {
                PrivacySection(
                    title = "Authentication & Security",
                    items = listOf(
                        PrivacyToggleItem(
                            title = "Biometric Authentication",
                            description = "Use fingerprint or face recognition to secure your account",
                            icon = Icons.Default.Fingerprint,
                            isEnabled = biometricEnabled,
                            onToggle = { biometricEnabled = it }
                        )
                    )
                )
            }
            
            // Data usage settings
            item {
                PrivacySection(
                    title = "Data Usage",
                    items = listOf(
                        PrivacyToggleItem(
                            title = "Financial Analytics",
                            description = "Allow analysis of your financial data to provide personalized insights",
                            icon = Icons.Default.Analytics,
                            isEnabled = dataAnalyticsEnabled,
                            onToggle = { dataAnalyticsEnabled = it }
                        ),
                        PrivacyToggleItem(
                            title = "Marketing Communications",
                            description = "Receive personalized offers and financial tips",
                            icon = Icons.Default.Email,
                            isEnabled = marketingEnabled,
                            onToggle = { marketingEnabled = it }
                        ),
                        PrivacyToggleItem(
                            title = "Location Tracking",
                            description = "Use location data to categorize transactions automatically",
                            icon = Icons.Default.LocationOn,
                            isEnabled = locationTrackingEnabled,
                            onToggle = { locationTrackingEnabled = it }
                        )
                    )
                )
            }
            
            // Data management actions
            item {
                PrivacySection(
                    title = "Data Management",
                    actionItems = listOf(
                        PrivacyActionItem(
                            title = "Export My Data",
                            description = "Download a copy of all your data",
                            icon = Icons.Default.Download,
                            onClick = onDataExport
                        ),
                        PrivacyActionItem(
                            title = "Manage Consent",
                            description = "Review and update your data processing consent",
                            icon = Icons.Default.Assignment,
                            onClick = onConsentManagement
                        ),
                        PrivacyActionItem(
                            title = "Delete My Account",
                            description = "Permanently delete your account and all data",
                            icon = Icons.Default.Delete,
                            onClick = onDataDeletion,
                            isDestructive = true
                        )
                    )
                )
            }
        }
    }
}

@Composable
fun PrivacyOverviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF10B981).copy(alpha = 0.1f)
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
                imageVector = Icons.Default.Security,
                contentDescription = "Privacy",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(32.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Your Privacy Matters",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
                Text(
                    "We're committed to protecting your financial data with bank-level security.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

data class PrivacyToggleItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isEnabled: Boolean,
    val onToggle: (Boolean) -> Unit
)

data class PrivacyActionItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false
)

@Composable
fun PrivacySection(
    title: String,
    items: List<PrivacyToggleItem> = emptyList(),
    actionItems: List<PrivacyActionItem> = emptyList()
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
                PrivacyToggleRow(item)
            }
            
            actionItems.forEach { item ->
                PrivacyActionRow(item)
            }
        }
    }
}

@Composable
fun PrivacyToggleRow(item: PrivacyToggleItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                item.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Switch(
            checked = item.isEnabled,
            onCheckedChange = item.onToggle
        )
    }
}

@Composable
fun PrivacyActionRow(item: PrivacyActionItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = if (item.isDestructive) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (item.isDestructive) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                item.description,
                fontSize = 14.sp,
                color = if (item.isDestructive) 
                    Color(0xFFEF4444).copy(alpha = 0.7f) 
                else 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = if (item.isDestructive) 
                Color(0xFFEF4444).copy(alpha = 0.6f) 
            else 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}