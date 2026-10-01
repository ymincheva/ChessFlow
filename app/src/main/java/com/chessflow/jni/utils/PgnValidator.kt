package com.chessflow.jni.utils

object PgnValidator {

    /**
     * Validates if a given string resembles a valid PGN format.
     */
    fun isValidPgn(pgn: String): Boolean {
        val trimmed = pgn.trim()
        if (trimmed.isEmpty()) return false

        val containsTags = trimmed.contains("[") && trimmed.contains("]")
        val containsMoveNotation = trimmed.contains("1.") || trimmed.contains("1...")

        if (!containsTags && !containsMoveNotation) {
            return false
        }

        var openBrackets = 0
        for (char in trimmed) {
            if (char == '[') openBrackets++
            if (char == ']') openBrackets--
            if (openBrackets < 0) return false
        }
        if (openBrackets != 0) return false

        return true
    }
}