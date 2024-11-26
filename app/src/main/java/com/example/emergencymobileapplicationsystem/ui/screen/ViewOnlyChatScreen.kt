package com.example.emergencymobileapplicationsystem.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.emergencymobileapplicationsystem.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewOnlyChatScreen(
    reportId: String,
    emergencyType: String,
    emergencyDescription: String,
    navController: NavController,
    onNavigateBack: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val specificUserIds = listOf("57105ppbxVOFt8tERjStegWKa53", "EwgGnc91uiRMIOEF8yQetkwBka53") // Specific user IDs
    var chatMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var userNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Fetch chat messages from Firebase for the specified reportId
    LaunchedEffect(reportId) {
        try {
            val snapshot = firestore.collection("emergencyReports")
                .document(reportId)
                .collection("chat")
                .get()
                .await()

            chatMessages = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Message::class.java)?.copy(id = doc.id)
            }

            // Fetch user names based on unique userIds in chat messages
            val userIds = chatMessages.map { it.userId }.distinct()
            val userNamesMap = mutableMapOf<String, String>()

            userIds.forEach { userId ->
                val userDoc = firestore.collection("users").document(userId).get().await()
                val userName = userDoc.getString("name") ?: "Unknown"
                userNamesMap[userId] = userName
            }
            userNames = userNamesMap
        } catch (e: Exception) {
            Log.e("ViewOnlyChatScreen", "Error fetching chat messages or user names", e)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Top App Bar with back button
        TopAppBar(
            title = { Text("") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Header with Emergency Type and Description
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFB3E5FC), shape = MaterialTheme.shapes.medium)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Emergency Type: $emergencyType",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Description: $emergencyDescription",
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display chat messages in a LazyColumn
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatMessages) { message ->
                val userName = userNames[message.userId] ?: "Unknown"
                ChatBubble(
                    message = message,
                    userName = userName,
                    isSpecificUser = message.userId in specificUserIds // Check if the sender is a specific user
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Back button to navigate back to the previous screen
        Button(onClick = onNavigateBack) {
            Text("Back")
        }
    }
}

@Composable
fun ChatBubble(message: Message, userName: String, isSpecificUser: Boolean) {
    val backgroundColor = if (isSpecificUser) Color(0xFFC8E6C9) else Color(0xFFE0E0E0)
    val textColor = if (isSpecificUser) Color(0xFF388E3C) else Color.Black
    val alignment = if (isSpecificUser) Alignment.End else Alignment.Start
    val timeFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = alignment
    ) {
        if (!isSpecificUser) {
            // Show sender's name for non-specific users
            Text(
                text = userName,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .background(color = backgroundColor, shape = MaterialTheme.shapes.medium)
                .padding(12.dp)
                .align(alignment) // Align bubble based on sender type
        ) {
            Text(
                text = message.text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = timeFormat.format(message.timestamp),
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.align(alignment)
        )
    }
}
