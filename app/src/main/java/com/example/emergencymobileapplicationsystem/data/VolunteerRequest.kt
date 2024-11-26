package com.example.emergencymobileapplicationsystem.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VolunteerRequest(
    var requestId: String = "",
    var userId: String = "",
    var serviceType: String = "",
    var location: String = "",
    var notes: String = "",
    var date: String = "",
    var status: String = "Pending",
    var assignedVolunteer: String? = null // New property for assigned volunteer
) : Parcelable
