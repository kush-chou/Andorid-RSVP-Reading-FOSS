package com.example.fossrsvp

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.min

@Suppress("UNUSED_VALUE")
@Composable
fun InputSelectionScreen(
    onStartReading: (String, String?, Boolean, String?) -> Unit,
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    tts: TextToSpeech?,
    isTtsReady: Boolean,
    libraryBooks: List<Book>,
    context: Context,
    modifier: Modifier = Modifier,
    onManageVoices: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val tabs = listOf("Paste", "Books", "Web", "Ask AI", "Stats")

    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = settings,
            onSettingsChanged = onSettingsChanged,
            onDismiss = { showSettingsDialog = false },
            tts = tts,
            isTtsReady = isTtsReady,
            onManageVoices = onManageVoices
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(28.dp))
            }

            Text(
                text = "FOSS RSVP",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Crossfade(targetState = selectedTab, label = "InputTabChange") { tabIndex ->
                        when(tabIndex) {
                            0 -> PasteInput(onStartReading)
                            1 -> LibraryInput(onStartReading, libraryBooks, context) { updatedBooks ->
                                // Callback safely ignored as parent updates automatically
                                PersistenceManager.saveLibrary(context, updatedBooks)
                            }
                            2 -> WebInput(onStartReading, settings)
                            3 -> GeminiChatInput(onStartReading, settings, context)
                            4 -> StatisticsScreen(context)
                        }
                    }
                }
                
                NavigationBar(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    windowInsets = WindowInsets(0.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                when(index) {
                                    0 -> Icon(Icons.Default.ContentPaste, contentDescription = null)
                                    1 -> Icon(Icons.Default.Book, contentDescription = null)
                                    2 -> Icon(Icons.Default.Language, contentDescription = null)
                                    3 -> Icon(Icons.Default.SmartToy, contentDescription = null)
                                    4 -> Icon(Icons.Default.ShowChart, contentDescription = null)
                                }
                            },
                            label = { Text(title) }
                        )
                    }
                }
            }
        }
    }
}

// ... PasteInput remains mostly same ...
@Composable
fun PasteInput(onStartReading: (String, String?, Boolean, String?) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Paste Text", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("Paste text to read immediately (not saved to library).", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().weight(1f),
            placeholder = { Text("Paste content here...") },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            shape = MaterialTheme.shapes.medium
        )
        Button(
            onClick = { onStartReading(text, null, false, null) },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Start Reading", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun LibraryInput(
    onStartReading: (String, String?, Boolean, String?) -> Unit,
    books: List<Book>,
    context: Context,
    onUpdateLibrary: (List<Book>) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)

                // Get Filename
                var title = "Unknown Document"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        title = cursor.getString(nameIndex)
                    }
                }

                val isEpub = title.endsWith(".epub", ignoreCase = true)
                val content = if (isEpub) extractTextFromEpub(context, uri) else extractTextFromPdf(context, uri)
                
                isLoading = false
                onStartReading(content, uri.toString(), isEpub, title)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
             modifier = Modifier.fillMaxWidth(),
             horizontalArrangement = Arrangement.SpaceBetween,
             verticalAlignment = Alignment.CenterVertically
        ) {
            Text("My Library", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Button(onClick = { launcher.launch(arrayOf("application/pdf", "application/epub+zip")) }) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (books.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No books imported yet.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(books.sortedByDescending { it.addedAt }) { book ->
                    NavigableBookItem(book, onOpen = {
                         scope.launch {
                             isLoading = true
                             val uri = Uri.parse(book.uri)
                             val content = if (book.isEpub) extractTextFromEpub(context, uri) else extractTextFromPdf(context, uri)
                             isLoading = false
                             onStartReading(content, book.uri, book.isEpub, book.title)
                         }
                    }, onDelete = {
                        onUpdateLibrary(books.filter { it.uri != book.uri })
                    })
                }
            }
        }
    }
}

