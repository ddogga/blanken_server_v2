package io.github.ddogga.blanken.repository


import io.github.ddogga.blanken.config.QuerydslConfig
import io.github.ddogga.blanken.support.PostgresTestContainerConfig
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals


@DataJpaTest
@Import(PostgresTestContainerConfig::class, QuerydslConfig::class)
@Sql(scripts = ["/schema.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql("/data.sql")
class QuizSetLikeRepositoryTest (
    @Autowired private val quizSetLikeRepository: QuizSetLikeRepository,
    @Autowired private val quizSetRepository: QuizSetRepository,
    @Autowired private val userRepository: UserRepository
) {


    @BeforeEach()
    fun init() {

        val user = requireNotNull(userRepository.findByEmail(TEST_USER_EMAIL)) {"해당 이메일로 테스트 유저를 찾을 수 없음."}
        TEST_USER_ID = user.id!!

    }

    @Test
    fun `좋아요가_눌린_퀴즈셋만_반환한다`() {

        // given
        val pageable = PageRequest.of(0, 10)

        // when
        val likeQuizSets = quizSetLikeRepository.getLikeQuizSets(TEST_USER_ID, pageable)

        // then
        assertEquals(3, likeQuizSets.totalPages)
        assertEquals(30, likeQuizSets.totalElements)
    }


    companion object {
        private var TEST_USER_ID = 1L
        private const val TEST_USER_EMAIL = "test@test.io"
    }

}