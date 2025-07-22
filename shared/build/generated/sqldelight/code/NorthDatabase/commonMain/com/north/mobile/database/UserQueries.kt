package com.north.mobile.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class UserQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAll(mapper: (
    id: String,
    email: String,
    firstName: String,
    lastName: String,
    phoneNumber: String?,
    dateOfBirth: String?,
    currency: String,
    language: String,
    notificationsEnabled: Long,
    biometricAuthEnabled: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = Query(189_892_739, arrayOf("User"), driver, "User.sq", "selectAll",
      "SELECT * FROM User") { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!
    )
  }

  public fun selectAll(): Query<User> = selectAll { id, email, firstName, lastName, phoneNumber,
      dateOfBirth, currency, language, notificationsEnabled, biometricAuthEnabled, createdAt,
      updatedAt ->
    User(
      id,
      email,
      firstName,
      lastName,
      phoneNumber,
      dateOfBirth,
      currency,
      language,
      notificationsEnabled,
      biometricAuthEnabled,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectById(id: String, mapper: (
    id: String,
    email: String,
    firstName: String,
    lastName: String,
    phoneNumber: String?,
    dateOfBirth: String?,
    currency: String,
    language: String,
    notificationsEnabled: Long,
    biometricAuthEnabled: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!
    )
  }

  public fun selectById(id: String): Query<User> = selectById(id) { id_, email, firstName, lastName,
      phoneNumber, dateOfBirth, currency, language, notificationsEnabled, biometricAuthEnabled,
      createdAt, updatedAt ->
    User(
      id_,
      email,
      firstName,
      lastName,
      phoneNumber,
      dateOfBirth,
      currency,
      language,
      notificationsEnabled,
      biometricAuthEnabled,
      createdAt,
      updatedAt
    )
  }

  public fun insert(
    id: String,
    email: String,
    firstName: String,
    lastName: String,
    phoneNumber: String?,
    dateOfBirth: String?,
    currency: String,
    language: String,
    notificationsEnabled: Long,
    biometricAuthEnabled: Long,
    createdAt: Long,
    updatedAt: Long,
  ) {
    driver.execute(-1_873_005_061, """
        |INSERT INTO User(id, email, firstName, lastName, phoneNumber, dateOfBirth, currency, language, notificationsEnabled, biometricAuthEnabled, createdAt, updatedAt)
        |VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 12) {
          bindString(0, id)
          bindString(1, email)
          bindString(2, firstName)
          bindString(3, lastName)
          bindString(4, phoneNumber)
          bindString(5, dateOfBirth)
          bindString(6, currency)
          bindString(7, language)
          bindLong(8, notificationsEnabled)
          bindLong(9, biometricAuthEnabled)
          bindLong(10, createdAt)
          bindLong(11, updatedAt)
        }
    notifyQueries(-1_873_005_061) { emit ->
      emit("User")
    }
  }

  public fun update(
    email: String,
    firstName: String,
    lastName: String,
    phoneNumber: String?,
    dateOfBirth: String?,
    currency: String,
    language: String,
    notificationsEnabled: Long,
    biometricAuthEnabled: Long,
    updatedAt: Long,
    id: String,
  ) {
    driver.execute(-1_528_058_869, """
        |UPDATE User SET 
        |    email = ?, 
        |    firstName = ?, 
        |    lastName = ?, 
        |    phoneNumber = ?, 
        |    dateOfBirth = ?, 
        |    currency = ?, 
        |    language = ?, 
        |    notificationsEnabled = ?, 
        |    biometricAuthEnabled = ?, 
        |    updatedAt = ?
        |WHERE id = ?
        """.trimMargin(), 11) {
          bindString(0, email)
          bindString(1, firstName)
          bindString(2, lastName)
          bindString(3, phoneNumber)
          bindString(4, dateOfBirth)
          bindString(5, currency)
          bindString(6, language)
          bindLong(7, notificationsEnabled)
          bindLong(8, biometricAuthEnabled)
          bindLong(9, updatedAt)
          bindString(10, id)
        }
    notifyQueries(-1_528_058_869) { emit ->
      emit("User")
    }
  }

  public fun delete(id: String) {
    driver.execute(-2_024_670_995, """DELETE FROM User WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(-2_024_670_995) { emit ->
      emit("Account")
      emit("Achievement")
      emit("FinancialGoal")
      emit("GamificationProfile")
      emit("PointsHistory")
      emit("Streak")
      emit("StreakRecovery")
      emit("StreakReminder")
      emit("User")
    }
  }

  private inner class SelectByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("User", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("User", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_591_748_912, """SELECT * FROM User WHERE id = ?""", mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "User.sq:selectById"
  }
}
