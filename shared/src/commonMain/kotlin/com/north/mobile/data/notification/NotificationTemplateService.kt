package com.north.mobile.data.notification

import kotlin.random.Random

class NotificationTemplateServiceImpl : NotificationTemplateService {

    private val templates = mapOf(
        NotificationType.STREAK_RISK to NotificationTemplate(
            type = NotificationType.STREAK_RISK,
            titleTemplates = listOf(
                "Don't break your streak! 🔥",
                "Keep the momentum going! 💪",
                "Your streak needs you! ⚡",
                "Almost there - don't stop now! 🎯"
            ),
            bodyTemplates = listOf(
                "Your {streakType} streak of {days} days is at risk. A quick check-in will keep it alive!",
                "You've built an amazing {days}-day {streakType} streak. Don't let it slip away!",
                "Just {hours} hours left to maintain your {streakType} streak. You've got this!",
                "Your {days}-day streak shows real commitment. Keep it going with a quick action!"
            ),
            dataKeys = listOf("streakType", "days", "hours")
        ),

        NotificationType.ENGAGEMENT_REMINDER to NotificationTemplate(
            type = NotificationType.ENGAGEMENT_REMINDER,
            titleTemplates = listOf(
                "Your finances miss you! 💙",
                "Ready for your next win? 🏆",
                "Time to check your progress! 📈",
                "Your goals are waiting! 🎯",
                "Let's build on your success! ✨"
            ),
            bodyTemplates = listOf(
                "You're {progress}% closer to your goals. See what's new and grab some quick wins!",
                "Great progress on {goalName}! Check in to see your latest achievements.",
                "You have {microWins} micro-wins waiting. Each one gets you closer to your goals!",
                "Your last milestone: {milestone}. Ready to unlock the next one?",
                "It's been {days} days since your last visit. Your progress is still here waiting!"
            ),
            dataKeys = listOf("progress", "goalName", "microWins", "milestone", "days")
        ),

        NotificationType.GOAL_PROGRESS to NotificationTemplate(
            type = NotificationType.GOAL_PROGRESS,
            titleTemplates = listOf(
                "Goal progress update! 📊",
                "You're making great progress! 🎉",
                "Milestone alert! 🎯",
                "Your goal is within reach! 💪"
            ),
            bodyTemplates = listOf(
                "Amazing! You're now {progress}% complete on your {goalName} goal.",
                "You've reached {progress}% of your {goalName} target. Keep up the great work!",
                "Fantastic progress! Your {goalName} goal is {progress}% complete.",
                "You're {amount} closer to your {goalName} goal. Only {remaining} to go!"
            ),
            dataKeys = listOf("progress", "goalName", "amount", "remaining")
        ),

        NotificationType.MILESTONE_CELEBRATION to NotificationTemplate(
            type = NotificationType.MILESTONE_CELEBRATION,
            titleTemplates = listOf(
                "🎉 Milestone achieved!",
                "🏆 You did it!",
                "🌟 Amazing achievement!",
                "🎊 Celebration time!",
                "💎 New milestone unlocked!"
            ),
            bodyTemplates = listOf(
                "Congratulations! You've achieved: {milestone}",
                "Incredible work! {milestone} - you should be proud!",
                "🎉 {milestone} achieved! Your dedication is paying off.",
                "Way to go! {milestone} completed. What's your next challenge?",
                "Outstanding! {milestone} unlocked. You're building great financial habits!"
            ),
            dataKeys = listOf("milestone")
        ),

        NotificationType.MICRO_WIN_AVAILABLE to NotificationTemplate(
            type = NotificationType.MICRO_WIN_AVAILABLE,
            titleTemplates = listOf(
                "Quick win available! ⚡",
                "Easy points waiting! 🎯",
                "Micro-win opportunity! ✨",
                "Fast progress ahead! 🚀"
            ),
            bodyTemplates = listOf(
                "You have {count} quick wins available. Each takes less than 2 minutes!",
                "Easy {points} points waiting: {action}",
                "Quick action available: {action} (+{points} points)",
                "{count} micro-wins ready to boost your progress!"
            ),
            dataKeys = listOf("count", "points", "action")
        ),

        NotificationType.WEEKLY_SUMMARY to NotificationTemplate(
            type = NotificationType.WEEKLY_SUMMARY,
            titleTemplates = listOf(
                "Your week in review 📊",
                "Weekly progress report 📈",
                "This week's achievements 🏆",
                "Week summary ready! ✨"
            ),
            bodyTemplates = listOf(
                "This week: {achievements} achievements, {streaks} active streaks, {progress}% goal progress!",
                "Great week! You earned {points} points and maintained {streaks} streaks.",
                "Weekly highlights: {topAchievement} and {progress}% closer to your goals!",
                "This week you: {summary}. Keep up the momentum!"
            ),
            dataKeys = listOf("achievements", "streaks", "progress", "points", "topAchievement", "summary")
        )
    )

    override fun getTemplate(type: NotificationType): NotificationTemplate {
        return templates[type] ?: throw IllegalArgumentException("No template found for type: $type")
    }

    override fun generatePersonalizedContent(
        template: NotificationTemplate,
        userData: UserEngagementData
    ): NotificationContent {
        val title = template.titleTemplates.random()
        val bodyTemplate = template.bodyTemplates.random()
        
        val personalizedBody = when (template.type) {
            NotificationType.ENGAGEMENT_REMINDER -> personalizeEngagementReminder(bodyTemplate, userData)
            NotificationType.GOAL_PROGRESS -> personalizeGoalProgress(bodyTemplate, userData)
            NotificationType.MILESTONE_CELEBRATION -> personalizeMilestone(bodyTemplate, userData)
            NotificationType.MICRO_WIN_AVAILABLE -> personalizeMicroWin(bodyTemplate, userData)
            NotificationType.WEEKLY_SUMMARY -> personalizeWeeklySummary(bodyTemplate, userData)
            else -> bodyTemplate
        }

        return NotificationContent(
            title = title,
            body = personalizedBody,
            data = mapOf(
                "userId" to userData.userId,
                "type" to template.type.name
            )
        )
    }

