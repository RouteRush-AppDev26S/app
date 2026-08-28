package com.example.appdevproject26s.gamification.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
class AchievementViewModel @Inject constructor(
    private val api: AchievementApi,
    authRepo: AuthRepository
) : ViewModel() {

    // Check if user is logged in
    val isLoggedIn: StateFlow<Boolean> = authRepo.isLoggedInFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    var achievements by mutableStateOf<List<Achievement>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    // Load/Reload achievements
    fun load() {
        viewModelScope.launch {
            isLoading = true
            message = null

            try {
                achievements = api.getAchievements()
            } catch (e: Exception) {
                message = if (e.message != null) {
                    e.message
                } else {
                    "Failed to load"
                }
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

        if (isLoggedIn) {
            viewModel.load()
        }
    }

    val title = stringResource(R.string.achievements_title)

    ScreenScaffold(navController = navController, title = title) {

        if (!isLoggedIn) {

            LoginPrompt(
                feature = "achievements",
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
            AchievementList(viewModel.achievements)
        }
    }
}

@Composable
fun AchievementList(achievements: List<Achievement>) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        for (achievement in achievements) {
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

        // Text color depending on unlocked state
        val textColor: Color = if (achievement.unlocked) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = achievement.description, color = textColor)

            if (achievement.unlocked) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Unlocked",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!achievement.unlocked) {

            // Progress
            val progress: Float = if (achievement.target > 0) {
                achievement.currentProgress.toFloat() / achievement.target.toFloat()
            } else {
                // Avoid dividing by zero if there is no target
                0f
            }

            LinearProgressIndicator(
                progress = { progress },
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