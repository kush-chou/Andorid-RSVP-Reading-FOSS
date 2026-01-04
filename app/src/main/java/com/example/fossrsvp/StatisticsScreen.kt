package com.example.fossrsvp

import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var sessions by remember { mutableStateOf<List<ReadingSession>>(emptyList()) }

    LaunchedEffect(Unit) {
        sessions = PersistenceManager.loadSessions(context)
    }

    val totalTimeSeconds = sessions.sumOf { it.durationSeconds }
    val totalWords = sessions.sumOf { it.wordsRead }
    val averageWpm = if (sessions.isNotEmpty()) sessions.map { it.wpm }.average().toInt() else 0

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Your Statistics", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    title = "Total Time",
                    value = formatDuration(totalTimeSeconds),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Words Read",
                    value = "$totalWords",
                    modifier = Modifier.weight(1f)
                )
            }

             StatCard(
                title = "Avg Speed",
                value = "$averageWpm WPM",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Recent Sessions", style = MaterialTheme.typography.titleLarge)

            if (sessions.isEmpty()) {
                Text("No reading sessions yet.", color = Color.Gray)
            } else {
                sessions.reversed().take(10).forEach { session ->
                    SessionItem(session)
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SessionItem(session: ReadingSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val date = Date(session.timestamp)
                val format = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                Text(format.format(date), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("${session.wordsRead} words", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${session.wpm} WPM", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(formatDuration(session.durationSeconds), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    if (hrs > 0) return "${hrs}h ${mins}m"
    return "${mins}m"
}
