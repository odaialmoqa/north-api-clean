package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class FinancialGoal(
  public val id: String,
  public val userId: String,
  public val title: String,
  public val description: String?,
  public val targetAmount: Long,
  public val currentAmount: Long,
  public val currency: String,
  public val targetDate: Long,
  public val priority: Long,
  public val category: String,
  public val isActive: Long,
  public val createdAt: Long,
  public val updatedAt: Long,
)
