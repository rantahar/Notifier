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
                // If the document doesn't exist, Firestore creates it on first write.
                // We can start with a null or empty log.
                _habitLog.value = null
                return@addSnapshotListener
            }
            _habitLog.value = snapshot?.toObject<HabitLog>()
        }
    }

    fun incrementSnackCount() {
        viewModelScope.launch {
            // Construct a nested map to safely update the nested field.
            // This is non-destructive and will not affect other fields in the 'habits' map.
            val update = mapOf("habits" to mapOf("snack" to FieldValue.increment(1)))
            getDocumentRef()?.set(update, com.google.firebase.firestore.SetOptions.merge())
        }
    }

    fun decrementSnackCount() {
        viewModelScope.launch {
            val currentCount = _habitLog.value?.habits?.snack ?: 0
            if (currentCount > 0) {
                // Use the same non-destructive nested map approach here.
                val update = mapOf("habits" to mapOf("snack" to FieldValue.increment(-1)))
                getDocumentRef()?.set(update, com.google.firebase.firestore.SetOptions.merge())
            }
        }
    }
}
