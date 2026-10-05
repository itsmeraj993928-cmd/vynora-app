package com.example.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink

@Composable
fun VynoraLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showGlow: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (showGlow) 12.dp else 2.dp,
                shape = RoundedCornerShape(size * 0.28f),
                ambientColor = VynoraNeonPink.copy(alpha = pulseAlpha * 0.4f),
                spotColor = VynoraNeonCyan.copy(alpha = pulseAlpha * 0.5f)
            )
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Color(0xFF07080F))
            .border(
                width = (size * 0.035f).coerceAtLeast(1.dp),
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        VynoraNeonPink.copy(alpha = 0.5f),
                        VynoraNeonCyan.copy(alpha = 0.6f),
                        Color.White.copy(alpha = 0.15f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(100f, 100f)
                ),
                shape = RoundedCornerShape(size * 0.28f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.72f)) {
            val w = this.size.width
            val h = this.size.height

            // Glowing neon 'V' Path
            val vPath = Path().apply {
                // Left top tip
                moveTo(w * 0.14f, h * 0.22f)
                // Curve down to bottom center
                cubicTo(
                    w * 0.24f, h * 0.52f,
                    w * 0.40f, h * 0.82f,
                    w * 0.50f, h * 0.88f
                )
                // Curve up to right top tip
                cubicTo(
                    w * 0.60f, h * 0.82f,
                    w * 0.76f, h * 0.52f,
                    w * 0.86f, h * 0.22f
                )
                // Top right rounded cap
                cubicTo(
                    w * 0.90f, h * 0.18f,
                    w * 0.84f, h * 0.14f,
                    w * 0.78f, h * 0.18f
                )
                // Inner curve down
                cubicTo(
                    w * 0.68f, h * 0.48f,
                    w * 0.56f, h * 0.70f,
                    w * 0.50f, h * 0.74f
                )
                // Inner curve back to left
                cubicTo(
                    w * 0.44f, h * 0.70f,
                    w * 0.32f, h * 0.48f,
                    w * 0.22f, h * 0.18f
                )
                close()
            }

            // Glow shadow behind path
            drawPath(
                path = vPath,
                color = VynoraNeonPink.copy(alpha = 0.35f * pulseAlpha),
                style = Stroke(width = w * 0.18f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Vibrant gradient fill for the V
            drawPath(
                path = vPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        VynoraNeonPink,
                        Color(0xFFD946EF),
                        VynoraNeonCyan
                    )
                )
            )

            // Sparkle star on right tip
            val starCenter = Offset(w * 0.88f, h * 0.20f)
            val starRadius = w * 0.13f

            val starPath = Path().apply {
                moveTo(starCenter.x, starCenter.y - starRadius)
                lineTo(starCenter.x + starRadius * 0.25f, starCenter.y - starRadius * 0.25f)
                lineTo(starCenter.x + starRadius, starCenter.y)
                lineTo(starCenter.x + starRadius * 0.25f, starCenter.y + starRadius * 0.25f)
                lineTo(starCenter.x, starCenter.y + starRadius)
                lineTo(starCenter.x - starRadius * 0.25f, starCenter.y + starRadius * 0.25f)
                lineTo(starCenter.x - starRadius, starCenter.y)
                lineTo(starCenter.x - starRadius * 0.25f, starCenter.y - starRadius * 0.25f)
                close()
            }

            drawPath(
                path = starPath,
                color = Color.White
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = starRadius * 0.35f,
                center = starCenter
            )
        }
    }
}

@Composable
fun VynoraBrandHeader(
    modifier: Modifier = Modifier,
    iconSize: Dp = 38.dp,
    textColor: Color = Color.White
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        VynoraLogoIcon(size = iconSize)
        Column {
            Text(
                text = "VYNORA",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                letterSpacing = 2.5.sp,
                color = textColor
            )
        }
    }
}
