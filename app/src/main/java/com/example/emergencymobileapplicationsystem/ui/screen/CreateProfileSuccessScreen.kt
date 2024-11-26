package com.example.emergencymobileapplicationsystem.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun CreateProfileSuccessScreen(
    message: String,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large tick icon
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = Color.Green,
            modifier = Modifier.size(100.dp) // Adjust size as needed
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Success message
        Text(
            text = message,
            fontSize = 24.sp,
            color = Color.Black,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Back to Profile button
        Button(onClick = {
            // Navigate back to the profile screen
            navController.navigate("profileScreen") {
                popUpTo("profileScreen") { inclusive = true }
            }
        }) {
            Text("Back to Profile")
        }
    }
}
