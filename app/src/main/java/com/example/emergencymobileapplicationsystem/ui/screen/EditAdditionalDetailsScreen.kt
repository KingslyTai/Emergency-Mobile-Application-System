package com.example.emergencymobileapplicationsystem.ui.screen

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.emergencymobileapplicationsystem.data.AdditionalDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAdditionalDetailsScreen(
    navController: NavController,
    onSaveSuccess: (String) -> Unit,
    onDeleteSuccess: (String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    // State variables for form fields
    var selectedDisability by remember { mutableStateOf("") }
    var otherDisability by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var selectedMedicalCondition by remember { mutableStateOf("") }
    var otherMedicalCondition by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var disabilityExpanded by remember { mutableStateOf(false) }
    var medicalConditionExpanded by remember { mutableStateOf(false) }
    var bloodTypeExpanded by remember { mutableStateOf(false) }
    var allergiesExpanded by remember { mutableStateOf(false) }

    // Options for dropdown menus
    val disabilityOptions = listOf(
        "Mobility Impairments", "Visual Impairment", "Hearing Impairment",
        "Intellectual Disabilities", "Autism Spectrum Disorder", "Learning Disabilities",
        "Chronic Illness", "Other"
    )
    val medicalConditionOptions = listOf(
        "Asthma", "Diabetes", "Hypertension", "Heart Disease", "Arthritis",
        "Epilepsy", "None", "Other"
    )
    val bloodTypeOptions = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-", "Unknown")
    val allergiesOptions = listOf("Peanuts", "Shellfish", "Dust", "None", "Other")

    // Load existing data from Firebase
    LaunchedEffect(userId) {
        if (userId != null) {
            firestore.collection("additionalDetails").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val details = document.toObject(AdditionalDetails::class.java)
                    if (details != null) {
                        selectedDisability = details.disability
                        allergies = details.allergies
                        bloodType = details.bloodType
                        selectedMedicalCondition = details.medicalConditions
                    }
                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    Log.e("EditAdditionalDetails", "Failed to load data: ${exception.message}")
                    errorMessage = "Failed to load additional details."
                    isLoading = false
                }
        } else {
            errorMessage = "User not logged in."
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopAppBar(
            title = { Text("Edit Additional Details") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Text(text = "Loading...", color = Color.Gray)
        } else if (errorMessage != null) {
            Text(text = errorMessage ?: "", color = Color.Red)
        } else {
            // Dropdown for Disability
            Column {
                Text("Type of Disability")
                OutlinedTextField(
                    value = selectedDisability,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Dropdown Arrow",
                            modifier = Modifier.clickable { disabilityExpanded = !disabilityExpanded }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().clickable { disabilityExpanded = true }
                )
                DropdownMenu(
                    expanded = disabilityExpanded,
                    onDismissRequest = { disabilityExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    disabilityOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedDisability = option
                                disabilityExpanded = false
                            }
                        )
                    }
                }
            }
            if (selectedDisability == "Other") {
                OutlinedTextField(
                    value = otherDisability,
                    onValueChange = { otherDisability = it },
                    label = { Text("Specify Other Disability") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dropdown for Allergies
            Column {
                Text("Allergies")
                OutlinedTextField(
                    value = allergies,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Dropdown Arrow",
                            modifier = Modifier.clickable { allergiesExpanded = !allergiesExpanded }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().clickable { allergiesExpanded = true }
                )
                DropdownMenu(
                    expanded = allergiesExpanded,
                    onDismissRequest = { allergiesExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allergiesOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                allergies = option
                                allergiesExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dropdown for Blood Type
            Column {
                Text("Blood Type")
                OutlinedTextField(
                    value = bloodType,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Dropdown Arrow",
                            modifier = Modifier.clickable { bloodTypeExpanded = !bloodTypeExpanded }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().clickable { bloodTypeExpanded = true }
                )
                DropdownMenu(
                    expanded = bloodTypeExpanded,
                    onDismissRequest = { bloodTypeExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    bloodTypeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                bloodType = option
                                bloodTypeExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dropdown for Medical Conditions
            Column {
                Text("Medical Conditions")
                OutlinedTextField(
                    value = selectedMedicalCondition,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Dropdown Arrow",
                            modifier = Modifier.clickable { medicalConditionExpanded = !medicalConditionExpanded }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().clickable { medicalConditionExpanded = true }
                )
                DropdownMenu(
                    expanded = medicalConditionExpanded,
                    onDismissRequest = { medicalConditionExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    medicalConditionOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedMedicalCondition = option
                                medicalConditionExpanded = false
                            }
                        )
                    }
                }
            }
            if (selectedMedicalCondition == "Other") {
                OutlinedTextField(
                    value = otherMedicalCondition,
                    onValueChange = { otherMedicalCondition = it },
                    label = { Text("Specify Other Medical Condition") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    isSaving = true
                    val finalDisability =
                        if (selectedDisability == "Other") otherDisability else selectedDisability
                    val finalMedicalCondition =
                        if (selectedMedicalCondition == "Other") otherMedicalCondition else selectedMedicalCondition

                    val updatedDetails = AdditionalDetails(
                        disability = finalDisability,
                        allergies = allergies,
                        bloodType = bloodType,
                        medicalConditions = finalMedicalCondition
                    )
                    updateAdditionalDetailsInFirebase(updatedDetails) {
                        isSaving = false
                        onSaveSuccess("Details updated successfully")
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (isSaving) "Saving..." else "Save Changes")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Delete Button
            Button(
                onClick = {
                    isDeleting = true
                    deleteAdditionalDetailsFromFirebase {
                        isDeleting = false
                        onDeleteSuccess("Details deleted successfully")
                    }
                },
                enabled = !isDeleting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (isDeleting) "Deleting..." else "Delete")
            }
        }
    }
}

// Function to update additional details in Firebase
private fun updateAdditionalDetailsInFirebase(details: AdditionalDetails, onSuccess: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    if (userId != null) {
        firestore.collection("additionalDetails").document(userId)
            .set(details)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { /* Handle error if needed */ }
    }
}

// Function to delete additional details from Firebase
private fun deleteAdditionalDetailsFromFirebase(onSuccess: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    if (userId != null) {
        firestore.collection("additionalDetails").document(userId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { /* Handle error if needed */ }
    }
}
