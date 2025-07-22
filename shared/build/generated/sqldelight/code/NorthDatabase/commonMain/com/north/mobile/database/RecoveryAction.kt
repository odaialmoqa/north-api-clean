package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class RecoveryAction(
  public val id: String,
  public val recoveryId: String,
  public val actionType: String,
  public val completedAt: Long,
  public val pointsAwarded: Long,
  public val description: String,
  public val createdAt: Long,
)
