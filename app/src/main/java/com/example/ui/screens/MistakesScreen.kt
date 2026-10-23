package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleData
import com.example.model.ExamResult
import com.example.model.Question
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakesScreen(
    pastResults: List<ExamResult>,
    onBack: () -> Unit
) {
    // Collect all unique missed questions across all past exam results
    val mistakeQuestions = remember(pastResults) {
        val allQuestionsMap = SampleData.defaultExams.flatMap { it.questions }.associateBy { it.id }
        val missed = mutableListOf<Pair<Question, Int>>() // Pair of Question and user's wrong answer index

        pastResults.forEach { result ->
            result.userAnswers.forEach { (qId, chosenIndex) ->
                val q = allQuestionsMap[qId]
                if (q != null && chosenIndex != q.correctIndex) {
                    if (missed.none { it.first.id == q.id }) {
                        missed.add(q to chosenIndex)
                    }
                }
            }
        }
        missed
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📕 ভুলের খাতা", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PinkAccent.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "পরীক্ষায় যে প্রশ্নগুলো ভুল হয়েছিল, সেগুলো এখানে স্বয়ংক্রিয়ভাবে জমা থাকে। নিয়মিত পর্যালোচনা করে প্রস্তুতি নিখুঁত করো!",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (mistakeQuestions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "ভুলের খাতায় কোনো প্রশ্ন নেই!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "পরীক্ষা দাও, ভুল হলে এখানে এসে আবার চর্চা করতে পারবে।",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    "ভুল হওয়া প্রশ্নাবলি (${mistakeQuestions.size}টি):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(mistakeQuestions) { (question, userWrongIndex) ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    question.body,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SuccessGreen.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val rightText = question.options.getOrNull(question.correctIndex) ?: ""
                                    Text(
                                        "✓ সঠিক উত্তর: $rightText",
                                        fontWeight = FontWeight.SemiBold,
                                        color = SuccessGreen,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ErrorRed.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val wrongText = question.options.getOrNull(userWrongIndex) ?: "স্কিপ করা হয়েছিল"
                                    Text(
                                        "✗ তোমার দাগানো: $wrongText",
                                        color = ErrorRed,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }

                                if (question.explanation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "💡 ব্যাখ্যা: ${question.explanation}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
