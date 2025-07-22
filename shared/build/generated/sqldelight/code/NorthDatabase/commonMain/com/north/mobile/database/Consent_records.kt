package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class Consent_records(
  public val id: String,
  public val user_id: String,
  public val purpose: String,
  public val granted: Long,
  public val timestamp: Long,
  public val ip_address: String?,
  public val user_agent: String?,
  public val version: String,
  public val expiry_date: Long?,
  public val created_at: Long,
)
