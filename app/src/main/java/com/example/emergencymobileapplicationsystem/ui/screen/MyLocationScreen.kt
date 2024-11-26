@file:Suppress("DEPRECATION", "NAME_SHADOWING")

package com.example.emergencymobileapplicationsystem.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.emergencymobileapplicationsystem.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.emergencymobileapplicationsystem.data.PlaceInfo
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyLocationScreen(
    navController: NavController,
    saveLocation: (latitude: String, longitude: String) -> Unit, // Callback for saving location,
    blockedLocations: List<String>
) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var locationText by remember { mutableStateOf("Fetching location...") }
    var selectedPlaceType by remember { mutableStateOf("police") } // Default place type is police
    var placeInfo by remember { mutableStateOf("Select a place type to view nearest location.") }
    var allPlaceMarkers by remember { mutableStateOf<List<PlaceInfo>>(emptyList()) } // Combined list of places
    var selectedPlace by remember { mutableStateOf<PlaceInfo?>(null) } // Store selected place
    var searchQuery by remember { mutableStateOf("") } // Search query for filtering results
    var showSearchBar by remember { mutableStateOf(false) } // Control the visibility of the search bar
    var showGoButton by remember { mutableStateOf(false) } // Control visibility of the "Go" button
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val firestore = FirebaseFirestore.getInstance()

    // Function to filter locations within a specified radius
    fun filterByDistance(locations: List<PlaceInfo>, radiusKm: Double): List<PlaceInfo> {
        return locations.filter { place ->
            currentLocation?.let { userLocation ->
                val distance = calculateDistance(
                    userLocation.latitude,
                    userLocation.longitude,
                    place.latLng.latitude,
                    place.latLng.longitude
                )
                distance.toDouble() <= radiusKm
            } ?: false
        }
    }

    // Fetch both user-created and nearby emergency locations based on the selected type
    fun fetchUserAndNearbyLocations(placeType: String) {
        val combinedPlaces = mutableListOf<PlaceInfo>()

        // Fetch user-created locations from Firestore based on type
        firestore.collection("emergencylocation")
            .whereEqualTo("type", placeType)
            .get()
            .addOnSuccessListener { result ->
                val userCreatedPlaceInfos = mutableListOf<PlaceInfo>()
                for (document in result) {
                    val lat = document.getDouble("latitude") ?: 0.0
                    val lng = document.getDouble("longitude") ?: 0.0
                    val name = document.getString("name") ?: "Unknown"
                    val address = document.getString("address") ?: "Unknown"
                    val phoneNumber = document.getString("phoneNumber")
                    val placeInfo = PlaceInfo(
                        latLng = LatLng(lat, lng),
                        name = name,
                        address = address,
                        phoneNumber = phoneNumber
                    )
                    userCreatedPlaceInfos.add(placeInfo)
                }
                combinedPlaces.addAll(userCreatedPlaceInfos) // Add user-created locations to the list

                // Fetch nearby places from Google Places API
                fetchNearbyPlaces(currentLocation, placeType, blockedLocations) { googlePlaces, info ->
                    placeInfo = info
                    combinedPlaces.addAll(googlePlaces) // Add nearby places to the list

                    // Filter places by distance (e.g., within 10 km)
                    allPlaceMarkers = filterByDistance(combinedPlaces, 10.0) // 10 km radius
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to fetch user-created locations", Toast.LENGTH_SHORT).show()
            }
    }

    // Check if GPS and Network-based location services are enabled
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
    val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)

    if (!isGpsEnabled && !isNetworkEnabled) {
        Toast.makeText(context, "Please enable GPS or Network location services", Toast.LENGTH_LONG).show()
        val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        context.startActivity(intent)
        return
    }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Check permissions before fetching location
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }

        coroutineScope.launch {
            fetchLocation(fusedLocationClient) { location ->
                currentLocation = location
                location?.let {
                    locationText = "Lat: ${it.latitude}, Lon: ${it.longitude}"
                    // Save the location to Firebase
                    saveLocation(it.latitude.toString(), it.longitude.toString())
                } ?: run {
                    locationText = "Unable to fetch location"
                }
                // Fetch user-created and nearby emergency locations after getting the user's location
                fetchUserAndNearbyLocations(selectedPlaceType)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // Top App Bar with back button
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )

            // Display current location at the top
            Text(
                text = "Current Location: $locationText",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Map View Container in the middle (adjust the height of the map)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp) // Adjust map size
            ) {
                MapViewContainer(
                    mapView = mapView,
                    currentLocation = currentLocation,
                    nearbyPlaces = allPlaceMarkers, // Combined places for map display
                    selectedPlaceType = selectedPlaceType,
                    selectedPlace = selectedPlace // Pass the selected place
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons to select the nearest place type
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = {
                    selectedPlaceType = "police"
                    fetchUserAndNearbyLocations(selectedPlaceType) // Fetch both user-created and Google Places data
                    showSearchBar = true
                }) {
                    Text("Police")
                }

                Button(onClick = {
                    selectedPlaceType = "fire_station"
                    fetchUserAndNearbyLocations(selectedPlaceType) // Fetch both user-created and Google Places data
                    showSearchBar = true
                }) {
                    Text("Fire")
                }

                Button(onClick = {
                    selectedPlaceType = "hospital"
                    fetchUserAndNearbyLocations(selectedPlaceType) // Fetch both user-created and Google Places data
                    showSearchBar = true
                }) {
                    Text("Hospital")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Conditionally display the search bar after a button is clicked
            if (showSearchBar) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search emergency locations...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable box for Location Information
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Takes up the remaining space below the map
                ) {
                    val filteredPlaces = allPlaceMarkers.filter {
                        it.name.contains(searchQuery, ignoreCase = true)
                    }

                    items(filteredPlaces) { place ->
                        EmergencyLocationCard(
                            place = place,
                            currentLocation = currentLocation // Pass current location to calculate distance
                        ) {
                            selectedPlace = place // Set selected place when clicked
                            showGoButton = true // Show the "Go" button
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // "Go" button at the bottom right
        if (showGoButton && selectedPlace != null) {
            Button(
                onClick = {
                    val gmmIntentUri = Uri.parse("google.navigation:q=${selectedPlace?.latLng?.latitude},${selectedPlace?.latLng?.longitude}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    context.startActivity(mapIntent)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd) // Align to bottom-right
                    .padding(24.dp) // Padding to move button down further
            ) {
                Text("Go")
            }
        }
    }
}

@Composable
fun EmergencyLocationCard(place: PlaceInfo, currentLocation: Location?, onClick: () -> Unit) {
    // Calculate distance between the user's location and the emergency service
    val distance = currentLocation?.let {
        calculateDistance(
            it.latitude, it.longitude,
            place.latLng.latitude, place.latLng.longitude
        )
    } ?: "Unknown"

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onClick() } // Make the card clickable
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = "Name: ${place.name}")
            Text(text = "Address: ${place.address}")
            Text(text = "Phone: ${place.phoneNumber ?: "Fetching..."}") // Show "Fetching..." while phone number is being retrieved
            Text(text = "Distance: $distance km") // Display distance
        }
    }
}

