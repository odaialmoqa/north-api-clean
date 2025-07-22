package com.north.mobile.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.north.mobile.ui.dashboard.DashboardScreen
import com.north.mobile.ui.dashboard.model.DashboardState
import com.north.mobile.ui.goals.GoalManagementScreen
import com.north.mobile.ui.goals.model.GoalDashboardState
import com.north.mobile.ui.chat.ChatScreen
import com.north.mobile.ui.chat.model.ChatState
import com.north.mobile.ui.insights.SpendingInsightsScreen
import com.north.mobile.ui.insights.model.SpendingInsightsState
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Usability tests focused on reducing financial anxiety and improving user experience
 * Tests that the app provides clear, non-intimidating financial information and positive reinforcement
 */
class UsabilityTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun dashboard_presentsFinancialDataInNonIntimidatingWay() {
        composeTestRule.setContent {
            DashboardScreen(
                state = DashboardState(
                    netWorth = 47250.0,
                    netWorthChange = 1200.0,
                    accounts = listOf(
                        com.north.mobile.domain.model.Account(
                            id = "checking-1",
                            institutionId = "rbc",
                            accountType = com.north.mobile.domain.model.AccountType.CHECKING,
                            balance = com.north.mobile.domain.model.Money.fromDollars(2450.0),
                            name = "RBC Checking"
                        ),
                        com.north.mobile.domain.model.Account(
                            id = "savings-1",
                            institutionId = "tangerine",
                            accountType = com.north.mobile.domain.model.AccountType.SAVINGS,
                            balance = com.north.mobile.domain.model.Money.fromDollars(15800.0),
                            name = "Tangerine Savings"
                        )
                    )
                ),
                onRefresh = { },
                onAccountClick = { },
                onGoalClick = { },
                onInsightClick = { },
                onQuickActionClick = { }
            )
        }
        
        // Verify positive framing of financial information
        composeTestRule.onNodeWithText("Good morning, Alex! 👋").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net Worth").assertIsDisplayed()
        composeTestRule.onNodeWithText("$47,250 CAD").assertIsDisplayed()
        composeTestRule.onNodeWithText("↗️ +$1,200").assertIsDisplayed() // Positive change indicator
        
        // Verify encouraging micro-wins are highlighted
        composeTestRule.onNodeWithText("Today's Micro-Wins").assertIsDisplayed()
        composeTestRule.onNodeWithText("✅ Check balance (+10 pts)").assertIsDisplayed()
        composeTestRule.onNodeWithText("🔥 3-day saving streak!").assertIsDisplayed()
        
