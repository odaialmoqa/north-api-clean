package com.north.mobile.integration

import com.north.mobile.data.gamification.*
import com.north.mobile.data.repository.GamificationRepository
import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

class StreakTrackingIntegrationTest {
    
    private lateinit var mockRepository: MockGamificationRepository
    private lateinit var celebrationManager: CelebrationManager
    private lateinit var gamificationService: GamificationService
    private lateinit var streakTrackingService: StreakTrackingService
    
    @BeforeTest
    fun setup() {
        mockRepository = MockGamificationRepository()
        celebrationManager = CelebrationManagerImpl()
        gamificationService = GamificationServiceImpl(mockRepository)
        streakTrackingService = StreakTrackingServiceImpl(mockRepository, celebrationManager)
    }
    
    @Test
    fun `complete user journey - building and maintaining streaks with micro-wins`() = runTest {
        val userId = "user123"
        
        // Day 1: User starts their journey
        var result = gamificationService.awardPoints(userId, UserAction.CHECK_BALANCE, description = "First balance check")
        assertTrue(result.isSuccess)
        
        // Check that streak was created
        var streakResult = streakTrackingService.updateStreakWithRiskAssessment(userId, StreakType.DAILY_CHECK_IN)
        assertTrue(streakResult.isSuccess)
        assertEquals(1, streakResult.getOrThrow().streak.currentCount)
        
        // Day 2: User continues the habit
        val tomorrow = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(1, DateTimeUnit.DAY)
        streakResult = streakTrackingService.updateStreakWithRiskAssessment(userId, StreakType.DAILY_CHECK_IN, tomorrow)
        assertTrue(streakResult.isSuccess)
        assertEquals(2, streakResult.getOrThrow().streak.currentCount)
        assertTrue(streakResult.getOrThrow().wasExtended)
        
        // Day 3-6: Building momentum
        var currentDate = tomorrow
        for (day in 3..6) {
            currentDate = currentDate.plus(1, DateTimeUnit.DAY)
            streakResult = streakTrackingService.updateStreakWithRiskAssessment(userId, StreakType.DAILY_CHECK_IN, currentDate)
            assertTrue(streakResult.isSuccess)
            assertEquals(day, streakResult.getOrThrow().streak.currentCount)
        }
        
        // Day 7: Milestone celebration should trigger
        currentDate = currentDate.plus(1, DateTimeUnit.DAY)
        streakResult = streakTrackingService.updateStreakWithRiskAssessment(userId, StreakType.DAILY_CHECK_IN, currentDate)
        assertTrue(streakResult.isSuccess)
        assertEquals(7, streakResult.getOrThrow().streak.currentCount)
        assertNotNull(streakResult.getOrThrow().celebrationEvent) // 7-day milestone
        
        // Day 8: User misses a day - streak breaks
        val dayAfterMissed = currentDate.plus(2, DateTimeUnit.DAY) // Skip day 8
        streakResult = streakTrackingService.updateStreakWithRiskAssessment(userId, StreakType.DAILY_CHECK_IN, dayAfterMissed)
        assertTrue(streakResult.isSuccess)
        assertEquals(1, streakResult.getOrThrow().streak.currentCount) // Reset to 1
        assertTrue(streakResult.getOrThrow().wasBroken)
        assertEquals(7, streakResult.getOrThrow().streak.bestCount) // Best count preserved
        
        // Initiate recovery
        val brokenStreak = streakResult.getOrThrow().streak
        val recoveryResult = streakTrackingService.initiateStreakRecovery(userId, brokenStreak.id)
        assertTrue(recoveryResult.isSuccess)
        val recovery = recoveryResult.getOrThrow()
        
        // Recovery process - 3 actions needed
        var recoveryActionResult = streakTrackingService.processRecoveryAction(userId, recovery.id, UserAction.CHECK_BALANCE)
        assertTrue(recoveryActionResult.isSuccess)
        assertFalse(recoveryActionResult.getOrThrow().isRecoveryComplete)
        
        recoveryActionResult = streakTrackingService.processRecoveryAction(userId, recovery.id, UserAction.CATEGORIZE_TRANSACTION)
        assertTrue(recoveryActionResult.isSuccess)
        assertFalse(recoveryActionResult.getOrThrow().isRecoveryComplete)
        
        recoveryActionResult = streakTrackingService.processRecoveryAction(userId, recovery.id, UserAction.REVIEW_INSIGHTS)
        assertTrue(recoveryActionResult.isSuccess)
        assertTrue(recoveryActionResult.getOrThrow().isRecoveryComplete)
        assertTrue(recoveryActionResult.getOrThrow().recovery.isSuccessful)
        assertNotNull(recoveryActionResult.getOrThrow().newStreakStarted)
        
        // Verify new streak started with recovery attempts tracked
        val newStreak = recoveryActionResult.getOrThrow().newStreakStarted!!
        assertEquals(1, newStreak.currentCount)
        assertEquals(7, newStreak.bestCount) // Preserved from original
        assertEquals(3, newStreak.recoveryAttempts)
    }
    
