package com.example.emergencymobileapplicationsystem.data

data class Feedback(
    val id: String = "",
    val content: String = "",
    val author: String = "",
    val userId: String = "", // To store the author's user ID
    val likeCount: Int = 0, // New field for like count
    val dislikeCount: Int = 0 // New field for dislike count
)
