package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.i18n.AppLanguage
import com.example.i18n.VynoraStrings
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink

@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    currentLanguage: AppLanguage,
    onToggleTheme: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onOpenLegalDoc: (String) -> Unit,
    onBack: () -> Unit,
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPrivateAccount by remember { mutableStateOf(false) }
    var showActivityStatus by remember { mutableStateOf(true) }
    var filterOffensiveComments by remember { mutableStateOf(true) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val screenBg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF0F121E) else Color(0xFFFFFFFF)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("settings_screen")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                text = VynoraStrings.settings(currentLanguage),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. Preferences Section ---
            item {
                Text(
                    text = "Preferences",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column {
                        // Dark Theme Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                                    contentDescription = null,
                                    tint = VynoraNeonPink
                                )
                                Text(
                                    text = VynoraStrings.darkMode(currentLanguage),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                )
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { onToggleTheme() },
                                colors = SwitchDefaults.colors(checkedThumbColor = VynoraNeonPink)
                            )
                        }

                        HorizontalDivider(color = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x1F000000))

                        DataSaverRow(isDarkMode)

                        HorizontalDivider(color = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x1F000000))

                        // Language Switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLanguageDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Translate,
                                    contentDescription = null,
                                    tint = VynoraNeonCyan
                                )
                                Text(
                                    text = VynoraStrings.language(currentLanguage),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${currentLanguage.displayName} (${currentLanguage.nativeName})",
                                    fontSize = 13.sp,
                                    color = VynoraNeonCyan
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- 2. Privacy & Safety Section ---
            item {
                Text(
                    text = "Privacy & Safety",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column {
                        // Private Account Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Private Account", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                Text("Only approved followers can see your posts", fontSize = 11.sp, color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B))
                            }
                            Switch(
                                checked = isPrivateAccount,
                                onCheckedChange = {
                                    isPrivateAccount = it
                                    onToast(if (it) "Account set to Private" else "Account set to Public")
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = VynoraNeonCyan)
                            )
                        }

                        HorizontalDivider(color = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x1F000000))

                        // Activity Status
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Activity Status", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                Text("Show when you are active on Chats", fontSize = 11.sp, color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B))
                            }
                            Switch(
                                checked = showActivityStatus,
                                onCheckedChange = { showActivityStatus = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = VynoraNeonCyan)
                            )
                        }

                        HorizontalDivider(color = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x1F000000))

                        // Keyword Filter for Offensive Comments
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Offensive Words Filter", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                Text("Automatically hide abusive comments and spam", fontSize = 11.sp, color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B))
                            }
                            Switch(
                                checked = filterOffensiveComments,
                                onCheckedChange = {
                                    filterOffensiveComments = it
                                    onToast(if (it) "Offensive comments filter enabled" else "Filter disabled")
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = VynoraNeonPink)
                            )
                        }
                    }
                }
            }

            // --- 3. Data & Legal Section (DPDP Act Compliance) ---
            item {
                Text(
                    text = "Legal & Compliance (DPDP Act)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column {
                        SettingsNavigationRow(
                            title = VynoraStrings.privacyPolicy(currentLanguage),
                            subtitle = "Digital Personal Data Protection Act compliance",
                            icon = Icons.Outlined.Security,
                            isDarkMode = isDarkMode,
                            onClick = { onOpenLegalDoc("privacy") }
                        )

                        HorizontalDivider(color = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x1F000000))

                        SettingsNavigationRow(
                            title = VynoraStrings.termsOfService(currentLanguage),
                            subtitle = "Platform conditions & user obligations",
                            icon = Icons.Outlined.Description,
                            isDarkMode = isDarkMode,
                            onClick = { onOpenLegalDoc("terms") }
                        )

                        HorizontalDivider(color = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x1F000000))

                        SettingsNavigationRow(
                            title = VynoraStrings.copyrightForm(currentLanguage),
                            subtitle = "Notice of takedown & appeal procedure",
                            icon = Icons.Outlined.Copyright,
                            isDarkMode = isDarkMode,
                            onClick = { onOpenLegalDoc("copyright") }
                        )

                        HorizontalDivider(color = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x1F000000))

                        SettingsNavigationRow(
                            title = "Download My Personal Data",
                            subtitle = "Receive an archive of your profile & media (DPDP Right)",
                            icon = Icons.Outlined.Download,
                            isDarkMode = isDarkMode,
                            onClick = { onToast("Your data archive is being prepared for export. Check your email shortly.") }
                        )
                    }
                }
            }

            // --- 4. Account Actions (Logout, Delete) ---
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToast("Logged out of VYNORA") }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = VynoraNeonPink)
                            Text("Log Out", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = VynoraNeonPink)
                        }

                        HorizontalDivider(color = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x1F000000))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToast("Account deletion request submitted (30-day grace period)") }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = Color(0xFFEF4444))
                            Text("Delete Account", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }
    }

    // Language Selection Modal
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Choose Language") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppLanguage.values().forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (currentLanguage == lang) VynoraNeonPink.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    onSelectLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(lang.displayName, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                                Text(lang.nativeName, fontSize = 12.sp, color = VynoraNeonCyan)
                            }
                            if (currentLanguage == lang) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = VynoraNeonPink)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = cardBg
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
    }
}


@Composable
private fun DataSaverRow(isDarkMode: Boolean) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Data Saver", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
            Text(
                "Smaller photo uploads, and reels wait for a tap on mobile data",
                fontSize = 11.sp, color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
        Switch(
            checked = com.example.util.DataSaver.enabled,
            onCheckedChange = { com.example.util.DataSaver.set(ctx, it) },
            colors = SwitchDefaults.colors(checkedThumbColor = VynoraNeonPink)
        )
    }
}
