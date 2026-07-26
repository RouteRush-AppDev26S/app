package com.example.appdevproject26s.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    val userProfile by profileViewModel.userProfile.collectAsState(initial = null)
    val personalBests by profileViewModel.personalBests.collectAsState(initial = emptyList())
    val isLoggedIn by profileViewModel.isLoggedIn.collectAsState(initial = false)

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            profileViewModel.fetchUserProfile()
            profileViewModel.fetchPersonalBests()
        }
    }

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
                UserProfileContent(
                    userProfile = userProfile,
                    personalBests = personalBests,
                    onLogout = { profileViewModel.logout() }
                )
            } else {
                AuthWizardContent(
                    viewModel = profileViewModel
                )
            }
        }
    }
}