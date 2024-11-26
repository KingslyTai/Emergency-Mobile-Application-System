package com.example.emergencymobileapplicationsystem.repository

import android.util.Log
import com.example.emergencymobileapplicationsystem.data.VolunteerRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject

class VolunteerRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    // Function to add a volunteer request to Firestore
    fun addVolunteerRequest(request: VolunteerRequest, onComplete: (Boolean, String?) -> Unit) {
        db.collection("volunteerRequests")
            .add(request)
            .addOnSuccessListener { documentReference ->
                Log.d("VolunteerRepository", "Request added with ID: ${documentReference.id}")
                onComplete(true, documentReference.id) // Call onComplete with success and the document ID
            }
            .addOnFailureListener { exception ->
                Log.e("VolunteerRepository", "Error adding request: ${exception.message}")
                onComplete(false, null) // Call onComplete with failure
            }
    }

    // Function to fetch all volunteer requests for volunteers to view and choose from
    fun getAllVolunteerRequests(onComplete: (List<VolunteerRequest>) -> Unit) {
        db.collection("volunteerRequests")
            .get()
            .addOnSuccessListener { result ->
                val requests = result.documents.mapNotNull { document ->
                    try {
                        document.toObject<VolunteerRequest>()?.apply {
                            requestId = document.id // Set the requestId field with the document ID
                        }
                    } catch (e: Exception) {
                        Log.e("VolunteerRepository", "Error mapping document: ${document.id}, ${e.message}")
                        null
                    }
                }
                Log.d("VolunteerRepository", "Fetched requests: $requests")
                onComplete(requests) // Return the list of volunteer requests
            }
            .addOnFailureListener { exception ->
                Log.e("VolunteerRepository", "Error fetching requests: ${exception.message}")
                onComplete(emptyList()) // Return an empty list on failure
            }
    }

    // Function to fetch requests created by a specific user
    fun getUserVolunteerRequests(userId: String, onComplete: (List<VolunteerRequest>) -> Unit) {
        db.collection("volunteerRequests")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val requests = result.documents.mapNotNull { document ->
                    try {
                        document.toObject<VolunteerRequest>()?.apply {
                            requestId = document.id // Set the requestId from Firestore document ID
                        }
                    } catch (e: Exception) {
                        Log.e("VolunteerRepository", "Error mapping document: ${document.id}, ${e.message}")
                        null
                    }
                }
                Log.d("VolunteerRepository", "Fetched user requests: $requests")
                onComplete(requests)
            }
            .addOnFailureListener { exception ->
                Log.e("VolunteerRepository", "Error fetching user requests: ${exception.message}")
                onComplete(emptyList()) // Return an empty list in case of failure
            }
    }

    // Function to check if a user is registered as a volunteer and get their availability status
    fun checkVolunteerRegistration(userId: String, onComplete: (Boolean, Boolean) -> Unit) {
        db.collection("volunteers").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val availability = document.getString("availability") == "available"
                    Log.d("VolunteerRepository", "Volunteer registered: $userId, Availability: $availability")
                    onComplete(true, availability) // User is registered and return their availability
                } else {
                    Log.d("VolunteerRepository", "Volunteer not registered: $userId")
                    onComplete(false, false) // User is not registered as a volunteer
                }
            }
            .addOnFailureListener { exception ->
                Log.e("VolunteerRepository", "Error checking registration: ${exception.message}")
                onComplete(false, false) // Error occurred, assume user is not registered
            }
    }

    fun getVolunteerRequestById(requestId: String, onComplete: (VolunteerRequest?) -> Unit) {
        db.collection("volunteerRequests").document(requestId).get()
            .addOnSuccessListener { document ->
                val request = document.toObject<VolunteerRequest>()?.apply {
                    this.requestId = document.id
                }
                onComplete(request)
            }
            .addOnFailureListener {
                onComplete(null) // Ensure it handles the failure gracefully
            }
    }


    fun assignVolunteerToRequest(requestId: String, volunteerId: String, onComplete: (Boolean) -> Unit) {
        db.collection("volunteerRequests").document(requestId)
            .update("assignedVolunteer", volunteerId)
            .addOnSuccessListener {
                Log.d("VolunteerRepository", "Volunteer $volunteerId assigned to request $requestId")
                onComplete(true)
            }
            .addOnFailureListener { exception ->
                Log.e("VolunteerRepository", "Error assigning volunteer: ${exception.message}")
                onComplete(false)
            }
    }

}
