package com.chessflow.jni.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chessflow.jni.R
import com.chessflow.jni.StockfishEngine
import com.chessflow.jni.models.Move
import com.chessflow.jni.utils.Board
import com.chessflow.jni.utils.UiText
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
    val infoMessage: UiText = UiText.ResourceString(R.string.msg_ready),
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
            infoMessage = UiText.ResourceString(R.string.msg_board_cleared)
        ) }
    }

    fun analyzePosition(moveTime: Int = 2000) {
        val currentState = _state.value
        val side = currentState.boardFen.split(" ").getOrNull(1) ?: "w"
        val cleanFen = currentState.board.toFen(side)

        Log.d("ChessFlow", "Sending Clean FEN to Engine: $cleanFen")

        if (!isPositionLegalBeforeAnalysis(cleanFen)) {
            _state.update { it.copy(
                isAnalyzing = false,
                statusMessage = "Illegal position!",
                infoMessage = UiText.ResourceString(R.string.msg_illegal_position)
            ) }
            return
        }

        _state.update { it.copy(
            isAnalyzing = true,
            statusMessage = "Stockfish is thinking...",
            infoMessage = UiText.ResourceString(R.string.msg_stockfish_thinking)
        ) }

        StockfishEngine.getNextMove(cleanFen, moveTime)
    }

    fun setPieceAt(pieceSymbol: String) {
        val currentState = _state.value
        val square = currentState.squareToEdit ?: return

        val newBoard = currentState.board.withPiece(square.first, square.second, pieceSymbol)
        newBoard.castlingRights = currentState.board.castlingRights

        updateBoard(newBoard, UiText.ResourceString(R.string.msg_board_updated))
        stopCorrectionMode()
    }

    fun updateSideToMove(side: Char) {
        val newBoard = _state.value.board
        val newFen = newBoard.toFen(side.toString())

        val resId = if (side == 'w') R.string.msg_white_to_move else R.string.msg_black_to_move

        _state.update { it.copy(
            boardFen = newFen,
            infoMessage = UiText.ResourceString(resId)
        ) }
    }

    fun resetToStartPosition() {
        val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        _state.update { it.copy(
            board = Board.parseFEN(startFen),
            boardFen = startFen,
            infoMessage = UiText.ResourceString(R.string.msg_reset_start)
        ) }
    }

    fun movePiece(from: Pair<Int, Int>, to: Pair<Int, Int>) {
        if (from == to) return

        val currentState = _state.value
        val newBoard = currentState.board.movePieceIfValid(from, to)

        val currentTurn = currentState.boardFen.split(" ").getOrNull(1) ?: "w"
        val nextTurn = if (currentTurn == "w") "b" else "w"

        updateBoard(newBoard, UiText.ResourceString(R.string.msg_piece_moved), nextTurn)
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

    private fun updateBoard(newBoard: Board, message: UiText = UiText.DynamicString(""), nextTurn: String? = null) {
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

        updateBoard(newBoard, UiText.ResourceString(R.string.msg_piece_removed))
    }

    private fun isPositionLegalBeforeAnalysis(fen: String): Boolean {
        val parts = fen.split(" ")
        val boardStr = parts.getOrNull(0) ?: return false
        val turn = parts.getOrNull(1) ?: "w"

        val rows = boardStr.split("/")
        if (rows.size != 8) return false

        var whiteKingRow = -1
        var whiteKingCol = -1
        var blackKingRow = -1
        var blackKingCol = -1

        val grid = Array(8) { CharArray(8) { ' ' } }

        for (r in 0 until 8) {
            var c = 0
            for (char in rows[r]) {
                if (char.isDigit()) {
                    val emptySquares = char.toString().toInt()
                    c += emptySquares
                } else {
                    grid[r][c] = char
                    if (char == 'K') { whiteKingRow = r; whiteKingCol = c }
                    if (char == 'k') { blackKingRow = r; blackKingCol = c }
                    c++
                }
            }
        }

        if (whiteKingRow == -1 || blackKingRow == -1) return false

        val rowDiff = Math.abs(whiteKingRow - blackKingRow)
        val colDiff = Math.abs(whiteKingCol - blackKingCol)
        if (rowDiff <= 1 && colDiff <= 1) return false

        val targetKingRow = if (turn == "w") blackKingRow else whiteKingRow
        val targetKingCol = if (turn == "w") blackKingCol else whiteKingCol
        val enemyRook = if (turn == "w") 'R' else 'r'
        val enemyQueen = if (turn == "w") 'Q' else 'q'

        for (c in (targetKingCol - 1) downTo 0) {
            val p = grid[targetKingRow][c]
            if (p != ' ') {
                if (p == enemyRook || p == enemyQueen) return false
                break
            }
        }

        for (c in (targetKingCol + 1) until 8) {
            val p = grid[targetKingRow][c]
            if (p != ' ') {
                if (p == enemyRook || p == enemyQueen) return false
                break
            }
        }

        for (r in (targetKingRow - 1) downTo 0) {
            val p = grid[r][targetKingCol]
            if (p != ' ') {
                if (p == enemyRook || p == enemyQueen) return false
                break
            }
        }

        for (r in (targetKingRow + 1) until 8) {
            val p = grid[r][targetKingCol]
            if (p != ' ') {
                if (p == enemyRook || p == enemyQueen) return false
                break
            }
        }

        return true
    }
}