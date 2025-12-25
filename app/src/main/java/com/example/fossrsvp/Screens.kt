package com.example.fossrsvp

import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import java.util.Locale
import kotlin.math.min

@Suppress("UNUSED_VALUE")
@Composable
fun InputSelectionScreen(
    onStartReading: (String) -> Unit,
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    tts: TextToSpeech?,
    isTtsReady: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val tabs = listOf("Paste", "Web", "PDF", "Generate")

    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = settings,
            onSettingsChanged = onSettingsChanged,
            onDismiss = {
                showSettingsDialog = false
            },
            tts = tts,
            isTtsReady = isTtsReady
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            IconButton(
                onClick = {
                    showSettingsDialog = true
                },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }

            Text(
                text = "FOSS RSVP",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        NavigationBar(windowInsets = WindowInsets(0.dp)) {
            tabs.forEachIndexed { index, title ->
                NavigationBarItem(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                    },
                    icon = {
                        when(index) {
                            0 -> Icon(Icons.Default.ContentPaste, contentDescription = null)
                            1 -> Icon(Icons.Default.Language, contentDescription = null)
                            2 -> Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            3 -> Icon(Icons.Default.SmartToy, contentDescription = null)
                        }
                    },
                    label = { Text(title) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            when(selectedTab) {
                0 -> PasteInput(onStartReading)
                1 -> WebInput(onStartReading, settings)
                2 -> PdfInput(onStartReading)
                3 -> GeminiInput(onStartReading, settings)
            }
        }
    }
}

@Composable
fun PasteInput(onStartReading: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Paste text to read:")
            Button(
                onClick = {
                    clipboardManager.getText()?.let {
                        text = it.text
                    }
                }
            ) {
                Text("Paste from Clipboard")
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
        Button(
            onClick = { onStartReading(text) },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Read")
        }
    }
}

@Composable
fun WebInput(onStartReading: (String) -> Unit, settings: AppSettings) {
    var url by remember { mutableStateOf("https://") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("Enter Article URL:")
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (isLoading) {
            CircularProgressIndicator()
        }
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    val content = extractTextFromUrl(url)

                    val finalContent = if (settings.geminiApiKey.isNotBlank() && !content.startsWith("Error")) {
                        generateTextWithGemini(
                            apiKey = settings.geminiApiKey,
                            prompt = "Website Content: $content",
                            preset = "You are a web scraper. Clean up the following text extracted from a webpage ($url). Your task is to extract the main article or content, removing any artifacts, navigation menus, ads, footers, headers, and any other elements that are not part of the primary piece of writing. Return only the cleaned article text.",
                            modelName = settings.aiModel
                        )
                    } else {
                        content
                    }

                    isLoading = false
                    onStartReading(finalContent)
                }
            },
            enabled = !isLoading && url.length > 8,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fetch & Clean Read")
        }
    }
}

@Composable
fun PdfInput(onStartReading: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                val content = extractTextFromPdf(context, uri)
                isLoading = false
                onStartReading(content)
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Select a PDF file from your device storage.")
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { launcher.launch("application/pdf") }) {
                Text("Pick PDF")
            }
        }
    }
}

@Suppress("UNUSED_VALUE")
@Composable
fun GeminiInput(onStartReading: (String) -> Unit, settings: AppSettings) {
    var prompt by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        if (settings.geminiApiKey.isBlank()) {
            Text("Gemini API Key missing. Please set it in Settings.", color = MaterialTheme.colorScheme.error)
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Prompt") },
            modifier = Modifier
                .fillMaxWidth()
        )

        if (isLoading) {
            CircularProgressIndicator()
        }

        Button(
            onClick = {
                if (settings.geminiApiKey.isBlank()) {
                    Toast.makeText(context, "Please set API Key in Settings", Toast.LENGTH_LONG).show()
                } else {
                    scope.launch {
                        isLoading = true
                        val content = generateTextWithGemini(settings.geminiApiKey, prompt, settings.promptPreset, settings.aiModel)
                        isLoading = false
                        onStartReading(content)
                    }
                }
            },
            enabled = !isLoading && prompt.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate & Read")
        }
    }
}

