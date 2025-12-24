package com.example.fossrsvp

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.example.fossrsvp.ui.theme.FOSSRSVPTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            FOSSRSVPTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RSVPApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Suppress("UNUSED_VALUE")
@Composable
fun RSVPApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("FossRsvpPrefs", Context.MODE_PRIVATE) }
    
    var tokens by remember { mutableStateOf(emptyList<RSVPToken>()) }
    var isReading by remember { mutableStateOf(false) }
    var settings by remember { 
        mutableStateOf(AppSettings(
            geminiApiKey = sharedPreferences.getString("GEMINI_API_KEY", "") ?: ""
        )) 
    }
    var isParsing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Global TTS instance holder so we can access voices in Settings
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.language = Locale.getDefault()
            }
        }
        onDispose {
            tts?.shutdown()
        }
    }

    if (isParsing) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Processing text...")
            }
        }
    } else if (isReading) {
        ReaderScreen(
            tokens = tokens,
            settings = settings,
            onSettingsChanged = { newSettings ->
                settings = newSettings 
                sharedPreferences.edit { putString("GEMINI_API_KEY", newSettings.geminiApiKey) }
            },
            onBack = { 
                isReading = false 
            },
            tts = tts,
            isTtsReady = isTtsReady,
            modifier = modifier
        )
    } else {
        InputSelectionScreen(
            onStartReading = { text ->
                if (text.isNotBlank()) {
                    scope.launch {
                        isParsing = true
                        tokens = parseMarkdownToTokens(text, settings.chunkSize)
                        isParsing = false
                        isReading = true
                    }
                }
            },
            settings = settings,
            onSettingsChanged = { newSettings ->
                settings = newSettings 
                sharedPreferences.edit { putString("GEMINI_API_KEY", newSettings.geminiApiKey) }
            },
            tts = tts,
            isTtsReady = isTtsReady,
            modifier = modifier
        )
    }
}
