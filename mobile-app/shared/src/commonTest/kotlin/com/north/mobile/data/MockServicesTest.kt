package com.north.mobile.data

import com.north.mobile.data.analytics.*
import com.north.mobile.data.auth.*
import com.north.mobile.data.gamification.*
import com.north.mobile.data.goal.*
import com.north.mobile.data.notification.*
import com.north.mobile.data.plaid.*
import com.north.mobile.data.privacy.*
import com.north.mobile.data.repository.*
import com.north.mobile.data.security.*
import com.north.mobile.data.sync.*
import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

/**
 * Comprehensive test suite for mock services used in external API testing
 */
class MockServicesTest {
    
    // Mock Authentication Service Tests
    
    @Test
    fun `MockAuthenticationService should handle authentication flows`() = runTest {
        val mockAuth = MockAuthenticationService()
        
        // Test successful authentication
        val successResult = mockAuth.authenticate("valid_user", "valid_password")
        assertTrue(successResult.isSuccess)
        assertNotNull(successResult.getOrNull())
        
        // Test failed authentication
        val failResult = mockAuth.authenticate("invalid_user", "wrong_password")
        assertTrue(failResult.isFailure)
        
        // Test biometric authentication
        val biometricResult = mockAuth.authenticateWithBiometrics()
        assertTrue(biometricResult.isSuccess)
        
        // Test session validation
        val token = successResult.getOrThrow()
        assertTrue(mockAuth.isSessionValid(token.accessToken))
        
        // Test session expiration
        mockAuth.expireSession(token.accessToken)
        assertFalse(mockAuth.isSessionValid(token.accessToken))
    }
    
    @Test
    fun `MockAuthenticationService should handle concurrent authentication attempts`() = runTest {
        val mockAuth = MockAuthenticationService()
        
        // Simulate multiple concurrent authentication attempts
        val results = (1..10).map { i ->
            mockAuth.authenticate("user$i", "password$i")
        }
        
        // All should succeed (mock service)
        results.forEach { result ->
            assertTrue(result.isSuccess)
        }
        
        // Each should have unique tokens
        val tokens = results.map { it.getOrThrow().accessToken }
        assertEquals(10, tokens.toSet().size) // All unique
    }
    
    // Mock Plaid Service Tests
    
    @Test
    fun `MockPlaidService should simulate account linking flow`() = runTest {
        val mockPlaid = MockPlaidService()
        
        // Test institution search
        val institutions = mockPlaid.searchInstitutions("RBC")
        assertTrue(institutions.isSuccess)
        assertTrue(institutions.getOrThrow().isNotEmpty())
        
        // Test link token creation
        val linkTokenResult = mockPlaid.createLinkToken("user123")
        assertTrue(linkTokenResult.isSuccess)
        val linkToken = linkTokenResult.getOrThrow()
        assertNotNull(linkToken.token)
        assertTrue(linkToken.expiration > Clock.System.now())
        
        // Test account linking
        val linkResult = mockPlaid.linkAccount(linkToken.token, "mock_public_token")
        assertTrue(linkResult.isSuccess)
        val linkedAccount = linkResult.getOrThrow()
        assertNotNull(linkedAccount.accessToken)
        assertTrue(linkedAccount.accounts.isNotEmpty())
        
        // Test account data retrieval
        val accountsResult = mockPlaid.getAccounts(linkedAccount.accessToken)
        assertTrue(accountsResult.isSuccess)
        assertTrue(accountsResult.getOrThrow().isNotEmpty())
        
        // Test transaction retrieval
        val transactionsResult = mockPlaid.getTransactions(
            linkedAccount.accessToken,
            LocalDate(2024, 1, 1),
            LocalDate(2024, 1, 31)
        )
        assertTrue(transactionsResult.isSuccess)
        assertTrue(transactionsResult.getOrThrow().isNotEmpty())
    }
    
    @Test
    fun `MockPlaidService should handle error scenarios`() = runTest {
        val mockPlaid = MockPlaidService()
        
        // Test invalid access token
        val invalidResult = mockPlaid.getAccounts("invalid_token")
        assertTrue(invalidResult.isFailure)
        assertTrue(invalidResult.exceptionOrNull() is PlaidException)
        
        // Test expired link token
        val expiredResult = mockPlaid.linkAccount("expired_token", "public_token")
        assertTrue(expiredResult.isFailure)
        
        // Test rate limiting
        repeat(100) {
            mockPlaid.getAccounts("test_token")
        }
        val rateLimitedResult = mockPlaid.getAccounts("test_token")
        // Mock service might simulate rate limiting
    }
    
