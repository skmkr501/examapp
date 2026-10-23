package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.model.ChatMessage
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PurpleAccent
import com.example.viewmodel.AiTutorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTutorScreen(
    tutorViewModel: AiTutorViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Chat, 1: Doubt Solver
    val messages by tutorViewModel.messages.collectAsState()
    val isThinking by tutorViewModel.isThinking.collectAsState()
    val doubtSolution by tutorViewModel.doubtSolution.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🤖 AI মশাই (AI শিক্ষক)", fontWeight = FontWeight.Bold)
                    }
                },
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
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("💬 আড্ডা ও প্রশ্ন") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("❓ ডাউট সলভ") }
                )
            }

            if (selectedTab == 0) {
                // Quick suggested question chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = { textInput = "সালোকসংশ্লেষণ সহজে বুঝিয়ে দাও" },
                        label = { Text("🌿 সালোকসংশ্লেষণ", fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = { textInput = "পিথাগোরাসের সূত্রটি কী?" },
                        label = { Text("📐 পিথাগোরাস", fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = { textInput = "পড়া মনে রাখার কৌশল" },
                        label = { Text("💡 পড়ার টিপস", fontSize = 11.sp) }
                    )
                }

                // Chat Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubbleItem(message = msg)
                    }

                    if (isThinking) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("মশাই ভাবছেন…", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Message input bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("মশাইকে যেকোনো প্রশ্ন করো…") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tutor_input"),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 3
                        )

                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    tutorViewModel.sendMessage(textInput)
                                    textInput = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = PurpleAccent,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("send_tutor_message")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            } else {
                // Doubt solver tab
                DoubtSolverTab(
                    onSolve = { subj, qText -> tutorViewModel.solveDoubt(subj, qText) },
                    solution = doubtSolution,
                    isThinking = isThinking
                )
            }
        }
    }
}

@Composable
fun ChatBubbleItem(message: ChatMessage) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 2.dp,
                bottomEnd = if (message.isUser) 2.dp else 16.dp
            ),
            color = if (message.isUser) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.text,
                color = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun DoubtSolverTab(
    onSolve: (String, String) -> Unit,
    solution: String?,
    isThinking: Boolean
) {
    var subject by remember { mutableStateOf("সাধারণ বিজ্ঞান") }
    var doubtText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "যেকোনো জটিল গাণিতিক সমস্যা বা বিজ্ঞানের প্রশ্ন লিখে সমাধান নাও:",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("বিষয়") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = doubtText,
                onValueChange = { doubtText = it },
                label = { Text("তোমার সমস্যা বা প্রশ্নটি বিস্তারিত লেখো…") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Button(
                onClick = { onSolve(subject, doubtText) },
                enabled = doubtText.isNotBlank() && !isThinking,
                colors = ButtonDefaults.buttonColors(containerColor = PinkAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isThinking) "সমাধান তৈরি হচ্ছে…" else "🔍 সমাধান বের করো", fontWeight = FontWeight.Bold)
            }
        }

        if (solution != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "ধাপ অনুযায়ী সমাধান:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PurpleAccent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = solution,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
