package com.vizx.mongodbclient.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.vizx.mongodbclient.data.AppScreen
import com.vizx.mongodbclient.ui.screens.home.HomeScreen
import com.vizx.mongodbclient.ui.screens.login.LoginScreen

/**
 * Root Application Composable.
 * Decoupled navigation between dedicated LoginScreen and HomeScreen.
 */
@Composable
fun MongoApp(viewModel: MongoViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState.currentScreen) {
        AppScreen.LOGIN -> LoginScreen(
            uiState = uiState,
            viewModel = viewModel
        )
        AppScreen.HOME -> HomeScreen(
            uiState = uiState,
            viewModel = viewModel
        )
    }
}
