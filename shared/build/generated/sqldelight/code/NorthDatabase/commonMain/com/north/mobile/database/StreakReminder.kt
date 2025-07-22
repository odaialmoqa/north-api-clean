package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class StreakReminder(
  public val id: String,
  public val userId: String,
  public val streakId: String,
  public val streakType: String,
  public val reminderType: String,
  public val message: String,
  public val scheduledFor: Long,
  public val sentAt: Long?,
  public val isRead: Long,
  public val createdAt: Long,
)
