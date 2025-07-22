package com.north.mobile.integration

import com.north.mobile.data.goal.*
import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

/**
 * Integration tests for the comprehensive goal management system
 */
class GoalManagementIntegrationTest {

    private lateinit var goalRepository: GoalRepository
    private lateinit var goalService: GoalService

    @BeforeTest
    fun setup() {
        goalRepository = MockGoalRepository()
        goalService = GoalServiceImpl(goalRepository)
    }

    @Test
    fun `complete goal lifecycle - create, track progress, complete, celebrate`() = runTest {
        // Given - Create a new goal
        val initialGoal = FinancialGoal(
            id = "emergency-fund-goal",
            userId = "user123",
            title = "Emergency Fund",
            description = "Build 6 months of emergency savings",
            targetAmount = Money.fromDollars(6000.0),
            currentAmount = Money.zero(),
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 6)),
            priority = Priority.HIGH,
            goalType = GoalType.EMERGENCY_FUND
        )

        // When - Create the goal
        val createResult = goalService.createGoal(initialGoal)
        assertTrue(createResult.isSuccess)
        val createdGoal = createResult.getOrThrow()

        // Then - Verify goal was created with micro tasks
        assertTrue(createdGoal.microTasks.isNotEmpty())
        assertEquals(GoalType.EMERGENCY_FUND, createdGoal.goalType)

        // When - Update progress multiple times
        val progress1 = goalService.updateGoalProgress(createdGoal.id, Money.fromDollars(1000.0))
        assertTrue(progress1.isSuccess)

        val progress2 = goalService.updateGoalProgress(createdGoal.id, Money.fromDollars(3000.0))
        assertTrue(progress2.isSuccess)

        // Then - Check progress tracking
        val progressResult = goalService.getGoalProgress(createdGoal.id)
        assertTrue(progressResult.isSuccess)
        val progressData = progressResult.getOrThrow()
        assertEquals(50.0, progressData.progressPercentage)
        assertTrue(progressData.isOnTrack)

        // When - Complete some micro tasks
        val microTasks = goalService.getMicroTasks(createdGoal.id).getOrThrow()
        val firstMicroTask = microTasks.first()
        val completedTask = goalService.completeMicroTask(firstMicroTask.id)
        assertTrue(completedTask.isSuccess)

        // When - Complete the goal
        val finalProgress = goalService.updateGoalProgress(createdGoal.id, Money.fromDollars(6000.0))
        assertTrue(finalProgress.isSuccess)
        val completedGoal = finalProgress.getOrThrow()
        assertTrue(completedGoal.isCompleted)

        // Then - Check for achievements
        val achievements = goalService.checkGoalAchievements("user123")
        assertTrue(achievements.isSuccess)
        val achievementList = achievements.getOrThrow()
        assertTrue(achievementList.any { it.achievementType == AchievementType.GOAL_COMPLETED })

        // When - Celebrate achievement
        val celebration = goalService.celebrateGoalAchievement(createdGoal.id)
        assertTrue(celebration.isSuccess)
        val celebrationData = celebration.getOrThrow()
        assertTrue(celebrationData.pointsAwarded > 0)
        assertTrue(celebrationData.nextSteps.isNotEmpty())
    }

    @Test
    fun `goal conflict detection and resolution workflow`() = runTest {
        // Given - Create multiple conflicting goals
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        val goal1 = FinancialGoal(
            id = "vacation-goal",
            userId = "user123",
            title = "Summer Vacation",
            targetAmount = Money.fromDollars(5000.0),
            currentAmount = Money.zero(),
            targetDate = today.plus(DatePeriod(months = 3)),
            priority = Priority.HIGH,
            goalType = GoalType.VACATION
        )
        
        val goal2 = FinancialGoal(
            id = "car-goal",
            userId = "user123",
            title = "New Car",
            targetAmount = Money.fromDollars(25000.0),
            currentAmount = Money.zero(),
            targetDate = today.plus(DatePeriod(months = 3)),
            priority = Priority.HIGH,
            goalType = GoalType.CAR_PURCHASE
        )

        // When - Create both goals
        goalService.createGoal(goal1)
        goalService.createGoal(goal2)

        // Then - Detect conflicts
        val conflictsResult = goalService.detectGoalConflicts("user123")
        assertTrue(conflictsResult.isSuccess)
        val conflicts = conflictsResult.getOrThrow()
        assertTrue(conflicts.isNotEmpty())
        
        val timelineConflict = conflicts.find { it.conflictType == ConflictType.TIMELINE_OVERLAP }
        assertNotNull(timelineConflict)
        
        val budgetConflict = conflicts.find { it.conflictType == ConflictType.BUDGET_COMPETITION }
        assertNotNull(budgetConflict)

        // When - Prioritize goals to resolve conflicts
        val prioritizationResult = goalService.prioritizeGoals("user123", listOf(goal2.id, goal1.id))
        assertTrue(prioritizationResult.isSuccess)
        val prioritizedGoals = prioritizationResult.getOrThrow()
        
        // Then - Verify priorities were updated
        assertEquals(Priority.CRITICAL, prioritizedGoals[0].priority) // Car goal
        assertEquals(Priority.HIGH, prioritizedGoals[1].priority) // Vacation goal

        // When - Resolve conflict by adjusting timeline
        val resolution = ConflictResolution(
            id = "timeline-adjustment",
            type = ResolutionType.ADJUST_TIMELINE,
            description = "Extend vacation goal timeline",
            impact = "Reduces timeline pressure",
            adjustments = listOf(
                GoalAdjustment(
                    goalId = goal1.id,
                    field = "targetDate",
                    oldValue = goal1.targetDate.toString(),
                    newValue = today.plus(DatePeriod(months = 6)).toString()
                )
            )
        )
        
        val resolutionResult = goalService.resolveGoalConflict(timelineConflict.id, resolution)
        assertTrue(resolutionResult.isSuccess)
    }

    @Test
    fun `micro task generation and completion workflow`() = runTest {
        // Given - Create a home purchase goal
        val homePurchaseGoal = FinancialGoal(
            id = "home-goal",
            userId = "user123",
            title = "First Home Purchase",
            targetAmount = Money.fromDollars(100000.0),
            currentAmount = Money.fromDollars(10000.0),
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 24)),
            priority = Priority.CRITICAL,
            goalType = GoalType.HOME_PURCHASE
        )

        // When - Create goal (should auto-generate micro tasks)
        val createResult = goalService.createGoal(homePurchaseGoal)
        assertTrue(createResult.isSuccess)
        val createdGoal = createResult.getOrThrow()

        // Then - Verify micro tasks were generated
        assertTrue(createdGoal.microTasks.isNotEmpty())
        val preApprovalTask = createdGoal.microTasks.find { it.title.contains("pre-approval") }
        assertNotNull(preApprovalTask)
        val downPaymentTask = createdGoal.microTasks.find { it.title.contains("down payment") }
        assertNotNull(downPaymentTask)

        // When - Generate additional micro tasks
        val additionalTasks = goalService.generateMicroTasks(createdGoal.id)
        assertTrue(additionalTasks.isSuccess)

        // When - Complete micro tasks in sequence
        val microTasks = goalService.getMicroTasks(createdGoal.id).getOrThrow()
        val sortedTasks = microTasks.sortedBy { it.dueDate }
        
        // Complete first task
        val firstTaskResult = goalService.completeMicroTask(sortedTasks.first().id)
        assertTrue(firstTaskResult.isSuccess)
        val completedFirstTask = firstTaskResult.getOrThrow()
        assertTrue(completedFirstTask.isCompleted)
        assertNotNull(completedFirstTask.completedAt)

        // Then - Verify progress tracking includes micro task completion
        val progressResult = goalService.getGoalProgress(createdGoal.id)
        assertTrue(progressResult.isSuccess)
        val progress = progressResult.getOrThrow()
        assertTrue(progress.completedMicroTasks > 0)
        assertEquals(microTasks.size, progress.totalMicroTasks)
    }

    @Test
    fun `goal insights and recommendations workflow`() = runTest {
        // Given - Create multiple goals with different states
        val goals = listOf(
            // Completed emergency fund
            FinancialGoal(
                id = "completed-emergency",
                userId = "user123",
                title = "Emergency Fund",
                targetAmount = Money.fromDollars(5000.0),
                currentAmount = Money.fromDollars(5000.0),
                targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 6)),
                priority = Priority.HIGH,
                goalType = GoalType.EMERGENCY_FUND
            ),
            // Behind schedule vacation goal
            FinancialGoal(
                id = "behind-vacation",
                userId = "user123",
                title = "Vacation Fund",
                targetAmount = Money.fromDollars(3000.0),
                currentAmount = Money.fromDollars(500.0),
                targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(days = 30)),
                priority = Priority.MEDIUM,
                goalType = GoalType.VACATION,
                createdAt = Clock.System.now().minus(60.days)
            ),
            // On-track car purchase
            FinancialGoal(
                id = "ontrack-car",
                userId = "user123",
                title = "Car Purchase",
                targetAmount = Money.fromDollars(20000.0),
                currentAmount = Money.fromDollars(10000.0),
                targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 12)),
                priority = Priority.HIGH,
                goalType = GoalType.CAR_PURCHASE,
                createdAt = Clock.System.now().minus(180.days)
            )
        )

        // When - Create all goals
        goals.forEach { goal ->
            goalService.createGoal(goal)
        }

        // Then - Get comprehensive insights
        val insightsResult = goalService.getGoalInsights("user123")
        assertTrue(insightsResult.isSuccess)
        val insights = insightsResult.getOrThrow()
        
        assertEquals(3, insights.totalGoals)
        assertEquals(2, insights.activeGoals) // Completed goal becomes inactive
        assertEquals(1, insights.completedGoals)
        assertTrue(insights.overallProgress > 0.0)
        assertEquals(1, insights.goalsOnTrack) // Car purchase goal
        assertEquals(1, insights.goalsOffTrack) // Vacation goal
        assertTrue(insights.topPerformingGoalTypes.isNotEmpty())

        // When - Get recommendations
        val recommendationsResult = goalService.getGoalRecommendations("user123")
        assertTrue(recommendationsResult.isSuccess)
        val recommendations = recommendationsResult.getOrThrow()
        
        // Then - Verify appropriate recommendations
        assertTrue(recommendations.isNotEmpty())
        
        // Should recommend increasing contributions for behind-schedule goal
        val increaseContributionRec = recommendations.find { 
            it.type == RecommendationType.INCREASE_CONTRIBUTIONS 
        }
        assertNotNull(increaseContributionRec)
        
        // Should recommend extending timeline for urgent goal
        val extendTimelineRec = recommendations.find { 
            it.type == RecommendationType.EXTEND_TIMELINE 
        }
        assertNotNull(extendTimelineRec)
    }

    @Test
    fun `goal projection and achievement tracking workflow`() = runTest {
        // Given - Create a goal with some progress
        val savingsGoal = FinancialGoal(
            id = "savings-goal",
            userId = "user123",
            title = "General Savings",
            targetAmount = Money.fromDollars(12000.0),
            currentAmount = Money.fromDollars(4000.0),
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 8)),
            priority = Priority.MEDIUM,
            goalType = GoalType.GENERAL_SAVINGS,
            createdAt = Clock.System.now().minus(120.days)
        )

        // When - Create goal
        goalService.createGoal(savingsGoal)

        // Then - Get projection
        val projectionResult = goalService.getGoalProjection(savingsGoal.id)
        assertTrue(projectionResult.isSuccess)
        val projection = projectionResult.getOrThrow()
        
        assertNotNull(projection.projectedCompletionDate)
        assertTrue(projection.confidenceLevel > 0.0)
        assertTrue(projection.requiredWeeklyAmount.isPositive)
        assertTrue(projection.requiredMonthlyAmount.isPositive)

        // When - Make significant progress to trigger milestone
        goalService.updateGoalProgress(savingsGoal.id, Money.fromDollars(6000.0)) // 50% milestone
        goalService.updateGoalProgress(savingsGoal.id, Money.fromDollars(9000.0)) // 75% milestone

        // Then - Check for milestone achievements
        val achievementsResult = goalService.checkGoalAchievements("user123")
        assertTrue(achievementsResult.isSuccess)
        val achievements = achievementsResult.getOrThrow()
        
        val milestoneAchievements = achievements.filter { 
            it.achievementType == AchievementType.MILESTONE_REACHED 
        }
        assertTrue(milestoneAchievements.isNotEmpty())

        // When - Complete goal early
        goalService.updateGoalProgress(savingsGoal.id, Money.fromDollars(12000.0))

        // Then - Check for early completion achievement
        val finalAchievements = goalService.checkGoalAchievements("user123")
        assertTrue(finalAchievements.isSuccess)
        val finalAchievementList = finalAchievements.getOrThrow()
        
        val completionAchievement = finalAchievementList.find { 
            it.achievementType == AchievementType.GOAL_COMPLETED 
        }
        assertNotNull(completionAchievement)

        // When - Get next step suggestions
        val nextStepsResult = goalService.getNextStepSuggestions(savingsGoal.id)
        assertTrue(nextStepsResult.isSuccess)
        val nextSteps = nextStepsResult.getOrThrow()
        
        // Then - Verify appropriate next steps
        assertTrue(nextSteps.any { it.type == NextStepType.CELEBRATE_ACHIEVEMENT })
        assertTrue(nextSteps.any { it.type == NextStepType.CREATE_NEW_GOAL })
    }

    @Test
    fun `real-time goal observation workflow`() = runTest {
        // Given - Create a goal
        val observedGoal = FinancialGoal(
            id = "observed-goal",
            userId = "user123",
            title = "Observed Goal",
            targetAmount = Money.fromDollars(5000.0),
            currentAmount = Money.zero(),
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 6)),
            priority = Priority.MEDIUM,
            goalType = GoalType.GENERAL_SAVINGS
        )

        goalService.createGoal(observedGoal)

        // When - Observe goal progress
        val progressFlow = goalService.observeGoalProgress(observedGoal.id)
        val userGoalsFlow = goalService.observeUserGoals("user123")

        // Then - Verify flows emit initial values
        // Note: In a real test, you would collect from the flows and verify emissions
        // This is a simplified verification that the flows are created without error
        assertNotNull(progressFlow)
        assertNotNull(userGoalsFlow)

        // When - Update goal progress
        goalService.updateGoalProgress(observedGoal.id, Money.fromDollars(1000.0))

        // Then - Flows should emit updated values
        // In a real implementation, you would verify the flow emissions
        val updatedGoal = goalService.getGoal(observedGoal.id).getOrThrow()
        assertNotNull(updatedGoal)
        assertEquals(Money.fromDollars(1000.0), updatedGoal.currentAmount)
    }
}