package com.north.mobile.domain.model

import com.north.mobile.domain.validation.ValidationResult
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserTest {
    
    private fun createValidUser() = User(
        id = "user123",
        email = "test@example.com",
        profile = UserProfile(
            firstName = "John",
            lastName = "Doe",
            phoneNumber = "416-555-1234",
            dateOfBirth = LocalDate(1990, 1, 1),
            postalCode = "K1A 0A6"
        ),
        preferences = UserPreferences(),
        gamificationData = GamificationProfile()
    )
    
    @Test
    fun testValidUser() {
        val user = createValidUser()
        val validation = user.validate()
        assertTrue(validation.isValid)
    }
    
    @Test
    fun testInvalidUserEmail() {
        val user = createValidUser().copy(email = "invalid-email")
        val validation = user.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("email") })
    }
    
    @Test
    fun testBlankUserId() {
        val user = createValidUser().copy(id = "")
        val validation = user.validate()
        assertTrue(validation.isInvalid)
        assertTrue((validation as ValidationResult.Invalid).errors.any { it.contains("User ID") })
    }
    
    @Test
    fun testUserProfileValidation() {
        val profile = UserProfile(
            firstName = "John",
            lastName = "Doe",
            phoneNumber = "416-555-1234",
            postalCode = "K1A 0A6"
        )
        assertTrue(profile.validate().isValid)
        
        // Test invalid phone number
        val invalidProfile = profile.copy(phoneNumber = "123-456-789")
        assertTrue(invalidProfile.validate().isInvalid)
        
        // Test invalid postal code
        val invalidPostal = profile.copy(postalCode = "INVALID")
        assertTrue(invalidPostal.validate().isInvalid)
        
        // Test short first name
        val shortName = profile.copy(firstName = "J")
        assertTrue(shortName.validate().isInvalid)
    }
    
    @Test
    fun testUserProfileDisplayProperties() {
        val profile = UserProfile(
            firstName = "John",
            lastName = "Doe"
        )
        assertEquals("John Doe", profile.fullName)
        assertEquals("John", profile.displayName)
    }
    
    @Test
    fun testUserPreferencesValidation() {
        val preferences = UserPreferences()
        assertTrue(preferences.validate().isValid)
        
        // Test invalid language code
        val invalidPrefs = preferences.copy(language = "invalid")
        assertTrue(invalidPrefs.validate().isInvalid)
    }
    
    @Test
    fun testGamificationProfileValidation() {
        val profile = GamificationProfile()
        assertTrue(profile.validate().isValid)
        
        // Test invalid level
        val invalidLevel = profile.copy(level = 0)
        assertTrue(invalidLevel.validate().isInvalid)
        
        // Test negative points
        val negativePoints = profile.copy(totalPoints = -100)
        assertTrue(negativePoints.validate().isInvalid)
    }
    
    @Test
    fun testGamificationProfileProgression() {
        val profile = GamificationProfile(level = 3, totalPoints = 2500)
        assertEquals(3000, profile.nextLevelPoints)
        assertEquals(0.5, profile.progressToNextLevel) // 500 points into level 3 (out of 1000)
    }
    
    @Test
    fun testStreakTypes() {
        val streak = Streak(
            id = "streak1",
            type = StreakType.DAILY_CHECK_IN,
            currentCount = 5,
            bestCount = 10,
            lastUpdated = LocalDate(2024, 1, 1)
        )
        assertEquals(StreakType.DAILY_CHECK_IN, streak.type)
    }
    
    @Test
    fun testAchievementCategories() {
        val achievement = Achievement(
            id = "achievement1",
            title = "First Save",
            description = "Made your first savings contribution",
            badgeIcon = "💰",
            unlockedAt = kotlinx.datetime.Clock.System.now(),
            category = AchievementCategory.SAVINGS
        )
        assertEquals(AchievementCategory.SAVINGS, achievement.category)
    }
}