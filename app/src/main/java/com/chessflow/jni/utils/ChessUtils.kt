package com.chessflow.jni.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import com.chessflow.jni.R

data class ChessPiece(val symbol: String, val isWhite: Boolean)

val pieceMap = mapOf(
    'K' to ChessPiece("♔", true),
    'Q' to ChessPiece("♕", true),
    'R' to ChessPiece("♖", true),
    'B' to ChessPiece("♗", true),
    'N' to ChessPiece("♘", true),
    'P' to ChessPiece("♙", true),
    'k' to ChessPiece("♚", false),
    'q' to ChessPiece("♛", false),
    'r' to ChessPiece("♜", false),
    'b' to ChessPiece("♝", false),
    'n' to ChessPiece("♞", false),
    'p' to ChessPiece("♟", false)
)


fun squareToIndex(square: String): Pair<Int, Int> {
    val file = square[0] - 'a'              // 'f' = 5
    val rank = 8 - square[1].digitToInt()   // '1' = 7
    return Pair(rank, file)
}


fun parseFEN(fen: String): Array<Array<ChessPiece?>> {
    val board = Array(8) { arrayOfNulls<ChessPiece>(8) }
    val rows = fen.split(" ")[0].split("/")

    for (r in 0..7) {
        var col = 0
        for (char in rows[r]) {
            if (char.isDigit()) {
                col += char.digitToInt()
            } else {
                board[r][col] = pieceMap[char]
                col++
            }
        }
    }

    return board
}

fun isValidMove(
    board: Array<Array<ChessPiece?>>,
    from: Pair<Int, Int>,
    to: Pair<Int, Int>,
    piece: ChessPiece
): Boolean {
    // Very basic validation: prevent moving to your own piece
    val target = board[to.first][to.second]
    return target == null || target.isWhite != piece.isWhite
}

fun Array<Array<ChessPiece?>>.movePieceIfValid(
    from: Pair<Int, Int>,
    to: Pair<Int, Int>
): Array<Array<ChessPiece?>> {
    val newBoard = map { it.copyOf() }.toTypedArray()
    newBoard[to.first][to.second] = newBoard[from.first][from.second]
    newBoard[from.first][from.second] = null
    return newBoard
}

fun formatDate(dateString: String?): String {
    if (dateString.isNullOrEmpty()) return "Unknown"

    return dateString
        .replace("?", "")
        .replace("\\.+".toRegex(), ".")
        .removeSuffix(".")
        .ifEmpty { "Unknown" }
}

fun changeLanguage(tag: String) {
    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(tag)
    AppCompatDelegate.setApplicationLocales(appLocale)
}

@Composable
fun translateDifficulty(diff: String): String {
    return when (diff) {
        "Very Easy" -> stringResource(R.string.diff_very_easy)
        "Easy"      -> stringResource(R.string.diff_easy)
        "Medium"    -> stringResource(R.string.diff_medium)
        "Hard"      -> stringResource(R.string.diff_hard)
        "Very Hard" -> stringResource(R.string.diff_very_hard)
        else        -> diff
    }
}

fun uciToPair(uci: String): Pair<Pair<Int, Int>, Pair<Int, Int>> {
    val from = com.chessflow.jni.utils.squareToIndex(uci.substring(0, 2))
    val to = com.chessflow.jni.utils.squareToIndex(uci.substring(2, 4))
    return from to to
}