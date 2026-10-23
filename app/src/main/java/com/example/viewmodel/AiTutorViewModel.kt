package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiTutorRepository
import com.example.data.ExamRepository
import com.example.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiTutorViewModel(
    private val examRepository: ExamRepository,
    private val tutorRepository: AiTutorRepository = AiTutorRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "নমস্কার! আমি AI মশাই 🧑‍🏫 — তোমার সার্বক্ষণিক শিক্ষক ও পড়ার সঙ্গী। গণিত, বিজ্ঞান, ব্যাকরণ বা যেকোনো বিষয়ে কোনো কিছু বুঝতে সমস্যা হলে আমাকে নিঃসঙ্কোচে জিজ্ঞেস করো!",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _doubtSolution = MutableStateFlow<String?>(null)
    val doubtSolution: StateFlow<String?> = _doubtSolution.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank() || _isThinking.value) return
        val userMsg = ChatMessage(text = text, isUser = true)
        _messages.value = _messages.value + userMsg

        _isThinking.value = true
        viewModelScope.launch {
            val classLevel = examRepository.userProfile.value.classLevel
            val reply = tutorRepository.getTutorReply(text, classLevel)
            _messages.value = _messages.value + ChatMessage(text = reply, isUser = false)
            _isThinking.value = false
        }
    }

    fun solveDoubt(subject: String, questionText: String) {
        if (questionText.isBlank() || _isThinking.value) return
        _isThinking.value = true
        _doubtSolution.value = null

        viewModelScope.launch {
            val solution = tutorRepository.solveDoubt(subject, questionText)
            _doubtSolution.value = solution
            _isThinking.value = false
        }
    }
}
