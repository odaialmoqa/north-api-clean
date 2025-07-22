package com.north.mobile.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class GamificationQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectGamificationProfile(userId: String, mapper: (
    userId: String,
    level: Long,
    totalPoints: Long,
    lastActivity: Long?,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectGamificationProfileQuery(userId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getLong(1)!!,
      cursor.getLong(2)!!,
      cursor.getLong(3),
      cursor.getLong(4)!!,
      cursor.getLong(5)!!
    )
  }

  public fun selectGamificationProfile(userId: String): Query<GamificationProfile> =
      selectGamificationProfile(userId) { userId_, level, totalPoints, lastActivity, createdAt,
      updatedAt ->
    GamificationProfile(
      userId_,
      level,
      totalPoints,
      lastActivity,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectStreaksByUserId(userId: String, mapper: (
    id: String,
    userId: String,
    type: String,
    currentCount: Long,
    bestCount: Long,
    lastUpdated: Long,
    isActive: Long,
    riskLevel: String,
    recoveryAttempts: Long,
    lastReminderSent: Long?,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectStreaksByUserIdQuery(userId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9),
      cursor.getLong(10)!!,
      cursor.getLong(11)!!
    )
  }

  public fun selectStreaksByUserId(userId: String): Query<Streak> = selectStreaksByUserId(userId) {
      id, userId_, type, currentCount, bestCount, lastUpdated, isActive, riskLevel,
      recoveryAttempts, lastReminderSent, createdAt, updatedAt ->
    Streak(
      id,
      userId_,
      type,
      currentCount,
      bestCount,
      lastUpdated,
      isActive,
      riskLevel,
      recoveryAttempts,
      lastReminderSent,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectStreakByType(
    userId: String,
    type: String,
    mapper: (
      id: String,
      userId: String,
      type: String,
      currentCount: Long,
      bestCount: Long,
      lastUpdated: Long,
      isActive: Long,
      riskLevel: String,
      recoveryAttempts: Long,
      lastReminderSent: Long?,
      createdAt: Long,
      updatedAt: Long,
    ) -> T,
  ): Query<T> = SelectStreakByTypeQuery(userId, type) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9),
      cursor.getLong(10)!!,
      cursor.getLong(11)!!
    )
  }

  public fun selectStreakByType(userId: String, type: String): Query<Streak> =
      selectStreakByType(userId, type) { id, userId_, type_, currentCount, bestCount, lastUpdated,
      isActive, riskLevel, recoveryAttempts, lastReminderSent, createdAt, updatedAt ->
    Streak(
      id,
      userId_,
      type_,
      currentCount,
      bestCount,
      lastUpdated,
      isActive,
      riskLevel,
      recoveryAttempts,
      lastReminderSent,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectStreakById(id: String, mapper: (
    id: String,
    userId: String,
    type: String,
    currentCount: Long,
    bestCount: Long,
    lastUpdated: Long,
    isActive: Long,
    riskLevel: String,
    recoveryAttempts: Long,
    lastReminderSent: Long?,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectStreakByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9),
      cursor.getLong(10)!!,
      cursor.getLong(11)!!
    )
  }

  public fun selectStreakById(id: String): Query<Streak> = selectStreakById(id) { id_, userId, type,
      currentCount, bestCount, lastUpdated, isActive, riskLevel, recoveryAttempts, lastReminderSent,
      createdAt, updatedAt ->
    Streak(
      id_,
      userId,
      type,
      currentCount,
      bestCount,
      lastUpdated,
      isActive,
      riskLevel,
      recoveryAttempts,
      lastReminderSent,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectAllUserStreaks(userId: String, mapper: (
    id: String,
    userId: String,
    type: String,
    currentCount: Long,
    bestCount: Long,
    lastUpdated: Long,
    isActive: Long,
    riskLevel: String,
    recoveryAttempts: Long,
    lastReminderSent: Long?,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectAllUserStreaksQuery(userId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9),
      cursor.getLong(10)!!,
      cursor.getLong(11)!!
    )
  }

  public fun selectAllUserStreaks(userId: String): Query<Streak> = selectAllUserStreaks(userId) {
      id, userId_, type, currentCount, bestCount, lastUpdated, isActive, riskLevel,
      recoveryAttempts, lastReminderSent, createdAt, updatedAt ->
    Streak(
      id,
      userId_,
      type,
      currentCount,
      bestCount,
      lastUpdated,
      isActive,
      riskLevel,
      recoveryAttempts,
      lastReminderSent,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectAchievementsByUserId(userId: String, mapper: (
    id: String,
    userId: String,
    achievementType: String,
    title: String,
    description: String,
    badgeIcon: String,
    category: String,
    unlockedAt: Long,
    createdAt: Long,
  ) -> T): Query<T> = SelectAchievementsByUserIdQuery(userId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!
    )
  }

  public fun selectAchievementsByUserId(userId: String): Query<Achievement> =
      selectAchievementsByUserId(userId) { id, userId_, achievementType, title, description,
      badgeIcon, category, unlockedAt, createdAt ->
    Achievement(
      id,
      userId_,
      achievementType,
      title,
      description,
      badgeIcon,
      category,
      unlockedAt,
      createdAt
    )
  }

  public fun <T : Any> selectAchievementByType(
    userId: String,
    achievementType: String,
    mapper: (
      id: String,
      userId: String,
      achievementType: String,
      title: String,
      description: String,
      badgeIcon: String,
      category: String,
      unlockedAt: Long,
      createdAt: Long,
    ) -> T,
  ): Query<T> = SelectAchievementByTypeQuery(userId, achievementType) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!
    )
  }

  public fun selectAchievementByType(userId: String, achievementType: String): Query<Achievement> =
      selectAchievementByType(userId, achievementType) { id, userId_, achievementType_, title,
      description, badgeIcon, category, unlockedAt, createdAt ->
    Achievement(
      id,
      userId_,
      achievementType_,
      title,
      description,
      badgeIcon,
      category,
      unlockedAt,
      createdAt
    )
  }

  public fun <T : Any> selectPointsHistoryByUserId(userId: String, mapper: (
    id: String,
    userId: String,
    points: Long,
    action: String,
    description: String?,
    earnedAt: Long,
  ) -> T): Query<T> = SelectPointsHistoryByUserIdQuery(userId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getLong(5)!!
    )
  }

  public fun selectPointsHistoryByUserId(userId: String): Query<PointsHistory> =
      selectPointsHistoryByUserId(userId) { id, userId_, points, action, description, earnedAt ->
    PointsHistory(
      id,
      userId_,
      points,
      action,
      description,
      earnedAt
    )
  }

  public fun <T : Any> selectRecentPointsHistory(
    userId: String,
    `value`: Long,
    mapper: (
      id: String,
      userId: String,
      points: Long,
      action: String,
      description: String?,
      earnedAt: Long,
    ) -> T,
  ): Query<T> = SelectRecentPointsHistoryQuery(userId, value) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getLong(5)!!
    )
  }

  public fun selectRecentPointsHistory(userId: String, value_: Long): Query<PointsHistory> =
      selectRecentPointsHistory(userId, value_) { id, userId_, points, action, description,
      earnedAt ->
    PointsHistory(
      id,
      userId_,
      points,
      action,
      description,
      earnedAt
    )
  }

  public fun <T : Any> selectStreakRecoveryById(id: String, mapper: (
    id: String,
    userId: String,
    originalStreakId: String,
    streakType: String,
    brokenAt: Long,
    recoveryStarted: Long,
    recoveryCompleted: Long?,
    originalCount: Long,
    isSuccessful: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectStreakRecoveryByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6),
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun selectStreakRecoveryById(id: String): Query<StreakRecovery> =
      selectStreakRecoveryById(id) { id_, userId, originalStreakId, streakType, brokenAt,
      recoveryStarted, recoveryCompleted, originalCount, isSuccessful, createdAt, updatedAt ->
    StreakRecovery(
      id_,
      userId,
      originalStreakId,
      streakType,
      brokenAt,
      recoveryStarted,
      recoveryCompleted,
      originalCount,
      isSuccessful,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectActiveRecoveriesByUserId(userId: String, mapper: (
    id: String,
    userId: String,
    originalStreakId: String,
    streakType: String,
    brokenAt: Long,
    recoveryStarted: Long,
    recoveryCompleted: Long?,
    originalCount: Long,
    isSuccessful: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectActiveRecoveriesByUserIdQuery(userId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6),
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun selectActiveRecoveriesByUserId(userId: String): Query<StreakRecovery> =
      selectActiveRecoveriesByUserId(userId) { id, userId_, originalStreakId, streakType, brokenAt,
      recoveryStarted, recoveryCompleted, originalCount, isSuccessful, createdAt, updatedAt ->
    StreakRecovery(
      id,
      userId_,
      originalStreakId,
      streakType,
      brokenAt,
      recoveryStarted,
      recoveryCompleted,
      originalCount,
      isSuccessful,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectAllRecoveriesByUserId(userId: String, mapper: (
    id: String,
    userId: String,
    originalStreakId: String,
    streakType: String,
    brokenAt: Long,
    recoveryStarted: Long,
    recoveryCompleted: Long?,
    originalCount: Long,
    isSuccessful: Long,
    createdAt: Long,
    updatedAt: Long,
  ) -> T): Query<T> = SelectAllRecoveriesByUserIdQuery(userId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6),
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun selectAllRecoveriesByUserId(userId: String): Query<StreakRecovery> =
      selectAllRecoveriesByUserId(userId) { id, userId_, originalStreakId, streakType, brokenAt,
      recoveryStarted, recoveryCompleted, originalCount, isSuccessful, createdAt, updatedAt ->
    StreakRecovery(
      id,
      userId_,
      originalStreakId,
      streakType,
      brokenAt,
      recoveryStarted,
      recoveryCompleted,
      originalCount,
      isSuccessful,
      createdAt,
      updatedAt
    )
  }

  public fun <T : Any> selectRecoveryActionsByRecoveryId(recoveryId: String, mapper: (
    id: String,
    recoveryId: String,
    actionType: String,
    completedAt: Long,
    pointsAwarded: Long,
    description: String,
    createdAt: Long,
  ) -> T): Query<T> = SelectRecoveryActionsByRecoveryIdQuery(recoveryId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getString(5)!!,
      cursor.getLong(6)!!
    )
  }

  public fun selectRecoveryActionsByRecoveryId(recoveryId: String): Query<RecoveryAction> =
      selectRecoveryActionsByRecoveryId(recoveryId) { id, recoveryId_, actionType, completedAt,
      pointsAwarded, description, createdAt ->
    RecoveryAction(
      id,
      recoveryId_,
      actionType,
      completedAt,
      pointsAwarded,
      description,
      createdAt
    )
  }

  public fun <T : Any> selectActiveRemindersByUserId(
    userId: String,
    scheduledFor: Long,
    mapper: (
      id: String,
      userId: String,
      streakId: String,
      streakType: String,
      reminderType: String,
      message: String,
      scheduledFor: Long,
      sentAt: Long?,
      isRead: Long,
      createdAt: Long,
    ) -> T,
  ): Query<T> = SelectActiveRemindersByUserIdQuery(userId, scheduledFor) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!
    )
  }

  public fun selectActiveRemindersByUserId(userId: String, scheduledFor: Long):
      Query<StreakReminder> = selectActiveRemindersByUserId(userId, scheduledFor) { id, userId_,
      streakId, streakType, reminderType, message, scheduledFor_, sentAt, isRead, createdAt ->
    StreakReminder(
      id,
      userId_,
      streakId,
      streakType,
      reminderType,
      message,
      scheduledFor_,
      sentAt,
      isRead,
      createdAt
    )
  }

  public fun <T : Any> selectReminderById(id: String, mapper: (
    id: String,
    userId: String,
    streakId: String,
    streakType: String,
    reminderType: String,
    message: String,
    scheduledFor: Long,
    sentAt: Long?,
    isRead: Long,
    createdAt: Long,
  ) -> T): Query<T> = SelectReminderByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!
    )
  }

  public fun selectReminderById(id: String): Query<StreakReminder> = selectReminderById(id) { id_,
      userId, streakId, streakType, reminderType, message, scheduledFor, sentAt, isRead,
      createdAt ->
    StreakReminder(
      id_,
      userId,
      streakId,
      streakType,
      reminderType,
      message,
      scheduledFor,
      sentAt,
      isRead,
      createdAt
    )
  }

  public fun insertGamificationProfile(
    userId: String,
    level: Long,
    totalPoints: Long,
    lastActivity: Long?,
    createdAt: Long,
    updatedAt: Long,
  ) {
    driver.execute(-1_848_384_477, """
        |INSERT INTO GamificationProfile(userId, level, totalPoints, lastActivity, createdAt, updatedAt)
        |VALUES(?, ?, ?, ?, ?, ?)
        """.trimMargin(), 6) {
          bindString(0, userId)
          bindLong(1, level)
          bindLong(2, totalPoints)
          bindLong(3, lastActivity)
          bindLong(4, createdAt)
          bindLong(5, updatedAt)
        }
    notifyQueries(-1_848_384_477) { emit ->
      emit("GamificationProfile")
    }
  }

  public fun updateGamificationProfile(
    level: Long,
    totalPoints: Long,
    lastActivity: Long?,
    updatedAt: Long,
    userId: String,
  ) {
    driver.execute(-808_315_885, """
        |UPDATE GamificationProfile SET 
        |    level = ?, 
        |    totalPoints = ?, 
        |    lastActivity = ?, 
        |    updatedAt = ? 
        |WHERE userId = ?
        """.trimMargin(), 5) {
          bindLong(0, level)
          bindLong(1, totalPoints)
          bindLong(2, lastActivity)
          bindLong(3, updatedAt)
          bindString(4, userId)
        }
    notifyQueries(-808_315_885) { emit ->
      emit("GamificationProfile")
    }
  }

  public fun insertStreak(
    id: String,
    userId: String,
    type: String,
    currentCount: Long,
    bestCount: Long,
    lastUpdated: Long,
    isActive: Long,
    riskLevel: String,
    recoveryAttempts: Long,
    lastReminderSent: Long?,
    createdAt: Long,
    updatedAt: Long,
  ) {
    driver.execute(-1_625_921_159, """
        |INSERT INTO Streak(id, userId, type, currentCount, bestCount, lastUpdated, isActive, riskLevel, recoveryAttempts, lastReminderSent, createdAt, updatedAt)
        |VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 12) {
          bindString(0, id)
          bindString(1, userId)
          bindString(2, type)
          bindLong(3, currentCount)
          bindLong(4, bestCount)
          bindLong(5, lastUpdated)
          bindLong(6, isActive)
          bindString(7, riskLevel)
          bindLong(8, recoveryAttempts)
          bindLong(9, lastReminderSent)
          bindLong(10, createdAt)
          bindLong(11, updatedAt)
        }
    notifyQueries(-1_625_921_159) { emit ->
      emit("Streak")
    }
  }

  public fun updateStreak(
    currentCount: Long,
    bestCount: Long,
    lastUpdated: Long,
    riskLevel: String,
    recoveryAttempts: Long,
    lastReminderSent: Long?,
    updatedAt: Long,
    id: String,
  ) {
    driver.execute(974_721_929, """
        |UPDATE Streak SET 
        |    currentCount = ?, 
        |    bestCount = ?, 
        |    lastUpdated = ?, 
        |    riskLevel = ?,
        |    recoveryAttempts = ?,
        |    lastReminderSent = ?,
        |    updatedAt = ? 
        |WHERE id = ?
        """.trimMargin(), 8) {
          bindLong(0, currentCount)
          bindLong(1, bestCount)
          bindLong(2, lastUpdated)
          bindString(3, riskLevel)
          bindLong(4, recoveryAttempts)
          bindLong(5, lastReminderSent)
          bindLong(6, updatedAt)
          bindString(7, id)
        }
    notifyQueries(974_721_929) { emit ->
      emit("Streak")
    }
  }

  public fun resetStreak(updatedAt: Long, id: String) {
    driver.execute(-1_680_669_813,
        """UPDATE Streak SET currentCount = 0, updatedAt = ? WHERE id = ?""", 2) {
          bindLong(0, updatedAt)
          bindString(1, id)
        }
    notifyQueries(-1_680_669_813) { emit ->
      emit("Streak")
    }
  }

  public fun insertAchievement(
    id: String,
    userId: String,
    achievementType: String,
    title: String,
    description: String,
    badgeIcon: String,
    category: String,
    unlockedAt: Long,
    createdAt: Long,
  ) {
    driver.execute(1_778_481_684, """
        |INSERT INTO Achievement(id, userId, achievementType, title, description, badgeIcon, category, unlockedAt, createdAt)
        |VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 9) {
          bindString(0, id)
          bindString(1, userId)
          bindString(2, achievementType)
          bindString(3, title)
          bindString(4, description)
          bindString(5, badgeIcon)
          bindString(6, category)
          bindLong(7, unlockedAt)
          bindLong(8, createdAt)
        }
    notifyQueries(1_778_481_684) { emit ->
      emit("Achievement")
    }
  }

  public fun insertPointsHistory(
    id: String,
    userId: String,
    points: Long,
    action: String,
    description: String?,
    earnedAt: Long,
  ) {
    driver.execute(337_588_182, """
        |INSERT INTO PointsHistory(id, userId, points, action, description, earnedAt)
        |VALUES(?, ?, ?, ?, ?, ?)
        """.trimMargin(), 6) {
          bindString(0, id)
          bindString(1, userId)
          bindLong(2, points)
          bindString(3, action)
          bindString(4, description)
          bindLong(5, earnedAt)
        }
    notifyQueries(337_588_182) { emit ->
      emit("PointsHistory")
    }
  }

  public fun insertStreakRecovery(
    id: String,
    userId: String,
    originalStreakId: String,
    streakType: String,
    brokenAt: Long,
    recoveryStarted: Long,
    recoveryCompleted: Long?,
    originalCount: Long,
    isSuccessful: Long,
    createdAt: Long,
    updatedAt: Long,
  ) {
    driver.execute(590_267_438, """
        |INSERT INTO StreakRecovery(id, userId, originalStreakId, streakType, brokenAt, recoveryStarted, recoveryCompleted, originalCount, isSuccessful, createdAt, updatedAt)
        |VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 11) {
          bindString(0, id)
          bindString(1, userId)
          bindString(2, originalStreakId)
          bindString(3, streakType)
          bindLong(4, brokenAt)
          bindLong(5, recoveryStarted)
          bindLong(6, recoveryCompleted)
          bindLong(7, originalCount)
          bindLong(8, isSuccessful)
          bindLong(9, createdAt)
          bindLong(10, updatedAt)
        }
    notifyQueries(590_267_438) { emit ->
      emit("StreakRecovery")
    }
  }

  public fun updateStreakRecovery(
    recoveryCompleted: Long?,
    isSuccessful: Long,
    updatedAt: Long,
    id: String,
  ) {
    driver.execute(1_378_770_494, """
        |UPDATE StreakRecovery SET 
        |    recoveryCompleted = ?,
        |    isSuccessful = ?,
        |    updatedAt = ?
        |WHERE id = ?
        """.trimMargin(), 4) {
          bindLong(0, recoveryCompleted)
          bindLong(1, isSuccessful)
          bindLong(2, updatedAt)
          bindString(3, id)
        }
    notifyQueries(1_378_770_494) { emit ->
      emit("StreakRecovery")
    }
  }

  public fun insertRecoveryAction(
    id: String,
    recoveryId: String,
    actionType: String,
    completedAt: Long,
    pointsAwarded: Long,
    description: String,
    createdAt: Long,
  ) {
    driver.execute(420_025_638, """
        |INSERT INTO RecoveryAction(id, recoveryId, actionType, completedAt, pointsAwarded, description, createdAt)
        |VALUES(?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 7) {
          bindString(0, id)
          bindString(1, recoveryId)
          bindString(2, actionType)
          bindLong(3, completedAt)
          bindLong(4, pointsAwarded)
          bindString(5, description)
          bindLong(6, createdAt)
        }
    notifyQueries(420_025_638) { emit ->
      emit("RecoveryAction")
    }
  }

  public fun insertStreakReminder(
    id: String,
    userId: String,
    streakId: String,
    streakType: String,
    reminderType: String,
    message: String,
    scheduledFor: Long,
    sentAt: Long?,
    isRead: Long,
    createdAt: Long,
  ) {
    driver.execute(870_778_123, """
        |INSERT INTO StreakReminder(id, userId, streakId, streakType, reminderType, message, scheduledFor, sentAt, isRead, createdAt)
        |VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 10) {
          bindString(0, id)
          bindString(1, userId)
          bindString(2, streakId)
          bindString(3, streakType)
          bindString(4, reminderType)
          bindString(5, message)
          bindLong(6, scheduledFor)
          bindLong(7, sentAt)
          bindLong(8, isRead)
          bindLong(9, createdAt)
        }
    notifyQueries(870_778_123) { emit ->
      emit("StreakReminder")
    }
  }

  public fun updateReminderAsRead(id: String) {
    driver.execute(690_227_461, """UPDATE StreakReminder SET isRead = 1 WHERE id = ?""", 1) {
          bindString(0, id)
        }
    notifyQueries(690_227_461) { emit ->
      emit("StreakReminder")
    }
  }

  public fun updateReminderSent(sentAt: Long?, id: String) {
    driver.execute(980_011_765, """UPDATE StreakReminder SET sentAt = ? WHERE id = ?""", 2) {
          bindLong(0, sentAt)
          bindString(1, id)
        }
    notifyQueries(980_011_765) { emit ->
      emit("StreakReminder")
    }
  }

  private inner class SelectGamificationProfileQuery<out T : Any>(
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
        driver.executeQuery(458_706_176, """SELECT * FROM GamificationProfile WHERE userId = ?""",
        mapper, 1) {
      bindString(0, userId)
    }

    override fun toString(): String = "Gamification.sq:selectGamificationProfile"
  }

  private inner class SelectStreaksByUserIdQuery<out T : Any>(
    public val userId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Streak", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Streak", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_969_219_308,
        """SELECT * FROM Streak WHERE userId = ? AND isActive = 1""", mapper, 1) {
      bindString(0, userId)
    }

    override fun toString(): String = "Gamification.sq:selectStreaksByUserId"
  }

  private inner class SelectStreakByTypeQuery<out T : Any>(
    public val userId: String,
    public val type: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Streak", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Streak", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-840_644_403,
        """SELECT * FROM Streak WHERE userId = ? AND type = ? AND isActive = 1""", mapper, 2) {
      bindString(0, userId)
      bindString(1, type)
    }

    override fun toString(): String = "Gamification.sq:selectStreakByType"
  }

  private inner class SelectStreakByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Streak", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Streak", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-117_076_114, """SELECT * FROM Streak WHERE id = ?""", mapper, 1) {
      bindString(0, id)
    }

    override fun toString(): String = "Gamification.sq:selectStreakById"
  }

  private inner class SelectAllUserStreaksQuery<out T : Any>(
    public val userId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Streak", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Streak", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_619_082_745, """SELECT * FROM Streak WHERE userId = ?""", mapper, 1)
        {
      bindString(0, userId)
    }

    override fun toString(): String = "Gamification.sq:selectAllUserStreaks"
  }

  private inner class SelectAchievementsByUserIdQuery<out T : Any>(
    public val userId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Achievement", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Achievement", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-19_146_177,
        """SELECT * FROM Achievement WHERE userId = ? ORDER BY unlockedAt DESC""", mapper, 1) {
      bindString(0, userId)
    }

    override fun toString(): String = "Gamification.sq:selectAchievementsByUserId"
  }

  private inner class SelectAchievementByTypeQuery<out T : Any>(
    public val userId: String,
    public val achievementType: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Achievement", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Achievement", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_023_250_306,
        """SELECT * FROM Achievement WHERE userId = ? AND achievementType = ?""", mapper, 2) {
      bindString(0, userId)
      bindString(1, achievementType)
    }

    override fun toString(): String = "Gamification.sq:selectAchievementByType"
  }

  private inner class SelectPointsHistoryByUserIdQuery<out T : Any>(
    public val userId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("PointsHistory", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("PointsHistory", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_653_519_088,
        """SELECT * FROM PointsHistory WHERE userId = ? ORDER BY earnedAt DESC""", mapper, 1) {
      bindString(0, userId)
    }

    override fun toString(): String = "Gamification.sq:selectPointsHistoryByUserId"
  }

  private inner class SelectRecentPointsHistoryQuery<out T : Any>(
    public val userId: String,
    public val `value`: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("PointsHistory", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("PointsHistory", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(678_452_824,
        """SELECT * FROM PointsHistory WHERE userId = ? ORDER BY earnedAt DESC LIMIT ?""", mapper,
        2) {
      bindString(0, userId)
      bindLong(1, value)
    }

    override fun toString(): String = "Gamification.sq:selectRecentPointsHistory"
  }

  private inner class SelectStreakRecoveryByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("StreakRecovery", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("StreakRecovery", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_526_573_917, """SELECT * FROM StreakRecovery WHERE id = ?""", mapper,
        1) {
      bindString(0, id)
    }

    override fun toString(): String = "Gamification.sq:selectStreakRecoveryById"
  }

  private inner class SelectActiveRecoveriesByUserIdQuery<out T : Any>(
    public val userId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("StreakRecovery", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("StreakRecovery", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(323_672_500,
        """SELECT * FROM StreakRecovery WHERE userId = ? AND isSuccessful = 0 AND recoveryCompleted IS NULL""",
        mapper, 1) {
      bindString(0, userId)
    }

    override fun toString(): String = "Gamification.sq:selectActiveRecoveriesByUserId"
  }

  private inner class SelectAllRecoveriesByUserIdQuery<out T : Any>(
    public val userId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("StreakRecovery", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("StreakRecovery", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-285_266_093,
        """SELECT * FROM StreakRecovery WHERE userId = ? ORDER BY recoveryStarted DESC""", mapper,
        1) {
      bindString(0, userId)
    }

    override fun toString(): String = "Gamification.sq:selectAllRecoveriesByUserId"
  }

  private inner class SelectRecoveryActionsByRecoveryIdQuery<out T : Any>(
    public val recoveryId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("RecoveryAction", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("RecoveryAction", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_559_489_583,
        """SELECT * FROM RecoveryAction WHERE recoveryId = ? ORDER BY completedAt ASC""", mapper, 1)
        {
      bindString(0, recoveryId)
    }

    override fun toString(): String = "Gamification.sq:selectRecoveryActionsByRecoveryId"
  }

  private inner class SelectActiveRemindersByUserIdQuery<out T : Any>(
    public val userId: String,
    public val scheduledFor: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("StreakReminder", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("StreakReminder", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-735_607_238,
        """SELECT * FROM StreakReminder WHERE userId = ? AND isRead = 0 AND scheduledFor <= ? ORDER BY scheduledFor ASC""",
        mapper, 2) {
      bindString(0, userId)
      bindLong(1, scheduledFor)
    }

    override fun toString(): String = "Gamification.sq:selectActiveRemindersByUserId"
  }

  private inner class SelectReminderByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("StreakReminder", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("StreakReminder", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_478_385_950, """SELECT * FROM StreakReminder WHERE id = ?""", mapper,
        1) {
      bindString(0, id)
    }

    override fun toString(): String = "Gamification.sq:selectReminderById"
  }
}
