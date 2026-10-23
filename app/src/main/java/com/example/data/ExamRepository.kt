package com.example.data

import android.content.Context
import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

class ExamRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val resultDao = db.resultDao()
    private val flashcardDao = db.flashcardDao()
    private val customExamDao = db.customExamDao()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _exams = MutableStateFlow<List<Exam>>(SampleData.defaultExams)
    val exams: StateFlow<List<Exam>> = _exams.asStateFlow()

    private val _dailyChallenge = MutableStateFlow(
        DailyChallengeState(
            dateString = "আজকের চ্যালেঞ্জ",
            questions = SampleData.defaultExams.flatMap { it.questions }.shuffled().take(5)
        )
    )
    val dailyChallenge: StateFlow<DailyChallengeState> = _dailyChallenge.asStateFlow()

    val resultsFlow: Flow<List<ExamResult>> = resultDao.getAllResults().map { entities ->
        entities.map { entity ->
            val answersMap = mutableMapOf<Long, Int>()
            try {
                val json = JSONObject(entity.answersJson)
                json.keys().forEach { k ->
                    answersMap[k.toLong()] = json.getInt(k)
                }
            } catch (_: Exception) {}
            ExamResult(
                id = entity.id,
                examId = entity.examId,
                examTitle = entity.examTitle,
                score = entity.score,
                totalMarks = entity.totalMarks,
                correctCount = entity.correctCount,
                wrongCount = entity.wrongCount,
                skipCount = entity.skipCount,
                timestamp = entity.timestamp,
                userAnswers = answersMap
            )
        }
    }

    val flashcardsFlow: Flow<List<Flashcard>> = flashcardDao.getAllFlashcards().map { entities ->
        if (entities.isEmpty()) {
            SampleData.flashcardSeeds
        } else {
            entities.map {
                Flashcard(
                    id = it.id,
                    front = it.front,
                    back = it.back,
                    box = it.box,
                    dueTime = it.dueTime,
                    tag = it.tag
                )
            }
        }
    }

    suspend fun submitExamResult(
        exam: Exam,
        userAnswers: Map<Long, Int>
    ): ExamResult {
        var correct = 0
        var wrong = 0
        var skipped = 0
        var rawMarks = 0f

        val totalMarks = exam.questions.sumOf { it.marks }

        exam.questions.forEach { q ->
            val chosen = userAnswers[q.id]
            when {
                chosen == null -> skipped++
                chosen == q.correctIndex -> {
                    correct++
                    rawMarks += q.marks
                }
                else -> {
                    wrong++
                    if (exam.negMark > 0f) {
                        rawMarks -= (q.marks * exam.negMark)
                    }
                }
            }
        }

        val finalScore = maxOf(0f, rawMarks)
        val answersJson = JSONObject().apply {
            userAnswers.forEach { (k, v) -> put(k.toString(), v) }
        }.toString()

        val entity = ResultEntity(
            examId = exam.id,
            examTitle = exam.title,
            score = finalScore,
            totalMarks = totalMarks,
            correctCount = correct,
            wrongCount = wrong,
            skipCount = skipped,
            timestamp = System.currentTimeMillis(),
            answersJson = answersJson
        )

        val id = resultDao.insertResult(entity)

        // Award XP and Coins
        val earnedXp = (finalScore * 10).toInt() + 20
        val earnedCoins = maxOf(5, (finalScore * 2).toInt())
        addXpAndCoins(earnedXp, earnedCoins)

        return ExamResult(
            id = id,
            examId = exam.id,
            examTitle = exam.title,
            score = finalScore,
            totalMarks = totalMarks,
            correctCount = correct,
            wrongCount = wrong,
            skipCount = skipped,
            userAnswers = userAnswers
        )
    }

    fun addXpAndCoins(xpGain: Int, coinGain: Int) {
        val cur = _userProfile.value
        val newXp = cur.xp + xpGain
        val newCoins = cur.coins + coinGain
        val newLevel = (Math.sqrt(newXp.toDouble() / 50.0).toInt()) + 1
        _userProfile.value = cur.copy(
            xp = newXp,
            coins = newCoins,
            level = maxOf(cur.level, newLevel)
        )
    }

    fun submitDailyChallenge(score: Int, total: Int) {
        val cur = _dailyChallenge.value
        _dailyChallenge.value = cur.copy(isDone = true, score = score, total = total)
        addXpAndCoins(score * 15 + 10, score * 3 + 5)
    }

    suspend fun addFlashcard(front: String, back: String, tag: String = "কাস্টম") {
        flashcardDao.insertFlashcard(
            FlashcardEntity(
                front = front,
                back = back,
                box = 0,
                dueTime = System.currentTimeMillis(),
                tag = tag
            )
        )
    }

    suspend fun updateFlashcardBox(flashcard: Flashcard, correct: Boolean) {
        val nextBox = if (correct) minOf(4, flashcard.box + 1) else 0
        val nextDue = System.currentTimeMillis() + (nextBox * 86400000L)
        flashcardDao.updateFlashcard(
            FlashcardEntity(
                id = flashcard.id,
                front = flashcard.front,
                back = flashcard.back,
                box = nextBox,
                dueTime = nextDue,
                tag = flashcard.tag
            )
        )
    }

    suspend fun addTeacherExam(
        title: String,
        durationMinutes: Int,
        classLevel: Int,
        subject: String,
        chapter: String,
        questions: List<Question>
    ) {
        val newExam = Exam(
            id = System.currentTimeMillis(),
            title = title,
            durationMinutes = durationMinutes,
            classLevel = classLevel,
            subject = subject,
            chapter = chapter,
            questions = questions
        )
        _exams.value = listOf(newExam) + _exams.value
    }
}