@Composable
fun NavigableBookItem(book: Book, onOpen: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
             Column(modifier = Modifier.weight(1f)) {
                 Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                 val progress = if (book.totalTokens > 0) book.progressIndex.toFloat() / book.totalTokens else 0f
                 Spacer(modifier = Modifier.height(8.dp))
                 LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(4.dp))
                 Spacer(modifier = Modifier.height(4.dp))
                 Text("${(progress * 100).toInt()}% Completed", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
             }
             IconButton(onClick = onDelete) {
                 Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
             }
        }
    }
}

@Composable
fun WebInput(onStartReading: (String, String?, Boolean, String?) -> Unit, settings: AppSettings) {
    var url by remember { mutableStateOf("https://") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Read from Web", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Article URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        
        Text(
            "Smart Text Extraction (No AI required)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
             Spacer(modifier = Modifier.height(32.dp))
        }
        
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    val content = extractTextFromUrl(url)
                    isLoading = false
                    onStartReading(content, null, false, null)
                }
            },
            enabled = !isLoading && url.length > 8,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Fetch & Read", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Suppress("UNUSED_VALUE")
@Composable
fun GeminiChatInput(
    onStartReading: (String, String?, Boolean, String?) -> Unit,
    settings: AppSettings,
    context: Context
) {
    var prompt by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    // Load local history state
    var history by remember { mutableStateOf(PersistenceManager.loadChatHistory(context)) }
    
    val scope = rememberCoroutineScope()
    val listState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("AI Chat", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)

        if (settings.geminiApiKey.isBlank()) {
            ElevatedCard(
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Gemini API Key missing.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Chat History List
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp)) {
             Column(modifier = Modifier.verticalScroll(listState)) {
                 history.forEach { msg ->
                     val isUser = msg.role == "user"
                     Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
                         Surface(
                             shape = MaterialTheme.shapes.medium,
                             color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                             modifier = Modifier.padding(4.dp).fillMaxWidth(0.8f)
                         ) {
                             Column(modifier = Modifier.padding(12.dp)) {
                                 Text(msg.content, style = MaterialTheme.typography.bodyMedium)
                                 if (!isUser) {
                                     Spacer(modifier = Modifier.height(8.dp))
                                     Button(
                                         onClick = { onStartReading(msg.content, null, false, null) },
                                         modifier = Modifier.height(32.dp)
                                     ) {
                                         Text("Read This", fontSize = 10.sp)
                                     }
                                 }
                             }
                         }
                     }
                 }
             }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                placeholder = { Text("Ask Gemini...") },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (settings.geminiApiKey.isNotBlank() && prompt.isNotBlank()) {
                         val userMsg = ChatMessage("user", prompt)
                         history = history + userMsg
                         PersistenceManager.saveChatHistory(context, history)
                         
                         val promptToSend = prompt
                         prompt = ""
                         
                         scope.launch {
                             isLoading = true
                             val response = generateTextWithGemini(settings.geminiApiKey, promptToSend, settings.promptPreset, settings.aiModel)
                             val modelMsg = ChatMessage("model", response)
                             history = history + modelMsg
                             PersistenceManager.saveChatHistory(context, history)
                             isLoading = false
                             // Auto-read response optional? Let user decide via button for now to keep chat flow
                         }
                    }
                },
                enabled = !isLoading && prompt.isNotBlank(),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Suppress("UNUSED_VALUE")
