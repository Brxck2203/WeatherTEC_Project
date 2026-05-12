package com.project.weathertec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.project.weathertec.ui.navigation.AppNavigation
import com.project.weathertec.ui.theme.WeatherTECTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherTECTheme {
                AppNavigation()
            }
        }
    }
}
