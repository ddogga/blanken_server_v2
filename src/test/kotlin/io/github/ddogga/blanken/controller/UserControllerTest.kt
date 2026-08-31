package io.github.ddogga.blanken.controller

import com.ninjasquad.springmockk.MockkBean
import io.github.ddogga.blanken.dto.common.PageResponse
import io.github.ddogga.blanken.dto.user.UserResponse
import io.github.ddogga.blanken.exception.DuplicateEmailException
import io.github.ddogga.blanken.exception.InvalidPasswordException
import io.github.ddogga.blanken.exception.UserNotFoundException
import io.github.ddogga.blanken.service.UserService
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.Instant

/**
 * `UserController` 슬라이스 테스트.
 *
 * 서비스는 목으로 끊고 **HTTP 계층의 계약만** 본다 — 상태 코드, `Location` 헤더,
 * 도메인 예외 → `ErrorCode` 변환, 응답 JSON 구조.
 * `@RestControllerAdvice` 인 `GlobalExceptionHandler` 는 `@WebMvcTest` 에 함께 로드되므로
 * 에러 응답도 실제 변환 경로를 그대로 탄다.
 */
@WebMvcTest(UserController::class)
class UserControllerTest(
	@Autowired private val mockMvc: MockMvc,
) {

	@MockkBean
	private lateinit var userService: UserService

	// --- 생성 ---

	@Test
	fun `201_유저_생성_성공`() {
		// given
		every { userService.create(any()) } returns userResponse()

		// when & then
		mockMvc.post("/api/users") {
			contentType = MediaType.APPLICATION_JSON
			content = """
				{"email":"$EMAIL","password":"$RAW_PASSWORD","nickname":"$NICKNAME"}
			""".trimIndent()
		}.andExpect {
			status { isCreated() }
			header { string("Location", "/api/users/$USER_ID") }
			jsonPath("$.id") { value(USER_ID) }
			jsonPath("$.email") { value(EMAIL) }
			jsonPath("$.nickname") { value(NICKNAME) }
			// 비밀번호는 해시라도 응답에 담기지 않는다.
			jsonPath("$.password") { doesNotExist() }
		}
	}

	@Test
	fun `409_중복_이메일_가입_실패`() {
		// given
		every { userService.create(any()) } throws DuplicateEmailException(EMAIL)

		// when & then
		mockMvc.post("/api/users") {
			contentType = MediaType.APPLICATION_JSON
			content = """
				{"email":"$EMAIL","password":"$RAW_PASSWORD","nickname":"$NICKNAME"}
			""".trimIndent()
		}.andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("U002") }
			// 응답에는 표준 메시지만 — 진단용 detail(email=...)은 로그로만 남는다.
			jsonPath("$.message") { value("이미 사용 중인 이메일입니다.") }
		}
	}

	/**
	 * `@Email` 이 동작하는지가 아니라, `GlobalExceptionHandler` 가
	 * `fieldErrors` 를 규약대로 조립하는지를 본다.
	 */
	@Test
	fun `400_유저생성_잘못된_형식의_요청_실패`() {
		// given — 이메일 형식 위반 + 비밀번호 길이 미달
		val invalidRequest = """
			{"email":"not-an-email","password":"short","nickname":"blanken"}
		""".trimIndent()

		// when & then
		mockMvc.post("/api/users") {
			contentType = MediaType.APPLICATION_JSON
			content = invalidRequest
		}.andExpect {
			status { isBadRequest() }
			jsonPath("$.code") { value("C001") }
			jsonPath("$.fieldErrors") { isNotEmpty() }
			jsonPath("$.fieldErrors[*].field") { value(org.hamcrest.Matchers.hasItem("email")) }
		}
	}

	// --- 조회 ---

	@Test
	fun `404_존재하지_않는_유저_조회_실패`() {
		// given
		every { userService.getById(USER_ID) } throws UserNotFoundException(USER_ID)

		// when & then
		mockMvc.get("/api/users/$USER_ID").andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("U001") }
			jsonPath("$.message") { value("유저를 찾을 수 없습니다.") }
		}
	}

	/**
	 * `Page` 를 그대로 직렬화하지 않고 `PageResponse` 로 감싸는 것은 API 규약이다.
	 * 안드로이드 클라이언트의 파싱이 이 필드명에 묶여 있어 구조를 고정한다.
	 */
	@Test
	fun `유저_목록을_PageResponse_구조로_반환한다`() {
		// given
		every { userService.getAll(any()) } returns PageResponse(
			content = listOf(userResponse()),
			page = 0,
			size = 20,
			totalElements = 1,
			totalPages = 1,
			first = true,
			last = true,
		)

		// when & then
		mockMvc.get("/api/users").andExpect {
			status { isOk() }
			jsonPath("$.content[0].id") { value(USER_ID) }
			jsonPath("$.page") { value(0) }
			jsonPath("$.size") { value(20) }
			jsonPath("$.totalElements") { value(1) }
			jsonPath("$.totalPages") { value(1) }
			jsonPath("$.first") { value(true) }
			jsonPath("$.last") { value(true) }
		}
	}

	// --- 수정 ---

	@Test
	fun `200_닉네임_변경_성공`() {
		// given
		every { userService.updateNickname(USER_ID, any()) } returns userResponse(nickname = NEW_NICKNAME)

		// when & then
		mockMvc.patch("/api/users/$USER_ID") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"nickname":"$NEW_NICKNAME"}"""
		}.andExpect {
			status { isOk() }
			jsonPath("$.nickname") { value(NEW_NICKNAME) }
		}
	}

	@Test
	fun `204_비밀번호_변경_성공`() {
		// given
		every { userService.changePassword(USER_ID, any()) } just Runs

		// when & then
		mockMvc.post("/api/users/$USER_ID/password") {
			contentType = MediaType.APPLICATION_JSON
			content = """
				{"currentPassword":"$RAW_PASSWORD","newPassword":"$NEW_RAW_PASSWORD"}
			""".trimIndent()
		}.andExpect {
			status { isNoContent() }
		}
	}

	@Test
	fun `400_비밀번호_변경_현재_비밀번호_불일치_실패`() {
		// given
		every { userService.changePassword(USER_ID, any()) } throws InvalidPasswordException()

		// when & then
		mockMvc.post("/api/users/$USER_ID/password") {
			contentType = MediaType.APPLICATION_JSON
			content = """
				{"currentPassword":"wrongpassword","newPassword":"$NEW_RAW_PASSWORD"}
			""".trimIndent()
		}.andExpect {
			status { isBadRequest() }
			jsonPath("$.code") { value("A001") }
			jsonPath("$.message") { value("현재 비밀번호가 일치하지 않습니다.") }
		}
	}

	// --- 삭제 ---

	@Test
	fun `204_유저_삭제_성공`() {
		// given
		every { userService.delete(USER_ID) } just Runs

		// when & then
		mockMvc.delete("/api/users/$USER_ID").andExpect {
			status { isNoContent() }
		}
	}

	private fun userResponse(
		id: Long = USER_ID,
		email: String = EMAIL,
		nickname: String = NICKNAME,
	): UserResponse = UserResponse(id = id, email = email, nickname = nickname, createdAt = CREATED_AT)

	companion object {
		private const val USER_ID = 1L
		private const val EMAIL = "learner@blanken.io"
		private const val NICKNAME = "blanken"
		private const val NEW_NICKNAME = "blanken2"
		private const val RAW_PASSWORD = "password1234"
		private const val NEW_RAW_PASSWORD = "newpassword1234"
		private val CREATED_AT: Instant = Instant.parse("2026-08-30T00:00:00Z")
	}
}
