package com.north.mobile.database

import kotlin.ByteArray
import kotlin.Long
import kotlin.String

public data class Export_data(
  public val export_id: String,
  public val data_: ByteArray,
  public val created_at: Long,
)
