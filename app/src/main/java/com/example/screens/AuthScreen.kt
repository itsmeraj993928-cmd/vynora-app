package com.example.screens

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.components.GoogleSignInButton
import com.example.components.VynoraLogoIcon
import com.example.i18n.AppLanguage
import com.example.i18n.VynoraStrings
import com.example.ui.theme.VynoraBlack
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink
import com.example.util.ImageCompressUtil
import com.example.util.VynoraOfficial
import com.example.viewmodel.VynoraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AvailabilityStatus {
    IDLE, CHECKING, AVAILABLE, TAKEN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    currentLanguage: AppLanguage,
    viewModel: VynoraViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isSigningIn by remember { mutableStateOf(false) }
    var isProfileSetupMode by remember { mutableStateOf(false) }
    var isSignUpTab by remember { mutableStateOf(false) } // Default to Sign In tab so app launches directly on Sign In!
    var signUpStep by remember { mutableStateOf(1) } // 1..2
    var signUpWithPhone by remember { mutableStateOf(false) }
    // One mobile number can have up to 5 accounts: slot 0 = "number", slot 1..4 = "number.N"
    fun toAuthEmail(input: String, slot: Int = 0): String {
        val t = input.trim()
        if (t.contains("@")) return t
        var d = t.filter { it.isDigit() }
        if (d.length == 10) d = "91$d"
        return if (slot == 0) "$d@phone.vynora.app" else "$d.$slot@phone.vynora.app"
    }

    // --- Sign In & Step 1 Fields ---
    var email by remember { mutableStateOf(viewModel.prefillIdentifier) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    // --- Forgot Password Dialog State ---
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var isSendingReset by remember { mutableStateOf(false) }
    var forgotPasswordSuccess by remember { mutableStateOf(false) }
    var forgotPasswordError by remember { mutableStateOf<String?>(null) }

    // --- Step 2 Fields: Profile Basics ---
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var usernameStatus by remember { mutableStateOf(AvailabilityStatus.IDLE) }
    var dobDay by remember { mutableStateOf("") }
    var dobMonth by remember { mutableStateOf("") }
    var dobYear by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Prefer not to say") }
    var genderDropdownExpanded by remember { mutableStateOf(false) }

    // --- Step 3 Fields: Mobile Number & Optional OTP ---
    var selectedCountryCode by remember { mutableStateOf("+91") }
    var countryCodeExpanded by remember { mutableStateOf(false) }
    var mobileNumber by remember { mutableStateOf("") }
    var phoneStatus by remember { mutableStateOf(AvailabilityStatus.IDLE) }
    var showOtpSection by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var isPhoneVerified by remember { mutableStateOf(false) }
    var isOtpSent by remember { mutableStateOf(false) }

    // --- Step 4 Fields: Profile Photo (Mandatory) ---
    var photoBase64 by remember { mutableStateOf("") }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    // --- Step 5 Fields: Terms & Age Gate ---
    var termsAgreed by remember { mutableStateOf(false) }
    var is13OrOlder by remember { mutableStateOf(false) }
    var contentRulesAccepted by remember { mutableStateOf(false) }
    var referralCode by remember { mutableStateOf("") }
    var activeLegalModal by remember { mutableStateOf<String?>(null) } // "terms", "privacy", "guidelines"

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Media Pickers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val compressed = ImageCompressUtil.uriToCompressedBase64(context, uri, 400)
            if (compressed != null) {
                photoBase64 = compressed
                errorMessage = null
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val compressed = ImageCompressUtil.bitmapToCompressedBase64(bitmap, 400)
            photoBase64 = compressed
            errorMessage = null
        }
    }

    // Google Sign-In with safe Activity context resolution
    fun startGoogleSignIn() {
        val hostActivity = when (context) {
            is Activity -> context
            is ContextWrapper -> {
                var cur: Any? = context
                while (cur is ContextWrapper) {
                    if (cur is Activity) break
                    cur = cur.baseContext
                }
                cur as? Activity
            }
            else -> null
        }

        if (hostActivity == null) {
            errorMessage = "Google Sign-In requires an active screen window."
            return
        }

        isSigningIn = true
        errorMessage = null

        viewModel.signInWithGoogle(
            activity = hostActivity,
            onProfileSetupRequired = { user ->
                isSigningIn = false
                val suggestedUsername = (user.email?.substringBefore("@") ?: "vynora_user")
                    .filter { it.isLetterOrDigit() || it == '_' }
                    .lowercase()
                username = suggestedUsername
                fullName = user.displayName ?: ""
                isProfileSetupMode = true
            },
            onSuccess = {
                isSigningIn = false
                onAuthSuccess()
            },
            onError = { err ->
                isSigningIn = false
                errorMessage = err
            }
        )
    }

    // Live Debounced Username Availability Check
    LaunchedEffect(username) {
        if (username.isBlank() || username.length < 3 || VynoraOfficial.isReservedUsername(username)) {
            usernameStatus = AvailabilityStatus.IDLE
            return@LaunchedEffect
        }
        usernameStatus = AvailabilityStatus.CHECKING
        delay(400) // debounce
        val isAvail = viewModel.checkUsernameAvailability(username)
        usernameStatus = if (isAvail) AvailabilityStatus.AVAILABLE else AvailabilityStatus.TAKEN
    }

    // Age validation helper (Must be 18+ years old)
    fun isAgeValid(): Boolean {
        val y = dobYear.toIntOrNull() ?: return false
        val m = dobMonth.toIntOrNull() ?: return false
        val d = dobDay.toIntOrNull() ?: return false
        if (y !in 1920..Calendar.getInstance().get(Calendar.YEAR) || m !in 1..12 || d !in 1..31) return false

        val dobCal = Calendar.getInstance().apply { set(y, m - 1, d) }
        val nowCal = Calendar.getInstance()
        var age = nowCal.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
        if (nowCal.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age >= 18
    }

    fun formattedDob(): String {
        val y = dobYear.padStart(4, '0')
        val m = dobMonth.padStart(2, '0')
        val d = dobDay.padStart(2, '0')
        return "$y-$m-$d"
    }

    // Phone format validation
    fun isPhoneFormatValid(): Boolean {
        val digits = mobileNumber.filter { it.isDigit() }
        return when (selectedCountryCode) {
            "+91" -> digits.length == 10 && digits.firstOrNull() in listOf('6', '7', '8', '9')
            "+1" -> digits.length == 10
            "+44" -> digits.length in 10..11
            else -> digits.length in 7..14
        }
    }

    val fullPhoneNumber = "$selectedCountryCode${mobileNumber.filter { it.isDigit() }}"

    // Live Phone Availability Check
    LaunchedEffect(mobileNumber, selectedCountryCode) {
        if (!isPhoneFormatValid()) {
            phoneStatus = AvailabilityStatus.IDLE
            return@LaunchedEffect
        }
        phoneStatus = AvailabilityStatus.CHECKING
        delay(400)
        val isAvail = viewModel.checkPhoneAvailability(fullPhoneNumber)
        phoneStatus = if (isAvail) AvailabilityStatus.AVAILABLE else AvailabilityStatus.TAKEN
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VynoraBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VynoraLogoIcon(size = 64.dp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "VYNORA",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            letterSpacing = 4.sp,
            color = Color.White
        )
        Text(
            text = VynoraStrings.tagline(currentLanguage),
            fontSize = 12.5.sp,
            color = VynoraNeonCyan.copy(alpha = 0.9f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (!isProfileSetupMode) {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE0E111F)),
                border = BorderStroke(
                    1.2.dp,
                    Brush.horizontalGradient(
                        listOf(
                            VynoraNeonPink.copy(alpha = 0.55f),
                            Color(0xFF7928CA).copy(alpha = 0.4f),
                            VynoraNeonCyan.copy(alpha = 0.55f)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Auth Tabs: Sign In / Create Account
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF1E2438))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (!isSignUpTab) VynoraNeonPink else Color.Transparent)
                                .clickable {
                                    isSignUpTab = false
                                    errorMessage = null
                                }
                                .testTag("tab_signin"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = VynoraStrings.signInTab(currentLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (!isSignUpTab) Color.White else Color(0xFF94A3B8)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSignUpTab) VynoraNeonCyan else Color.Transparent)
                                .clickable {
                                    isSignUpTab = true
                                    errorMessage = null
                                }
                                .testTag("tab_signup"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = VynoraStrings.signUpTab(currentLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSignUpTab) Color.Black else Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ----------------------------------------------------
                    // TAB 1: SIGN IN SCREEN
                    // ----------------------------------------------------
                    if (!isSignUpTab) {
                        // Modern Material 3 Neon Google Sign-In Button
                        GoogleSignInButton(
                            onClick = { startGoogleSignIn() },
                            isLoading = isSigningIn,
                            enabled = !isSigningIn,
                            text = "Sign in with Google",
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_signin_button")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Modern Neon Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = VynoraNeonCyan.copy(alpha = 0.25f)
                            )
                            Text(
                                text = "  OR SIGN IN WITH EMAIL  ",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = VynoraNeonCyan.copy(alpha = 0.25f)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMessage = null },
                            label = { Text("Email or Mobile number") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Email, contentDescription = null, tint = VynoraNeonCyan)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("email_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VynoraNeonCyan,
                                focusedContainerColor = Color(0xFF131726),
                                unfocusedContainerColor = Color(0xFF131726)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = { Text(VynoraStrings.passwordLabel(currentLanguage)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = VynoraNeonPink)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { isPasswordVisible = !isPasswordVisible },
                                    modifier = Modifier.testTag("toggle_password_visibility")
                                ) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("password_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VynoraNeonPink,
                                focusedContainerColor = Color(0xFF131726),
                                unfocusedContainerColor = Color(0xFF131726)
                            )
                        )

                        // Forgot Password Link
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = VynoraStrings.forgotPassword(currentLanguage),
                                color = VynoraNeonCyan,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable {
                                        forgotPasswordEmail = email
                                        forgotPasswordError = null
                                        forgotPasswordSuccess = false
                                        showForgotPasswordDialog = true
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                                    .testTag("forgot_password_button")
                            )
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Sign In Submit Button
                        Button(
                            onClick = {
                                isSigningIn = true
                                errorMessage = null
                                fun attemptLogin(slot: Int) {
                                    viewModel.signInWithEmail(
                                        email = toAuthEmail(email, slot),
                                        pass = password,
                                        onSuccess = {
                                            isSigningIn = false
                                            onAuthSuccess()
                                        },
                                        onError = { err ->
                                            if (!email.contains("@") && slot < 4) {
                                                attemptLogin(slot + 1)
                                            } else {
                                                isSigningIn = false
                                                errorMessage = err
                                            }
                                        }
                                    )
                                }
                                attemptLogin(0)
                            },
                            enabled = !isSigningIn && email.isNotBlank() && password.length >= 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("auth_submit_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VynoraNeonPink,
                                disabledContainerColor = VynoraNeonPink.copy(alpha = 0.35f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = "Sign In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Security guarantee badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = VynoraNeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Secured with Firebase Authentication",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        // ----------------------------------------------------
                        // TAB 2: MULTI-STEP SIGN UP FLOW (STEPS 1 TO 5)
                        // ----------------------------------------------------

                        // Stepped Progress Indicator
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = VynoraStrings.stepIndicator(signUpStep, 2, currentLanguage),
                                    color = VynoraNeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = when (signUpStep) {
                                        1 -> VynoraStrings.step1Title(currentLanguage)
                                        2 -> VynoraStrings.step2Title(currentLanguage)
                                        3 -> VynoraStrings.step3Title(currentLanguage)
                                        4 -> VynoraStrings.step4Title(currentLanguage)
                                        else -> VynoraStrings.step5Title(currentLanguage)
                                    },
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 5 Stepped Progress Segments
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                (1..2).forEach { stepIndex ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (stepIndex <= signUpStep) VynoraNeonCyan else Color(0xFF1E2438)
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // STEP 1: EMAIL & PASSWORDS
                        if (signUpStep == 1) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = !signUpWithPhone, onClick = { signUpWithPhone = false; errorMessage = null }, label = { Text("Email") })
                                FilterChip(selected = signUpWithPhone, onClick = { signUpWithPhone = true; errorMessage = null }, label = { Text("Mobile number") })
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (!signUpWithPhone) {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it; errorMessage = null },
                                    label = { Text("Email Address") },
                                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = VynoraNeonCyan) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("signup_email_input"),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            } else {
                                OutlinedTextField(
                                    value = mobileNumber,
                                    onValueChange = { v -> if (v.length <= 10 && v.all { c -> c.isDigit() }) { mobileNumber = v; selectedCountryCode = "+91" }; errorMessage = null },
                                    label = { Text("Mobile number (India)") },
                                    prefix = { Text("+91 ") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    isError = phoneStatus == AvailabilityStatus.TAKEN,
                                    supportingText = { if (phoneStatus == AvailabilityStatus.TAKEN) Text("This number is already registered") },
                                    modifier = Modifier.fillMaxWidth().testTag("signup_phone_input"),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it; errorMessage = null },
                                label = { Text(VynoraStrings.passwordLabel(currentLanguage)) },
                                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = VynoraNeonPink) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { isPasswordVisible = !isPasswordVisible },
                                        modifier = Modifier.testTag("toggle_signup_password_visibility")
                                    ) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8)
                                        )
                                    }
                                },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("signup_password_input"),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VynoraNeonPink,
                                    focusedContainerColor = Color(0xFF131726),
                                    unfocusedContainerColor = Color(0xFF131726)
                                )
                            )

                            // Password Strength Indicator
                            val strength = calculatePasswordStrength(password)
                            if (password.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val activeColor = when (strength) {
                                        PasswordStrength.WEAK -> Color(0xFFEF4444)
                                        PasswordStrength.MEDIUM -> Color(0xFFFBBF24)
                                        PasswordStrength.STRONG -> Color(0xFF10B981)
                                        PasswordStrength.NONE -> Color.Transparent
                                    }
                                    Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(activeColor))
                                    Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (strength == PasswordStrength.MEDIUM || strength == PasswordStrength.STRONG) activeColor else Color(0xFF334155)))
                                    Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (strength == PasswordStrength.STRONG) activeColor else Color(0xFF334155)))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (false) {
// Confirm Password
                            val isMismatch = confirmPassword.isNotEmpty() && password != confirmPassword
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; errorMessage = null },
                                label = { Text(VynoraStrings.confirmPasswordLabel(currentLanguage)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = if (confirmPassword.isNotEmpty() && !isMismatch) Color(0xFF10B981) else VynoraNeonPink
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                                        modifier = Modifier.testTag("toggle_signup_confirm_password")
                                    ) {
                                        Icon(
                                            imageVector = if (isConfirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8)
                                        )
                                    }
                                },
                                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("signup_confirm_password_input"),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isMismatch) Color(0xFFEF4444) else VynoraNeonPink,
                                    unfocusedBorderColor = if (isMismatch) Color(0xFFEF4444) else Color(0x3364748B),
                                    focusedContainerColor = Color(0xFF131726),
                                    unfocusedContainerColor = Color(0xFF131726)
                                )
                            )

                            if (isMismatch) {
                                Text(
                                    text = VynoraStrings.passwordsDoNotMatch(currentLanguage),
                                    color = Color(0xFFEF4444),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                )
                            }
                        }
}

                        // STEP 2: PROFILE DETAILS (NAME, USERNAME, DOB, GENDER)
                        if (signUpStep == 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it; errorMessage = null },
                                label = { Text(VynoraStrings.fullNameLabel(currentLanguage)) },
                                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = VynoraNeonPink) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("step2_name_input"),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VynoraNeonPink,
                                    focusedContainerColor = Color(0xFF131726),
                                    unfocusedContainerColor = Color(0xFF131726)
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Username with live check
                            OutlinedTextField(
                                value = username,
                                onValueChange = {
                                    username = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' }.lowercase()
                                    errorMessage = null
                                },
                                label = { Text(VynoraStrings.usernameLabel(currentLanguage)) },
                                leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null, tint = VynoraNeonCyan) },
                                trailingIcon = {
                                    when (usernameStatus) {
                                        AvailabilityStatus.CHECKING -> CircularProgressIndicator(modifier = Modifier.size(16.dp), color = VynoraNeonCyan, strokeWidth = 2.dp)
                                        AvailabilityStatus.AVAILABLE -> Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                        AvailabilityStatus.TAKEN -> Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444))
                                        AvailabilityStatus.IDLE -> Unit
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("step2_username_input"),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = when (usernameStatus) {
                                        AvailabilityStatus.AVAILABLE -> Color(0xFF10B981)
                                        AvailabilityStatus.TAKEN -> Color(0xFFEF4444)
                                        else -> VynoraNeonCyan
                                    },
                                    focusedContainerColor = Color(0xFF131726),
                                    unfocusedContainerColor = Color(0xFF131726)
                                )
                            )

                            if (usernameStatus == AvailabilityStatus.AVAILABLE) {
                                Text(VynoraStrings.usernameAvailable(currentLanguage), color = Color(0xFF10B981), fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                            } else if (usernameStatus == AvailabilityStatus.TAKEN) {
                                Text(VynoraStrings.usernameTaken(currentLanguage), color = Color(0xFFEF4444), fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                            }

                        }
                        if (signUpStep == 2) {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Date of Birth (DD / MM / YYYY - 18+ only)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = VynoraStrings.dobLabel(currentLanguage),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = dobDay,
                                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) dobDay = it },
                                        placeholder = { Text("DD") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("dob_day"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = dobMonth,
                                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) dobMonth = it },
                                        placeholder = { Text("MM") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("dob_month"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = dobYear,
                                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) dobYear = it },
                                        placeholder = { Text("YYYY") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1.5f).testTag("dob_year"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }

                                if (dobDay.isNotBlank() && dobMonth.isNotBlank() && dobYear.length == 4) {
                                    if (!isAgeValid()) {
                                        Text(
                                            text = "You must be at least 18 years old to join VYNORA. / VYNORA से जुड़ने के लिए आपकी उम्र कम से कम 18 वर्ष होनी चाहिए।",
                                            color = Color(0xFFEF4444),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "Age verified (18+) ✓",
                                            color = Color(0xFF10B981),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (false) {
Spacer(modifier = Modifier.height(10.dp))

                            // Gender Dropdown
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = gender,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(VynoraStrings.genderLabel(currentLanguage)) },
                                    leadingIcon = { Icon(Icons.Outlined.Wc, contentDescription = null, tint = Color(0xFFA855F7)) },
                                    trailingIcon = {
                                        IconButton(onClick = { genderDropdownExpanded = !genderDropdownExpanded }) {
                                            Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Select Gender")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { genderDropdownExpanded = true }.testTag("signup_gender_dropdown"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFA855F7),
                                        focusedContainerColor = Color(0xFF131726),
                                        unfocusedContainerColor = Color(0xFF131726)
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

                        // STEP 3: MOBILE NUMBER WITH COUNTRY CODE & OPTIONAL OTP
                        if (signUpStep == 30) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = VynoraStrings.mobileNumberLabel(currentLanguage),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Country Code Picker (Default +91 India)
                                    Box {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFF131726),
                                            border = BorderStroke(1.dp, VynoraNeonCyan),
                                            modifier = Modifier
                                                .height(56.dp)
                                                .clickable { countryCodeExpanded = true }
                                                .padding(horizontal = 12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(selectedCountryCode, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = VynoraNeonCyan)
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = countryCodeExpanded,
                                            onDismissRequest = { countryCodeExpanded = false }
                                        ) {
                                            listOf(
                                                "+91 (India 🇮🇳)",
                                                "+1 (US/Canada 🇺🇸)",
                                                "+44 (UK 🇬🇧)",
                                                "+971 (UAE 🇦🇪)",
                                                "+966 (Saudi Arabia 🇸🇦)",
                                                "+65 (Singapore 🇸🇬)",
                                                "+61 (Australia 🇦🇺)",
                                                "+49 (Germany 🇩🇪)"
                                            ).forEach { codeOption ->
                                                DropdownMenuItem(
                                                    text = { Text(codeOption) },
                                                    onClick = {
                                                        selectedCountryCode = codeOption.substringBefore(" ")
                                                        countryCodeExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Phone Number Input
                                    OutlinedTextField(
                                        value = mobileNumber,
                                        onValueChange = {
                                            if (it.length <= 15 && it.all { c -> c.isDigit() }) {
                                                mobileNumber = it
                                                errorMessage = null
                                            }
                                        },
                                        placeholder = { Text("10-digit number") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        trailingIcon = {
                                            when (phoneStatus) {
                                                AvailabilityStatus.CHECKING -> CircularProgressIndicator(modifier = Modifier.size(16.dp), color = VynoraNeonCyan, strokeWidth = 2.dp)
                                                AvailabilityStatus.AVAILABLE -> Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                                AvailabilityStatus.TAKEN -> Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444))
                                                AvailabilityStatus.IDLE -> Unit
                                            }
                                        },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("step3_phone_input"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = VynoraNeonCyan,
                                            focusedContainerColor = Color(0xFF131726),
                                            unfocusedContainerColor = Color(0xFF131726)
                                        )
                                    )
                                }

                                if (phoneStatus == AvailabilityStatus.TAKEN) {
                                    Text(VynoraStrings.mobileTaken(currentLanguage), color = Color(0xFFEF4444), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                } else {
                                    Text(VynoraStrings.mobilePrivateNote(currentLanguage), color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Optional OTP Card (built for Firebase Phone Auth)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF131726),
                                border = BorderStroke(1.dp, if (isPhoneVerified) Color(0xFF10B981) else Color(0x3364748B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Outlined.Sms, contentDescription = null, tint = VynoraNeonCyan, modifier = Modifier.size(18.dp))
                                            Text(VynoraStrings.phoneAuthOptionalTitle(currentLanguage), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color.White)
                                        }
                                        if (isPhoneVerified) {
                                            Text(VynoraStrings.phoneVerifiedBadge(currentLanguage), color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (!isPhoneVerified) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        if (!isOtpSent) {
                                            Button(
                                                onClick = {
                                                    if (isPhoneFormatValid()) {
                                                        isOtpSent = true
                                                        viewModel.showToast(VynoraStrings.otpSentMessage(currentLanguage))
                                                    } else {
                                                        errorMessage = VynoraStrings.mobileInvalid(currentLanguage)
                                                    }
                                                },
                                                enabled = isPhoneFormatValid() && phoneStatus != AvailabilityStatus.TAKEN,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2438)),
                                                border = BorderStroke(1.dp, VynoraNeonCyan.copy(alpha = 0.5f)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth().testTag("send_otp_button")
                                            ) {
                                                Text(VynoraStrings.sendOtp(currentLanguage), color = VynoraNeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = otpCode,
                                                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) otpCode = it },
                                                    placeholder = { Text("6-digit code") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f).testTag("otp_code_input"),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                Button(
                                                    onClick = {
                                                        // Firebase Phone Auth verification logic / test code accepted
                                                        if (otpCode.length == 6) {
                                                            isPhoneVerified = true
                                                            viewModel.showToast("Phone verified successfully! ✓")
                                                        }
                                                    },
                                                    enabled = otpCode.length == 6,
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(VynoraStrings.verifyOtp(currentLanguage), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // STEP 4: MANDATORY PROFILE PHOTO (CROP TO CIRCLE, COMPRESS MAX 400x400 BASE64)
                        if (signUpStep == 40) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = VynoraStrings.step4Title(currentLanguage),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = VynoraStrings.profilePhotoDesc(currentLanguage),
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Circular Photo Preview Frame
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .border(
                                            3.dp,
                                            Brush.sweepGradient(listOf(VynoraNeonPink, VynoraNeonCyan, VynoraNeonPink)),
                                            CircleShape
                                        )
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF131726))
                                        .clickable { showPhotoSourceDialog = true }
                                        .testTag("signup_photo_preview"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (photoBase64.isNotBlank()) {
                                        val bitmap = remember(photoBase64) { ImageCompressUtil.base64ToBitmap(photoBase64) }
                                        if (bitmap != null) {
                                            androidx.compose.foundation.Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Profile Photo",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Outlined.Person, contentDescription = null, tint = VynoraNeonCyan, modifier = Modifier.size(56.dp))
                                        }
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Outlined.AddAPhoto, contentDescription = null, tint = VynoraNeonCyan, modifier = Modifier.size(36.dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Required", fontSize = 10.sp, color = VynoraNeonCyan)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                        colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonCyan.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, VynoraNeonCyan),
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier.testTag("signup_gallery_button")
                                    ) {
                                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = VynoraNeonCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(VynoraStrings.pickFromGallery(currentLanguage), color = VynoraNeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { cameraLauncher.launch(null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, VynoraNeonPink),
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier.testTag("signup_camera_button")
                                    ) {
                                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = VynoraNeonPink, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(VynoraStrings.takeCameraPhoto(currentLanguage), color = VynoraNeonPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (photoBase64.isBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = VynoraStrings.photoMandatoryError(currentLanguage),
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // STEP 5: TERMS OF SERVICE, PRIVACY POLICY & AGE VERIFICATION
                        if (signUpStep == 2) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Profile Summary Card
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF131726),
                                    border = BorderStroke(1.dp, Color(0x3300D1FF)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val bitmap = remember(photoBase64) { ImageCompressUtil.base64ToBitmap(photoBase64) }
                                        if (bitmap != null) {
                                            androidx.compose.foundation.Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.size(50.dp).clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Outlined.Person, contentDescription = null, tint = VynoraNeonCyan, modifier = Modifier.size(50.dp))
                                        }

                                        Column {
                                            Text(fullName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                            Text("@$username", color = VynoraNeonCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Text(if (signUpWithPhone) fullPhoneNumber else email, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Optional Referral Code
                                OutlinedTextField(
                                    value = referralCode,
                                    onValueChange = { referralCode = it.uppercase().trim() },
                                    label = { Text("Referral Code (Optional)") },
                                    leadingIcon = { Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = Color(0xFFFBBF24)) },
                                    placeholder = { Text("e.g. VIP2026") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("signup_referral_input"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFFBBF24),
                                        focusedContainerColor = Color(0xFF131726),
                                        unfocusedContainerColor = Color(0xFF131726)
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Checkbox 1: Terms of Service, Privacy Policy (DPDP Act) & Community Guidelines
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Checkbox(
                                        checked = termsAgreed,
                                        onCheckedChange = { termsAgreed = it },
                                        colors = CheckboxDefaults.colors(checkedColor = VynoraNeonCyan),
                                        modifier = Modifier.testTag("terms_checkbox")
                                    )
                                    Column(modifier = Modifier.padding(top = 10.dp)) {
                                        Text(
                                            text = "I agree to the Terms of Service, Privacy Policy (DPDP Act) and Community Guidelines",
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            lineHeight = 16.sp
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text(
                                                text = "Terms",
                                                color = VynoraNeonCyan,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable { activeLegalModal = "terms" }
                                            )
                                            Text("•", color = Color(0xFF64748B), fontSize = 11.sp)
                                            Text(
                                                text = "Privacy (DPDP Act)",
                                                color = VynoraNeonCyan,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable { activeLegalModal = "privacy" }
                                            )
                                            Text("•", color = Color(0xFF64748B), fontSize = 11.sp)
                                            Text(
                                                text = "Guidelines",
                                                color = VynoraNeonCyan,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable { activeLegalModal = "guidelines" }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Checkbox 2: 18+ Age Verification
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = is13OrOlder,
                                        onCheckedChange = { is13OrOlder = it },
                                        colors = CheckboxDefaults.colors(checkedColor = VynoraNeonPink),
                                        modifier = Modifier.testTag("age_checkbox")
                                    )
                                    Text(
                                        text = "I confirm that I am 18 years or older. / मैं पुष्टि करता/करती हूँ कि मेरी उम्र 18 वर्ष या उससे अधिक है।",
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Checkbox 3: Content rules (no nudity / 18+ / violence) - English + Hindi
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Checkbox(
                                        checked = contentRulesAccepted,
                                        onCheckedChange = { contentRulesAccepted = it },
                                        colors = CheckboxDefaults.colors(checkedColor = VynoraNeonPink),
                                        modifier = Modifier.testTag("content_rules_checkbox")
                                    )
                                    Column(modifier = Modifier.padding(top = 12.dp)) {
                                        Text(
                                            text = "I agree that VYNORA does not allow nudity, porn/18+ content, violence, hate or illegal content. If I post such content, my account can be suspended or permanently deleted without notice. I accept all the Terms, Privacy Policy and these rules.",
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "मैं सहमत हूँ कि VYNORA पर नग्नता, पोर्न/18+ सामग्री, हिंसा, नफ़रत या गैरकानूनी सामग्री डालना मना है। ऐसा करने पर मेरा अकाउंट बिना सूचना के बंद या हमेशा के लिए डिलीट किया जा सकता है। मैं सभी शर्तें, गोपनीयता नीति और ये नियम स्वीकार करता/करती हूँ।",
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Multi-Step Navigation: Back and Next / Create Account Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (signUpStep > 1) {
                                OutlinedButton(
                                    onClick = {
                                        signUpStep--
                                        errorMessage = null
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("step_back_button"),
                                    shape = RoundedCornerShape(24.dp),
                                    border = BorderStroke(1.dp, Color(0xFF64748B))
                                ) {
                                    Text(VynoraStrings.backButton(currentLanguage), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (signUpStep < 2) {
                                val isNextEnabled = when (signUpStep) {
                                    1 -> (if (signUpWithPhone) (isPhoneFormatValid() && phoneStatus != AvailabilityStatus.TAKEN) else (email.isNotBlank() && email.contains("@"))) && password.length >= 6 && fullName.isNotBlank() && username.length >= 3 && usernameStatus == AvailabilityStatus.AVAILABLE
                                    2 -> isAgeValid()
                                    3 -> isPhoneFormatValid() && phoneStatus != AvailabilityStatus.TAKEN
                                    4 -> photoBase64.isNotBlank()
                                    else -> true
                                }

                                Button(
                                    onClick = {
                                                                                signUpStep++
                                        errorMessage = null
                                    },
                                    enabled = isNextEnabled,
                                    modifier = Modifier.weight(1.5f).height(48.dp).testTag("step_next_button"),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonCyan)
                                ) {
                                    Text(VynoraStrings.nextButton(currentLanguage), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            } else {
                                // Step 5: Final "Create Account" CTA Button (stays disabled until terms, 18+ and content rules are ticked)
                                Button(
                                    onClick = {
                                        isSigningIn = true
                                        errorMessage = null
                                        fun attemptSignUp(slot: Int) {
                                            viewModel.signUpWithEmail(
                                                email = if (signUpWithPhone) toAuthEmail(fullPhoneNumber, slot) else email,
                                                pass = password,
                                                username = username,
                                                fullName = fullName,
                                                referralCode = referralCode.ifBlank { null },
                                                gender = gender,
                                                dob = formattedDob(),
                                                phone = if (signUpWithPhone) fullPhoneNumber else "",
                                                phoneVerified = false,
                                                photoBase64 = photoBase64,
                                                onSuccess = {
                                                    isSigningIn = false
                                                    onAuthSuccess()
                                                },
                                                onError = { err ->
                                                    if (signUpWithPhone && slot < 4 && err.contains("already in use", ignoreCase = true)) {
                                                        attemptSignUp(slot + 1)
                                                    } else {
                                                        isSigningIn = false
                                                        errorMessage = err
                                                    }
                                                }
                                            )
                                        }
                                        attemptSignUp(0)
                                    },
                                    enabled = !isSigningIn && isAgeValid() && termsAgreed && is13OrOlder && contentRulesAccepted,
                                    modifier = Modifier.weight(1.5f).height(48.dp).testTag("create_account_final_button"),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonCyan)
                                ) {
                                    if (isSigningIn) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                                    } else {
                                        Text(VynoraStrings.createAccount(currentLanguage), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Google Profile Setup Card for New Google Users
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F121E)),
                border = BorderStroke(1.dp, Color(0x33FF2E93)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                ) {
                    Text(
                        text = "Complete Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pick your unique handle to finish setting up your account.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it.filter { char -> char.isLetterOrDigit() || char == '_' }.lowercase()
                        },
                        label = { Text("Unique Username (@handle)") },
                        leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null, tint = VynoraNeonCyan) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VynoraNeonCyan,
                            focusedContainerColor = Color(0xFF131726),
                            unfocusedContainerColor = Color(0xFF131726)
                        )
                    )

                    if (VynoraOfficial.isReservedUsername(username)) {
                        Text(
                            text = "Reserved handle (vynora/admin/official)",
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = VynoraNeonPink) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VynoraNeonPink,
                            focusedContainerColor = Color(0xFF131726),
                            unfocusedContainerColor = Color(0xFF131726)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = referralCode,
                        onValueChange = { referralCode = it.uppercase().trim() },
                        label = { Text("Referral Code (Optional)") },
                        leadingIcon = { Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = Color(0xFFFBBF24)) },
                        placeholder = { Text("e.g. ALEX42") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFBBF24),
                            focusedContainerColor = Color(0xFF131726),
                            unfocusedContainerColor = Color(0xFF131726)
                        )
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (username.isBlank()) {
                                errorMessage = "Please enter a username"
                                return@Button
                            }
                            if (VynoraOfficial.isReservedUsername(username)) {
                                errorMessage = "Username '$username' is reserved by VYNORA"
                                return@Button
                            }
                            isSigningIn = true
                            errorMessage = null
                            viewModel.completeGoogleProfileSetup(
                                username = username,
                                fullName = fullName,
                                referralCode = referralCode.ifBlank { null },
                                onSuccess = {
                                    isSigningIn = false
                                    onAuthSuccess()
                                },
                                onError = { err ->
                                    isSigningIn = false
                                    errorMessage = err
                                }
                            )
                        },
                        enabled = !isSigningIn && username.isNotBlank() && !VynoraOfficial.isReservedUsername(username),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("auth_complete_button"),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink)
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text(
                                text = "Enter VYNORA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // --- In-App Legal Modal Dialog (Terms, Privacy under DPDP Act, Community Guidelines) ---
    if (activeLegalModal != null) {
        val doc = activeLegalModal ?: "terms"
        val (title, text) = when (doc) {
            "privacy" -> "Privacy Policy (DPDP Act Compliance)" to """
                1. INTRODUCTION & DATA FIDUCIARY
                Welcome to VYNORA. We respect your privacy and are committed to protecting personal data in strict compliance with the Digital Personal Data Protection Act, 2023 (DPDP Act) of India and international standards.

                2. GROUNDS OF PROCESSING & CONSENT
                We process your personal data (name, email, profile photos, phone number, and device identifiers) only with your free, specific, informed, and unconditional consent.

                3. PRIVATE PERSONAL DATA PROTECTION
                Your mobile phone number and date of birth are private data principles strictly protected by Firestore zero-trust rules. They are never displayed to other users.

                4. CHILDREN'S DATA & AGE GATE
                Users must be at least 18 years of age. VYNORA strictly complies with the age threshold provisions under the DPDP Act.

                5. RIGHTS OF THE DATA PRINCIPAL
                You retain the right to access, rectify, or request erasure of your data at any time via Settings -> Account.
            """.trimIndent()

            "terms" -> "Terms of Service" to """
                1. ACCEPTANCE OF TERMS
                By registering or using VYNORA, you agree to be bound by these Terms of Service.

                2. USER ACCOUNTS & AGE RESTRICTIONS
                You must be 18 years of age or older to register an account. You are responsible for safeguarding your login credentials.

                3. ORIGINALITY & INTELLECTUAL PROPERTY
                You retain ownership rights to content you post. Automated scrapers, spamming, and harassment are strictly prohibited.
            """.trimIndent()

            else -> "Community Guidelines" to """
                1. RESPECT THE COMMUNITY
                VYNORA is a creative, neon-lit space for creators, writers, and visual storytellers. Treat members with dignity.

                2. ZERO TOLERANCE FOR HARASSMENT
                Bullying, hate speech, spam, and malicious behavior result in immediate account suspension.
            """.trimIndent()
        }

        AlertDialog(
            onDismissRequest = { activeLegalModal = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF0F121E),
            title = {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp).verticalScroll(rememberScrollState())
                ) {
                    Text(text, fontSize = 12.5.sp, color = Color(0xFFCBD5E1), lineHeight = 18.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { activeLegalModal = null },
                    colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonCyan),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // --- Forgot Password Dialog ---
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSendingReset) showForgotPasswordDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color(0xFF0F121E),
            tonalElevation = 8.dp,
            modifier = Modifier.border(1.dp, Color(0x3300D1FF), RoundedCornerShape(24.dp)),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LockReset,
                        contentDescription = null,
                        tint = VynoraNeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = VynoraStrings.resetPassword(currentLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (forgotPasswordSuccess) {
                        Surface(
                            color = Color(0x2210B981),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = VynoraStrings.resetLinkSent(currentLanguage),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = VynoraStrings.resetPasswordDesc(currentLanguage),
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 18.sp
                        )

                        OutlinedTextField(
                            value = forgotPasswordEmail,
                            onValueChange = {
                                forgotPasswordEmail = it
                                forgotPasswordError = null
                            },
                            label = { Text("Email Address") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Email, contentDescription = null, tint = VynoraNeonCyan)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth().testTag("reset_email_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VynoraNeonCyan,
                                focusedContainerColor = Color(0xFF131726),
                                unfocusedContainerColor = Color(0xFF131726)
                            )
                        )

                        if (forgotPasswordError != null) {
                            Text(
                                text = forgotPasswordError ?: "",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                modifier = Modifier.testTag("reset_password_error")
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (!forgotPasswordSuccess) {
                    Button(
                        onClick = {
                            isSendingReset = true
                            forgotPasswordError = null
                            viewModel.sendPasswordReset(
                                email = forgotPasswordEmail,
                                onSuccess = {
                                    isSendingReset = false
                                    forgotPasswordSuccess = true
                                },
                                onError = { err ->
                                    isSendingReset = false
                                    forgotPasswordError = err
                                }
                            )
                        },
                        enabled = !isSendingReset && forgotPasswordEmail.isNotBlank() && forgotPasswordEmail.contains("@"),
                        colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonCyan),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("send_reset_button")
                    ) {
                        if (isSendingReset) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = VynoraStrings.sendResetLink(currentLanguage),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { showForgotPasswordDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonCyan),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!forgotPasswordSuccess) {
                    TextButton(
                        onClick = { showForgotPasswordDialog = false },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = VynoraStrings.cancel(currentLanguage),
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        )
    }
}

enum class PasswordStrength {
    NONE, WEAK, MEDIUM, STRONG
}

fun calculatePasswordStrength(pass: String): PasswordStrength {
    if (pass.isEmpty()) return PasswordStrength.NONE
    if (pass.length < 6) return PasswordStrength.WEAK
    var score = 0
    if (pass.length >= 8) score++
    if (pass.any { it.isDigit() }) score++
    if (pass.any { it.isUpperCase() } && pass.any { it.isLowerCase() }) score++
    if (pass.any { !it.isLetterOrDigit() }) score++
    return when {
        score <= 1 -> PasswordStrength.WEAK
        score in 2..3 -> PasswordStrength.MEDIUM
        else -> PasswordStrength.STRONG
    }
}
