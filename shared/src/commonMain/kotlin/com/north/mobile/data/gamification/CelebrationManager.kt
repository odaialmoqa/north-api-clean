package com.north.mobile.data.gamification

import com.north.mobile.domain.model.Achievement
import com.north.mobile.domain.model.UserAction
import kotlinx.datetime.Instant

/**
 * Manager for handling celebrations, animations, and user feedback for gamification events.
 */
interface CelebrationManager {
    
    /**
     * Triggers a celebration for points awarded.
     * @param pointsAwarded The number of points awarded
     * @param action The action that triggered the points
     * @param totalPoints The user's total points after the award
     */
    suspend fun celebratePointsAwarded(
        pointsAwarded: Int,
        action: UserAction,
        totalPoints: Int
    ): CelebrationEvent
    
    /**
     * Triggers a level-up celebration.
     * @param levelUpResult The level up information
     */
    suspend fun celebrateLevelUp(levelUpResult: LevelUpResult): CelebrationEvent
    
    /**
     * Triggers an achievement unlock celebration.
     * @param achievement The unlocked achievement
     */
    suspend fun celebrateAchievement(achievement: Achievement): CelebrationEvent
    
    /**
     * Triggers a streak milestone celebration.
     * @param streakType The type of streak
     * @param streakCount The current streak count
     * @param isNewRecord Whether this is a new personal record
     */
    suspend fun celebrateStreak(
        streakType: com.north.mobile.domain.model.StreakType,
        streakCount: Int,
        isNewRecord: Boolean
    ): CelebrationEvent
    
    /**
     * Triggers a micro-win celebration.
     * @param microWinTitle The title of the micro-win
     * @param pointsAwarded Points awarded for the micro-win
     */
    suspend fun celebrateMicroWin(
        microWinTitle: String,
        pointsAwarded: Int
    ): CelebrationEvent
}

/**
 * Implementation of the CelebrationManager.
 */
class CelebrationManagerImpl : CelebrationManager {
    
    override suspend fun celebratePointsAwarded(
        pointsAwarded: Int,
        action: UserAction,
        totalPoints: Int
    ): CelebrationEvent {
        val intensity = when {
            pointsAwarded >= 50 -> CelebrationIntensity.HIGH
            pointsAwarded >= 20 -> CelebrationIntensity.MEDIUM
            else -> CelebrationIntensity.LOW
        }
        
        return CelebrationEvent(
            type = CelebrationType.POINTS_AWARDED,
            title = "+$pointsAwarded points!",
            message = getPointsMessage(action, pointsAwarded),
            intensity = intensity,
            duration = getDurationForIntensity(intensity),
            animations = getPointsAnimations(intensity),
            sounds = getPointsSounds(intensity),
            hapticFeedback = getPointsHaptics(intensity),
            timestamp = kotlinx.datetime.Clock.System.now()
        )
    }
    
    override suspend fun celebrateLevelUp(levelUpResult: LevelUpResult): CelebrationEvent {
        return CelebrationEvent(
            type = CelebrationType.LEVEL_UP,
            title = "Level Up! 🎉",
            message = levelUpResult.celebrationMessage,
            intensity = CelebrationIntensity.HIGH,
            duration = 3000L, // 3 seconds
            animations = listOf(
                AnimationType.CONFETTI,
                AnimationType.LEVEL_UP_BURST,
                AnimationType.PROGRESS_RING_FILL,
                AnimationType.BOUNCE_IN
            ),
            sounds = listOf(SoundType.LEVEL_UP_FANFARE),
            hapticFeedback = listOf(HapticType.SUCCESS_PATTERN),
            timestamp = kotlinx.datetime.Clock.System.now(),
            additionalData = mapOf(
                "oldLevel" to levelUpResult.oldLevel.toString(),
                "newLevel" to levelUpResult.newLevel.toString(),
                "unlockedFeatures" to levelUpResult.unlockedFeatures.joinToString(", ")
            )
        )
    }
    
    override suspend fun celebrateAchievement(achievement: Achievement): CelebrationEvent {
        return CelebrationEvent(
            type = CelebrationType.ACHIEVEMENT_UNLOCKED,
            title = "Achievement Unlocked! 🏆",
            message = "${achievement.badgeIcon} ${achievement.title}",
            intensity = CelebrationIntensity.HIGH,
            duration = 2500L,
            animations = listOf(
                AnimationType.BADGE_REVEAL,
                AnimationType.SPARKLE_BURST,
                AnimationType.SLIDE_IN_FROM_TOP
            ),
            sounds = listOf(SoundType.ACHIEVEMENT_CHIME),
            hapticFeedback = listOf(HapticType.ACHIEVEMENT_PATTERN),
            timestamp = kotlinx.datetime.Clock.System.now(),
            additionalData = mapOf(
                "achievementId" to achievement.id,
                "badgeIcon" to achievement.badgeIcon,
                "category" to achievement.category.name,
                "description" to achievement.description
            )
        )
    }
    