    // Mock Notification Service Tests
    
    @Test
    fun `MockNotificationService should handle push notifications`() = runTest {
        val mockNotification = MockNotificationService()
        
        // Test sending notification
        val notification = NotificationRequest(
            userId = "user123",
            title = "Test Notification",
            body = "This is a test notification",
            type = NotificationType.GOAL_REMINDER,
            data = mapOf("goalId" to "goal123")
        )
        
        val sendResult = mockNotification.sendNotification(notification)
        assertTrue(sendResult.isSuccess)
        
        // Test scheduling notification
        val scheduledTime = Clock.System.now().plus(1, DateTimeUnit.HOUR)
        val scheduleResult = mockNotification.scheduleNotification(notification, scheduledTime)
        assertTrue(scheduleResult.isSuccess)
        
        // Test getting notification history
        val historyResult = mockNotification.getNotificationHistory("user123")
        assertTrue(historyResult.isSuccess)
        assertTrue(historyResult.getOrThrow().isNotEmpty())
        
        // Test canceling scheduled notification
        val notificationId = scheduleResult.getOrThrow()
        val cancelResult = mockNotification.cancelNotification(notificationId)
        assertTrue(cancelResult.isSuccess)
    }
    
    @Test
    fun `MockNotificationService should handle batch notifications`() = runTest {
        val mockNotification = MockNotificationService()
        
        val notifications = (1..50).map { i ->
            NotificationRequest(
                userId = "user$i",
                title = "Batch Notification $i",
                body = "This is batch notification $i",
                type = NotificationType.SPENDING_ALERT,
                data = emptyMap()
            )
        }
        
        val batchResult = mockNotification.sendBatchNotifications(notifications)
        assertTrue(batchResult.isSuccess)
        
        val results = batchResult.getOrThrow()
        assertEquals(50, results.size)
        assertTrue(results.all { it.isSuccess })
    }
    
    // Mock Analytics Service Tests
    
    @Test
    fun `MockAnalyticsService should generate financial insights`() = runTest {
        val mockAnalytics = MockAnalyticsService()
        
        // Test spending analysis
        val spendingResult = mockAnalytics.analyzeSpending(
            "user123",
            DateRange(LocalDate(2024, 1, 1), LocalDate(2024, 1, 31))
        )
        assertTrue(spendingResult.isSuccess)
        val analysis = spendingResult.getOrThrow()
        assertTrue(analysis.totalSpent.isPositive)
        assertTrue(analysis.categoryBreakdown.isNotEmpty())
        
        // Test net worth calculation
        val netWorthResult = mockAnalytics.calculateNetWorth("user123")
        assertTrue(netWorthResult.isSuccess)
        val netWorth = netWorthResult.getOrThrow()
        assertNotNull(netWorth.totalAssets)
        assertNotNull(netWorth.totalLiabilities)
        assertNotNull(netWorth.netWorth)
        
        // Test recommendations
        val recommendationsResult = mockAnalytics.generateRecommendations("user123")
        assertTrue(recommendationsResult.isSuccess)
        assertTrue(recommendationsResult.getOrThrow().isNotEmpty())
    }
    
    // Mock Sync Service Tests
    
    @Test
    fun `MockSyncService should handle data synchronization`() = runTest {
        val mockSync = MockSyncService()
        
        // Test full sync
        val fullSyncResult = mockSync.performFullSync("user123")
        assertTrue(fullSyncResult.isSuccess)
        val syncResult = fullSyncResult.getOrThrow()
        assertTrue(syncResult.accountsUpdated >= 0)
        assertTrue(syncResult.transactionsUpdated >= 0)
        
        // Test incremental sync
        val lastSync = Clock.System.now().minus(1, DateTimeUnit.DAY)
        val incrementalResult = mockSync.performIncrementalSync("user123", lastSync)
        assertTrue(incrementalResult.isSuccess)
        
        // Test sync status
        val statusResult = mockSync.getSyncStatus("user123")
        assertTrue(statusResult.isSuccess)
        val status = statusResult.getOrThrow()
        assertNotNull(status.lastSyncTime)
        assertTrue(status.isHealthy)
        
        // Test conflict resolution
        val conflicts = listOf(
            SyncConflict(
                id = "conflict1",
                type = ConflictType.TRANSACTION_MISMATCH,
                localData = "local_transaction",
                remoteData = "remote_transaction"
            )
        )
        val resolveResult = mockSync.resolveConflicts("user123", conflicts)
        assertTrue(resolveResult.isSuccess)
    }
    
