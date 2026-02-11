package com.example.notifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * This receiver is triggered when the user explicitly dismisses the notification (e.g., by swiping it away).
 * Its sole purpose is to ensure the alarm chain is not broken by this action.
 */
class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // The user dismissed the notification, so we schedule the next one immediately.
        scheduleNextNotification(context)
    }
}