    override suspend fun celebrateStreak(
        streakType: com.north.mobile.domain.model.StreakType,
        streakCount: Int,
        isNewRecord: Boolean
    ): CelebrationEvent {
        val intensity = when {
            streakCount >= 30 -> CelebrationIntensity.HIGH
            streakCount >= 7 -> CelebrationIntensity.MEDIUM
            else -> CelebrationIntensity.LOW
        }
        
        val title = if (isNewRecord) "New Record! 🔥" else "Streak Continues! 🔥"
        val message = "$streakCount day${if (streakCount != 1) "s" else ""} ${getStreakTypeDisplayName(streakType)}"
        
        return CelebrationEvent(
            type = CelebrationType.STREAK_MILESTONE,
            title = title,
            message = message,
            intensity = intensity,
            duration = getDurationForIntensity(intensity),
            animations = getStreakAnimations(intensity, isNewRecord),
            sounds = getStreakSounds(intensity),
            hapticFeedback = getStreakHaptics(intensity),
            timestamp = kotlinx.datetime.Clock.System.now(),
            additionalData = mapOf(
                "streakType" to streakType.name,
                "streakCount" to streakCount.toString(),
                "isNewRecord" to isNewRecord.toString()
            )
        )
    }
    
    override suspend fun celebrateMicroWin(
        microWinTitle: String,
        pointsAwarded: Int
    ): CelebrationEvent {
        return CelebrationEvent(
            type = CelebrationType.MICRO_WIN,
            title = "Micro Win! ⭐",
            message = microWinTitle,
            intensity = CelebrationIntensity.LOW,
            duration = 1500L,
            animations = listOf(
                AnimationType.GENTLE_BOUNCE,
                AnimationType.STAR_TWINKLE
            ),
            sounds = listOf(SoundType.MICRO_WIN_CHIME),
            hapticFeedback = listOf(HapticType.GENTLE_TAP),
            timestamp = kotlinx.datetime.Clock.System.now(),
            additionalData = mapOf(
                "pointsAwarded" to pointsAwarded.toString()
            )
        )
    }
    
    private fun getPointsMessage(action: UserAction, points: Int): String {
        return when (action) {
            UserAction.CHECK_BALANCE -> "Great job checking your balance!"
            UserAction.CATEGORIZE_TRANSACTION -> "Thanks for organizing your transactions!"
            UserAction.UPDATE_GOAL -> "Goal updated successfully!"
            UserAction.LINK_ACCOUNT -> "Account linked! You're all set!"
            UserAction.COMPLETE_MICRO_TASK -> "Micro task completed!"
            UserAction.REVIEW_INSIGHTS -> "Insights reviewed! Stay informed!"
            UserAction.SET_BUDGET -> "Budget set! You're on track!"
            UserAction.MAKE_SAVINGS_CONTRIBUTION -> "Great savings contribution!"
        }
    }
    
    private fun getDurationForIntensity(intensity: CelebrationIntensity): Long {
        return when (intensity) {
            CelebrationIntensity.LOW -> 1000L
            CelebrationIntensity.MEDIUM -> 2000L
            CelebrationIntensity.HIGH -> 3000L
        }
    }
    
    private fun getPointsAnimations(intensity: CelebrationIntensity): List<AnimationType> {
        return when (intensity) {
            CelebrationIntensity.LOW -> listOf(AnimationType.GENTLE_BOUNCE)
            CelebrationIntensity.MEDIUM -> listOf(AnimationType.BOUNCE_IN, AnimationType.SPARKLE)
            CelebrationIntensity.HIGH -> listOf(AnimationType.CONFETTI, AnimationType.BOUNCE_IN, AnimationType.SPARKLE)
        }
    }
    
    private fun getPointsSounds(intensity: CelebrationIntensity): List<SoundType> {
        return when (intensity) {
            CelebrationIntensity.LOW -> listOf(SoundType.GENTLE_CHIME)
            CelebrationIntensity.MEDIUM -> listOf(SoundType.SUCCESS_CHIME)
            CelebrationIntensity.HIGH -> listOf(SoundType.BIG_SUCCESS_FANFARE)
        }
    }
    
    private fun getPointsHaptics(intensity: CelebrationIntensity): List<HapticType> {
        return when (intensity) {
            CelebrationIntensity.LOW -> listOf(HapticType.GENTLE_TAP)
            CelebrationIntensity.MEDIUM -> listOf(HapticType.SUCCESS_PATTERN)
            CelebrationIntensity.HIGH -> listOf(HapticType.CELEBRATION_PATTERN)
        }
    }
    
