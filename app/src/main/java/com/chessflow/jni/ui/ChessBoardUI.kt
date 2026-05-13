package com.chessflow.jni.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chessflow.jni.R
import com.chessflow.jni.models.Move
import com.chessflow.jni.utils.Board
import kotlin.math.roundToInt

@Composable
fun ChessBoardUI(
    board: Board,
    correctMove: Move?,
    selectedSquare: Pair<Int, Int>?,
    showAnswer: Boolean,
    isFlipped: Boolean = false,
    isEditable: Boolean = true,
    isEraserMode: Boolean = false,
    onDeletePiece: (Int, Int) -> Unit,
    onSquareClick: (Int, Int) -> Unit,
    onMoveAttempt: (from: Pair<Int, Int>, to: Pair<Int, Int>) -> Unit,
    squareSize: Dp
) {
    val files = if (isFlipped) listOf("h", "g", "f", "e", "d", "c", "b", "a")
    else listOf("a", "b", "c", "d", "e", "f", "g", "h")

    val ranks = if (isFlipped) (1..8).toList()
    else (8 downTo 1).toList()

    val rowIndices = if (isFlipped) (7 downTo 0).toList() else (0..7).toList()
    val colIndices = if (isFlipped) (7 downTo 0).toList() else (0..7).toList()

    val darkColor = colorResource(id = R.color.moss_dark_light)
    val lightColor = colorResource(id = R.color.champagne)
    val highlightYellow = Color.Yellow.copy(alpha = 0.5f)
    val highlightGreen = Color.Green.copy(alpha = 0.5f)

    val density = LocalDensity.current
    val squareSizePx = with(density) { squareSize.toPx() }
    var draggingSquare by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.width(16.dp))
            files.forEach { file ->
                Text(
                    text = file,
                    fontSize = 12.sp,
                    modifier = Modifier.width(squareSize),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
        }

        Box {
            Column {
                rowIndices.forEachIndexed { displayRowIndex, actualRow ->
                    val rank = ranks[displayRowIndex]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = rank.toString(),
                            fontSize = 12.sp,
                            modifier = Modifier.width(16.dp),
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Row {
                            colIndices.forEachIndexed { displayColIndex, actualCol ->
                                val isDark = (displayRowIndex + displayColIndex) % 2 != 0
                                val squareColor = if (isDark) darkColor else lightColor

                                val isSelected =
                                    selectedSquare?.first == actualRow && selectedSquare.second == actualCol
                                val isMoveSquare = showAnswer && correctMove != null &&
                                        ((correctMove.from.first == actualRow && correctMove.from.second == actualCol) ||
                                                (correctMove.to.first == actualRow && correctMove.to.second == actualCol))

                                val highlightOverlay = when {
                                    isSelected -> highlightYellow
                                    isMoveSquare -> highlightGreen
                                    else -> Color.Transparent
                                }

                                val piece = board[actualRow][actualCol]

                                Box(
                                    modifier = Modifier
                                        .size(squareSize)
                                        .background(squareColor)
                                        .pointerInput(actualRow, actualCol, piece) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    if (piece != null) {
                                                        draggingSquare = actualRow to actualCol
                                                        dragOffset = Offset.Zero
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount
                                                },
                                                onDragEnd = {
                                                    val from = draggingSquare
                                                    if (from != null) {
                                                        val multiplier = if (isFlipped) -1 else 1
                                                        val colMove =
                                                            ((dragOffset.x / squareSizePx) * multiplier).roundToInt()
                                                        val rowMove =
                                                            ((dragOffset.y / squareSizePx) * multiplier).roundToInt()

                                                        val targetRow = from.first + rowMove
                                                        val targetCol = from.second + colMove

                                                        if (targetRow in 0..7 && targetCol in 0..7) {
                                                            onMoveAttempt(
                                                                from,
                                                                targetRow to targetCol
                                                            )
                                                        }
                                                    }
                                                    draggingSquare = null
                                                },
                                                onDragCancel = { draggingSquare = null }
                                            )
                                        }
                                        .clickable(enabled = isEditable) {
                                            if (isEraserMode) {
                                                onDeletePiece(actualRow, actualCol)
                                            } else {
                                                onSquareClick(actualRow, actualCol)
                                            }
                                        }
                                ) {
                                    if (isEraserMode && piece != null) {
                                        Box(
                                            Modifier
                                                .matchParentSize()
                                                .background(Color.Red.copy(alpha = 0.1f))
                                        )
                                    }
                                    if (highlightOverlay != Color.Transparent) {
                                        Box(
                                            Modifier
                                                .matchParentSize()
                                                .background(highlightOverlay))
                                    }

                                    if (piece != null && draggingSquare != (actualRow to actualCol)) {
                                        val resId = getPieceResource(piece.symbol)
                                        if (resId != 0) {
                                            Image(
                                                painter = painterResource(id = resId),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(4.dp)
                                                    .graphicsLayer(alpha = 0.9f),
                                                colorFilter = if (piece.symbol.first()
                                                        .isLowerCase()
                                                ) {
                                                    androidx.compose.ui.graphics.ColorFilter.tint(
                                                        Color(0xFF444444)
                                                    )
                                                } else {
                                                    null
                                                }
                                            )

                                        }
                                    }
                                }
                            }
                        }
                        // Цифрите отдясно
                        Text(
                            text = rank.toString(),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .width(16.dp)
                                .padding(start = 4.dp),
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Dragging
            draggingSquare?.let { (r, c) ->
                val piece = board[r][c]
                if (piece != null) {
                    val resId = getPieceResource(piece.symbol)
                    val displayRow = if (isFlipped) 7 - r else r
                    val displayCol = if (isFlipped) 7 - c else c

                    if (resId != 0) {
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = (16.dp.toPx() + displayCol * squareSizePx + dragOffset.x).roundToInt(),
                                        y = (displayRow * squareSizePx + dragOffset.y).roundToInt()
                                    )
                                }
                                .size(squareSize),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(squareSize * 1.1f)
                                    .graphicsLayer(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getPieceResource(symbol: String): Int {
    return when (symbol) {
        "P" -> R.drawable.ic_white_pawn
        "R" -> R.drawable.ic_white_rook
        "N" -> R.drawable.ic_white_knight
        "B" -> R.drawable.ic_white_bishop
        "Q" -> R.drawable.ic_white_queen
        "K" -> R.drawable.ic_white_king
        "p" -> R.drawable.ic_black_pawn
        "r" -> R.drawable.ic_black_rook
        "n" -> R.drawable.ic_black_knight
        "b" -> R.drawable.ic_black_bishop
        "q" -> R.drawable.ic_black_queen
        "k" -> R.drawable.ic_black_king
        else -> 0
    }
}