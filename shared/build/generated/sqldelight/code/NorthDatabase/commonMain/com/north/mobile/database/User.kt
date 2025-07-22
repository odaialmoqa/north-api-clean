package com.north.mobile.database

import kotlin.Long
import kotlin.String

public data class User(
  public val id: String,
  public val email: String,
  public val firstName: String,
  public val lastName: String,
  public val phoneNumber: String?,
  public val dateOfBirth: String?,
  public val currency: String,
  public val language: String,
  public val notificationsEnabled: Long,
  public val biometricAuthEnabled: Long,
  public val createdAt: Long,
  public val updatedAt: Long,
)
