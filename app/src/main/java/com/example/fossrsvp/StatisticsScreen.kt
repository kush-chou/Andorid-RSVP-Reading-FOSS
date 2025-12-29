package com.example.fossrsvp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    books: List<Book>,
    onBack: () -> Unit
) {
    val allSessions = books.flatMap { it.sessions }
    val totalWordsRead = allSessions.sumOf { it.wordsRead }
    val totalTimeSeconds = allSessions.sumOf { (it.endTime - it.startTime) / 1000 }
    val averageWpm = if (allSessions.isNotEmpty()) {
        allSessions.sumOf { it.wpm } / allSessions.size
    } else 0

    val totalHours = totalTimeSeconds / 3600
    val totalMinutes = (totalTimeSeconds % 3600) / 60

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Global Stats", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Words Read", "$totalWordsRead", modifier = Modifier.weight(1f))
                    StatCard("Time Read", "${totalHours}h ${totalMinutes}m", modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                StatCard("Average Speed", "$averageWpm WPM", modifier = Modifier.fillMaxWidth())
            }

            item {
                Text("Recent Activity (Last 7 Days)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                ActivityChart(allSessions)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ActivityChart(sessions: List<ReadingSession>) {
    // Group sessions by day
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault()) // Mon, Tue...

    // We want last 7 days including today
    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L

    val dailyWords = (0..6).map { offset ->
        val targetDayStart = now - (offset * oneDayMs)
        // Normalize to start of day? For simplicity, we just check if session timestamp is within the 24h window
        // Better: Calendar. But for quick impl, simple range check.
        // Actually, let's use SimpleDateFormat to group by date string
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(targetDayStart))

        val wordsForDay = sessions.filter {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.startTime)) == dateStr
        }.sumOf { it.wordsRead }

        dateStr to wordsForDay
    }.reversed()

    val maxWords = dailyWords.maxOfOrNull { it.second } ?: 1

    ElevatedCard(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = size.width / (dailyWords.size * 1.5f)
                val spacing = size.width / dailyWords.size
                val maxHeight = size.height - 40f // leave room for text

                dailyWords.forEachIndexed { index, (dateStr, words) ->
                    val height = if (maxWords > 0) (words.toFloat() / maxWords) * maxHeight else 0f
                    val x = index * spacing + 20f
                    val y = size.height - height - 20f

                    drawRect(
                        color = Color(0xFF6200EE), // Primary color
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, height)
                    )

                    // Draw Label (Day Name)
                    // We need text measuring but for Canvas it's tricky without native paint.
                    // Simplified: We skip text in Canvas and use Row of Text below?
                    // Actually, let's just draw rectangles.
                }

                // Draw base line
                drawLine(
                    color = Color.Gray,
                    start = Offset(0f, size.height - 20f),
                    end = Offset(size.width, size.height - 20f),
                    strokeWidth = 2f
                )
            }
        }
    }

    // Legends
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        dailyWords.forEach { (dateStr, _) ->
            // Parse back to get Day Name
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
            val dayName = dayFormat.format(date!!)
            Text(dayName, style = MaterialTheme.typography.labelSmall)
        }
    }
}
