package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class Audit_logs(
  public val id: String,
  public val user_id: String?,
  public val event_type: String,
  public val timestamp: Long,
  public val ip_address: String?,
  public val user_agent: String?,
  public val session_id: String?,
  public val details: String,
  public val result: String,
  public val risk_level: String,
  public val created_at: Long,
)
