package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class GamificationProfile(
  public val userId: String,
  public val level: Long,
  public val totalPoints: Long,
  public val lastActivity: Long?,
  public val createdAt: Long,
  public val updatedAt: Long,
)