    @Test
    fun `micro-win detection and streak interaction`() = runTest {
        val userId = "user123"
        
        // User performs bulk transaction categorization
        val microWinResult = streakTrackingService.detectAndAwardMicroWins(
            userId,
            UserAction.CATEGORIZE_TRANSACTION,
            mapOf("transactionCount" to "8")
        )
        
        assertTrue(microWinResult.isSuccess)
        val microWins = microWinResult.getOrThrow()
        assertTrue(microWins.isNotEmpty())
        
        // Should include bulk categorization micro-win
        val bulkMicroWin = microWins.find { it.microWin.title.contains("Bulk") }
        assertNotNull(bulkMicroWin)
        assertEquals(MicroWinDifficulty.MEDIUM, bulkMicroWin.microWin.difficulty)
        
        // Should also update transaction categorization streak
        assertTrue(bulkMicroWin.streaksAffected.isNotEmpty())
        val categorizationStreak = bulkMicroWin.streaksAffected.find { it.type == StreakType.TRANSACTION_CATEGORIZATION }
        assertNotNull(categorizationStreak)
        assertEquals(1, categorizationStreak.currentCount)
    }
    
    @Test
    fun `risk assessment and reminder system`() = runTest {
        val userId = "user123"
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        // Create a streak that's at risk
        val riskStreak = Streak(
            id = "streak1",
            type = StreakType.SAVINGS_CONTRIBUTION,
            currentCount = 10,
            bestCount = 15,
            lastActivityDate = today.minus(2, DateTimeUnit.DAY), // 2 days ago
            isActive = true,
            riskLevel = StreakRiskLevel.MEDIUM_RISK
        )
        
        mockRepository.activeStreaks[userId] = listOf(riskStreak)
        
        // Analyze risks
        val riskAnalysisResult = streakTrackingService.analyzeStreakRisks(userId)
        assertTrue(riskAnalysisResult.isSuccess)
        
        val riskAnalyses = riskAnalysisResult.getOrThrow()
        assertEquals(1, riskAnalyses.size)
        
        val analysis = riskAnalyses.first()
        assertEquals(StreakRiskLevel.MEDIUM_RISK, analysis.riskLevel)
        assertEquals(2, analysis.daysSinceLastActivity)
        assertTrue(analysis.urgencyScore > 3)
        assertTrue(analysis.recommendedActions.isNotEmpty())
        assertTrue(analysis.reminderMessage.contains("10-day"))
    }
    
