package com.example.fossrsvp

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import kotlin.math.roundToInt
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.min

val PremiumRadius = 28.dp

@Composable
fun SettingsDialog(
    currentSettings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    onDismiss: () -> Unit,
    tts: TextToSpeech?,
    isTtsReady: Boolean,
    onManageVoices: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(21.dp),
            shape = RoundedCornerShape(PremiumRadius),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(21.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(21.dp)
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
                SettingsContent(currentSettings, onSettingsChanged, tts, isTtsReady, onManageVoices)
            }
        }
    }
}

@Composable
fun SettingsContent(
    currentSettings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    tts: TextToSpeech?,
    isTtsReady: Boolean,
    onManageVoices: () -> Unit
) {
    UserInterfaceSettings(currentSettings, onSettingsChanged)
    HorizontalDivider()
    ReaderBehaviorSettings(currentSettings, onSettingsChanged)
    HorizontalDivider()
    VoiceSettings(currentSettings, onSettingsChanged, tts, isTtsReady, onManageVoices)
    HorizontalDivider()
    GenerativeAISettings(currentSettings, onSettingsChanged)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettings(
    currentSettings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    tts: TextToSpeech?,
    isTtsReady: Boolean,
    onManageVoices: () -> Unit
) {
    Text("Voice Settings", style = MaterialTheme.typography.titleMedium)
    
    if (isTtsReady && tts != null) {
        Text("Voice Engine", style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
             Button(onClick = onManageVoices) {
                 Text(if (currentSettings.useNeuralTts) "Using Neural (Tap to Change)" else "Use Neural Voice (Offline)")
             }
        }
        if (currentSettings.useNeuralTts) {
            Text("Selected: ${currentSettings.voiceName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        } else {
            Text("Using System TTS", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        // System Voice Selector (Only if Neural is OFF)
        if (!currentSettings.useNeuralTts) {
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
        currentSettings.savedPresets.forEach { (name, content) ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSettingsChanged(currentSettings.copy(promptPreset = content)) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(name, modifier = Modifier.padding(8.dp).weight(1f))
                Row {
                    IconButton(onClick = {
                        newPresetName = "$name (Copy)"
                        newPresetContent = content
                        showPresetEditor = true
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Clone/Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        val newPresets = currentSettings.savedPresets.filter { it.first != name }
                        onSettingsChanged(currentSettings.copy(savedPresets = newPresets))
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
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
        val label = if (currentSettings.chunkSize == 0) "Max (Fit Screen)" else "${currentSettings.chunkSize}"
        Text("Words per Chunk: $label", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = if (currentSettings.chunkSize == 0) 6f else currentSettings.chunkSize.toFloat(),
            onValueChange = {
                val newStep = it.roundToInt()
                // Map 6 to 0 (Max), otherwise use value
                val finalSize = if (newStep == 6) 0 else newStep
                onSettingsChanged(currentSettings.copy(chunkSize = finalSize))
            },
            valueRange = 1f..6f,
            steps = 4 // Steps between 1 and 6: 2, 3, 4, 5 (4 steps)
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
fun RSVPWordDisplay(
    chunkMode: Boolean, // Kept for API compatibility, but derived from settings/tokens usually
    tokens: List<RSVPToken>, 
    focusIndex: Int, 
    settings: AppSettings,
    maxWidthPx: Float
) {
    if (tokens.isEmpty()) return

    val density = LocalDensity.current
    val userFontSizePx = with(density) { settings.fontSize.sp.toPx() }
    val fontSizeSp = with(density) { userFontSizePx.toSp() }
    
    val commonTextStyle = MaterialTheme.typography.displayLarge.copy(
        fontSize = fontSizeSp,
        fontFamily = settings.font.fontFamily,
        lineHeight = fontSizeSp * 1.5f
    )

    // Mode Detection:
    // If chunkMode is true (Settings > 1 or Max), we use Sequential Reveal (Row layout),
    // even if it happens to be a single word (consistency).
    // If chunkMode is false (Settings = 1), we use Classic RSVP (Center Fixed).
    
    val isMultiWord = chunkMode || tokens.size > 1

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // TOP RULER
        if (settings.focusLetterIndicator) {
             Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                val rulerColor = settings.colorScheme.contextText
                val centerMarkColor = settings.colorScheme.text
                
                // Base line
                drawLine(
                    color = rulerColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2f
                )
                
                // "Center" Tick
                // In Classic: Fixed Center.
                // In Sequential: Moves with the word. Hard to know exact X without LayoutCoordinates.
                // Compromise: In Sequential Mode, we might NOT show the "Center" tick, or show it above the word if possible.
                // Since this is a Canvas above the text, we don't know the text layout positions easily.
                // User said: "pointers (above and below the ORP) ... move as well".
                // To do this properly, we need to draw the rulers INSIDE the Text layout or Overlay it using LayoutCoordinates.
                // For this iteration, let's keep the Standard Center Tick for Classic, 
                // and for Sequential, we might rely on the Red Letter highlight solely, 
                // OR we accept that the Tick doesn't align perfectly without complex Layout logic.
                // Let's hide the Tick for MultiWord for now to avoid confusion, focus on Red Letter.
                
                if (!isMultiWord) {
                     drawLine(
                        color = centerMarkColor,
                        start = Offset(size.width / 2, size.height - 10f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = 4f
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isMultiWord) {
            // SEQUENTIAL REVEAL MODE (Row)
            // SEQUENTIAL REVEAL MODE (Single Text with Spans)
            // This allows wrapping if the chunk is too long for one line.
            val annotatedString = buildAnnotatedString {
                tokens.forEachIndexed { index, token ->
                    val isVisible = index <= focusIndex
                    val isFocused = index == focusIndex
                    
                    val color = if (isFocused) settings.centerLetterColor else settings.colorScheme.text
                    // If not visible, use Transparent color (alpha 0) so it takes up space but isn't seen
                    val finalColor = if (isVisible) color else Color.Transparent
                    
                    val fontWeight = if (token.style == WordStyle.Bold || token.style == WordStyle.Header) FontWeight.Bold else FontWeight.Normal
                    val decoration = if (token.style == WordStyle.Link) TextDecoration.Underline else TextDecoration.None
                    
                    // Clean text (optional, user wanted markdown rendered)
                    val textToRender = token.word.replace(Regex("[*#_`]"), "")
                    
                    withStyle(
                        style = SpanStyle(
                            color = finalColor,
                            fontWeight = fontWeight,
                            textDecoration = decoration,
                            fontSize = fontSizeSp
                        )
                    ) {
                        append(textToRender)
                        append(" ")
                    }
                }
            }

            Text(
                text = annotatedString,
                style = commonTextStyle,
                textAlign = TextAlign.Center,
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Visible // Allow overflow if it happens (shouldn't with correct chunking)
            )
        } else {
            // CLASSIC RSVP (Single Word)
            val targetToken = tokens[0]
            val word = targetToken.word
            val cleanWord = word.replace(Regex("[*#_`]"), "")
            
            val pivotIndex = (cleanWord.length - 1) / 2
            
            val fixedBoxHeightDp = with(density) { (userFontSizePx * 1.5f).toDp() }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fixedBoxHeightDp), 
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically 
                ) {
                    Text(
                        text = cleanWord.take(pivotIndex),
                        style = commonTextStyle.copy(fontWeight = if (targetToken.style == WordStyle.Bold) FontWeight.Bold else FontWeight.Normal),
                        textAlign = TextAlign.End,
                        color = settings.colorScheme.text,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = cleanWord[pivotIndex].toString(),
                        style = commonTextStyle.copy(
                            color = if (settings.highlightCenterLetter) settings.centerLetterColor else settings.colorScheme.text,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = cleanWord.drop(pivotIndex + 1),
                        style = commonTextStyle.copy(fontWeight = if (targetToken.style == WordStyle.Bold) FontWeight.Bold else FontWeight.Normal),
                        textAlign = TextAlign.Start,
                        color = settings.colorScheme.text,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // BOTTOM RULER
        if (settings.focusLetterIndicator) {
            Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                 val rulerColor = settings.colorScheme.contextText
                val centerMarkColor = settings.colorScheme.text
                 drawLine(
                    color = rulerColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f
                )
                 if (!isMultiWord) {
                     drawLine(
                        color = centerMarkColor,
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, 10f),
                        strokeWidth = 4f
                    )
                }
            }
        }
    }
}
