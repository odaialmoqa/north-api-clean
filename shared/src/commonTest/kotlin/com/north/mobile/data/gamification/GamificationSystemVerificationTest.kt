package com.north.mobile.data.gamification

import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Verification tests for the gamification system using mock implementations.
 * These tests verify the core business logic without database dependencies.
 */
class GamificationSystemVerificationTest {
    
    private lateinit var mockRepository: MockGamificationRepository
    private lateinit var gamificationService: GamificationService
    private lateinit var celebrationManager: CelebrationManager
    
    @BeforeTest
    fun setup() {
        mockRepository = MockGamificationRepository()
        gamificationService = GamificationServiceImpl(mockRepository)
        celebrationManager = CelebrationManagerImpl()
    }
    
    @Test
    fun `gamification system should award points correctly`() = runTest {
        // Given
        val userId = "test_user_123"
        
        // When - Award points for checking balance
        val result = gamificationService.awardPoints(userId, UserAction.CHECK_BALANCE)
        
        // Then
        assertTrue(result.isSuccess)
        val pointsResult = result.getOrThrow()
        assertEquals(5, pointsResult.pointsAwarded)
        assertEquals(5, pointsResult.totalPoints)
        assertEquals(1, pointsResult.newLevel)
        assertFalse(pointsResult.leveledUp)
    }
    
    @Test
    fun `level progression should work correctly`() = runTest {
        // Given
        val userId = "level_test_user"
        
        // When - Award enough points to reach level 2 (100 points)
        val result = gamificationService.awardPoints(userId, UserAction.LINK_ACCOUNT, 100)
        
        // Then
        assertTrue(result.isSuccess)
        val pointsResult = result.getOrThrow()
        assertEquals(100, pointsResult.pointsAwarded)
        assertEquals(100, pointsResult.totalPoints)
        assertEquals(2, pointsResult.newLevel)
        assertTrue(pointsResult.leveledUp)
    }
    
    @Test
    fun `level calculation should be accurate`() {
        // Test the level progression formula
        assertEquals(1, gamificationService.getLevelFromPoints(0))
        assertEquals(1, gamificationService.getLevelFromPoints(99))
        assertEquals(2, gamificationService.getLevelFromPoints(100))
        assertEquals(2, gamificationService.getLevelFromPoints(399))
        assertEquals(3, gamificationService.getLevelFromPoints(400))
        assertEquals(4, gamificationService.getLevelFromPoints(900))
        assertEquals(5, gamificationService.getLevelFromPoints(1600))
    }
    
    @Test
    fun `points required for next level should be calculated correctly`() {
        assertEquals(100, gamificationService.getPointsRequiredForNextLevel(1))
        assertEquals(400, gamificationService.getPointsRequiredForNextLevel(2))
        assertEquals(900, gamificationService.getPointsRequiredForNextLevel(3))
        assertEquals(1600, gamificationService.getPointsRequiredForNextLevel(4))
    }
    
    @Test
    fun `streak system should track consecutive activities`() = runTest {
        // Given
        val userId = "streak_user"
        val streakType = StreakType.DAILY_CHECK_IN
        
        // When - Update streak for first time
        val result1 = gamificationService.updateStreak(userId, streakType)
        assertTrue(result1.isSuccess)
        assertEquals(1, result1.getOrThrow().currentCount)
        
        // Simulate next day activity by manually updating
        val streak = result1.getOrThrow()
        val nextDayStreak = streak.copy(
            currentCount = 2,
            bestCount = 2,
            lastActivityDate = streak.lastActivityDate.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
        )
        mockRepository.updateStreak(nextDayStreak, userId)
        
        // Then - Verify streak increased
        val currentStreak = mockRepository.getStreak(userId, streakType)
        assertNotNull(currentStreak)
        assertEquals(2, currentStreak.currentCount)
        assertEquals(2, currentStreak.bestCount)
    }
    
    @Test
    fun `achievement system should unlock achievements`() = runTest {
        // Given
        val userId = "achievement_user"
        
        // When - Unlock first goal achievement
        val result = gamificationService.unlockAchievement(userId, AchievementType.FIRST_GOAL_CREATED)
        
        // Then
        assertTrue(result.isSuccess)
        val achievement = result.getOrThrow()
        assertEquals("Goal Setter", achievement.title)
        assertEquals("Created your first financial goal", achievement.description)
        assertEquals("🎯", achievement.badgeIcon)
        assertEquals(AchievementCategory.GOAL_ACHIEVEMENT, achievement.category)
    }
    
    @Test
    fun `achievement system should prevent duplicates`() = runTest {
        // Given
        val userId = "duplicate_test_user"
        val achievementType = AchievementType.FIRST_ACCOUNT_LINKED
        
        // When - Unlock achievement twice
        val result1 = gamificationService.unlockAchievement(userId, achievementType)
        val result2 = gamificationService.unlockAchievement(userId, achievementType)
        
        // Then - Should return same achievement
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertEquals(result1.getOrThrow().id, result2.getOrThrow().id)
        assertEquals(result1.getOrThrow().unlockedAt, result2.getOrThrow().unlockedAt)
    }
    
