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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.min

val PremiumRadius = 28.dp

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
    onManageVoices: () -> Unit,
    scaffoldPadding: androidx.compose.foundation.layout.PaddingValues,
    onShowStatistics: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val tabs = listOf("Paste", "Books", "Web", "Ask AI")
    
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 480

    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = settings,
            onSettingsChanged = onSettingsChanged,
            onDismiss = { showSettingsDialog = false },
            tts = tts,
            isTtsReady = isTtsReady,
            onManageVoices = onManageVoices,
            onShowStatistics = onShowStatistics
        )
    }



    // Golden Ratio Spacing (phi ≈ 1.618)
    val screenPadding = if (isCompact) 13.dp else 21.dp
    val headerPadding = if (isCompact) 13.dp else 26.dp
    
    val topPadding = scaffoldPadding.calculateTopPadding()

    val bottomSystemPadding = scaffoldPadding.calculateBottomPadding()
    val navBarReservedHeight = 84.dp
    val totalNavBarHeight = bottomSystemPadding + navBarReservedHeight
    
    val imePaddingRaw = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    
    // Add extra margin at bottom equal to headerPadding for vertical symmetry
    // AND when IME is open, add it to sit "above" the keyboard with breathing room.
    val contentBottomPadding = if (imePaddingRaw > totalNavBarHeight) {
        imePaddingRaw + headerPadding
    } else {
        totalNavBarHeight + headerPadding
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = screenPadding, end = screenPadding, top = topPadding + screenPadding, bottom = contentBottomPadding)
        ) {
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = headerPadding)) {
            IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(32.dp))
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
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(PremiumRadius)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(screenPadding),
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
                            3 -> GeminiChatInput(onStartReading, settings, context, onSettingsChanged)
                        }
                    }
                }
                
                }
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent, // Color provided by Surface
                windowInsets = NavigationBarDefaults.windowInsets // Default insets include navigation bars
            ) {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            val iconSize = if (isCompact) 28.dp else 24.dp
                            when(index) {
                                0 -> Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(iconSize))
                                1 -> Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(iconSize))
                                2 -> Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(iconSize))
                                3 -> Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(iconSize))
                            }
                        },
                        label = if (isCompact) null else { { Text(title) } },
                        alwaysShowLabel = !isCompact
                    )
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
    
    val config = LocalConfiguration.current
    val isCompact = config.screenWidthDp < 480

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 13.dp else 21.dp)
    ) {
        Text("Paste Text", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("Paste text to read immediately (not saved to library).", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().weight(1f),
            placeholder = { Text("Paste content here...") },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(PremiumRadius),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
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
                 LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp))
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
    var url by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Smart Prefix Logic: Show "https://" ghost text unless user typed a protocol
    val hasProtocol = url.startsWith("http://") || url.startsWith("https://")
    val effectiveUrl = if (hasProtocol) url else "https://$url"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(21.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Read from Web", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        
        TextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Article URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(PremiumRadius),
            prefix = {
                if (!hasProtocol && url.isNotEmpty()) {
                    Text("https://", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
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
                    val content = extractTextFromUrl(effectiveUrl)
                    isLoading = false
                    onStartReading(content, null, false, null)
                }
            },
            enabled = !isLoading && url.isNotBlank(), // effective URL is likely valid if user typed anything
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
    context: Context,
    onSettingsChanged: (AppSettings) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    // Load local history state
    var history by remember { mutableStateOf(PersistenceManager.loadChatHistory(context)) }
    
    val scope = rememberCoroutineScope()
    val listState = rememberScrollState()
    
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 480

    val primaryColor = MaterialTheme.colorScheme.primary
    val titleStyle = MaterialTheme.typography.titleMedium
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AI Chat", style = MaterialTheme.typography.headlineSmall, color = primaryColor)
            
            if (history.isNotEmpty()) {
                // "New Tab" State
                Button(
                    onClick = { 
                        history = emptyList()
                        PersistenceManager.saveChatHistory(context, history)
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(PremiumRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Tab")
                }
            } else {
                // "History >" State
                TextButton(
                    onClick = { 
                        // TODO: Implement History View
                        Toast.makeText(context, "History not available yet", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = primaryColor)
                ) {
                    Text("History >", style = titleStyle)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

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
                 if (history.isEmpty()) {
                     Text(
                         "Start a new conversation...", 
                         modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 32.dp),
                         color = Color.Gray
                     )
                 }
                 history.forEach { msg ->
                     val isUser = msg.role == "user"
                     Row(
                         modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), 
                         horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                     ) {
                         // Message Bubble
                         Surface(
                             shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = PremiumRadius, topEnd = PremiumRadius, bottomStart = if (isUser) PremiumRadius else 4.dp, bottomEnd = if (isUser) 4.dp else PremiumRadius),
                             color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                             modifier = Modifier.widthIn(max = (configuration.screenWidthDp * 0.85f).dp)
                         ) {
                             Column(modifier = Modifier.padding(20.dp)) {
                                 // Message Content
                                 Text(
                                     msg.content, 
                                     style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp)
                                 )
                                 
                                 Spacer(modifier = Modifier.height(8.dp))
                                 Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                     // "Read This" Action (For AI only)
                                     if (!isUser) {
                                         Button(
                                             onClick = { onStartReading(msg.content, null, false, null) },
                                             modifier = Modifier
                                                 // Taller button for easier touch on compact phones
                                                 .height(if (isCompact) 48.dp else 32.dp)
                                                 // Ensure minimum width on compact
                                                 .widthIn(min = if (isCompact) 80.dp else 0.dp),
                                             contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = if (isCompact) 12.dp else 8.dp)
                                         ) {
                                             Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(if (isCompact) 20.dp else 14.dp))
                                             Spacer(modifier = Modifier.width(4.dp))
                                             // On very small screens, keep text but maybe smaller? User wants "Read This" to be usable.
                                             // Actually removing text might make it cryptic. Keeping text but bigger button is better.
                                             Text("Read", fontSize = if (isCompact) 14.sp else 12.sp)
                                         }
                                     }

                                     // Delete Message Action
                                     IconButton(
                                         onClick = {
                                             history = history.filter { it != msg }
                                             PersistenceManager.saveChatHistory(context, history)
                                         },
                                         modifier = Modifier.size(32.dp)
                                     ) {
                                         Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                     }
                                 }
                             }
                         }
                     }
                 }
             }
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = prompt,
                onValueChange = { prompt = it },
                placeholder = { Text("Ask Gemini...", style = MaterialTheme.typography.bodyLarge) },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Send Button
            androidx.compose.material3.FilledIconButton(
                onClick = {
                    val trimmedPrompt = prompt.trim()
                    if (trimmedPrompt.startsWith("/key", ignoreCase = true)) {
                        // Handle API Key setting
                        val newKey = trimmedPrompt.removePrefix("/key").trim()
                        if (newKey.isNotBlank()) {
                            onSettingsChanged(settings.copy(geminiApiKey = newKey))
                            prompt = ""
                            Toast.makeText(context, "API Key Updated", Toast.LENGTH_SHORT).show()
                        }
                    } else if (prompt.isNotBlank()) {
                        val newMessage = ChatMessage("user", prompt)
                        history = history + newMessage
                        PersistenceManager.saveChatHistory(context, history)
                        
                        val promptToSend = prompt
                        prompt = ""
                        
                        // Scroll to bottom
                        scope.launch {
                            kotlinx.coroutines.delay(50) 
                            listState.animateScrollTo(listState.maxValue)
                        }

                        // Call Gemini
                        scope.launch {
                            isLoading = true
                            // Note: Current logic only sends the last prompt, not full history, as per existing implementation.
                            val response = generateTextWithGemini(settings.geminiApiKey, promptToSend, settings.promptPreset, settings.aiModel)
                            val modelMessage = ChatMessage("model", response)
                            history = history + modelMessage
                            PersistenceManager.saveChatHistory(context, history)
                            isLoading = false
                            
                            kotlinx.coroutines.delay(50) 
                            listState.animateScrollTo(listState.maxValue)
                        }
                    }
                },
                enabled = !isLoading && (prompt.isNotBlank() && (settings.geminiApiKey.isNotBlank() || prompt.trim().startsWith("/key", true))),
                modifier = Modifier.size(56.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(24.dp))
            }
        }
    }
}

