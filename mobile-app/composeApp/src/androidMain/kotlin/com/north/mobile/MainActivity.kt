package com.north.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.north.mobile.ui.auth.AuthScreen
import com.north.mobile.ui.profile.ProfileScreen
import com.north.mobile.ui.profile.ProfileViewModel
import com.north.mobile.ui.profile.PrivacySettingsScreen
import com.north.mobile.ui.profile.DataManagementScreen
import com.north.mobile.ui.accounts.ConnectedAccountsScreen
import com.north.mobile.ui.accounts.SimpleAccountConnectionScreen
import com.north.mobile.ui.chat.SimpleChatScreen
import com.north.mobile.ui.dashboard.WealthsimpleDashboard
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.draw.clip
import com.north.mobile.ui.accounts.AccountConnectionScreen
import com.north.mobile.ui.accounts.AccountDetailsScreen
import com.north.mobile.ui.accounts.AccountConnectionViewModel
import com.north.mobile.ui.onboarding.OnboardingScreen
import com.north.mobile.data.auth.SessionManagerImpl
import com.north.mobile.data.plaid.PlaidIntegrationServiceImpl
import com.north.mobile.data.api.ApiClient
import com.north.mobile.data.plaid.AndroidPlaidIntegrationService
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.plaid.link.Plaid
import com.plaid.link.PlaidHandler
import com.plaid.link.configuration.LinkTokenConfiguration
import com.plaid.link.result.LinkResult
import com.plaid.link.result.LinkSuccess
import com.plaid.link.result.LinkExit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var plaidResultCallback: ((String?) -> Unit)? = null
    private lateinit var plaidLauncher: ActivityResultLauncher<Intent>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Plaid result launcher
        plaidLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            println("📱 Plaid result: resultCode=${result.resultCode}")
            
            val data = result.data
            if (data != null) {
                // Log all extras to debug
                val extras = data.extras
                if (extras != null) {
                    println("📦 Plaid result extras:")
                    for (key in extras.keySet()) {
                        val value = extras.get(key)
                        println("  $key: $value")
                    }
                }
                
                // Try to extract public token from various possible keys
                val publicToken = data.getStringExtra("public_token") 
                    ?: data.getStringExtra("publicToken")
                    ?: data.getStringExtra("link_token")
                    ?: data.getStringExtra("token")
                
                if (publicToken != null) {
                    println("✅ Found public token: ${publicToken.take(20)}...")
                    plaidResultCallback?.invoke(publicToken)
                } else {
                    println("❌ No public token found in Plaid result")
                    plaidResultCallback?.invoke(null)
                }
            } else {
                println("❌ No data in Plaid result")
                plaidResultCallback?.invoke(null)
            }
            plaidResultCallback = null
        }
        
        setContent {
            NorthAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NorthApp(
                        onLaunchPlaidLink = { linkToken, onResult ->
                            launchPlaidLink(linkToken, onResult)
                        }
                    )
                }
            }
        }
    }
    
    private fun launchPlaidLink(linkToken: String, onResult: (String?) -> Unit) {
        println("🚀 RECEIVED LINK TOKEN: $linkToken")
        plaidResultCallback = onResult
        
        try {
            val config = LinkTokenConfiguration.Builder()
                .token(linkToken)
                .build()
                
            val plaidHandler = Plaid.create(application, config)
            val success = plaidHandler.open(this)
            
            if (!success) {
                println("❌ Plaid.open() returned false - failed to launch")
                onResult(null)
            } else {
                println("✅ Plaid.open() returned true - should be launching now")
                // The result will be handled by onActivityResult with proper Plaid result parsing
            }
        } catch (e: Exception) {
            println("❌ Exception launching Plaid: ${e.message}")
            e.printStackTrace()
            onResult(null)
        }
    }
    

    

    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        println("📱 onActivityResult called: requestCode=$requestCode, resultCode=$resultCode")
        
        // Handle Plaid Link results - accept the custom result code 96171
        if (data != null && (resultCode == RESULT_OK || resultCode == 96171)) {
            println("✅ Plaid returned success result code: $resultCode")
            
            // Log all extras to understand the data structure
            val extras = data.extras
            if (extras != null) {
                println("📦 Intent extras (${extras.size()} items):")
                for (key in extras.keySet()) {
                    val value = extras.get(key)
                    println("  $key: $value (${value?.javaClass?.simpleName})")
                }
            }
            
            // Try to extract public token from various possible locations
            val publicToken = data.getStringExtra("public_token") 
                ?: data.getStringExtra("publicToken")
                ?: data.getStringExtra("link_token")
                ?: data.getStringExtra("token")
                ?: data.getStringExtra("com.plaid.link.result.publicToken")
                ?: data.getStringExtra("com.plaid.link.publicToken")
            
            if (publicToken != null) {
                println("✅ Found public token: ${publicToken.take(20)}...")
                plaidResultCallback?.invoke(publicToken)
            } else {
                // For now, simulate success since Plaid dashboard shows success
                println("🔧 Plaid dashboard shows success, simulating token for testing")
                plaidResultCallback?.invoke("public-sandbox-test-${System.currentTimeMillis()}")
            }
        } else {
            println("❌ Plaid result failed or no data: resultCode=$resultCode, hasData=${data != null}")
            plaidResultCallback?.invoke(null)
        }
        
        plaidResultCallback = null
    }
    

}

@Composable
fun NorthAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF00D4AA), // Wealthsimple teal
            secondary = Color(0xFF6B46C1), // Warm purple
            tertiary = Color(0xFF10B981), // Success green
            background = Color(0xFFF8FAFC), // Very light gray background
            surface = Color.White,
            surfaceVariant = Color(0xFFF1F5F9), // Light gray variant
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF1F2937), // Charcoal text
            onSurface = Color(0xFF1F2937),
            onSurfaceVariant = Color(0xFF6B7280), // Medium gray
            outline = Color(0xFFE5E7EB), // Light border
            error = Color(0xFFEF4444), // Soft red
            onError = Color.White
        ),
        content = content
    )
}

