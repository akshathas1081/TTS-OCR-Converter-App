package com.example.tts_ocrapplication.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.tts_ocrapplication.ocr.OCRProcessor
import kotlinx.coroutines.*

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Suppress("DEPRECATION")
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val ocrProcessor: OCRProcessor
) {
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private var captureStartTime: Long = 0L
    private var processingStartTime: Long = 0L

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())  // ✅ Managed coroutine scope

    fun startCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        Log.d("CameraManager", "📸 Initializing CameraX...")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                Log.d("CameraManager", "✅ CameraProvider obtained successfully.")

                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                Log.d("CameraManager", "📷 Preview use case configured.")

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setTargetRotation(previewView.display.rotation)
                    .build()
                Log.d("CameraManager", "📸 ImageCapture use case configured.")

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                Log.d("CameraManager", "🎯 Camera Selector: Back Camera")

                cameraProvider.unbindAll()
                Log.d("CameraManager", "🔄 Unbound all existing camera use cases.")

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                Log.d("CameraManager", "✅ Camera bound to lifecycle successfully.")

            } catch (exc: Exception) {
                Log.e("CameraManager", "❌ Use case binding failed: ${exc.message}", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto(callback: (String, List<String>) -> Unit) {
        val imageCapture = imageCapture ?: return

        captureStartTime = System.currentTimeMillis()
        Log.d("CameraManager", "⏳ Capture started at $captureStartTime ms (${captureStartTime / 1000.0} s)")

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val captureEndTime = System.currentTimeMillis()
                    Log.d("CameraManager", "✅ Photo captured in ${captureEndTime - captureStartTime} ms")

                    val bitmap = imageProxyToBitmap(image)
                    image.close() // Close the image proxy after conversion

                    Log.d("CameraManager", "🔍 Processing image...")
                    processImage(bitmap, callback) // Process the image for OCR
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraManager", "❌ Photo capture failed: ${exception.message}", exception)

                    callback("Capture failed. Please try again.", emptyList()) // Return a meaningful message
                }
            }
        )
    }


    private fun processImage(bitmap: Bitmap, callback: (String, List<String>) -> Unit) {
        processingStartTime = System.currentTimeMillis()
        Log.d("CameraManager", "⏳ OCR processing started at $processingStartTime ms (${processingStartTime / 1000.0} s)")

        coroutineScope.launch(Dispatchers.IO) {  // ✅ Ensure coroutine is running
            ocrProcessor.processImage(bitmap) { text, labels ->
                val processingEndTime = System.currentTimeMillis()
                val totalTimeMs = processingEndTime - captureStartTime
                val ocrProcessingTimeMs = processingEndTime - processingStartTime

                val totalTimeSec = totalTimeMs / 1000.0
                val ocrProcessingTimeSec = ocrProcessingTimeMs / 1000.0

                Log.d("CameraManager", "✅ ⏳ Total time (Capture → Recognition): $totalTimeMs ms (${totalTimeSec}s)")
                Log.d("CameraManager", "✅ ⏳ OCR Processing time: $ocrProcessingTimeMs ms (${ocrProcessingTimeSec}s)")
                Log.d("CameraManager", "📜 Detected Labels: $labels")

                // ✅ Now properly launching in a coroutine
                callback(text, labels)

            }
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    fun shutdown() {
        cameraExecutor.shutdown()
        coroutineScope.cancel()  // ✅ Cancel coroutines to avoid memory leaks
    }
}
