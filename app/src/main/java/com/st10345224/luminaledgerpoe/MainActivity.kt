package com.st10345224.luminaledgerpoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.auth.FirebaseAuth
import com.st10345224.luminaledgerpoe.ui.theme.LuminaLedgerPOETheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge()

        setContent {
            LuminaLedgerPOETheme {
                var isSplashScreenVisible by remember { mutableStateOf(true) }
                var isRegistering by remember { mutableStateOf(false) } // State to track if the user is on the registration screen
                var isLoggedIn by remember { mutableStateOf(false) } // State to track if the user is logged in
                rememberCoroutineScope()

                LaunchedEffect(key1 = true) {
                    delay(3000) // Simulate splash screen duration
                    isSplashScreenVisible = false // Hide splash screen after delay
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Conditional rendering based on app state
                    if (isSplashScreenVisible) {
                        SplashScreen() // Show the splash screen
                    } else if (isLoggedIn) {
                        NavigationMap() // Show the main navigation if logged in
                    } else {
                        // Show either the login or registration screen
                        if (isRegistering) {
                            RegisterScreen(
                                onNavigateToLogin = { isRegistering = false }, // Callback to go back to login
                                onRegistrationSuccess = { isLoggedIn = true } // Callback for successful registration
                            )
                        } else {
                            LoginScreen(
                                onNavigateToRegister = { isRegistering = true }, // Callback to go to registration
                                onLoginSuccess = { isLoggedIn = true } // Callback for successful login
                            )
                        }
                    }
                }
            }
        }
    }
}
