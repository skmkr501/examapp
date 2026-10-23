package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleData
import com.example.model.ExamResult
import com.example.model.UserProfile
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    pastResults: List<ExamResult>,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val totalTests = pastResults.size
    val averageScore = if (totalTests > 0) {
        val sum = pastResults.sumOf { (it.score / it.totalMarks.toFloat() * 100).toDouble() }
        (sum / totalTests).toInt()
    } else 0

    val grade = when {
        totalTests == 0 -> "নাই"
        averageScore >= 80 -> "A+"
        averageScore >= 70 -> "A"
        averageScore >= 60 -> "A-"
        averageScore >= 50 -> "B"
        averageScore >= 40 -> "C"
        averageScore >= 33 -> "D"
        else -> "F"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👤 প্রোফাইল ও রিপোর্ট", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Header Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
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
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(PurpleAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(userProfile.avatar, fontSize = 34.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                userProfile.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "শ্রেণি ${userProfile.classLevel} · ${userProfile.school}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = PurpleAccent.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        "⚡ ${userProfile.xp} XP",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = PurpleAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = GoldAccent.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        "🪙 ${userProfile.coins} কয়েন",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = GoldAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Academic KPI Report
            item {
                Text(
                    "পারফরম্যান্স রিপোর্ট",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResultStatBox(
                        title = "গড় পারফরম্যান্স",
                        value = "$averageScore%",
                        color = PurpleAccent,
                        modifier = Modifier.weight(1f)
                    )
                    ResultStatBox(
                        title = "মোট পরীক্ষা",
                        value = "$totalTests টি",
                        color = TealAccent,
                        modifier = Modifier.weight(1f)
                    )
                    ResultStatBox(
                        title = "চলতি গ্রেড",
                        value = grade,
                        color = GoldAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // WhatsApp Guardian Report
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👪", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "অভিভাবক সাপ্তাহিক রিপোর্ট",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "সাপ্তাহিক পরীক্ষার ফলাফল ও উন্নতির খতিয়ান এক ক্লিকে অভিভাবকের WhatsApp-এ পাঠাও।",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val message = "📊 Exam Site সাপ্তাহিক অগ্রগতি রিপোর্ট\n" +
                                        "শিক্ষার্থী: ${userProfile.name}\n" +
                                        "শ্রেণি: ${userProfile.classLevel}\n" +
                                        "সম্পন্ন পরীক্ষা: $totalTests টি\n" +
                                        "গড় ফলাফল: $averageScore% (গ্রেড: $grade)\n" +
                                        "স্ট্রিক: ${userProfile.currentStreak} দিন 🔥"
                                val uri = Uri.parse("https://wa.me/?text=" + Uri.encode(message))
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📲 WhatsApp-এ রিপোর্ট পাঠান", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Badges Section
            item {
                Text(
                    "অর্জিত ব্যাজ ও সম্মাননা",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            items(SampleData.badgesList) { badge ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(badge.icon, fontSize = 28.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                badge.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                badge.description,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (badge.isUnlocked) SuccessGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                        ) {
                            Text(
                                if (badge.isUnlocked) "অর্জিত ✓" else "লকড 🔒",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (badge.isUnlocked) SuccessGreen else Color.Gray,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
