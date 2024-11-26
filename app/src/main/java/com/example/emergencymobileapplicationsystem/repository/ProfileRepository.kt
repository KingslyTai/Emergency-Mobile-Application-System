package com.example.emergencymobileapplicationsystem.repository

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProfileRepository(private val firestore: FirebaseFirestore) {

    // Create a new profile
    suspend fun createProfile(user: FirebaseUser, profileData: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("profiles").document(user.uid).set(profileData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Retrieve an existing profile
    suspend fun getProfile(userId: String): Result<Map<String, Any>> {
        return try {
            val document = firestore.collection("profiles").document(userId).get().await()
            if (document.exists()) {
                Result.success(document.data ?: emptyMap())
            } else {
                Result.failure(Exception("Profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update an existing profile
    suspend fun editProfile(userId: String, updatedData: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("profiles").document(userId).update(updatedData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete a profile
    suspend fun deleteProfile(userId: String): Result<Unit> {
        return try {
            firestore.collection("profiles").document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
