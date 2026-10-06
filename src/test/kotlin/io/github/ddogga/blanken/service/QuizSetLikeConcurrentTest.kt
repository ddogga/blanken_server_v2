package io.github.ddogga.blanken.service


import io.github.ddogga.blanken.domain.quiz.Category
import io.github.ddogga.blanken.domain.quiz.QuizSet
import io.github.ddogga.blanken.domain.user.User
import io.github.ddogga.blanken.domain.quiz.Visibility
import io.github.ddogga.blanken.repository.CategoryRepository
import io.github.ddogga.blanken.repository.QuizSetLikeRepository
import io.github.ddogga.blanken.repository.QuizSetRepository
import io.github.ddogga.blanken.repository.UserRepository
import io.github.ddogga.blanken.support.PostgresTestContainerConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(PostgresTestContainerConfig::class)
@Sql("/user.sql")
class QuizSetLikeConcurrentTest (

    @Autowired private val quizSetLikeService: QuizSetLikeService,
    @Autowired private val quizSetRepository: QuizSetRepository,
    @Autowired private val quizSetLikeRepository: QuizSetLikeRepository,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val categoryRepository: CategoryRepository
){

    @BeforeEach
    fun init() {
        val category = categoryRepository.save(category(CATEGORY_NAME))
        CATEGORY_ID = category.id!!
        val user = userRepository.findFirstByOrderByIdAsc()!!
        val quizSet = quizSetRepository.save(quizSet(category, user))
        QUIZ_SET_ID = quizSet.id!!
    }

    // Transactional이 없으므로 데이터를 수동으로 삭제 해야 함.
    @AfterEach
    fun refresh() {
        quizSetLikeRepository.deleteAllInBatch()
        quizSetRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
        categoryRepository.deleteAllInBatch()
    }

    @Test
    fun `퀴즈셋_좋아요_동시_카운트시_원자적으로_카운트_된다`() {

        // given
        val users = userRepository.findAll()
        val threadCount = users.size

        val executor = Executors.newFixedThreadPool(threadCount)
        val ready = CountDownLatch(threadCount)
        val start = CountDownLatch(1)
        val done = CountDownLatch(threadCount)
        val errors = ConcurrentLinkedQueue<Throwable>()

        // when
        users.forEach { user ->
            executor.submit {
                try {
                    ready.countDown()
                    start.await()
                    quizSetLikeService.addLikeQuizSet(QUIZ_SET_ID, user.id!!)
                } catch (e: Throwable) {
                    errors.add(e)
                } finally {
                    done.countDown()
                }
            }
        }

        ready.await()   // 모든 스레드가 준비 될 때 까지 대기
        start.countDown()   // 동시에 시작
        val finished = done.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        // then
        assertTrue(finished, "제한 시간 내에 모든 요청이 끝나지 않음")
        assertTrue(errors.isEmpty(), "요청 중 예외 발셍: $errors")
        val result = quizSetRepository.findById(QUIZ_SET_ID).orElseThrow()
        assertEquals(threadCount, result.likeCount)
    }


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
        private var QUIZ_SET_ID = 1L
        private var CATEGORY_ID = 1L
    }

}