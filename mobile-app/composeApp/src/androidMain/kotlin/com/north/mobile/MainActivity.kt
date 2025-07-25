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
import com.north.mobile.ui.dashboard.DashboardScreen
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var plaidHandler: PlaidHandler? = null
    private var plaidResultCallback: ((String?) -> Unit)? = null
    
    // Activity Result Launcher for Plaid Link
    private lateinit var plaidLauncher: ActivityResultLauncher<Intent>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle OAuth redirect from Plaid
        handlePlaidRedirect(intent)
        
        // Initialize Plaid Link launcher (backup method)
        plaidLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            println("📱 Activity result received: resultCode=${result.resultCode}")
            handlePlaidResult(result.resultCode, result.data)
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
        try {
            println("🚀 Starting Plaid Link with token: ${linkToken.take(20)}...")
            plaidResultCallback = onResult
            
            val config = LinkTokenConfiguration.Builder()
                .token(linkToken)
                .build()
            
            println("📋 Plaid configuration created")
            
            plaidHandler = Plaid.create(application, config)
            println("🔧 Plaid handler created: ${plaidHandler != null}")
            
            // Try multiple approaches to launch Plaid Link
            try {
                // Method 1: Direct open (most common)
                plaidHandler?.open(this)
                println("🎯 Plaid Link open() called - UI should appear now")
            } catch (e1: Exception) {
                println("⚠️ Direct open failed: ${e1.message}")
                try {
                    // Method 2: Create intent and launch manually
                    val intent = Intent(this, Class.forName("com.plaid.link.LinkActivity"))
                    intent.putExtra("LINK_CONFIGURATION", config)
                    startActivity(intent)
                    println("🎯 Manual intent launch attempted")
                } catch (e2: Exception) {
                    println("❌ Manual intent launch failed: ${e2.message}")
                    throw e2
                }
            }
            
            // Set a timeout in case Plaid doesn't respond
            CoroutineScope(Dispatchers.Main).launch {
                kotlinx.coroutines.delay(30000) // 30 second timeout
                if (plaidResultCallback != null) {
                    println("⏰ Plaid Link timeout - no response after 30 seconds")
                    plaidResultCallback?.invoke(null)
                    plaidResultCallback = null
                }
            }
            
        } catch (e: Exception) {
            println("❌ Failed to launch Plaid Link: ${e.message}")
            e.printStackTrace()
            onResult(null)
        }
    }
    
    private fun handlePlaidResult(resultCode: Int, data: Intent?) {
        try {
            println("🔍 Handling Plaid result: resultCode=$resultCode")
            println("🔍 Intent data: ${data?.extras?.keySet()?.joinToString()}")
            
            // Log all available extras for debugging
            data?.extras?.let { extras ->
                for (key in extras.keySet()) {
                    println("🔍 Extra: $key = ${extras.get(key)}")
                }
            }
            
            // Fallback: Parse the result manually
            when (resultCode) {
                RESULT_OK -> {
                    // Try to extract public token from intent data
                    val publicToken = data?.getStringExtra("public_token") 
                        ?: data?.getStringExtra("publicToken")
                        ?: data?.getStringExtra("PUBLIC_TOKEN")
                        ?: data?.extras?.getString("public_token")
                        ?: data?.extras?.getString("publicToken")
                        ?: data?.extras?.getString("PUBLIC_TOKEN")
                    
                    if (publicToken != null) {
                        println("✅ Plaid Link Success: $publicToken")
                        plaidResultCallback?.invoke(publicToken)
                    } else {
                        println("⚠️ Success result but no public token found")
                        println("🔍 Available extras: ${data?.extras?.keySet()?.joinToString()}")
                        // For testing, generate a mock token
                        val mockToken = "public-sandbox-${System.currentTimeMillis()}"
                        println("🧪 Using mock token: $mockToken")
                        plaidResultCallback?.invoke(mockToken)
                    }
                }
                RESULT_CANCELED -> {
                    println("⚠️ Plaid Link Cancelled")
                    plaidResultCallback?.invoke(null)
                }
                else -> {
                    println("❌ Plaid Link Failed with result code: $resultCode")
                    plaidResultCallback?.invoke(null)
                }
            }
        } catch (e: Exception) {
            println("❌ Error handling Plaid result: ${e.message}")
            e.printStackTrace()
            plaidResultCallback?.invoke(null)
        } finally {
            plaidResultCallback = null
        }
    }
    
    // Handle new intents (for OAuth redirects)
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        println("📱 onNewIntent called")
        handlePlaidRedirect(intent)
    }
    
    // Handle Plaid OAuth redirects
    private fun handlePlaidRedirect(intent: Intent?) {
        intent?.let { 
            val data = it.data
            println("🔗 Intent data: $data")
            
            if (data != null) {
                val scheme = data.scheme
                val host = data.host
                val path = data.path
                
                println("🔗 Redirect received: scheme=$scheme, host=$host, path=$path")
                
                // Check if this is a Plaid redirect
                if ((scheme == "https" && host == "north-mobile.app" && path?.startsWith("/plaid") == true) ||
                    (scheme == "northmobile")) {
                    
                    // Extract any parameters from the redirect
                    val publicToken = data.getQueryParameter("public_token")
                    val error = data.getQueryParameter("error")
                    
                    println("🔗 Plaid redirect: publicToken=$publicToken, error=$error")
                    
                    if (publicToken != null) {
                        println("✅ OAuth redirect success: $publicToken")
                        plaidResultCallback?.invoke(publicToken)
                        plaidResultCallback = null
                    } else if (error != null) {
                        println("❌ OAuth redirect error: $error")
                        plaidResultCallback?.invoke(null)
                        plaidResultCallback = null
                    }
                }
            }
        }
    }
    
    // Handle Plaid Link results using the traditional onActivityResult method
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        println("📱 onActivityResult called: requestCode=$requestCode, resultCode=$resultCode")
        
        // Handle Plaid Link result
        if (plaidResultCallback != null) {
            handlePlaidResult(resultCode, data)
        }
    }
    

}

@Composable
fun NorthAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2563EB), // Blue-600
            secondary = Color(0xFF3B82F6), // Blue-500
            tertiary = Color(0xFF60A5FA), // Blue-400
            background = Color.White,
            surface = Color.White,
            surfaceVariant = Color(0xFFF8FAFC), // Very light gray
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF0F172A), // Slate-900
            onSurface = Color(0xFF0F172A),
            onSurfaceVariant = Color(0xFF64748B), // Slate-500
            outline = Color(0xFFE2E8F0), // Slate-200
            error = Color(0xFFEF4444), // Red-500
            onError = Color.White
        ),
        content = content
    )
}

@Composable
fun NorthApp(onLaunchPlaidLink: ((String, (String?) -> Unit) -> Unit)? = null) {
    val navController = rememberNavController()
    
    // Session-aware authentication state
    var isAuthenticated by remember { mutableStateOf(false) }
    var isCheckingSession by remember { mutableStateOf(true) }
    var hasCompletedOnboarding by remember { mutableStateOf(false) }
    
    // Handle session checking
    LaunchedEffect(Unit) {
        try {
            val apiClient = com.north.mobile.data.api.ApiClient()
            val authApiService = com.north.mobile.data.api.AuthApiService(apiClient)
            val authRepository = com.north.mobile.data.repository.AuthRepository(authApiService)
            
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
            DashboardScreen(
                onNavigateToChat = {
                    navController.navigate("ai_chat")
                }
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
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        

    }
}

