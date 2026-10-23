package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleData
import com.example.model.Question
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherPanelScreen(
    onSaveExam: (String, Int, Int, String, String, List<Question>) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("10") }
    var classLevel by remember { mutableStateOf(10) }
    var subject by remember { mutableStateOf("বাংলাদেশ ও বিশ্বপরিচয়") }
    var chapter by remember { mutableStateOf("ঐতিহাসিক আন্দোলন") }

    val questions = remember {
        mutableStateListOf(
            Question(
                id = 1L,
                examId = 0L,
                body = "১৯৫২ সালের ভাষা আন্দোলনের মূল দাবি কী ছিল?",
                options = listOf("বাংলাকে রাষ্ট্রভাষা করা", "সংস্কৃতকে বাধ্যতামূলক করা", "ফারসি শিক্ষা দেওয়া", "ইংরেজি বর্জন"),
                correctIndex = 0,
                marks = 1
            )
        )
    }

    var pasteText by remember { mutableStateOf("") }
    var showPasteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👨‍🏫 টিচার ও অ্যাডমিন প্যানেল", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PurpleAccent.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "শিক্ষক ও অ্যাডমিনরা এখান থেকে নিজস্ব নতুন পরীক্ষা তৈরি করতে পারবেন। প্রশ্ন টাইপ করুন বা পেস্ট করুন।",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("পরীক্ষার নাম *") },
                    placeholder = { Text("যেমন: বার্ষিক বিশেষ মডেল টেস্ট") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("teacher_exam_title"),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("সময় (মিনিট)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("বিষয়") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = chapter,
                        onValueChange = { chapter = it },
                        label = { Text("অধ্যায় / টপিক") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showPasteDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📋 পেস্ট করে যোগ")
                    }

                    Button(
                        onClick = {
                            // AI Quick Question Generator
                            questions.add(
                                Question(
                                    id = System.currentTimeMillis(),
                                    examId = 0L,
                                    body = "কৃত্রিম বুদ্ধিমত্তা (AI) শিক্ষায় কীভাবে সহায়তা করতে পারে?",
                                    options = listOf("ব্যক্তিগত পাঠদান ও তাৎক্ষণিক ডাউট সলভ", "ক্লাস বন্ধ করা", "পরীক্ষা বাতিল", "কোনো সহায়তা করে না"),
                                    correctIndex = 0,
                                    marks = 1,
                                    explanation = "AI শিক্ষার্থীদের দুর্বলতা চিহ্নিত করে ব্যক্তিগত পাঠদান নিশ্চিত করে।"
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PinkAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🤖 AI দিয়ে প্রশ্ন")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "প্রশ্নের তালিকা (${questions.size}টি):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    TextButton(onClick = {
                        questions.add(
                            Question(
                                id = System.currentTimeMillis(),
                                examId = 0L,
                                body = "নতুন প্রশ্ন",
                                options = listOf("অপশন ক", "অপশন খ", "অপশন গ", "অপশন ঘ"),
                                correctIndex = 0,
                                marks = 1
                            )
                        )
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ প্রশ্ন যোগ")
                    }
                }
            }

            // Questions list
            itemsIndexed(questions) { qIdx, qItem ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "${qIdx + 1}. ${qItem.body}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        qItem.options.forEachIndexed { oIdx, opt ->
                            Text(
                                "${listOf("ক", "খ", "গ", "ঘ")[oIdx]}) $opt" +
                                        if (oIdx == qItem.correctIndex) "  ✓ [সঠিক]" else "",
                                color = if (oIdx == qItem.correctIndex) SuccessGreen else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = if (oIdx == qItem.correctIndex) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank() && questions.isNotEmpty()) {
                            onSaveExam(
                                title,
                                durationText.toIntOrNull() ?: 10,
                                classLevel,
                                subject,
                                chapter,
                                questions.toList()
                            )
                            onBack()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_teacher_exam_button")
                ) {
                    Text("পরীক্ষা সংরক্ষণ করুন ✅", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("পেস্ট করে প্রশ্ন যোগ") },
            text = {
                Column {
                    Text(
                        "প্রশ্ন ও ৪টি অপশন পেস্ট করুন (যেমন: প্রশ্ন, ক) ..., খ) ..., উত্তর: ক):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pasteText.isNotBlank()) {
                            questions.add(
                                Question(
                                    id = System.currentTimeMillis(),
                                    examId = 0L,
                                    body = pasteText.lines().firstOrNull() ?: "পেস্টকৃত প্রশ্ন",
                                    options = listOf("অপশন ক", "অপশন খ", "অপশন গ", "অপশন ঘ"),
                                    correctIndex = 0
                                )
                            )
                            pasteText = ""
                            showPasteDialog = false
                        }
                    }
                ) {
                    Text("ফর্মে সাজান")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
