package com.example.tts_ocrapplication.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TTSManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null
    private var onComplete: (() -> Unit)? = null  // Store completion callback
    private var isSpeaking = false // ✅ Track speech status

    init {
        val googleTtsPackage = "com.google.android.tts"
        val engine = if (isGoogleTtsAvailable(context, googleTtsPackage)) googleTtsPackage else null
        tts = TextToSpeech(context, this, engine)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS && tts != null) {
            tts?.language = Locale("en", "IN")

            // ✅ Prioritize a female voice
            val femaleVoice = tts?.voices?.firstOrNull {
                it.name.contains("en-in", ignoreCase = true) && it.name.contains("female", ignoreCase = true)
            }

            if (femaleVoice != null) {
                tts?.voice = femaleVoice
                Log.d("TTSManager", "✅ Selected Female Voice: ${femaleVoice.name}")
            } else {
                val defaultVoice = tts?.defaultVoice
                if (defaultVoice != null) {
                    tts?.voice = defaultVoice
                    Log.d("TTSManager", "⚠️ Preferred female voice not found. Using default voice: ${defaultVoice.name}")
                } else {
                    Log.d("TTSManager", "⚠️ No suitable voice found. Using system default.")
                }
            }

            // ✅ Ensure proper settings
            tts?.setSpeechRate(1.0f)  // Normal Speed
            tts?.setPitch(1.0f)       // Neutral Pitch

            isInitialized = true

            // ✅ Set up the Utterance Progress Listener only once
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    if (utteranceId == "TTS_ID") {
                        onComplete?.invoke()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    Log.e("TTSManager", "⚠️ TTS Error occurred for Utterance ID: $utteranceId")
                    onComplete?.invoke()
                }
            })

            // If there was pending text, speak it now
            pendingText?.let {
                speak(it, onComplete)
                pendingText = null
            }
        } else {
            Log.e("TTSManager", "❌ Failed to initialize TextToSpeech")
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (isInitialized) {
            this.onComplete = onComplete  // Store callback
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "TTS_ID")
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "TTS_ID")
        } else {
            pendingText = text
        }
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch)
    }

    fun isSpeaking(): Boolean = isSpeaking // ✅ External check

    fun stopSpeaking() {
        tts?.stop()
        isSpeaking = false
    }

    fun shutdown() {
        Log.d("TTSManager", "🛑 Shutting down TTS service")
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        isSpeaking = false
    }

    private fun isGoogleTtsAvailable(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
