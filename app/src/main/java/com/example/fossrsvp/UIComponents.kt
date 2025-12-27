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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.min

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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Moving Focus Mode (Experimental)")
        Switch(
            checked = currentSettings.movingFocus,
            onCheckedChange = { onSettingsChanged(currentSettings.copy(movingFocus = it)) }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Dynamic Chunking (Fit Screen)")
        Switch(
            checked = currentSettings.dynamicChunking,
            onCheckedChange = { onSettingsChanged(currentSettings.copy(dynamicChunking = it)) }
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
fun RSVPWordDisplay(
    chunkMode: Boolean, // True if we are sending a full chunk for "Moving Focus"
    tokens: List<RSVPToken>, // The visible chunk
    focusIndex: Int, // The index WITHIN the chunk to highlight
    settings: AppSettings,
    maxWidthPx: Float
) {
    if (tokens.isEmpty()) return

    // If classic RSVP (Single word, centered), treat it as a chunk of 1 with focus 0 for simplicity in logic,
    // BUT we need to maintain the specific "Fixed Anchor" look of Classic RSVP.
    // Moving Focus mode renders the whole chunk statically and moves the indicator.

    val density = LocalDensity.current
    val userFontSizePx = with(density) { settings.fontSize.sp.toPx() }
    
    // Safety size calculation (based on the longest word in the chunk if dynamic?)
    // For Moving Focus, we fit the *line*. 
    // For Classic, we fit the *single word*.
    
    // Let's assume for Moving Focus, the text wraps naturally or is one line?
    // User requested: "ORP changes and moves left-to-right... jumping to the 'would-be in single chunking' ORP"
    
    // Common Styles
    val fontSizeSp = with(density) { userFontSizePx.toSp() } // Simplified sizing for now
    
    val commonTextStyle = MaterialTheme.typography.displayLarge.copy(
        fontSize = fontSizeSp,
        fontFamily = settings.font.fontFamily,
        lineHeight = fontSizeSp * 1.5f
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Top Ruler
        if (settings.focusLetterIndicator) {
             Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                val rulerColor = settings.colorScheme.contextText
                val centerMarkColor = settings.colorScheme.text
                
                // Draw base line
                drawLine(
                    color = rulerColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2f
                )
                
                // If classic mode, center mark is fixed.
                // If moving focus, we need to calculate X offset of the focused word's ORP.
                // This is hard to do perfectly without TextLayoutResult. 
                // For now, in Classic Mode, it's always center.
                if (!settings.movingFocus) {
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

        if (settings.movingFocus) {
            // MOVING FOCUS MODE
            // Render the full chunk as a flow/row, and highlight the specific word at focusIndex.
            // We need to know the position of that word to draw the "Moving Ruler" if we were to implement it fully.
            // For now, just Highlighting the word red is the first step.
            
            // Experimental: Horizontal Row of words
             Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center, // Or Start?
                verticalAlignment = Alignment.CenterVertically
            ) {
                tokens.forEachIndexed { index, token ->
                    val isFocused = index == focusIndex
                    val color = if (isFocused) settings.centerLetterColor else settings.colorScheme.text.copy(alpha = 0.5f)
                    val fontWeight = if (token.style == WordStyle.Bold || token.style == WordStyle.Header) FontWeight.Bold else FontWeight.Normal
                    val decoration = if (token.style == WordStyle.Link) TextDecoration.Underline else TextDecoration.None

                    Text(
                        text = token.word + " ", // Add space
                        style = commonTextStyle.copy(
                            color = color,
                            fontWeight = fontWeight,
                            textDecoration = decoration
                        )
                    )
                }
            }
        } else {
            // CLASSIC RSVP (Single Word or Static Chunk)
            // Even if chunkSize > 1 in settings, Classic RSVP simply Concatenates them into one string "Hello World"
            // and centers the *middle* of that string (or ORP).
            // But if we reuse this logic for standard single-word...
            
            val targetToken = tokens.getOrNull(0) ?: return
            val word = targetToken.word
            
             // Strip markdown for Classic Mode visual cleanliness (as previously implemented)
             // UNLESS user specifically asked for markdown rendering.
             // User said: "Make sure ... markdown ... gets rendered at all".
             // So we should NOT strip, but Apply Style.
             
             // Clean specific chars but keep text
             val displayWord = when(targetToken.style) {
                WordStyle.Bold -> word.replace("**", "").replace("__", "")
                WordStyle.Italic -> word.replace("*", "").replace("_", "")
                WordStyle.Header -> word.replace("#", "")
                else -> word
             }

            val pivotIndex = (displayWord.length - 1) / 2
            
            // Classic Jitter-Free Box
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
                        text = displayWord.take(pivotIndex),
                        style = commonTextStyle.copy(fontWeight = if (targetToken.style == WordStyle.Bold) FontWeight.Bold else FontWeight.Normal),
                        textAlign = TextAlign.End,
                        color = settings.colorScheme.text,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = displayWord[pivotIndex].toString(),
                        style = commonTextStyle.copy(
                            color = if (settings.highlightCenterLetter) settings.centerLetterColor else settings.colorScheme.text,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = displayWord.drop(pivotIndex + 1),
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

        // Bottom Ruler
        if (settings.focusLetterIndicator) {
            Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                // Same logic as top
                 val rulerColor = settings.colorScheme.contextText
                val centerMarkColor = settings.colorScheme.text
                 drawLine(
                    color = rulerColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f
                )
                 if (!settings.movingFocus) {
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
