package com.north.mobile.ui.onboarding

/**
 * Represents a step in the onboarding process
 */
enum class OnboardingStep {
    WELCOME,
    FINANCIAL_GOALS,
    SPENDING_HABITS,
    INCOME_SOURCES,
    ACCOUNT_CONNECTION,
    NOTIFICATION_PREFERENCES,
    COMPLETED
}

/**
 * Data class representing onboarding step completion status
 */
data class OnboardingStatus(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val isCompleted: Boolean = false,
    val skipped: Boolean = false
)
