package com.example.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonEmerald
import com.example.ui.theme.VynoraNeonPink

@Composable
fun StoryRingAvatar(
    avatarUrl: String,
    hasStory: Boolean = false,
    isCloseFriends: Boolean = false,
    isViewed: Boolean = false,
    size: Dp = 56.dp,
    onClick: () -> Unit = {}
) {
    val ringBrush = when {
        !hasStory -> null
        isCloseFriends -> Brush.sweepGradient(listOf(VynoraNeonEmerald, Color(0xFF6EE7B7), VynoraNeonEmerald))
        isViewed -> Brush.linearGradient(listOf(Color(0xFF475569), Color(0xFF64748B)))
        else -> Brush.sweepGradient(
            listOf(
                VynoraNeonPink,
                Color(0xFFE879F9),
                VynoraNeonCyan,
                VynoraNeonPink
            )
        )
    }

    Box(
        modifier = Modifier
            .size(size)
            .then(
                if (ringBrush != null) {
                    Modifier.border(width = 2.5.dp, brush = ringBrush, shape = CircleShape)
                } else Modifier
            )
            .padding(if (hasStory) 3.5.dp else 0.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl.isNotBlank()) {
            if (avatarUrl.startsWith("data:image")) {
                // Base64 photo (saved at sign-up): decode ourselves, Coil can't open data: links
                val bitmap by produceState<Bitmap?>(initialValue = null, avatarUrl) {
                    value = withContext(Dispatchers.Default) { decodeDataUriBitmap(avatarUrl) }
                }
                val bmp = bitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(size).clip(CircleShape).background(Color(0xFF1E2235)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Avatar",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(size * 0.55f)
                        )
                    }
                }
            } else {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(VynoraNeonPink, VynoraNeonCyan))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
    }
}

private fun decodeDataUriBitmap(dataUri: String): Bitmap? {
    return try {
        val base64 = dataUri.substringAfter("base64,", "")
        if (base64.isBlank()) return null
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 600 || bounds.outHeight / sample > 600) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    } catch (e: Throwable) {
        null
    }
}