    @Test
    fun `personalized micro-win generation based on user behavior`() = runTest {
        val userId = "user123"
        
        // Set up user context
        val profile = GamificationProfile(
            level = 3,
            totalPoints = 250,
            currentStreaks = emptyList(),
            achievements = emptyList(),
            lastActivity = Clock.System.now()
        )
        
        val activeStreak = Streak(
            id = "streak1",
            type = StreakType.GOAL_PROGRESS,
            currentCount = 5,
            bestCount = 8,
            lastActivityDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(1, DateTimeUnit.DAY),
            isActive = true,
            riskLevel = StreakRiskLevel.LOW_RISK
        )
        
        val recentActions = listOf(
            PointsHistoryEntry(
                id = "entry1",
                points = 10,
                action = UserAction.CHECK_BALANCE,
                description = "Balance check",
                earnedAt = Clock.System.now().minus(1, DateTimeUnit.HOUR)
            ),
            PointsHistoryEntry(
                id = "entry2",
                points = 15,
                action = UserAction.UPDATE_GOAL,
                description = "Goal update",
                earnedAt = Clock.System.now().minus(2, DateTimeUnit.HOUR)
            )
        )
        
        mockRepository.gamificationProfiles[userId] = profile
        mockRepository.activeStreaks[userId] = listOf(activeStreak)
        mockRepository.pointsHistory[userId] = recentActions
        
        // Generate personalized micro-wins
        val microWinResult = streakTrackingService.generatePersonalizedMicroWins(userId, 5)
        assertTrue(microWinResult.isSuccess)
        
        val microWins = microWinResult.getOrThrow()
        assertTrue(microWins.isNotEmpty())
        
        // Should include streak maintenance micro-win for at-risk streak
        val streakMaintenance = microWins.find { it.contextData.containsKey("streakId") }
        assertNotNull(streakMaintenance)
        assertTrue(streakMaintenance.isPersonalized)
        assertEquals(activeStreak.id, streakMaintenance.contextData["streakId"])
        
        // Should include habit-building micro-wins for underutilized actions
        val habitBuilding = microWins.find { it.title.contains("habit") }
        assertNotNull(habitBuilding)
        assertTrue(habitBuilding.isPersonalized)
        
        // Should prioritize personalized micro-wins
        val personalizedCount = microWins.count { it.isPersonalized }
        assertTrue(personalizedCount > 0)
    }
    
