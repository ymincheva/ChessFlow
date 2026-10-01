package com.chessflow.jni.validators

import com.chessflow.jni.utils.Board
import com.chessflow.jni.utils.FenValidator
import com.chessflow.jni.utils.PgnValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying FEN, PGN, and Board validation logic.
 */
class ChessValidationTest {

    // --- FEN TESTS ---

    @Test
    fun `valid standard start FEN returns true`() {
        val validFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        assertTrue(FenValidator.isValidFen(validFen))
    }

    @Test
    fun `invalid FEN missing UCI parameters returns false`() {
        val invalidFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"
        assertFalse(FenValidator.isValidFen(invalidFen))
    }

    @Test
    fun `invalid FEN with row length exceeding 8 squares returns false`() {
        val invalidFen = "rnbqkbnr/pppppppp/9/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        assertFalse(FenValidator.isValidFen(invalidFen))
    }

    @Test
    fun `invalid FEN with unknown castling characters returns false`() {
        val invalidFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w X%# - 0 1"
        assertFalse(FenValidator.isValidFen(invalidFen))
    }

    // --- PGN TESTS ---

    @Test
    fun `valid PGN format returns true`() {
        val validPgn = """
            [Event "Casual Game"]
            1. e4 e5 2. Nf3 Nc6 3. Bb5
        """.trimIndent()

        assertTrue(PgnValidator.isValidPgn(validPgn))
    }

    @Test
    fun `invalid PGN without moves or tags returns false`() {
        val invalidPgn = "Hello World, this is not a chess game!"
        assertFalse(PgnValidator.isValidPgn(invalidPgn))
    }

    @Test
    fun `invalid PGN with unbalanced brackets returns false`() {
        val invalidPgn = "[Event \"Test Game\" 1. e4 e5"
        assertFalse(PgnValidator.isValidPgn(invalidPgn))
    }

    // --- BOARD KING VALIDATION TESTS ---

    @Test
    fun `board with missing black king returns false`() {
        // FEN with black king 'k' replaced by knight 'n' (rnbqnnnr instead of rnbqkbnr)
        val missingKingFen = "rnbqnnnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val board = Board.parseFEN(missingKingFen)

        assertFalse(board.hasValidKings())
    }
}