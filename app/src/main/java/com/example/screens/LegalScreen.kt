package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink

@Composable
fun LegalScreen(
    docType: String,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenBg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF0F121E) else Color(0xFFFFFFFF)

    val (title, content) = when (docType) {
        "privacy" -> "Privacy Policy (DPDP Act Compliance)" to """
1. INTRODUCTION & DATA FIDUCIARY
Welcome to VYNORA. We respect your privacy and are committed to protecting personal data in strict compliance with applicable data protection laws, specifically the Digital Personal Data Protection Act, 2023 (DPDP Act) of India and international standards.

2. GROUNDS OF PROCESSING & CONSENT
We process your personal data (name, email, profile photos, clips, chats, and device identifiers) only with your free, specific, informed, and unconditional consent. You have the right to withdraw consent at any time via Settings -> Account.

3. RIGHTS OF THE DATA PRINCIPAL
Under the DPDP Act, you are entitled to:
- Right to Access Information: Request a summary of personal data processed.
- Right to Correction & Erasure: Request rectification of inaccurate data or complete deletion.
- Right of Grievance Redressal: Contact our designated Data Protection Officer (DPO) at dpo@vynora.io.
- Right to Nominate: Designate an individual to exercise data rights in the event of incapacity.

4. CHILDREN'S DATA & AGE GATE
Users must be at least 13 years of age. VYNORA does not engage in targeted behavioral advertising to minors or tracking of children's personal data.

5. SECURITY & DATA LOCALIZATION
All telemetry and user creations are encrypted with TLS 1.3 in transit and AES-256 at rest.
        """.trimIndent()

        "terms" -> "Terms of Service" to """
1. ACCEPTANCE OF TERMS
By accessing or using VYNORA, you agree to be bound by these Terms of Service. If you do not agree, please do not use the platform.

2. USER ACCOUNTS & AGE RESTRICTIONS
You must be 13 years of age or older to register an account. You are responsible for safeguarding your login credentials and for all activities that occur under your account.

3. CONTENT OWNERSHIP & LICENSES
You retain all ownership rights to the photos, clips, moments, and music you post on VYNORA. By sharing content publicly, you grant VYNORA a non-exclusive, royalty-free license to display and distribute your content across the platform.

4. PROHIBITED CONDUCT
You agree not to post content that is illegal, defamatory, abusive, infringing on intellectual property, or sexually explicit. Automated bots, scraping, and coordinated spam are strictly prohibited.

5. TERMINATION
We reserve the right to suspend or terminate accounts that repeatedly violate Community Guidelines or copyright rules.
        """.trimIndent()

        "copyright" -> "Copyright & IPR Takedown Notice" to """
1. DIGITAL MILLENNIUM & IPR TAKEDOWN PROCEDURE
If you believe that your copyrighted musical composition, visual art, photograph, or video has been reproduced on VYNORA without authorization, you may submit a formal takedown request.

2. NOTICE REQUIREMENTS
Your notice must include:
- Identification of the copyrighted work claimed to have been infringed.
- Direct URL or post ID of the infringing material on VYNORA.
- Contact information (name, address, email, telephone).
- A statement of good faith belief that the use is unauthorized.
- A physical or electronic signature of the copyright owner or authorized representative.

3. COUNTER-NOTICE & APPEAL PROCEDURE
Users whose content has been removed may submit an appeal within 14 days demonstrating fair use, licensing, or mistaken identification. Send inquiries to legal@vynora.io.
        """.trimIndent()

        else -> "Community Guidelines" to """
1. RESPECT THE COMMUNITY
VYNORA is a creative space for original creators, visual storytellers, and dreamers. Treat fellow members with dignity.

2. ZERO TOLERANCE FOR HARASSMENT
Bullying, stalking, hate speech, and doxxing result in immediate account suspension.

3. ORIGINALITY & INTELLECTUAL PROPERTY
Share your own creations or respect the creators behind the sounds and visuals you remix.
        """.trimIndent()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("legal_screen")
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
                text = title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                maxLines = 1,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 30.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = content,
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (docType == "copyright") {
                        Button(
                            onClick = { onToast("Copyright takedown form submitted to legal review team") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink)
                        ) {
                            Text("Submit Formal Notice to DPO", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
