package com.north.mobile.data.gamification

import com.north.mobile.data.repository.GamificationRepository
import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

class StreakTrackingServiceTest {
    
    private lateinit var mockRepository: MockGamificationRepository
    private lateinit var mockCelebrationManager: MockCelebrationManager
    private lateinit var streakTrackingService: StreakTrackingService
    
    @BeforeTest
    fun setup() {
        mockRepository = MockGamificationRepository()
        mockCelebrationManager = MockCelebrationManager()
        streakTrackingService = StreakTrackingServiceImpl(mockRepository, mockCelebrationManager)
    }
    
    @Test
    fun `updateStreakWithRiskAssessment should create new streak for first time`() = runTest {
        // Given
        val userId = "user123"
        val streakType = StreakType.DAILY_CHECK_IN
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        // When
        val result = streakTrackingService.updateStreakWithRiskAssessment(userId, streakType, today)
        
        // Then
        assertTrue(result.isSuccess)
        val streakResult = result.getOrThrow()
        assertEquals(1, streakResult.streak.currentCount)
        assertEquals(1, streakResult.streak.bestCount)
        assertEquals(today, streakResult.streak.lastActivityDate)
        assertTrue(streakResult.wasExtended)
        assertFalse(streakResult.wasBroken)
        assertEquals(StreakRiskLevel.SAFE, streakResult.newRiskLevel)
    }
    
    @Test
    fun `updateStreakWithRiskAssessment should extend existing streak for consecutive day`() = runTest {
        // Given
        val userId = "user123"
        val streakType = StreakType.DAILY_CHECK_IN
        val yesterday = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(1, DateTimeUnit.DAY)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        val existingStreak = Streak(
            id = "streak1",
            type = streakType,
            currentCount = 5,
            bestCount = 10,
            lastActivityDate = yesterday,
            isActive = true,
            riskLevel = StreakRiskLevel.SAFE,
            recoveryAttempts = 0
        )
        mockRepository.streaks[Pair(userId, streakType)] = existingStreak
        
        // When
        val result = streakTrackingService.updateStreakWithRiskAssessment(userId, streakType, today)
        
        // Then
        assertTrue(result.isSuccess)
        val streakResult = result.getOrThrow()
        assertEquals(6, streakResult.streak.currentCount)
        assertEquals(10, streakResult.streak.bestCount)
        assertEquals(today, streakResult.streak.lastActivityDate)
        assertTrue(streakResult.wasExtended)
        assertFalse(streakResult.wasBroken)
    }
    
    @Test
    fun `updateStreakWithRiskAssessment should break streak for non-consecutive day`() = runTest {
        // Given
        val userId = "user123"
        val streakType = StreakType.DAILY_CHECK_IN
        val threeDaysAgo = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(3, DateTimeUnit.DAY)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        val existingStreak = Streak(
            id = "streak1",
            type = streakType,
            currentCount = 5,
            bestCount = 10,
            lastActivityDate = threeDaysAgo,
            isActive = true,
            riskLevel = StreakRiskLevel.SAFE,
            recoveryAttempts = 0
        )
        mockRepository.streaks[Pair(userId, streakType)] = existingStreak
        
        // When
        val result = streakTrackingService.updateStreakWithRiskAssessment(userId, streakType, today)
        
        // Then
        assertTrue(result.isSuccess)
        val streakResult = result.getOrThrow()
        assertEquals(1, streakResult.streak.currentCount) // Reset to 1
        assertEquals(10, streakResult.streak.bestCount) // Best count preserved
        assertEquals(today, streakResult.streak.lastActivityDate)
        assertFalse(streakResult.wasExtended)
        assertTrue(streakResult.wasBroken)
        assertEquals(1, streakResult.streak.recoveryAttempts)
    }
    
    @Test
    fun `updateStreakWithRiskAssessment should update best count when exceeded`() = runTest {
        // Given
        val userId = "user123"
        val streakType = StreakType.DAILY_CHECK_IN
        val yesterday = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(1, DateTimeUnit.DAY)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        val existingStreak = Streak(
            id = "streak1",
            type = streakType,
            currentCount = 10,
            bestCount = 10,
            lastActivityDate = yesterday,
            isActive = true,
            riskLevel = StreakRiskLevel.SAFE,
            recoveryAttempts = 0
        )
        mockRepository.streaks[Pair(userId, streakType)] = existingStreak
        
        // When
        val result = streakTrackingService.updateStreakWithRiskAssessment(userId, streakType, today)
        
        // Then
        assertTrue(result.isSuccess)
        val streakResult = result.getOrThrow()
        assertEquals(11, streakResult.streak.currentCount)
        assertEquals(11, streakResult.streak.bestCount) // Updated best count
    }
    
