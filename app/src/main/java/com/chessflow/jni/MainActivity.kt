package com.chessflow.jni

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.*
import com.chessflow.jni.screens.MainApp
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stockfish = StockfishEngine
        stockfish.start(this, "nn-c288c895ea92.nnue", "nn-37f18f62d772.nnue")

        setContent {
            MaterialTheme {
                MainApp()
            }
        }
    }

    override fun onDestroy() {
        StockfishEngine.stopEngine()
        super.onDestroy()
    }
}

