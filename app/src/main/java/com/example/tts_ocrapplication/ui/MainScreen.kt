package com.example.tts_ocrapplication.ui

import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.tts_ocrapplication.camera.CameraManager
import com.example.tts_ocrapplication.tts.LabelAnnouncer
import com.example.tts_ocrapplication.tts.TTSManager
import kotlinx.coroutines.launch

private const val PLACEHOLDER_TEXT = "Recognized text will appear here"

@Composable
fun MainScreen(
    cameraManager: CameraManager?,
    ttsManager: TTSManager,
    labelAnnouncer: LabelAnnouncer,  // ✅ Used correctly now
    capturedText: MutableState<String>,  // ✅ Used correctly now
    useCloudVision: MutableState<Boolean>,
    cameraPermissionGranted: MutableState<Boolean>
) {
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    if (cameraPermissionGranted.value) {
        Log.d("MainScreen", "✅ Camera permission granted. Ready to capture images.")
    } else {
        Log.d("MainScreen", "⚠️ Camera permission is not granted. Please enable it in settings.")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
    ) {
        if (cameraPermissionGranted.value) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.8f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        Log.d("MainScreen", "✅ Initializing Camera PreviewView...")
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { previewView ->
                        Log.d("MainScreen", "✅ Binding camera to preview...")
                        cameraManager?.startCamera(previewView, lifecycleOwner)
                    }
                )
            }
        }

        // ✅ Display Recognized Text
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.4f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = capturedText.value.ifEmpty { PLACEHOLDER_TEXT },
                    fontSize = if (capturedText.value.isEmpty()) 17.sp else 17.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = if (capturedText.value.isEmpty()) Color.DarkGray else Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Text(
            text = "OCR Mode: ${if (useCloudVision.value) "Google Cloud Vision" else "ML Kit"}",
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // 🔘 Capture and Repeat Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CustomButton(
                text = "Capture",
                onClick = {
                    Log.d("MainScreen", "📸 Capture button pressed.")
                    coroutineScope.launch {
                        ttsManager.speak("Capturing text...") {
                            // ✅ Clear previous text before new capture
                            capturedText.value = ""

                            cameraManager?.takePhoto { recognizedText, labels ->
                                Log.d("MainScreen", "📸 Captured Text: '$recognizedText'")
                                Log.d("MainScreen", "🏷️ Detected Labels: ${labels.joinToString()}")

                                capturedText.value = recognizedText
                                labelAnnouncer.announce(recognizedText, labels)  // ✅ Now using labelAnnouncer properly
                            }
                        }
                    }
                },
                modifier = Modifier.weight(2.0f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            CustomButton(
                text = "Repeat",
                onClick = {
                    Log.d("MainScreen", "🔁 Repeat button pressed.")
                    coroutineScope.launch {
                        if (capturedText.value.isNotEmpty() && capturedText.value != PLACEHOLDER_TEXT) {
                            ttsManager.speak("Repeating text...") {
                                ttsManager.speak(capturedText.value)
                            }
                        } else {
                            Log.d("MainScreen", "⚠️ No text available to repeat.")
                            ttsManager.speak("No text available to repeat.")
                        }
                    }
                },
                modifier = Modifier.weight(2.0f)
            )
        }
    }
}

// 🔹 Reusable Custom Button Component
@Composable
fun CustomButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        elevation = ButtonDefaults.buttonElevation(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF3B82F6), Color(0xFF60A5FA))
                    ),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, fontSize = 19.sp, color = Color.White, fontFamily = FontFamily.SansSerif)
        }
    }
}
