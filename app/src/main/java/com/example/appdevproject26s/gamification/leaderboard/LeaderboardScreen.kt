package com.example.appdevproject26s.gamification.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
class LeaderboardViewModel @Inject constructor(
    private val api: LeaderboardApi,
    authRepo: AuthRepository
) : ViewModel() {

    // Check if user is logged in
    val isLoggedIn: StateFlow<Boolean> = authRepo.isLoggedInFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    var entries by mutableStateOf<List<LeaderboardEntry>>(emptyList())
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
                entries = api.getLeaderboard()
            } catch (e: Exception) {
                message = e.message ?: "Failed to load"
            }

            isLoading = false
        }
    }
}

@Composable
fun LeaderboardScreen(
    navController: NavController,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn)
            viewModel.load()
    }

    ScreenScaffold(
        navController = navController,
        title = stringResource(R.string.leaderboard_title)
    ) {
        if (!isLoggedIn) {
            LoginPrompt(
                feature = "the leaderboard",
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
            LeaderboardList(viewModel.entries)
        }
    }
}

@Composable
fun LeaderboardList(entries: List<LeaderboardEntry>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        for (entry in entries) {
            LeaderboardRow(entry)
            HorizontalDivider()
        }
    }
}

@Composable
fun LeaderboardRow(entry: LeaderboardEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("#${entry.rank} ${entry.username}")
        Text("Level ${entry.level} - ${entry.xp} XP")
    }
}