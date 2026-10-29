package com.avatar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.avatar.app.ui.auth.AuthViewModel
import com.avatar.app.ui.auth.LoginScreen
import com.avatar.app.ui.auth.RegisterScreen
import com.avatar.app.ui.home.HomeScreen
import com.avatar.app.ui.theme.AvatarTheme

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AvatarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authState by authViewModel.uiState.collectAsState()
                    var currentAuthScreen by remember { mutableStateOf("login") }

                    if (authState.isLoggedIn && authState.user != null) {
                        HomeScreen(
                            user = authState.user!!,
                            authViewModel = authViewModel
                        )
                    } else {
                        when (currentAuthScreen) {
                            "register" -> {
                                RegisterScreen(
                                    viewModel = authViewModel,
                                    onNavigateToLogin = { currentAuthScreen = "login" }
                                )
                            }
                            else -> {
                                LoginScreen(
                                    viewModel = authViewModel,
                                    onNavigateToRegister = { currentAuthScreen = "register" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
