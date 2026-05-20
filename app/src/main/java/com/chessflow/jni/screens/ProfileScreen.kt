package com.chessflow.jni.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chessflow.jni.R
import com.chessflow.jni.ui.ConnectivityBanner
import com.chessflow.jni.viewModels.ProfileViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.chessflow.jni.viewModels.AuthViewModel

val totalPuzzles = mapOf(
    "easy" to 343,
    "medium" to 645,
    "hard" to 264,
    "very_hard" to 1058
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onAccountDeleted: () -> Unit
) {
    val context = LocalContext.current

    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    val user = viewModel.user

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val userState by authViewModel.user.collectAsState()

    if (userState == null) {
        return
    }

    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = stringResource(R.string.delete_account),
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.brown)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_account_confirmation_message),
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteAccount(
                            onSuccess = {
                                authViewModel.logout(context)
                                onAccountDeleted()
                            },
                            onError = { resId ->
                                val message = context.getString(resId)
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            }
                        )
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel), color = Color.Gray)
                }
            },
            containerColor = colorResource(id = R.color.vanilla_paper),
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.player_profile),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            ConnectivityBanner(isOnline = isOnline)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = user?.displayName ?: "Chess Player",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.brown)
                )

                user?.email?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.masterclass_progress),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    color = colorResource(id = R.color.brown)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = colorResource(id = R.color.moss_dark))
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val filteredStats = stats.toList().filter { (difficulty, _) ->
                                totalPuzzles.containsKey(difficulty.lowercase())
                            }

                            items(filteredStats.size) { index ->
                                val (difficulty, count) = filteredStats[index]
                                val total = totalPuzzles[difficulty.lowercase()] ?: 0
                                StatRow(difficulty, count, total)
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.delete_account),
                        color = Color.Red.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun StatRow(difficulty: String, solvedCount: Int, totalCount: Int) {
    val remaining = (totalCount - solvedCount).coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.vanilla_paper)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = difficulty.replace("_", " ").uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.brown)
                )
                Text(
                    text = "Solved: $solvedCount / $totalCount",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                if (remaining > 0) {
                    Text(
                        stringResource(
                            R.string.remaining_to_master,
                            remaining
                        ),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        stringResource(
                            R.string.level_completed
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.moss_dark)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (remaining == 0) Color(0xFFFFD700) else colorResource(id = R.color.moss_dark),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}