package com.example.emergencymobileapplicationsystem.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergencymobileapplicationsystem.data.VolunteerRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptedRequestDetailsScreen(
    onBack: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    var acceptedRequest by remember { mutableStateOf<VolunteerRequest?>(null) }
    var requestCreatorName by remember { mutableStateOf("Loading...") }
    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch the accepted request and request creator's name
    LaunchedEffect(userId) {
        if (userId != null) {
            firestore.collection("volunteerRequests")
                .whereEqualTo("assignedVolunteer", userId)
                .get()
                .addOnSuccessListener { documents ->
                    val filteredRequests = documents.documents.mapNotNull { document ->
                        val request = document.toObject<VolunteerRequest>()?.apply {
                            this.requestId = document.id
                        }
                        if (request?.status != "Completed") request else null
                    }

                    if (filteredRequests.isNotEmpty()) {
                        val fetchedRequest = filteredRequests[0]
                        acceptedRequest = fetchedRequest

                        // Fetch request creator's name
                        fetchRequestCreatorName(fetchedRequest.userId, firestore) { name ->
                            requestCreatorName = name
                        }
                    } else {
                        errorMessage = "No active accepted requests available."
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    errorMessage = "Failed to fetch request: ${e.message}"
                    isLoading = false
                }
        } else {
            errorMessage = "User not logged in."
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Details", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
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
                    .padding(padding)
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator()
                    }

                    errorMessage != null -> {
                        Text(
                            text = errorMessage ?: "An error occurred.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    acceptedRequest != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Display the user name of the request creator
                            Text(
                                text = requestCreatorName,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Display the request details in a card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    AcceptedRequestDetailItem(label = "Service Type", value = acceptedRequest?.serviceType.orEmpty())
                                    AcceptedRequestDetailItem(label = "Location", value = acceptedRequest?.location.orEmpty())
                                    if (acceptedRequest?.notes?.isNotEmpty() == true) {
                                        AcceptedRequestDetailItem(label = "Notes", value = acceptedRequest?.notes.orEmpty())
                                    }
                                    AcceptedRequestDetailItem(label = "Date", value = acceptedRequest?.date.orEmpty())
                                    AcceptedRequestDetailItem(label = "Status", value = acceptedRequest?.status.orEmpty())
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Status Update Button
                            Button(
                                onClick = {
                                    isUpdating = true
                                    updateVolunteerRequestStatus(
                                        requestId = acceptedRequest?.requestId.orEmpty(),
                                        firestore = firestore,
                                        newStatus = "Completed",
                                        onSuccess = {
                                            isUpdating = false
                                            acceptedRequest = null
                                            errorMessage = "The request has been marked as completed."
                                        },
                                        onFailure = { error ->
                                            isUpdating = false
                                            errorMessage = error
                                        }
                                    )
                                },
                                enabled = !isUpdating
                            ) {
                                Text(if (isUpdating) "Updating..." else "Mark as Completed")
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun AcceptedRequestDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (label) {
                "Location" -> Icons.Default.Place
                "Date" -> Icons.Default.DateRange
                else -> Icons.Default.Info
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(24.dp)
                .padding(end = 8.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                color = Color.Black
            )
        }
    }
}

// Function to update the status of a volunteer request
private fun updateVolunteerRequestStatus(
    requestId: String,
    firestore: FirebaseFirestore,
    newStatus: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    if (requestId.isNotEmpty()) {
        firestore.collection("volunteerRequests")
            .document(requestId)
            .update("status", newStatus)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("UpdateStatus", "Error updating status: ${e.message}")
                onFailure("Failed to update status: ${e.message}")
            }
    } else {
        onFailure("Request ID is empty.")
    }
}

// Function to fetch the name of the request creator
private fun fetchRequestCreatorName(
    creatorUserId: String,
    firestore: FirebaseFirestore,
    onResult: (String) -> Unit
) {
    firestore.collection("profiles").document(creatorUserId)
        .get()
        .addOnSuccessListener { document ->
            val name = document.getString("name") ?: "Unknown"
            onResult(name)
        }
        .addOnFailureListener { e ->
            Log.e("RequestCreatorName", "Error fetching name: ${e.message}")
            onResult("Unknown")
        }
}
