package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.quiz.Quiz
import io.github.ddogga.blanken.domain.quiz.QuizSet
import io.github.ddogga.blanken.dto.quiz.QuizRequest
import io.github.ddogga.blanken.dto.quiz.QuizSetUpdateRequest
import io.github.ddogga.blanken.repository.QuizSetRepository
import io.github.ddogga.blanken.support.PostgresTestContainerConfig
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * `QuizSetService` 통합 테스트 — 실제 PostgreSQL(Testcontainers) 로 돌린다.
 *
 * 단위 테스트(`QuizSetServiceTest`)로 검증할 수 없는 것만 여기서 본다:
 * 수정 시 새로 추가한 퀴즈가 cascade 로 INSERT 되고, flush 로 받은 IDENTITY id 가 응답에 담기는지.
 * 목 저장소의 flush 는 INSERT 를 하지 않아 이 동작을 흉내 낼 수 없다.
 *
 * `@Transactional` 이라 테스트마다 롤백된다. 서비스의 트랜잭션은 이 트랜잭션에 참여하므로
 * 서비스 안의 flush 로 INSERT 는 실행되지만 커밋되지는 않는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(PostgresTestContainerConfig::class)
@Sql("/data.sql")
@Transactional
class QuizSetServiceIntegrationTest(
	@Autowired private val quizSetService: QuizSetService,
	@Autowired private val quizSetRepository: QuizSetRepository,
	@Autowired private val entityManager: EntityManager,
) {

	@Test
	fun `퀴즈셋_수정시_새로_추가한_퀴즈는_flush로_id가_채워져_응답에_담긴다`() {
		// given — 시드 퀴즈셋 중 퀴즈가 있는 것 하나. 제목·카테고리는 그대로 두고 퀴즈만 하나 추가한다.
		val quizSet = quizSetWithQuizzes()
		val existing = quizSet.quizzes
		val request = QuizSetUpdateRequest(
			title = quizSet.title,
			description = quizSet.description,
			visibility = quizSet.visibility,
			categoryId = quizSet.category.id!!,
			quizzes = existing.map { it.toRequest() } + QuizRequest(
				id = null,
				sentence = NEW_SENTENCE,
				answers = listOf(NEW_ANSWER),
				hint = null,
				orderIndex = existing.size,
			),
		)

		// when
		val response = quizSetService.update(quizSet.id!!, request)

		// then — 응답
		assertEquals(existing.size + 1, response.quizCount)
		assertEquals(existing.size + 1, response.quizzes.size)
		val added = response.quizzes.single { it.sentence == NEW_SENTENCE }
		assertNotNull(added.id)                         // flush 로 INSERT 되어 IDENTITY id 를 받았다
		assertEquals(listOf(NEW_ANSWER), added.answers)
		assertEquals(existing.size, added.orderIndex)
		assertEquals(response.quizzes.map { it.orderIndex }.sorted(), response.quizzes.map { it.orderIndex })

		// then — DB. 1차 캐시를 비우고 다시 읽어 실제로 저장된 행을 본다.
		entityManager.flush()
		entityManager.clear()
		val reloaded = quizSetRepository.findWithCategoryAndQuizzesById(quizSet.id!!)!!
		assertEquals(existing.size + 1, reloaded.quizzes.size)
		assertEquals(existing.size + 1, reloaded.quizCount)
		assertEquals(added.id, reloaded.quizzes.single { it.sentence == NEW_SENTENCE }.id)
	}

	/** 시드(data.sql)에서 퀴즈가 2개 이상인 퀴즈셋 중 id 가 가장 작은 것 */
	private fun quizSetWithQuizzes(): QuizSet {
		val id = quizSetRepository.findAll()
			.filter { it.quizCount >= 2 }
			.minOf { it.id!! }
		return requireNotNull(quizSetRepository.findWithCategoryAndQuizzesById(id))
	}

	private fun Quiz.toRequest() = QuizRequest(
		id = id,
		sentence = sentence,
		answers = answers,
		hint = hint,
		orderIndex = orderIndex,
	)

	companion object {
		private const val NEW_SENTENCE = "Integration {{}} sentence added on update."
		private const val NEW_ANSWER = "test"
	}
}
