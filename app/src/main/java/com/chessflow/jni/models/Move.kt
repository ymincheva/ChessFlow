package com.chessflow.jni.models

/**
 * Represents a chess move from one square to another.
 */
data class Move(
    val from: Pair<Int, Int>,
    val to: Pair<Int, Int>
) {
    constructor(fromIndex: Int, toIndex: Int) : this(
        Pair(fromIndex / 8, fromIndex % 8),
        Pair(toIndex / 8, toIndex % 8)
    )

    fun toUci(): String {
        val files = listOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h')
        val ranks = listOf('1', '2', '3', '4', '5', '6', '7', '8')

        val fromFile = files[from.second]
        val fromRank = ranks[7 - from.first]
        val toFile = files[to.second]
        val toRank = ranks[7 - to.first]

        return "$fromFile$fromRank$toFile$toRank"
    }

    companion object {
        /**
         * Converts a UCI move string (e.g. "e2e4") into a Move object.
         */
        fun fromUci(uci: String): Move {
            require(uci.length >= 4) { "Invalid UCI move: $uci" }

            val fileToCol = mapOf('a' to 0, 'b' to 1, 'c' to 2, 'd' to 3, 'e' to 4, 'f' to 5, 'g' to 6, 'h' to 7)
            val rankToRow = mapOf('1' to 7, '2' to 6, '3' to 5, '4' to 4, '5' to 3, '6' to 2, '7' to 1, '8' to 0)

            val fromFile = fileToCol[uci[0]] ?: 0
            val fromRank = rankToRow[uci[1]] ?: 0
            val toFile = fileToCol[uci[2]] ?: 0
            val toRank = rankToRow[uci[3]] ?: 0

            return Move(Pair(fromRank, fromFile), Pair(toRank, toFile))
        }
    }
}
