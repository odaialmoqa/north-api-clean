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
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NorthAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NorthApp()
                }
            }
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

