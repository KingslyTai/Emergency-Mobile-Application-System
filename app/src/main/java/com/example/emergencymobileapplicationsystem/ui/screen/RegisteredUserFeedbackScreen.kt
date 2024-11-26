package com.example.emergencymobileapplicationsystem.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.emergencymobileapplicationsystem.data.Feedback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisteredUserFeedbackScreen(
    navController: NavController,
    onCreateFeedbackClick: () -> Unit,
    onCommentClick: (String) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: ""
    var feedbackList by remember { mutableStateOf<List<Feedback>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }
    var editDialogOpen by remember { mutableStateOf(false) }
    var feedbackToEdit by remember { mutableStateOf<Feedback?>(null) }
    var editedContent by remember { mutableStateOf("") }
    var authorNameMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Fetch feedback and author names
    LaunchedEffect(Unit) {
        firestore.collection("profiles").get()
            .addOnSuccessListener { result ->
                val nameMap = result.documents.associate { doc ->
                    val userId = doc.id
                    val name = doc.getString("name") ?: ""
                    userId to name
                }
                authorNameMap = nameMap
            }

        firestore.collection("feedback").get()
            .addOnSuccessListener { result ->
                val list = result.documents.map { document ->
                    Feedback(
                        id = document.id,
                        content = document.getString("content") ?: "",
                        author = document.getString("author") ?: "",
                        userId = document.getString("userId") ?: ""
                    )
                }
                feedbackList = list
            }
            .addOnFailureListener { exception ->
                errorMessage = "Error fetching feedback: ${exception.message}"
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Feedback",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateFeedbackClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add Feedback")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                feedbackList.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(feedbackList) { feedback ->
                            var commentCount by remember { mutableStateOf(0) }
                            var expanded by remember { mutableStateOf(false) }

                            LaunchedEffect(feedback.id) {
                                firestore.collection("feedback")
                                    .document(feedback.id)
                                    .collection("comments")
                                    .get()
                                    .addOnSuccessListener { result ->
                                        commentCount = result.size()
                                    }
                            }

                            val authorDisplayName = authorNameMap[feedback.userId]
                                ?.takeIf { it.isNotBlank() }
                                ?: feedback.author

                            FeedbackCard(
                                feedback = feedback,
                                authorDisplayName = authorDisplayName,
                                commentCount = commentCount,
                                expanded = expanded,
                                onExpandChange = { expanded = it },
                                onEditClick = {
                                    expanded = false
                                    feedbackToEdit = feedback
                                    editedContent = feedback.content
                                    editDialogOpen = true
                                },
                                onDeleteClick = {
                                    expanded = false
                                    firestore.collection("feedback").document(feedback.id)
                                        .delete()
                                        .addOnSuccessListener {
                                            feedbackList = feedbackList.filter { it.id != feedback.id }
                                        }
                                        .addOnFailureListener { exception ->
                                            errorMessage = "Failed to delete feedback: ${exception.message}"
                                        }
                                },
                                onCommentClick = { onCommentClick(feedback.id) },
                                isOwner = feedback.userId == userId
                            )
                        }
                    }
                }
                errorMessage.isNotEmpty() -> {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Text(
                        text = "No feedback available.",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (editDialogOpen) {
        AlertDialog(
            onDismissRequest = { editDialogOpen = false },
            title = { Text("Edit Feedback") },
            text = {
                TextField(
                    value = editedContent,
                    onValueChange = { editedContent = it },
                    label = { Text("Feedback Content") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    feedbackToEdit?.let { feedback ->
                        firestore.collection("feedback").document(feedback.id)
                            .update("content", editedContent)
                            .addOnSuccessListener {
                                feedbackList = feedbackList.map {
                                    if (it.id == feedback.id) it.copy(content = editedContent) else it
                                }
                                editDialogOpen = false
                            }
                            .addOnFailureListener { exception ->
                                errorMessage = "Failed to edit feedback: ${exception.message}"
                                editDialogOpen = false
                            }
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { editDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FeedbackCard(
    feedback: Feedback,
    authorDisplayName: String,
    commentCount: Int,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCommentClick: () -> Unit,
    isOwner: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Author: $authorDisplayName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isOwner) {
                    Box {
                        IconButton(onClick = { onExpandChange(!expanded) }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { onExpandChange(false) }
                        ) {
                            DropdownMenuItem(onClick = onEditClick) { Text("Edit") }
                            DropdownMenuItem(onClick = onDeleteClick) { Text("Delete") }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = feedback.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Comments: $commentCount", style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = onCommentClick,
                    modifier = Modifier.padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                ) {
                    Text("Comment", color = Color.White)
                }
            }
        }
    }
}
