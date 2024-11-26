package com.example.emergencymobileapplicationsystem.ui.screen

import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVolunteerScreen(
    onNavigateToStatus: () -> Unit, // Callback to navigate to the Volunteer Status screen
    onNavigateBack: () -> Unit // Callback to navigate back to the previous screen
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Predefined skills for the dropdown menu
    val predefinedSkills = listOf(
        "Multilingual Communication",
        "Basic Translation Skills",
        "Licensed Driver",
        "First Aid and CPR",
        "Emotional Support"
    )
    val selectedSkills = remember { mutableStateMapOf<String, Boolean>() }
    var skillsExpanded by remember { mutableStateOf(false) }

    // Input fields
    var experience by remember { mutableStateOf("") }
    var preferredLocation by remember { mutableStateOf("") }
    var certification by remember { mutableStateOf("") }

    // Languages dropdown menu
    val languageOptions = listOf("Malay", "English", "Chinese")
    val selectedLanguages = remember { mutableStateMapOf<String, Boolean>().apply {
        languageOptions.forEach { this[it] = false }
    } }
    var languageExpanded by remember { mutableStateOf(false) }
    val selectedLanguagesText = selectedLanguages.filter { it.value }.keys.joinToString(", ")

    // State variables for submission and status
    var isUploading by remember { mutableStateOf(false) }
    var isPending by remember { mutableStateOf(false) }

    // Check if the user's registration status is pending
    LaunchedEffect(auth.currentUser?.uid) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("pendingVolunteers").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.getString("status") == "Pending") {
                        isPending = true
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("CreateVolunteerScreen", "Error fetching pending status: ${e.message}")
                }
        }
    }

    // Function to submit the registration
    fun submitRegistration() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not logged in. Please log in to register.", Toast.LENGTH_SHORT).show()
            return
        }

        val volunteerData = mapOf(
            "userId" to userId,
            "skills" to selectedSkills.filter { it.value }.keys.toList(),
            "experience" to experience,
            "preferredLocation" to preferredLocation,
            "certification" to certification,
            "languageSpoken" to selectedLanguages.filter { it.value }.keys.toList(),
            "status" to "Pending",
            "startDate" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        isUploading = true
        firestore.collection("pendingVolunteers").document(userId)
            .set(volunteerData)
            .addOnSuccessListener {
                Toast.makeText(context, "Registration submitted successfully!", Toast.LENGTH_SHORT).show()
                isUploading = false
                isPending = true // Set status to pending after successful submission
            }
            .addOnFailureListener { e ->
                isUploading = false
                Toast.makeText(context, "Failed to submit registration: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("CreateVolunteerScreen", "Error: ${e.message}")
            }
    }

    // UI layout
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register as a Volunteer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") // Changed icon to <-- (ArrowBack)
                    }
                }
            )
        },
        content = { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Join as a Volunteer",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Conditional button based on registration status
                    if (isPending) {
                        Button(
                            onClick = onNavigateToStatus, // Navigate to status screen if pending
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Check Volunteer Application Status",
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Registration form
                        DropdownWithCheckboxes(
                            label = "Skills",
                            selectedItems = selectedSkills,
                            items = predefinedSkills,
                            expanded = skillsExpanded,
                            onExpandedChange = { skillsExpanded = it }
                        )

                        OutlinedTextField(
                            value = experience,
                            onValueChange = { experience = it },
                            label = { Text("Previous Experience") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )

                        OutlinedTextField(
                            value = preferredLocation,
                            onValueChange = { preferredLocation = it },
                            label = { Text("Preferred Location") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )

                        OutlinedTextField(
                            value = certification,
                            onValueChange = { certification = it },
                            label = { Text("Certification") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )

                        DropdownWithCheckboxes(
                            label = "Languages Spoken",
                            selectedItems = selectedLanguages,
                            items = languageOptions,
                            expanded = languageExpanded,
                            onExpandedChange = { languageExpanded = it }
                        )

                        Button(
                            onClick = { submitRegistration() },
                            enabled = !isUploading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(if (isUploading) "Submitting..." else "Submit Registration")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Back button
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    )
}

@Composable
fun DropdownWithCheckboxes(
    label: String,
    selectedItems: MutableMap<String, Boolean>,
    items: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        OutlinedTextField(
            value = selectedItems.filter { it.value }.keys.joinToString(", "),
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    Modifier.clickable { onExpandedChange(!expanded) }
                )
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selectedItems[item] == true,
                                onCheckedChange = { isChecked -> selectedItems[item] = isChecked }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(item)
                        }
                    },
                    onClick = {
                        selectedItems[item] = !(selectedItems[item] ?: false)
                    }
                )
            }
        }
    }
}
