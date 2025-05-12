package com.example.tts_ocrapplication.tts

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LabelAnnouncer(private val ttsManager: TTSManager) {
    private val scope = CoroutineScope(Dispatchers.Main)

    // 🔹 Natural speech templates for varied label announcements
    private val templates = listOf(
        "This might be %s.",
        "I see something that looks like %s.",
        "It appears to be %s.",
        "It seems like %s."
    )

    fun announce(text: String, labels: List<String>?) {
        scope.launch {
            Log.d("LabelAnnouncer", "🔹 Announce function triggered.")

            // 🔹 Prepare label text (if labels exist)
            // 🔹 Prepare label announcement with natural speech
            val labelText = labels?.take(2)?.joinToString(" or ")?.let {
                templates.random().format(it)  // 🎯 Pick a random speech template
            }

            Log.d("LabelAnnouncer", "📌 Labels to announce: $labelText")

            // 🔹 Prepare detected text announcement
            val detectedText = if (text.isNotEmpty()) "Detected text: $text." else null
            Log.d("LabelAnnouncer", "📝 Detected text...")

            // ✅ Speak labels first (if available), then detected text
            if (labelText != null) {
                Log.d("LabelAnnouncer", "🗣 Speaking labels first...")
                ttsManager.speak(labelText) {
                    Log.d("LabelAnnouncer", "✅ Finished speaking labels.")
                    // 🛑 Ensure detected text is spoken only after labels
                    detectedText?.let {
                        Log.d("LabelAnnouncer", "🗣 Now speaking detected text...")
                        ttsManager.speak(it)
                    }
                }
            } else {
                // 🎯 If no labels, announce detected text immediately
                detectedText?.let {
                    Log.d("LabelAnnouncer", "🗣 No labels detected. Speaking text directly...")
                    ttsManager.speak(it)
                }
            }
        }
    }

    fun stop() {
        Log.d("LabelAnnouncer", "🛑 Stopping LabelAnnouncer, canceling coroutines.")
        scope.cancel()  // ✅ Proper coroutine cleanup
    }
}
