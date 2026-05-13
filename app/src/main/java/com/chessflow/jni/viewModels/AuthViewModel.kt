package com.chessflow.jni.viewModels

import androidx.lifecycle.ViewModel
import com.chessflow.jni.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user = _user.asStateFlow()

    fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    firebaseUser?.let {
                        _user.value = it
                        saveUserToDatabase(it)
                    }
                }
            }
    }

    fun logout(context: android.content.Context) {
        auth.signOut()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .build()

        val googleSignInClient = GoogleSignIn.getClient(context, gso)

        googleSignInClient.signOut().addOnCompleteListener {
            _user.value = null
        }
    }

    private fun saveUserToDatabase(firebaseUser: FirebaseUser) {
        val uid = firebaseUser.uid
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)

        val basicInfo = mapOf(
            "username" to (firebaseUser.displayName ?: "Anonymous"),
            "email" to firebaseUser.email,
            "last_login" to System.currentTimeMillis()
        )
        userRef.updateChildren(basicInfo)
    }
}