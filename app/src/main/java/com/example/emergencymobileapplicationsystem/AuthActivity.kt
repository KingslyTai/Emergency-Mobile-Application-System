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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val isRegister = intent.getBooleanExtra("isRegister", true)

        setContent {
            val navController = rememberNavController()

            EmergencyMobileApplicationSystemTheme {
                Surface {
                    NavHost(
                        navController = navController,
                        startDestination = if (isRegister) "register" else "login"
                    ) {

                        // Register screen
                        composable("register") {
                            RegisterScreen(
                                navController = navController,
                                onRegisterSuccess = {
                                    // After successful registration, navigate back to MainActivity
                                    startActivity(Intent(this@AuthActivity, MainActivity::class.java).apply {
                                        putExtra("startDestination", "emergencyScreen")
                                    })
                                    finish() // Close AuthActivity after successful registration
                                }
                            )
                        }

                        // Login screen
                        composable("login") {
                            LoginScreen(
                                navController = navController,
                                onLoginSuccess = {
                                    val specificEmails = listOf("polis@gmail.com", "bomba@gmail.com")
                                    val currentUser = auth.currentUser

                                    // Determine start destination based on user email
                                    val startDestination = if (currentUser?.email in specificEmails) {
                                        "specificUserHomeScreen"
                                    } else {
                                        "emergencyScreen"
                                    }

                                    startActivity(Intent(this@AuthActivity, MainActivity::class.java).apply {
                                        putExtra("startDestination", startDestination)
                                    })
                                    finish()
                                },
                                onForgotPassword = {
                                    navController.navigate("forgotPassword")
                                }
                            )
                        }

                        // Forgot password screen
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
}
