package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class Consent_preferences(
  public val user_id: String,
  public val marketing_opt_in: Long,
  public val analytics_opt_in: Long,
  public val data_retention_period: String,
  public val updated_at: Long,
)
