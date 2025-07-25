package com.north.mobile.data.plaid

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class PlaidServiceTest {
    
    private lateinit var mockEngine: MockEngine
    private lateinit var plaidService: PlaidService
    
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
    }
    
    @Test
    fun testCreateLinkToken() = kotlinx.coroutines.test.runTest {
        val result = plaidService.createLinkToken("test-user-id")
        
        assertTrue(result.isSuccess)
        val linkToken = result.getOrThrow()
        assertEquals("link-sandbox-test-token", linkToken.linkToken)
        assertEquals("test-request-id", linkToken.requestId)
    }
    
    @Test
    fun testExchangePublicToken() = kotlinx.coroutines.test.runTest {
        val result = plaidService.exchangePublicToken("public-sandbox-test-token")
        
        assertTrue(result.isSuccess)
        val accessToken = result.getOrThrow()
        assertEquals("access-sandbox-test-token", accessToken.accessToken)
        assertEquals("test-item-id", accessToken.itemId)
    }
    
    @Test
    fun testGetAccounts() = kotlinx.coroutines.test.runTest {
        val result = plaidService.getAccounts("access-sandbox-test-token")
        
        assertTrue(result.isSuccess)
        val accounts = result.getOrThrow()
        assertEquals(1, accounts.size)
        
        val account = accounts.first()
        assertEquals("test-account-id", account.accountId)
        assertEquals("Plaid Checking", account.name)
        assertEquals(PlaidAccountType.DEPOSITORY, account.type)
        assertEquals(PlaidAccountSubtype.CHECKING, account.subtype)
        assertEquals(100.0, account.balances.available)
        assertEquals(110.0, account.balances.current)
    }
    
    @Test
    fun testGetCanadianInstitutions() = kotlinx.coroutines.test.runTest {
        val result = plaidService.getCanadianInstitutions()
        
        assertTrue(result.isSuccess)
        val institutions = result.getOrThrow()
        assertTrue(institutions.isNotEmpty())
        
        // Check that major Canadian banks are included
        val institutionNames = institutions.map { it.displayName }
        assertTrue(institutionNames.contains("RBC Royal Bank"))
        assertTrue(institutionNames.contains("TD Canada Trust"))
        assertTrue(institutionNames.contains("Bank of Montreal"))
    }
    
    @Test
    fun testSearchInstitutions() = kotlinx.coroutines.test.runTest {
        val result = plaidService.searchInstitutions("RBC")
        
        assertTrue(result.isSuccess)
        val institutions = result.getOrThrow()
        assertTrue(institutions.isNotEmpty())
        
        val rbcInstitution = institutions.find { it.displayName.contains("RBC") }
        assertNotNull(rbcInstitution)
        assertEquals("RBC Royal Bank", rbcInstitution.displayName)
    }
    
    @AfterTest
    fun tearDown() {
        mockEngine.close()
    }
}