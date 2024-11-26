package com.example.emergencymobileapplicationsystem.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.emergencymobileapplicationsystem.data.Report
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportHistoryScreen(
    navController: NavController,
    onNavigateBack: () -> Unit,
    onViewChat: (String, String, String) -> Unit // Updated to accept reportId, emergencyType, and description
) {
    var closedReports by remember { mutableStateOf<List<Report>>(emptyList()) }
    var selectedReportType by remember { mutableStateOf("All") }
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(selectedReportType) {
        try {
            val query = firestore.collection("emergencyReports")
                .whereEqualTo("status", "Close")

            val snapshot = if (selectedReportType != "All") {
                query.whereEqualTo("reportType", selectedReportType).get().await()
            } else {
                query.get().await()
            }

            closedReports = snapshot.documents.mapNotNull {
                it.toObject(Report::class.java)?.apply { reportId = it.id }
            }
        } catch (e: Exception) {
            Log.e("ReportHistoryScreen", "Error fetching closed reports", e)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Top App Bar with back button
        TopAppBar(
            title = { Text("Report") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Text(text = "Closed Reports", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        // Filter button card
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
                        onClick = { selectedReportType = type },
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

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(closedReports) { report ->
                ReportHistoryItem(
                    report = report,
                    onViewChat = {
                        onViewChat(report.reportId, report.reportType, report.description)
                    } // Pass reportId, emergencyType, and description
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateBack) {
            Text("Back")
        }
    }
}

@Composable
fun ReportHistoryItem(
    report: Report,
    onViewChat: () -> Unit // Callback for viewing chat
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Type: ${report.reportType}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Description: ${report.description}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Date: ${report.timestamp}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                HistoryStatusChip(status = report.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // New View Chat button
            Button(onClick = onViewChat) {
                Text("View Chat")
            }
        }
    }
}

@Composable
fun HistoryStatusChip(status: String) {
    val backgroundColor = when (status) {
        "Open" -> Color(0xFFFFCDD2) // Light red for Open
        "In Progress" -> Color(0xFFFFE082) // Light orange for In Progress
        "Close" -> Color(0xFFC8E6C9) // Light green for Close
        else -> Color.LightGray
    }

    val textColor = when (status) {
        "Open" -> Color(0xFFD32F2F) // Dark red for Open
        "In Progress" -> Color(0xFFFFA000) // Dark orange for In Progress
        "Close" -> Color(0xFF388E3C) // Dark green for Close
        else -> Color.DarkGray
    }

    Box(
        modifier = Modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
