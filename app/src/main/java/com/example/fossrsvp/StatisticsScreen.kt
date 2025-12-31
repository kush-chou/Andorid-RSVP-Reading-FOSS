package com.example.fossrsvp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sessions = remember { PersistenceManager.loadReadingSessions(context).sortedByDescending { it.timestamp } }

    // Aggregated Stats
    val totalTimeSeconds = sessions.sumOf { it.durationSeconds }
    val totalWords = sessions.sumOf { it.wordsRead }
    val averageWpm = if (sessions.isNotEmpty()) sessions.map { it.wpm }.average().toInt() else 0

    val totalTimeFormatted = if (totalTimeSeconds < 3600) {
        "${totalTimeSeconds / 60}m"
    } else {
        "${totalTimeSeconds / 3600}h ${(totalTimeSeconds % 3600) / 60}m"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Overview Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Time",
                    value = totalTimeFormatted,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Words Read",
                    value = totalWords.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Avg WPM",
                    value = averageWpm.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Recent Sessions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (sessions.isEmpty()) {
                    item {
                         Text("No reading history yet.", color = Color.Gray, modifier = Modifier.padding(8.dp))
                    }
                } else {
                    items(sessions) { session ->
                        SessionItem(session)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
fun SessionItem(session: ReadingSession) {
    val date = remember(session.timestamp) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(session.timestamp))
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("${session.wpm} WPM", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${session.wordsRead} words", style = MaterialTheme.typography.bodyMedium)
                val duration = if (session.durationSeconds < 60) "${session.durationSeconds}s" else "${session.durationSeconds / 60}m"
                Text(duration, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
