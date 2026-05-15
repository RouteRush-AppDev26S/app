package com.example.appdevproject26s

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appdevproject26s.ui.theme.AppDevProject26STheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDevProject26STheme {
                NavigationApp()
            }
        }
    }
}

@Composable
fun NavigationApp(modifier: Modifier = Modifier) {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable( route = "route" ) { RouteScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
    }
}

class MapApplication : Application() {
    val repository: MapSettingsRepository by lazy {
        MapSettingsRepository(dataStore)
    }
}