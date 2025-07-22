package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class StreakRecovery(
  public val id: String,
  public val userId: String,
  public val originalStreakId: String,
  public val streakType: String,
  public val brokenAt: Long,
  public val recoveryStarted: Long,
  public val recoveryCompleted: Long?,
  public val originalCount: Long,
  public val isSuccessful: Long,
  public val createdAt: Long,
  public val updatedAt: Long,
)
