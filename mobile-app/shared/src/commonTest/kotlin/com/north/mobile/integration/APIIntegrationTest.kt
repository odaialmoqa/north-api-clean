package com.north.mobile.integration

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Integration tests for API communication with mock backends
 * Tests various API scenarios including success, failure, and edge cases
 */
class APIIntegrationTest {

    private lateinit var mockEngine: MockEngine
    private lateinit var httpClient: HttpClient

    @BeforeTest
    fun setup() {
        mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath == "/api/v1/auth/login" -> {
                    if (request.headers["Authorization"] == "Bearer valid-token") {
                        respond(
                            content = """
                                {
                                    "success": true,
                                    "data": {
                                        "userId": "user123",
                                        "accessToken": "new-access-token",
                                        "refreshToken": "new-refresh-token",
                                        "expiresIn": 3600
                                    }
                                }
                            """.trimIndent(),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    } else {
                        respond(
                            content = """
                                {
                                    "success": false,
                                    "error": {
                                        "code": "INVALID_CREDENTIALS",
                                        "message": "Invalid credentials provided"
                                    }
                                }
                            """.trimIndent(),
                            status = HttpStatusCode.Unauthorized,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    }
                }
                
                request.url.encodedPath == "/api/v1/users/profile" -> {
                    respond(
                        content = """
                            {
                                "success": true,
                                "data": {
                                    "id": "user123",
                                    "email": "test@example.com",
                                    "profile": {
                                        "firstName": "John",
                                        "lastName": "Doe",
                                        "phoneNumber": "+1-416-555-0123"
                                    },
                                    "preferences": {
                                        "currency": "CAD",
                                        "language": "en",
                                        "notificationsEnabled": true
                                    }
                                }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                
                request.url.encodedPath == "/api/v1/accounts" -> {
                    when (request.method) {
                        HttpMethod.Get -> {
                            respond(
                                content = """
                                    {
                                        "success": true,
                                        "data": [
                                            {
                                                "id": "acc123",
                                                "institutionId": "inst456",
                                                "institutionName": "RBC Royal Bank",
                                                "accountType": "CHECKING",
                                                "balance": {
                                                    "amount": 250000,
                                                    "currency": "CAD"
                                                },
                                                "lastUpdated": "2024-01-15T10:30:00Z"
                                            }
                                        ]
                                    }
                                """.trimIndent(),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                        HttpMethod.Post -> {
                            respond(
                                content = """
                                    {
                                        "success": true,
                                        "data": {
                                            "id": "acc124",
                                            "institutionId": "inst456",
                                            "institutionName": "TD Canada Trust",
                                            "accountType": "SAVINGS",
                                            "balance": {
                                                "amount": 150000,
                                                "currency": "CAD"
                                            },
                                            "lastUpdated": "2024-01-15T10:35:00Z"
                                        }
                                    }
                                """.trimIndent(),
                                status = HttpStatusCode.Created,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                        else -> {
                            respond(
                                content = """{"error": "Method not allowed"}""",
                                status = HttpStatusCode.MethodNotAllowed,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                    }
                }
                
                request.url.encodedPath.startsWith("/api/v1/accounts/") && request.url.encodedPath.endsWith("/transactions") -> {
                    val accountId = request.url.encodedPath.split("/")[4]
                    respond(
                        content = """
                            {
                                "success": true,
                                "data": [
                                    {
                                        "id": "txn123",
                                        "accountId": "$accountId",
                                        "amount": {
                                            "amount": -2550,
                                            "currency": "CAD"
                                        },
                                        "description": "Coffee Shop Purchase",
                                        "category": "FOOD_AND_DRINK",
                                        "date": "2024-01-15",
                                        "isRecurring": false
                                    }
                                ],
                                "pagination": {
                                    "page": 1,
                                    "limit": 50,
                                    "total": 1,
                                    "hasMore": false
                                }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                
                request.url.encodedPath == "/api/v1/goals" -> {
                    when (request.method) {
                        HttpMethod.Get -> {
                            respond(
                                content = """
                                    {
                                        "success": true,
                                        "data": [
                                            {
                                                "id": "goal123",
                                                "title": "Emergency Fund",
                                                "targetAmount": {
                                                    "amount": 1000000,
                                                    "currency": "CAD"
                                                },
                                                "currentAmount": {
                                                    "amount": 750000,
                                                    "currency": "CAD"
                                                },
                                                "targetDate": "2024-12-31",
                                                "priority": "HIGH"
                                            }
                                        ]
                                    }
                                """.trimIndent(),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                        HttpMethod.Post -> {
                            respond(
                                content = """
                                    {
                                        "success": true,
                                        "data": {
                                            "id": "goal124",
                                            "title": "Vacation Fund",
                                            "targetAmount": {
                                                "amount": 300000,
                                                "currency": "CAD"
                                            },
                                            "currentAmount": {
                                                "amount": 0,
                                                "currency": "CAD"
                                            },
                                            "targetDate": "2024-08-15",
                                            "priority": "MEDIUM"
                                        }
                                    }
                                """.trimIndent(),
                                status = HttpStatusCode.Created,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                        else -> {
                            respond(
                                content = """{"error": "Method not allowed"}""",
                                status = HttpStatusCode.MethodNotAllowed,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                    }
                }
                
                request.url.encodedPath == "/api/v1/analytics/insights" -> {
                    respond(
                        content = """
                            {
                                "success": true,
                                "data": {
                                    "spendingInsights": [
                                        {
                                            "category": "FOOD_AND_DRINK",
                                            "currentMonth": 45000,
                                            "previousMonth": 52000,
                                            "trend": "DECREASING",
                                            "recommendation": "Great job reducing dining expenses!"
                                        }
                                    ],
                                    "netWorth": {
                                        "current": 400000,
                                        "previous": 385000,
                                        "change": 15000,
                                        "trend": "INCREASING"
                                    }
                                }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                
                request.url.encodedPath == "/api/v1/gamification/profile" -> {
                    respond(
                        content = """
                            {
                                "success": true,
                                "data": {
                                    "level": 5,
                                    "totalPoints": 2500,
                                    "currentStreaks": [
                                        {
                                            "id": "daily_checkin",
                                            "type": "DAILY_CHECK_IN",
                                            "currentCount": 7,
                                            "bestCount": 14
                                        }
                                    ],
                                    "achievements": [
                                        {
                                            "id": "first_goal",
                                            "title": "Goal Setter",
                                            "description": "Created your first financial goal",
                                            "unlockedAt": "2024-01-10T09:00:00Z"
                                        }
                                    ]
                                }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                
                request.url.encodedPath == "/api/v1/sync/trigger" -> {
                    respond(
                        content = """
                            {
                                "success": true,
                                "data": {
                                    "syncId": "sync123",
                                    "status": "STARTED",
                                    "estimatedDuration": 30
                                }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.Accepted,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                
                request.url.encodedPath.startsWith("/api/v1/sync/status/") -> {
                    val syncId = request.url.encodedPath.split("/").last()
                    respond(
                        content = """
                            {
                                "success": true,
                                "data": {
                                    "syncId": "$syncId",
                                    "status": "COMPLETED",
                                    "accountsUpdated": 2,
                                    "transactionsAdded": 15,
                                    "duration": 25
                                }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                
                // Rate limiting test endpoint
                request.url.encodedPath == "/api/v1/rate-limited" -> {
                    respond(
                        content = """
                            {
                                "success": false,
                                "error": {
                                    "code": "RATE_LIMIT_EXCEEDED",
                                    "message": "Too many requests",
                                    "retryAfter": 60
                                }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.TooManyRequests,
                        headers = headersOf(
                            HttpHeaders.ContentType to listOf("application/json"),
                            "Retry-After" to listOf("60")
                        )
                    )
                }
                
                // Server error test endpoint
                request.url.encodedPath == "/api/v1/server-error" -> {
                    respond(
                        content = """
                            {
                                "success": false,
                                "error": {
                                    "code": "INTERNAL_SERVER_ERROR",
                                    "message": "An unexpected error occurred"
                                }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                
                // Network timeout simulation
                request.url.encodedPath == "/api/v1/timeout" -> {
                    // Simulate timeout by not responding
                    throw Exception("Request timeout")
                }
                
                else -> {
                    respond(
                        content = """
                            {
                                "success": false,
                                "error": {
                                    "code": "NOT_FOUND",
                                    "message": "Endpoint not found"
                                }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }

        httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    @AfterTest
    fun tearDown() {
        mockEngine.close()
        httpClient.close()
    }

    @Test
    fun testSuccessfulAuthentication() = runTest {
        val response = httpClient.post("https://api.north.com/api/v1/auth/login") {
            headers {
                append(HttpHeaders.Authorization, "Bearer valid-token")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody("""{"email": "test@example.com", "password": "password"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        assertTrue(json["success"]?.jsonPrimitive?.boolean == true)
        assertNotNull(json["data"]?.jsonObject?.get("accessToken"))
        assertNotNull(json["data"]?.jsonObject?.get("userId"))
    }

    @Test
    fun testFailedAuthentication() = runTest {
        val response = httpClient.post("https://api.north.com/api/v1/auth/login") {
            headers {
                append(HttpHeaders.Authorization, "Bearer invalid-token")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody("""{"email": "test@example.com", "password": "wrong"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        assertTrue(json["success"]?.jsonPrimitive?.boolean == false)
        assertEquals("INVALID_CREDENTIALS", json["error"]?.jsonObject?.get("code")?.jsonPrimitive?.content)
    }

    @Test
    fun testUserProfileRetrieval() = runTest {
        val response = httpClient.get("https://api.north.com/api/v1/users/profile") {
            headers {
                append(HttpHeaders.Authorization, "Bearer valid-access-token")
            }
        }

        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        assertTrue(json["success"]?.jsonPrimitive?.boolean == true)
        val userData = json["data"]?.jsonObject
        assertNotNull(userData)
        assertEquals("user123", userData["id"]?.jsonPrimitive?.content)
        assertEquals("test@example.com", userData["email"]?.jsonPrimitive?.content)
        
        val profile = userData["profile"]?.jsonObject
        assertNotNull(profile)
        assertEquals("John", profile["firstName"]?.jsonPrimitive?.content)
        assertEquals("Doe", profile["lastName"]?.jsonPrimitive?.content)
    }

    @Test
    fun testAccountsListRetrieval() = runTest {
        val response = httpClient.get("https://api.north.com/api/v1/accounts") {
            headers {
                append(HttpHeaders.Authorization, "Bearer valid-access-token")
            }
        }

        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        assertTrue(json["success"]?.jsonPrimitive?.boolean == true)
        val accounts = json["data"]?.jsonArray
        assertNotNull(accounts)
        assertEquals(1, accounts.size)
        
        val account = accounts.first().jsonObject
        assertEquals("acc123", account["id"]?.jsonPrimitive?.content)
        assertEquals("RBC Royal Bank", account["institutionName"]?.jsonPrimitive?.content)
        assertEquals("CHECKING", account["accountType"]?.jsonPrimitive?.content)
        
        val balance = account["balance"]?.jsonObject
        assertNotNull(balance)
        assertEquals(250000, balance["amount"]?.jsonPrimitive?.long)
        assertEquals("CAD", balance["currency"]?.jsonPrimitive?.content)
    }

    @Test
    fun testAccountCreation() = runTest {
        val requestBody = """
            {
                "institutionId": "inst456",
                "accountType": "SAVINGS",
                "publicToken": "public-token-123"
            }
        """.trimIndent()

        val response = httpClient.post("https://api.north.com/api/v1/accounts") {
            headers {
                append(HttpHeaders.Authorization, "Bearer valid-access-token")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        assertTrue(json["success"]?.jsonPrimitive?.boolean == true)
        val account = json["data"]?.jsonObject
        assertNotNull(account)
        assertEquals("acc124", account["id"]?.jsonPrimitive?.content)
        assertEquals("TD Canada Trust", account["institutionName"]?.jsonPrimitive?.content)
        assertEquals("SAVINGS", account["accountType"]?.jsonPrimitive?.content)
    }

    @Test
    fun testTransactionsRetrieval() = runTest {
        val accountId = "acc123"
        val response = httpClient.get("https://api.north.com/api/v1/accounts/$accountId/transactions") {
            headers {
                append(HttpHeaders.Authorization, "Bearer valid-access-token")
            }
            parameter("startDate", "2024-01-01")
            parameter("endDate", "2024-01-31")
            parameter("limit", "50")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        assertTrue(json["success"]?.jsonPrimitive?.boolean == true)
        val transactions = json["data"]?.jsonArray
        assertNotNull(transactions)
        assertEquals(1, transactions.size)
        
        val transaction = transactions.first().jsonObject
        assertEquals("txn123", transaction["id"]?.jsonPrimitive?.content)
        assertEquals(accountId, transaction["accountId"]?.jsonPrimitive?.content)
        assertEquals("Coffee Shop Purchase", transaction["description"]?.jsonPrimitive?.content)
        assertEquals("FOOD_AND_DRINK", transaction["category"]?.jsonPrimitive?.content)
        
        val pagination = json["pagination"]?.jsonObject
        assertNotNull(pagination)
        assertEquals(1, pagination["page"]?.jsonPrimitive?.int)
        assertEquals(50, pagination["limit"]?.jsonPrimitive?.int)
        assertEquals(1, pagination["total"]?.jsonPrimitive?.int)
        assertEquals(false, pagination["hasMore"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun testGoalsManagement() = runTest {
        // Test getting goals
        val getResponse = httpClient.get("https://api.north.com/api/v1/goals") {
            headers {
                append(HttpHeaders.Authorization, "Bearer valid-access-token")
            }
        }

        assertEquals(HttpStatusCode.OK, getResponse.status)
        
        val getResponseBody = getResponse.bodyAsText()
        val getJson = Json.parseToJsonElement(getResponseBody).jsonObject
        
        assertTrue(getJson["success"]?.jsonPrimitive?.boolean == true)
        val goals = getJson["data"]?.jsonArray
        assertNotNull(goals)
        assertEquals(1, goals.size)
        
        val goal = goals.first().jsonObject
        assertEquals("goal123", goal["id"]?.jsonPrimitive?.content)
        assertEquals("Emergency Fund", goal["title"]?.jsonPrimitive?.content)
        assertEquals("HIGH", goal["priority"]?.jsonPrimitive?.content)

        // Test creating a goal
        val createRequestBody = """
            {
                "title": "Vacation Fund",
                "targetAmount": {
                    "amount": 300000,
                    "currency": "CAD"
                },
                "targetDate": "2024-08-15",
                "priority": "MEDIUM"
            }
        """.trimIndent()

        val postResponse = httpClient.post("https://api.north.com/api/v1/goals") {
            headers {
                append(HttpHeaders.Authorization, "Bearer valid-access-token")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(createRequestBody)
        }

        assertEquals(HttpStatusCode.Created, postResponse.status)
        
        val postResponseBody = postResponse.bodyAsText()
        val postJson = Json.parseToJsonElement(postResponseBody).jsonObject
        
        assertTrue(postJson["success"]?.jsonPrimitive?.boolean == true)
        val createdGoal = postJson["data"]?.jsonObject
        assertNotNull(createdGoal)
        assertEquals("goal124", createdGoal["id"]?.jsonPrimitive?.content)
        assertEquals("Vacation Fund", createdGoal["title"]?.jsonPrimitive?.content)
    }

    @Test
    fun testAnalyticsInsights() = runTest {
        val response = httpClient.get("https://api.north.com/api/v1/analytics/insights") {
            headers {
                append(HttpHeaders.Authorization, "Bearer valid-access-token")
            }
            parameter("period", "current_month")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        assertTrue(json["success"]?.jsonPrimitive?.boolean == true)
        val data = json["data"]?.jsonObject
        assertNotNull(data)
        
        val spendingInsights = data["spendingInsights"]?.jsonArray
        assertNotNull(spendingInsights)
        assertEquals(1, spendingInsights.size)
        
        val insight = spendingInsights.first().jsonObject
        assertEquals("FOOD_AND_DRINK", insight["category"]?.jsonPrimitive?.content)
        assertEquals("DECREASING", insight["trend"]?.jsonPrimitive?.content)
        assertNotNull(insight["recommendation"]?.jsonPrimitive?.content)
        
        val netWorth = data["netWorth"]?.jsonObject
        assertNotNull(netWorth)
        assertEquals(400000, netWorth["current"]?.jsonPrimitive?.long)
        assertEquals(385000, netWorth["previous"]?.jsonPrimitive?.long)
        assertEquals("INCREASING", netWorth["trend"]?.jsonPrimitive?.content)
    }

    @Test
    fun testGamificationProfile() = runTest {
        val response = httpClient.get("https://api.north.com/api/v1/gamification/profile") {
            headers {
                append(HttpHeaders.Authorization, "Bearer valid-access-token")
            }
        }

        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        assertTrue(json["success"]?.jsonPrimitive?.boolean == true)
        val profile = json["data"]?.jsonObject
        assertNotNull(profile)
        
        assertEquals(5, profile["level"]?.jsonPrimitive?.int)
        assertEquals(2500, profile["totalPoints"]?.jsonPrimitive?.int)
        
        val streaks = profile["currentStreaks"]?.jsonArray
        assertNotNull(streaks)
        assertEquals(1, streaks.size)
        
        val streak = streaks.first().jsonObject
        assertEquals("daily_checkin", streak["id"]?.jsonPrimitive?.content)
        assertEquals("DAILY_CHECK_IN", streak["type"]?.jsonPrimitive?.content)
        assertEquals(7, streak["currentCount"]?.jsonPrimitive?.int)
        
        val achievements = profile["achievements"]?.jsonArray
        assertNotNull(achievements)
        assertEquals(1, achievements.size)
        
        val achievement = achievements.first().jsonObject
        assertEquals("first_goal", achievement["id"]?.jsonPrimitive?.content)
        assertEquals("Goal Setter", achievement["title"]?.jsonPrimitive?.content)
    }

    @Test
    fun testSyncOperations() = runTest {
        // Test triggering sync
        val triggerResponse = httpClient.post("https://api.north.com/api/v1/sync/trigger") {
            headers {
                append(HttpHeaders.Authorization, "Bearer valid-access-token")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody("""{"accountIds": ["acc123", "acc124"]}""")
        }

        assertEquals(HttpStatusCode.Accepted, triggerResponse.status)
        
        val triggerResponseBody = triggerResponse.bodyAsText()
        val triggerJson = Json.parseToJsonElement(triggerResponseBody).jsonObject
        
        assertTrue(triggerJson["success"]?.jsonPrimitive?.boolean == true)
        val triggerData = triggerJson["data"]?.jsonObject
        assertNotNull(triggerData)
        
        val syncId = triggerData["syncId"]?.jsonPrimitive?.content
        assertEquals("sync123", syncId)
        assertEquals("STARTED", triggerData["status"]?.jsonPrimitive?.content)

        // Test checking sync status
        val statusResponse = httpClient.get("https://api.north.com/api/v1/sync/status/$syncId") {
            headers {
                append(HttpHeaders.Authorization, "Bearer valid-access-token")
            }
        }

        assertEquals(HttpStatusCode.OK, statusResponse.status)
        
        val statusResponseBody = statusResponse.bodyAsText()
        val statusJson = Json.parseToJsonElement(statusResponseBody).jsonObject
        
        assertTrue(statusJson["success"]?.jsonPrimitive?.boolean == true)
        val statusData = statusJson["data"]?.jsonObject
        assertNotNull(statusData)
        
        assertEquals(syncId, statusData["syncId"]?.jsonPrimitive?.content)
        assertEquals("COMPLETED", statusData["status"]?.jsonPrimitive?.content)
        assertEquals(2, statusData["accountsUpdated"]?.jsonPrimitive?.int)
        assertEquals(15, statusData["transactionsAdded"]?.jsonPrimitive?.int)
    }

    @Test
    fun testRateLimitHandling() = runTest {
        val response = httpClient.get("https://api.north.com/api/v1/rate-limited") {
            headers {
                append(HttpHeaders.Authorization, "Bearer valid-access-token")
            }
        }

        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        assertEquals("60", response.headers["Retry-After"])
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        assertTrue(json["success"]?.jsonPrimitive?.boolean == false)
        val error = json["error"]?.jsonObject
        assertNotNull(error)
        assertEquals("RATE_LIMIT_EXCEEDED", error["code"]?.jsonPrimitive?.content)
        assertEquals(60, error["retryAfter"]?.jsonPrimitive?.int)
    }

    @Test
    fun testServerErrorHandling() = runTest {
        val response = httpClient.get("https://api.north.com/api/v1/server-error") {
            headers {
                append(HttpHeaders.Authorization, "Bearer valid-access-token")
            }
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        assertTrue(json["success"]?.jsonPrimitive?.boolean == false)
        val error = json["error"]?.jsonObject
        assertNotNull(error)
        assertEquals("INTERNAL_SERVER_ERROR", error["code"]?.jsonPrimitive?.content)
        assertTrue(error["message"]?.jsonPrimitive?.content?.isNotBlank() == true)
    }

    @Test
    fun testNetworkTimeoutHandling() = runTest {
        try {
            httpClient.get("https://api.north.com/api/v1/timeout") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer valid-access-token")
                }
            }
            fail("Expected exception for timeout")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("timeout") == true)
        }
    }

    @Test
    fun testNotFoundEndpoint() = runTest {
        val response = httpClient.get("https://api.north.com/api/v1/nonexistent") {
            headers {
                append(HttpHeaders.Authorization, "Bearer valid-access-token")
            }
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        assertTrue(json["success"]?.jsonPrimitive?.boolean == false)
        val error = json["error"]?.jsonObject
        assertNotNull(error)
        assertEquals("NOT_FOUND", error["code"]?.jsonPrimitive?.content)
    }

    @Test
    fun testConcurrentAPIRequests() = runTest {
        val requests = (1..5).map {
            kotlinx.coroutines.async {
                httpClient.get("https://api.north.com/api/v1/users/profile") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer valid-access-token")
                    }
                }
            }
        }

        val responses = requests.map { it.await() }
        
        // All requests should succeed
        assertTrue(responses.all { it.status == HttpStatusCode.OK })
        
        // All responses should have the same user data
        val responseBodies = responses.map { it.bodyAsText() }
        val userIds = responseBodies.map { body ->
            Json.parseToJsonElement(body).jsonObject["data"]?.jsonObject?.get("id")?.jsonPrimitive?.content
        }
        
        assertTrue(userIds.all { it == "user123" })
    }

    @Test
    fun testRequestWithQueryParameters() = runTest {
        val response = httpClient.get("https://api.north.com/api/v1/accounts/acc123/transactions") {
            headers {
                append(HttpHeaders.Authorization, "Bearer valid-access-token")
            }
            parameter("startDate", "2024-01-01")
            parameter("endDate", "2024-01-31")
            parameter("category", "FOOD_AND_DRINK")
            parameter("limit", "25")
            parameter("offset", "0")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        assertTrue(json["success"]?.jsonPrimitive?.boolean == true)
        assertNotNull(json["data"]?.jsonArray)
        assertNotNull(json["pagination"]?.jsonObject)
    }

    @Test
    fun testRequestWithComplexPayload() = runTest {
        val complexPayload = """
            {
                "title": "House Down Payment",
                "description": "Saving for a house down payment in Toronto",
                "targetAmount": {
                    "amount": 10000000,
                    "currency": "CAD"
                },
                "targetDate": "2025-06-01",
                "priority": "HIGH",
                "microTasks": [
                    {
                        "title": "Save $500 monthly",
                        "targetAmount": {
                            "amount": 50000,
                            "currency": "CAD"
                        }
                    },
                    {
                        "title": "Reduce dining out",
                        "targetAmount": {
                            "amount": 20000,
                            "currency": "CAD"
                        }
                    }
                ],
                "tags": ["housing", "investment", "long-term"]
            }
        """.trimIndent()

        val response = httpClient.post("https://api.north.com/api/v1/goals") {
            headers {
                append(HttpHeaders.Authorization, "Bearer valid-access-token")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(complexPayload)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        assertTrue(json["success"]?.jsonPrimitive?.boolean == true)
        assertNotNull(json["data"]?.jsonObject)
    }
}