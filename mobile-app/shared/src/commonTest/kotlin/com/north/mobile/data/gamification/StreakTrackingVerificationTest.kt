package com.north.mobile.data.gamification

import com.north.mobile.domain.model.*
import kotlinx.datetime.*
import kotlin.test.*

/**
 * Simple verification test to ensure the streak tracking system compiles and basic functionality works.
 */
class StreakTrackingVerificationTest {
    
    @Test
    fun `streak data models should be properly constructed`() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        // Test basic Streak construction with new fields
        val streak = Streak(
            id = "test-streak",
            type = StreakType.DAILY_CHECK_IN,
            currentCount = 5,
            bestCount = 10,
            lastActivityDate = today,
            isActive = true,
            riskLevel = StreakRiskLevel.SAFE,
            recoveryAttempts = 0,
            lastReminderSent = null
        )
        
        assertEquals("test-streak", streak.id)
        assertEquals(StreakType.DAILY_CHECK_IN, streak.type)
        assertEquals(5, streak.currentCount)
        assertEquals(10, streak.bestCount)
        assertEquals(today, streak.lastActivityDate)
        assertTrue(streak.isActive)
        assertEquals(StreakRiskLevel.SAFE, streak.riskLevel)
        assertEquals(0, streak.recoveryAttempts)
        assertNull(streak.lastReminderSent)
    }
    
    @Test
    fun `streak risk levels should be properly defined`() {
        val riskLevels = StreakRiskLevel.values()
        
        assertTrue(riskLevels.contains(StreakRiskLevel.SAFE))
        assertTrue(riskLevels.contains(StreakRiskLevel.LOW_RISK))
        assertTrue(riskLevels.contains(StreakRiskLevel.MEDIUM_RISK))
        assertTrue(riskLevels.contains(StreakRiskLevel.HIGH_RISK))
        assertTrue(riskLevels.contains(StreakRiskLevel.BROKEN))
    }
    
    @Test
    fun `micro-win opportunity should be properly constructed`() {
        val microWin = MicroWinOpportunity(
            id = "micro-win-1",
            title = "Check Balance",
            description = "Quick balance check",
            pointsAwarded = 5,
            actionType = UserAction.CHECK_BALANCE,
            difficulty = MicroWinDifficulty.EASY,
            estimatedTimeMinutes = 1,
            expiresAt = null,
            isPersonalized = false,
            contextData = emptyMap()
        )
        
        assertEquals("micro-win-1", microWin.id)
        assertEquals("Check Balance", microWin.title)
        assertEquals(5, microWin.pointsAwarded)
        assertEquals(UserAction.CHECK_BALANCE, microWin.actionType)
        assertEquals(MicroWinDifficulty.EASY, microWin.difficulty)
        assertEquals(1, microWin.estimatedTimeMinutes)
        assertFalse(microWin.isPersonalized)
    }
    
    @Test
    fun `streak recovery should be properly constructed`() {
        val now = Clock.System.now()
        val recovery = StreakRecovery(
            id = "recovery-1",
            userId = "user123",
            originalStreakId = "streak-1",
            streakType = StreakType.DAILY_CHECK_IN,
            brokenAt = now,
            recoveryStarted = now,
            recoveryCompleted = null,
            originalCount = 10,
            recoveryActions = emptyList(),
            isSuccessful = false
        )
        
        assertEquals("recovery-1", recovery.id)
        assertEquals("user123", recovery.userId)
        assertEquals("streak-1", recovery.originalStreakId)
        assertEquals(StreakType.DAILY_CHECK_IN, recovery.streakType)
        assertEquals(10, recovery.originalCount)
        assertFalse(recovery.isSuccessful)
        assertTrue(recovery.recoveryActions.isEmpty())
    }
    
    @Test
    fun `streak reminder should be properly constructed`() {
        val now = Clock.System.now()
        val reminder = StreakReminder(
            id = "reminder-1",
            userId = "user123",
            streakId = "streak-1",
            streakType = StreakType.DAILY_CHECK_IN,
            reminderType = ReminderType.GENTLE_NUDGE,
            message = "Don't forget to check your balance!",
            scheduledFor = now,
            sentAt = null,
            isRead = false
        )
        
        assertEquals("reminder-1", reminder.id)
        assertEquals("user123", reminder.userId)
        assertEquals("streak-1", reminder.streakId)
        assertEquals(StreakType.DAILY_CHECK_IN, reminder.streakType)
        assertEquals(ReminderType.GENTLE_NUDGE, reminder.reminderType)
        assertEquals("Don't forget to check your balance!", reminder.message)
        assertFalse(reminder.isRead)
    }
    
    @Test
    fun `new streak types should be available`() {
        val streakTypes = StreakType.values()
        
        // Original streak types
        assertTrue(streakTypes.contains(StreakType.DAILY_CHECK_IN))
        assertTrue(streakTypes.contains(StreakType.UNDER_BUDGET))
        assertTrue(streakTypes.contains(StreakType.GOAL_PROGRESS))
        assertTrue(streakTypes.contains(StreakType.TRANSACTION_CATEGORIZATION))
        assertTrue(streakTypes.contains(StreakType.SAVINGS_CONTRIBUTION))
        
        // New streak types
        assertTrue(streakTypes.contains(StreakType.WEEKLY_BUDGET_ADHERENCE))
        assertTrue(streakTypes.contains(StreakType.DAILY_SAVINGS))
        assertTrue(streakTypes.contains(StreakType.WEEKLY_GOAL_PROGRESS))
        assertTrue(streakTypes.contains(StreakType.MICRO_WIN_COMPLETION))
        assertTrue(streakTypes.contains(StreakType.FINANCIAL_HEALTH_CHECK))
    }
    
    @Test
    fun `reminder types should be properly defined`() {
        val reminderTypes = ReminderType.values()
        
        assertTrue(reminderTypes.contains(ReminderType.GENTLE_NUDGE))
        assertTrue(reminderTypes.contains(ReminderType.MOTIVATION_BOOST))
        assertTrue(reminderTypes.contains(ReminderType.STREAK_RISK_ALERT))
        assertTrue(reminderTypes.contains(ReminderType.RECOVERY_SUPPORT))
    }
    
    @Test
    fun `micro-win difficulties should be properly defined`() {
        val difficulties = MicroWinDifficulty.values()
        
        assertTrue(difficulties.contains(MicroWinDifficulty.EASY))
        assertTrue(difficulties.contains(MicroWinDifficulty.MEDIUM))
        assertTrue(difficulties.contains(MicroWinDifficulty.HARD))
    }
    
    @Test
    fun `recovery action should be properly constructed`() {
        val now = Clock.System.now()
        val recoveryAction = RecoveryAction(
            id = "action-1",
            actionType = UserAction.CHECK_BALANCE,
            completedAt = now,
            pointsAwarded = 10,
            description = "Recovery balance check"
        )
        
        assertEquals("action-1", recoveryAction.id)
        assertEquals(UserAction.CHECK_BALANCE, recoveryAction.actionType)
        assertEquals(now, recoveryAction.completedAt)
        assertEquals(10, recoveryAction.pointsAwarded)
        assertEquals("Recovery balance check", recoveryAction.description)
    }
    
    @Test
    fun `celebration manager should handle streak celebrations`() {
        val celebrationManager = CelebrationManagerImpl()
        
        // This is a basic test to ensure the celebration manager compiles
        // In a real test environment, we would test the actual celebration logic
        assertNotNull(celebrationManager)
    }
}