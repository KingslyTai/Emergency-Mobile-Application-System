package com.example.emergencymobileapplicationsystem.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.emergencymobileapplicationsystem.data.AdditionalDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    refreshKey: Int,
    onNavigateToCreateProfile: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToCreateAdditionalDetails: () -> Unit,
    onNavigateToEditAdditionalDetails: () -> Unit,
    onDeleteProfile: () -> Unit,
    onDeleteAdditionalDetails: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current

    var profileExists by remember { mutableStateOf(false) }
    var additionalDetailsExists by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Profile fields
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }

    // Additional details fields
    var additionalDetails by remember { mutableStateOf<AdditionalDetails?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Fetch profile and additional details data whenever refreshKey changes
    LaunchedEffect(currentUser, refreshKey) {
        currentUser?.let { user ->
            coroutineScope.launch {
                isLoading = true
                try {
                    // Fetch profile data
                    val profileDoc = firestore.collection("profiles").document(user.uid).get().await()
                    if (profileDoc.exists()) {
                        profileExists = true
                        name = profileDoc.getString("name") ?: ""
                        age = profileDoc.getString("age") ?: ""
                        phone = profileDoc.getString("phone") ?: ""
                        address = profileDoc.getString("address") ?: ""
                        emergencyContact = profileDoc.getString("emergencyContact") ?: ""
                        dateOfBirth = profileDoc.getString("dateOfBirth") ?: ""
                        gender = profileDoc.getString("gender") ?: ""
                    } else {
                        profileExists = false
                    }

                    // Fetch additional details data
                    val additionalDetailsDoc = firestore.collection("additionalDetails").document(user.uid).get().await()
                    if (additionalDetailsDoc.exists()) {
                        additionalDetailsExists = true
                        additionalDetails = additionalDetailsDoc.toObject(AdditionalDetails::class.java)
                    } else {
                        additionalDetailsExists = false
                    }
                } catch (e: Exception) {
                    errorMessage = "Failed to load data: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        } ?: run {
            isLoading = false
            errorMessage = "User not logged in."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    isLoading -> {
                        item { CircularProgressIndicator() }
                    }
                    errorMessage != null -> {
                        item {
                            Text(
                                errorMessage ?: "An error occurred",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    else -> {
                        // Profile Section
                        item {
                            Text("Profile", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (!profileExists) {
                                Button(onClick = onNavigateToCreateProfile, modifier = Modifier.fillMaxWidth()) {
                                    Text("Create Profile")
                                }
                            } else {
                                ProfileDetailsSection(
                                    name = name,
                                    age = age,
                                    phone = phone,
                                    address = address,
                                    emergencyContact = emergencyContact,
                                    dateOfBirth = dateOfBirth,
                                    gender = gender,
                                    onEditClick = onNavigateToEditProfile,
                                    onDeleteClick = {
                                        coroutineScope.launch {
                                            deleteDocument(
                                                collection = "profiles",
                                                documentId = currentUser?.uid ?: "",
                                                onDeleteSuccess = {
                                                    profileExists = false
                                                    Toast.makeText(context, "Profile deleted successfully", Toast.LENGTH_SHORT).show()
                                                    onDeleteProfile()
                                                },
                                                onDeleteFailure = { errorMessage = it }
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        // Additional Details Section
                        item {
                            Text("Additional Details", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (!additionalDetailsExists) {
                                Button(onClick = onNavigateToCreateAdditionalDetails, modifier = Modifier.fillMaxWidth()) {
                                    Text("Create Additional Details")
                                }
                            } else {
                                AdditionalDetailsSection(
                                    additionalDetails = additionalDetails,
                                    onEditClick = onNavigateToEditAdditionalDetails,
                                    onDeleteClick = {
                                        coroutineScope.launch {
                                            deleteDocument(
                                                collection = "additionalDetails",
                                                documentId = currentUser?.uid ?: "",
                                                onDeleteSuccess = {
                                                    additionalDetailsExists = false
                                                    Toast.makeText(context, "Additional Details deleted successfully", Toast.LENGTH_SHORT).show()
                                                    onDeleteAdditionalDetails()
                                                },
                                                onDeleteFailure = { errorMessage = it }
                                            )
                                        }
                                    }
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
fun ProfileDetailsSection(
    name: String,
    age: String,
    phone: String,
    address: String,
    emergencyContact: String,
    dateOfBirth: String,
    gender: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Name: $name")
            Text("Age: $age")
            Text("Phone: $phone")
            Text("Address: $address")
            Text("Emergency Contact: $emergencyContact")
            Text("Date of Birth: $dateOfBirth")
            Text("Gender: $gender")

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onEditClick) {
                    Text("Edit")
                }
                Button(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun AdditionalDetailsSection(
    additionalDetails: AdditionalDetails?,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Disability: ${additionalDetails?.disability ?: "N/A"}")
            Text("Allergies: ${additionalDetails?.allergies ?: "N/A"}")
            Text("Blood Type: ${additionalDetails?.bloodType ?: "N/A"}")
            Text("Medical Conditions: ${additionalDetails?.medicalConditions ?: "N/A"}")

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onEditClick) {
                    Text("Edit")
                }
                Button(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

suspend fun deleteDocument(
    collection: String,
    documentId: String,
    onDeleteSuccess: () -> Unit,
    onDeleteFailure: (String) -> Unit
) {
    try {
        FirebaseFirestore.getInstance().collection(collection).document(documentId)
            .delete()
            .await()
        onDeleteSuccess()
    } catch (e: Exception) {
        onDeleteFailure("Failed to delete document in $collection: ${e.message}")
    }
}
