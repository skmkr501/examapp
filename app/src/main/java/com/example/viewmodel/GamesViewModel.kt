package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ExamRepository
import com.example.data.SampleData
import com.example.model.PairMatchItem
import com.example.model.SpinPrize
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class GamesViewModel(private val repository: ExamRepository) : ViewModel() {

    // Spin wheel state
    private val _isSpinning = MutableStateFlow(false)
    val isSpinning: StateFlow<Boolean> = _isSpinning.asStateFlow()

    private val _spinPrize = MutableStateFlow<SpinPrize?>(null)
    val spinPrize: StateFlow<SpinPrize?> = _spinPrize.asStateFlow()

    private val _spinAngle = MutableStateFlow(0f)
    val spinAngle: StateFlow<Float> = _spinAngle.asStateFlow()

    // Math Blitz state
    private val _mathProblem = MutableStateFlow("৭ + ৮")
    val mathProblem: StateFlow<String> = _mathProblem.asStateFlow()
    private var mathAnswer = 15
    private val _mathScore = MutableStateFlow(0)
    val mathScore: StateFlow<Int> = _mathScore.asStateFlow()
    private val _mathTimeRemaining = MutableStateFlow(30)
    val mathTimeRemaining: StateFlow<Int> = _mathTimeRemaining.asStateFlow()
    private val _mathActive = MutableStateFlow(false)
    val mathActive: StateFlow<Boolean> = _mathActive.asStateFlow()

    // Memory cards state
    val memoryPairs = listOf(
        PairMatchItem(1, "Brave", "সাহসী"),
        PairMatchItem(2, "Water", "পানি"),
        PairMatchItem(3, "Earth", "পৃথিবী"),
        PairMatchItem(4, "Sun", "সূর্য")
    )
    private val _memoryScore = MutableStateFlow(0)
    val memoryScore: StateFlow<Int> = _memoryScore.asStateFlow()

    fun spinWheel() {
        if (_isSpinning.value) return
        _isSpinning.value = true
        _spinPrize.value = null

        viewModelScope.launch {
            val prizes = SampleData.spinPrizes
            val selectedIndex = Random.nextInt(prizes.size)
            val selected = prizes[selectedIndex]

            // 5 full rotations + segment angle
            val targetAngle = _spinAngle.value + 360f * 5 + (selectedIndex * (360f / prizes.size))
            _spinAngle.value = targetAngle

            delay(2800) // spin duration
            _isSpinning.value = false
            _spinPrize.value = selected

            if (selected.kind == "coin") {
                repository.addXpAndCoins(0, selected.value)
            } else if (selected.kind == "xp") {
                repository.addXpAndCoins(selected.value, 0)
            }
        }
    }

    fun startMathBlitz() {
        _mathActive.value = true
        _mathScore.value = 0
        _mathTimeRemaining.value = 30
        nextMathProblem()

        viewModelScope.launch {
            while (_mathTimeRemaining.value > 0 && _mathActive.value) {
                delay(1000)
                _mathTimeRemaining.value -= 1
            }
            _mathActive.value = false
            repository.addXpAndCoins(_mathScore.value * 10, _mathScore.value * 2)
        }
    }

    private fun nextMathProblem() {
        val a = Random.nextInt(5, 25)
        val b = Random.nextInt(3, 15)
        val isAdd = Random.nextBoolean()
        if (isAdd) {
            mathAnswer = a + b
            _mathProblem.value = "$a + $b"
        } else {
            mathAnswer = a - b
            _mathProblem.value = "$a - $b"
        }
    }

    fun submitMathAnswer(ans: Int) {
        if (ans == mathAnswer) {
            _mathScore.value += 1
            nextMathProblem()
        }
    }

    fun addMemoryMatch() {
        _memoryScore.value += 1
        repository.addXpAndCoins(15, 5)
    }
}
