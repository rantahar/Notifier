package com.example.notifier

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DndModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val isDndOff = notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL

            if (isDndOff) {
                // DND was just turned off, show a notification immediately
                // and schedule the next one.
                showNotification(context)
                scheduleNextNotification(context)
            }
        }
    }
}
