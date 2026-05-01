package com.example.crowdalert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Application
import android.content.Context
import android.os.Build
import com.example.crowdalert.fcm.CrowdAlertMessagingService
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Hilt: generates the app-wide component and enables
 * [dagger.hilt.android.AndroidEntryPoint] injection in activities and services.
 */
@HiltAndroidApp
class CrowdAlertApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        subscribeToDemoAlertsTopic()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel =
            NotificationChannel(
                CrowdAlertMessagingService.CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun subscribeToDemoAlertsTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic(CrowdAlertMessagingService.DEMO_TOPIC)
    }
}
