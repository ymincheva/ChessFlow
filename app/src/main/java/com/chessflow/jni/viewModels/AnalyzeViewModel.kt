package com.chessflow.jni.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chessflow.jni.StockfishEngine
import com.chessflow.jni.models.Move
import com.chessflow.jni.utils.Board
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalyzeState(
    val isAnalyzing: Boolean = false,
    val bestMoveUci: String = "",
    val evaluation: String = "0.0",
    val statusMessage: String = "",
    val infoMessage: String = "Ready",
    val boardFen: String = "",
    val board: Board = Board.parseFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"),
    val bestMove: Move? = null,
    val isCorrectionMode: Boolean = false,
    val squareToEdit: Pair<Int, Int>? = null,
    val isBoardFlipped: Boolean = false,
    val isEraserMode: Boolean = false
)

class AnalyzeViewModel : ViewModel() {
    private val _state = MutableStateFlow(AnalyzeState())
    val state: StateFlow<AnalyzeState> = _state

    init {
        resetToStartPosition()
        _state.update { it.copy(boardFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1") }

        viewModelScope.launch(Dispatchers.IO) {
            StockfishEngine.bestMoveChannel.receiveAsFlow().collect { moveUci ->
                handleEngineMove(moveUci)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            StockfishEngine.evaluationChannel.receiveAsFlow().collect { score ->
                _state.update { it.copy(evaluation = score) }
            }
        }
    }

    private fun handleEngineMove(uciMove: String) {
        val cleanMove = uciMove.replace("bestmove ", "").trim()
        val parsedMove = try { Move.fromUci(cleanMove) } catch (e: Exception) { null }

        _state.update { it.copy(
            isAnalyzing = false,
            bestMoveUci = cleanMove,
            bestMove = parsedMove,
            statusMessage = ""
        ) }
    }

    fun setupEmptyBoard() {
        val emptyFen = "8/8/8/8/8/8/8/8 w - - 0 1"
        _state.update { it.copy(
            board = Board.parseFEN(emptyFen),
            boardFen = emptyFen,
            infoMessage = "Board cleared."
        ) }
    }

    fun analyzePosition(moveTime: Int = 2000) {
        val currentState = _state.value
        val side = currentState.boardFen.split(" ").getOrNull(1) ?: "w"

        val cleanFen = currentState.board.toFen(side)

        Log.d("StockfishDebug", "Sending Clean FEN to Engine: $cleanFen")

        _state.update { it.copy(
            isAnalyzing = true,
            statusMessage = "Stockfish is thinking...",
            infoMessage = ""
        ) }

        StockfishEngine.getNextMove(cleanFen, moveTime)
    }

    fun setPieceAt(pieceSymbol: String) {
        val currentState = _state.value
        val square = currentState.squareToEdit ?: return

        val newBoard = currentState.board.withPiece(square.first, square.second, pieceSymbol)

        newBoard.castlingRights = currentState.board.castlingRights

        updateBoard(newBoard, "Board updated.")
        stopCorrectionMode()
    }

    fun updateSideToMove(side: Char) {
        val newBoard = _state.value.board
        val newFen = newBoard.toFen(side.toString())

        _state.update { it.copy(
            boardFen = newFen,
            infoMessage = if (side == 'w') "White to move" else "Black to move"
        ) }
    }

    fun resetToStartPosition() {
        val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        _state.update { it.copy(
            board = Board.parseFEN(startFen),
            boardFen = startFen,
            infoMessage = "Reset to start position."
        ) }
    }

    fun movePiece(from: Pair<Int, Int>, to: Pair<Int, Int>) {
        if (from == to) return

        val currentState = _state.value
        val newBoard = currentState.board.movePieceIfValid(from, to)

        val currentTurn = currentState.boardFen.split(" ").getOrNull(1) ?: "w"
        val nextTurn = if (currentTurn == "w") "b" else "w"

        updateBoard(newBoard, "Piece moved.", nextTurn)
    }

    fun startCorrectionMode(row: Int, col: Int) {
        if (_state.value.isAnalyzing) return
        _state.update { it.copy(isCorrectionMode = true, squareToEdit = Pair(row, col)) }
    }

    fun stopCorrectionMode() {
        _state.update { it.copy(isCorrectionMode = false, squareToEdit = null) }
    }

    fun toggleBoardFlip() {
        _state.update { it.copy(isBoardFlipped = !it.isBoardFlipped) }
    }


    private fun updateBoard(newBoard: Board, message: String = "", nextTurn: String? = null) {
        val turn = nextTurn ?: (_state.value.boardFen.split(" ").getOrNull(1) ?: "w")

        val finalFen = newBoard.toFen(turn)

        Log.d("StockfishDebug", "Final Clean FEN: $finalFen")

        _state.update { currentState ->
            currentState.copy(
                board = newBoard,
                boardFen = finalFen,
                bestMove = null,
                bestMoveUci = "",
                evaluation = "0.0",
                infoMessage = message,
                isAnalyzing = false
            )
        }
    }

    fun toggleEraserMode() {
        _state.update { it.copy(isEraserMode = !it.isEraserMode, isCorrectionMode = false) }
    }

    fun deletePieceAt(row: Int, col: Int) {
        val currentState = _state.value
        val newBoard = currentState.board.withPiece(row, col, "")
        newBoard.castlingRights = currentState.board.castlingRights

        updateBoard(newBoard, "Piece removed")
    }
}