    @Test
    fun `analyzeStreakRisks should identify streaks at risk`() = runTest {
        // Given
        val userId = "user123"
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val twoDaysAgo = today.minus(2, DateTimeUnit.DAY)
        val threeDaysAgo = today.minus(3, DateTimeUnit.DAY)
        
        val safeStreak = Streak(
            id = "streak1",
            type = StreakType.DAILY_CHECK_IN,
            currentCount = 5,
            bestCount = 10,
            lastActivityDate = today,
            isActive = true,
            riskLevel = StreakRiskLevel.SAFE
        )
        
        val riskStreak = Streak(
            id = "streak2",
            type = StreakType.SAVINGS_CONTRIBUTION,
            currentCount = 7,
            bestCount = 15,
            lastActivityDate = twoDaysAgo,
            isActive = true,
            riskLevel = StreakRiskLevel.MEDIUM_RISK
        )
        
        val highRiskStreak = Streak(
            id = "streak3",
            type = StreakType.UNDER_BUDGET,
            currentCount = 12,
            bestCount = 20,
            lastActivityDate = threeDaysAgo,
            isActive = true,
            riskLevel = StreakRiskLevel.HIGH_RISK
        )
        
        mockRepository.activeStreaks[userId] = listOf(safeStreak, riskStreak, highRiskStreak)
        
        // When
        val result = streakTrackingService.analyzeStreakRisks(userId)
        
        // Then
        assertTrue(result.isSuccess)
        val riskAnalyses = result.getOrThrow()
        assertEquals(3, riskAnalyses.size)
        
        // Should be sorted by urgency score (descending)
        assertTrue(riskAnalyses[0].urgencyScore >= riskAnalyses[1].urgencyScore)
        assertTrue(riskAnalyses[1].urgencyScore >= riskAnalyses[2].urgencyScore)
        
        // High risk streak should have highest urgency
        val highRiskAnalysis = riskAnalyses.find { it.streak.id == "streak3" }
        assertNotNull(highRiskAnalysis)
        assertEquals(StreakRiskLevel.HIGH_RISK, highRiskAnalysis.riskLevel)
        assertTrue(highRiskAnalysis.urgencyScore > 5)
    }
    
    @Test
    fun `generatePersonalizedMicroWins should create relevant opportunities`() = runTest {
        // Given
        val userId = "user123"
        val profile = GamificationProfile(
            level = 5,
            totalPoints = 500,
            currentStreaks = emptyList(),
            achievements = emptyList(),
            lastActivity = Clock.System.now()
        )
        
        val activeStreak = Streak(
            id = "streak1",
            type = StreakType.DAILY_CHECK_IN,
            currentCount = 3,
            bestCount = 10,
            lastActivityDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(1, DateTimeUnit.DAY),
            isActive = true,
            riskLevel = StreakRiskLevel.LOW_RISK
        )
        
        mockRepository.gamificationProfiles[userId] = profile
        mockRepository.activeStreaks[userId] = listOf(activeStreak)
        mockRepository.pointsHistory[userId] = listOf(
            PointsHistoryEntry(
                id = "entry1",
                points = 10,
                action = UserAction.CHECK_BALANCE,
                description = "Balance check",
                earnedAt = Clock.System.now().minus(1, DateTimeUnit.DAY)
            )
        )
        
        // When
        val result = streakTrackingService.generatePersonalizedMicroWins(userId, 5)
        
        // Then
        assertTrue(result.isSuccess)
        val microWins = result.getOrThrow()
        assertTrue(microWins.isNotEmpty())
        
        // Should include streak maintenance micro-win
        val streakMaintenance = microWins.find { it.contextData.containsKey("streakId") }
        assertNotNull(streakMaintenance)
        assertTrue(streakMaintenance.isPersonalized)
        assertEquals(activeStreak.id, streakMaintenance.contextData["streakId"])
    }
    