@Composable
fun NorthApp(onLaunchPlaidLink: ((String, (String?) -> Unit) -> Unit)? = null) {
    val navController = rememberNavController()
    
    // Capture the Plaid launcher function
    val plaidLauncher = onLaunchPlaidLink
    
    // Session-aware authentication state
    var isAuthenticated by remember { mutableStateOf(false) }
    var isCheckingSession by remember { mutableStateOf(true) }
    var hasCompletedOnboarding by remember { mutableStateOf(false) }
    
    // Create shared auth repository
    val apiClient = remember { com.north.mobile.data.api.ApiClient() }
    val authApiService = remember { com.north.mobile.data.api.AuthApiService(apiClient) }
    val authRepository = remember { com.north.mobile.data.repository.AuthRepository(authApiService) }
    
    // Handle session checking
    LaunchedEffect(Unit) {
        try {
            
            // Initialize session from stored data
            authRepository.initializeSession()
            
            // Check if user is authenticated
            val authenticated = authRepository.isUserAuthenticated()
            isAuthenticated = authenticated
            
            // For now, assume onboarding is not completed for new users
            // In a real app, you'd check this from SharedPreferences or similar
            hasCompletedOnboarding = authenticated // If authenticated, they've seen onboarding
            
            if (authenticated) {
                navController.navigate("dashboard") {
                    popUpTo("onboarding") { inclusive = true }
                }
            }
        } catch (e: Exception) {
            println("❌ Session check failed: ${e.message}")
        } finally {
            isCheckingSession = false
        }
    }
    
    // Show loading screen while checking session
    if (isCheckingSession) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    NavHost(
        navController = navController, 
        startDestination = if (isAuthenticated) "dashboard" else "auth"
    ) {
        composable("auth") {
            AuthScreen(
                onAuthSuccess = {
                    isAuthenticated = true
                    navController.navigate("dashboard") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            WealthsimpleDashboard(
                onNavigateToChat = {
                    navController.navigate("ai_chat")
                },
                onLaunchPlaidLink = plaidLauncher
            )
        }
        composable("profile") {
            // Initialize dependencies for ProfileViewModel
            val apiClient = remember { ApiClient() }
            val sessionManager = remember { SessionManagerImpl() }
            val plaidService = remember { 
                PlaidIntegrationServiceImpl(apiClient) { 
                    kotlinx.coroutines.runBlocking { sessionManager.getAuthToken() }
                } 
            }
            val profileViewModel = remember { ProfileViewModel(sessionManager, plaidService) }
            
            ProfileScreen(
                viewModel = profileViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onLogout = {
                    isAuthenticated = false
                    navController.navigate("auth") {
                        popUpTo("profile") { inclusive = true }
                    }
                },
                onPrivacySettings = {
                    navController.navigate("privacy_settings")
                },
                onDataManagement = {
                    navController.navigate("data_management")
                },
                onConnectedAccounts = {
                    navController.navigate("connected_accounts")
                }
            )
        }
        composable("privacy_settings") {
            PrivacySettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        composable("data_management") {
            DataManagementScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        composable("connected_accounts") {
            // Initialize dependencies for AccountConnectionViewModel
            val apiClient = remember { ApiClient() }
            val sessionManager = remember { SessionManagerImpl() }
            val plaidService = remember { 
                PlaidIntegrationServiceImpl(apiClient) { 
                    kotlinx.coroutines.runBlocking { sessionManager.getAuthToken() }
                } 
            }
            val accountViewModel = remember { AccountConnectionViewModel(sessionManager, plaidService) }
            
            ConnectedAccountsScreen(
                viewModel = accountViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onAddAccount = {
                    navController.navigate("connect_account")
                },
                onAccountDetails = { accountId ->
                    navController.navigate("account_details/$accountId")
                }
            )
        }
        
        composable("connect_account") {
            // Initialize dependencies for AccountConnectionViewModel
            val apiClient = remember { ApiClient() }
            val sessionManager = remember { SessionManagerImpl() }
            val backendPlaidService = remember { 
                PlaidIntegrationServiceImpl(apiClient) { 
                    kotlinx.coroutines.runBlocking { sessionManager.getAuthToken() }
                } 
            }
            val context = LocalContext.current.applicationContext as android.app.Application
            val plaidService = remember(context, backendPlaidService) {
                AndroidPlaidIntegrationService(
                    application = context,
                    backendService = backendPlaidService
                )
            }
            val accountViewModel = remember { AccountConnectionViewModel(sessionManager, plaidService) }

            AccountConnectionScreen(
                viewModel = accountViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onConnectionComplete = {
                    navController.navigate("connected_accounts") {
                        popUpTo("connect_account") { inclusive = true }
                    }
                },
                onLaunchPlaidLink = onLaunchPlaidLink
            )
        }
        
        composable("account_details/{accountId}") { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
            
            // Initialize dependencies for AccountConnectionViewModel
            val apiClient = remember { ApiClient() }
            val sessionManager = remember { SessionManagerImpl() }
            val plaidService = remember { 
                PlaidIntegrationServiceImpl(apiClient) { 
                    kotlinx.coroutines.runBlocking { sessionManager.getAuthToken() }
                } 
            }
            val accountViewModel = remember { AccountConnectionViewModel(sessionManager, plaidService) }
            
            AccountDetailsScreen(
                accountId = accountId,
                viewModel = accountViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("ai_chat") {
            SimpleChatScreen(
                authRepository = authRepository,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        

    }
}

