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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
                    RSVPApp(scaffoldPadding = innerPadding)
                }
            }
        }
    }
}

@Suppress("UNUSED_VALUE")
@Composable
fun RSVPApp(scaffoldPadding: androidx.compose.foundation.layout.PaddingValues) {
    val modifier = Modifier // Keep consistent if needed elsewhere, but mostly using scaffoldPadding now
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State backed by PersistenceManager
    var settings by remember { mutableStateOf(PersistenceManager.loadSettings(context)) }
    var libraryBooks by remember { mutableStateOf(PersistenceManager.loadLibrary(context)) }
    
    // Runtime State
    var tokens by remember { mutableStateOf(emptyList<RSVPToken>()) }
    var isReading by remember { mutableStateOf(false) }
    var isParsing by remember { mutableStateOf(false) }
    var showVoiceManager by remember { mutableStateOf(false) }
    var showStatistics by remember { mutableStateOf(false) }
    
    // Current book tracking for resume
    var currentBookUri by remember { mutableStateOf<String?>(null) }
    
    // Global TTS instance holder
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

    // Auto-save effects
    LaunchedEffect(settings) {
        PersistenceManager.saveSettings(context, settings)
    }
    
    LaunchedEffect(libraryBooks) {
        PersistenceManager.saveLibrary(context, libraryBooks)
    }
    
    // Initialize Neural TTS if enabled and voice selected
    LaunchedEffect(settings.useNeuralTts, settings.voiceName) {
        if (settings.useNeuralTts && settings.voiceName.isNotEmpty()) {
            NeuralTTSManager.init(context, settings.voiceName)
        }
    }

    if (showVoiceManager) {
        VoiceManagerScreen(
            currentSettings = settings,
            onSettingsChanged = { settings = it },
            onBack = { showVoiceManager = false }
        )
    } else if (showStatistics) {
        var sessions by remember { mutableStateOf(emptyList<ReadingSession>()) }
        LaunchedEffect(Unit) {
            sessions = PersistenceManager.loadSessions(context)
        }
        StatisticsScreen(
            sessions = sessions,
            onBack = { showStatistics = false }
        )
    } else if (isParsing) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Processing text...")
            }
        }
    } else if (isReading) {
        val initialIndex = if (currentBookUri != null) {
            libraryBooks.find { it.uri == currentBookUri }?.progressIndex ?: 0
        } else 0
        
        // Track start time for statistics
        val startTime = remember { System.currentTimeMillis() }

        ReaderScreen(
            tokens = tokens,
            initialIndex = initialIndex,
            settings = settings,
            onSettingsChanged = { newSettings -> settings = newSettings },
            onBack = { progressIndex ->
                // Update book progress on exit
                if (currentBookUri != null) {
                    val updatedList = libraryBooks.map { book ->
                        if (book.uri == currentBookUri) {
                            book.copy(progressIndex = progressIndex)
                        } else book
                    }
                    libraryBooks = updatedList
                }

                // Record Session Statistics
                val duration = (System.currentTimeMillis() - startTime) / 1000
                val wordsRead = progressIndex - initialIndex
                if (duration > 5 && wordsRead > 10) { // Only save meaningful sessions
                    val session = ReadingSession(
                        durationSeconds = duration.toInt(),
                        wordsRead = wordsRead,
                        averageWpm = settings.wpm.toInt(),
                        source = if (currentBookUri != null) "Book" else "Quick Read" // Could be refined
                    )
                    PersistenceManager.saveSession(context, session)
                }

                isReading = false 
                currentBookUri = null
            },
            tts = tts,
            isTtsReady = isTtsReady,
            modifier = Modifier.padding(scaffoldPadding),
            onManageVoices = {
                showVoiceManager = true
            }
        )
    } else {
        InputSelectionScreen(
            onStartReading = { text, bookUri, isEpub, title ->
                scope.launch {
                    isParsing = true
                    
                    // Handle Persistence for Library
                    if (bookUri != null && title != null) {
                        currentBookUri = bookUri
                        val existingBook = libraryBooks.find { it.uri == bookUri }
                        if (existingBook == null) {
                            val newBook = Book(uri = bookUri, title = title, isEpub = isEpub)
                            libraryBooks = libraryBooks + newBook
                        }
                    } else {
                        currentBookUri = null // Transient reading (Paste/Web)
                    }

                    // Always parse into single tokens. Chunking is handled dynamically in ReaderScreen.
                    tokens = parseMarkdownToTokens(text, 1)
                    
                    // Update total tokens for the book if newly added or updated
                    if (currentBookUri != null) {
                         val updatedList = libraryBooks.map { book ->
                            if (book.uri == currentBookUri) {
                                book.copy(totalTokens = tokens.size)
                            } else book
                        }
                        libraryBooks = updatedList
                    }

                    isParsing = false
                    isReading = true
                }
            },
            settings = settings,
            onSettingsChanged = { newSettings -> settings = newSettings },
            tts = tts,
            isTtsReady = isTtsReady,
            libraryBooks = libraryBooks,
            context = context,
            modifier = Modifier,
            onManageVoices = { showVoiceManager = true },
            onShowStatistics = { showStatistics = true },
            scaffoldPadding = scaffoldPadding
        )
    }
}