    @Test
    fun `detectAndAwardMicroWins should detect action-specific micro-wins`() = runTest {
        // Given
        val userId = "user123"
        val action = UserAction.CATEGORIZE_TRANSACTION
        val contextData = mapOf("transactionCount" to "5")
        
        // When
        val result = streakTrackingService.detectAndAwardMicroWins(userId, action, contextData)
        
        // Then
        assertTrue(result.isSuccess)
        val microWinResults = result.getOrThrow()
        assertTrue(microWinResults.isNotEmpty())
        
        // Should include bulk categorization micro-win for 5+ transactions
        val bulkMicroWin = microWinResults.find { it.microWin.title.contains("Bulk") }
        assertNotNull(bulkMicroWin)
        assertEquals(MicroWinDifficulty.MEDIUM, bulkMicroWin.microWin.difficulty)
        assertTrue(bulkMicroWin.pointsAwarded > 5)
    }
    
    @Test
    fun `initiateStreakRecovery should create recovery process`() = runTest {
        // Given
        val userId = "user123"
        val brokenStreakId = "streak1"
        val brokenStreak = Streak(
            id = brokenStreakId,
            type = StreakType.DAILY_CHECK_IN,
            currentCount = 1,
            bestCount = 15,
            lastActivityDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            isActive = true,
            riskLevel = StreakRiskLevel.BROKEN
        )
        
        mockRepository.streaksById[brokenStreakId] = brokenStreak
        
        // When
        val result = streakTrackingService.initiateStreakRecovery(userId, brokenStreakId)
        
        // Then
        assertTrue(result.isSuccess)
        val recovery = result.getOrThrow()
        assertEquals(userId, recovery.userId)
        assertEquals(brokenStreakId, recovery.originalStreakId)
        assertEquals(StreakType.DAILY_CHECK_IN, recovery.streakType)
        assertEquals(15, recovery.originalCount)
        assertFalse(recovery.isSuccessful)
        assertTrue(recovery.recoveryActions.isEmpty())
    }
    
    @Test
    fun `processRecoveryAction should track recovery progress`() = runTest {
        // Given
        val userId = "user123"
        val recoveryId = "recovery1"
        val recovery = StreakRecovery(
            id = recoveryId,
            userId = userId,
            originalStreakId = "streak1",
            streakType = StreakType.DAILY_CHECK_IN,
            brokenAt = Clock.System.now().minus(1, DateTimeUnit.DAY),
            recoveryStarted = Clock.System.now().minus(1, DateTimeUnit.DAY),
            originalCount = 10,
            recoveryActions = emptyList(),
            isSuccessful = false
        )
        
        mockRepository.streakRecoveries[recoveryId] = recovery
        
        // When
        val result = streakTrackingService.processRecoveryAction(userId, recoveryId, UserAction.CHECK_BALANCE)
        
        // Then
        assertTrue(result.isSuccess)
        val recoveryResult = result.getOrThrow()
        assertEquals(1, recoveryResult.recovery.recoveryActions.size)
        assertFalse(recoveryResult.isRecoveryComplete) // Need 3 actions
        assertNull(recoveryResult.newStreakStarted)
        
        val action = recoveryResult.actionProcessed
        assertEquals(UserAction.CHECK_BALANCE, action.actionType)
        assertTrue(action.pointsAwarded > 0)
    }
    
