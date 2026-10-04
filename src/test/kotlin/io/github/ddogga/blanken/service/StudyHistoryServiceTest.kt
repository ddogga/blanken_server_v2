package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.Category
import io.github.ddogga.blanken.domain.Quiz
import io.github.ddogga.blanken.domain.QuizSet
import io.github.ddogga.blanken.domain.User
import io.github.ddogga.blanken.domain.UserRole
import io.github.ddogga.blanken.domain.UserStatus
import io.github.ddogga.blanken.domain.Visibility
import io.github.ddogga.blanken.dto.history.StudyHistoryDetailRequest
import io.github.ddogga.blanken.dto.history.StudyHistoryRequest
import io.github.ddogga.blanken.repository.CategoryRepository
import io.github.ddogga.blanken.repository.QuizRepository
import io.github.ddogga.blanken.repository.QuizSetRepository
import io.github.ddogga.blanken.repository.StudyHistoryRepository
import io.github.ddogga.blanken.repository.UserRepository
import io.github.ddogga.blanken.support.PostgresTestContainerConfig
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(PostgresTestContainerConfig::class)
@Transactional
class StudyHistoryServiceTest(

    @Autowired private val userRepository: UserRepository,
    @Autowired private val categoryRepository: CategoryRepository,
    @Autowired private val quizRepository: QuizRepository,
    @Autowired private val quizSetRepository: QuizSetRepository,
    @Autowired private val studyHistoryRepository: StudyHistoryRepository,
    @Autowired private val studyHistoryService: StudyHistoryService,
    @Autowired private val entityManager: EntityManager,
) {

    private var ownerId = 0L
    private var quizSetId = 0L
    private var quizId1 = 0L
    private var quizId2 = 0L
    private lateinit var category: Category
    private lateinit var owner: User

    @BeforeEach
    fun init() {
        category = categoryRepository.save(category(CATEGORY_NAME))
        owner = userRepository.save(user())
        val quizSet = quizSetRepository.save(quizSet(category, owner, TITLE))

        val quizzes = quizzes().onEach { quizSet.addQuiz(it) }
        val savedQuizzes = quizRepository.saveAll(quizzes)

        ownerId = owner.id!!
        quizSetId = quizSet.id!!
        quizId1 = savedQuizzes[0].id!!
        quizId2 = savedQuizzes[1].id!!
    }


    @Test
    fun `학습_히스토리를_정상적으로_생성한다`() {
        // when
        val response = studyHistoryService.create(historyRequest())

        // then
        assertEquals(ownerId, response.userId)
        assertEquals(quizSetId, response.quizSetId)
        assertEquals(TITLE, response.quizSetTitle)
        assertEquals(TOTAL_COUNT, response.totalCount)
        assertEquals(CORRECT_COUNT, response.correctCount)
        assertEquals(BigDecimal("100.00"), response.score)

        // 1차 캐시를 비워 DB 에 실제로 적재됐는지 본다 — cascade 만 믿지 않는다.
        entityManager.flush()
        entityManager.clear()

        val saved = studyHistoryRepository.findById(response.id).orElseThrow()
        assertEquals(2, saved.details.size)
        assertEquals(setOf(quizId1, quizId2), saved.details.map { it.quiz.id }.toSet())
        assertTrue(saved.details.none { it.gaveUp })
    }

    @Test
    fun `포기한_문제는_오답으로_기록되고_점수에_반영된다`() {
        val request = historyRequest(
            correctCount = 1,
            details = listOf(
                StudyHistoryDetailRequest(quizId = quizId1, gaveUp = false),
                StudyHistoryDetailRequest(quizId = quizId2, gaveUp = true),
            ),
        )

        // when
        val response = studyHistoryService.create(request)

        // then
        assertEquals(BigDecimal("50.00"), response.score)

        entityManager.flush()
        entityManager.clear()

        val saved = studyHistoryRepository.findById(response.id).orElseThrow()
        assertEquals(setOf(quizId2), saved.details.filter { it.gaveUp }.map { it.quiz.id }.toSet())
    }


    private fun historyRequest(
        userId: Long = ownerId,
        quizSetId: Long = this.quizSetId,
        totalCount: Int = TOTAL_COUNT,
        correctCount: Int = CORRECT_COUNT,
        details: List<StudyHistoryDetailRequest> = defaultDetails(),
    ): StudyHistoryRequest =
        StudyHistoryRequest(
            userId = userId,
            quizSetId = quizSetId,
            totalCount = totalCount,
            correctCount = correctCount,
            details = details,
        )

    private fun defaultDetails(): List<StudyHistoryDetailRequest> =
        listOf(
            StudyHistoryDetailRequest(quizId = quizId1, gaveUp = false),
            StudyHistoryDetailRequest(quizId = quizId2, gaveUp = false),
        )

    private fun user(): User =
        User(email = EMAIL,
            nickname = NICKNAME,
            null,
            null,
            UserStatus.ACTIVE,
            UserRole.USER
        )

    private fun category(name: String): Category = Category(name = name)

    private fun quizSet(category: Category, user: User, title: String): QuizSet =
        QuizSet(
            user,
            category = category,
            title = title,
            description = DESCRIPTION,
            visibility = Visibility.PUBLIC,
        )

    private fun quizzes(): List<Quiz> =
        listOf(
            Quiz(
                sentence = "She decided to {{}} the meeting until next week.",
                answerWord = "postpone",
                hint = "미루다, 연기하다",
            ),
            Quiz(
                sentence = "Please {{}} the attached document before Friday.",
                answerWord = "review",
                hint = "검토하다",
            ),
        )

    companion object {
        private const val CATEGORY_NAME = "토익"
        private const val TITLE = "토익 빈출 동사"
        private const val DESCRIPTION = "30선"
        private const val EMAIL = "owner@blanken.io"
        private const val NICKNAME = "blanken"
        private const val TOTAL_COUNT = 2
        private const val CORRECT_COUNT = 2
    }

}
