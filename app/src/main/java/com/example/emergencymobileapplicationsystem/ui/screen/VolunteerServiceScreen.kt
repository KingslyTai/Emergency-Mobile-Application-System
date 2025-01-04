package com.example.emergencymobileapplicationsystem.ui.screen

import BottomNavigationRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.emergencymobileapplicationsystem.data.VolunteerRequest
import com.example.emergencymobileapplicationsystem.ui.components.DrawerContent
import com.example.emergencymobileapplicationsystem.repository.VolunteerRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerServiceScreen(
    onRequestVolunteerService: () -> Unit,
    onRegisterAsVolunteer: () -> Unit,
    onNavigateToStatusScreen: () -> Unit,
    onNavigateToEmergencyHelp: () -> Unit,
    isInEmergencyScreen: Boolean,
    onNavigateToFeedback: () -> Unit,
    onCreateAccount: () -> Unit,
    isLoggedInState: MutableState<Boolean>,
    onLogin: () -> Unit,
    onSignOut: () -> Unit,
    onManageProfile: () -> Unit,
    onNavigateToMyLocation: () -> Unit,
    onNavigateToReportListScreen: () -> Unit,
    onNavigateToVolunteerDetails: () -> Unit,
    onNavigateToVolunteerRequestList: () -> Unit,
    onNavigateToAcceptedRequestDetails: () -> Unit,
    onNavigateToCompletedRequests: () -> Unit,
    onNavigateToCreatedRequests: () -> Unit,
    onNavigateToSettings:() -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val volunteerRepository = VolunteerRepository()
    val isDrawerOpen = remember { mutableStateOf(false) }
    val isRegistered = remember { mutableStateOf(false) }
    val isPending = remember { mutableStateOf(false) }

    var acceptedRequest by remember { mutableStateOf<VolunteerRequest?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            isLoading = true

            volunteerRepository.checkVolunteerRegistration(userId) { registered, _ ->
                isRegistered.value = registered
            }

            firestore.collection("pendingVolunteers").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.getString("status") == "Pending") {
                        isPending.value = true
                    }
                }

            firestore.collection("volunteerRequests")
                .whereEqualTo("assignedVolunteer", userId)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val fetchedRequest =
                            documents.documents[0].toObject(VolunteerRequest::class.java)
                        if (fetchedRequest != null) {
                            fetchedRequest.requestId = documents.documents[0].id
                        }
                        acceptedRequest = fetchedRequest
                        errorMessage = null
                    } else {
                        acceptedRequest = null
                        errorMessage = "No accepted requests found."
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    errorMessage = "Failed to fetch accepted request: ${e.message}"
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                TopAppBar(
                    title = { Text("Volunteer Service", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { isDrawerOpen.value = !isDrawerOpen.value }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            ActionCard(
                                text = "Request Volunteer Service",
                                onClick = onRequestVolunteerService
                            )
                        }

                        if (isPending.value) {
                            item {
                                ActionCard(
                                    text = "Check Volunteer Application Status",
                                    onClick = onNavigateToStatusScreen
                                )
                            }
                        } else if (!isRegistered.value) {
                            item {
                                ActionCard(
                                    text = "Register as a Volunteer",
                                    onClick = onRegisterAsVolunteer
                                )
                            }
                        }

                        item {
                            ActionCard(
                                text = "View Accepted Request",
                                onClick = onNavigateToAcceptedRequestDetails
                            )
                        }

                        item {
                            ActionCard(
                                text = "Volunteer Request List",
                                onClick = onNavigateToVolunteerRequestList
                            )
                        }

                        item {
                            ActionCard(
                                text = "View Created Requests",
                                onClick = onNavigateToCreatedRequests
                            )
                        }

                        item {
                            ActionCard(
                                text = "View Completed Requests",
                                onClick = onNavigateToCompletedRequests
                            )
                        }
                    }
                }
            }

            BottomNavigationRow(
                isEmergencyHelpSelected = !isInEmergencyScreen,
                onEmergencyHelpClick = onNavigateToEmergencyHelp,
                onVolunteerServiceClick = { /* Already on Volunteer Service screen */ },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (isDrawerOpen.value) {
                Popup(
                    alignment = Alignment.TopStart,
                    onDismissRequest = { isDrawerOpen.value = false }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(with(LocalDensity.current) { 300.dp })
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium,
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            DrawerContent(
                                onNavigateToFeedback = {
                                    isDrawerOpen.value = false
                                    onNavigateToFeedback()
                                },
                                onCreateAccount = {
                                    isDrawerOpen.value = false
                                    onCreateAccount()
                                },
                                isLoggedIn = isLoggedInState.value,
                                isVolunteer = isRegistered.value,
                                onLogin = {
                                    isDrawerOpen.value = false
                                    onLogin()
                                },
                                onSignOut = {
                                    isDrawerOpen.value = false
                                    onSignOut()
                                },
                                onManageProfile = {
                                    isDrawerOpen.value = false
                                    onManageProfile()
                                },
                                onNavigateToEmergencyServiceInformation = {
                                    isDrawerOpen.value = false
                                    onNavigateToMyLocation()
                                },
                                onNavigateToReportListScreen = {
                                    isDrawerOpen.value = false
                                    onNavigateToReportListScreen()
                                },
                                onNavigateToVolunteerDetails = {
                                    isDrawerOpen.value = false
                                    onNavigateToVolunteerDetails()
                                },
                                onNavigateToVolunteerRequestList = {
                                    isDrawerOpen.value = false
                                    onNavigateToVolunteerRequestList()
                                },
                                onNavigateToSettings = {
                                    isDrawerOpen.value = false
                                    onNavigateToSettings()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(text: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
