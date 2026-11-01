package com.carribu.parentnative

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.carribu.parentnative.push.PushTokenRegistrar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CarribuApplication : Application() {
    lateinit var container: AppContainer
        private set
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
        applicationScope.launch {
            container.sessionRepository.session.filterNotNull().first()
            PushTokenRegistrar.registerIfAvailable(this@CarribuApplication, container.parentRepository)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Carribu updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Bus, school, and message alerts"
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "carribu_updates"
    }
}
