package com.north.mobile.domain.model

import com.north.mobile.domain.validation.ValidationResult
import com.north.mobile.domain.validation.ValidationUtils
import com.north.mobile.domain.validation.combine
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class Account(
    val id: String,
    val institutionId: String,
    val institutionName: String,
    val accountType: AccountType,
    val balance: Money,
    val availableBalance: Money? = null, // For credit accounts
    val currency: Currency = Currency.CAD,
    val lastUpdated: Instant,
    val accountNumber: String? = null, // Last 4 digits only for security
    val transitNumber: String? = null, // Canadian bank transit number
    val institutionNumber: String? = null, // Canadian bank institution number
    val nickname: String? = null,
    val isActive: Boolean = true
) {
    fun validate(): ValidationResult {
        val validations = mutableListOf<ValidationResult>()
        
        // Validate required fields
        validations.add(
            if (id.isNotBlank()) ValidationResult.Valid 
            else ValidationResult.Invalid("Account ID cannot be blank")
        )
        
        validations.add(
            if (institutionId.isNotBlank()) ValidationResult.Valid 
            else ValidationResult.Invalid("Institution ID cannot be blank")
        )
        
        validations.add(
            if (institutionName.isNotBlank()) ValidationResult.Valid 
            else ValidationResult.Invalid("Institution name cannot be blank")
        )
        
        // Validate optional Canadian banking fields
        accountNumber?.let { accNum ->
            validations.add(
                if (ValidationUtils.isValidBankAccountNumber(accNum)) ValidationResult.Valid
                else ValidationResult.Invalid("Invalid bank account number format")
            )
        }
        
        transitNumber?.let { transit ->
            validations.add(
                if (ValidationUtils.isValidTransitNumber(transit)) ValidationResult.Valid
                else ValidationResult.Invalid("Invalid transit number format")
            )
        }
        
        institutionNumber?.let { instNum ->
            validations.add(
                if (ValidationUtils.isValidInstitutionNumber(instNum)) ValidationResult.Valid
                else ValidationResult.Invalid("Invalid institution number format")
            )
        }
        
        // Validate balance currency matches account currency
        validations.add(
            if (balance.currency == currency) ValidationResult.Valid
            else ValidationResult.Invalid("Balance currency must match account currency")
        )
        
        availableBalance?.let { availBal ->
            validations.add(
                if (availBal.currency == currency) ValidationResult.Valid
                else ValidationResult.Invalid("Available balance currency must match account currency")
            )
        }
        
        return validations.combine()
    }
    
    val displayName: String
        get() = nickname ?: "${accountType.displayName} - ${institutionName}"
    
    val maskedAccountNumber: String?
        get() = accountNumber?.let { "****$it" }
    
    val isCredit: Boolean
        get() = accountType == AccountType.CREDIT_CARD
    
    val isDebt: Boolean
        get() = accountType in listOf(AccountType.CREDIT_CARD, AccountType.LOAN, AccountType.MORTGAGE)
}

@Serializable
enum class AccountType {
    CHECKING,
    SAVINGS,
    CREDIT_CARD,
    INVESTMENT,
    LOAN,
    MORTGAGE;
    
    val displayName: String
        get() = when (this) {
            CHECKING -> "Checking"
            SAVINGS -> "Savings"
            CREDIT_CARD -> "Credit Card"
            INVESTMENT -> "Investment"
            LOAN -> "Loan"
            MORTGAGE -> "Mortgage"
        }
}

@Serializable
enum class Currency {
    CAD,
    USD;
    
    val currencyCode: String
        get() = name
}

@Serializable
data class Money(
    val amount: Long, // Amount in cents to avoid floating point issues
    val currency: Currency = Currency.CAD
) {
    val dollars: Double
        get() = amount / 100.0
    
    val isPositive: Boolean
        get() = amount > 0
    
    val isNegative: Boolean
        get() = amount < 0
    
    val isZero: Boolean
        get() = amount == 0L
    
    val absoluteValue: Money
        get() = Money(kotlin.math.abs(amount), currency)
    
    val amountInCents: Long
        get() = amount
    
    companion object {
        fun fromDollars(dollars: Double, currency: Currency = Currency.CAD): Money {
            return Money((dollars * 100).toLong(), currency)
        }
        
        fun zero(currency: Currency = Currency.CAD): Money {
            return Money(0, currency)
        }
        
        fun fromCents(cents: Long, currency: Currency = Currency.CAD): Money {
            return Money(cents, currency)
        }
        
        fun parse(value: String, currency: Currency = Currency.CAD): Money? {
            return try {
                val cleanValue = value.replace(Regex("[^0-9.-]"), "")
                val dollars = cleanValue.toDouble()
                fromDollars(dollars, currency)
            } catch (e: NumberFormatException) {
                null
            }
        }
    }
    
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add different currencies: $currency + ${other.currency}" }
        return Money(amount + other.amount, currency)
    }
    
    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Cannot subtract different currencies: $currency - ${other.currency}" }
        return Money(amount - other.amount, currency)
    }
    
    operator fun times(multiplier: Double): Money {
        return Money((amount * multiplier).toLong(), currency)
    }
    
    operator fun div(divisor: Double): Money {
        require(divisor != 0.0) { "Cannot divide by zero" }
        return Money((amount / divisor).toLong(), currency)
    }
    
    operator fun compareTo(other: Money): Int {
        require(currency == other.currency) { "Cannot compare different currencies: $currency vs ${other.currency}" }
        return amount.compareTo(other.amount)
    }
    
    /**
     * Format money according to Canadian conventions
     * Examples: $1,234.56, -$1,234.56, $0.00
     */
    fun formatCAD(): String {
        val dollarsValue = kotlin.math.abs(dollars)
        val formatted = when {
            dollarsValue >= 1_000_000 -> "%.1fM".format(dollarsValue / 1_000_000)
            dollarsValue >= 1_000 -> "%.2f".format(dollarsValue)
            else -> "%.2f".format(dollarsValue)
        }
        
        val prefix = if (isNegative) "-$" else "$"
        return "$prefix$formatted"
    }
    
    /**
     * Format money with currency symbol
     */
    fun format(): String {
        return when (currency) {
            Currency.CAD -> formatCAD()
            Currency.USD -> {
                val dollarsValue = kotlin.math.abs(dollars)
                val formatted = "%.2f".format(dollarsValue)
                val prefix = if (isNegative) "-US$" else "US$"
                "$prefix$formatted"
            }
        }
    }
    
    override fun toString(): String = format()
}