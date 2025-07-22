package com.north.mobile.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.north.mobile.data.gamification.AchievementType
import com.north.mobile.data.gamification.PointsHistoryEntry
import com.north.mobile.database.NorthDatabase
import com.north.mobile.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * SQLDelight implementation of the GamificationRepository.
 */
class GamificationRepositoryImpl(
    private val database: NorthDatabase
) : GamificationRepository {
    
    override suspend fun getGamificationProfile(userId: String): GamificationProfile? {
        return database.gamificationQueries
            .selectGamificationProfile(userId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .firstOrNull()
            ?.let { row ->
                val streaks = getActiveStreaks(userId)
                val achievements = getAchievements(userId)
                
                GamificationProfile(
                    level = row.level.toInt(),
                    totalPoints = row.totalPoints.toInt(),
                    currentStreaks = streaks,
                    achievements = achievements,
                    lastActivity = row.lastActivity?.let { Instant.fromEpochMilliseconds(it) }
                        ?: Clock.System.now()
                )
            }
    }
    
    override suspend fun createGamificationProfile(profile: GamificationProfile, userId: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        database.gamificationQueries.insertGamificationProfile(
            userId = userId,
            level = profile.level.toLong(),
            totalPoints = profile.totalPoints.toLong(),
            lastActivity = profile.lastActivity.toEpochMilliseconds(),
            createdAt = now,
            updatedAt = now
        )
    }
    
    override suspend fun updateGamificationProfile(profile: GamificationProfile, userId: String) {
        database.gamificationQueries.updateGamificationProfile(
            level = profile.level.toLong(),
            totalPoints = profile.totalPoints.toLong(),
            lastActivity = profile.lastActivity.toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            userId = userId
        )
    }
    
    override suspend fun getStreak(userId: String, streakType: StreakType): Streak? {
        return database.gamificationQueries
            .selectStreakByType(userId, streakType.name)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .firstOrNull()
            ?.let { row ->
                Streak(
                    id = row.id,
                    type = StreakType.valueOf(row.type),
                    currentCount = row.currentCount.toInt(),
                    bestCount = row.bestCount.toInt(),
                    lastActivityDate = Instant.fromEpochMilliseconds(row.lastUpdated)
                        .toLocalDateTime(TimeZone.currentSystemDefault()).date,
                    isActive = row.isActive == 1L,
                    riskLevel = StreakRiskLevel.valueOf(row.riskLevel),
                    recoveryAttempts = row.recoveryAttempts.toInt(),
                    lastReminderSent = row.lastReminderSent?.let { Instant.fromEpochMilliseconds(it) }
                )
            }
    }
    
    override suspend fun getActiveStreaks(userId: String): List<Streak> {
        return database.gamificationQueries
            .selectStreaksByUserId(userId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .firstOrNull()
            ?.map { row ->
                Streak(
                    id = row.id,
                    type = StreakType.valueOf(row.type),
                    currentCount = row.currentCount.toInt(),
                    bestCount = row.bestCount.toInt(),
                    lastActivityDate = Instant.fromEpochMilliseconds(row.lastUpdated)
                        .toLocalDateTime(TimeZone.currentSystemDefault()).date,
                    isActive = row.isActive == 1L,
                    riskLevel = StreakRiskLevel.valueOf(row.riskLevel),
                    recoveryAttempts = row.recoveryAttempts.toInt(),
                    lastReminderSent = row.lastReminderSent?.let { Instant.fromEpochMilliseconds(it) }
                )
            } ?: emptyList()
    }
    
    override suspend fun updateStreak(streak: Streak, userId: String) {
        val existingStreak = getStreak(userId, streak.type)
        val now = Clock.System.now().toEpochMilliseconds()
        
        if (existingStreak != null) {
            database.gamificationQueries.updateStreak(
                currentCount = streak.currentCount.toLong(),
                bestCount = streak.bestCount.toLong(),
                lastUpdated = streak.lastActivityDate.toEpochDays() * 24 * 60 * 60 * 1000, // Convert to milliseconds
                riskLevel = streak.riskLevel.name,
                recoveryAttempts = streak.recoveryAttempts.toLong(),
                lastReminderSent = streak.lastReminderSent?.toEpochMilliseconds(),
                updatedAt = now,
                id = streak.id
            )
        } else {
            database.gamificationQueries.insertStreak(
                id = streak.id,
                userId = userId,
                type = streak.type.name,
                currentCount = streak.currentCount.toLong(),
                bestCount = streak.bestCount.toLong(),
                lastUpdated = streak.lastActivityDate.toEpochDays() * 24 * 60 * 60 * 1000,
                isActive = if (streak.isActive) 1L else 0L,
                riskLevel = streak.riskLevel.name,
                recoveryAttempts = streak.recoveryAttempts.toLong(),
                lastReminderSent = streak.lastReminderSent?.toEpochMilliseconds(),
                createdAt = now,
                updatedAt = now
            )
        }
    }
    
    override suspend fun getAchievement(userId: String, achievementType: AchievementType): Achievement? {
        return database.gamificationQueries
            .selectAchievementByType(userId, achievementType.name)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .firstOrNull()
            ?.let { row ->
                Achievement(
                    id = row.id,
                    title = row.title,
                    description = row.description,
                    badgeIcon = row.badgeIcon,
                    pointsAwarded = 0, // Points are awarded separately, not stored in achievement
                    unlockedAt = Instant.fromEpochMilliseconds(row.unlockedAt),
                    category = AchievementCategory.valueOf(row.category)
                )
            }
    }
    
    override suspend fun getAchievements(userId: String): List<Achievement> {
        return database.gamificationQueries
            .selectAchievementsByUserId(userId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .firstOrNull()
            ?.map { row ->
                Achievement(
                    id = row.id,
                    title = row.title,
                    description = row.description,
                    badgeIcon = row.badgeIcon,
                    pointsAwarded = 0, // Points are awarded separately
                    unlockedAt = Instant.fromEpochMilliseconds(row.unlockedAt),
                    category = AchievementCategory.valueOf(row.category)
                )
            } ?: emptyList()
    }
    
    override suspend fun addAchievement(achievement: Achievement, userId: String, achievementType: AchievementType) {
        database.gamificationQueries.insertAchievement(
            id = achievement.id,
            userId = userId,
            achievementType = achievementType.name,
            title = achievement.title,
            description = achievement.description,
            badgeIcon = achievement.badgeIcon,
            category = achievement.category.name,
            unlockedAt = achievement.unlockedAt.toEpochMilliseconds(),
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
    }
    
    override suspend fun addPointsHistory(entry: PointsHistoryEntry, userId: String) {
        database.gamificationQueries.insertPointsHistory(
            id = entry.id,
            userId = userId,
            points = entry.points.toLong(),
            action = entry.action.name,
            description = entry.description,
            earnedAt = entry.earnedAt.toEpochMilliseconds()
        )
    }
    
    override suspend fun getPointsHistory(userId: String, limit: Int): List<PointsHistoryEntry> {
        return database.gamificationQueries
            .selectRecentPointsHistory(userId, limit.toLong())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .firstOrNull()
            ?.map { row ->
                PointsHistoryEntry(
                    id = row.id,
                    points = row.points.toInt(),
                    action = UserAction.valueOf(row.action),
                    description = row.description,
                    earnedAt = Instant.fromEpochMilliseconds(row.earnedAt)
                )
            } ?: emptyList()
    }
    
    // Enhanced streak tracking methods
    
    override suspend fun getStreakById(streakId: String): Streak? {
        return database.gamificationQueries
            .selectStreakById(streakId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .firstOrNull()
            ?.let { row ->
                Streak(
                    id = row.id,
                    type = StreakType.valueOf(row.type),
                    currentCount = row.currentCount.toInt(),
                    bestCount = row.bestCount.toInt(),
                    lastActivityDate = Instant.fromEpochMilliseconds(row.lastUpdated)
                        .toLocalDateTime(TimeZone.currentSystemDefault()).date,
                    isActive = row.isActive == 1L,
                    riskLevel = StreakRiskLevel.valueOf(row.riskLevel),
                    recoveryAttempts = row.recoveryAttempts.toInt(),
                    lastReminderSent = row.lastReminderSent?.let { Instant.fromEpochMilliseconds(it) }
                )
            }
    }
    
    override suspend fun getAllUserStreaks(userId: String): List<Streak> {
        return database.gamificationQueries
            .selectAllUserStreaks(userId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .firstOrNull()
            ?.map { row ->
                Streak(
                    id = row.id,
                    type = StreakType.valueOf(row.type),
                    currentCount = row.currentCount.toInt(),
                    bestCount = row.bestCount.toInt(),
                    lastActivityDate = Instant.fromEpochMilliseconds(row.lastUpdated)
                        .toLocalDateTime(TimeZone.currentSystemDefault()).date,
                    isActive = row.isActive == 1L,
                    riskLevel = StreakRiskLevel.valueOf(row.riskLevel),
                    recoveryAttempts = row.recoveryAttempts.toInt(),
                    lastReminderSent = row.lastReminderSent?.let { Instant.fromEpochMilliseconds(it) }
                )
            } ?: emptyList()
    }
    
    override suspend fun createStreakRecovery(recovery: StreakRecovery) {
        val now = Clock.System.now().toEpochMilliseconds()
        database.gamificationQueries.insertStreakRecovery(
            id = recovery.id,
            userId = recovery.userId,
            originalStreakId = recovery.originalStreakId,
            streakType = recovery.streakType.name,
            brokenAt = recovery.brokenAt.toEpochMilliseconds(),
            recoveryStarted = recovery.recoveryStarted.toEpochMilliseconds(),
            recoveryCompleted = recovery.recoveryCompleted?.toEpochMilliseconds(),
            originalCount = recovery.originalCount.toLong(),
            isSuccessful = if (recovery.isSuccessful) 1L else 0L,
            createdAt = now,
            updatedAt = now
        )
        
        // Insert recovery actions if any
        for (action in recovery.recoveryActions) {
            database.gamificationQueries.insertRecoveryAction(
                id = action.id,
                recoveryId = recovery.id,
                actionType = action.actionType.name,
                completedAt = action.completedAt.toEpochMilliseconds(),
                pointsAwarded = action.pointsAwarded.toLong(),
                description = action.description,
                createdAt = now
            )
        }
    }
    
    override suspend fun getStreakRecovery(recoveryId: String): StreakRecovery? {
        return database.gamificationQueries
            .selectStreakRecoveryById(recoveryId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .firstOrNull()
            ?.let { row ->
                val recoveryActions = database.gamificationQueries
                    .selectRecoveryActionsByRecoveryId(recoveryId)
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .firstOrNull()
                    ?.map { actionRow ->
                        RecoveryAction(
                            id = actionRow.id,
                            actionType = UserAction.valueOf(actionRow.actionType),
                            completedAt = Instant.fromEpochMilliseconds(actionRow.completedAt),
                            pointsAwarded = actionRow.pointsAwarded.toInt(),
                            description = actionRow.description
                        )
                    } ?: emptyList()
                
                StreakRecovery(
                    id = row.id,
                    userId = row.userId,
                    originalStreakId = row.originalStreakId,
                    streakType = StreakType.valueOf(row.streakType),
                    brokenAt = Instant.fromEpochMilliseconds(row.brokenAt),
                    recoveryStarted = Instant.fromEpochMilliseconds(row.recoveryStarted),
                    recoveryCompleted = row.recoveryCompleted?.let { Instant.fromEpochMilliseconds(it) },
                    originalCount = row.originalCount.toInt(),
                    recoveryActions = recoveryActions,
                    isSuccessful = row.isSuccessful == 1L
                )
            }
    }
    
    override suspend fun updateStreakRecovery(recovery: StreakRecovery) {
        database.gamificationQueries.updateStreakRecovery(
            recoveryCompleted = recovery.recoveryCompleted?.toEpochMilliseconds(),
            isSuccessful = if (recovery.isSuccessful) 1L else 0L,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            id = recovery.id
        )
        
        // Add any new recovery actions
        val existingActions = database.gamificationQueries
            .selectRecoveryActionsByRecoveryId(recovery.id)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .firstOrNull()
            ?.map { it.id }
            ?.toSet() ?: emptySet()
        
        val newActions = recovery.recoveryActions.filter { it.id !in existingActions }
        for (action in newActions) {
            database.gamificationQueries.insertRecoveryAction(
                id = action.id,
                recoveryId = recovery.id,
                actionType = action.actionType.name,
                completedAt = action.completedAt.toEpochMilliseconds(),
                pointsAwarded = action.pointsAwarded.toLong(),
                description = action.description,
                createdAt = Clock.System.now().toEpochMilliseconds()
            )
        }
    }
    
    override suspend fun getActiveRecoveries(userId: String): List<StreakRecovery> {
        return database.gamificationQueries
            .selectActiveRecoveriesByUserId(userId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .firstOrNull()
            ?.map { row ->
                val recoveryActions = database.gamificationQueries
                    .selectRecoveryActionsByRecoveryId(row.id)
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .firstOrNull()
                    ?.map { actionRow ->
                        RecoveryAction(
                            id = actionRow.id,
                            actionType = UserAction.valueOf(actionRow.actionType),
                            completedAt = Instant.fromEpochMilliseconds(actionRow.completedAt),
                            pointsAwarded = actionRow.pointsAwarded.toInt(),
                            description = actionRow.description
                        )
                    } ?: emptyList()
                
                StreakRecovery(
                    id = row.id,
                    userId = row.userId,
                    originalStreakId = row.originalStreakId,
                    streakType = StreakType.valueOf(row.streakType),
                    brokenAt = Instant.fromEpochMilliseconds(row.brokenAt),
                    recoveryStarted = Instant.fromEpochMilliseconds(row.recoveryStarted),
                    recoveryCompleted = row.recoveryCompleted?.let { Instant.fromEpochMilliseconds(it) },
                    originalCount = row.originalCount.toInt(),
                    recoveryActions = recoveryActions,
                    isSuccessful = row.isSuccessful == 1L
                )
            } ?: emptyList()
    }
    
    override suspend fun getAllRecoveries(userId: String): List<StreakRecovery> {
        return database.gamificationQueries
            .selectAllRecoveriesByUserId(userId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .firstOrNull()
            ?.map { row ->
                val recoveryActions = database.gamificationQueries
                    .selectRecoveryActionsByRecoveryId(row.id)
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .firstOrNull()
                    ?.map { actionRow ->
                        RecoveryAction(
                            id = actionRow.id,
                            actionType = UserAction.valueOf(actionRow.actionType),
                            completedAt = Instant.fromEpochMilliseconds(actionRow.completedAt),
                            pointsAwarded = actionRow.pointsAwarded.toInt(),
                            description = actionRow.description
                        )
                    } ?: emptyList()
                
                StreakRecovery(
                    id = row.id,
                    userId = row.userId,
                    originalStreakId = row.originalStreakId,
                    streakType = StreakType.valueOf(row.streakType),
                    brokenAt = Instant.fromEpochMilliseconds(row.brokenAt),
                    recoveryStarted = Instant.fromEpochMilliseconds(row.recoveryStarted),
                    recoveryCompleted = row.recoveryCompleted?.let { Instant.fromEpochMilliseconds(it) },
                    originalCount = row.originalCount.toInt(),
                    recoveryActions = recoveryActions,
                    isSuccessful = row.isSuccessful == 1L
                )
            } ?: emptyList()
    }
    
    override suspend fun createStreakReminder(reminder: StreakReminder) {
        database.gamificationQueries.insertStreakReminder(
            id = reminder.id,
            userId = reminder.userId,
            streakId = reminder.streakId,
            streakType = reminder.streakType.name,
            reminderType = reminder.reminderType.name,
            message = reminder.message,
            scheduledFor = reminder.scheduledFor.toEpochMilliseconds(),
            sentAt = reminder.sentAt?.toEpochMilliseconds(),
            isRead = if (reminder.isRead) 1L else 0L,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
    }
    
    override suspend fun getActiveReminders(userId: String): List<StreakReminder> {
        val now = Clock.System.now().toEpochMilliseconds()
        return database.gamificationQueries
            .selectActiveRemindersByUserId(userId, now)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .firstOrNull()
            ?.map { row ->
                StreakReminder(
                    id = row.id,
                    userId = row.userId,
                    streakId = row.streakId,
                    streakType = StreakType.valueOf(row.streakType),
                    reminderType = ReminderType.valueOf(row.reminderType),
                    message = row.message,
                    scheduledFor = Instant.fromEpochMilliseconds(row.scheduledFor),
                    sentAt = row.sentAt?.let { Instant.fromEpochMilliseconds(it) },
                    isRead = row.isRead == 1L
                )
            } ?: emptyList()
    }
    
    override suspend fun markReminderAsRead(reminderId: String) {
        database.gamificationQueries.updateReminderAsRead(reminderId)
    }
}