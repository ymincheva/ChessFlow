package com.chessflow.jni

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.*
import androidx.lifecycle.lifecycleScope
import com.chessflow.jni.screens.MainApp
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appUpdateManager: AppUpdateManager
    private val updateType = AppUpdateType.IMMEDIATE

    private val updateActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Log.e("Update", "Update flow failed or was cancelled by user. Re-checking...")
            checkForForceUpdate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForForceUpdate()

        lifecycleScope.launch(Dispatchers.IO) {
            StockfishEngine.start(this@MainActivity, "nn-c288c895ea92.nnue", "nn-37f18f62d772.nnue")
        }

        setContent {
            MaterialTheme {
                MainApp()
            }
        }
    }

    private fun checkForForceUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(updateType)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateActivityResultLauncher,
                        AppUpdateOptions.newBuilder(updateType).build()
                    )
                } catch (e: Exception) {
                    Log.e("UpdateError", "Error starting update flow: ${e.message}")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateActivityResultLauncher,
                        AppUpdateOptions.newBuilder(updateType).build()
                    )
                } catch (e: Exception) {
                    Log.e("UpdateError", "Error resuming update flow: ${e.message}")
                }
            }
        }
    }

    override fun onDestroy() {
        StockfishEngine.stopEngine()
        super.onDestroy()
    }
}

