package com.example.tts_ocrapplication.utils

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.tts_ocrapplication.MainActivity
import com.example.tts_ocrapplication.tts.TTSManager

class PermissionsHelper(private val activity: ComponentActivity, private val ttsManager: TTSManager) {

    var cameraPermissionGranted = false
        private set

    private val requestCameraPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d("PermissionsHelper", "✅ Camera permission result: $isGranted")
            cameraPermissionGranted = isGranted
            if (isGranted) {
                Log.d("PermissionsHelper", " Camera permission granted.")
                ttsManager.speak("Camera permission granted.")
                (activity as? MainActivity)?.initializeComponents() // ✅ Initialize after permission granted
            } else {
                Log.d("PermissionsHelper", "🚨 Camera permission denied, guiding user to settings")
                ttsManager.speak("Camera permission denied. Open settings to enable it.")
                guideUserToSettings()
            }
        }

    fun checkCameraPermission(): Boolean {
        cameraPermissionGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        return cameraPermissionGranted
    }

    fun requestCameraPermission() {
        Log.d("PermissionsHelper", "🔔 Requesting camera permission")
        ttsManager.speak("Camera permission is required for text recognition. A pop-up will appear. Press Allow to continue.")
        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun guideUserToSettings() {
        Log.d("PermissionsHelper", "⚙️ Guiding user to settings for permission")
        ttsManager.speak("Permission denied. Open settings and enable camera access.")
        activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        })
    }
}
