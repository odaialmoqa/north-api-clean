package com.north.mobile.database.shared

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.north.mobile.database.AccountQueries
import com.north.mobile.database.FinancialGoalQueries
import com.north.mobile.database.GamificationQueries
import com.north.mobile.database.MicroTaskQueries
import com.north.mobile.database.NorthDatabase
import com.north.mobile.database.PrivacyQueries
import com.north.mobile.database.TransactionQueries
import com.north.mobile.database.UserQueries
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<NorthDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = NorthDatabaseImpl.Schema

internal fun KClass<NorthDatabase>.newInstance(driver: SqlDriver): NorthDatabase =
    NorthDatabaseImpl(driver)

private class NorthDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver), NorthDatabase {
  override val accountQueries: AccountQueries = AccountQueries(driver)

  override val financialGoalQueries: FinancialGoalQueries = FinancialGoalQueries(driver)

  override val gamificationQueries: GamificationQueries = GamificationQueries(driver)

  override val microTaskQueries: MicroTaskQueries = MicroTaskQueries(driver)

  override val privacyQueries: PrivacyQueries = PrivacyQueries(driver)

  override val transactionQueries: TransactionQueries = TransactionQueries(driver)

  override val userQueries: UserQueries = UserQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE Account (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    userId TEXT NOT NULL,
          |    institutionId TEXT NOT NULL,
          |    institutionName TEXT NOT NULL,
          |    accountType TEXT NOT NULL,
          |    balance INTEGER NOT NULL, -- Amount in cents
          |    availableBalance INTEGER, -- Amount in cents, nullable for non-credit accounts
          |    currency TEXT NOT NULL DEFAULT 'CAD',
          |    lastUpdated INTEGER NOT NULL,
          |    accountNumber TEXT, -- Last 4 digits only
          |    transitNumber TEXT, -- Canadian bank transit number
          |    institutionNumber TEXT, -- Canadian bank institution number
          |    nickname TEXT,
          |    isActive INTEGER NOT NULL DEFAULT 1,
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER NOT NULL,
          |    FOREIGN KEY (userId) REFERENCES User(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE FinancialGoal (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    userId TEXT NOT NULL,
          |    title TEXT NOT NULL,
          |    description TEXT,
          |    targetAmount INTEGER NOT NULL, -- Amount in cents
          |    currentAmount INTEGER NOT NULL DEFAULT 0, -- Amount in cents
          |    currency TEXT NOT NULL DEFAULT 'CAD',
          |    targetDate INTEGER NOT NULL, -- Unix timestamp
          |    priority INTEGER NOT NULL DEFAULT 1, -- 1=Low, 2=Medium, 3=High
          |    category TEXT NOT NULL, -- EMERGENCY, SAVINGS, DEBT_PAYOFF, etc.
          |    isActive INTEGER NOT NULL DEFAULT 1,
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER NOT NULL,
          |    FOREIGN KEY (userId) REFERENCES User(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE GamificationProfile (
          |    userId TEXT NOT NULL PRIMARY KEY,
          |    level INTEGER NOT NULL DEFAULT 1,
          |    totalPoints INTEGER NOT NULL DEFAULT 0,
          |    lastActivity INTEGER, -- Unix timestamp
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER NOT NULL,
          |    FOREIGN KEY (userId) REFERENCES User(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Streak (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    userId TEXT NOT NULL,
          |    type TEXT NOT NULL, -- DAILY_CHECK_IN, BUDGET_ADHERENCE, etc.
          |    currentCount INTEGER NOT NULL DEFAULT 0,
          |    bestCount INTEGER NOT NULL DEFAULT 0,
          |    lastUpdated INTEGER NOT NULL, -- Unix timestamp (date only)
          |    isActive INTEGER NOT NULL DEFAULT 1,
          |    riskLevel TEXT NOT NULL DEFAULT 'SAFE', -- SAFE, LOW_RISK, MEDIUM_RISK, HIGH_RISK, BROKEN
          |    recoveryAttempts INTEGER NOT NULL DEFAULT 0,
          |    lastReminderSent INTEGER, -- Unix timestamp, nullable
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER NOT NULL,
          |    FOREIGN KEY (userId) REFERENCES User(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Achievement (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    userId TEXT NOT NULL,
          |    achievementType TEXT NOT NULL, -- FIRST_GOAL, SAVINGS_MILESTONE, etc.
          |    title TEXT NOT NULL,
          |    description TEXT NOT NULL,
          |    badgeIcon TEXT NOT NULL,
          |    category TEXT NOT NULL, -- SAVINGS, BUDGETING, GOALS, etc.
          |    unlockedAt INTEGER NOT NULL, -- Unix timestamp
          |    createdAt INTEGER NOT NULL,
          |    FOREIGN KEY (userId) REFERENCES User(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE PointsHistory (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    userId TEXT NOT NULL,
          |    points INTEGER NOT NULL,
          |    action TEXT NOT NULL, -- CHECK_BALANCE, CATEGORIZE_TRANSACTION, etc.
          |    description TEXT,
          |    earnedAt INTEGER NOT NULL, -- Unix timestamp
          |    FOREIGN KEY (userId) REFERENCES User(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE StreakRecovery (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    userId TEXT NOT NULL,
          |    originalStreakId TEXT NOT NULL,
          |    streakType TEXT NOT NULL,
          |    brokenAt INTEGER NOT NULL, -- Unix timestamp
          |    recoveryStarted INTEGER NOT NULL, -- Unix timestamp
          |    recoveryCompleted INTEGER, -- Unix timestamp, nullable
          |    originalCount INTEGER NOT NULL,
          |    isSuccessful INTEGER NOT NULL DEFAULT 0,
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER NOT NULL,
          |    FOREIGN KEY (userId) REFERENCES User(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE RecoveryAction (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    recoveryId TEXT NOT NULL,
          |    actionType TEXT NOT NULL,
          |    completedAt INTEGER NOT NULL, -- Unix timestamp
          |    pointsAwarded INTEGER NOT NULL,
          |    description TEXT NOT NULL,
          |    createdAt INTEGER NOT NULL,
          |    FOREIGN KEY (recoveryId) REFERENCES StreakRecovery(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE StreakReminder (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    userId TEXT NOT NULL,
          |    streakId TEXT NOT NULL,
          |    streakType TEXT NOT NULL,
          |    reminderType TEXT NOT NULL, -- GENTLE_NUDGE, MOTIVATION_BOOST, etc.
          |    message TEXT NOT NULL,
          |    scheduledFor INTEGER NOT NULL, -- Unix timestamp
          |    sentAt INTEGER, -- Unix timestamp, nullable
          |    isRead INTEGER NOT NULL DEFAULT 0,
          |    createdAt INTEGER NOT NULL,
          |    FOREIGN KEY (userId) REFERENCES User(id) ON DELETE CASCADE,
          |    FOREIGN KEY (streakId) REFERENCES Streak(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE MicroTask (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    goalId TEXT NOT NULL,
          |    title TEXT NOT NULL,
          |    description TEXT NOT NULL,
          |    targetAmount INTEGER NOT NULL, -- Amount in cents
          |    isCompleted INTEGER NOT NULL DEFAULT 0,
          |    dueDate INTEGER, -- Unix timestamp (optional)
          |    completedAt INTEGER, -- Unix timestamp (optional)
          |    createdAt INTEGER NOT NULL,
          |    FOREIGN KEY (goalId) REFERENCES FinancialGoal(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE consent_records (
          |    id TEXT PRIMARY KEY NOT NULL,
          |    user_id TEXT NOT NULL,
          |    purpose TEXT NOT NULL,
          |    granted INTEGER NOT NULL DEFAULT 0,
          |    timestamp INTEGER NOT NULL,
          |    ip_address TEXT,
          |    user_agent TEXT,
          |    version TEXT NOT NULL,
          |    expiry_date INTEGER,
          |    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE consent_preferences (
          |    user_id TEXT PRIMARY KEY NOT NULL,
          |    marketing_opt_in INTEGER NOT NULL DEFAULT 0,
          |    analytics_opt_in INTEGER NOT NULL DEFAULT 1,
          |    data_retention_period TEXT NOT NULL DEFAULT 'STANDARD',
          |    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE data_export_requests (
          |    id TEXT PRIMARY KEY NOT NULL,
          |    user_id TEXT NOT NULL,
          |    format TEXT NOT NULL,
          |    requested_at INTEGER NOT NULL,
          |    status TEXT NOT NULL DEFAULT 'PENDING',
          |    completed_at INTEGER,
          |    download_url TEXT,
          |    expires_at INTEGER,
          |    file_size INTEGER,
          |    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE export_data (
          |    export_id TEXT PRIMARY KEY NOT NULL,
          |    data BLOB NOT NULL,
          |    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE data_deletion_requests (
          |    id TEXT PRIMARY KEY NOT NULL,
          |    user_id TEXT NOT NULL,
          |    data_types TEXT NOT NULL, -- JSON array of data types
          |    reason TEXT,
          |    requested_at INTEGER NOT NULL,
          |    status TEXT NOT NULL DEFAULT 'PENDING',
          |    scheduled_for INTEGER NOT NULL,
          |    completed_at INTEGER,
          |    grace_period_ends INTEGER NOT NULL,
          |    verification_required INTEGER NOT NULL DEFAULT 1,
          |    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE audit_logs (
          |    id TEXT PRIMARY KEY NOT NULL,
          |    user_id TEXT,
          |    event_type TEXT NOT NULL,
          |    timestamp INTEGER NOT NULL,
          |    ip_address TEXT,
          |    user_agent TEXT,
          |    session_id TEXT,
          |    details TEXT NOT NULL, -- JSON object
          |    result TEXT NOT NULL,
          |    risk_level TEXT NOT NULL DEFAULT 'LOW',
          |    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE TransactionEntity (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    accountId TEXT NOT NULL,
          |    amount INTEGER NOT NULL, -- Amount in cents
          |    description TEXT NOT NULL,
          |    category TEXT,
          |    subcategory TEXT,
          |    date INTEGER NOT NULL, -- Unix timestamp
          |    isRecurring INTEGER NOT NULL DEFAULT 0,
          |    merchantName TEXT,
          |    location TEXT,
          |    isVerified INTEGER NOT NULL DEFAULT 0,
          |    notes TEXT,
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER NOT NULL,
          |    FOREIGN KEY (accountId) REFERENCES Account(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE User (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    email TEXT NOT NULL UNIQUE,
          |    firstName TEXT NOT NULL,
          |    lastName TEXT NOT NULL,
          |    phoneNumber TEXT,
          |    dateOfBirth TEXT,
          |    currency TEXT NOT NULL DEFAULT 'CAD',
          |    language TEXT NOT NULL DEFAULT 'en',
          |    notificationsEnabled INTEGER NOT NULL DEFAULT 1,
          |    biometricAuthEnabled INTEGER NOT NULL DEFAULT 0,
          |    createdAt INTEGER NOT NULL,
          |    updatedAt INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, "CREATE INDEX idx_account_user_id ON Account(userId)", 0)
      driver.execute(null, "CREATE INDEX idx_account_institution ON Account(institutionId)", 0)
      driver.execute(null, "CREATE INDEX idx_account_type ON Account(accountType)", 0)
      driver.execute(null, "CREATE INDEX idx_goal_user_id ON FinancialGoal(userId)", 0)
      driver.execute(null, "CREATE INDEX idx_goal_category ON FinancialGoal(category)", 0)
      driver.execute(null, "CREATE INDEX idx_goal_priority ON FinancialGoal(priority)", 0)
      driver.execute(null, "CREATE INDEX idx_goal_target_date ON FinancialGoal(targetDate)", 0)
      driver.execute(null, "CREATE INDEX idx_gamification_user_id ON GamificationProfile(userId)",
          0)
      driver.execute(null, "CREATE INDEX idx_streak_user_id ON Streak(userId)", 0)
      driver.execute(null, "CREATE INDEX idx_streak_type ON Streak(type)", 0)
      driver.execute(null, "CREATE INDEX idx_achievement_user_id ON Achievement(userId)", 0)
      driver.execute(null, "CREATE INDEX idx_achievement_category ON Achievement(category)", 0)
      driver.execute(null, "CREATE INDEX idx_points_user_id ON PointsHistory(userId)", 0)
      driver.execute(null, "CREATE INDEX idx_points_earned_at ON PointsHistory(earnedAt)", 0)
      driver.execute(null, "CREATE INDEX idx_streak_recovery_user_id ON StreakRecovery(userId)", 0)
      driver.execute(null,
          "CREATE INDEX idx_recovery_action_recovery_id ON RecoveryAction(recoveryId)", 0)
      driver.execute(null, "CREATE INDEX idx_streak_reminder_user_id ON StreakReminder(userId)", 0)
      driver.execute(null,
          "CREATE INDEX idx_streak_reminder_scheduled ON StreakReminder(scheduledFor)", 0)
      driver.execute(null, "CREATE INDEX idx_microtask_goal_id ON MicroTask(goalId)", 0)
      driver.execute(null, "CREATE INDEX idx_microtask_due_date ON MicroTask(dueDate)", 0)
      driver.execute(null, "CREATE INDEX idx_microtask_completed ON MicroTask(isCompleted)", 0)
      driver.execute(null, "CREATE INDEX idx_consent_records_user_id ON consent_records(user_id)",
          0)
      driver.execute(null, "CREATE INDEX idx_consent_records_purpose ON consent_records(purpose)",
          0)
      driver.execute(null,
          "CREATE INDEX idx_consent_records_timestamp ON consent_records(timestamp)", 0)
      driver.execute(null,
          "CREATE INDEX idx_data_export_requests_user_id ON data_export_requests(user_id)", 0)
      driver.execute(null,
          "CREATE INDEX idx_data_export_requests_status ON data_export_requests(status)", 0)
      driver.execute(null,
          "CREATE INDEX idx_data_deletion_requests_user_id ON data_deletion_requests(user_id)", 0)
      driver.execute(null,
          "CREATE INDEX idx_data_deletion_requests_status ON data_deletion_requests(status)", 0)
      driver.execute(null,
          "CREATE INDEX idx_data_deletion_requests_scheduled_for ON data_deletion_requests(scheduled_for)",
          0)
      driver.execute(null, "CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id)", 0)
      driver.execute(null, "CREATE INDEX idx_audit_logs_event_type ON audit_logs(event_type)", 0)
      driver.execute(null, "CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp)", 0)
      driver.execute(null, "CREATE INDEX idx_audit_logs_risk_level ON audit_logs(risk_level)", 0)
      driver.execute(null,
          "CREATE INDEX idx_transaction_account_id ON TransactionEntity(accountId)", 0)
      driver.execute(null, "CREATE INDEX idx_transaction_date ON TransactionEntity(date)", 0)
      driver.execute(null, "CREATE INDEX idx_transaction_category ON TransactionEntity(category)",
          0)
      driver.execute(null, "CREATE INDEX idx_transaction_amount ON TransactionEntity(amount)", 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
