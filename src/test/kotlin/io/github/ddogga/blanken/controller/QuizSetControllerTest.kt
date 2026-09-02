package io.github.ddogga.blanken.controller

import com.ninjasquad.springmockk.MockkBean
import io.github.ddogga.blanken.domain.Visibility
import io.github.ddogga.blanken.dto.category.CategoryResponse
import io.github.ddogga.blanken.dto.quiz.QuizSetResponse
import io.github.ddogga.blanken.exception.CategoryNotFoundException
import io.github.ddogga.blanken.exception.UserNotFoundException
import io.github.ddogga.blanken.service.QuizSetService
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant

/**
 * `QuizSetController` 슬라이스 테스트.
 *
 * 서비스는 목으로 끊고 HTTP 계약만 본다 — 상태 코드, `Location` 헤더, 도메인 예외 → `ErrorCode` 변환, 응답 JSON 구조.
 */
@WebMvcTest(QuizSetController::class)
class QuizSetControllerTest(
	@Autowired private val mockMvc: MockMvc,
) {

	@MockkBean
	private lateinit var quizSetService: QuizSetService

	@Test
	fun `201_퀴즈셋_생성_성공`() {
		// given
		every { quizSetService.create(any()) } returns quizSetResponse()

		// when & then
		mockMvc.post("/api/quiz-sets") {
			contentType = MediaType.APPLICATION_JSON
			content = REQUEST_BODY
		}.andExpect {
			status { isCreated() }
			header { string("Location", "/api/quiz-sets/$QUIZ_SET_ID") }
			jsonPath("$.id") { value(QUIZ_SET_ID) }
			jsonPath("$.title") { value(TITLE) }
			jsonPath("$.ownerNickname") { value(NICKNAME) }
			// 퀴즈셋 생성 플로우상 생성 직후에는 퀴즈가 없음.
			jsonPath("$.quizCount") { value(0) }
			jsonPath("$.likeCount") { value(0) }
			jsonPath("$.categories[0].name") { value("토익") }
		}
	}

	@Test
	fun `404_존재하지_않는_유저_퀴즈셋_생성_실패`() {
		// given
		every { quizSetService.create(any()) } throws UserNotFoundException(OWNER_ID)

		// when & then
		mockMvc.post("/api/quiz-sets") {
			contentType = MediaType.APPLICATION_JSON
			content = REQUEST_BODY
		}.andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("U001") }
			jsonPath("$.message") { value("유저를 찾을 수 없습니다.") }
		}
	}

	@Test
	fun `404_존재하지_않는_카테고리_퀴즈셋_생성_실패`() {
		// given
		every { quizSetService.create(any()) } throws CategoryNotFoundException(listOf(7L, 9L))

		// when & then — 어떤 id 가 없었는지는 로그로만 남고 응답에는 표준 메시지만 나간다.
		mockMvc.post("/api/quiz-sets") {
			contentType = MediaType.APPLICATION_JSON
			content = REQUEST_BODY
		}.andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("G001") }
			jsonPath("$.message") { value("카테고리를 찾을 수 없습니다.") }
		}
	}

	/** 카테고리 없는 퀴즈셋은 카테고리 필터 검색에 잡히지 않으므로 최소 1개를 요구한다. */
	@Test
	fun `400_퀴즈셋_생성_카테고리_미선택_실패`() {
		// when & then
		mockMvc.post("/api/quiz-sets") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"ownerId":$OWNER_ID,"title":"$TITLE","categoryIds":[]}"""
		}.andExpect {
			status { isBadRequest() }
			jsonPath("$.code") { value("C001") }
			jsonPath("$.fieldErrors[*].field") { value(org.hamcrest.Matchers.hasItem("categoryIds")) }
		}
	}

	/**
	 * 역직렬화 자체가 실패하는 경우. `@Valid` 는 돌기도 전이라 `fieldErrors` 가 없다.
	 * 핸들러가 없으면 스프링 기본 에러 바디가 나가 응답 형식이 둘로 갈라진다.
	 */
	@Test
	fun `400_퀴즈셋_생성_잘못된_형식의_요청_실패`() {
		// when & then — ownerId 누락 (Kotlin 논-널 필드)
		mockMvc.post("/api/quiz-sets") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"title":"$TITLE","categoryIds":[1]}"""
		}.andExpect {
			status { isBadRequest() }
			jsonPath("$.code") { value("C001") }
			jsonPath("$.fieldErrors") { doesNotExist() }
		}
	}

	private fun quizSetResponse(): QuizSetResponse = QuizSetResponse(
		id = QUIZ_SET_ID,
		ownerId = OWNER_ID,
		ownerNickname = NICKNAME,
		title = TITLE,
		description = "30선",
		visibility = Visibility.PUBLIC,
		likeCount = 0,
		quizCount = 0,
		categories = listOf(CategoryResponse(id = 1L, name = "토익")),
		createdAt = CREATED_AT,
		updatedAt = CREATED_AT,
	)

	companion object {
		private const val QUIZ_SET_ID = 1L
		private const val OWNER_ID = 1L
		private const val NICKNAME = "blanken"
		private const val TITLE = "토익 빈출 동사"
		private const val REQUEST_BODY =
			"""{"ownerId":1,"title":"토익 빈출 동사","description":"30선","visibility":"PUBLIC","categoryIds":[1]}"""
		private val CREATED_AT: Instant = Instant.parse("2026-09-02T00:00:00Z")
	}
}
