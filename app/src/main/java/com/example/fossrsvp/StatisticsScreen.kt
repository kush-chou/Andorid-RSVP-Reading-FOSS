package com.example.fossrsvp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    sessions: List<ReadingSession>,
    onBack: () -> Unit
) {
    val totalTimeSeconds = sessions.sumOf { it.durationSeconds }
    val totalWords = sessions.sumOf { it.wordsRead }
    val averageWpm = if (sessions.isNotEmpty()) sessions.map { it.wpm }.average().toInt() else 0

    Scaffold(
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Reading Statistics",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    title = "Total Time",
                    value = formatDuration(totalTimeSeconds),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Total Words",
                    value = totalWords.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    title = "Avg WPM",
                    value = averageWpm.toString(),
                    modifier = Modifier.weight(1f)
                )
                 StatCard(
                    title = "Sessions",
                    value = sessions.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("WPM History", style = MaterialTheme.typography.titleLarge)

            // WPM Graph
            if (sessions.isNotEmpty()) {
                WpmGraph(sessions = sessions.takeLast(20), modifier = Modifier.fillMaxWidth().height(200.dp))
            } else {
                 Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                     Text("No data available", color = Color.Gray)
                 }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Recent Sessions", style = MaterialTheme.typography.titleLarge)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions.sortedByDescending { it.timestamp }.take(10)) { session ->
                    SessionItem(session)
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SessionItem(session: ReadingSession) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(dateFormat.format(Date(session.timestamp)), style = MaterialTheme.typography.bodyLarge)
                Text("${session.wordsRead} words", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                 Text("${session.wpm} WPM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                 Text(formatDuration(session.durationSeconds), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun WpmGraph(sessions: List<ReadingSession>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val maxWpm = sessions.maxOfOrNull { it.wpm }?.toFloat() ?: 100f

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(16.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val stepX = width / (sessions.size - 1).coerceAtLeast(1)

            val points = sessions.mapIndexed { index, session ->
                val x = index * stepX
                val y = height - ((session.wpm / maxWpm) * height)
                Offset(x, y)
            }

            for (i in 0 until points.size - 1) {
                drawLine(
                    color = primaryColor,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Draw points
             points.forEach { point ->
                drawCircle(
                    color = primaryColor,
                    center = point,
                    radius = 4.dp.toPx()
                )
            }
        }
    }
}

fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) String.format("%dh %02dm", hrs, mins) else String.format("%02dm %02ds", mins, secs)
}
