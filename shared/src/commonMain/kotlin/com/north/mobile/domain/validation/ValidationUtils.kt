package com.north.mobile.domain.validation

/**
 * Validation utilities for Canadian financial data
 */
object ValidationUtils {
    
    /**
     * Validates Canadian Social Insurance Number (SIN)
     * Format: XXX-XXX-XXX where X is a digit
     */
    fun isValidSIN(sin: String): Boolean {
        val cleanSin = sin.replace("-", "").replace(" ", "")
        
        // Must be exactly 9 digits
        if (cleanSin.length != 9 || !cleanSin.all { it.isDigit() }) {
            return false
        }
        
        // Cannot start with 0 or 8
        if (cleanSin.startsWith("0") || cleanSin.startsWith("8")) {
            return false
        }
        
        // Luhn algorithm check
        return isValidLuhn(cleanSin)
    }
    
    /**
     * Validates Canadian postal code
     * Format: A1A 1A1 (letter-digit-letter space digit-letter-digit)
     */
    fun isValidCanadianPostalCode(postalCode: String): Boolean {
        val cleanCode = postalCode.replace(" ", "").uppercase()
        
        if (cleanCode.length != 6) return false
        
        val pattern = Regex("^[ABCEGHJKLMNPRSTVXY][0-9][ABCEGHJKLMNPRSTVWXYZ][0-9][ABCEGHJKLMNPRSTVWXYZ][0-9]$")
        return pattern.matches(cleanCode)
    }
    
    /**
     * Validates Canadian phone number
     * Accepts various formats: (XXX) XXX-XXXX, XXX-XXX-XXXX, XXX.XXX.XXXX, XXXXXXXXXX
     */
    fun isValidCanadianPhoneNumber(phoneNumber: String): Boolean {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        
        // Must be exactly 10 digits for Canadian numbers
        if (cleanNumber.length != 10) return false
        
        // Area code cannot start with 0 or 1
        val areaCode = cleanNumber.substring(0, 3)
        if (areaCode.startsWith("0") || areaCode.startsWith("1")) {
            return false
        }
        
        // Exchange code cannot start with 0 or 1
        val exchangeCode = cleanNumber.substring(3, 6)
        if (exchangeCode.startsWith("0") || exchangeCode.startsWith("1")) {
            return false
        }
        
        return true
    }
    
    /**
     * Validates email address using basic regex
     */
    fun isValidEmail(email: String): Boolean {
        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return emailPattern.matches(email)
    }
    
    /**
     * Validates that a string is not blank and meets minimum length requirements
     */
    fun isValidName(name: String, minLength: Int = 1): Boolean {
        return name.isNotBlank() && name.trim().length >= minLength
    }
    
    /**
     * Validates Canadian bank account number (basic validation)
     * Account numbers are typically 7-12 digits
     */
    fun isValidBankAccountNumber(accountNumber: String): Boolean {
        val cleanNumber = accountNumber.replace(Regex("[^0-9]"), "")
        return cleanNumber.length in 7..12 && cleanNumber.all { it.isDigit() }
    }
    
    /**
     * Validates Canadian institution number (3 digits)
     */
    fun isValidInstitutionNumber(institutionNumber: String): Boolean {
        val cleanNumber = institutionNumber.replace(Regex("[^0-9]"), "")
        return cleanNumber.length == 3 && cleanNumber.all { it.isDigit() }
    }
    
    /**
     * Validates Canadian transit number (5 digits)
     */
    fun isValidTransitNumber(transitNumber: String): Boolean {
        val cleanNumber = transitNumber.replace(Regex("[^0-9]"), "")
        return cleanNumber.length == 5 && cleanNumber.all { it.isDigit() }
    }
    
    /**
     * Luhn algorithm implementation for checksum validation
     */
    private fun isValidLuhn(number: String): Boolean {
        var sum = 0
        var alternate = false
        
        for (i in number.length - 1 downTo 0) {
            var digit = number[i].digitToInt()
            
            if (alternate) {
                digit *= 2
                if (digit > 9) {
                    digit = (digit % 10) + 1
                }
            }
            
            sum += digit
            alternate = !alternate
        }
        
        return sum % 10 == 0
    }
}

/**
 * Validation result class
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult() {
        constructor(error: String) : this(listOf(error))
    }
    
    val isValid: Boolean
        get() = this is Valid
    
    val isInvalid: Boolean
        get() = this is Invalid
}

/**
 * Extension function to combine validation results
 */
fun List<ValidationResult>.combine(): ValidationResult {
    val errors = this.filterIsInstance<ValidationResult.Invalid>()
        .flatMap { it.errors }
    
    return if (errors.isEmpty()) {
        ValidationResult.Valid
    } else {
        ValidationResult.Invalid(errors)
    }
}