package com.example.emergencymobileapplicationsystem.ui.screen

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
import com.example.emergencymobileapplicationsystem.data.Feedback
import com.google.firebase.firestore.FirebaseFirestore

data class Comment(
    val author: String = "Unknown",
    val content: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewCommentsScreen(navController: NavController, feedbackId: String) {
    val firestore = FirebaseFirestore.getInstance()
    var feedback by remember { mutableStateOf<Feedback?>(null) }
    var commentsList by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    // Fetch feedback details and comments for the selected feedback
    LaunchedEffect(feedbackId) {
        isLoading = true
        firestore.collection("feedback").document(feedbackId).get()
            .addOnSuccessListener { document ->
                feedback = if (document.exists()) {
                    Feedback(
                        id = document.id,
                        content = document.getString("content") ?: "",
                        author = document.getString("author") ?: "Unknown"
                    )
                } else null
            }
            .addOnFailureListener { exception ->
                errorMessage = "Error fetching feedback: ${exception.message}"
            }

        firestore.collection("feedback").document(feedbackId).collection("comments")
            .get()
            .addOnSuccessListener { result ->
                commentsList = if (!result.isEmpty) {
                    result.documents.map { document ->
                        Comment(
                            author = document.getString("author") ?: "Unknown",
                            content = document.getString("comment") ?: ""
                        )
                    }
                } else emptyList()
                isLoading = false
            }
            .addOnFailureListener { exception ->
                errorMessage = "Error fetching comments: ${exception.message}"
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Comments",
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
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    errorMessage.isNotEmpty() -> {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    feedback == null -> {
                        Text(
                            text = "Feedback not found.",
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            FeedbackCard(feedback!!)

                            Text(
                                text = "Comments",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            if (commentsList.isEmpty()) {
                                Text(
                                    text = "No comments available.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(commentsList) { comment ->
                                        CommentCard(comment)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun FeedbackCard(feedback: Feedback) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Author: ${feedback.author}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = feedback.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CommentCard(comment: Comment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Author: ${comment.author}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