// Helper for UI logic
private fun fragmentIsCommandOrReady(prompt: String, apiKey: String): Boolean {
    val trimmed = prompt.trim()
    if (trimmed.startsWith("/key", ignoreCase = true)) return true
    return trimmed.isNotBlank() && apiKey.isNotBlank()
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
    var showQuizDialog by remember { mutableStateOf(false) }
    var quiz by remember { mutableStateOf<Quiz?>(null) }
    var isGeneratingQuiz by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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

    if (showQuizDialog && quiz != null) {
        QuizDialog(quiz = quiz!!, onDismiss = { showQuizDialog = false })
    }

    if (isGeneratingQuiz) {
        Dialog(onDismissRequest = {}) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
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
                        // Generate Quiz
                        if (settings.geminiApiKey.isNotBlank()) {
                            scope.launch {
                                isGeneratingQuiz = true
                                val textContext = tokens.joinToString(" ") { it.word }
                                // Take a slice to avoid token limit if too long
                                val limitedText = textContext.take(10000)
                                quiz = generateQuiz(settings.geminiApiKey, limitedText)
                                isGeneratingQuiz = false
                                showQuizDialog = true
                            }
                        } else {
                            Toast.makeText(context, "Set Gemini API Key to use Quiz", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.SmartToy, contentDescription = "Quiz Me", tint = settings.colorScheme.contextText)
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
