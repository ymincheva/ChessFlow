package com.chessflow.jni.repositories

import android.util.Log
import com.chessflow.jni.models.Puzzle
import com.chessflow.jni.models.SourceGame
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PuzzleRepository @Inject constructor() {

    private val TAG = "PuzzleRepo"
    private val cache = mutableMapOf<String, List<Puzzle>>() // difficulty -> puzzles

    private fun refFor(difficulty: String) =
        FirebaseDatabase.getInstance().getReference("puzzles_final/$difficulty")

    private suspend fun loadPuzzles(difficulty: String): List<Puzzle> {
        if (cache.containsKey(difficulty)) return cache[difficulty]!!

        val snapshot = refFor(difficulty).get().await()
        val puzzles = mutableListOf<Puzzle>()

        for (child in snapshot.children) {
            try {
                val puzzleId = child.child("puzzle_id").getValue(Long::class.java)
                    ?: child.child("puzzleId").getValue(Long::class.java)
                    ?: child.child("puzzle_id").getValue(Int::class.java)?.toLong()
                    ?: child.child("puzzleId").getValue(Int::class.java)?.toLong()
                    ?: 0L

                val fen = child.child("fen").getValue(String::class.java) ?: ""

                // Best move
                val bestMove = child.child("bestMove").getValue(String::class.java)
                    ?: child.child("best_move").getValue(String::class.java) ?: ""
                val bestMoveSan = child.child("bestMoveSan").getValue(String::class.java)
                    ?: child.child("best_move_san").getValue(String::class.java) ?: ""
                val bestMoveEvalCp = child.child("bestMoveEvalCp").getValue(Int::class.java)
                    ?: child.child("best_move_eval_cp").getValue(Int::class.java) ?: 0

                // Second best move
                val secondBestMove = child.child("secondBestMove").getValue(String::class.java)
                    ?: child.child("second_best_move").getValue(String::class.java) ?: ""
                val secondBestSan = child.child("secondBestSan").getValue(String::class.java)
                    ?: child.child("second_best_san").getValue(String::class.java) ?: ""
                val secondBestEvalCp = child.child("secondBestEvalCp").getValue(Int::class.java)
                    ?: child.child("second_best_eval_cp").getValue(Int::class.java) ?: 0

                // Played move
                val playedMove = child.child("playedMove").getValue(String::class.java)
                    ?: child.child("played_move").getValue(String::class.java) ?: ""
                val playedMoveSan = child.child("playedMoveSan").getValue(String::class.java)
                    ?: child.child("played_move_san").getValue(String::class.java) ?: ""
                val playedMoveEvalCp = child.child("playedMoveEvalCp").getValue(Int::class.java)
                    ?: child.child("played_move_eval_cp").getValue(Int::class.java) ?: 0

                // Evaluations
                val evalDiff = child.child("evalDifference").getValue(Int::class.java)
                    ?: child.child("eval_difference").getValue(Int::class.java) ?: 0

                // Metadata
                val difficultyVal = child.child("difficulty").getValue(String::class.java) ?: ""
                val moveNumber = child.child("moveNumber").getValue(Int::class.java)
                    ?: child.child("move_number").getValue(Int::class.java) ?: 0
                val puzzleType = child.child("puzzleType").getValue(String::class.java)
                    ?: child.child("puzzle_type").getValue(String::class.java) ?: ""
                val sideToMove = child.child("sideToMove").getValue(String::class.java)
                    ?: child.child("side_to_move").getValue(String::class.java) ?: ""

                // Source game
                val sgNode = child.child("sourceGame")
                val sourceGame = if (sgNode.exists()) {
                    SourceGame(
                        White = sgNode.child("White").getValue(String::class.java)
                            ?: sgNode.child("white").getValue(String::class.java) ?: "",
                        Black = sgNode.child("Black").getValue(String::class.java)
                            ?: sgNode.child("black").getValue(String::class.java) ?: "",
                        Event = sgNode.child("Event").getValue(String::class.java)
                            ?: sgNode.child("event").getValue(String::class.java) ?: "",
                        Date = sgNode.child("Date").getValue(String::class.java)
                            ?: sgNode.child("date").getValue(String::class.java) ?: ""
                    )
                } else SourceGame()

                puzzles.add(
                    Puzzle(
                        puzzleId = puzzleId,
                        fen = fen,

                        bestMove = bestMove,
                        bestMoveSan = bestMoveSan,
                        bestMoveEvalCp = bestMoveEvalCp,

                        playedMove = playedMove,
                        playedMoveSan = playedMoveSan,
                        playedMoveEvalCp = playedMoveEvalCp,

                        evalDifference = evalDiff,
                        difficulty = difficultyVal,
                        moveNumber = moveNumber,
                        puzzleType = puzzleType,
                        sideToMove = sideToMove,
                        sourceGame = sourceGame
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse child ${child.key}: ${e.message}")
            }
        }

        val sorted = puzzles.sortedBy { it.puzzleId }
        Log.d(
            TAG,
            "Loaded ${sorted.size} puzzles for '$difficulty'. ids=${sorted.map { it.puzzleId }}"
        )
        cache[difficulty] = sorted
        return sorted
    }


    private suspend fun getSolvedPuzzleIds(userId: String, difficulty: String): Set<Long> {
        return try {
            val snapshot = FirebaseDatabase.getInstance()
                .getReference("users/$userId/solved_puzzles")
                .get()
                .await()

            snapshot.children
                .mapNotNull { child ->
                    val key = child.key ?: return@mapNotNull null
                    if (key.startsWith("${difficulty}_")) {
                        key.substringAfter("${difficulty}_").toLongOrNull()
                    } else {
                        null
                    }
                }.toSet()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching solved puzzles", e)
            emptySet()
        }
    }

    suspend fun loadUnsolvedPuzzles(userId: String, difficulty: String): List<Puzzle> {
        val allPuzzles = loadPuzzles(difficulty)

        val solvedIds = getSolvedPuzzleIds(userId,difficulty)

        val unsolved = allPuzzles.filter { it.puzzleId !in solvedIds }

        Log.d(TAG, "User $userId has ${unsolved.size} unsolved puzzles out of ${allPuzzles.size}")
        return unsolved
    }

    fun markPuzzleAsSolved(userId: String, puzzleId: Long, currentDifficulty: String) {
        val database = FirebaseDatabase.getInstance()

        val uniqueKey = "${currentDifficulty}_$puzzleId"

        val solvedRef = database.getReference("users")
            .child(userId)
            .child("solved_puzzles")
            .child(uniqueKey)

        val data = mapOf(
            "timestamp" to ServerValue.TIMESTAMP,
            "difficulty" to currentDifficulty,
            "puzzleId" to puzzleId
        )

        solvedRef.setValue(data)
    }

    suspend fun getUserStats(userId: String): Map<String, Int> {
        return try {
            val snapshot = FirebaseDatabase.getInstance()
                .getReference("users/$userId/solved_puzzles")
                .get()
                .await()

            val stats = mutableMapOf(
                "very_easy" to 0,
                "easy" to 0,
                "medium" to 0,
                "hard" to 0,
                "very_hard" to 0
            )

            for (child in snapshot.children) {
                val diff = child.child("difficulty").getValue(String::class.java)
                if (diff != null && stats.containsKey(diff)) {
                    stats[diff] = stats[diff]!! + 1
                }
            }
            stats
        } catch (e: Exception) {
            Log.e("Repo", "Error fetching stats", e)
            emptyMap()
        }
    }
}
