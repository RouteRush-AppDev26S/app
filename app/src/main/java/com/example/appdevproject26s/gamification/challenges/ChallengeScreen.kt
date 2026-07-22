package com.example.appdevproject26s.gamification.challenges

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.appdevproject26s.R
import com.example.appdevproject26s.ScreenScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChallengeViewModel @Inject constructor(
    private val api: ChallengeApi
) : ViewModel() {

    var challenge by mutableStateOf<WeeklyChallenge?>(null)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun load() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                challenge = api.getCurrentChallenge()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load"
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
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    ScreenScaffold(navController = navController, title = stringResource(R.string.challenge_title)) {
        if (viewModel.isLoading) {
            CircularProgressIndicator()
        } else if (viewModel.errorMessage != null) {
            Text("Error: ${viewModel.errorMessage}")
        } else {
            viewModel.challenge?.let { ChallengeDetails(it) }
        }
    }
}

@Composable
fun ChallengeDetails(challenge: WeeklyChallenge) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(challenge.description)

        LinearProgressIndicator(
            progress = { challenge.currentProgress.toFloat() / challenge.target.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        Text("${challenge.currentProgress} / ${challenge.target}")

        if (challenge.completed) {
            Text("Completed!")
        }
    }
}