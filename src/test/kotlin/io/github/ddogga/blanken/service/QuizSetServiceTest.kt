package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.Category
import io.github.ddogga.blanken.domain.QuizSet
import io.github.ddogga.blanken.domain.User
import io.github.ddogga.blanken.domain.Visibility
import io.github.ddogga.blanken.dto.quiz.QuizSetCreateRequest
import io.github.ddogga.blanken.dto.quiz.QuizSetUpdateRequest
import io.github.ddogga.blanken.exception.CategoryNotFoundException
import io.github.ddogga.blanken.exception.ErrorCode
import io.github.ddogga.blanken.exception.QuizSetNotFoundExceptions
import io.github.ddogga.blanken.exception.QuizSetTitleDuplicationException
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
 * 소유자/카테고리/퀴즈셋 조회 실패의 도메인 예외 변환, 그리고 조회한 카테고리를 퀴즈셋에 연결·교체하는 것.
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
		val category = category(CATEGORY_ID_1, "토익")
		val savedQuizSet = slot<QuizSet>()

		every { userRepository.findById(OWNER_ID) } returns Optional.of(owner)
        every { quizSetRepository.existsByOwnerIdAndTitle(OWNER_ID, TITLE) } returns false
        every { categoryRepository.findById(CATEGORY_ID_1) } returns Optional.of(category)
		every { quizSetRepository.save(capture(savedQuizSet)) } returns quizSet(owner, category)

		// when
		val response = quizSetService.create(
			QuizSetCreateRequest(
				ownerId = OWNER_ID,
				title = TITLE,
				description = DESCRIPTION,
				visibility = Visibility.PUBLIC,
				categoryId = CATEGORY_ID_1,
			)
		)

		// then
		assertEquals(TITLE, response.title)
		assertEquals(DESCRIPTION, response.description)
		assertEquals(OWNER_ID, response.ownerId)
		assertEquals(NICKNAME, response.ownerNickname)
		assertEquals("토익", response.category.name)

		// 조회한 카테고리가 저장 대상 엔티티에 실제로 연결됐는지
		assertEquals(CATEGORY_ID_1, savedQuizSet.captured.category.id)
	}

    @Test
    fun `이름_중복시_QUIZ_SET_TITLE_DUPLICATION_예외를_던진다`() {
        // given
        val owner = user()

        every { userRepository.findById(OWNER_ID) } returns Optional.of(owner)
        every { quizSetRepository.existsByOwnerIdAndTitle(OWNER_ID, TITLE)} returns true

        // when
        val exception = assertFailsWith<QuizSetTitleDuplicationException> {
            quizSetService.create(createRequest())
        }

        // then
        assertEquals(ErrorCode.QUIZ_SET_TITLE_DUPLICATION, exception.errorCode)
        assertEquals(TITLE, exception.title)
        verify(exactly = 0) { quizSetRepository.save(any()) }
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

	@Test
	fun `존재하지_않는_카테고리로_생성시_CATEGORY_NOT_FOUND_예외를_던진다`() {
		// given
        every { quizSetRepository.existsByOwnerIdAndTitle(OWNER_ID, TITLE) } returns false
		every { userRepository.findById(OWNER_ID) } returns Optional.of(user())
		every { categoryRepository.findById(MISSING_CATEGORY_ID) } returns Optional.empty()

		// when
		val exception = assertFailsWith<CategoryNotFoundException> {
			quizSetService.create(createRequest(categoryId = MISSING_CATEGORY_ID))
		}

		// then
		assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
		assertEquals(MISSING_CATEGORY_ID, exception.categoryId)
		verify(exactly = 0) { quizSetRepository.save(any()) }
	}



    @Test
    fun `퀴즈셋을_정상적으로_수정한다`()  {

        // given
        val owner = user()
        val category = category(CATEGORY_ID_1, "토익")
        val newCategory = category(CATEGORY_ID_3, "일상회화")

        every { quizSetRepository.existsByOwnerIdAndTitle(OWNER_ID, NEW_TITLE) } returns false
        every { categoryRepository.findById(CATEGORY_ID_3) } returns Optional.of(newCategory)
        every { quizSetRepository.findWithCategoryById(QUIZ_SET_ID) } returns quizSet(owner, category)

        // when
        val response = quizSetService.update(QUIZ_SET_ID, updateRequest())

        // then
        assertEquals(NEW_TITLE, response.title)
        assertEquals(NEW_DESCRIPTION, response.description)
        assertEquals(Visibility.PRIVATE, response.visibility)
        assertEquals("일상회화", response.category.name)

    }

	@Test
	fun `존재하지_않는_퀴즈셋_수정시_QUIZ_SET_NOT_FOUND_예외를_던진다`() {
		// given
		every { quizSetRepository.findWithCategoryById(MISSING_QUIZ_SET_ID) } returns null

		// when
		val exception = assertFailsWith<QuizSetNotFoundExceptions> {
			quizSetService.update(MISSING_QUIZ_SET_ID, updateRequest())
		}

		// then
		assertEquals(ErrorCode.QUIZ_SET_NOT_FOUND, exception.errorCode)
		assertEquals(MISSING_QUIZ_SET_ID, exception.quizSetId)
		verify(exactly = 0) { categoryRepository.findById(any()) }
	}

	private fun createRequest(
		categoryId: Long = CATEGORY_ID_1,
	) = QuizSetCreateRequest(
		ownerId = OWNER_ID,
		title = TITLE,
		description = DESCRIPTION,
		visibility = Visibility.PUBLIC,
		categoryId = categoryId,
	)

	private fun updateRequest(
		categoryId: Long = CATEGORY_ID_3,
	) = QuizSetUpdateRequest(
		title = NEW_TITLE,
		description = NEW_DESCRIPTION,
		visibility = Visibility.PRIVATE,
		categoryId = categoryId,
	)

	private fun user(): User =
		User(email = EMAIL, password = "hashed", nickname = NICKNAME, id = OWNER_ID).apply {
			createdAt = CREATED_AT
			updatedAt = CREATED_AT
		}

	private fun category(id: Long, name: String): Category = Category(name = name, id = id)

	/** `save` 가 돌려주는 엔티티. 실제 DB 가 없으므로 id 를 직접 넣어 만든다. */
	private fun quizSet(owner: User, category: Category): QuizSet =
		QuizSet(
			owner = owner,
			category = category,
			title = TITLE,
			description = DESCRIPTION,
			visibility = Visibility.PUBLIC,
			id = QUIZ_SET_ID,
		).apply {
			createdAt = CREATED_AT
			updatedAt = CREATED_AT
		}

	companion object {
		private const val QUIZ_SET_ID = 1L
		private const val MISSING_QUIZ_SET_ID = 99L
		private const val OWNER_ID = 1L
		private const val CATEGORY_ID_1 = 1L
        private const val CATEGORY_ID_3 = 3L
		private const val MISSING_CATEGORY_ID = 7L
		private const val EMAIL = "owner@blanken.io"
		private const val NICKNAME = "blanken"
		private const val TITLE = "토익 빈출 동사"
		private const val DESCRIPTION = "30선"
		private val CREATED_AT: Instant = Instant.parse("2026-01-02T00:00:00Z")
        private const val NEW_TITLE = "토플 빈출 구동사"
        private const val NEW_DESCRIPTION = "100선"
	}
}