    // Mock Security Service Tests
    
    @Test
    fun `MockSecurityService should handle encryption operations`() = runTest {
        val mockSecurity = MockSecurityService()
        
        // Test data encryption
        val sensitiveData = "This is sensitive financial data"
        val encryptResult = mockSecurity.encrypt(sensitiveData)
        assertTrue(encryptResult.isSuccess)
        val encryptedData = encryptResult.getOrThrow()
        assertNotEquals(sensitiveData, String(encryptedData.encryptedContent))
        
        // Test data decryption
        val decryptResult = mockSecurity.decrypt(encryptedData)
        assertTrue(decryptResult.isSuccess)
        assertEquals(sensitiveData, decryptResult.getOrThrow())
        
        // Test key generation
        val keyResult = mockSecurity.generateSecureKey()
        assertTrue(keyResult.isSuccess)
        val key = keyResult.getOrThrow()
        assertTrue(key.isNotEmpty())
        
        // Test key rotation
        val rotateResult = mockSecurity.rotateKeys()
        assertTrue(rotateResult.isSuccess)
    }
    
    // Integration Test with Multiple Mock Services
    
    @Test
    fun `mock services should work together in integration scenarios`() = runTest {
        val mockAuth = MockAuthenticationService()
        val mockPlaid = MockPlaidService()
        val mockAnalytics = MockAnalyticsService()
        val mockNotification = MockNotificationService()
        
        // Simulate user onboarding flow
        val authResult = mockAuth.authenticate("new_user", "password")
        assertTrue(authResult.isSuccess)
        val token = authResult.getOrThrow()
        
        // Link account
        val linkTokenResult = mockPlaid.createLinkToken("new_user")
        assertTrue(linkTokenResult.isSuccess)
        val linkResult = mockPlaid.linkAccount(
            linkTokenResult.getOrThrow().token,
            "mock_public_token"
        )
        assertTrue(linkResult.isSuccess)
        
        // Analyze initial data
        val analysisResult = mockAnalytics.analyzeSpending(
            "new_user",
            DateRange(LocalDate(2024, 1, 1), LocalDate(2024, 1, 31))
        )
        assertTrue(analysisResult.isSuccess)
        
        // Send welcome notification
        val welcomeNotification = NotificationRequest(
            userId = "new_user",
            title = "Welcome to North!",
            body = "Your account has been successfully linked.",
            type = NotificationType.WELCOME,
            data = emptyMap()
        )
        val notificationResult = mockNotification.sendNotification(welcomeNotification)
        assertTrue(notificationResult.isSuccess)
    }
    
    // Performance and Load Testing with Mock Services
    
    @Test
    fun `mock services should handle high load scenarios`() = runTest {
        val mockAuth = MockAuthenticationService()
        val mockPlaid = MockPlaidService()
        
        // Simulate high concurrent load
        val concurrentUsers = 100
        val authResults = (1..concurrentUsers).map { i ->
            mockAuth.authenticate("user$i", "password$i")
        }
        
        // All authentications should succeed
        authResults.forEach { result ->
            assertTrue(result.isSuccess)
        }
        
        // Simulate concurrent account linking
        val linkResults = (1..concurrentUsers).map { i ->
            mockPlaid.createLinkToken("user$i")
        }
        
        linkResults.forEach { result ->
            assertTrue(result.isSuccess)
        }
    }
    
    // Error Simulation and Recovery Testing
    
