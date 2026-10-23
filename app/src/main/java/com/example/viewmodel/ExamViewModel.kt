package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ExamRepository
import com.example.data.SampleData
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExamViewModel(application: Application) : AndroidViewModel(application) {
    val repository = ExamRepository(application)

    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val exams: StateFlow<List<Exam>> = repository.exams
    val dailyChallenge: StateFlow<DailyChallengeState> = repository.dailyChallenge
    val pastResults = repository.resultsFlow
    val flashcards = repository.flashcardsFlow

    // Active Exam Running State
    private val _activeExam = MutableStateFlow<Exam?>(null)
    val activeExam: StateFlow<Exam?> = _activeExam.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _userAnswers = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val userAnswers: StateFlow<Map<Long, Int>> = _userAnswers.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(0)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    private val _lastResult = MutableStateFlow<ExamResult?>(null)
    val lastResult: StateFlow<ExamResult?> = _lastResult.asStateFlow()

    private var timerJob: Job? = null

    fun startExam(exam: Exam) {
        _activeExam.value = exam
        _currentQuestionIndex.value = 0
        _userAnswers.value = emptyMap()
        _secondsRemaining.value = exam.durationMinutes * 60

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_secondsRemaining.value > 0) {
                delay(1000)
                _secondsRemaining.value -= 1
            }
            // Auto submit if timer runs out
            submitExam()
        }
    }

    fun selectOption(questionId: Long, optionIndex: Int) {
        // If already selected for this question, lock it in as in the web app
        if (_userAnswers.value.containsKey(questionId)) return
        _userAnswers.value = _userAnswers.value + (questionId to optionIndex)
    }

    fun goToQuestion(index: Int) {
        val total = _activeExam.value?.questions?.size ?: 0
        if (index in 0 until total) {
            _currentQuestionIndex.value = index
        }
    }

    fun submitExam(onFinished: ((ExamResult) -> Unit)? = null) {
        timerJob?.cancel()
        val exam = _activeExam.value ?: return
        val answers = _userAnswers.value

        viewModelScope.launch {
            val result = repository.submitExamResult(exam, answers)
            _lastResult.value = result
            _activeExam.value = null
            onFinished?.invoke(result)
        }
    }

    fun exitExam() {
        timerJob?.cancel()
        _activeExam.value = null
    }

    fun submitDailyChallenge(score: Int, total: Int) {
        repository.submitDailyChallenge(score, total)
    }

    fun addFlashcard(front: String, back: String, tag: String) {
        viewModelScope.launch {
            repository.addFlashcard(front, back, tag)
        }
    }

    fun updateFlashcard(flashcard: Flashcard, correct: Boolean) {
        viewModelScope.launch {
            repository.updateFlashcardBox(flashcard, correct)
        }
    }

    fun addTeacherExam(title: String, duration: Int, classLevel: Int, subject: String, chapter: String, questions: List<Question>) {
        viewModelScope.launch {
            repository.addTeacherExam(title, duration, classLevel, subject, chapter, questions)
        }
    }
}
