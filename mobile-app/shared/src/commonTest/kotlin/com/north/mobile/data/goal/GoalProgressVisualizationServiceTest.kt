package com.north.mobile.data.goal

import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

class GoalProgressVisualizationServiceTest {

    private lateinit var mockGoalRepository: MockGoalRepository
    private lateinit var mockGoalService: GoalService
    private lateinit var visualizationService: GoalProgressVisualizationService

    @BeforeTest
    fun setup() {
        mockGoalRepository = MockGoalRepository()
        mockGoalService = GoalServiceImpl(mockGoalRepository)
        visualizationService = GoalProgressVisualizationServiceImpl(mockGoalRepository, mockGoalService)
    }

    @Test
    fun `getProgressVisualization should return complete visualization data`() = runTest {
        // Given
        val goal = createTestGoal(progressPercentage = 60.0)
        mockGoalRepository.insertGoal(goal)

        // When
        val result = visualizationService.getProgressVisualization(goal.id)

        // Then
        assertTrue(result.isSuccess)
        val visualization = result.getOrThrow()
        
        assertEquals(goal.id, visualization.goalId)
        assertEquals(0.6, visualization.progressRing.currentProgress, 0.01)
        assertTrue(visualization.milestones.isNotEmpty())
        assertNotNull(visualization.projectionData)
        assertTrue(visualization.animationTriggers.isNotEmpty())
        assertNotNull(visualization.statusIndicators)
    }

    @Test
    fun `checkMilestoneAchievements should detect reached milestones`() = runTest {
        // Given
        val goal = createTestGoal(progressPercentage = 75.0)
        mockGoalRepository.insertGoal(goal)

        // When
        val result = visualizationService.checkMilestoneAchievements(goal.id)

        // Then
        assertTrue(result.isSuccess)
        val milestones = result.getOrThrow()
        
        // Should have milestones for 10%, 25%, 50%, and 75%
        val reachedMilestones = milestones.filter { it.isReached }
        assertTrue(reachedMilestones.size >= 4)
        
        // Check specific milestones
        val quarterMilestone = milestones.find { it.percentage == 0.25 }
        assertNotNull(quarterMilestone)
        assertTrue(quarterMilestone.isReached)
        assertEquals("Quarter Way", quarterMilestone.title)
        
        val halfwayMilestone = milestones.find { it.percentage == 0.5 }
        assertNotNull(halfwayMilestone)
        assertTrue(halfwayMilestone.isReached)
        assertEquals("Halfway There", halfwayMilestone.title)
    }

    @Test
    fun `celebrateMilestone should return celebration data`() = runTest {
        // Given
        val goal = createTestGoal(progressPercentage = 50.0)
        mockGoalRepository.insertGoal(goal)
        val milestoneId = "${goal.id}_milestone_0.5"

        // When
        val result = visualizationService.celebrateMilestone(milestoneId)

        // Then
        assertTrue(result.isSuccess)
        val celebration = result.getOrThrow()
        
        assertEquals("Halfway There! 🎖️", celebration.title)
        assertTrue(celebration.message.contains("50%"))
        assertTrue(celebration.message.contains(goal.title))
        assertEquals(50, celebration.pointsAwarded)
        assertEquals("milestone_50_percent", celebration.badgeEarned)
    }

    @Test
    fun `calculateProjectedCompletion should provide accurate projections`() = runTest {
        // Given
        val goal = createTestGoal(
            progressPercentage = 40.0,
            daysRemaining = 60
        )
        mockGoalRepository.insertGoal(goal)

        // When
        val result = visualizationService.calculateProjectedCompletion(goal.id)

        // Then
        assertTrue(result.isSuccess)
        val projection = result.getOrThrow()
        
        assertEquals(goal.id, projection.goalId)
        assertTrue(projection.confidenceLevel > 0.0)
        assertTrue(projection.trendLine.isNotEmpty())
        assertTrue(projection.projectionLine.isNotEmpty())
        assertNotNull(projection.projectedCompletionDate)
        assertTrue(projection.adjustmentRecommendations.isNotEmpty())
    }

