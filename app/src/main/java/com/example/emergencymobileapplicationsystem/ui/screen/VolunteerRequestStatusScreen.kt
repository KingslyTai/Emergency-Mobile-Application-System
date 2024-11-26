package com.example.emergencymobileapplicationsystem.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergencymobileapplicationsystem.data.VolunteerRequest
import com.example.emergencymobileapplicationsystem.repository.VolunteerRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerRequestStatusScreen(
    userId: String,
    onRequestDetailClick: (VolunteerRequest) -> Unit
) {
    val volunteerRepository = VolunteerRepository()
    val coroutineScope = rememberCoroutineScope()
    var requestList by remember { mutableStateOf<List<VolunteerRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load requests for the current user
    LaunchedEffect(userId) {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                volunteerRepository.getUserVolunteerRequests(userId) { requests ->
                    if (requests.isNotEmpty()) {
                        requestList = requests
                    } else {
                        errorMessage = "No volunteer requests available."
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Error fetching requests: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Volunteer Requests") },
                modifier = Modifier.fillMaxWidth()
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
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    errorMessage != null -> {
                        Text(
                            text = errorMessage!!,
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 18.sp,
                            color = Color.Red
                        )
                    }
                    requestList.isNotEmpty() -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(requestList) { request ->
                                VolunteerRequestCard(
                                    request = request,
                                    onClick = { onRequestDetailClick(request) }
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
fun VolunteerRequestCard(
    request: VolunteerRequest,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Service Type: ${request.serviceType}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(text = "Date: ${request.date}", fontSize = 14.sp, color = Color.DarkGray)
            Text(text = "Location: ${request.location}", fontSize = 14.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(8.dp))

            // Status Indicator with color coding
            val statusColor = when (request.status) {
                "Pending" -> Color.Gray
                "Accepted" -> Color.Green
                else -> Color.Red
            }
            Text(
                text = "Status: ${request.status}",
                color = statusColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            // Display assigned volunteer if request is accepted
            if (request.status == "Accepted" && !request.assignedVolunteer.isNullOrEmpty()) {
                Text(
                    text = "Assigned Volunteer: ${request.assignedVolunteer}",
                    color = Color.Blue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
