package com.example.appdevproject26s

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    ScreenScaffold(
        navController = navController,
        title = stringResource(R.string.home_title),
        showBackButton = false
    ) {
        Text("Home Screen")
    }
}