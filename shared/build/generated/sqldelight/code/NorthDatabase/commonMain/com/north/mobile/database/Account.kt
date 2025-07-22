package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class Account(
  public val id: String,
  public val userId: String,
  public val institutionId: String,
  public val institutionName: String,
  public val accountType: String,
  public val balance: Long,
  public val availableBalance: Long?,
  public val currency: String,
  public val lastUpdated: Long,
  public val accountNumber: String?,
  public val transitNumber: String?,
  public val institutionNumber: String?,
  public val nickname: String?,
  public val isActive: Long,
  public val createdAt: Long,
  public val updatedAt: Long,
)
