package com.example

import com.example.i18n.AppLanguage
import com.example.i18n.VynoraStrings
import com.example.model.User
import com.example.util.VynoraOfficial
import org.junit.Assert.*
import org.junit.Test

class VynoraLogicTest {

    @Test
    fun testOfficialAccountProperties() {
        val official = VynoraOfficial.createOfficialUser()
        assertEquals(VynoraOfficial.OFFICIAL_USER_ID, official.id)
        assertEquals("vynora_official", official.username)
        assertEquals("VYNORA OFFICIAL", official.fullName)
        assertTrue(official.isOfficial)
        assertTrue(official.badgeActive)
        assertEquals("official", official.badgeSource)
        assertEquals("ADMIN", official.role)
        assertTrue(official.isPlusActive)
    }

    @Test
    fun testReservedUsernameRules() {
        // Official username itself is allowed
        assertFalse(VynoraOfficial.isReservedUsername("vynora_official"))

        // Any username containing reserved keywords must be blocked
        assertTrue(VynoraOfficial.isReservedUsername("vynora_fan"))
        assertTrue(VynoraOfficial.isReservedUsername("official_page"))
        assertTrue(VynoraOfficial.isReservedUsername("admin_alex"))
        assertTrue(VynoraOfficial.isReservedUsername("support_help"))
        assertTrue(VynoraOfficial.isReservedUsername("staff_member"))
        assertTrue(VynoraOfficial.isReservedUsername("VYNORA_VIP"))

        // Standard handles are allowed
        assertFalse(VynoraOfficial.isReservedUsername("cyber_sarah"))
        assertFalse(VynoraOfficial.isReservedUsername("neon_pixel"))
        assertFalse(VynoraOfficial.isReservedUsername("alex_rivera"))
    }

    @Test
    fun testOfficialAccountIdentification() {
        assertTrue(VynoraOfficial.isOfficialAccount(VynoraOfficial.OFFICIAL_USER_ID, "vynora_official"))
        assertTrue(VynoraOfficial.isOfficialAccount(null, "vynora_official"))
        assertTrue(VynoraOfficial.isOfficialAccount("vynora_official_uid", null))
        assertFalse(VynoraOfficial.isOfficialAccount("random_id", "random_user"))
    }

    @Test
    fun testBadgeActiveAndExpiryCalculations() {
        val now = System.currentTimeMillis()

        // 1. User with active paid badge (future expiry)
        val activePaidUser = User(
            id = "u1",
            username = "alice",
            badgeActive = true,
            badgeExpiry = now + (15L * 24 * 60 * 60 * 1000L), // 15 days left
            badgeSource = "paid"
        )
        assertTrue(activePaidUser.isPlusActive)
        assertEquals(16, activePaidUser.daysRemaining) // ceil/round up day

        // 2. User with expired badge
        val expiredUser = User(
            id = "u2",
            username = "bob",
            badgeActive = true,
            badgeExpiry = now - 1000L, // Expired 1 sec ago
            badgeSource = "paid"
        )
        assertFalse(expiredUser.isPlusActive)
        assertEquals(0, expiredUser.daysRemaining)

        // 3. User with referral badge
        val referralUser = User(
            id = "u3",
            username = "charlie",
            badgeActive = true,
            badgeExpiry = now + (30L * 24 * 60 * 60 * 1000L),
            badgeSource = "referral"
        )
        assertTrue(referralUser.isPlusActive)

        // 4. Banned user loses badge benefits
        val bannedUser = referralUser.copy(isBanned = true)
        assertFalse(bannedUser.isPlusActive)
    }

    @Test
    fun testLanguageTranslationsAndRtl() {
        assertEquals("VYNORA", VynoraStrings.appName(AppLanguage.ENGLISH))
        assertEquals("वायनोरा", VynoraStrings.appName(AppLanguage.HINDI))
        assertEquals("وائنورا", VynoraStrings.appName(AppLanguage.URDU))

        assertEquals(androidx.compose.ui.unit.LayoutDirection.Ltr, AppLanguage.ENGLISH.layoutDirection)
        assertEquals(androidx.compose.ui.unit.LayoutDirection.Ltr, AppLanguage.HINDI.layoutDirection)
        assertEquals(androidx.compose.ui.unit.LayoutDirection.Rtl, AppLanguage.URDU.layoutDirection)

        assertEquals("VYNORA Plus", VynoraStrings.vynoraPlus(AppLanguage.ENGLISH))
        assertEquals("Official", VynoraStrings.officialBadge(AppLanguage.ENGLISH))
    }

