package com.north.mobile.data.goal

import com.north.mobile.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

class GoalServiceTest {

    private lateinit var mockRepository: MockGoalRepository
    private lateinit var goalService: GoalService

    @BeforeTest
    fun setup() {
        mockRepository = MockGoalRepository()
        goalService = GoalServiceImpl(mockRepository)
    }

    @Test
    fun `createGoal should validate goal and generate micro tasks`() = runTest {
        // Given
        val goal = createTestGoal()

        // When
        val result = goalService.createGoal(goal)

        // Then
        assertTrue(result.isSuccess)
        val createdGoal = result.getOrThrow()
        assertTrue(createdGoal.microTasks.isNotEmpty())
        assertEquals(goal.title, createdGoal.title)
    }

    @Test
    fun `createGoal should fail with invalid goal`() = runTest {
        // Given
        val invalidGoal = createTestGoal().copy(
            title = "", // Invalid empty title
            targetAmount = Money.fromDollars(-100.0) // Invalid negative amount
        )

        // When
        val result = goalService.createGoal(invalidGoal)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `updateGoalProgress should update current amount`() = runTest {
        // Given
        val goal = createTestGoal()
        mockRepository.insertGoal(goal)
        val newAmount = Money.fromDollars(2500.0)

        // When
        val result = goalService.updateGoalProgress(goal.id, newAmount)

        // Then
        assertTrue(result.isSuccess)
        val updatedGoal = result.getOrThrow()
        assertEquals(newAmount, updatedGoal.currentAmount)
    }

    @Test
    fun `getGoalProgress should return detailed progress information`() = runTest {
        // Given
        val goal = createTestGoal().copy(
            currentAmount = Money.fromDollars(2500.0),
            targetAmount = Money.fromDollars(10000.0)
        )
        mockRepository.insertGoal(goal)

        // When
        val result = goalService.getGoalProgress(goal.id)

        // Then
        assertTrue(result.isSuccess)
        val progress = result.getOrThrow()
        assertEquals(25.0, progress.progressPercentage)
        assertEquals(Money.fromDollars(7500.0), progress.remainingAmount)
        assertTrue(progress.daysRemaining > 0)
    }

    @Test
    fun `getGoalProjection should calculate realistic completion date`() = runTest {
        // Given
        val goal = createTestGoal().copy(
            currentAmount = Money.fromDollars(5000.0),
            targetAmount = Money.fromDollars(10000.0),
            createdAt = Clock.System.now().minus(30.days)
        )
        mockRepository.insertGoal(goal)

        // When
        val result = goalService.getGoalProjection(goal.id)

        // Then
        assertTrue(result.isSuccess)
        val projection = result.getOrThrow()
        assertNotNull(projection.projectedCompletionDate)
        assertTrue(projection.confidenceLevel > 0.0)
        assertTrue(projection.requiredWeeklyAmount.isPositive)
    }

    @Test
    fun `generateMicroTasks should create appropriate tasks for goal type`() = runTest {
        // Given
        val emergencyFundGoal = createTestGoal().copy(
            goalType = GoalType.EMERGENCY_FUND,
            targetAmount = Money.fromDollars(6000.0)
        )
        mockRepository.insertGoal(emergencyFundGoal)

        // When
        val result = goalService.generateMicroTasks(emergencyFundGoal.id)

        // Then
        assertTrue(result.isSuccess)
        val microTasks = result.getOrThrow()
        assertTrue(microTasks.isNotEmpty())
        assertTrue(microTasks.all { it.goalId == emergencyFundGoal.id })
        assertTrue(microTasks.any { it.title.contains("Month") })
    }

    @Test
    fun `completeMicroTask should mark task as completed`() = runTest {
        // Given
        val goal = createTestGoal()
        val microTask = createTestMicroTask(goal.id)
        mockRepository.insertGoal(goal)
        mockRepository.insertMicroTask(microTask)

        // When
        val result = goalService.completeMicroTask(microTask.id)

        // Then
        assertTrue(result.isSuccess)
        val completedTask = result.getOrThrow()
        assertTrue(completedTask.isCompleted)
        assertNotNull(completedTask.completedAt)
    }

    @Test
    fun `detectGoalConflicts should identify timeline conflicts`() = runTest {
        // Given
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val goal1 = createTestGoal().copy(
            id = "goal1",
            title = "Emergency Fund",
            priority = Priority.HIGH,
            targetDate = today.plus(DatePeriod(days = 30))
        )
        val goal2 = createTestGoal().copy(
            id = "goal2",
            title = "Vacation Fund",
            priority = Priority.HIGH,
            targetDate = today.plus(DatePeriod(days = 25))
        )
        
        mockRepository.insertGoal(goal1)
        mockRepository.insertGoal(goal2)

        // When
        val result = goalService.detectGoalConflicts("user1")

        // Then
        assertTrue(result.isSuccess)
        val conflicts = result.getOrThrow()
        assertTrue(conflicts.isNotEmpty())
        assertTrue(conflicts.any { it.conflictType == ConflictType.TIMELINE_OVERLAP })
    }

    @Test
    fun `detectGoalConflicts should identify budget conflicts`() = runTest {
        // Given
        val goal1 = createTestGoal().copy(
            id = "goal1",
            targetAmount = Money.fromDollars(50000.0), // High amount requiring high weekly savings
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 6))
        )
        val goal2 = createTestGoal().copy(
            id = "goal2",
            targetAmount = Money.fromDollars(30000.0), // Another high amount
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 6))
        )
        
        mockRepository.insertGoal(goal1)
        mockRepository.insertGoal(goal2)

        // When
        val result = goalService.detectGoalConflicts("user1")

        // Then
        assertTrue(result.isSuccess)
        val conflicts = result.getOrThrow()
        assertTrue(conflicts.any { it.conflictType == ConflictType.BUDGET_COMPETITION })
    }

    @Test
    fun `prioritizeGoals should update goal priorities based on order`() = runTest {
        // Given
        val goal1 = createTestGoal().copy(id = "goal1", priority = Priority.LOW)
        val goal2 = createTestGoal().copy(id = "goal2", priority = Priority.LOW)
        val goal3 = createTestGoal().copy(id = "goal3", priority = Priority.LOW)
        
        mockRepository.insertGoal(goal1)
        mockRepository.insertGoal(goal2)
        mockRepository.insertGoal(goal3)

        // When
        val result = goalService.prioritizeGoals("user1", listOf("goal1", "goal2", "goal3"))

        // Then
        assertTrue(result.isSuccess)
        val prioritizedGoals = result.getOrThrow()
        assertEquals(Priority.CRITICAL, prioritizedGoals[0].priority)
        assertEquals(Priority.HIGH, prioritizedGoals[1].priority)
        assertEquals(Priority.MEDIUM, prioritizedGoals[2].priority)
    }

    @Test
    fun `checkGoalAchievements should detect completed goals`() = runTest {
        // Given
        val completedGoal = createTestGoal().copy(
            currentAmount = Money.fromDollars(10000.0),
            targetAmount = Money.fromDollars(10000.0)
        )
        mockRepository.insertGoal(completedGoal)

        // When
        val result = goalService.checkGoalAchievements("user1")

        // Then
        assertTrue(result.isSuccess)
        val achievements = result.getOrThrow()
        assertTrue(achievements.isNotEmpty())
        assertTrue(achievements.any { it.achievementType == AchievementType.GOAL_COMPLETED })
    }

    @Test
    fun `checkGoalAchievements should detect milestone achievements`() = runTest {
        // Given
        val halfwayGoal = createTestGoal().copy(
            currentAmount = Money.fromDollars(5000.0),
            targetAmount = Money.fromDollars(10000.0)
        )
        mockRepository.insertGoal(halfwayGoal)

        // When
        val result = goalService.checkGoalAchievements("user1")

        // Then
        assertTrue(result.isSuccess)
        val achievements = result.getOrThrow()
        assertTrue(achievements.any { it.achievementType == AchievementType.MILESTONE_REACHED })
    }

    @Test
    fun `celebrateGoalAchievement should return appropriate celebration`() = runTest {
        // Given
        val goal = createTestGoal()
        mockRepository.insertGoal(goal)

        // When
        val result = goalService.celebrateGoalAchievement(goal.id)

        // Then
        assertTrue(result.isSuccess)
        val celebration = result.getOrThrow()
        assertEquals(goal.id, celebration.goalId)
        assertTrue(celebration.title.contains("Goal Achieved"))
        assertTrue(celebration.pointsAwarded > 0)
        assertNotNull(celebration.animationType)
    }

    @Test
    fun `getNextStepSuggestions should provide relevant suggestions for completed goal`() = runTest {
        // Given
        val completedGoal = createTestGoal().copy(
            currentAmount = Money.fromDollars(10000.0),
            targetAmount = Money.fromDollars(10000.0)
        )
        mockRepository.insertGoal(completedGoal)

        // When
        val result = goalService.getNextStepSuggestions(completedGoal.id)

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrThrow()
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.type == NextStepType.CELEBRATE_ACHIEVEMENT })
        assertTrue(suggestions.any { it.type == NextStepType.CREATE_NEW_GOAL })
    }

    @Test
    fun `getGoalInsights should provide comprehensive analytics`() = runTest {
        // Given
        val goal1 = createTestGoal().copy(id = "goal1", goalType = GoalType.EMERGENCY_FUND)
        val goal2 = createTestGoal().copy(
            id = "goal2", 
            goalType = GoalType.VACATION,
            currentAmount = Money.fromDollars(10000.0),
            targetAmount = Money.fromDollars(10000.0)
        )
        
        mockRepository.insertGoal(goal1)
        mockRepository.insertGoal(goal2)

        // When
        val result = goalService.getGoalInsights("user1")

        // Then
        assertTrue(result.isSuccess)
        val insights = result.getOrThrow()
        assertEquals(2, insights.totalGoals)
        assertEquals(1, insights.completedGoals)
        assertTrue(insights.overallProgress > 0.0)
        assertTrue(insights.topPerformingGoalTypes.isNotEmpty())
    }

    @Test
    fun `getGoalRecommendations should provide actionable recommendations`() = runTest {
        // Given
        val behindScheduleGoal = createTestGoal().copy(
            currentAmount = Money.fromDollars(1000.0),
            targetAmount = Money.fromDollars(10000.0),
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(days = 30)),
            createdAt = Clock.System.now().minus(60.days)
        )
        mockRepository.insertGoal(behindScheduleGoal)

        // When
        val result = goalService.getGoalRecommendations("user1")

        // Then
        assertTrue(result.isSuccess)
        val recommendations = result.getOrThrow()
        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.any { it.type == RecommendationType.INCREASE_CONTRIBUTIONS })
    }

    @Test
    fun `getGoalRecommendations should suggest emergency fund if missing`() = runTest {
        // Given
        val vacationGoal = createTestGoal().copy(goalType = GoalType.VACATION)
        mockRepository.insertGoal(vacationGoal)

        // When
        val result = goalService.getGoalRecommendations("user1")

        // Then
        assertTrue(result.isSuccess)
        val recommendations = result.getOrThrow()
        assertTrue(recommendations.any { it.type == RecommendationType.CREATE_EMERGENCY_FUND })
    }

    // Helper methods

    private fun createTestGoal(): FinancialGoal {
        return FinancialGoal(
            id = "test-goal-1",
            userId = "user1",
            title = "Emergency Fund",
            description = "Build emergency fund for 6 months of expenses",
            targetAmount = Money.fromDollars(10000.0),
            currentAmount = Money.fromDollars(1000.0),
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 12)),
            priority = Priority.HIGH,
            goalType = GoalType.EMERGENCY_FUND,
            microTasks = emptyList(),
            isActive = true,
            createdAt = Clock.System.now().minus(30.days)
        )
    }

    private fun createTestMicroTask(goalId: String): MicroTask {
        return MicroTask(
            id = "micro-task-1",
            goalId = goalId,
            title = "Save $500 this month",
            description = "Set aside $500 for emergency fund",
            targetAmount = Money.fromDollars(500.0),
            isCompleted = false,
            dueDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(days = 30))
        )
    }
}