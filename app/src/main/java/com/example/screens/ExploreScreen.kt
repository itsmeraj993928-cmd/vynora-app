package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.components.StoryRingAvatar
import com.example.model.Post
import com.example.model.User
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink
import com.example.util.formatCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    posts: List<Post>,
    isDarkMode: Boolean,
    onUserClick: (String) -> Unit,
    onFollowToggle: (String) -> Unit,
    onPostClick: (Post) -> Unit,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Accounts", "#Trends", "Nightlife", "Aesthetic", "Music")
    val trendingTags = listOf("#VynoraVibes", "#CyberBeats", "#NeonAesthetic", "#TokyoRain", "#3DDesign", "#CreatorFund")

    val screenBg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF0F121E) else Color(0xFFFFFFFF)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = 12.dp)
            .testTag("explore_screen")
    ) {
        // --- 1. Search Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search accounts, tags, places...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search", tint = VynoraNeonCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_bar"),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VynoraNeonCyan,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg
                )
            )
        }

        // --- 2. Category Pills ---
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) VynoraNeonPink else cardBg,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) VynoraNeonPink else (if (isDarkMode) Color(0x33FFFFFF) else Color(0x22000000))
                    ),
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else (if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 3. Suggested Creators Row ---
        val suggestedCreators = remember(posts, searchQuery) {
            val q = searchQuery.trim().removePrefix("@")
            posts.map { it.author }
                .filter { it.username.isNotBlank() }
                .filter { if (q.isBlank() || q.startsWith("#")) !it.isFollowing else it.username.contains(q, ignoreCase = true) || it.fullName.contains(q, ignoreCase = true) }
                .distinctBy { it.id.ifEmpty { it.username } }
                .take(10)
        }

        if ((selectedCategory == "All" || selectedCategory == "Accounts") && suggestedCreators.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = if (searchQuery.isBlank()) "Suggested accounts" else "Accounts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(suggestedCreators) { user ->
                        SuggestedUserCard(
                            user = user,
                            isDarkMode = isDarkMode,
                            onUserClick = { onUserClick(user.username) },
                            onFollowToggle = { onFollowToggle(user.username) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- 4. Trending Hashtags Bar ---
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(trendingTags) { tag ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDarkMode) Color(0x2200D1FF) else Color(0x1500D1FF))
                        .border(1.dp, Color(0x3300D1FF), RoundedCornerShape(12.dp))
                        .clickable { searchQuery = tag }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.TrendingUp,
                        contentDescription = null,
                        tint = VynoraNeonCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = tag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VynoraNeonCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 5. Media Grid ---
        val filteredPosts = if (searchQuery.isBlank()) {
            posts
        } else {
            posts.filter {
                it.caption.contains(searchQuery, ignoreCase = true) ||
                it.author.username.contains(searchQuery, ignoreCase = true) ||
                it.hashtags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
            }
        }

        if (filteredPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No posts found",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No results matching \"$searchQuery\"" else "No posts yet. Follow people or create your first post!",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filteredPosts) { post ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(Color(0xFF1E2235))
                            .clickable { onPostClick(post) }
                    ) {
                        AsyncImage(
                            model = post.mediaUrls.firstOrNull()?.let { u ->
                                if (post.isVideo || u.contains("/video/upload/")) {
                                    u.substringBeforeLast('.', u).replace("/video/upload/", "/video/upload/so_0,w_480,h_480,c_fill/") + ".jpg"
                                } else u
                            },
                            contentDescription = "Explore Item",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                    // Multiple images or video badge
                    if (post.mediaUrls.size > 1) {
                        Icon(
                            imageVector = Icons.Filled.Collections,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                }
            }
        }
}
    }
}
}

@Composable
private fun SuggestedUserCard(
    user: User,
    isDarkMode: Boolean,
    onUserClick: () -> Unit,
    onFollowToggle: () -> Unit
) {
    val cardBg = if (isDarkMode) Color(0xFF0F121E) else Color(0xFFFFFFFF)

    Column(
        modifier = Modifier
            .width(115.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, if (isDarkMode) Color(0x2800D1FF) else Color(0x1F000000), RoundedCornerShape(16.dp))
            .clickable(onClick = onUserClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        ) {
            StoryRingAvatar(
                avatarUrl = user.avatarUrl,
                size = 48.dp,
                onClick = onUserClick
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = user.username,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
        )

        Text(
            text = "${formatCount(user.followersCount)} followers",
            fontSize = 10.sp,
            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onFollowToggle,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (user.isFollowing) Color.Gray.copy(alpha = 0.4f) else VynoraNeonPink
            )
        ) {
            Text(
                text = if (user.isFollowing) "Following" else "Follow",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
