package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleData
import com.example.model.ExamResult
import com.example.model.Question
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamResultScreen(
    result: ExamResult,
    onHome: () -> Unit,
    onExplainMistakes: () -> Unit
) {
    val context = LocalContext.current
    var showReview by remember { mutableStateOf(false) }

    val percentage = if (result.totalMarks > 0) {
        (result.score / result.totalMarks.toFloat()) * 100f
    } else 0f

    var animatedProgress by remember { mutableStateOf(0f) }
    val progressAnimation by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 1200),
        label = "score_ring"
    )

    LaunchedEffect(Unit) {
        animatedProgress = percentage / 100f
    }

    // Find the exam questions for review
    val exam = SampleData.defaultExams.find { it.id == result.examId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎯 আপনার ফলাফল", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = result.examTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            // Circular Score Ring
            item {
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = BorderLine,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = if (percentage >= 80f) TealAccent else if (percentage >= 50f) PurpleAccent else PinkAccent,
                            startAngle = -90f,
                            sweepAngle = progressAnimation * 360f,
                            useCenter = false,
                            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${percentage.toInt()}%",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (percentage >= 80f) "অসাধারণ! 🎉" else if (percentage >= 50f) "ভালো করেছ! 👍" else "অনুশীলন চলুক! 💪",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Stats breakdown grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResultStatBox(
                        title = "প্রাপ্ত নম্বর",
                        value = "${result.score} / ${result.totalMarks}",
                        color = PurpleAccent,
                        modifier = Modifier.weight(1f)
                    )
                    ResultStatBox(
                        title = "সঠিক উত্তর",
                        value = "${result.correctCount}",
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    ResultStatBox(
                        title = "ভুল উত্তর",
                        value = "${result.wrongCount}",
                        color = ErrorRed,
                        modifier = Modifier.weight(1f)
                    )
                    ResultStatBox(
                        title = "স্কিপ",
                        value = "${result.skipCount}",
                        color = GoldAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Rewards card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "+${(result.score * 10).toInt() + 20} XP",
                                fontWeight = FontWeight.Bold,
                                color = PurpleAccent,
                                fontSize = 16.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🪙", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "+${maxOf(5, (result.score * 2).toInt())} কয়েন",
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (result.wrongCount > 0) {
                        Button(
                            onClick = onExplainMistakes,
                            colors = ButtonDefaults.buttonColors(containerColor = PinkAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("explain_mistakes_button")
                        ) {
                            Text("🧠 ভুল উত্তর বুঝিয়ে দাও (AI মশাই)", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { showReview = !showReview },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_review_button")
                    ) {
                        Text(if (showReview) "উত্তরপত্র লুকান" else "📄 উত্তরপত্র পর্যালোচনা")
                    }

                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Exam Site-এ আমি '${result.examTitle}' পরীক্ষায় ${result.score}/${result.totalMarks} (${percentage.toInt()}%) পেয়েছি! 🚀"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "ফলাফল শেয়ার করুন"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📤 ফলাফল শেয়ার করুন")
                    }

                    Button(
                        onClick = onHome,
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_from_result_button")
                    ) {
                        Text("হোমে ফিরে যান")
                    }
                }
            }

            // Question Review Section
            if (showReview && exam != null) {
                item {
                    Text(
                        "উত্তরপত্র বিশ্লেষণ:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                }

                itemsIndexed(exam.questions) { idx, q ->
                    val chosen = result.userAnswers[q.id]
                    val isCorrect = chosen == q.correctIndex
                    val isSkipped = chosen == null

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCorrect) SuccessGreen.copy(alpha = 0.5f)
                            else if (isSkipped) GoldAccent.copy(alpha = 0.5f)
                            else ErrorRed.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${idx + 1}. ${q.body}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    if (isCorrect) "✅ সঠিক" else if (isSkipped) "⚠️ স্কিপ" else "❌ ভুল",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isCorrect) SuccessGreen else if (isSkipped) GoldAccent else ErrorRed
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            q.options.forEachIndexed { oIdx, opt ->
                                val isRightAnswer = oIdx == q.correctIndex
                                val isUserChoice = oIdx == chosen

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        isRightAnswer -> SuccessGreen.copy(alpha = 0.15f)
                                        isUserChoice -> ErrorRed.copy(alpha = 0.15f)
                                        else -> Color.Transparent
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${listOf("ক", "খ", "গ", "ঘ")[oIdx]}) $opt" +
                                                (if (isRightAnswer) "  ✓ সঠিক উত্তর" else "") +
                                                (if (isUserChoice && !isRightAnswer) "  ✗ আপনার উত্তর" else ""),
                                        fontSize = 13.sp,
                                        fontWeight = if (isRightAnswer || isUserChoice) FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = if (isRightAnswer) SuccessGreen else if (isUserChoice) ErrorRed else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (q.explanation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "💡 ব্যাখ্যা: ${q.explanation}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun ResultStatBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            Text(title, fontSize = 10.5.sp, color = color, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
