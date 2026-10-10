package com.example.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.components.VynoraPlusBadge
import com.example.components.StoryRingAvatar
import com.example.i18n.AppLanguage
import com.example.i18n.VynoraStrings
import com.example.model.User
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink
import com.example.util.CloudinaryUploader
import com.example.util.ImageCompressUtil
import com.example.util.VynoraOfficial
import com.example.viewmodel.ScreenDestination
import com.example.viewmodel.VynoraViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    user: User,
    isDarkMode: Boolean,
    currentLanguage: AppLanguage,
    viewModel: VynoraViewModel,
    onBack: () -> Unit,
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val screenBg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF0F121E) else Color(0xFFFFFFFF)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val fieldBg = if (isDarkMode) Color(0xFF131726) else Color(0xFFF1F5F9)

    // Form state
    var fullName by remember { mutableStateOf(user.fullName) }
    var username by remember { mutableStateOf(user.username) }
    var pronouns by remember { mutableStateOf(user.pronouns) }
    var bio by remember { mutableStateOf(user.bio) }
    var links by remember {
        mutableStateOf(
            if (user.links.isNotEmpty()) user.links.take(3)
            else if (user.website.isNotBlank()) listOf(user.website)
            else emptyList()
        )
    }
    var gender by remember { mutableStateOf(user.gender.ifBlank { "Prefer not to say" }) }
    var dob by remember { mutableStateOf(user.dob) }
    var phone by remember { mutableStateOf(user.phone) }
    var category by remember { mutableStateOf(user.category.ifBlank { "Creator" }) }
    var isPrivateAccount by remember { mutableStateOf(user.isPrivateAccount) }
    var showEmailOnProfile by remember { mutableStateOf(user.showEmailOnProfile) }
    var showPhoneOnProfile by remember { mutableStateOf(user.showPhoneOnProfile) }
    var hideFollowersCount by remember { mutableStateOf(user.hideFollowersCount) }
    var isAiGeneratedProfile by remember { mutableStateOf(user.isAiGeneratedProfile) }
    var photoBase64 by remember { mutableStateOf(user.photoBase64) }
    var avatarUrl by remember { mutableStateOf(user.avatarUrl) }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    var genderDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // 14-day username cooldown check
    val fourteenDaysMillis = 14L * 24L * 60L * 60L * 1000L
    val timeSinceUsernameChange = System.currentTimeMillis() - user.usernameLastChangedAt
    val isUsernameOnCooldown = user.usernameLastChangedAt > 0L && timeSinceUsernameChange < fourteenDaysMillis
    val daysRemainingForUsername = if (isUsernameOnCooldown) {
        ((fourteenDaysMillis - timeSinceUsernameChange) / (24L * 60L * 60L * 1000L)).toInt() + 1
    } else 0

    // Unsaved changes detector
    val hasUnsavedChanges by remember {
        derivedStateOf {
            fullName != user.fullName ||
            username != user.username ||
            pronouns != user.pronouns ||
            bio != user.bio ||
            links != user.links ||
            gender != user.gender ||
            dob != user.dob ||
            phone != user.phone ||
            category != user.category ||
            isPrivateAccount != user.isPrivateAccount ||
            showEmailOnProfile != user.showEmailOnProfile ||
            showPhoneOnProfile != user.showPhoneOnProfile ||
            hideFollowersCount != user.hideFollowersCount ||
            isAiGeneratedProfile != user.isAiGeneratedProfile ||
            photoBase64 != user.photoBase64
        }
    }

    // Intercept back navigation
    BackHandler(enabled = true) {
        if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    // Cloudinary Photo Upload State
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var photoUploadProgress by remember { mutableStateOf(0) }
    var photoUploadError by remember { mutableStateOf<String?>(null) }
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var lastPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    fun retryPhotoUpload() {
        val uri = lastPhotoUri
        val bmp = lastPhotoBitmap
        if (uri != null) {
            photoUploadError = null
            photoUploadProgress = 0
            isUploadingPhoto = true
            coroutineScope.launch {
                val res = CloudinaryUploader.uploadUri(
                    context = context,
                    uri = uri,
                    onProgress = { p -> photoUploadProgress = p }
                )
                isUploadingPhoto = false
                if (res.isSuccess) {
                    val result = res.getOrNull()
                    if (result != null) {
                        avatarUrl = result.secureUrl
                        viewModel.updateUserAvatar(result.secureUrl)
                        onToast("Photo uploaded to Cloudinary! ☁️")
                    }
                } else {
                    photoUploadError = res.exceptionOrNull()?.localizedMessage ?: "Retry failed"
                    onToast("Upload failed: $photoUploadError")
                }
            }
        } else if (bmp != null) {
            photoUploadError = null
            photoUploadProgress = 0
            isUploadingPhoto = true
            coroutineScope.launch {
                val res = CloudinaryUploader.uploadBitmap(
                    bitmap = bmp,
                    onProgress = { p -> photoUploadProgress = p }
                )
                isUploadingPhoto = false
                if (res.isSuccess) {
                    val result = res.getOrNull()
                    if (result != null) {
                        avatarUrl = result.secureUrl
                        viewModel.updateUserAvatar(result.secureUrl)
                        onToast("Photo uploaded to Cloudinary! ☁️")
                    }
                } else {
                    photoUploadError = res.exceptionOrNull()?.localizedMessage ?: "Retry failed"
                    onToast("Upload failed: $photoUploadError")
                }
            }
        }
    }

    // Media Pickers: photo is compressed on the phone and stored with the profile (no Cloudinary needed)
    fun applyPhoto(compressed: String?) {
        if (compressed.isNullOrBlank()) {
            photoUploadError = "Photo load nahi hui, dobara try karo"
            onToast("Photo load nahi hui, dobara try karo")
            return
        }
        val raw = if (compressed.contains("base64,")) compressed.substringAfter("base64,") else compressed
        photoUploadError = null
        photoBase64 = raw
        avatarUrl = "data:image/jpeg;base64,$raw"
        viewModel.updateUserAvatar(avatarUrl)
        onToast("Photo set ho gayi ✓")
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            applyPhoto(ImageCompressUtil.uriToCompressedBase64(context, uri, 400))
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            applyPhoto(ImageCompressUtil.bitmapToCompressedBase64(bitmap, 400))
        }
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = VynoraStrings.editProfileTitle(currentLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (hasUnsavedChanges) showUnsavedDialog = true
                            else onBack()
                        },
                        modifier = Modifier.testTag("edit_profile_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (fullName.isBlank()) {
                                errorMessage = "Full name cannot be blank"
                                return@TextButton
                            }
                            if (username.isBlank()) {
                                errorMessage = "Username cannot be blank"
                                return@TextButton
                            }
                            if (username != user.username && isUsernameOnCooldown) {
                                errorMessage = VynoraStrings.usernameCooldownWarning(daysRemainingForUsername, currentLanguage)
                                return@TextButton
                            }
                            if (username != user.username && VynoraOfficial.isReservedUsername(username)) {
                                errorMessage = "Username '$username' is reserved by VYNORA"
                                return@TextButton
                            }

                            isSaving = true
                            errorMessage = null

                            coroutineScope.launch {
                                // Live check username availability if changed
                                if (username != user.username) {
                                    val available = viewModel.checkUsernameAvailability(username)
                                    if (!available) {
                                        isSaving = false
                                        errorMessage = VynoraStrings.usernameTaken(currentLanguage)
                                        return@launch
                                    }
                                }

                                val updated = user.copy(
                                    fullName = fullName.trim(),
                                    username = username.lowercase().trim(),
                                    pronouns = pronouns.trim(),
                                    bio = bio.take(150),
                                    links = links.filter { it.isNotBlank() },
                                    website = links.firstOrNull() ?: "",
                                    gender = gender,
                                    dob = dob.trim(),
                                    phone = phone.filter { it.isDigit() || it == '+' },
                                    category = category,
                                    isPrivateAccount = isPrivateAccount,
                                    showEmailOnProfile = showEmailOnProfile,
                                    showPhoneOnProfile = showPhoneOnProfile,
                                    hideFollowersCount = hideFollowersCount,
                                    isAiGeneratedProfile = isAiGeneratedProfile,
                                    photoBase64 = photoBase64,
                                    avatarUrl = if (photoBase64.isNotBlank()) "data:image/jpeg;base64,$photoBase64" else avatarUrl
                                )

                                viewModel.saveFullUserProfile(
                                    updatedUser = updated,
                                    onSuccess = {
                                        isSaving = false
                                        onToast(VynoraStrings.profileSavedSuccess(currentLanguage))
                                        onBack()
                                    },
                                    onError = { err ->
                                        isSaving = false
                                        errorMessage = err
                                    }
                                )
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.testTag("save_profile_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = VynoraNeonCyan, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = "Save",
                                color = VynoraNeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = screenBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Profile Photo Section ---
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .border(
                                2.5.dp,
                                Brush.sweepGradient(listOf(VynoraNeonPink, VynoraNeonCyan, VynoraNeonPink)),
                                CircleShape
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2438))
                            .clickable { showPhotoOptionsDialog = true }
                            .testTag("edit_profile_avatar"),
                        contentAlignment = Alignment.Center
                    ) {
                        StoryRingAvatar(
                            avatarUrl = avatarUrl,
                            hasStory = false,
                            size = 92.dp,
                            onClick = { showPhotoOptionsDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showPhotoOptionsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonCyan.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, VynoraNeonCyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("change_photo_button")
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = VynoraNeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(VynoraStrings.changePhoto(currentLanguage), color = VynoraNeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (photoBase64.isNotBlank() || avatarUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    photoBase64 = ""
                                    avatarUrl = ""
                                    onToast("Photo removed")
                                },
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("remove_photo_button")
                            ) {
                                Text(VynoraStrings.removePhoto(currentLanguage), color = Color(0xFFEF4444), fontSize = 12.sp)
                            }
                        }
                    }

                    // Cloudinary Upload Progress
                    if (isUploadingPhoto) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { photoUploadProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = VynoraNeonCyan,
                            trackColor = Color(0xFF1E2438)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Uploading to Cloudinary... $photoUploadProgress%",
                            fontSize = 11.5.sp,
                            color = VynoraNeonCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Cloudinary Upload Error with Retry
                    if (photoUploadError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Photo upload error: $photoUploadError",
                            fontSize = 11.5.sp,
                            color = Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { retryPhotoUpload() },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Retry Upload", color = Color(0xFFEF4444), fontSize = 11.5.sp)
                        }
                    }
                }
            }

            // --- VYNORA Plus Status (Blue Badge) ---
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, if (user.isPlusActive) VynoraNeonCyan.copy(alpha = 0.5f) else Color(0x2264748B)),
                modifier = Modifier.fillMaxWidth()
            ) {
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
                        VynoraPlusBadge(size = 28.dp)
                        Column {
                            Text(
                                text = VynoraStrings.vynoraPlusStatusTitle(currentLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = textPrimary
                            )
                            Text(
                                text = if (user.isPlusActive) {
                                    if (user.isOfficial) "Permanent Official Creator"
                                    else "${VynoraStrings.vynoraPlusActive(currentLanguage)} (${user.daysRemaining} days left)"
                                } else VynoraStrings.vynoraPlusInactive(currentLanguage),
                                fontSize = 12.sp,
                                color = if (user.isPlusActive) VynoraNeonCyan else textSecondary
                            )
                        }
                    }

                    if (!user.isPlusActive) {
                        Button(
                            onClick = {
                                viewModel.navigateTo(ScreenDestination.GetBadge)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(VynoraStrings.getVynoraPlusButton(currentLanguage), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- Basic Info Section ---
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Display Name
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; errorMessage = null },
                        label = { Text(VynoraStrings.fullNameLabel(currentLanguage)) },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = VynoraNeonPink) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VynoraNeonPink,
                            focusedContainerColor = fieldBg,
                            unfocusedContainerColor = fieldBg
                        )
                    )

                    // Username (@handle) with 14-day check
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                if (!isUsernameOnCooldown) {
                                    username = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' }.lowercase()
                                    errorMessage = null
                                }
                            },
                            readOnly = isUsernameOnCooldown,
                            label = { Text(VynoraStrings.usernameLabel(currentLanguage)) },
                            leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null, tint = VynoraNeonCyan) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_username_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VynoraNeonCyan,
                                focusedContainerColor = fieldBg,
                                unfocusedContainerColor = fieldBg
                            )
                        )

                        if (isUsernameOnCooldown) {
                            Text(
                                text = VynoraStrings.usernameCooldownWarning(daysRemainingForUsername, currentLanguage),
                                color = Color(0xFFFBBF24),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        } else {
                            Text(
                                text = "Can be changed once every 14 days",
                                color = textSecondary,
                                fontSize = 10.5.sp,
                                modifier = Modifier.padding(top = 3.dp, start = 4.dp)
                            )
                        }
                    }

                    // Pronouns
                    OutlinedTextField(
                        value = pronouns,
                        onValueChange = { pronouns = it },
                        label = { Text(VynoraStrings.pronounsLabel(currentLanguage)) },
                        leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null, tint = Color(0xFFA855F7)) },
                        placeholder = { Text("e.g. they/them, she/her, he/him") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_pronouns_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFA855F7),
                            focusedContainerColor = fieldBg,
                            unfocusedContainerColor = fieldBg
                        )
                    )

                    // Bio (150 Char Counter)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = bio,
                            onValueChange = {
                                if (it.length <= 150) {
                                    bio = it
                                }
                            },
                            label = { Text(VynoraStrings.bioLabel(currentLanguage)) },
                            leadingIcon = { Icon(Icons.Outlined.EditNote, contentDescription = null, tint = VynoraNeonCyan) },
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth().testTag("edit_bio_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VynoraNeonCyan,
                                focusedContainerColor = fieldBg,
                                unfocusedContainerColor = fieldBg
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${bio.length} / 150",
                                fontSize = 11.sp,
                                color = if (bio.length >= 140) Color(0xFFEF4444) else textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Category Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(VynoraStrings.categoryLabel(currentLanguage)) },
                            leadingIcon = { Icon(Icons.Outlined.Category, contentDescription = null, tint = Color(0xFF10B981)) },
                            trailingIcon = {
                                IconButton(onClick = { categoryDropdownExpanded = !categoryDropdownExpanded }) {
                                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Select Category")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { categoryDropdownExpanded = true }
                                .testTag("category_dropdown"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                focusedContainerColor = fieldBg,
                                unfocusedContainerColor = fieldBg
                            )
                        )
                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            listOf("Creator", "Writer", "Artist", "Musician", "Business", "Gamer", "Developer", "Personal Blog", "Community").forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Gender Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(VynoraStrings.genderLabel(currentLanguage)) },
                            leadingIcon = { Icon(Icons.Outlined.Wc, contentDescription = null, tint = VynoraNeonPink) },
                            trailingIcon = {
                                IconButton(onClick = { genderDropdownExpanded = !genderDropdownExpanded }) {
                                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Select Gender")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { genderDropdownExpanded = true }
                                .testTag("gender_dropdown"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VynoraNeonPink,
                                focusedContainerColor = fieldBg,
                                unfocusedContainerColor = fieldBg
                            )
                        )
                        DropdownMenu(
                            expanded = genderDropdownExpanded,
                            onDismissRequest = { genderDropdownExpanded = false }
                        ) {
                            listOf(
                                VynoraStrings.genderMale(currentLanguage) to "Male",
                                VynoraStrings.genderFemale(currentLanguage) to "Female",
                                VynoraStrings.genderOther(currentLanguage) to "Other",
                                VynoraStrings.genderPreferNot(currentLanguage) to "Prefer not to say"
                            ).forEach { (display, raw) ->
                                DropdownMenuItem(
                                    text = { Text(display) },
                                    onClick = {
                                        gender = raw
                                        genderDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- Links Section (Add up to 3) ---
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = VynoraStrings.linksTitle(currentLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textPrimary
                        )
                        Text(
                            text = "${links.size} / 3",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }

                    links.forEachIndexed { index, link ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = link,
                                onValueChange = { newUrl ->
                                    val mutable = links.toMutableList()
                                    mutable[index] = newUrl
                                    links = mutable
                                },
                                label = { Text("Link ${index + 1}") },
                                leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null, tint = VynoraNeonCyan) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("link_input_$index"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VynoraNeonCyan,
                                    focusedContainerColor = fieldBg,
                                    unfocusedContainerColor = fieldBg
                                )
                            )
                            IconButton(
                                onClick = {
                                    val mutable = links.toMutableList()
                                    mutable.removeAt(index)
                                    links = mutable
                                }
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Remove Link", tint = Color(0xFFEF4444))
                            }
                        }
                    }

                    if (links.size < 3) {
                        OutlinedButton(
                            onClick = {
                                if (links.size < 3) {
                                    links = links + ""
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, VynoraNeonCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().testTag("add_link_button")
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, tint = VynoraNeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(VynoraStrings.addLink(currentLanguage), color = VynoraNeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- Private / Contact Info Section ---
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Private Information",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textPrimary
                    )

                    // Email (Read-Only)
                    OutlinedTextField(
                        value = user.email,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Email Address (Read-only)") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = textSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("readonly_email_field"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0x3364748B),
                            unfocusedBorderColor = Color(0x3364748B),
                            focusedContainerColor = fieldBg.copy(alpha = 0.6f),
                            unfocusedContainerColor = fieldBg.copy(alpha = 0.6f)
                        )
                    )

                    // Mobile Number (Private)
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("${VynoraStrings.mobileNumberLabel(currentLanguage)} (Private)") },
                        leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null, tint = Color(0xFF10B981)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        supportingText = { Text(VynoraStrings.mobilePrivateNote(currentLanguage), color = textSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_phone_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            focusedContainerColor = fieldBg,
                            unfocusedContainerColor = fieldBg
                        )
                    )

                    // Date of Birth (Private)
                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("${VynoraStrings.dobLabel(currentLanguage)} (Private)") },
                        leadingIcon = { Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = Color(0xFFA855F7)) },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        supportingText = { Text(VynoraStrings.dobPrivateNote(currentLanguage), color = textSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_dob_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFA855F7),
                            focusedContainerColor = fieldBg,
                            unfocusedContainerColor = fieldBg
                        )
                    )
                }
            }

            // --- Contact Options & Profile Visibility Toggles ---
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = VynoraStrings.contactOptionsTitle(currentLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textPrimary
                    )

                    // Show Email Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(VynoraStrings.showEmailToggle(currentLanguage), fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                            Text("Allow visitors to email you", fontSize = 11.sp, color = textSecondary)
                        }
                        Switch(
                            checked = showEmailOnProfile,
                            onCheckedChange = { showEmailOnProfile = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = VynoraNeonCyan, checkedTrackColor = VynoraNeonCyan.copy(alpha = 0.3f))
                        )
                    }

                    HorizontalDivider(color = Color(0x1A64748B))

                    // Show Phone Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(VynoraStrings.showPhoneToggle(currentLanguage), fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                            Text("Allow visitors to see phone button", fontSize = 11.sp, color = textSecondary)
                        }
                        Switch(
                            checked = showPhoneOnProfile,
                            onCheckedChange = { showPhoneOnProfile = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = VynoraNeonCyan, checkedTrackColor = VynoraNeonCyan.copy(alpha = 0.3f))
                        )
                    }

                    HorizontalDivider(color = Color(0x1A64748B))

                    // Private Account Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(VynoraStrings.privateAccountTitle(currentLanguage), fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                            Text(VynoraStrings.privateAccountDesc(currentLanguage), fontSize = 11.sp, color = textSecondary)
                        }
                        Switch(
                            checked = isPrivateAccount,
                            onCheckedChange = { isPrivateAccount = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = VynoraNeonPink, checkedTrackColor = VynoraNeonPink.copy(alpha = 0.3f))
                        )
                    }

                    HorizontalDivider(color = Color(0x1A64748B))

                    // Hide Followers Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(VynoraStrings.hideFollowersToggle(currentLanguage), fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                            Text("Followers and following counts will be hidden", fontSize = 11.sp, color = textSecondary)
                        }
                        Switch(
                            checked = hideFollowersCount,
                            onCheckedChange = { hideFollowersCount = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = VynoraNeonCyan, checkedTrackColor = VynoraNeonCyan.copy(alpha = 0.3f))
                        )
                    }

                    HorizontalDivider(color = Color(0x1A64748B))

                    // AI Generated Profile Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(VynoraStrings.aiProfileToggle(currentLanguage), fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                            Text(VynoraStrings.aiProfileDesc(currentLanguage), fontSize = 11.sp, color = textSecondary)
                        }
                        Switch(
                            checked = isAiGeneratedProfile,
                            onCheckedChange = { isAiGeneratedProfile = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFA855F7), checkedTrackColor = Color(0xFFA855F7).copy(alpha = 0.3f))
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Surface(
                    color = Color(0x22EF4444),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Save CTA Button
            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        errorMessage = "Full name cannot be blank"
                        return@Button
                    }
                    if (username.isBlank()) {
                        errorMessage = "Username cannot be blank"
                        return@Button
                    }
                    if (username != user.username && isUsernameOnCooldown) {
                        errorMessage = VynoraStrings.usernameCooldownWarning(daysRemainingForUsername, currentLanguage)
                        return@Button
                    }
                    isSaving = true
                    errorMessage = null

                    coroutineScope.launch {
                        if (username != user.username) {
                            val available = viewModel.checkUsernameAvailability(username)
                            if (!available) {
                                isSaving = false
                                errorMessage = VynoraStrings.usernameTaken(currentLanguage)
                                return@launch
                            }
                        }

                        val updated = user.copy(
                            fullName = fullName.trim(),
                            username = username.lowercase().trim(),
                            pronouns = pronouns.trim(),
                            bio = bio.take(150),
                            links = links.filter { it.isNotBlank() },
                            website = links.firstOrNull() ?: "",
                            gender = gender,
                            dob = dob.trim(),
                            phone = phone.filter { it.isDigit() || it == '+' },
                            category = category,
                            isPrivateAccount = isPrivateAccount,
                            showEmailOnProfile = showEmailOnProfile,
                            showPhoneOnProfile = showPhoneOnProfile,
                            hideFollowersCount = hideFollowersCount,
                            isAiGeneratedProfile = isAiGeneratedProfile,
                            photoBase64 = photoBase64,
                            avatarUrl = if (photoBase64.isNotBlank()) "data:image/jpeg;base64,$photoBase64" else avatarUrl
                        )

                        viewModel.saveFullUserProfile(
                            updatedUser = updated,
                            onSuccess = {
                                isSaving = false
                                onToast(VynoraStrings.profileSavedSuccess(currentLanguage))
                                onBack()
                            },
                            onError = { err ->
                                isSaving = false
                                errorMessage = err
                            }
                        )
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_profile_bottom_button"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = VynoraStrings.saveProfileButton(currentLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --- Photo Options Dialog (Gallery or Camera) ---
    if (showPhotoOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = cardBg,
            title = {
                Text(VynoraStrings.changePhoto(currentLanguage), fontWeight = FontWeight.Bold, color = textPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            showPhotoOptionsDialog = false
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonCyan.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, VynoraNeonCyan)
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = VynoraNeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(VynoraStrings.pickFromGallery(currentLanguage), color = VynoraNeonCyan, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showPhotoOptionsDialog = false
                            cameraLauncher.launch(null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, VynoraNeonPink)
                    ) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = VynoraNeonPink)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(VynoraStrings.takeCameraPhoto(currentLanguage), color = VynoraNeonPink, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoOptionsDialog = false }) {
                    Text(VynoraStrings.cancel(currentLanguage), color = textSecondary)
                }
            }
        )
    }

    // --- Unsaved Changes Dialog ---
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = cardBg,
            title = {
                Text(
                    text = VynoraStrings.unsavedChangesTitle(currentLanguage),
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            },
            text = {
                Text(
                    text = VynoraStrings.unsavedChangesDesc(currentLanguage),
                    color = textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("confirm_discard_button")
                ) {
                    Text(VynoraStrings.discard(currentLanguage), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUnsavedDialog = false },
                    modifier = Modifier.testTag("keep_editing_button")
                ) {
                    Text(VynoraStrings.keepEditing(currentLanguage), color = VynoraNeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
