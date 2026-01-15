package com.example.notifier

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.notifier.ui.theme.NotifierTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var clickCount by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotifierTheme {
                MainScreen(
                    clickCount = clickCount,
                    onIncrement = {
                        // Immediately cancel the currently showing notification
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(NOTIFICATION_ID)
                        // Then update the count and schedule the next one
                        manualUpdateCount(1)
                        scheduleNextNotification(this)
                    },
                    onDecrement = { manualUpdateCount(-1) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == NOTIFICATION_CLICK_ACTION) {
            updateDailyClickCount()
            scheduleNextNotification(this)
        }
    }

    override fun onResume() {
        super.onResume()
        updateClickCount()
    }

    private fun updateClickCount() {
        val prefs = getSharedPreferences("NotifierPrefs", Context.MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        val lastClickDate = prefs.getString("lastClickDate", null)

        clickCount = if (today == lastClickDate) {
            prefs.getInt("clickCount", 0)
        } else {
            0
        }
    }

    private fun manualUpdateCount(delta: Int) {
        val prefs = getSharedPreferences("NotifierPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())

        val lastClickDate = prefs.getString("lastClickDate", null)
        if (today != lastClickDate) {
            editor.putString("lastClickDate", today)
        }

        val currentClickCount = prefs.getInt("clickCount", 0)
        val newCount = (currentClickCount + delta).coerceAtLeast(0)
        editor.putInt("clickCount", newCount)
        editor.apply()
        updateClickCount()
    }

    private fun updateDailyClickCount() {
        val prefs = getSharedPreferences("NotifierPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())

        val lastClickDate = prefs.getString("lastClickDate", null)
        var currentClickCount = prefs.getInt("clickCount", 0)

        if (today == lastClickDate) {
            currentClickCount++
        } else {
            currentClickCount = 1
        }

        editor.putString("lastClickDate", today)
        editor.putInt("clickCount", currentClickCount)
        editor.apply()
        updateClickCount()
    }
}

@Composable
fun MainScreen(
    clickCount: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val context = LocalContext.current
    var showAlarmPermissionRationale by remember { mutableStateOf(false) }
    var showBatteryOptimizationRationale by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* The new DisposableEffect will handle re-checking on resume */ }
    )
    
    fun checkPermissions() {
        // 1. Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        // 2. Exact Alarm Permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showAlarmPermissionRationale = true
                return
            }
        }

        // 3. Battery Optimization
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            showBatteryOptimizationRationale = true
            return
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showAlarmPermissionRationale) {
        PermissionRationaleDialog(
            title = "Permission Required",
            text = "To ensure timely reminders, this app needs permission to schedule alarms. Please grant this permission in the upcoming settings screen.",
            onConfirm = {
                showAlarmPermissionRationale = false
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            onDismiss = { showAlarmPermissionRationale = false }
        )
    }

    if (showBatteryOptimizationRationale) {
        PermissionRationaleDialog(
            title = "Battery Optimization",
            text = "To ensure the reminders work reliably, please set battery usage to \"Unrestricted\" for this app in the next screen.",
            onConfirm = {
                showBatteryOptimizationRationale = false
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            onDismiss = { showBatteryOptimizationRationale = false }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Daily Clicks: $clickCount",
                modifier = Modifier.padding(16.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onDecrement) { Text("-") }
                Button(onClick = onIncrement) { Text("+") }
            }
        }
    }
}

@Composable
fun PermissionRationaleDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Go to Settings")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
