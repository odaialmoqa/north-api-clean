package com.north.mobile.ui.accounts

import com.north.mobile.data.plaid.PlaidIntegrationService
import com.north.mobile.data.plaid.SimplePlaidLinkResult
import com.north.mobile.data.plaid.SimpleAccountConnectionResult

/**
 * Manager for handling Plaid Link integration in the UI layer
 */
class PlaidLinkManager(
    private val plaidService: PlaidIntegrationService
) {
    
    /**
     * Initialize Plaid Link and get the link token
     */
    suspend fun initializePlaidLink(): SimplePlaidLinkResult {
        return plaidService.initializePlaidLink()
    }
    
    /**
     * Exchange public token after successful Plaid Link flow
     */
    suspend fun exchangePublicToken(publicToken: String): SimpleAccountConnectionResult {
        return plaidService.exchangePublicToken(publicToken)
    }
    
    /**
     * Get connected accounts
     */
    suspend fun getConnectedAccounts(): List<com.north.mobile.data.plaid.SimplePlaidAccount> {
        return plaidService.getAccounts("current_user")
    }
}