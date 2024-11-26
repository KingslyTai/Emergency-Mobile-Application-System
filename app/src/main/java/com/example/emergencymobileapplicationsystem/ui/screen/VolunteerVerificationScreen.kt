package com.example.emergencymobileapplicationsystem.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergencymobileapplicationsystem.data.AdditionalDetails
import com.example.emergencymobileapplicationsystem.data.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerVerificationScreen(
    onNavigateBack: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingVolunteers by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var profilesMap by remember { mutableStateOf(mapOf<String, String>()) }
    var isLoading by remember { mutableStateOf(true) }

    // States for user profile dialog
    var showUserDetailsDialog by remember { mutableStateOf(false) }
    var selectedUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var selectedUserDetails by remember { mutableStateOf<AdditionalDetails?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }

    // Fetch pending volunteers
    fun fetchPendingVolunteers() {
        isLoading = true
        firestore.collection("pendingVolunteers").get()
            .addOnSuccessListener { snapshot ->
                pendingVolunteers = snapshot.documents.map { it.data!! }
                isLoading = false
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to fetch pending volunteers: ${e.message}", Toast.LENGTH_SHORT).show()
                isLoading = false
                Log.e("VolunteerVerification", "Error fetching pending volunteers: ${e.message}")
            }
    }

    // Fetch profiles
    suspend fun fetchProfilesFromFirebase(): Map<String, String> {
        return try {
            FirebaseFirestore.getInstance().collection("profiles")
                .get()
                .await()
                .associate { document ->
                    val userId = document.id
                    val name = document.getString("name") ?: "Unknown"
                    userId to name
                }
        } catch (e: Exception) {
            Log.e("fetchProfilesFromFirebase", "Error: ${e.message}")
            emptyMap()
        }
    }

    // Fetch user profile and additional details
    suspend fun fetchUserDetails(userId: String) {
        isLoadingDetails = true
        try {
            val profileSnapshot = firestore.collection("profiles").document(userId).get().await()
            selectedUserProfile = profileSnapshot.toObject(UserProfile::class.java)

            val detailsSnapshot = firestore.collection("additionalDetails").document(userId).get().await()
            selectedUserDetails = detailsSnapshot.toObject(AdditionalDetails::class.java)
        } catch (e: Exception) {
            Log.e("VolunteerVerification", "Error fetching user details: ${e.message}")
            Toast.makeText(context, "Failed to fetch user details.", Toast.LENGTH_SHORT).show()
        } finally {
            isLoadingDetails = false
            showUserDetailsDialog = true
        }
    }

    // Update volunteer status
    fun updateVolunteerStatus(userId: String, isApproved: Boolean) {
        val documentRef = firestore.collection("pendingVolunteers").document(userId)
        documentRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    if (isApproved) {
                        val volunteerData = snapshot.data!!
                        firestore.collection("volunteers").document(userId)
                            .set(volunteerData)
                            .addOnSuccessListener {
                                documentRef.delete()
                                Toast.makeText(context, "Volunteer approved successfully!", Toast.LENGTH_SHORT).show()
                                fetchPendingVolunteers()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error approving volunteer: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        documentRef.update("status", "Rejected")
                            .addOnSuccessListener {
                                Toast.makeText(context, "Volunteer rejected successfully!", Toast.LENGTH_SHORT).show()
                                fetchPendingVolunteers()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error rejecting volunteer: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            profilesMap = fetchProfilesFromFirebase()
            fetchPendingVolunteers()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Volunteer Verification") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (pendingVolunteers.isEmpty()) {
                    Text(
                        text = "No pending volunteer requests.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pendingVolunteers) { volunteer ->
                            VolunteerVerificationItem(
                                volunteer = volunteer,
                                profilesMap = profilesMap,
                                onNameClick = { userId ->
                                    scope.launch {
                                        fetchUserDetails(userId)
                                    }
                                },
                                onApprove = { userId -> updateVolunteerStatus(userId, true) },
                                onReject = { userId -> updateVolunteerStatus(userId, false) }
                            )
                        }
                    }
                }
            }
        }
    )

    if (showUserDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showUserDetailsDialog = false },
            title = { Text("User Details") },
            text = {
                if (isLoadingDetails) {
                    CircularProgressIndicator()
                } else {
                    Column {
                        selectedUserProfile?.let { profile ->
                            Text("Name: ${profile.name}")
                            Text("Age: ${profile.age}")
                            Text("Phone: ${profile.phone}")
                            Text("Address: ${profile.address}")
                            Text("Emergency Contact: ${profile.emergencyContact}")
                            Text("Date of Birth: ${profile.dateOfBirth}")
                            Text("Gender: ${profile.gender}")
                        } ?: Text("No profile information available.")

                        Spacer(modifier = Modifier.height(16.dp))

                        selectedUserDetails?.let { details ->
                            Text("Disability: ${details.disability}")
                            Text("Allergies: ${details.allergies}")
                            Text("Blood Type: ${details.bloodType}")
                            Text("Medical Conditions: ${details.medicalConditions}")
                        } ?: Text("No additional details available.")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showUserDetailsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun VolunteerVerificationItem(
    volunteer: Map<String, Any>,
    profilesMap: Map<String, String>,
    onNameClick: (String) -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    val userId = volunteer["userId"] as String
    val name = profilesMap[userId] ?: "Unknown"
    val skills = (volunteer["skills"] as? List<*>)?.joinToString(", ") ?: "No skills listed"
    val experience = volunteer["experience"] as? String ?: "No experience provided"
    val preferredLocation = volunteer["preferredLocation"] as? String ?: "No preferred location"
    val certification = volunteer["certification"] as? String ?: "No certification provided"
    val languages = (volunteer["languageSpoken"] as? List<*>)?.joinToString(", ") ?: "No languages listed"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Name: $name",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onNameClick(userId) }
            )
            Text("Skills: $skills", style = MaterialTheme.typography.bodyMedium)
            Text("Experience: $experience", style = MaterialTheme.typography.bodyMedium)
            Text("Preferred Location: $preferredLocation", style = MaterialTheme.typography.bodyMedium)
            Text("Certification: $certification", style = MaterialTheme.typography.bodyMedium)
            Text("Languages: $languages", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onApprove(userId) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Approve")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve")
                }

                Button(
                    onClick = { onReject(userId) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Reject")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }
            }
        }
    }
}
