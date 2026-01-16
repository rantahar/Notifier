package com.example.notifier

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class HabitLog(
    // I'm using the 'snack' field from your example to store the 'Feel Rest' click count.
    val snack: Int = 0,
    @ServerTimestamp val timestamp: Date? = null // Automatically set by Firestore
)
