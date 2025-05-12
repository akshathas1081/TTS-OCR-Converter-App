plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.example.ttsocrapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ttsocrapp"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        debug {
            // Optional: add any debug-specific configurations here if needed
        }
    }

    buildFeatures {
        buildConfig = true // ✅ Enables BuildConfig
    }

    secrets {
        propertiesFileName = "local.properties"
    }

//    defaultConfig {
//        val apiKey: String? = project.findProperty("GOOGLE_CLOUD_VISION_API_KEY") as String?
//        buildConfigField("String", "GOOGLE_CLOUD_VISION_API_KEY", "\"${apiKey ?: ""}\"") // ✅ Set API_KEY in BuildConfig
//    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Enable Jetpack Compose
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.0"
    }

    android {
        packagingOptions {
            resources {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}" // Exclude license files
                    excludes += "META-INF/*.kotlin_module" // Exclude Kotlin metadata
                    excludes += "META-INF/DEPENDENCIES" // Exclude dependencies metadata
                    excludes += "META-INF/INDEX.LIST" // Exclude INDEX.LIST to fix the issue
                }
            }
        }
    }

}

dependencies {
    // ML Kit Text Recognition
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // Jetpack Compose dependencies
    implementation("androidx.compose.ui:ui:1.7.7")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.7")
    implementation("androidx.activity:activity-compose:1.10.0")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.3.0")

    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // CameraX libraries for image capture
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Google Cloud Vision API
    implementation("com.google.cloud:google-cloud-vision:3.12.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")

// Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Coroutine support for async requests
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Coroutine Support for Google Play Services Tasks (Required for .await())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")



}
