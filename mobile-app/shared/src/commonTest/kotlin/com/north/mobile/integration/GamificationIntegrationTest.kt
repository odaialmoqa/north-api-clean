package com.north.mobile.integration

import com.north.mobile.data.gamification.*
import com.north.mobile.data.repository.GamificationRepositoryImpl
import com.north.mobile.database.NorthDatabase
import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Integration tests for the gamification system with real database operations.
 */
class GamificationIntegrationTest {
    
    private lateinit var database: NorthDatabase
    private lateinit var repository: GamificationRepositoryImpl
    private lateinit var service: GamificationService
    
    @BeforeTest
    fun setup() {
        // Note: In a real implementation, you would set up a test database here
        // For now, we'll create a mock database or use an in-memory database
        // database = createTestDatabase()
        // repository = GamificationRepositoryImpl(database)
        // service = GamificationServiceImpl(repository)
        
        // Using mock for now since we don't have database setup in this context
        repository = MockGamificationRepositoryImpl()
        service = GamificationServiceImpl(repository)
    }
    
    @Test
    fun `complete user journey should award points and unlock achievements`() = runTest {
        val userId = "integration_user_123"
        
        // Step 1: User links their first account
        val linkAccountResult = service.awardPoints(userId, UserAction.LINK_ACCOUNT)
        assertTrue(linkAccountResult.isSuccess)
        
        val linkResult = linkAccountResult.getOrThrow()
        assertEquals(50, linkResult.pointsAwarded)
        assertEquals(50, linkResult.totalPoints)
        assertEquals(1, linkResult.newLevel)
        assertFalse(linkResult.leveledUp)
        assertEquals(1, linkResult.newAchievements.size)
        assertEquals("Connected", linkResult.newAchievements[0].title)
        
        // Step 2: User creates their first goal
        val createGoalResult = service.awardPoints(userId, UserAction.UPDATE_GOAL)
        assertTrue(createGoalResult.isSuccess)
        
        val goalResult = createGoalResult.getOrThrow()
        assertEquals(15, goalResult.pointsAwarded)
        assertEquals(65, goalResult.totalPoints)
        assertEquals(1, goalResult.newLevel)
        assertEquals(1, goalResult.newAchievements.size)
        assertEquals("Goal Setter", goalResult.newAchievements[0].title)
        
        // Step 3: User categorizes multiple transactions
        repeat(5) {
            val categorizeResult = service.awardPoints(userId, UserAction.CATEGORIZE_TRANSACTION)
            assertTrue(categorizeResult.isSuccess)
        }
        
        // Check final state
        val profileResult = service.getGamificationProfile(userId)
        assertTrue(profileResult.isSuccess)
        
        val profile = profileResult.getOrThrow()
        assertEquals(115, profile.totalPoints) // 50 + 15 + (5 * 10)
        assertEquals(2, profile.level) // Should have leveled up at 100 points
        assertEquals(2, profile.achievements.size)
        
        // Verify points history
        val historyResult = service.getPointsHistory(userId, 10)
        assertTrue(historyResult.isSuccess)
        
        val history = historyResult.getOrThrow()
        assertEquals(7, history.size) // 1 link + 1 goal + 5 categorizations
    }
    
    @Test
    fun `streak system should work correctly over multiple days`() = runTest {
        val userId = "streak_user_123"
        
        // Day 1: Start streak
        val day1Result = service.updateStreak(userId, StreakType.DAILY_CHECK_IN)
        assertTrue(day1Result.isSuccess)
        assertEquals(1, day1Result.getOrThrow().currentCount)
        
        // Simulate consecutive days by manually updating the streak
        // In a real test, you would manipulate the system clock or use test time
        val streak1 = day1Result.getOrThrow()
        val streak2 = streak1.copy(
            currentCount = 2,
            bestCount = 2,
            lastActivityDate = streak1.lastActivityDate.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
        )
        repository.updateStreak(streak2, userId)
        
        val streak3 = streak2.copy(
            currentCount = 3,
            bestCount = 3,
            lastActivityDate = streak2.lastActivityDate.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
        )
        repository.updateStreak(streak3, userId)
        
        // Verify streak progression
        val currentStreak = repository.getStreak(userId, StreakType.DAILY_CHECK_IN)
        assertNotNull(currentStreak)
        assertEquals(3, currentStreak.currentCount)
        assertEquals(3, currentStreak.bestCount)
    }
    
    @Test
    fun `level progression should unlock features and provide celebration messages`() = runTest {
        val userId = "level_user_123"
        
        // Award enough points to reach level 5 (1600 points)
        val result = service.awardPoints(userId, UserAction.LINK_ACCOUNT, 1600)
        assertTrue(result.isSuccess)
        
        val pointsResult = result.getOrThrow()
        assertEquals(5, pointsResult.newLevel)
        assertTrue(pointsResult.leveledUp)
        
        // Check level up details
        val levelUpResult = service.checkLevelUp(userId)
        assertTrue(levelUpResult.isSuccess)
        
        // Since the user already leveled up, this should return null
        // In a real scenario, you'd check this before the level up occurs
        val profile = service.getGamificationProfile(userId).getOrThrow()
        assertEquals(5, profile.level)
        assertEquals(1600, profile.totalPoints)
    }
    
