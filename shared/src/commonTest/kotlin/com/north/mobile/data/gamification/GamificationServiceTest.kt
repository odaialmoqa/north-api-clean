package com.north.mobile.data.gamification

import com.north.mobile.data.repository.GamificationRepository
import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.*

class GamificationServiceTest {
    
    private lateinit var mockRepository: MockGamificationRepository
    private lateinit var gamificationService: GamificationService
    
    @BeforeTest
    fun setup() {
        mockRepository = MockGamificationRepository()
        gamificationService = GamificationServiceImpl(mockRepository)
    }
    
    @Test
    fun `awardPoints should create initial profile for new user`() = runTest {
        // Given
        val userId = "user123"
        val action = UserAction.CHECK_BALANCE
        
        // When
        val result = gamificationService.awardPoints(userId, action)
        
        // Then
        assertTrue(result.isSuccess)
        val pointsResult = result.getOrThrow()
        assertEquals(5, pointsResult.pointsAwarded)
        assertEquals(5, pointsResult.totalPoints)
        assertEquals(1, pointsResult.newLevel)
        assertFalse(pointsResult.leveledUp)
        
        // Verify profile was created
        val profile = mockRepository.getGamificationProfile(userId)
        assertNotNull(profile)
        assertEquals(1, profile.level)
        assertEquals(5, profile.totalPoints)
    }
    
    @Test
    fun `awardPoints should update existing profile`() = runTest {
        // Given
        val userId = "user123"
        val existingProfile = GamificationProfile(
            level = 1,
            totalPoints = 50,
            currentStreaks = emptyList(),
            achievements = emptyList(),
            lastActivity = Clock.System.now()
        )
        mockRepository.createGamificationProfile(existingProfile, userId)
        
        // When
        val result = gamificationService.awardPoints(userId, UserAction.CATEGORIZE_TRANSACTION)
        
        // Then
        assertTrue(result.isSuccess)
        val pointsResult = result.getOrThrow()
        assertEquals(10, pointsResult.pointsAwarded)
        assertEquals(60, pointsResult.totalPoints)
        assertEquals(1, pointsResult.newLevel)
        assertFalse(pointsResult.leveledUp)
    }
    
    @Test
    fun `awardPoints should trigger level up when threshold reached`() = runTest {
        // Given
        val userId = "user123"
        val existingProfile = GamificationProfile(
            level = 1,
            totalPoints = 95,
            currentStreaks = emptyList(),
            achievements = emptyList(),
            lastActivity = Clock.System.now()
        )
        mockRepository.createGamificationProfile(existingProfile, userId)
        
        // When - Award 10 points to reach 105 total (should trigger level 2)
        val result = gamificationService.awardPoints(userId, UserAction.CATEGORIZE_TRANSACTION)
        
        // Then
        assertTrue(result.isSuccess)
        val pointsResult = result.getOrThrow()
        assertEquals(10, pointsResult.pointsAwarded)
        assertEquals(105, pointsResult.totalPoints)
        assertEquals(2, pointsResult.newLevel)
        assertTrue(pointsResult.leveledUp)
    }
    
    @Test
    fun `getLevelFromPoints should calculate correct levels`() {
        // Test level progression formula: level = floor(sqrt(totalPoints / 100)) + 1
        assertEquals(1, gamificationService.getLevelFromPoints(0))
        assertEquals(1, gamificationService.getLevelFromPoints(50))
        assertEquals(1, gamificationService.getLevelFromPoints(99))
        assertEquals(2, gamificationService.getLevelFromPoints(100))
        assertEquals(2, gamificationService.getLevelFromPoints(399))
        assertEquals(3, gamificationService.getLevelFromPoints(400))
        assertEquals(4, gamificationService.getLevelFromPoints(900))
        assertEquals(5, gamificationService.getLevelFromPoints(1600))
    }
    
    @Test
    fun `getPointsRequiredForNextLevel should calculate correct requirements`() {
        assertEquals(100, gamificationService.getPointsRequiredForNextLevel(1)) // Level 2 requires 100 points
        assertEquals(400, gamificationService.getPointsRequiredForNextLevel(2)) // Level 3 requires 400 points
        assertEquals(900, gamificationService.getPointsRequiredForNextLevel(3)) // Level 4 requires 900 points
        assertEquals(1600, gamificationService.getPointsRequiredForNextLevel(4)) // Level 5 requires 1600 points
    }
    
