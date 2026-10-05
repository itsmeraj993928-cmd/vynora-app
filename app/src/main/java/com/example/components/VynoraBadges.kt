package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink

@Composable
fun VynoraPlusBadge(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    // VYNORA Plus: Electric Blue / Cyan Star-Check Badge
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF00D1FF), Color(0xFF0284C7))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = "VYNORA Plus",
            tint = Color.White,
            modifier = Modifier.size(size * 0.7f)
        )
    }
}

@Composable
fun VynoraOfficialBadge(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    // Permanent Official Badge: Gold / White Metallic with Shield/Star
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFFF8DC))
                )
            )
            .border(1.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "VYNORA OFFICIAL",
            tint = Color(0xFF451A03),
            modifier = Modifier.size(size * 0.65f)
        )
    }
}

@Composable
fun UserBadgeRow(
    username: String,
    isOfficial: Boolean,
    isPlusActive: Boolean,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    fontSize: Dp = 14.dp,
    badgeSize: Dp = 15.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = username,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.value.sp,
            color = textColor
        )

        if (isOfficial) {
            VynoraOfficialBadge(size = badgeSize)
        } else if (isPlusActive) {
            VynoraPlusBadge(size = badgeSize)
        }
    }
}