    override fun getStreakRiskMessage(streakType: String, daysActive: Int): NotificationContent {
        val template = getTemplate(NotificationType.STREAK_RISK)
        val title = template.titleTemplates.random()
        val bodyTemplate = template.bodyTemplates.random()
        
        val body = bodyTemplate
            .replace("{streakType}", formatStreakType(streakType))
            .replace("{days}", daysActive.toString())
            .replace("{hours}", "4") // Assuming 4 hours left in day

        return NotificationContent(
            title = title,
            body = body,
            data = mapOf(
                "streakType" to streakType,
                "days" to daysActive.toString(),
                "type" to NotificationType.STREAK_RISK.name
            )
        )
    }

    override fun getEngagementReminderMessage(userData: UserEngagementData): NotificationContent {
        val template = getTemplate(NotificationType.ENGAGEMENT_REMINDER)
        return generatePersonalizedContent(template, userData)
    }

    override fun getGoalProgressMessage(goalName: String, progress: Double): NotificationContent {
        val template = getTemplate(NotificationType.GOAL_PROGRESS)
        val title = template.titleTemplates.random()
        val bodyTemplate = template.bodyTemplates.random()
        
        val progressPercent = (progress * 100).toInt()
        val body = bodyTemplate
            .replace("{goalName}", goalName)
            .replace("{progress}", progressPercent.toString())

        return NotificationContent(
            title = title,
            body = body,
            data = mapOf(
                "goalName" to goalName,
                "progress" to progressPercent.toString(),
                "type" to NotificationType.GOAL_PROGRESS.name
            )
        )
    }

    override fun getMilestoneMessage(milestone: String): NotificationContent {
        val template = getTemplate(NotificationType.MILESTONE_CELEBRATION)
        val title = template.titleTemplates.random()
        val bodyTemplate = template.bodyTemplates.random()
        
        val body = bodyTemplate.replace("{milestone}", milestone)

        return NotificationContent(
            title = title,
            body = body,
            data = mapOf(
                "milestone" to milestone,
                "type" to NotificationType.MILESTONE_CELEBRATION.name
            )
        )
    }

    private fun personalizeEngagementReminder(template: String, userData: UserEngagementData): String {
        val daysSinceLastOpen = kotlin.math.max(1, 
            (kotlinx.datetime.Clock.System.now() - userData.lastAppOpen).inWholeDays.toInt())
        
        val avgGoalProgress = if (userData.goalProgress.isNotEmpty()) {
            (userData.goalProgress.values.average() * 100).toInt()
        } else 50

        val topGoal = userData.goalProgress.maxByOrNull { it.value }?.key ?: "your main goal"
        val recentMilestone = userData.recentMilestones.firstOrNull() ?: "your recent progress"
        val microWinCount = userData.availableMicroWins.size

        return template
            .replace("{progress}", avgGoalProgress.toString())
            .replace("{goalName}", topGoal)
            .replace("{microWins}", microWinCount.toString())
            .replace("{milestone}", recentMilestone)
            .replace("{days}", daysSinceLastOpen.toString())
    }

    private fun personalizeGoalProgress(template: String, userData: UserEngagementData): String {
        val topGoal = userData.goalProgress.maxByOrNull { it.value }
        val goalName = topGoal?.key ?: "your goal"
        val progress = ((topGoal?.value ?: 0.0) * 100).toInt()

        return template
            .replace("{goalName}", goalName)
            .replace("{progress}", progress.toString())
            .replace("{amount}", "$${Random.nextInt(50, 500)}")
            .replace("{remaining}", "$${Random.nextInt(500, 2000)}")
    }

    private fun personalizeMilestone(template: String, userData: UserEngagementData): String {
        val milestone = userData.recentMilestones.firstOrNull() ?: "a new achievement"
        return template.replace("{milestone}", milestone)
    }

    private fun personalizeMicroWin(template: String, userData: UserEngagementData): String {
        val count = userData.availableMicroWins.size
        val points = Random.nextInt(5, 25)
        val action = userData.availableMicroWins.firstOrNull() ?: "check your balance"

        return template
            .replace("{count}", count.toString())
            .replace("{points}", points.toString())
            .replace("{action}", action)
    }

    private fun personalizeWeeklySummary(template: String, userData: UserEngagementData): String {
        val achievements = Random.nextInt(3, 8)
        val streaks = userData.currentStreaks.size
        val progress = Random.nextInt(5, 25)
        val points = Random.nextInt(100, 500)
        val topAchievement = userData.recentMilestones.firstOrNull() ?: "consistent progress"

        return template
            .replace("{achievements}", achievements.toString())
            .replace("{streaks}", streaks.toString())
            .replace("{progress}", progress.toString())
            .replace("{points}", points.toString())
            .replace("{topAchievement}", topAchievement)
            .replace("{summary}", "stayed consistent and made progress")
    }

    private fun formatStreakType(streakType: String): String {
        return when (streakType.lowercase()) {
            "daily_checkin" -> "daily check-in"
            "budget_tracking" -> "budget tracking"
            "goal_progress" -> "goal progress"
            "savings" -> "savings"
            else -> streakType.replace("_", " ")
        }
    }
}