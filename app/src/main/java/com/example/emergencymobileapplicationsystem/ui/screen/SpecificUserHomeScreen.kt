package com.example.emergencymobileapplicationsystem.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.emergencymobileapplicationsystem.data.AdditionalDetails
import com.example.emergencymobileapplicationsystem.data.Report
import com.example.emergencymobileapplicationsystem.data.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecificUserHomeScreen(
    onNavigateToEmergencyInfo: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    onManageUsers: () -> Unit,
    onSignOut: () -> Unit,
    onChatClick: (String) -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onNavigateToVolunteerVerification: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var reports by remember { mutableStateOf(listOf<Report>()) }
    var profilesMap by remember { mutableStateOf(mapOf<String, String>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedReportType by remember { mutableStateOf("All") }

    var showUserDetailsDialog by remember { mutableStateOf(false) }
    var selectedUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var selectedUserDetails by remember { mutableStateOf<AdditionalDetails?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }

    // Fetch data from Firebase
    suspend fun fetchData() {
        isLoading = true
        try {
            profilesMap = fetchProfilesFromFirebase() // Fetch profiles first
            reports = fetchReportsFromFirebase() // Fetch reports after profiles are loaded
            errorMessage = if (reports.isEmpty()) {
                "No data available."
            } else null
        } catch (e: Exception) {
            errorMessage = "Failed to load data."
            Log.e("SpecificUserHomeScreen", "Error fetching data: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        fetchData()
    }

    // Filter reports based on selected type and exclude reports with status "Close"
    val filteredReports = reports.filter { report ->
        (selectedReportType == "All" || report.reportType.contains(selectedReportType, ignoreCase = true)) &&
                report.status != "Close"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(200.dp) // Set width to 200 dp
                    .background(Color.White) // Set background color to white
                    .padding(16.dp)
            ) {
                CustomDrawerContent(
                    onNavigateToEmergencyService = onNavigateToEmergencyInfo,
                    onNavigateToFeedback = onNavigateToFeedback,
                    onNavigateToManageUsers = onManageUsers,
                    onNavigateToVolunteerVerification = onNavigateToVolunteerVerification,
                    onSignOut = onSignOut
                )
            }
        }
    ){
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Specific User Home") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Reports", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

                // Box containing the filter buttons
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("All", "Police", "Fire", "Hospital").forEach { type ->
                            Button(
                                onClick = {
                                    selectedReportType = type
                                    scope.launch { fetchData() }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedReportType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    contentColor = if (selectedReportType == type) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary
                                ),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(type)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                } else if (errorMessage != null) {
                    Text(errorMessage ?: "An error occurred", color = MaterialTheme.colorScheme.error)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredReports) { report ->
                            ReportItem(
                                report = report,
                                profilesMap = profilesMap,
                                onChatClick = { onChatClick(report.reportId) },
                                onUpdateStatus = { newStatus ->
                                    scope.launch {
                                        onUpdateStatus(report.reportId, newStatus)
                                        fetchData()
                                    }
                                },
                                onUserNameClick = { userId ->
                                    isLoadingDetails = true
                                    showUserDetailsDialog = true
                                    scope.launch {
                                        try {
                                            // Fetch the profile data
                                            val profileSnapshot = FirebaseFirestore.getInstance()
                                                .collection("profiles").document(userId).get().await()
                                            selectedUserProfile = profileSnapshot.toObject(UserProfile::class.java)

                                            // Fetch the additional details data
                                            val detailsSnapshot = FirebaseFirestore.getInstance()
                                                .collection("additionalDetails").document(userId).get().await()
                                            selectedUserDetails = detailsSnapshot.toObject(AdditionalDetails::class.java)

                                        } catch (e: Exception) {
                                            Log.e("SpecificUserHomeScreen", "Error fetching user details: ${e.message}")
                                        } finally {
                                            isLoadingDetails = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUserDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showUserDetailsDialog = false },
            title = { Text("User Details") },
            text = {
                if (isLoadingDetails) {
                    CircularProgressIndicator()
                } else {
                    Column {
                        selectedUserProfile?.let { profile ->
                            Text("Name: ${profile.name}")
                            Text("Age: ${profile.age}")
                            Text("Phone: ${profile.phone}")
                            Text("Address: ${profile.address}")
                            Text("Emergency Contact: ${profile.emergencyContact}")
                            Text("Date of Birth: ${profile.dateOfBirth}")
                            Text("Gender: ${profile.gender}")
                        } ?: Text("No profile information available.")

                        Spacer(modifier = Modifier.height(16.dp))

                        selectedUserDetails?.let { details ->
                            Text("Disability: ${details.disability}")
                            Text("Allergies: ${details.allergies}")
                            Text("Blood Type: ${details.bloodType}")
                            Text("Medical Conditions: ${details.medicalConditions}")
                        } ?: Text("No additional details available.")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showUserDetailsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun CustomDrawerContent(
    onNavigateToEmergencyService: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    onNavigateToManageUsers: () -> Unit,
    onNavigateToVolunteerVerification: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Drawer Header
        Text(
            text = "Menu",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Emergency Service
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToEmergencyService() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Place, // Replace with your icon
                contentDescription = "Emergency Service",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Emergency Service",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Feedback
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToFeedback() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Forum, // Replace with your icon
                contentDescription = "Feedback",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Feedback",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Manage Users
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToManageUsers() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person, // Replace with your icon
                contentDescription = "Report History",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Report History",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Volunteer Verification
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToVolunteerVerification() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Verified, // Replace with your icon
                contentDescription = "Volunteer Verification",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Volunteer Verification",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Sign Out
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSignOut() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp, // Replace with your icon
                contentDescription = "Sign Out",
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Sign Out",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}


@Composable
fun ReportItem(
    report: Report,
    profilesMap: Map<String, String>,
    onChatClick: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onUserNameClick: (String) -> Unit // New parameter for user name click
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf(report.status) }
    val statusOptions = listOf("Open", "In Progress", "Close")

    val userName = profilesMap[report.userId] ?: "Unknown"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onChatClick,
                    modifier = Modifier.padding(end = 8.dp)
                ) { Text("Chat") }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Name: $userName",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable {
                            report.userId?.let { userId ->
                                onUserNameClick(userId)
                            }
                        } // Only call if userId is non-null
                    )
                    Text("Type: ${report.reportType}", style = MaterialTheme.typography.bodyLarge)
                    Text("Description: ${report.description}", style = MaterialTheme.typography.bodyMedium)
                    Text("Date: ${report.timestamp}", style = MaterialTheme.typography.bodySmall)

                    report.latitude?.let { lat ->
                        report.longitude?.let { lng ->
                            Text("Location: ($lat, $lng)", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Status: $selectedStatus", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { showDialog = true }) {
                            Text("Update Status")
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select New Status") },
            text = {
                Column {
                    statusOptions.forEach { status ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedStatus == status),
                                onClick = {
                                    selectedStatus = status
                                    onUpdateStatus(status)
                                    showDialog = false
                                }
                            )
                            Text(text = status, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


// Functions to fetch reports, profiles, and additional details from Firebase

suspend fun fetchReportsFromFirebase(): List<Report> {
    return try {
        FirebaseFirestore.getInstance().collection("emergencyReports")
            .get()
            .await()
            .mapNotNull { document ->
                document.toObject(Report::class.java).copy(reportId = document.id)
            }
    } catch (e: Exception) {
        Log.e("fetchReportsFromFirebase", "Error: ${e.message}")
        emptyList()
    }
}

suspend fun fetchProfilesFromFirebase(): Map<String, String> {
    return try {
        FirebaseFirestore.getInstance().collection("profiles")
            .get()
            .await()
            .associate { document ->
                val userId = document.id
                val name = document.getString("name") ?: "Unknown"
                userId to name
            }
    } catch (e: Exception) {
        Log.e("fetchProfilesFromFirebase", "Error: ${e.message}")
        emptyMap()
    }
}