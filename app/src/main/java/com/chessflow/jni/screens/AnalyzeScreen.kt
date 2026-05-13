package com.chessflow.jni.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chessflow.jni.R
import com.chessflow.jni.ui.ChessBoardUI
import com.chessflow.jni.ui.PieceCorrectionDialog
import com.chessflow.jni.ui.SingleSideToggle
import com.chessflow.jni.viewModels.AnalyzeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen(
    viewModel: AnalyzeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.confirm_clear_title)) },
            text = { Text(stringResource(R.string.confirm_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setupEmptyBoard()
                        showClearDialog = false
                    }
                ) {
                    Text(
                        stringResource(R.string.action_clear),
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cansel))
                }
            }
        )
    }

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

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.olive).copy(alpha = 0.1f)
                ),
                border = BorderStroke(1.dp, colorResource(R.color.olive).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = colorResource(R.color.olive)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.ai_board_recognition_is_coming),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorResource(R.color.brown)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (state.boardFen.isNotEmpty()) {
                SelectionContainer {
                    Text(
                        text = "FEN: ${state.boardFen}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(
                                start = 4.dp,
                                end = 4.dp,
                                bottom = 8.dp
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        softWrap = false,
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                }
            }

            if (state.infoMessage.isNotEmpty()) {
                Text(
                    text = state.infoMessage,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorResource(R.color.olive),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic
                )
            }

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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        FilledIconButton(
                            onClick = { viewModel.toggleEraserMode() },
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (state.isEraserMode) {
                                    Color.Red.copy(alpha = 0.8f)
                                } else {
                                    colorResource(R.color.olive).copy(alpha = 0.8f)
                                }
                            )
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_eraser),
                                contentDescription = "Eraser Mode",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        FilledIconButton(
                            onClick = { viewModel.toggleBoardFlip() },
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colorResource(R.color.olive).copy(alpha = 0.8f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = "Flip Board",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = { viewModel.resetToStartPosition() },
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colorResource(R.color.olive).copy(alpha = 0.8f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = "Reset to Start",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(Modifier.width(8.dp))


                        FilledIconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colorResource(R.color.olive).copy(alpha = 0.8f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Board",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val squareSize = maxWidth / 10
                        ChessBoardUI(
                            board = state.board,
                            correctMove = state.bestMove,
                            selectedSquare = state.squareToEdit,
                            showAnswer = true,
                            isFlipped = state.isBoardFlipped,
                            isEditable = true,
                            isEraserMode = state.isEraserMode,
                            onDeletePiece = { r, c -> viewModel.deletePieceAt(r, c) },
                            onSquareClick = { row, col ->
                                viewModel.startCorrectionMode(row, col)
                            },
                            onMoveAttempt = { from, to ->
                                viewModel.movePiece(from, to)
                            },
                            squareSize = squareSize
                        )
                    }

                    val currentSide = if (state.boardFen.contains(" w ")) "w" else "b"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SingleSideToggle(
                            currentSide = currentSide,
                            onSideChange = {
                                val nextSide = if (currentSide == "w") 'b' else 'w'
                                viewModel.updateSideToMove(nextSide)
                            }
                        )
                    }
                }
            }


            if (state.isCorrectionMode && state.squareToEdit != null) {
                val (r, c) = state.squareToEdit!!
                PieceCorrectionDialog(
                    currentPiece = state.board[r][c]?.symbol ?: "",
                    onPieceSelected = { symbol -> viewModel.setPieceAt(symbol) },
                    onDismiss = { viewModel.stopCorrectionMode() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.analyzePosition(2000) },
                enabled = !state.isAnalyzing && state.boardFen.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.olive))
            ) {
                if (state.isAnalyzing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = state.statusMessage, fontSize = 14.sp)
                    }
                } else {
                    Text(
                        stringResource(R.string.analyze_position),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.moss_dark).copy(
                        alpha = 0.05f
                    )
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(
                            R.string.evaluation,
                            state.evaluation.ifEmpty { "–" }),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorResource(R.color.moss_dark),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            R.string.best_move,
                            state.bestMoveUci.ifEmpty { "–" }),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorResource(R.color.moss_dark),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
