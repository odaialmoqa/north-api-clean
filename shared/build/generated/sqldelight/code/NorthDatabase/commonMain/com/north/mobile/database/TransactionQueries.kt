package com.north.mobile.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class TransactionQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAll(mapper: (
    id: String,
    accountId: String,
    amount: Long,
    description: String,
    category: String?,
    subcategory: String?,
    date: Long,
    isRecurring: Long,
    merchantName: String?,
    location: String?,
    isVerified: Long,
    notes: String?,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = Query(-53_460_030, arrayOf("TransactionEntity"), driver, "Transaction.sq",
      "selectAll", "SELECT * FROM TransactionEntity") { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getString(8),
      cursor.getString(9),
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13)!!
    )
  }

  public fun selectAll(): Query<TransactionEntity> = selectAll { id, accountId, amount, description,
      category, subcategory, date, isRecurring, merchantName, location, isVerified, notes,
      createdAt, updatedAt ->
    TransactionEntity(
      id,
      accountId,
      amount,
      description,
      category,
      subcategory,
      date,
      isRecurring,
      merchantName,
      location,
      isVerified,
      notes,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectByAccountId(accountId: String, mapper: (
    id: String,
    accountId: String,
    amount: Long,
    description: String,
    category: String?,
    subcategory: String?,
    date: Long,
    isRecurring: Long,
    merchantName: String?,
    location: String?,
    isVerified: Long,
    notes: String?,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectByAccountIdQuery(accountId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getString(8),
      cursor.getString(9),
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13)!!
    )
  }

  public fun selectByAccountId(accountId: String): Query<TransactionEntity> =
      selectByAccountId(accountId) { id, accountId_, amount, description, category, subcategory,
      date, isRecurring, merchantName, location, isVerified, notes, createdAt, updatedAt ->
    TransactionEntity(
      id,
      accountId_,
      amount,
      description,
      category,
      subcategory,
      date,
      isRecurring,
      merchantName,
      location,
      isVerified,
      notes,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectByDateRange(
    accountId: String,
    date: Long,
    date_: Long,
    mapper: (
      id: String,
      accountId: String,
      amount: Long,
      description: String,
      category: String?,
      subcategory: String?,
      date: Long,
      isRecurring: Long,
      merchantName: String?,
      location: String?,
      isVerified: Long,
      notes: String?,
      createdAt: Long,
      updatedAt: Long,
    ) -> T,
  ): Query<T> = SelectByDateRangeQuery(accountId, date, date_) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getString(8),
      cursor.getString(9),
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13)!!
    )
  }

  public fun selectByDateRange(
    accountId: String,
    date: Long,
    date_: Long,
  ): Query<TransactionEntity> = selectByDateRange(accountId, date, date_) { id, accountId_, amount,
      description, category, subcategory, date__, isRecurring, merchantName, location, isVerified,
      notes, createdAt, updatedAt ->
    TransactionEntity(
      id,
      accountId_,
      amount,
      description,
      category,
      subcategory,
      date__,
      isRecurring,
      merchantName,
      location,
      isVerified,
      notes,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectByCategory(
    accountId: String,
    category: String?,
    mapper: (
      id: String,
      accountId: String,
      amount: Long,
      description: String,
      category: String?,
      subcategory: String?,
      date: Long,
      isRecurring: Long,
      merchantName: String?,
      location: String?,
      isVerified: Long,
      notes: String?,
      createdAt: Long,
      updatedAt: Long,
    ) -> T,
  ): Query<T> = SelectByCategoryQuery(accountId, category) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getString(8),
      cursor.getString(9),
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13)!!
    )
  }

  public fun selectByCategory(accountId: String, category: String?): Query<TransactionEntity> =
      selectByCategory(accountId, category) { id, accountId_, amount, description, category_,
      subcategory, date, isRecurring, merchantName, location, isVerified, notes, createdAt,
      updatedAt ->
    TransactionEntity(
      id,
      accountId_,
      amount,
      description,
      category_,
      subcategory,
      date,
      isRecurring,
      merchantName,
      location,
      isVerified,
      notes,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectRecent(
    accountId: String,
    `value`: Long,
    mapper: (
      id: String,
      accountId: String,
      amount: Long,
      description: String,
      category: String?,
      subcategory: String?,
      date: Long,
      isRecurring: Long,
      merchantName: String?,
      location: String?,
      isVerified: Long,
      notes: String?,
      createdAt: Long,
      updatedAt: Long,
    ) -> T,
  ): Query<T> = SelectRecentQuery(accountId, value) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getString(8),
      cursor.getString(9),
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13)!!
    )
  }

  public fun selectRecent(accountId: String, value_: Long): Query<TransactionEntity> =
      selectRecent(accountId, value_) { id, accountId_, amount, description, category, subcategory,
      date, isRecurring, merchantName, location, isVerified, notes, createdAt, updatedAt ->
    TransactionEntity(
      id,
      accountId_,
      amount,
      description,
      category,
      subcategory,
      date,
      isRecurring,
      merchantName,
      location,
      isVerified,
      notes,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectUnverified(accountId: String, mapper: (
    id: String,
    accountId: String,
    amount: Long,
    description: String,
    category: String?,
    subcategory: String?,
    date: Long,
    isRecurring: Long,
    merchantName: String?,
    location: String?,
    isVerified: Long,
    notes: String?,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectUnverifiedQuery(accountId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getString(8),
      cursor.getString(9),
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13)!!
    )
  }

  public fun selectUnverified(accountId: String): Query<TransactionEntity> =
      selectUnverified(accountId) { id, accountId_, amount, description, category, subcategory,
      date, isRecurring, merchantName, location, isVerified, notes, createdAt, updatedAt ->
    TransactionEntity(
      id,
      accountId_,
      amount,
      description,
      category,
      subcategory,
      date,
      isRecurring,
      merchantName,
      location,
      isVerified,
      notes,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectByAccountIdPaged(
    accountId: String,
    startDate: Long?,
    endDate: Long?,
    limit: Long,
    offset: Long,
    mapper: (
      id: String,
      accountId: String,
      amount: Long,
      description: String,
      category: String?,
      subcategory: String?,
      date: Long,
      isRecurring: Long,
      merchantName: String?,
      location: String?,
      isVerified: Long,
      notes: String?,
      createdAt: Long,
      updatedAt: Long,
    ) -> T,
  ): Query<T> = SelectByAccountIdPagedQuery(accountId, startDate, endDate, limit, offset) {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getString(8),
      cursor.getString(9),
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13)!!
    )
  }

  public fun selectByAccountIdPaged(
    accountId: String,
    startDate: Long?,
    endDate: Long?,
    limit: Long,
    offset: Long,
  ): Query<TransactionEntity> = selectByAccountIdPaged(accountId, startDate, endDate, limit,
      offset) { id, accountId_, amount, description, category, subcategory, date, isRecurring,
      merchantName, location, isVerified, notes, createdAt, updatedAt ->
    TransactionEntity(
      id,
      accountId_,
      amount,
      description,
      category,
      subcategory,
      date,
      isRecurring,
      merchantName,
      location,
      isVerified,
      notes,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> findDuplicates(
    accountId: String,
    amount: Long,
    date: Long,
    id: String,
    mapper: (
      id: String,
      accountId: String,
      amount: Long,
      description: String,
      category: String?,
      subcategory: String?,
      date: Long,
      isRecurring: Long,
      merchantName: String?,
      location: String?,
      isVerified: Long,
      notes: String?,
      createdAt: Long,
      updatedAt: Long,
    ) -> T,
  ): Query<T> = FindDuplicatesQuery(accountId, amount, date, id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getString(8),
      cursor.getString(9),
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!,
      cursor.getLong(13)!!
    )
  }

  public fun findDuplicates(
    accountId: String,
    amount: Long,
    date: Long,
    id: String,
  ): Query<TransactionEntity> = findDuplicates(accountId, amount, date, id) { id_, accountId_,
      amount_, description, category, subcategory, date_, isRecurring, merchantName, location,
      isVerified, notes, createdAt, updatedAt ->
    TransactionEntity(
      id_,
      accountId_,
      amount_,
      description,
      category,
      subcategory,
      date_,
      isRecurring,
      merchantName,
      location,
      isVerified,
      notes,
      createdAt,
      updatedAt
    )
  }

  public fun insert(
    id: String,
    accountId: String,
    amount: Long,
    description: String,
    category: String?,
    subcategory: String?,
    date: Long,
    isRecurring: Long,
    merchantName: String?,
    location: String?,
    isVerified: Long,
    notes: String?,
    createdAt: Long,
    updatedAt: Long,
  ) {
    driver.execute(-1_535_223_012, """
        |INSERT INTO TransactionEntity(
        |    id, accountId, amount, description, category, subcategory, date,
        |    isRecurring, merchantName, location, isVerified, notes, createdAt, updatedAt
        |) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 14) {
          bindString(0, id)
          bindString(1, accountId)
          bindLong(2, amount)
          bindString(3, description)
          bindString(4, category)
          bindString(5, subcategory)
          bindLong(6, date)
          bindLong(7, isRecurring)
          bindString(8, merchantName)
          bindString(9, location)
          bindLong(10, isVerified)
          bindString(11, notes)
          bindLong(12, createdAt)
          bindLong(13, updatedAt)
        }
    notifyQueries(-1_535_223_012) { emit ->
      emit("TransactionEntity")
    }
  }

  public fun update(
    amount: Long,
    description: String,
    category: String?,
    subcategory: String?,
    date: Long,
    isRecurring: Long,
    merchantName: String?,
    location: String?,
    isVerified: Long,
    notes: String?,
    updatedAt: Long,
    id: String,
  ) {
    driver.execute(-1_190_276_820, """
        |UPDATE TransactionEntity SET 
        |    amount = ?,
        |    description = ?,
        |    category = ?,
        |    subcategory = ?,
        |    date = ?,
        |    isRecurring = ?,
        |    merchantName = ?,
        |    location = ?,
        |    isVerified = ?,
        |    notes = ?,
        |    updatedAt = ?
        |WHERE id = ?
        """.trimMargin(), 12) {
          bindLong(0, amount)
          bindString(1, description)
          bindString(2, category)
          bindString(3, subcategory)
          bindLong(4, date)
          bindLong(5, isRecurring)
          bindString(6, merchantName)
          bindString(7, location)
          bindLong(8, isVerified)
          bindString(9, notes)
          bindLong(10, updatedAt)
          bindString(11, id)
        }
    notifyQueries(-1_190_276_820) { emit ->
      emit("TransactionEntity")
    }
  }

  public fun updateCategory(
    category: String?,
    subcategory: String?,
    updatedAt: Long,
    id: String,
  ) {
    driver.execute(-1_119_025_590,
        """UPDATE TransactionEntity SET category = ?, subcategory = ?, updatedAt = ? WHERE id = ?""",
        4) {
          bindString(0, category)
          bindString(1, subcategory)
          bindLong(2, updatedAt)
          bindString(3, id)
        }
    notifyQueries(-1_119_025_590) { emit ->
      emit("TransactionEntity")
    }
  }

  public fun markVerified(updatedAt: Long, id: String) {
    driver.execute(-719_786_280,
        """UPDATE TransactionEntity SET isVerified = 1, updatedAt = ? WHERE id = ?""", 2) {
          bindLong(0, updatedAt)
          bindString(1, id)
        }
    notifyQueries(-719_786_280) { emit ->
      emit("TransactionEntity")
    }
  }

  public fun delete(id: String) {
    driver.execute(-1_686_888_946, """DELETE FROM TransactionEntity WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(-1_686_888_946) { emit ->
      emit("TransactionEntity")
    }
  }

  public fun updateRecurring(
    isRecurring: Long,
    updatedAt: Long,
    id: String,
  ) {
    driver.execute(-730_149_103,
        """UPDATE TransactionEntity SET isRecurring = ?, updatedAt = ? WHERE id = ?""", 3) {
          bindLong(0, isRecurring)
          bindLong(1, updatedAt)
          bindString(2, id)
        }
    notifyQueries(-730_149_103) { emit ->
      emit("TransactionEntity")
    }
  }

  private inner class SelectByAccountIdQuery<out T : Any>(
    public val accountId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TransactionEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TransactionEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-335_160_654,
        """SELECT * FROM TransactionEntity WHERE accountId = ? ORDER BY date DESC""", mapper, 1) {
      bindString(0, accountId)
    }

    override fun toString(): String = "Transaction.sq:selectByAccountId"
  }

  private inner class SelectByDateRangeQuery<out T : Any>(
    public val accountId: String,
    public val date: Long,
    public val date_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TransactionEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TransactionEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_230_443_705, """
    |SELECT * FROM TransactionEntity 
    |WHERE accountId = ? AND date BETWEEN ? AND ? 
    |ORDER BY date DESC
    """.trimMargin(), mapper, 3) {
      bindString(0, accountId)
      bindLong(1, date)
      bindLong(2, date_)
    }

    override fun toString(): String = "Transaction.sq:selectByDateRange"
  }

  private inner class SelectByCategoryQuery<out T : Any>(
    public val accountId: String,
    public val category: String?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TransactionEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TransactionEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null, """
    |SELECT * FROM TransactionEntity 
    |WHERE accountId = ? AND category ${ if (category == null) "IS" else "=" } ? 
    |ORDER BY date DESC
    """.trimMargin(), mapper, 2) {
      bindString(0, accountId)
      bindString(1, category)
    }

    override fun toString(): String = "Transaction.sq:selectByCategory"
  }

  private inner class SelectRecentQuery<out T : Any>(
    public val accountId: String,
    public val `value`: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TransactionEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TransactionEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_285_176_474, """
    |SELECT * FROM TransactionEntity 
    |WHERE accountId = ? 
    |ORDER BY date DESC 
    |LIMIT ?
    """.trimMargin(), mapper, 2) {
      bindString(0, accountId)
      bindLong(1, value)
    }

    override fun toString(): String = "Transaction.sq:selectRecent"
  }

  private inner class SelectUnverifiedQuery<out T : Any>(
    public val accountId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TransactionEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TransactionEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_795_603_680, """
    |SELECT * FROM TransactionEntity 
    |WHERE accountId = ? AND isVerified = 0 
    |ORDER BY date DESC
    """.trimMargin(), mapper, 1) {
      bindString(0, accountId)
    }

    override fun toString(): String = "Transaction.sq:selectUnverified"
  }

  private inner class SelectByAccountIdPagedQuery<out T : Any>(
    public val accountId: String,
    public val startDate: Long?,
    public val endDate: Long?,
    public val limit: Long,
    public val offset: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TransactionEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TransactionEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(65_405_987, """
    |SELECT * FROM TransactionEntity 
    |WHERE accountId = ? 
    |AND (? IS NULL OR date >= ?)
    |AND (? IS NULL OR date <= ?)
    |ORDER BY date DESC 
    |LIMIT ? OFFSET ?
    """.trimMargin(), mapper, 7) {
      bindString(0, accountId)
      bindLong(1, startDate)
      bindLong(2, startDate)
      bindLong(3, endDate)
      bindLong(4, endDate)
      bindLong(5, limit)
      bindLong(6, offset)
    }

    override fun toString(): String = "Transaction.sq:selectByAccountIdPaged"
  }

  private inner class FindDuplicatesQuery<out T : Any>(
    public val accountId: String,
    public val amount: Long,
    public val date: Long,
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TransactionEntity", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TransactionEntity", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-210_855_740, """
    |SELECT * FROM TransactionEntity 
    |WHERE accountId = ? AND amount = ? AND date = ? AND id != ?
    |ORDER BY date DESC
    """.trimMargin(), mapper, 4) {
      bindString(0, accountId)
      bindLong(1, amount)
      bindLong(2, date)
      bindString(3, id)
    }

    override fun toString(): String = "Transaction.sq:findDuplicates"
  }
}
