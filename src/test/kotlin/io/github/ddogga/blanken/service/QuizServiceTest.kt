package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.Category
import io.github.ddogga.blanken.domain.Quiz
import io.github.ddogga.blanken.domain.QuizSet
import io.github.ddogga.blanken.domain.User
import io.github.ddogga.blanken.domain.Visibility
import io.github.ddogga.blanken.dto.quiz.QuizRequest
import io.github.ddogga.blanken.dto.quiz.QuizResponse
import io.github.ddogga.blanken.repository.QuizRepository
import io.github.ddogga.blanken.repository.QuizSetRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals


class QuizServiceTest {

    private val quizRepository = mockk<QuizRepository>()
    private val quizSetRepository = mockk<QuizSetRepository>()
    private val quizService = QuizService(quizSetRepository, quizRepository)

    @Test
    fun `퀴즈를_정상적으로_생성한다`() {

        // given
        val quizSet = quizSet(category(CATEGORY_ID_1, "토익"))
        val savedQuiz = slot<Quiz>()

        every { quizSetRepository.findById(QUIZ_SET_ID) } returns Optional.of(quizSet)
        every { quizRepository.save(capture(savedQuiz)) } returns quiz(quizSet)

        // when
        val response = quizService.create(
            QUIZ_SET_ID,
            QuizRequest(
                sentence = QUIZ_SENTENCE,
                answerWord = QUIZ_ANSWER_WORD,
                hint = QUIZ_HINT
            )
        )

        // then
        assertEquals(QUIZ_SENTENCE, response.sentence)
        assertEquals(QUIZ_ANSWER_WORD, response.answerWord)
        assertEquals(QUIZ_HINT, response.hint)
        assertEquals(QUIZ_SET_ID, response.quizSetId)

    }

    @Test
    fun `퀴즈를_정상적으로_수정한다`() {

        // given
        val quizSet = quizSet(category(CATEGORY_ID_1, "토익"))
        val savedQuiz = slot<Quiz>()

        every { quizRepository.findById(QUIZ_ID) } returns Optional.of(quiz(quizSet))

        // when
        val response = quizService.update(QUIZ_ID, QUIZ_SET_ID, updateRequest())

        // then
        assertEquals(UPDATED_SENTENCE, response.sentence)
        assertEquals(UPDATED_ANSWER_WORD, response.answerWord)
    }

    @Test
    fun `퀴즈를_다른_퀴즈셋으로_정상적으로_옮긴다`() {

        // given
        val quizSet = quizSet(category(CATEGORY_ID_1, "토익"))

        every { quizSetRepository.findById(NEW_QUIZ_SET_ID) } returns Optional.of(newQuizSet(category(CATEGORY_ID_1, "토익")))
        every { quizRepository.findById(QUIZ_ID) } returns Optional.of(quiz(quizSet))

        // when
        val response = quizService.changeQuizSet(QUIZ_ID, NEW_QUIZ_SET_ID)

        // then
        assertEquals(NEW_QUIZ_SET_ID, response.quizSetId)
    }



    private fun updateRequest()
    = QuizRequest(
        sentence = UPDATED_SENTENCE,
        answerWord = UPDATED_ANSWER_WORD,
        hint = QUIZ_HINT
    )


    private fun category(id: Long, name: String): Category = Category(name = name, id = id)

    private fun quizSet(category: Category): QuizSet = quizSet(category, QUIZ_SET_ID)

    private fun newQuizSet(category: Category): QuizSet = quizSet(category, NEW_QUIZ_SET_ID)

    private fun quizSet(category: Category, id: Long): QuizSet =
        QuizSet(
            owner = User("x@x.io", "pass", "nick", 1L).apply {
                createdAt = CREATED_AT
                updatedAt = CREATED_AT
            },
            category = category,
            title = TITLE,
            description = DESCRIPTION,
            visibility = Visibility.PUBLIC,
            id = id,
        ).apply {
            createdAt = CREATED_AT
            updatedAt = CREATED_AT
        }


    private fun quiz(quizSet: QuizSet): Quiz =
        Quiz(
            sentence = QUIZ_SENTENCE,
            answerWord = QUIZ_ANSWER_WORD,
            hint = QUIZ_HINT,
            id = QUIZ_ID
        ).apply {
            this.quizSet = quizSet
            createdAt = CREATED_AT
            updatedAt = CREATED_AT
        }




    companion object {
        private const val QUIZ_SET_ID = 1L
        private const val TITLE = "토익 빈출 동사"
        private const val DESCRIPTION = "30선"
        private const val CATEGORY_ID_1 = 1L
        private const val QUIZ_ID = 10L
        private const val QUIZ_SENTENCE = "She decided to {{}} the meeting until next week."
        private const val QUIZ_ANSWER_WORD = "postpone"
        private const val QUIZ_HINT = "미루다, 연기하다"
        private const val UPDATED_SENTENCE = "He tried to {{}} the deadline by two days."
        private const val UPDATED_ANSWER_WORD = "extend"
        private const val NEW_QUIZ_SET_ID = 2L
        private val CREATED_AT: Instant = Instant.parse("2026-01-02T00:00:00Z")

    }

}