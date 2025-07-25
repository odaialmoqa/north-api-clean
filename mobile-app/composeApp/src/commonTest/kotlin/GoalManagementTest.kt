package com.north.mobile.ui.goals

import com.north.mobile.domain.model.*
import com.north.mobile.ui.goals.model.*
import kotlinx.datetime.*
import kotlin.test.*

class GoalManagementTest {

    @Test
    fun testGoalCreationStateValidation() {
        // Test invalid state
        val invalidState = GoalCreationState()
        assertFalse(invalidState.isValid)
        
        // Test valid state
        val validState = GoalCreationState(
            title = "Emergency Fund",
            targetAmount = "10000",
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 6))
        )
        assertTrue(validState.isValid)
    }

    @Test
    fun testGoalValidation() {
        // Test title validation
        assertNull(GoalValidation.validateTitle("Valid Title"))
        assertNotNull(GoalValidation.validateTitle(""))
        assertNotNull(GoalValidation.validateTitle("AB"))
        
        // Test amount validation
        assertNull(GoalValidation.validateTargetAmount("1000"))
        assertNotNull(GoalValidation.validateTargetAmount(""))
        assertNotNull(GoalValidation.validateTargetAmount("0"))
        assertNotNull(GoalValidation.validateTargetAmount("invalid"))
        
        // Test date validation
        val futureDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(days = 30))
        val pastDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(DatePeriod(days = 1))
        
        assertNull(GoalValidation.validateTargetDate(futureDate))
        assertNotNull(GoalValidation.validateTargetDate(pastDate))
        assertNotNull(GoalValidation.validateTargetDate(null))
    }

    @Test
    fun testGoalEditStateChanges() {
        val originalGoal = createTestGoal()
        
        val editState = GoalEditState(
            originalGoal = originalGoal,
            title = originalGoal.title,
            description = originalGoal.description ?: "",
            targetAmount = originalGoal.targetAmount.amount.toString(),
            targetDate = originalGoal.targetDate,
            priority = originalGoal.priority
        )
        
        // Initially no changes
        assertFalse(editState.hasChanges)
        
        // After changing title
        val modifiedState = editState.copy(title = "New Title")
        assertTrue(modifiedState.hasChanges)
    }

    @Test
    fun testGoalProgressCalculations() {
        val goal = createTestGoal()
        
        // Test progress percentage
        assertEquals(50.0, goal.progressPercentage, 0.1)
        
        // Test remaining amount
        assertEquals(Money.fromDollars(5000.0), goal.remainingAmount)
        
        // Test completion status
        assertFalse(goal.isCompleted)
        
        // Test completed goal
        val completedGoal = goal.copy(currentAmount = Money.fromDollars(10000.0))
        assertTrue(completedGoal.isCompleted)
        assertEquals(100.0, completedGoal.progressPercentage, 0.1)
    }

    @Test
    fun testGoalTimelineCalculations() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val targetDate = today.plus(DatePeriod(days = 100))
        
        val goal = FinancialGoal(
            id = "test-goal",
            userId = "test-user",
            title = "Test Goal",
            targetAmount = Money.fromDollars(1000.0),
            currentAmount = Money.fromDollars(200.0),
            targetDate = targetDate,
            priority = Priority.MEDIUM,
            goalType = GoalType.GENERAL_SAVINGS,
            createdAt = Clock.System.now()
        )
        
        assertEquals(100L, goal.daysRemaining)
        assertFalse(goal.isOverdue)
        
        // Test weekly and monthly targets
        assertTrue(goal.weeklyTargetAmount.amount > 0.0)
        assertTrue(goal.monthlyTargetAmount.amount > 0.0)
    }

    @Test
    fun testMicroTaskValidation() {
        val validMicroTask = MicroTask(
            id = "task-1",
            goalId = "goal-1",
            title = "Save $100",
            description = "Weekly savings target",
            targetAmount = Money.fromDollars(100.0)
        )
        
        val validationResult = validMicroTask.validate()
        assertTrue(validationResult is com.north.mobile.domain.validation.ValidationResult.Valid)
        
        // Test invalid micro task
        val invalidMicroTask = MicroTask(
            id = "",
            goalId = "goal-1",
            title = "",
            description = "",
            targetAmount = Money.fromDollars(-100.0)
        )
        
        val invalidResult = invalidMicroTask.validate()
        assertTrue(invalidResult is com.north.mobile.domain.validation.ValidationResult.Invalid)
    }

    @Test
    fun testGoalTypeProperties() {
        assertEquals("Emergency Fund", GoalType.EMERGENCY_FUND.displayName)
        assertEquals("🛡️", GoalType.EMERGENCY_FUND.icon)
        assertEquals("Vacation", GoalType.VACATION.displayName)
        assertEquals("🏖️", GoalType.VACATION.icon)
    }

    @Test
    fun testPriorityOrdering() {
        assertEquals(4, Priority.CRITICAL.sortOrder)
        assertEquals(3, Priority.HIGH.sortOrder)
        assertEquals(2, Priority.MEDIUM.sortOrder)
        assertEquals(1, Priority.LOW.sortOrder)
        
        // Test sorting
        val priorities = listOf(Priority.LOW, Priority.CRITICAL, Priority.MEDIUM, Priority.HIGH)
        val sorted = priorities.sortedByDescending { it.sortOrder }
        assertEquals(Priority.CRITICAL, sorted[0])
        assertEquals(Priority.HIGH, sorted[1])
        assertEquals(Priority.MEDIUM, sorted[2])
        assertEquals(Priority.LOW, sorted[3])
    }

    @Test
    fun testGoalDashboardStateInitialization() {
        val dashboardState = GoalDashboardState()
        
        assertTrue(dashboardState.goals.isEmpty())
        assertFalse(dashboardState.isLoading)
        assertNull(dashboardState.error)
        assertNull(dashboardState.selectedGoal)
        assertFalse(dashboardState.showCreateGoal)
        assertFalse(dashboardState.showEditGoal)
    }

    private fun createTestGoal(): FinancialGoal {
        return FinancialGoal(
            id = "test-goal-1",
            userId = "test-user-1",
            title = "Emergency Fund",
            description = "Build emergency fund for 6 months expenses",
            targetAmount = Money.fromDollars(10000.0),
            currentAmount = Money.fromDollars(5000.0),
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 6)),
            priority = Priority.HIGH,
            goalType = GoalType.EMERGENCY_FUND,
            microTasks = listOf(
                MicroTask(
                    id = "task-1",
                    goalId = "test-goal-1",
                    title = "Save first $1000",
                    description = "Initial emergency fund milestone",
                    targetAmount = Money.fromDollars(1000.0),
                    isCompleted = true
                ),
                MicroTask(
                    id = "task-2",
                    goalId = "test-goal-1",
                    title = "Save next $2000",
                    description = "Second milestone",
                    targetAmount = Money.fromDollars(2000.0),
                    isCompleted = false
                )
            ),
            createdAt = Clock.System.now().minus(kotlin.time.Duration.parse("P30D"))
        )
    }
}

