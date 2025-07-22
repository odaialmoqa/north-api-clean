package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class Data_deletion_requests(
  public val id: String,
  public val user_id: String,
  public val data_types: String,
  public val reason: String?,
  public val requested_at: Long,
  public val status: String,
  public val scheduled_for: Long,
  public val completed_at: Long?,
  public val grace_period_ends: Long,
  public val verification_required: Long,
  public val created_at: Long,
)
