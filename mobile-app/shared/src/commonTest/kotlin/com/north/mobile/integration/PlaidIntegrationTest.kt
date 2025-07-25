package com.north.mobile.integration

import com.north.mobile.data.plaid.*
import com.north.mobile.domain.model.CanadianInstitutions
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.*

class PlaidIntegrationTest {
    
    private lateinit var mockEngine: MockEngine
    private lateinit var plaidService: PlaidService
    private lateinit var accountLinkingManager: AccountLinkingManager
    
    @BeforeTest
    fun setup() {
        mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/link/token/create" -> {
                    respond(
                        content = """
                            {
                                "link_token": "link-sandbox-test-token",
                                "expiration": "2024-01-01T00:00:00Z",
                                "request_id": "test-request-id"
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                "/item/public_token/exchange" -> {
                    respond(
                        content = """
                            {
                                "access_token": "access-sandbox-test-token",
                                "item_id": "test-item-id"
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                "/accounts/get" -> {
                    respond(
                        content = """
                            {
                                "accounts": [
                                    {
                                        "account_id": "test-account-id",
                                        "name": "Plaid Checking",
                                        "official_name": "Plaid Gold Standard 0% Interest Checking",
                                        "type": "depository",
                                        "subtype": "checking",
                                        "mask": "0000",
                                        "balances": {
                                            "available": 100.0,
                                            "current": 110.0,
                                            "iso_currency_code": "CAD"
                                        }
                                    }
                                ]
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                "/transactions/get" -> {
                    respond(
                        content = """
                            {
                                "accounts": [
                                    {
                                        "account_id": "test-account-id",
                                        "name": "Plaid Checking",
                                        "type": "depository",
                                        "subtype": "checking"
                                    }
                                ],
                                "transactions": [
                                    {
                                        "transaction_id": "test-transaction-1",
                                        "account_id": "test-account-id",
                                        "amount": 25.50,
                                        "name": "Coffee Shop",
                                        "date": "2024-01-15",
                                        "category": ["Food and Drink", "Restaurants", "Coffee Shop"]
                                    }
                                ],
                                "total_transactions": 1
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                "/item/get" -> {
                    respond(
                        content = """
                            {
                                "item": {
                                    "item_id": "test-item-id",
                                    "institution_id": "ins_109508",
                                    "webhook": null,
                                    "error": null,
                                    "available_products": ["transactions", "auth", "identity"],
                                    "billed_products": ["transactions"],
                                    "consent_expiration_time": null,
                                    "update_type": "background"
                                }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                "/item/remove" -> {
                    respond(
                        content = """{"removed": true}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> {
                    respond(
                        content = """{"error": "Not found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }
        
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
        
        plaidService = PlaidServiceImpl(
            httpClient = httpClient,
            clientId = "test-client-id",
            secret = "test-secret",
            environment = PlaidEnvironment.SANDBOX
        )
        
        accountLinkingManager = AccountLinkingManager(
            plaidService = plaidService,
            linkHandlerFactory = MockPlaidLinkHandlerFactory()
        )
    }
    
    @AfterTest
    fun tearDown() {
        mockEngine.close()
    }
    
    @Test
    fun testCompleteAccountLinkingFlow() = runTest {
        val userId = "test-user-123"
        
        // Step 1: Create link token
        val linkTokenResult = plaidService.createLinkToken(userId)
        assertTrue(linkTokenResult.isSuccess)
        val linkToken = linkTokenResult.getOrThrow()
        assertEquals("link-sandbox-test-token", linkToken.linkToken)
        
        // Step 2: Simulate user completing Plaid Link flow
        val publicToken = "public-sandbox-test-token"
        
        // Step 3: Exchange public token for access token
        val accessTokenResult = plaidService.exchangePublicToken(publicToken)
        assertTrue(accessTokenResult.isSuccess)
        val accessToken = accessTokenResult.getOrThrow()
        assertEquals("access-sandbox-test-token", accessToken.accessToken)
        assertEquals("test-item-id", accessToken.itemId)
        
        // Step 4: Get accounts
        val accountsResult = plaidService.getAccounts(accessToken.accessToken)
        assertTrue(accountsResult.isSuccess)
        val accounts = accountsResult.getOrThrow()
        assertEquals(1, accounts.size)
        
        val account = accounts.first()
        assertEquals("test-account-id", account.accountId)
        assertEquals("Plaid Checking", account.name)
        assertEquals(PlaidAccountType.DEPOSITORY, account.type)
        assertEquals(PlaidAccountSubtype.CHECKING, account.subtype)
        
        // Step 5: Get transactions
        val startDate = kotlinx.datetime.LocalDate(2024, 1, 1)
        val endDate = kotlinx.datetime.LocalDate(2024, 1, 31)
        val transactionsResult = plaidService.getTransactions(
            accessToken.accessToken, 
            startDate, 
            endDate, 
            listOf(account.accountId)
        )
        assertTrue(transactionsResult.isSuccess)
        val transactions = transactionsResult.getOrThrow()
        assertEquals(1, transactions.size)
        
        val transaction = transactions.first()
        assertEquals("test-transaction-1", transaction.id)
        assertEquals("test-account-id", transaction.accountId)
        assertEquals("Coffee Shop", transaction.description)
    }
    
    @Test
    fun testAccountLinkingWithErrorHandling() = runTest {
        // Test error scenarios
        val mockEngineWithErrors = MockEngine { request ->
            when (request.url.encodedPath) {
                "/link/token/create" -> {
                    respond(
                        content = """
                            {
                                "error_type": "INVALID_REQUEST",
                                "error_code": "MISSING_FIELDS",
                                "error_message": "Required field missing",
                                "display_message": "Please check your request",
                                "request_id": "error-request-id"
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.BadRequest,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> {
                    respond(
                        content = """{"error": "Not found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }
        
        val httpClientWithErrors = HttpClient(mockEngineWithErrors) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
        
        val plaidServiceWithErrors = PlaidServiceImpl(
            httpClient = httpClientWithErrors,
            clientId = "test-client-id",
            secret = "test-secret",
            environment = PlaidEnvironment.SANDBOX
        )
        
        // Test error handling
        val linkTokenResult = plaidServiceWithErrors.createLinkToken("test-user")
        assertTrue(linkTokenResult.isFailure)
        
        val exception = linkTokenResult.exceptionOrNull()
        assertTrue(exception is PlaidServiceError.InvalidRequestError)
        
        mockEngineWithErrors.close()
    }
    
    @Test
    fun testAccountLinkingManagerIntegration() = runTest {
        val userId = "test-user-123"
        val institutionId = "ins_109508"
        
        // Test complete linking flow through manager
        val linkingResult = accountLinkingManager.startAccountLinking(userId, institutionId)
        assertTrue(linkingResult.isSuccess)
        
        val linkToken = linkingResult.getOrThrow()
        assertNotNull(linkToken)
        assertEquals("link-sandbox-test-token", linkToken.linkToken)
        
        // Simulate successful link completion
        val publicToken = "public-sandbox-test-token"
        val completionResult = accountLinkingManager.completeAccountLinking(userId, publicToken)
        assertTrue(completionResult.isSuccess)
        
        val linkedAccounts = completionResult.getOrThrow()
        assertEquals(1, linkedAccounts.size)
        
        val linkedAccount = linkedAccounts.first()
        assertEquals("test-account-id", linkedAccount.id)
        assertEquals("ins_109508", linkedAccount.institutionId)
    }
    
    @Test
    fun testCanadianInstitutionsAreAvailable() {
        val institutions = CanadianInstitutions.MAJOR_CANADIAN_BANKS
        
        assertTrue(institutions.isNotEmpty(), "Should have Canadian institutions available")
        
        // Verify major banks are present
        val institutionNames = institutions.map { it.displayName }
        assertTrue(institutionNames.contains("RBC Royal Bank"))
        assertTrue(institutionNames.contains("TD Canada Trust"))
        assertTrue(institutionNames.contains("Bank of Montreal"))
        assertTrue(institutionNames.contains("Scotiabank"))
        assertTrue(institutionNames.contains("CIBC"))
    }
    
    @Test
    fun testInstitutionSearch() {
        val rbcResults = CanadianInstitutions.searchByName("RBC")
        assertTrue(rbcResults.isNotEmpty())
        assertTrue(rbcResults.any { it.displayName.contains("RBC") })
        
        val tdResults = CanadianInstitutions.searchByName("TD")
        assertTrue(tdResults.isNotEmpty())
        assertTrue(tdResults.any { it.displayName.contains("TD") })
    }
    
    @Test
    fun testPlaidErrorHandling() {
        val error = PlaidError(
            errorType = "ITEM_ERROR",
            errorCode = "ITEM_LOGIN_REQUIRED",
            errorMessage = "Login required",
            displayMessage = null,
            requestId = "test-123"
        )
        
        val displayMessage = PlaidErrorHandler.getDisplayMessage(error)
        assertNotNull(displayMessage)
        assertTrue(displayMessage.isNotBlank())
        
        assertTrue(PlaidErrorHandler.requiresReauth(error))
        assertFalse(PlaidErrorHandler.isRecoverable(error))
    }
    
    @Test
    fun testAccountLinkingStatusTransitions() {
        // Test status transitions
        val notStarted = AccountLinkingStatus.NotStarted
        val inProgress = AccountLinkingStatus.InProgress
        val connected = AccountLinkingStatus.Connected("item-123", 3)
        val failed = AccountLinkingStatus.Failed(
            PlaidError("ERROR", "CODE", "Message", null, null)
        )
        val requiresReauth = AccountLinkingStatus.RequiresReauth("item-123")
        val disconnected = AccountLinkingStatus.Disconnected
        
        // Verify all status types can be created
        assertNotNull(notStarted)
        assertNotNull(inProgress)
        assertNotNull(connected)
        assertNotNull(failed)
        assertNotNull(requiresReauth)
        assertNotNull(disconnected)
        
        // Verify connected status contains expected data
        assertTrue(connected is AccountLinkingStatus.Connected)
        assertEquals("item-123", connected.itemId)
        assertEquals(3, connected.accountCount)
    }
    
    @Test
    fun testPlaidAccountConversion() {
        val plaidAccount = PlaidAccount(
            accountId = "test-account-id",
            itemId = "test-item-id",
            name = "Test Checking",
            officialName = "Test Bank Checking Account",
            type = PlaidAccountType.DEPOSITORY,
            subtype = PlaidAccountSubtype.CHECKING,
            mask = "1234",
            balances = PlaidBalances(
                available = 1000.0,
                current = 1100.0,
                limit = null,
                isoCurrencyCode = "CAD"
            ),
            verificationStatus = null
        )
        
        val account = plaidAccount.toAccount("inst-123", "Test Bank")
        
        assertEquals("test-account-id", account.id)
        assertEquals("inst-123", account.institutionId)
        assertEquals("Test Bank", account.institutionName)
        assertEquals(com.north.mobile.domain.model.AccountType.CHECKING, account.accountType)
        assertEquals(110000L, account.balance.amount) // $1100 in cents
        assertEquals(100000L, account.availableBalance?.amount) // $1000 in cents
        assertEquals("1234", account.accountNumber)
    }
    
    @Test
    fun testItemManagementOperations() = runTest {
        val accessToken = "access-sandbox-test-token"
        
        // Test getting item information
        val itemResult = plaidService.getItem(accessToken)
        assertTrue(itemResult.isSuccess)
        val item = itemResult.getOrThrow()
        assertEquals("test-item-id", item.itemId)
        assertEquals("ins_109508", item.institutionId)
        assertTrue(item.availableProducts.contains("transactions"))
        
        // Test removing item
        val removeResult = plaidService.removeItem(accessToken)
        assertTrue(removeResult.isSuccess)
    }
    
    @Test
    fun testUpdateLinkTokenCreation() = runTest {
        val accessToken = "access-sandbox-test-token"
        
        val updateTokenResult = plaidService.createUpdateLinkToken(accessToken)
        assertTrue(updateTokenResult.isSuccess)
        val updateToken = updateTokenResult.getOrThrow()
        assertEquals("link-sandbox-test-token", updateToken.linkToken)
        assertNotNull(updateToken.expiration)
    }
    
    @Test
    fun testReauthenticationFlow() = runTest {
        val userId = "test-user-123"
        val itemId = "test-item-id"
        
        // Simulate reauth requirement
        val status = AccountLinkingStatus.RequiresReauth(itemId)
        assertTrue(status is AccountLinkingStatus.RequiresReauth)
        assertEquals(itemId, status.itemId)
        
        // Test reauth flow through manager
        val reauthResult = accountLinkingManager.startReauthentication(userId, itemId)
        assertTrue(reauthResult.isSuccess)
        
        val reauthToken = reauthResult.getOrThrow()
        assertEquals("link-sandbox-test-token", reauthToken.linkToken)
    }
}

// Mock implementations for testing
private class MockPlaidLinkHandlerFactory : PlaidLinkHandlerFactory {
    override fun create(): PlaidLinkHandler {
        return MockPlaidLinkHandler()
    }
}

private class MockPlaidLinkHandler : PlaidLinkHandler {
    override suspend fun openLink(linkToken: String): Result<PlaidLinkResult> {
        return Result.success(
            PlaidLinkResult.Success(
                publicToken = "public-sandbox-test-token",
                metadata = PlaidLinkMetadata(
                    institution = PlaidInstitutionMetadata("ins_109508", "Test Bank"),
                    accounts = listOf(
                        PlaidAccountMetadata(
                            accountId = "test-account-id",
                            name = "Test Checking",
                            mask = "0000",
                            type = "depository",
                            subtype = "checking",
                            verificationStatus = null
                        )
                    ),
                    linkSessionId = "test-session-id",
                    metadataJson = null
                )
            )
        )
    }
    
    override fun destroy() {
        // Mock implementation
    }
}