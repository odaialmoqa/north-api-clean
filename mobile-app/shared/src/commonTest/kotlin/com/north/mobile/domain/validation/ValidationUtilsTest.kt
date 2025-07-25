package com.north.mobile.domain.validation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationUtilsTest {
    
    @Test
    fun testValidSIN() {
        // Valid SINs
        assertTrue(ValidationUtils.isValidSIN("123-456-782"))
        assertTrue(ValidationUtils.isValidSIN("123456782"))
        assertTrue(ValidationUtils.isValidSIN("123 456 782"))
        
        // Invalid SINs
        assertFalse(ValidationUtils.isValidSIN("000-000-000")) // Starts with 0
        assertFalse(ValidationUtils.isValidSIN("800-000-000")) // Starts with 8
        assertFalse(ValidationUtils.isValidSIN("123-456-789")) // Invalid checksum
        assertFalse(ValidationUtils.isValidSIN("123-456-78")) // Too short
        assertFalse(ValidationUtils.isValidSIN("123-456-7890")) // Too long
        assertFalse(ValidationUtils.isValidSIN("abc-def-ghi")) // Non-numeric
    }
    
    @Test
    fun testValidCanadianPostalCode() {
        // Valid postal codes
        assertTrue(ValidationUtils.isValidCanadianPostalCode("K1A 0A6"))
        assertTrue(ValidationUtils.isValidCanadianPostalCode("M5V 3L9"))
        assertTrue(ValidationUtils.isValidCanadianPostalCode("k1a0a6")) // Case insensitive
        assertTrue(ValidationUtils.isValidCanadianPostalCode("K1A0A6")) // No space
        
        // Invalid postal codes
        assertFalse(ValidationUtils.isValidCanadianPostalCode("D1A 0A6")) // D not allowed
        assertFalse(ValidationUtils.isValidCanadianPostalCode("K1D 0A6")) // D not allowed in 3rd position
        assertFalse(ValidationUtils.isValidCanadianPostalCode("K1A 0D6")) // D not allowed in 5th position
        assertFalse(ValidationUtils.isValidCanadianPostalCode("K1A 0A")) // Too short
        assertFalse(ValidationUtils.isValidCanadianPostalCode("K1A 0A67")) // Too long
        assertFalse(ValidationUtils.isValidCanadianPostalCode("123 456")) // All numbers
    }
    
    @Test
    fun testValidCanadianPhoneNumber() {
        // Valid phone numbers
        assertTrue(ValidationUtils.isValidCanadianPhoneNumber("(416) 555-1234"))
        assertTrue(ValidationUtils.isValidCanadianPhoneNumber("416-555-1234"))
        assertTrue(ValidationUtils.isValidCanadianPhoneNumber("416.555.1234"))
        assertTrue(ValidationUtils.isValidCanadianPhoneNumber("4165551234"))
        assertTrue(ValidationUtils.isValidCanadianPhoneNumber("613 555 1234"))
        
        // Invalid phone numbers
        assertFalse(ValidationUtils.isValidCanadianPhoneNumber("016-555-1234")) // Area code starts with 0
        assertFalse(ValidationUtils.isValidCanadianPhoneNumber("116-555-1234")) // Area code starts with 1
        assertFalse(ValidationUtils.isValidCanadianPhoneNumber("416-055-1234")) // Exchange starts with 0
        assertFalse(ValidationUtils.isValidCanadianPhoneNumber("416-155-1234")) // Exchange starts with 1
        assertFalse(ValidationUtils.isValidCanadianPhoneNumber("416-555-123")) // Too short
        assertFalse(ValidationUtils.isValidCanadianPhoneNumber("416-555-12345")) // Too long
        assertFalse(ValidationUtils.isValidCanadianPhoneNumber("1-416-555-1234")) // 11 digits
    }
    
    @Test
    fun testValidEmail() {
        // Valid emails
        assertTrue(ValidationUtils.isValidEmail("test@example.com"))
        assertTrue(ValidationUtils.isValidEmail("user.name@domain.co.uk"))
        assertTrue(ValidationUtils.isValidEmail("user+tag@example.org"))
        assertTrue(ValidationUtils.isValidEmail("123@example.com"))
        
        // Invalid emails
        assertFalse(ValidationUtils.isValidEmail("invalid.email"))
        assertFalse(ValidationUtils.isValidEmail("@example.com"))
        assertFalse(ValidationUtils.isValidEmail("user@"))
        assertFalse(ValidationUtils.isValidEmail("user@.com"))
        assertFalse(ValidationUtils.isValidEmail("user name@example.com"))
    }
    
    @Test
    fun testValidName() {
        // Valid names
        assertTrue(ValidationUtils.isValidName("John"))
        assertTrue(ValidationUtils.isValidName("Mary Jane"))
        assertTrue(ValidationUtils.isValidName("O'Connor"))
        assertTrue(ValidationUtils.isValidName("Jean-Luc", 2))
        
        // Invalid names
        assertFalse(ValidationUtils.isValidName(""))
        assertFalse(ValidationUtils.isValidName("   "))
        assertFalse(ValidationUtils.isValidName("A", 2)) // Too short for minimum length
    }
    
    @Test
    fun testValidBankAccountNumber() {
        // Valid account numbers
        assertTrue(ValidationUtils.isValidBankAccountNumber("1234567"))
        assertTrue(ValidationUtils.isValidBankAccountNumber("123456789012"))
        assertTrue(ValidationUtils.isValidBankAccountNumber("123-456-789"))
        
        // Invalid account numbers
        assertFalse(ValidationUtils.isValidBankAccountNumber("123456")) // Too short
        assertFalse(ValidationUtils.isValidBankAccountNumber("1234567890123")) // Too long
        assertFalse(ValidationUtils.isValidBankAccountNumber("abc1234567")) // Contains letters
    }
    
    @Test
    fun testValidInstitutionNumber() {
        // Valid institution numbers
        assertTrue(ValidationUtils.isValidInstitutionNumber("001"))
        assertTrue(ValidationUtils.isValidInstitutionNumber("123"))
        assertTrue(ValidationUtils.isValidInstitutionNumber("999"))
        
        // Invalid institution numbers
        assertFalse(ValidationUtils.isValidInstitutionNumber("12")) // Too short
        assertFalse(ValidationUtils.isValidInstitutionNumber("1234")) // Too long
        assertFalse(ValidationUtils.isValidInstitutionNumber("abc")) // Contains letters
    }
    
    @Test
    fun testValidTransitNumber() {
        // Valid transit numbers
        assertTrue(ValidationUtils.isValidTransitNumber("12345"))
        assertTrue(ValidationUtils.isValidTransitNumber("00001"))
        assertTrue(ValidationUtils.isValidTransitNumber("99999"))
        
        // Invalid transit numbers
        assertFalse(ValidationUtils.isValidTransitNumber("1234")) // Too short
        assertFalse(ValidationUtils.isValidTransitNumber("123456")) // Too long
        assertFalse(ValidationUtils.isValidTransitNumber("abcde")) // Contains letters
    }
    
    @Test
    fun testValidationResultCombine() {
        // All valid
        val allValid = listOf(ValidationResult.Valid, ValidationResult.Valid)
        assertTrue(allValid.combine().isValid)
        
        // Some invalid
        val someInvalid = listOf(
            ValidationResult.Valid,
            ValidationResult.Invalid("Error 1"),
            ValidationResult.Invalid("Error 2")
        )
        val combined = someInvalid.combine()
        assertTrue(combined.isInvalid)
        assertTrue((combined as ValidationResult.Invalid).errors.contains("Error 1"))
        assertTrue(combined.errors.contains("Error 2"))
        
        // Empty list
        val empty = emptyList<ValidationResult>()
        assertTrue(empty.combine().isValid)
    }
}