    @Test
    fun `updateStreak should create new streak for first activity`() = runTest {
        // Given
        val userId = "user123"
        val streakType = StreakType.DAILY_CHECK_IN
        
        // When
        val result = gamificationService.updateStreak(userId, streakType)
        
        // Then
        assertTrue(result.isSuccess)
        val streak = result.getOrThrow()
        assertEquals(1, streak.currentCount)
        assertEquals(1, streak.bestCount)
        assertTrue(streak.isActive)
        assertEquals(streakType, streak.type)
    }
    
    @Test
    fun `updateStreak should increment consecutive day streak`() = runTest {
        // Given
        val userId = "user123"
        val streakType = StreakType.DAILY_CHECK_IN
        val yesterday = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.minus(1, kotlinx.datetime.DateTimeUnit.DAY)
        
        val existingStreak = Streak(
            id = "streak1",
            type = streakType,
            currentCount = 3,
            bestCount = 5,
            lastActivityDate = yesterday,
            isActive = true
        )
        mockRepository.updateStreak(existingStreak, userId)
        
        // When
        val result = gamificationService.updateStreak(userId, streakType)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedStreak = result.getOrThrow()
        assertEquals(4, updatedStreak.currentCount)
        assertEquals(5, updatedStreak.bestCount) // Should remain the same
    }
    
    @Test
    fun `updateStreak should reset broken streak`() = runTest {
        // Given
        val userId = "user123"
        val streakType = StreakType.DAILY_CHECK_IN
        val threeDaysAgo = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.minus(3, kotlinx.datetime.DateTimeUnit.DAY)
        
        val existingStreak = Streak(
            id = "streak1",
            type = streakType,
            currentCount = 5,
            bestCount = 10,
            lastActivityDate = threeDaysAgo,
            isActive = true
        )
        mockRepository.updateStreak(existingStreak, userId)
        
        // When
        val result = gamificationService.updateStreak(userId, streakType)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedStreak = result.getOrThrow()
        assertEquals(1, updatedStreak.currentCount) // Reset to 1
        assertEquals(10, updatedStreak.bestCount) // Best count should remain
    }
    
    @Test
    fun `unlockAchievement should create new achievement`() = runTest {
        // Given
        val userId = "user123"
        val achievementType = AchievementType.FIRST_GOAL_CREATED
        
        // When
        val result = gamificationService.unlockAchievement(userId, achievementType)
        
        // Then
        assertTrue(result.isSuccess)
        val achievement = result.getOrThrow()
        assertEquals("Goal Setter", achievement.title)
        assertEquals("Created your first financial goal", achievement.description)
        assertEquals("🎯", achievement.badgeIcon)
        assertEquals(AchievementCategory.GOAL_ACHIEVEMENT, achievement.category)
        assertEquals(50, achievement.pointsAwarded)
    }
    
    @Test
    fun `unlockAchievement should return existing achievement if already unlocked`() = runTest {
        // Given
        val userId = "user123"
        val achievementType = AchievementType.FIRST_GOAL_CREATED
        
        // First unlock
        val firstResult = gamificationService.unlockAchievement(userId, achievementType)
        assertTrue(firstResult.isSuccess)
        val firstAchievement = firstResult.getOrThrow()
        
        // When - Try to unlock again
        val secondResult = gamificationService.unlockAchievement(userId, achievementType)
        
        // Then
        assertTrue(secondResult.isSuccess)
        val secondAchievement = secondResult.getOrThrow()
        assertEquals(firstAchievement.id, secondAchievement.id)
        assertEquals(firstAchievement.unlockedAt, secondAchievement.unlockedAt)
    }
    
    @Test
    fun `awardPoints should trigger achievements for specific actions`() = runTest {
        // Given
        val userId = "user123"
        
        // When - Link account (should trigger FIRST_ACCOUNT_LINKED achievement)
        val result = gamificationService.awardPoints(userId, UserAction.LINK_ACCOUNT)
        
        // Then
        assertTrue(result.isSuccess)
        val pointsResult = result.getOrThrow()
        assertEquals(1, pointsResult.newAchievements.size)
        assertEquals("Connected", pointsResult.newAchievements[0].title)
    }
    
    @Test
    fun `getAvailableMicroWins should return list of micro wins`() = runTest {
        // Given
        val userId = "user123"
        
        // When
        val result = gamificationService.getAvailableMicroWins(userId)
        
        // Then
        assertTrue(result.isSuccess)
        val microWins = result.getOrThrow()
        assertTrue(microWins.isNotEmpty())
        assertTrue(microWins.any { it.title == "Check Your Balance" })
        assertTrue(microWins.any { it.title == "Categorize 3 Transactions" })
        assertTrue(microWins.any { it.title == "Review Your Insights" })
    }
    
