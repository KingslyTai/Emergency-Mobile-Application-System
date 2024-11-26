package com.example.emergencymobileapplicationsystem.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.emergencymobileapplicationsystem.model.Message
import com.example.emergencymobileapplicationsystem.data.Report
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(reportId: String, userId: String?, onNavigateBack: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var reportDetails by remember { mutableStateOf<Report?>(null) }
    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }

    val firestore = FirebaseFirestore.getInstance()

    // Fetch report details
    LaunchedEffect(reportId) {
        try {
            val reportSnapshot = firestore.collection("emergencyReports").document(reportId).get().await()
            reportDetails = reportSnapshot.toObject(Report::class.java)
        } catch (e: Exception) {
            Log.w("ChatScreen", "Error fetching report details", e)
        }
    }

    // Real-time message updates
    LaunchedEffect(reportId) {
        firestore.collection("emergencyReports").document(reportId)
            .collection("chat")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ChatScreen", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    messages = snapshot.documents.map { doc ->
                        Message(
                            id = doc.id,
                            text = doc.getString("text") ?: "",
                            userId = doc.getString("userId") ?: "",
                            timestamp = doc.getTimestamp("timestamp")?.toDate() ?: Date(),
                            replyingToMessageText = doc.getString("replyingToMessageText")
                        )
                    }
                    Log.d("ChatScreen", "Fetched ${messages.size} messages")
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { selectedMessage = null })
                    }
            ) {
                // Show emergency report details
                reportDetails?.let { report ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Emergency Type: ${report.reportType}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Description: ${report.description}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Display chat messages
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { selectedMessage = null },
                                        onLongPress = { selectedMessage = message }
                                    )
                                }
                        ) {
                            MessageBubble(
                                message = message,
                                isCurrentUser = message.userId == userId
                            )

                            if (selectedMessage == message) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xCC333333), shape = MaterialTheme.shapes.small)
                                        .align(Alignment.CenterStart)
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        TextButton(onClick = {
                                            messageText = message.text
                                            isEditing = true
                                            selectedMessage = message
                                        }) {
                                            Text("Edit", color = Color.White)
                                        }
                                        TextButton(onClick = {
                                            deleteMessage(reportId, message.id)
                                            selectedMessage = null
                                        }) {
                                            Text("Delete", color = Color.Red)
                                        }
                                        TextButton(onClick = {
                                            replyingToMessage = message
                                            selectedMessage = null
                                        }) {
                                            Text("Reply", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Show the replying message context above the input if in reply mode
                replyingToMessage?.let {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFDDDDDD))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Replying to: ${it.text}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { replyingToMessage = null }) {
                            Text("✕") // Close reply mode
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Input field and send button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, shape = RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.Gray, shape = RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (messageText.isEmpty()) {
                                    Text("Type a message...", style = MaterialTheme.typography.bodyMedium)
                                }
                                innerTextField()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                if (isEditing && selectedMessage != null) {
                                    updateMessage(reportId, selectedMessage!!.id, messageText)
                                    isEditing = false
                                    selectedMessage = null
                                } else {
                                    sendMessage(reportId, messageText, userId, replyingToMessage)
                                    replyingToMessage = null
                                }
                                messageText = ""
                            }
                        }
                    ) {
                        Text(if (isEditing) "Save" else "Send")
                    }
                }
            }
        }
    )
}

@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isCurrentUser) Color(0xFFD1FFC4) else Color(0xFFE0E0E0),
            tonalElevation = 2.dp,
            modifier = Modifier
                .padding(4.dp)
                .widthIn(max = (LocalConfiguration.current.screenWidthDp * 0.75).dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
            ) {
                message.replyingToMessageText?.let { repliedText ->
                    Text(
                        text = "Replying to: $repliedText",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentUser) Color.Black else Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.timestamp.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = if (isCurrentUser) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

fun sendMessage(reportId: String, text: String, userId: String?, replyingToMessage: Message?) {
    val firestore = FirebaseFirestore.getInstance()
    val messageData = hashMapOf(
        "text" to text,
        "userId" to userId,
        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        "replyingToMessageText" to replyingToMessage?.text
    )

    firestore.collection("emergencyReports").document(reportId)
        .collection("chat")
        .add(messageData)
        .addOnSuccessListener {
            Log.d("ChatScreen", "Message successfully sent!")
        }
        .addOnFailureListener { e ->
            Log.w("ChatScreen", "Error sending message", e)
        }
}

fun deleteMessage(reportId: String, messageId: String) {
    val firestore = FirebaseFirestore.getInstance()
    firestore.collection("emergencyReports").document(reportId)
        .collection("chat").document(messageId)
        .delete()
        .addOnSuccessListener {
            Log.d("ChatScreen", "Message successfully deleted!")
        }
        .addOnFailureListener { e ->
            Log.w("ChatScreen", "Error deleting message", e)
        }
}

fun updateMessage(reportId: String, messageId: String, newText: String) {
    val firestore = FirebaseFirestore.getInstance()
    val updatedData = mapOf("text" to newText)

    firestore.collection("emergencyReports").document(reportId)
        .collection("chat").document(messageId)
        .update(updatedData)
        .addOnSuccessListener {
            Log.d("ChatScreen", "Message successfully updated!")
        }
        .addOnFailureListener { e ->
            Log.w("ChatScreen", "Error updating message", e)
        }
}