        // Verify positive insights are shown
        composeTestRule.onNodeWithText("Smart Insights").assertIsDisplayed()
        composeTestRule.onNodeWithText("You're spending 15% less on dining this month! 🎉").assertIsDisplayed()
    }
    
    @Test
    fun dashboard_handlesNegativeFinancialDataGently() {
        composeTestRule.setContent {
            DashboardScreen(
                state = DashboardState(
                    netWorth = 45000.0,
                    netWorthChange = -500.0, // Negative change
                    accounts = listOf(
                        com.north.mobile.domain.model.Account(
                            id = "checking-1",
                            institutionId = "rbc",
                            accountType = com.north.mobile.domain.model.AccountType.CHECKING,
                            balance = com.north.mobile.domain.model.Money.fromDollars(150.0), // Low balance
                            name = "RBC Checking"
                        )
                    )
                ),
                onRefresh = { },
                onAccountClick = { },
                onGoalClick = { },
                onInsightClick = { },
                onQuickActionClick = { }
            )
        }
        
        // Verify negative information is presented constructively
        composeTestRule.onNodeWithText("$45,000 CAD").assertIsDisplayed()
        
        // Should show gentle guidance instead of alarming messages
        composeTestRule.onNodeWithText("Let's work on building your balance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Small steps lead to big progress").assertIsDisplayed()
        
        // Should still show positive elements and encouragement
        composeTestRule.onNodeWithText("Today's Micro-Wins").assertIsDisplayed()
        composeTestRule.onNodeWithText("You checked your balance - that's progress! 💪").assertIsDisplayed()
    }
    
    @Test
    fun goalManagement_providesEncouragingProgressFeedback() {
        val testGoal = com.north.mobile.domain.model.FinancialGoal(
            id = "emergency-fund",
            userId = "user-1",
            title = "Emergency Fund",
            targetAmount = com.north.mobile.domain.model.Money.fromDollars(10000.0),
            currentAmount = com.north.mobile.domain.model.Money.fromDollars(8500.0),
            targetDate = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault()).plus(kotlinx.datetime.DatePeriod(months = 2)),
            priority = com.north.mobile.domain.model.Priority.HIGH,
            goalType = com.north.mobile.domain.model.GoalType.EMERGENCY_FUND,
            createdAt = kotlinx.datetime.Clock.System.now()
        )
        
        composeTestRule.setContent {
            GoalManagementScreen(
                state = GoalDashboardState(goals = listOf(testGoal)),
                onCreateGoal = { },
                onEditGoal = { },
                onDeleteGoal = { },
                onUpdateProgress = { _, _ -> },
                onGoalClick = { }
            )
        }
        
        // Verify encouraging progress messaging
        composeTestRule.onNodeWithText("🏠 Emergency Fund").assertIsDisplayed()
        composeTestRule.onNodeWithText("$8,500 / $10,000").assertIsDisplayed()
        composeTestRule.onNodeWithText("85%").assertIsDisplayed() // Clear progress percentage
        
        // Verify positive reinforcement
        composeTestRule.onNodeWithText("You're so close! 🎯").assertIsDisplayed()
        composeTestRule.onNodeWithText("Only $1,500 to go!").assertIsDisplayed()
        
        // Verify actionable guidance
        composeTestRule.onNodeWithText("$125/week to stay on track").assertIsDisplayed()
        
        // Verify celebration of progress
        composeTestRule.onNodeWithText("Amazing progress! You've saved 85% of your goal! 🌟").assertIsDisplayed()
    }
    
    @Test
    fun spendingInsights_presentsDataPositively() {
        composeTestRule.setContent {
            SpendingInsightsScreen(
                state = SpendingInsightsState(
                    totalSpent = 3245.0,
                    previousMonthSpent = 3045.0,
                    categories = listOf(
                        com.north.mobile.ui.insights.model.CategorySpending(
                            category = "Housing",
                            amount = 1200.0,
                            percentage = 37.0,
                            trend = "stable"
                        ),
                        com.north.mobile.ui.insights.model.CategorySpending(
                            category = "Food",
                            amount = 450.0,
                            percentage = 14.0,
                            trend = "down"
                        )
                    )
                ),
                onPeriodChange = { },
                onCategoryClick = { },
                onTransactionClick = { },
                onRecommendationClick = { }
            )
        }
        
        // Verify spending data is presented clearly
        composeTestRule.onNodeWithText("This Month: $3,245 spent").assertIsDisplayed()
        
        // Verify positive framing of changes
        composeTestRule.onNodeWithText("Great job!").assertIsDisplayed()
        composeTestRule.onNodeWithText("You spent 15% less on dining out this month").assertIsDisplayed()
        
        // Verify constructive suggestions
        composeTestRule.onNodeWithText("Potential Savings").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 unused subscriptions found").assertIsDisplayed()
        composeTestRule.onNodeWithText("Could save $47/month").assertIsDisplayed()
        
        // Verify actionable recommendations
        composeTestRule.onNodeWithText("Review Subscriptions").assertIsDisplayed()
    }
    
    @Test
    fun chatInterface_providesReassuringSupportiveResponses() {
        composeTestRule.setContent {
            ChatScreen(
                state = ChatState(
                    messages = listOf(
                        com.north.mobile.ui.chat.model.ChatMessage(
                            id = "1",
                            content = "Can I afford a $400 weekend trip to Montreal next month?",
                            type = com.north.mobile.ui.chat.model.MessageType.USER
                        ),
                        com.north.mobile.ui.chat.model.ChatMessage(
                            id = "2",
                            content = "Based on your spending patterns and current savings rate, yes! 🎉\n\nHere's what I found:\n• Your entertainment budget has $180 available\n• You're $125 ahead on your emergency fund goal\n• Your dining out spending is down 15% this month\n\nRecommendation: Book it! This won't impact your financial goals.",
                            type = com.north.mobile.ui.chat.model.MessageType.AI
                        )
                    )
                ),
                onSendMessage = { },
                onQuickQuestionClick = { },
                onClearChat = { }
            )
        }
        
        // Verify supportive AI responses
        composeTestRule.onNodeWithText("Based on your spending patterns and current savings rate, yes! 🎉").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recommendation: Book it! This won't impact your financial goals.").assertIsDisplayed()
        
        // Verify clear explanations
        composeTestRule.onNodeWithText("Your entertainment budget has $180 available").assertIsDisplayed()
        composeTestRule.onNodeWithText("You're $125 ahead on your emergency fund goal").assertIsDisplayed()
        
        // Verify encouraging tone
        composeTestRule.onNodeWithText("Hi Alex! I'm your personal CFO. I've analyzed your finances and I'm here to help.").assertIsDisplayed()
    }
    
    @Test
    fun errorStates_provideReassuranceAndClearNextSteps() {
        composeTestRule.setContent {
            DashboardScreen(
                state = DashboardState(
                    error = "We're having trouble connecting to your bank. This happens sometimes and is usually temporary."
                ),
                onRefresh = { },
                onAccountClick = { },
                onGoalClick = { },
                onInsightClick = { },
                onQuickActionClick = { }
            )
        }
        
        // Verify error messages are reassuring, not alarming
        composeTestRule.onNodeWithText("We're having trouble connecting to your bank. This happens sometimes and is usually temporary.").assertIsDisplayed()
        
        // Verify clear next steps
        composeTestRule.onNodeWithText("Try Again").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your data is safe and secure").assertIsDisplayed()
        
        // Verify no panic-inducing language
        composeTestRule.onNodeWithText("ERROR").assertDoesNotExist()
        composeTestRule.onNodeWithText("FAILED").assertDoesNotExist()
        composeTestRule.onNodeWithText("CRITICAL").assertDoesNotExist()
    }
    
    @Test
    fun onboarding_reducesAnxietyAboutFinancialDataSharing() {
        composeTestRule.setContent {
            com.north.mobile.ui.onboarding.OnboardingScreen(
                onboardingState = com.north.mobile.ui.onboarding.model.OnboardingState(
                    currentStep = com.north.mobile.ui.onboarding.model.OnboardingStep.ACCOUNT_LINKING
                ),
                onNextStep = { },
                onPreviousStep = { },
                onSkipStep = { },
                onCompleteOnboarding = { },
                onSetupBiometric = { },
                onSetupPIN = { },
                onLinkAccount = { },
                onCreateGoal = { }
            )
        }
        
        // Verify clear security messaging
        composeTestRule.onNodeWithText("🔒 Bank-level security guaranteed").assertIsDisplayed()
        composeTestRule.onNodeWithText("✅ We only access account balances and transaction history").assertIsDisplayed()
        composeTestRule.onNodeWithText("✅ We never store your login credentials").assertIsDisplayed()
        composeTestRule.onNodeWithText("✅ You can disconnect anytime").assertIsDisplayed()
        composeTestRule.onNodeWithText("✅ Read-only access only").assertIsDisplayed()
        
        // Verify privacy reassurance
        composeTestRule.onNodeWithText("Your data stays in Canada and follows PIPEDA privacy laws").assertIsDisplayed()
        
        // Verify clear action buttons
        composeTestRule.onNodeWithText("Continue Securely").assertIsDisplayed()
        composeTestRule.onNodeWithText("Learn more about security").assertIsDisplayed()
    }
    
    @Test
    fun gamification_providesPositiveReinforcementWithoutPressure() {
        composeTestRule.setContent {
            com.north.mobile.ui.gamification.GamificationScreen(
                state = com.north.mobile.ui.gamification.model.GamificationState(
                    level = 7,
                    currentPoints = 1250,
                    pointsToNextLevel = 250,
                    currentStreaks = listOf(
                        com.north.mobile.domain.model.Streak(
                            id = "daily-checkin",
                            type = com.north.mobile.domain.model.StreakType.DAILY_CHECK_IN,
                            currentCount = 12,
                            bestCount = 15,
                            lastActivityDate = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
                        )
                    ),
                    recentAchievements = listOf(
                        com.north.mobile.domain.model.Achievement(
                            id = "savings-superstar",
                            title = "Savings Superstar",
                            description = "Saved $500 this month",
                            icon = "💎",
                            unlockedAt = kotlinx.datetime.Clock.System.now()
                        )
                    )
                ),
                onQuickWinClick = { },
                onAchievementClick = { },
                onStreakClick = { }
            )
        }
        
        // Verify positive reinforcement
        composeTestRule.onNodeWithText("Level 7 - Money Master 💰").assertIsDisplayed()
        composeTestRule.onNodeWithText("🔥 Current Streaks").assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily Check-in 🔥 12 days").assertIsDisplayed()
        
        // Verify achievements celebrate progress
        composeTestRule.onNodeWithText("🏅 Recent Achievements").assertIsDisplayed()
        composeTestRule.onNodeWithText("💎 Savings Superstar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saved $500 this month").assertIsDisplayed()
        
        // Verify optional quick wins (no pressure)
        composeTestRule.onNodeWithText("⚡ Quick Wins Available").assertIsDisplayed()
        composeTestRule.onNodeWithText("Categorize 3 transactions (+15)").assertIsDisplayed()
        
        // Verify no guilt or pressure language
        composeTestRule.onNodeWithText("MUST").assertDoesNotExist()
        composeTestRule.onNodeWithText("REQUIRED").assertDoesNotExist()
        composeTestRule.onNodeWithText("BEHIND").assertDoesNotExist()
    }
    
    @Test
    fun loadingStates_provideReassuranceAndContext() {
        composeTestRule.setContent {
            DashboardScreen(
                state = DashboardState(isLoading = true),
                onRefresh = { },
                onAccountClick = { },
                onGoalClick = { },
                onInsightClick = { },
                onQuickActionClick = { }
            )
        }
        
        // Verify loading messages are reassuring
        composeTestRule.onNodeWithText("Loading your financial data").assertIsDisplayed()
        composeTestRule.onNodeWithText("Securely connecting to your accounts").assertIsDisplayed()
        composeTestRule.onNodeWithText("This usually takes just a moment").assertIsDisplayed()
        
        // Verify progress indication
        composeTestRule.onNodeWithContentDescription("Loading progress").assertIsDisplayed()
        
        // Verify no anxiety-inducing language
        composeTestRule.onNodeWithText("PROCESSING").assertDoesNotExist()
        composeTestRule.onNodeWithText("WAIT").assertDoesNotExist()
    }
    
    @Test
    fun helpAndSupport_isEasilyAccessible() {
        composeTestRule.setContent {
            DashboardScreen(
                state = DashboardState(),
                onRefresh = { },
                onAccountClick = { },
                onGoalClick = { },
                onInsightClick = { },
                onQuickActionClick = { }
            )
        }
        
        // Verify help is easily accessible
        composeTestRule.onNodeWithContentDescription("Help and support").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Chat with North AI for help").assertIsDisplayed()
        
        // Verify contextual help hints
        composeTestRule.onNodeWithContentDescription("Tap for more information about net worth").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Learn about account security").assertIsDisplayed()
    }
    
    @Test
    fun financialTerms_areExplainedInSimpleLanguage() {
        composeTestRule.setContent {
            ChatScreen(
                state = ChatState(
                    messages = listOf(
                        com.north.mobile.ui.chat.model.ChatMessage(
                            id = "1",
                            content = "What's my net worth?",
                            type = com.north.mobile.ui.chat.model.MessageType.USER
                        ),
                        com.north.mobile.ui.chat.model.ChatMessage(
                            id = "2",
                            content = "Your net worth is $47,250! 🎉\n\nNet worth is simply what you own minus what you owe. Think of it as your financial scorecard.\n\nYour assets (what you own): $52,750\n• Checking: $2,450\n• Savings: $15,800\n• Investments: $34,500\n\nYour debts (what you owe): $5,500\n• Credit card: $1,200\n• Student loan: $4,300\n\nNet worth = $52,750 - $5,500 = $47,250\n\nYou're doing great! Your net worth has grown by $1,200 this month.",
                            type = com.north.mobile.ui.chat.model.MessageType.AI
                        )
                    )
                ),
                onSendMessage = { },
                onQuickQuestionClick = { },
                onClearChat = { }
            )
        }
        
        // Verify simple explanations
        composeTestRule.onNodeWithText("Net worth is simply what you own minus what you owe. Think of it as your financial scorecard.").assertIsDisplayed()
        
        // Verify clear breakdown
        composeTestRule.onNodeWithText("Your assets (what you own): $52,750").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your debts (what you owe): $5,500").assertIsDisplayed()
        
        // Verify encouraging context
        composeTestRule.onNodeWithText("You're doing great! Your net worth has grown by $1,200 this month.").assertIsDisplayed()
    }
}