package com.north.mobile.data.sync

import com.north.mobile.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.*

class ConflictResolverTest {
    
    private lateinit var conflictResolver: ConflictResolverImpl
    
    @BeforeTest
    fun setup() {
        conflictResolver = ConflictResolverImpl()
    }
    
    @Test
    fun `detectTransactionConflict should return null for identical transactions`() {
        // Given
        val transaction1 = createTestTransaction("txn1", "acc1", Money.fromDollars(100.0))
        val transaction2 = createTestTransaction("txn1", "acc1", Money.fromDollars(100.0))
        
        // When
        val conflict = conflictResolver.detectTransactionConflict(transaction1, transaction2)
        
        // Then
        assertNull(conflict)
    }
    
    @Test
    fun `detectTransactionConflict should detect modified transaction`() {
        // Given
        val localTransaction = createTestTransaction("txn1", "acc1", Money.fromDollars(100.0))
        val remoteTransaction = createTestTransaction("txn1", "acc1", Money.fromDollars(105.0))
        
        // When
        val conflict = conflictResolver.detectTransactionConflict(localTransaction, remoteTransaction)
        
        // Then
        assertNotNull(conflict)
        assertEquals(ConflictType.MODIFIED_TRANSACTION, conflict.conflictType)
        assertEquals(localTransaction, conflict.localData)
        assertEquals(remoteTransaction, conflict.remoteData)
    }
    
    @Test
    fun `detectTransactionConflict should detect duplicate transactions`() {
        // Given
        val localTransaction = createTestTransaction("txn1", "acc1", Money.fromDollars(100.0))
        val remoteTransaction = createTestTransaction("txn2", "acc1", Money.fromDollars(100.0)) // Different ID, same details
        
        // When
        val conflict = conflictResolver.detectTransactionConflict(localTransaction, remoteTransaction)
        
        // Then
        assertNotNull(conflict)
        assertEquals(ConflictType.DUPLICATE_TRANSACTION, conflict.conflictType)
    }
    
    @Test
    fun `resolveTransactionConflict should prefer remote for modified transactions`() {
        // Given
        val localTransaction = createTestTransaction("txn1", "acc1", Money.fromDollars(100.0))
        val remoteTransaction = createTestTransaction("txn1", "acc1", Money.fromDollars(105.0))
        val conflict = ConflictDetails(
            conflictType = ConflictType.MODIFIED_TRANSACTION,
            localData = localTransaction,
            remoteData = remoteTransaction,
            resolution = ConflictResolution.USE_REMOTE
        )
        
        // When
        val resolvedConflict = conflictResolver.resolveTransactionConflict(conflict)
        
        // Then
        assertEquals(ConflictResolution.USE_REMOTE, resolvedConflict.resolution)
    }
    
    @Test
    fun `resolveTransactionConflict should prefer transaction with more complete data for duplicates`() {
        // Given
        val localTransaction = createTestTransaction("txn1", "acc1", Money.fromDollars(100.0))
        val remoteTransaction = createTestTransaction("txn2", "acc1", Money.fromDollars(100.0)).copy(
            merchantName = "Test Merchant",
            location = "Toronto, ON"
        )
        val conflict = ConflictDetails(
            conflictType = ConflictType.DUPLICATE_TRANSACTION,
            localData = localTransaction,
            remoteData = remoteTransaction,
            resolution = ConflictResolution.USE_REMOTE
        )
        
        // When
        val resolvedConflict = conflictResolver.resolveTransactionConflict(conflict)
        
        // Then
        assertEquals(ConflictResolution.USE_REMOTE, resolvedConflict.resolution)
    }
    
    @Test
    fun `detectAccountConflict should detect balance mismatch`() {
        // Given
        val localAccount = createTestAccount("acc1", "inst1", Money.fromDollars(1000.0))
        val remoteAccount = createTestAccount("acc1", "inst1", Money.fromDollars(1200.0))
        
        // When
        val conflict = conflictResolver.detectAccountConflict(localAccount, remoteAccount)
        
        // Then
        assertNotNull(conflict)
        assertEquals(ConflictType.BALANCE_MISMATCH, conflict.conflictType)
    }
    
    @Test
    fun `detectAccountConflict should detect account status change`() {
        // Given
        val localAccount = createTestAccount("acc1", "inst1", Money.fromDollars(1000.0), isActive = true)
        val remoteAccount = createTestAccount("acc1", "inst1", Money.fromDollars(1000.0), isActive = false)
        
        // When
        val conflict = conflictResolver.detectAccountConflict(localAccount, remoteAccount)
        
        // Then
        assertNotNull(conflict)
        assertEquals(ConflictType.ACCOUNT_STATUS_CHANGE, conflict.conflictType)
    }
    
    @Test
    fun `resolveAccountConflict should prefer remote balance`() {
        // Given
        val localAccount = createTestAccount("acc1", "inst1", Money.fromDollars(1000.0))
        val remoteAccount = createTestAccount("acc1", "inst1", Money.fromDollars(1200.0))
        val conflict = ConflictDetails(
            conflictType = ConflictType.BALANCE_MISMATCH,
            localData = localAccount,
            remoteData = remoteAccount,
            resolution = ConflictResolution.USE_REMOTE
        )
        
        // When
        val resolvedConflict = conflictResolver.resolveAccountConflict(conflict)
        
        // Then
        assertEquals(ConflictResolution.USE_REMOTE, resolvedConflict.resolution)
    }
    
    @Test
    fun `resolveAccountConflict should prefer local for recent manual deactivation`() {
        // Given
        val now = Clock.System.now()
        val recentlyDeactivated = createTestAccount("acc1", "inst1", Money.fromDollars(1000.0), 
            isActive = false, lastUpdated = now)
        val remoteAccount = createTestAccount("acc1", "inst1", Money.fromDollars(1000.0), isActive = true)
        val conflict = ConflictDetails(
            conflictType = ConflictType.ACCOUNT_STATUS_CHANGE,
            localData = recentlyDeactivated,
            remoteData = remoteAccount,
            resolution = ConflictResolution.USE_REMOTE
        )
        
        // When
        val resolvedConflict = conflictResolver.resolveAccountConflict(conflict)
        
        // Then
        assertEquals(ConflictResolution.USE_LOCAL, resolvedConflict.resolution)
    }
    
    private fun createTestTransaction(
        id: String,
        accountId: String,
        amount: Money,
        date: LocalDate = LocalDate(2024, 1, 15),
        description: String = "Test Transaction"
    ): Transaction {
        return Transaction(
            id = id,
            accountId = accountId,
            amount = amount,
            description = description,
            category = Category.UNCATEGORIZED,
            date = date
        )
    }
    
    private fun createTestAccount(
        id: String,
        institutionId: String,
        balance: Money,
        isActive: Boolean = true,
        lastUpdated: kotlinx.datetime.Instant = Clock.System.now()
    ): Account {
        return Account(
            id = id,
            institutionId = institutionId,
            institutionName = "Test Bank",
            accountType = AccountType.CHECKING,
            balance = balance,
            currency = Currency.CAD,
            lastUpdated = lastUpdated,
            isActive = isActive
        )
    }
}