@Composable
fun ReaderScreen(
    tokens: List<RSVPToken>,
    initialIndex: Int,
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    onBack: (Int) -> Unit,
    tts: TextToSpeech?,
    isTtsReady: Boolean,
    modifier: Modifier = Modifier,
    onManageVoices: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    var isPlaying by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isTtsEnabled by remember { mutableStateOf(false) }

    // Channel to signal TTS completion of a chunk
    val ttsChunkDoneChannel = remember { Channel<Unit>(Channel.CONFLATED) }
    var currentChunkTokenOffsets by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentChunkStartIndex by remember { mutableIntStateOf(0) }

    // Moving Focus State for Sequential Reveal
    var focusOffset by remember { mutableIntStateOf(0) }

    // Statistics Tracking
    var activeReadingDuration by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var sessionStartTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    val initialSessionIndex = remember { initialIndex }

    // Resources for measurement
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    // Effective width for text: Screen Width - (Horizontal Padding 64dp * 2 for arrows)
    // We use a slightly safer padding to avoid edge-touching.
    val effectiveMaxWidthPx = with(density) { (screenWidthDp - 140).dp.toPx() }
    val userFontSizePx = with(density) { settings.fontSize.sp.toPx() }

    // Track Reading Session
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            sessionStartTime = System.currentTimeMillis()
        } else {
            if (sessionStartTime > 0) {
                activeReadingDuration += System.currentTimeMillis() - sessionStartTime
                sessionStartTime = 0
            }
        }
    }

    LaunchedEffect(isPlaying, settings.wpm, currentIndex, settings.chunkSize) {
        if (isPlaying) {
             while (currentIndex < tokens.size && isPlaying) {
                 // Determine current chunk size (Dynamic or Fixed)
                 val currentChunkLimit = if (settings.chunkSize == 0) {
                     calculateFitCapacity(
                         tokens = tokens, 
                         startIndex = currentIndex, 
                         maxWidthPx = effectiveMaxWidthPx, 
                         fontSizePx = userFontSizePx, 
                         fontFamily = settings.font.fontFamily
                     )
                 } else {
                     min(settings.chunkSize, tokens.size - currentIndex)
                 }
                 
                 // If using sequential reveal (chunk > 1), we iterate INSIDE the chunk
                 // If chunk == 1, loop runs once, same as classic.
                 
                 // Reset focus offset if we are starting a NEW chunk traversal 
                 // (handled by logic below, assumes focusOffset starts at 0 for new chunk)
                 
                 // Wait, we need to know if we are "in the middle" of a chunk from a pause?
                 // If paused, we remember focusOffset.
                 
                 val chunkTokens = tokens.subList(currentIndex, currentIndex + currentChunkLimit)
                 
                 // Playback Loop for this Chunk
                 while (focusOffset < currentChunkLimit && isPlaying) {
                     val currentToken = chunkTokens[focusOffset]
                     
                     // TTS Trigger (Per Word)
                     if (isTtsEnabled) {
                         // Fix "1." pronunciation for single word logic
                         var speakText = currentToken.word
                         if (speakText.matches(Regex("\\d+\\."))) {
                             speakText = speakText.replace(".", "")
                         }
                         
                         // Calculate TTS Rate
                         // Base rate 1.0 is roughly 150-180 WPM depending on engine.
                         // We use 150 as a conservative baseline.
                         val ttsRate = (settings.wpm / 150f).coerceIn(0.5f, 4.0f)
                         
                         if (isTtsReady && tts != null) {
                            try {
                                if (settings.useNeuralTts) {
                                    NeuralTTSManager.speak(speakText, ttsRate)
                                } else {
                                    tts.setSpeechRate(ttsRate)
                                    tts.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                         }
                     }

                     // Delay Calculation
                     val baseDelay = (60000 / settings.wpm).toLong()
                     val finalDelay = (baseDelay * currentToken.delayMultiplier).toLong()
                     
                     delay(finalDelay)
                     
                     // Advance Focus
                     if (isPlaying) { // Check again after delay
                         focusOffset++
                     }
                 }
                 
                 // Chunk Finished?
                 if (focusOffset >= currentChunkLimit) {
                     currentIndex += currentChunkLimit
                     focusOffset = 0 // Reset for next chunk
                 }
                 
                 // Safety break
                 if (!isPlaying) break
             }
             
             if (currentIndex >= tokens.size) {
                 isPlaying = false
                 currentIndex = 0 // Reset to start
                 focusOffset = 0
             }
        }
    }

    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Helper to finish session
    fun finishSession() {
        if (isPlaying && sessionStartTime > 0) {
            activeReadingDuration += System.currentTimeMillis() - sessionStartTime
        }

        val wordsRead = currentIndex - initialSessionIndex
        if (wordsRead > 10 && activeReadingDuration > 1000) { // Filter insignificant sessions
            val durationSeconds = activeReadingDuration / 1000
            val minutes = durationSeconds / 60f
            val wpm = if (minutes > 0) wordsRead / minutes else 0f

            val session = ReadingSession(
                durationSeconds = durationSeconds,
                wordsRead = wordsRead,
                averageWpm = wpm
            )
            PersistenceManager.addReadingSession(context, session)
        }
    }

    BackHandler(onBack = {
        tts?.stop()
        finishSession()
        onBack(currentIndex)
    })

    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = settings,
            onSettingsChanged = onSettingsChanged,
            onDismiss = { showSettingsDialog = false },
            tts = tts,
            isTtsReady = isTtsReady,
            onManageVoices = onManageVoices
        )
    }

    Surface(
        color = settings.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Spacebar, Key.K -> {
                            isPlaying = !isPlaying; true
                        }
                        Key.J -> {
                            currentIndex = (currentIndex - 5).coerceAtLeast(0); true
                        }
                        Key.L -> {
                            currentIndex = (currentIndex + 5).coerceAtMost(tokens.size - 1); true
                        }
                        Key.Semicolon -> {
                            currentIndex = 0; true
                        }
                        Key.Escape, Key.Q -> {
                            tts?.stop()
                            finishSession()
                            onBack(currentIndex); true
                        }
                        else -> false
                    }
                } else false
            }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        Box(modifier = modifier.fillMaxSize()) {
            // ... (Top Bar - Same as before but with added TTS toggle visual feedback if needed) ...
            // Simplified for brevity, reusing previous structure
             Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                 // Left Group
                 Row(verticalAlignment = Alignment.CenterVertically) {
                    val remainingWords = tokens.size - currentIndex
                    val minutesRemaining = remainingWords / settings.wpm
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", minutesRemaining)} min",
                        color = settings.colorScheme.contextText,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onSettingsChanged(settings.copy(wpm = (settings.wpm - settings.wpmStep).coerceAtLeast(50f))) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease WPM", tint = settings.colorScheme.contextText, modifier = Modifier.size(16.dp))
                    }
                     
                    val speedDisplay = if (isTtsEnabled) {
                        if (settings.wpm > 450f) "Max (TTS)" else "Rate ${(settings.wpm / 150f).coerceIn(0.5f, 3.0f).toString().take(3)}x"
                    } else {
                        "WPM ${settings.wpm.toInt()}"
                    }

                    Text(
                        text = speedDisplay,
                        color = settings.colorScheme.contextText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(70.dp)
                    )
                    IconButton(
                        onClick = { onSettingsChanged(settings.copy(wpm = (settings.wpm + settings.wpmStep).coerceAtMost(1000f))) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase WPM", tint = settings.colorScheme.contextText, modifier = Modifier.size(16.dp))
                    }
                }

                // Right Group
                 Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { 
                            isTtsEnabled = !isTtsEnabled 
                            if (!isTtsEnabled) tts?.stop()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                         Icon(
                             imageVector = if (isTtsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                             contentDescription = "Toggle TTS",
                             tint = if (isTtsEnabled) settings.colorScheme.text else settings.colorScheme.contextText
                         )
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { onSettingsChanged(settings.copy(fontSize = (settings.fontSize - 4).coerceAtLeast(12))) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("A-", color = settings.colorScheme.contextText, fontSize = 12.sp)
                    }
                    Text(
                        text = "Size ${settings.fontSize}",
                        color = settings.colorScheme.contextText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(70.dp)
                    )
                    IconButton(
                        onClick = { onSettingsChanged(settings.copy(fontSize = (settings.fontSize + 4).coerceAtMost(128))) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("A+", color = settings.colorScheme.contextText, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = settings.colorScheme.contextText)
                    }
                    IconButton(onClick = { 
                        tts?.stop()
                        onBack(currentIndex) 
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = settings.colorScheme.contextText)
                    }
                }
            }

            // Center Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                 // 1. Past Context
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val shouldShowContext = when(settings.showContext) {
                        ContextDisplayOption.Always -> true
                        ContextDisplayOption.Paused -> !isPlaying
                        ContextDisplayOption.Never -> false
                    }

                    if (shouldShowContext) {
                        Text(
                            text = buildAnnotatedString {
                                val contextRange = if (settings.contextStyle == ContextStyleOption.CurrentLine) 8 else 30
                                val start = (currentIndex - contextRange).coerceAtLeast(0)
                                val end = currentIndex
                                for (i in start until end) {
                                    val token = tokens[i]
                                    val fontWeight = when(token.style) {
                                        WordStyle.Bold, WordStyle.BoldItalic, WordStyle.Header -> FontWeight.Bold
                                        else -> FontWeight.Normal
                                    }
                                    val fontStyle = when(token.style) {
                                        WordStyle.Italic, WordStyle.BoldItalic -> FontStyle.Italic
                                        else -> FontStyle.Normal
                                    }
                                    val fontFamily = if (token.style == WordStyle.Code) FontFamily.Monospace else settings.font.fontFamily
                                    val color = if (token.style == WordStyle.Link) Color(0xFF64B5F6) else settings.colorScheme.contextText.copy(alpha = settings.contextOpacity)
                                    val textDecoration = if (token.style == WordStyle.Link) TextDecoration.Underline else TextDecoration.None

                                    withStyle(style = SpanStyle(
                                        color = color,
                                        fontWeight = fontWeight,
                                        fontStyle = fontStyle,
                                        fontFamily = fontFamily,
                                        textDecoration = textDecoration
                                    )) {
                                        val cleanWord = token.word
                                            .replace("**", "")
                                            .replace("__", "")
                                            .replace("*", "")
                                            .replace("_", "")
                                            .replace("`", "")
                                            .replace("#", "")

                                        append("$cleanWord ")
                                    }
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp,
                                fontFamily = settings.font.fontFamily
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Center Row: Arrow - Word - Arrow
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    @Suppress("DEPRECATION")
                    BoxWithConstraints(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 64.dp)
                    ) {
                         val density = LocalDensity.current
                        val maxWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }

                         // Calculate current chunk tokens for display
                        val currentChunkLimit = if (settings.chunkSize == 0) {
                             calculateFitCapacity(
                                 tokens = tokens, 
                                 startIndex = currentIndex, 
                                 maxWidthPx = with(density) { (configuration.screenWidthDp - 140).dp.toPx() }, // Use global screen width calculation for consistency with loop
                                 fontSizePx = with(density) { settings.fontSize.sp.toPx() }, 
                                 fontFamily = settings.font.fontFamily
                             )
                        } else {
                            min(settings.chunkSize, tokens.size - currentIndex)
                        }
                        
                        val displayTokens = if (tokens.isNotEmpty()) {
                            // Safety measure: Ensure indices are valid
                            val end = (currentIndex + currentChunkLimit).coerceAtMost(tokens.size)
                            if (currentIndex < end) tokens.subList(currentIndex, end) else emptyList()
                        } else emptyList()

                        RSVPWordDisplay(
                            chunkMode = settings.chunkSize != 1,
                            tokens = displayTokens,
                            focusIndex = focusOffset,
                            settings = settings,
                            maxWidthPx = maxWidthPx
                        )
                    }

                    IconButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous", tint = settings.colorScheme.contextText, modifier = Modifier.fillMaxSize())
                    }

                    IconButton(
                        onClick = { if (currentIndex < tokens.size - 1) currentIndex++ },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next", tint = settings.colorScheme.contextText, modifier = Modifier.fillMaxSize())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Future Context
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val shouldShowContext = when(settings.showContext) {
                        ContextDisplayOption.Always -> true
                        ContextDisplayOption.Paused -> !isPlaying
                        ContextDisplayOption.Never -> false
                    }

                    if (shouldShowContext) {
                        Text(
                            text = buildAnnotatedString {
                                val contextRange = if (settings.contextStyle == ContextStyleOption.CurrentLine) 8 else 30
                                
                                // Re-calculate current chunk size to know what to skip.
                                // NOTE: We use the same parameters as the main display.
                                val density = LocalDensity.current
                                val currentChunkSize = if (settings.chunkSize == 0) {
                                     calculateFitCapacity(
                                         tokens = tokens, 
                                         startIndex = currentIndex, 
                                         maxWidthPx = with(density) { (configuration.screenWidthDp - 140).dp.toPx() }, 
                                         fontSizePx = with(density) { settings.fontSize.sp.toPx() }, 
                                         fontFamily = settings.font.fontFamily
                                     )
                                } else {
                                    min(settings.chunkSize, tokens.size - currentIndex)
                                }
                                
                                val start = (currentIndex + currentChunkSize).coerceAtMost(tokens.size)
                                val end = (start + contextRange).coerceAtMost(tokens.size)
                                for (i in start until end) {
                                    val token = tokens[i]
                                    val fontWeight = when(token.style) {
                                        WordStyle.Bold, WordStyle.BoldItalic, WordStyle.Header -> FontWeight.Bold
                                        else -> FontWeight.Normal
                                    }
                                    val fontStyle = when(token.style) {
                                        WordStyle.Italic, WordStyle.BoldItalic -> FontStyle.Italic
                                        else -> FontStyle.Normal
                                    }
                                    val fontFamily = if (token.style == WordStyle.Code) FontFamily.Monospace else settings.font.fontFamily
                                    val color = if (token.style == WordStyle.Link) Color(0xFF64B5F6) else settings.colorScheme.contextText.copy(alpha = settings.contextOpacity)
                                    val textDecoration = if (token.style == WordStyle.Link) TextDecoration.Underline else TextDecoration.None

                                    withStyle(style = SpanStyle(
                                        color = color,
                                        fontWeight = fontWeight,
                                        fontStyle = fontStyle,
                                        fontFamily = fontFamily,
                                        textDecoration = textDecoration
                                    )) {
                                        val cleanWord = token.word
                                            .replace("**", "")
                                            .replace("__", "")
                                            .replace("*", "")
                                            .replace("_", "")
                                            .replace("`", "")
                                            .replace("#", "")

                                        append("$cleanWord ")
                                    }
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp,
                                fontFamily = settings.font.fontFamily
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Slider(
                    value = currentIndex.toFloat(),
                    onValueChange = { currentIndex = it.toInt() },
                    onValueChangeFinished = {
                         // Optional: Auto-rewind slightly on scrubbing completion
                    },
                    valueRange = 0f..maxOf(0f, (tokens.size - 1).toFloat()),
                    colors = SliderDefaults.colors(
                        thumbColor = settings.colorScheme.text,
                        activeTrackColor = settings.colorScheme.text,
                        inactiveTrackColor = settings.colorScheme.contextText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${currentIndex + 1} / ${tokens.size}", color = settings.colorScheme.contextText, fontSize = 12.sp, textAlign = TextAlign.Start, modifier = Modifier.width(60.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = settings.colorScheme.text,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { isPlaying = !isPlaying }
                        )
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Restart",
                            tint = settings.colorScheme.contextText,
                            modifier = Modifier.clickable { currentIndex = 0 }
                        )
                    }
                    Text("", modifier = Modifier.width(60.dp))
                }
            }
        }
    }
}

// Helper function for dynamic chunking
fun calculateFitCapacity(
    tokens: List<RSVPToken>,
    startIndex: Int,
    maxWidthPx: Float,
    fontSizePx: Float,
    fontFamily: androidx.compose.ui.text.font.FontFamily
): Int {
    if (startIndex >= tokens.size) return 0
    
    val paint = android.text.TextPaint()
    paint.textSize = fontSizePx
    paint.typeface = when(fontFamily) {
        androidx.compose.ui.text.font.FontFamily.Serif -> android.graphics.Typeface.SERIF
        androidx.compose.ui.text.font.FontFamily.SansSerif -> android.graphics.Typeface.SANS_SERIF
        androidx.compose.ui.text.font.FontFamily.Monospace -> android.graphics.Typeface.MONOSPACE
        else -> android.graphics.Typeface.DEFAULT
    }

    var currentWidth = 0f
    var count = 0
    
    // Safety limit to prevent freezing on extremely long texts or weird states
    val safetyLimit = 30 

    for (i in startIndex until tokens.size) {
        if (count >= safetyLimit) break
        
        val word = tokens[i].word
        // Add space if not first word
        val textToAdd = if (count == 0) word else " $word"
        val width = paint.measureText(textToAdd)
        
        if (currentWidth + width > maxWidthPx) {
            // If the VERY FIRST word doesn't fit, we must show it anyway to avoid partial hangs
            if (count == 0) return 1
            break
        }
        currentWidth += width
        count++
    }
    return count.coerceAtLeast(1)
}
