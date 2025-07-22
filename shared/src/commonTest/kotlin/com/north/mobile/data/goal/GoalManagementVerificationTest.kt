package com.north.mobile.data.goal

import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

/**
 * Verification test for goal progress visualization and tracking implementation
 * This test verifies that task 12 requirements are met:
 * - Create visual progress indicators with animations
 * - Build projected completion date calculations
 * - Implement progress milestone detection and celebrations
 * - Add goal adjustment recommendations when off-track
 * - Create goal history and achievement tracking
 */
class GoalManagementVerificationTest {

    private lateinit var goalRepository: MockGoalRepository
    private lateinit var goalService: GoalService
    private lateinit var visualizationService: GoalProgressVisualizationService

    @BeforeTest
    fun setup() {
        goalRepository = MockGoalRepository()
        goalService = GoalServiceImpl(goalRepository)
        visualizationService = GoalProgressVisualizationServiceImpl(goalRepository, goalService)
    }

    @Test
    fun `requirement 7_2 - visual progress indicators with animations`() = runTest {
        // Given - Create a goal with 60% progress
        val goal = createTestGoal(progressPercentage = 60.0)
        goalRepository.insertGoal(goal)

        // When - Get progress visualization
        val result = visualizationService.getProgressVisualization(goal.id)

        // Then - Verify visual progress indicators
        assertTrue(result.isSuccess, "Should successfully get progress visualization")
        val visualization = result.getOrThrow()
        
        // Verify progress ring data
        val progressRing = visualization.progressRing
        assertEquals(0.6, progressRing.currentProgress, 0.01, "Progress ring should show 60% progress")
        assertTrue(progressRing.animationDuration > 0, "Should have animation duration")
        assertTrue(progressRing.showPercentage, "Should show percentage")
        assertNotNull(progressRing.ringColor, "Should have ring color")
        
        // Verify animation triggers
        val animationTriggers = visualization.animationTriggers
        assertTrue(animationTriggers.isNotEmpty(), "Should have animation triggers")
        
        val progressTrigger = animationTriggers.find { it.triggerType == AnimationTriggerType.PROGRESS_UPDATE }
        assertNotNull(progressTrigger, "Should have progress update animation trigger")
        assertEquals("progress_ring_fill", progressTrigger.animationType, "Should have correct animation type")
        
        println("✅ Requirement 7.2 - Visual progress indicators with animations: PASSED")
    }

    @Test
    fun `requirement 7_3 - projected completion date calculations`() = runTest {
        // Given - Create a goal with some progress
        val goal = createTestGoal(
            progressPercentage = 40.0,
            daysRemaining = 90
        )
        goalRepository.insertGoal(goal)

        // When - Calculate projected completion
        val result = visualizationService.calculateProjectedCompletion(goal.id)

        // Then - Verify projection calculations
        assertTrue(result.isSuccess, "Should successfully calculate projection")
        val projection = result.getOrThrow()
        
        assertNotNull(projection.projectedCompletionDate, "Should have projected completion date")
        assertTrue(projection.confidenceLevel >= 0.0 && projection.confidenceLevel <= 1.0, 
                  "Confidence level should be between 0 and 1")
        assertTrue(projection.trendLine.isNotEmpty(), "Should have trend line data")
        assertTrue(projection.projectionLine.isNotEmpty(), "Should have projection line data")
        
        // Verify trend points have correct structure
        val trendPoint = projection.trendLine.first()
        assertFalse(trendPoint.isProjected, "Trend points should not be projected")
        assertEquals(1.0, trendPoint.confidence, "Historical trend should have full confidence")
        
        val projectionPoint = projection.projectionLine.first()
        assertTrue(projectionPoint.isProjected, "Projection points should be marked as projected")
        
        println("✅ Requirement 7.3 - Projected completion date calculations: PASSED")
    }

