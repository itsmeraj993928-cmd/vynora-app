package com.example.i18n

import androidx.compose.ui.unit.LayoutDirection
import com.example.util.formatCount

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String, val layoutDirection: LayoutDirection) {
    ENGLISH("en", "English", "English", LayoutDirection.Ltr),
    HINDI("hi", "Hindi", "हिन्दी", LayoutDirection.Ltr),
    URDU("ur", "Urdu", "اردو", LayoutDirection.Rtl)
}

object VynoraStrings {
    // Brand
    fun appName(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "VYNORA"
        AppLanguage.HINDI -> "वायनोरा"
        AppLanguage.URDU -> "وائنورا"
    }

    fun tagline(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Share your world."
        AppLanguage.HINDI -> "अपनी दुनिया साझा करें।"
        AppLanguage.URDU -> "اپنی دنیا شیئر کریں۔"
    }

    // Navigation
    fun navHome(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Home"
        AppLanguage.HINDI -> "होम"
        AppLanguage.URDU -> "ہوم"
    }

    fun navExplore(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Explore"
        AppLanguage.HINDI -> "एक्सप्लोर"
        AppLanguage.URDU -> "تلاش"
    }

    fun navCreate(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Create"
        AppLanguage.HINDI -> "बनाएं"
        AppLanguage.URDU -> "تخلیق"
    }

    fun navClips(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Clips"
        AppLanguage.HINDI -> "क्लिप्स"
        AppLanguage.URDU -> "کلپس"
    }

    fun navProfile(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Profile"
        AppLanguage.HINDI -> "प्रोफ़ाइल"
        AppLanguage.URDU -> "پروفائل"
    }

    fun navChats(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Chats"
        AppLanguage.HINDI -> "बातचीत"
        AppLanguage.URDU -> "چیٹس"
    }

    fun navNotifications(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Notifications"
        AppLanguage.HINDI -> "सूचनाएं"
        AppLanguage.URDU -> "اطلاعات"
    }

    // VYNORA Plus & Badges
    fun vynoraPlus(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "VYNORA Plus"
        AppLanguage.HINDI -> "वायनोरा प्लस"
        AppLanguage.URDU -> "وائنورا پلس"
    }

    fun officialBadge(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Official"
        AppLanguage.HINDI -> "आधिकारिक"
        AppLanguage.URDU -> "سرکاری"
    }

    fun getBlueBadge(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Get VYNORA Plus"
        AppLanguage.HINDI -> "वायनोरा प्लस प्राप्त करें"
        AppLanguage.URDU -> "وائنورا پلس حاصل کریں"
    }

    fun badgePrice(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Rs 49 / month"
        AppLanguage.HINDI -> "₹49 / महीना"
        AppLanguage.URDU -> "49 روپے / ماہانہ"
    }

    fun buyBadge(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Subscribe for Rs 49/month"
        AppLanguage.HINDI -> "₹49/महीना में सदस्यता लें"
        AppLanguage.URDU -> "49 روپے/ماہ میں سبسکرائب کریں"
    }

    fun renewBadge(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Renew VYNORA Plus (Rs 49)"
        AppLanguage.HINDI -> "नवीनीकृत करें (₹49)"
        AppLanguage.URDU -> "تجدید کریں (49 روپے)"
    }

    fun daysRemaining(lang: AppLanguage, days: Int): String = when (lang) {
        AppLanguage.ENGLISH -> "$days days remaining"
        AppLanguage.HINDI -> "$days दिन शेष"
        AppLanguage.URDU -> "$days دن باقی ہیں"
    }

    fun inviteFriends(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Invite 10 friends to get 30 days of VYNORA Plus for free!"
        AppLanguage.HINDI -> "10 दोस्तों को आमंत्रित करें और 30 दिन मुफ्त वायनोरा प्लस पाएं!"
        AppLanguage.URDU -> "10 دوستوں کو مدعو کریں اور مفت 30 دن کا وائنورا پلس حاصل کریں!"
    }

    fun referralProgress(lang: AppLanguage, count: Int): String = when (lang) {
        AppLanguage.ENGLISH -> "$count/10 invited"
        AppLanguage.HINDI -> "$count/10 आमंत्रित"
        AppLanguage.URDU -> "$count/10 مدعو"
    }

