package com.example.emergencymobileapplicationsystem.ui.screen

import BottomNavigationRow
import androidx.compose.foundation.layout.*
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
    onNavigateToVolunteerRequestDetails: (VolunteerRequest) -> Unit // Pass selected request to navigate to its details
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val volunteerRepository = VolunteerRepository()
    val isDrawerOpen = remember { mutableStateOf(false) }
    val isRegistered = remember { mutableStateOf(false) }
    val isPending = remember { mutableStateOf(false) }

    // State for tracking the accepted request
    var acceptedRequest by remember { mutableStateOf<VolunteerRequest?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(auth.currentUser?.uid) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Check volunteer registration
            volunteerRepository.checkVolunteerRegistration(userId) { registered, _ ->
                isRegistered.value = registered
            }

            // Check pending applications
            firestore.collection("pendingVolunteers").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.getString("status") == "Pending") {
                        isPending.value = true
                    }
                }

            // Fetch accepted volunteer request
            firestore.collection("volunteerRequests")
                .whereEqualTo("assignedVolunteer", userId)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        acceptedRequest = documents.documents[0].toObject(VolunteerRequest::class.java)
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
                    .padding(bottom = 56.dp),
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

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onRequestVolunteerService,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Request Volunteer Service",
                            fontSize = 18.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isPending.value) {
                        Button(
                            onClick = onNavigateToStatusScreen,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Check Volunteer Application Status",
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (!isRegistered.value) {
                        Button(
                            onClick = onRegisterAsVolunteer,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Register as a Volunteer",
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            acceptedRequest?.let {
                                onNavigateToVolunteerRequestDetails(it)
                            }
                        },
                        enabled = acceptedRequest != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (acceptedRequest != null) "View Accepted Request" else "No Accepted Request",
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Button(
                        onClick = onNavigateToVolunteerRequestList,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Volunteer Request List",
                            fontSize = 16.sp,
                            color = Color.White
                        )
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
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
