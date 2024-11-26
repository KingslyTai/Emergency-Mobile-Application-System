package com.example.emergencymobileapplicationsystem.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.emergencymobileapplicationsystem.data.Report
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportListScreen(
    userId: String?,
    navController: NavController,
    onNavigateBack: () -> Unit,
    onChatClick: (String) -> Unit // Callback to navigate to ChatScreen
) {
    var reports by remember { mutableStateOf<List<Report>>(emptyList()) }
    val firestore = FirebaseFirestore.getInstance()
    rememberCoroutineScope()

    // Fetch user reports from Firebase
    suspend fun fetchReports() {
        if (userId != null) {
            try {
                val snapshot = firestore.collection("emergencyReports")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                reports = snapshot.documents.mapNotNull {
                    it.toObject(Report::class.java)?.apply { reportId = it.id }
                }
            } catch (e: Exception) {
                Log.e("ReportListScreen", "Error fetching reports", e)
            }
        }
    }

    LaunchedEffect(userId) {
        fetchReports()
    }

    // Periodically refresh the data from Firebase to ensure latest status is shown
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // Refresh every 5 seconds
            fetchReports()
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

        Text(text = "My Reports", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(reports) { report ->
                ReportListItem(
                    report = report,
                    onChatClick = {
                        if (report.reportId.isNotBlank()) {
                            onChatClick(report.reportId)
                            Log.d("ReportListScreen", "Navigating to ChatScreen with reportId: ${report.reportId}")
                        } else {
                            Log.e("ReportListScreen", "Report ID is empty. Cannot navigate to ChatScreen.")
                        }
                    }
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
fun ReportListItem(
    report: Report,
    onChatClick: () -> Unit // Callback for chat button
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
                StatusChip(status = report.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onChatClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Chat")
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
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
