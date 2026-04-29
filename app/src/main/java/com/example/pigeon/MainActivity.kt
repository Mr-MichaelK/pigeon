package com.example.pigeon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pigeon.data.service.MeshServiceController
import com.example.pigeon.domain.model.MeshPowerState
import com.example.pigeon.domain.repository.UserRepository
import com.example.pigeon.ui.navigation.PigeonNavGraph
import com.example.pigeon.ui.theme.PigeonTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var meshController: MeshServiceController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Boot the radio under the foreground service immediately so the mesh
        // stays alive when the user backgrounds the app — independent of which
        // screen mounts first. POST_NOTIFICATIONS is requested as part of the
        // bundled mesh-permission prompt on Onboarding/Radar; once granted, the
        // next state change in MeshService re-posts the notification.
        meshController.setPowerState(MeshPowerState.PASSIVE, isSticky = false)
        setContent {
            PigeonTheme {
                PigeonNavGraph(userRepository = userRepository)
            }
        }
    }
}
