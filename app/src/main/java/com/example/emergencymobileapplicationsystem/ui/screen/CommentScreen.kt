package com.example.emergencymobileapplicationsystem.ui.screen

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(navController: NavController, feedbackId: String) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var commentText by remember { mutableStateOf("") }
    var commentsList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var feedback by remember { mutableStateOf<Map<String, Any>?>(null) }
    var editingComment by remember { mutableStateOf<String?>(null) }
    var commentBeingEdited by remember { mutableStateOf<String?>(null) }
    val errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(feedbackId) {
        firestore.collection("feedback").document(feedbackId)
            .addSnapshotListener { documentSnapshot, _ ->
                documentSnapshot?.let { document ->
                    feedback = document.data
                }
            }

        refreshComments(firestore, feedbackId) { updatedComments ->
            commentsList = updatedComments
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comments", fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    feedback?.let {
                        FeedbackCard(feedback = it)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Comments",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } ?: run {
                        Text(
                            text = "Loading feedback...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    commentsList.forEach { comment ->
                        val commentId = comment["id"].toString()
                        val commentAuthor = comment["author"].toString()
                        val commentTextContent = comment["comment"].toString()
                        var showMenu by remember { mutableStateOf(false) }

                        if (editingComment == commentId) {
                            TextField(
                                value = commentBeingEdited ?: commentTextContent,
                                onValueChange = { commentBeingEdited = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Edit Comment") }
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        editComment(firestore, feedbackId, commentId, commentBeingEdited ?: "") {
                                            editingComment = null
                                            commentBeingEdited = null
                                            refreshComments(firestore, feedbackId) { updatedComments ->
                                                commentsList = updatedComments
                                            }
                                        }
                                    }
                                ) {
                                    Text("Save")
                                }
                                Button(onClick = { editingComment = null }) {
                                    Text("Cancel")
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            if (currentUser?.email == commentAuthor) {
                                                showMenu = true
                                            }
                                        }
                                    )
                                }
                            ) {
                                CommentBubble(commentText = commentTextContent, author = commentAuthor, isCurrentUser = currentUser?.email == commentAuthor)

                                if (showMenu) {
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit") },
                                            onClick = {
                                                editingComment = commentId
                                                commentBeingEdited = commentTextContent
                                                showMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete") },
                                            onClick = {
                                                deleteComment(firestore, feedbackId, commentId) {
                                                    refreshComments(firestore, feedbackId) { updatedComments ->
                                                        commentsList = updatedComments
                                                    }
                                                }
                                                showMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (errorMessage.isNotEmpty()) {
                        Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    TextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        label = { Text("Add a comment") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    Button(
                        onClick = {
                            currentUser?.email?.let { email ->
                                addCommentToFeedback(firestore, feedbackId, email, commentText) {
                                    commentText = ""
                                    refreshComments(firestore, feedbackId) { updatedComments ->
                                        commentsList = updatedComments
                                    }
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Submit Comment")
                    }
                }
            }
        }
    )
}

@Composable
fun CommentBubble(commentText: String, author: String, isCurrentUser: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = author,
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 12.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = commentText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
fun FeedbackCard(feedback: Map<String, Any>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Author: ${feedback["author"]}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = feedback["content"]?.toString() ?: "No content available",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


fun addCommentToFeedback(
    firestore: FirebaseFirestore,
    feedbackId: String,
    author: String,
    comment: String,
    onSuccess: () -> Unit
) {
    val commentData = hashMapOf(
        "author" to author,
        "comment" to comment
    )
    firestore.collection("feedback").document(feedbackId).collection("comments")
        .add(commentData)
        .addOnSuccessListener {
            onSuccess()
        }
}

fun editComment(
    firestore: FirebaseFirestore,
    feedbackId: String,
    commentId: String,
    newCommentText: String,
    onSuccess: () -> Unit
) {
    firestore.collection("feedback").document(feedbackId).collection("comments").document(commentId)
        .update("comment", newCommentText)
        .addOnSuccessListener {
            onSuccess()
        }
}

fun deleteComment(
    firestore: FirebaseFirestore,
    feedbackId: String,
    commentId: String,
    onSuccess: () -> Unit
) {
    firestore.collection("feedback").document(feedbackId).collection("comments").document(commentId)
        .delete()
        .addOnSuccessListener {
            onSuccess()
        }
}

fun refreshComments(
    firestore: FirebaseFirestore,
    feedbackId: String,
    onCommentsFetched: (List<Map<String, Any>>) -> Unit
) {
    firestore.collection("feedback").document(feedbackId).collection("comments")
        .get()
        .addOnSuccessListener { result ->
            val updatedComments = result.documents.map { document ->
                mapOf(
                    "id" to document.id,
                    "author" to (document.getString("author") ?: "Unknown"),
                    "comment" to (document.getString("comment") ?: "")
                )
            }
            onCommentsFetched(updatedComments)
        }
}
