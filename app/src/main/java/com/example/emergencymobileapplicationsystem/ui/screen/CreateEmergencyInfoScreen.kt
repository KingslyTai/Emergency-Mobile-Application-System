package com.example.emergencymobileapplicationsystem.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.emergencymobileapplicationsystem.data.PlaceInfo
import com.google.android.gms.maps.model.LatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEmergencyInfoScreen(
    navController: NavController,
    onSaveEmergencyLocation: (PlaceInfo, String, (Boolean, String?) -> Unit) -> Unit // Pass PlaceInfo, Type, and a callback
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("police") } // Default type selection is police
    var isLatitudeValid by remember { mutableStateOf(true) }
    var isLongitudeValid by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) } // To handle and display error messages

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopAppBar(
            title = { Text("") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Input fields for name, address, latitude, longitude, and phone number
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Latitude field with validation
        TextField(
            value = latitude,
            onValueChange = {
                latitude = it
                isLatitudeValid = latitude.toDoubleOrNull() != null
            },
            label = { Text("Latitude") },
            modifier = Modifier.fillMaxWidth(),
            isError = !isLatitudeValid
        )
        if (!isLatitudeValid) {
            Text("Invalid Latitude", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Longitude field with validation
        TextField(
            value = longitude,
            onValueChange = {
                longitude = it
                isLongitudeValid = longitude.toDoubleOrNull() != null
            },
            label = { Text("Longitude") },
            modifier = Modifier.fillMaxWidth(),
            isError = !isLongitudeValid
        )
        if (!isLongitudeValid) {
            Text("Invalid Longitude", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(8.dp))

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
            // Police button
            Button(
                onClick = { selectedType = "police" },
                colors = ButtonDefaults.buttonColors(
                    if (selectedType == "police") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Police")
            }

            // Fire Station button
            Button(
                onClick = { selectedType = "fire_station" },
                colors = ButtonDefaults.buttonColors(
                    if (selectedType == "fire_station") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Fire")
            }

            // Hospital button
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

        // Display error message if it exists
        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
        }

        // Save Button
        Button(onClick = {
            val lat = latitude.toDoubleOrNull()
            val lon = longitude.toDoubleOrNull()

            if (lat != null && lon != null) {
                val newPlaceInfo = PlaceInfo(
                    latLng = LatLng(lat, lon),
                    name = name,
                    address = address,
                    phoneNumber = phoneNumber
                )
                // Pass the place information, type, and a callback for success/failure
                onSaveEmergencyLocation(newPlaceInfo, selectedType) { success, error ->
                    if (success) {
                        navController.navigate("createEmergencyInfoSuccessScreen/Success") // Pass message to success screen
                    } else {
                        errorMessage = error ?: "Unknown error occurred"
                    }
                }
            } else {
                // Handle invalid inputs
                isLatitudeValid = lat != null
                isLongitudeValid = lon != null
            }
        }) {
            Text("Create Emergency Location")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cancel Button
        Button(onClick = { navController.navigateUp() }) {
            Text("Cancel")
        }
    }
}
