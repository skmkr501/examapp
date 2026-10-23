package com.example.model

enum class GameType(val title: String, val emoji: String, val descr: String) {
    MATH_BLITZ("ম্যাথ ব্লিটজ", "🧮", "দ্রুত মানসিক গণিতের চ্যালেঞ্জ"),
    NUM_SEQ("সংখ্যার ধাঁধা", "🔢", "ধারার পরবর্তী সংখ্যাটি খুঁজে বের করো"),
    MEMORY("কার্ড মেলানো", "🃏", "জোড়া কার্ড খুঁজে স্মৃতিশক্তি বাড়াও"),
    TRUE_FALSE("সত্য নাকি মিথ্যা", "❓", "বিজ্ঞান ও সাধারণ জ্ঞানের বিবৃতি যাচাই"),
    SCIENCE_DETECTIVE("বিজ্ঞান ডিটেকটিভ", "🕵️", "ক্লু দেখে বিজ্ঞানের রহস্য ভেদ করো"),
    BANGLADESH_QUIZ("বাংলাদেশ জানো", "🇧🇩", "ইতিহাস, ঐতিহ্য ও ভূগোলের কুইজ")
}

data class PairMatchItem(
    val id: Int,
    val left: String,
    val right: String
)

data class SpinPrize(
    val id: Int,
    val emoji: String,
    val label: String,
    val kind: String, // "coin", "xp", "badge", "none"
    val value: Int
)

data class Flashcard(
    val id: Long = System.currentTimeMillis(),
    val front: String,
    val back: String,
    val box: Int = 0, // Leitner box 0..4
    val dueTime: Long = System.currentTimeMillis(),
    val tag: String = "সাধারণ"
)

data class BadgeItem(
    val id: String,
    val icon: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val xpReward: Int = 50,
    val coinReward: Int = 10
)

data class FriendItem(
    val id: String,
    val name: String,
    val avatar: String,
    val level: Int,
    val xp: Int,
    val streakDays: Int,
    val wins: Int = 0,
    val isFriend: Boolean = true
)

data class ShopItem(
    val id: String,
    val title: String,
    val descr: String,
    val cost: Int,
    val kind: String, // "doubt", "tutor", "boost"
    val qty: Int
)
