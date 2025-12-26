package com.example.fossrsvp

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import com.example.fossrsvp.ui.theme.FOSSRSVPTheme
import com.google.ai.client.generativeai.GenerativeModel
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.min

// --- Markdown / Data Models ---

enum class WordStyle {
    Normal, Bold, Italic, BoldItalic, Code, Header, Link
}

data class RSVPToken(
    val word: String,
    val style: WordStyle = WordStyle.Normal,
    val delayMultiplier: Float = 1.0f
)

data class AppSettings(
    val colorScheme: ColorSchemeOption = ColorSchemeOption.WhiteOnBlack,
    val font: FontOption = FontOption.Bookerly,
    val showContext: ContextDisplayOption = ContextDisplayOption.Always,
    val contextStyle: ContextStyleOption = ContextStyleOption.MultiLine,
    val contextOpacity: Float = 0.5f,
    val highlightCenterLetter: Boolean = true,
    val focusLetterIndicator: Boolean = true,
    val centerLetterColor: Color = Color(0xFFFF9800),
    val wpmStep: Int = 25,
    val autoStartSeconds: Int = 2,
    val wpm: Float = 300f,
    val fontSize: Int = 20,
    val promptPreset: String = "",
    val chunkSize: Int = 1, // 1 = Single word, 2 = Pair, 3 = Triplet
    val voiceName: String = "",
    val aiModel: String = "gemini-flash-latest",
    val geminiApiKey: String = "",
    val savedPresets: List<Pair<String, String>> = listOf(
        "Summarize" to "Summarize the following text in a concise manner:",
        "Explain Like I'm 5" to "Explain the following concept simply, as if to a 5-year-old:",
        "Key Points" to "Extract the main key points from the following text:"
    )
)

enum class ColorSchemeOption(val displayName: String, val background: Color, val text: Color, val contextText: Color) {
    WhiteOnBlack("White on Black", Color(0xFF222222), Color.White, Color.Gray),
    BlackOnWhite("Black on White", Color.White, Color.Black, Color.DarkGray),
    Beige("Beige", Color(0xFFF5F5DC), Color(0xFF5D4037), Color(0xFFA1887F)),
    BlackOnBlue("Black on Blue", Color(0xFFBBDEFB), Color.Black, Color.DarkGray),
    BlackOnYellow("Black on Yellow", Color(0xFFFFF9C4), Color.Black, Color.DarkGray)
}

enum class FontOption(val displayName: String, val fontFamily: FontFamily) {
    Georgia("Georgia", FontFamily.Serif),
    Arial("Arial", FontFamily.SansSerif),
    Bookerly("Bookerly", FontFamily.Serif),
    OpenSans("Open Sans", FontFamily.SansSerif),
    HelveticaNeue("Helvetica Neue", FontFamily.SansSerif),
    Garamond("Garamond", FontFamily.Serif),
    Minion("Minion", FontFamily.Serif),
    Merriweather("Merriweather", FontFamily.Serif),
    Dyslexie("Dyslexie", FontFamily.Monospace),
    Tisa("Tisa", FontFamily.Serif)
}

enum class ContextDisplayOption(val displayName: String) {
    Always("Always"),
    Paused("Only show when paused"),
    Never("Never")
}

enum class ContextStyleOption(val displayName: String) {
    CurrentLine("Current line only"),
    MultiLine("Multi-line")
}

// --- Logic ---

