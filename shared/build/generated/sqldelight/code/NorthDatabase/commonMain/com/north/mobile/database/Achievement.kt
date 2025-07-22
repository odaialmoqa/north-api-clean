package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class Achievement(
  public val id: String,
  public val userId: String,
  public val achievementType: String,
  public val title: String,
  public val description: String,
  public val badgeIcon: String,
  public val category: String,
  public val unlockedAt: Long,
  public val createdAt: Long,
)
