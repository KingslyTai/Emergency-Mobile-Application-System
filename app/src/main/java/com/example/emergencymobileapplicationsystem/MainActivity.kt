package com.example.emergencymobileapplicationsystem

import RequestCreationScreen
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.emergencymobileapplicationsystem.data.PlaceInfo
import com.example.emergencymobileapplicationsystem.data.VolunteerRequest
import com.example.emergencymobileapplicationsystem.repository.ProfileRepository
import com.example.emergencymobileapplicationsystem.repository.VolunteerRepository
import com.example.emergencymobileapplicationsystem.ui.screen.AcceptedRequestDetailsScreen
import com.example.emergencymobileapplicationsystem.ui.screen.AdditionalDetailsSuccessScreen
import com.example.emergencymobileapplicationsystem.ui.screen.ChatScreen
import com.example.emergencymobileapplicationsystem.ui.screen.CommentScreen
import com.example.emergencymobileapplicationsystem.ui.screen.CompletedRequestsScreen
import com.example.emergencymobileapplicationsystem.ui.screen.CreateAdditionalDetailsScreen
import com.example.emergencymobileapplicationsystem.ui.screen.CreateEmergencyInfoScreen
import com.example.emergencymobileapplicationsystem.ui.screen.CreateEmergencyInfoSuccessScreen
import com.example.emergencymobileapplicationsystem.ui.screen.CreateFeedbackScreen
import com.example.emergencymobileapplicationsystem.ui.screen.CreateProfileScreen
import com.example.emergencymobileapplicationsystem.ui.screen.CreateProfileSuccessScreen
import com.example.emergencymobileapplicationsystem.ui.screen.CreateVolunteerScreen
import com.example.emergencymobileapplicationsystem.ui.screen.CreatedRequestsScreen
import com.example.emergencymobileapplicationsystem.ui.screen.EditAdditionalDetailsScreen
import com.example.emergencymobileapplicationsystem.ui.screen.EditEmergencyInfoScreen
import com.example.emergencymobileapplicationsystem.ui.screen.EditProfileScreen
import com.example.emergencymobileapplicationsystem.ui.screen.EditVolunteerScreen
import com.example.emergencymobileapplicationsystem.ui.screen.EmergencyScreen
import com.example.emergencymobileapplicationsystem.ui.screen.MyLocationScreen
import com.example.emergencymobileapplicationsystem.ui.screen.ProfileScreen
import com.example.emergencymobileapplicationsystem.ui.screen.RegisteredUserFeedbackScreen
import com.example.emergencymobileapplicationsystem.ui.screen.ReportHistoryScreen
import com.example.emergencymobileapplicationsystem.ui.screen.ReportListScreen
import com.example.emergencymobileapplicationsystem.ui.screen.ReportScreen
import com.example.emergencymobileapplicationsystem.ui.screen.SettingsScreen
import com.example.emergencymobileapplicationsystem.ui.screen.SpecificUserHomeScreen
import com.example.emergencymobileapplicationsystem.ui.screen.SpecificUserLocationScreen
import com.example.emergencymobileapplicationsystem.ui.screen.ViewCommentsScreen
import com.example.emergencymobileapplicationsystem.ui.screen.ViewFeedbackScreen
import com.example.emergencymobileapplicationsystem.ui.screen.ViewOnlyChatScreen
import com.example.emergencymobileapplicationsystem.ui.screen.ViewProfileScreen
import com.example.emergencymobileapplicationsystem.ui.screen.VolunteerDetailsScreen
import com.example.emergencymobileapplicationsystem.ui.screen.VolunteerRequestDetailsScreen
import com.example.emergencymobileapplicationsystem.ui.screen.VolunteerRequestListScreen
import com.example.emergencymobileapplicationsystem.ui.screen.VolunteerServiceScreen
import com.example.emergencymobileapplicationsystem.ui.screen.VolunteerStatusScreen
import com.example.emergencymobileapplicationsystem.ui.screen.VolunteerVerificationScreen
import com.example.emergencymobileapplicationsystem.ui.theme.EmergencyMobileApplicationSystemTheme
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import volunteerRepository
import java.util.Locale

