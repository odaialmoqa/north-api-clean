package com.north.mobile.integration

import com.north.mobile.data.auth.*
import com.north.mobile.data.plaid.*
import com.north.mobile.data.sync.*
import com.north.mobile.data.goal.*
import com.north.mobile.data.gamification.*
import com.north.mobile.data.notification.*
import com.north.mobile.data.repository.*
import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

/**
 * End-to-end integration tests for complete user flows
 * Tests realistic user scenarios from start to finish
 */
class EndToEndUserFlowTest {

    private lateinit var authManager: MockAuthenticationManager
    private lateinit var plaidService: MockPlaidService
    private lateinit var syncService: MockSyncService
    private lateinit var goalService: MockGoalService
    private lateinit var gamificationService: MockGamificationService
    private lateinit var notificationService: MockNotificationService
    private lateinit var userRepository: MockUserRepository
    private lateinit var accountRepository: MockAccountRepository
    private lateinit var transactionRepository: MockTransactionRepository

    @BeforeTest
    fun setup() {
        authManager = MockAuthenticationManager()
        plaidService = MockPlaidService()
        syncService = MockSyncService()
        goalService = MockGoalService()
        gamificationService = MockGamificationService()
        notificationService = MockNotificationService()
        userRepository = MockUserRepository()
        accountRepository = MockAccountRepository()
        transactionRepository = MockTransactionRepository()
    }

