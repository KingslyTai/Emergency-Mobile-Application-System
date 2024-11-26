plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id ("kotlin-parcelize")
}

android {
    namespace = "com.example.emergencymobileapplicationsystem"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.emergencymobileapplicationsystem"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "androidx.appcompat") {
                useVersion("1.4.1") // Set a specific version for appcompat
            }
        }
    }
}

dependencies {
    // Firebase BOM - ensures Firebase libraries have compatible versions
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.google.firebase.auth.ktx)
    implementation(libs.google.firebase.firestore.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.storage.ktx)

    // Core AndroidX dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Explicit appcompat dependencies to avoid conflicts
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.appcompat:appcompat-resources:1.4.1")

    // Compose UI dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.ui.graphics)
    implementation("androidx.compose.material:material-icons-extended:1.3.1")
    implementation("androidx.compose.material:material:1.4.3")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.1.0")

    // Room dependencies
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.runtime.runtime.livedata)
    implementation(libs.firebase.storage)
    ksp(libs.androidx.room.compiler)

    // Navigation and testing dependencies
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui.test.android)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core.v350)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)

    // Google Play Services for Maps and Location
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.places)
    implementation(libs.places.v330)
    implementation(libs.play.services.maps.v1810)
    implementation(libs.places.v260)

    // Coroutine dependencies
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // ViewModel dependency
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Retrofit and OkHttp dependencies
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.gson)
    implementation(libs.squareup.okhttp)
    implementation(libs.squareup.okhttp.logging)

    // Moshi for JSON parsing
    implementation(libs.squareup.moshi)
    implementation(libs.squareup.moshi.kotlin)

    // Kotlin BOM
    implementation(platform(libs.kotlin.bom))

    // Debugging tools for Compose
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // Unit Testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:3.11.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")

    // Instrumentation Testing (UI tests)
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
