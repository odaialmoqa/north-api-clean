package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class PointsHistory(
  public val id: String,
  public val userId: String,
  public val points: Long,
  public val action: String,
  public val description: String?,
  public val earnedAt: Long,
)
