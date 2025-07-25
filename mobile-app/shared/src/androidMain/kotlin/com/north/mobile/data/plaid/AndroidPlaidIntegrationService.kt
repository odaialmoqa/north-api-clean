package com.north.mobile.data.plaid

import android.app.Activity
import android.app.Application
import com.plaid.link.Plaid
import com.plaid.link.PlaidHandler
import com.plaid.link.configuration.LinkTokenConfiguration

class AndroidPlaidIntegrationService(
    private val application: Application,
    private val backendService: PlaidIntegrationService // Use your existing backend service for API calls
) : PlaidIntegrationService {
    override suspend fun initializePlaidLink(): SimplePlaidLinkResult {
        // Get link token from backend
        return backendService.initializePlaidLink()
    }

    override suspend fun exchangePublicToken(publicToken: String): SimpleAccountConnectionResult {
        // Exchange public token with backend
        return backendService.exchangePublicToken(publicToken)
    }

    override suspend fun getAccounts(userId: String): List<SimplePlaidAccount> = backendService.getAccounts(userId)
    override suspend fun syncTransactions(accountId: String): SimpleTransactionSyncResult = backendService.syncTransactions(accountId)
    override suspend fun disconnectAccount(accountId: String): Boolean = backendService.disconnectAccount(accountId)
    override suspend fun refreshAccountConnection(accountId: String): SimpleAccountConnectionResult = backendService.refreshAccountConnection(accountId)

    /**
     * Launch Plaid Link using the Handler API (Plaid v3.x)
     * @param activity The Activity
     * @param linkToken The link token from backend
     * Note: Result must be handled in MainActivity's onActivityResult.
     */
    fun launchPlaidLink(
        activity: Activity,
        linkToken: String
    ) {
        val config = LinkTokenConfiguration.Builder()
            .token(linkToken)
            .build()
        val handler: PlaidHandler = Plaid.create(application, config)
        handler.open(activity)
    }
} 