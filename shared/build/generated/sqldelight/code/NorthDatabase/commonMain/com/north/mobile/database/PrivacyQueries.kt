package com.north.mobile.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Long
import kotlin.String

public class PrivacyQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> getLatestConsentsForUser(user_id: String, mapper: (
    id: String,
    user_id: String,
    purpose: String,
    granted: Long,
    timestamp: Long,
    ip_address: String?,
    user_agent: String?,
    version: String,
    expiry_date: Long?,
    created_at: Long,
  ) -> T): Query<T> = GetLatestConsentsForUserQuery(user_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7)!!,
      cursor.getLong(8),
      cursor.getLong(9)!!
    )
  }

  public fun getLatestConsentsForUser(user_id: String): Query<Consent_records> =
      getLatestConsentsForUser(user_id) { id, user_id_, purpose, granted, timestamp, ip_address,
      user_agent, version, expiry_date, created_at ->
    Consent_records(
      id,
      user_id_,
      purpose,
      granted,
      timestamp,
      ip_address,
      user_agent,
      version,
      expiry_date,
      created_at
    )
  }

  public fun <T : Any> getLatestConsentForPurpose(
    user_id: String,
    purpose: String,
    mapper: (
      id: String,
      user_id: String,
      purpose: String,
      granted: Long,
      timestamp: Long,
      ip_address: String?,
      user_agent: String?,
      version: String,
      expiry_date: Long?,
      created_at: Long,
    ) -> T,
  ): Query<T> = GetLatestConsentForPurposeQuery(user_id, purpose) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7)!!,
      cursor.getLong(8),
      cursor.getLong(9)!!
    )
  }

  public fun getLatestConsentForPurpose(user_id: String, purpose: String): Query<Consent_records> =
      getLatestConsentForPurpose(user_id, purpose) { id, user_id_, purpose_, granted, timestamp,
      ip_address, user_agent, version, expiry_date, created_at ->
    Consent_records(
      id,
      user_id_,
      purpose_,
      granted,
      timestamp,
      ip_address,
      user_agent,
      version,
      expiry_date,
      created_at
    )
  }

  public fun <T : Any> getConsentHistory(user_id: String, mapper: (
    id: String,
    user_id: String,
    purpose: String,
    granted: Long,
    timestamp: Long,
    ip_address: String?,
    user_agent: String?,
    version: String,
    expiry_date: Long?,
    created_at: Long,
  ) -> T): Query<T> = GetConsentHistoryQuery(user_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7)!!,
      cursor.getLong(8),
      cursor.getLong(9)!!
    )
  }

  public fun getConsentHistory(user_id: String): Query<Consent_records> =
      getConsentHistory(user_id) { id, user_id_, purpose, granted, timestamp, ip_address,
      user_agent, version, expiry_date, created_at ->
    Consent_records(
      id,
      user_id_,
      purpose,
      granted,
      timestamp,
      ip_address,
      user_agent,
      version,
      expiry_date,
      created_at
    )
  }

  public fun hasUserConsents(user_id: String): Query<Boolean> = HasUserConsentsQuery(user_id) {
      cursor ->
    cursor.getBoolean(0)!!
  }

  public fun <T : Any> getConsentPreferences(user_id: String, mapper: (
    user_id: String,
    marketing_opt_in: Long,
    analytics_opt_in: Long,
    data_retention_period: String,
    updated_at: Long,
  ) -> T): Query<T> = GetConsentPreferencesQuery(user_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getLong(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!,
      cursor.getLong(4)!!
    )
  }

  public fun getConsentPreferences(user_id: String): Query<Consent_preferences> =
      getConsentPreferences(user_id) { user_id_, marketing_opt_in, analytics_opt_in,
      data_retention_period, updated_at ->
    Consent_preferences(
      user_id_,
      marketing_opt_in,
      analytics_opt_in,
      data_retention_period,
      updated_at
    )
  }

  public fun <T : Any> getDataExportRequest(id: String, mapper: (
    id: String,
    user_id: String,
    format: String,
    requested_at: Long,
    status: String,
    completed_at: Long?,
    download_url: String?,
    expires_at: Long?,
    file_size: Long?,
    created_at: Long,
  ) -> T): Query<T> = GetDataExportRequestQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getString(4)!!,
      cursor.getLong(5),
      cursor.getString(6),
      cursor.getLong(7),
      cursor.getLong(8),
      cursor.getLong(9)!!
    )
  }

  public fun getDataExportRequest(id: String): Query<Data_export_requests> =
      getDataExportRequest(id) { id_, user_id, format, requested_at, status, completed_at,
      download_url, expires_at, file_size, created_at ->
    Data_export_requests(
      id_,
      user_id,
      format,
      requested_at,
      status,
      completed_at,
      download_url,
      expires_at,
      file_size,
      created_at
    )
  }

  public fun <T : Any> getDataExportHistory(user_id: String, mapper: (
    id: String,
    user_id: String,
    format: String,
    requested_at: Long,
    status: String,
    completed_at: Long?,
    download_url: String?,
    expires_at: Long?,
    file_size: Long?,
    created_at: Long,
  ) -> T): Query<T> = GetDataExportHistoryQuery(user_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getString(4)!!,
      cursor.getLong(5),
      cursor.getString(6),
      cursor.getLong(7),
      cursor.getLong(8),
      cursor.getLong(9)!!
    )
  }

  public fun getDataExportHistory(user_id: String): Query<Data_export_requests> =
      getDataExportHistory(user_id) { id, user_id_, format, requested_at, status, completed_at,
      download_url, expires_at, file_size, created_at ->
    Data_export_requests(
      id,
      user_id_,
      format,
      requested_at,
      status,
      completed_at,
      download_url,
      expires_at,
      file_size,
      created_at
    )
  }

  public fun getExportData(export_id: String): Query<ByteArray> = GetExportDataQuery(export_id) {
      cursor ->
    cursor.getBytes(0)!!
  }

  public fun <T : Any> getDeletionRequest(id: String, mapper: (
    id: String,
    user_id: String,
    data_types: String,
    reason: String?,
    requested_at: Long,
    status: String,
    scheduled_for: Long,
    completed_at: Long?,
    grace_period_ends: Long,
    verification_required: Long,
    created_at: Long,
  ) -> T): Query<T> = GetDeletionRequestQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun getDeletionRequest(id: String): Query<Data_deletion_requests> =
      getDeletionRequest(id) { id_, user_id, data_types, reason, requested_at, status,
      scheduled_for, completed_at, grace_period_ends, verification_required, created_at ->
    Data_deletion_requests(
      id_,
      user_id,
      data_types,
      reason,
      requested_at,
      status,
      scheduled_for,
      completed_at,
      grace_period_ends,
      verification_required,
      created_at
    )
  }

  public fun <T : Any> getDeletionHistory(user_id: String, mapper: (
    id: String,
    user_id: String,
    data_types: String,
    reason: String?,
    requested_at: Long,
    status: String,
    scheduled_for: Long,
    completed_at: Long?,
    grace_period_ends: Long,
    verification_required: Long,
    created_at: Long,
  ) -> T): Query<T> = GetDeletionHistoryQuery(user_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getLong(4)!!,
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun getDeletionHistory(user_id: String): Query<Data_deletion_requests> =
      getDeletionHistory(user_id) { id, user_id_, data_types, reason, requested_at, status,
      scheduled_for, completed_at, grace_period_ends, verification_required, created_at ->
    Data_deletion_requests(
      id,
      user_id_,
      data_types,
      reason,
      requested_at,
      status,
      scheduled_for,
      completed_at,
      grace_period_ends,
      verification_required,
      created_at
    )
  }

  public fun <T : Any> getAuditLogs(user_id: String?, mapper: (
    id: String,
    user_id: String?,
    event_type: String,
    timestamp: Long,
    ip_address: String?,
    user_agent: String?,
    session_id: String?,
    details: String,
    result: String,
    risk_level: String,
    created_at: Long,
  ) -> T): Query<T> = GetAuditLogsQuery(user_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1),
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7)!!,
      cursor.getString(8)!!,
      cursor.getString(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun getAuditLogs(user_id: String?): Query<Audit_logs> = getAuditLogs(user_id) { id,
      user_id_, event_type, timestamp, ip_address, user_agent, session_id, details, result,
      risk_level, created_at ->
    Audit_logs(
      id,
      user_id_,
      event_type,
      timestamp,
      ip_address,
      user_agent,
      session_id,
      details,
      result,
      risk_level,
      created_at
    )
  }

  public fun <T : Any> getAuditLogsByType(
    event_type: String,
    timestamp: Long,
    timestamp_: Long,
    mapper: (
      id: String,
      user_id: String?,
      event_type: String,
      timestamp: Long,
      ip_address: String?,
      user_agent: String?,
      session_id: String?,
      details: String,
      result: String,
      risk_level: String,
      created_at: Long,
    ) -> T,
  ): Query<T> = GetAuditLogsByTypeQuery(event_type, timestamp, timestamp_) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1),
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7)!!,
      cursor.getString(8)!!,
      cursor.getString(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun getAuditLogsByType(
    event_type: String,
    timestamp: Long,
    timestamp_: Long,
  ): Query<Audit_logs> = getAuditLogsByType(event_type, timestamp, timestamp_) { id, user_id,
      event_type_, timestamp__, ip_address, user_agent, session_id, details, result, risk_level,
      created_at ->
    Audit_logs(
      id,
      user_id,
      event_type_,
      timestamp__,
      ip_address,
      user_agent,
      session_id,
      details,
      result,
      risk_level,
      created_at
    )
  }

  public fun <T : Any> getAllAuditLogs(
    timestamp: Long,
    timestamp_: Long,
    mapper: (
      id: String,
      user_id: String?,
      event_type: String,
      timestamp: Long,
      ip_address: String?,
      user_agent: String?,
      session_id: String?,
      details: String,
      result: String,
      risk_level: String,
      created_at: Long,
    ) -> T,
  ): Query<T> = GetAllAuditLogsQuery(timestamp, timestamp_) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1),
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7)!!,
      cursor.getString(8)!!,
      cursor.getString(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun getAllAuditLogs(timestamp: Long, timestamp_: Long): Query<Audit_logs> =
      getAllAuditLogs(timestamp, timestamp_) { id, user_id, event_type, timestamp__, ip_address,
      user_agent, session_id, details, result, risk_level, created_at ->
    Audit_logs(
      id,
      user_id,
      event_type,
      timestamp__,
      ip_address,
      user_agent,
      session_id,
      details,
      result,
      risk_level,
      created_at
    )
  }

  public fun hasUserProfile(id: String): Query<Boolean> = HasUserProfileQuery(id) { cursor ->
    cursor.getBoolean(0)!!
  }

  public fun hasUserAccounts(userId: String): Query<Boolean> = HasUserAccountsQuery(userId) {
      cursor ->
    cursor.getBoolean(0)!!
  }

  public fun hasUserTransactions(userId: String): Query<Boolean> =
      HasUserTransactionsQuery(userId) { cursor ->
    cursor.getBoolean(0)!!
  }

  public fun hasUserGoals(userId: String): Query<Boolean> = HasUserGoalsQuery(userId) { cursor ->
    cursor.getBoolean(0)!!
  }

  public fun hasUserGamification(userId: String): Query<Boolean> =
      HasUserGamificationQuery(userId) { cursor ->
    cursor.getBoolean(0)!!
  }

  public fun hasUserAnalytics(user_id: String?): Query<Boolean> = HasUserAnalyticsQuery(user_id) {
      cursor ->
    cursor.getBoolean(0)!!
  }

  public fun hasUserAuditLogs(user_id: String?): Query<Boolean> = HasUserAuditLogsQuery(user_id) {
      cursor ->
    cursor.getBoolean(0)!!
  }

  public fun insertConsentRecord(
    id: String,
    user_id: String,
    purpose: String,
    granted: Long,
    timestamp: Long,
    ip_address: String?,
    user_agent: String?,
    version: String,
    expiry_date: Long?,
  ) {
    driver.execute(-1_098_900_507, """
        |INSERT INTO consent_records (id, user_id, purpose, granted, timestamp, ip_address, user_agent, version, expiry_date)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 9) {
          bindString(0, id)
          bindString(1, user_id)
          bindString(2, purpose)
          bindLong(3, granted)
          bindLong(4, timestamp)
          bindString(5, ip_address)
          bindString(6, user_agent)
          bindString(7, version)
          bindLong(8, expiry_date)
        }
    notifyQueries(-1_098_900_507) { emit ->
      emit("consent_records")
    }
  }

  public fun markConsentsAsDeleted(user_id: String) {
    driver.execute(-976_123_660, """
        |UPDATE consent_records 
        |SET granted = 0, version = version || '_DELETED' 
        |WHERE user_id = ?
        """.trimMargin(), 1) {
          bindString(0, user_id)
        }
    notifyQueries(-976_123_660) { emit ->
      emit("consent_records")
    }
  }

  public fun insertOrUpdateConsentPreferences(
    user_id: String,
    marketing_opt_in: Long,
    analytics_opt_in: Long,
    data_retention_period: String,
  ) {
    driver.execute(1_545_024_400, """
        |INSERT OR REPLACE INTO consent_preferences (user_id, marketing_opt_in, analytics_opt_in, data_retention_period)
        |VALUES (?, ?, ?, ?)
        """.trimMargin(), 4) {
          bindString(0, user_id)
          bindLong(1, marketing_opt_in)
          bindLong(2, analytics_opt_in)
          bindString(3, data_retention_period)
        }
    notifyQueries(1_545_024_400) { emit ->
      emit("consent_preferences")
    }
  }

  public fun insertDataExportRequest(
    id: String,
    user_id: String,
    format: String,
    requested_at: Long,
    status: String,
    expires_at: Long?,
  ) {
    driver.execute(1_798_843_019, """
        |INSERT INTO data_export_requests (id, user_id, format, requested_at, status, expires_at)
        |VALUES (?, ?, ?, ?, ?, ?)
        """.trimMargin(), 6) {
          bindString(0, id)
          bindString(1, user_id)
          bindString(2, format)
          bindLong(3, requested_at)
          bindString(4, status)
          bindLong(5, expires_at)
        }
    notifyQueries(1_798_843_019) { emit ->
      emit("data_export_requests")
    }
  }

  public fun updateExportStatus(
    status: String,
    `value`: String,
    id: String,
  ) {
    driver.execute(-1_664_232_260, """
        |UPDATE data_export_requests 
        |SET status = ?, completed_at = CASE WHEN ? = 'COMPLETED' THEN strftime('%s', 'now') * 1000 ELSE completed_at END
        |WHERE id = ?
        """.trimMargin(), 3) {
          bindString(0, status)
          bindString(1, value)
          bindString(2, id)
        }
    notifyQueries(-1_664_232_260) { emit ->
      emit("data_export_requests")
    }
  }

  public fun updateExportRequest(
    status: String,
    completed_at: Long?,
    download_url: String?,
    file_size: Long?,
    id: String,
  ) {
    driver.execute(-1_353_742_587, """
        |UPDATE data_export_requests 
        |SET status = ?, completed_at = ?, download_url = ?, file_size = ?
        |WHERE id = ?
        """.trimMargin(), 5) {
          bindString(0, status)
          bindLong(1, completed_at)
          bindString(2, download_url)
          bindLong(3, file_size)
          bindString(4, id)
        }
    notifyQueries(-1_353_742_587) { emit ->
      emit("data_export_requests")
    }
  }

  public fun storeExportData(export_id: String, data_: ByteArray) {
    driver.execute(581_667_794,
        """INSERT OR REPLACE INTO export_data (export_id, data) VALUES (?, ?)""", 2) {
          bindString(0, export_id)
          bindBytes(1, data_)
        }
    notifyQueries(581_667_794) { emit ->
      emit("export_data")
    }
  }

  public fun insertDeletionRequest(
    id: String,
    user_id: String,
    data_types: String,
    reason: String?,
    requested_at: Long,
    status: String,
    scheduled_for: Long,
    grace_period_ends: Long,
    verification_required: Long,
  ) {
    driver.execute(-552_594_629, """
        |INSERT INTO data_deletion_requests (id, user_id, data_types, reason, requested_at, status, scheduled_for, grace_period_ends, verification_required)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 9) {
          bindString(0, id)
          bindString(1, user_id)
          bindString(2, data_types)
          bindString(3, reason)
          bindLong(4, requested_at)
          bindString(5, status)
          bindLong(6, scheduled_for)
          bindLong(7, grace_period_ends)
          bindLong(8, verification_required)
        }
    notifyQueries(-552_594_629) { emit ->
      emit("data_deletion_requests")
    }
  }

  public fun updateDeletionStatus(
    status: String,
    `value`: Boolean,
    id: String,
  ) {
    driver.execute(-1_404_075_050, """
        |UPDATE data_deletion_requests 
        |SET status = ?, completed_at = CASE WHEN ? IN ('COMPLETED', 'FAILED') THEN strftime('%s', 'now') * 1000 ELSE completed_at END
        |WHERE id = ?
        """.trimMargin(), 3) {
          bindString(0, status)
          bindBoolean(1, value)
          bindString(2, id)
        }
    notifyQueries(-1_404_075_050) { emit ->
      emit("data_deletion_requests")
    }
  }

  public fun updateDeletionRequest(
    status: String,
    completed_at: Long?,
    verification_required: Long,
    id: String,
  ) {
    driver.execute(-1_878_803_669, """
        |UPDATE data_deletion_requests 
        |SET status = ?, completed_at = ?, verification_required = ?
        |WHERE id = ?
        """.trimMargin(), 4) {
          bindString(0, status)
          bindLong(1, completed_at)
          bindLong(2, verification_required)
          bindString(3, id)
        }
    notifyQueries(-1_878_803_669) { emit ->
      emit("data_deletion_requests")
    }
  }

  public fun insertAuditLogEntry(
    id: String,
    user_id: String?,
    event_type: String,
    timestamp: Long,
    ip_address: String?,
    user_agent: String?,
    session_id: String?,
    details: String,
    result: String,
    risk_level: String,
  ) {
    driver.execute(-1_316_367_517, """
        |INSERT INTO audit_logs (id, user_id, event_type, timestamp, ip_address, user_agent, session_id, details, result, risk_level)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 10) {
          bindString(0, id)
          bindString(1, user_id)
          bindString(2, event_type)
          bindLong(3, timestamp)
          bindString(4, ip_address)
          bindString(5, user_agent)
          bindString(6, session_id)
          bindString(7, details)
          bindString(8, result)
          bindString(9, risk_level)
        }
    notifyQueries(-1_316_367_517) { emit ->
      emit("audit_logs")
    }
  }

  public fun deleteUserProfile(id: String) {
    driver.execute(877_505_158, """DELETE FROM User WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(877_505_158) { emit ->
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

  public fun deleteUserAccounts(userId: String) {
    driver.execute(297_973_417, """DELETE FROM Account WHERE userId = ?""", 1) {
          bindString(0, userId)
        }
    notifyQueries(297_973_417) { emit ->
      emit("Account")
      emit("TransactionEntity")
    }
  }

  public fun deleteUserTransactions(userId: String) {
    driver.execute(-1_588_761_960,
        """DELETE FROM TransactionEntity WHERE accountId IN (SELECT id FROM Account WHERE userId = ?)""",
        1) {
          bindString(0, userId)
        }
    notifyQueries(-1_588_761_960) { emit ->
      emit("TransactionEntity")
    }
  }

  public fun deleteUserGoals(userId: String) {
    driver.execute(-1_799_677_987, """DELETE FROM FinancialGoal WHERE userId = ?""", 1) {
          bindString(0, userId)
        }
    notifyQueries(-1_799_677_987) { emit ->
      emit("FinancialGoal")
      emit("MicroTask")
    }
  }

  public fun deleteUserGamification(userId: String) {
    driver.execute(1_206_847_022, """DELETE FROM GamificationProfile WHERE userId = ?""", 1) {
          bindString(0, userId)
        }
    notifyQueries(1_206_847_022) { emit ->
      emit("GamificationProfile")
    }
  }

  public fun deleteUserAnalytics(user_id: String?) {
    driver.execute(null,
        """DELETE FROM audit_logs WHERE user_id ${ if (user_id == null) "IS" else "=" } ? AND event_type LIKE '%ANALYTICS%'""",
        1) {
          bindString(0, user_id)
        }
    notifyQueries(781_252_899) { emit ->
      emit("audit_logs")
    }
  }

  public fun deleteUserAuditLogs(user_id: String?, `value`: Long) {
    driver.execute(null, """
        |DELETE FROM audit_logs 
        |WHERE user_id ${ if (user_id == null) "IS" else "=" } ? 
        |AND (? = 0 OR event_type NOT IN ('CONSENT_GRANTED', 'CONSENT_WITHDRAWN', 'DATA_DELETION_REQUESTED', 'DATA_DELETION_COMPLETED'))
        """.trimMargin(), 2) {
          bindString(0, user_id)
          bindLong(1, value)
        }
    notifyQueries(-1_628_123_705) { emit ->
      emit("audit_logs")
    }
  }

  private inner class GetLatestConsentsForUserQuery<out T : Any>(
    public val user_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("consent_records", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("consent_records", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-138_749_717, """
    |SELECT * FROM consent_records 
    |WHERE user_id = ? 
    |AND id IN (
    |    SELECT id FROM consent_records cr2 
    |    WHERE cr2.user_id = consent_records.user_id 
    |    AND cr2.purpose = consent_records.purpose 
    |    ORDER BY timestamp DESC 
    |    LIMIT 1
    |)
    |ORDER BY timestamp DESC
    """.trimMargin(), mapper, 1) {
      bindString(0, user_id)
    }

    override fun toString(): String = "Privacy.sq:getLatestConsentsForUser"
  }

  private inner class GetLatestConsentForPurposeQuery<out T : Any>(
    public val user_id: String,
    public val purpose: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("consent_records", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("consent_records", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(706_139_743, """
    |SELECT * FROM consent_records 
    |WHERE user_id = ? AND purpose = ? 
    |ORDER BY timestamp DESC 
    |LIMIT 1
    """.trimMargin(), mapper, 2) {
      bindString(0, user_id)
      bindString(1, purpose)
    }

    override fun toString(): String = "Privacy.sq:getLatestConsentForPurpose"
  }

  private inner class GetConsentHistoryQuery<out T : Any>(
    public val user_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("consent_records", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("consent_records", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-117_056_029, """
    |SELECT * FROM consent_records 
    |WHERE user_id = ? 
    |ORDER BY timestamp DESC
    """.trimMargin(), mapper, 1) {
      bindString(0, user_id)
    }

    override fun toString(): String = "Privacy.sq:getConsentHistory"
  }

  private inner class HasUserConsentsQuery<out T : Any>(
    public val user_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("consent_records", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("consent_records", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_214_416_593,
        """SELECT COUNT(*) > 0 FROM consent_records WHERE user_id = ?""", mapper, 1) {
      bindString(0, user_id)
    }

    override fun toString(): String = "Privacy.sq:hasUserConsents"
  }

  private inner class GetConsentPreferencesQuery<out T : Any>(
    public val user_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("consent_preferences", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("consent_preferences", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-2_028_627_769,
        """SELECT * FROM consent_preferences WHERE user_id = ?""", mapper, 1) {
      bindString(0, user_id)
    }

    override fun toString(): String = "Privacy.sq:getConsentPreferences"
  }

  private inner class GetDataExportRequestQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("data_export_requests", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("data_export_requests", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(750_237_000, """SELECT * FROM data_export_requests WHERE id = ?""",
        mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "Privacy.sq:getDataExportRequest"
  }

  private inner class GetDataExportHistoryQuery<out T : Any>(
    public val user_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("data_export_requests", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("data_export_requests", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(581_478_221, """
    |SELECT * FROM data_export_requests 
    |WHERE user_id = ? 
    |ORDER BY requested_at DESC
    """.trimMargin(), mapper, 1) {
      bindString(0, user_id)
    }

    override fun toString(): String = "Privacy.sq:getDataExportHistory"
  }

  private inner class GetExportDataQuery<out T : Any>(
    public val export_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("export_data", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("export_data", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_865_473_223, """SELECT data FROM export_data WHERE export_id = ?""",
        mapper, 1) {
      bindString(0, export_id)
    }

    override fun toString(): String = "Privacy.sq:getExportData"
  }

  private inner class GetDeletionRequestQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("data_deletion_requests", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("data_deletion_requests", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(125_643_064, """SELECT * FROM data_deletion_requests WHERE id = ?""",
        mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "Privacy.sq:getDeletionRequest"
  }

  private inner class GetDeletionHistoryQuery<out T : Any>(
    public val user_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("data_deletion_requests", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("data_deletion_requests", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-43_115_715, """
    |SELECT * FROM data_deletion_requests 
    |WHERE user_id = ? 
    |ORDER BY requested_at DESC
    """.trimMargin(), mapper, 1) {
      bindString(0, user_id)
    }

    override fun toString(): String = "Privacy.sq:getDeletionHistory"
  }

  private inner class GetAuditLogsQuery<out T : Any>(
    public val user_id: String?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("audit_logs", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("audit_logs", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null, """
    |SELECT * FROM audit_logs 
    |WHERE user_id ${ if (user_id == null) "IS" else "=" } ? 
    |ORDER BY timestamp DESC
    """.trimMargin(), mapper, 1) {
      bindString(0, user_id)
    }

    override fun toString(): String = "Privacy.sq:getAuditLogs"
  }

  private inner class GetAuditLogsByTypeQuery<out T : Any>(
    public val event_type: String,
    public val timestamp: Long,
    public val timestamp_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("audit_logs", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("audit_logs", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-716_052_654, """
    |SELECT * FROM audit_logs 
    |WHERE event_type = ? 
    |AND timestamp >= ? 
    |AND timestamp <= ?
    |ORDER BY timestamp DESC
    """.trimMargin(), mapper, 3) {
      bindString(0, event_type)
      bindLong(1, timestamp)
      bindLong(2, timestamp_)
    }

    override fun toString(): String = "Privacy.sq:getAuditLogsByType"
  }

  private inner class GetAllAuditLogsQuery<out T : Any>(
    public val timestamp: Long,
    public val timestamp_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("audit_logs", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("audit_logs", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(14_224_530, """
    |SELECT * FROM audit_logs 
    |WHERE timestamp >= ? 
    |AND timestamp <= ?
    |ORDER BY timestamp DESC
    """.trimMargin(), mapper, 2) {
      bindLong(0, timestamp)
      bindLong(1, timestamp_)
    }

    override fun toString(): String = "Privacy.sq:getAllAuditLogs"
  }

  private inner class HasUserProfileQuery<out T : Any>(
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
        driver.executeQuery(-251_920_527, """SELECT COUNT(*) > 0 FROM User WHERE id = ?""", mapper,
        1) {
      bindString(0, id)
    }

    override fun toString(): String = "Privacy.sq:hasUserProfile"
  }

  private inner class HasUserAccountsQuery<out T : Any>(
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
        driver.executeQuery(-354_484_450, """SELECT COUNT(*) > 0 FROM Account WHERE userId = ?""",
        mapper, 1) {
      bindString(0, userId)
    }

    override fun toString(): String = "Privacy.sq:hasUserAccounts"
  }

  private inner class HasUserTransactionsQuery<out T : Any>(
    public val userId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TransactionEntity", "Account", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TransactionEntity", "Account", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_988_726_643,
        """SELECT COUNT(*) > 0 FROM TransactionEntity WHERE accountId IN (SELECT id FROM Account WHERE userId = ?)""",
        mapper, 1) {
      bindString(0, userId)
    }

    override fun toString(): String = "Privacy.sq:hasUserTransactions"
  }

  private inner class HasUserGoalsQuery<out T : Any>(
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
        driver.executeQuery(541_043_592,
        """SELECT COUNT(*) > 0 FROM FinancialGoal WHERE userId = ?""", mapper, 1) {
      bindString(0, userId)
    }

    override fun toString(): String = "Privacy.sq:hasUserGoals"
  }

  private inner class HasUserGamificationQuery<out T : Any>(
    public val userId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("GamificationProfile", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("GamificationProfile", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(806_882_339,
        """SELECT COUNT(*) > 0 FROM GamificationProfile WHERE userId = ?""", mapper, 1) {
      bindString(0, userId)
    }

    override fun toString(): String = "Privacy.sq:hasUserGamification"
  }

  private inner class HasUserAnalyticsQuery<out T : Any>(
    public val user_id: String?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("audit_logs", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("audit_logs", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null,
        """SELECT COUNT(*) > 0 FROM audit_logs WHERE user_id ${ if (user_id == null) "IS" else "=" } ? AND event_type LIKE '%ANALYTICS%'""",
        mapper, 1) {
      bindString(0, user_id)
    }

    override fun toString(): String = "Privacy.sq:hasUserAnalytics"
  }

  private inner class HasUserAuditLogsQuery<out T : Any>(
    public val user_id: String?,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("audit_logs", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("audit_logs", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null,
        """SELECT COUNT(*) > 0 FROM audit_logs WHERE user_id ${ if (user_id == null) "IS" else "=" } ?""",
        mapper, 1) {
      bindString(0, user_id)
    }

    override fun toString(): String = "Privacy.sq:hasUserAuditLogs"
  }
}
