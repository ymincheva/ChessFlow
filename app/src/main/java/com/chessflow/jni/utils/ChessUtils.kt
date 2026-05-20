package com.chessflow.jni.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import com.chessflow.jni.R

data class ChessPiece(val symbol: String, val isWhite: Boolean)

fun squareToIndex(square: String): Pair<Int, Int> {
    val file = square[0] - 'a'              // 'f' = 5
    val rank = 8 - square[1].digitToInt()   // '1' = 7
    return Pair(rank, file)
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
        "Easy" -> stringResource(R.string.diff_easy)
        "Medium" -> stringResource(R.string.diff_medium)
        "Hard" -> stringResource(R.string.diff_hard)
        "Very Hard" -> stringResource(R.string.diff_very_hard)
        else -> diff
    }
}

fun uciToPair(uci: String): Pair<Pair<Int, Int>, Pair<Int, Int>> {
    val from = com.chessflow.jni.utils.squareToIndex(uci.substring(0, 2))
    val to = com.chessflow.jni.utils.squareToIndex(uci.substring(2, 4))
    return from to to
}