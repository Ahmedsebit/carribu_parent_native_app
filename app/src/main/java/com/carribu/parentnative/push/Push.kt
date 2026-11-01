package com.carribu.parentnative.push

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.carribu.parentnative.CarribuApplication
import com.carribu.parentnative.MainActivity
import com.carribu.parentnative.R
import com.carribu.parentnative.data.repository.ParentRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object PushTokenRegistrar {
    suspend fun registerIfAvailable(context: Context, repository: ParentRepository): Boolean {
        if (FirebaseApp.getApps(context).isEmpty()) return false
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            repository.savePushToken(token)
            true
        } catch (error: Exception) {
            Log.w("CarribuPush", "FCM token registration is unavailable; it will be retried later.", error)
            false
        }
    }
}

class CarribuFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val app = application as CarribuApplication
        app.applicationScope.launch {
            app.container.parentRepository.savePushToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Carribu"
        val body = message.notification?.body ?: message.data["message"] ?: message.data["body"] ?: return
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CarribuApplication.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
        getSystemService(NotificationManager::class.java)
            .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}