    @Test
    fun `processRecoveryAction should complete recovery after 3 actions`() = runTest {
        // Given
        val userId = "user123"
        val recoveryId = "recovery1"
        val existingActions = listOf(
            RecoveryAction(
                id = "action1",
                actionType = UserAction.CHECK_BALANCE,
                completedAt = Clock.System.now().minus(2, DateTimeUnit.DAY),
                pointsAwarded = 10,
                description = "Recovery action 1"
            ),
            RecoveryAction(
                id = "action2",
                actionType = UserAction.CATEGORIZE_TRANSACTION,
                completedAt = Clock.System.now().minus(1, DateTimeUnit.DAY),
                pointsAwarded = 10,
                description = "Recovery action 2"
            )
        )
        
        val recovery = StreakRecovery(
            id = recoveryId,
            userId = userId,
            originalStreakId = "streak1",
            streakType = StreakType.DAILY_CHECK_IN,
            brokenAt = Clock.System.now().minus(3, DateTimeUnit.DAY),
            recoveryStarted = Clock.System.now().minus(3, DateTimeUnit.DAY),
            originalCount = 10,
            recoveryActions = existingActions,
            isSuccessful = false
        )
        
        mockRepository.streakRecoveries[recoveryId] = recovery
        
        // When - Third recovery action
        val result = streakTrackingService.processRecoveryAction(userId, recoveryId, UserAction.REVIEW_INSIGHTS)
        
        // Then
        assertTrue(result.isSuccess)
        val recoveryResult = result.getOrThrow()
        assertEquals(3, recoveryResult.recovery.recoveryActions.size)
        assertTrue(recoveryResult.isRecoveryComplete)
        assertTrue(recoveryResult.recovery.isSuccessful)
        assertNotNull(recoveryResult.newStreakStarted)
        
        val newStreak = recoveryResult.newStreakStarted!!
        assertEquals(1, newStreak.currentCount)
        assertEquals(10, newStreak.bestCount) // Preserved from original
        assertEquals(StreakType.DAILY_CHECK_IN, newStreak.type)
        assertEquals(3, newStreak.recoveryAttempts) // Number of recovery actions
    }
    
    @Test
    fun `getStreakStatistics should provide comprehensive statistics`() = runTest {
        // Given
        val userId = "user123"
        val activeStreaks = listOf(
            Streak(
                id = "streak1",
                type = StreakType.DAILY_CHECK_IN,
                currentCount = 5,
                bestCount = 10,
                lastActivityDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
                isActive = true,
                riskLevel = StreakRiskLevel.SAFE
            ),
            Streak(
                id = "streak2",
                type = StreakType.SAVINGS_CONTRIBUTION,
                currentCount = 3,
                bestCount = 15,
                lastActivityDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(1, DateTimeUnit.DAY),
                isActive = true,
                riskLevel = StreakRiskLevel.LOW_RISK
            )
        )
        
        val allStreaks = activeStreaks + listOf(
            Streak(
                id = "streak3",
                type = StreakType.UNDER_BUDGET,
                currentCount = 0,
                bestCount = 20,
                lastActivityDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(5, DateTimeUnit.DAY),
                isActive = false,
                riskLevel = StreakRiskLevel.BROKEN
            )
        )
        
        val recoveries = listOf(
            StreakRecovery(
                id = "recovery1",
                userId = userId,
                originalStreakId = "streak3",
                streakType = StreakType.UNDER_BUDGET,
                brokenAt = Clock.System.now().minus(5, DateTimeUnit.DAY),
                recoveryStarted = Clock.System.now().minus(4, DateTimeUnit.DAY),
                originalCount = 20,
                recoveryActions = emptyList(),
                isSuccessful = true
            )
        )
        
        mockRepository.activeStreaks[userId] = activeStreaks
        mockRepository.allUserStreaks[userId] = allStreaks
        mockRepository.allRecoveries[userId] = recoveries
        
        // When
        val result = streakTrackingService.getStreakStatistics(userId)
        
        // Then
        assertTrue(result.isSuccess)
        val stats = result.getOrThrow()
        assertEquals(2, stats.totalActiveStreaks)
        assertEquals("streak1", stats.longestCurrentStreak?.id)
        assertEquals("streak3", stats.longestEverStreak?.id)
        assertEquals(45, stats.totalStreakDays) // 10 + 15 + 20
        assertEquals(15.0, stats.averageStreakLength) // 45 / 3
        assertEquals(1.0, stats.recoverySuccessRate) // 1 successful out of 1 total
        
        // Risk distribution
        assertEquals(1, stats.riskDistribution[StreakRiskLevel.SAFE])
        assertEquals(1, stats.riskDistribution[StreakRiskLevel.LOW_RISK])
        
        // Streaks by type
        assertEquals(1, stats.streaksByType[StreakType.DAILY_CHECK_IN]?.size)
        assertEquals(1, stats.streaksByType[StreakType.SAVINGS_CONTRIBUTION]?.size)
    }
    
