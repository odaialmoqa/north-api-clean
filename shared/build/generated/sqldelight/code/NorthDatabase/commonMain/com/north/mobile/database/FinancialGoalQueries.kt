package com.north.mobile.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class FinancialGoalQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAll(mapper: (
    id: String,
    userId: String,
    title: String,
    description: String?,
    targetAmount: Long,
    currentAmount: Long,
    currency: String,
    targetDate: Long,
    priority: Long,
    category: String,
    isActive: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = Query(759_852_516, arrayOf("FinancialGoal"), driver, "FinancialGoal.sq",
      "selectAll", "SELECT * FROM FinancialGoal") { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getString(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!
    )
  }

  public fun selectAll(): Query<FinancialGoal> = selectAll { id, userId, title, description,
      targetAmount, currentAmount, currency, targetDate, priority, category, isActive, createdAt,
      updatedAt ->
    FinancialGoal(
      id,
      userId,
      title,
      description,
      targetAmount,
      currentAmount,
      currency,
      targetDate,
      priority,
      category,
      isActive,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectByUserId(userId: String, mapper: (
    id: String,
    userId: String,
    title: String,
    description: String?,
    targetAmount: Long,
    currentAmount: Long,
    currency: String,
    targetDate: Long,
    priority: Long,
    category: String,
    isActive: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectByUserIdQuery(userId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getString(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!
    )
  }

  public fun selectByUserId(userId: String): Query<FinancialGoal> = selectByUserId(userId) { id,
      userId_, title, description, targetAmount, currentAmount, currency, targetDate, priority,
      category, isActive, createdAt, updatedAt ->
    FinancialGoal(
      id,
      userId_,
      title,
      description,
      targetAmount,
      currentAmount,
      currency,
      targetDate,
      priority,
      category,
      isActive,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectById(id: String, mapper: (
    id: String,
    userId: String,
    title: String,
    description: String?,
    targetAmount: Long,
    currentAmount: Long,
    currency: String,
    targetDate: Long,
    priority: Long,
    category: String,
    isActive: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getString(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!
    )
  }

  public fun selectById(id: String): Query<FinancialGoal> = selectById(id) { id_, userId, title,
      description, targetAmount, currentAmount, currency, targetDate, priority, category, isActive,
      createdAt, updatedAt ->
    FinancialGoal(
      id_,
      userId,
      title,
      description,
      targetAmount,
      currentAmount,
      currency,
      targetDate,
      priority,
      category,
      isActive,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectByCategory(
    userId: String,
    category: String,
    mapper: (
      id: String,
      userId: String,
      title: String,
      description: String?,
      targetAmount: Long,
      currentAmount: Long,
      currency: String,
      targetDate: Long,
      priority: Long,
      category: String,
      isActive: Long,
      createdAt: Long,
      updatedAt: Long,
    ) -> T,
  ): Query<T> = SelectByCategoryQuery(userId, category) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getString(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!
    )
  }

  public fun selectByCategory(userId: String, category: String): Query<FinancialGoal> =
      selectByCategory(userId, category) { id, userId_, title, description, targetAmount,
      currentAmount, currency, targetDate, priority, category_, isActive, createdAt, updatedAt ->
    FinancialGoal(
      id,
      userId_,
      title,
      description,
      targetAmount,
      currentAmount,
      currency,
      targetDate,
      priority,
      category_,
      isActive,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectActive(userId: String, mapper: (
    id: String,
    userId: String,
    title: String,
    description: String?,
    targetAmount: Long,
    currentAmount: Long,
    currency: String,
    targetDate: Long,
    priority: Long,
    category: String,
    isActive: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectActiveQuery(userId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getString(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!
    )
  }

  public fun selectActive(userId: String): Query<FinancialGoal> = selectActive(userId) { id,
      userId_, title, description, targetAmount, currentAmount, currency, targetDate, priority,
      category, isActive, createdAt, updatedAt ->
    FinancialGoal(
      id,
      userId_,
      title,
      description,
      targetAmount,
      currentAmount,
      currency,
      targetDate,
      priority,
      category,
      isActive,
      createdAt,
      updatedAt
    )
  }

  public fun insert(
    id: String,
    userId: String,
    title: String,
    description: String?,
    targetAmount: Long,
    currentAmount: Long,
    currency: String,
    targetDate: Long,
    priority: Long,
    category: String,
    isActive: Long,
    createdAt: Long,
    updatedAt: Long,
  ) {
    driver.execute(-340_315_078, """
        |INSERT INTO FinancialGoal(
        |    id, userId, title, description, targetAmount, currentAmount, currency,
        |    targetDate, priority, category, isActive, createdAt, updatedAt
        |) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 13) {
          bindString(0, id)
          bindString(1, userId)
          bindString(2, title)
          bindString(3, description)
          bindLong(4, targetAmount)
          bindLong(5, currentAmount)
          bindString(6, currency)
          bindLong(7, targetDate)
          bindLong(8, priority)
          bindString(9, category)
          bindLong(10, isActive)
          bindLong(11, createdAt)
          bindLong(12, updatedAt)
        }
    notifyQueries(-340_315_078) { emit ->
      emit("FinancialGoal")
    }
  }

  public fun update(
    title: String,
    description: String?,
    targetAmount: Long,
    currentAmount: Long,
    currency: String,
    targetDate: Long,
    priority: Long,
    category: String,
    isActive: Long,
    updatedAt: Long,
    id: String,
  ) {
    driver.execute(4_631_114, """
        |UPDATE FinancialGoal SET 
        |    title = ?,
        |    description = ?,
        |    targetAmount = ?,
        |    currentAmount = ?,
        |    currency = ?,
        |    targetDate = ?,
        |    priority = ?,
        |    category = ?,
        |    isActive = ?,
        |    updatedAt = ?
        |WHERE id = ?
        """.trimMargin(), 11) {
          bindString(0, title)
          bindString(1, description)
          bindLong(2, targetAmount)
          bindLong(3, currentAmount)
          bindString(4, currency)
          bindLong(5, targetDate)
          bindLong(6, priority)
          bindString(7, category)
          bindLong(8, isActive)
          bindLong(9, updatedAt)
          bindString(10, id)
        }
    notifyQueries(4_631_114) { emit ->
      emit("FinancialGoal")
    }
  }

  public fun updateProgress(
    currentAmount: Long,
    updatedAt: Long,
    id: String,
  ) {
    driver.execute(1_878_255_255,
        """UPDATE FinancialGoal SET currentAmount = ?, updatedAt = ? WHERE id = ?""", 3) {
          bindLong(0, currentAmount)
          bindLong(1, updatedAt)
          bindString(2, id)
        }
    notifyQueries(1_878_255_255) { emit ->
      emit("FinancialGoal")
    }
  }

  public fun deactivate(updatedAt: Long, id: String) {
    driver.execute(1_740_347_765,
        """UPDATE FinancialGoal SET isActive = 0, updatedAt = ? WHERE id = ?""", 2) {
          bindLong(0, updatedAt)
          bindString(1, id)
        }
    notifyQueries(1_740_347_765) { emit ->
      emit("FinancialGoal")
    }
  }

  public fun delete(id: String) {
    driver.execute(-491_981_012, """DELETE FROM FinancialGoal WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(-491_981_012) { emit ->
      emit("FinancialGoal")
      emit("MicroTask")
    }
  }

  private inner class SelectByUserIdQuery<out T : Any>(
    public val userId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("FinancialGoal", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("FinancialGoal", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(219_776_794,
        """SELECT * FROM FinancialGoal WHERE userId = ? AND isActive = 1 ORDER BY priority DESC, targetDate ASC""",
        mapper, 1) {
      bindString(0, userId)
    }

    override fun toString(): String = "FinancialGoal.sq:selectByUserId"
  }

  private inner class SelectByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("FinancialGoal", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("FinancialGoal", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(2_080_632_815, """SELECT * FROM FinancialGoal WHERE id = ?""", mapper,
        1) {
      bindString(0, id)
    }

    override fun toString(): String = "FinancialGoal.sq:selectById"
  }

  private inner class SelectByCategoryQuery<out T : Any>(
    public val userId: String,
    public val category: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("FinancialGoal", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("FinancialGoal", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_069_428_946,
        """SELECT * FROM FinancialGoal WHERE userId = ? AND category = ? AND isActive = 1""",
        mapper, 2) {
      bindString(0, userId)
      bindString(1, category)
    }

    override fun toString(): String = "FinancialGoal.sq:selectByCategory"
  }

  private inner class SelectActiveQuery<out T : Any>(
    public val userId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("FinancialGoal", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("FinancialGoal", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-2_014_281_757,
        """SELECT * FROM FinancialGoal WHERE userId = ? AND isActive = 1""", mapper, 1) {
      bindString(0, userId)
    }

    override fun toString(): String = "FinancialGoal.sq:selectActive"
  }
}
