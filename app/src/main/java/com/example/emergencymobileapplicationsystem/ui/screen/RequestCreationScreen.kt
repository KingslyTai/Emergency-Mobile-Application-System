import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.emergencymobileapplicationsystem.data.VolunteerRequest
import com.example.emergencymobileapplicationsystem.repository.VolunteerRepository
import java.text.SimpleDateFormat
import java.util.*

// Initialize the VolunteerRepository
val volunteerRepository = VolunteerRepository()

// Firestore submission function using VolunteerRepository
fun submitRequestToFirestore(
    request: VolunteerRequest,
    onSuccess: (Boolean) -> Unit
) {
    volunteerRepository.addVolunteerRequest(request) { success, documentId ->
        if (success && documentId != null) {
            Log.d("RequestCreation", "Request added with ID: $documentId")

            // Assign the request ID to the request object
            request.requestId = documentId
            onSuccess(true) // Indicate success
        } else {
            Log.w("RequestCreation", "Error adding request")
            onSuccess(false) // Indicate failure
        }
    }
}

// List of predefined service types with "Other" option added
val serviceTypes = listOf(
    "Language Translation Assistance",
    "Transport to Emergency Facilities",
    "Pet Assistance and Care",
    "Medical Support and First Aid",
    "Emotional Support and Counseling",
    "Elderly Assistance and Mobility Support",
    "Child Care and Supervision",
    "Home Repair and Debris Removal",
    "Other" // Option for custom service type
)

// RequestCreationScreen Composable
@Composable
fun RequestCreationScreen(
    userId: String,
    onSubmitRequest: (VolunteerRequest) -> Unit, // Callback for submitting the request
    onBack: () -> Unit
) {
    var selectedServiceType by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var customServiceType by remember { mutableStateOf(TextFieldValue("")) }
    var location by remember { mutableStateOf(TextFieldValue("")) }
    var notes by remember { mutableStateOf(TextFieldValue("")) }
    var isSubmitting by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Display DatePickerDialog when showDatePicker is true
    if (showDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = dateFormat.format(calendar.time)
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Volunteer Service") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dropdown for selecting service type
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (selectedServiceType == "Other") "Other" else selectedServiceType,
                        onValueChange = {},
                        label = { Text("Select Service Type") },
                        placeholder = { Text("Choose a service type", color = Color.Gray) },
                        textStyle = MaterialTheme.typography.body1.copy(color = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDropdownExpanded = true },
                        enabled = false,
                        trailingIcon = {
                            IconButton(onClick = { isDropdownExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Dropdown Menu",
                                    tint = Color.Gray
                                )
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        serviceTypes.forEach { service ->
                            DropdownMenuItem(onClick = {
                                selectedServiceType = service
                                isDropdownExpanded = false
                                if (service != "Other") {
                                    customServiceType = TextFieldValue("")
                                }
                            }) {
                                Text(service, color = Color.Black)
                            }
                        }
                    }
                }

                // Custom input field if "Other" is selected
                if (selectedServiceType == "Other") {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = customServiceType,
                        onValueChange = { customServiceType = it },
                        label = { Text("Specify your service") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date Picker Field
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = {},
                    label = { Text("Select Date") },
                    placeholder = { Text("YYYY-MM-DD", color = Color.Gray) },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = "Calendar Icon",
                                tint = Color.Gray
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Location and Notes Fields
                TextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Additional Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = {
                        isSubmitting = true
                        val request = VolunteerRequest(
                            requestId = "",
                            userId = userId,
                            serviceType = if (selectedServiceType == "Other") customServiceType.text else selectedServiceType,
                            location = location.text,
                            notes = notes.text,
                            date = selectedDate,
                            status = "Pending"
                        )
                        onSubmitRequest(request) // Submit request
                        isSubmitting = false
                    },
                    enabled = !isSubmitting && selectedDate.isNotEmpty() &&
                            (selectedServiceType.isNotEmpty() &&
                                    (selectedServiceType != "Other" || customServiceType.text.isNotEmpty())),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Submit Request")
                }

                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
    )
}

// Wrapper for the Request Creation Screen
@Composable
fun VolunteerRequestScreen(userId: String, onNavigateToStatus: () -> Unit, onBack: () -> Unit) {
    RequestCreationScreen(
        userId = userId,
        onSubmitRequest = { request ->
            submitRequestToFirestore(request) { success ->
                if (success) {
                    onNavigateToStatus()
                }
            }
        },
        onBack = onBack
    )
}

// Preview Function
@Preview
@Composable
fun PreviewRequestCreationScreen() {
    VolunteerRequestScreen(userId = "testUserId", onNavigateToStatus = {}, onBack = {})
}
