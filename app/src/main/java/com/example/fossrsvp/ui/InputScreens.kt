package com.example.fossrsvp.ui

import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import android.provider.OpenableColumns
import com.example.fossrsvp.*

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
    scaffoldPadding: PaddingValues
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
            onManageVoices = onManageVoices
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
