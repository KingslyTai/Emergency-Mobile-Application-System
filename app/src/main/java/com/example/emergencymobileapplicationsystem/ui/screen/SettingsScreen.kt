package com.example.emergencymobileapplicationsystem.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit, // Callback to navigate back to the previous page
    onLogout: () -> Unit,
    onNavigateToAccountSettings: () -> Unit,
    isDarkModeEnabled: Boolean, // Current dark mode state passed as a parameter
    onDarkModeToggle: (Boolean) -> Unit, // Callback to toggle dark mode
    onLanguageChange: (String) -> Unit // Callback for changing language
) {
    val availableLanguages = listOf("English", "Spanish", "French", "Chinese")
    var selectedLanguage by remember { mutableStateOf("English") } // Default language

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preferences Section Title
            Text(
                text = "Preferences",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Notifications Switch
            var notificationsEnabled by remember { mutableStateOf(false) }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Enable Notifications", fontSize = 16.sp)
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
            }

            // Dark Mode Toggle
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Dark Mode", fontSize = 16.sp)
                Switch(
                    checked = isDarkModeEnabled,
                    onCheckedChange = { onDarkModeToggle(it) }
                )
            }

            // Change Language Dropdown
            Column {
                Text(
                    text = "Language",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DropdownMenuWithSelectedOption(
                    options = availableLanguages,
                    selectedOption = selectedLanguage,
                    onOptionSelected = { newLanguage ->
                        selectedLanguage = newLanguage
                        onLanguageChange(newLanguage) // Callback for changing language
                    }
                )
            }

            Divider()

            // Account Settings Button
            Button(
                onClick = onNavigateToAccountSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Account Settings")
            }

            // Logout Button
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}

@Composable
fun DropdownMenuWithSelectedOption(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = selectedOption)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    text = { Text(option) }
                )
            }
        }
    }
}
