package io.github.ddogga.blanken.service


import io.github.ddogga.blanken.domain.Category
import io.github.ddogga.blanken.domain.QuizSet
import io.github.ddogga.blanken.domain.User
import io.github.ddogga.blanken.domain.Visibility
import io.github.ddogga.blanken.exception.ErrorCode
import io.github.ddogga.blanken.exception.QuizSetLikeDuplicationException
import io.github.ddogga.blanken.repository.CategoryRepository
import io.github.ddogga.blanken.repository.QuizSetRepository
import io.github.ddogga.blanken.repository.UserRepository
import io.github.ddogga.blanken.support.PostgresTestContainerConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(PostgresTestContainerConfig::class)
@Transactional
class QuizSetLikeServiceTest(
    @Autowired private val quizSetLikeService: QuizSetLikeService,
    @Autowired private val quizSetRepository: QuizSetRepository,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val categoryRepository: CategoryRepository
) {

    @BeforeEach
    fun init() {
        val category = categoryRepository.save(category(CATEGORY_NAME))
        val user = userRepository.save(user())
        val quizSet = quizSetRepository.save(quizSet(category, user))

        CATEGORY_ID = category.id!!
        OWNER_ID = user.id!!
        QUIZ_SET_ID = quizSet.id!!

    }

    @Test
    fun `퀴즈셋_좋아요_추가_로직을_정상적으로_수행한다`() {

        // when
        val response = quizSetLikeService.addLikeQuizSet(QUIZ_SET_ID, OWNER_ID)

        // then
        val update = quizSetRepository.findById(QUIZ_SET_ID).get()

        assertEquals(QUIZ_SET_ID, response.id)
        assertEquals(update.likeCount, response.likeCount)

    }

    @Test
    fun `퀴즈셋_좋아요_중복_생성시_중복_예외를_던진다`() {

        // given
        quizSetLikeService.addLikeQuizSet(QUIZ_SET_ID, OWNER_ID)

        // when
        val exception = assertFailsWith<QuizSetLikeDuplicationException> {
            quizSetLikeService.addLikeQuizSet(QUIZ_SET_ID, OWNER_ID)
        }

        // then
        assertEquals(ErrorCode.QUIZ_SET_LIKE_DUPLICATION, exception.errorCode)
        assertEquals(QUIZ_SET_ID, exception.quizSetId)
    }

    @Test
    fun `퀴즈셋_좋아요_동시_카운트시_원자적으로_카운트_된다`() {
        // TODO
    }


    private fun user(): User =
        User(email = EMAIL, password = "hashed", nickname = NICKNAME)

    private fun category(name: String): Category = Category(name = name)

    private fun quizSet(category: Category, user: User): QuizSet =
        QuizSet(
            user,
            category = category,
            title = TITLE,
            description = DESCRIPTION,
            visibility = Visibility.PUBLIC,
        )

    companion object {
        private const val CATEGORY_NAME = "토익"
        private const val TITLE = "토익 빈출 동사"
        private const val DESCRIPTION = "30선"
        private const val EMAIL = "owner@blanken.io"
        private const val NICKNAME = "blanken"
        private var OWNER_ID = 1L
        private var QUIZ_SET_ID = 1L
        private var CATEGORY_ID = 1L
    }

}