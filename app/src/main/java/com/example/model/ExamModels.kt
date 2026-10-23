package com.example.model

data class Exam(
    val id: Long,
    val title: String,
    val durationMinutes: Int,
    val classLevel: Int, // 1..12 or 13 for Admission, 0 for General
    val subject: String,
    val chapter: String = "",
    val maxAttempts: Int = 1,
    val negMark: Float = 0f,
    val shuffleQuestions: Boolean = true,
    val shuffleOptions: Boolean = true,
    val reviewOpen: Boolean = true,
    val questions: List<Question> = emptyList()
)

data class Question(
    val id: Long,
    val examId: Long,
    val body: String,
    val options: List<String>,
    val correctIndex: Int,
    val marks: Int = 1,
    val explanation: String = ""
)

data class ExamResult(
    val id: Long = System.currentTimeMillis(),
    val examId: Long,
    val examTitle: String,
    val score: Float,
    val totalMarks: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val skipCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val userAnswers: Map<Long, Int> = emptyMap() // questionId -> chosenIndex
)

data class QuestionReview(
    val question: Question,
    val chosenIndex: Int?,
    val isCorrect: Boolean,
    val isSkipped: Boolean
)

data class SubjectCategory(
    val name: String,
    val icon: String,
    val examCount: Int = 0,
    val contentCount: Int = 0
)

data class ContentItem(
    val id: Long,
    val classLevel: Int,
    val subject: String,
    val chapter: String,
    val title: String,
    val kind: String, // "note", "video", "link"
    val body: String = "",
    val url: String = "",
    val isFree: Boolean = true
)
