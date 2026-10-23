package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_results")
data class ResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val examTitle: String,
    val score: Float,
    val totalMarks: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val skipCount: Int,
    val timestamp: Long,
    val answersJson: String
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val front: String,
    val back: String,
    val box: Int = 0,
    val dueTime: Long,
    val tag: String
)

@Entity(tableName = "custom_exams")
data class CustomExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val durationMinutes: Int,
    val classLevel: Int,
    val subject: String,
    val chapter: String,
    val questionsJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_preferences")
data class UserPrefEntity(
    @PrimaryKey val key: String,
    val value: String
)
