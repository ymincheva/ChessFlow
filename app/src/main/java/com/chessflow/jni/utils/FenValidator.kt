package com.chessflow.jni.utils

import java.util.regex.Pattern

/**
 * Utility object providing FEN validation using Regex and structural rules.
 */
object FenValidator {

    // Regex matching standard FEN structure
    private val FEN_REGEX = Pattern.compile(
        "^([rnbqkpRNBQKP1-8]{1,8}/){7}[rnbqkpRNBQKP1-8]{1,8}\\s+[wb]\\s+([KQkq]+|-)\\s+([a-h][36]|-)\\s+\\d+\\s+\\d+$"
    )

    /**
     * Validates both structure and board dimensions of a FEN string.
     */
    fun isValidFen(fen: String): Boolean {
        val cleanFen = fen.trim()
        if (cleanFen.isEmpty()) return false

        // 1. Basic structural regex check
        if (!FEN_REGEX.matcher(cleanFen).matches()) {
            return false
        }

        // 2. Validate board row character totals (each row must sum to exactly 8 squares)
        val boardPart = cleanFen.split(" ")[0]
        val rows = boardPart.split("/")

        if (rows.size != 8) return false

        for (row in rows) {
            var squareCount = 0
            for (char in row) {
                if (char.isDigit()) {
                    squareCount += char.digitToInt()
                } else if ("rnbqkpRNBQKP".contains(char)) {
                    squareCount++
                } else {
                    return false
                }
            }
            if (squareCount != 8) return false
        }

        return true
    }
}