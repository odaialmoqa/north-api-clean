package com.north.mobile.domain.model

import com.north.mobile.domain.validation.ValidationResult
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransactionTest {
    
    private fun createValidTransaction() = Transaction(
        id = "txn123",
        accountId = "account123",
        amount = Money.fromDollars(-50.00), // Debit transaction
        description = "Coffee Shop Purchase",
        category = Category.FOOD,
        date = LocalDate(2024, 1, 15),
        merchantName = "Tim Hortons"
    )
    
    @Test
    fun testValidTransaction() {
        val transaction = createValidTransaction()
        val validation = transaction.validate()
        assertTrue(validation.isValid)
    }
    
    @Test
    fun testBlankTransactionId() {
        val transaction = createValidTransaction().copy(id = "")
        val validation = transaction.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("Transaction ID") })
    }
    
    @Test
    fun testBlankAccountId() {
        val transaction = createValidTransaction().copy(accountId = "")
        val validation = transaction.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("Account ID") })
    }
    
    @Test
    fun testBlankDescription() {
        val transaction = createValidTransaction().copy(description = "")
        val validation = transaction.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("description") })
    }
    
    @Test
    fun testZeroAmount() {
        val transaction = createValidTransaction().copy(amount = Money.zero())
        val validation = transaction.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("amount cannot be zero") })
    }
    
    @Test
    fun testTransactionTypeDetection() {
        val debitTransaction = createValidTransaction()
        assertEquals(TransactionType.DEBIT, debitTransaction.transactionType)
        assertTrue(debitTransaction.isDebit)
        assertFalse(debitTransaction.isCredit)
        
        val creditTransaction = debitTransaction.copy(
            amount = Money.fromDollars(100.00),
            transactionType = TransactionType.CREDIT
        )
        assertEquals(TransactionType.CREDIT, creditTransaction.transactionType)
        assertFalse(creditTransaction.isDebit)
        assertTrue(creditTransaction.isCredit)
    }
    
    @Test
    fun testAbsoluteAmount() {
        val debitTransaction = createValidTransaction()
        assertEquals(Money.fromDollars(50.00), debitTransaction.absoluteAmount)
        
        val creditTransaction = debitTransaction.copy(amount = Money.fromDollars(100.00))
        assertEquals(Money.fromDollars(100.00), creditTransaction.absoluteAmount)
    }
    
    @Test
    fun testDisplayDescription() {
        val transaction = createValidTransaction()
        assertEquals("Tim Hortons", transaction.displayDescription)
        
        val transactionWithoutMerchant = transaction.copy(merchantName = null)
        assertEquals("Coffee Shop Purchase", transactionWithoutMerchant.displayDescription)
    }
    
    @Test
    fun testCategoryValidation() {
        val validCategory = Category("food", "Food & Dining", color = "#FF9800")
        assertTrue(validCategory.validate().isValid)
        
        val invalidCategory = Category("", "Food & Dining")
        assertTrue(invalidCategory.validate().isInvalid)
        
        val invalidColorCategory = Category("food", "Food & Dining", color = "invalid-color")
        assertTrue(invalidColorCategory.validate().isInvalid)
        
        val validHexColor = Category("food", "Food & Dining", color = "#FF9800")
        assertTrue(validHexColor.validate().isValid)
    }
    
    @Test
    fun testDefaultCategories() {
        val categories = Category.getDefaultCategories()
        assertTrue(categories.isNotEmpty())
        assertTrue(categories.contains(Category.FOOD))
        assertTrue(categories.contains(Category.TRANSPORT))
        assertTrue(categories.contains(Category.RRSP))
        assertTrue(categories.contains(Category.TFSA))
        
        // Test Canadian-specific categories
        assertTrue(categories.any { it.name == "Hydro/Electricity" })
        assertTrue(categories.any { it.name == "RRSP Contribution" })
        assertTrue(categories.any { it.name == "TFSA Contribution" })
    }
    
    @Test
    fun testCategoryHierarchy() {
        val groceries = Category.GROCERIES
        assertEquals("food", groceries.parentCategoryId)
        
        val rrsp = Category.RRSP
        assertEquals("investment", rrsp.parentCategoryId)
    }
    
    @Test
    fun testTransactionStatus() {
        val transaction = createValidTransaction()
        assertEquals(TransactionStatus.POSTED, transaction.status)
        
        val pendingTransaction = transaction.copy(status = TransactionStatus.PENDING)
        assertEquals(TransactionStatus.PENDING, pendingTransaction.status)
    }
    
    @Test
    fun testRecurringTransaction() {
        val transaction = createValidTransaction()
        assertFalse(transaction.isRecurring)
        
        val recurringTransaction = transaction.copy(isRecurring = true)
        assertTrue(recurringTransaction.isRecurring)
    }
}