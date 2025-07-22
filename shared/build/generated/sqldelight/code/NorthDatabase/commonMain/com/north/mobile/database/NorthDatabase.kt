package com.north.mobile.database

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.north.mobile.database.shared.newInstance
import com.north.mobile.database.shared.schema
import kotlin.Unit

public interface NorthDatabase : Transacter {
  public val accountQueries: AccountQueries

  public val financialGoalQueries: FinancialGoalQueries

  public val gamificationQueries: GamificationQueries

  public val microTaskQueries: MicroTaskQueries

  public val privacyQueries: PrivacyQueries

  public val transactionQueries: TransactionQueries

  public val userQueries: UserQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = NorthDatabase::class.schema

    public operator fun invoke(driver: SqlDriver): NorthDatabase =
        NorthDatabase::class.newInstance(driver)
  }
}