private const val PREFERENCES_NAME = "BlockedLocationsPrefs"
private const val BLOCKED_LOCATIONS_KEY = "blocked_locations"

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavHostController
    private val firestore = FirebaseFirestore.getInstance()
    private var profileRefreshKey by mutableIntStateOf(0)
    private val profileRepository by lazy { ProfileRepository(firestore) }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (locationGranted || coarseLocationGranted) {
            navigateToLocationScreen()
        } else {
            showPermissionDeniedMessage()
        }
    }

    // Function to load blocked locations from SharedPreferences
    private fun loadBlockedLocations(context: Context): MutableList<String> {
        val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(BLOCKED_LOCATIONS_KEY, emptySet())?.toMutableList()
            ?: mutableListOf()
    }

    // Function to save a blocked location to SharedPreferences
    private fun saveBlockedLocation(context: Context, placeId: String) {
        val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val blockedSet = prefs.getStringSet(BLOCKED_LOCATIONS_KEY, mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()
        blockedSet.add(placeId)
        prefs.edit().putStringSet(BLOCKED_LOCATIONS_KEY, blockedSet).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        auth = FirebaseAuth.getInstance()

        generateAndSaveFcmToken()

        setContent {
            navController = rememberNavController()

            var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
            val isSpecificUser = isSpecificUser(auth)
            val blockedLocations =
                remember { mutableStateListOf<String>().apply { addAll(loadBlockedLocations(this@MainActivity)) } }
            val isDrawerOpen = remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            var isDarkModeEnabled by remember { mutableStateOf(false) }


            DisposableEffect(auth) {
                val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    isLoggedIn = firebaseAuth.currentUser != null
                }
                auth.addAuthStateListener(authListener)

                onDispose {
                    auth.removeAuthStateListener(authListener)
                }
            }

            EmergencyMobileApplicationSystemTheme(darkTheme = isDarkModeEnabled) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = if (isLoggedIn && isSpecificUser) "specificUserHomeScreen" else "emergencyScreen"
                    ) {

                        composable("emergencyScreen") {
                            // Reset drawer state when entering this screen
                            LaunchedEffect(Unit) {
                                isDrawerOpen.value = false
                            }

                            EmergencyScreen(
                                onNavigateToFeedback = {
                                    isDrawerOpen.value = false
                                    if (isLoggedIn) {
                                        navController.navigate("registeredUserFeedbackScreen")
                                    } else {
                                        navController.navigate("viewFeedbackScreen")
                                    }
                                },

                                onCreateAccount = {
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            AuthActivity::class.java
                                        ).apply {
                                            putExtra("isRegister", true)
                                        })
                                },
                                isLoggedIn = isLoggedIn,
                                onLogin = {
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            AuthActivity::class.java
                                        ).apply {
                                            putExtra("isRegister", false)
                                        })
                                },
                                onSignOut = {
                                    auth.signOut()
                                },
                                onManageProfile = {
                                    isDrawerOpen.value = false
                                    navController.navigate("profileScreen")
                                },
                                onNavigateToMyLocation = {
                                    isDrawerOpen.value = false
                                    checkLocationPermissions(isSpecificUser(auth))
                                },
                                onNavigateToReportScreen = { _, refreshReports ->
                                    navController.navigate("reportScreen") {
                                        refreshReports()
                                    }
                                },
                                onNavigateToReportListScreen = {
                                    isDrawerOpen.value = false
                                    navController.navigate("reportListScreen")
                                },
                                onNavigateToVolunteerDetails = {
                                    navController.navigate("volunteerDetailsScreen") // Navigates to the Volunteer Details screen
                                },
                                onChatClick = { reportId ->
                                    navController.navigate("chatScreen/$reportId")
                                },
                                isDrawerOpen = isDrawerOpen,
                                onToggleDrawer = {
                                    coroutineScope.launch {
                                        isDrawerOpen.value = !isDrawerOpen.value
                                    }
                                },
                                onNavigateToVolunteerService = {
                                    navController.navigate("volunteerServiceScreen")
                                },
                                onNavigateToVolunteerRequestList = {
                                    // Navigate to the Volunteer Request List Screen
                                    navController.navigate("volunteerRequestListScreen")
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settingsScreen")
                                },
                                isInEmergencyScreen = true
                            )
                        }

                        composable("volunteerServiceScreen") {
                            VolunteerServiceScreen(
                                // Navigate to Request Creation Screen
                                onRequestVolunteerService = {
                                    navController.navigate("requestCreationScreen")
                                },
                                // Navigate to Emergency Help (return to Emergency Screen)
                                onNavigateToEmergencyHelp = {
                                    navController.navigate("emergencyScreen") {
                                        popUpTo("volunteerServiceScreen") { inclusive = true }
                                    }
                                },
                                // Navigate to Register as Volunteer Screen
                                onRegisterAsVolunteer = {
                                    navController.navigate("createVolunteerScreen")
                                },
                                // Navigate to Volunteer Application Status Screen
                                onNavigateToStatusScreen = {
                                    navController.navigate("volunteerStatusScreen")
                                },
                                // Volunteer Request List Navigation
                                onNavigateToVolunteerRequestList = {
                                    navController.navigate("volunteerRequestListScreen")
                                },
                                isInEmergencyScreen = false,
                                // Navigate to Feedback Screen
                                onNavigateToFeedback = {
                                    navController.navigate("feedbackScreen")
                                },
                                // Navigate to Account Creation Screen
                                onCreateAccount = {
                                    navController.navigate("registerScreen")
                                },
                                // Pass the current login state
                                isLoggedInState = remember { mutableStateOf(auth.currentUser != null) },
                                // Navigate to Login Screen
                                onLogin = {
                                    navController.navigate("loginScreen")
                                },
                                // Handle Sign Out
                                onSignOut = {
                                    auth.signOut() // Sign out user
                                    navController.navigate("emergencyScreen") {
                                        popUpTo("volunteerServiceScreen") { inclusive = true }
                                    }
                                },
                                // Navigate to Profile Management Screen
                                onManageProfile = {
                                    navController.navigate("profileScreen")
                                },
                                // Navigate to My Location Screen
                                onNavigateToMyLocation = {
                                    navController.navigate("myLocationScreen")
                                },
                                // Navigate to Report List Screen
                                onNavigateToReportListScreen = {
                                    navController.navigate("reportListScreen")
                                },
                                // Navigate to Volunteer Details Screen
                                onNavigateToVolunteerDetails = {
                                    navController.navigate("volunteerDetailsScreen")
                                },
                                onNavigateToAcceptedRequestDetails = {
                                    navController.navigate("acceptedRequestDetailsScreen")
                                },
                                onNavigateToCompletedRequests = {
                                    navController.navigate("completedRequestScreen")
                                },
                                onNavigateToCreatedRequests = {
                                    navController.navigate("createdRequestsScreen")
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settingsScreen")
                                }
                            )
                        }


                        composable("volunteerRequestListScreen") {
                            VolunteerRequestListScreen(
                                volunteerId = auth.currentUser?.uid ?: "",
                                onNavigateToRequestDetails = { selectedRequest ->
                                    // Log the selected request ID
                                    Log.d("Navigation", "Navigating to details with requestId: ${selectedRequest.requestId}")

                                    // Pass the request ID to the details screen
                                    navController.navigate("volunteerRequestDetailsScreen/${selectedRequest.requestId}")
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("settingsScreen") {
                            SettingsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onLogout = { /* Handle logout */ },
                                onNavigateToAccountSettings = { navController.navigate("accountSettings") },
                                isDarkModeEnabled = isDarkModeEnabled,
                                onDarkModeToggle = { isDarkModeEnabled = it },
                                        onLanguageChange = { newLanguage ->
                                            // Handle language change logic
                                            setAppLanguage(newLanguage)
                                        }
                            )
                        }


                        composable("completedRequestScreen") {
                            CompletedRequestsScreen(
                                onBack = {
                                    navController.popBackStack() // Navigate back to Volunteer Service Screen
                                }
                            )
                        }

                        composable("createdRequestsScreen") {
                            CreatedRequestsScreen(
                                onBack = {
                                    navController.popBackStack() // Navigate back to Volunteer Service Screen
                                }
                            )
                        }
                        
                        composable(
                            route = "volunteerRequestDetailsScreen/{requestId}",
                            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val requestId = backStackEntry.arguments?.getString("requestId")

                            if (requestId != null) {
                                var request by remember { mutableStateOf<VolunteerRequest?>(null) }
                                val volunteerRepository = VolunteerRepository()

                                LaunchedEffect(requestId) {
                                    volunteerRepository.getVolunteerRequestById(requestId) { fetchedRequest ->
                                        request = fetchedRequest
                                    }
                                }

                                if (request != null) {
                                    VolunteerRequestDetailsScreen(
                                        request = request,
                                        onBack = { navController.popBackStack() },
                                        onNavigateToViewProfile = { userId ->
                                            navController.navigate("viewProfileScreen/$userId")
                                        }
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Request not found.")
                                    }
                                }
                            }
                        }

                        composable("acceptedRequestDetailsScreen") {
                            AcceptedRequestDetailsScreen(
                                onBack = {
                                    navController.popBackStack() // Navigate back to Volunteer Service Screen
                                }
                            )
                        }


                        composable(
                            route = "viewProfileScreen/{userId}",
                            arguments = listOf(navArgument("userId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: ""

                            ViewProfileScreen(
                                userId = userId,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }


                        composable("createVolunteerScreen") {
                            CreateVolunteerScreen(
                                onNavigateToStatus = {
                                    // Navigate to VolunteerStatusScreen after submission
                                    navController.navigate("volunteerStatusScreen")
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("requestCreationScreen") {
                            RequestCreationScreen(
                                userId = auth.currentUser?.uid ?: "",
                                onSubmitRequest = { request ->
                                    submitRequestToFirestore(request) { success ->
                                        if (success) {
                                            Toast.makeText(this@MainActivity, "Request Submitted!", Toast.LENGTH_SHORT).show()
                                            navController.navigate("volunteerServiceScreen") // Navigate to request status screen
                                        } else {
                                            Toast.makeText(this@MainActivity, "Failed to Submit Request", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onBack = {
                                    navController.popBackStack() // Navigate back to VolunteerServiceScreen
                                }
                            )
                        }

                        composable("volunteerDetailsScreen") {
                            VolunteerDetailsScreen(
                                navController = navController,
                                onRegisterAsVolunteer = {
                                    navController.navigate("createVolunteerScreen")
                                },
                                onEditVolunteer = {
                                    navController.navigate("editVolunteerScreen")
                                },
                                onDeleteVolunteer = {
                                    // Handle the delete action, such as deleting the volunteer data from Firebase
                                    val auth = FirebaseAuth.getInstance()
                                    val firestore = FirebaseFirestore.getInstance()
                                    val userId = auth.currentUser?.uid


                                    if (userId != null) {
                                        firestore.collection("volunteers").document(userId).delete()
                                            .addOnSuccessListener {
                                                // Navigate back or show a confirmation message
                                                navController.popBackStack()
                                                Toast.makeText(this@MainActivity, "Volunteer profile deleted successfully!", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this@MainActivity, "Failed to delete profile: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                            )
                        }

                        composable("editVolunteerScreen") {
                            EditVolunteerScreen(
                                onSaveChanges = {
                                    navController.popBackStack() // Navigate back after saving
                                },
                                onCancel = {
                                    navController.popBackStack() // Navigate back without saving
                                }
                            )
                        }


                        composable("createVolunteerScreen") {
                            CreateVolunteerScreen(
                                onNavigateToStatus = {
                                    navController.navigate("volunteerStatusScreen")
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("volunteerStatusScreen") {
                            VolunteerStatusScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }


                        composable("createAdditionalDetailsScreen") {
                            CreateAdditionalDetailsScreen(
                                navController = navController, // Pass the NavController for back navigation
                                onSaveSuccess = {
                                    navController.navigate("additionalDetailsSuccessScreen?message=Created Successfully")
                                }
                            )
                        }

                        composable("editAdditionalDetailsScreen") {
                            EditAdditionalDetailsScreen(
                                navController = navController, // Pass navController for back navigation
                                onSaveSuccess = { message ->
                                    navController.navigate("additionalDetailsSuccessScreen?message=${message}")
                                },
                                onDeleteSuccess = { message ->
                                    navController.navigate("additionalDetailsSuccessScreen?message=${message}")
                                }
                            )
                        }

                        composable("additionalDetailsSuccessScreen?message={message}") { backStackEntry ->
                            val message = backStackEntry.arguments?.getString("message") ?: "Operation Successful!"
                            AdditionalDetailsSuccessScreen(
                                successMessage = message,
                                onBackToDetails = { navController.popBackStack("ProfileScreen", inclusive = false) }
                            )
                        }


                        composable("specificUserHomeScreen") {
                            SpecificUserHomeScreen(
                                onNavigateToEmergencyInfo = {
                                    navController.navigate("specificUserLocationScreen")
                                },
                                onNavigateToFeedback = {
                                    navController.navigate("registeredUserFeedbackScreen")
                                },
                                onManageUsers = {
                                    navController.navigate("reportHistoryScreen")
                                },
                                onSignOut = {
                                    auth.signOut()
                                    navController.navigate("emergencyScreen") {
                                        popUpTo("specificUserHomeScreen") { inclusive = true }
                                    }
                                },
                                onChatClick = { reportId ->
                                    navController.navigate("chatScreen/$reportId")
                                },
                                onUpdateStatus = { reportId, newStatus ->
                                    saveEditedReportStatus(reportId, newStatus)
                                },
                                onNavigateToVolunteerVerification = {
                                    navController.navigate("volunteerVerificationScreen") }
                            )
                        }

                        composable("volunteerVerificationScreen") {
                            VolunteerVerificationScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("specificUserLocationScreen") {
                            SpecificUserLocationScreen(
                                navController = navController,
                                saveLocation = { latitude, longitude ->
                                    saveLocationToFirebase(latitude, longitude)
                                },
                                deleteLocation = { placeInfo, onDeleteSuccess, onDeleteFailure ->
                                    if (placeInfo.documentId == null) {
                                        placeInfo.placeId?.let {
                                            blockedLocations.add(it)
                                            saveBlockedLocation(
                                                this@MainActivity,
                                                it
                                            ) // Save to SharedPreferences
                                        }
                                        onDeleteSuccess()
                                    } else {
                                        deleteLocationFromFirebase(
                                            placeInfo,
                                            onDeleteSuccess = onDeleteSuccess,
                                            onDeleteFailure = onDeleteFailure
                                        )
                                    }
                                },
                                blockedLocations = blockedLocations // Pass blocked locations list
                            )
                        }

                        composable("reportScreen") {
                            ReportScreen(
                                navController = navController,
                                userId = auth.currentUser?.uid ?: "Unknown",
                                onSaveReport = { reportType, description, latitude, longitude, status, userId, onReportSaved ->
                                    saveReportToFirebase(
                                        reportType,
                                        description,
                                        latitude,
                                        longitude,
                                        status,
                                        userId
                                    ) { reportId ->
                                        onReportSaved(reportId)
                                    }
                                },
                                onRefreshReports = { navController.popBackStack("emergencyScreen", inclusive = false) } // Refresh after report is saved
                            )
                        }

                        composable("reportHistoryScreen") {
                            ReportHistoryScreen(
                                navController = navController,
                                onNavigateBack = { navController.popBackStack() },
                                onViewChat = { reportId, emergencyType, emergencyDescription ->
                                    navController.navigate("viewOnlyChatScreen/$reportId/$emergencyType/$emergencyDescription")
                                }
                            )
                        }

                        composable(
                            "viewOnlyChatScreen/{reportId}/{emergencyType}/{emergencyDescription}",
                            arguments = listOf(
                                navArgument("reportId") { type = NavType.StringType },
                                navArgument("emergencyType") { type = NavType.StringType },
                                navArgument("emergencyDescription") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val reportId = backStackEntry.arguments?.getString("reportId") ?: ""
                            val emergencyType = backStackEntry.arguments?.getString("emergencyType")
                                ?: "Unknown Type"
                            val emergencyDescription =
                                backStackEntry.arguments?.getString("emergencyDescription")
                                    ?: "No description available"

                            ViewOnlyChatScreen(
                                reportId = reportId,
                                emergencyType = emergencyType,
                                emergencyDescription = emergencyDescription,
                                navController = navController,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("profileScreen") {
                            ProfileScreen(
                                navController = navController,
                                refreshKey = profileRefreshKey, // Pass the refresh key here
                                onNavigateToCreateProfile = { navController.navigate("createProfile") },
                                onNavigateToEditProfile = { navController.navigate("editProfile") },
                                onNavigateToCreateAdditionalDetails = { navController.navigate("createAdditionalDetailsScreen") },
                                onNavigateToEditAdditionalDetails = { navController.navigate("editAdditionalDetailsScreen") },

                                onDeleteProfile = {
                                    auth.currentUser?.let { user ->
                                        coroutineScope.launch {
                                            profileRepository.deleteProfile(user.uid)
                                                .onSuccess {
                                                    Toast.makeText(this@MainActivity, "Profile deleted successfully", Toast.LENGTH_SHORT).show()
                                                    navController.popBackStack()
                                                }
                                                .onFailure { e ->
                                                    Log.e("ProfileScreen", "Failed to delete profile: ${e.message}")
                                                    Toast.makeText(this@MainActivity, "Failed to delete profile", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    } ?: run {
                                        Toast.makeText(this@MainActivity, "User not authenticated", Toast.LENGTH_SHORT).show()
                                    }
                                },

                                onDeleteAdditionalDetails = {
                                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                                    if (userId != null) {
                                        FirebaseFirestore.getInstance().collection("additionalDetails").document(userId)
                                            .delete()
                                            .addOnSuccessListener {
                                                profileRefreshKey++ // Increment key to trigger refresh
                                                navController.popBackStack("profileScreen", inclusive = false)
                                                Toast.makeText(navController.context, "Additional Details deleted successfully", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("ProfileScreen", "Failed to delete additional details: ${e.message}")
                                                Toast.makeText(navController.context, "Failed to delete additional details", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                            )
                        }


                        composable("reportListScreen") {
                            ReportListScreen(
                                userId = getCurrentUserId(),
                                navController = navController,
                                onNavigateBack = { navController.popBackStack() },
                                onChatClick = { reportId ->
                                    navController.navigate("chatScreen/$reportId") // Pass reportId to ChatScreen
                                }
                            )
                        }

                        composable(
                            "chatScreen/{reportId}",
                            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val reportId = backStackEntry.arguments?.getString("reportId") ?: ""
                            ChatScreen(
                                reportId = reportId,
                                userId = getCurrentUserId(),
                                onNavigateBack = { navController.popBackStack() } // Add back navigation
                            )
                        }


                        composable("myLocationScreen") {
                            MyLocationScreen(
                                navController = navController,
                                saveLocation = { latitude, longitude ->
                                    saveLocationToFirebase(
                                        latitude,
                                        longitude
                                    )
                                },
                                blockedLocations = blockedLocations // Pass the blocked locations list
                            )
                        }

                        composable(
                            "editEmergencyInfoScreen/{documentId}/{name}/{address}/{latitude}/{longitude}/{phoneNumber}/{isGoogleLocation}",
                            arguments = listOf(
                                navArgument("documentId") { type = NavType.StringType },
                                navArgument("name") { type = NavType.StringType },
                                navArgument("address") { type = NavType.StringType },
                                navArgument("latitude") { type = NavType.StringType },
                                navArgument("longitude") { type = NavType.StringType },
                                navArgument("phoneNumber") { type = NavType.StringType },
                                navArgument("isGoogleLocation") { type = NavType.BoolType }
                            )
                        ) { backStackEntry ->
                            val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
                            val name = backStackEntry.arguments?.getString("name") ?: ""
                            val address = backStackEntry.arguments?.getString("address") ?: ""
                            val latitude =
                                backStackEntry.arguments?.getString("latitude")?.toDouble() ?: 0.0
                            val longitude =
                                backStackEntry.arguments?.getString("longitude")?.toDouble() ?: 0.0
                            val phoneNumber =
                                backStackEntry.arguments?.getString("phoneNumber") ?: ""
                            val isGoogleLocationValue =
                                backStackEntry.arguments?.getBoolean("isGoogleLocation") ?: false

                            val place = PlaceInfo(
                                latLng = LatLng(latitude, longitude),
                                name = name,
                                address = address,
                                phoneNumber = phoneNumber,
                                placeId = documentId
                            )

                            EditEmergencyInfoScreen(
                                navController = navController,
                                place = place,
                                documentId = documentId,
                                isGoogleLocation = isGoogleLocationValue,
                                onSaveChanges = { updatedPlace, docId, isGoogleLocationInternal, type ->
                                    if (isGoogleLocationInternal) {
                                        saveNewEditedEmergencyLocationToFirebase(updatedPlace, type)
                                    } else {
                                        if (!docId.isNullOrEmpty()) {
                                            saveEditedEmergencyLocationToFirebase(
                                                updatedPlace,
                                                docId,
                                                type
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        composable("createEmergencyInfoScreen") {
                            CreateEmergencyInfoScreen(
                                navController = navController,
                                onSaveEmergencyLocation = { placeInfo, type, callback ->
                                    saveEmergencyLocationToFirebase(placeInfo, type, callback)
                                }
                            )
                        }

                        composable("createEmergencyInfoSuccessScreen/{message}") { backStackEntry ->
                            val message = backStackEntry.arguments?.getString("message")
                                ?: "Operation Successful!"
                            CreateEmergencyInfoSuccessScreen(
                                message = message,
                                navController = navController
                            )
                        }

                        composable("createProfile") {
                            CreateProfileScreen(navController = navController, profileRepository = profileRepository)
                        }

                        composable("editProfile") {
                            EditProfileScreen(navController = navController, profileRepository = profileRepository)
                        }

                        composable("CreateProfileSuccessScreen/{message}") { backStackEntry ->
                            val message = backStackEntry.arguments?.getString("message")
                                ?: "Operation Successful!"
                            CreateProfileSuccessScreen(
                                message = message,
                                navController = navController
                            )
                        }

                        composable("viewFeedbackScreen") {
                            ViewFeedbackScreen(
                                navController = navController,
                                onCommentClick = { feedbackId ->
                                    navController.navigate("viewCommentScreen/$feedbackId")
                                }
                            )
                        }

                        composable("registeredUserFeedbackScreen") {
                            RegisteredUserFeedbackScreen(
                                navController = navController,
                                onCommentClick = { feedbackId ->
                                    navController.navigate("commentScreen/$feedbackId")
                                },
                                onCreateFeedbackClick = {
                                    navController.navigate("createFeedbackScreen")
                                }
                            )
                        }

                        composable("createFeedbackScreen") {
                            CreateFeedbackScreen(
                                navController = navController,
                                onSaveFeedback = { feedbackContent ->
                                    saveFeedbackToFirebase(feedbackContent)
                                }
                            )
                        }

                        composable(
                            "commentScreen/{feedbackId}",
                            arguments = listOf(
                                navArgument("feedbackId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val feedbackId = backStackEntry.arguments?.getString("feedbackId") ?: ""
                            CommentScreen(feedbackId = feedbackId, navController = navController)
                        }

                        composable(
                            "viewCommentScreen/{feedbackId}",
                            arguments = listOf(
                                navArgument("feedbackId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val feedbackId = backStackEntry.arguments?.getString("feedbackId") ?: ""
                            ViewCommentsScreen(
                                feedbackId = feedbackId,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setAppLanguage(language: String) {
        val locale = when (language) {
            "English" -> Locale("en")
            "Spanish" -> Locale("es")
            "French" -> Locale("fr")
            "Chinese" -> Locale("zh")
            else -> Locale.getDefault()
        }
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate() // Optional: Restart activity to apply changes
    }


    // Function to submit a request to Firestore
    private fun submitRequestToFirestore(
        request: VolunteerRequest,
        onSuccess: (Boolean) -> Unit
    ) {
        volunteerRepository.addVolunteerRequest(request) { success, documentId ->
            if (success) {
                Log.d("RequestCreation", "Request added with ID: $documentId")

                // Call onSuccess to indicate successful request submission
                onSuccess(true)
            } else {
                Log.w("RequestCreation", "Error adding request")
                onSuccess(false)
            }
        }
    }


    // Helper function to get current user's ID
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }


    private fun saveEditedReportStatus(reportId: String, newStatus: String) {
        if (reportId.isNotEmpty()) {
            firestore.collection("emergencyReports")
                .document(reportId)
                .update("status", newStatus)  // Only update the 'status' field
                .addOnSuccessListener {
                    Log.d("MainActivity", "Successfully updated report status to: $newStatus")
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Error updating report status: ${e.message}")
                }
        } else {
            Log.e("MainActivity", "Invalid report ID")
        }
    }


    private fun saveReportToFirebase(
        reportType: String,
        description: String,
        latitude: Double?,
        longitude: Double?,
        status: String,
        userId: String?,  // Include userId
        onReportSaved: (String) -> Unit
    ) {
        val reportData = hashMapOf(
            "reportType" to reportType,
            "description" to description,
            "latitude" to latitude,
            "longitude" to longitude,
            "status" to status,
            "userId" to userId,  // Save the user ID (or null for non-registered users)
            "timestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("emergencyReports")
            .add(reportData)
            .addOnSuccessListener { documentReference ->
                val reportId = documentReference.id
                Log.d("MainActivity", "Report successfully created with ID: $reportId")
                onReportSaved(reportId)  // Pass the reportId to the callback
            }
            .addOnFailureListener { e ->
                Log.w("MainActivity", "Error adding report", e)
            }
    }

    private fun saveEmergencyLocationToFirebase(
        placeInfo: PlaceInfo,
        type: String,
        callback: (Boolean, String?) -> Unit
    ) {
        firestore.collection("emergencylocation")
            .whereEqualTo("name", placeInfo.name)
            .whereEqualTo("type", type)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    val emergencyData = hashMapOf(
                        "name" to placeInfo.name,
                        "address" to placeInfo.address,
                        "latitude" to placeInfo.latLng.latitude,
                        "longitude" to placeInfo.latLng.longitude,
                        "phoneNumber" to placeInfo.phoneNumber,
                        "type" to type
                    )
                    firestore.collection("emergencylocation")
                        .add(emergencyData)
                        .addOnSuccessListener {
                            callback(true, null)
                            navController.navigate("createEmergencyInfoSuccessScreen/Success")
                        }
                        .addOnFailureListener { e ->
                            callback(false, "Failed to create emergency location: ${e.message}")
                        }
                } else {
                    callback(false, "A location with the same name and type already exists.")
                }
            }
            .addOnFailureListener { e ->
                callback(false, "Error checking for duplicates: ${e.message}")
            }
    }

    private fun saveEditedEmergencyLocationToFirebase(
        placeInfo: PlaceInfo,
        documentId: String,
        type: String
    ) {
        val updatedData = hashMapOf(
            "name" to placeInfo.name,
            "address" to placeInfo.address,
            "latitude" to placeInfo.latLng.latitude,
            "longitude" to placeInfo.latLng.longitude,
            "phoneNumber" to placeInfo.phoneNumber,
            "type" to type
        )

        firestore.collection("emergencylocation").document(documentId)
            .set(updatedData)
            .addOnSuccessListener {
                Log.d("MainActivity", "Emergency location updated successfully.")
                navController.popBackStack("specificUserLocationScreen", inclusive = false)
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Failed to update emergency location", e)
            }
    }

    private fun saveNewEditedEmergencyLocationToFirebase(placeInfo: PlaceInfo, type: String) {
        val newLocationData = hashMapOf(
            "name" to placeInfo.name,
            "address" to placeInfo.address,
            "latitude" to placeInfo.latLng.latitude,
            "longitude" to placeInfo.latLng.longitude,
            "phoneNumber" to placeInfo.phoneNumber,
            "type" to type
        )

        firestore.collection("emergencylocation")
            .add(newLocationData)
            .addOnSuccessListener {
                Log.d("MainActivity", "New edited emergency location saved successfully.")
                navController.popBackStack("specificUserLocationScreen", inclusive = false)
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Failed to save new edited emergency location", e)
            }
    }

    private fun checkLocationPermissions(isSpecificUser: Boolean) {
        val fineLocationGranted = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {
            if (isSpecificUser) {
                navController.navigate("specificUserLocationScreen")
            } else {
                navController.navigate("myLocationScreen")
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show()
    }

    private fun isSpecificUser(auth: FirebaseAuth): Boolean {
        val specificEmails = listOf("polis@gmail.com", "bomba@gmail.com")
        return auth.currentUser?.email in specificEmails
    }

    private fun navigateToLocationScreen() {
        navController.navigate("myLocationScreen")
    }

    private fun saveLocationToFirebase(latitude: String, longitude: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val locationData = hashMapOf(
                "userId" to userId,
                "latitude" to latitude,
                "longitude" to longitude
            )

            firestore.collection("locations").document(userId)
                .set(locationData)
                .addOnSuccessListener {
                    Log.d(
                        "MainActivity",
                        "Location saved successfully: Lat $latitude, Lon $longitude"
                    )
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Failed to save location to Firebase", e)
                }
        } else {
            Log.e("MainActivity", "User not logged in, unable to save location")
        }
    }

    private fun deleteLocationFromFirebase(
        placeInfo: PlaceInfo,
        onDeleteSuccess: () -> Unit,
        onDeleteFailure: (String) -> Unit
    ) {
        val documentId = placeInfo.documentId
        if (!documentId.isNullOrEmpty()) {
            firestore.collection("emergencylocation").document(documentId)
                .delete()
                .addOnSuccessListener {
                    Log.d("MainActivity", "Emergency location deleted successfully.")
                    onDeleteSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Failed to delete emergency location", e)
                    onDeleteFailure("Failed to delete emergency location: ${e.message}")
                }
        } else {
            Log.e("MainActivity", "Document ID is null or empty; cannot delete location.")
            onDeleteFailure("Document ID is null or empty; cannot delete location.")
        }
    }

    private fun saveFeedbackToFirebase(feedbackContent: String) {
        val userId = auth.currentUser?.uid ?: "" // Get the current user ID
        val feedbackData = hashMapOf(
            "content" to feedbackContent,
            "author" to (auth.currentUser?.email ?: "Anonymous"),
            "userId" to userId // Add the userId field to store the creator's ID
        )

        firestore.collection("feedback")
            .add(feedbackData)
            .addOnSuccessListener {
                Log.d("MainActivity", "Feedback successfully added!")
                navController.popBackStack()
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Failed to add feedback", e)
            }
    }
    private fun generateAndSaveFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                println("Fetching FCM token failed: ${task.exception}")
                return@addOnCompleteListener
            }

            // Get new FCM token
            val token = task.result
            println("FCM Token: $token")

            // Save token in Firestore (optional)
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            userId?.let {
                FirebaseFirestore.getInstance().collection("users").document(it)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        println("FCM token successfully saved!")
                    }
                    .addOnFailureListener {
                        println("Failed to save FCM token: ${it.message}")
                    }
            }
        }
    }
}