package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class Streak(
  public val id: String,
  public val userId: String,
  public val type: String,
  public val currentCount: Long,
  public val bestCount: Long,
  public val lastUpdated: Long,
  public val isActive: Long,
  public val riskLevel: String,
  public val recoveryAttempts: Long,
  public val lastReminderSent: Long?,
  public val createdAt: Long,
  public val updatedAt: Long,
)
