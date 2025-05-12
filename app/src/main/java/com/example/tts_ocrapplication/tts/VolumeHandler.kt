package com.example.tts_ocrapplication.tts

import android.content.Context
import android.media.AudioManager
import android.util.Log

class VolumeHandler(private val context: Context) {

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun ensureMinimumVolume() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val minRequiredVolume = (maxVolume * 0.90).toInt() // 90% of max volume

        if (currentVolume < minRequiredVolume) {
            Log.d("VolumeHandler", "🔊 Increasing volume to 90%")
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, minRequiredVolume, AudioManager.FLAG_SHOW_UI)
        } else {
            Log.d("VolumeHandler", "✅ Volume is already sufficient")
        }
    }
}