    @Test
    fun `mock services should simulate various error conditions`() = runTest {
        val mockPlaid = MockPlaidService()
        
        // Test network timeout simulation
        mockPlaid.simulateNetworkDelay(5000) // 5 second delay
        val slowResult = mockPlaid.getAccounts("test_token")
        // Should eventually succeed or timeout appropriately
        
        // Test service unavailable
        mockPlaid.simulateServiceUnavailable(true)
        val unavailableResult = mockPlaid.getAccounts("test_token")
        assertTrue(unavailableResult.isFailure)
        
        // Test recovery
        mockPlaid.simulateServiceUnavailable(false)
        val recoveryResult = mockPlaid.getAccounts("test_token")
        assertTrue(recoveryResult.isSuccess)
        
        // Test partial failures
        mockPlaid.simulatePartialFailure(0.3f) // 30% failure rate
        val partialResults = (1..10).map {
            mockPlaid.getAccounts("test_token_$it")
        }
        
        val successCount = partialResults.count { it.isSuccess }
        val failureCount = partialResults.count { it.isFailure }
        assertTrue(successCount > 0)
        assertTrue(failureCount > 0)
    }
}

// Mock Service Implementations for Testing

class MockAuthenticationService {
    private val validCredentials = mutableMapOf<String, String>()
    private val activeSessions = mutableSetOf<String>()
    
    suspend fun authenticate(username: String, password: String): Result<AuthToken> {
        return if (username.startsWith("valid_") || !validCredentials.containsKey(username)) {
            val token = AuthToken(
                accessToken = "mock_access_token_${System.currentTimeMillis()}",
                refreshToken = "mock_refresh_token_${System.currentTimeMillis()}",
                expiresAt = Clock.System.now().plus(1, DateTimeUnit.HOUR),
                scope = listOf("read", "write")
            )
            activeSessions.add(token.accessToken)
            validCredentials[username] = password
            Result.success(token)
        } else {
            Result.failure(AuthenticationException("Invalid credentials"))
        }
    }
    
    suspend fun authenticateWithBiometrics(): Result<AuthToken> {
        val token = AuthToken(
            accessToken = "mock_biometric_token_${System.currentTimeMillis()}",
            refreshToken = "mock_refresh_token_${System.currentTimeMillis()}",
            expiresAt = Clock.System.now().plus(1, DateTimeUnit.HOUR),
            scope = listOf("read", "write")
        )
        activeSessions.add(token.accessToken)
        return Result.success(token)
    }
    
    fun isSessionValid(accessToken: String): Boolean {
        return activeSessions.contains(accessToken)
    }
    
    fun expireSession(accessToken: String) {
        activeSessions.remove(accessToken)
    }
}

class MockPlaidService {
    private var networkDelay = 0L
    private var serviceUnavailable = false
    private var partialFailureRate = 0.0f
    private val requestCount = mutableMapOf<String, Int>()
    
    suspend fun searchInstitutions(query: String): Result<List<FinancialInstitution>> {
        if (serviceUnavailable) return Result.failure(PlaidException("Service unavailable"))
        simulateDelay()
        
        val institutions = listOf(
            FinancialInstitution(
                id = "rbc",
                name = "Royal Bank of Canada",
                logo = "rbc_logo.png",
                primaryColor = "#005DAA",
                url = "https://www.rbc.com"
            ),
            FinancialInstitution(
                id = "td",
                name = "TD Canada Trust",
                logo = "td_logo.png",
                primaryColor = "#00B04F",
                url = "https://www.td.com"
            )
        ).filter { it.name.contains(query, ignoreCase = true) }
        
        return Result.success(institutions)
    }
    
    suspend fun createLinkToken(userId: String): Result<PlaidLinkToken> {
        if (serviceUnavailable) return Result.failure(PlaidException("Service unavailable"))
        simulateDelay()
        
        val token = PlaidLinkToken(
            token = "link_token_${userId}_${System.currentTimeMillis()}",
            expiration = Clock.System.now().plus(1, DateTimeUnit.HOUR)
        )
        return Result.success(token)
    }
    
    suspend fun linkAccount(linkToken: String, publicToken: String): Result<PlaidLinkedAccount> {
        if (serviceUnavailable) return Result.failure(PlaidException("Service unavailable"))
        if (linkToken.contains("expired")) return Result.failure(PlaidException("Link token expired"))
        simulateDelay()
        
        val linkedAccount = PlaidLinkedAccount(
            accessToken = "access_token_${System.currentTimeMillis()}",
            itemId = "item_${System.currentTimeMillis()}",
            accounts = listOf(
                Account(
                    id = "account_1",
                    institutionId = "rbc",
                    institutionName = "Royal Bank of Canada",
                    accountType = AccountType.CHECKING,
                    balance = Money.fromDollars(2500.0),
                    lastUpdated = Clock.System.now()
                )
            )
        )
        return Result.success(linkedAccount)
    }
    
