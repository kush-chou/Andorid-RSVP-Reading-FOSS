package com.example.fossrsvp

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun QuizDialog(
    apiKey: String,
    textContext: String,
    onDismiss: () -> Unit
) {
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var showResults by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        questions = generateQuiz(apiKey, textContext)
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Generating Quiz from your reading...")
                    }
                } else if (questions.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Could not generate questions. Ensure you have a valid API Key and enough text.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onDismiss) { Text("Close") }
                    }
                } else if (showResults) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Quiz Complete!", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("You scored $score out of ${questions.size}", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = onDismiss) { Text("Finish") }
                    }
                } else {
                    QuizQuestionView(
                        question = questions[currentQuestionIndex],
                        questionIndex = currentQuestionIndex,
                        totalQuestions = questions.size,
                        onAnswer = { isCorrect ->
                            if (isCorrect) score++
                        },
                        onNext = {
                            if (currentQuestionIndex < questions.size - 1) {
                                currentQuestionIndex++
                            } else {
                                showResults = true
                            }
                        }
                    )
                }

                // Close button at top right
                if (!showResults) {
                    androidx.compose.material3.IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }
        }
    }
}

@Composable
fun QuizQuestionView(
    question: QuizQuestion,
    questionIndex: Int,
    totalQuestions: Int,
    onAnswer: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    var selectedOption by remember(question) { mutableIntStateOf(-1) }
    var isAnswered by remember(question) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Question ${questionIndex + 1} of $totalQuestions",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            question.question,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        question.options.forEachIndexed { index, option ->
            val isSelected = selectedOption == index
            val isCorrect = index == question.correctIndex

            val backgroundColor = if (isAnswered) {
                if (isCorrect) Color(0xFFE8F5E9) // Light Green
                else if (isSelected) Color(0xFFFFEBEE) // Light Red
                else MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
            }

            val borderColor = if (isAnswered) {
                if (isCorrect) Color(0xFF4CAF50)
                else if (isSelected) Color(0xFFF44336)
                else Color.Transparent
            } else {
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
                    .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                    .clickable(enabled = !isAnswered) {
                        selectedOption = index
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                        .background(if (isSelected || (isAnswered && isCorrect)) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAnswered) {
                        if (isCorrect) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        else if (isSelected) Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else if (isSelected) {
                        Box(modifier = Modifier.size(10.dp).background(Color.White, CircleShape))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(option, style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (isAnswered) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text("Explanation: ${question.explanation}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(if (questionIndex < totalQuestions - 1) "Next Question" else "See Results")
            }
        } else {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    isAnswered = true
                    onAnswer(selectedOption == question.correctIndex)
                },
                enabled = selectedOption != -1,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Check Answer")
            }
        }
    }
}
