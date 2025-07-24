package com.north.mobile.gamification

import androidx.compose.ui.graphics.vector.ImageVector

enum class AchievementLevel {
    BRONZE, SILVER, GOLD, PLATINUM
}

enum class ChallengeDifficulty {
    EASY, MEDIUM, HARD
}

data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val icon: String, // Using emoji/text instead of ImageVector for simplicity
    val level: AchievementLevel,
    val unlocked: Boolean,
    val progress: Float = 0f, // 0.0 to 1.0
    val category: String
)

data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: ChallengeDifficulty,
    val rewardPoints: Int,
    val durationDays: Int,
    val accepted: Boolean = false,
    val completed: Boolean = false,
    val progress: Float = 0f // 0.0 to 1.0
)

data class UserLevel(
    val level: Int,
    val currentPoints: Int,
    val pointsToNextLevel: Int,
    val unlockedFeatures: List<String>
)

// Sample data for testing
object SampleGamificationData {
    val achievements = listOf(
        Achievement(
            id = "save_first_1000",
            name = "First $1,000 Saved",
            description = "Save your first $1,000",
            icon = "💰",
            level = AchievementLevel.BRONZE,
            unlocked = true,
            progress = 1.0f,
            category = "Savings"
        ),
        Achievement(
            id = "tfsa_contribution",
            name = "TFSA Contributor",
            description = "Make your first TFSA contribution",
            icon = "📈",
            level = AchievementLevel.SILVER,
            unlocked = true,
            progress = 1.0f,
            category = "Investing"
        ),
        Achievement(
            id = "budget_master",
            name = "Budget Master",
            description = "Stay within budget for 3 consecutive months",
            icon = "🎯",
            level = AchievementLevel.GOLD,
            unlocked = false,
            progress = 0.66f,
            category = "Budgeting"
        ),
        Achievement(
            id = "financial_learner",
            name = "Financial Scholar",
            description = "Complete 5 financial education modules",
            icon = "🎓",
            level = AchievementLevel.BRONZE,
            unlocked = false,
            progress = 0.4f,
            category = "Education"
        )
    )
    
    val currentChallenge = Challenge(
        id = "reduce_dining",
        title = "Dining Dieter",
        description = "Reduce dining expenses by 15% this week",
        difficulty = ChallengeDifficulty.MEDIUM,
        rewardPoints = 50,
        durationDays = 7,
        accepted = true,
        completed = false,
        progress = 0.7f
    )
    
    val userLevel = UserLevel(
        level = 3,
        currentPoints = 275,
        pointsToNextLevel = 400,
        unlockedFeatures = listOf("Basic Analytics", "Goal Tracking", "Budget Categories")
    )
    
    val currentStreak = 5
    val bestStreak = 12
    val financialHealthScore = 78
}