    suspend fun getAccounts(accessToken: String): Result<List<Account>> {
        if (serviceUnavailable) return Result.failure(PlaidException("Service unavailable"))
        if (accessToken == "invalid_token") return Result.failure(PlaidException("Invalid access token"))
        
        // Simulate rate limiting
        val count = requestCount.getOrDefault(accessToken, 0) + 1
        requestCount[accessToken] = count
        if (count > 100) return Result.failure(PlaidException("Rate limit exceeded"))
        
        // Simulate partial failures
        if (partialFailureRate > 0 && Math.random() < partialFailureRate) {
            return Result.failure(PlaidException("Temporary failure"))
        }
        
        simulateDelay()
        
        val accounts = listOf(
            Account(
                id = "account_1",
                institutionId = "rbc",
                institutionName = "Royal Bank of Canada",
                accountType = AccountType.CHECKING,
                balance = Money.fromDollars(2500.0),
                lastUpdated = Clock.System.now()
            ),
            Account(
                id = "account_2",
                institutionId = "rbc",
                institutionName = "Royal Bank of Canada",
                accountType = AccountType.SAVINGS,
                balance = Money.fromDollars(15000.0),
                lastUpdated = Clock.System.now()
            )
        )
        return Result.success(accounts)
    }
    
    suspend fun getTransactions(
        accessToken: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<Transaction>> {
        if (serviceUnavailable) return Result.failure(PlaidException("Service unavailable"))
        simulateDelay()
        
        val transactions = listOf(
            Transaction(
                id = "txn_1",
                accountId = "account_1",
                amount = Money.fromDollars(-85.50),
                description = "GROCERY STORE",
                category = Category.GROCERIES,
                date = LocalDate(2024, 1, 15),
                merchantName = "Metro",
                subcategory = null,
                isRecurring = false,
                isVerified = true,
                notes = null
            ),
            Transaction(
                id = "txn_2",
                accountId = "account_1",
                amount = Money.fromDollars(-45.00),
                description = "GAS STATION",
                category = Category.GAS,
                date = LocalDate(2024, 1, 14),
                merchantName = "Shell",
                subcategory = null,
                isRecurring = false,
                isVerified = true,
                notes = null
            )
        )
        return Result.success(transactions)
    }
    
    fun simulateNetworkDelay(delayMs: Long) {
        networkDelay = delayMs
    }
    
    fun simulateServiceUnavailable(unavailable: Boolean) {
        serviceUnavailable = unavailable
    }
    
    fun simulatePartialFailure(failureRate: Float) {
        partialFailureRate = failureRate
    }
    
    private suspend fun simulateDelay() {
        if (networkDelay > 0) {
            kotlinx.coroutines.delay(networkDelay)
        }
    }
}

class MockNotificationService {
    private val notificationHistory = mutableMapOf<String, MutableList<NotificationRecord>>()
    private val scheduledNotifications = mutableMapOf<String, ScheduledNotification>()
    
    suspend fun sendNotification(request: NotificationRequest): Result<String> {
        val notificationId = "notification_${System.currentTimeMillis()}"
        val record = NotificationRecord(
            id = notificationId,
            request = request,
            sentAt = Clock.System.now(),
            status = NotificationStatus.SENT
        )
        
        notificationHistory.getOrPut(request.userId) { mutableListOf() }.add(record)
        return Result.success(notificationId)
    }
    
    suspend fun scheduleNotification(
        request: NotificationRequest,
        scheduledTime: Instant
    ): Result<String> {
        val notificationId = "scheduled_${System.currentTimeMillis()}"
        val scheduled = ScheduledNotification(
            id = notificationId,
            request = request,
            scheduledTime = scheduledTime,
            status = ScheduledNotificationStatus.SCHEDULED
        )
        
        scheduledNotifications[notificationId] = scheduled
        return Result.success(notificationId)
    }
    
    suspend fun cancelNotification(notificationId: String): Result<Unit> {
        scheduledNotifications[notificationId]?.let { scheduled ->
            scheduledNotifications[notificationId] = scheduled.copy(
                status = ScheduledNotificationStatus.CANCELLED
            )
        }
        return Result.success(Unit)
    }
    
