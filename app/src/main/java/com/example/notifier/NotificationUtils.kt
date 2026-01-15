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

fun showNotification(context: Context) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "default_channel_id"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Default Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        action = NOTIFICATION_CLICK_ACTION
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        1, // Unique request code
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("Feel Rest")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(1, notification)
}

fun scheduleNextNotification(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        2, // Unique request code
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val fiveMinutesInMillis: Long = 5 * 60 * 1000

    val triggerAtMillis = System.currentTimeMillis() + fiveMinutesInMillis
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }
}
