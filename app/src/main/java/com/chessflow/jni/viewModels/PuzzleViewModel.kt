package com.chessflow.jni.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chessflow.jni.R
import com.chessflow.jni.data.PuzzlePreferences
import com.chessflow.jni.models.Puzzle
import com.chessflow.jni.repositories.PuzzleRepository
import com.chessflow.jni.utils.Board
import com.chessflow.jni.utils.NetworkObserver
import com.chessflow.jni.utils.UiText
import com.chessflow.jni.utils.uciToPair
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PuzzleViewModel @Inject constructor(
    private val repository: PuzzleRepository,
    val prefs: PuzzlePreferences,
    networkObserver: NetworkObserver
) : ViewModel() {

    // --- UI States ---
    private val _nextPuzzle = MutableStateFlow<Puzzle?>(null)
    val nextPuzzle = _nextPuzzle.asStateFlow()

    private val _board = MutableStateFlow(Board.parseFEN("8/8/8/8/8/8/8/8"))
    val board = _board.asStateFlow()

    private val _selectedSquare = MutableStateFlow<Pair<Int, Int>?>(null)
    val selectedSquare = _selectedSquare.asStateFlow()

    private val _message =
        MutableStateFlow<UiText>(UiText.ResourceString(R.string.only_the_move_is_allowed))
    val message = _message.asStateFlow()

    private val _showAnswer = MutableStateFlow(false)
    val showAnswer = _showAnswer.asStateFlow()

    private val _error = MutableStateFlow<UiText?>(null)
    val error = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // --- Internal Logic ---
    private var unsolvedPuzzles: List<Puzzle> = emptyList()
    private var currentIndex = 0
    private var currentDifficulty: String = "easy"
    private val auth = FirebaseAuth.getInstance()

    private val _wrongAttempts = MutableStateFlow(0)
    val wrongAttempts: StateFlow<Int> = _wrongAttempts

    private val _lastMoveArg = MutableStateFlow<String?>(null)
    val lastMoveArg = _lastMoveArg.asStateFlow()

    private var isAnswerRevealedForCurrentPuzzle = false

    val isOnline = networkObserver.observe.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    // --- Search / Filter State ---
    private val _activePlayerQuery = MutableStateFlow<String?>(null)
    val activePlayerQuery = _activePlayerQuery.asStateFlow()

    fun loadCurrentPuzzles() {
        viewModelScope.launch {
            val lastDiff = prefs.lastDifficulty.firstOrNull() ?: "easy"
            currentDifficulty = lastDiff
            fetchAndFilterPuzzles()
        }
        /*     viewModelScope.launch {
                 _isLoading.value = true
                 _error.value = null
                 val userId = auth.currentUser?.uid ?: return@launch

                 try {
                     val lastDiff = prefs.lastDifficulty.firstOrNull() ?: "easy"
                     currentDifficulty = lastDiff

                     allPuzzles = repository.loadUnsolvedPuzzles(userId, currentDifficulty)

                     applyPlayerFilter()

                    *//* unsolvedPuzzles = repository.loadUnsolvedPuzzles(userId, currentDifficulty)

                if (unsolvedPuzzles.isNotEmpty()) {
                    currentIndex = 0
                    updateUIWithCurrentPuzzle()
                } else {
                    _nextPuzzle.value = null
                    _error.value = UiText.ResourceString(R.string.puzzles_solved)
                }*//*
            } catch (e: Exception) {
                _error.value = UiText.ResourceString(R.string.error_loading)

            } finally {
                _isLoading.value = false
            }
        }*/
    }

    fun onDifficultyChanged(newDifficulty: String) {
        viewModelScope.launch {
            currentDifficulty = newDifficulty
            prefs.saveDifficulty(newDifficulty)
            _activePlayerQuery.value = null
            loadCurrentPuzzles()
        }
    }

    private fun updateUIWithCurrentPuzzle() {
        if (currentIndex in unsolvedPuzzles.indices) {
            val puzzle = unsolvedPuzzles[currentIndex]

            _nextPuzzle.value = puzzle

            _board.value = Board.parseFEN(puzzle.fen)
            _showAnswer.value = false
            _message.value = UiText.ResourceString(R.string.only_the_move_is_allowed)
            _selectedSquare.value = null
            _lastMoveArg.value = null
            _wrongAttempts.value = 0
            isAnswerRevealedForCurrentPuzzle = false
            startPuzzleAnimation(puzzle)
        }
    }

    private fun startPuzzleAnimation(puzzle: Puzzle) {
        viewModelScope.launch {
            delay(800)

            try {
                val (from, to) = uciToPair(puzzle.playedMove)
                val originalBoard = _board.value

                _board.value = originalBoard.movePieceIfValid(from, to)
                _lastMoveArg.value = puzzle.playedMove
                _message.value = UiText.ResourceString(R.string.opponent_played_this)

                delay(1500)

                _board.value = originalBoard

                _message.value = if (puzzle.sideToMove == "white")
                    UiText.ResourceString(R.string.white_to_move) else UiText.ResourceString(R.string.black_to_move)

            } catch (e: Exception) {
                Log.e("PuzzleViewModel", "Error in animation: ${e.message}")
            }
        }
    }

    fun hasNextPuzzle(): Boolean = currentIndex < unsolvedPuzzles.size - 1

    fun hasPreviousPuzzle(): Boolean = currentIndex > 0

    fun loadNextPuzzle() {
        if (hasNextPuzzle()) {
            currentIndex++
            updateUIWithCurrentPuzzle()
        }
    }

    fun loadPreviousPuzzle() {
        if (hasPreviousPuzzle()) {
            currentIndex--
            updateUIWithCurrentPuzzle()
        }
    }

    fun reloadCurrentPuzzle() {
        updateUIWithCurrentPuzzle()
    }

    fun toggleShowAnswer() {
        _showAnswer.value = !_showAnswer.value
        if (_showAnswer.value) {
            isAnswerRevealedForCurrentPuzzle = true
            _message.value = UiText.ResourceString(R.string.answer_revealed_warning)
            _lastMoveArg.value = null
        }
    }

    fun onSquareClick(row: Int, col: Int) {
        val from = _selectedSquare.value
        if (from == null) {
            _selectedSquare.value = Pair(row, col)
        } else {
            val to = Pair(row, col)

            if (checkMove(from, to)) {
                handleCorrectMove(from, to)
            } else {
                handleWrongMove()
            }
            _selectedSquare.value = null
        }
    }

    private fun handleCorrectMove(from: Pair<Int, Int>, to: Pair<Int, Int>) {
        _board.value = _board.value.movePieceIfValid(from, to)
        _wrongAttempts.value = 0

        val userId = auth.currentUser?.uid
        val puzzle = _nextPuzzle.value

        val targetMessage = if (isAnswerRevealedForCurrentPuzzle) {
            UiText.ResourceString(R.string.correct_move_but_revealed)
        } else {
            if (userId != null && puzzle != null) {
                repository.markPuzzleAsSolved(userId, puzzle.puzzleId, currentDifficulty)
            }
            UiText.ResourceString(R.string.correct_move)
        }

        _message.value = targetMessage
    }

    private fun handleWrongMove() {
        _message.value = UiText.ResourceString(R.string.wrong_move)
        _wrongAttempts.value += 1
    }

    private fun checkMove(from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        val puzzle = _nextPuzzle.value ?: return false

        try {
            val fromSquare = puzzle.bestMove.substring(0, 2)
            val toSquare = puzzle.bestMove.substring(2, 4)

            val correctFrom = com.chessflow.jni.utils.squareToIndex(fromSquare)
            val correctTo = com.chessflow.jni.utils.squareToIndex(toSquare)

            return from == correctFrom && to == correctTo
        } catch (e: Exception) {
            Log.e("PuzzleViewModel", "Error parsing bestMove: ${e.message}")
            return false
        }
    }

    fun onMoveAttempt(from: Pair<Int, Int>, to: Pair<Int, Int>) {
        if (checkMove(from, to)) {
            handleCorrectMove(from, to)
        } else {
            handleWrongMove()
        }
        _selectedSquare.value = null
    }

    /**
     * Search for puzzles by chess player's name (White or Black player).
     */
    fun searchPuzzlesByPlayer(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            clearPlayerFilter()
            return
        }

        _activePlayerQuery.value = trimmedQuery
        fetchAndFilterPuzzles()
    }

    /**
     * Removes the player filter and restores all puzzles..
     */
    fun clearPlayerFilter() {
        _activePlayerQuery.value = null
        fetchAndFilterPuzzles()
    }

    /**
     * Filters unsolved puzzles by the specified player's name
     */
    private fun fetchAndFilterPuzzles() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val userId = auth.currentUser?.uid ?: run {
                _isLoading.value = false
                return@launch
            }

            try {
                val freshUnsolved = repository.loadUnsolvedPuzzles(userId, currentDifficulty)

                val query = _activePlayerQuery.value

                unsolvedPuzzles = if (!query.isNullOrBlank()) {
                    freshUnsolved.filter { puzzle ->
                        puzzle.sourceGame.White.contains(query, ignoreCase = true) ||
                                puzzle.sourceGame.Black.contains(query, ignoreCase = true)
                    }
                } else {
                    freshUnsolved
                }


                if (unsolvedPuzzles.isNotEmpty()) {
                    currentIndex = 0
                    updateUIWithCurrentPuzzle()
                } else {
                    _nextPuzzle.value = null
                    if (freshUnsolved.isEmpty()) {
                        _error.value = UiText.ResourceString(R.string.puzzles_solved)
                    } else {
                       _error.value = UiText.ResourceString(R.string.no_puzzles_found_for_player)
                    }
                }
            } catch (e: Exception) {
                _error.value = UiText.ResourceString(R.string.error_loading)
            } finally {
                _isLoading.value = false
            }
        }
    }

}