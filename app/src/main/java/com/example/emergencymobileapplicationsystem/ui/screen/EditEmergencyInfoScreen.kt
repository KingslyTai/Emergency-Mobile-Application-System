package com.example.emergencymobileapplicationsystem.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.emergencymobileapplicationsystem.data.PlaceInfo
import com.google.android.gms.maps.model.LatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEmergencyInfoScreen(
    navController: NavController,
    place: PlaceInfo,
    documentId: String?,
    isGoogleLocation: Boolean,  // Distinguish if the location is from Google
    onSaveChanges: (PlaceInfo, String?, Boolean, String) -> Unit  // Updated to pass type
) {
    // Initialize state variables with values from the `place` object
    var name by remember { mutableStateOf(place.name) }
    var address by remember { mutableStateOf(place.address) }
    var latitude by remember { mutableStateOf(place.latLng.latitude.toString()) }
    var longitude by remember { mutableStateOf(place.latLng.longitude.toString()) }
    var phoneNumber by remember { mutableStateOf(place.phoneNumber ?: "") }
    var selectedType by remember { mutableStateOf("police") }  // Default type is police

    var isLatitudeValid by remember { mutableStateOf(true) }
    var isLongitudeValid by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // Top App Bar with back button
        TopAppBar(
            title = { Text("Edit Emergency Info") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Name input field
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Address input field
        TextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Latitude input field with validation
        TextField(
            value = latitude,
            onValueChange = {
                latitude = it
                isLatitudeValid = it.toDoubleOrNull() != null
            },
            label = { Text("Latitude") },
            isError = !isLatitudeValid,
            modifier = Modifier.fillMaxWidth()
        )
        if (!isLatitudeValid) {
            Text(text = "Invalid Latitude", color = androidx.compose.ui.graphics.Color.Red)
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Longitude input field with validation
        TextField(
            value = longitude,
            onValueChange = {
                longitude = it
                isLongitudeValid = it.toDoubleOrNull() != null
            },
            label = { Text("Longitude") },
            isError = !isLongitudeValid,
            modifier = Modifier.fillMaxWidth()
        )
        if (!isLongitudeValid) {
            Text(text = "Invalid Longitude", color = androidx.compose.ui.graphics.Color.Red)
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Phone number input field
        TextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Type Selection (Police, Fire, Hospital)
        Text("Select Emergency Location Type:")
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
                onClick = { selectedType = "police" },
                colors = ButtonDefaults.buttonColors(
                    if (selectedType == "police") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Police")
            }

            Button(
                onClick = { selectedType = "fire_station" },
                colors = ButtonDefaults.buttonColors(
                    if (selectedType == "fire_station") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Fire")
            }

            Button(
                onClick = { selectedType = "hospital" },
                colors = ButtonDefaults.buttonColors(
                    if (selectedType == "hospital") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Hospital")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save button
        Button(onClick = {
            val lat = latitude.toDoubleOrNull()
            val lon = longitude.toDoubleOrNull()

            if (lat != null && lon != null) {
                val updatedPlace = place.copy(
                    name = name,
                    address = address,
                    latLng = LatLng(lat, lon),
                    phoneNumber = phoneNumber
                )

                // Call the onSaveChanges function with the updated place info, document ID, and type
                onSaveChanges(updatedPlace, documentId, isGoogleLocation, selectedType)

                // Navigate back after saving
                navController.navigateUp()
            }
        }, enabled = isLatitudeValid && isLongitudeValid) {
            Text("Save Changes")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
