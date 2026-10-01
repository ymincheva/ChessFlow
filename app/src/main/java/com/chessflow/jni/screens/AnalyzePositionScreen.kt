package com.chessflow.jni.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chessflow.jni.R
import com.chessflow.jni.componets.AnalysisResultsCard
import com.chessflow.jni.componets.ChessInputField
import com.chessflow.jni.ui.ChessBoardUI
import com.chessflow.jni.viewModels.AnalyzePositionViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chessflow.jni.utils.UiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzePositionScreen(
    viewModel: AnalyzePositionViewModel = hiltViewModel()
) {
    // Observe screen state from ViewModel
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val infoMessageText = state.infoMessage.asString(context)

    // Local state for FEN and PGN input fields
    var fenInputText by remember(state.boardFen) { mutableStateOf(state.boardFen) }
    var pgnInputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.chess_position_analysis),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorResource(id = R.color.moss_dark)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(16.dp))

            // PGN Input Field
            ChessInputField(
                value = pgnInputText,
                onValueChange = { pgnInputText = it },
                labelRes = R.string.pgn_input_label,
                onSubmit = { pgn ->
                    if (pgn.isNotBlank()) {
                        viewModel.loadPgnGame(pgn)
                        keyboardController?.hide()
                    }
                }
            )
            Spacer(Modifier.height(8.dp))

            // FEN Input Field
            ChessInputField(
                value = fenInputText,
                onValueChange = { fenInputText = it },
                labelRes = R.string.fen_position_label,
                onSubmit = { fen ->
                    if (fen.isNotBlank()) {
                        viewModel.loadFenPosition(fen)
                        pgnInputText = ""
                        keyboardController?.hide()
                    }
                }
            )

            // Info or status message below inputs
            val infoMessage = state.infoMessage
            val infoMessageText = infoMessage.asString(context)

            if (infoMessageText.isNotBlank()) {
                val isError = infoMessage is UiText.ResourceString && (
                        infoMessage.resId == R.string.error_invalid_fen_format ||
                                infoMessage.resId == R.string.error_parsing_fen
                        )

                Surface(
                    color = if (isError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        colorResource(R.color.olive).copy(alpha = 0.12f)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = infoMessageText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            colorResource(R.color.olive)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Container card for the chessboard and navigation controls
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.vanilla_paper)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(4.dp))

                    // Chessboard rendering
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val squareSize = maxWidth / 9

                        // Clean board UI view without square highlights
                        ChessBoardUI(
                            board = state.board,
                            squareSize = squareSize,
                            correctMove = state.bestMove,
                            showAnswer = state.bestMove != null
                        )
                    }

                    // Move navigation controls (First, Previous, Next, Last)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // First Move Button
                        IconButton(
                            onClick = { viewModel.goToFirstMove() },
                            enabled = state.canGoPrevious
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = stringResource(R.string.first_move),
                                tint = if (state.canGoPrevious) colorResource(R.color.olive) else Color.Gray
                            )
                        }

                        // Previous Move Button
                        IconButton(
                            onClick = { viewModel.goToPreviousMove() },
                            enabled = state.canGoPrevious
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                                contentDescription = stringResource(R.string.previous_move),
                                tint = if (state.canGoPrevious) colorResource(R.color.olive) else Color.Gray
                            )
                        }

                        // Next Move Button
                        IconButton(
                            onClick = { viewModel.goToNextMove() },
                            enabled = state.canGoNext
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = stringResource(R.string.next_move),
                                tint = if (state.canGoNext) colorResource(R.color.olive) else Color.Gray
                            )
                        }

                        // Last Move Button
                        IconButton(
                            onClick = { viewModel.goToLastMove() },
                            enabled = state.canGoNext
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = stringResource(R.string.last_move),
                                tint = if (state.canGoNext) colorResource(R.color.olive) else Color.Gray
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

          /*  Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Action button: Get best move from Stockfish
                OutlinedButton(
                    onClick = { viewModel.analyzePosition(2000) },
                    enabled = !state.isAnalyzing && !state.isEvaluatingQuality && state.currentMoveIndex > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colorResource(id = R.color.olive)
                    )
                ) {
                    if (state.isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colorResource(id = R.color.olive)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.get_best_move),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Action button: Check quality of the current move
                Button(
                    onClick = { viewModel.evaluateCurrentMoveQuality() },
                    enabled = !state.isAnalyzing && !state.isEvaluatingQuality && state.currentMoveIndex > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.olive)
                    )
                ) {
                    if (state.isEvaluatingQuality) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.check_move_quality),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }*/

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Action button: Get best move from Stockfish
                val isGetBestMoveEnabled = !state.isAnalyzing &&
                        !state.isEvaluatingQuality &&
                        state.boardFen.isNotBlank()

                OutlinedButton(
                    onClick = { viewModel.analyzePosition(2000) },
                    enabled = isGetBestMoveEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colorResource(id = R.color.olive)
                    )
                ) {
                    if (state.isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colorResource(id = R.color.olive)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.get_best_move),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Action button: Check quality of the current move
                val isCheckQualityEnabled = !state.isAnalyzing &&
                        !state.isEvaluatingQuality &&
                        state.currentMoveIndex > 0

                Button(
                    onClick = { viewModel.evaluateCurrentMoveQuality() },
                    enabled = isCheckQualityEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.olive)
                    )
                ) {
                    if (state.isEvaluatingQuality) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.check_move_quality),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val moveText = viewModel.formatMoveText(state.currentMoveIndex)

            val qualityValue = state.moveQuality?.let { quality ->
                "${quality.emoji} ${stringResource(quality.labelRes)} ($moveText)"
            }

            AnalysisResultsCard(
                currentMoveIndex = state.currentMoveIndex,
                moveText = moveText,
                evaluation = state.evaluation,
                bestMoveUci = state.bestMoveUci,
                moveQualityText = qualityValue
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}