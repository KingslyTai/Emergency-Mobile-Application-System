package com.example.emergencymobileapplicationsystem.ui.screen

import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.emergencymobileapplicationsystem.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    profileRepository: ProfileRepository = ProfileRepository(FirebaseFirestore.getInstance()) // Provide default repository instance
) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }

    var genderExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val currentUser = auth.currentUser

    // DatePickerDialog for Date of Birth
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            dateOfBirth = "${selectedDay.toString().padStart(2, '0')}/${(selectedMonth + 1).toString().padStart(2, '0')}/$selectedYear"
        },
        year,
        month,
        day
    )

    // Fetch current profile data using ProfileRepository
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            isLoading = true
            coroutineScope.launch {
                val result = profileRepository.getProfile(user.uid)
                result.onSuccess { data ->
                    name = data["name"] as? String ?: ""
                    age = data["age"] as? String ?: ""
                    phone = data["phone"] as? String ?: ""
                    address = data["address"] as? String ?: ""
                    emergencyContact = data["emergencyContact"] as? String ?: ""
                    dateOfBirth = data["dateOfBirth"] as? String ?: ""
                    gender = data["gender"] as? String ?: ""
                    isLoading = false
                }.onFailure { exception ->
                    Log.e("EditProfile", "Error fetching profile data", exception)
                    errorMessage = "Error fetching profile data: ${exception.message}"
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("Edit Profile") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }

        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        TextField(
            value = age,
            onValueChange = { age = it },
            label = { Text("Age") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        TextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        TextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        TextField(
            value = emergencyContact,
            onValueChange = { emergencyContact = it },
            label = { Text("Emergency Contact") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        // Date of Birth with DatePickerDialog and Calendar Icon
        OutlinedTextField(
            value = dateOfBirth,
            onValueChange = { },
            readOnly = true,
            label = { Text("Date of Birth") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Select Date of Birth",
                    modifier = Modifier.clickable {
                        datePickerDialog.show()
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable {
                    datePickerDialog.show()
                }
        )

        // Gender Dropdown Selection
        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = !genderExpanded }
        ) {
            OutlinedTextField(
                value = gender,
                onValueChange = { },
                readOnly = true,
                label = { Text("Gender") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = genderExpanded,
                onDismissRequest = { genderExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Male") },
                    onClick = {
                        gender = "Male"
                        genderExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Female") },
                    onClick = {
                        gender = "Female"
                        genderExpanded = false
                    }
                )
            }
        }

        Button(onClick = {
            if (name.isNotBlank() && age.isNotBlank() && phone.isNotBlank() &&
                address.isNotBlank() && emergencyContact.isNotBlank()) {

                currentUser?.let { user ->
                    isLoading = true
                    val updatedProfile = mapOf(
                        "name" to name,
                        "age" to age,
                        "phone" to phone,
                        "address" to address,
                        "emergencyContact" to emergencyContact,
                        "dateOfBirth" to dateOfBirth,
                        "gender" to gender
                    )

                    coroutineScope.launch {
                        val result = profileRepository.editProfile(user.uid, updatedProfile)
                        result.onSuccess {
                            isLoading = false
                            navController.navigate("CreateProfileSuccessScreen/Edit Profile Successful!")
                        }.onFailure { exception ->
                            isLoading = false
                            errorMessage = "Error updating profile: ${exception.message}"
                        }
                    }
                }
            } else {
                errorMessage = "All fields must be filled."
            }
        }) {
            Text("Save Changes")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                currentUser?.let { user ->
                    isLoading = true
                    coroutineScope.launch {
                        val result = profileRepository.deleteProfile(user.uid)
                        result.onSuccess {
                            isLoading = false
                            navController.navigate("CreateProfileSuccessScreen/Delete Profile Successful!")
                        }.onFailure { exception ->
                            isLoading = false
                            errorMessage = "Error deleting profile: ${exception.message}"
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
        ) {
            Text("Delete Profile")
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }
    }
}
