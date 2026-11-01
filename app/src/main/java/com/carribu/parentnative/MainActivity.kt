package com.carribu.parentnative

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carribu.parentnative.push.PushTokenRegistrar
import com.carribu.parentnative.ui.*
import com.carribu.parentnative.ui.auth.ForcePasswordScreen
import com.carribu.parentnative.ui.auth.LoginScreen
import com.carribu.parentnative.ui.auth.SetupAccountScreen
import com.carribu.parentnative.ui.main.MainShell
import com.carribu.parentnative.ui.theme.CarribuTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as CarribuApplication
        setContent {
            CarribuTheme {
                val appViewModel: AppViewModel = viewModel(
                    factory = SimpleFactory { AppViewModel(app.container.sessionRepository) }
                )
                val authViewModel: AuthViewModel = viewModel(
                    factory = SimpleFactory { AuthViewModel(app.container.authRepository) }
                )
                val destination by appViewModel.destination.collectAsStateWithLifecycle()
                var showingSetup by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(destination) {
                    if (destination is AppDestination.Main) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        launch {
                            PushTokenRegistrar.registerIfAvailable(
                                this@MainActivity,
                                app.container.parentRepository
                            )
                        }
                    }
                }

                when (val current = destination) {
                    AppDestination.Loading -> com.carribu.parentnative.ui.components.LoadingState("Restoring secure session…")
                    AppDestination.Login -> if (showingSetup) {
                        SetupAccountScreen(authViewModel) { showingSetup = false }
                    } else {
                        LoginScreen(authViewModel) { showingSetup = true }
                    }
                    is AppDestination.ForcePassword -> ForcePasswordScreen(authViewModel)
                    is AppDestination.Main -> MainShell(
                        session = current.session,
                        container = app.container
                    )
                }
            }
        }
    }
}