    @Test
    fun `celebrateStreakMilestone should create appropriate celebration`() = runTest {
        // Given
        val userId = "user123"
        val streak = Streak(
            id = "streak1",
            type = StreakType.DAILY_CHECK_IN,
            currentCount = 7, // Milestone
            bestCount = 10,
            lastActivityDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            isActive = true,
            riskLevel = StreakRiskLevel.SAFE
        )
        
        // When
        val result = streakTrackingService.celebrateStreakMilestone(userId, streak)
        
        // Then
        assertTrue(result.isSuccess)
        val celebration = result.getOrThrow()
        assertEquals(CelebrationType.STREAK_MILESTONE, celebration.type)
        assertTrue(celebration.title.contains("Streak"))
        assertTrue(celebration.message.contains("7"))
        assertTrue(celebration.intensity == CelebrationIntensity.MEDIUM)
    }
}

// Mock implementations for testing

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

class MockCelebrationManager : CelebrationManager {
    override suspend fun celebratePointsAwarded(pointsAwarded: Int, action: UserAction, totalPoints: Int): CelebrationEvent {
        return CelebrationEvent(
            type = CelebrationType.POINTS_AWARDED,
            title = "+$pointsAwarded points!",
            message = "Great job!",
            intensity = CelebrationIntensity.LOW,
            duration = 1000L,
            animations = listOf(AnimationType.GENTLE_BOUNCE),
            sounds = listOf(SoundType.GENTLE_CHIME),
            hapticFeedback = listOf(HapticType.GENTLE_TAP),
            timestamp = Clock.System.now()
        )
    }
    
    override suspend fun celebrateLevelUp(levelUpResult: LevelUpResult): CelebrationEvent {
        return CelebrationEvent(
            type = CelebrationType.LEVEL_UP,
            title = "Level Up!",
            message = levelUpResult.celebrationMessage,
            intensity = CelebrationIntensity.HIGH,
            duration = 3000L,
            animations = listOf(AnimationType.CONFETTI),
            sounds = listOf(SoundType.LEVEL_UP_FANFARE),
            hapticFeedback = listOf(HapticType.CELEBRATION_PATTERN),
            timestamp = Clock.System.now()
        )
    }
    
    override suspend fun celebrateAchievement(achievement: Achievement): CelebrationEvent {
        return CelebrationEvent(
            type = CelebrationType.ACHIEVEMENT_UNLOCKED,
            title = "Achievement Unlocked!",
            message = achievement.title,
            intensity = CelebrationIntensity.HIGH,
            duration = 2500L,
            animations = listOf(AnimationType.BADGE_REVEAL),
            sounds = listOf(SoundType.ACHIEVEMENT_CHIME),
            hapticFeedback = listOf(HapticType.ACHIEVEMENT_PATTERN),
            timestamp = Clock.System.now()
        )
    }
    
    override suspend fun celebrateStreak(streakType: StreakType, streakCount: Int, isNewRecord: Boolean): CelebrationEvent {
        val intensity = when {
            streakCount >= 30 -> CelebrationIntensity.HIGH
            streakCount >= 7 -> CelebrationIntensity.MEDIUM
            else -> CelebrationIntensity.LOW
        }
        
        return CelebrationEvent(
            type = CelebrationType.STREAK_MILESTONE,
            title = if (isNewRecord) "New Record!" else "Streak Continues!",
            message = "$streakCount day${if (streakCount != 1) "s" else ""} streak",
            intensity = intensity,
            duration = 2000L,
            animations = listOf(AnimationType.FLAME_FLICKER),
            sounds = listOf(SoundType.STREAK_CHIME),
            hapticFeedback = listOf(HapticType.SUCCESS_PATTERN),
            timestamp = Clock.System.now()
        )
    }
    
    override suspend fun celebrateMicroWin(microWinTitle: String, pointsAwarded: Int): CelebrationEvent {
        return CelebrationEvent(
            type = CelebrationType.MICRO_WIN,
            title = "Micro Win!",
            message = microWinTitle,
            intensity = CelebrationIntensity.LOW,
            duration = 1500L,
            animations = listOf(AnimationType.STAR_TWINKLE),
            sounds = listOf(SoundType.MICRO_WIN_CHIME),
            hapticFeedback = listOf(HapticType.GENTLE_TAP),
            timestamp = Clock.System.now()
        )
    }
}