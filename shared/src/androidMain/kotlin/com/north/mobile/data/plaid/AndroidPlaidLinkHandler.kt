package com.north.mobile.data.plaid

import android.app.Activity
import android.content.Context
import com.plaid.link.Plaid
import com.plaid.link.PlaidHandler
import com.plaid.link.configuration.LinkTokenConfiguration
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidPlaidLinkHandler(
    private val context: Context
) : PlaidLinkHandler {
    
    private var plaidHandler: PlaidHandler? = null
    
    override suspend fun openLink(linkToken: String): Result<PlaidLinkResult> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val linkTokenConfiguration = LinkTokenConfiguration.builder()
                    .token(linkToken)
                    .build()
                
                plaidHandler = Plaid.create(
                    application = context.applicationContext as android.app.Application,
                    linkTokenConfiguration = linkTokenConfiguration
                )
                
                val linkHandler = plaidHandler ?: run {
                    continuation.resume(Result.failure(Exception("Failed to create Plaid handler")))
                    return@suspendCancellableCoroutine
                }
                
                linkHandler.open(context as Activity)
                
                // Set up result handlers
                linkHandler.setOnSuccessListener { linkSuccess: LinkSuccess ->
                    val result = PlaidLinkResult.Success(
                        publicToken = linkSuccess.publicToken,
                        metadata = PlaidLinkMetadata(
                            institution = linkSuccess.metadata.institution?.let { institution ->
                                PlaidInstitutionMetadata(
                                    institutionId = institution.id,
                                    name = institution.name
                                )
                            },
                            accounts = linkSuccess.metadata.accounts.map { account ->
                                PlaidAccountMetadata(
                                    accountId = account.id,
                                    name = account.name,
                                    mask = account.mask,
                                    type = account.type.toString(),
                                    subtype = account.subtype?.toString(),
                                    verificationStatus = account.verificationStatus?.toString()
                                )
                            },
                            linkSessionId = linkSuccess.metadata.linkSessionId,
                            metadataJson = linkSuccess.metadata.metadataJson
                        )
                    )
                    continuation.resume(Result.success(result))
                }
                
                linkHandler.setOnExitListener { linkExit: LinkExit ->
                    val error = linkExit.error?.let { error ->
                        PlaidError(
                            errorType = error.errorType.toString(),
                            errorCode = error.errorCode.toString(),
                            errorMessage = error.errorMessage,
                            displayMessage = error.displayMessage,
                            requestId = null
                        )
                    }
                    
                    val metadata = linkExit.metadata?.let { metadata ->
                        PlaidLinkMetadata(
                            institution = metadata.institution?.let { institution ->
                                PlaidInstitutionMetadata(
                                    institutionId = institution.id,
                                    name = institution.name
                                )
                            },
                            accounts = metadata.accounts.map { account ->
                                PlaidAccountMetadata(
                                    accountId = account.id,
                                    name = account.name,
                                    mask = account.mask,
                                    type = account.type.toString(),
                                    subtype = account.subtype?.toString(),
                                    verificationStatus = account.verificationStatus?.toString()
                                )
                            },
                            linkSessionId = metadata.linkSessionId,
                            metadataJson = metadata.metadataJson
                        )
                    }
                    
                    val result = PlaidLinkResult.Exit(error = error, metadata = metadata)
                    continuation.resume(Result.success(result))
                }
                
                // Handle cancellation
                continuation.invokeOnCancellation {
                    plaidHandler?.destroy()
                    plaidHandler = null
                }
                
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }
    
    override fun destroy() {
        plaidHandler?.destroy()
        plaidHandler = null
    }
}

// Interface moved to common source