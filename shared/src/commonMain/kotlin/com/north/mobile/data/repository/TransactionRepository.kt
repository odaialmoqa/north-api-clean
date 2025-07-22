package com.north.mobile.data.repository

import com.north.mobile.database.NorthDatabase
import com.north.mobile.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

/**
 * Repository for Transaction data operations
 */
interface TransactionRepository : Repository<Transaction, String> {
    suspend fun findByAccountId(accountId: String): Result<List<Transaction>>
    suspend fun findByAccountAndDateRange(
        accountId: String, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): Result<List<Transaction>>
    suspend fun findByCategory(categoryId: String): Result<List<Transaction>>
    suspend fun findByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<Transaction>>
    suspend fun updateCategory(transactionId: String, category: Category): Result<Unit>
    suspend fun markAsRecurring(transactionId: String, isRecurring: Boolean): Result<Unit>
    suspend fun findDuplicates(transaction: Transaction): Result<List<Transaction>>
    
    // Lazy loading support
    suspend fun findByAccountIdPaged(
        accountId: String,
        limit: Int = 50,
        offset: Int = 0,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<List<Transaction>>
}

class TransactionRepositoryImpl(
    private val database: NorthDatabase
) : TransactionRepository {
    
    override suspend fun insert(entity: Transaction): Result<Transaction> {
        return try {
            val now = Clock.System.now()
            
            database.transactionQueries.insert(
                id = entity.id,
                accountId = entity.accountId,
                amount = (entity.amount.amount * 100).toLong(), // Convert to cents
                description = entity.description,
                category = entity.category.id,
                subcategory = null,
                date = now.toEpochMilliseconds(),
                isRecurring = if (entity.isRecurring) 1L else 0L,
                merchantName = entity.merchantName,
                location = entity.location,
                isVerified = 0L,
                notes = null,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = now.toEpochMilliseconds()
            )
            
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to insert transaction", e))
        }
    }
    
    override suspend fun update(entity: Transaction): Result<Transaction> {
        return try {
            val now = Clock.System.now()
            
            database.transactionQueries.update(
                amount = (entity.amount.amount * 100).toLong(), // Convert to cents
                description = entity.description,
                category = entity.category.id,
                subcategory = null,
                date = now.toEpochMilliseconds(),
                isRecurring = if (entity.isRecurring) 1L else 0L,
                merchantName = entity.merchantName,
                location = entity.location,
                isVerified = 0L,
                notes = null,
                updatedAt = now.toEpochMilliseconds(),
                id = entity.id
            )
            
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update transaction", e))
        }
    }
    
    override suspend fun delete(id: String): Result<Unit> {
        return try {
            database.transactionQueries.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete transaction", e))
        }
    }
    
    override suspend fun findById(id: String): Result<Transaction?> {
        return try {
            val transactionRow = database.transactionQueries.selectAll().executeAsList()
                .find { it.id == id }
            val transaction = transactionRow?.let { mapToTransaction(it) }
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to find transaction by id", e))
        }
    }
    
    override suspend fun findAll(): Result<List<Transaction>> {
        return try {
            val transactions = database.transactionQueries.selectAll().executeAsList()
                .map { mapToTransaction(it) }
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to find all transactions", e))
        }
    }
    
    override suspend fun findByAccountId(accountId: String): Result<List<Transaction>> {
        return try {
            val transactions = database.transactionQueries.selectByAccountId(accountId).executeAsList()
                .map { mapToTransaction(it) }
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to find transactions by account id", e))
        }
    }
    
    override suspend fun findByAccountAndDateRange(
        accountId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<Transaction>> {
        return try {
            val startEpochDays = startDate.toEpochDays().toLong()
            val endEpochDays = endDate.toEpochDays().toLong()
            
            val transactions = database.transactionQueries.selectByAccountAndDateRange(
                accountId, startEpochDays, endEpochDays
            ).executeAsList().map { mapToTransaction(it) }
            
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to find transactions by account and date range", e))
        }
    }
    
    override suspend fun findByCategory(categoryId: String): Result<List<Transaction>> {
        return try {
            val transactions = database.transactionQueries.selectByCategory(categoryId).executeAsList()
                .map { mapToTransaction(it) }
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to find transactions by category", e))
        }
    }
    
    override suspend fun findByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<Transaction>> {
        return try {
            val startEpochDays = startDate.toEpochDays().toLong()
            val endEpochDays = endDate.toEpochDays().toLong()
            
            val transactions = database.transactionQueries.selectByDateRange(
                startEpochDays, endEpochDays
            ).executeAsList().map { mapToTransaction(it) }
            
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to find transactions by date range", e))
        }
    }
    
    override suspend fun updateCategory(transactionId: String, category: Category): Result<Unit> {
        return try {
            val now = Clock.System.now()
            
            database.transactionQueries.updateCategory(
                category = category.id,
                subcategory = null,
                updatedAt = now.toEpochMilliseconds(),
                id = transactionId
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update transaction category", e))
        }
    }
    
    override suspend fun markAsRecurring(transactionId: String, isRecurring: Boolean): Result<Unit> {
        return try {
            val now = Clock.System.now()
            
            database.transactionQueries.updateRecurring(
                isRecurring = if (isRecurring) 1L else 0L,
                updatedAt = now.toEpochMilliseconds(),
                id = transactionId
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update transaction recurring status", e))
        }
    }
    
    override suspend fun findDuplicates(transaction: Transaction): Result<List<Transaction>> {
        return try {
            val dateEpochDays = transaction.date.toEpochDays().toLong()
            
            val duplicates = database.transactionQueries.findDuplicates(
                accountId = transaction.accountId,
                amount = transaction.amount.amount,
                date = dateEpochDays,
                excludeId = transaction.id
            ).executeAsList().map { mapToTransaction(it) }
            
            Result.success(duplicates)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to find duplicate transactions", e))
        }
    }
    
    override suspend fun findByAccountIdPaged(
        accountId: String,
        limit: Int,
        offset: Int,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Result<List<Transaction>> {
        return try {
            val startEpochDays = startDate?.toEpochDays()?.toLong()
            val endEpochDays = endDate?.toEpochDays()?.toLong()
            
            val transactions = database.transactionQueries.selectByAccountIdPaged(
                accountId = accountId,
                limit = limit.toLong(),
                offset = offset.toLong(),
                startDate = startEpochDays,
                endDate = endEpochDays
            ).executeAsList().map { mapToTransaction(it) }
            
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to find paged transactions by account id", e))
        }
    }
    
    private fun mapToTransaction(transactionRow: com.north.mobile.database.TransactionEntity): Transaction {
        return Transaction(
            id = transactionRow.id,
            accountId = transactionRow.accountId,
            amount = Money(transactionRow.amount.toDouble() / 100.0, Currency.CAD), // Convert from cents
            description = transactionRow.description,
            category = Category(
                id = transactionRow.category ?: "uncategorized",
                name = transactionRow.category ?: "Uncategorized"
            ),
            date = LocalDate.fromEpochDays((transactionRow.date / (24 * 60 * 60 * 1000)).toInt()), // Convert from timestamp
            isRecurring = transactionRow.isRecurring == 1L,
            merchantName = transactionRow.merchantName,
            location = transactionRow.location
        )
    }
}