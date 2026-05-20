package com.chessflow.jni.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.chessflow.jni.R
import com.chessflow.jni.utils.changeLanguage
import com.chessflow.jni.viewModels.AuthViewModel

@Composable
fun MainApp(authViewModel: AuthViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val user by authViewModel.user.collectAsState()

    val startDest = remember(user) { if (user == null) "login" else "home" }

    LaunchedEffect(user) {
        if (user == null) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            if (navController.currentBackStackEntry?.destination?.route == "login") {
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDest) {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                }
            )
        }
        composable("home") {
            HomeScreen(navController, authViewModel)
        }
        composable("puzzles") { PuzzleScreen() }
        composable("analyze") { AnalyzeScreen() }
        composable("profile") {
            ProfileScreen(
                onAccountDeleted = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("about") { AboutScreen() }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, authViewModel: AuthViewModel) {
    val olive = colorResource(id = R.color.olive)
    val mossDark = colorResource(id = R.color.moss_dark)
    var showLanguageMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showLanguageMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Change Language",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        R.string.english
                                    )
                                )
                            },
                            onClick = {
                                changeLanguage("en")
                                showLanguageMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        R.string.german
                                    )
                                )
                            },
                            onClick = {
                                changeLanguage("de")
                                showLanguageMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        R.string.bulgarian
                                    )
                                )
                            },
                            onClick = {
                                changeLanguage("bg")
                                showLanguageMenu = false
                            }
                        )
                    }

                    IconButton(onClick = { authViewModel.logout(context) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = mossDark,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { navController.navigate("puzzles") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = olive)
            ) {
                Text(stringResource(R.string.puzzles))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("analyze") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = olive)
            ) {
                Text(stringResource(R.string.analyze))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("profile") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = olive)
            ) {
                Text(stringResource(R.string.profile))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("about") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = olive)
            ) {
                Text(stringResource(R.string.about))
            }


        }
    }
}
