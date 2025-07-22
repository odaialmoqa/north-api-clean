package com.north.mobile.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class AccountQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAll(mapper: (
    id: String,
    userId: String,
    institutionId: String,
    institutionName: String,
    accountType: String,
    balance: Long,
    availableBalance: Long?,
    currency: String,
    lastUpdated: Long,
    accountNumber: String?,
    transitNumber: String?,
    institutionNumber: String?,
    nickname: String?,
    isActive: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = Query(-1_040_944_813, arrayOf("Account"), driver, "Account.sq", "selectAll",
      "SELECT * FROM Account") { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6),
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getString(12),
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getLong(15)!!
    )
  }

  public fun selectAll(): Query<Account> = selectAll { id, userId, institutionId, institutionName,
      accountType, balance, availableBalance, currency, lastUpdated, accountNumber, transitNumber,
      institutionNumber, nickname, isActive, createdAt, updatedAt ->
    Account(
      id,
      userId,
      institutionId,
      institutionName,
      accountType,
      balance,
      availableBalance,
      currency,
      lastUpdated,
      accountNumber,
      transitNumber,
      institutionNumber,
      nickname,
      isActive,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectByUserId(userId: String, mapper: (
    id: String,
    userId: String,
    institutionId: String,
    institutionName: String,
    accountType: String,
    balance: Long,
    availableBalance: Long?,
    currency: String,
    lastUpdated: Long,
    accountNumber: String?,
    transitNumber: String?,
    institutionNumber: String?,
    nickname: String?,
    isActive: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectByUserIdQuery(userId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6),
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getString(12),
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getLong(15)!!
    )
  }

  public fun selectByUserId(userId: String): Query<Account> = selectByUserId(userId) { id, userId_,
      institutionId, institutionName, accountType, balance, availableBalance, currency, lastUpdated,
      accountNumber, transitNumber, institutionNumber, nickname, isActive, createdAt, updatedAt ->
    Account(
      id,
      userId_,
      institutionId,
      institutionName,
      accountType,
      balance,
      availableBalance,
      currency,
      lastUpdated,
      accountNumber,
      transitNumber,
      institutionNumber,
      nickname,
      isActive,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectById(id: String, mapper: (
    id: String,
    userId: String,
    institutionId: String,
    institutionName: String,
    accountType: String,
    balance: Long,
    availableBalance: Long?,
    currency: String,
    lastUpdated: Long,
    accountNumber: String?,
    transitNumber: String?,
    institutionNumber: String?,
    nickname: String?,
    isActive: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6),
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getString(12),
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getLong(15)!!
    )
  }

  public fun selectById(id: String): Query<Account> = selectById(id) { id_, userId, institutionId,
      institutionName, accountType, balance, availableBalance, currency, lastUpdated, accountNumber,
      transitNumber, institutionNumber, nickname, isActive, createdAt, updatedAt ->
    Account(
      id_,
      userId,
      institutionId,
      institutionName,
      accountType,
      balance,
      availableBalance,
      currency,
      lastUpdated,
      accountNumber,
      transitNumber,
      institutionNumber,
      nickname,
      isActive,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectByInstitution(
    userId: String,
    institutionId: String,
    mapper: (
      id: String,
      userId: String,
      institutionId: String,
      institutionName: String,
      accountType: String,
      balance: Long,
      availableBalance: Long?,
      currency: String,
      lastUpdated: Long,
      accountNumber: String?,
      transitNumber: String?,
      institutionNumber: String?,
      nickname: String?,
      isActive: Long,
      createdAt: Long,
      updatedAt: Long,
    ) -> T,
  ): Query<T> = SelectByInstitutionQuery(userId, institutionId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6),
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getString(12),
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getLong(15)!!
    )
  }

  public fun selectByInstitution(userId: String, institutionId: String): Query<Account> =
      selectByInstitution(userId, institutionId) { id, userId_, institutionId_, institutionName,
      accountType, balance, availableBalance, currency, lastUpdated, accountNumber, transitNumber,
      institutionNumber, nickname, isActive, createdAt, updatedAt ->
    Account(
      id,
      userId_,
      institutionId_,
      institutionName,
      accountType,
      balance,
      availableBalance,
      currency,
      lastUpdated,
      accountNumber,
      transitNumber,
      institutionNumber,
      nickname,
      isActive,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectByInstitutionId(institutionId: String, mapper: (
    id: String,
    userId: String,
    institutionId: String,
    institutionName: String,
    accountType: String,
    balance: Long,
    availableBalance: Long?,
    currency: String,
    lastUpdated: Long,
    accountNumber: String?,
    transitNumber: String?,
    institutionNumber: String?,
    nickname: String?,
    isActive: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectByInstitutionIdQuery(institutionId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6),
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getString(12),
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getLong(15)!!
    )
  }

  public fun selectByInstitutionId(institutionId: String): Query<Account> =
      selectByInstitutionId(institutionId) { id, userId, institutionId_, institutionName,
      accountType, balance, availableBalance, currency, lastUpdated, accountNumber, transitNumber,
      institutionNumber, nickname, isActive, createdAt, updatedAt ->
    Account(
      id,
      userId,
      institutionId_,
      institutionName,
      accountType,
      balance,
      availableBalance,
      currency,
      lastUpdated,
      accountNumber,
      transitNumber,
      institutionNumber,
      nickname,
      isActive,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectActiveByUserId(userId: String, mapper: (
    id: String,
    userId: String,
    institutionId: String,
    institutionName: String,
    accountType: String,
    balance: Long,
    availableBalance: Long?,
    currency: String,
    lastUpdated: Long,
    accountNumber: String?,
    transitNumber: String?,
    institutionNumber: String?,
    nickname: String?,
    isActive: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectActiveByUserIdQuery(userId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6),
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getString(12),
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getLong(15)!!
    )
  }

  public fun selectActiveByUserId(userId: String): Query<Account> = selectActiveByUserId(userId) {
      id, userId_, institutionId, institutionName, accountType, balance, availableBalance, currency,
      lastUpdated, accountNumber, transitNumber, institutionNumber, nickname, isActive, createdAt,
      updatedAt ->
    Account(
      id,
      userId_,
      institutionId,
      institutionName,
      accountType,
      balance,
      availableBalance,
      currency,
      lastUpdated,
      accountNumber,
      transitNumber,
      institutionNumber,
      nickname,
      isActive,
      createdAt,
      updatedAt
    )
  }

  public fun insert(
    id: String,
    userId: String,
    institutionId: String,
    institutionName: String,
    accountType: String,
    balance: Long,
    availableBalance: Long?,
    currency: String,
    lastUpdated: Long,
    accountNumber: String?,
    transitNumber: String?,
    institutionNumber: String?,
    nickname: String?,
    isActive: Long,
    createdAt: Long,
    updatedAt: Long,
  ) {
    driver.execute(666_795_819, """
        |INSERT INTO Account(
        |    id, userId, institutionId, institutionName, accountType, balance, availableBalance,
        |    currency, lastUpdated, accountNumber, transitNumber, institutionNumber, nickname,
        |    isActive, createdAt, updatedAt
        |) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 16) {
          bindString(0, id)
          bindString(1, userId)
          bindString(2, institutionId)
          bindString(3, institutionName)
          bindString(4, accountType)
          bindLong(5, balance)
          bindLong(6, availableBalance)
          bindString(7, currency)
          bindLong(8, lastUpdated)
          bindString(9, accountNumber)
          bindString(10, transitNumber)
          bindString(11, institutionNumber)
          bindString(12, nickname)
          bindLong(13, isActive)
          bindLong(14, createdAt)
          bindLong(15, updatedAt)
        }
    notifyQueries(666_795_819) { emit ->
      emit("Account")
    }
  }

  public fun update(
    institutionName: String,
    accountType: String,
    balance: Long,
    availableBalance: Long?,
    currency: String,
    lastUpdated: Long,
    accountNumber: String?,
    transitNumber: String?,
    institutionNumber: String?,
    nickname: String?,
    isActive: Long,
    updatedAt: Long,
    id: String,
  ) {
    driver.execute(1_011_742_011, """
        |UPDATE Account SET 
        |    institutionName = ?,
        |    accountType = ?,
        |    balance = ?,
        |    availableBalance = ?,
        |    currency = ?,
        |    lastUpdated = ?,
        |    accountNumber = ?,
        |    transitNumber = ?,
        |    institutionNumber = ?,
        |    nickname = ?,
        |    isActive = ?,
        |    updatedAt = ?
        |WHERE id = ?
        """.trimMargin(), 13) {
          bindString(0, institutionName)
          bindString(1, accountType)
          bindLong(2, balance)
          bindLong(3, availableBalance)
          bindString(4, currency)
          bindLong(5, lastUpdated)
          bindString(6, accountNumber)
          bindString(7, transitNumber)
          bindString(8, institutionNumber)
          bindString(9, nickname)
          bindLong(10, isActive)
          bindLong(11, updatedAt)
          bindString(12, id)
        }
    notifyQueries(1_011_742_011) { emit ->
      emit("Account")
    }
  }

  public fun updateBalance(
    balance: Long,
    lastUpdated: Long,
    updatedAt: Long,
    id: String,
  ) {
    driver.execute(-2_098_542_303,
        """UPDATE Account SET balance = ?, lastUpdated = ?, updatedAt = ? WHERE id = ?""", 4) {
          bindLong(0, balance)
          bindLong(1, lastUpdated)
          bindLong(2, updatedAt)
          bindString(3, id)
        }
    notifyQueries(-2_098_542_303) { emit ->
      emit("Account")
    }
  }

  public fun deactivate(updatedAt: Long, id: String) {
    driver.execute(1_750_205_414, """UPDATE Account SET isActive = 0, updatedAt = ? WHERE id = ?""",
        2) {
          bindLong(0, updatedAt)
          bindString(1, id)
        }
    notifyQueries(1_750_205_414) { emit ->
      emit("Account")
    }
  }

  public fun delete(id: String) {
    driver.execute(515_129_885, """DELETE FROM Account WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(515_129_885) { emit ->
      emit("Account")
      emit("TransactionEntity")
    }
  }

  private inner class SelectByUserIdQuery<out T : Any>(
    public val userId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Account", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Account", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_365_028_597,
        """SELECT * FROM Account WHERE userId = ? AND isActive = 1""", mapper, 1) {
      bindString(0, userId)
    }

    override fun toString(): String = "Account.sq:selectByUserId"
  }

  private inner class SelectByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Account", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Account", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(2_090_490_464, """SELECT * FROM Account WHERE id = ?""", mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "Account.sq:selectById"
  }

  private inner class SelectByInstitutionQuery<out T : Any>(
    public val userId: String,
    public val institutionId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Account", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Account", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(882_996_755,
        """SELECT * FROM Account WHERE userId = ? AND institutionId = ? AND isActive = 1""", mapper,
        2) {
      bindString(0, userId)
      bindString(1, institutionId)
    }

    override fun toString(): String = "Account.sq:selectByInstitution"
  }

  private inner class SelectByInstitutionIdQuery<out T : Any>(
    public val institutionId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Account", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Account", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_843_640_690,
        """SELECT * FROM Account WHERE institutionId = ? AND isActive = 1""", mapper, 1) {
      bindString(0, institutionId)
    }

    override fun toString(): String = "Account.sq:selectByInstitutionId"
  }

  private inner class SelectActiveByUserIdQuery<out T : Any>(
    public val userId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Account", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Account", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_943_245_903,
        """SELECT * FROM Account WHERE userId = ? AND isActive = 1""", mapper, 1) {
      bindString(0, userId)
    }

    override fun toString(): String = "Account.sq:selectActiveByUserId"
  }
}
