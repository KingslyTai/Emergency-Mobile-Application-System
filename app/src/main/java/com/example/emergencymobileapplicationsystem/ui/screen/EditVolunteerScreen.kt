package com.example.emergencymobileapplicationsystem.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVolunteerScreen(
    onSaveChanges: () -> Unit, // Callback for navigating back after saving
    onCancel: () -> Unit // Callback for navigating back without saving
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Predefined options for skills and languages
    val predefinedSkills = listOf("Multilingual Communication", "Basic Translation Skills", "Licensed Driver", "First Aid and CPR", "Emotional Support")
    val languageOptions = listOf("Malay", "English", "Chinese")

    // State variables for volunteer data fields
    val selectedSkills = remember { mutableStateMapOf<String, Boolean>() }
    val selectedLanguages = remember { mutableStateMapOf("Malay" to false, "English" to false, "Chinese" to false) }
    var skillsExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    var experience by remember { mutableStateOf("") }
    var preferredLocation by remember { mutableStateOf("") }
    var certification by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch current volunteer data from Firestore
    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                val document = firestore.collection("volunteers").document(userId).get().await()
                document.data?.let { data ->
                    experience = data["experience"] as? String ?: ""
                    preferredLocation = data["preferredLocation"] as? String ?: ""
                    certification = data["certification"] as? String ?: ""

                    // Load selected skills
                    val skills = data["skills"] as? List<*>
                    skills?.forEach { skill ->
                        if (skill is String && skill in predefinedSkills) {
                            selectedSkills[skill] = true
                        }
                    }

                    // Load selected languages
                    val languages = data["languageSpoken"] as? List<*>
                    languages?.forEach { language ->
                        if (language is String && language in languageOptions) {
                            selectedLanguages[language] = true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("EditVolunteerScreen", "Error fetching volunteer data: ${e.message}")
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    // Function to save the updated volunteer data to Firestore
    fun saveVolunteerData() {
        val updatedData = mapOf(
            "skills" to selectedSkills.filter { it.value }.keys.toList(),
            "languageSpoken" to selectedLanguages.filter { it.value }.keys.toList(),
            "experience" to experience,
            "preferredLocation" to preferredLocation,
            "certification" to certification
        )

        coroutineScope.launch {
            userId?.let {
                firestore.collection("volunteers").document(it).set(updatedData)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Volunteer data updated successfully.", Toast.LENGTH_SHORT).show()
                        onSaveChanges() // Navigate back after saving
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to update data: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("EditVolunteerScreen", "Error saving volunteer data: ${e.message}")
                    }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Text(
                    "Edit Volunteer Profile",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = Color.Gray
                )
                Text("Skills", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    OutlinedTextField(
                        value = selectedSkills.filter { it.value }.keys.joinToString(", "),
                        onValueChange = {},
                        label = { Text("Select Skills") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { skillsExpanded = true },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                Modifier.clickable { skillsExpanded = !skillsExpanded }
                            )
                        }
                    )
                    DropdownMenu(
                        expanded = skillsExpanded,
                        onDismissRequest = { skillsExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        predefinedSkills.forEach { skill ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = selectedSkills[skill] == true,
                                            onCheckedChange = { isChecked ->
                                                selectedSkills[skill] = isChecked
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(skill)
                                    }
                                },
                                onClick = {
                                    selectedSkills[skill] = !(selectedSkills[skill] ?: false)
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = Color.Gray
                )
                Text("Experience", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = experience,
                    onValueChange = { experience = it },
                    label = { Text("Previous Experience") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = Color.Gray
                )
                Text("Preferred Location", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = preferredLocation,
                    onValueChange = { preferredLocation = it },
                    label = { Text("Preferred Location") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = Color.Gray
                )
                Text("Certification", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = certification,
                    onValueChange = { certification = it },
                    label = { Text("Certification") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = Color.Gray
                )
                Text("Languages", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    OutlinedTextField(
                        value = selectedLanguages.filter { it.value }.keys.joinToString(", "),
                        onValueChange = {},
                        label = { Text("Select Languages") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { languageExpanded = true },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                Modifier.clickable { languageExpanded = !languageExpanded }
                            )
                        }
                    )
                    DropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        languageOptions.forEach { language ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = selectedLanguages[language] == true,
                                            onCheckedChange = { isChecked ->
                                                selectedLanguages[language] = isChecked
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(language)
                                    }
                                },
                                onClick = {
                                    selectedLanguages[language] =
                                        !(selectedLanguages[language] ?: false)
                                }
                            )
                        }
                    }
                }

                // Save and Cancel buttons
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { saveVolunteerData() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Text("Save", color = Color.White)
                    }

                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                }
            }
        }
    }
}
