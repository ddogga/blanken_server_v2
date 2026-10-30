package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.OauthProvider
import io.github.ddogga.blanken.domain.quiz.Category
import io.github.ddogga.blanken.domain.quiz.Quiz
import io.github.ddogga.blanken.domain.quiz.QuizSet
import io.github.ddogga.blanken.domain.user.User
import io.github.ddogga.blanken.domain.user.UserRole
import io.github.ddogga.blanken.domain.user.UserStatus
import io.github.ddogga.blanken.domain.quiz.Visibility
import io.github.ddogga.blanken.dto.quiz.QuizRequest
import io.github.ddogga.blanken.dto.quiz.QuizSetCreateRequest
import io.github.ddogga.blanken.dto.quiz.QuizSetUpdateRequest
import io.github.ddogga.blanken.exception.CategoryNotFoundException
import io.github.ddogga.blanken.exception.ErrorCode
import io.github.ddogga.blanken.exception.QuizSetNotFoundException
import io.github.ddogga.blanken.exception.QuizSetTitleDuplicationException
import io.github.ddogga.blanken.exception.UserNotFoundException
import io.github.ddogga.blanken.repository.CategoryRepository
import io.github.ddogga.blanken.repository.QuizSetRepository
import io.github.ddogga.blanken.repository.UserRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

