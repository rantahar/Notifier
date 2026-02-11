package com.example.notifier

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

const val NOTIFICATION_ID = 1
const val NOTIFICATION_CLICK_ACTION = "com.example.notifier.NOTIFICATION_CLICK"
private const val ALARM_REQUEST_CODE = 100
private const val NOTIFICATION_CLICK_REQUEST_CODE = 101
private const val NOTIFICATION_DISMISS_REQUEST_CODE = 102

fun showNotification(context: Context) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Using a new channel ID to force the system to apply new settings.
    val channelId = "reminder_channel_v3"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Habit Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows reminders to log your habits."
            // Make the notification silent
            enableVibration(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    // Intent to open the app and increment the count on tap.
    // Removing incorrect flags to ensure onNewIntent is called reliably.
    val clickIntent = Intent(context, MainActivity::class.java).apply {
        action = NOTIFICATION_CLICK_ACTION
    }
    val clickPendingIntent = PendingIntent.getActivity(
        context,
        NOTIFICATION_CLICK_REQUEST_CODE,
        clickIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    // Intent to reschedule the alarm if the user dismisses the notification
    val dismissIntent = Intent(context, NotificationDismissReceiver::class.java)
    val dismissPendingIntent = PendingIntent.getBroadcast(
        context,
        NOTIFICATION_DISMISS_REQUEST_CODE,
        dismissIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("See Back")
        .setContentText("")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentIntent(clickPendingIntent)
        .setDeleteIntent(dismissPendingIntent) // This is the crucial part for handling dismissals
        .setAutoCancel(true) // Notification dismisses on tap
        .build()

    notificationManager.notify(NOTIFICATION_ID, notification)
}

fun scheduleNextNotification(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    
    // Always cancel any previously scheduled alarm before setting a new one.
    alarmManager.cancel(pendingIntent)

    // Set to 5 seconds for testing
    val delayInMillis: Long = 5 * 60 * 1000
    val triggerAtMillis = System.currentTimeMillis() + delayInMillis

    // Use setExactAndAllowWhileIdle for precision
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
}
