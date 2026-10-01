package com.chessflow.jni.utils

import android.util.Log

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
        return Board(newSquares, castlingRights)
    }

    fun withPiece(row: Int, col: Int, pieceSymbol: String): Board {
        val newSquares = Array(8) { r -> squares[r].copyOf() }
        newSquares[row][col] = if (pieceSymbol.isEmpty()) null else Piece.fromSymbol(pieceSymbol)
        return Board(newSquares, castlingRights)
    }

    fun toFen(sideToMove: String = "w"): String {
        val sb = StringBuilder()
        for (row in 0..7) {
            var emptyCount = 0
            for (col in 0..7) {
                val piece = squares[row][col]
                if (piece == null) emptyCount++
                else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount); emptyCount = 0
                    }
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

    private fun updateCastlingRights(
        from: Pair<Int, Int>,
        to: Pair<Int, Int>,
        piece: Piece?,
        currentRights: String
    ): String {
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

    fun hasValidKings(): Boolean {
        var whiteKings = 0
        var blackKings = 0

        for (row in 0..7) {
            for (col in 0..7) {
                val piece = squares[row][col]
                if (piece?.type == 'k') {
                    if (piece.isWhite) whiteKings++ else blackKings++
                }
            }
        }

        return whiteKings == 1 && blackKings == 1
    }

    data class ParsedPgnResult(
        val fenList: List<String>,
        val sanMoves: List<String>
    )

    companion object {
        const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        fun startPosition(): Board {
            return parseFEN(START_FEN)
        }

        @Throws(IllegalArgumentException::class)
        fun fromFen(fen: String): Board {
            val board = parseFEN(fen)
            if (!board.hasValidKings()) {
                throw IllegalArgumentException("The position must contain exactly 1 white king and 1 black king.")
            }
            return board
        }

        fun parseFEN(fen: String): Board {
            val parts = fen.trim().split("\\s+".toRegex())
            val boardArray = Array(8) { arrayOfNulls<Piece>(8) }
            val position = parts[0]

            val rights = if (parts.size > 2 && parts[2].isNotBlank()) parts[2] else "-"

            val rows = position.split("/")
            require(rows.size == 8) { "FEN position must have exactly 8 ranks." }

            for (row in 0 until 8) {
                var col = 0
                for (char in rows[row]) {
                    if (char.isDigit()) {
                        col += char.digitToInt()
                    } else {
                        if (col < 8) {
                            boardArray[row][col] = Piece.fromSymbol(char.toString())
                            col++
                        }
                    }
                }
            }
            return Board(boardArray, rights)
        }

        fun parsePgnToFenList(pgn: String): ParsedPgnResult {
            val fenList = mutableListOf<String>()
            val sanList = mutableListOf<String>()

            var currentBoard = startPosition()
            var isWhiteTurn = true

            fenList.add(currentBoard.toFen(if (isWhiteTurn) "w" else "b"))

            val cleanMoves = pgn
                .replace(Regex("\\[.*?\\]"), "")
                .replace(Regex("\\{.*?\\}"), "")
                .replace(Regex(";.*"), "")
                .replace("0-0-0", "O-O-O")
                .replace("0-0", "O-O")
                .replace(Regex("\\d+\\.\\.\\."), "")
                .replace(Regex("\\d+\\."), "")
                .replace(Regex("[?!+#]"), "")
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() && it != "*" && !it.contains("1-0") && !it.contains("0-1") && !it.contains("1/2-1/2") }

            for (sanMove in cleanMoves) {
                try {
                    val nextBoard = applySanMove(currentBoard, sanMove, isWhiteTurn)
                    if (nextBoard != null) {
                        currentBoard = nextBoard
                        isWhiteTurn = !isWhiteTurn
                        fenList.add(currentBoard.toFen(if (isWhiteTurn) "w" else "b"))
                        sanList.add(sanMove)
                    } else {
                        Log.e("BoardPgn", "Failed to apply move: $sanMove at FEN: ${currentBoard.toFen(if (isWhiteTurn) "w" else "b")}")
                        break
                    }
                } catch (e: Exception) {
                    Log.e("BoardPgn", "Error applying move $sanMove: ${e.message}")
                    break
                }
            }

            return ParsedPgnResult(fenList = fenList, sanMoves = sanList)
        }

        private fun applySanMove(board: Board, san: String, isWhite: Boolean): Board? {
            val cleanSan = san.trim()

            if (cleanSan == "O-O" || cleanSan == "0-0") {
                val row = if (isWhite) 7 else 0
                return board.movePieceIfValid(Pair(row, 4), Pair(row, 6))
                    .movePieceIfValid(Pair(row, 7), Pair(row, 5))
            }
            if (cleanSan == "O-O-O" || cleanSan == "0-0-0") {
                val row = if (isWhite) 7 else 0
                return board.movePieceIfValid(Pair(row, 4), Pair(row, 2))
                    .movePieceIfValid(Pair(row, 0), Pair(row, 3))
            }

            val isCapture = cleanSan.contains("x")
            val move = cleanSan.replace("x", "")
            val targetSquareStr = move.takeLast(2)
            if (targetSquareStr.length < 2) return null

            val toCol = targetSquareStr[0] - 'a'
            val toRow = 8 - (targetSquareStr[1] - '0')
            if (toCol !in 0..7 || toRow !in 0..7) return null

            val pieceChar = if (move[0].isUpperCase()) move[0].lowercaseChar() else 'p'
            val specifier = if (move[0].isUpperCase()) {
                move.drop(1).dropLast(2)
            } else {
                move.dropLast(2)
            }

            for (r in 0..7) {
                for (c in 0..7) {
                    val p = board[r, c] ?: continue
                    if (p.isWhite == isWhite && p.type == pieceChar) {
                        if (specifier.isNotEmpty()) {
                            if (specifier.length == 1) {
                                if (specifier[0].isLetter() && (specifier[0] - 'a') != c) continue
                                if (specifier[0].isDigit() && (8 - (specifier[0] - '0')) != r) continue
                            }
                        }

                        if (isValidCandidateMove(board, Pair(r, c), Pair(toRow, toCol), p, isCapture)) {
                            return board.movePieceIfValid(Pair(r, c), Pair(toRow, toCol))
                        }
                    }
                }
            }
            return null
        }

        private fun isValidCandidateMove(
            board: Board,
            from: Pair<Int, Int>,
            to: Pair<Int, Int>,
            piece: Piece,
            isCapture: Boolean
        ): Boolean {
            val dr = to.first - from.first
            val dc = to.second - from.second

            return when (piece.type) {
                'p' -> {
                    val dir = if (piece.isWhite) -1 else 1
                    if (!isCapture && dc == 0 && dr == dir && board[to.first, to.second] == null) {
                        true
                    } else if (!isCapture && dc == 0 && dr == 2 * dir &&
                        ((piece.isWhite && from.first == 6) || (!piece.isWhite && from.first == 1)) &&
                        board[to.first, to.second] == null && board[from.first + dir, from.second] == null) {
                        true
                    } else if (isCapture && Math.abs(dc) == 1 && dr == dir) {
                        true
                    } else {
                        false
                    }
                }

                'n' -> (Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2)
                'b' -> Math.abs(dr) == Math.abs(dc)
                'r' -> dr == 0 || dc == 0
                'q' -> Math.abs(dr) == Math.abs(dc) || dr == 0 || dc == 0
                'k' -> Math.abs(dr) <= 1 && Math.abs(dc) <= 1
                else -> false
            }
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