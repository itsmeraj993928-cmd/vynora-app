package com.example.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.i18n.AppLanguage
import com.example.i18n.VynoraStrings
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink
import com.example.viewmodel.MainTab

@Composable
fun VynoraBottomNav(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    userAvatarUrl: String,
    currentLanguage: AppLanguage,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("floating_bottom_nav"),
        contentAlignment = Alignment.Center
    ) {
        // Floating Glassmorphic Pill Container
        val containerBg = if (isDarkMode) Color(0xF00D0F18) else Color(0xF5FFFFFF)
        val containerBorder = if (isDarkMode) Color(0x3300D1FF) else Color(0x22000000)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .height(64.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = if (isDarkMode) VynoraNeonPink.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.08f),
                    spotColor = if (isDarkMode) VynoraNeonCyan.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(containerBg)
                .border(1.dp, containerBorder, RoundedCornerShape(32.dp))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                NavItem(
                    iconSelected = Icons.Filled.Home,
                    iconUnselected = Icons.Outlined.Home,
                    label = VynoraStrings.navHome(currentLanguage),
                    isSelected = selectedTab == MainTab.HOME,
                    onClick = { onTabSelected(MainTab.HOME) },
                    isDarkMode = isDarkMode,
                    testTag = "nav_home"
                )
            }

            // 2. Explore
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                NavItem(
                    iconSelected = Icons.Filled.Search,
                    iconUnselected = Icons.Outlined.Search,
                    label = VynoraStrings.navExplore(currentLanguage),
                    isSelected = selectedTab == MainTab.EXPLORE,
                    onClick = { onTabSelected(MainTab.EXPLORE) },
                    isDarkMode = isDarkMode,
                    testTag = "nav_explore"
                )
            }

            // 3. Central Elevated Glowing Create "+" Button
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CreateFloatingButton(
                    isSelected = selectedTab == MainTab.CREATE,
                    onClick = { onTabSelected(MainTab.CREATE) }
                )
            }

            // 4. Clips (Short Videos)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                NavItem(
                    iconSelected = Icons.Filled.Movie,
                    iconUnselected = Icons.Outlined.Movie,
                    label = VynoraStrings.navClips(currentLanguage),
                    isSelected = selectedTab == MainTab.CLIPS,
                    onClick = { onTabSelected(MainTab.CLIPS) },
                    isDarkMode = isDarkMode,
                    testTag = "nav_clips"
                )
            }

            // 4b. AI video
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                NavItem(
                    iconSelected = Icons.Filled.AutoAwesome,
                    iconUnselected = Icons.Outlined.AutoAwesome,
                    label = "AI",
                    isSelected = selectedTab == MainTab.AI,
                    onClick = { onTabSelected(MainTab.AI) },
                    isDarkMode = isDarkMode,
                    testTag = "nav_ai"
                )
            }

            // 5. Profile
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                ProfileNavItem(
                    avatarUrl = userAvatarUrl,
                    isSelected = selectedTab == MainTab.PROFILE,
                    label = VynoraStrings.navProfile(currentLanguage),
                    onClick = { onTabSelected(MainTab.PROFILE) },
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    iconSelected: androidx.compose.ui.graphics.vector.ImageVector,
    iconUnselected: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkMode: Boolean,
    testTag: String
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "nav_scale"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) {
            VynoraNeonPink
        } else {
            if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
        },
        label = "nav_color"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 24.dp),
                onClick = onClick
            )
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSelected) iconSelected else iconUnselected,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier
                .size(22.dp)
                .scale(scale)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = iconColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CreateFloatingButton(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.10f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "create_scale"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = VynoraNeonPink,
                spotColor = VynoraNeonCyan
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        VynoraNeonPink,
                        Color(0xFFB5179E),
                        VynoraNeonCyan
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick
            )
            .testTag("nav_create"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Create",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ProfileNavItem(
    avatarUrl: String,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "profile_scale"
    )
    val textColor = if (isSelected) VynoraNeonCyan else if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 24.dp),
                onClick = onClick
            )
            .testTag("nav_profile"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) VynoraNeonCyan else Color.Gray.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = label,
                    tint = if (isSelected) VynoraNeonCyan else Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
