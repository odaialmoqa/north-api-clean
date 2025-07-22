package com.north.mobile.data.notification

import kotlinx.datetime.Clock
import platform.UserNotifications.*
import platform.Foundation.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IOSPushNotificationProvider : PushNotificationProvider {

    override suspend fun sendNotification(
        userId: String,
        content: NotificationContent
    ): Result<NotificationDeliveryResult> {
        return try {
            val notificationId = showLocalNotification(content)
            
            Result.success(NotificationDeliveryResult(
                notificationId = notificationId,
                success = true,
                deliveredAt = Clock.System.now()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun subscribeToTopic(userId: String, topic: String): Result<Unit> {
        // iOS doesn't have direct topic subscription like FCM
        // This would typically be handled by your backend service
        return Result.success(Unit)
    }

    override suspend fun unsubscribeFromTopic(userId: String, topic: String): Result<Unit> {
        // iOS doesn't have direct topic unsubscription like FCM
        // This would typically be handled by your backend service
        return Result.success(Unit)
    }

    override suspend fun updateDeviceToken(userId: String, token: String): Result<Unit> {
        return try {
            // Store the APNS token for this user - typically sent to your backend
            if (token.isNotBlank()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestNotificationPermission(): Boolean {
        return suspendCoroutine { continuation ->
            val center = UNUserNotificationCenter.currentNotificationCenter()
            center.requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            ) { granted, error ->
                continuation.resume(granted && error == null)
            }
        }
    }

    private suspend fun showLocalNotification(content: NotificationContent): String {
        return suspendCoroutine { continuation ->
            val notificationContent = UNMutableNotificationContent().apply {
                setTitle(content.title)
                setBody(content.body)
                setSound(UNNotificationSound.defaultSound)
                setBadge(NSNumber.numberWithInt(1))
                
                // Add custom data
                if (content.data.isNotEmpty()) {
                    setUserInfo(content.data.mapKeys { it.key as Any }.mapValues { it.value as Any })
                }
            }

            val notificationId = "north_${System.currentTimeMillis()}"
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(0.1, false)
            val request = UNNotificationRequest.requestWithIdentifier(
                notificationId,
                notificationContent,
                trigger
            )

            val center = UNUserNotificationCenter.currentNotificationCenter()
            center.addNotificationRequest(request) { error ->
                if (error == null) {
                    continuation.resume(notificationId)
                } else {
                    continuation.resume("")
                }
            }
        }
    }

    suspend fun getDeviceToken(): String {
        // This would typically be obtained from your iOS app delegate
        // when registering for remote notifications
        return ""
    }
}