    @Test
    fun `requirement 7_4 - progress milestone detection and celebrations`() = runTest {
        // Given - Create a goal with 75% progress (should trigger multiple milestones)
        val goal = createTestGoal(progressPercentage = 75.0)
        goalRepository.insertGoal(goal)

        // When - Check milestone achievements
        val milestonesResult = visualizationService.checkMilestoneAchievements(goal.id)

        // Then - Verify milestone detection
        assertTrue(milestonesResult.isSuccess, "Should successfully detect milestones")
        val milestones = milestonesResult.getOrThrow()
        
        assertTrue(milestones.isNotEmpty(), "Should have milestones")
        
        // Verify specific milestones are reached
        val reachedMilestones = milestones.filter { it.isReached }
        assertTrue(reachedMilestones.size >= 4, "Should have reached at least 4 milestones (10%, 25%, 50%, 75%)")
        
        // Test specific milestone
        val halfwayMilestone = milestones.find { it.percentage == 0.5 }
        assertNotNull(halfwayMilestone, "Should have halfway milestone")
        assertTrue(halfwayMilestone.isReached, "Halfway milestone should be reached")
        assertEquals("Halfway There", halfwayMilestone.title, "Should have correct milestone title")
        
        // When - Celebrate a milestone
        val celebrationResult = visualizationService.celebrateMilestone(halfwayMilestone.id)
        
        // Then - Verify celebration
        assertTrue(celebrationResult.isSuccess, "Should successfully create celebration")
        val celebration = celebrationResult.getOrThrow()
        
        assertTrue(celebration.title.contains("Halfway"), "Celebration should reference milestone")
        assertTrue(celebration.pointsAwarded > 0, "Should award points")
        assertNotNull(celebration.badgeEarned, "Should earn a badge")
        
        println("✅ Requirement 7.4 - Progress milestone detection and celebrations: PASSED")
    }

    @Test
    fun `requirement 7_4 - goal adjustment recommendations when off-track`() = runTest {
        // Given - Create a goal that's significantly behind schedule
        val behindGoal = createTestGoal(
            progressPercentage = 15.0, // Only 15% progress
            daysRemaining = 30 // But only 30 days left
        )
        goalRepository.insertGoal(behindGoal)

        // When - Generate adjustment recommendations
        val recommendationsResult = visualizationService.generateAdjustmentRecommendations(behindGoal.id)

        // Then - Verify recommendations for off-track goal
        assertTrue(recommendationsResult.isSuccess, "Should successfully generate recommendations")
        val recommendations = recommendationsResult.getOrThrow()
        
        assertTrue(recommendations.isNotEmpty(), "Should have recommendations for off-track goal")
        
        // Should have critical recommendations
        val criticalRecs = recommendations.filter { it.priority == Priority.CRITICAL }
        assertTrue(criticalRecs.isNotEmpty(), "Should have critical recommendations")
        
        // Should include increase weekly amount recommendation
        val increaseWeeklyRec = recommendations.find { it.type == AdjustmentType.INCREASE_WEEKLY_AMOUNT }
        assertNotNull(increaseWeeklyRec, "Should recommend increasing weekly amount")
        assertTrue(increaseWeeklyRec.estimatedImprovement > 0.0, "Should show estimated improvement")
        
        // Should include extend deadline recommendation
        val extendDeadlineRec = recommendations.find { it.type == AdjustmentType.EXTEND_DEADLINE }
        assertNotNull(extendDeadlineRec, "Should recommend extending deadline")
        
        // When - Apply a recommendation
        val applyResult = visualizationService.applyAdjustmentRecommendation(
            behindGoal.id, 
            extendDeadlineRec.id
        )
        
        // Then - Verify recommendation was applied
        assertTrue(applyResult.isSuccess, "Should successfully apply recommendation")
        val updatedGoal = applyResult.getOrThrow()
        assertTrue(updatedGoal.targetDate > behindGoal.targetDate, "Should extend target date")
        
        println("✅ Requirement 7.4 - Goal adjustment recommendations when off-track: PASSED")
    }

