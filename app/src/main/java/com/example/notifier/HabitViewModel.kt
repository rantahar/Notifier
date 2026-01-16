package com.example.notifier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HabitViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private val _habitLog = MutableStateFlow<HabitLog?>(null)
    val habitLog: StateFlow<HabitLog?> = _habitLog.asStateFlow()

    private fun getTodaysDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getDocumentRef() = auth.currentUser?.uid?.let { userId ->
        db.collection("users").document(userId)
            .collection("daily_logs").document(getTodaysDateString())
    }

    fun listenToTodaysLog() {
        getDocumentRef()?.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Handle error - for now, just assume a new log
                _habitLog.value = HabitLog()
                return@addSnapshotListener
            }
            _habitLog.value = snapshot?.toObject<HabitLog>() ?: HabitLog()
        }
    }

    fun incrementSnackCount() {
        viewModelScope.launch {
            // Use FieldValue to atomically increment the count on the server
            getDocumentRef()?.set(mapOf("snack" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge())
        }
    }

    fun decrementSnackCount() {
        viewModelScope.launch {
            val currentCount = _habitLog.value?.snack ?: 0
            if (currentCount > 0) {
                getDocumentRef()?.set(mapOf("snack" to FieldValue.increment(-1)), com.google.firebase.firestore.SetOptions.merge())
            }
        }
    }
}
