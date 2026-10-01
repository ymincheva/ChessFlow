
package com.chessflow.jni.states

import com.chessflow.jni.models.Move
import com.chessflow.jni.models.MoveQuality
import com.chessflow.jni.utils.Board
import com.chessflow.jni.utils.UiText

data class AnalyzePositionState(
    val board: Board = Board(),
    val boardFen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    val loadedPgn: String = "",
    val currentMoveIndex: Int = -1,
    val totalMoves: Int = 0,
    val lastMoveFrom: String? = null,
    val lastMoveTo: String? = null,
    val canGoPrevious: Boolean = false,
    val canGoNext: Boolean = false,
    val bestMove: Move? = null,
    val bestMoveUci: String = "",
    val evaluation: String = "0.0",
    val moveQuality: MoveQuality? = null,
    val cpLoss: Int? = null,
    val evaluatedMoveText: String = "",
    val evaluatedMoveUci: String = "",
    val isAnalyzing: Boolean = false,
    val isEvaluatingQuality: Boolean = false,
    val statusMessage: String = "",
    val infoMessage: UiText = UiText.DynamicString("")
)