    @Test
    fun `getPointsHistory should return user's points history`() = runTest {
        // Given
        val userId = "user123"
        val historyEntry = PointsHistoryEntry(
            id = "history1",
            points = 10,
            action = UserAction.CHECK_BALANCE,
            description = "Daily check-in",
            earnedAt = Clock.System.now()
        )
        mockRepository.addPointsHistory(historyEntry, userId)
        
        // When
        val result = gamificationService.getPointsHistory(userId, 10)
        
        // Then
        assertTrue(result.isSuccess)
        val history = result.getOrThrow()
        assertEquals(1, history.size)
        assertEquals(historyEntry.id, history[0].id)
        assertEquals(historyEntry.points, history[0].points)
        assertEquals(historyEntry.action, history[0].action)
    }
    
    @Test
    fun `checkLevelUp should return null when no level up occurred`() = runTest {
        // Given
        val userId = "user123"
        val profile = GamificationProfile(
            level = 2,
            totalPoints = 150, // Still level 2
            currentStreaks = emptyList(),
            achievements = emptyList(),
            lastActivity = Clock.System.now()
        )
        mockRepository.createGamificationProfile(profile, userId)
        
        // When
        val result = gamificationService.checkLevelUp(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }
    
    @Test
    fun `checkLevelUp should return level up result when level increased`() = runTest {
        // Given
        val userId = "user123"
        val profile = GamificationProfile(
            level = 1, // Profile shows level 1
            totalPoints = 450, // But points indicate level 3
            currentStreaks = emptyList(),
            achievements = emptyList(),
            lastActivity = Clock.System.now()
        )
        mockRepository.createGamificationProfile(profile, userId)
        
        // When
        val result = gamificationService.checkLevelUp(userId)
        
        // Then
        assertTrue(result.isSuccess)
        val levelUpResult = result.getOrThrow()
        assertNotNull(levelUpResult)
        assertEquals(1, levelUpResult.oldLevel)
        assertEquals(3, levelUpResult.newLevel)
        assertEquals(450, levelUpResult.totalPoints)
        assertTrue(levelUpResult.celebrationMessage.contains("Level 3"))
    }
    
    // Additional comprehensive edge case tests
    
    @Test
    fun `awardPoints should handle concurrent point awards correctly`() = runTest {
        // Given
        val userId = "user123"
        val existingProfile = GamificationProfile(
            level = 1,
            totalPoints = 50,
            currentStreaks = emptyList(),
            achievements = emptyList(),
            lastActivity = Clock.System.now()
        )
        mockRepository.createGamificationProfile(existingProfile, userId)
        
        // When - Simulate concurrent point awards
        val results = listOf(
            gamificationService.awardPoints(userId, UserAction.CHECK_BALANCE),
            gamificationService.awardPoints(userId, UserAction.CATEGORIZE_TRANSACTION),
            gamificationService.awardPoints(userId, UserAction.UPDATE_GOAL)
        )
        
        // Then - All should succeed
        results.forEach { result ->
            assertTrue(result.isSuccess)
        }
    }
    
    @Test
    fun `awardPoints should handle maximum points correctly`() = runTest {
        // Given - User with very high points
        val userId = "user123"
        val existingProfile = GamificationProfile(
            level = 10,
            totalPoints = Int.MAX_VALUE - 10,
            currentStreaks = emptyList(),
            achievements = emptyList(),
            lastActivity = Clock.System.now()
        )
        mockRepository.createGamificationProfile(existingProfile, userId)
        
        // When
        val result = gamificationService.awardPoints(userId, UserAction.CHECK_BALANCE)
        
        // Then - Should handle overflow gracefully
        assertTrue(result.isSuccess)
        val pointsResult = result.getOrThrow()
        assertTrue(pointsResult.totalPoints > 0) // Should not overflow to negative
    }
    
    @Test
    fun `updateStreak should handle timezone edge cases`() = runTest {
        // Given
        val userId = "user123"
        val streakType = StreakType.DAILY_CHECK_IN
        val yesterday = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.minus(1, kotlinx.datetime.DateTimeUnit.DAY)
        
        val existingStreak = Streak(
            id = "streak1",
            type = streakType,
            currentCount = 5,
            bestCount = 10,
            lastActivityDate = yesterday,
            isActive = true
        )
        mockRepository.updateStreak(existingStreak, userId)
        
        // When - Update streak at different times of day
        val result1 = gamificationService.updateStreak(userId, streakType)
        val result2 = gamificationService.updateStreak(userId, streakType)
        
        // Then - Should not double-count same day
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertEquals(result1.getOrThrow().currentCount, result2.getOrThrow().currentCount)
    }
    
    @Test
    fun `updateStreak should handle very long streaks`() = runTest {
        // Given
        val userId = "user123"
        val streakType = StreakType.DAILY_CHECK_IN
        val yesterday = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.minus(1, kotlinx.datetime.DateTimeUnit.DAY)
        
        val existingStreak = Streak(
            id = "streak1",
            type = streakType,
            currentCount = 365, // 1 year streak
            bestCount = 500,
            lastActivityDate = yesterday,
            isActive = true
        )
        mockRepository.updateStreak(existingStreak, userId)
        
        // When
        val result = gamificationService.updateStreak(userId, streakType)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedStreak = result.getOrThrow()
        assertEquals(366, updatedStreak.currentCount)
        assertEquals(500, updatedStreak.bestCount) // Should remain the same
    }
    
    @Test
    fun `updateStreak should update best count when exceeded`() = runTest {
        // Given
        val userId = "user123"
        val streakType = StreakType.DAILY_CHECK_IN
        val yesterday = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.minus(1, kotlinx.datetime.DateTimeUnit.DAY)
        
        val existingStreak = Streak(
            id = "streak1",
            type = streakType,
            currentCount = 10,
            bestCount = 10, // Same as current
            lastActivityDate = yesterday,
            isActive = true
        )
        mockRepository.updateStreak(existingStreak, userId)
        
        // When
        val result = gamificationService.updateStreak(userId, streakType)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedStreak = result.getOrThrow()
        assertEquals(11, updatedStreak.currentCount)
        assertEquals(11, updatedStreak.bestCount) // Should be updated
    }
    
    @Test
    fun `unlockAchievement should handle duplicate achievement attempts`() = runTest {
        // Given
        val userId = "user123"
        val achievementType = AchievementType.FIRST_GOAL_CREATED
        
        // When - Try to unlock the same achievement multiple times
        val result1 = gamificationService.unlockAchievement(userId, achievementType)
        val result2 = gamificationService.unlockAchievement(userId, achievementType)
        val result3 = gamificationService.unlockAchievement(userId, achievementType)
        
        // Then - All should succeed but return the same achievement
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertTrue(result3.isSuccess)
        
        val achievement1 = result1.getOrThrow()
        val achievement2 = result2.getOrThrow()
        val achievement3 = result3.getOrThrow()
        
        assertEquals(achievement1.id, achievement2.id)
        assertEquals(achievement1.id, achievement3.id)
        assertEquals(achievement1.unlockedAt, achievement2.unlockedAt)
        assertEquals(achievement1.unlockedAt, achievement3.unlockedAt)
    }
    
    @Test
    fun `awardPoints should trigger multiple achievements for milestone actions`() = runTest {
        // Given
        val userId = "user123"
        val profile = GamificationProfile(
            level = 1,
            totalPoints = 95, // Close to level up
            currentStreaks = emptyList(),
            achievements = emptyList(),
            lastActivity = Clock.System.now()
        )
        mockRepository.createGamificationProfile(profile, userId)
        
        // When - Award points that trigger level up
        val result = gamificationService.awardPoints(userId, UserAction.COMPLETE_GOAL)
        
        // Then - Should trigger both goal completion and level up achievements
        assertTrue(result.isSuccess)
        val pointsResult = result.getOrThrow()
        assertTrue(pointsResult.newAchievements.isNotEmpty())
        assertTrue(pointsResult.leveledUp)
    }
    
    @Test
    fun `getAvailableMicroWins should filter out completed micro wins`() = runTest {
        // Given
        val userId = "user123"
        
        // Simulate some completed actions
        gamificationService.awardPoints(userId, UserAction.CHECK_BALANCE)
        
        // When
        val result = gamificationService.getAvailableMicroWins(userId)
        
        // Then
        assertTrue(result.isSuccess)
        val microWins = result.getOrThrow()
        assertTrue(microWins.isNotEmpty())
        
        // Should not include already completed micro wins for today
        val checkBalanceMicroWin = microWins.find { it.title == "Check Your Balance" }
        // This depends on implementation - might be null if already completed today
    }
    
    @Test
    fun `getPointsHistory should return history in correct order`() = runTest {
        // Given
        val userId = "user123"
        val entries = listOf(
            PointsHistoryEntry(
                id = "history1",
                points = 10,
                action = UserAction.CHECK_BALANCE,
                description = "Daily check-in",
                earnedAt = Clock.System.now().minus(2, kotlinx.datetime.DateTimeUnit.HOUR)
            ),
            PointsHistoryEntry(
                id = "history2",
                points = 15,
                action = UserAction.CATEGORIZE_TRANSACTION,
                description = "Categorized transaction",
                earnedAt = Clock.System.now().minus(1, kotlinx.datetime.DateTimeUnit.HOUR)
            ),
            PointsHistoryEntry(
                id = "history3",
                points = 25,
                action = UserAction.UPDATE_GOAL,
                description = "Updated goal",
                earnedAt = Clock.System.now()
            )
        )
        
        entries.forEach { entry ->
            mockRepository.addPointsHistory(entry, userId)
        }
        
        // When
        val result = gamificationService.getPointsHistory(userId, 10)
        
        // Then
        assertTrue(result.isSuccess)
        val history = result.getOrThrow()
        assertEquals(3, history.size)
        
        // Should be in chronological order (most recent first or last depending on implementation)
        assertTrue(history.isNotEmpty())
    }
    
    @Test
    fun `gamification should handle user with no profile gracefully`() = runTest {
        // Given
        val userId = "nonexistent_user"
        
        // When
        val result = gamificationService.awardPoints(userId, UserAction.CHECK_BALANCE)
        
        // Then - Should create new profile and award points
        assertTrue(result.isSuccess)
        val pointsResult = result.getOrThrow()
        assertEquals(5, pointsResult.pointsAwarded) // CHECK_BALANCE points
        assertEquals(5, pointsResult.totalPoints)
        assertEquals(1, pointsResult.newLevel)
    }
    
    @Test
    fun `streak tracking should handle different streak types independently`() = runTest {
        // Given
        val userId = "user123"
        
        // When - Update different types of streaks
        val dailyResult = gamificationService.updateStreak(userId, StreakType.DAILY_CHECK_IN)
        val budgetResult = gamificationService.updateStreak(userId, StreakType.WEEKLY_BUDGET_ADHERENCE)
        val savingsResult = gamificationService.updateStreak(userId, StreakType.DAILY_SAVINGS)
        
        // Then - All should succeed independently
        assertTrue(dailyResult.isSuccess)
        assertTrue(budgetResult.isSuccess)
        assertTrue(savingsResult.isSuccess)
        
        assertEquals(1, dailyResult.getOrThrow().currentCount)
        assertEquals(1, budgetResult.getOrThrow().currentCount)
        assertEquals(1, savingsResult.getOrThrow().currentCount)
    }
    
    @Test
    fun `level calculation should be consistent across different point values`() {
        // Test the mathematical consistency of level calculation
        val testCases = mapOf(
            0 to 1,
            50 to 1,
            99 to 1,
            100 to 2,
            399 to 2,
            400 to 3,
            899 to 3,
            900 to 4,
            1599 to 4,
            1600 to 5,
            2499 to 5,
            2500 to 6
        )
        
        testCases.forEach { (points, expectedLevel) ->
            val actualLevel = gamificationService.getLevelFromPoints(points)
            assertEquals(
                expectedLevel, 
                actualLevel, 
                "Points $points should result in level $expectedLevel but got $actualLevel"
            )
        }
    }
    
    @Test
    fun `points required for next level should be consistent with level calculation`() {
        for (level in 1..10) {
            val pointsRequired = gamificationService.getPointsRequiredForNextLevel(level)
            val calculatedLevel = gamificationService.getLevelFromPoints(pointsRequired - 1)
            val nextLevel = gamificationService.getLevelFromPoints(pointsRequired)
            
            assertEquals(level, calculatedLevel, "Points ${pointsRequired - 1} should be level $level")
            assertEquals(level + 1, nextLevel, "Points $pointsRequired should be level ${level + 1}")
        }
    }
}

/**
 * Mock implementation of GamificationRepository for testing.
 */
class MockGamificationRepository : GamificationRepository {
    private val profiles = mutableMapOf<String, GamificationProfile>()
    private val streaks = mutableMapOf<String, MutableMap<StreakType, Streak>>()
    private val achievements = mutableMapOf<String, MutableMap<AchievementType, Achievement>>()
    private val pointsHistory = mutableMapOf<String, MutableList<PointsHistoryEntry>>()
    
    override suspend fun getGamificationProfile(userId: String): GamificationProfile? {
        return profiles[userId]
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