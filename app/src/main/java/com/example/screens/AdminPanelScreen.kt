package com.example.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReportItem
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink

@Composable
fun AdminPanelScreen(
    reports: List<ReportItem>,
    isDarkMode: Boolean,
    onResolveReport: (reportId: String, action: String) -> Unit,
    onRemovePost: (postId: String) -> Unit,
    onBanUser: (username: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Reports Queue, 1: Copyright Takedowns, 2: System Health

    val screenBg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF0F121E) else Color(0xFFFFFFFF)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("admin_panel_screen")
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
                    text = "VYNORA Admin Console",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFEF4444)
            ) {
                Text(
                    text = "ADMIN ONLY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = VynoraNeonPink
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Reports (${reports.size})", fontSize = 12.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Copyright & IPR", fontSize = 12.sp) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Compliance (DPDP)", fontSize = 12.sp) })
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (selectedTab == 0) {
                items(reports, key = { it.id }) { report ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (report.status.startsWith("RESOLVED")) Color.Gray.copy(alpha = 0.3f) else VynoraNeonPink.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (report.status.startsWith("RESOLVED")) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = report.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (report.status.startsWith("RESOLVED")) Color(0xFF10B981) else Color(0xFFEF4444),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = report.timestamp,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "${report.targetType} reported: @${report.reportedUsername}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = "Reason: ${report.reason}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VynoraNeonCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = report.details,
                                fontSize = 12.sp,
                                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
                            )
                            Text(
                                text = "Reported by: @${report.reporterUsername}",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            if (!report.status.startsWith("RESOLVED")) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onRemovePost(report.targetId)
                                            onResolveReport(report.id, "Post Removed")
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Remove Post", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            onBanUser(report.reportedUsername)
                                            onResolveReport(report.id, "User Suspended")
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Suspend User", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { onResolveReport(report.id, "Dismissed") },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Dismiss", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Active Copyright & IPR Takedowns",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Notice #CR-94812: 'Electric Pulse' Master Track by Kai Sterling.\nStatus: Media hashed & audio muting enforced platform-wide.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {},
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonCyan)
                            ) {
                                Text("Review Counter-Notice Appeals", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "India DPDP Act 2023 Compliance Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Data Fiduciary: VYNORA Interactive Private Ltd.\n" +
                                        "• Data Protection Officer (DPO): dpo@vynora.io\n" +
                                        "• Consent Architecture: Granular opt-in for algorithmic suggestions\n" +
                                        "• Data Erasure SLA: Requests handled within 72 hours\n" +
                                        "• Minor Protection: Explicit 13+ age gate and parental notice active.",
                                fontSize = 12.sp,
                                lineHeight = 20.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }
        }
    }
}
