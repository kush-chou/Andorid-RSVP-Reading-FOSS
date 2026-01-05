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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun QuizDialog(
    quiz: Quiz,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (quiz.questions.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No questions generated.", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            } else {
                QuizContent(quiz, onDismiss)
            }
        }
    }
}

@Composable
fun QuizContent(quiz: Quiz, onFinish: () -> Unit) {
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var showResult by remember { mutableStateOf(false) }

    if (showResult) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Quiz Finished!", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your Score: $score / ${quiz.questions.size}", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onFinish) {
                Text("Close")
            }
        }
    } else {
        val question = quiz.questions[currentQuestionIndex]
        var selectedOption by remember { mutableIntStateOf(-1) }
        var isAnswered by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Question ${currentQuestionIndex + 1} / ${quiz.questions.size}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = question.text,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            question.options.forEachIndexed { index, option ->
                val isSelected = selectedOption == index
                val isCorrect = index == question.correctOptionIndex

                val backgroundColor = if (isAnswered) {
                    if (isCorrect) Color(0xFF4CAF50).copy(alpha = 0.2f)
                    else if (isSelected) Color(0xFFF44336).copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceContainer
                } else {
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                }

                val borderColor = if (isAnswered) {
                     if (isCorrect) Color(0xFF4CAF50)
                     else if (isSelected) Color(0xFFF44336)
                     else Color.Transparent
                } else {
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(backgroundColor)
                        .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable(enabled = !isAnswered) {
                            selectedOption = index
                        }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (isAnswered) {
                            if (isCorrect) Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50))
                            if (isSelected && !isCorrect) Icon(Icons.Default.Close, null, tint = Color(0xFFF44336))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (!isAnswered) {
                        isAnswered = true
                        if (selectedOption == question.correctOptionIndex) {
                            score++
                        }
                    } else {
                        if (currentQuestionIndex < quiz.questions.size - 1) {
                            currentQuestionIndex++
                            selectedOption = -1
                            isAnswered = false
                        } else {
                            showResult = true
                        }
                    }
                },
                enabled = selectedOption != -1,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(if (isAnswered) (if (currentQuestionIndex < quiz.questions.size - 1) "Next Question" else "Finish Quiz") else "Submit Answer")
            }
        }
    }
}
