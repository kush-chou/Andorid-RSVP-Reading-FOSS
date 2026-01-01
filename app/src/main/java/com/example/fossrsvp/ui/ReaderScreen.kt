package com.example.fossrsvp.ui

import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.min
import com.example.fossrsvp.*

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

    var showQuizDialog by remember { mutableStateOf(false) }
    var generatedQuiz by remember { mutableStateOf<Quiz?>(null) }
    var isGeneratingQuiz by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Channel to signal TTS completion of a chunk
    val ttsChunkDoneChannel = remember { Channel<Unit>(Channel.CONFLATED) }
    
    // Set up TTS Listener
    LaunchedEffect(tts) {
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                ttsChunkDoneChannel.trySend(Unit) 
            }
            override fun onError(utteranceId: String?) {
                ttsChunkDoneChannel.trySend(Unit) // Prevent hanging on error
            }
        })
    }
    
    var currentChunkTokenOffsets by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentChunkStartIndex by remember { mutableIntStateOf(0) }
    
    // Moving Focus State for Sequential Reveal
    var focusOffset by remember { mutableIntStateOf(0) }
    
    // Resources for measurement
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val textMeasurer = rememberTextMeasurer()
    
    // Compact Mode Logic (Standardized to 480dp across app)
    val isCompact = screenWidthDp < 480
    
    // Golden Ratio Spacing
    val goldenPadding = if (isCompact) 13.dp else 21.dp
    
    // Effective width for text: Screen Width - (Horizontal Padding)
    // Arrows are ~64dp each side (48 size + 16 pad). 
    // We increase safety margin to 160dp to absolutely ensure no overlap.
    val effectiveMaxWidthPx = with(density) { (screenWidthDp - 160).dp.toPx() }
    val userFontSizePx = with(density) { settings.fontSize.sp.toPx() }

    // Navigation History
    val chunkStartHistory = remember { androidx.compose.runtime.mutableStateListOf<Int>() }

    LaunchedEffect(isPlaying, settings.wpm, currentIndex, settings.chunkSize) {
        if (isPlaying) {
             while (currentIndex < tokens.size && isPlaying) {
                 // Determine current chunk size (Dynamic or Fixed)
                 val currentChunkLimit = if (settings.chunkSize == 0) {
                     calculateFitCapacity(
                         tokens = tokens, 
                         startIndex = currentIndex, 
                         maxWidthPx = effectiveMaxWidthPx, 
                         fontSizeSp = settings.fontSize.sp, 
                         fontFamily = settings.font.fontFamily,
                         textMeasurer = textMeasurer,
                         density = density
                     )
                 } else {
                     min(settings.chunkSize, tokens.size - currentIndex)
                 }
                 
                 val chunkTokens = tokens.subList(currentIndex, currentIndex + currentChunkLimit)
                 
                 // Playback Loop for this Chunk
                 while (focusOffset < currentChunkLimit && isPlaying) {
                     val currentToken = chunkTokens[focusOffset]
                     val isLastInChunk = focusOffset == currentChunkLimit - 1
                     
                     // TTS Logic
                     if (isTtsEnabled) {
                         // Fix "1." pronunciation for single word logic
                         var speakText = currentToken.word
                         if (speakText.matches(Regex("\\d+\\."))) {
                             speakText = speakText.replace(".", "")
                         }
                         val ttsRate = (settings.wpm / 150f).coerceIn(0.5f, 4.0f)
                         if (isTtsReady && tts != null) {
                            try {
                                if (settings.useNeuralTts) {
                                    NeuralTTSManager.speak(speakText, ttsRate)
                                } else {
                                    tts.setSpeechRate(ttsRate)
                                    tts.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                         }
                     }

                     // TIMING LOGIC: Stream (70%) vs Breath (Remaining Budget)
                     
                     // Helper to calculate target duration for a single word
                     fun calculateWordTarget(token: RSVPToken): Long {
                         val baseDelay = (60000 / settings.wpm).toLong()
                         
                         // 1. Complexity Penalty: +10% for every char > 7
                         val length = token.word.length
                         val penaltyMultiplier = if (length > 7) {
                             1.0f + ((length - 7) * 0.1f) 
                         } else {
                             1.0f
                         }
                         
                         // 2. Combined Duration
                         return (baseDelay * token.delayMultiplier * penaltyMultiplier).toLong()
                     }

                     val finalDelay = if (!isLastInChunk) {
                         // STREAMING PHASE:
                         // Allocating 70% of THIS word's ideal duration to the stream delay.
                         // This gives a readable but fast pace that scales with word complexity.
                         (calculateWordTarget(currentToken) * 0.7f).toLong().coerceAtLeast(30L)
                     } else {
                         // BREATH PHASE (Inter-Chunk):
                         // Calculate the Total Budget for the entire chunk
                         var totalChunkBudget = 0L
                         var timeSpentStreaming = 0L
                         
                         chunkTokens.forEachIndexed { index, t ->
                             val target = calculateWordTarget(t)
                             totalChunkBudget += target
                             
                             // Accumulate what we *actually* spent in the loop for previous words
                             if (index < chunkTokens.size - 1) {
                                  // We used 70% of target for streaming steps
                                 timeSpentStreaming += (target * 0.7f).toLong().coerceAtLeast(30L)
                             }
                         }
                         
                         // The remaining budget is the breath.
                         // This naturally becomes approx 30% of total time + 30% of the last word's time
                         // effectively creating a strong pause distributed from the savings of the stream.
                         val breathDuration = totalChunkBudget - timeSpentStreaming
                         
                         // Ensure a minimum meaningful pause (at least 1 base beat)
                         val minBreath = (60000 / settings.wpm).toLong()
                         breathDuration.coerceAtLeast(minBreath)
                     }
                     
                     delay(finalDelay)
                     
                     // Advance Focus
                     if (isPlaying) {
                         focusOffset++
                     }
                 }
                 
                 // Chunk Finished?
                 if (focusOffset >= currentChunkLimit) {
                     // Push to history before advancing
                     if (chunkStartHistory.isEmpty() || chunkStartHistory.last() != currentIndex) {
                         chunkStartHistory.add(currentIndex)
                         if (chunkStartHistory.size > 50) chunkStartHistory.removeAt(0)
                     }
                     
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
    BackHandler(onBack = { 
        tts?.stop()
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

    if (showQuizDialog && generatedQuiz != null) {
        QuizDialog(
            quiz = generatedQuiz!!,
            onDismiss = { showQuizDialog = false }
        )
    }

    if (isGeneratingQuiz) {
         Dialog(onDismissRequest = {}) {
             Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                 Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                     CircularProgressIndicator()
                     Spacer(modifier = Modifier.height(16.dp))
                     Text("Generating Quiz...")
                 }
             }
         }
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
                        Key.J, Key.LeftBracket -> {
                             // Back Navigation Logic
                             if (focusOffset > 0) {
                                 // Rewind to start of current chunk
                                 focusOffset = 0
                             } else {
                                 // Go to previous chunk
                                 if (chunkStartHistory.isNotEmpty()) {
                                     val prev = chunkStartHistory.removeLast()
                                     currentIndex = prev
                                 } else {
                                     currentIndex = (currentIndex - 5).coerceAtLeast(0) // Fallback
                                 }
                                 focusOffset = 0
                             }
                             true
                        }
                        Key.L, Key.RightBracket -> {
                            // Forward logic: Jump forward? Or just simple skip?
                            // Simple skip for now to avoid complexity with calculating next chunk size without measuring
                            currentIndex = (currentIndex + 5).coerceAtMost(tokens.size - 1); true
                        }
                        Key.Semicolon -> {
                            currentIndex = 0; chunkStartHistory.clear(); focusOffset = 0; true
                        }
                        Key.Escape, Key.Q -> {
                            tts?.stop()
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
            
            // ... (Top Bar - Left Group / Right Group omitted for brevity as they are unchanged) ...
             Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(goldenPadding),
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
                         if (settings.geminiApiKey.isNotBlank()) {
                             scope.launch {
                                 isPlaying = false // Pause reading
                                 isGeneratingQuiz = true
                                 // Reconstruct text from tokens for context (limited)
                                 // Take up to 2000 tokens around current index
                                 val start = (currentIndex - 1000).coerceAtLeast(0)
                                 val end = (currentIndex + 1000).coerceAtMost(tokens.size)
                                 val textContext = tokens.subList(start, end).joinToString(" ") { it.word }

                                 val quiz = generateQuiz(settings.geminiApiKey, textContext, settings.aiModel)
                                 isGeneratingQuiz = false
                                 if (quiz != null) {
                                     generatedQuiz = quiz
                                     showQuizDialog = true
                                 } else {
                                     android.widget.Toast.makeText(context, "Failed to generate quiz", android.widget.Toast.LENGTH_SHORT).show()
                                 }
                             }
                         } else {
                             android.widget.Toast.makeText(context, "API Key Required for Quiz", android.widget.Toast.LENGTH_SHORT).show()
                             showSettingsDialog = true
                         }
                    }) {
                        Icon(Icons.Default.School, contentDescription = "Quiz Me", tint = settings.colorScheme.contextText)
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
                    .padding(vertical = 84.dp),
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
                            .padding(horizontal = 80.dp) // Physically moved away from arrows (64dp -> 80dp)
                    ) {
                         val density = LocalDensity.current
                        val maxWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }

                         // Calculate current chunk tokens for display
                        val currentChunkLimit = if (settings.chunkSize == 0) {
                             calculateFitCapacity(
                                 tokens = tokens, 
                                 startIndex = currentIndex, 
                                 maxWidthPx = effectiveMaxWidthPx, // Use safe variable
                                 fontSizeSp = settings.fontSize.sp, 
                                 fontFamily = settings.font.fontFamily,
                                 textMeasurer = textMeasurer,
                                 density = density
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
                        onClick = { 
                             // Same logic as Key J
                             if (focusOffset > 0) {
                                 focusOffset = 0
                             } else {
                                 if (chunkStartHistory.isNotEmpty()) {
                                     currentIndex = chunkStartHistory.removeLast()
                                 } else {
                                     currentIndex = (currentIndex - 5).coerceAtLeast(0)
                                 }
                                 focusOffset = 0
                             }
                        },
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
                                         fontSizeSp = settings.fontSize.sp, 
                                         fontFamily = settings.font.fontFamily,
                                         textMeasurer = textMeasurer,
                                         density = density
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
                    .padding(goldenPadding)
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
// ... inside ReaderScreen Box ...
        if (tokens.isNotEmpty() && currentIndex < tokens.size) {
            val currentToken = tokens[currentIndex.coerceAtMost(tokens.size - 1)]
             // Check if current token (or any in chunk) is image
             // For chunk > 1, this is tricky, but pause logic stops exactly at image token.
             // Actually, simple logic: check current focused token.
             
             // WE NEED A WAY TO DETECT IMAGE STATE from the Composable scope
        }
        
    } // End of ReaderScreen content Box
    
    // IMAGE OVERLAY
    // We check if the *current active token* is an image.
    if (tokens.isNotEmpty() && currentIndex < tokens.size) {
        // If sequential reveal is on, we need to check sub-token? 
        // No, current logic pauses loop. Visuals are updated.
        // But we need to check the EXACT token at focusOffset if chunking.
        
        // Wait, loop uses focusOffset. But UI uses currentIndex + focusOffset logic inside RSVPWordDisplay.
        // Simpler: Just check if we are Paused AND current token is Image type.
        
        val actualTokenIndex = (currentIndex + focusOffset).coerceAtMost(tokens.size - 1)
        val activeToken = tokens[actualTokenIndex]
        
        if (!isPlaying && activeToken.type == TokenType.Image && activeToken.imageUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { /* Absorb clicks */ },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    coil.compose.AsyncImage(
                        model = activeToken.imageUrl,
                        contentDescription = "Content Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Takes most space
                            .padding(goldenPadding),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    
                    Button(
                        onClick = { 
                            // Skip the image token and resume
                            if (focusOffset < tokens.size - currentIndex - 1) {
                                focusOffset++ // Next word in chunk (rare for image)
                            } else {
                                currentIndex++ 
                                focusOffset = 0
                            }
                            isPlaying = true 
                        },
                        modifier = Modifier.padding(32.dp).fillMaxWidth().height(56.dp)
                    ) {
                        Text("Continue Reading")
                    }
                }
            }
        }
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
    fontSizeSp: androidx.compose.ui.unit.TextUnit,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    textMeasurer: TextMeasurer,
    density: androidx.compose.ui.unit.Density
): Int {
    if (startIndex >= tokens.size) return 0
    
    // Safety limit to prevent freezing/over-processing
    // We treat maxWidthPx conservatively (95%)
    val effectiveLimit = maxWidthPx * 0.95f
    
    val style = TextStyle(
        fontSize = fontSizeSp,
        fontFamily = fontFamily
    )

    var currentChunkText = ""
    var count = 0
    
    // Look ahead loop
    for (i in startIndex until min(tokens.size, startIndex + 10)) {
        val nextWord = tokens[i].word
        // Determine spacing: if it's the first word, no space. Otherwise add space.
        val potentialText = if (currentChunkText.isEmpty()) nextWord else "$currentChunkText $nextWord"
        
        val result = textMeasurer.measure(
            text = potentialText,
            style = style,
            softWrap = false,
            maxLines = 1,
            density = density
        )
        
        val width = result.size.width
        
        if (width <= effectiveLimit) {
            currentChunkText = potentialText
            count++
        } else {
            // If the very first word is too long, we MUST include it alone (logic for single word clipping handled by overflow=Visible)
            if (count == 0) return 1
            break
        }
    }
    return count.coerceAtLeast(1)
}
