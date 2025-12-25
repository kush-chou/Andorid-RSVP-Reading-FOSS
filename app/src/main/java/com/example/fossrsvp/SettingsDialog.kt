package com.example.fossrsvp

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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
        label = { ActivePresetContent() },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 3
    )
}

@Composable
private fun ActivePresetContent() {
    Text("Active Preset Content")
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
