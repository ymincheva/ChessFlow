package com.chessflow.jni.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chessflow.jni.R
import com.chessflow.jni.repositories.PuzzleRepository
import com.chessflow.jni.utils.NetworkObserver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: PuzzleRepository,
    networkObserver: NetworkObserver
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _stats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val stats = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val user = auth.currentUser

    val isOnline = networkObserver.observe.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun loadStats() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _stats.value = repository.getUserStats(userId)
            } catch (e: Exception) {
                e.message?.let { Log.e("ChessFlow", it) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (Int) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid

        if (userId != null && user != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

            user.delete().addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {

                    userRef.removeValue().addOnCompleteListener { dbTask ->
                        if (dbTask.isSuccessful) {
                            onSuccess()
                        } else {
                            Log.e("ProfileVM", "Database cleanup failed: ${dbTask.exception?.message}")
                            onSuccess()
                        }
                    }

                } else {
                    val exception = authTask.exception
                    if (exception is FirebaseAuthRecentLoginRequiredException) {
                        onError(R.string.delete_account_security_error)
                    } else {
                        Log.e("ProfileVM", "Auth deletion failed: ${authTask.exception?.message}")
                        onError(R.string.error_deleting_auth)
                    }
                }
            }
        } else {
            onError(R.string.error_delete_user)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}