    @Test
    fun testPasswordStrengthCalculation() {
        assertEquals(com.example.screens.PasswordStrength.NONE, com.example.screens.calculatePasswordStrength(""))
        assertEquals(com.example.screens.PasswordStrength.WEAK, com.example.screens.calculatePasswordStrength("12345"))
        assertEquals(com.example.screens.PasswordStrength.WEAK, com.example.screens.calculatePasswordStrength("abcdef"))
        assertEquals(com.example.screens.PasswordStrength.MEDIUM, com.example.screens.calculatePasswordStrength("Pass1234"))
        assertEquals(com.example.screens.PasswordStrength.STRONG, com.example.screens.calculatePasswordStrength("Vynora#2026!"))
    }

    @Test
    fun testAuthTranslations() {
        assertEquals("Passwords do not match", VynoraStrings.passwordsDoNotMatch(AppLanguage.ENGLISH))
        assertEquals("पासवर्ड मेल नहीं खाते", VynoraStrings.passwordsDoNotMatch(AppLanguage.HINDI))
        assertEquals("پاس ورڈ مماثل نہیں ہیں", VynoraStrings.passwordsDoNotMatch(AppLanguage.URDU))

        assertEquals("Forgot password?", VynoraStrings.forgotPassword(AppLanguage.ENGLISH))
        assertEquals("पासवर्ड भूल गए?", VynoraStrings.forgotPassword(AppLanguage.HINDI))
        assertEquals("پاس ورڈ بھول گئے؟", VynoraStrings.forgotPassword(AppLanguage.URDU))

        assertEquals("Reset link sent. Check your inbox and spam folder.", VynoraStrings.resetLinkSent(AppLanguage.ENGLISH))
        assertEquals("रीसेट लिंक भेज दिया गया है। अपना इनबॉक्स और स्पैम फ़ोल्डर जांचें।", VynoraStrings.resetLinkSent(AppLanguage.HINDI))
        assertEquals("ری سیٹ لنک بھیج دیا گیا ہے۔ اپنا ان باکس اور اسپام فولڈر چیک کریں۔", VynoraStrings.resetLinkSent(AppLanguage.URDU))
    }

    @Test
    fun testNewUserFieldsAndModelIntegrity() {
        val user = User(
            id = "test_user_1",
            email = "user@vynora.io",
            username = "vynora_creator",
            fullName = "Alex Vynora",
            gender = "Male",
            dob = "2000-01-15",
            phone = "+919876543210",
            phoneVerified = true,
            photoBase64 = "base64_sample_string",
            bio = "Visual artist & creator on VYNORA",
            links = listOf("https://vynora.io/@alex", "https://artstation.com/alex"),
            category = "Creator",
            pronouns = "they/them",
            termsAccepted = true,
            termsVersion = "1.0",
            termsAcceptedAt = 1700000000000L,
            usernameLastChangedAt = 1700000000000L,
            isPrivateAccount = true,
            showEmailOnProfile = true,
            showPhoneOnProfile = false,
            hideFollowersCount = true,
            isAiGeneratedProfile = false
        )

        assertEquals("Male", user.gender)
        assertEquals("2000-01-15", user.dob)
        assertEquals("+919876543210", user.phone)
        assertTrue(user.phoneVerified)
        assertEquals(2, user.links.size)
        assertEquals("Creator", user.category)
        assertEquals("they/them", user.pronouns)
        assertTrue(user.termsAccepted)
        assertEquals("1.0", user.termsVersion)
        assertTrue(user.isPrivateAccount)
        assertTrue(user.showEmailOnProfile)
        assertFalse(user.showPhoneOnProfile)
        assertTrue(user.hideFollowersCount)
    }

