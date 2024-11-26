package com.example.emergencymobileapplicationsystem.data

import java.util.Date

data class Report(
    var reportId: String = "",
    val userId: String? = null,        // User ID of the report creator
    val name: String = "",         // Name of the report creator
    val reportType: String = "",       // Type of report (e.g., "Police", "Fire", "Hospital")
    val description: String = "",      // Description of the report
    val timestamp: Date = Date(),      // Timestamp when the report was created
    val status: String = "Open",        // Status of the report (e.g., "Open", "In Progress", "Closed")
    val latitude: Double? = null,     // Add latitude
    val longitude: Double? = null     // Add longitude
)
