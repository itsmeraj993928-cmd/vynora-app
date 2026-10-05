package com.example.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.VynoraLogoIcon
import com.example.i18n.AppLanguage
import com.example.i18n.VynoraStrings
import com.example.ui.theme.VynoraBlack
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonEmerald
import com.example.ui.theme.VynoraNeonPink
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VynoraBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            VynoraLogoIcon(size = 96.dp, showGlow = true)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "VYNORA",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Share your world.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = VynoraNeonCyan.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    currentLanguage: AppLanguage,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    var isAgeConfirmed by remember { mutableStateOf(false) }

    val slides = listOf(
        Triple(
            "Share Your World",
            "Express your creative vision with vibrant photography, immersive carousels, and stories that glow with personality.",
            VynoraNeonPink
        ),
        Triple(
            "Clips & 24h Moments",
            "Immerse yourself in full-screen vertical short videos, trending audio tracks, and ephemeral moments that expire after 24 hours.",
            VynoraNeonCyan
        ),
        Triple(
            "Direct Chats & AI Studio",
            "Real-time messaging, simulated voice notes, and smart Gemini AI helpers that craft captions and auto-translate comments.",
            Color(0xFFA855F7)
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VynoraBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top row with Skip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VynoraLogoIcon(size = 36.dp)
            Text(
                text = VynoraStrings.skip(currentLanguage),
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { onComplete() }
                    .padding(8.dp)
            )
        }

        // Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val slide = slides[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(slide.third.copy(alpha = 0.35f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    VynoraLogoIcon(size = 80.dp)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = slide.first,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = slide.second,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Pager indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(width = if (pagerState.currentPage == index) 24.dp else 8.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == index) VynoraNeonPink else Color(0xFF334155))
                )
            }
        }

        // Age Gate Notice
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F121E))
                .border(1.dp, Color(0x3300D1FF), RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = VynoraNeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = VynoraStrings.ageGateTitle(currentLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = VynoraStrings.ageGateDesc(currentLanguage),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { isAgeConfirmed = !isAgeConfirmed }
                    .padding(4.dp)
            ) {
                Checkbox(
                    checked = isAgeConfirmed,
                    onCheckedChange = { isAgeConfirmed = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = VynoraNeonEmerald,
                        uncheckedColor = Color(0xFF64748B)
                    )
                )
                Text(
                    text = VynoraStrings.ageGateConfirm(currentLanguage),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAgeConfirmed) VynoraNeonEmerald else Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom CTA Button
        Button(
            onClick = {
                if (pagerState.currentPage < 2) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    onComplete()
                }
            },
            enabled = isAgeConfirmed || pagerState.currentPage < 2,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("onboarding_cta_button"),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VynoraNeonPink,
                disabledContainerColor = Color(0xFF334155)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage == 2) VynoraStrings.getStarted(currentLanguage) else VynoraStrings.next(currentLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