    @Test
    fun `comprehensive streak statistics calculation`() = runTest {
        val userId = "user123"
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        // Set up comprehensive streak data
        val activeStreaks = listOf(
            Streak(
                id = "streak1",
                type = StreakType.DAILY_CHECK_IN,
                currentCount = 15,
                bestCount = 20,
                lastActivityDate = today,
                isActive = true,
                riskLevel = StreakRiskLevel.SAFE
            ),
            Streak(
                id = "streak2",
                type = StreakType.SAVINGS_CONTRIBUTION,
                currentCount = 8,
                bestCount = 12,
                lastActivityDate = today.minus(1, DateTimeUnit.DAY),
                isActive = true,
                riskLevel = StreakRiskLevel.LOW_RISK
            ),
            Streak(
                id = "streak3",
                type = StreakType.UNDER_BUDGET,
                currentCount = 3,
                bestCount = 25,
                lastActivityDate = today.minus(2, DateTimeUnit.DAY),
                isActive = true,
                riskLevel = StreakRiskLevel.MEDIUM_RISK
            )
        )
        
        val allStreaks = activeStreaks + listOf(
            Streak(
                id = "streak4",
                type = StreakType.TRANSACTION_CATEGORIZATION,
                currentCount = 0,
                bestCount = 30,
                lastActivityDate = today.minus(10, DateTimeUnit.DAY),
                isActive = false,
                riskLevel = StreakRiskLevel.BROKEN
            )
        )
        
        val recoveries = listOf(
            StreakRecovery(
                id = "recovery1",
                userId = userId,
                originalStreakId = "streak4",
                streakType = StreakType.TRANSACTION_CATEGORIZATION,
                brokenAt = Clock.System.now().minus(10, DateTimeUnit.DAY),
                recoveryStarted = Clock.System.now().minus(9, DateTimeUnit.DAY),
                originalCount = 30,
                recoveryActions = emptyList(),
                isSuccessful = true
            ),
            StreakRecovery(
                id = "recovery2",
                userId = userId,
                originalStreakId = "streak5",
                streakType = StreakType.GOAL_PROGRESS,
                brokenAt = Clock.System.now().minus(5, DateTimeUnit.DAY),
                recoveryStarted = Clock.System.now().minus(4, DateTimeUnit.DAY),
                originalCount = 10,
                recoveryActions = emptyList(),
                isSuccessful = false
            )
        )
        
        mockRepository.activeStreaks[userId] = activeStreaks
        mockRepository.allUserStreaks[userId] = allStreaks
        mockRepository.allRecoveries[userId] = recoveries
        
        // Get comprehensive statistics
        val statsResult = streakTrackingService.getStreakStatistics(userId)
        assertTrue(statsResult.isSuccess)
        
        val stats = statsResult.getOrThrow()
        
        // Verify basic statistics
        assertEquals(3, stats.totalActiveStreaks)
        assertEquals("streak1", stats.longestCurrentStreak?.id) // 15 days
        assertEquals("streak4", stats.longestEverStreak?.id) // 30 days best
        assertEquals(87, stats.totalStreakDays) // 20 + 12 + 25 + 30
        assertEquals(21.75, stats.averageStreakLength) // 87 / 4
        
        // Verify risk distribution
        assertEquals(1, stats.riskDistribution[StreakRiskLevel.SAFE])
        assertEquals(1, stats.riskDistribution[StreakRiskLevel.LOW_RISK])
        assertEquals(1, stats.riskDistribution[StreakRiskLevel.MEDIUM_RISK])
        
        // Verify streaks by type
        assertEquals(1, stats.streaksByType[StreakType.DAILY_CHECK_IN]?.size)
        assertEquals(1, stats.streaksByType[StreakType.SAVINGS_CONTRIBUTION]?.size)
        assertEquals(1, stats.streaksByType[StreakType.UNDER_BUDGET]?.size)
        
        // Verify recovery success rate
        assertEquals(0.5, stats.recoverySuccessRate) // 1 successful out of 2 total
        
        // Verify weekly trend exists
        assertEquals(7, stats.weeklyStreakTrend.size)
        
        // Verify monthly milestones exist
        assertTrue(stats.monthlyMilestones.isNotEmpty())
    }
    
    @Test
    fun `streak celebration intensity based on milestone significance`() = runTest {
        val userId = "user123"
        
        // Test different milestone celebrations
        val milestones = listOf(
            3 to CelebrationIntensity.LOW,
            7 to CelebrationIntensity.MEDIUM,
            30 to CelebrationIntensity.HIGH,
            90 to CelebrationIntensity.HIGH
        )
        
        for ((count, expectedIntensity) in milestones) {
            val streak = Streak(
                id = "streak_$count",
                type = StreakType.DAILY_CHECK_IN,
                currentCount = count,
                bestCount = count,
                lastActivityDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
                isActive = true,
                riskLevel = StreakRiskLevel.SAFE
            )
            
            val celebrationResult = streakTrackingService.celebrateStreakMilestone(userId, streak)
            assertTrue(celebrationResult.isSuccess)
            
            val celebration = celebrationResult.getOrThrow()
            assertEquals(expectedIntensity, celebration.intensity)
            assertTrue(celebration.message.contains(count.toString()))
            
            if (count >= 30) {
                assertTrue(celebration.duration >= 2500L) // Longer celebrations for major milestones
            }
        }
    }
}

// Reuse the mock classes from StreakTrackingServiceTest
class MockGamificationRepository : GamificationRepository {
    val gamificationProfiles = mutableMapOf<String, GamificationProfile>()
    val streaks = mutableMapOf<Pair<String, StreakType>, Streak>()
    val streaksById = mutableMapOf<String, Streak>()
    val activeStreaks = mutableMapOf<String, List<Streak>>()
    val allUserStreaks = mutableMapOf<String, List<Streak>>()
    val achievements = mutableMapOf<Pair<String, AchievementType>, Achievement>()
    val userAchievements = mutableMapOf<String, List<Achievement>>()
    val pointsHistory = mutableMapOf<String, List<PointsHistoryEntry>>()
    val streakRecoveries = mutableMapOf<String, StreakRecovery>()
    val activeRecoveries = mutableMapOf<String, List<StreakRecovery>>()
    val allRecoveries = mutableMapOf<String, List<StreakRecovery>>()
    val streakReminders = mutableMapOf<String, List<StreakReminder>>()
    
