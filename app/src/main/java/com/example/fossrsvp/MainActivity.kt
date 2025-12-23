package com.example.fossrsvp

import android.content.Context
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
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
    val fontSize: Int = 56,
    val promptPreset: String = "",
    val chunkWords: Boolean = false, // Experimental Feature
    val enableTts: Boolean = false
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

suspend fun parseMarkdownToTokens(text: String, chunkWords: Boolean = false): List<RSVPToken> = withContext(Dispatchers.Default) {
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

    if (chunkWords) {
        val chunkedTokens = mutableListOf<RSVPToken>()
        var i = 0
        while (i < rawTokens.size) {
            val t1 = rawTokens[i]
            if (i + 1 < rawTokens.size) {
                val t2 = rawTokens[i + 1]
                val combinedWord = "${t1.word} ${t2.word}"
                chunkedTokens.add(RSVPToken(
                    word = combinedWord,
                    style = t1.style,
                    delayMultiplier = (t1.delayMultiplier + t2.delayMultiplier) * 0.8f
                ))
                i += 2
            } else {
                chunkedTokens.add(t1)
                i++
            }
        }
        chunkedTokens
    } else {
        rawTokens
    }
}

suspend fun extractTextFromPdf(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            PDDocument.load(inputStream).use { document ->
                val stripper = PDFTextStripper()
                stripper.getText(document)
            }
        } ?: "Error reading file"
    } catch (e: Exception) {
        "Error: ${e.localizedMessage}"
    }
}

suspend fun extractTextFromUrl(url: String): String = withContext(Dispatchers.IO) {
    try {
        val doc = Jsoup.connect(url).get()
        doc.body().text()
    } catch (e: Exception) {
        "Error fetching URL: ${e.localizedMessage}"
    }
}

suspend fun generateTextWithGemini(apiKey: String, prompt: String, preset: String): String = withContext(Dispatchers.IO) {
    try {
        val model = GenerativeModel("gemini-2.5-pro", apiKey)
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
    private var ttsHelper: TextToSpeechHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsHelper = TextToSpeechHelper(this)
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            FOSSRSVPTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RSVPApp(
                        ttsHelper = ttsHelper,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsHelper?.shutdown()
    }
}

@Composable
fun RSVPApp(
    ttsHelper: TextToSpeechHelper?,
    modifier: Modifier = Modifier
) {
    var tokens by remember { mutableStateOf(emptyList<RSVPToken>()) }
    var isReading by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(AppSettings()) }
    var isParsing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
            ttsHelper = ttsHelper,
            onSettingsChanged = { settings = it },
            onBack = { isReading = false },
            modifier = modifier
        )
    } else {
        InputSelectionScreen(
            onStartReading = { text ->
                if (text.isNotBlank()) {
                    scope.launch {
                        isParsing = true
                        tokens = parseMarkdownToTokens(text, settings.chunkWords)
                        isParsing = false
                        isReading = true
                    }
                }
            },
            settings = settings,
            onSettingsChanged = { settings = it },
            modifier = modifier
        )
    }
}

