package com.example.emergencymobileapplicationsystem.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.emergencymobileapplicationsystem.data.VolunteerRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun VolunteerRequestDetailsScreen(
    request: VolunteerRequest?,
    onBack: () -> Unit,
    onNavigateToViewProfile: (String) -> Unit
) {
    if (request == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Error: Unable to load request details.")
        }
        return
    }

    // State for user name and loading status
    var userName by remember { mutableStateOf("Loading...") }
    var isLoading by remember { mutableStateOf(true) }
    val firestore = FirebaseFirestore.getInstance()

    // Function to fetch the user name from Firestore
    suspend fun fetchUserName(userId: String): String {
        return try {
            val documentSnapshot = firestore.collection("profiles").document(userId).get().await()
            documentSnapshot.getString("name") ?: "Unknown"
        } catch (e: Exception) {
            Log.e("VolunteerRequestDetails", "Error fetching user name for userId: $userId, ${e.message}")
            "Unknown"
        }
    }

    // Load the user name
    LaunchedEffect(request.userId) {
        isLoading = true
        userName = fetchUserName(request.userId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)) // Subtle gray background
                    .padding(padding)
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Name of the user (Clickable for navigation)
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.h4.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6200EA)
                            ),
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .clickable {
                                    onNavigateToViewProfile(request.userId)
                                }
                        )

                        // Card for request details
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                RequestDetailItem(label = "Service Type", value = request.serviceType)
                                RequestDetailItem(label = "Location", value = request.location)
                                if (request.notes.isNotEmpty()) {
                                    RequestDetailItem(label = "Notes", value = request.notes)
                                }
                                RequestDetailItem(label = "Date", value = request.date)
                                RequestDetailItem(label = "Status", value = request.status)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun RequestDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (label) {
                "Location" -> Icons.Default.Place
                "Date" -> Icons.Default.DateRange
                else -> Icons.Default.Info
            },
            contentDescription = null,
            tint = Color(0xFF6200EA),
            modifier = Modifier
                .size(24.dp)
                .padding(end = 8.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold),
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Medium),
                color = Color.Black
            )
        }
    }
}
