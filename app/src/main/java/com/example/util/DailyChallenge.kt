package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** One photo/video theme per day, the same for everybody. */
object DailyChallenge {
    private val themes = listOf(
        "#ChaiTime" to "Show us your chai or coffee moment",
        "#MySky" to "Post the sky above you right now",
        "#StreetFood" to "What is your favourite street food?",
        "#Throwback" to "Share an old photo with a story behind it",
        "#NightLights" to "Capture lights after sunset",
        "#MyDesk" to "Show your desk or study corner",
        "#Smile" to "Post something that made you smile today",
        "#FriendsForever" to "Tag a friend and share a memory",
        "#Nature" to "Plants, trees, rain - show us nature",
        "#MyCity" to "Show a corner of your city people should visit",
        "#Fitness" to "Your workout, walk or game of the day",
        "#Handmade" to "Show something you made yourself",
        "#MusicMood" to "Post a reel with the song you are playing on repeat",
        "#Family" to "A moment with family",
        "#Festival" to "How are you celebrating this week?",
        "#BeforeAfter" to "Show a before and after",
        "#PetLove" to "Your pet, or one you met today",
        "#Rooftop" to "View from the roof or window",
        "#Shoes" to "Show your shoes or style of the day",
        "#Sunset" to "Golden hour from where you are",
        "#Books" to "What are you reading or learning?",
        "#Cooking" to "Show what you cooked today",
        "#Travel" to "A place you want to go back to",
        "#Colors" to "Find one colour around you and shoot it",
        "#Mirror" to "Mirror selfie, your best style",
        "#Rain" to "Rain, clouds or fog - show the weather",
        "#Learn" to "Teach us one small thing in a reel",
        "#Funny" to "Make us laugh in 15 seconds",
        "#Gratitude" to "One thing you are thankful for today",
        "#Surprise" to "Post anything that surprises us"
    )

    fun today(): Pair<String, String> {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return themes[day % themes.size]
    }

    private fun fmt(c: Calendar): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    fun todayKey(): String = fmt(Calendar.getInstance())
    fun yesterdayKey(): String = fmt(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) })
}
