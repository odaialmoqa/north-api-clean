package com.north.mobile.ui.main

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.north.mobile.ui.dashboard.WealthsimpleDashboard
import com.north.mobile.ui.insights.InsightsScreen
import com.north.mobile.ui.chat.SimpleChatScreen
import com.north.mobile.data.repository.InsightsRepository
import com.north.mobile.data.service.FinancialAnalysisService
import org.koin.compose.koinInject

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            WealthsimpleDashboard(
                onNavigateToChat = { 
                    navController.navigate("chat") 
                },
                onNavigateToInsights = { 
                    navController.navigate("insights") 
                }
            )
        }
        
        composable("insights") {
            val insightsRepository: InsightsRepository = koinInject()
            
            InsightsScreen(
                insightsRepository = insightsRepository,
                onNavigateBack = { 
                    navController.popBackStack() 
                }
            )
        }
        
        composable("chat") {
            SimpleChatScreen(
                onNavigateBack = { 
                    navController.popBackStack() 
                }
            )
        }
    }
}

/**
 * Example of how to trigger transaction analysis after Plaid connection
 */
@Composable
fun PlaidConnectionHandler() {
    val financialAnalysisService: FinancialAnalysisService = koinInject()
    
    // This would be called after successful Plaid connection
    fun onPlaidConnectionSuccess() {
        financialAnalysisService.triggerAnalysis()
    }
    
    // Your Plaid connection UI here
}