package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class MicroTask(
  public val id: String,
  public val goalId: String,
  public val title: String,
  public val description: String,
  public val targetAmount: Long,
  public val isCompleted: Long,
  public val dueDate: Long?,
  public val completedAt: Long?,
  public val createdAt: Long,
)
