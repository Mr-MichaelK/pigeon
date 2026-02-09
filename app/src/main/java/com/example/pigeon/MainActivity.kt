package com.example.pigeon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pigeon.domain.repository.UserRepository
import com.example.pigeon.ui.navigation.PigeonNavGraph
import com.example.pigeon.ui.theme.PigeonTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PigeonTheme {
                PigeonNavGraph(userRepository = userRepository)
            }
        }
    }
}