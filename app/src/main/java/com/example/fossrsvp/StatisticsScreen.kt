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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(context: Context) {
    var sessions by remember { mutableStateOf(emptyList<ReadingSession>()) }

    LaunchedEffect(Unit) {
        sessions = PersistenceManager.loadReadingSessions(context).sortedByDescending { it.timestamp }
    }

    val totalWords = sessions.sumOf { it.wordsRead }
    val totalSeconds = sessions.sumOf { it.durationSeconds }
    val averageWpm = if (totalSeconds > 0) (totalWords.toFloat() / (totalSeconds / 60f)) else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Reading Statistics", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)

        // Summary Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(label = "Total Words", value = "$totalWords")
                StatItem(label = "Time (min)", value = "${totalSeconds / 60}")
                StatItem(label = "Avg WPM", value = "${averageWpm.toInt()}")
            }
        }

        Text("Recent Sessions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("No reading sessions yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions) { session ->
                    SessionItem(session)
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SessionItem(session: ReadingSession) {
    val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(session.timestamp))

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("${session.wordsRead} words", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${session.averageWpm.toInt()} WPM", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("${session.durationSeconds / 60}m ${session.durationSeconds % 60}s", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
