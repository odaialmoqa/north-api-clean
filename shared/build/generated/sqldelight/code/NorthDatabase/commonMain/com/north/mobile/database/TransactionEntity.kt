package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class TransactionEntity(
  public val id: String,
  public val accountId: String,
  public val amount: Long,
  public val description: String,
  public val category: String?,
  public val subcategory: String?,
  public val date: Long,
  public val isRecurring: Long,
  public val merchantName: String?,
  public val location: String?,
  public val isVerified: Long,
  public val notes: String?,
  public val createdAt: Long,
  public val updatedAt: Long,
)
