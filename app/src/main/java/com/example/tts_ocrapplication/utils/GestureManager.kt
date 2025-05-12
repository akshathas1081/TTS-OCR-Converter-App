package com.example.tts_ocrapplication.utils

import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.example.tts_ocrapplication.tts.TTSManager

class GestureManager(context: Context, private val ttsManager: TTSManager) {

    private val gestureDetector: GestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Log touch event details to check if it's properly detecting double-tap on different areas
            Log.d("GestureManager", "👆 Double-tap detected at: x=${e.x}, y=${e.y}")
            // Check if TTS is currently speaking
            if (ttsManager.isSpeaking()) {
                // Stop speaking if TTS is currently speaking
                ttsManager.stopSpeaking()
                Log.d("GestureManager", "🛑 TTS stopped on double-tap")
            } else {
                // Log that TTS is not speaking when double-tap happens
                Log.d("GestureManager", "❌ Double-tap detected, but TTS is not speaking")
            }
            return true
        }
    })

    // Method to attach gesture detection to any view
    fun attachToView(view: View) {
        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            view.performClick() // To ensure that click actions are also properly handled
            true
        }
    }
}