    suspend fun getNotificationHistory(userId: String): Result<List<NotificationRecord>> {
        return Result.success(notificationHistory[userId] ?: emptyList())
    }
    
    suspend fun sendBatchNotifications(
        requests: List<NotificationRequest>
    ): Result<List<Result<String>>> {
        val results = requests.map { request ->
            sendNotification(request)
        }
        return Result.success(results)
    }
}

class MockAnalyticsService {
    suspend fun analyzeSpending(userId: String, period: DateRange): Result<SpendingAnalysis> {
        val analysis = SpendingAnalysis(
            period = period,
            totalSpent = Money.fromDollars(1250.0),
            totalIncome = Money.fromDollars(3000.0),
            netCashFlow = Money.fromDollars(1750.0),
            categoryBreakdown = listOf(
                CategorySpending(
                    category = Category.GROCERIES,
                    totalAmount = Money.fromDollars(400.0),
                    transactionCount = 12,
                    averageAmount = Money.fromDollars(33.33),
                    percentageOfTotal = 32.0,
                    trend = TrendDirection.STABLE,
                    comparedToPrevious = Money.fromDollars(25.0)
                ),
                CategorySpending(
                    category = Category.RESTAURANTS,
                    totalAmount = Money.fromDollars(300.0),
                    transactionCount = 8,
                    averageAmount = Money.fromDollars(37.50),
                    percentageOfTotal = 24.0,
                    trend = TrendDirection.INCREASING,
                    comparedToPrevious = Money.fromDollars(50.0)
                )
            ),
            insights = listOf(
                SpendingInsight(
                    id = "insight_1",
                    type = InsightType.SPENDING_PATTERN,
                    title = "Restaurant spending increased",
                    description = "You spent 20% more on restaurants this month",
                    impact = InsightImpact.MEDIUM,
                    actionableRecommendations = listOf("Consider cooking more meals at home"),
                    potentialSavings = Money.fromDollars(100.0),
                    category = Category.RESTAURANTS,
                    confidence = 0.85f
                )
            )
        )
        return Result.success(analysis)
    }
    
    suspend fun calculateNetWorth(userId: String): Result<NetWorthSummary> {
        val summary = NetWorthSummary(
            totalAssets = Money.fromDollars(45000.0),
            totalLiabilities = Money.fromDollars(15000.0),
            netWorth = Money.fromDollars(30000.0),
            assetBreakdown = listOf(
                AssetCategory(
                    type = AssetType.CHECKING,
                    amount = Money.fromDollars(5000.0),
                    accounts = listOf("account_1"),
                    percentageOfTotal = 11.1,
                    monthlyChange = Money.fromDollars(200.0)
                )
            ),
            liabilityBreakdown = listOf(
                LiabilityCategory(
                    type = LiabilityType.CREDIT_CARD,
                    amount = Money.fromDollars(2000.0),
                    accounts = listOf("cc_1"),
                    percentageOfTotal = 13.3,
                    monthlyChange = Money.fromDollars(-100.0),
                    interestRate = 19.99
                )
            ),
            monthlyChange = Money.fromDollars(500.0),
            projections = emptyList()
        )
        return Result.success(summary)
    }
    
    suspend fun generateRecommendations(userId: String): Result<List<Recommendation>> {
        val recommendations = listOf(
            Recommendation(
                id = "rec_1",
                type = RecommendationType.SPENDING_REDUCTION,
                title = "Reduce restaurant spending",
                description = "You could save $100/month by cooking more meals at home",
                priority = Priority.MEDIUM,
                potentialSavings = Money.fromDollars(100.0),
                category = Category.RESTAURANTS,
                actionSteps = listOf("Plan weekly meals", "Buy groceries in bulk"),
                estimatedTimeToImplement = "1 week",
                confidence = 0.8f
            )
        )
        return Result.success(recommendations)
    }
}

class MockSyncService {
    suspend fun performFullSync(userId: String): Result<SyncResult> {
        val result = SyncResult(
            userId = userId,
            syncType = SyncType.FULL,
            startTime = Clock.System.now().minus(30, DateTimeUnit.SECOND),
            endTime = Clock.System.now(),
            accountsUpdated = 3,
            transactionsUpdated = 25,
            conflictsResolved = 0,
            errors = emptyList()
        )
        return Result.success(result)
    }
    