@Composable
fun MapViewContainer(
    mapView: MapView,
    currentLocation: Location?,
    nearbyPlaces: List<PlaceInfo>,
    selectedPlaceType: String,
    selectedPlace: PlaceInfo? // Pass the selected place to focus on
) {
    AndroidView({ mapView }) { mapView ->
        mapView.getMapAsync { googleMap ->
            if (ActivityCompat.checkSelfPermission(
                    mapView.context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                googleMap.isMyLocationEnabled = true

                currentLocation?.let { it ->
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.clear()

                    // Add user's current location marker
                    googleMap.addMarker(MarkerOptions().position(userLatLng).title("You are here"))
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))

                    // Add markers for nearby places with custom icons
                    nearbyPlaces.forEach { placeInfo ->
                        val markerOptions = MarkerOptions().position(placeInfo.latLng).title(placeInfo.name)
                        when (selectedPlaceType) {
                            "police" -> {
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(
                                    getScaledBitmapFromVector(mapView.context, R.drawable.royal_malaysian_police, 128, 128)
                                ))
                            }
                            "hospital" -> {
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(
                                    getScaledBitmapFromVector(mapView.context, R.drawable.hospital, 128, 128)
                                ))
                            }
                            "fire_station" -> {
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(
                                    getScaledBitmapFromVector(mapView.context, R.drawable.fire_station, 128, 128)
                                ))
                            }
                        }
                        val marker = googleMap.addMarker(markerOptions)
                        marker?.tag = placeInfo // Attach place info to marker for later use
                    }

                    // Handle marker clicks to show more info
                    googleMap.setOnMarkerClickListener { marker ->
                        val placeInfo = marker.tag as? PlaceInfo
                        placeInfo?.let {
                            Toast.makeText(mapView.context, "${it.name}: ${it.address}", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }

                    // Focus on the selected place
                    selectedPlace?.let {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it.latLng, 15f))
                    }
                }
            }
        }
    }
}

