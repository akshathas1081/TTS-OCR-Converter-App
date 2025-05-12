package com.example.tts_ocrapplication.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class MLKitOCR {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processImage(bitmap: Bitmap): String {
        logImageInfo(bitmap) // 🧩 Log image size + resolution

        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(image).await()
            result.text.ifEmpty { "No text recognized." }
        } catch (e: Exception) {
            e.printStackTrace()
            "OCR failed. Try again."
        }
    }

    private fun logImageInfo(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate size
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        val byteArray = stream.toByteArray()
        val sizeKB = byteArray.size / 1024.0
        val sizeMB = sizeKB / 1024.0

        Log.d("MLKitOCR", "MLKit Image Resolution = Width: $width px, Height: $height px")
        Log.d("MLKitOCR", "MLKit Image Size: %.2f MB (%.2f KB)".format(sizeMB, sizeKB))
    }
}
