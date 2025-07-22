package com.north.mobile.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class MicroTaskQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAll(mapper: (
    id: String,
    goalId: String,
    title: String,
    description: String,
    targetAmount: Long,
    isCompleted: Long,
    dueDate: Long?,
    completedAt: Long?,
    createdAt: Long,
  ) -> T): Query<T> = Query(-1_567_528_873, arrayOf("MicroTask"), driver, "MicroTask.sq",
      "selectAll", "SELECT * FROM MicroTask") { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6),
      cursor.getLong(7),
      cursor.getLong(8)!!
    )
  }

  public fun selectAll(): Query<MicroTask> = selectAll { id, goalId, title, description,
      targetAmount, isCompleted, dueDate, completedAt, createdAt ->
    MicroTask(
      id,
      goalId,
      title,
      description,
      targetAmount,
      isCompleted,
      dueDate,
      completedAt,
      createdAt
    )
  }

  public fun <T : Any> selectByGoalId(goalId: String, mapper: (
    id: String,
    goalId: String,
    title: String,
    description: String,
    targetAmount: Long,
    isCompleted: Long,
    dueDate: Long?,
    completedAt: Long?,
    createdAt: Long,
  ) -> T): Query<T> = SelectByGoalIdQuery(goalId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6),
      cursor.getLong(7),
      cursor.getLong(8)!!
    )
  }

  public fun selectByGoalId(goalId: String): Query<MicroTask> = selectByGoalId(goalId) { id,
      goalId_, title, description, targetAmount, isCompleted, dueDate, completedAt, createdAt ->
    MicroTask(
      id,
      goalId_,
      title,
      description,
      targetAmount,
      isCompleted,
      dueDate,
      completedAt,
      createdAt
    )
  }

  public fun <T : Any> selectById(id: String, mapper: (
    id: String,
    goalId: String,
    title: String,
    description: String,
    targetAmount: Long,
    isCompleted: Long,
    dueDate: Long?,
    completedAt: Long?,
    createdAt: Long,
  ) -> T): Query<T> = SelectByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6),
      cursor.getLong(7),
      cursor.getLong(8)!!
    )
  }

  public fun selectById(id: String): Query<MicroTask> = selectById(id) { id_, goalId, title,
      description, targetAmount, isCompleted, dueDate, completedAt, createdAt ->
    MicroTask(
      id_,
      goalId,
      title,
      description,
      targetAmount,
      isCompleted,
      dueDate,
      completedAt,
      createdAt
    )
  }

  public fun <T : Any> selectPending(goalId: String, mapper: (
    id: String,
    goalId: String,
    title: String,
    description: String,
    targetAmount: Long,
    isCompleted: Long,
    dueDate: Long?,
    completedAt: Long?,
    createdAt: Long,
  ) -> T): Query<T> = SelectPendingQuery(goalId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6),
      cursor.getLong(7),
      cursor.getLong(8)!!
    )
  }

  public fun selectPending(goalId: String): Query<MicroTask> = selectPending(goalId) { id, goalId_,
      title, description, targetAmount, isCompleted, dueDate, completedAt, createdAt ->
    MicroTask(
      id,
      goalId_,
      title,
      description,
      targetAmount,
      isCompleted,
      dueDate,
      completedAt,
      createdAt
    )
  }

  public fun <T : Any> selectCompleted(goalId: String, mapper: (
    id: String,
    goalId: String,
    title: String,
    description: String,
    targetAmount: Long,
    isCompleted: Long,
    dueDate: Long?,
    completedAt: Long?,
    createdAt: Long,
  ) -> T): Query<T> = SelectCompletedQuery(goalId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6),
      cursor.getLong(7),
      cursor.getLong(8)!!
    )
  }

  public fun selectCompleted(goalId: String): Query<MicroTask> = selectCompleted(goalId) { id,
      goalId_, title, description, targetAmount, isCompleted, dueDate, completedAt, createdAt ->
    MicroTask(
      id,
      goalId_,
      title,
      description,
      targetAmount,
      isCompleted,
      dueDate,
      completedAt,
      createdAt
    )
  }

  public fun <T : Any> selectOverdue(
    goalId: String,
    dueDate: Long?,
    mapper: (
      id: String,
      goalId: String,
      title: String,
      description: String,
      targetAmount: Long,
      isCompleted: Long,
      dueDate: Long?,
      completedAt: Long?,
      createdAt: Long,
    ) -> T,
  ): Query<T> = SelectOverdueQuery(goalId, dueDate) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6),
      cursor.getLong(7),
      cursor.getLong(8)!!
    )
  }

  public fun selectOverdue(goalId: String, dueDate: Long?): Query<MicroTask> = selectOverdue(goalId,
      dueDate) { id, goalId_, title, description, targetAmount, isCompleted, dueDate_, completedAt,
      createdAt ->
    MicroTask(
      id,
      goalId_,
      title,
      description,
      targetAmount,
      isCompleted,
      dueDate_,
      completedAt,
      createdAt
    )
  }

  public fun insert(
    id: String,
    goalId: String,
    title: String,
    description: String,
    targetAmount: Long,
    isCompleted: Long,
    dueDate: Long?,
    completedAt: Long?,
    createdAt: Long,
  ) {
    driver.execute(1_179_158_183, """
        |INSERT INTO MicroTask(
        |    id, goalId, title, description, targetAmount, isCompleted, dueDate, completedAt, createdAt
        |) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 9) {
          bindString(0, id)
          bindString(1, goalId)
          bindString(2, title)
          bindString(3, description)
          bindLong(4, targetAmount)
          bindLong(5, isCompleted)
          bindLong(6, dueDate)
          bindLong(7, completedAt)
          bindLong(8, createdAt)
        }
    notifyQueries(1_179_158_183) { emit ->
      emit("MicroTask")
    }
  }

  public fun update(
    title: String,
    description: String,
    targetAmount: Long,
    isCompleted: Long,
    dueDate: Long?,
    completedAt: Long?,
    id: String,
  ) {
    driver.execute(1_524_104_375, """
        |UPDATE MicroTask SET 
        |    title = ?,
        |    description = ?,
        |    targetAmount = ?,
        |    isCompleted = ?,
        |    dueDate = ?,
        |    completedAt = ?
        |WHERE id = ?
        """.trimMargin(), 7) {
          bindString(0, title)
          bindString(1, description)
          bindLong(2, targetAmount)
          bindLong(3, isCompleted)
          bindLong(4, dueDate)
          bindLong(5, completedAt)
          bindString(6, id)
        }
    notifyQueries(1_524_104_375) { emit ->
      emit("MicroTask")
    }
  }

  public fun complete(
    isCompleted: Long,
    completedAt: Long?,
    id: String,
  ) {
    driver.execute(-1_841_581_657,
        """UPDATE MicroTask SET isCompleted = ?, completedAt = ? WHERE id = ?""", 3) {
          bindLong(0, isCompleted)
          bindLong(1, completedAt)
          bindString(2, id)
        }
    notifyQueries(-1_841_581_657) { emit ->
      emit("MicroTask")
    }
  }

  public fun delete(id: String) {
    driver.execute(1_027_492_249, """DELETE FROM MicroTask WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(1_027_492_249) { emit ->
      emit("MicroTask")
    }
  }

  public fun deleteByGoalId(goalId: String) {
    driver.execute(-2_011_353_218, """DELETE FROM MicroTask WHERE goalId = ?""", 1) {
          bindString(0, goalId)
        }
    notifyQueries(-2_011_353_218) { emit ->
      emit("MicroTask")
    }
  }

  private inner class SelectByGoalIdQuery<out T : Any>(
    public val goalId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("MicroTask", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("MicroTask", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(993_918_415,
        """SELECT * FROM MicroTask WHERE goalId = ? ORDER BY dueDate ASC, createdAt ASC""", mapper,
        1) {
      bindString(0, goalId)
    }

    override fun toString(): String = "MicroTask.sq:selectByGoalId"
  }

  private inner class SelectByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("MicroTask", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("MicroTask", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_348_713_508, """SELECT * FROM MicroTask WHERE id = ?""", mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "MicroTask.sq:selectById"
  }

  private inner class SelectPendingQuery<out T : Any>(
    public val goalId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("MicroTask", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("MicroTask", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_103_221_427,
        """SELECT * FROM MicroTask WHERE goalId = ? AND isCompleted = 0 ORDER BY dueDate ASC""",
        mapper, 1) {
      bindString(0, goalId)
    }

    override fun toString(): String = "MicroTask.sq:selectPending"
  }

  private inner class SelectCompletedQuery<out T : Any>(
    public val goalId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("MicroTask", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("MicroTask", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_904_966_527,
        """SELECT * FROM MicroTask WHERE goalId = ? AND isCompleted = 1 ORDER BY completedAt DESC""",
        mapper, 1) {
      bindString(0, goalId)
    }

    override fun toString(): String = "MicroTask.sq:selectCompleted"
  }

  private inner class SelectOverdueQuery<out T : Any>(
    public val goalId: String,
    public val dueDate: Long?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("MicroTask", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("MicroTask", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_511_928_746,
        """SELECT * FROM MicroTask WHERE goalId = ? AND isCompleted = 0 AND dueDate < ? ORDER BY dueDate ASC""",
        mapper, 2) {
      bindString(0, goalId)
      bindLong(1, dueDate)
    }

    override fun toString(): String = "MicroTask.sq:selectOverdue"
  }
}