// Fetches the nearest places and returns both the LatLng for map markers and information for display
fun fetchNearbyPlaces(
    currentLocation: Location?,
    placeType: String,
    blockedLocations: List<String>,
    onPlaceFound: (List<PlaceInfo>, String) -> Unit
) {
    currentLocation?.let { it ->
        val apiKey = "AIzaSyBEw8-YKgIAdvV81qFEJ5ql872YG1NU4-c"
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=${it.latitude},${it.longitude}" +
                "&radius=10000" + // Radius in meters (10km)
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
                    val placeNames = mutableListOf<String>()

                    for (i in 0 until results.length()) {
                        val place = results.getJSONObject(i)
                        val placeId = place.getString("place_id")
                        if (placeId !in blockedLocations) {
                            val name = place.getString("name")
                            val address = place.getString("vicinity")
                            val location = place.getJSONObject("geometry").getJSONObject("location")
                            val lat = location.getDouble("lat")
                            val lng = location.getDouble("lng")

                            val placeInfo =
                                PlaceInfo(LatLng(lat, lng), name, address, placeId = placeId)
                            placeInfos.add(placeInfo)
                            placeNames.add(name)
                        }
                    }

                    if (placeNames.isNotEmpty()) {
                        onPlaceFound(placeInfos, "Nearest ${placeType.capitalize(Locale.ROOT)}: ${placeNames.joinToString(", ")}")
                    } else {
                        onPlaceFound(emptyList(), "No nearby $placeType found.")
                    }
                } ?: run {
                    onPlaceFound(emptyList(), "No results found.")
                }
            }
        })
    } ?: run {
        onPlaceFound(emptyList(), "Current location is null. Unable to fetch nearby places.")
    }
}


fun getScaledBitmapFromVector(context: Context, vectorResId: Int, width: Int, height: Int): Bitmap {
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
    vectorDrawable?.setBounds(0, 0, width, height)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    vectorDrawable?.draw(canvas)
    return bitmap
}

// Fetch detailed place info (like phone number) using Place Details API
fun fetchPlaceDetails(placeId: String, onPlaceDetailsFound: (PlaceInfo) -> Unit) {
    val apiKey = "YOUR_API_KEY"
    val url = "https://maps.googleapis.com/maps/api/place/details/json" +
            "?place_id=$placeId" +
            "&fields=name,formatted_phone_number" + // Request phone number
            "&key=$apiKey"

    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }

        override fun onResponse(call: Call, response: Response) {
            response.body?.let {
                val jsonResponse = JSONObject(it.string())
                val result = jsonResponse.getJSONObject("result")
                val phoneNumber = result.optString("formatted_phone_number", "Not available")
                val name = result.getString("name")
                onPlaceDetailsFound(PlaceInfo(LatLng(0.0, 0.0), name, "", phoneNumber)) // Return place info with phone number
            }
        }
    })
}

// Helper function to calculate the distance between two locations
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
    val earthRadius = 6371.0 // Radius of the earth in kilometers
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    val distance = earthRadius * c
    return "%.2f".format(distance)
}

@SuppressLint("MissingPermission")
fun fetchLocation(
    fusedLocationClient: FusedLocationProviderClient,
    onLocationFound: (Location?) -> Unit
) {
    // Create a location request with high accuracy
    val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 5000 // 5 seconds interval
    ).setWaitForAccurateLocation(true).build()

    // Location callback to receive updates
    val locationCallback = object : com.google.android.gms.location.LocationCallback() {
        override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
            val location = locationResult.lastLocation
            if (location != null) {
                onLocationFound(location)
                fusedLocationClient.removeLocationUpdates(this) // Stop updates once we have a location
            } else {
                onLocationFound(null)
            }
        }
    }

    // Start location updates with the request and callback
    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
}

// Helper function to manage MapView lifecycle
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }

        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    return mapView
}
