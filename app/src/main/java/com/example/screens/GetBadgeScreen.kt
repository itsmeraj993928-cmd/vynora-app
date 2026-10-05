package com.example.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.VynoraPlusBadge
import com.example.i18n.AppLanguage
import com.example.i18n.VynoraStrings
import com.example.model.User
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonEmerald
import com.example.ui.theme.VynoraNeonPink

@Composable
fun GetBadgeScreen(
    user: User,
    isDarkMode: Boolean,
    currentLanguage: AppLanguage,
    onBuyClicked: () -> Unit,
    onBack: () -> Unit,
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val screenBg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF0F121E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDarkMode) Color(0x2800D1FF) else Color(0x1F000000)

    val referralLink = "https://vynora.io/invite/${user.referralCode}"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("get_badge_screen")
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
                text = VynoraStrings.vynoraPlus(currentLanguage),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Large Hero Blue Badge Icon
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF00D1FF).copy(alpha = 0.35f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                VynoraPlusBadge(size = 72.dp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "VYNORA Plus",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
            )

            Text(
                text = "Stand out with the official blue star-check badge",
                fontSize = 13.sp,
                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // --- Status Box ---
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (user.isPlusActive) VynoraNeonCyan else cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Status: " + if (user.isPlusActive) "Active" else "Inactive",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (user.isPlusActive) VynoraNeonCyan else Color.Gray
                        )
                        if (user.isPlusActive) {
                            Text(
                                text = "${VynoraStrings.daysRemaining(currentLanguage, user.daysRemaining)} (${user.badgeSource})",
                                fontSize = 12.sp,
                                color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF334155)
                            )
                        } else {
                            Text(
                                text = "Get badge via subscription or referrals",
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    if (user.isPlusActive) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Option 1: Buy Badge (Rs 49 / month) ---
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, VynoraNeonCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1 Month Subscription",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = VynoraStrings.badgePrice(currentLanguage),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = VynoraNeonCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Instant activation of VYNORA Plus badge\n• Extends active badges by 30 days\n• Secure payment verified with Google Play Billing",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onBuyClicked,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("buy_badge_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = Color.Black)
                            Text(
                                text = if (user.isPlusActive) VynoraStrings.renewBadge(currentLanguage) else VynoraStrings.buyBadge(currentLanguage),
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- Option 2: Invite 10 Friends & Get Free (30 Days) ---
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, VynoraNeonPink),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Get Free via Referrals",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VynoraNeonPink.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "FREE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = VynoraNeonPink,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = VynoraStrings.inviteFriends(currentLanguage),
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Referral Progress",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                        Text(
                            text = VynoraStrings.referralProgress(currentLanguage, user.referralCount),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VynoraNeonPink
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (user.referralCount / 10f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = VynoraNeonPink,
                        trackColor = if (isDarkMode) Color(0xFF1E2235) else Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Referral Code Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDarkMode) Color(0xFF161928) else Color(0xFFF1F5F9))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Your Code", fontSize = 10.sp, color = Color.Gray)
                            Text(user.referralCode, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 2.sp, color = VynoraNeonCyan)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Copy button
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Vynora Invite Link", referralLink))
                                    onToast("Invite link copied to clipboard! 📋")
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = VynoraNeonCyan)
                            }

                            // Share via Android Share Sheet
                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Join VYNORA with my invite!")
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Join me on VYNORA — Share your world! Use my referral code ${user.referralCode} when signing up:\n$referralLink"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share VYNORA Invite"))
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Anti-fraud notice: Each invite must be a unique device and verify their email address. Self-referrals and accounts deleted within 7 days are disqualified.",
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
