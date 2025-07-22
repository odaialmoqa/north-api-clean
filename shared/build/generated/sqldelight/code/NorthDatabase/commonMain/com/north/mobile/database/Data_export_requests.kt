package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class Data_export_requests(
  public val id: String,
  public val user_id: String,
  public val format: String,
  public val requested_at: Long,
  public val status: String,
  public val completed_at: Long?,
  public val download_url: String?,
  public val expires_at: Long?,
  public val file_size: Long?,
  public val created_at: Long,
)
