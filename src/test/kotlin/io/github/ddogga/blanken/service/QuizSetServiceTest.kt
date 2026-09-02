package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.Category
import io.github.ddogga.blanken.domain.QuizSet
import io.github.ddogga.blanken.domain.User
import io.github.ddogga.blanken.domain.Visibility
import io.github.ddogga.blanken.dto.quiz.QuizSetCreateRequest
import io.github.ddogga.blanken.exception.CategoryNotFoundException
import io.github.ddogga.blanken.exception.ErrorCode
import io.github.ddogga.blanken.exception.UserNotFoundException
import io.github.ddogga.blanken.repository.CategoryRepository
import io.github.ddogga.blanken.repository.QuizSetRepository
import io.github.ddogga.blanken.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * `QuizSetService` 단위 테스트.
 *
 * 저장소는 목으로 끊고 서비스가 책임지는 분기만 본다 —
 * 소유자/카테고리 조회 실패의 도메인 예외 변환, 그리고 조회한 카테고리를 퀴즈셋에 연결하는 것.
 */
class QuizSetServiceTest {

	private val quizSetRepository = mockk<QuizSetRepository>()
	private val userRepository = mockk<UserRepository>()
	private val categoryRepository = mockk<CategoryRepository>()
	private val quizSetService = QuizSetService(quizSetRepository, userRepository, categoryRepository)

	@Test
	fun `퀴즈셋을_정상적으로_생성한다`() {
		// given
		val owner = user()
		val categories = listOf(category(CATEGORY_ID_1, "토익"), category(CATEGORY_ID_2, "비즈니스"))
		val savedQuizSet = slot<QuizSet>()

		every { userRepository.findById(OWNER_ID) } returns Optional.of(owner)
		every { categoryRepository.findAllById(setOf(CATEGORY_ID_1, CATEGORY_ID_2)) } returns categories
		every { quizSetRepository.save(capture(savedQuizSet)) } returns quizSet(owner, categories)

		// when
		val response = quizSetService.create(
			QuizSetCreateRequest(
				ownerId = OWNER_ID,
				title = TITLE,
				description = DESCRIPTION,
				visibility = Visibility.PUBLIC,
				categoryIds = listOf(CATEGORY_ID_1, CATEGORY_ID_2),
			)
		)

		// then
		assertEquals(TITLE, response.title)
		assertEquals(DESCRIPTION, response.description)
		assertEquals(OWNER_ID, response.ownerId)
		assertEquals(NICKNAME, response.ownerNickname)
		assertEquals(listOf("토익", "비즈니스"), response.categories.map { it.name })

		// 조회한 카테고리가 저장 대상 엔티티에 실제로 연결됐는지
		assertEquals(
			listOf(CATEGORY_ID_1, CATEGORY_ID_2),
			savedQuizSet.captured.categories.map { it.category.id },
		)
	}

	@Test
	fun `존재하지_않는_유저로_생성시_USER_NOT_FOUND_예외를_던진다`() {
		// given
		every { userRepository.findById(OWNER_ID) } returns Optional.empty()

		// when
		val exception = assertFailsWith<UserNotFoundException> {
			quizSetService.create(createRequest())
		}

		// then
		assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
		assertEquals(OWNER_ID, exception.userId)
		verify(exactly = 0) { quizSetRepository.save(any()) }
	}

	/**
	 * `findAllById` 는 없는 id 를 조용히 빼고 돌려준다.
	 * 요청한 id 집합과의 차집합으로 **누락된 id 만** 골라내는지가 관심사다.
	 */
	@Test
	fun `존재하지_않는_카테고리로_생성시_CATEGORY_NOT_FOUND_예외를_던진다`() {
		// given — 1 만 있고 7, 9 는 없다
		val requestedIds = listOf(CATEGORY_ID_1, MISSING_CATEGORY_ID_1, MISSING_CATEGORY_ID_2)
		every { userRepository.findById(OWNER_ID) } returns Optional.of(user())
		every { categoryRepository.findAllById(requestedIds.toSet()) } returns
			listOf(category(CATEGORY_ID_1, "토익"))

		// when
		val exception = assertFailsWith<CategoryNotFoundException> {
			quizSetService.create(createRequest(categoryIds = requestedIds))
		}

		// then
		assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
		assertEquals(
			listOf(MISSING_CATEGORY_ID_1, MISSING_CATEGORY_ID_2),
			exception.categoryIds.sorted(),
		)
		verify(exactly = 0) { quizSetRepository.save(any()) }
	}

	private fun createRequest(
		categoryIds: List<Long> = listOf(CATEGORY_ID_1),
	) = QuizSetCreateRequest(
		ownerId = OWNER_ID,
		title = TITLE,
		description = DESCRIPTION,
		visibility = Visibility.PUBLIC,
		categoryIds = categoryIds,
	)

	private fun user(): User =
		User(email = EMAIL, password = "hashed", nickname = NICKNAME, id = OWNER_ID).apply {
			createdAt = CREATED_AT
			updatedAt = CREATED_AT
		}

	private fun category(id: Long, name: String): Category = Category(name = name, id = id)

	/** `save` 가 돌려주는 엔티티. 실제 DB 가 없으므로 id 를 직접 넣어 만든다. */
	private fun quizSet(owner: User, categories: List<Category>): QuizSet =
		QuizSet(
			owner = owner,
			title = TITLE,
			description = DESCRIPTION,
			visibility = Visibility.PUBLIC,
			id = QUIZ_SET_ID,
		).apply {
			createdAt = CREATED_AT
			updatedAt = CREATED_AT
			categories.forEach { addCategory(it) }
		}

	companion object {
		private const val QUIZ_SET_ID = 1L
		private const val OWNER_ID = 1L
		private const val CATEGORY_ID_1 = 1L
		private const val CATEGORY_ID_2 = 3L
		private const val MISSING_CATEGORY_ID_1 = 7L
		private const val MISSING_CATEGORY_ID_2 = 9L
		private const val EMAIL = "owner@blanken.io"
		private const val NICKNAME = "blanken"
		private const val TITLE = "토익 빈출 동사"
		private const val DESCRIPTION = "30선"
		private val CREATED_AT: Instant = Instant.parse("2026-01-02T00:00:00Z")
	}
}