@Suppress("UNUSED_VALUE")
@Composable
fun ReaderScreen(
    tokens: List<RSVPToken>,
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    onBack: () -> Unit,
    tts: TextToSpeech?,
    isTtsReady: Boolean,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isTtsEnabled by remember { mutableStateOf(false) }

    // Channel to signal TTS completion of a chunk
    val ttsChunkDoneChannel = remember { Channel<Unit>(Channel.CONFLATED) }

    var currentChunkTokenOffsets by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentChunkStartIndex by remember { mutableIntStateOf(0) }

    // TTS Synchronization Logic
    LaunchedEffect(tts) {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                if (utteranceId == "RSVP_ACTIVE") {
                    ttsChunkDoneChannel.trySend(Unit)
                }
            }

            @Suppress("DEPRECATION")
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId == "RSVP_ACTIVE") {
                    ttsChunkDoneChannel.trySend(Unit)
                }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                if (utteranceId == "RSVP_ACTIVE") {
                    val offsets = currentChunkTokenOffsets
                    val baseIndex = currentChunkStartIndex
                    for (i in 0 until offsets.size) {
                        val tokenStart = offsets[i]
                        val tokenEnd = if (i < offsets.size - 1) offsets[i+1] else Int.MAX_VALUE
                        if (start in tokenStart until tokenEnd) {
                            currentIndex = baseIndex + i
                            break
                        }
                    }
                }
            }
        })
    }

    LaunchedEffect(settings.voiceName, isTtsReady) {
        if (isTtsReady && settings.voiceName.isNotEmpty()) {
            val voice = tts?.voices?.find { it.name == settings.voiceName }
            if (voice != null) {
                tts?.voice = voice
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying && isTtsReady) {
            tts?.stop()
        }
    }

    LaunchedEffect(isPlaying, settings.wpm, isTtsEnabled) {
        if (!isPlaying || tokens.isEmpty()) return@LaunchedEffect

        if (isTtsEnabled && isTtsReady) {
            val maxTtsWpm = 450f
            val effectiveTtsWpm = min(settings.wpm, maxTtsWpm)
            val rate = (effectiveTtsWpm / 150f).coerceIn(0.5f, 3.0f)
            tts?.setSpeechRate(rate)

            while (isPlaying && currentIndex < tokens.size) {
                var lookAhead = currentIndex
                while (lookAhead < tokens.size && lookAhead < currentIndex + 100) {
                    val w = tokens[lookAhead].word
                    lookAhead++
                    if (w.endsWith(".") || w.endsWith("!") || w.endsWith("?")) break
                }
                val finalEnd = lookAhead

                val chunkTokens = tokens.subList(currentIndex, finalEnd)

                val sb = StringBuilder()
                val offsets = mutableListOf<Int>()

                chunkTokens.forEach { token ->
                    offsets.add(sb.length)
                    val clean = token.word
                        .replace("**", "")
                        .replace("__", "")
                        .replace("*", "")
                        .replace("_", "")
                        .replace("`", "")
                        .replace("#", "")
                    sb.append(clean).append(" ")
                }

                currentChunkTokenOffsets = offsets
                currentChunkStartIndex = currentIndex

                // Clear any previous completions
                while(ttsChunkDoneChannel.tryReceive().isSuccess) { /* Drain */ }

                val params = Bundle()
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "RSVP_ACTIVE")
                tts?.speak(sb.toString(), TextToSpeech.QUEUE_FLUSH, params, "RSVP_ACTIVE")

                val targetIndex = finalEnd - 1
                while (isPlaying && currentIndex < targetIndex) {
                    delay(50)
                }

                if (isPlaying) {
                    // Wait for completion signal instead of fixed delay
                    while (isPlaying) {
                        val result = ttsChunkDoneChannel.tryReceive()
                        if (result.isSuccess) break
                        delay(20)
                    }

                    if (isPlaying) {
                        currentIndex++
                    }
                }
            }
            isPlaying = false
        }
        else {
             val baseDelayMillis = (60000 / settings.wpm).toLong()
             while (isPlaying && currentIndex < tokens.size) {
                 val currentToken = tokens[currentIndex]
                 delay((baseDelayMillis * currentToken.delayMultiplier).toLong())
                 if (!isPlaying) break
                 if (currentIndex < tokens.size - 1) currentIndex++ else isPlaying = false
             }
        }
    }

    val focusRequester = remember { FocusRequester() }
    BackHandler(onBack = onBack)

    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = settings,
            onSettingsChanged = onSettingsChanged,
            onDismiss = {
                showSettingsDialog = false
            },
            tts = tts,
            isTtsReady = isTtsReady
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
                            onBack(); true
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
            // Top Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Group: Time and WPM
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

                // Right Group: Font Size, Settings, Close
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isTtsEnabled = !isTtsEnabled },
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
                    IconButton(onClick = onBack) {
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

                        RSVPWordDisplay(
                            token = if(tokens.isNotEmpty()) tokens[currentIndex] else RSVPToken(""),
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
                                val start = (currentIndex + 1).coerceAtMost(tokens.size)
                                val end = (currentIndex + contextRange).coerceAtMost(tokens.size)
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
