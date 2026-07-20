package com.example.appdevproject26s.social

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.appdevproject26s.R
import com.example.appdevproject26s.ScreenScaffold

@Composable
fun MessagingScreen(
    navController: NavController,
    messagingViewModel: MessagingScreenViewModel = hiltViewModel()
) {
    ScreenScaffold(
        navController = navController,
        title = stringResource(R.string.messages_title),
        showBackButton=true
    ) {
        val chats = messagingViewModel.chats.collectAsState().value

        LazyColumn(

        ) {
            items(items = chats) {
                chat ->
                Text(chat)
            }
        }
    }
}
