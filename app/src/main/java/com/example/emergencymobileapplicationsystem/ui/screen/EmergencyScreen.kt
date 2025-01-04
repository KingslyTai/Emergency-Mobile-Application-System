package com.example.emergencymobileapplicationsystem.ui.screen

import BottomNavigationRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.emergencymobileapplicationsystem.data.Report
import com.example.emergencymobileapplicationsystem.ui.components.DrawerContent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    onNavigateToFeedback: () -> Unit,
    onCreateAccount: () -> Unit,
    isLoggedIn: Boolean,
    onLogin: () -> Unit,
    onSignOut: () -> Unit,
    onManageProfile: () -> Unit,
    onNavigateToMyLocation: () -> Unit,
    onNavigateToReportScreen: (String, () -> Unit) -> Unit,
    onNavigateToReportListScreen: () -> Unit,
    onChatClick: (String) -> Unit,
    isDrawerOpen: MutableState<Boolean>,
    onToggleDrawer: () -> Unit,
    onNavigateToVolunteerService: () -> Unit,
    onNavigateToVolunteerRequestList: () -> Unit,
    onNavigateToVolunteerDetails: () -> Unit,
    onNavigateToSettings:() -> Unit,
    isInEmergencyScreen: Boolean
) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val userId = currentUser?.uid ?: "Unknown"
    val firestore = FirebaseFirestore.getInstance()
    var reports by remember { mutableStateOf<List<Report>>(emptyList()) }
    val isVolunteer = remember { mutableStateOf(false) } // Track if the user is a volunteer
    val coroutineScope = rememberCoroutineScope()

    // Check if the user is a volunteer
    LaunchedEffect(userId) {
        if (userId != "Unknown") {
            firestore.collection("volunteers").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    isVolunteer.value = document.exists()
                }
                .addOnFailureListener {
                    isVolunteer.value = false
                }
        }
    }

    // Function to fetch reports
    fun fetchReports() {
        firestore.collection("emergencyReports")
            .whereEqualTo("userId", userId)
            .whereIn("status", listOf("Open", "In Progress"))
            .get()
            .addOnSuccessListener { documents ->
                val fetchedReports = documents.map { document ->
                    Report(
                        reportId = document.id,
                        reportType = document.getString("reportType") ?: "Unknown",
                        description = document.getString("description") ?: "",
                        status = document.getString("status") ?: "Unknown",
                        timestamp = document.getTimestamp("timestamp")?.toDate() ?: Date()
                    )
                }
                reports = fetchedReports
            }
            .addOnFailureListener {
                reports = emptyList()
            }
    }

    // Reset drawer state and fetch reports on re-entry to this screen
    LaunchedEffect(Unit) {
        isDrawerOpen.value = false
        fetchReports()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TopAppBar(
                title = { Text("", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onToggleDrawer() }) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(reports) { report ->
                    ReportCard(report = report, onChatClick = onChatClick)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large central SOS button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmergencyActionButton(
                    text = "SOS",
                    onClick = {
                        onNavigateToReportScreen(userId) {
                            fetchReports()
                        }
                    },
                    modifier = Modifier.size(200.dp),
                    color = Color.Red
                )
            }
        }

        if (isLoggedIn) {
            BottomNavigationRow(
                isEmergencyHelpSelected = isInEmergencyScreen,
                onEmergencyHelpClick = { /* Navigate back to EmergencyScreen */ },
                onVolunteerServiceClick = onNavigateToVolunteerService,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }

        // Custom left-side drawer
        if (isDrawerOpen.value) {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { isDrawerOpen.value = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(with(LocalDensity.current) { 200.dp })
                ) {
                    Surface(
                        color = Color.White,
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
                            onNavigateToEmergencyServiceInformation = {
                                isDrawerOpen.value = false
                                onNavigateToMyLocation()
                            },
                            isLoggedIn = isLoggedIn,
                            isVolunteer = isVolunteer.value, // Pass volunteer state
                            onLogin = {
                                isDrawerOpen.value = false
                                onLogin()
                            },
                            onSignOut = {
                                isDrawerOpen.value = false
                                auth.signOut()
                                reports = emptyList()
                                onSignOut()
                            },
                            onManageProfile = {
                                isDrawerOpen.value = false
                                onManageProfile()
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

@Composable
fun ReportCard(report: Report, onChatClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Type: ${report.reportType}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = "Description: ${report.description}", fontSize = 14.sp)
            Text(text = "Date: ${formatDate(report.timestamp)}", fontSize = 12.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                EmergencyStatusChip(status = report.status)
                Button(
                    onClick = { onChatClick(report.reportId) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(text = "Chat", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EmergencyStatusChip(status: String) {
    val backgroundColor: Color
    val textColor: Color
    val statusText: String

    when (status) {
        "Open" -> {
            backgroundColor = Color(0xFFFFCDD2)
            textColor = Color.Red
            statusText = "Open"
        }
        "In Progress" -> {
            backgroundColor = Color(0xFFFFF59D)
            textColor = Color(0xFFFFC107)
            statusText = "In Progress"
        }
        "Closed" -> {
            backgroundColor = Color(0xFFC8E6C9)
            textColor = Color(0xFF4CAF50)
            statusText = "Closed"
        }
        else -> {
            backgroundColor = Color.Gray
            textColor = Color.White
            statusText = "Unknown"
        }
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = statusText,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp
        )
    }
}

fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("EEE, MMM dd yyyy HH:mm:ss", Locale.getDefault())
    return formatter.format(date)
}

@Composable
fun EmergencyActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
