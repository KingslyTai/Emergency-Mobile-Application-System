package com.example.emergencymobileapplicationsystem.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.emergencymobileapplicationsystem.data.VolunteerRequest
import com.example.emergencymobileapplicationsystem.repository.VolunteerRepository
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun VolunteerRequestListScreen(
    volunteerId: String, // Current logged-in volunteer's ID
    onNavigateToRequestDetails: (VolunteerRequest) -> Unit, // Callback to navigate to request details screen
    onBack: () -> Unit // Callback to handle back navigation
) {
    val volunteerRepository = VolunteerRepository()
    val firestore = FirebaseFirestore.getInstance()
    val requests = remember { mutableStateOf<List<VolunteerRequest>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val acceptingRequestId = remember { mutableStateOf<String?>(null) } // Track the request being accepted

    // Fetch volunteer requests from Firestore
    LaunchedEffect(Unit) {
        volunteerRepository.getAllVolunteerRequests { fetchedRequests ->
            // Filter out requests that already have an assigned volunteer
            val unassignedRequests = fetchedRequests.filter { it.assignedVolunteer == null }
            if (unassignedRequests.isNotEmpty()) {
                requests.value = unassignedRequests
                errorMessage.value = null
            } else {
                errorMessage.value = "No unassigned volunteer requests available."
            }
            isLoading.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Volunteer Requests") },
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
            ) {
                when {
                    isLoading.value -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(50.dp)
                                .align(Alignment.Center)
                        )
                    }

                    errorMessage.value != null -> {
                        Text(
                            text = errorMessage.value!!,
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.h6
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(requests.value) { request ->
                                VolunteerRequestCardList(
                                    request = request,
                                    isAccepting = acceptingRequestId.value == request.requestId,
                                    onAccept = {
                                        acceptingRequestId.value = request.requestId

                                        // Assign the volunteer to the request
                                        volunteerRepository.assignVolunteerToRequest(
                                            requestId = request.requestId,
                                            volunteerId = volunteerId
                                        ) { success ->
                                            if (success) {
                                                // Update the volunteer's availability to "not available"
                                                firestore.collection("volunteers").document(volunteerId)
                                                    .update("availability", "not available")
                                                    .addOnSuccessListener {
                                                        // Navigate to the request details screen if successful
                                                        onNavigateToRequestDetails(request)
                                                    }
                                                    .addOnFailureListener { e ->
                                                        errorMessage.value =
                                                            "Failed to update volunteer availability: ${e.message}"
                                                    }
                                            } else {
                                                errorMessage.value = "Failed to assign the request."
                                            }
                                            acceptingRequestId.value = null // Reset the accepting state
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun VolunteerRequestCardList(
    request: VolunteerRequest,
    isAccepting: Boolean,
    onAccept: () -> Unit
) {
    Card(
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Service Type: ${request.serviceType}",
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Date: ${request.date}",
                style = MaterialTheme.typography.body1
            )
            Text(
                text = "Location: ${request.location}",
                style = MaterialTheme.typography.body1
            )
            if (request.notes.isNotEmpty()) {
                Text(
                    text = "Notes: ${request.notes}",
                    style = MaterialTheme.typography.body1
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAccept,
                enabled = !isAccepting, // Disable button while accepting
                modifier = Modifier.align(Alignment.End)
            ) {
                if (isAccepting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Accept Request")
                }
            }
        }
    }
}
