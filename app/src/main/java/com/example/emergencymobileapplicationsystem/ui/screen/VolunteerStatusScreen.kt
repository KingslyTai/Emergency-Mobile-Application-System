package com.example.emergencymobileapplicationsystem.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerStatusScreen(
    onNavigateBack: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var statusMessage by remember { mutableStateOf("Fetching your status...") }
    var isLoading by remember { mutableStateOf(true) }
    var errorOccurred by remember { mutableStateOf(false) }

    fun fetchVolunteerStatus(userId: String) {
        firestore.collection("pendingVolunteers").document(userId)
            .get()
            .addOnSuccessListener { document ->
                isLoading = false
                if (document.exists()) {
                    val status = document.getString("status") ?: "Pending"
                    statusMessage = when (status) {
                        "Approved" -> "Your registration has been approved!"
                        "Rejected" -> "Your registration was rejected."
                        else -> "Your registration is still pending."
                    }
                } else {
                    statusMessage = "No registration request found."
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorOccurred = true
                Log.e("VolunteerStatusScreen", "Error fetching status: ${e.message}")
                Toast.makeText(context, "Failed to fetch status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            isLoading = false
            errorOccurred = true
            statusMessage = "User not logged in."
        } else {
            fetchVolunteerStatus(userId)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Volunteer Status",
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                CircularProgressIndicator()
            } else if (errorOccurred) {
                Text(
                    text = "Failed to fetch status. Please try again later.",
                    color = Color.Red,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                Text(
                    text = statusMessage,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Back")
            }
        }
    }
}