    override suspend fun getGamificationProfile(userId: String): GamificationProfile? {
        return gamificationProfiles[userId]
    }
    
    override suspend fun createGamificationProfile(profile: GamificationProfile, userId: String) {
        gamificationProfiles[userId] = profile
    }
    
    override suspend fun updateGamificationProfile(profile: GamificationProfile, userId: String) {
        gamificationProfiles[userId] = profile
    }
    
    override suspend fun getStreak(userId: String, streakType: StreakType): Streak? {
        return streaks[Pair(userId, streakType)]
    }
    
    override suspend fun getActiveStreaks(userId: String): List<Streak> {
        return activeStreaks[userId] ?: emptyList()
    }
    
    override suspend fun updateStreak(streak: Streak, userId: String) {
        streaks[Pair(userId, streak.type)] = streak
        streaksById[streak.id] = streak
        
        // Update active streaks list
        val currentActive = activeStreaks[userId]?.toMutableList() ?: mutableListOf()
        val existingIndex = currentActive.indexOfFirst { it.id == streak.id }
        if (existingIndex >= 0) {
            currentActive[existingIndex] = streak
        } else {
            currentActive.add(streak)
        }
        activeStreaks[userId] = currentActive
    }
    
    override suspend fun getAchievement(userId: String, achievementType: AchievementType): Achievement? {
        return achievements[Pair(userId, achievementType)]
    }
    
    override suspend fun getAchievements(userId: String): List<Achievement> {
        return userAchievements[userId] ?: emptyList()
    }
    
    override suspend fun addAchievement(achievement: Achievement, userId: String, achievementType: AchievementType) {
        achievements[Pair(userId, achievementType)] = achievement
        userAchievements[userId] = (userAchievements[userId] ?: emptyList()) + achievement
    }
    
    override suspend fun addPointsHistory(entry: PointsHistoryEntry, userId: String) {
        pointsHistory[userId] = (pointsHistory[userId] ?: emptyList()) + entry
    }
    
    override suspend fun getPointsHistory(userId: String, limit: Int): List<PointsHistoryEntry> {
        return pointsHistory[userId]?.take(limit) ?: emptyList()
    }
    
    override suspend fun getStreakById(streakId: String): Streak? {
        return streaksById[streakId]
    }
    
    override suspend fun getAllUserStreaks(userId: String): List<Streak> {
        return allUserStreaks[userId] ?: emptyList()
    }
    
    override suspend fun createStreakRecovery(recovery: StreakRecovery) {
        streakRecoveries[recovery.id] = recovery
    }
    
    override suspend fun getStreakRecovery(recoveryId: String): StreakRecovery? {
        return streakRecoveries[recoveryId]
    }
    
    override suspend fun updateStreakRecovery(recovery: StreakRecovery) {
        streakRecoveries[recovery.id] = recovery
    }
    
    override suspend fun getActiveRecoveries(userId: String): List<StreakRecovery> {
        return activeRecoveries[userId] ?: emptyList()
    }
    
    override suspend fun getAllRecoveries(userId: String): List<StreakRecovery> {
        return allRecoveries[userId] ?: emptyList()
    }
    
    override suspend fun createStreakReminder(reminder: StreakReminder) {
        streakReminders[reminder.userId] = (streakReminders[reminder.userId] ?: emptyList()) + reminder
    }
    
    override suspend fun getActiveReminders(userId: String): List<StreakReminder> {
        return streakReminders[userId] ?: emptyList()
    }
    
    override suspend fun markReminderAsRead(reminderId: String) {
        // Implementation for testing
    }
}