package com.example.emergencymobileapplicationsystem.data

data class PlaceInfo(
    val latLng: com.google.android.gms.maps.model.LatLng,
    val name: String,
    val address: String,
    var phoneNumber: String? = null,
    val placeId: String? = null, // Place ID from Google Places API
    val documentId: String? = null // Document ID from Firestore for CRUD operations
)
