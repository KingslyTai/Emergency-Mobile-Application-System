package com.example.emergencymobileapplicationsystem.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    navController: NavController,
    userId: String?,
    onSaveReport: (String, String, Double?, Double?, String, String?, (String) -> Unit) -> Unit,
    onRefreshReports: () -> Unit
) {
    var selectedReportType by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                fetchLocation(context) { lat, lng ->
                    latitude = lat
                    longitude = lng
                }
            } else {
                Log.d("ReportScreen", "Location permission not granted.")
            }
        }
    )

    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLocation(context) { lat, lng ->
                latitude = lat
                longitude = lng
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Report", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    "Select Emergency Type",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Emergency Type Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EmergencyTypeButton(
                        text = "Police",
                        isSelected = selectedReportType == "Police",
                        onClick = { selectedReportType = "Police" }
                    )
                    EmergencyTypeButton(
                        text = "Fire",
                        isSelected = selectedReportType == "Fire Station",
                        onClick = { selectedReportType = "Fire Station" }
                    )
                    EmergencyTypeButton(
                        text = "Hospital",
                        isSelected = selectedReportType == "Hospital",
                        onClick = { selectedReportType = "Hospital" }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Form Section
                selectedReportType?.let {
                    Text(
                        "Create $selectedReportType Report",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Description Field
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        placeholder = { Text("Enter details about the emergency...") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Location Information
                    if (latitude != null && longitude != null) {
                        Text(
                            text = "Location: $latitude, $longitude",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                        )
                    } else {
                        Text(
                            text = "Fetching current location...",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (selectedReportType != null && description.isNotBlank()) {
                                onSaveReport(
                                    selectedReportType!!,
                                    description,
                                    latitude,
                                    longitude,
                                    "Open",
                                    userId
                                ) { reportId ->
                                    Log.d("ReportScreen", "Report created with ID: $reportId")
                                    onRefreshReports()
                                    navController.popBackStack("emergencyScreen", inclusive = false)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        enabled = description.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Submit Report", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    )
}

@Composable
fun EmergencyTypeButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(110.dp)
            .height(48.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

private fun fetchLocation(context: Context, onLocationReceived: (Double?, Double?) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationReceived(location.latitude, location.longitude)
                } else {
                    onLocationReceived(null, null)
                    Log.d("ReportScreen", "Location is null")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ReportScreen", "Error fetching location: ${e.message}", e)
                onLocationReceived(null, null)
            }
    } else {
        onLocationReceived(null, null)
    }
}