    private fun getStreakAnimations(intensity: CelebrationIntensity, isNewRecord: Boolean): List<AnimationType> {
        val baseAnimations = when (intensity) {
            CelebrationIntensity.LOW -> listOf(AnimationType.FLAME_FLICKER)
            CelebrationIntensity.MEDIUM -> listOf(AnimationType.FLAME_FLICKER, AnimationType.SPARKLE)
            CelebrationIntensity.HIGH -> listOf(AnimationType.FLAME_BURST, AnimationType.CONFETTI)
        }
        
        return if (isNewRecord) {
            baseAnimations + AnimationType.RECORD_BADGE_REVEAL
        } else {
            baseAnimations
        }
    }
    
    private fun getStreakSounds(intensity: CelebrationIntensity): List<SoundType> {
        return when (intensity) {
            CelebrationIntensity.LOW -> listOf(SoundType.GENTLE_CHIME)
            CelebrationIntensity.MEDIUM -> listOf(SoundType.STREAK_CHIME)
            CelebrationIntensity.HIGH -> listOf(SoundType.STREAK_FANFARE)
        }
    }
    
    private fun getStreakHaptics(intensity: CelebrationIntensity): List<HapticType> {
        return when (intensity) {
            CelebrationIntensity.LOW -> listOf(HapticType.GENTLE_TAP)
            CelebrationIntensity.MEDIUM -> listOf(HapticType.SUCCESS_PATTERN)
            CelebrationIntensity.HIGH -> listOf(HapticType.CELEBRATION_PATTERN)
        }
    }
    
    private fun getStreakTypeDisplayName(streakType: com.north.mobile.domain.model.StreakType): String {
        return when (streakType) {
            com.north.mobile.domain.model.StreakType.DAILY_CHECK_IN -> "checking in"
            com.north.mobile.domain.model.StreakType.UNDER_BUDGET -> "staying under budget"
            com.north.mobile.domain.model.StreakType.GOAL_PROGRESS -> "making goal progress"
            com.north.mobile.domain.model.StreakType.TRANSACTION_CATEGORIZATION -> "categorizing transactions"
            com.north.mobile.domain.model.StreakType.SAVINGS_CONTRIBUTION -> "saving money"
        }
    }
}

/**
 * Represents a celebration event with all its visual and audio components.
 */
data class CelebrationEvent(
    val type: CelebrationType,
    val title: String,
    val message: String,
    val intensity: CelebrationIntensity,
    val duration: Long, // Duration in milliseconds
    val animations: List<AnimationType>,
    val sounds: List<SoundType>,
    val hapticFeedback: List<HapticType>,
    val timestamp: Instant,
    val additionalData: Map<String, String> = emptyMap()
)

/**
 * Types of celebrations that can occur.
 */
enum class CelebrationType {
    POINTS_AWARDED,
    LEVEL_UP,
    ACHIEVEMENT_UNLOCKED,
    STREAK_MILESTONE,
    MICRO_WIN,
    GOAL_ACHIEVED,
    MILESTONE_REACHED
}

/**
 * Intensity levels for celebrations.
 */
enum class CelebrationIntensity {
    LOW,    // Subtle feedback
    MEDIUM, // Noticeable celebration
    HIGH    // Full celebration with all effects
}

/**
 * Types of animations that can be triggered.
 */
enum class AnimationType {
    // Basic animations
    GENTLE_BOUNCE,
    BOUNCE_IN,
    SLIDE_IN_FROM_TOP,
    FADE_IN,
    
    // Particle effects
    CONFETTI,
    SPARKLE,
    SPARKLE_BURST,
    STAR_TWINKLE,
    
    // Specific animations
    LEVEL_UP_BURST,
    PROGRESS_RING_FILL,
    BADGE_REVEAL,
    FLAME_FLICKER,
    FLAME_BURST,
    RECORD_BADGE_REVEAL
}

/**
 * Types of sounds that can be played.
 */
enum class SoundType {
    GENTLE_CHIME,
    SUCCESS_CHIME,
    BIG_SUCCESS_FANFARE,
    LEVEL_UP_FANFARE,
    ACHIEVEMENT_CHIME,
    MICRO_WIN_CHIME,
    STREAK_CHIME,
    STREAK_FANFARE
}

/**
 * Types of haptic feedback patterns.
 */
enum class HapticType {
    GENTLE_TAP,
    SUCCESS_PATTERN,
    ACHIEVEMENT_PATTERN,
    CELEBRATION_PATTERN
}