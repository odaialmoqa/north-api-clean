package com.north.mobile.data.notification

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidPushNotificationProvider(
    private val context: Context,
    private val firebaseMessaging: FirebaseMessaging
) : PushNotificationProvider {

    override suspend fun sendNotification(
        userId: String,
        content: NotificationContent
    ): Result<NotificationDeliveryResult> {
        return try {
            // For local notifications, we'll use Android's NotificationManager
            // For remote notifications, this would typically go through your backend
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
        return try {
            firebaseMessaging.subscribeToTopic(topic).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unsubscribeFromTopic(userId: String, topic: String): Result<Unit> {
        return try {
            firebaseMessaging.unsubscribeFromTopic(topic).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDeviceToken(userId: String, token: String): Result<Unit> {
        return try {
            // Store the token for this user - typically sent to your backend
            // For now, we'll just validate the token format
            if (token.isNotBlank()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDeviceToken(): String {
        return suspendCoroutine { continuation ->
            firebaseMessaging.token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resume("")
                }
            }
        }
    }

    private fun showLocalNotification(content: NotificationContent): String {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) 
            as android.app.NotificationManager
        
        val channelId = "north_engagement"
        val notificationId = System.currentTimeMillis().toInt()
        
        // Create notification channel if needed (Android 8.0+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "North Engagement",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications to help you stay engaged with your financial goals"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app icon
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
        return notificationId.toString()
    }
}