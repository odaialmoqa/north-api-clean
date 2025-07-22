package com.north.mobile.domain.model

import com.north.mobile.domain.validation.ValidationResult
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountTest {
    
    private fun createValidAccount() = Account(
        id = "account123",
        institutionId = "rbc",
        institutionName = "Royal Bank of Canada",
        accountType = AccountType.CHECKING,
        balance = Money.fromDollars(1500.00),
        lastUpdated = Clock.System.now(),
        accountNumber = "1234",
        transitNumber = "12345",
        institutionNumber = "003"
    )
    
    @Test
    fun testValidAccount() {
        val account = createValidAccount()
        val validation = account.validate()
        assertTrue(validation.isValid)
    }
    
    @Test
    fun testBlankAccountId() {
        val account = createValidAccount().copy(id = "")
        val validation = account.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("Account ID") })
    }
    
    @Test
    fun testBlankInstitutionId() {
        val account = createValidAccount().copy(institutionId = "")
        val validation = account.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("Institution ID") })
    }
    
    @Test
    fun testBlankInstitutionName() {
        val account = createValidAccount().copy(institutionName = "")
        val validation = account.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("Institution name") })
    }
    
    @Test
    fun testInvalidAccountNumber() {
        val account = createValidAccount().copy(accountNumber = "123") // Too short
        val validation = account.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("account number") })
    }
    
    @Test
    fun testInvalidTransitNumber() {
        val account = createValidAccount().copy(transitNumber = "123") // Too short
        val validation = account.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("transit number") })
    }
    
    @Test
    fun testInvalidInstitutionNumber() {
        val account = createValidAccount().copy(institutionNumber = "12") // Too short
        val validation = account.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("institution number") })
    }
    
    @Test
    fun testCurrencyMismatch() {
        val account = createValidAccount().copy(
            balance = Money.fromDollars(1500.00, Currency.USD),
            currency = Currency.CAD
        )
        val validation = account.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("currency") })
    }
    
    @Test
    fun testAvailableBalanceCurrencyMismatch() {
        val account = createValidAccount().copy(
            availableBalance = Money.fromDollars(1000.00, Currency.USD),
            currency = Currency.CAD
        )
        val validation = account.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("Available balance currency") })
    }
    
    @Test
    fun testAccountTypeDisplayNames() {
        assertEquals("Checking", AccountType.CHECKING.displayName)
        assertEquals("Savings", AccountType.SAVINGS.displayName)
        assertEquals("Credit Card", AccountType.CREDIT_CARD.displayName)
        assertEquals("Investment", AccountType.INVESTMENT.displayName)
        assertEquals("Loan", AccountType.LOAN.displayName)
        assertEquals("Mortgage", AccountType.MORTGAGE.displayName)
    }
    
    @Test
    fun testAccountDisplayProperties() {
        val account = createValidAccount()
        assertEquals("Checking - Royal Bank of Canada", account.displayName)
        assertEquals("****1234", account.maskedAccountNumber)
        assertFalse(account.isCredit)
        assertFalse(account.isDebt)
        
        val creditAccount = account.copy(accountType = AccountType.CREDIT_CARD)
        assertTrue(creditAccount.isCredit)
        assertTrue(creditAccount.isDebt)
        
        val loanAccount = account.copy(accountType = AccountType.LOAN)
        assertFalse(loanAccount.isCredit)
        assertTrue(loanAccount.isDebt)
        
        val nicknameAccount = account.copy(nickname = "My Checking")
        assertEquals("My Checking", nicknameAccount.displayName)
    }
    
    @Test
    fun testAccountWithNullOptionalFields() {
        val account = Account(
            id = "account123",
            institutionId = "rbc",
            institutionName = "Royal Bank of Canada",
            accountType = AccountType.CHECKING,
            balance = Money.fromDollars(1500.00),
            lastUpdated = Clock.System.now()
        )
        val validation = account.validate()
        assertTrue(validation.isValid)
        assertEquals(null, account.maskedAccountNumber)
    }
}