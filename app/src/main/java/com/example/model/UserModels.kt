package com.example.model

data class UserProfile(
    val name: String = "শিক্ষার্থী",
    val email: String = "student@examsite.com",
    val classLevel: Int = 10,
    val avatar: String = "🙂",
    val xp: Int = 240,
    val coins: Int = 85,
    val currentStreak: Int = 3,
    val bestStreak: Int = 7,
    val level: Int = 3,
    val phone: String = "01711223344",
    val guardianPhone: String = "01811223344",
    val school: String = "ঢাকা গভর্নমেন্ট হাই স্কুল"
)

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isExplain: Boolean = false
)

data class DailyChallengeState(
    val dateString: String,
    val questions: List<Question>,
    val isDone: Boolean = false,
    val score: Int = 0,
    val total: Int = 5
)
