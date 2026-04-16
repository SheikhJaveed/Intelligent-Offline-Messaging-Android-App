package com.example.intelligent_messaging_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.intelligent_messaging_app.data.repository.UserPreferencesRepository
import com.example.intelligent_messaging_app.ui.navigation.NavGraph
import com.example.intelligent_messaging_app.ui.theme.IntelligentMessagingappTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IntelligentMessagingappTheme {
                NavGraph(userPreferencesRepository = userPreferencesRepository)
            }
        }
    }
}