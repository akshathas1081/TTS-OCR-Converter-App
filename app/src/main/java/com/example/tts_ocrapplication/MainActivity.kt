package com.example.tts_ocrapplication

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.example.tts_ocrapplication.camera.CameraManager
import com.example.tts_ocrapplication.ocr.CloudVisionOCR
import com.example.tts_ocrapplication.ocr.MLKitOCR
import com.example.tts_ocrapplication.ocr.OCRProcessor
import com.example.tts_ocrapplication.tts.LabelAnnouncer
import com.example.tts_ocrapplication.tts.TTSManager
import com.example.tts_ocrapplication.tts.VolumeHandler
import com.example.tts_ocrapplication.ui.MainScreen
import com.example.tts_ocrapplication.utils.GestureManager
import com.example.tts_ocrapplication.utils.PermissionsHelper
import com.example.ttsocrapp.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private lateinit var cameraManager: CameraManager
    private lateinit var ocrProcessor: OCRProcessor
    private lateinit var ttsManager: TTSManager
    private lateinit var permissionsHelper: PermissionsHelper
    private var vibrator: Vibrator? = null  // Declare at the class level
    private lateinit var labelAnnouncer: LabelAnnouncer  // ✅ Added LabelAnnouncer
    private lateinit var gestureManager: GestureManager

    private var lastVolumeDownPressTime = 0L
    private val doublePressInterval = 500L
    private var isExiting = false

    private val capturedText: MutableState<String> = mutableStateOf("Recognized text will appear here")
    private val useCloudVision: MutableState<Boolean> = mutableStateOf(false)
    private var cameraPermissionGranted = mutableStateOf(false) // ✅ Mutable state
    private val lastCapturedLabels: MutableState<List<String>> = mutableStateOf(emptyList())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize TTS and permission handler
        ttsManager = TTSManager(this)
        permissionsHelper = PermissionsHelper(this, ttsManager)

        // Handle back button press to trigger exit
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitApp()
            }
        })

        // Initialize GestureManager with TTSManager
        gestureManager = GestureManager(this, ttsManager)
        Log.d("MainActivity", "👆 GestureManager initialized")

        // Attach gesture detector to the root view using ViewTreeObserver
        val rootView = findViewById<View>(android.R.id.content)

        // Use ViewTreeObserver to wait until the layout is complete
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            // Now the layout is ready and has proper dimensions
            Log.d("MainActivity", "Root view width: ${rootView.width}, height: ${rootView.height}")

            // Attach GestureManager to the root view after layout is done
            gestureManager.attachToView(rootView)
            Log.d("MainActivity", "✅ GestureManager attached to root view")
        }

        // Check camera permission and initialize components accordingly
        if (permissionsHelper.checkCameraPermission()) {
            Log.d("MainActivity", "✅ Camera permission already granted, initializing components")
            cameraPermissionGranted.value = true
            initializeComponents()
        } else {
            Log.d("MainActivity", "⚠️ Requesting camera permission")
            permissionsHelper.requestCameraPermission()
        }

        // ✅ Initialize vibrator before calling any function that uses it
        initializeVibrator()
        Log.d("MainActivity", "⚡ Vibrator Instance: $vibrator")
    }


    private fun initializeVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator  // ✅ Avoids null pointer
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator  // ✅ Safe cast
        }

        if (vibrator == null) {
            Log.e("MainActivity", "❌ Vibrator initialization failed! Retrying...")

            // 🔄 Retry mechanism
            lifecycleScope.launch {
                delay(1000) // Give the system some time
                @Suppress("DEPRECATION")
                vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null) {
                    Log.d("MainActivity", "✅ Vibrator successfully reinitialized on retry.")
                } else {
                    Log.e("MainActivity", "❌ Vibrator still null after retry. Check device settings.")
                }
            }
        } else {
            Log.d("MainActivity", "✅ Vibrator initialized successfully.")
        }
    }




    //Initializes necessary components such as OCR and Camera.
    fun initializeComponents() {
        Log.d("MainActivity", "🚀 Initializing components...")

        // Initialize OCR methods (MLKit and Google Cloud Vision)
        val mlKitOCR = MLKitOCR()
        val cloudVisionOCR = CloudVisionOCR(apiKey = BuildConfig.GOOGLE_CLOUD_VISION_API_KEY)
        ocrProcessor = OCRProcessor.getInstance(mlKitOCR, cloudVisionOCR)

        // Initialize CameraManager
        cameraManager = CameraManager(this, this, ocrProcessor)
        Log.d("MainActivity", "✅ CameraManager initialized successfully!")

        // ✅ Initialize and update permission state
        cameraPermissionGranted.value = permissionsHelper.checkCameraPermission()

        labelAnnouncer = LabelAnnouncer(ttsManager)  // ✅ Initialize LabelAnnouncer

        // Determine OCR method based on internet availability
        useCloudVision.value = isInternetAvailable()
        ocrProcessor.updateOCRMethod(useCloudVision.value)
        Log.d("MainActivity", "Using OCR mode: ${if (useCloudVision.value) "Cloud Vision API" else "ML Kit"}")

        // 🔊 Ensure device volume is at least 75%
        val volumeHandler = VolumeHandler(this)
        volumeHandler.ensureMinimumVolume()

        // Provide voice instructions to the user
        lifecycleScope.launch {
            delay(1500)

            Log.d("MainActivity", "🔔 Vibrating during intro speech")
            vibrateOnEvent(500)  // Vibrate for the duration of the speech (adjust as needed)

            Log.d("MainActivity", "🔊 Speaking initial instructions")
            ttsManager.speak("App is ready. Press volume down twice to capture an image. " +
                    "Press once to repeat the last text. Press volume up to exit.")
        }

        // Load the UI
        setContent {
            Log.d("MainActivity", "🎨 Setting up UI")
            MainScreen(
                cameraManager = cameraManager,
                ttsManager = ttsManager,
                labelAnnouncer = labelAnnouncer,
                capturedText = capturedText,
                useCloudVision = useCloudVision,
                cameraPermissionGranted = cameraPermissionGranted
            )
        }
    }

    //Handles volume button presses for capturing and repeating text.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                Log.d("MainActivity", "Volume up button pressed: Preparing to exit...")
                return if (!permissionsHelper.checkCameraPermission()) {
                    Log.d("MainActivity", "🚨 Camera permission missing, guiding user")
                    ttsManager.speak("Press the Allow button on the screen to grant permission.")
                    true
                } else {
                    exitApp()
                    true
                }
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastVolumeDownPressTime <= doublePressInterval) {
                    Log.d("MainActivity", "📸 Double press detected: Capturing image")
                    captureImage()
                } else {
                    Log.d("MainActivity", "🔁 Single press detected: Repeating last text")
                    repeatLastText()
                }
                lastVolumeDownPressTime = currentTime
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            Log.d("MainActivity", "Volume up button released: Exiting...")
            exitApp()  // Exit only when the button is fully released
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    //Captures an image and processes text using OCR.
    // Captures an image and processes text using OCR and Label Detection.
    private fun captureImage() {
        if (!permissionsHelper.cameraPermissionGranted) {
            Log.d("MainActivity", "🚨 Camera permission missing, cannot capture image")
            ttsManager.speak("Camera permission is missing. Please enable it in settings.")
            return
        }

        // Update OCR method based on internet connectivity
        val newUseCloudVision = isInternetAvailable()
        if (newUseCloudVision != useCloudVision.value) {
            useCloudVision.value = newUseCloudVision
            ocrProcessor.updateOCRMethod(newUseCloudVision)
            val method = if (newUseCloudVision) "Google Cloud Vision" else "ML Kit"
            Log.d("MainActivity", "Switched to: $method OCR")
            ttsManager.speak("Switched to $method OCR")
        }

        // ✅ Clear text BEFORE taking a new photo to show placeholder
        capturedText.value = ""

        // Capture image and process text + labels
        ttsManager.speak("Capturing text...") {
            cameraManager.takePhoto { text, labels ->
                capturedText.value = text.ifEmpty { "No text recognized." }
                lastCapturedLabels.value = labels

                // ✅ NEW: Only announce labels if Vision API is being used and labels are not empty
                if (useCloudVision.value && labels.isNotEmpty()) {
                    labelAnnouncer.announce(text, labels)
                } else {
                    // ✅ Otherwise just speak detected text
                    labelAnnouncer.stop() // just in case
                    ttsManager.speak("Detected text: $text")
                }
            }
        }
    }




    //Repeats the last captured text
    private fun repeatLastText() {
        lifecycleScope.launch {
            delay(50)
            if (capturedText.value.isNotEmpty() && capturedText.value != "Recognized text will appear here") {
                Log.d("MainActivity", "🔊 Repeating text: ${capturedText.value}")
                ttsManager.speak("Repeating text...") {
                    ttsManager.speak(capturedText.value)
                }
            } else {
                Log.d("MainActivity", "⚠️ No text available to repeat.")
                ttsManager.speak("No text available to repeat.")
            }
        }
    }

    //Handles app exit functionality.
    private fun exitApp() {
        if (isExiting) return

        isExiting = true
        Log.d("MainActivity", "🚪 Exiting app...")
        // 🔔 Vibrate when the app is closing (200ms)
        vibrateOnEvent(350)
        ttsManager.speak("Exiting...") {
            lifecycleScope.launch {
                delay(300)
                finishAffinity()
//                exitProcess(0)   // Forces app termination
            }
        }
    }

    //Handles activity pause events.
    //Ensures the app exits properly if not changing configurations or missing permissions.
    override fun onPause() {
        super.onPause()

        // Prevent exit if configuration is changing or camera permission is missing
        if (isChangingConfigurations || !permissionsHelper.cameraPermissionGranted) return

        if (isExiting) return // Avoid duplicate exit attempts

        isExiting = true
        Log.d("MainActivity", "🔄 Exiting due to pause")
        // 🔔 Vibrate while exiting due to pause (300ms)
        vibrateOnEvent(350)
        ttsManager.speak("Exiting...") {
            lifecycleScope.launch {
                delay(300)
                finishAffinity()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "🛑 Destroying activity, shutting down TTS")
        ttsManager.shutdown()
    }

    //Checks for active internet connectivity.
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.activeNetwork ?: return false
        } else {
            TODO("VERSION.SDK_INT < M")
        }
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


    private fun vibrateOnEvent(duration: Long) {
        if (vibrator == null) {
            Log.e("MainActivity", "❌ Vibrator is null, skipping vibration!")
            return
        }

        if (!vibrator!!.hasVibrator()) {
            Log.d("MainActivity", "⚠️ Device does not support vibration.")
            return
        }

        Log.d("MainActivity", "🔔 Vibrating for $duration ms")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator!!.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator!!.vibrate(duration)
        }
    }
}