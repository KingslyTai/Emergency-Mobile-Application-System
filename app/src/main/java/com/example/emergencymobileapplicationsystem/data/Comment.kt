package com.example.emergencymobileapplicationsystem.data

data class Comment(
    val id: String = "",
    val author: String = "Unknown",
    val content: String = "",
    val likeCount: Int = 0,
    val dislikeCount: Int = 0
)
