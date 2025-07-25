package com.north.mobile.data.repository

import com.north.mobile.database.NorthDatabase
import com.north.mobile.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Repository for Account data operations
 */
interface AccountRepository : Repository<Account, String> {
    suspend fun findByUserId(userId: String): Result<List<Account>>
    suspend fun findByInstitution(userId: String, institutionId: String): Result<List<Account>>
    suspend fun updateBalance(accountId: String, balance: Money): Result<Unit>
    suspend fun deactivateAccount(accountId: String): Result<Unit>
    
    // Additional methods for Plaid integration
    suspend fun saveAccount(account: Account): Result<Account>
    suspend fun updateAccount(account: Account): Result<Account>
    suspend fun getAllAccounts(): List<Account>
    suspend fun getAccountById(accountId: String): Account?
    
    // Methods needed by UI ViewModels
    suspend fun createAccount(account: Account): Result<Account>
    suspend fun getAccount(accountId: String): Result<Account?>
    suspend fun getUserAccounts(userId: String): Result<List<Account>>
    suspend fun deleteAccount(accountId: String): Result<Unit>
    suspend fun getAccountsByInstitution(institutionId: String): Result<List<Account>>
    suspend fun updateAccountBalance(accountId: String, balance: Money): Result<Unit>
    suspend fun getActiveAccounts(userId: String): Result<List<Account>>
    suspend fun syncAccountData(accountId: String): Result<Account>
}