    @Test
    fun testFourteenDayUsernameCooldownLogic() {
        val now = System.currentTimeMillis()
        val fourteenDaysMillis = 14L * 24L * 60L * 60L * 1000L

        // Username changed 5 days ago -> on cooldown with ~9-10 days remaining
        val recentChangeTime = now - (5L * 24L * 60L * 60L * 1000L)
        val elapsedRecent = now - recentChangeTime
        val isCooldownActiveRecent = elapsedRecent < fourteenDaysMillis
        assertTrue(isCooldownActiveRecent)

        val daysRemaining = ((fourteenDaysMillis - elapsedRecent) / (24L * 60L * 60L * 1000L)).toInt() + 1
        assertEquals(10, daysRemaining)

        // Username changed 20 days ago -> cooldown expired, user can change username
        val oldChangeTime = now - (20L * 24L * 60L * 60L * 1000L)
        val elapsedOld = now - oldChangeTime
        val isCooldownActiveOld = elapsedOld < fourteenDaysMillis
        assertFalse(isCooldownActiveOld)
    }

    @Test
    fun testMultiStepAndEditProfileTranslations() {
        // Step Indicators
        assertEquals("Step 1 of 5", VynoraStrings.stepIndicator(1, 5, AppLanguage.ENGLISH))
        assertEquals("चरण 1 का 5", VynoraStrings.stepIndicator(1, 5, AppLanguage.HINDI))
        assertEquals("مرحلہ 1 از 5", VynoraStrings.stepIndicator(1, 5, AppLanguage.URDU))

        // Age Verification
        assertEquals("I am 13 years of age or older", VynoraStrings.age13Checkbox(AppLanguage.ENGLISH))
        assertEquals("मेरी आयु 13 वर्ष या उससे अधिक है", VynoraStrings.age13Checkbox(AppLanguage.HINDI))
        assertEquals("میری عمر 13 سال یا اس سے زیادہ ہے", VynoraStrings.age13Checkbox(AppLanguage.URDU))

        // Edit Profile
        assertEquals("Edit Profile", VynoraStrings.editProfileTitle(AppLanguage.ENGLISH))
        assertEquals("प्रोफ़ाइल संपादित करें", VynoraStrings.editProfileTitle(AppLanguage.HINDI))
        assertEquals("پروفائل میں ترمیم کریں", VynoraStrings.editProfileTitle(AppLanguage.URDU))

        // Discard Unsaved Changes
        assertEquals("Discard unsaved changes?", VynoraStrings.unsavedChangesTitle(AppLanguage.ENGLISH))
        assertEquals("बिना सहेजे परिवर्तन छोड़ें?", VynoraStrings.unsavedChangesTitle(AppLanguage.HINDI))
        assertEquals("کیا غیر محفوظ تبدیلیاں مسترد کر دیں؟", VynoraStrings.unsavedChangesTitle(AppLanguage.URDU))
    }

    @Test
    fun testFormatCount() {
        // Zero and below
        assertEquals("0", com.example.util.formatCount(0))
        assertEquals("0", com.example.util.formatCount(-5))

        // Less than 1000
        assertEquals("1", com.example.util.formatCount(1))
        assertEquals("42", com.example.util.formatCount(42))
        assertEquals("999", com.example.util.formatCount(999))

        // Thousands (K)
        assertEquals("1K", com.example.util.formatCount(1000))
        assertEquals("1K", com.example.util.formatCount(1050)) // truncated, no .0
        assertEquals("1.1K", com.example.util.formatCount(1100))
        assertEquals("1.1K", com.example.util.formatCount(1190)) // truncated
        assertEquals("10K", com.example.util.formatCount(10000))
        assertEquals("10.5K", com.example.util.formatCount(10500))
        assertEquals("999.9K", com.example.util.formatCount(999900))

        // Millions (M)
        assertEquals("1M", com.example.util.formatCount(1000000))
        assertEquals("1.1M", com.example.util.formatCount(1100000))
        assertEquals("2M", com.example.util.formatCount(2000000))

        // Billions (B)
        assertEquals("1B", com.example.util.formatCount(1000000000L))
        assertEquals("1.1B", com.example.util.formatCount(1100000000L))
    }
}