    suspend fun performIncrementalSync(userId: String, lastSync: Instant): Result<SyncResult> {
        val result = SyncResult(
            userId = userId,
            syncType = SyncType.INCREMENTAL,
            startTime = Clock.System.now().minus(10, DateTimeUnit.SECOND),
            endTime = Clock.System.now(),
            accountsUpdated = 1,
            transactionsUpdated = 5,
            conflictsResolved = 0,
            errors = emptyList()
        )
        return Result.success(result)
    }
    
    suspend fun getSyncStatus(userId: String): Result<SyncStatus> {
        val status = SyncStatus(
            userId = userId,
            lastSyncTime = Clock.System.now().minus(1, DateTimeUnit.HOUR),
            nextScheduledSync = Clock.System.now().plus(1, DateTimeUnit.HOUR),
            isHealthy = true,
            pendingConflicts = 0,
            lastError = null
        )
        return Result.success(status)
    }
    
    suspend fun resolveConflicts(userId: String, conflicts: List<SyncConflict>): Result<List<ConflictResolution>> {
        val resolutions = conflicts.map { conflict ->
            ConflictResolution(
                conflictId = conflict.id,
                resolution = ConflictResolutionType.USE_REMOTE,
                resolvedAt = Clock.System.now()
            )
        }
        return Result.success(resolutions)
    }
}

class MockSecurityService {
    suspend fun encrypt(data: String): Result<EncryptedData> {
        val encrypted = EncryptedData(
            encryptedContent = data.reversed().toByteArray(), // Simple mock encryption
            iv = ByteArray(16) { it.toByte() },
            keyAlias = "mock_key"
        )
        return Result.success(encrypted)
    }
    
    suspend fun decrypt(encryptedData: EncryptedData): Result<String> {
        val decrypted = String(encryptedData.encryptedContent).reversed()
        return Result.success(decrypted)
    }
    
    suspend fun generateSecureKey(): Result<String> {
        val key = "mock_secure_key_${System.currentTimeMillis()}"
        return Result.success(key)
    }
    
    suspend fun rotateKeys(): Result<Unit> {
        return Result.success(Unit)
    }
}

// Supporting Data Classes for Mock Services

data class NotificationRequest(
    val userId: String,
    val title: String,
    val body: String,
    val type: NotificationType,
    val data: Map<String, String>
)

data class NotificationRecord(
    val id: String,
    val request: NotificationRequest,
    val sentAt: Instant,
    val status: NotificationStatus
)

data class ScheduledNotification(
    val id: String,
    val request: NotificationRequest,
    val scheduledTime: Instant,
    val status: ScheduledNotificationStatus
)

enum class NotificationStatus { SENT, FAILED, PENDING }
enum class ScheduledNotificationStatus { SCHEDULED, SENT, CANCELLED, FAILED }
enum class NotificationType { WELCOME, GOAL_REMINDER, SPENDING_ALERT }

data class PlaidLinkToken(
    val token: String,
    val expiration: Instant
)

data class PlaidLinkedAccount(
    val accessToken: String,
    val itemId: String,
    val accounts: List<Account>
)

class PlaidException(message: String) : Exception(message)
class AuthenticationException(message: String) : Exception(message)

data class SyncResult(
    val userId: String,
    val syncType: SyncType,
    val startTime: Instant,
    val endTime: Instant,
    val accountsUpdated: Int,
    val transactionsUpdated: Int,
    val conflictsResolved: Int,
    val errors: List<String>
)

data class SyncStatus(
    val userId: String,
    val lastSyncTime: Instant,
    val nextScheduledSync: Instant,
    val isHealthy: Boolean,
    val pendingConflicts: Int,
    val lastError: String?
)

data class SyncConflict(
    val id: String,
    val type: ConflictType,
    val localData: String,
    val remoteData: String
)

data class ConflictResolution(
    val conflictId: String,
    val resolution: ConflictResolutionType,
    val resolvedAt: Instant
)

enum class SyncType { FULL, INCREMENTAL }
enum class ConflictType { TRANSACTION_MISMATCH, ACCOUNT_BALANCE_MISMATCH }
enum class ConflictResolutionType { USE_LOCAL, USE_REMOTE, MERGE }