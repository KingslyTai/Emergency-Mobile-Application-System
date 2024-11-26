package com.example.emergencymobileapplicationsystem.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DrawerContent(
    onNavigateToFeedback: () -> Unit,
    onCreateAccount: () -> Unit,
    isLoggedIn: Boolean,
    isVolunteer: Boolean, // New parameter to determine if the user is a registered volunteer
    onLogin: () -> Unit,
    onSignOut: () -> Unit, // Updated to handle navigation after signing out
    onManageProfile: () -> Unit,
    onNavigateToEmergencyServiceInformation: () -> Unit,
    onNavigateToReportListScreen: () -> Unit,
    onNavigateToVolunteerDetails: () -> Unit,
    onNavigateToVolunteerRequestList: () -> Unit // Callback for navigating to the Volunteer Request List
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp), // Space between items
        horizontalAlignment = Alignment.Start
    ) {
        // Emergency Service Button
        DrawerRow(
            icon = Icons.Default.LocationOn,
            label = "Emergency Service",
            onClick = onNavigateToEmergencyServiceInformation
        )

        // Feedback Button
        DrawerRow(
            icon = Icons.Default.Feedback,
            label = "Feedback",
            onClick = onNavigateToFeedback
        )

        if (!isLoggedIn) {
            // Create Account and Login Buttons for Non-Logged-In Users
            DrawerRow(
                icon = Icons.Default.AccountCircle,
                label = "Create Account",
                onClick = onCreateAccount
            )
            DrawerRow(
                icon = Icons.Default.Login,
                label = "Login",
                onClick = onLogin
            )
        } else {
            // Manage Profile, Report List, Volunteer Details, and Sign Out
            DrawerRow(
                icon = Icons.Default.AccountCircle,
                label = "Manage Profile",
                onClick = onManageProfile
            )
            DrawerRow(
                icon = Icons.Default.AccountCircle, // Replace with appropriate icons
                label = "Report List",
                onClick = onNavigateToReportListScreen
            )
            DrawerRow(
                icon = Icons.Default.AccountCircle, // Replace with appropriate icons
                label = "Volunteer Details",
                onClick = onNavigateToVolunteerDetails
            )

            // Conditional: Volunteer Request List for Registered Volunteers
            if (isVolunteer) {
                DrawerRow(
                    icon = Icons.Default.AccountCircle, // Replace with appropriate icons
                    label = "Volunteer Request List",
                    onClick = onNavigateToVolunteerRequestList
                )
            }

            DrawerRow(
                icon = Icons.Default.Logout,
                label = "Sign Out",
                onClick = {
                    onSignOut() // Executes sign-out callback
                }
            )
        }
    }
}

@Composable
fun DrawerRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp), // Larger icon for better visibility
            tint = Color(0xFF6200EE)
        )
        Spacer(modifier = Modifier.width(24.dp)) // Increased spacing between icon and text
        Text(
            text = label,
            fontSize = 18.sp, // Increased font size for better readability
            fontWeight = FontWeight.Bold,
            color = Color.Black // High contrast for visibility
        )
    }
}
