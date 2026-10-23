package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ShopItem
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.PurpleAccent

@Composable
fun ShopDialog(
    currentCoins: Int,
    onBuyItem: (ShopItem) -> Unit,
    onDismiss: () -> Unit
) {
    val shopItems = listOf(
        ShopItem("doubt5", "❓ ডাউট প্যাক ×৫", "৫টি অতিরিক্ত AI ডাউট সমাধান", 40, "doubt", 5),
        ShopItem("doubt15", "❓ ডাউট প্যাক ×১৫", "১৫টি অতিরিক্ত AI ডাউট সমাধান (সাশ্রয়ী)", 100, "doubt", 15),
        ShopItem("tutor30", "🤖 মশাই বুস্ট +৩০", "AI মশাইয়ের সাথে ৩০টি অতিরিক্ত আলোচনা", 30, "tutor", 30)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🪙 কয়েন শপ", fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = GoldAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        "🪙 $currentCoins কয়েন",
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "পরীক্ষা দিয়ে এবং খেলায় জিতে কয়েন অর্জন করো। অর্জিত কয়েন দিয়ে ডাউট ও AI সুবিধা আনলক করো!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                shopItems.forEach { item ->
                    val canAfford = currentCoins >= item.cost
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(item.descr, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { onBuyItem(item) },
                                enabled = canAfford,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                            ) {
                                Text("🪙 ${item.cost}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}