suspend fun parseMarkdownToTokens(text: String, chunkSize: Int = 1): List<RSVPToken> = withContext(Dispatchers.Default) {
    val rawTokens = mutableListOf<RSVPToken>()
    // Corrected Regex: removed redundant escape on first $ and removed redundant second $ escape
    val regex = Regex("""(\$[^$]+\$)|(\S+)""")
    val matches = regex.findAll(text)

    for (match in matches) {
        var word = match.value
        var style = WordStyle.Normal

        if (word.startsWith("$") && word.endsWith("$")) {
            style = WordStyle.Code
        }
        else if (word.contains("***") && word.length > 6 && (word.length - word.replace("***", "").length == 6)) {
            word = word.replace("***", "")
            style = WordStyle.BoldItalic
        }
        else if (word.contains("**") && word.length > 4 && (word.length - word.replace("**", "").length == 4)) {
            word = word.replace("**", "")
            style = WordStyle.Bold
        }
        else if (word.contains("__") && word.length > 4 && (word.length - word.replace("__", "").length == 4)) {
            word = word.replace("__", "")
            style = WordStyle.Bold
        }
        else if (word.contains("*") && word.length > 2 && (word.length - word.replace("*", "").length == 2)) {
            word = word.replace("*", "")
            style = WordStyle.Italic
        }
        else if (word.contains("_") && word.length > 2 && (word.length - word.replace("_", "").length == 2)) {
            word = word.replace("_", "")
            style = WordStyle.Italic
        }
        else if (word.contains("`") && word.length > 2 && (word.length - word.replace("`", "").length == 2)) {
            word = word.replace("`", "")
            style = WordStyle.Code
        }
        else if (word.startsWith("#")) {
            word = word.trimStart('#')
            style = WordStyle.Header
        }
        else if (word.startsWith("[") && word.contains("]")) {
            val endBracket = word.indexOf(']')
            if (endBracket > 1) {
                word = word.substring(1, endBracket)
                style = WordStyle.Link
            }
        }

        var delayMultiplier = 1.0f
        if (word.endsWith(".") || word.endsWith("!") || word.endsWith("?")) {
            delayMultiplier = 2.0f
        } else if (word.endsWith(",") || word.endsWith(";") || word.endsWith(":")) {
            delayMultiplier = 1.5f
        }
        if (word.length > 10 || style == WordStyle.Code) {
            delayMultiplier += 0.5f
        }

        if (word.isNotEmpty()) {
            rawTokens.add(RSVPToken(word, style, delayMultiplier))
        }
    }

    if (chunkSize > 1) {
        val chunkedTokens = mutableListOf<RSVPToken>()
        var i = 0
        while (i < rawTokens.size) {
            val chunk = mutableListOf<String>()
            var maxDelay = 0f
            var style = WordStyle.Normal
            
            val count = min(chunkSize, rawTokens.size - i)
            for (j in 0 until count) {
                val t = rawTokens[i + j]
                chunk.add(t.word)
                if (t.delayMultiplier > maxDelay) maxDelay = t.delayMultiplier
                if (t.style != WordStyle.Normal) style = t.style
            }
            
            chunkedTokens.add(RSVPToken(
                word = chunk.joinToString(" "),
                style = style,
                delayMultiplier = maxDelay
            ))
            i += count
        }
        chunkedTokens
    } else {
        rawTokens
    }
}


suspend fun generateTextWithGemini(apiKey: String, prompt: String, preset: String, modelName: String): String = withContext(Dispatchers.IO) {
    try {
        val model = GenerativeModel(modelName, apiKey)
        val systemContext = "You are an AI assistant embedded within a Speed Reading application (RSVP - Rapid Serial Visual Presentation). " +
                "The user wants to read the text you generate using this method, which displays one word at a time at high speed. " +
                "Therefore, please structure your response to be reader-friendly and linear, similar to a newspaper article or a clean essay. " +
                "Avoid complex formatting like tables, excessive bullet points, or ascii art that would break the flow. " +
                "Use standard paragraphs. " +
                "Here is the user's prompt:"

        val fullPrompt = if (preset.isNotBlank()) {
            "$systemContext\n\n[User Custom Preset Instruction]: $preset\n\n[User Prompt]: $prompt"
        } else {
            "$systemContext\n\n$prompt"
        }

        val response = model.generateContent(fullPrompt)
        response.text ?: "No response generated."
    } catch (e: Exception) {
        "Gemini Error: ${e.localizedMessage}"
    }
}

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
    val tabs = listOf("Paste", "Web", "PDF", "EPUB", "Generate")

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
                            3 -> Icon(Icons.Default.Book, contentDescription = null)
                            4 -> Icon(Icons.Default.SmartToy, contentDescription = null)
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
                3 -> EpubInput(onStartReading)
                4 -> GeminiInput(onStartReading, settings)
            }
        }
    }
}