/**
 * Test for goal celebration functionality
 */
class GoalCelebrationTest {

    @Test
    fun testCelebrationStateInitialization() {
        val celebration = com.north.mobile.data.goal.GoalCelebration(
            goalId = "test-goal",
            celebrationType = com.north.mobile.data.goal.CelebrationType.CONFETTI,
            title = "Goal Achieved!",
            message = "Congratulations on completing your goal!",
            animationType = "confetti_burst",
            soundEffect = "celebration.mp3",
            badgeEarned = "Goal Achiever",
            pointsAwarded = 100,
            nextSteps = emptyList()
        )
        
        val celebrationState = GoalCelebrationState(
            celebration = celebration,
            isVisible = true,
            animationProgress = 0.5f
        )
        
        assertTrue(celebrationState.isVisible)
        assertEquals(0.5f, celebrationState.animationProgress)
        assertEquals("Goal Achieved!", celebrationState.celebration.title)
        assertEquals(100, celebrationState.celebration.pointsAwarded)
    }

    @Test
    fun testNextStepSuggestions() {
        val nextStep = com.north.mobile.data.goal.NextStepSuggestion(
            id = "next-1",
            type = com.north.mobile.data.goal.NextStepType.CREATE_NEW_GOAL,
            title = "Create Another Goal",
            description = "Set up your next financial milestone",
            actionText = "Get Started",
            priority = Priority.MEDIUM
        )
        
        assertEquals("Create Another Goal", nextStep.title)
        assertEquals(com.north.mobile.data.goal.NextStepType.CREATE_NEW_GOAL, nextStep.type)
        assertEquals(Priority.MEDIUM, nextStep.priority)
    }
}

/**
 * Test for goal actions and state management
 */
class GoalActionTest {

    @Test
    fun testGoalActions() {
        val loadAction = GoalAction.LoadGoals
        val createAction = GoalAction.CreateNewGoal
        val editAction = GoalAction.EditGoal(createTestGoal())
        val deleteAction = GoalAction.DeleteGoal("goal-id")
        
        assertTrue(loadAction is GoalAction.LoadGoals)
        assertTrue(createAction is GoalAction.CreateNewGoal)
        assertTrue(editAction is GoalAction.EditGoal)
        assertTrue(deleteAction is GoalAction.DeleteGoal)
        
        assertEquals("goal-id", (deleteAction as GoalAction.DeleteGoal).goalId)
    }

    @Test
    fun testGoalCreationActions() {
        val titleAction = GoalCreationAction.UpdateTitle("New Title")
        val amountAction = GoalCreationAction.UpdateTargetAmount("5000")
        val createAction = GoalCreationAction.CreateGoal
        val cancelAction = GoalCreationAction.Cancel
        
        assertEquals("New Title", (titleAction as GoalCreationAction.UpdateTitle).title)
        assertEquals("5000", (amountAction as GoalCreationAction.UpdateTargetAmount).amount)
        assertTrue(createAction is GoalCreationAction.CreateGoal)
        assertTrue(cancelAction is GoalCreationAction.Cancel)
    }

    @Test
    fun testGoalEditActions() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val futureDate = today.plus(DatePeriod(months = 3))
        
        val titleAction = GoalEditAction.UpdateTitle("Updated Title")
        val dateAction = GoalEditAction.UpdateTargetDate(futureDate)
        val priorityAction = GoalEditAction.UpdatePriority(Priority.HIGH)
        val saveAction = GoalEditAction.SaveChanges
        val cancelAction = GoalEditAction.Cancel
        
        assertEquals("Updated Title", (titleAction as GoalEditAction.UpdateTitle).title)
        assertEquals(futureDate, (dateAction as GoalEditAction.UpdateTargetDate).date)
        assertEquals(Priority.HIGH, (priorityAction as GoalEditAction.UpdatePriority).priority)
        assertTrue(saveAction is GoalEditAction.SaveChanges)
        assertTrue(cancelAction is GoalEditAction.Cancel)
    }

    private fun createTestGoal(): FinancialGoal {
        return FinancialGoal(
            id = "test-goal",
            userId = "test-user",
            title = "Test Goal",
            targetAmount = Money.fromDollars(1000.0),
            targetDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(months = 6)),
            priority = Priority.MEDIUM,
            goalType = GoalType.GENERAL_SAVINGS,
            createdAt = Clock.System.now()
        )
    }
}