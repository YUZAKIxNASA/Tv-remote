package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DeviceConnectionScreen
import com.example.ui.screens.DeviceDiscoveryScreen
import com.example.ui.screens.MainRemoteScreen
import com.example.ui.screens.MoreOptionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryBg
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.RemoteViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PrimaryBg
                ) {
                    val viewModel: RemoteViewModel = viewModel()
                    NasaRemoteApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun NasaRemoteApp(
    viewModel: RemoteViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Crossfade(
        targetState = currentScreen,
        label = "ScreenTransition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            AppScreen.DISCOVERY -> DeviceDiscoveryScreen(viewModel = viewModel)
            AppScreen.CONNECTION -> DeviceConnectionScreen(viewModel = viewModel)
            AppScreen.REMOTE -> MainRemoteScreen(viewModel = viewModel)
            AppScreen.MORE_OPTIONS -> MoreOptionsScreen(viewModel = viewModel)
        }
    }
}
