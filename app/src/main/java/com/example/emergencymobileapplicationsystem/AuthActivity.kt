package com.example.emergencymobileapplicationsystem

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.emergencymobileapplicationsystem.ui.screen.*
import com.example.emergencymobileapplicationsystem.ui.theme.EmergencyMobileApplicationSystemTheme
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private var isDarkModeEnabled = false // Dark mode toggle state

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val isRegister = intent.getBooleanExtra("isRegister", true)

        setContent {
            val navController = rememberNavController()

            EmergencyMobileApplicationSystemTheme(darkTheme = isDarkModeEnabled) {
                Surface {
                    NavHost(
                        navController = navController,
                        startDestination = if (isRegister) "register" else "login"
                    ) {
                        // Register Screen
                        composable("register") {
                            RegisterScreen(
                                navController = navController,
                                onRegisterSuccess = {
                                    // Navigate to MainActivity after successful registration
                                    navigateToMainActivity("emergencyScreen")
                                }
                            )
                        }

                        // Login Screen
                        composable("login") {
                            LoginScreen(
                                navController = navController,
                                onLoginSuccess = {
                                    val specificEmails = listOf("polis@gmail.com", "bomba@gmail.com")
                                    val currentUser = auth.currentUser

                                    // Determine the start destination based on user email
                                    val startDestination = if (currentUser?.email in specificEmails) {
                                        "specificUserHomeScreen"
                                    } else {
                                        "emergencyScreen"
                                    }

                                    navigateToMainActivity(startDestination)
                                },
                                onForgotPassword = {
                                    navController.navigate("forgotPassword")
                                }
                            )
                        }

                        // Forgot Password Screen
                        composable("forgotPassword") {
                            ForgotPasswordScreen(
                                navController = navController,
                                onOtpSent = {
                                    navController.navigate("resetPassword")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Navigates to MainActivity with the specified start destination.
     */
    private fun navigateToMainActivity(startDestination: String) {
        startActivity(Intent(this@AuthActivity, MainActivity::class.java).apply {
            putExtra("startDestination", startDestination)
        })
        finish() // Close AuthActivity after navigation
    }
}