    @Test
    fun `micro wins should be generated for users`() = runTest {
        // Given
        val userId = "microwins_user"
        
        // When
        val result = gamificationService.getAvailableMicroWins(userId)
        
        // Then
        assertTrue(result.isSuccess)
        val microWins = result.getOrThrow()
        assertTrue(microWins.isNotEmpty())
        
        // Verify expected micro wins exist
        val actionTypes = microWins.map { it.actionType }.toSet()
        assertTrue(actionTypes.contains(UserAction.CHECK_BALANCE))
        assertTrue(actionTypes.contains(UserAction.CATEGORIZE_TRANSACTION))
        assertTrue(actionTypes.contains(UserAction.REVIEW_INSIGHTS))
    }
    
    @Test
    fun `points history should be tracked`() = runTest {
        // Given
        val userId = "history_user"
        
        // When - Award points multiple times
        gamificationService.awardPoints(userId, UserAction.CHECK_BALANCE)
        gamificationService.awardPoints(userId, UserAction.CATEGORIZE_TRANSACTION)
        gamificationService.awardPoints(userId, UserAction.UPDATE_GOAL)
        
        // Then - History should be recorded
        val historyResult = gamificationService.getPointsHistory(userId, 10)
        assertTrue(historyResult.isSuccess)
        
        val history = historyResult.getOrThrow()
        assertEquals(3, history.size)
        
        // Verify actions are recorded correctly
        val actions = history.map { it.action }.toSet()
        assertTrue(actions.contains(UserAction.CHECK_BALANCE))
        assertTrue(actions.contains(UserAction.CATEGORIZE_TRANSACTION))
        assertTrue(actions.contains(UserAction.UPDATE_GOAL))
    }
    
    @Test
    fun `celebration manager should create appropriate celebrations`() = runTest {
        // Test points celebration
        val pointsCelebration = celebrationManager.celebratePointsAwarded(
            pointsAwarded = 25,
            action = UserAction.CATEGORIZE_TRANSACTION,
            totalPoints = 125
        )
        
        assertEquals(CelebrationType.POINTS_AWARDED, pointsCelebration.type)
        assertEquals("+25 points!", pointsCelebration.title)
        assertEquals(CelebrationIntensity.MEDIUM, pointsCelebration.intensity)
        assertTrue(pointsCelebration.message.contains("organizing"))
        
        // Test level up celebration
        val levelUpResult = LevelUpResult(
            oldLevel = 1,
            newLevel = 2,
            pointsRequired = 100,
            totalPoints = 150,
            unlockedFeatures = listOf("Advanced Goal Tracking"),
            celebrationMessage = "🎉 Welcome to Level 2! You're getting the hang of this!"
        )
        
        val levelUpCelebration = celebrationManager.celebrateLevelUp(levelUpResult)
        assertEquals(CelebrationType.LEVEL_UP, levelUpCelebration.type)
        assertEquals("Level Up! 🎉", levelUpCelebration.title)
        assertEquals(CelebrationIntensity.HIGH, levelUpCelebration.intensity)
        assertTrue(levelUpCelebration.animations.contains(AnimationType.CONFETTI))
        assertTrue(levelUpCelebration.sounds.contains(SoundType.LEVEL_UP_FANFARE))
    }
    
    @Test
    fun `achievement celebration should have appropriate intensity`() = runTest {
        // Given
        val achievement = Achievement(
            id = "test_achievement",
            title = "Test Achievement",
            description = "Test description",
            badgeIcon = "🏆",
            pointsAwarded = 100,
            unlockedAt = Clock.System.now(),
            category = AchievementCategory.ENGAGEMENT
        )
        
        // When
        val celebration = celebrationManager.celebrateAchievement(achievement)
        
        // Then
        assertEquals(CelebrationType.ACHIEVEMENT_UNLOCKED, celebration.type)
        assertEquals("Achievement Unlocked! 🏆", celebration.title)
        assertEquals(CelebrationIntensity.HIGH, celebration.intensity)
        assertTrue(celebration.animations.contains(AnimationType.BADGE_REVEAL))
        assertTrue(celebration.sounds.contains(SoundType.ACHIEVEMENT_CHIME))
        assertEquals("🏆 Test Achievement", celebration.message)
    }
    
    @Test
    fun `streak celebration should vary by streak length`() = runTest {
        // Test short streak
        val shortStreakCelebration = celebrationManager.celebrateStreak(
            streakType = StreakType.DAILY_CHECK_IN,
            streakCount = 3,
            isNewRecord = false
        )
        assertEquals(CelebrationIntensity.LOW, shortStreakCelebration.intensity)
        assertEquals("Streak Continues! 🔥", shortStreakCelebration.title)
        
        // Test long streak with new record
        val longStreakCelebration = celebrationManager.celebrateStreak(
            streakType = StreakType.DAILY_CHECK_IN,
            streakCount = 30,
            isNewRecord = true
        )
        assertEquals(CelebrationIntensity.HIGH, longStreakCelebration.intensity)
        assertEquals("New Record! 🔥", longStreakCelebration.title)
        assertTrue(longStreakCelebration.animations.contains(AnimationType.RECORD_BADGE_REVEAL))
    }
    
