# VYNORA — "Share your world."

VYNORA is a next-generation social media Android application built with **Kotlin** and **Jetpack Compose**, featuring a near-black neon glassmorphism aesthetic (`#050509`, `#FF2E93` magenta to `#00D1FF` cyan gradients).

---

## ⚡ Features & Modules

1. **Splash & Onboarding**:
   - Animated glowing neon "V" logo splash.
   - 3-slide introductory carousel.
   - 13+ age gate confirmation with parental advisory notice.

2. **Authentication**:
   - Tabbed Log In and Sign Up with validation.
   - Google Sign-In button.
   - Password recovery dialogue.

3. **Home Feed**:
   - Multi-image photo carousels & video post cards.
   - Double-tap big heart animation to like.
   - Comment bottom sheet, bookmark/save, and share drawer.
   - Horizontal "Moments" story bar with glowing rainbow rings.

4. **Moments (24-Hour Stories)**:
   - Fullscreen story viewer with segmented progress bars.
   - Tap left to go back, tap right to go forward, long-press to pause.
   - "Close Friends" badge indicator (emerald green star).
   - Direct replies to author & viewer count sheet.

5. **Clips (Short Videos)**:
   - Full-screen vertical swipe pager (`VerticalPager`).
   - Action stack: Creator avatar with quick-follow badge, like, comments drawer, save, share.
   - Rotating vinyl audio disc and soundtrack ticker.
   - Mute / unmute audio toggle.

6. **Create Studio**:
   - Choose between New Post, 24h Moment, or Clip.
   - Aesthetic filters: Original, Neon Glow, Cyberpunk, Sunset Warm, Noir, Vibrant.
   - Gemini AI Caption Helper & AI Hashtags Generator.
   - Location tagging, scheduling toggle, and Close Friends toggle.

7. **Profile & Highlights**:
   - Avatar with glowing story ring, verified badge, stats (Posts, Followers, Following).
   - Bio and clickable website link.
   - Story highlights (Travel, Neon, Music, 3D Art).
   - Profile tabs: Grid Posts, Clips, Saved bookmarks.
   - QR code sharing card & Edit Profile sheet.

8. **Explore & Search**:
   - Search creators, hashtags, and locations.
   - Category filter pills (All, Creators, Trends, Nightlife, Aesthetic).
   - Suggested accounts carousel with quick-follow buttons.
   - Masonry-style media grid.

9. **Chats**:
   - Direct conversations and message requests tabs.
   - Text chat with read receipts and active status indicator.
   - Simulated voice notes with waveform audio player.
   - Emoji reactions on messages (❤️, 🔥, 🚀, 😂, 👏, 😮).

10. **Notifications**:
    - Grouped by Today, Yesterday, and Earlier.
    - Filter chips: All, Likes, Comments, Follows, Mentions.
    - Quick "Follow Back" action and "Mark all read".

11. **Live Studio**:
    - Live broadcast preview with viewer count simulation.
    - Real-time scrolling live comment stream.
    - Floating neon flying hearts animation.

12. **Creator Studio**:
    - Analytics dashboard: Total Impressions, Reach, Engagement Rate, Tips Earned.
    - Weekly follower growth bar chart.
    - Creator subscriptions and tip jar toggle.

13. **Settings & Legal Compliance**:
    - Dark / Light mode toggle.
    - Multi-language switcher: English, Hindi (हिन्दी), and Urdu (اردو) with bidirectional RTL layout support.
    - Private account toggle, activity status toggle, and offensive comment keyword filter.
    - Privacy Policy compliant with India's Digital Personal Data Protection Act, 2023 (DPDP Act).
    - Terms of Service, Community Guidelines, and Copyright Takedown Request Form.

14. **Admin Console**:
    - Moderation queue for reported posts, comments, and users.
    - Ban / suspend user and remove post actions.
    - Copyright infringement takedown review.

---

## 🛠 Tech Stack
- **OS**: Android (Min SDK 24, Target SDK 36)
- **Language**: Kotlin 2.2
- **UI Framework**: Jetpack Compose with Material Design 3
- **Image Loading**: Coil 2.7.0
- **AI Integration**: Gemini 3.5 Flash (`generativelanguage.googleapis.com`)
- **Architecture**: MVVM with Kotlin Coroutines & StateFlow
- **Localization**: English, Hindi, Urdu (RTL)
