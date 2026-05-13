package com.example.appdevproject26s

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.appdevproject26s.ui.theme.AppDevProject26STheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDevProject26STheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MapLayer(innerPadding)
                }
            }
        }
    }
}


