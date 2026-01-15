package com.example.notifier

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

const val NOTIFICATION_CLICK_ACTION = "com.example.notifier.NOTIFICATION_CLICK"
const val NOTIFICATION_ID = 1 // Use a constant ID for the notification

fun showNotification(context: Context) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancelAll()

    val channelId = "silent_reminder_channel_2"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableVibration(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        action = NOTIFICATION_CLICK_ACTION
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        3, // A unique request code for the PendingIntent
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("Feel Rest")
        .setContentText("Feel rest")
        .setStyle(NotificationCompat.BigTextStyle().bigText("Feel Rest"))
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setGroup(null)
        .setGroupSummary(false)
        .build()

    notificationManager.notify(NOTIFICATION_ID, notification)
}

fun scheduleNextNotification(context: Context) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        2, // A unique request code that is CONSISTENT for this alarm
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    // Always cancel the previous alarm before setting a new one. This makes the operation idempotent.
    notificationManager.cancelAll()
    alarmManager.cancelAll()

    // Set to 5 seconds for testing.
    val delayInMillis: Long = 5 * 60 * 1000

    val triggerAtMillis = System.currentTimeMillis() + delayInMillis
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }
}
