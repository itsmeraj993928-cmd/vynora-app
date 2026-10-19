package com.example.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.StoryRingAvatar
import com.example.components.VynoraOfficialBadge
import com.example.components.VynoraPlusBadge
import com.example.model.User
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    username: String,
    initialTab: Int,
    isDarkMode: Boolean,
    myId: String,
    loadFollowers: suspend () -> List<User>,
    loadFollowing: suspend () -> List<User>,
    isFollowing: (String) -> Boolean,
    onFollowToggle: (User) -> Unit,
    onUserClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    var tab by remember { mutableIntStateOf(if (initialTab == 1) 1 else 0) }
    var query by remember { mutableStateOf("") }
    var version by remember { mutableIntStateOf(0) } // bumps after Follow / Unfollow so buttons refresh

    var followers by remember { mutableStateOf<List<User>?>(null) }
    var following by remember { mutableStateOf<List<User>?>(null) }

    LaunchedEffect(Unit) { followers = try { loadFollowers() } catch (e: Exception) { emptyList() } }
    LaunchedEffect(Unit) { following = try { loadFollowing() } catch (e: Exception) { emptyList() } }

    val current = if (tab == 0) followers else following
    val filtered = current?.filter {
        query.isBlank() ||
            it.username.contains(query.trim(), ignoreCase = true) ||
            it.fullName.contains(query.trim(), ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
            }
            Text(username, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = textPrimary)
        }

        TabRow(
            selectedTabIndex = tab,
            containerColor = bg,
            contentColor = VynoraNeonPink
        ) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text("${followers?.size ?: 0} Followers", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text("${following?.size ?: 0} Following", fontWeight = FontWeight.SemiBold) }
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = textSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        )

        when {
            filtered == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VynoraNeonCyan)
                }
            }
            filtered.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (query.isNotBlank()) "No results found"
                        else if (tab == 0) "No followers yet" else "Not following anyone yet",
                        color = textSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { u ->
                        // reading `version` makes this row redraw after a follow toggle
                        val followingNow = remember(version, u.id) { isFollowing(u.id) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUserClick(u.id) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StoryRingAvatar(avatarUrl = u.avatarUrl, hasStory = false, size = 48.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        u.username,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (u.isOfficial) VynoraOfficialBadge(size = 14.dp)
                                    else if (u.isPlusActive) VynoraPlusBadge(size = 14.dp)
                                }
                                if (u.fullName.isNotBlank()) {
                                    Text(u.fullName, fontSize = 12.sp, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (u.id != myId && !u.isOfficial) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (followingNow) Color.Transparent else VynoraNeonPink,
                                    border = if (followingNow) BorderStroke(1.dp, Color(0x6694A3B8)) else null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onFollowToggle(u)
                                            version++
                                        }
                                ) {
                                    Text(
                                        text = if (followingNow) "Following" else "Follow",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (followingNow) textPrimary else Color.White,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
