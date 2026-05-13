package com.chessflow.jni.screens

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chessflow.jni.R
import com.chessflow.jni.componets.PuzzleNavButton
import com.chessflow.jni.componets.SourceGameCard
import com.chessflow.jni.models.Move
import com.chessflow.jni.ui.AnswerCard
import com.chessflow.jni.ui.ChessBoardUI
import com.chessflow.jni.ui.ConnectivityBanner
import com.chessflow.jni.ui.SingleSideToggle
import com.chessflow.jni.utils.formatDate
import com.chessflow.jni.utils.translateDifficulty
import com.chessflow.jni.viewModels.PuzzleViewModel
import java.util.Locale

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(viewModel: PuzzleViewModel = hiltViewModel()) {
    val nextPuzzle by viewModel.nextPuzzle.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val board by viewModel.board.collectAsStateWithLifecycle()
    val selectedSquare by viewModel.selectedSquare.collectAsStateWithLifecycle()
    val showAnswer by viewModel.showAnswer.collectAsStateWithLifecycle()

    val difficulties = listOf("Easy", "Medium", "Hard", "Very Hard")
    var selectedIndex by remember { mutableStateOf(2) }
    val lastDifficulty by viewModel.prefs.lastDifficulty.collectAsStateWithLifecycle(initialValue = "medium")
    val errorResId by viewModel.error.collectAsStateWithLifecycle()

    val wrongAttempts by viewModel.wrongAttempts.collectAsStateWithLifecycle()
    val remainingAttempts = 3 - wrongAttempts
    val canShowAnswer = remainingAttempts <= 0

    val messageResId by viewModel.message.collectAsStateWithLifecycle()
    val moveArg by viewModel.lastMoveArg.collectAsStateWithLifecycle()

    LaunchedEffect(lastDifficulty) {
        val idx = difficulties.indexOfFirst {
            it.equals(lastDifficulty.replace("_", " "), ignoreCase = true)
        }
        if (idx != -1) selectedIndex = idx
    }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentPuzzles()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = buildString {
                            append(stringResource(R.string.chess_puzzle))
                            if (nextPuzzle != null) {
                                append(" • ${translateDifficulty(nextPuzzle!!.difficulty)}")
                            }
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorResource(id = R.color.moss_dark)
                )
            )
        }
    ) { innerPadding ->

        // Основният контейнер, който подрежда Банера и Съдържанието вертикално
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Проверка за интернет (Banner)
            ConnectivityBanner(isOnline = isOnline)

            // 2. Основно съдържание на екрана
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    // Случай 1: Зареждане
                    isLoading && nextPuzzle == null -> {
                        CircularProgressIndicator(color = colorResource(R.color.moss_dark))
                    }

                    // Случай 2: Грешка или липса на пъзели
                    (errorResId != null && nextPuzzle == null) -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colorResource(R.color.moss_dark),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(errorResId!!),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(24.dp))
                            DifficultySelector(viewModel, difficulties, selectedIndex) {
                                selectedIndex = it
                            }
                        }
                    }

                    // Случай 3: Има зареден пъзел
                    nextPuzzle != null -> {
                        val bestMoveStr = nextPuzzle!!.bestMove
                        val fromIndex =
                            com.chessflow.jni.utils.squareToIndex(bestMoveStr.substring(0, 2))
                        val toIndex =
                            com.chessflow.jni.utils.squareToIndex(bestMoveStr.substring(2, 4))
                        val correctMove = Move(fromIndex, toIndex)

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                DifficultySelector(viewModel, difficulties, selectedIndex) {
                                    selectedIndex = it
                                }
                            }

                            // ✅ Шахматна дъска с анимация
                            item {
                                AnimatedContent(
                                    targetState = nextPuzzle?.puzzleId,
                                    transitionSpec = {
                                        slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                                                slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                                    },
                                    label = "BoardTransition"
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = colorResource(
                                                R.color.vanilla_paper
                                            )
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                    ) {
                                        BoxWithConstraints(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val squareSize = maxWidth / 10
                                            ChessBoardUI(
                                                board = board,
                                                correctMove = correctMove,
                                                selectedSquare = selectedSquare,
                                                showAnswer = showAnswer,
                                                isEditable = false,
                                                isEraserMode = false,
                                                onDeletePiece = { _, _ -> },
                                                onSquareClick = { row, col ->
                                                    viewModel.onSquareClick(row, col)
                                                },
                                                onMoveAttempt = { from, to ->
                                                    viewModel.onMoveAttempt(from, to)
                                                },
                                                squareSize = squareSize
                                            )
                                        }
                                    }
                                }
                            }

                            // ✅ Контроли и Информация
                            item {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        SingleSideToggle(
                                            currentSide = if (nextPuzzle!!.sideToMove == "white") "w" else "b",
                                            onSideChange = {}
                                        )
                                    }

                                    Text(
                                        text = if (moveArg != null) stringResource(
                                            messageResId,
                                            moveArg!!
                                        ) else stringResource(messageResId),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )

                                    SourceGameCard(
                                        white = nextPuzzle!!.sourceGame.White,
                                        black = nextPuzzle!!.sourceGame.Black,
                                        event = nextPuzzle!!.sourceGame.Event,
                                        date = formatDate(nextPuzzle!!.sourceGame.Date)
                                    )

                                    // Навигационни бутони
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 12.dp)
                                    ) {
                                        PuzzleNavButton(
                                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                                            enabled = viewModel.hasPreviousPuzzle(),
                                            onClick = { viewModel.loadPreviousPuzzle() }
                                        )

                                        PuzzleNavButton(
                                            icon = Icons.Default.Refresh,
                                            enabled = true,
                                            onClick = { viewModel.reloadCurrentPuzzle() },
                                            outlined = true
                                        )

                                        OutlinedButton(
                                            onClick = { viewModel.toggleShowAnswer() },
                                            enabled = !isLoading && (canShowAnswer || showAnswer)
                                        ) {
                                            Text(
                                                if (showAnswer) stringResource(R.string.hide_answer) else if (canShowAnswer) stringResource(
                                                    R.string.show_answer
                                                ) else stringResource(
                                                    R.string.try_again_count,
                                                    remainingAttempts
                                                )
                                            )
                                        }

                                        PuzzleNavButton(
                                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                                            enabled = viewModel.hasNextPuzzle(),
                                            onClick = { viewModel.loadNextPuzzle() }
                                        )
                                    }
                                }
                            }

                            // ✅ Секция с отговори
                            if (showAnswer) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                colorResource(R.color.vanilla_paper),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AnswerCard(
                                            title = stringResource(R.string.puzzel_best_move),
                                            value = nextPuzzle!!.bestMoveSan
                                        )
                                        AnswerCard(
                                            title = stringResource(R.string.played_move),
                                            value = nextPuzzle!!.playedMove
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DifficultySelector(
    viewModel: PuzzleViewModel,
    difficulties: List<String>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.select_difficulty),
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.brown)
        )
        Spacer(Modifier.width(10.dp))
        Box {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colorResource(R.color.vanilla_paper),
                modifier = Modifier
                    .clickable { expanded = true }
                    .height(44.dp)
                    .widthIn(min = 140.dp),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        translateDifficulty(difficulties[selectedIndex]),
                        color = colorResource(R.color.brown)
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = colorResource(R.color.brown))
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                difficulties.forEachIndexed { index, diff ->
                    DropdownMenuItem(
                        text = { Text(translateDifficulty(diff)) },
                        onClick = {
                            onIndexChange(index)
                            expanded = false
                            viewModel.onDifficultyChanged(diff.lowercase().replace(" ", "_"))
                        }
                    )
                }
            }
        }
    }
}