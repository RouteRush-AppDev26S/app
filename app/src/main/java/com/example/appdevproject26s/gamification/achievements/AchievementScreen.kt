package com.example.appdevproject26s.gamification.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.appdevproject26s.auth.AuthRepository
import com.example.appdevproject26s.social.LoginPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementViewModel @Inject constructor(
    private val api: AchievementApi,
    authRepo: AuthRepository
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authRepo.isLoggedInFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    var achievements by mutableStateOf<List<Achievement>>(emptyList())
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
                achievements = api.getAchievements()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load"
            }

            isLoading = false
        }
    }
}

@Composable
fun AchievementScreen(
    navController: NavController,
    viewModel: AchievementViewModel = hiltViewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) viewModel.load()
    }

    ScreenScaffold(navController = navController, title = stringResource(R.string.achievements_title)) {
        if (!isLoggedIn) {
            LoginPrompt(
                feature = "achievements",
                onNavigateToLogin = { navController.navigate("profile") }
            )
        } else if (viewModel.isLoading) {
            CircularProgressIndicator()
        } else if (viewModel.errorMessage != null) {
            Text("Error: ${viewModel.errorMessage}")
        } else {
            AchievementList(viewModel.achievements)
        }
    }
}

@Composable
fun AchievementList(achievements: List<Achievement>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(achievements) { achievement ->
            AchievementRow(achievement)
            HorizontalDivider()
        }
    }
}

@Composable
fun AchievementRow(achievement: Achievement) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = achievement.description,
                color = if (achievement.unlocked)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (achievement.unlocked) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Unlocked",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!achievement.unlocked) {
            LinearProgressIndicator(
                progress = { achievement.currentProgress.toFloat() / achievement.target.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
            Text(
                text = "${achievement.currentProgress} / ${achievement.target}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}