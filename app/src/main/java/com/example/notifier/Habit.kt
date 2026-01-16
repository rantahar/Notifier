package com.example.notifier

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// This class represents the nested 'habits' object in your Firestore document
data class Habits(
    val snack: Int = 0
)

data class HabitLog(
    // This field now corresponds to the 'habits' map in Firestore
    val habits: Habits = Habits(),
    @ServerTimestamp val timestamp: Date? = null // Automatically set by Firestore
)
