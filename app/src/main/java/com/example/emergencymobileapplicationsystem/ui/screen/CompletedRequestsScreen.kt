package com.example.emergencymobileapplicationsystem.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergencymobileapplicationsystem.data.VolunteerRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedRequestsScreen(
    onBack: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    var completedRequests by remember { mutableStateOf<List<VolunteerRequest>>(emptyList()) }
    var filteredRequests by remember { mutableStateOf<List<VolunteerRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Fetch completed requests assigned to the current volunteer
    LaunchedEffect(userId) {
        if (userId != null) {
            firestore.collection("volunteerRequests")
                .whereEqualTo("assignedVolunteer", userId)
                .whereEqualTo("status", "Completed")
                .get()
                .addOnSuccessListener { documents ->
                    val requests = documents.documents.mapNotNull { document ->
                        document.toObject(VolunteerRequest::class.java)?.apply {
                            this.requestId = document.id
                        }
                    }

                    if (requests.isNotEmpty()) {
                        completedRequests = requests
                        filteredRequests = requests // Initialize filtered list
                    } else {
                        errorMessage = "No completed requests found."
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    errorMessage = "Failed to fetch completed requests: ${e.message}"
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
                title = { Text("Completed Requests", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
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
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    errorMessage != null -> {
                        Text(
                            text = errorMessage ?: "An error occurred.",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    completedRequests.isNotEmpty() -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Search bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { query ->
                                    searchQuery = query
                                    filteredRequests = if (query.isEmpty()) {
                                        completedRequests
                                    } else {
                                        completedRequests.filter {
                                            it.serviceType.contains(query, ignoreCase = true) ||
                                                    it.location.contains(query, ignoreCase = true) ||
                                                    it.notes.contains(query, ignoreCase = true) ||
                                                    it.date.contains(query, ignoreCase = true)
                                        }
                                    }
                                },
                                label = { Text("Search Requests") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(filteredRequests) { request ->
                                    CompletedRequestCard(request)
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun CompletedRequestCard(request: VolunteerRequest) {
    Card(
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Service Type: ${request.serviceType}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Location: ${request.location}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (request.notes.isNotEmpty()) {
                Text(
                    text = "Notes: ${request.notes}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "Date: ${request.date}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Status: ${request.status}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
