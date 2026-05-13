package com.chessflow.jni

import android.content.Context
import android.util.Log
import com.chessflow.jni.utils.StockfishAssetManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.coroutineContext

object StockfishEngine {
    val inputChannel = Channel<String>(Channel.UNLIMITED)
    val bestMoveChannel = Channel<String>(Channel.CONFLATED)
    val evaluationChannel = Channel<String>(Channel.CONFLATED)

    private var engineStarted = false
    private var engineJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private external fun initializeEngine()
    private external fun sendCommand(cmd: String)
    private external fun readOutput(): String?
    private external fun shutdownEngine()

    fun start(context: Context, evalFileName: String, evalFileSmallName: String) {
        if (engineStarted) return

        try {
            System.loadLibrary("stockfish-jni")
            initializeEngine()
            engineStarted = true

            engineJob = engineScope.launch {
                launch { sendInput() }
                launch { listenOutput() }

                val evalFilePath = StockfishAssetManager.copyNNUEFile(context, evalFileName)
                val evalFileSmallPath = StockfishAssetManager.copyNNUEFile(context, evalFileSmallName)

                safeSend("uci")
                delay(200)
                safeSend("setoption name EvalFile value $evalFilePath")
                safeSend("setoption name EvalFileSmall value $evalFileSmallPath")
                safeSend("setoption name UCI_LimitStrength value true")
                safeSend("setoption name UCI_Elo value 3000")
                safeSend("isready")
            }
        } catch (e: Exception) {
            Log.e("Stockfish", "Грешка при стартиране: ${e.message}")
        }
    }

    private suspend fun safeSend(command: String) {
        if (engineStarted && !inputChannel.isClosedForSend) {
            inputChannel.send(command)
        }
    }

    fun getNextMove(fen: String, moveTime: Int = 1000) {
        engineScope.launch {
            safeSend("stop")
            safeSend("position fen $fen")
            safeSend("go movetime $moveTime")
        }
    }

    private suspend fun sendInput() {
        for (cmd in inputChannel) {
            if (!engineStarted) break
            Log.d("StockfishEngine", "Stockfish Input: $cmd")
            sendCommand(cmd)
        }
    }

    private suspend fun listenOutput() {
        while (engineStarted && coroutineContext.isActive) {
            val output = readOutput()
            if (!output.isNullOrBlank()) {
                val trimmed = output.trim()
                processOutput(trimmed)
            } else {
                delay(20)
            }
        }
    }

    private suspend fun processOutput(trimmed: String) {
        Log.d("StockfishEngine", "Stockfish: $trimmed")

        if (trimmed.startsWith("bestmove")) {
            val parts = trimmed.split(" ")
            if (parts.size >= 2) {
                bestMoveChannel.send(parts[1])
            }
        }

        if (trimmed.contains("score cp")) {
            val parts = trimmed.split(" ")
            val index = parts.indexOf("cp")
            if (index != -1 && index + 1 < parts.size) {
                val cp = parts[index + 1].toFloatOrNull() ?: 0f
                evaluationChannel.send(String.format("%.2f", cp / 100.0))
            }
        } else if (trimmed.contains("score mate")) {
            val parts = trimmed.split(" ")
            val index = parts.indexOf("mate")
            if (index != -1 && index + 1 < parts.size) {
                evaluationChannel.send("M${parts[index + 1]}")
            }
        }
    }

    fun stopEngine() {
        if (!engineStarted) return

        engineStarted = false
        engineScope.launch {
            try {
                if (!inputChannel.isClosedForSend) {
                    inputChannel.send("quit")
                }
                delay(100)

                engineJob?.cancelAndJoin()
                shutdownEngine()

                Log.d("StockfishEngine", "✅ Native engine destroyed successfully")
            } catch (e: Exception) {
                Log.e("StockfishEngine", "❌ Shutdown error: ${e.message}")
            }
        }
    }
}