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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.ui.draw.clip

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
    val mossDark = colorResource(id = R.color.moss_dark)
    val olive = colorResource(id = R.color.olive)
    var showLanguageMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val user by authViewModel.user.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showLanguageMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Change Language",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.english)) },
                            onClick = {
                                changeLanguage("en")
                                showLanguageMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.german)) },
                            onClick = {
                                changeLanguage("de")
                                showLanguageMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.bulgarian)) },
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
                            tint = Color.White
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ♟️ Background Subtle Chessboard Pattern
            ChessboardBackgroundPattern(
                color = mossDark.copy(alpha = 0.04f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // User Welcome Header (Optional)
                user?.displayName?.let { name ->
                    Text(
                        text = stringResource(R.string.welcome_user, name),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = mossDark
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Feature Action Cards
                HomeNavigationCard(
                    title = stringResource(R.string.puzzles),
                    icon = Icons.Default.Psychology,
                    onClick = { navController.navigate("puzzles") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                HomeNavigationCard(
                    title = stringResource(R.string.analyze),
                    icon = Icons.Default.Analytics,
                    onClick = { navController.navigate("analyze") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                HomeNavigationCard(
                    title = stringResource(R.string.profile),
                    icon = Icons.Default.Person,
                    onClick = { navController.navigate("profile") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                HomeNavigationCard(
                    title = stringResource(R.string.about),
                    icon = Icons.Default.Info,
                    onClick = { navController.navigate("about") }
                )
            }
        }
    }
}

// ♟️ Canvas Drawing Component for Watermark Chessboard
@Composable
fun ChessboardBackgroundPattern(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val columns = 8
        val rows = 12
        val squareWidth = size.width / columns
        val squareHeight = size.height / rows

        for (row in 0 until rows) {
            for (col in 0 until columns) {
                if ((row + col) % 2 == 1) {
                    drawRect(
                        color = color,
                        topLeft = Offset(col * squareWidth, row * squareHeight),
                        size = Size(squareWidth, squareHeight)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeNavigationCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val champagne = colorResource(id = R.color.champagne)
    val brown = colorResource(id = R.color.brown)
    val mossDark = colorResource(id = R.color.moss_dark)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = champagne),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(mossDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = brown
                    )
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = brown.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

