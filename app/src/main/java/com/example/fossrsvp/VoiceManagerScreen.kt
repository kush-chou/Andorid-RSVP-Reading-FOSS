package com.example.fossrsvp

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceManagerScreen(
    currentSettings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val availableVoices = VoiceRepository.availableVoices
    
    // Track download progress
    val downloadProgress by VoiceDownloader.downloadProgress.collectAsState()
    
    // Refresh installed state occasionally or after actions
    var installedTrigger by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neural Voice Store") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Available Voices (Piper/Supertonic)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Download voices to use them offline. These models are high-quality and private.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(availableVoices) { voice ->
                val isInstalled = remember(installedTrigger) { 
                    VoiceDownloader.isVoiceInstalled(context, voice.id) 
                }
                val isDownloading = downloadProgress.containsKey(voice.id)
                val progress = downloadProgress[voice.id] ?: 0f
                val isSelected = currentSettings.voiceName == voice.id && currentSettings.useNeuralTts

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = voice.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${voice.language} • ${voice.quality} Quality",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.size(24.dp),
                                )
                            } else if (isInstalled) {
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Button(
                                        onClick = {
                                            onSettingsChanged(currentSettings.copy(
                                                voiceName = voice.id,
                                                useNeuralTts = true
                                            ))
                                        },
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Select")
                                    }
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                VoiceDownloader.downloadVoice(context, voice)
                                                installedTrigger++ // Force refresh
                                                // Auto-select after download? Optional.
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = "Download")
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(voice.description, style = MaterialTheme.typography.bodySmall)
                        
                        if (isInstalled && !isSelected) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(
                                    onClick = {
                                        VoiceDownloader.deleteVoice(context, voice.id)
                                        installedTrigger++
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
