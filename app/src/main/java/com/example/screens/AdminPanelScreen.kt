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
    onUserAction: (String, String, (String) -> Unit) -> Unit = { _, _, _ -> },
    loadAppeals: suspend () -> List<com.example.model.Appeal> = { emptyList() },
    loadAudit: suspend () -> List<com.example.model.AuditEntry> = { emptyList() },
    onResolveAppeal: (com.example.model.Appeal, Boolean, () -> Unit) -> Unit = { _, _, _ -> },
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
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = VynoraNeonPink,
            edgePadding = 8.dp
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Reports (${reports.size})", fontSize = 12.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Copyright & IPR", fontSize = 12.sp) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Compliance (DPDP)", fontSize = 12.sp) })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Users", fontSize = 12.sp) })
            Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Appeals", fontSize = 12.sp) })
            Tab(selected = selectedTab == 5, onClick = { selectedTab = 5 }, text = { Text("Audit log", fontSize = 12.sp) })
        }

        var userInput by remember { mutableStateOf("") }
        var userMsg by remember { mutableStateOf("") }
        var appeals by remember { mutableStateOf<List<com.example.model.Appeal>>(emptyList()) }
        var audit by remember { mutableStateOf<List<com.example.model.AuditEntry>>(emptyList()) }
        var reloadKey by remember { mutableStateOf(0) }
        LaunchedEffect(selectedTab, reloadKey) {
            if (selectedTab == 4) appeals = loadAppeals()
            if (selectedTab == 5) audit = loadAudit()
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
                if (reports.isEmpty()) {
                    item {
                        Text(
                            text = "No reports yet. When users report a post or account, it will appear here.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
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
            } else if (selectedTab == 3) {
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cardBg)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Manage a user", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                            OutlinedTextField(value = userInput, onValueChange = { userInput = it }, label = { Text("Username (without @)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onUserAction(userInput, "RESTRICT") { userMsg = it } }) { Text("Restrict 7d", fontSize = 12.sp) }
                                OutlinedButton(onClick = { onUserAction(userInput, "UNRESTRICT") { userMsg = it } }) { Text("Lift", fontSize = 12.sp) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onUserAction(userInput, "BAN") { userMsg = it } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) { Text("Ban", fontSize = 12.sp) }
                                OutlinedButton(onClick = { onUserAction(userInput, "UNBAN") { userMsg = it } }) { Text("Unban", fontSize = 12.sp) }
                            }
                            if (userMsg.isNotBlank()) Text(userMsg, fontSize = 12.sp, color = VynoraNeonCyan)
                            Text("Restricted users can't post or comment. Banned users see an appeal screen. Every action is saved in the Audit log.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            } else if (selectedTab == 4) {
                if (appeals.isEmpty()) item { Text("No pending appeals.", color = Color(0xFF94A3B8)) }
                items(appeals, key = { it.id }) { a ->
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cardBg)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("@${a.username}", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                            Text(a.message, fontSize = 13.sp, color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onResolveAppeal(a, true) { reloadKey++ } }) { Text("Approve (unban)", fontSize = 12.sp) }
                                OutlinedButton(onClick = { onResolveAppeal(a, false) { reloadKey++ } }) { Text("Reject", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            } else if (selectedTab == 5) {
                if (audit.isEmpty()) item { Text("No admin actions yet.", color = Color(0xFF94A3B8)) }
                items(audit, key = { it.id }) { e ->
                    val whenText = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(e.createdAt))
                    Text("$whenText  •  @${e.adminName}  •  ${e.action}  •  ${e.target}", fontSize = 12.sp, color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155))
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
                                text = "Copyright & IPR Takedowns",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No copyright notices received yet.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
                            )
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
                                text = "Compliance (DPDP)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Company name, Data Protection Officer contact and data-erasure process are not set up yet. Add them here once they are real.",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }
    }
}
