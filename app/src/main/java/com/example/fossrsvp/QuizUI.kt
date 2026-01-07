package com.example.fossrsvp

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizUI(
    quiz: Quiz,
    onClose: () -> Unit
) {
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var isQuizFinished by remember { mutableStateOf(false) }
    var showExplanation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isQuizFinished) "Quiz Results" else quiz.title) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Close Quiz")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (isQuizFinished) {
                QuizResults(
                    score = score,
                    totalQuestions = quiz.questions.size,
                    onClose = onClose,
                    onRetry = {
                        currentQuestionIndex = 0
                        selectedOptionIndex = null
                        score = 0
                        isQuizFinished = false
                        showExplanation = false
                    }
                )
            } else {
                val question = quiz.questions[currentQuestionIndex]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { (currentQuestionIndex + 1).toFloat() / quiz.questions.size },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .padding(bottom = 16.dp),
                        )

                        Text(
                            text = "Question ${currentQuestionIndex + 1} of ${quiz.questions.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = question.text,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        question.options.forEachIndexed { index, option ->
                            val isSelected = selectedOptionIndex == index
                            val isCorrect = index == question.correctOptionIndex

                            val cardColor = if (showExplanation) {
                                when {
                                    isCorrect -> Color(0xFFE8F5E9) // Light Green
                                    isSelected && !isCorrect -> Color(0xFFFFEBEE) // Light Red
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            } else {
                                if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            }

                            val borderColor = if (showExplanation && isCorrect) Color(0xFF4CAF50) else Color.Transparent

                            Card(
                                onClick = { if (!showExplanation) selectedOptionIndex = index },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = cardColor),
                                border = if (showExplanation && isCorrect) androidx.compose.foundation.BorderStroke(2.dp, borderColor) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null, // Handled by Card
                                        enabled = !showExplanation
                                    )
                                    Text(
                                        text = option,
                                        modifier = Modifier.padding(start = 8.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (showExplanation) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (isCorrect) {
                                            Icon(Icons.Default.Check, contentDescription = "Correct", tint = Color(0xFF4CAF50))
                                        } else if (isSelected) {
                                            Icon(Icons.Default.Close, contentDescription = "Incorrect", tint = Color(0xFFF44336))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (showExplanation) {
                                // Next Question
                                if (currentQuestionIndex < quiz.questions.size - 1) {
                                    currentQuestionIndex++
                                    selectedOptionIndex = null
                                    showExplanation = false
                                } else {
                                    isQuizFinished = true
                                }
                            } else {
                                // Check Answer
                                if (selectedOptionIndex != null) {
                                    if (selectedOptionIndex == question.correctOptionIndex) {
                                        score++
                                    }
                                    showExplanation = true
                                }
                            }
                        },
                        enabled = selectedOptionIndex != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .height(56.dp)
                    ) {
                        Text(if (showExplanation) (if (currentQuestionIndex < quiz.questions.size - 1) "Next Question" else "Finish Quiz") else "Check Answer")
                    }
                }
            }
        }
    }
}

@Composable
fun QuizResults(
    score: Int,
    totalQuestions: Int,
    onClose: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Quiz Completed!",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "You scored $score out of $totalQuestions",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Text("Retry Quiz")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Text("Back to Reader")
        }
    }
}
