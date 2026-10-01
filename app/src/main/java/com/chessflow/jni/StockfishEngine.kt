package com.chessflow.jni

import android.content.Context
import android.util.Log
import com.chessflow.jni.utils.StockfishAssetManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

/**
 * Thread-safe Singleton managing the native Stockfish C++ process via JNI.
 * Handles bidirectional communication over Kotlin Coroutine Channels and Mutex locks.
 */
object StockfishEngine {

    val inputChannel = Channel<String>(Channel.UNLIMITED)
    val bestMoveChannel = Channel<String>(Channel.CONFLATED)
    val evaluationChannel = Channel<String>(Channel.CONFLATED)

    // Mutex to prevent race conditions during rapid consecutive evaluation requests
    private val evalMutex = Mutex()

    @Volatile
    private var engineStarted = false
    private var engineJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Single-shot holder for synchronous evaluateFen requests
    @Volatile
    private var singleEvalDeferred: CompletableDeferred<Int>? = null

    // Native C++ JNI function declarations
    private external fun initializeEngine()
    private external fun sendCommand(cmd: String)
    private external fun readOutput(): String?
    private external fun shutdownEngine()

    /**
     * Loads the native binary and initializes Stockfish with required NNUE neural network files.
     */
    fun start(context: Context, evalFileName: String, evalFileSmallName: String) {
        if (engineStarted) return

        try {
            System.loadLibrary("stockfish-jni")
            initializeEngine()
            engineStarted = true

            engineJob = engineScope.launch {
                launch { sendInput() }
                launch { listenOutput() }

                // Asynchronously copy NNUE assets to internal storage
                val evalFilePath = StockfishAssetManager.copyNNUEFile(context, evalFileName)
                val evalFileSmallPath = StockfishAssetManager.copyNNUEFile(context, evalFileSmallName)

                // Initialize standard UCI settings
                safeSend("uci")
                delay(100)
                safeSend("setoption name EvalFile value $evalFilePath")
                safeSend("setoption name EvalFileSmall value $evalFileSmallPath")
                safeSend("setoption name UCI_LimitStrength value true")
                safeSend("setoption name UCI_Elo value 3000")
                safeSend("isready")
            }
        } catch (e: Exception) {
            Log.e("StockfishEngine", "Error starting native Stockfish engine: ${e.message}")
        }
    }

    /**
     * Safely dispatches a UCI command to the input channel if the engine is running.
     */
    private suspend fun safeSend(command: String) {
        if (engineStarted && !inputChannel.isClosedForSend) {
            inputChannel.send(command)
        }
    }

    /**
     * Requests the engine to calculate the best move for a target FEN position.
     */
    fun getNextMove(fen: String, moveTime: Int = 1000) {
        engineScope.launch {
            safeSend("stop")
            safeSend("position fen $fen")
            safeSend("go movetime $moveTime")
        }
    }

    /**
     * Worker coroutine sending queued input commands down to the native JNI layer.
     */
    private suspend fun sendInput() {
        for (cmd in inputChannel) {
            if (!engineStarted) break
            sendCommand(cmd)
        }
    }

    /**
     * Worker coroutine continuously polling output lines from the native JNI process.
     */
    private suspend fun listenOutput() {
        while (engineStarted && coroutineContext.isActive) {
            val output = withContext(Dispatchers.IO) { readOutput() }
            if (!output.isNullOrBlank()) {
                processOutput(output.trim())
            } else {
                delay(10) // Small pause to lower CPU consumption if non-blocking
            }
        }
    }

    /**
     * Parses output string lines received from Stockfish UCI protocol.
     */
    private suspend fun processOutput(trimmed: String) {
        if (trimmed.startsWith("bestmove")) {
            val parts = trimmed.split(" ")
            if (parts.size >= 2) {
                bestMoveChannel.trySend(parts[1])
            }
        } else if (trimmed.contains("score cp")) {
            val parts = trimmed.split(" ")
            val index = parts.indexOf("cp")
            if (index != -1 && index + 1 < parts.size) {
                val cpFloat = parts[index + 1].toFloatOrNull() ?: 0f
                val cpInt = cpFloat.toInt()

                // Fulfill single evaluation waiter if active
                singleEvalDeferred?.complete(cpInt)

                evaluationChannel.trySend(String.format("%.2f", cpFloat / 100.0))
            }
        } else if (trimmed.contains("score mate")) {
            val parts = trimmed.split(" ")
            val index = parts.indexOf("mate")
            if (index != -1 && index + 1 < parts.size) {
                val mateIn = parts[index + 1].toIntOrNull() ?: 1
                val cpVal = if (mateIn > 0) 10000 - (mateIn * 100) else -10000 - (mateIn * 100)

                // Fulfill single evaluation waiter if active
                singleEvalDeferred?.complete(cpVal)

                evaluationChannel.trySend("M${parts[index + 1]}")
            }
        }
    }

    /**
     * Synchronously evaluates a FEN position in centipawns with Mutex locks and timeout protection.
     * Prevents engine thread deadlock on rapid user interactions.
     */
    suspend fun evaluateFen(fen: String, moveTime: Int = 1000): Int = evalMutex.withLock {
        return withContext(Dispatchers.IO) {
            val deferred = CompletableDeferred<Int>()
            singleEvalDeferred = deferred

            safeSend("stop")
            safeSend("position fen $fen")
            safeSend("go movetime $moveTime")

            var result = 0
            try {
                // Wait for engine response using standard Kotlin timeout mechanism
                val evalResult = withTimeoutOrNull(moveTime.toLong() + 300L) {
                    deferred.await()
                }
                result = evalResult ?: 0
            } catch (e: Exception) {
                Log.e("StockfishEngine", "Evaluation error: ${e.message}")
            } finally {
                safeSend("stop")
                singleEvalDeferred = null
            }

            result
        }
    }

    /**
     * Gracefully terminates the C++ Stockfish instance and cancels running coroutine scope.
     */
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
            } catch (e: Exception) {
                Log.e("StockfishEngine", "Shutdown error: ${e.message}")
            }
        }
    }
}