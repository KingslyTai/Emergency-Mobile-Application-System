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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewFeedbackScreen(
    navController: NavController,
    onCommentClick: (String) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    var feedbackList by remember { mutableStateOf<List<Feedback>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    // Fetch feedback from Firestore
    LaunchedEffect(Unit) {
        firestore.collection("feedback")
            .get()
            .addOnSuccessListener { result ->
                feedbackList = if (!result.isEmpty) {
                    result.documents.map { document ->
                        Feedback(
                            id = document.id,
                            content = document.getString("content") ?: "",
                            author = document.getString("author") ?: ""
                        )
                    }
                } else emptyList()
                isLoading = false
            }
            .addOnFailureListener { exception ->
                errorMessage = "Error fetching feedback: ${exception.message}"
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Feedback",
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
                    feedbackList.isEmpty() -> {
                        Text(
                            text = "No feedback available.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(feedbackList) { feedback ->
                                FeedbackCard(
                                    feedback = feedback,
                                    onCommentClick = onCommentClick,
                                    firestore = firestore
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun FeedbackCard(
    feedback: Feedback,
    onCommentClick: (String) -> Unit,
    firestore: FirebaseFirestore
) {
    var commentCount by remember { mutableStateOf(0) }

    // Fetch the comment count for each feedback
    LaunchedEffect(feedback.id) {
        firestore.collection("feedback").document(feedback.id)
            .collection("comments")
            .get()
            .addOnSuccessListener { result ->
                commentCount = result.size()
            }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Author and Feedback Content
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
            Spacer(modifier = Modifier.height(12.dp))

            // Comment Count and Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Comments: $commentCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Button(
                    onClick = { onCommentClick(feedback.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "Comment",
                        style = MaterialTheme.typography.labelLarge.copy(color = Color.White)
                    )
                }
            }
        }
    }
}
