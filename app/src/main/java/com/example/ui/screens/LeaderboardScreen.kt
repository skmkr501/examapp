package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TealAccent

data class LeaderboardUser(
    val rank: Int,
    val name: String,
    val avatar: String,
    val score: Int,
    val total: Int,
    val isMe: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    userProfile: UserProfile,
    onBack: () -> Unit
) {
    val leaderboardList = remember {
        listOf(
            LeaderboardUser(1, "সাদিয়া ইসলাম", "🌸", 98, 100),
            LeaderboardUser(2, "মেহজাবিন চৌধুরী", "⭐", 94, 100),
            LeaderboardUser(3, "তানভীর আহমেদ", "🦊", 91, 100),
            LeaderboardUser(4, userProfile.name + " (আপনি)", userProfile.avatar, 88, 100, isMe = true),
            LeaderboardUser(5, "রাকিবুল হাসান", "🚀", 85, 100),
            LeaderboardUser(6, "আহনাফ রহমান", "🦁", 82, 100),
            LeaderboardUser(7, "নুসরাত জাহান", "🦄", 79, 100)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🥇 সেরা মেধা তালিকা (লিডারবোর্ড)", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PurpleAccent.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "সব পরীক্ষার মোট অর্জিত নম্বরের ভিত্তিতে সার্বিক লিডারবোর্ড। নিয়মিত পরীক্ষা দিলে তোমার অবস্থান উপরে উঠবে!",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            itemsIndexed(leaderboardList) { _, item ->
                val medal = when (item.rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> "${item.rank}."
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isMe) PurpleAccent.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (item.isMe) PurpleAccent else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            medal,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.rank <= 3) GoldAccent else MaterialTheme.colorScheme.onSurface
                        )

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.avatar, fontSize = 20.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.name,
                                fontWeight = if (item.isMe) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PurpleAccent
                        ) {
                            Text(
                                "${item.score} / ${item.total}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
