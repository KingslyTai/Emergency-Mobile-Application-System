package com.example.emergencymobileapplicationsystem.model

import java.util.Date

data class Message(
    val id: String = "",
    val text: String = "",
    val userId: String = "",
    val timestamp: Date = Date(),
    val replyingToMessageText: String? = null // Add this field for reply feature
)