    @Test
    fun `generateAdjustmentRecommendations should provide critical recommendations for behind schedule goals`() = runTest {
        // Given - Goal significantly behind schedule
        val goal = createTestGoal(
            progressPercentage = 20.0,
            daysRemaining = 30 // Should be much further along
        )
        mockGoalRepository.insertGoal(goal)

        // When
        val result = visualizationService.generateAdjustmentRecommendations(goal.id)

        // Then
        assertTrue(result.isSuccess)
        val recommendations = result.getOrThrow()
        
        assertTrue(recommendations.isNotEmpty())
        
        // Should have critical recommendations
        val criticalRecommendations = recommendations.filter { it.priority == Priority.CRITICAL }
        assertTrue(criticalRecommendations.isNotEmpty())
        
        // Should include increase weekly amount recommendation
        val increaseWeeklyRec = recommendations.find { it.type == AdjustmentType.INCREASE_WEEKLY_AMOUNT }
        assertNotNull(increaseWeeklyRec)
        assertTrue(increaseWeeklyRec.estimatedImprovement > 0.0)
    }

    @Test
    fun `generateAdjustmentRecommendations should provide optimization recommendations for ahead schedule goals`() = runTest {
        // Given - Goal well ahead of schedule
        val goal = createTestGoal(
            progressPercentage = 80.0,
            daysRemaining = 120 // Plenty of time left
        )
        mockGoalRepository.insertGoal(goal)

        // When
        val result = visualizationService.generateAdjustmentRecommendations(goal.id)

        // Then
        assertTrue(result.isSuccess)
        val recommendations = result.getOrThrow()
        
        assertTrue(recommendations.isNotEmpty())
        
        // Should have optimization recommendations
        val optimizationRecs = recommendations.filter { 
            it.title.contains("Accelerate") || it.title.contains("Optimize") 
        }
        assertTrue(optimizationRecs.isNotEmpty())
    }

    @Test
    fun `updateProgressVisualization should update progress and check milestones`() = runTest {
        // Given
        val goal = createTestGoal(progressPercentage = 40.0)
        mockGoalRepository.insertGoal(goal)
        val newAmount = Money.fromDollars(500.0) // This should push progress to 50%

        // When
        val result = visualizationService.updateProgressVisualization(goal.id, newAmount)

        // Then
        assertTrue(result.isSuccess)
        val visualization = result.getOrThrow()
        
        // Progress should be updated
        assertTrue(visualization.progressRing.currentProgress > 0.4)
        
        // Should have milestone achievements
        val halfwayMilestone = visualization.milestones.find { it.percentage == 0.5 }
        assertNotNull(halfwayMilestone)
    }

    @Test
    fun `trackGoalAchievements should return achievement badges`() = runTest {
        // Given
        val completedGoal = createTestGoal(progressPercentage = 100.0)
        val halfwayGoal = createTestGoal(progressPercentage = 50.0, goalId = "goal2")
        
        mockGoalRepository.insertGoal(completedGoal)
        mockGoalRepository.insertGoal(halfwayGoal)

        // When
        val result = visualizationService.trackGoalAchievements("user1")

        // Then
        assertTrue(result.isSuccess)
        val achievements = result.getOrThrow()
        
        assertTrue(achievements.isNotEmpty())
        
        // Should have completion badge for completed goal
        val completionBadge = achievements.find { it.title == "Goal Achiever" }
        assertNotNull(completionBadge)
        assertEquals(BadgeCategory.MILESTONE, completionBadge.category)
        assertEquals(100, completionBadge.pointsValue)
        
        // Should have progress badge for halfway goal
        val halfwayBadge = achievements.find { it.title == "Halfway Hero" }
        assertNotNull(halfwayBadge)
        assertEquals(50, halfwayBadge.pointsValue)
    }

    @Test
    fun `getGoalHistory should return chronological history entries`() = runTest {
        // Given
        val goal1 = createTestGoal(progressPercentage = 75.0)
        val goal2 = createTestGoal(progressPercentage = 100.0, goalId = "goal2")
        
        mockGoalRepository.insertGoal(goal1)
        mockGoalRepository.insertGoal(goal2)

        // When
        val result = visualizationService.getGoalHistory("user1", limit = 10)

        // Then
        assertTrue(result.isSuccess)
        val history = result.getOrThrow()
        
        assertTrue(history.isNotEmpty())
        
        // Should have creation entries
        val creationEntries = history.filter { it.eventType == GoalHistoryEventType.GOAL_CREATED }
        assertEquals(2, creationEntries.size)
        
        // Should have milestone entries
        val milestoneEntries = history.filter { it.eventType == GoalHistoryEventType.MILESTONE_REACHED }
        assertTrue(milestoneEntries.isNotEmpty())
        
        // Should have completion entry for completed goal
        val completionEntries = history.filter { it.eventType == GoalHistoryEventType.GOAL_COMPLETED }
        assertEquals(1, completionEntries.size)
        
        // Should be sorted by timestamp (most recent first)
        val timestamps = history.map { it.timestamp.toEpochMilliseconds() }
        assertEquals(timestamps.sortedDescending(), timestamps)
    }

