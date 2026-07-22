package com.example.appdevproject26s

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val api: LeaderboardApi
) : ViewModel() {

    var entries by mutableStateOf<List<LeaderboardEntry>>(emptyList())
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
                entries = api.getLeaderboard()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load"
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
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    ScreenScaffold(navController = navController, title = stringResource(R.string.leaderboard_title)) {
        if (viewModel.isLoading) {
            CircularProgressIndicator()
        } else if (viewModel.errorMessage != null) {
            Text("Error: ${viewModel.errorMessage}")
        } else {
            LeaderboardList(viewModel.entries)
        }
    }
}

@Composable
fun LeaderboardList(entries: List<LeaderboardEntry>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(entries) { entry ->
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