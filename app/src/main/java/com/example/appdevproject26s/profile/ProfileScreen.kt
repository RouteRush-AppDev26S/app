package com.example.appdevproject26s.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.appdevproject26s.ScreenScaffold

@Composable
fun ProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val authToken by profileViewModel.authToken.collectAsState(initial = null)
    val userProfile by profileViewModel.userProfile.collectAsState(initial = null)
    val isLoggedIn = !authToken.isNullOrBlank()

    ScreenScaffold(
        navController = navController,
        title = if (isLoggedIn) "Profile" else "Account Access",
        showBackButton = true
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoggedIn) {
                // View 1: Logged-In User Profile Dashboard
                UserProfileContent(
                    userProfile = userProfile,
                    onLogout = { profileViewModel.logout() }
                )
            } else {
                // View 2: Authentication Wizard (Login or Register)
                AuthWizardContent(
                    viewModel = profileViewModel
                )
            }
        }
    }
}