    @Test
    fun `getAnimationTriggers should return appropriate triggers`() = runTest {
        // Given
        val goal = createTestGoal(progressPercentage = 50.0)
        mockGoalRepository.insertGoal(goal)

        // When
        val result = visualizationService.getAnimationTriggers(goal.id)

        // Then
        assertTrue(result.isSuccess)
        val triggers = result.getOrThrow()
        
        assertTrue(triggers.isNotEmpty())
        
        // Should have progress update trigger
        val progressTrigger = triggers.find { it.triggerType == AnimationTriggerType.PROGRESS_UPDATE }
        assertNotNull(progressTrigger)
        assertEquals("progress_ring_fill", progressTrigger.animationType)
        
        // Should have milestone triggers for reached milestones
        val milestoneTriggers = triggers.filter { it.triggerType == AnimationTriggerType.MILESTONE_REACHED }
        assertTrue(milestoneTriggers.isNotEmpty())
        
        // Check specific milestone trigger
        val halfwayTrigger = milestoneTriggers.find { it.id.contains("0.5") }
        assertNotNull(halfwayTrigger)
        assertEquals("confetti_burst", halfwayTrigger.animationType)
        assertNotNull(halfwayTrigger.celebrationData)
    }

    @Test
    fun `progress ring should have appropriate colors based on progress`() = runTest {
        // Test different progress levels
        val testCases = listOf(
            0.0 to "#FF6B6B", // Red for no progress
            0.3 to "#FFE66D", // Yellow for quarter progress
            0.6 to "#4ECDC4", // Teal for half progress
            0.8 to "#45B7D1", // Blue for three-quarter progress
            1.0 to "#96CEB4"  // Green for completion
        )

        testCases.forEach { (progress, expectedColor) ->
            // Given
            val goal = createTestGoal(progressPercentage = progress * 100)
            mockGoalRepository.insertGoal(goal)

            // When
            val result = visualizationService.getProgressVisualization(goal.id)

            // Then
            assertTrue(result.isSuccess)
            val visualization = result.getOrThrow()
            assertEquals(expectedColor, visualization.progressRing.ringColor)
            
            // Cleanup
            mockGoalRepository.deleteGoal(goal.id)
        }
    }

    @Test
    fun `risk indicators should be generated based on goal status`() = runTest {
        // Given - Goal with approaching deadline
        val urgentGoal = createTestGoal(
            progressPercentage = 30.0,
            daysRemaining = 15 // Less than 30 days
        )
        mockGoalRepository.insertGoal(urgentGoal)

        // When
        val result = visualizationService.getProgressVisualization(urgentGoal.id)

        // Then
        assertTrue(result.isSuccess)
        val visualization = result.getOrThrow()
        
        val riskIndicators = visualization.statusIndicators.riskIndicators
        assertTrue(riskIndicators.isNotEmpty())
        
        // Should have timeline risk
        val timelineRisk = riskIndicators.find { it.type == RiskType.TIMELINE_RISK }
        assertNotNull(timelineRisk)
        assertEquals(RiskSeverity.HIGH, timelineRisk.severity)
        assertTrue(timelineRisk.title.contains("Deadline"))
        
        // Should have progress stall risk
        val progressRisk = riskIndicators.find { it.type == RiskType.PROGRESS_STALL }
        assertNotNull(progressRisk)
        assertEquals(RiskSeverity.CRITICAL, progressRisk.severity)
    }

    // Helper methods

    private fun createTestGoal(
        goalId: String = "test-goal-1",
        userId: String = "user1",
        title: String = "Emergency Fund",
        targetAmount: Double = 1000.0,
        currentAmount: Double = 0.0,
        progressPercentage: Double = 0.0,
        daysRemaining: Long = 90
    ): FinancialGoal {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val targetDate = today.plus(DatePeriod(days = daysRemaining.toInt()))
        val actualCurrentAmount = Money.fromDollars(targetAmount * (progressPercentage / 100.0))
        
        return FinancialGoal(
            id = goalId,
            userId = userId,
            title = title,
            description = "Test goal for $title",
            targetAmount = Money.fromDollars(targetAmount),
            currentAmount = actualCurrentAmount,
            targetDate = targetDate,
            priority = Priority.HIGH,
            goalType = GoalType.EMERGENCY_FUND,
            microTasks = emptyList(),
            isActive = true,
            createdAt = Clock.System.now().minus(DatePeriod(days = (90 - daysRemaining).toInt()).toDuration())
        )
    }
}