package com.north.mobile.data.plaid

import kotlinx.coroutines.delay

class MockPlaidLinkHandler(
    private val shouldSucceed: Boolean = true,
    private val simulateUserCancellation: Boolean = false
) : PlaidLinkHandler {
    
    override suspend fun openLink(linkToken: String): Result<PlaidLinkResult> {
        // Simulate network delay
        delay(1000)
        
        return if (simulateUserCancellation) {
            Result.success(
                PlaidLinkResult.Exit(
                    error = PlaidError(
                        errorType = "USER_EXIT",
                        errorCode = "USER_CANCELLED",
                        errorMessage = "User cancelled the linking process",
                        displayMessage = "Account linking was cancelled",
                        requestId = "mock_request_id"
                    ),
                    metadata = null
                )
            )
        } else if (shouldSucceed) {
            Result.success(
                PlaidLinkResult.Success(
                    publicToken = "public-sandbox-${System.currentTimeMillis()}",
                    metadata = PlaidLinkMetadata(
                        institution = PlaidInstitutionMetadata(
                            institutionId = "ins_3",
                            name = "RBC Royal Bank"
                        ),
                        accounts = listOf(
                            PlaidAccountMetadata(
                                accountId = "mock_account_1",
                                name = "Chequing Account",
                                mask = "0000",
                                type = "depository",
                                subtype = "checking",
                                verificationStatus = "verified"
                            ),
                            PlaidAccountMetadata(
                                accountId = "mock_account_2",
                                name = "Savings Account",
                                mask = "1111",
                                type = "depository",
                                subtype = "savings",
                                verificationStatus = "verified"
                            )
                        ),
                        linkSessionId = "mock_session_${System.currentTimeMillis()}",
                        metadataJson = "{\"mock\": true}"
                    )
                )
            )
        } else {
            Result.success(
                PlaidLinkResult.Exit(
                    error = PlaidError(
                        errorType = "INSTITUTION_ERROR",
                        errorCode = "INSTITUTION_DOWN",
                        errorMessage = "The institution is temporarily unavailable",
                        displayMessage = "We're having trouble connecting to your bank. Please try again later.",
                        requestId = "mock_request_id"
                    ),
                    metadata = null
                )
            )
        }
    }
    
    override fun destroy() {
        // Nothing to clean up in mock implementation
    }
}