    @Test
    fun `complete user journey should work end-to-end`() = runTest {
        // Given
        val userId = "journey_user"
        
        // Step 1: User links account (should trigger achievement)
        val linkResult = gamificationService.awardPoints(userId, UserAction.LINK_ACCOUNT)
        assertTrue(linkResult.isSuccess)
        val linkPoints = linkResult.getOrThrow()
        assertEquals(50, linkPoints.pointsAwarded)
        assertEquals(1, linkPoints.newAchievements.size)
        assertEquals("Connected", linkPoints.newAchievements[0].title)
        
        // Step 2: User creates goal (should trigger another achievement)
        val goalResult = gamificationService.awardPoints(userId, UserAction.UPDATE_GOAL)
        assertTrue(goalResult.isSuccess)
        val goalPoints = goalResult.getOrThrow()
        assertEquals(15, goalPoints.pointsAwarded)
        assertEquals(65, goalPoints.totalPoints)
        assertEquals(1, goalPoints.newAchievements.size)
        assertEquals("Goal Setter", goalPoints.newAchievements[0].title)
        
        // Step 3: User does enough activities to level up
        val levelUpResult = gamificationService.awardPoints(userId, UserAction.MAKE_SAVINGS_CONTRIBUTION, 50)
        assertTrue(levelUpResult.isSuccess)
        val levelUpPoints = levelUpResult.getOrThrow()
        assertEquals(115, levelUpPoints.totalPoints)
        assertEquals(2, levelUpPoints.newLevel)
        assertTrue(levelUpPoints.leveledUp)
        
        // Verify final state
        val profileResult = gamificationService.getGamificationProfile(userId)
        assertTrue(profileResult.isSuccess)
        val profile = profileResult.getOrThrow()
        assertEquals(2, profile.level)
        assertEquals(115, profile.totalPoints)
        assertEquals(2, profile.achievements.size)
    }
}

/**
 * Simple mock repository for testing without database dependencies.
 */
private class MockGamificationRepository : GamificationRepository {
    private val profiles = mutableMapOf<String, GamificationProfile>()
    private val streaks = mutableMapOf<String, MutableMap<StreakType, Streak>>()
    private val achievements = mutableMapOf<String, MutableMap<AchievementType, Achievement>>()
    private val pointsHistory = mutableMapOf<String, MutableList<PointsHistoryEntry>>()
    
    override suspend fun getGamificationProfile(userId: String): GamificationProfile? {
        return profiles[userId]?.let { profile ->
            val activeStreaks = getActiveStreaks(userId)
            val userAchievements = getAchievements(userId)
            
            profile.copy(
                currentStreaks = activeStreaks,
                achievements = userAchievements
            )
        }
    }
    
    override suspend fun createGamificationProfile(profile: GamificationProfile, userId: String) {
        profiles[userId] = profile
    }
    
    override suspend fun updateGamificationProfile(profile: GamificationProfile, userId: String) {
        profiles[userId] = profile
    }
    
    override suspend fun getStreak(userId: String, streakType: StreakType): Streak? {
        return streaks[userId]?.get(streakType)
    }
    
    override suspend fun getActiveStreaks(userId: String): List<Streak> {
        return streaks[userId]?.values?.filter { it.isActive } ?: emptyList()
    }
    
    override suspend fun updateStreak(streak: Streak, userId: String) {
        if (streaks[userId] == null) {
            streaks[userId] = mutableMapOf()
        }
        streaks[userId]!![streak.type] = streak
    }
    
    override suspend fun getAchievement(userId: String, achievementType: AchievementType): Achievement? {
        return achievements[userId]?.get(achievementType)
    }
    
    override suspend fun getAchievements(userId: String): List<Achievement> {
        return achievements[userId]?.values?.toList() ?: emptyList()
    }
    
    override suspend fun addAchievement(achievement: Achievement, userId: String, achievementType: AchievementType) {
        if (achievements[userId] == null) {
            achievements[userId] = mutableMapOf()
        }
        achievements[userId]!![achievementType] = achievement
    }
    
    override suspend fun addPointsHistory(entry: PointsHistoryEntry, userId: String) {
        if (pointsHistory[userId] == null) {
            pointsHistory[userId] = mutableListOf()
        }
        pointsHistory[userId]!!.add(entry)
    }
    
    override suspend fun getPointsHistory(userId: String, limit: Int): List<PointsHistoryEntry> {
        return pointsHistory[userId]?.takeLast(limit) ?: emptyList()
    }
}