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

// Money and Currency classes are defined in Money.kt