package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.ui.theme.*

data class ModuleTile(
    val title: String,
    val icon: ImageVector? = null,
    val emoji: String? = null,
    val color: Color,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile,
    onNavigate: (String) -> Unit,
    onOpenShop: () -> Unit
) {
    var showClassDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(PurpleAccent, PinkAccent, GoldAccent)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📝", fontSize = 18.sp)
                        }
                        Column {
                            Text(
                                "অনুশীলন",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "জানো · শেখো · এগিয়ে যাও",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Coin pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = GoldAccent.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clickable { onOpenShop() }
                            .testTag("home_coins_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🪙 ", fontSize = 12.sp)
                            Text(
                                "${userProfile.coins}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }
                    }

                    // XP Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = PurpleAccent.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("⚡ ", fontSize = 12.sp)
                            Text(
                                "${userProfile.xp} XP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleAccent
                            )
                        }
                    }

                    // Avatar button
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onNavigate("profile") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(userProfile.avatar, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Hero Greeting Banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(PurpleAccent, PinkAccent, GoldAccent)
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Text(
                                "স্বাগতম, ${userProfile.name}!",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "পড়ালেখা হোক আনন্দময় ও সফল। আজ নতুন কিছু শিখে নিজেকে প্রস্তুত করো!",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        "লেভেল ${userProfile.level}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        "🔥 ${userProfile.currentStreak} দিনের স্ট্রিক",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Streak card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("🔥", fontSize = 36.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "বর্তমান স্ট্রিক",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${userProfile.currentStreak} দিন",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                            Text(
                                "স্ট্রিক ধরে রাখতে আজ অন্তত একটি পরীক্ষা বা কুইজ সম্পন্ন করো!",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Daily Challenge CTA
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TealAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 22.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "ডেইলি চ্যালেঞ্জ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "প্রতিদিনের ৫টি বাছাইকৃত প্রশ্ন দিয়ে বোনাস কয়েন ও XP জেতো",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { onNavigate("exam_catalog") },
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("শুরু", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Grid of all Application Modules
            item {
                Text(
                    "সব মডিউল ও সুবিধাসমূহ",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            val modules = listOf(
                ModuleTile("পরীক্ষা", emoji = "📝", color = PurpleAccent, route = "exam_catalog"),
                ModuleTile("AI মশাই", emoji = "🤖", color = PinkAccent, route = "ai_tutor"),
                ModuleTile("গেম জোন", emoji = "🎮", color = TealAccent, route = "game_zone"),
                ModuleTile("দৈনিক স্পিন", emoji = "🎡", color = GoldAccent, route = "game_zone"),
                ModuleTile("লিডারবোর্ড", emoji = "🥇", color = BlueAccent, route = "leaderboard"),
                ModuleTile("ভুলের খাতা", emoji = "📕", color = PinkAccent, route = "mistakes"),
                ModuleTile("ফ্ল্যাশকার্ড", emoji = "🧠", color = PurpleAccent, route = "flashcards"),
                ModuleTile("বুক ও নোট", emoji = "📖", color = TealAccent, route = "notes"),
                ModuleTile("ফ্রেন্ড জোন", emoji = "👥", color = BlueAccent, route = "friends"),
                ModuleTile("কয়েন শপ", emoji = "🪙", color = GoldAccent, route = "shop"),
                ModuleTile("প্রোফাইল", emoji = "👤", color = PurpleAccent, route = "profile"),
                ModuleTile("শিক্ষক প্যানেল", emoji = "👨‍🏫", color = PinkAccent, route = "teacher_panel")
            )

            // Render as 4-column grid rows
            for (rowChunk in modules.chunked(4)) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (mod in rowChunk) {
                            Box(modifier = Modifier.weight(1f)) {
                                ModuleTileItem(
                                    module = mod,
                                    onClick = {
                                        if (mod.route == "shop") onOpenShop() else onNavigate(mod.route)
                                    }
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
fun ModuleTileItem(
    module: ModuleTile,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp)
            .testTag("tile_${module.title}")
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(module.color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            if (module.emoji != null) {
                Text(module.emoji, fontSize = 24.sp)
            } else if (module.icon != null) {
                Icon(
                    module.icon,
                    contentDescription = module.title,
                    tint = module.color,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = module.title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
