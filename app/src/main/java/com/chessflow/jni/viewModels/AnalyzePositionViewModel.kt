package com.chessflow.jni.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chessflow.jni.R
import com.chessflow.jni.StockfishEngine
import com.chessflow.jni.models.Move
import com.chessflow.jni.models.MoveQuality
import com.chessflow.jni.states.AnalyzePositionState
import com.chessflow.jni.utils.Board
import com.chessflow.jni.utils.FenValidator
import com.chessflow.jni.utils.PgnValidator
import com.chessflow.jni.utils.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing position analysis and PGN/FEN navigation
 * using the Stockfish chess engine via JNI.
 */
class AnalyzePositionViewModel : ViewModel() {

    private val _state = MutableStateFlow(AnalyzePositionState())

    val state: StateFlow<AnalyzePositionState> = _state
        .onStart {
            if (_state.value.currentMoveIndex == -1 && _state.value.boardFen.isEmpty()) {
                resetToStartPosition()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnalyzePositionState()
        )

    private var pgnMoveHistory: List<String> = emptyList()
    private var sanMoveHistory: List<String> = emptyList()

    private var activeTaskJob: Job? = null

    init {
        resetToStartPosition()

        viewModelScope.launch(Dispatchers.IO) {
            StockfishEngine.bestMoveChannel.receiveAsFlow().collect { moveUci ->
                if (_state.value.isAnalyzing) {
                    handleEngineMove(moveUci)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            StockfishEngine.evaluationChannel.receiveAsFlow().collect { score ->
                if (_state.value.isAnalyzing) {
                    _state.update { it.copy(evaluation = score) }
                }
            }
        }
    }

    private fun handleEngineMove(uciMove: String) {
        val cleanMove = uciMove.replace("bestmove ", "").trim()
        val parsedMove = runCatching { Move.fromUci(cleanMove) }.getOrNull()

        _state.update {
            it.copy(
                isAnalyzing = false,
                bestMoveUci = cleanMove,
                bestMove = parsedMove,
                statusMessage = ""
            )
        }
    }

    fun resetToStartPosition() {
        stopAnalysis()
        val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        pgnMoveHistory = emptyList()
        sanMoveHistory = emptyList()
        _state.update {
            it.copy(
                board = Board.parseFEN(startFen),
                boardFen = startFen,
                loadedPgn = "",
                currentMoveIndex = -1,
                totalMoves = 0,
                canGoPrevious = false,
                canGoNext = false,
                bestMove = null,
                bestMoveUci = "",
                moveQuality = null,
                cpLoss = null,
                infoMessage = UiText.ResourceString(R.string.msg_reset_start)
            )
        }
    }

    fun loadFenPosition(fen: String) {
        stopAnalysis()

        val cleanFen = fen.trim()

        if (!FenValidator.isValidFen(cleanFen)) {
            _state.update {
                it.copy(
                    isAnalyzing = false,
                    statusMessage = "",
                    infoMessage = UiText.ResourceString(R.string.error_invalid_fen_format)
                )
            }
            return
        }

        activeTaskJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val parsedBoard = Board.parseFEN(cleanFen)
                pgnMoveHistory = emptyList()
                sanMoveHistory = emptyList()

                _state.update { currentState ->
                    currentState.copy(
                        board = parsedBoard,
                        boardFen = cleanFen,
                        loadedPgn = "",
                        currentMoveIndex = -1,
                        totalMoves = 0,
                        canGoPrevious = false,
                        canGoNext = false,
                        bestMove = null,
                        bestMoveUci = "",
                        evaluation = "0.0",
                        moveQuality = null,
                        cpLoss = null,
                        statusMessage = "",
                        infoMessage = UiText.ResourceString(R.string.msg_position_loaded),
                        isAnalyzing = false,
                        isEvaluatingQuality = false
                    )
                }
            } catch (e: Exception) {
                Log.e("AnalyzeViewModel", "Error loading FEN: ${e.message}", e)
                _state.update {
                    it.copy(
                        isAnalyzing = false,
                        statusMessage = "",
                        infoMessage = UiText.ResourceString(R.string.error_parsing_fen)
                    )
                }
            }
        }
    }

    fun loadPgnGame(pgn: String) {
        stopAnalysis()

        val cleanPgn = pgn.trim()
        if (!PgnValidator.isValidPgn(cleanPgn)) {
            _state.update {
                it.copy(
                    isAnalyzing = false,
                    statusMessage = "",
                    infoMessage = UiText.ResourceString(R.string.error_invalid_pgn_format)
                )
            }
            return
        }

        activeTaskJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val result = Board.parsePgnToFenList(cleanPgn)

                if (result.fenList.isNotEmpty()) {
                    pgnMoveHistory = result.fenList
                    sanMoveHistory = result.sanMoves

                    updateMoveIndex(0, isInitialLoad = true)
                } else {
                    _state.update {
                        it.copy(
                            isAnalyzing = false,
                            statusMessage = "",
                            infoMessage = UiText.ResourceString(R.string.error_parsing_pgn)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AnalyzeViewModel", "Error loading PGN: ${e.message}", e)
                _state.update {
                    it.copy(
                        isAnalyzing = false,
                        statusMessage = "",
                        infoMessage = UiText.DynamicString("Error loading PGN: ${e.message}")
                    )
                }
            }
        }
    }

    private fun updateMoveIndex(index: Int, isInitialLoad: Boolean = false) {
        if (index !in pgnMoveHistory.indices) return

        val targetFen = pgnMoveHistory[index]
        val (moveFrom, moveTo) = if (index > 0) {
            extractMoveSquares(index)
        } else {
            Pair(null, null)
        }

        _state.update { currentState ->
            currentState.copy(
                board = Board.parseFEN(targetFen),
                boardFen = targetFen,
                currentMoveIndex = index,
                totalMoves = pgnMoveHistory.size,
                lastMoveFrom = moveFrom,
                lastMoveTo = moveTo,
                canGoPrevious = index > 0,
                canGoNext = index < pgnMoveHistory.size - 1,
                bestMove = null,
                bestMoveUci = "",
                evaluation = "0.0",
                moveQuality = null,
                cpLoss = null,
                isAnalyzing = false,
                isEvaluatingQuality = false,
                statusMessage = "",
                infoMessage = if (isInitialLoad) UiText.ResourceString(R.string.msg_pgn_loaded) else currentState.infoMessage
            )
        }
    }

    fun evaluateCurrentMoveQuality() {
        val index = _state.value.currentMoveIndex
        if (index <= 0 || index >= pgnMoveHistory.size) return

        stopAnalysis()

        val fenBefore = pgnMoveHistory[index - 1]
        val fenAfter = pgnMoveHistory[index]

        val evaluatedMoveText = formatMoveText(index)
        val uciMove = getUciMoveForIndex(index)

        activeTaskJob = viewModelScope.launch(Dispatchers.Default) {
            _state.update {
                it.copy(
                    isAnalyzing = false,
                    isEvaluatingQuality = true,
                    bestMove = null,
                    bestMoveUci = "",
                    moveQuality = null,
                    cpLoss = null
                )
            }

            while (StockfishEngine.bestMoveChannel.tryReceive().isSuccess) { }

            val evalBefore = StockfishEngine.evaluateFen(fenBefore, moveTime = 1000)
            val evalAfterRaw = StockfishEngine.evaluateFen(fenAfter, moveTime = 1000)

            val evalAfterFromMyPerspective = -evalAfterRaw

            val cpLoss = (evalBefore - evalAfterFromMyPerspective).coerceAtLeast(0)
            val quality = MoveQuality.fromCentipawnLoss(cpLoss)

            while (StockfishEngine.bestMoveChannel.tryReceive().isSuccess) { }

            _state.update {
                it.copy(
                    isEvaluatingQuality = false,
                    moveQuality = quality,
                    cpLoss = cpLoss,
                    evaluatedMoveText = evaluatedMoveText,
                    evaluatedMoveUci = uciMove ?: "",
                    bestMove = null,
                    bestMoveUci = ""
                )
            }
        }
    }

    private fun extractMoveSquares(moveIndex: Int): Pair<String?, String?> {
        val uciMove = getUciMoveForIndex(moveIndex)
        if (uciMove != null && uciMove.length >= 4) {
            return Pair(uciMove.substring(0, 2), uciMove.substring(2, 4))
        }
        return Pair(null, null)
    }

    private fun getUciMoveForIndex(moveIndex: Int): String? {
        if (moveIndex <= 0 || moveIndex >= pgnMoveHistory.size) return null

        val fenBefore = pgnMoveHistory[moveIndex - 1]
        val fenAfter = pgnMoveHistory[moveIndex]

        return convertFenDiffToUci(fenBefore, fenAfter)
    }

    private fun convertFenDiffToUci(fenBefore: String, fenAfter: String): String? {
        return runCatching {
            val boardBefore = Board.parseFEN(fenBefore)
            val boardAfter = Board.parseFEN(fenAfter)

            val activeColorBefore = fenBefore.split(" ").getOrNull(1) ?: "w"
            val isWhiteMove = activeColorBefore == "w"

            var fromSquare: String? = null
            var toSquare: String? = null

            for (r in 0..7) {
                for (c in 0..7) {
                    val pieceBefore = boardBefore[r][c]
                    val pieceAfter = boardAfter[r][c]

                    if (pieceBefore?.symbol != pieceAfter?.symbol) {
                        val squareName = formatSquareName(r, c)

                        if (pieceBefore != null && isPieceColorMatching(pieceBefore.symbol, isWhiteMove) && pieceAfter == null) {
                            fromSquare = squareName
                        } else if (pieceAfter != null && isPieceColorMatching(pieceAfter.symbol, isWhiteMove)) {
                            toSquare = squareName
                        }
                    }
                }
            }

            if (fromSquare == "e1" && (toSquare == "c1" || toSquare == "g1")) return "e1$toSquare"
            if (fromSquare == "e8" && (toSquare == "c8" || toSquare == "g8")) return "e8$toSquare"

            if (fromSquare != null && toSquare != null) {
                return "$fromSquare$toSquare"
            }
            null
        }.getOrNull()
    }

    private fun formatSquareName(row: Int, col: Int): String {
        val file = ('a' + col)
        val rank = 8 - row
        return "$file$rank"
    }

    private fun isPieceColorMatching(pieceSymbol: String, isWhite: Boolean): Boolean {
        if (pieceSymbol.isEmpty()) return false
        val isUppercase = pieceSymbol.first().isUpperCase()
        return if (isWhite) isUppercase else !isUppercase
    }

    fun formatMoveText(index: Int): String {
        if (index <= 0 || sanMoveHistory.isEmpty()) {
            return "Start Position"
        }

        val sanIndex = index - 1
        if (sanIndex !in sanMoveHistory.indices) {
            return "Start Position"
        }

        val moveSan = sanMoveHistory[sanIndex]
        val moveNumber = (index + 1) / 2
        val isWhiteMove = index % 2 != 0

        return if (isWhiteMove) {
            "$moveNumber. $moveSan"
        } else {
            "$moveNumber... $moveSan"
        }
    }

    // --- Navigation controls ---

    fun goToFirstMove() {
        if (pgnMoveHistory.isEmpty()) return
        stopAnalysis()
        updateMoveIndex(0)
    }

    fun goToPreviousMove() {
        val currentIndex = _state.value.currentMoveIndex
        if (currentIndex > 0) {
            stopAnalysis()
            updateMoveIndex(currentIndex - 1)
        }
    }

    fun goToNextMove() {
        val currentIndex = _state.value.currentMoveIndex
        if (currentIndex < pgnMoveHistory.size - 1) {
            stopAnalysis()
            updateMoveIndex(currentIndex + 1)
        }
    }

    fun goToLastMove() {
        if (pgnMoveHistory.isEmpty()) return
        stopAnalysis()
        updateMoveIndex(pgnMoveHistory.size - 1)
    }

    fun analyzePosition(moveTime: Int = 2000) {
        val currentState = _state.value
        val cleanFen = currentState.boardFen

        if (!currentState.board.hasValidKings()) {
            _state.update {
                it.copy(
                    isAnalyzing = false,
                    statusMessage = "Illegal position!",
                    infoMessage = UiText.ResourceString(R.string.msg_illegal_position)
                )
            }
            return
        }

        stopAnalysis()

        _state.update {
            it.copy(
                isAnalyzing = true,
                isEvaluatingQuality = false,
                bestMove = null,
                bestMoveUci = "",
                statusMessage = "Stockfish is thinking...",
                infoMessage = UiText.ResourceString(R.string.msg_stockfish_thinking)
            )
        }

        StockfishEngine.getNextMove(cleanFen, moveTime)
    }

    private fun stopAnalysis() {
        activeTaskJob?.cancel()
        activeTaskJob = null

        _state.update {
            it.copy(
                isAnalyzing = false,
                isEvaluatingQuality = false,
                statusMessage = ""
            )
        }
    }
}