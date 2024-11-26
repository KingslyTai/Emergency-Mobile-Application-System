package com.example.emergencymobileapplicationsystem.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdditionalDetailsSuccessScreen(
    successMessage: String, // Accepts a success message to display
    onBackToDetails: () -> Unit
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
            text = successMessage,
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Back button
        Button(onClick = onBackToDetails) {
            Text(text = "Back")
        }
    }
}
