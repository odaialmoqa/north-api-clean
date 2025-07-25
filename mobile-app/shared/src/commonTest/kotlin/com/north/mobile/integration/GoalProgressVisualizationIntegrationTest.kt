package com.north.mobile.integration

import com.north.mobile.data.goal.*
import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

class GoalProgressVisualizationIntegrationTest {

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
    fun `complete goal progress visualization workflow`() = runTest {
        // Given - Create a new goal
        val goal = FinancialGoal(
            id = "integration-test-goal",
            userId = "test-user",
            title = "Vacation Fund",
            description = "Save for summer vacation",
            targetAmount = Money.fromDollars(2000.0),
            currentAmount = Money.zero(),
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 6)),
            priority = Priority.HIGH,
            goalType = GoalType.VACATION,
            isActive = true
        )

        // When - Create goal and get initial visualization
        val createResult = goalService.createGoal(goal)
        assertTrue(createResult.isSuccess)

        val initialVisualization = visualizationService.getProgressVisualization(goal.id)
        assertTrue(initialVisualization.isSuccess)

        // Then - Verify initial state
        val initialViz = initialVisualization.getOrThrow()
        assertEquals(0.0, initialViz.progressRing.currentProgress)
        assertEquals(GoalStatus.ON_TRACK, initialViz.statusIndicators.overallStatus)
        assertTrue(initialViz.milestones.none { it.isReached })

        // When - Update progress to 25%
        val quarterProgress = Money.fromDollars(500.0)
        val quarterUpdate = visualizationService.updateProgressVisualization(goal.id, quarterProgress)
        assertTrue(quarterUpdate.isSuccess)

        // Then - Verify quarter milestone
        val quarterViz = quarterUpdate.getOrThrow()
        assertEquals(0.25, quarterViz.progressRing.currentProgress, 0.01)
        
        val quarterMilestone = quarterViz.milestones.find { it.percentage == 0.25 }
        assertNotNull(quarterMilestone)
        assertTrue(quarterMilestone.isReached)
        assertEquals("Quarter Way", quarterMilestone.title)

        // Verify animation triggers
        val quarterTriggers = quarterViz.animationTriggers.filter { it.shouldTrigger }
        assertTrue(quarterTriggers.isNotEmpty())
        val milestoneTrigger = quarterTriggers.find { it.triggerType == AnimationTriggerType.MILESTONE_REACHED }
        assertNotNull(milestoneTrigger)

        // When - Update progress to 50%
        val halfwayProgress = Money.fromDollars(1000.0)
        val halfwayUpdate = visualizationService.updateProgressVisualization(goal.id, halfwayProgress)
        assertTrue(halfwayUpdate.isSuccess)

        // Then - Verify halfway milestone and celebration
        val halfwayViz = halfwayUpdate.getOrThrow()
        assertEquals(0.5, halfwayViz.progressRing.currentProgress, 0.01)
        
        val halfwayMilestone = halfwayViz.milestones.find { it.percentage == 0.5 }
        assertNotNull(halfwayMilestone)
        assertTrue(halfwayMilestone.isReached)
        assertEquals("Halfway There", halfwayMilestone.title)
        assertEquals(MilestoneCelebrationType.CONFETTI, halfwayMilestone.celebrationType)

        // Test milestone celebration
        val celebrationResult = visualizationService.celebrateMilestone(halfwayMilestone.id)
        assertTrue(celebrationResult.isSuccess)
        val celebration = celebrationResult.getOrThrow()
        assertEquals("Halfway There! 🎖️", celebration.title)
        assertEquals(50, celebration.pointsAwarded)

        // When - Update progress to 90% (almost complete)
        val almostCompleteProgress = Money.fromDollars(1800.0)
        val almostCompleteUpdate = visualizationService.updateProgressVisualization(goal.id, almostCompleteProgress)
        assertTrue(almostCompleteUpdate.isSuccess)

        // Then - Verify almost complete state
        val almostCompleteViz = almostCompleteUpdate.getOrThrow()
        assertEquals(0.9, almostCompleteViz.progressRing.currentProgress, 0.01)
        assertTrue(almostCompleteViz.progressRing.glowEffect) // Should have glow effect at 90%+
        
        val almostDoneMilestone = almostCompleteViz.milestones.find { it.percentage == 0.9 }
        assertNotNull(almostDoneMilestone)
        assertTrue(almostDoneMilestone.isReached)
        assertEquals("Almost Done", almostDoneMilestone.title)

        // When - Complete the goal
        val completeProgress = Money.fromDollars(2000.0)
        val completeUpdate = visualizationService.updateProgressVisualization(goal.id, completeProgress)
        assertTrue(completeUpdate.isSuccess)

        // Then - Verify completion
        val completeViz = completeUpdate.getOrThrow()
        assertEquals(1.0, completeViz.progressRing.currentProgress, 0.01)
        assertEquals(GoalStatus.COMPLETED, completeViz.statusIndicators.overallStatus)
        
        val completionMilestone = completeViz.milestones.find { it.percentage == 1.0 }
        assertNotNull(completionMilestone)
        assertTrue(completionMilestone.isReached)
        assertEquals("Goal Achieved", completionMilestone.title)

        // Verify achievement badges
        val achievements = visualizationService.trackGoalAchievements("test-user")
        assertTrue(achievements.isSuccess)
        val badges = achievements.getOrThrow()
        
        val completionBadge = badges.find { it.title == "Goal Achiever" }
        assertNotNull(completionBadge)
        assertEquals(BadgeCategory.MILESTONE, completionBadge.category)
        assertEquals(BadgeRarity.EPIC, completionBadge.rarity)

        // Verify goal history
        val historyResult = visualizationService.getGoalHistory("test-user")
        assertTrue(historyResult.isSuccess)
        val history = historyResult.getOrThrow()
        
        assertTrue(history.isNotEmpty())
        val creationEntry = history.find { it.eventType == GoalHistoryEventType.GOAL_CREATED }
        assertNotNull(creationEntry)
        
        val milestoneEntries = history.filter { it.eventType == GoalHistoryEventType.MILESTONE_REACHED }
        assertTrue(milestoneEntries.size >= 4) // Should have multiple milestone entries
        
        val completionEntry = history.find { it.eventType == GoalHistoryEventType.GOAL_COMPLETED }
        assertNotNull(completionEntry)
    }

    @Test
    fun `goal adjustment recommendations workflow for behind schedule goal`() = runTest {
        // Given - Create a goal that's behind schedule
        val behindGoal = FinancialGoal(
            id = "behind-goal",
            userId = "test-user",
            title = "Emergency Fund",
            targetAmount = Money.fromDollars(5000.0),
            currentAmount = Money.fromDollars(500.0), // Only 10% progress
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(days = 30)), // 30 days left
            priority = Priority.CRITICAL,
            goalType = GoalType.EMERGENCY_FUND,
            isActive = true,
            createdAt = Clock.System.now().minus(DatePeriod(months = 5).toDuration()) // Started 5 months ago
        )

        goalRepository.insertGoal(behindGoal)

        // When - Get visualization and recommendations
        val visualizationResult = visualizationService.getProgressVisualization(behindGoal.id)
        assertTrue(visualizationResult.isSuccess)
        val visualization = visualizationResult.getOrThrow()

        // Then - Verify critical status and risk indicators
        assertEquals(GoalStatus.CRITICAL, visualization.statusIndicators.overallStatus)
        assertEquals(ProgressStatus.STALLED, visualization.statusIndicators.progressStatus)
        assertEquals(TimelineStatus.OVERDUE, visualization.statusIndicators.timelineStatus)
        assertEquals(RiskLevel.CRITICAL, visualization.projectionData.riskLevel)

        // Verify risk indicators
        val riskIndicators = visualization.statusIndicators.riskIndicators
        assertTrue(riskIndicators.isNotEmpty())
        
        val timelineRisk = riskIndicators.find { it.type == RiskType.TIMELINE_RISK }
        assertNotNull(timelineRisk)
        assertEquals(RiskSeverity.HIGH, timelineRisk.severity)

        val progressRisk = riskIndicators.find { it.type == RiskType.PROGRESS_STALL }
        assertNotNull(progressRisk)
        assertEquals(RiskSeverity.CRITICAL, progressRisk.severity)

        // When - Get adjustment recommendations
        val recommendationsResult = visualizationService.generateAdjustmentRecommendations(behindGoal.id)
        assertTrue(recommendationsResult.isSuccess)
        val recommendations = recommendationsResult.getOrThrow()

        // Then - Verify critical recommendations
        assertTrue(recommendations.isNotEmpty())
        
        val criticalRecs = recommendations.filter { it.priority == Priority.CRITICAL }
        assertTrue(criticalRecs.isNotEmpty())

        val increaseWeeklyRec = recommendations.find { it.type == AdjustmentType.INCREASE_WEEKLY_AMOUNT }
        assertNotNull(increaseWeeklyRec)
        assertTrue(increaseWeeklyRec.estimatedImprovement > 0.0)
        assertFalse(increaseWeeklyRec.isAutomatable) // Should require user action

        val extendDeadlineRec = recommendations.find { it.type == AdjustmentType.EXTEND_DEADLINE }
        assertNotNull(extendDeadlineRec)
        assertTrue(extendDeadlineRec.isAutomatable) // Can be automated

        // When - Apply a recommendation
        val applyResult = visualizationService.applyAdjustmentRecommendation(
            behindGoal.id, 
            extendDeadlineRec.id
        )
        assertTrue(applyResult.isSuccess)

        // Then - Verify goal was updated
        val updatedGoal = applyResult.getOrThrow()
        assertTrue(updatedGoal.targetDate > behindGoal.targetDate)
    }

    @Test
    fun `projection calculations and trend analysis`() = runTest {
        // Given - Create a goal with some progress
        val progressGoal = FinancialGoal(
            id = "progress-goal",
            userId = "test-user",
            title = "Car Purchase",
            targetAmount = Money.fromDollars(15000.0),
            currentAmount = Money.fromDollars(6000.0), // 40% progress
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 8)),
            priority = Priority.HIGH,
            goalType = GoalType.CAR_PURCHASE,
            isActive = true,
            createdAt = Clock.System.now().minus(DatePeriod(months = 4).toDuration()) // 4 months of progress
        )

        goalRepository.insertGoal(progressGoal)

        // When - Calculate projection
        val projectionResult = visualizationService.calculateProjectedCompletion(progressGoal.id)
        assertTrue(projectionResult.isSuccess)
        val projection = projectionResult.getOrThrow()

        // Then - Verify projection data
        assertEquals(progressGoal.id, projection.goalId)
        assertTrue(projection.confidenceLevel > 0.0)
        assertTrue(projection.confidenceLevel <= 1.0)
        assertNotNull(projection.projectedCompletionDate)
        
        // Should have trend and projection lines
        assertTrue(projection.trendLine.isNotEmpty())
        assertTrue(projection.projectionLine.isNotEmpty())
        
        // Verify trend points
        val trendPoints = projection.trendLine
        assertTrue(trendPoints.all { !it.isProjected })
        assertTrue(trendPoints.all { it.confidence == 1.0 })
        
        val projectionPoints = projection.projectionLine
        assertTrue(projectionPoints.all { it.isProjected })
        assertTrue(projectionPoints.any { it.confidence < 1.0 }) // Confidence should decrease over time

        // Verify on-track status
        assertTrue(projection.isOnTrack) // 40% progress in 4 months should be on track

        // Should have appropriate recommendations
        assertTrue(projection.adjustmentRecommendations.isNotEmpty())
    }

    @Test
    fun `streak tracking and consistency monitoring`() = runTest {
        // Given - Create a goal with streak data
        val streakGoal = FinancialGoal(
            id = "streak-goal",
            userId = "test-user",
            title = "Retirement Savings",
            targetAmount = Money.fromDollars(50000.0),
            currentAmount = Money.fromDollars(12000.0),
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(years = 2)),
            priority = Priority.MEDIUM,
            goalType = GoalType.RETIREMENT,
            isActive = true
        )

        goalRepository.insertGoal(streakGoal)

        // When - Get visualization with streak data
        val visualizationResult = visualizationService.getProgressVisualization(streakGoal.id)
        assertTrue(visualizationResult.isSuccess)
        val visualization = visualizationResult.getOrThrow()

        // Then - Verify streak data
        val streakData = visualization.statusIndicators.streakData
        assertNotNull(streakData)
        assertEquals(streakGoal.id, streakData.goalId)
        assertTrue(streakData.currentStreak > 0)
        assertTrue(streakData.longestStreak >= streakData.currentStreak)
        assertEquals(StreakType.WEEKLY_SAVINGS, streakData.streakType)
        assertTrue(streakData.isActive)
        assertFalse(streakData.riskOfBreaking)
    }

    @Test
    fun `animation triggers and celebration system`() = runTest {
        // Given - Create a goal at milestone threshold
        val milestoneGoal = FinancialGoal(
            id = "milestone-goal",
            userId = "test-user",
            title = "Home Down Payment",
            targetAmount = Money.fromDollars(40000.0),
            currentAmount = Money.fromDollars(30000.0), // 75% progress
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 3)),
            priority = Priority.CRITICAL,
            goalType = GoalType.HOME_PURCHASE,
            isActive = true
        )

        goalRepository.insertGoal(milestoneGoal)

        // When - Get animation triggers
        val triggersResult = visualizationService.getAnimationTriggers(milestoneGoal.id)
        assertTrue(triggersResult.isSuccess)
        val triggers = triggersResult.getOrThrow()

        // Then - Verify triggers
        assertTrue(triggers.isNotEmpty())

        // Should have progress update trigger
        val progressTrigger = triggers.find { it.triggerType == AnimationTriggerType.PROGRESS_UPDATE }
        assertNotNull(progressTrigger)
        assertTrue(progressTrigger.shouldTrigger)
        assertEquals("progress_ring_fill", progressTrigger.animationType)

        // Should have milestone triggers for reached milestones
        val milestoneTriggers = triggers.filter { 
            it.triggerType == AnimationTriggerType.MILESTONE_REACHED && it.shouldTrigger 
        }
        assertTrue(milestoneTriggers.size >= 4) // Should have 10%, 25%, 50%, 75%

        // Verify specific milestone trigger
        val threeQuarterTrigger = milestoneTriggers.find { it.id.contains("0.75") }
        assertNotNull(threeQuarterTrigger)
        assertEquals("confetti_burst", threeQuarterTrigger.animationType)
        assertEquals(3000L, threeQuarterTrigger.duration)
        
        val celebrationData = threeQuarterTrigger.celebrationData
        assertNotNull(celebrationData)
        assertEquals("Three Quarters Done! 🏆", celebrationData.title)
        assertEquals(75, celebrationData.pointsAwarded)

        // When - Mark trigger as triggered
        val markResult = visualizationService.markAnimationTriggered(threeQuarterTrigger.id)
        assertTrue(markResult.isSuccess)
    }
}