@Composable
fun PasteInput(onStartReading: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Paste text to read:")
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

@Composable
fun EpubInput(onStartReading: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                val content = extractTextFromEpub(context, uri)
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
        Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Select an EPUB file from your device storage.")
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            // Using generic type and filtering is safer, but mime type helps
            Button(onClick = { launcher.launch("application/epub+zip") }) {
                Text("Pick EPUB")
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

@Composable
fun SettingsDialog(
    currentSettings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    onDismiss: () -> Unit,
    tts: TextToSpeech?,
    isTtsReady: Boolean
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()
                SettingsContent(currentSettings, onSettingsChanged, tts, isTtsReady)
            }
        }
    }
}

@Composable
fun SettingsContent(
    currentSettings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    tts: TextToSpeech?,
    isTtsReady: Boolean
) {
    UserInterfaceSettings(currentSettings, onSettingsChanged)
    HorizontalDivider()
    ReaderBehaviorSettings(currentSettings, onSettingsChanged)
    HorizontalDivider()
    VoiceSettings(currentSettings, onSettingsChanged, tts, isTtsReady)
    HorizontalDivider()
    GenerativeAISettings(currentSettings, onSettingsChanged)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettings(
    currentSettings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    tts: TextToSpeech?,
    isTtsReady: Boolean
) {
    Text("Voice Settings", style = MaterialTheme.typography.titleMedium)
    
    if (isTtsReady && tts != null) {
        var expanded by remember { mutableStateOf(false) }
        val voices = remember(tts) { tts.voices?.toList()?.sortedBy { it.name } ?: emptyList() }
        
        fun getReadableName(voice: Voice): String {
            val name = voice.name
            val locale = voice.locale.displayLanguage
            val country = voice.locale.displayCountry
            
            val parts = name.split("-")
            val personaCode = parts.find { it.length == 3 && it != "low" && it != "mid" } ?: ""
            
            val delivery = if (name.contains("network")) "Neural (Network)" else "Standard (Local)"
            
            val quality = when {
                voice.quality >= Voice.QUALITY_VERY_HIGH -> "Studio"
                voice.quality >= Voice.QUALITY_HIGH -> "HD"
                else -> ""
            }
            
            val gender = when {
                name.contains("female", ignoreCase = true) -> "Female"
                name.contains("male", ignoreCase = true) -> "Male"
                personaCode.endsWith("f") -> "Female"
                personaCode.endsWith("m") -> "Male"
                else -> ""
            }

            val details = listOfNotNull(
                personaCode.uppercase().takeIf { it.isNotEmpty() },
                gender.takeIf { it.isNotEmpty() },
                quality.takeIf { it.isNotEmpty() },
                delivery
            ).joinToString(" - ")

            return "$locale ($country): $details [$name]"
        }

        val currentVoice = voices.find { it.name == currentSettings.voiceName }
        val displayText = currentVoice?.let { getReadableName(it) } ?: "System Default"

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { 
                @Suppress("UNUSED_VALUE")
                expanded = !expanded 
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = displayText,
                onValueChange = {},
                readOnly = true,
                label = { Text("Voice") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { 
                    @Suppress("UNUSED_VALUE")
                    expanded = false 
                }
            ) {
                voices.take(100).forEach { voice ->
                    DropdownMenuItem(
                        text = { 
                            Column {
                                Text(getReadableName(voice), style = MaterialTheme.typography.bodyMedium)
                                Text(voice.name, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        },
                        onClick = {
                            onSettingsChanged(currentSettings.copy(voiceName = voice.name))
                            @Suppress("UNUSED_VALUE")
                            expanded = false
                        }
                    )
                }
            }
        }
    } else {
        Text("TTS not ready or unavailable", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    }
}

@Suppress("UNUSED_VALUE")
@Composable
fun GenerativeAISettings(
    currentSettings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit
) {
    Text("Generative AI", style = MaterialTheme.typography.titleMedium)
    
    OutlinedTextField(
        value = currentSettings.geminiApiKey,
        onValueChange = { onSettingsChanged(currentSettings.copy(geminiApiKey = it)) },
        label = { Text("API Key") },
        placeholder = { Text("gemini-only") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    var modelExpanded by remember { mutableStateOf(false) }
    val models = listOf("gemini-flash-latest", "gemini-flash-lite-latest", "gemini-3-flash-preview")
    
    Box {
        OutlinedTextField(
            value = currentSettings.aiModel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Model") },
            trailingIcon = { IconButton(onClick = { modelExpanded = true }) { Icon(Icons.Default.Settings, null) } },
            modifier = Modifier.fillMaxWidth().clickable { modelExpanded = true }
        )
        DropdownMenu(
            expanded = modelExpanded,
            onDismissRequest = { 
                modelExpanded = false 
            }
        ) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model) },
                    onClick = {
                        onSettingsChanged(currentSettings.copy(aiModel = model))
                        modelExpanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text("Prompt Presets", style = MaterialTheme.typography.titleSmall)
    
    var showPresetEditor by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    var newPresetContent by remember { mutableStateOf("") }

    if (showPresetEditor) {
        Dialog(onDismissRequest = { 
            showPresetEditor = false 
        }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add Preset", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = newPresetName, onValueChange = { newPresetName = it }, label = { Text("Name") })
                    OutlinedTextField(value = newPresetContent, onValueChange = { newPresetContent = it }, label = { Text("Instruction") }, minLines = 3)
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { 
                            showPresetEditor = false 
                        }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (newPresetName.isNotBlank() && newPresetContent.isNotBlank()) {
                                val newPresets = currentSettings.savedPresets + (newPresetName to newPresetContent)
                                onSettingsChanged(currentSettings.copy(savedPresets = newPresets))
                                showPresetEditor = false
                            }
                        }) { Text("Save") }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        currentSettings.savedPresets.forEach { (name, content) ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSettingsChanged(currentSettings.copy(promptPreset = content)) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(name, modifier = Modifier.padding(8.dp))
                IconButton(onClick = {
                     val newPresets = currentSettings.savedPresets.filter { it.first != name }
                     onSettingsChanged(currentSettings.copy(savedPresets = newPresets))
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
        Button(onClick = { 
            showPresetEditor = true 
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Add New Preset")
        }
    }

    OutlinedTextField(
        value = currentSettings.promptPreset,
        onValueChange = { onSettingsChanged(currentSettings.copy(promptPreset = it)) },
        label = { Text("Active Preset Content") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 3
    )
}

@Composable
fun UserInterfaceSettings(
    currentSettings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit
) {
    // Color Scheme
    Text("Color Scheme", style = MaterialTheme.typography.titleMedium)
    ColorSchemeOption.entries.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSettingsChanged(currentSettings.copy(colorScheme = option)) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentSettings.colorScheme == option,
                onClick = { onSettingsChanged(currentSettings.copy(colorScheme = option)) }
            )
            Text(text = option.displayName, modifier = Modifier.padding(start = 8.dp))
        }
    }

    HorizontalDivider()

    // Font
    Text("Font", style = MaterialTheme.typography.titleMedium)
    FontOption.entries.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSettingsChanged(currentSettings.copy(font = option)) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentSettings.font == option,
                onClick = { onSettingsChanged(currentSettings.copy(font = option)) }
            )
            Text(text = option.displayName, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun ReaderBehaviorSettings(
    currentSettings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit
) {
    // Toggles
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Highlight Center Letter")
        Switch(
            checked = currentSettings.highlightCenterLetter,
            onCheckedChange = { onSettingsChanged(currentSettings.copy(highlightCenterLetter = it)) }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Show Focus Indicator")
        Switch(
            checked = currentSettings.focusLetterIndicator,
            onCheckedChange = { onSettingsChanged(currentSettings.copy(focusLetterIndicator = it)) }
        )
    }

    // Word Chunking
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Words per Chunk: ${currentSettings.chunkSize}", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = currentSettings.chunkSize.toFloat(),
            onValueChange = { onSettingsChanged(currentSettings.copy(chunkSize = it.toInt())) },
            valueRange = 1f..3f,
            steps = 1
        )
    }

    HorizontalDivider()

    // Context Settings
    Text("Context Display", style = MaterialTheme.typography.titleMedium)
    ContextDisplayOption.entries.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSettingsChanged(currentSettings.copy(showContext = option)) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentSettings.showContext == option,
                onClick = { onSettingsChanged(currentSettings.copy(showContext = option)) }
            )
            Text(text = option.displayName, modifier = Modifier.padding(start = 8.dp))
        }
    }

    HorizontalDivider()

    Text("Context Style", style = MaterialTheme.typography.titleMedium)
    ContextStyleOption.entries.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSettingsChanged(currentSettings.copy(contextStyle = option)) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentSettings.contextStyle == option,
                onClick = { onSettingsChanged(currentSettings.copy(contextStyle = option)) }
            )
            Text(text = option.displayName, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun RSVPWordDisplay(token: RSVPToken, settings: AppSettings, maxWidthPx: Float) {
    if (token.word.isEmpty()) return

    // Remove Markdown symbols BUT KEEP PUNCTUATION
    val word = when(token.style) {
        WordStyle.Bold -> token.word.replace("**", "").replace("__", "")
        WordStyle.Italic -> token.word.replace("*", "").replace("_", "")
        WordStyle.BoldItalic -> token.word.replace("***", "")
        WordStyle.Code -> token.word // Do NOT strip from code/math
        WordStyle.Header -> token.word.replace("#", "")
        WordStyle.Link -> token.word // Links usually need more parsing, keeping as is
        else -> token.word
    }

    // Calculate pivot based on CLEANED word
    val pivotIndex = (word.length - 1) / 2

    // Safety Limit: Calculate max possible font size to fit on screen
    // Reduced factor from 0.6f to 0.5f to be safer against overlap
    val estimatedMaxFontSize = (maxWidthPx / (word.length * 0.5f)).coerceAtLeast(12f)

    // Combine user setting with safety limit
    val density = LocalDensity.current
    val userFontSizePx = with(density) { settings.fontSize.sp.toPx() }

    // Use the SMALLER of: User's desired size OR the Safety Max size
    val finalFontSizePx = min(userFontSizePx, estimatedMaxFontSize)
    val fontSizeSp = with(density) { finalFontSizePx.toSp() }

    // Style logic
    val fontWeight = when(token.style) {
        WordStyle.Bold, WordStyle.BoldItalic, WordStyle.Header -> FontWeight.Bold
        else -> FontWeight.Normal
    }
    val fontStyle = when(token.style) {
        WordStyle.Italic, WordStyle.BoldItalic -> FontStyle.Italic
        else -> FontStyle.Normal
    }
    val fontFamily = if (token.style == WordStyle.Code) FontFamily.Monospace else settings.font.fontFamily
    val color = if (token.style == WordStyle.Link) Color(0xFF64B5F6) else settings.colorScheme.text
    val textDecoration = if (token.style == WordStyle.Link) TextDecoration.Underline else TextDecoration.None
    
    // Fix Jitter: Enforce a constant Line Height regardless of ascenders/descenders
    val actualLineHeightSp = fontSizeSp * 1.5f
    val fixedBoxHeightDp = with(density) { (userFontSizePx * 1.5f).toDp() }

    val commonTextStyle = MaterialTheme.typography.displayLarge.copy(
        fontSize = fontSizeSp,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        textDecoration = textDecoration,
        lineHeight = actualLineHeightSp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (settings.focusLetterIndicator) {
            Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                drawLine(
                    color = settings.colorScheme.contextText,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2f
                )
                drawLine(
                    color = settings.colorScheme.text,
                    start = Offset(size.width / 2, size.height - 10f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 4f
                )
            }
        }

        // Fixed Spacer is okay, but the Container below is crucial
        Spacer(modifier = Modifier.height(16.dp))

        // Fix Jitter: Use a Box with a FIXED height derived from the font size
        // This anchors the visual center of the text row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(fixedBoxHeightDp), // Anchor height
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically // Center within the fixed box
            ) {
                Text(
                    text = word.take(pivotIndex),
                    style = commonTextStyle,
                    textAlign = TextAlign.End,
                    color = color,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip // Prevent painting over center
                )
                Text(
                    text = word[pivotIndex].toString(),
                    style = commonTextStyle.copy(
                        color = if (settings.highlightCenterLetter) settings.centerLetterColor else color,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = word.drop(pivotIndex + 1),
                    style = commonTextStyle,
                    textAlign = TextAlign.Start,
                    color = color,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip // Prevent painting over center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (settings.focusLetterIndicator) {
            Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                drawLine(
                    color = settings.colorScheme.contextText,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = settings.colorScheme.text,
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, 10f),
                    strokeWidth = 4f
                )
            }
        }
    }
}