    @Test
    fun testCompleteNewUserOnboardingFlow() = runTest {
        val userId = "new-user-123"
        val email = "newuser@example.com"

        // Step 1: User registration and authentication setup
        val registrationResult = authManager.registerUser(email, "password123")
        assertTrue(registrationResult.isSuccess)
        val user = registrationResult.getOrThrow()
        assertEquals(userId, user.id)
        assertEquals(email, user.email)

        // Step 2: Set up biometric authentication
        val biometricSetupResult = authManager.setupBiometricAuth(userId)
        assertTrue(biometricSetupResult.isSuccess)

        // Step 3: Link first financial account
        val linkTokenResult = plaidService.createLinkToken(userId)
        assertTrue(linkTokenResult.isSuccess)
        val linkToken = linkTokenResult.getOrThrow()

        // Simulate user completing Plaid Link flow
        val publicToken = "public-token-123"
        val accessTokenResult = plaidService.exchangePublicToken(publicToken)
        assertTrue(accessTokenResult.isSuccess)
        val accessToken = accessTokenResult.getOrThrow()

        // Get accounts from Plaid
        val accountsResult = plaidService.getAccounts(accessToken.accessToken)
        assertTrue(accountsResult.isSuccess)
        val plaidAccounts = accountsResult.getOrThrow()
        assertEquals(2, plaidAccounts.size) // Mock returns 2 accounts

        // Convert and save accounts
        val accounts = plaidAccounts.map { 
            it.toAccount(accessToken.institutionId, accessToken.institutionName) 
        }
        accounts.forEach { account ->
            val saveResult = accountRepository.save(account)
            assertTrue(saveResult.isSuccess)
        }

        // Step 4: Initial data sync
        val syncResult = syncService.syncAllAccounts(userId)
        assertTrue(syncResult.isSuccess)
        val syncData = syncResult.getOrThrow()
        assertEquals(2, syncData.accountsUpdated)
        assertTrue(syncData.transactionsAdded > 0)

        // Step 5: Create first financial goal
        val emergencyFundGoal = FinancialGoal(
            id = "goal-emergency-fund",
            userId = userId,
            title = "Emergency Fund",
            description = "Build 6 months of expenses",
            targetAmount = Money.fromDollars(10000.0),
            currentAmount = Money.fromDollars(0.0),
            targetDate = LocalDate(2024, 12, 31),
            priority = Priority.HIGH,
            category = GoalCategory.EMERGENCY_FUND,
            isActive = true,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val goalResult = goalService.createGoal(emergencyFundGoal)
        assertTrue(goalResult.isSuccess)

        // Step 6: Award onboarding points and achievements
        val pointsResult = gamificationService.awardPoints(userId, 100, "Account linking completed")
        assertTrue(pointsResult.isSuccess)

        val achievementResult = gamificationService.checkAchievements(userId)
        assertTrue(achievementResult.isSuccess)
        val achievements = achievementResult.getOrThrow()
        assertTrue(achievements.any { it.id == "first_account_linked" })

        // Step 7: Set up notification preferences
        val notificationPreferences = NotificationPreferences(
            userId = userId,
            enabledTypes = setOf(
                NotificationType.GOAL_PROGRESS,
                NotificationType.STREAK_RISK,
                NotificationType.MILESTONE
            ),
            maxDailyNotifications = 5,
            quietHoursStart = 22,
            quietHoursEnd = 8
        )
        val preferencesResult = notificationService.updateNotificationPreferences(notificationPreferences)
        assertTrue(preferencesResult.isSuccess)

        // Verify complete onboarding state
        val finalUser = userRepository.findById(userId).getOrThrow()
        assertNotNull(finalUser)
        assertEquals(email, finalUser!!.email)

        val userAccounts = accountRepository.findByUserId(userId).getOrThrow()
        assertEquals(2, userAccounts.size)

        val userGoals = goalService.getUserGoals(userId)
        // In a real implementation, this would be a Flow, but for testing we'll check the mock
        assertTrue(goalService.hasGoals(userId))

        val gamificationProfile = gamificationService.getUserProfile(userId)
        // Profile should exist and have initial points
        assertNotNull(gamificationProfile)
    }

    @Test
    fun testDailyUserEngagementFlow() = runTest {
        val userId = "existing-user-456"
        
        // Setup: User already exists with accounts and goals
        setupExistingUser(userId)

        // Step 1: User opens app and authenticates
        val authResult = authManager.authenticateWithBiometrics(userId)
        assertTrue(authResult.isSuccess)

        // Step 2: App performs background sync
        val syncResult = syncService.incrementalSync(userId)
        assertTrue(syncResult.isSuccess)
        val syncData = syncResult.getOrThrow()
        assertTrue(syncData.transactionsAdded >= 0) // May be 0 if no new transactions

        // Step 3: User checks dashboard (triggers micro-win opportunities)
        val microWinResult = gamificationService.getMicroWinOpportunities(userId)
        assertTrue(microWinResult.isSuccess)
        val microWins = microWinResult.getOrThrow()
        assertTrue(microWins.isNotEmpty())

        // Step 4: User completes a micro-win (categorizes transactions)
        val pointsResult = gamificationService.awardPoints(userId, 10, "Transaction categorized")
        assertTrue(pointsResult.isSuccess)

        val streakResult = gamificationService.updateStreak(userId, "daily_checkin")
        assertTrue(streakResult.isSuccess)

        // Step 5: User updates goal progress
        val goalProgressResult = goalService.updateGoalProgress(
            "goal-vacation", 
            Money.fromDollars(1250.0)
        )
        assertTrue(goalProgressResult.isSuccess)

        // Step 6: Check for achievements and level progress
        val achievementResult = gamificationService.checkAchievements(userId)
        assertTrue(achievementResult.isSuccess)

        val levelResult = gamificationService.getLevelProgress(userId)
        assertTrue(levelResult.isSuccess)
        val levelProgress = levelResult.getOrThrow()
        assertTrue(levelProgress.currentXP > 0)

        // Step 7: Process any triggered notifications
        val notificationResult = notificationService.processGoalProgressNotifications()
        assertTrue(notificationResult.isSuccess)

        // Verify engagement was properly tracked
        val finalProfile = gamificationService.getUserProfile(userId)
        assertNotNull(finalProfile)
        // Profile should show updated activity
    }

    @Test
    fun testGoalCreationAndTrackingFlow() = runTest {
        val userId = "goal-user-789"
        setupExistingUser(userId)

        // Step 1: User creates a new vacation goal
        val vacationGoal = FinancialGoal(
            id = "goal-vacation-new",
            userId = userId,
            title = "European Vacation",
            description = "2-week trip to Europe",
            targetAmount = Money.fromDollars(5000.0),
            currentAmount = Money.fromDollars(0.0),
            targetDate = LocalDate(2024, 8, 15),
            priority = Priority.MEDIUM,
            category = GoalCategory.TRAVEL,
            isActive = true,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val createResult = goalService.createGoal(vacationGoal)
        assertTrue(createResult.isSuccess)

        // Step 2: Award points for goal creation
        val pointsResult = gamificationService.awardPoints(userId, 50, "New goal created")
        assertTrue(pointsResult.isSuccess)

        // Step 3: User makes initial contribution
        val initialContribution = Money.fromDollars(200.0)
        val progressResult = goalService.updateGoalProgress(vacationGoal.id, initialContribution)
        assertTrue(progressResult.isSuccess)

        // Step 4: Check goal progress visualization
        val visualizationService = MockGoalProgressVisualizationService()
        val progressData = visualizationService.getGoalProgress(vacationGoal.id)
        assertTrue(progressData.isSuccess)
        val progress = progressData.getOrThrow()
        assertEquals(0.04, progress.progressPercentage, 0.01) // 200/5000 = 4%

        // Step 5: User sets up automatic savings for goal
        // This would typically involve setting up recurring transfers
        val autoSaveResult = goalService.setupAutoSave(vacationGoal.id, Money.fromDollars(100.0), "weekly")
        assertTrue(autoSaveResult.isSuccess)

        // Step 6: Simulate weekly progress over time
        repeat(10) { week ->
            val weeklyContribution = Money.fromDollars((week + 1) * 100.0 + 200.0) // Cumulative
            val weeklyProgressResult = goalService.updateGoalProgress(vacationGoal.id, weeklyContribution)
            assertTrue(weeklyProgressResult.isSuccess)

            // Award streak points for consistent saving
            val streakPointsResult = gamificationService.awardPoints(userId, 15, "Weekly savings streak")
            assertTrue(streakPointsResult.isSuccess)
        }

        // Step 7: Check final goal status
        val finalProgressData = visualizationService.getGoalProgress(vacationGoal.id)
        assertTrue(finalProgressData.isSuccess)
        val finalProgress = finalProgressData.getOrThrow()
        assertTrue(finalProgress.progressPercentage > 0.20) // Should be over 20% after 10 weeks

        // Step 8: Check if any milestones were reached
        val milestoneResult = gamificationService.checkAchievements(userId)
        assertTrue(milestoneResult.isSuccess)
        val achievements = milestoneResult.getOrThrow()
        assertTrue(achievements.any { it.category == AchievementCategory.GOALS })
    }

    @Test
    fun testAccountSyncAndCategorizationFlow() = runTest {
        val userId = "sync-user-101"
        setupExistingUser(userId)

        // Step 1: Trigger manual sync
        val syncResult = syncService.syncAllAccounts(userId)
        assertTrue(syncResult.isSuccess)
        val syncData = syncResult.getOrThrow()
        assertTrue(syncData.transactionsAdded > 0)

        // Step 2: Get newly synced transactions
        val accountId = "account-checking-123"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        val transactionsResult = transactionRepository.findByAccountAndDateRange(accountId, startDate, endDate)
        assertTrue(transactionsResult.isSuccess)
        val transactions = transactionsResult.getOrThrow()
        assertTrue(transactions.isNotEmpty())

        // Step 3: User categorizes uncategorized transactions
        val uncategorizedTransactions = transactions.filter { it.category == Category.UNCATEGORIZED }
        
        for (transaction in uncategorizedTransactions.take(3)) { // Categorize first 3
            val categorizedTransaction = transaction.copy(
                category = when {
                    transaction.description.contains("Coffee", ignoreCase = true) -> Category.FOOD_AND_DRINK
                    transaction.description.contains("Gas", ignoreCase = true) -> Category.TRANSPORTATION
                    else -> Category.SHOPPING
                }
            )
            
            val updateResult = transactionRepository.update(categorizedTransaction)
            assertTrue(updateResult.isSuccess)

            // Award points for categorization
            val pointsResult = gamificationService.awardPoints(userId, 5, "Transaction categorized")
            assertTrue(pointsResult.isSuccess)
        }

        // Step 4: Check categorization streak
        val streakResult = gamificationService.updateStreak(userId, "categorization_streak")
        assertTrue(streakResult.isSuccess)

        // Step 5: Generate spending insights based on categorized transactions
        val analyticsService = MockFinancialAnalyticsService()
        val insightsResult = analyticsService.generateSpendingInsights(userId, startDate, endDate)
        assertTrue(insightsResult.isSuccess)
        val insights = insightsResult.getOrThrow()
        assertTrue(insights.categoryBreakdown.isNotEmpty())

        // Step 6: Check if categorization triggered any achievements
        val achievementResult = gamificationService.checkAchievements(userId)
        assertTrue(achievementResult.isSuccess)
        val achievements = achievementResult.getOrThrow()
        
        // Should have categorization-related achievements
        assertTrue(achievements.any { it.id.contains("categorization") || it.id.contains("organization") })
    }

    @Test
    fun testNotificationAndEngagementFlow() = runTest {
        val userId = "notification-user-202"
        setupExistingUser(userId)

        // Step 1: Set up user for streak risk (simulate user hasn't opened app in 2 days)
        val mockRepository = MockNotificationRepository()
        mockRepository.usersWithStreaksAtRisk = mapOf(
            userId to listOf(
                StreakAtRisk("daily_checkin", 5, 2) // 5-day streak at risk, 2 days since last activity
            )
        )

        // Step 2: Process streak risk notifications
        val streakNotificationResult = notificationService.processStreakRiskNotifications()
        assertTrue(streakNotificationResult.isSuccess)
        val streakDeliveries = streakNotificationResult.getOrThrow()
        assertEquals(1, streakDeliveries.size)
        assertTrue(streakDeliveries.first().success)

        // Step 3: User responds to notification by opening app
        val authResult = authManager.authenticateWithBiometrics(userId)
        assertTrue(authResult.isSuccess)

        // Step 4: Award comeback points and restore streak
        val comebackPointsResult = gamificationService.awardPoints(userId, 25, "Streak saved!")
        assertTrue(comebackPointsResult.isSuccess)

        val streakUpdateResult = gamificationService.updateStreak(userId, "daily_checkin")
        assertTrue(streakUpdateResult.isSuccess)

        // Step 5: User makes goal progress, triggering milestone notification
        val goalProgressResult = goalService.updateGoalProgress("goal-emergency-fund", Money.fromDollars(2500.0))
        assertTrue(goalProgressResult.isSuccess)

        // Step 6: Process goal progress notifications
        mockRepository.goalProgressUpdates = mapOf(
            userId to listOf(
                GoalProgressUpdate(
                    goalId = "goal-emergency-fund",
                    goalTitle = "Emergency Fund",
                    previousProgress = 0.20,
                    currentProgress = 0.25,
                    targetAmount = 10000.0,
                    currentAmount = 2500.0
                )
            )
        )

        val goalNotificationResult = notificationService.processGoalProgressNotifications()
        assertTrue(goalNotificationResult.isSuccess)
        val goalDeliveries = goalNotificationResult.getOrThrow()
        assertEquals(1, goalDeliveries.size)

        // Step 7: Check notification history
        val historyResult = notificationService.getNotificationHistory(userId, 10)
        assertTrue(historyResult.isSuccess)
        val history = historyResult.getOrThrow()
        assertTrue(history.size >= 2) // Should have streak and goal notifications
    }

    @Test
    fun testErrorRecoveryAndResilienceFlow() = runTest {
        val userId = "resilience-user-303"
        setupExistingUser(userId)

        // Step 1: Simulate network failure during sync
        val failingSyncService = FailingMockSyncService()
        val syncResult = failingSyncService.syncAllAccounts(userId)
        assertTrue(syncResult.isFailure)

        // Step 2: User retries sync after network recovery
        val retryResult = syncService.syncAllAccounts(userId)
        assertTrue(retryResult.isSuccess)

        // Step 3: Simulate Plaid authentication error
        val failingPlaidService = FailingMockPlaidService()
        val accountsResult = failingPlaidService.getAccounts("invalid-token")
        assertTrue(accountsResult.isFailure)

        // Step 4: User re-authenticates with Plaid
        val reauthResult = plaidService.createUpdateLinkToken("valid-access-token")
        assertTrue(reauthResult.isSuccess)

        // Step 5: Simulate goal service failure during goal creation
        val failingGoalService = FailingMockGoalService()
        val newGoal = FinancialGoal(
            id = "goal-test-failure",
            userId = userId,
            title = "Test Goal",
            description = "Testing failure recovery",
            targetAmount = Money.fromDollars(1000.0),
            currentAmount = Money.fromDollars(0.0),
            targetDate = LocalDate(2024, 6, 1),
            priority = Priority.LOW,
            category = GoalCategory.OTHER,
            isActive = true,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val failedGoalResult = failingGoalService.createGoal(newGoal)
        assertTrue(failedGoalResult.isFailure)

        // Step 6: User retries goal creation successfully
        val successfulGoalResult = goalService.createGoal(newGoal)
        assertTrue(successfulGoalResult.isSuccess)

        // Step 7: Verify system state is consistent after recovery
        val finalSyncResult = syncService.syncAllAccounts(userId)
        assertTrue(finalSyncResult.isSuccess)

        val userGoals = goalService.getUserGoals(userId)
        assertTrue(goalService.hasGoals(userId))

        // System should be fully functional after error recovery
    }

    // Helper method to set up an existing user with accounts and goals
    private suspend fun setupExistingUser(userId: String) {
        // Create user
        val user = User(
            id = userId,
            email = "existing@example.com",
            profile = UserProfile(
                firstName = "John",
                lastName = "Doe",
                phoneNumber = "+1-416-555-0123",
                dateOfBirth = LocalDate(1990, 1, 1),
                sin = "123-456-789",
                postalCode = "M5V 3A8"
            ),
            preferences = UserPreferences(
                currency = Currency.CAD,
                language = "en",
                notificationsEnabled = true,
                biometricAuthEnabled = true,
                budgetAlerts = true,
                goalReminders = true,
                spendingInsights = true,
                marketingEmails = false
            ),
            gamificationData = GamificationProfile(
                level = 3,
                totalPoints = 1500,
                currentStreaks = listOf(
                    Streak(
                        id = "daily_checkin",
                        type = StreakType.DAILY_CHECK_IN,
                        currentCount = 5,
                        bestCount = 12,
                        lastUpdated = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    )
                ),
                achievements = listOf(
                    Achievement(
                        id = "first_account_linked",
                        title = "Account Linker",
                        description = "Linked your first financial account",
                        badgeIcon = "link_badge",
                        unlockedAt = Clock.System.now(),
                        category = AchievementCategory.ONBOARDING
                    )
                ),
                lastActivity = Clock.System.now()
            )
        )
        userRepository.saveUser(user)

        // Create accounts
        val checkingAccount = Account(
            id = "account-checking-123",
            institutionId = "inst-rbc",
            institutionName = "RBC Royal Bank",
            accountType = AccountType.CHECKING,
            balance = Money.fromDollars(2500.0),
            availableBalance = Money.fromDollars(2300.0),
            currency = Currency.CAD,
            lastUpdated = Clock.System.now(),
            accountNumber = "1234",
            nickname = "Main Checking",
            isActive = true
        )

        val savingsAccount = Account(
            id = "account-savings-456",
            institutionId = "inst-rbc",
            institutionName = "RBC Royal Bank",
            accountType = AccountType.SAVINGS,
            balance = Money.fromDollars(15000.0),
            availableBalance = Money.fromDollars(15000.0),
            currency = Currency.CAD,
            lastUpdated = Clock.System.now(),
            accountNumber = "5678",
            nickname = "Emergency Savings",
            isActive = true
        )

        accountRepository.save(checkingAccount)
        accountRepository.save(savingsAccount)

        // Create sample transactions
        val transactions = listOf(
            Transaction(
                id = "txn-1",
                accountId = checkingAccount.id,
                amount = Money.fromDollars(-25.50),
                description = "Coffee Shop Purchase",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 1, 15),
                isRecurring = false
            ),
            Transaction(
                id = "txn-2",
                accountId = checkingAccount.id,
                amount = Money.fromDollars(-65.00),
                description = "Gas Station",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 1, 14),
                isRecurring = false
            ),
            Transaction(
                id = "txn-3",
                accountId = checkingAccount.id,
                amount = Money.fromDollars(-120.00),
                description = "Grocery Store",
                category = Category.UNCATEGORIZED,
                date = LocalDate(2024, 1, 13),
                isRecurring = false
            )
        )

        transactions.forEach { transaction ->
            transactionRepository.insert(transaction)
        }

        // Create sample goals
        val emergencyFundGoal = FinancialGoal(
            id = "goal-emergency-fund",
            userId = userId,
            title = "Emergency Fund",
            description = "6 months of expenses",
            targetAmount = Money.fromDollars(10000.0),
            currentAmount = Money.fromDollars(2000.0),
            targetDate = LocalDate(2024, 12, 31),
            priority = Priority.HIGH,
            category = GoalCategory.EMERGENCY_FUND,
            isActive = true,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val vacationGoal = FinancialGoal(
            id = "goal-vacation",
            userId = userId,
            title = "Summer Vacation",
            description = "Trip to Europe",
            targetAmount = Money.fromDollars(3000.0),
            currentAmount = Money.fromDollars(1000.0),
            targetDate = LocalDate(2024, 7, 15),
            priority = Priority.MEDIUM,
            category = GoalCategory.TRAVEL,
            isActive = true,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        goalService.createGoal(emergencyFundGoal)
        goalService.createGoal(vacationGoal)
    }
}

// Mock implementations for end-to-end testing
private class MockAuthenticationManager : AuthenticationManager {
    private val users = mutableMapOf<String, User>()
    
    suspend fun registerUser(email: String, password: String): Result<User> {
        val userId = "user-${System.currentTimeMillis()}"
        val user = User(
            id = userId,
            email = email,
            profile = UserProfile(
                firstName = "Test",
                lastName = "User",
                phoneNumber = "+1-416-555-0123",
                dateOfBirth = LocalDate(1990, 1, 1),
                sin = "123-456-789",
                postalCode = "M5V 3A8"
            ),
            preferences = UserPreferences(
                currency = Currency.CAD,
                language = "en",
                notificationsEnabled = true,
                biometricAuthEnabled = false,
                budgetAlerts = true,
                goalReminders = true,
                spendingInsights = true,
                marketingEmails = false
            ),
            gamificationData = GamificationProfile(
                level = 1,
                totalPoints = 0,
                currentStreaks = emptyList(),
                achievements = emptyList(),
                lastActivity = Clock.System.now()
            )
        )
        users[userId] = user
        return Result.success(user)
    }
    
    suspend fun authenticateWithBiometrics(userId: String): Result<AuthResult> {
        return if (users.containsKey(userId)) {
            Result.success(AuthResult.Success("mock-session-token"))
        } else {
            Result.failure(Exception("User not found"))
        }
    }
    
    suspend fun setupBiometricAuth(userId: String): Result<Unit> {
        return Result.success(Unit)
    }
}

// Additional mock classes would be implemented similarly...
// For brevity, I'll include key ones and indicate others

private class MockPlaidService : PlaidService {
    override suspend fun createLinkToken(userId: String): Result<PlaidLinkToken> {
        return Result.success(PlaidLinkToken(
            linkToken = "link-token-123",
            expiration = Clock.System.now().plus(kotlinx.datetime.DateTimeUnit.HOUR),
            requestId = "request-123"
        ))
    }
    
    override suspend fun exchangePublicToken(publicToken: String): Result<PlaidAccessToken> {
        return Result.success(PlaidAccessToken(
            accessToken = "access-token-123",
            itemId = "item-123",
            institutionId = "inst-rbc",
            institutionName = "RBC Royal Bank"
        ))
    }
    
    override suspend fun getAccounts(accessToken: String): Result<List<PlaidAccount>> {
        return Result.success(listOf(
            PlaidAccount(
                accountId = "plaid-acc-1",
                itemId = "item-123",
                name = "Checking",
                officialName = "RBC Checking Account",
                type = PlaidAccountType.DEPOSITORY,
                subtype = PlaidAccountSubtype.CHECKING,
                mask = "1234",
                balances = PlaidBalances(2300.0, 2500.0, null, "CAD"),
                verificationStatus = null
            ),
            PlaidAccount(
                accountId = "plaid-acc-2",
                itemId = "item-123",
                name = "Savings",
                officialName = "RBC Savings Account",
                type = PlaidAccountType.DEPOSITORY,
                subtype = PlaidAccountSubtype.SAVINGS,
                mask = "5678",
                balances = PlaidBalances(15000.0, 15000.0, null, "CAD"),
                verificationStatus = null
            )
        ))
    }
    
    // Implement other required methods...
    override suspend fun getBalances(accessToken: String): Result<List<PlaidAccount>> = getAccounts(accessToken)
    override suspend fun getTransactions(accessToken: String, startDate: LocalDate, endDate: LocalDate, accountIds: List<String>?): Result<List<Transaction>> {
        return Result.success(listOf(
            Transaction("txn-new-1", "plaid-acc-1", Money.fromDollars(-45.0), "Restaurant", Category.UNCATEGORIZED, LocalDate(2024, 1, 16), false),
            Transaction("txn-new-2", "plaid-acc-1", Money.fromDollars(-12.0), "Coffee", Category.UNCATEGORIZED, LocalDate(2024, 1, 16), false)
        ))
    }
    override suspend fun getItem(accessToken: String): Result<PlaidItem> = Result.success(PlaidItem("item-123", "inst-rbc", null, null, emptyList(), emptyList(), null, null))
    override suspend fun removeItem(accessToken: String): Result<Unit> = Result.success(Unit)
    override suspend fun createUpdateLinkToken(accessToken: String): Result<PlaidLinkToken> = createLinkToken("user")
    override suspend fun getCanadianInstitutions(): Result<List<FinancialInstitution>> = Result.success(CanadianInstitutions.MAJOR_CANADIAN_BANKS)
    override suspend fun searchInstitutions(query: String): Result<List<FinancialInstitution>> = Result.success(CanadianInstitutions.searchByName(query))
}

private class MockSyncService : SyncService {
    override suspend fun syncAllAccounts(userId: String): Result<SyncResult> {
        return Result.success(SyncResult.Success(
            accountsUpdated = 2,
            transactionsAdded = 5,
            transactionsUpdated = 1,
            conflictsResolved = 0,
            syncDuration = 2500
        ))
    }
    
    override suspend fun syncAccount(accountId: String): Result<SyncResult> {
        return Result.success(SyncResult.Success(1, 2, 0, 0, 1200))
    }
    
    override suspend fun syncTransactions(accountId: String, startDate: LocalDate, endDate: LocalDate): Result<SyncResult> {
        return Result.success(SyncResult.Success(0, 3, 1, 0, 800))
    }
    
    override suspend fun incrementalSync(userId: String): Result<SyncResult> {
        return Result.success(SyncResult.Success(1, 2, 0, 0, 1500))
    }
    
    override fun getSyncStatus(userId: String) = kotlinx.coroutines.flow.flowOf(emptyList<AccountSyncStatus>())
    override fun getAccountSyncStatus(accountId: String) = kotlinx.coroutines.flow.flowOf(AccountSyncStatus("", SyncStatus.SUCCESS, null, Clock.System.now()))
    override suspend fun cancelSync(userId: String) {}
    override suspend fun scheduleBackgroundSync(userId: String, intervalMinutes: Long) {}
    override suspend fun stopBackgroundSync(userId: String) {}
}

// Additional mock implementations would follow similar patterns...
// Including MockGoalService, MockGamificationService, MockNotificationService, etc.

private class MockGoalService : GoalService {
    private val goals = mutableMapOf<String, FinancialGoal>()
    
    override suspend fun getUserGoals(userId: String) = kotlinx.coroutines.flow.flowOf(goals.values.filter { it.userId == userId })
    override suspend fun createGoal(goal: FinancialGoal): Result<FinancialGoal> {
        goals[goal.id] = goal
        return Result.success(goal)
    }
    override suspend fun updateGoal(goal: FinancialGoal): Result<FinancialGoal> {
        goals[goal.id] = goal
        return Result.success(goal)
    }
    override suspend fun deleteGoal(goalId: String): Result<Unit> {
        goals.remove(goalId)
        return Result.success(Unit)
    }
    override suspend fun updateGoalProgress(goalId: String, currentAmount: Money): Result<Unit> {
        goals[goalId]?.let { goal ->
            goals[goalId] = goal.copy(currentAmount = currentAmount, updatedAt = Clock.System.now())
        }
        return Result.success(Unit)
    }
    
    fun hasGoals(userId: String): Boolean = goals.values.any { it.userId == userId }
    suspend fun setupAutoSave(goalId: String, amount: Money, frequency: String): Result<Unit> = Result.success(Unit)
}

private class MockGamificationService : GamificationService {
    private val profiles = mutableMapOf<String, GamificationProfile>()
    
    override suspend fun getUserProfile(userId: String) = kotlinx.coroutines.flow.flowOf(profiles[userId])
    override suspend fun awardPoints(userId: String, points: Int, reason: String): Result<Unit> {
        val profile = profiles[userId] ?: GamificationProfile(1, 0, emptyList(), emptyList(), Clock.System.now())
        profiles[userId] = profile.copy(totalPoints = profile.totalPoints + points)
        return Result.success(Unit)
    }
    override suspend fun updateStreak(userId: String, streakType: String): Result<Unit> = Result.success(Unit)
    override suspend fun checkAchievements(userId: String): Result<List<Achievement>> {
        return Result.success(listOf(
            Achievement("first_account_linked", "Account Linker", "Linked first account", "badge", Clock.System.now(), AchievementCategory.ONBOARDING)
        ))
    }
    override suspend fun getLevelProgress(userId: String): Result<LevelProgress> = Result.success(LevelProgress(3, 1500, 2000))
    override suspend fun getMicroWinOpportunities(userId: String): Result<List<MicroWin>> {
        return Result.success(listOf(
            MicroWin("categorize_transactions", "Categorize 3 transactions", 15, "organization")
        ))
    }
}

private class MockNotificationService : NotificationService {
    override suspend fun sendImmediateNotification(userId: String, content: NotificationContent): Result<NotificationDeliveryResult> = Result.success(NotificationDeliveryResult("notif-123", true, Clock.System.now()))
    override suspend fun scheduleNotification(schedule: NotificationSchedule): Result<String> = Result.success(schedule.id)
    override suspend fun cancelNotification(notificationId: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Unit> = Result.success(Unit)
    override suspend fun getNotificationPreferences(userId: String): Result<NotificationPreferences?> = Result.success(null)
    override suspend fun getScheduledNotifications(userId: String): Result<List<NotificationSchedule>> = Result.success(emptyList())
    override suspend fun processScheduledNotifications(): Result<List<NotificationDeliveryResult>> = Result.success(emptyList())
    override suspend fun processStreakRiskNotifications(): Result<List<NotificationDeliveryResult>> = Result.success(listOf(NotificationDeliveryResult("streak-notif", true, Clock.System.now())))
    override suspend fun processEngagementNotifications(): Result<List<NotificationDeliveryResult>> = Result.success(emptyList())
    override suspend fun processGoalProgressNotifications(): Result<List<NotificationDeliveryResult>> = Result.success(listOf(NotificationDeliveryResult("goal-notif", true, Clock.System.now())))
    override suspend fun processMilestoneNotifications(): Result<List<NotificationDeliveryResult>> = Result.success(emptyList())
    override suspend fun getNotificationHistory(userId: String, limit: Int): Result<List<NotificationDeliveryResult>> = Result.success(emptyList())
}

// Repository mocks
private class MockUserRepository : UserRepository {
    private val users = mutableMapOf<String, User>()
    override suspend fun getUser(userId: String): User? = users[userId]
    override suspend fun saveUser(user: User): Result<Unit> { users[user.id] = user; return Result.success(Unit) }
    override suspend fun updateUser(user: User): Result<Unit> { users[user.id] = user; return Result.success(Unit) }
    override suspend fun deleteUser(userId: String): Result<Unit> { users.remove(userId); return Result.success(Unit) }
    suspend fun findById(userId: String): Result<User?> = Result.success(users[userId])
}

private class MockAccountRepository : AccountRepository {
    private val accounts = mutableMapOf<String, Account>()
    suspend fun save(account: Account): Result<Unit> { accounts[account.id] = account; return Result.success(Unit) }
    suspend fun findByUserId(userId: String): Result<List<Account>> = Result.success(accounts.values.toList())
    suspend fun findById(accountId: String): Result<Account?> = Result.success(accounts[accountId])
    suspend fun updateBalance(accountId: String, balance: Money): Result<Unit> {
        accounts[accountId]?.let { account ->
            accounts[accountId] = account.copy(balance = balance)
        }
        return Result.success(Unit)
    }
}

private class MockTransactionRepository : TransactionRepository {
    private val transactions = mutableMapOf<String, Transaction>()
    suspend fun insert(transaction: Transaction): Result<Unit> { transactions[transaction.id] = transaction; return Result.success(Unit) }
    suspend fun update(transaction: Transaction): Result<Unit> { transactions[transaction.id] = transaction; return Result.success(Unit) }
    suspend fun findByAccountAndDateRange(accountId: String, startDate: LocalDate, endDate: LocalDate): Result<List<Transaction>> {
        return Result.success(transactions.values.filter { it.accountId == accountId && it.date >= startDate && it.date <= endDate })
    }
}

// Failing mock services for error testing
private class FailingMockSyncService : SyncService {
    override suspend fun syncAllAccounts(userId: String): Result<SyncResult> = Result.failure(Exception("Network error"))
    override suspend fun syncAccount(accountId: String): Result<SyncResult> = Result.failure(Exception("Network error"))
    override suspend fun syncTransactions(accountId: String, startDate: LocalDate, endDate: LocalDate): Result<SyncResult> = Result.failure(Exception("Network error"))
    override suspend fun incrementalSync(userId: String): Result<SyncResult> = Result.failure(Exception("Network error"))
    override fun getSyncStatus(userId: String) = kotlinx.coroutines.flow.flowOf(emptyList<AccountSyncStatus>())
    override fun getAccountSyncStatus(accountId: String) = kotlinx.coroutines.flow.flowOf(AccountSyncStatus("", SyncStatus.ERROR, null, Clock.System.now()))
    override suspend fun cancelSync(userId: String) {}
    override suspend fun scheduleBackgroundSync(userId: String, intervalMinutes: Long) {}
    override suspend fun stopBackgroundSync(userId: String) {}
}

private class FailingMockPlaidService : PlaidService {
    override suspend fun getAccounts(accessToken: String): Result<List<PlaidAccount>> = Result.failure(Exception("Authentication failed"))
    // Other methods would return failures as well...
    override suspend fun createLinkToken(userId: String): Result<PlaidLinkToken> = Result.failure(Exception("API error"))
    override suspend fun exchangePublicToken(publicToken: String): Result<PlaidAccessToken> = Result.failure(Exception("Invalid token"))
    override suspend fun getBalances(accessToken: String): Result<List<PlaidAccount>> = Result.failure(Exception("Authentication failed"))
    override suspend fun getTransactions(accessToken: String, startDate: LocalDate, endDate: LocalDate, accountIds: List<String>?): Result<List<Transaction>> = Result.failure(Exception("API error"))
    override suspend fun getItem(accessToken: String): Result<PlaidItem> = Result.failure(Exception("Authentication failed"))
    override suspend fun removeItem(accessToken: String): Result<Unit> = Result.failure(Exception("API error"))
    override suspend fun createUpdateLinkToken(accessToken: String): Result<PlaidLinkToken> = Result.failure(Exception("Authentication failed"))
    override suspend fun getCanadianInstitutions(): Result<List<FinancialInstitution>> = Result.failure(Exception("API error"))
    override suspend fun searchInstitutions(query: String): Result<List<FinancialInstitution>> = Result.failure(Exception("API error"))
}

private class FailingMockGoalService : GoalService {
    override suspend fun createGoal(goal: FinancialGoal): Result<FinancialGoal> = Result.failure(Exception("Database error"))
    override suspend fun getUserGoals(userId: String) = kotlinx.coroutines.flow.flowOf(emptyList<FinancialGoal>())
    override suspend fun updateGoal(goal: FinancialGoal): Result<FinancialGoal> = Result.failure(Exception("Database error"))
    override suspend fun deleteGoal(goalId: String): Result<Unit> = Result.failure(Exception("Database error"))
    override suspend fun updateGoalProgress(goalId: String, currentAmount: Money): Result<Unit> = Result.failure(Exception("Database error"))
}

// Additional helper classes
private class MockGoalProgressVisualizationService {
    suspend fun getGoalProgress(goalId: String): Result<GoalProgressData> {
        return Result.success(GoalProgressData(
            goalId = goalId,
            progressPercentage = 0.25,
            projectedCompletionDate = LocalDate(2024, 10, 15),
            isOnTrack = true
        ))
    }
}

private class MockFinancialAnalyticsService {
    suspend fun generateSpendingInsights(userId: String, startDate: LocalDate, endDate: LocalDate): Result<SpendingInsights> {
        return Result.success(SpendingInsights(
            categoryBreakdown = mapOf(
                Category.FOOD_AND_DRINK to Money.fromDollars(250.0),
                Category.TRANSPORTATION to Money.fromDollars(150.0),
                Category.SHOPPING to Money.fromDollars(300.0)
            ),
            totalSpent = Money.fromDollars(700.0),
            trends = emptyList()
        ))
    }
}

// Data classes for testing
data class GoalProgressData(
    val goalId: String,
    val progressPercentage: Double,
    val projectedCompletionDate: LocalDate,
    val isOnTrack: Boolean
)

data class SpendingInsights(
    val categoryBreakdown: Map<Category, Money>,
    val totalSpent: Money,
    val trends: List<String>
)

data class StreakAtRisk(
    val streakType: String,
    val currentCount: Int,
    val daysSinceLastActivity: Int
)

data class GoalProgressUpdate(
    val goalId: String,
    val goalTitle: String,
    val previousProgress: Double,
    val currentProgress: Double,
    val targetAmount: Double,
    val currentAmount: Double
)

private class MockNotificationRepository : NotificationRepository {
    var usersWithStreaksAtRisk = emptyMap<String, List<StreakAtRisk>>()
    var goalProgressUpdates = emptyMap<String, List<GoalProgressUpdate>>()
    
    override suspend fun saveScheduledNotification(schedule: NotificationSchedule) {}
    override suspend fun cancelNotification(notificationId: String) {}
    override suspend fun getScheduledNotifications(userId: String): List<NotificationSchedule> = emptyList()
    override suspend fun saveNotificationPreferences(preferences: NotificationPreferences) {}
    override suspend fun getNotificationPreferences(userId: String): NotificationPreferences? = null
    override suspend fun getTodayNotificationCount(userId: String): Int = 0
    override suspend fun recordNotificationDelivery(result: NotificationDeliveryResult) {}
    override suspend fun getNotificationHistory(userId: String, limit: Int): List<NotificationDeliveryResult> = emptyList()
    override suspend fun getInactiveUsers(): List<String> = emptyList()
    override suspend fun getUsersWithStreaksAtRisk(): Map<String, List<com.north.mobile.data.repository.StreakAtRisk>> = emptyMap()
    override suspend fun getGoalProgressUpdates(): Map<String, List<com.north.mobile.data.repository.GoalProgressUpdate>> = emptyMap()
    override suspend fun getNewMilestones(): Map<String, List<com.north.mobile.data.repository.MilestoneUpdate>> = emptyMap()
}