    fun referralCodeLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Referral Code (Optional)"
        AppLanguage.HINDI -> "रेफ़रल कोड (वैकल्पिक)"
        AppLanguage.URDU -> "ریفرل کوڈ (اختیاری)"
    }

    fun emailVerificationRequired(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Please verify your email before posting, inviting or buying the badge."
        AppLanguage.HINDI -> "कृपया पोस्ट करने, आमंत्रित करने या बैज खरीदने से पहले अपना ईमेल सत्यापित करें।"
        AppLanguage.URDU -> "براہ کرم پوسٹ کرنے، مدعو کرنے یا بیج خریدنے سے پہلے اپنے ای میل کی تصدیق کریں۔"
    }

    fun resendVerification(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Resend Verification Email"
        AppLanguage.HINDI -> "सत्यापन ईमेल पुनः भेजें"
        AppLanguage.URDU -> "تصدیقی ای میل دوبارہ بھیجیں"
    }

    // Empty States
    fun emptyHome(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "No posts yet. Follow people or create your first post."
        AppLanguage.HINDI -> "अभी तक कोई पोस्ट नहीं है। लोगों को फॉलो करें या अपनी पहली पोस्ट बनाएं।"
        AppLanguage.URDU -> "ابھی تک کوئی پوسٹ نہیں۔ لوگوں کو فالو کریں یا اپنی پہلی پوسٹ بنائیں۔"
    }

    fun emptyExplore(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "No posts found. Start exploring or share your world."
        AppLanguage.HINDI -> "कोई पोस्ट नहीं मिली। एक्सप्लोर करें या अपनी दुनिया साझा करें।"
        AppLanguage.URDU -> "کوئی پوسٹ نہیں ملی۔ تلاش کریں یا اپنی دنیا شیئر کریں۔"
    }

    fun emptyClips(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "No clips yet. Be the first to share a short video clip!"
        AppLanguage.HINDI -> "अभी कोई क्लिप नहीं है। लघु वीडियो साझा करने वाले पहले व्यक्ति बनें!"
        AppLanguage.URDU -> "ابھی تک کوئی کلپ نہیں۔ مختصر ویڈیو شیئر کرنے والے پہلے بنیں!"
    }

    fun emptyChats(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "No chats yet. Search for friends to start messaging."
        AppLanguage.HINDI -> "अभी कोई चैट नहीं है। बातचीत शुरू करने के लिए दोस्तों को खोजें।"
        AppLanguage.URDU -> "ابھی تک کوئی چیٹ نہیں۔ بات چیت شروع کرنے کے لیے دوستوں کو تلاش کریں۔"
    }

    fun emptyNotifications(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "No notifications yet."
        AppLanguage.HINDI -> "अभी कोई सूचनाएं नहीं हैं।"
        AppLanguage.URDU -> "ابھی تک کوئی اطلاع نہیں۔"
    }

    fun emptyProfilePosts(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "No posts shared yet."
        AppLanguage.HINDI -> "अभी तक कोई पोस्ट साझा नहीं की गई है।"
        AppLanguage.URDU -> "ابھی تک کوئی پوسٹ شیئر نہیں کی گئی۔"
    }

    // Auth
    fun login(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Log In"
        AppLanguage.HINDI -> "लॉग इन"
        AppLanguage.URDU -> "لاگ ان"
    }

    fun signUp(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Sign Up"
        AppLanguage.HINDI -> "साइन अप"
        AppLanguage.URDU -> "سائن اپ"
    }

    fun emailLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Email"
        AppLanguage.HINDI -> "ईमेल"
        AppLanguage.URDU -> "ای میل"
    }

    fun usernameLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Username"
        AppLanguage.HINDI -> "यूज़रनेम"
        AppLanguage.URDU -> "یوزر نام"
    }

    fun passwordLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Password (min 6 characters)"
        AppLanguage.HINDI -> "पासवर्ड (न्यूनतम 6 अक्षर)"
        AppLanguage.URDU -> "پاس ورڈ (کم از کم 6 حروف)"
    }

    fun continueWithGoogle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Sign in with Google"
        AppLanguage.HINDI -> "गूगल के साथ साइन इन करें"
        AppLanguage.URDU -> "گوگل کے ساتھ سائن ان کریں"
    }

    fun forgotPassword(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Forgot password?"
        AppLanguage.HINDI -> "पासवर्ड भूल गए?"
        AppLanguage.URDU -> "پاس ورڈ بھول گئے؟"
    }

    // Feed & Actions
    fun moments(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Moments"
        AppLanguage.HINDI -> "मोमेंट्स"
        AppLanguage.URDU -> "مومنٹس"
    }

    fun yourMoment(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Your Moment"
        AppLanguage.HINDI -> "आपका मोमेंट"
        AppLanguage.URDU -> "آپ کا مومنٹ"
    }

    fun likes(lang: AppLanguage, count: String): String = when (lang) {
        AppLanguage.ENGLISH -> "$count likes"
        AppLanguage.HINDI -> "$count पसंद"
        AppLanguage.URDU -> "$count پسندیدگیاں"
    }

    fun likes(lang: AppLanguage, count: Int): String = likes(lang, formatCount(count))

    fun comments(lang: AppLanguage, count: String): String = when (lang) {
        AppLanguage.ENGLISH -> "View all $count comments"
        AppLanguage.HINDI -> "सभी $count टिप्पणियाँ देखें"
        AppLanguage.URDU -> "تمام $count تبصرے دیکھیں"
    }

    fun comments(lang: AppLanguage, count: Int): String = comments(lang, formatCount(count))

    fun addComment(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Add a comment..."
        AppLanguage.HINDI -> "एक टिप्पणी जोड़ें..."
        AppLanguage.URDU -> "ایک تبصرہ شامل کریں..."
    }

    fun post(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Post"
        AppLanguage.HINDI -> "पोस्ट"
        AppLanguage.URDU -> "پوسٹ"
    }

    fun share(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Share"
        AppLanguage.HINDI -> "शेयर करें"
        AppLanguage.URDU -> "شیئر"
    }

    fun follow(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Follow"
        AppLanguage.HINDI -> "फॉलो करें"
        AppLanguage.URDU -> "فالو کریں"
    }

    fun following(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Following"
        AppLanguage.HINDI -> "फॉलो कर रहे हैं"
        AppLanguage.URDU -> "فالو کر رہے ہیں"
    }

    fun createPost(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "New Post"
        AppLanguage.HINDI -> "नई पोस्ट"
        AppLanguage.URDU -> "نئی پوسٹ"
    }

    fun createMoment(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "New Moment"
        AppLanguage.HINDI -> "नया मोमेंट"
        AppLanguage.URDU -> "نیا مومنٹ"
    }

    fun createClip(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "New Clip"
        AppLanguage.HINDI -> "नई क्लिप"
        AppLanguage.URDU -> "نئی کلپ"
    }

    fun aiCaptionHelper(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "AI Caption"
        AppLanguage.HINDI -> "एआई कैप्शन"
        AppLanguage.URDU -> "AI کیپشن"
    }

    fun aiHashtagHelper(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "AI Hashtags"
        AppLanguage.HINDI -> "एआई हैशटैग"
        AppLanguage.URDU -> "AI ہیش ٹیگز"
    }

    fun autoTranslate(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Translate"
        AppLanguage.HINDI -> "अनुवाद करें"
        AppLanguage.URDU -> "ترجمہ کریں"
    }

    fun editProfile(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Edit Profile"
        AppLanguage.HINDI -> "प्रोफ़ाइल संपादित करें"
        AppLanguage.URDU -> "پروفائل تبدیل کریں"
    }

    fun creatorStudio(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Creator Studio"
        AppLanguage.HINDI -> "क्रिएटर स्टूडियो"
        AppLanguage.URDU -> "کری ایٹر اسٹوڈیو"
    }

    fun settings(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Settings"
        AppLanguage.HINDI -> "सेटिंग्स"
        AppLanguage.URDU -> "ترتیبات"
    }

    fun adminPanel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Admin Panel"
        AppLanguage.HINDI -> "व्यवस्थापक पैनल"
        AppLanguage.URDU -> "ایڈمن پینل"
    }

    fun darkMode(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Dark Theme"
        AppLanguage.HINDI -> "डार्क थीम"
        AppLanguage.URDU -> "ڈارک تھیم"
    }

    fun language(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Language"
        AppLanguage.HINDI -> "भाषा"
        AppLanguage.URDU -> "زبان"
    }

    fun report(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Report"
        AppLanguage.HINDI -> "रिपोर्ट करें"
        AppLanguage.URDU -> "رپورٹ کریں"
    }

    fun privacyPolicy(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Privacy Policy (DPDP Act)"
        AppLanguage.HINDI -> "गोपनीयता नीति (DPDP अधिनियम)"
        AppLanguage.URDU -> "رازداری کی پالیسی (DPDP ایکٹ)"
    }

    fun termsOfService(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Terms of Service"
        AppLanguage.HINDI -> "सेवा की शर्तें"
        AppLanguage.URDU -> "سروس کی شرائط"
    }

    fun skip(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Skip"
        AppLanguage.HINDI -> "छोड़ें"
        AppLanguage.URDU -> "چھوڑیں"
    }

    fun next(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Next"
        AppLanguage.HINDI -> "आगे"
        AppLanguage.URDU -> "اگلا"
    }

    fun getStarted(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Get Started"
        AppLanguage.HINDI -> "शुरू करें"
        AppLanguage.URDU -> "شروع کریں"
    }

    fun ageGateTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Age Confirmation (13+)"
        AppLanguage.HINDI -> "आयु पुष्टि (13+)"
        AppLanguage.URDU -> "عمر کی تصدیق (13+)"
    }

    fun ageGateDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "By joining VYNORA, you confirm that you are at least 13 years of age and agree to our Community Guidelines."
        AppLanguage.HINDI -> "वायनोरा में शामिल होकर, आप पुष्टि करते हैं कि आपकी आयु कम से कम 13 वर्ष है और हमारे सामुदायिक दिशानिर्देशों से सहमत हैं।"
        AppLanguage.URDU -> "وائنورا میں شامل ہو کر، آپ تصدیق کرتے ہیں کہ آپ کی عمر کم از कम 13 سال ہے اور ہماری کمیونٹی گائیڈ لائنز سے متفق ہیں۔"
    }

    fun ageGateConfirm(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "I confirm that I am 13 or older"
        AppLanguage.HINDI -> "मैं पुष्टि करता हूँ कि मेरी आयु 13 वर्ष या अधिक है"
        AppLanguage.URDU -> "میں تصدیق کرتا ہوں کہ میری عمر 13 سال یا اس سے زیادہ ہے"
    }

    fun copyrightForm(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Copyright Takedown"
        AppLanguage.HINDI -> "कॉपीराइट हटाने का अनुरोध"
        AppLanguage.URDU -> "کاپی رائٹ ہٹانے کی درخواست"
    }

    // Auth screen strings
    fun signInTab(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Sign In"
        AppLanguage.HINDI -> "साइन इन"
        AppLanguage.URDU -> "سائن ان"
    }

    fun signUpTab(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Sign Up"
        AppLanguage.HINDI -> "साइन अप"
        AppLanguage.URDU -> "سائن اپ"
    }

    fun createFreeAccount(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Create Free Account"
        AppLanguage.HINDI -> "मुफ़्त खाता बनाएं"
        AppLanguage.URDU -> "مفت اکاؤنٹ بنائیں"
    }

    fun confirmPasswordLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Confirm Password"
        AppLanguage.HINDI -> "पासवर्ड की पुष्टि करें"
        AppLanguage.URDU -> "پاس ورڈ کی تصدیق کریں"
    }

    fun passwordsDoNotMatch(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Passwords do not match"
        AppLanguage.HINDI -> "पासवर्ड मेल नहीं खाते"
        AppLanguage.URDU -> "پاس ورڈ مماثل نہیں ہیں"
    }



    fun resetPassword(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Reset Password"
        AppLanguage.HINDI -> "पासवर्ड रीसेट करें"
        AppLanguage.URDU -> "پاس ورڈ ری سیٹ کریں"
    }

    fun resetPasswordDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Enter your registered email address and we'll send you a link to reset your password."
        AppLanguage.HINDI -> "अपना पंजीकृत ईमेल दर्ज करें और हम आपको पासवर्ड रीसेट करने के लिए एक लिंक भेजेंगे।"
        AppLanguage.URDU -> "اپنا رجسٹرڈ ای میل درج کریں اور ہم آپ کو پاس ورڈ ری سیٹ کرنے کے لیے ایک لنک بھیجیں گے۔"
    }

    fun sendResetLink(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Send Reset Link"
        AppLanguage.HINDI -> "रीसेट लिंक भेजें"
        AppLanguage.URDU -> "ری سیٹ لنک بھیجیں"
    }

    fun resetLinkSent(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Reset link sent. Check your inbox and spam folder."
        AppLanguage.HINDI -> "रीसेट लिंक भेज दिया गया है। अपना इनबॉक्स और स्पैम फ़ोल्डर जांचें।"
        AppLanguage.URDU -> "ری سیٹ لنک بھیج دیا گیا ہے۔ اپنا ان باکس اور اسپام فولڈر چیک کریں۔"
    }

    fun passwordWeak(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Weak"
        AppLanguage.HINDI -> "कमज़ोर"
        AppLanguage.URDU -> "کمزور"
    }

    fun passwordMedium(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Medium"
        AppLanguage.HINDI -> "मध्यम"
        AppLanguage.URDU -> "درمیانہ"
    }

    fun passwordStrong(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Strong"
        AppLanguage.HINDI -> "मजबूत"
        AppLanguage.URDU -> "مضبوط"
    }

    fun cancel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.HINDI -> "रद्द करें"
        AppLanguage.URDU -> "منسوخ کریں"
    }

    // --- Multi-Step Sign Up Strings ---
    fun stepIndicator(step: Int, total: Int, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Step $step of $total"
        AppLanguage.HINDI -> "चरण $step का $total"
        AppLanguage.URDU -> "مرحلہ $step از $total"
    }

    fun step1Title(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Account Credentials"
        AppLanguage.HINDI -> "खाता क्रेडेंशियल"
        AppLanguage.URDU -> "اکاؤنٹ کی تفصیلات"
    }

    fun step2Title(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Profile Details"
        AppLanguage.HINDI -> "प्रोफ़ाइल विवरण"
        AppLanguage.URDU -> "پروفائل کی معلومات"
    }

    fun step3Title(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Mobile Contact"
        AppLanguage.HINDI -> "मोबाइल संपर्क"
        AppLanguage.URDU -> "موبائل رابطہ"
    }

    fun step4Title(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Profile Photo"
        AppLanguage.HINDI -> "प्रोफ़ाइल फ़ोटो"
        AppLanguage.URDU -> "پروفائل تصویر"
    }

    fun step5Title(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Terms & Age Verification"
        AppLanguage.HINDI -> "शर्तें और आयु सत्यापन"
        AppLanguage.URDU -> "شرائط اور عمر کی تصدیق"
    }

    fun backButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Back"
        AppLanguage.HINDI -> "पीछे"
        AppLanguage.URDU -> "واپس"
    }

    fun nextButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Next"
        AppLanguage.HINDI -> "आगे"
        AppLanguage.URDU -> "اگلا"
    }

    fun fullNameLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Full Name"
        AppLanguage.HINDI -> "पूरा नाम"
        AppLanguage.URDU -> "پورا نام"
    }

    fun uniqueUsernameLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Unique Username (@handle)"
        AppLanguage.HINDI -> "अद्वितीय उपयोगकर्ता नाम (@handle)"
        AppLanguage.URDU -> "منفرد صارف نام (@handle)"
    }

    fun usernameChecking(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Checking availability..."
        AppLanguage.HINDI -> "उपलब्धता की जांच हो रही है..."
        AppLanguage.URDU -> "دستیابی چیک ہو رہی ہے..."
    }

    fun usernameAvailable(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Username is available ✓"
        AppLanguage.HINDI -> "उपयोगकर्ता नाम उपलब्ध है ✓"
        AppLanguage.URDU -> "صارف نام دستیاب ہے ✓"
    }

    fun usernameTaken(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Username is already taken"
        AppLanguage.HINDI -> "यह उपयोगकर्ता नाम पहले से लिया जा चुका है"
        AppLanguage.URDU -> "یہ صارف نام پہلے ہی لیا جا چکا ہے"
    }

    fun dobLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Date of Birth (13+ only)"
        AppLanguage.HINDI -> "जन्म तिथि (केवल 13+)"
        AppLanguage.URDU -> "تاریخ پیدائش (صرف 13+)"
    }

    fun dobPrivateNote(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Private - never shared publicly"
        AppLanguage.HINDI -> "निजी - कभी सार्वजनिक रूप से साझा नहीं किया जाएगा"
        AppLanguage.URDU -> "نجی - کبھی بھی عوامی طور پر شیئر نہیں کیا جائے گا"
    }

    fun dobErrorUnderage(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "You must be at least 13 years old to join VYNORA"
        AppLanguage.HINDI -> "वायनोरा में शामिल होने के लिए आपकी आयु कम से कम 13 वर्ष होनी चाहिए"
        AppLanguage.URDU -> "وائنورا میں شامل ہونے کے لیے آپ کی عمر کم از کم 13 سال ہونی چاہیے"
    }

    fun genderLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Gender"
        AppLanguage.HINDI -> "लिंग"
        AppLanguage.URDU -> "جنس"
    }

    fun genderMale(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Male"
        AppLanguage.HINDI -> "पुरुष"
        AppLanguage.URDU -> "مرد"
    }

    fun genderFemale(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Female"
        AppLanguage.HINDI -> "महिला"
        AppLanguage.URDU -> "خاتون"
    }

    fun genderOther(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Other"
        AppLanguage.HINDI -> "अन्य"
        AppLanguage.URDU -> "دیگر"
    }

    fun genderPreferNot(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Prefer not to say"
        AppLanguage.HINDI -> "बताना नहीं चाहते"
        AppLanguage.URDU -> "بتانا نہیں چاہتے"
    }

    fun mobileNumberLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Mobile Number"
        AppLanguage.HINDI -> "मोबाइल नंबर"
        AppLanguage.URDU -> "موبائل نمبر"
    }

    fun mobilePrivateNote(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Private - only you can view this"
        AppLanguage.HINDI -> "निजी - केवल आप इसे देख सकते हैं"
        AppLanguage.URDU -> "نجی - صرف آپ اسے دیکھ سکتے ہیں"
    }

    fun mobileInvalid(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Please enter a valid mobile number"
        AppLanguage.HINDI -> "कृपया एक मान्य मोबाइल नंबर दर्ज करें"
        AppLanguage.URDU -> "براہ کرم ایک درست موبائل نمبر درج کریں"
    }

    fun mobileTaken(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Mobile number is already registered"
        AppLanguage.HINDI -> "यह मोबाइल नंबर पहले से पंजीकृत है"
        AppLanguage.URDU -> "یہ موبائل نمبر پہلے سے رجسٹرڈ ہے"
    }

    fun phoneAuthOptionalTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Optional Phone OTP Verification"
        AppLanguage.HINDI -> "वैकल्पिक फ़ोन ओटीपी सत्यापन"
        AppLanguage.URDU -> "اختیاری فون او ٹی پی کی تصدیق"
    }

    fun sendOtp(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Send OTP"
        AppLanguage.HINDI -> "ओटीपी भेजें"
        AppLanguage.URDU -> "او ٹی پی بھیجیں"
    }

    fun verifyOtp(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Verify OTP"
        AppLanguage.HINDI -> "ओटीपी सत्यापित करें"
        AppLanguage.URDU -> "او ٹی پی کی تصدیق کریں"
    }

    fun otpSentMessage(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "OTP sent. Enter 6-digit code (use 123456 in preview)"
        AppLanguage.HINDI -> "ओटीपी भेज दिया गया है। 6 अंकों का कोड दर्ज करें (पूर्वावलोकन में 123456 उपयोग करें)"
        AppLanguage.URDU -> "او ٹی پی بھیج دیا گیا۔ 6 ہندسوں کا کوڈ درج کریں (پیش نظارہ میں 123456 استعمال کریں)"
    }

    fun phoneVerifiedBadge(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Phone Verified ✓"
        AppLanguage.HINDI -> "फ़ोन सत्यापित ✓"
        AppLanguage.URDU -> "فون کی تصدیق ہو گئی ✓"
    }

    fun profilePhotoDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Upload a clear profile photo. It will be compressed to 400x400 and stored securely."
        AppLanguage.HINDI -> "एक स्पष्ट प्रोफ़ाइल फ़ोटो अपलोड करें। इसे 400x400 में संपीड़ित करके सुरक्षित रूप से सहेजा जाएगा।"
        AppLanguage.URDU -> "ایک واضح پروفائل تصویر اپ لوڈ کریں۔ اسے 400x400 میں کمپریس کر کے محفوظ طریقے سے محفوظ کیا جائے گا۔"
    }

    fun pickFromGallery(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Choose from Gallery"
        AppLanguage.HINDI -> "गैलरी से चुनें"
        AppLanguage.URDU -> "گیلری سے منتخب کریں"
    }

    fun takeCameraPhoto(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Take Photo"
        AppLanguage.HINDI -> "कैमरे से फ़ोटो लें"
        AppLanguage.URDU -> "کیمرے سے تصویر لیں"
    }

    fun photoMandatoryError(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Profile photo is mandatory. Please select a photo."
        AppLanguage.HINDI -> "प्रोफ़ाइल फ़ोटो अनिवार्य है। कृपया एक फ़ोटो चुनें।"
        AppLanguage.URDU -> "پروفائل تصویر لازمی ہے۔ براہ کرم تصویر منتخب کریں۔"
    }

    fun termsAgreementCheckbox(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "I agree to the Terms of Service, Privacy Policy and Community Guidelines"
        AppLanguage.HINDI -> "मैं सेवा की शर्तों, गोपनीयता नीति और सामुदायिक दिशानिर्देशों से सहमत हूँ"
        AppLanguage.URDU -> "میں سروس کی شرائط، رازداری کی پالیسی اور کمیونٹی گائیڈ لائنز سے متفق ہوں"
    }

    fun age13Checkbox(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "I am 13 years of age or older"
        AppLanguage.HINDI -> "मेरी आयु 13 वर्ष या उससे अधिक है"
        AppLanguage.URDU -> "میری عمر 13 سال یا اس سے زیادہ ہے"
    }

    fun communityGuidelines(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Community Guidelines"
        AppLanguage.HINDI -> "सामुदायिक दिशानिर्देश"
        AppLanguage.URDU -> "کمیونٹی گائیڈ لائنز"
    }

    // --- Edit Profile Screen Strings ---
    fun editProfileTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Edit Profile"
        AppLanguage.HINDI -> "प्रोफ़ाइल संपादित करें"
        AppLanguage.URDU -> "پروفائل میں ترمیم کریں"
    }

    fun changePhoto(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Change Photo"
        AppLanguage.HINDI -> "फ़ोटो बदलें"
        AppLanguage.URDU -> "تصویر تبدیل کریں"
    }

    fun removePhoto(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Remove Photo"
        AppLanguage.HINDI -> "फ़ोटो हटाएं"
        AppLanguage.URDU -> "تصویر ہٹائیں"
    }

    fun pronounsLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Pronouns (e.g. they/them, she/her)"
        AppLanguage.HINDI -> "सर्वनाम (जैसे they/them, she/her)"
        AppLanguage.URDU -> "ضمیریں (مثلاً they/them، she/her)"
    }

    fun bioLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Bio"
        AppLanguage.HINDI -> "बायो"
        AppLanguage.URDU -> "تعارف"
    }

    fun linksTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Links (Add up to 3)"
        AppLanguage.HINDI -> "लिंक्स (अधिकतम 3)"
        AppLanguage.URDU -> "روابط (زیادہ سے زیادہ 3)"
    }

    fun addLink(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Add Link"
        AppLanguage.HINDI -> "लिंक जोड़ें"
        AppLanguage.URDU -> "ربط شامل کریں"
    }

    fun categoryLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Category"
        AppLanguage.HINDI -> "श्रेणी"
        AppLanguage.URDU -> "زمرہ"
    }

    fun contactOptionsTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Contact Options"
        AppLanguage.HINDI -> "संपर्क विकल्प"
        AppLanguage.URDU -> "رابطے کے اختیارات"
    }

    fun showEmailToggle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Show email button on profile"
        AppLanguage.HINDI -> "प्रोफ़ाइल पर ईमेल बटन दिखाएं"
        AppLanguage.URDU -> "پروفائل پر ای میل کا بٹن دکھائیں"
    }

    fun showPhoneToggle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Show phone button on profile"
        AppLanguage.HINDI -> "प्रोफ़ाइल पर फ़ोन बटन दिखाएं"
        AppLanguage.URDU -> "پروفائل پر فون کا بٹن دکھائیں"
    }

    fun privateAccountTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Private Account"
        AppLanguage.HINDI -> "निजी खाता"
        AppLanguage.URDU -> "نجی اکاؤنٹ"
    }

    fun privateAccountDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "When your account is private, only approved followers can see your posts and clips."
        AppLanguage.HINDI -> "जब आपका खाता निजी होता है, तो केवल स्वीकृत फ़ॉलोअर्स ही आपकी पोस्ट और क्लिप देख सकते हैं।"
        AppLanguage.URDU -> "جب آپ کا اکاؤنٹ نجی ہوتا ہے تو صرف منظور شدہ فالوورز ہی آپ کی پوسٹس اور کلپس دیکھ سکتے ہیں۔"
    }

    fun hideFollowersToggle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Hide followers and following count"
        AppLanguage.HINDI -> "फ़ॉलोअर्स और फ़ॉलोइंग संख्या छुपाएं"
        AppLanguage.URDU -> "فالوورز اور فالونگ کی تعداد چھپائیں"
    }

    fun aiProfileToggle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Show \"AI-generated profile\" label"
        AppLanguage.HINDI -> "\"AI-जनरेटेड प्रोफ़ाइल\" लेबल दिखाएं"
        AppLanguage.URDU -> "\"AI سے تخلیق شدہ پروفائل\" لیبل دکھائیں"
    }

    fun vynoraPlusStatusTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "VYNORA Plus Status"
        AppLanguage.HINDI -> "वायनोरा प्लस स्थिति"
        AppLanguage.URDU -> "وائنورا پلس اسٹیٹس"
    }

    fun vynoraPlusActive(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "VYNORA Plus Active (Blue Badge)"
        AppLanguage.HINDI -> "वायनोरा प्लस सक्रिय (ब्लू बैज)"
        AppLanguage.URDU -> "وائنورا پلس فعال (نیلا بیج)"
    }

    fun vynoraPlusInactive(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Not Active"
        AppLanguage.HINDI -> "सक्रिय नहीं है"
        AppLanguage.URDU -> "غیر فعال"
    }

    fun getVynoraPlusButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Get VYNORA Plus"
        AppLanguage.HINDI -> "वायनोरा प्लस प्राप्त करें"
        AppLanguage.URDU -> "وائنورا پلس حاصل کریں"
    }

    fun saveProfileButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Save Changes"
        AppLanguage.HINDI -> "परिवर्तन सहेजें"
        AppLanguage.URDU -> "تبدیلیاں محفوظ کریں"
    }

    fun unsavedChangesTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Discard unsaved changes?"
        AppLanguage.HINDI -> "बिना सहेजे परिवर्तन छोड़ें?"
        AppLanguage.URDU -> "کیا غیر محفوظ تبدیلیاں مسترد کر دیں؟"
    }

    fun unsavedChangesDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "You have unsaved edits. If you leave now, your changes will be lost."
        AppLanguage.HINDI -> "आपके पास बिना सहेजे संपादन हैं। यदि आप अभी छोड़ते हैं, तो परिवर्तन खो जाएंगे।"
        AppLanguage.URDU -> "آپ کے پاس غیر محفوظ شدہ تبدیلیاں ہیں۔ اگر آپ ابھی چھوڑ دیتے ہیں تو تبدیلیاں ضائع ہو جائیں گی۔"
    }

    fun keepEditing(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Keep Editing"
        AppLanguage.HINDI -> "संपादन जारी रखें"
        AppLanguage.URDU -> "ترمیم جاری رکھیں"
    }

    fun discard(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Discard"
        AppLanguage.HINDI -> "छोड़ें"
        AppLanguage.URDU -> "مسترد کریں"
    }

    fun usernameCooldownWarning(daysRemaining: Int, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Username can only be changed once every 14 days ($daysRemaining days remaining)"
        AppLanguage.HINDI -> "उपयोगकर्ता नाम हर 14 दिनों में केवल एक बार बदला जा सकता है ($daysRemaining दिन शेष)"
        AppLanguage.URDU -> "صارف نام صرف ہر 14 دن میں ایک بار تبدیل کیا جا سکتا ہے ($daysRemaining دن باقی)"
    }

    fun nameCooldownWarning(daysRemaining: Int, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Name can only be changed once every 14 days ($daysRemaining days remaining)"
        AppLanguage.HINDI -> "नाम हर 14 दिनों में केवल एक बार बदला जा सकता है ($daysRemaining दिन शेष)"
        AppLanguage.URDU -> "نام صرف ہر 14 دن میں ایک بار تبدیل کیا جا سکتا ہے ($daysRemaining دن باقی)"
    }

    fun createAccount(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Create Account"
        AppLanguage.HINDI -> "खाता बनाएं"
        AppLanguage.URDU -> "اکاؤنٹ بنائیں"
    }

    fun profileSavedSuccess(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Profile saved successfully ✨"
        AppLanguage.HINDI -> "प्रोफ़ाइल सफलतापूर्वक सहेजी गई ✨"
        AppLanguage.URDU -> "پروفائل کامیابی سے محفوظ ہو گئی ✨"
    }

    fun aiProfileDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Displays an AI-created badge on your profile"
        AppLanguage.HINDI -> "आपकी प्रोफ़ाइल पर AI बैज दिखाता है"
        AppLanguage.URDU -> "آپ کے پروفائل پر AI بیج دکھاتا ہے"
    }
}
