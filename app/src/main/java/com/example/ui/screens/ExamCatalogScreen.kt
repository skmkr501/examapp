package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleData
import com.example.model.Exam
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TealAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamCatalogScreen(
    exams: List<Exam>,
    onSelectExam: (Exam) -> Unit,
    onBack: () -> Unit
) {
    var selectedClass by remember { mutableStateOf<Int?>(null) }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredExams = exams.filter { exam ->
        val matchesClass = selectedClass == null || exam.classLevel == selectedClass
        val matchesSubj = selectedSubject == null || exam.subject.contains(selectedSubject!!, ignoreCase = true)
        val matchesQuery = searchQuery.isBlank() || exam.title.contains(searchQuery, ignoreCase = true) || exam.subject.contains(searchQuery, ignoreCase = true)
        matchesClass && matchesSubj && matchesQuery
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📚 শ্রেণি, বিষয় ও পরীক্ষা", fontWeight = FontWeight.Bold) },
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
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("exam_search_input"),
                placeholder = { Text("পরীক্ষার নাম বা বিষয় খুঁজুন…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Class selection chip row
            Text(
                "শ্রেণি নির্বাচন",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedClass == null,
                        onClick = { selectedClass = null },
                        label = { Text("সব শ্রেণি") }
                    )
                }
                items(SampleData.classLabels.keys.toList().sorted()) { c ->
                    FilterChip(
                        selected = selectedClass == c,
                        onClick = { selectedClass = if (selectedClass == c) null else c },
                        label = { Text(SampleData.classLabels[c] ?: "শ্রেণি $c") }
                    )
                }
            }

            // Subject selection chip row
            Text(
                "বিষয় নির্বাচন",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedSubject == null,
                        onClick = { selectedSubject = null },
                        label = { Text("সব বিষয়") }
                    )
                }
                items(SampleData.subjectsList) { s ->
                    FilterChip(
                        selected = selectedSubject == s,
                        onClick = { selectedSubject = if (selectedSubject == s) null else s },
                        label = { Text(s) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Exams list
            if (filteredExams.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "এই ক্যাটাগরিতে কোনো পরীক্ষা পাওয়া যায়নি।\nঅন্য কোনো শ্রেণি বা বিষয় নির্বাচন করে দেখুন।",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredExams) { exam ->
                        ExamCardItem(exam = exam, onStart = { onSelectExam(exam) })
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun ExamCardItem(
    exam: Exam,
    onStart: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = PurpleAccent.copy(alpha = 0.12f)
                ) {
                    Text(
                        exam.subject,
                        color = PurpleAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                if (exam.chapter.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            exam.chapter,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = exam.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "⏱ ${exam.durationMinutes} মিনিট",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "📝 ${exam.questions.size} টি প্রশ্ন",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (exam.negMark > 0f) {
                    Text(
                        "➖ নেগেটিভ ${exam.negMark}",
                        fontSize = 12.sp,
                        color = PinkAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onStart,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_exam_${exam.id}"),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
            ) {
                Text("পরীক্ষা শুরু করুন 🚀", fontWeight = FontWeight.Bold)
            }
        }
    }
}
