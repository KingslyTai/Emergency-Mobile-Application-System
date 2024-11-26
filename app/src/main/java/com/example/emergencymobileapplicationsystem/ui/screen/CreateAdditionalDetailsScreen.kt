package com.example.emergencymobileapplicationsystem.ui.screen

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.emergencymobileapplicationsystem.data.AdditionalDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAdditionalDetailsScreen(
    navController: NavController,
    onSaveSuccess: () -> Unit
) {
    var selectedDisability by remember { mutableStateOf("") }
    var otherDisability by remember { mutableStateOf("") }

    var selectedAllergy by remember { mutableStateOf("None") }
    var otherAllergy by remember { mutableStateOf("") }

    var selectedBloodType by remember { mutableStateOf("") }
    var otherBloodType by remember { mutableStateOf("") }

    var selectedMedicalCondition by remember { mutableStateOf("None") }
    var otherMedicalCondition by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Expanded states for dropdown menus
    var disabilityExpanded by remember { mutableStateOf(false) }
    var allergyExpanded by remember { mutableStateOf(false) }
    var bloodTypeExpanded by remember { mutableStateOf(false) }
    var medicalConditionExpanded by remember { mutableStateOf(false) }

    // Options for dropdown menus
    val disabilityOptions = listOf(
        "Mobility Impairments", "Visual Impairment", "Hearing Impairment",
        "Intellectual Disabilities", "Autism Spectrum Disorder",
        "Learning Disabilities", "Chronic Illness", "Other"
    )
    val allergyOptions = listOf(
        "Peanuts", "Shellfish", "Milk", "Eggs", "Pollen", "Other"
    )
    val bloodTypeOptions = listOf(
        "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    )
    val medicalConditionOptions = listOf(
        "Asthma", "Diabetes", "Hypertension", "Heart Disease",
        "Arthritis", "Epilepsy", "Allergies", "Other"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Create Additional Details") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // DropdownMenu for Disabilities
        DropdownWithOtherOption(
            label = "Type of Disability",
            selectedValue = selectedDisability,
            onValueSelected = { selectedDisability = it },
            options = disabilityOptions,
            isExpanded = disabilityExpanded,
            onExpandChange = { disabilityExpanded = it },
            otherValue = otherDisability,
            onOtherValueChange = { otherDisability = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // DropdownMenu for Allergies
        DropdownWithOtherOption(
            label = "Allergies",
            selectedValue = selectedAllergy,
            onValueSelected = { selectedAllergy = it },
            options = allergyOptions,
            isExpanded = allergyExpanded,
            onExpandChange = { allergyExpanded = it },
            otherValue = otherAllergy,
            onOtherValueChange = { otherAllergy = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // DropdownMenu for Blood Type
        DropdownWithOtherOption(
            label = "Blood Type",
            selectedValue = selectedBloodType,
            onValueSelected = { selectedBloodType = it },
            options = bloodTypeOptions,
            isExpanded = bloodTypeExpanded,
            onExpandChange = { bloodTypeExpanded = it },
            otherValue = otherBloodType,
            onOtherValueChange = { otherBloodType = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // DropdownMenu for Medical Conditions
        DropdownWithOtherOption(
            label = "Medical Conditions",
            selectedValue = selectedMedicalCondition,
            onValueSelected = { selectedMedicalCondition = it },
            options = medicalConditionOptions,
            isExpanded = medicalConditionExpanded,
            onExpandChange = { medicalConditionExpanded = it },
            otherValue = otherMedicalCondition,
            onOtherValueChange = { otherMedicalCondition = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display error message
        errorMessage?.let {
            Text(text = it, color = Color.Red)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Save Button
        Button(
            onClick = {
                isSaving = true
                val finalDisability = if (selectedDisability == "Other") otherDisability else selectedDisability
                val finalAllergy = if (selectedAllergy == "Other") otherAllergy else selectedAllergy
                val finalBloodType = if (selectedBloodType == "Other") otherBloodType else selectedBloodType
                val finalMedicalCondition = if (selectedMedicalCondition == "Other") otherMedicalCondition else selectedMedicalCondition

                val additionalDetails = AdditionalDetails(
                    disability = finalDisability,
                    allergies = finalAllergy,
                    bloodType = finalBloodType,
                    medicalConditions = finalMedicalCondition
                )
                saveAdditionalDetailsToFirebase(additionalDetails, {
                    isSaving = false
                    onSaveSuccess()
                }, { error ->
                    isSaving = false
                    errorMessage = error
                })
            },
            enabled = !isSaving &&
                    (selectedDisability != "Other" || otherDisability.isNotBlank()) &&
                    (selectedAllergy != "Other" || otherAllergy.isNotBlank()) &&
                    (selectedBloodType != "Other" || otherBloodType.isNotBlank()) &&
                    (selectedMedicalCondition != "Other" || otherMedicalCondition.isNotBlank()),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (isSaving) "Saving..." else "Save")
        }
    }
}

@Composable
fun DropdownWithOtherOption(
    label: String,
    selectedValue: String,
    onValueSelected: (String) -> Unit,
    options: List<String>,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    otherValue: String,
    onOtherValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            label = { Text("Select $label") },
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Dropdown Arrow",
                    modifier = Modifier.clickable { onExpandChange(!isExpanded) }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandChange(true) }
        )
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { onExpandChange(false) },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onValueSelected(option)
                        onExpandChange(false)
                    },
                    text = { Text(option) }
                )
            }
        }
        if (selectedValue == "Other") {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = otherValue,
                onValueChange = { onOtherValueChange(it) },
                label = { Text("Specify Other $label") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Firebase Saving Function
private fun saveAdditionalDetailsToFirebase(
    details: AdditionalDetails,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    if (userId != null) {
        firestore.collection("additionalDetails").document(userId)
            .set(details)
            .addOnSuccessListener {
                Log.d("FirebaseSuccess", "Additional details saved successfully.")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseError", "Failed to save additional details: ${exception.message}")
                onError("Failed to save additional details: ${exception.message}")
            }
    } else {
        Log.e("FirebaseError", "User ID is null, cannot save additional details.")
        onError("User not logged in. Please log in to save details.")
    }
}