class AccountRepositoryImpl(
    private val database: NorthDatabase
) : AccountRepository {
    
    override suspend fun insert(entity: Account): Result<Account> {
        return try {
            val now = Clock.System.now()
            
            database.accountQueries.insert(
                id = entity.id,
                userId = "", // This should be passed separately or included in Account model
                institutionId = entity.institutionId,
                institutionName = entity.institutionName,
                accountType = entity.accountType.name,
                balance = entity.balance.amount,
                availableBalance = entity.availableBalance?.amount,
                currency = entity.currency.name,
                lastUpdated = entity.lastUpdated.toEpochMilliseconds(),
                accountNumber = entity.accountNumber,
                transitNumber = entity.transitNumber,
                institutionNumber = entity.institutionNumber,
                nickname = entity.nickname,
                isActive = if (entity.isActive) 1L else 0L,
                createdAt = now.toEpochMilliseconds(),
                updatedAt = now.toEpochMilliseconds()
            )
            
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to insert account", e))
        }
    }
    
    override suspend fun update(entity: Account): Result<Account> {
        return try {
            val now = Clock.System.now()
            
            database.accountQueries.update(
                institutionName = entity.institutionName,
                accountType = entity.accountType.name,
                balance = entity.balance.amount,
                availableBalance = entity.availableBalance?.amount,
                currency = entity.currency.name,
                lastUpdated = entity.lastUpdated.toEpochMilliseconds(),
                accountNumber = entity.accountNumber,
                transitNumber = entity.transitNumber,
                institutionNumber = entity.institutionNumber,
                nickname = entity.nickname,
                isActive = if (entity.isActive) 1L else 0L,
                updatedAt = now.toEpochMilliseconds(),
                id = entity.id
            )
            
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to update account", e))
        }
    }
    
    override suspend fun delete(id: String): Result<Unit> {
        return try {
            database.accountQueries.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to delete account", e))
        }
    }
    
    override suspend fun findById(id: String): Result<Account?> {
        return try {
            val accountRow = database.accountQueries.selectById(id).executeAsOneOrNull()
            val account = accountRow?.let { mapToAccount(it) }
            Result.success(account)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find account by id", e))
        }
    }
    
    override suspend fun findAll(): Result<List<Account>> {
        return try {
            val accounts = database.accountQueries.selectAll().executeAsList()
                .map { mapToAccount(it) }
            Result.success(accounts)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find all accounts", e))
        }
    }
    
    override suspend fun findByUserId(userId: String): Result<List<Account>> {
        return try {
            val accounts = database.accountQueries.selectByUserId(userId).executeAsList()
                .map { mapToAccount(it) }
            Result.success(accounts)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find accounts by user id", e))
        }
    }
    
    override suspend fun findByInstitution(userId: String, institutionId: String): Result<List<Account>> {
        return try {
            val accounts = database.accountQueries.selectByInstitution(userId, institutionId).executeAsList()
                .map { mapToAccount(it) }
            Result.success(accounts)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find accounts by institution", e))
        }
    }
    
    override suspend fun updateBalance(accountId: String, balance: Money): Result<Unit> {
        return try {
            val now = Clock.System.now()
            
            database.accountQueries.updateBalance(
                balance = balance.amount,
                lastUpdated = now.toEpochMilliseconds(),
                updatedAt = now.toEpochMilliseconds(),
                id = accountId
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to update account balance", e))
        }
    }
    
    override suspend fun deactivateAccount(accountId: String): Result<Unit> {
        return try {
            val now = Clock.System.now()
            
            database.accountQueries.deactivate(
                updatedAt = now.toEpochMilliseconds(),
                id = accountId
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to deactivate account", e))
        }
    }
    
    override suspend fun saveAccount(account: Account): Result<Account> {
        return insert(account)
    }
    
    override suspend fun updateAccount(account: Account): Result<Account> {
        return update(account)
    }
    
    override suspend fun getAllAccounts(): List<Account> {
        return try {
            database.accountQueries.selectAll().executeAsList()
                .map { mapToAccount(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun getAccountById(accountId: String): Account? {
        return try {
            val accountRow = database.accountQueries.selectById(accountId).executeAsOneOrNull()
            accountRow?.let { mapToAccount(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    // Implementation of additional methods needed by UI ViewModels
    override suspend fun createAccount(account: Account): Result<Account> {
        return insert(account)
    }
    
    override suspend fun getAccount(accountId: String): Result<Account?> {
        return findById(accountId)
    }
    
    override suspend fun getUserAccounts(userId: String): Result<List<Account>> {
        return findByUserId(userId)
    }
    
    override suspend fun deleteAccount(accountId: String): Result<Unit> {
        return delete(accountId)
    }
    
    override suspend fun getAccountsByInstitution(institutionId: String): Result<List<Account>> {
        return try {
            val accounts = database.accountQueries.selectByInstitutionId(institutionId).executeAsList()
                .map { mapToAccount(it) }
            Result.success(accounts)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find accounts by institution", e))
        }
    }
    
    override suspend fun updateAccountBalance(accountId: String, balance: Money): Result<Unit> {
        return updateBalance(accountId, balance)
    }
    
    override suspend fun getActiveAccounts(userId: String): Result<List<Account>> {
        return try {
            val accounts = database.accountQueries.selectActiveByUserId(userId).executeAsList()
                .map { mapToAccount(it) }
            Result.success(accounts)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find active accounts", e))
        }
    }
    
    override suspend fun syncAccountData(accountId: String): Result<Account> {
        // This would typically involve calling external APIs to refresh account data
        // For now, just return the existing account
        return findById(accountId).mapCatching { account ->
            account ?: throw RepositoryException("Account not found: $accountId")
        }
    }
    
    private fun mapToAccount(accountRow: com.north.mobile.database.Account): Account {
        return Account(
            id = accountRow.id,
            institutionId = accountRow.institutionId,
            institutionName = accountRow.institutionName,
            accountType = AccountType.valueOf(accountRow.accountType),
            balance = Money(accountRow.balance, Currency.valueOf(accountRow.currency)),
            availableBalance = accountRow.availableBalance?.let { 
                Money(it, Currency.valueOf(accountRow.currency)) 
            },
            currency = Currency.valueOf(accountRow.currency),
            lastUpdated = Instant.fromEpochMilliseconds(accountRow.lastUpdated),
            accountNumber = accountRow.accountNumber,
            transitNumber = accountRow.transitNumber,
            institutionNumber = accountRow.institutionNumber,
            nickname = accountRow.nickname,
            isActive = accountRow.isActive == 1L
        )
    }
}