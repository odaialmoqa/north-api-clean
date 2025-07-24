package com.north.mobile.ui.accounts

import com.north.mobile.data.api.ApiClient
import com.north.mobile.data.plaid.PlaidIntegrationServiceImpl
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class PlaidIntegrationTest {
    
    @Test
    fun testPlaidServiceInitialization() = runTest {
        // Mock API client
        val apiClient = ApiClient()
        
        // Mock auth token provider
        val getAuthToken = { "mock-auth-token" }
        
        // Create service
        val plaidService = PlaidIntegrationServiceImpl(apiClient, getAuthToken)
        
        // Test initialization (this will use mock data from server)
        val result = plaidService.initializePlaidLink()
        
        // Should succeed with mock implementation
        assertTrue(result.success, "Plaid Link initialization should succeed")
    }
    
    @Test
    fun testAccountsRetrieval() = runTest {
        val apiClient = ApiClient()
        val getAuthToken = { "mock-auth-token" }
        val plaidService = PlaidIntegrationServiceImpl(apiClient, getAuthToken)
        
        // Test getting accounts
        val accounts = plaidService.getAccounts("test-user")
        
        // Should return mock accounts
        assertTrue(accounts.isNotEmpty(), "Should return mock accounts")
    }
}