package com.example.tts_ocrapplication.ocr

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class CloudVisionOCR(private val apiKey: String) {

    private val client = OkHttpClient()
    private val useCloudVision: MutableState<Boolean> = mutableStateOf(false)

    suspend fun processImage(bitmap: Bitmap): Pair<String, List<Pair<String, Float>>> = withContext(Dispatchers.IO) {
        try {
            // Resize image to 2560x1920
            val resizedBitmap = resizeBitmap(bitmap, 2560, 1920)

            // Log resized image resolution
            Log.d("CloudVisionOCR", "🖼️ Resized Image Resolution = Width: ${resizedBitmap.width} px, Height: ${resizedBitmap.height} px")

            // Convert resized bitmap to base64
            val base64Image = convertBitmapToBase64(resizedBitmap)
            Log.d("CloudVisionOCR", "📸 Encoded Image (First 30 chars): ${base64Image.take(30)}...")

            val jsonRequest = buildJsonRequest(base64Image)
            Log.d("CloudVisionOCR", "📤 JSON Request: $jsonRequest")

            val request = Request.Builder()
                .url("https://vision.googleapis.com/v1/images:annotate?key=$apiKey")
                .post(jsonRequest.toRequestBody("application/json".toMediaType()))
                .build()

            Log.d("CloudVisionOCR", "⏳ Sending request to Vision API...")

            // 🚀 Start API Timer
            val apiStartTime = System.currentTimeMillis()

            val response = client.newCall(request).execute()

            // ✅ API Response Received
            val apiEndTime = System.currentTimeMillis()
            val apiResponseTimeMs = apiEndTime - apiStartTime
            val apiResponseTimeSec = apiResponseTimeMs / 1000.0

            Log.d("CloudVisionOCR", "✅ ⏳ Vision API Response Time: $apiResponseTimeMs ms (${apiResponseTimeSec}s)")

            val responseBody = response.body?.string() ?: "{}"

            Log.d("CloudVisionOCR", "🟢 API Response Status: ${response.code}")
            Log.d("CloudVisionOCR", "📝 API Response Body: $responseBody")

            // 👮 Check for Invalid API Key
            if (response.code == 400 && responseBody.contains("API key not valid", ignoreCase = true)) {
                Log.w("CloudVisionOCR", "🚫 Invalid API Key detected. Falling back to ML Kit OCR.")
                useCloudVision.value = false
                val mlKitOCR = MLKitOCR()
                val mlKitText = mlKitOCR.processImage(bitmap)
                return@withContext Pair(mlKitText, emptyList())
            }

            // ✅ Parse response normally
            return@withContext parseResponse(responseBody)

        } catch (e: Exception) {
            Log.e("CloudVisionOCR", "❌ Cloud OCR Failed: ${e.localizedMessage}", e)
            // 🧠 FALLBACK TO ML KIT OCR
            Log.d("CloudVisionOCR", "🔁 Falling back to ML Kit OCR (offline)...")
            // 🔄 Update UI state
            useCloudVision.value = false  // <-- Update this if you're passing state into the OCR function
            val mlKitOCR = MLKitOCR()
            val mlKitText = mlKitOCR.processImage(bitmap)
            return@withContext Pair(mlKitText, emptyList()) // You can still parse elements if needed
        }
    }

    // Resize bitmap to target resolution
    private fun resizeBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        return if (originalWidth <= targetWidth && originalHeight <= targetHeight) {
            Log.d("CloudVisionOCR", "⚠️ Skipping resize: Original image is smaller or equal to target.")
            bitmap  // Return original if already small
        } else {
            Log.d("CloudVisionOCR", "🔄 Resizing image from ${originalWidth}x$originalHeight to ${targetWidth}x$targetHeight")
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        }
    }

    // Convert bitmap to Base64 and log size
    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)

        val byteArray = stream.toByteArray()
        val imageSizeKB = byteArray.size / 1024.0
        val imageSizeMB = imageSizeKB / 1024.0

        Log.d("CloudVisionOCR", "Vision API Image Size: %.2f MB (%.2f KB)".format(imageSizeMB, imageSizeKB))

        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    // Build JSON request for Vision API
    private fun buildJsonRequest(base64Image: String): String {
        return """
            {
              "requests": [
                {
                  "image": {
                    "content": "$base64Image"
                  },
                  "features": [
                    {"type": "TEXT_DETECTION"},
                    {"type": "LABEL_DETECTION"}
                  ]
                }
              ]
            }
        """.trimIndent()
    }

    // Parse response from Vision API
    private fun parseResponse(response: String): Pair<String, List<Pair<String, Float>>> {
        val jsonObject = JSONObject(response)
        val responses = jsonObject.optJSONArray("responses") ?: JSONArray()

        if (responses.length() == 0) {
            Log.w("CloudVisionOCR", "No valid response from API.")
            return Pair("No text recognized.", emptyList())
        }

        val responseObj = responses.getJSONObject(0)

        val extractedText = responseObj.optJSONArray("textAnnotations")
            ?.optJSONObject(0)
            ?.optString("description")
            ?: "No text recognized."

        val labels = mutableListOf<Pair<String, Float>>()
        responseObj.optJSONArray("labelAnnotations")?.let { labelArray ->
            for (i in 0 until labelArray.length()) {
                val label = labelArray.getJSONObject(i)
                val description = label.optString("description")
                val score = label.optDouble("score").toFloat()
                labels.add(description to score)
            }
        }

        val topLabels = labels.sortedByDescending { it.second }.take(2)

        Log.d("CloudVisionOCR", "Extracted Text: $extractedText")
        Log.d("CloudVisionOCR", "Extracted Top Labels: $topLabels")

        return Pair(extractedText, topLabels)
    }
}
