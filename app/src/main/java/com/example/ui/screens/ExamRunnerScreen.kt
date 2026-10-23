package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Exam
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamRunnerScreen(
    exam: Exam,
    currentIndex: Int,
    userAnswers: Map<Long, Int>,
    secondsRemaining: Int,
    onSelectOption: (Long, Int) -> Unit,
    onGoToIndex: (Int) -> Unit,
    onSubmitExam: () -> Unit,
    onExit: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }

    BackHandler {
        showExitDialog = true
    }

    val currentQuestion = exam.questions.getOrNull(currentIndex)
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timerText = String.format("%02d:%02d", minutes, seconds)
    val isTimerLow = secondsRemaining in 1..60

    val timerBgColor by animateColorAsState(
        targetValue = if (isTimerLow) ErrorRed else PurpleAccent,
        label = "timer_color"
    )

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("পরীক্ষা বাতিল করবেন?") },
            text = { Text("পরীক্ষা থেকে বের হয়ে গেলে বর্তমান অগ্রগতি সংরক্ষিত হবে না।") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onExit()
                }) {
                    Text("হ্যাঁ, বের হব", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("চালিয়ে যান")
                }
            }
        )
    }

    if (showSubmitDialog) {
        val answeredCount = userAnswers.size
        val unanswered = exam.questions.size - answeredCount
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("পরীক্ষা জমা দিন") },
            text = {
                Text(
                    if (unanswered > 0)
                        "আপনার $unanswered টি প্রশ্নের উত্তর বাকি রয়েছে। আপনি কি নিশ্চিত যে এখন জমা দিতে চান?"
                    else
                        "আপনি সবগুলো প্রশ্নের উত্তর দিয়েছেন। এখন পরীক্ষা জমা দিতে চান?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        onSubmitExam()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
                    Text("জমা দিন ✅")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        exam.title,
                        maxLines = 1,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = timerBgColor,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "⏱ $timerText",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Answered Progress Bar
            val progress = if (exam.questions.isNotEmpty()) {
                userAnswers.size.toFloat() / exam.questions.size.toFloat()
            } else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = PurpleAccent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Content Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (currentQuestion != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "প্রশ্ন ${currentIndex + 1} / ${exam.questions.size}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "[${currentQuestion.marks} নম্বর]",
                            fontSize = 12.sp,
                            color = PurpleAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = currentQuestion.body,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "সতর্কতা: একবার অপশন বেছে নিলে আর বদলানো যাবে না।",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Options list
                    val selectedOption = userAnswers[currentQuestion.id]
                    val prefixes = listOf("ক", "খ", "গ", "ঘ", "ঙ", "চ")

                    currentQuestion.options.forEachIndexed { optIndex, optText ->
                        val isSelected = selectedOption == optIndex
                        val hasAnsweredThis = selectedOption != null

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PurpleAccent.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = !hasAnsweredThis) {
                                    onSelectOption(currentQuestion.id, optIndex)
                                }
                                .testTag("opt_${currentQuestion.id}_$optIndex")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = prefixes.getOrElse(optIndex) { "${optIndex + 1}" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = optText,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Question navigator dots row
                    Text(
                        "প্রশ্নাবলি সূচক:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(exam.questions) { qIdx, qItem ->
                            val answered = userAnswers.containsKey(qItem.id)
                            val isCurrent = qIdx == currentIndex

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isCurrent -> PinkAccent
                                            answered -> TealAccent
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                    .clickable { onGoToIndex(qIdx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${qIdx + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent || answered) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation & Submit Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onGoToIndex(currentIndex - 1) },
                        enabled = currentIndex > 0,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("আগের")
                    }

                    Button(
                        onClick = { showSubmitDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("submit_exam_button")
                    ) {
                        Text("জমা দিন", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onGoToIndex(currentIndex + 1) },
                        enabled = currentIndex < exam.questions.size - 1,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("পরের")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
