package com.example.fossrsvp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.fossrsvp.PremiumRadius
import com.example.fossrsvp.Quiz
import com.example.fossrsvp.QuizQuestion

@Composable
fun QuizDialog(
    quiz: Quiz,
    onDismiss: () -> Unit
) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableStateOf(0) }
    var showResult by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PremiumRadius),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            if (showResult) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Quiz Completed!", style = MaterialTheme.typography.headlineMedium)
                    Text("Your Score: $score / ${quiz.questions.size}", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Close")
                    }
                }
            } else {
                val question = quiz.questions.getOrNull(currentQuestionIndex)
                if (question != null) {
                    QuizQuestionView(
                        question = question,
                        selectedOption = selectedOptionIndex,
                        onOptionSelected = { selectedOptionIndex = it },
                        onNext = {
                            if (selectedOptionIndex == question.correctOptionIndex) {
                                score++
                            }
                            if (currentQuestionIndex < quiz.questions.size - 1) {
                                currentQuestionIndex++
                                selectedOptionIndex = null
                            } else {
                                showResult = true
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuizQuestionView(
    question: QuizQuestion,
    selectedOption: Int?,
    onOptionSelected: (Int) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Question", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        Text(question.question, style = MaterialTheme.typography.titleMedium)

        HorizontalDivider()

        question.options.forEachIndexed { index, option ->
            val isSelected = selectedOption == index
            val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh

            Surface(
                onClick = { if (selectedOption == null) onOptionSelected(index) },
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { if (selectedOption == null) onOptionSelected(index) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(option, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Button(
            onClick = onNext,
            enabled = selectedOption != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
        }
    }
}
