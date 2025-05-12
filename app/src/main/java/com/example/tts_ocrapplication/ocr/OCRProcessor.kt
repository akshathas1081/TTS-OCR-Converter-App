package com.example.tts_ocrapplication.ocr

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OCRProcessor private constructor(
    private val mlKitOCR: MLKitOCR,
    private val cloudVisionOCR: CloudVisionOCR
) {
    var useCloudVision: Boolean = false
        private set // Prevent direct modification from outside

    companion object {
        @Volatile
        private var INSTANCE: OCRProcessor? = null

        fun getInstance(mlKitOCR: MLKitOCR, cloudVisionOCR: CloudVisionOCR): OCRProcessor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OCRProcessor(mlKitOCR, cloudVisionOCR).also { INSTANCE = it }
            }
        }
    }

    fun updateOCRMethod(useCloud: Boolean) {
        useCloudVision = useCloud
        Log.d("OCRProcessor", "OCR Mode updated: useCloudVision = $useCloudVision")
    }

    fun processImage(bitmap: Bitmap, callback: (String, List<String>) -> Unit) {
        Log.d("OCRProcessor", "Processing image... useCloudVision = $useCloudVision")

        CoroutineScope(Dispatchers.IO).launch {
            val (extractedText, labelPairs) = try {
                if (useCloudVision) {
                    Log.d("OCRProcessor", "Using Google Cloud Vision API")
                    cloudVisionOCR.processImage(bitmap)  // ✅ Returns (text, labels)
                } else {
                    Log.d("OCRProcessor", "Using ML Kit OCR")
                    mlKitOCR.processImage(bitmap) to  emptyList()  // ✅ FIXED: Wrap in Pair
                }
            } catch (e: Exception) {
                Log.e("OCRProcessor", "OCR Processing Failed: ${e.localizedMessage}", e)
                "OCR failed." to emptyList()
            }

            // ✅ Extract only label names from labelPairs
            val labelNames = labelPairs.map { it.first }  // Convert (label, score) → label name only

            withContext(Dispatchers.Main) {
                Log.d("OCRProcessor", "Final OCR Output: $extractedText, Labels: $labelNames")
                callback(extractedText, labelNames)  // ✅ Now correctly returns List<String> for labels
            }
        }
    }
}
