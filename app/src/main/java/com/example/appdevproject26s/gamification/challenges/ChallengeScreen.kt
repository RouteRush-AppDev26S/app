package com.example.appdevproject26s.gamification.challenges

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.appdevproject26s.R
import com.example.appdevproject26s.ScreenScaffold
import com.example.appdevproject26s.auth.AuthRepository
import com.example.appdevproject26s.social.LoginPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChallengeViewModel @Inject constructor(
    private val api: ChallengeApi,
    authRepo: AuthRepository
) : ViewModel() {

    // Check if user is logged in
    val isLoggedIn: StateFlow<Boolean> = authRepo.isLoggedInFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    var challenge by mutableStateOf<WeeklyChallenge?>(null)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    fun load() {
        viewModelScope.launch {
            isLoading = true
            message = null

            try {
                challenge = api.getCurrentChallenge()
            } catch (e: Exception) {
                message = e.message ?: "Failed to load"
            }

            isLoading = false
        }
    }
}

@Composable
fun ChallengeScreen(
    navController: NavController,
    viewModel: ChallengeViewModel = hiltViewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn)
            viewModel.load()
    }

    ScreenScaffold(
        navController = navController,
        title = stringResource(R.string.challenge_title)
    ) {
        if (!isLoggedIn) {
            LoginPrompt(
                feature = "weekly challenges",
                onNavigateToLogin = { navController.navigate("profile") }
            )
        } else if (viewModel.isLoading) {
            CircularProgressIndicator()
        } else if (viewModel.message != null) {

            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text("Error: ${viewModel.message}")

                Button(
                    onClick = { viewModel.load() },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Retry")
                }
            }
        } else {
            viewModel.challenge?.let { ChallengeDetails(it) }
        }
    }
}

@Composable
fun ChallengeDetails(challenge: WeeklyChallenge) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(challenge.description)

        LinearProgressIndicator(

            // Guard against target == 0
            progress = {
                if (challenge.target > 0)
                    challenge.currentProgress.toFloat() / challenge.target.toFloat()
                else
                    0f
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Text("${challenge.currentProgress} / ${challenge.target}")

        if (challenge.completed) {
            Text("Completed!")
        }
    }
}