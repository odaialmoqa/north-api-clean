package com.north.mobile.domain.model

import com.north.mobile.domain.validation.ValidationResult
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FinancialGoalTest {
    
    private fun createValidGoal() = FinancialGoal(
        id = "goal123",
        userId = "user123",
        title = "Emergency Fund",
        description = "Build a 6-month emergency fund",
        targetAmount = Money.fromDollars(10000.00),
        currentAmount = Money.fromDollars(2500.00),
        targetDate = LocalDate(2025, 12, 31),
        priority = Priority.HIGH,
        goalType = GoalType.EMERGENCY_FUND
    )
    
    @Test
    fun testValidGoal() {
        val goal = createValidGoal()
        val validation = goal.validate()
        assertTrue(validation.isValid)
    }
    
    @Test
    fun testBlankGoalId() {
        val goal = createValidGoal().copy(id = "")
        val validation = goal.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("Goal ID") })
    }
    
    @Test
    fun testBlankUserId() {
        val goal = createValidGoal().copy(userId = "")
        val validation = goal.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("User ID") })
    }
    
    @Test
    fun testShortTitle() {
        val goal = createValidGoal().copy(title = "Hi")
        val validation = goal.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("title must be at least 3") })
    }
    
    @Test
    fun testNegativeTargetAmount() {
        val goal = createValidGoal().copy(targetAmount = Money.fromDollars(-1000.00))
        val validation = goal.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("Target amount must be positive") })
    }
    
    @Test
    fun testNegativeCurrentAmount() {
        val goal = createValidGoal().copy(currentAmount = Money.fromDollars(-500.00))
        val validation = goal.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("Current amount cannot be negative") })
    }
    
    @Test
    fun testCurrencyMismatch() {
        val goal = createValidGoal().copy(
            targetAmount = Money.fromDollars(10000.00, Currency.CAD),
            currentAmount = Money.fromDollars(2500.00, Currency.USD)
        )
        val validation = goal.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("same currency") })
    }
    
    @Test
    fun testPastTargetDate() {
        val goal = createValidGoal().copy(targetDate = LocalDate(2020, 1, 1))
        val validation = goal.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("Target date must be in the future") })
    }
    
    @Test
    fun testProgressCalculations() {
        val goal = createValidGoal()
        assertEquals(25.0, goal.progressPercentage) // 2500/10000 * 100
        assertEquals(Money.fromDollars(7500.00), goal.remainingAmount)
        assertFalse(goal.isCompleted)
        
        val completedGoal = goal.copy(currentAmount = Money.fromDollars(10000.00))
        assertEquals(100.0, completedGoal.progressPercentage)
        assertEquals(Money.zero(), completedGoal.remainingAmount)
        assertTrue(completedGoal.isCompleted)
        
        val overAchievedGoal = goal.copy(currentAmount = Money.fromDollars(12000.00))
        assertEquals(100.0, overAchievedGoal.progressPercentage) // Capped at 100%
        assertEquals(Money.zero(), overAchievedGoal.remainingAmount)
        assertTrue(overAchievedGoal.isCompleted)
    }
    
    @Test
    fun testDaysRemaining() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val futureDate = LocalDate(today.year + 1, today.monthNumber, today.dayOfMonth)
        val goal = createValidGoal().copy(targetDate = futureDate)
        
        assertTrue(goal.daysRemaining > 0)
        assertFalse(goal.isOverdue)
        
        val pastGoal = goal.copy(
            targetDate = LocalDate(today.year - 1, today.monthNumber, today.dayOfMonth),
            currentAmount = Money.fromDollars(5000.00) // Not completed
        )
        assertTrue(pastGoal.daysRemaining < 0)
        assertTrue(pastGoal.isOverdue)
        
        val pastCompletedGoal = pastGoal.copy(currentAmount = Money.fromDollars(10000.00))
        assertTrue(pastCompletedGoal.daysRemaining < 0)
        assertFalse(pastCompletedGoal.isOverdue) // Completed goals are not overdue
    }
    
    @Test
    fun testTargetAmountCalculations() {
        val goal = createValidGoal()
        val weeklyTarget = goal.weeklyTargetAmount
        val monthlyTarget = goal.monthlyTargetAmount
        
        assertTrue(weeklyTarget.isPositive)
        assertTrue(monthlyTarget.isPositive)
        assertTrue(monthlyTarget > weeklyTarget) // Monthly should be larger than weekly
    }
    
    @Test
    fun testPriorityDisplayAndSorting() {
        assertEquals("Low", Priority.LOW.displayName)
        assertEquals("Medium", Priority.MEDIUM.displayName)
        assertEquals("High", Priority.HIGH.displayName)
        assertEquals("Critical", Priority.CRITICAL.displayName)
        
        assertEquals(1, Priority.LOW.sortOrder)
        assertEquals(2, Priority.MEDIUM.sortOrder)
        assertEquals(3, Priority.HIGH.sortOrder)
        assertEquals(4, Priority.CRITICAL.sortOrder)
    }
    
    @Test
    fun testGoalTypeDisplayAndIcons() {
        assertEquals("Emergency Fund", GoalType.EMERGENCY_FUND.displayName)
        assertEquals("Vacation", GoalType.VACATION.displayName)
        assertEquals("Car Purchase", GoalType.CAR_PURCHASE.displayName)
        assertEquals("Home Purchase", GoalType.HOME_PURCHASE.displayName)
        
        assertEquals("🛡️", GoalType.EMERGENCY_FUND.icon)
        assertEquals("🏖️", GoalType.VACATION.icon)
        assertEquals("🚗", GoalType.CAR_PURCHASE.icon)
        assertEquals("🏠", GoalType.HOME_PURCHASE.icon)
    }
    
    @Test
    fun testMicroTaskValidation() {
        val validMicroTask = MicroTask(
            id = "task123",
            goalId = "goal123",
            title = "Save $500",
            description = "Save $500 this month",
            targetAmount = Money.fromDollars(500.00)
        )
        assertTrue(validMicroTask.validate().isValid)
        
        val invalidMicroTask = validMicroTask.copy(title = "Hi") // Too short
        assertTrue(invalidMicroTask.validate().isInvalid)
        
        val negativeAmountTask = validMicroTask.copy(targetAmount = Money.fromDollars(-100.00))
        assertTrue(negativeAmountTask.validate().isInvalid)
        
        val completedTaskWithoutTimestamp = validMicroTask.copy(isCompleted = true, completedAt = null)
        assertTrue(completedTaskWithoutTimestamp.validate().isInvalid)
    }
    
    @Test
    fun testMicroTaskOverdue() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val pastDate = LocalDate(today.year - 1, today.monthNumber, today.dayOfMonth)
        val futureDate = LocalDate(today.year + 1, today.monthNumber, today.dayOfMonth)
        
        val overdueMicroTask = MicroTask(
            id = "task123",
            goalId = "goal123",
            title = "Save $500",
            description = "Save $500 this month",
            targetAmount = Money.fromDollars(500.00),
            dueDate = pastDate,
            isCompleted = false
        )
        assertTrue(overdueMicroTask.isOverdue)
        
        val futureMicroTask = overdueMicroTask.copy(dueDate = futureDate)
        assertFalse(futureMicroTask.isOverdue)
        
        val completedOverdueMicroTask = overdueMicroTask.copy(isCompleted = true)
        assertFalse(completedOverdueMicroTask.isOverdue) // Completed tasks are not overdue
        
        val noDueDateTask = overdueMicroTask.copy(dueDate = null)
        assertFalse(noDueDateTask.isOverdue) // No due date means not overdue
    }
}