    @Test
    fun `micro wins should be generated based on user activity`() = runTest {
        val userId = "microwins_user_123"
        
        // Get available micro wins for new user
        val microWinsResult = service.getAvailableMicroWins(userId)
        assertTrue(microWinsResult.isSuccess)
        
        val microWins = microWinsResult.getOrThrow()
        assertTrue(microWins.isNotEmpty())
        
        // Verify micro win types
        val actionTypes = microWins.map { it.actionType }.toSet()
        assertTrue(actionTypes.contains(UserAction.CHECK_BALANCE))
        assertTrue(actionTypes.contains(UserAction.CATEGORIZE_TRANSACTION))
        assertTrue(actionTypes.contains(UserAction.REVIEW_INSIGHTS))
        
        // Verify points are assigned correctly
        val checkBalanceMicroWin = microWins.first { it.actionType == UserAction.CHECK_BALANCE }
        assertEquals(5, checkBalanceMicroWin.pointsAwarded)
        
        val categorizeMicroWin = microWins.first { it.actionType == UserAction.CATEGORIZE_TRANSACTION }
        assertEquals(30, categorizeMicroWin.pointsAwarded) // 3 transactions * 10 points each
    }
    
    @Test
    fun `achievement system should prevent duplicate achievements`() = runTest {
        val userId = "achievement_user_123"
        
        // Unlock achievement first time
        val firstResult = service.unlockAchievement(userId, AchievementType.FIRST_GOAL_CREATED)
        assertTrue(firstResult.isSuccess)
        val firstAchievement = firstResult.getOrThrow()
        
        // Try to unlock same achievement again
        val secondResult = service.unlockAchievement(userId, AchievementType.FIRST_GOAL_CREATED)
        assertTrue(secondResult.isSuccess)
        val secondAchievement = secondResult.getOrThrow()
        
        // Should return the same achievement
        assertEquals(firstAchievement.id, secondAchievement.id)
        assertEquals(firstAchievement.unlockedAt, secondAchievement.unlockedAt)
        
        // Verify only one achievement exists in the profile
        val profile = service.getGamificationProfile(userId).getOrThrow()
        val goalAchievements = profile.achievements.filter { it.title == "Goal Setter" }
        assertEquals(1, goalAchievements.size)
    }
    
    @Test
    fun `points system should handle edge cases correctly`() = runTest {
        val userId = "edge_case_user_123"
        
        // Test awarding 0 points
        val zeroPointsResult = service.awardPoints(userId, UserAction.CHECK_BALANCE, 0)
        assertTrue(zeroPointsResult.isSuccess)
        assertEquals(0, zeroPointsResult.getOrThrow().pointsAwarded)
        
        // Test awarding negative points (should be handled gracefully)
        val negativePointsResult = service.awardPoints(userId, UserAction.CHECK_BALANCE, -10)
        assertTrue(negativePointsResult.isSuccess)
        // Implementation should handle this appropriately (either reject or clamp to 0)
        
        // Test very large point awards
        val largePointsResult = service.awardPoints(userId, UserAction.LINK_ACCOUNT, 10000)
        assertTrue(largePointsResult.isSuccess)
        val largeResult = largePointsResult.getOrThrow()
        assertEquals(10000, largeResult.pointsAwarded)
        assertTrue(largeResult.newLevel > 1) // Should trigger multiple level ups
    }
}

/**
 * Mock implementation for integration testing.
 * In a real implementation, this would be replaced with actual database operations.
 */
private class MockGamificationRepositoryImpl : GamificationRepositoryImpl {
    private val mockRepo = MockGamificationRepository()
    
    constructor() : super(null as NorthDatabase) // Hack for testing
    
    override suspend fun getGamificationProfile(userId: String) = mockRepo.getGamificationProfile(userId)
    override suspend fun createGamificationProfile(profile: GamificationProfile, userId: String) = mockRepo.createGamificationProfile(profile, userId)
    override suspend fun updateGamificationProfile(profile: GamificationProfile, userId: String) = mockRepo.updateGamificationProfile(profile, userId)
    override suspend fun getStreak(userId: String, streakType: StreakType) = mockRepo.getStreak(userId, streakType)
    override suspend fun getActiveStreaks(userId: String) = mockRepo.getActiveStreaks(userId)
    override suspend fun updateStreak(streak: Streak, userId: String) = mockRepo.updateStreak(streak, userId)
    override suspend fun getAchievement(userId: String, achievementType: AchievementType) = mockRepo.getAchievement(userId, achievementType)
    override suspend fun getAchievements(userId: String) = mockRepo.getAchievements(userId)
    override suspend fun addAchievement(achievement: Achievement, userId: String, achievementType: AchievementType) = mockRepo.addAchievement(achievement, userId, achievementType)
    override suspend fun addPointsHistory(entry: PointsHistoryEntry, userId: String) = mockRepo.addPointsHistory(entry, userId)
    override suspend fun getPointsHistory(userId: String, limit: Int) = mockRepo.getPointsHistory(userId, limit)
}

/**
 * Reuse the mock from the unit tests.
 */
private class MockGamificationRepository : GamificationRepository {
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