    @Test
    fun `requirement 7_4 - goal history and achievement tracking`() = runTest {
        // Given - Create multiple goals with different completion states
        val completedGoal = createTestGoal(
            goalId = "completed-goal",
            progressPercentage = 100.0
        )
        val progressGoal = createTestGoal(
            goalId = "progress-goal", 
            progressPercentage = 60.0
        )
        
        goalRepository.insertGoal(completedGoal)
        goalRepository.insertGoal(progressGoal)

        // When - Track goal achievements
        val achievementsResult = visualizationService.trackGoalAchievements("test-user")

        // Then - Verify achievement tracking
        assertTrue(achievementsResult.isSuccess, "Should successfully track achievements")
        val achievements = achievementsResult.getOrThrow()
        
        assertTrue(achievements.isNotEmpty(), "Should have achievements")
        
        // Should have completion badge for completed goal
        val completionBadge = achievements.find { it.title == "Goal Achiever" }
        assertNotNull(completionBadge, "Should have goal completion badge")
        assertEquals(BadgeCategory.MILESTONE, completionBadge.category, "Should be milestone category")
        assertEquals(100, completionBadge.pointsValue, "Should award 100 points")
        
        // When - Get goal history
        val historyResult = visualizationService.getGoalHistory("test-user")

        // Then - Verify goal history tracking
        assertTrue(historyResult.isSuccess, "Should successfully get goal history")
        val history = historyResult.getOrThrow()
        
        assertTrue(history.isNotEmpty(), "Should have history entries")
        
        // Should have creation entries
        val creationEntries = history.filter { it.eventType == GoalHistoryEventType.GOAL_CREATED }
        assertEquals(2, creationEntries.size, "Should have creation entries for both goals")
        
        // Should have milestone entries
        val milestoneEntries = history.filter { it.eventType == GoalHistoryEventType.MILESTONE_REACHED }
        assertTrue(milestoneEntries.isNotEmpty(), "Should have milestone entries")
        
        // Should have completion entry
        val completionEntries = history.filter { it.eventType == GoalHistoryEventType.GOAL_COMPLETED }
        assertEquals(1, completionEntries.size, "Should have completion entry for completed goal")
        
        // History should be sorted by timestamp (most recent first)
        val timestamps = history.map { it.timestamp.toEpochMilliseconds() }
        assertEquals(timestamps.sortedDescending(), timestamps, "History should be sorted by timestamp descending")
        
        println("✅ Requirement 7.4 - Goal history and achievement tracking: PASSED")
    }

    @Test
    fun `integration test - complete goal progress visualization workflow`() = runTest {
        // Given - Create a new goal
        val goal = createTestGoal(progressPercentage = 0.0)
        goalRepository.insertGoal(goal)

        // When - Get initial visualization
        val initialViz = visualizationService.getProgressVisualization(goal.id).getOrThrow()
        
        // Then - Verify initial state
        assertEquals(0.0, initialViz.progressRing.currentProgress, "Should start at 0% progress")
        assertTrue(initialViz.milestones.none { it.isReached }, "No milestones should be reached initially")

        // When - Update progress to 25%
        val quarterUpdate = visualizationService.updateProgressVisualization(
            goal.id, 
            Money.fromDollars(250.0)
        ).getOrThrow()
        
        // Then - Verify quarter milestone
        assertEquals(0.25, quarterUpdate.progressRing.currentProgress, 0.01, "Should show 25% progress")
        val quarterMilestone = quarterUpdate.milestones.find { it.percentage == 0.25 }
        assertNotNull(quarterMilestone, "Should have quarter milestone")
        assertTrue(quarterMilestone.isReached, "Quarter milestone should be reached")

        // When - Complete the goal
        val completeUpdate = visualizationService.updateProgressVisualization(
            goal.id, 
            Money.fromDollars(1000.0)
        ).getOrThrow()
        
        // Then - Verify completion
        assertEquals(1.0, completeUpdate.progressRing.currentProgress, 0.01, "Should show 100% progress")
        assertEquals(GoalStatus.COMPLETED, completeUpdate.statusIndicators.overallStatus, "Should be completed")
        
        val completionMilestone = completeUpdate.milestones.find { it.percentage == 1.0 }
        assertNotNull(completionMilestone, "Should have completion milestone")
        assertTrue(completionMilestone.isReached, "Completion milestone should be reached")

        println("✅ Integration test - Complete goal progress visualization workflow: PASSED")
    }

    // Helper method to create test goals
    private fun createTestGoal(
        goalId: String = "test-goal",
        userId: String = "test-user",
        title: String = "Emergency Fund",
        targetAmount: Double = 1000.0,
        progressPercentage: Double = 0.0,
        daysRemaining: Long = 90
    ): FinancialGoal {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val targetDate = today.plus(DatePeriod(days = daysRemaining.toInt()))
        val currentAmount = Money.fromDollars(targetAmount * (progressPercentage / 100.0))
        
        return FinancialGoal(
            id = goalId,
            userId = userId,
            title = title,
            description = "Test goal for $title",
            targetAmount = Money.fromDollars(targetAmount),
            currentAmount = currentAmount,
            targetDate = targetDate,
            priority = Priority.HIGH,
            goalType = GoalType.EMERGENCY_FUND,
            microTasks = emptyList(),
            isActive = true,
            createdAt = Clock.System.now().minus(DatePeriod(days = (90 - daysRemaining).toInt()).toDuration())
        )
    }
}