@Composable
fun InputSelectionScreen(
    onStartReading: (String) -> Unit,
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val tabs = listOf("Paste", "Web", "PDF", "Generate")

    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = settings,
            onSettingsChanged = onSettingsChanged,
            onDismiss = { showSettingsDialog = false }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            IconButton(
                onClick = { showSettingsDialog = true },
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
                    onClick = { selectedTab = index },
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
                1 -> WebInput(onStartReading)
                2 -> PdfInput(onStartReading)
                3 -> GeminiInput(onStartReading, settings.promptPreset)
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
fun WebInput(onStartReading: (String) -> Unit) {
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
                    isLoading = false
                    onStartReading(content)
                }
            },
            enabled = !isLoading && url.length > 8,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fetch & Read")
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
fun GeminiInput(onStartReading: (String) -> Unit, preset: String) {
    var prompt by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isKeySaved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("FossRsvpPrefs", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        val savedKey = sharedPreferences.getString("GEMINI_API_KEY", "")
        if (!savedKey.isNullOrBlank()) {
            apiKey = savedKey
            isKeySaved = true
        }
    }

    val saveKey = {
        sharedPreferences.edit { putString("GEMINI_API_KEY", apiKey) }
        isKeySaved = true
        Toast.makeText(context, "API Key Saved", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        if (!isKeySaved) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { saveKey() })
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = saveKey) {
                    Icon(Icons.Default.Check, contentDescription = "Save Key")
                }
            }
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
                val currentKey = sharedPreferences.getString("GEMINI_API_KEY", "") ?: ""
                if (currentKey.isBlank()) {
                    Toast.makeText(context, "Please set API Key in Settings or above", Toast.LENGTH_LONG).show()
                } else {
                    scope.launch {
                        isLoading = true
                        val content = generateTextWithGemini(currentKey, prompt, preset)
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

// AI: Implement the text-to-speech feature as described in the comments below.
//
// TODO: Handover to Neural Text-to-Speech Expert
//
// Our next major feature is to integrate text-to-speech (TTS) as a reading aid.
// The goal is to generate audio for each word as it's displayed on the screen.
// This will provide auditory reinforcement for the user, potentially improving
// reading speed and comprehension.
//
// We should use the "Read Aloud" feature from the "Swift Read" application as a
// reference for the desired user experience. This involves pre-generating the audio
// for the words (or generating it on-the-fly with minimal latency) and playing
// the corresponding sound clip as each `RSVPToken` is displayed.
//
// Key areas for implementation will be within the `ReaderScreen` composable,
// likely involving a new function to handle TTS generation and playback logic
// that syncs with the `LaunchedEffect` that controls word timing.
@Composable
fun ReaderScreen(
    tokens: List<RSVPToken>,
    settings: AppSettings,
    ttsHelper: TextToSpeechHelper?,
    onSettingsChanged: (AppSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    BackHandler(onBack = onBack)

    LaunchedEffect(currentIndex) {
        if (settings.enableTts) {
            val token = tokens.getOrNull(currentIndex)
            if (token != null && token.word.isNotEmpty()) {
                val wordToSpeak = token.word
                    .replace("**", "")
                    .replace("__", "")
                    .replace("*", "")
                    .replace("_", "")
                    .replace("`", "")
                    .replace("#", "")
                ttsHelper?.speak(wordToSpeak)
            }
        }
    }

    LaunchedEffect(isPlaying, settings.wpm) {
        if (isPlaying && tokens.isNotEmpty()) {
            val baseDelayMillis = (60000 / settings.wpm).toLong()
            while (currentIndex < tokens.size) {
                val currentToken = tokens[currentIndex]
                delay((baseDelayMillis * currentToken.delayMultiplier).toLong())

                if (!isPlaying) break

                if (currentIndex < tokens.size - 1) {
                    currentIndex++
                } else {
                    isPlaying = false
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = settings,
            onSettingsChanged = onSettingsChanged,
            onDismiss = { showSettingsDialog = false }
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
                    Text(
                        text = "WPM ${settings.wpm.toInt()}",
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
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val density = LocalDensity.current
                    val maxWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }

                    RSVPWordDisplay(
                        token = if(tokens.isNotEmpty()) tokens[currentIndex] else RSVPToken(""),
                        settings = settings,
                        maxWidthPx = maxWidthPx
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Context Text
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
                            val end = (currentIndex + contextRange).coerceAtMost(tokens.size)
                            for (i in start until end) {
                                val isCurrent = i == currentIndex
                                val token = tokens[i]

                                val fontWeight = when(token.style) {
                                    WordStyle.Bold, WordStyle.BoldItalic, WordStyle.Header -> FontWeight.Bold
                                    else -> if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                }
                                val fontStyle = when(token.style) {
                                    WordStyle.Italic, WordStyle.BoldItalic -> FontStyle.Italic
                                    else -> FontStyle.Normal
                                }
                                val fontFamily = if (token.style == WordStyle.Code) FontFamily.Monospace else settings.font.fontFamily
                                val color = if (token.style == WordStyle.Link) Color(0xFF64B5F6) else {
                                    if (isCurrent) settings.colorScheme.text else settings.colorScheme.contextText.copy(alpha = settings.contextOpacity)
                                }
                                val textDecoration = if (token.style == WordStyle.Link) TextDecoration.Underline else TextDecoration.None

                                withStyle(style = SpanStyle(
                                    color = color,
                                    fontWeight = fontWeight,
                                    fontStyle = fontStyle,
                                    fontFamily = fontFamily,
                                    textDecoration = textDecoration
                                )) {
                                    // Strip formatting chars for context view too, but keep word
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

            // Arrows
            IconButton(
                onClick = { if (currentIndex > 0) currentIndex-- },
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous", tint = settings.colorScheme.contextText, modifier = Modifier.size(48.dp))
            }

            IconButton(
                onClick = { if (currentIndex < tokens.size - 1) currentIndex++ },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next", tint = settings.colorScheme.contextText, modifier = Modifier.size(48.dp))
            }

            // Bottom Control Bar
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
    val estimatedMaxFontSize = (maxWidthPx / (word.length * 0.6f)).coerceAtLeast(12f)

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

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = word.take(pivotIndex),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = fontSizeSp,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight,
                    fontStyle = fontStyle,
                    textDecoration = textDecoration
                ),
                textAlign = TextAlign.End,
                color = color,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible
            )
            Text(
                text = word[pivotIndex].toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = fontSizeSp,
                    color = if (settings.highlightCenterLetter) settings.centerLetterColor else color,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    fontStyle = fontStyle,
                    textDecoration = textDecoration
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = word.drop(pivotIndex + 1),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = fontSizeSp,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight,
                    fontStyle = fontStyle,
                    textDecoration = textDecoration
                ),
                textAlign = TextAlign.Start,
                color = color,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible
            )
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

@Composable
fun SettingsDialog(
    currentSettings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    onDismiss: () -> Unit
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

                HorizontalDivider()

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chunk Words (Experimental)")
                    Switch(
                        checked = currentSettings.chunkWords,
                        onCheckedChange = { onSettingsChanged(currentSettings.copy(chunkWords = it)) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Text-to-Speech")
                    Switch(
                        checked = currentSettings.enableTts,
                        onCheckedChange = { onSettingsChanged(currentSettings.copy(enableTts = it)) }
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

                // Context Style (Added to fix unused property warning and feature completeness)
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
        }
    }
}
