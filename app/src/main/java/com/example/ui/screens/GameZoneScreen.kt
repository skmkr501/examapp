package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleData
import com.example.model.GameType
import com.example.ui.theme.*
import com.example.viewmodel.GamesViewModel
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameZoneScreen(
    gamesViewModel: GamesViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Spin, 1: Math Blitz, 2: Memory Match
    val isSpinning by gamesViewModel.isSpinning.collectAsState()
    val spinPrize by gamesViewModel.spinPrize.collectAsState()
    val spinAngle by gamesViewModel.spinAngle.collectAsState()

    val mathActive by gamesViewModel.mathActive.collectAsState()
    val mathProblem by gamesViewModel.mathProblem.collectAsState()
    val mathScore by gamesViewModel.mathScore.collectAsState()
    val mathTime by gamesViewModel.mathTimeRemaining.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎮 গেম জোন ও স্পিন", fontWeight = FontWeight.Bold) },
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
                    text = { Text("🎡 দৈনিক স্পিন") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🧮 ম্যাথ ব্লিটজ") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("🃏 কার্ড মেলানো") }
                )
            }

            when (selectedTab) {
                0 -> SpinWheelTab(
                    isSpinning = isSpinning,
                    spinAngle = spinAngle,
                    spinPrize = spinPrize,
                    onSpin = { gamesViewModel.spinWheel() }
                )
                1 -> MathBlitzTab(
                    mathActive = mathActive,
                    mathProblem = mathProblem,
                    mathScore = mathScore,
                    mathTime = mathTime,
                    onStart = { gamesViewModel.startMathBlitz() },
                    onSubmitAnswer = { gamesViewModel.submitMathAnswer(it) }
                )
                2 -> MemoryMatchTab(
                    gamesViewModel = gamesViewModel
                )
            }
        }
    }
}

@Composable
fun SpinWheelTab(
    isSpinning: Boolean,
    spinAngle: Float,
    spinPrize: com.example.model.SpinPrize?,
    onSpin: () -> Unit
) {
    val prizes = SampleData.spinPrizes
    val animatedAngle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(spinAngle) {
        if (spinAngle > 0f) {
            animatedAngle.animateTo(
                targetValue = spinAngle,
                animationSpec = tween(durationMillis = 2800, easing = FastOutSlowInEasing)
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "প্রতিদিন চাকা ঘুরিয়ে কয়েন ও বোনাস XP অর্জন করো!",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Spin wheel canvas
        item {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pointer arrow at the top
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-14).dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PinkAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▼", color = Color.White, fontSize = 14.sp)
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(animatedAngle.value)
                ) {
                    val colors = listOf(PurpleAccent, PinkAccent, GoldAccent, TealAccent, BlueAccent, Color(0xFFEF4444))
                    val sweep = 360f / prizes.size
                    val radius = size.minDimension / 2

                    for (i in prizes.indices) {
                        drawArc(
                            color = colors[i % colors.size],
                            startAngle = i * sweep - 90f,
                            sweepAngle = sweep,
                            useCenter = true
                        )
                    }

                    // Inner circle
                    drawCircle(
                        color = Color.White,
                        radius = radius * 0.28f,
                        center = center
                    )
                }

                Text("🎁", fontSize = 28.sp)
            }
        }

        // Prize result announcement
        item {
            if (spinPrize != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GoldAccent.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(spinPrize.emoji, fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "অভিনন্দন! আপনি পেয়েছেন ${spinPrize.label}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Spin CTA Button
        item {
            Button(
                onClick = onSpin,
                enabled = !isSpinning,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(50.dp)
                    .testTag("spin_button")
            ) {
                Text(
                    if (isSpinning) "ঘুরছে..." else "🎡 স্পিন ঘোরান",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MathBlitzTab(
    mathActive: Boolean,
    mathProblem: String,
    mathScore: Int,
    mathTime: Int,
    onStart: () -> Unit,
    onSubmitAnswer: (Int) -> Unit
) {
    var inputAnswer by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!mathActive) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🧮", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "ম্যাথ ব্লিটজ (Math Blitz)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "৩০ সেকেন্ডে যত বেশি সম্ভব অঙ্কের সঠিক উত্তর দাও! প্রতিটি সঠিক উত্তরে অতিরিক্ত কয়েন ও XP অর্জিত হবে।",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onStart,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        modifier = Modifier.testTag("start_math_blitz")
                    ) {
                        Text("খেলা শুরু করো ⏱", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⏱ সময় বাকি: $mathTime সে.", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("স্কোর: $mathScore", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PurpleAccent.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$mathProblem = ?",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (inputAnswer.isEmpty()) "উত্তর লিখুন" else inputAnswer,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Keypad 1-9
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    listOf(1, 2, 3),
                    listOf(4, 5, 6),
                    listOf(7, 8, 9)
                ).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { num ->
                            Button(
                                onClick = {
                                    inputAnswer += num
                                    inputAnswer.toIntOrNull()?.let { onSubmitAnswer(it) }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .size(70.dp)
                                    .testTag("key_$num")
                            ) {
                                Text("$num", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { inputAnswer = "" },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        modifier = Modifier.size(70.dp)
                    ) {
                        Text("C", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            inputAnswer += "0"
                            inputAnswer.toIntOrNull()?.let { onSubmitAnswer(it) }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(70.dp)
                    ) {
                        Text("0", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            inputAnswer.toIntOrNull()?.let {
                                onSubmitAnswer(it)
                                inputAnswer = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier.size(70.dp)
                    ) {
                        Text("✔", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryMatchTab(
    gamesViewModel: GamesViewModel
) {
    val score by gamesViewModel.memoryScore.collectAsState()
    val pairs = gamesViewModel.memoryPairs

    var selectedLeft by remember { mutableStateOf<String?>(null) }
    var matchedPairs by remember { mutableStateOf<Set<Int>>(emptySet()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "জোড়া মেলাও খেলা",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "বাম পাশের শব্দের সাথে ডান পাশের সঠিক বাংলা প্রতিশব্দ মেলাও!",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text("সফল মিল: $score", fontWeight = FontWeight.Bold, color = SuccessGreen, fontSize = 16.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left column
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pairs.forEach { p ->
                    val isMatched = matchedPairs.contains(p.id)
                    val isSelected = selectedLeft == p.left

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMatched) SuccessGreen.copy(alpha = 0.2f)
                            else if (isSelected) PurpleAccent.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) PurpleAccent else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isMatched) { selectedLeft = p.left }
                    ) {
                        Text(
                            text = p.left + if (isMatched) " ✓" else "",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(14.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Right column (shuffled)
            val rightItems = remember { pairs.shuffled() }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rightItems.forEach { p ->
                    val isMatched = matchedPairs.contains(p.id)

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMatched) SuccessGreen.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isMatched && selectedLeft != null) {
                                val expectedLeft = pairs.find { it.id == p.id }?.left
                                if (selectedLeft == expectedLeft) {
                                    matchedPairs = matchedPairs + p.id
                                    selectedLeft = null
                                    gamesViewModel.addMemoryMatch()
                                } else {
                                    selectedLeft = null
                                }
                            }
                    ) {
                        Text(
                            text = p.right + if (isMatched) " ✓" else "",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(14.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
