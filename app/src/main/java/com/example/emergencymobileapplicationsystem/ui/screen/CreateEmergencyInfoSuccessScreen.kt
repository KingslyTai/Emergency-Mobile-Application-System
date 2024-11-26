package com.example.emergencymobileapplicationsystem.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController

@Composable
fun CreateEmergencyInfoSuccessScreen(
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
        // Big tick icon
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            modifier = Modifier.size(100.dp), // Set size for the icon
            tint = Color.Green // Set the color of the icon
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Success message
        Text(
            text = message,
            fontSize = 24.sp
        )

        // Back button to go to Specific User Location screen
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            onClick = {
                // Navigate back to "specificUserLocationScreen"
                navController.navigate("specificUserLocationScreen") {
                    popUpTo("specificUserLocationScreen") {
                        inclusive = true
                    }
                }
            }
        ) {
            Text("Back")
        }
    }
}
