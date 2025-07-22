package com.north.mobile.domain.model

import com.north.mobile.domain.validation.ValidationResult
import com.north.mobile.domain.validation.ValidationUtils
import com.north.mobile.domain.validation.combine
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@Serializable
data class FinancialGoal(
    val id: String,
    val userId: String,
    val title: String,
    val description: String? = null,
    val targetAmount: Money,
    val currentAmount: Money = Money.zero(),
    val targetDate: LocalDate,
    val priority: Priority,
    val goalType: GoalType,
    val microTasks: List<MicroTask> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: kotlinx.datetime.Instant = Clock.System.now()
) {
    fun validate(): ValidationResult {
        val validations = mutableListOf<ValidationResult>()
        
        // Validate required fields
        validations.add(
            if (id.isNotBlank()) ValidationResult.Valid 
            else ValidationResult.Invalid("Goal ID cannot be blank")
        )
        
        validations.add(
            if (userId.isNotBlank()) ValidationResult.Valid 
            else ValidationResult.Invalid("User ID cannot be blank")
        )
        
        validations.add(
            if (ValidationUtils.isValidName(title, 3)) ValidationResult.Valid 
            else ValidationResult.Invalid("Goal title must be at least 3 characters")
        )
        
        // Validate amounts
        validations.add(
            if (targetAmount.isPositive) ValidationResult.Valid 
            else ValidationResult.Invalid("Target amount must be positive")
        )
        
        validations.add(
            if (!currentAmount.isNegative) ValidationResult.Valid 
            else ValidationResult.Invalid("Current amount cannot be negative")
        )
        
        validations.add(
            if (targetAmount.currency == currentAmount.currency) ValidationResult.Valid 
            else ValidationResult.Invalid("Target and current amounts must have the same currency")
        )
        
        // Validate target date is in the future
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        validations.add(
            if (targetDate > today) ValidationResult.Valid 
            else ValidationResult.Invalid("Target date must be in the future")
        )
        
        // Validate micro tasks
        microTasks.forEach { task ->
            validations.add(task.validate())
        }
        
        return validations.combine()
    }
    
    val progressPercentage: Double
        get() = if (targetAmount.isZero) 0.0 
                else (currentAmount.amount.toDouble() / targetAmount.amount.toDouble() * 100.0).coerceIn(0.0, 100.0)
    
    val remainingAmount: Money
        get() = (targetAmount - currentAmount).let { if (it.isNegative) Money.zero(targetAmount.currency) else it }
    
    val isCompleted: Boolean
        get() = currentAmount >= targetAmount
    
    val daysRemaining: Long
        get() {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            return (targetDate.toEpochDays() - today.toEpochDays()).toLong()
        }
    
    val isOverdue: Boolean
        get() = daysRemaining < 0 && !isCompleted
    
    val weeklyTargetAmount: Money
        get() {
            val weeksRemaining = (daysRemaining / 7.0).coerceAtLeast(1.0)
            return remainingAmount / weeksRemaining
        }
    
    val monthlyTargetAmount: Money
        get() {
            val monthsRemaining = (daysRemaining / 30.0).coerceAtLeast(1.0)
            return remainingAmount / monthsRemaining
        }
}

@Serializable
enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;
    
    val displayName: String
        get() = when (this) {
            LOW -> "Low"
            MEDIUM -> "Medium"
            HIGH -> "High"
            CRITICAL -> "Critical"
        }
    
    val sortOrder: Int
        get() = when (this) {
            CRITICAL -> 4
            HIGH -> 3
            MEDIUM -> 2
            LOW -> 1
        }
}

@Serializable
enum class GoalType {
    EMERGENCY_FUND,
    VACATION,
    CAR_PURCHASE,
    HOME_PURCHASE,
    DEBT_PAYOFF,
    RETIREMENT,
    EDUCATION,
    GENERAL_SAVINGS;
    
    val displayName: String
        get() = when (this) {
            EMERGENCY_FUND -> "Emergency Fund"
            VACATION -> "Vacation"
            CAR_PURCHASE -> "Car Purchase"
            HOME_PURCHASE -> "Home Purchase"
            DEBT_PAYOFF -> "Debt Payoff"
            RETIREMENT -> "Retirement"
            EDUCATION -> "Education"
            GENERAL_SAVINGS -> "General Savings"
        }
    
    val icon: String
        get() = when (this) {
            EMERGENCY_FUND -> "🛡️"
            VACATION -> "🏖️"
            CAR_PURCHASE -> "🚗"
            HOME_PURCHASE -> "🏠"
            DEBT_PAYOFF -> "💳"
            RETIREMENT -> "🏖️"
            EDUCATION -> "🎓"
            GENERAL_SAVINGS -> "💰"
        }
}

@Serializable
data class MicroTask(
    val id: String,
    val goalId: String,
    val title: String,
    val description: String,
    val targetAmount: Money,
    val isCompleted: Boolean = false,
    val dueDate: LocalDate? = null,
    val completedAt: kotlinx.datetime.Instant? = null
) {
    fun validate(): ValidationResult {
        val validations = mutableListOf<ValidationResult>()
        
        // Validate required fields
        validations.add(
            if (id.isNotBlank()) ValidationResult.Valid 
            else ValidationResult.Invalid("MicroTask ID cannot be blank")
        )
        
        validations.add(
            if (goalId.isNotBlank()) ValidationResult.Valid 
            else ValidationResult.Invalid("Goal ID cannot be blank")
        )
        
        validations.add(
            if (ValidationUtils.isValidName(title, 3)) ValidationResult.Valid 
            else ValidationResult.Invalid("MicroTask title must be at least 3 characters")
        )
        
        validations.add(
            if (description.isNotBlank()) ValidationResult.Valid 
            else ValidationResult.Invalid("MicroTask description cannot be blank")
        )
        
        // Validate target amount
        validations.add(
            if (targetAmount.isPositive) ValidationResult.Valid 
            else ValidationResult.Invalid("MicroTask target amount must be positive")
        )
        
        // Validate completion consistency
        if (isCompleted && completedAt == null) {
            validations.add(ValidationResult.Invalid("Completed micro task must have completion timestamp"))
        }
        
        return validations.combine()
    }
    
    val isOverdue: Boolean
        get() = dueDate?.let { due ->
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            due < today && !isCompleted
        } ?: false
}