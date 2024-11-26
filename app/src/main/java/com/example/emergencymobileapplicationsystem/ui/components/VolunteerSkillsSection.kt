package com.example.emergencymobileapplicationsystem.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

@Composable
fun VolunteerSkillsSection(
    predefinedSkills: List<String>,                    // List of predefined skills
    selectedSkills: MutableMap<String, Boolean>,       // Map to store selected skills
    onFileUploadRequest: (String) -> Unit,             // Callback to handle file upload request
    licensePhotoUri: Uri?,                             // License photo Uri
    onLicensePhotoChange: (Uri?) -> Unit               // Callback for license photo change
) {
    // Image picker launcher for the driver’s license photo
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onLicensePhotoChange(uri)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Select Your Skills", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        // Display checkboxes for each predefined skill
        predefinedSkills.forEach { skill ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = selectedSkills[skill] == true,
                    onCheckedChange = { isChecked ->
                        selectedSkills[skill] = isChecked
                        // Trigger file upload request for 'Licensed Driver' skill
                        if (skill == "Licensed Driver" && isChecked) {
                            onFileUploadRequest(skill)
                        } else if (skill == "Licensed Driver" && !isChecked) {
                            onLicensePhotoChange(null)  // Clear license photo if unchecked
                        }
                    }
                )
                Text(text = skill)
            }
        }

        // Show license photo upload option if 'Licensed Driver' is selected
        if (selectedSkills["Licensed Driver"] == true) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Upload Driver's License")

            // Display the selected image if available
            if (licensePhotoUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(licensePhotoUri),
                    contentDescription = "Driver's License",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(bottom = 8.dp)
                )
            }

            // Button to trigger image picker
            Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                Text("Upload License Photo")
            }
        }
    }
}
