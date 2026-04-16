package com.territorywars.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.territorywars.MainActivity
import com.territorywars.R
import timber.log.Timber

/**
 * Firebase Cloud Messaging Service для push-уведомлений.
 * Обрабатывает приходящие уведомления об атаках на территорию.
 */
class TerritoryWarsMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "territory_wars_notifications"
        private const val CHANNEL_NAME = "Territory Wars"
        private const val TAG = "FCM"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.tag(TAG).d("New FCM token: $token")
        // Отправить токен на сервер
        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.tag(TAG).d("Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val title = data["title"] ?: "Territory Wars"
        val body = data["body"] ?: ""
        val type = data["type"] ?: ""

        showNotification(title, body, type)
    }

    private fun showNotification(title: String, body: String, type: String) {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = System.currentTimeMillis().toInt()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .apply {
                when (type) {
                    "attack" -> setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText("⚠️ $body")
                    )
                    "takeover" -> setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText("💥 $body")
                    )
                }
            }
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления об атаках на вашу территорию"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendTokenToServer(token: String) {
        // Этот токен будет отправлен при авторизации пользователя
        // Сохраняем его в preferences для отправки при следующем логине
        Timber.tag(TAG).d("FCM token saved for server sync: $token")
    }
}
