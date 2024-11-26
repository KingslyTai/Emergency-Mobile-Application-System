package com.example.emergencymobileapplicationsystem.data

data class UserProfile(
    val profileId: String = "",  // Primary key for the profile
    val userId: String = "",     // Foreign key for the associated user
    val name: String = "",
    val age: String ="",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val emergencyContact: String = "",
    val dateOfBirth: String = "",
    val gender: String = ""
)
