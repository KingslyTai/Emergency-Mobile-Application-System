package com.example.emergencymobileapplicationsystem.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFeedbackScreen(
    navController: NavController,
    onSaveFeedback: (String) -> Unit // Added onSaveFeedback parameter
) {
    val auth = FirebaseAuth.getInstance()

    var feedbackContent by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    // Check if a user is logged in to retrieve the author information
    val currentUser = auth.currentUser
    currentUser?.email ?: "Anonymous"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {

        TopAppBar(
            title = { Text("") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Text field to enter feedback
        TextField(
            value = feedbackContent,
            onValueChange = { feedbackContent = it },
            label = { Text("Enter Feedback") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        // Display error message if there's any
        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = androidx.compose.ui.graphics.Color.Red)
        }

        // Display success message after feedback creation
        if (successMessage.isNotEmpty()) {
            Text(text = successMessage, color = androidx.compose.ui.graphics.Color.Green)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to submit the feedback
        Button(onClick = {
            if (feedbackContent.isBlank()) {
                errorMessage = "Feedback content cannot be empty."
            } else {
                // Call onSaveFeedback to store feedback
                onSaveFeedback(feedbackContent)
                successMessage = "Feedback submitted successfully!"
                feedbackContent = "" // Clear the input after submission
            }
        }) {
            Text("Submit Feedback")
        }

        Spacer(modifier = Modifier.height(16.dp))

    }
}
