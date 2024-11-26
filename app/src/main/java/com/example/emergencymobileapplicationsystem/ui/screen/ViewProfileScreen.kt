package com.example.emergencymobileapplicationsystem.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.emergencymobileapplicationsystem.data.AdditionalDetails
import com.example.emergencymobileapplicationsystem.data.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ViewProfileScreen(
    userId: String,
    onBack: () -> Unit
) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var additionalDetails by remember { mutableStateOf<AdditionalDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val firestore = FirebaseFirestore.getInstance()

    // Fetch user profile and additional details from Firestore
    LaunchedEffect(userId) {
        isLoading = true
        try {
            val profileSnapshot = firestore.collection("profiles").document(userId).get().await()
            profile = profileSnapshot.toObject(UserProfile::class.java)

            val detailsSnapshot = firestore.collection("additionalDetails").document(userId).get().await()
            additionalDetails = detailsSnapshot.toObject(AdditionalDetails::class.java)
        } catch (e: Exception) {
            Log.e("ViewProfileScreen", "Error fetching data: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("View Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)) // Subtle gray background
                    .padding(padding)
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Profile Section
                        profile?.let {
                            ProfileCard(profile = it)
                        } ?: Text(
                            text = "Profile not available.",
                            style = MaterialTheme.typography.body1,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Additional Details Section
                        additionalDetails?.let {
                            AdditionalDetailsCard(details = it)
                        } ?: Text(
                            text = "No additional details available.",
                            style = MaterialTheme.typography.body1,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ProfileCard(profile: UserProfile) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Name: ${profile.name}",
                style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold)
            )
            Text("Email: ${profile.email}", style = MaterialTheme.typography.body1)
            Text("Phone: ${profile.phone}", style = MaterialTheme.typography.body1)
            Text("Address: ${profile.address}", style = MaterialTheme.typography.body1)
            Text("Emergency Contact: ${profile.emergencyContact}", style = MaterialTheme.typography.body1)
            Text("Date of Birth: ${profile.dateOfBirth}", style = MaterialTheme.typography.body1)
            Text("Gender: ${profile.gender}", style = MaterialTheme.typography.body1)
        }
    }
}

@Composable
fun AdditionalDetailsCard(details: AdditionalDetails) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Disability: ${details.disability}",
                style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold)
            )
            Text("Allergies: ${details.allergies}", style = MaterialTheme.typography.body1)
            Text("Blood Type: ${details.bloodType}", style = MaterialTheme.typography.body1)
            Text("Medical Conditions: ${details.medicalConditions}", style = MaterialTheme.typography.body1)
        }
    }
}
