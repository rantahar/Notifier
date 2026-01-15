package com.example.notifier

import android.Manifest
import android.app.AlarmManager
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
                MainScreen(clickCount = clickCount)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == NOTIFICATION_CLICK_ACTION) {
            updateDailyClickCount()
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // The MainScreen will handle showing the rationale when the app is resumed
            } else {
                scheduleNextNotification(this)
            }
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
fun MainScreen(clickCount: Int) {
    val context = LocalContext.current
    var showAlarmPermissionRationale by remember { mutableStateOf(false) }
    var showBatteryOptimizationRationale by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // The LaunchedEffect will re-run on the next composition
            }
        }
    )

    // This effect runs on first launch and whenever the app is resumed
    LaunchedEffect(Unit) {
        // 1. Check for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@LaunchedEffect // Wait for user's response
            }
        }

        // 2. Check for exact alarm permission (Android 12+)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                showAlarmPermissionRationale = true
                return@LaunchedEffect // Show rationale and wait for user to act
            }
        }

        // 3. Check for battery optimization
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            showBatteryOptimizationRationale = true
            return@LaunchedEffect // Show rationale and wait for user to act
        }

        // 4. If all permissions are granted, schedule the first notification
        scheduleNextNotification(context)
    }

    if (showAlarmPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showAlarmPermissionRationale = false },
            title = { Text("Permission Required") },
            text = { Text("To ensure timely reminders, this app needs permission to schedule alarms. Please grant this permission in the upcoming settings screen.") },
            confirmButton = {
                Button(
                    onClick = {
                        showAlarmPermissionRationale = false
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                Button(onClick = { showAlarmPermissionRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBatteryOptimizationRationale) {
        AlertDialog(
            onDismissRequest = { showBatteryOptimizationRationale = false },
            title = { Text("Battery Optimization") },
            text = { Text("To ensure the reminders work reliably, please set battery usage to \"Unrestricted\" for this app in the next screen.") },
            confirmButton = {
                Button(
                    onClick = {
                        showBatteryOptimizationRationale = false
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                Button(onClick = { showBatteryOptimizationRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Daily Clicks: $clickCount",
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
        )
    }
}
