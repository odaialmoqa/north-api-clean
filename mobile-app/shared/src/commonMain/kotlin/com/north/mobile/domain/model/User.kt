package com.north.mobile.domain.model

import com.north.mobile.domain.validation.ValidationResult
import com.north.mobile.domain.validation.ValidationUtils
import com.north.mobile.domain.validation.combine
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

@Serializable
data class User(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val isActive: Boolean = true,
    val profile: UserProfile? = null,
    val preferences: UserPreferences? = null,
    val gamificationData: GamificationProfile? = null
) {
    fun validate(): ValidationResult {
        val validations = listOf(
            if (id.isNotBlank()) ValidationResult.Valid else ValidationResult.Invalid("User ID cannot be blank"),
            if (ValidationUtils.isValidEmail(email)) ValidationResult.Valid else ValidationResult.Invalid("Invalid email format"),
            if (firstName.isNotBlank()) ValidationResult.Valid else ValidationResult.Invalid("First name cannot be blank"),
            if (lastName.isNotBlank()) ValidationResult.Valid else ValidationResult.Invalid("Last name cannot be blank")
        )
        return validations.combine()
    }
    
    val fullName: String
        get() = "$firstName $lastName"
    
    val displayName: String
        get() = firstName
}

@Serializable
data class UserProfile(
    val firstName: String,
    val lastName: String,
    val phoneNumber: String? = null,
    val dateOfBirth: LocalDate? = null,
    val sin: String? = null, // Social Insurance Number (encrypted/hashed in storage)
    val postalCode: String? = null
) {
    fun validate(): ValidationResult {
        val validations = mutableListOf<ValidationResult>()
        
        // Validate required fields
        validations.add(
            if (ValidationUtils.isValidName(firstName, 2)) ValidationResult.Valid 
            else ValidationResult.Invalid("First name must be at least 2 characters")
        )
        
        validations.add(
            if (ValidationUtils.isValidName(lastName, 2)) ValidationResult.Valid 
            else ValidationResult.Invalid("Last name must be at least 2 characters")
        )
        
        // Validate optional fields if provided
        phoneNumber?.let { phone ->
            validations.add(
                if (ValidationUtils.isValidCanadianPhoneNumber(phone)) ValidationResult.Valid
                else ValidationResult.Invalid("Invalid Canadian phone number format")
            )
        }
        
        sin?.let { socialInsuranceNumber ->
            validations.add(
                if (ValidationUtils.isValidSIN(socialInsuranceNumber)) ValidationResult.Valid
                else ValidationResult.Invalid("Invalid Social Insurance Number")
            )
        }
        
        postalCode?.let { postal ->
            validations.add(
                if (ValidationUtils.isValidCanadianPostalCode(postal)) ValidationResult.Valid
                else ValidationResult.Invalid("Invalid Canadian postal code format")
            )
        }
        
        return validations.combine()
    }
    
    val fullName: String
        get() = "$firstName $lastName"
    
    val displayName: String
        get() = firstName
}

@Serializable
data class UserPreferences(
    val currency: Currency = Currency.CAD,
    val language: String = "en",
    val notificationsEnabled: Boolean = true,
    val biometricAuthEnabled: Boolean = false,
    val budgetAlerts: Boolean = true,
    val goalReminders: Boolean = true,
    val spendingInsights: Boolean = true,
    val marketingEmails: Boolean = false
) {
    fun validate(): ValidationResult {
        val validations = listOf(
            if (language.isNotBlank() && language.length == 2) ValidationResult.Valid 
            else ValidationResult.Invalid("Language code must be 2 characters")
        )
        return validations.combine()
    }
}