package com.example.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CreatorStats
import com.example.model.Post
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonEmerald
import com.example.ui.theme.VynoraNeonPink
import com.example.util.formatCount

@Composable
fun CreatorAnalyticsScreen(
    stats: CreatorStats,
    topPosts: List<Post>,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isProfessionalAccount by remember { mutableStateOf(true) }
    var areSubscriptionsEnabled by remember { mutableStateOf(true) }

    val screenBg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF0F121E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDarkMode) Color(0x2800D1FF) else Color(0x1F000000)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("creator_analytics_screen")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                }
                Text(
                    text = "Creator Studio",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = VynoraNeonCyan.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, VynoraNeonCyan)
            ) {
                Text(
                    text = "PRO CREATOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = VynoraNeonCyan,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. KPI Cards Row ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Impressions",
                        value = stats.totalImpressions,
                        growth = stats.impressionsGrowth,
                        accentColor = VynoraNeonPink,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        isDarkMode = isDarkMode,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Total Reach",
                        value = stats.totalReach,
                        growth = stats.reachGrowth,
                        accentColor = VynoraNeonCyan,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        isDarkMode = isDarkMode,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Engagement",
                        value = stats.engagementRate,
                        growth = stats.engagementGrowth,
                        accentColor = Color(0xFFA855F7),
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        isDarkMode = isDarkMode,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Tips Earned",
                        value = stats.totalTipsEarned,
                        growth = "${formatCount(stats.subscriberCount)} subs",
                        accentColor = VynoraNeonEmerald,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        isDarkMode = isDarkMode,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- 2. Follower Growth Bar Chart ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Weekly Follower Inflow",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = "+5,960 this week",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VynoraNeonCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Custom Canvas Bar Chart
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            val maxVal = 1600f
                            val barWidth = 24.dp.toPx()
                            val spacing = (size.width - (barWidth * stats.weeklyGrowth.size)) / (stats.weeklyGrowth.size + 1)

                            stats.weeklyGrowth.forEachIndexed { i, pair ->
                                val barHeight = (pair.second / maxVal) * (size.height - 20.dp.toPx())
                                val x = spacing + i * (barWidth + spacing)
                                val y = size.height - barHeight

                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(VynoraNeonCyan, VynoraNeonPink)
                                    ),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Days Labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            stats.weeklyGrowth.forEach { pair ->
                                Text(
                                    text = pair.first,
                                    fontSize = 11.sp,
                                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            // --- 3. Creator Subscriptions & Tips Section ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Subscriber Perks & Badges",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Allow fans to subscribe for $4.99/mo with custom badges",
                                    fontSize = 11.sp,
                                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = areSubscriptionsEnabled,
                                onCheckedChange = {
                                    areSubscriptionsEnabled = it
                                    onToast(if (it) "Subscriptions enabled ✨" else "Subscriptions paused")
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = VynoraNeonPink)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { onToast("Payout settings configured (Direct Deposit)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color(0xFF1E2235) else Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = "Manage Payout Bank Account",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            // --- 4. Top Performing Posts ---
            item {
                Text(
                    text = "Top Performing Posts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
            }

            items(topPosts.take(3), key = { it.id }) { post ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = post.mediaUrls.firstOrNull(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = post.caption.take(45) + "...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "❤️ ${formatCount(post.likesCount)} likes",
                                    fontSize = 11.sp,
                                    color = VynoraNeonPink
                                )
                                Text(
                                    text = "💬 ${formatCount(post.comments.size)} comments",
                                    fontSize = 11.sp,
                                    color = VynoraNeonCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    growth: String,
    accentColor: Color,
    cardBg: Color,
    cardBorder: Color,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = growth,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}
