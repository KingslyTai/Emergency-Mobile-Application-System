@file:Suppress("DEPRECATION", "NAME_SHADOWING")

package com.example.emergencymobileapplicationsystem.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.example.emergencymobileapplicationsystem.data.PlaceInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecificUserLocationScreen(
    navController: NavController,
    saveLocation: (latitude: String, longitude: String) -> Unit,
    deleteLocation: (PlaceInfo, () -> Unit, (String) -> Unit) -> Unit,
    blockedLocations: MutableList<String> // Track blocked Google locations to avoid re-fetch
) {
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var selectedPlaceType by remember { mutableStateOf("police") }
    var googlePlacesData by remember { mutableStateOf<List<PlaceInfo>>(emptyList()) }
    var createdPlacesData by remember { mutableStateOf<List<PlaceInfo>>(emptyList()) }
    var editedPlacesData by remember { mutableStateOf<List<PlaceInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val showSearchBar by remember { mutableStateOf(true) }
    val showCreateButton by remember { mutableStateOf(true) }

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

    fun refreshData() {
        currentLocation?.let { location ->
            isLoading = true
            fetchAllLocationsData(location, selectedPlaceType, firestore, blockedLocations) { googlePlaces, createdPlaces, editedPlaces ->
                googlePlacesData = googlePlaces
                createdPlacesData = createdPlaces
                editedPlacesData = editedPlaces
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedPlaceType, currentLocation, blockedLocations) {
        refreshData()
    }

    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }

        coroutineScope.launch {
            fetchSpecificUserLocation(fusedLocationClient) { location ->
                currentLocation = location
                location?.let {
                    saveLocation(it.latitude.toString(), it.longitude.toString())
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TopAppBar(
            title = { Text("") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { selectedPlaceType = "police" }) {
                Text("Police")
            }

            Button(onClick = { selectedPlaceType = "fire_station" }) {
                Text("Fire")
            }

            Button(onClick = { selectedPlaceType = "hospital" }) {
                Text("Hospital")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showSearchBar) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search emergency locations...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val combinedPlaces = mutableListOf<PlaceInfo>()
                    combinedPlaces.addAll(googlePlacesData)
                    createdPlacesData.forEach { createdPlace ->
                        val existsInGoogle = googlePlacesData.any { googlePlace ->
                            googlePlace.name == createdPlace.name && googlePlace.address == createdPlace.address
                        }
                        if (!existsInGoogle) combinedPlaces.add(createdPlace)
                    }
                    editedPlacesData.forEach { editedPlace ->
                        val index = combinedPlaces.indexOfFirst { it.name == editedPlace.name && it.address == editedPlace.address }
                        if (index != -1) combinedPlaces[index] = editedPlace else combinedPlaces.add(editedPlace)
                    }

                    val filteredPlaces = combinedPlaces.filter {
                        it.name.contains(searchQuery, ignoreCase = true)
                    }

                    items(filteredPlaces) { place ->
                        EmergencyLocationCardForSpecificUser(
                            place = place,
                            onEditClick = {
                                val documentId = place.documentId ?: ""
                                val name = place.name
                                val address = place.address
                                val latitude = place.latLng.latitude
                                val longitude = place.latLng.longitude
                                val phoneNumber = place.phoneNumber ?: ""
                                val isGoogleLocation = documentId.isEmpty()

                                navController.navigate(
                                    "editEmergencyInfoScreen/$documentId/$name/$address/$latitude/$longitude/$phoneNumber/$isGoogleLocation"
                                )
                            },
                            onDeleteClick = {
                                if (place.documentId == null) {
                                    googlePlacesData = googlePlacesData.filter { it != place }
                                    place.placeId?.let { blockedLocations.add(it) }
                                    Toast.makeText(context, "Google location removed from view permanently.", Toast.LENGTH_SHORT).show()
                                } else {
                                    deleteLocation(
                                        place,
                                        {
                                            Toast.makeText(context, "Location deleted successfully.", Toast.LENGTH_SHORT).show()
                                            refreshData()
                                        },
                                        { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showCreateButton) {
            Button(onClick = {
                navController.navigate("createEmergencyInfoScreen")
            }) {
                Text("Create Emergency Location")
            }
        }
    }
}

@Composable
fun EmergencyLocationCardForSpecificUser(
    place: PlaceInfo,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = "Name: ${place.name}")
            Text(text = "Address: ${place.address}")
            Text(text = "Phone: ${place.phoneNumber ?: "Fetching..."}")

            Row(horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEditClick) {
                    Text("Edit")
                }
                TextButton(onClick = onDeleteClick) {
                    Text("Delete")
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun fetchSpecificUserLocation(
    fusedLocationClient: FusedLocationProviderClient,
    onLocationFound: (Location?) -> Unit
) {
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location: Location? ->
            onLocationFound(location)
        }
}

fun fetchAllLocationsData(
    currentLocation: Location?,
    placeType: String,
    firestore: FirebaseFirestore,
    blockedLocations: List<String>,
    onDataFetched: (List<PlaceInfo>, List<PlaceInfo>, List<PlaceInfo>) -> Unit
) {
    fetchNearbyPlacesForSpecificUser(currentLocation, placeType, blockedLocations) { googlePlaces, _ ->
        fetchCreatedLocationsFromFirestore(firestore, placeType, currentLocation) { createdPlaces ->  // Pass currentLocation here
            fetchEditedLocationsFromFirestore(firestore, placeType, currentLocation) { editedPlaces ->
                onDataFetched(googlePlaces, createdPlaces, editedPlaces)
            }
        }
    }
}

fun fetchNearbyPlacesForSpecificUser(
    currentLocation: Location?,
    placeType: String,
    blockedLocations: List<String>,
    onPlaceFound: (List<PlaceInfo>, String) -> Unit
) {
    currentLocation?.let { location ->
        val apiKey = "AIzaSyBEw8-YKgIAdvV81qFEJ5ql872YG1NU4-c"
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=${location.latitude},${location.longitude}" +
                "&radius=10000" +
                "&type=$placeType" +
                "&key=$apiKey"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                onPlaceFound(emptyList(), "Failed to fetch nearby $placeType.")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val jsonResponse = JSONObject(it.string())
                    val results = jsonResponse.getJSONArray("results")
                    val placeInfos = mutableListOf<PlaceInfo>()

                    for (i in 0 until results.length()) {
                        val place = results.getJSONObject(i)
                        val placeId = place.getString("place_id")
                        if (!blockedLocations.contains(placeId)) {
                            val name = place.getString("name")
                            val address = place.getString("vicinity")
                            val location = place.getJSONObject("geometry").getJSONObject("location")
                            val lat = location.getDouble("lat")
                            val lng = location.getDouble("lng")

                            val placeInfo = PlaceInfo(LatLng(lat, lng), name, address, placeId = placeId)
                            placeInfos.add(placeInfo)
                        }
                    }

                    onPlaceFound(placeInfos, "Nearest ${placeType.capitalize(Locale.ROOT)} locations found.")
                } ?: run {
                    onPlaceFound(emptyList(), "No results found.")
                }
            }
        })
    } ?: run {
        onPlaceFound(emptyList(), "Current location is null. Unable to fetch nearby places.")
    }
}

// Define a maximum distance (in meters) for displaying nearby locations
const val MAX_DISTANCE_METERS = 10000  // e.g., 10 km

// Function to calculate the distance between two LatLng points
fun calculateDistanceBetweenPoints(latLng1: LatLng, latLng2: LatLng): Float {
    val location1 = Location("").apply {
        latitude = latLng1.latitude
        longitude = latLng1.longitude
    }
    val location2 = Location("").apply {
        latitude = latLng2.latitude
        longitude = latLng2.longitude
    }
    return location1.distanceTo(location2)
}

// Modified fetchCreatedLocationsFromFirestore function with filtering
fun fetchCreatedLocationsFromFirestore(
    firestore: FirebaseFirestore,
    placeType: String,
    userLocation: Location?,  // User's current location
    onCreatedLocationsFetched: (List<PlaceInfo>) -> Unit
) {
    firestore.collection("emergencylocation")
        .whereEqualTo("type", placeType)
        .get()
        .addOnSuccessListener { result ->
            val createdPlaces = mutableListOf<PlaceInfo>()
            for (document in result) {
                val lat = document.getDouble("latitude") ?: 0.0
                val lng = document.getDouble("longitude") ?: 0.0
                val name = document.getString("name") ?: "Unknown"
                val address = document.getString("address") ?: "Unknown"
                val phoneNumber = document.getString("phoneNumber")

                val placeLatLng = LatLng(lat, lng)
                // Check if the user location is available and the place is within the desired radius
                val distance = userLocation?.let {
                    calculateDistanceBetweenPoints(placeLatLng, LatLng(it.latitude, it.longitude))
                } ?: Float.MAX_VALUE  // If user location is not available, set distance to max

                // Add place only if it is within the MAX_DISTANCE_METERS radius
                if (distance <= MAX_DISTANCE_METERS) {
                    val placeInfo = PlaceInfo(
                        placeLatLng,
                        name,
                        address,
                        phoneNumber,
                        documentId = document.id  // Capture document ID for editing/deleting
                    )
                    createdPlaces.add(placeInfo)
                }
            }
            onCreatedLocationsFetched(createdPlaces)
        }
        .addOnFailureListener {
            onCreatedLocationsFetched(emptyList())
        }
}

fun fetchEditedLocationsFromFirestore(
    firestore: FirebaseFirestore,
    placeType: String,
    userLocation: Location?,  // Added user location parameter
    onEditedLocationsFetched: (List<PlaceInfo>) -> Unit
) {
    firestore.collection("emergencylocation")
        .whereEqualTo("type", placeType)
        .get()
        .addOnSuccessListener { result ->
            val editedPlaces = mutableListOf<PlaceInfo>()
            for (document in result) {
                val lat = document.getDouble("latitude") ?: 0.0
                val lng = document.getDouble("longitude") ?: 0.0
                val name = document.getString("name") ?: "Unknown"
                val address = document.getString("address") ?: "Unknown"
                val phoneNumber = document.getString("phoneNumber")

                val placeLatLng = LatLng(lat, lng)
                val distance = userLocation?.let {
                    calculateDistanceBetweenPoints(placeLatLng, LatLng(it.latitude, it.longitude))
                } ?: Float.MAX_VALUE  // Set distance to max if userLocation is null

                // Add place only if it is within the MAX_DISTANCE_METERS radius
                if (distance <= MAX_DISTANCE_METERS) {
                    val placeInfo = PlaceInfo(
                        placeLatLng,
                        name,
                        address,
                        phoneNumber,
                        documentId = document.id
                    )
                    editedPlaces.add(placeInfo)
                }
            }
            onEditedLocationsFetched(editedPlaces)
        }
        .addOnFailureListener {
            onEditedLocationsFetched(emptyList())
        }
}

