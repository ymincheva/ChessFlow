package com.chessflow.jni.utils

class Board(
    private val squares: Array<Array<Piece?>> = Array(8) { arrayOfNulls<Piece>(8) },
    var castlingRights: String = "KQkq"
) {
    operator fun get(row: Int, col: Int): Piece? = squares.getOrNull(row)?.getOrNull(col)
    operator fun get(row: Int): Array<Piece?> = squares[row]

    fun withMove(from: Pair<Int, Int>, to: Pair<Int, Int>): Board {
        val newSquares = Array(8) { r -> squares[r].copyOf() }
        val piece = newSquares[from.first][from.second]
        newSquares[to.first][to.second] = piece
        newSquares[from.first][from.second] = null
        return Board(newSquares)
    }

    fun withPiece(row: Int, col: Int, pieceSymbol: String): Board {
        val newSquares = Array(8) { r -> squares[r].copyOf() }
        newSquares[row][col] = if (pieceSymbol.isEmpty()) null else Piece.fromSymbol(pieceSymbol)
        return Board(newSquares)
    }


    fun toFen(sideToMove: String = "w"): String {
        val sb = StringBuilder()
        for (row in 0..7) {
            var emptyCount = 0
            for (col in 0..7) {
                val piece = squares[row][col]
                if (piece == null) emptyCount++
                else {
                    if (emptyCount > 0) { sb.append(emptyCount); emptyCount = 0 }
                    sb.append(piece.symbol)
                }
            }
            if (emptyCount > 0) sb.append(emptyCount)
            if (row < 7) sb.append("/")
        }
        return "${sb.toString()} $sideToMove $castlingRights - 0 1"
    }

    fun movePieceIfValid(from: Pair<Int, Int>, to: Pair<Int, Int>): Board {
        val newBoard = copy()
        val piece = newBoard[from.first][from.second]

        newBoard.castlingRights = updateCastlingRights(from, to, piece, newBoard.castlingRights)

        newBoard.squares[from.first][from.second] = null
        newBoard.squares[to.first][to.second] = piece
        return newBoard
    }

    private fun updateCastlingRights(from: Pair<Int, Int>, to: Pair<Int, Int>, piece: Piece?, currentRights: String): String {
        if (currentRights == "-") return "-"
        var newRights = currentRights

        if (piece?.type == 'k') {
            newRights = if (piece.isWhite) {
                newRights.replace("K", "").replace("Q", "")
            } else {
                newRights.replace("k", "").replace("q", "")
            }
        }

        val rookSquares = mapOf(
            Pair(7, 7) to "K", Pair(7, 0) to "Q",
            Pair(0, 7) to "k", Pair(0, 0) to "q"
        )

        rookSquares[from]?.let { newRights = newRights.replace(it, "") }
        rookSquares[to]?.let { newRights = newRights.replace(it, "") }

        return if (newRights.isEmpty()) "-" else newRights
    }

    fun copy(): Board {
        val newSquares = Array(8) { row -> squares[row].copyOf() }
        return Board(newSquares, castlingRights)
    }

    companion object {
        fun parseFEN(fen: String): Board {
            val parts = fen.split(" ")
            val boardArray = Array(8) { arrayOfNulls<Piece>(8) }
            val position = parts[0]

            val rights = if (parts.size > 2 && parts[2] != "") parts[2] else "-"

            val rows = position.split("/")
            for (row in 0 until rows.size.coerceAtMost(8)) {
                var col = 0
                for (char in rows[row]) {
                    if (char.isDigit()) col += char.digitToInt()
                    else {
                        if (col < 8) {
                            boardArray[row][col] = Piece.fromSymbol(char.toString())
                            col++
                        }
                    }
                }
            }
            return Board(boardArray, rights)
        }
    }
}

data class Piece(
    val type: Char, // 'p', 'n', 'b', 'r', 'q', 'k'
    val isWhite: Boolean
) {
    val symbol: String
        get() = if (isWhite) type.uppercaseChar().toString() else type.lowercaseChar().toString()

    companion object {
        fun fromSymbol(s: String): Piece? {
            val char = s.firstOrNull() ?: return null
            if (char.isDigit()) return null
            return Piece(
                type = char.lowercaseChar(),
                isWhite = char.isUpperCase()
            )
        }
    }
}