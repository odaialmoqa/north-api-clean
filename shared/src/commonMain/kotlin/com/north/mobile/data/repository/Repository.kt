package com.north.mobile.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * Base repository interface for common CRUD operations
 */
interface Repository<T, ID> {
    suspend fun insert(entity: T): Result<T>
    suspend fun update(entity: T): Result<T>
    suspend fun delete(id: ID): Result<Unit>
    suspend fun findById(id: ID): Result<T?>
    suspend fun findAll(): Result<List<T>>
}

/**
 * Repository exception for data access errors
 */
class RepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)