/**
 * `QuizSetService` 단위 테스트.
 *
 * 저장소는 목으로 끊고 서비스가 책임지는 분기만 본다 —
 * 소유자/카테고리/퀴즈셋 조회 실패의 도메인 예외 변환, 그리고 조회한 카테고리를 퀴즈셋에 연결·교체하는 것.
 *
 * **수정 시 새 퀴즈 추가는 여기서 검증하지 않는다** → `QuizSetServiceIntegrationTest`.
 * 서비스가 flush 로 INSERT 를 실행해 IDENTITY id 를 받은 뒤 응답을 만드는데, 목 저장소의 flush 는
 * INSERT 를 하지 않아 새 퀴즈의 id 가 null 로 남고 응답 변환(`requireNotNull`)에서 실패한다.
 * "flush 하면 id 가 채워진다" 는 Hibernate·DB 의 동작이라 실제 DB 로만 검증할 수 있다.
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
        every { quizSetRepository.findWithCategoryAndQuizzesById(QUIZ_SET_ID) } returns quizSet(owner, category)
        every { quizSetRepository.flush() } just Runs

        // when
        val response = quizSetService.update(QUIZ_SET_ID, updateRequest())

        // then
        assertEquals(NEW_TITLE, response.title)
        assertEquals(NEW_DESCRIPTION, response.description)
        assertEquals(Visibility.PRIVATE, response.visibility)
        assertEquals("일상회화", response.category.name)

    }

	@Test
	fun `퀴즈셋_수정시_id가_있는_퀴즈는_기존_퀴즈를_수정한다`() {
		// given
		val quizSet = quizSet(user(), category(CATEGORY_ID_1, "토익"))
		val existing = quiz(id = QUIZ_ID_1, orderIndex = 0)
		quizSet.addQuiz(existing)

		givenUpdatable(quizSet)

		// when
		val response = quizSetService.update(
			QUIZ_SET_ID,
			updateRequest(quizzes = listOf(
				quizRequest(id = QUIZ_ID_1, sentence = NEW_SENTENCE, answers = listOf(NEW_ANSWER), hint = NEW_HINT, orderIndex = 0, translation = NEW_TRANSLATION),
			)),
		)

		// then
		// 새 엔티티로 갈아끼우지 않고 같은 영속 엔티티를 고쳐야 더티 체킹으로 UPDATE 된다
		assertEquals(1, quizSet.quizzes.size)
		assertSame(existing, quizSet.quizzes.single())
		assertEquals(NEW_SENTENCE, existing.sentence)
		assertEquals(listOf(NEW_ANSWER), existing.answers)
		assertEquals(NEW_HINT, existing.hint)
		assertEquals(NEW_TRANSLATION, existing.translation)
		assertEquals(1, response.quizCount)
	}

	@Test
	fun `퀴즈셋_수정시_요청에_없는_기존_퀴즈는_삭제한다`() {
		// given
		val quizSet = quizSet(user(), category(CATEGORY_ID_1, "토익"))
		val kept = quiz(id = QUIZ_ID_1, orderIndex = 0)
		val removed = quiz(id = QUIZ_ID_2, orderIndex = 1)
		quizSet.addQuiz(kept)
		quizSet.addQuiz(removed)

		givenUpdatable(quizSet)

		// when
		val response = quizSetService.update(
			QUIZ_SET_ID,
			updateRequest(quizzes = listOf(quizRequest(id = QUIZ_ID_1, orderIndex = 0))),
		)

		// then
		// 컬렉션에서 빠져야 orphanRemoval 로 커밋 시 DELETE 된다
		assertEquals(listOf(QUIZ_ID_1), quizSet.quizzes.map { it.id })
		assertEquals(1, quizSet.quizCount)
		assertEquals(1, response.quizCount)
	}


	@Test
	fun `존재하지_않는_퀴즈셋_수정시_QUIZ_SET_NOT_FOUND_예외를_던진다`() {
		// given
		every { quizSetRepository.findWithCategoryAndQuizzesById(MISSING_QUIZ_SET_ID) } returns null

		// when
		val exception = assertFailsWith<QuizSetNotFoundException> {
			quizSetService.update(MISSING_QUIZ_SET_ID, updateRequest())
		}

		// then
		assertEquals(ErrorCode.QUIZ_SET_NOT_FOUND, exception.errorCode)
		assertEquals(MISSING_QUIZ_SET_ID, exception.quizSetId)
		verify(exactly = 0) { categoryRepository.findById(any()) }
	}

	/**
	 * 수정 성공 경로의 공통 목 — 퀴즈셋 조회, 제목 중복 없음, 새 카테고리 조회, flush.
	 * flush 는 목이라 실제 INSERT 가 일어나지 않는다 → 새로 추가한 퀴즈의 id 는 null 로 남는다.
	 */
	private fun givenUpdatable(quizSet: QuizSet) {
		every { quizSetRepository.findWithCategoryAndQuizzesById(QUIZ_SET_ID) } returns quizSet
		every { quizSetRepository.existsByOwnerIdAndTitle(OWNER_ID, NEW_TITLE) } returns false
		every { categoryRepository.findById(CATEGORY_ID_3) } returns Optional.of(category(CATEGORY_ID_3, "일상회화"))
		every { quizSetRepository.flush() } just Runs
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
		quizzes: List<QuizRequest> = emptyList(),
	) = QuizSetUpdateRequest(
		title = NEW_TITLE,
		description = NEW_DESCRIPTION,
		visibility = Visibility.PRIVATE,
		categoryId = categoryId,
		quizzes = quizzes,
	)

	private fun quizRequest(
		id: Long?,
		sentence: String = SENTENCE,
		answers: List<String> = listOf(ANSWER),
		hint: String? = null,
		orderIndex: Int,
		translation: String = TRANSLATION,
	) = QuizRequest(
		id = id,
		sentence = sentence,
		answers = answers,
		hint = hint,
		orderIndex = orderIndex,
		translation = translation,
	)

	/** 영속 상태의 기존 퀴즈를 흉내 낸다. 실제 DB 가 없으므로 id 를 직접 넣는다. */
	private fun quiz(id: Long, orderIndex: Int): Quiz =
		Quiz(
			sentence = SENTENCE,
			answers = listOf(ANSWER),
			orderIndex = orderIndex,
			translation = TRANSLATION,
			id = id,
		)

	private fun user(): User =
		User(email = EMAIL,
            nickname = NICKNAME,
            oauthProvider = OauthProvider.KAKAO,
            providerId = PROVIDER_ID,
            UserStatus.ACTIVE,
            UserRole.USER,
            id = OWNER_ID).apply {
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
        private const val PROVIDER_ID = "122"
		private const val QUIZ_ID_1 = 10L
		private const val QUIZ_ID_2 = 11L
		private const val SENTENCE = "She decided to {{}} the meeting until next week."
		private const val ANSWER = "postpone"
		private const val NEW_SENTENCE = "Please {{}} the attached document before Friday."
		private const val NEW_ANSWER = "review"
		private const val NEW_HINT = "검토하다"
		private const val TRANSLATION = "그녀는 회의를 다음 주로 미루기로 했다."
		private const val NEW_TRANSLATION = "금요일 전에 첨부 문서를 검토해 주세요."
	}
}
