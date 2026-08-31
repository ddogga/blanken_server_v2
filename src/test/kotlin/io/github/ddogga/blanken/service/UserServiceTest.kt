package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.User
import io.github.ddogga.blanken.dto.user.PasswordChangeRequest
import io.github.ddogga.blanken.dto.user.UserCreateRequest
import io.github.ddogga.blanken.dto.user.UserUpdateRequest
import io.github.ddogga.blanken.exception.DuplicateEmailException
import io.github.ddogga.blanken.exception.ErrorCode
import io.github.ddogga.blanken.exception.InvalidPasswordException
import io.github.ddogga.blanken.exception.UserNotFoundException
import io.github.ddogga.blanken.repository.UserRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals


class UserServiceTest {

	private val userRepository = mockk<UserRepository>()
	private val passwordEncoder = mockk<PasswordEncoder>()
	private val userService = UserService(userRepository, passwordEncoder)

	// --- 생성 ---

	@Test
	fun `유저를_정상적으로_생성한다`() {
		// given
		val request = UserCreateRequest(email = EMAIL, password = RAW_PASSWORD, nickname = NICKNAME)
		val savedUser = slot<User>()
		every { userRepository.existsByEmail(EMAIL) } returns false
		every { passwordEncoder.encode(RAW_PASSWORD) } returns ENCODED_PASSWORD
		every { userRepository.saveAndFlush(capture(savedUser)) } returns user()

		// when
		val response = userService.create(request)

		// then
		assertEquals(EMAIL, response.email)
		assertEquals(NICKNAME, response.nickname)

		// 원문 비밀번호가 아니라 해시가 저장돼야 한다.
		assertEquals(ENCODED_PASSWORD, savedUser.captured.password)
		assertNotEquals(RAW_PASSWORD, savedUser.captured.password)
	}

	@Test
	fun `중복된_이메일로_가입시_DUPLICATE_EMAIL_예외를_던진다`() {
		// given
		val request = UserCreateRequest(email = EMAIL, password = RAW_PASSWORD, nickname = NICKNAME)
		every { userRepository.existsByEmail(EMAIL) } returns true

		// when
		val exception = assertFailsWith<DuplicateEmailException> { userService.create(request) }

		// then
		assertEquals(ErrorCode.DUPLICATE_EMAIL, exception.errorCode)
		assertEquals(EMAIL, exception.email)
		verify(exactly = 0) { userRepository.saveAndFlush(any()) }
	}


	@Test
	fun `이메일_UNIQUE_제약_위반시_DUPLICATE_EMAIL_예외로_변환한다`() {
		// given
		val request = UserCreateRequest(email = EMAIL, password = RAW_PASSWORD, nickname = NICKNAME)
		every { userRepository.existsByEmail(EMAIL) } returns false
		every { passwordEncoder.encode(RAW_PASSWORD) } returns ENCODED_PASSWORD
		every { userRepository.saveAndFlush(any()) } throws
			DataIntegrityViolationException("duplicate key value violates unique constraint")

		// when
		val exception = assertFailsWith<DuplicateEmailException> { userService.create(request) }

		// then
		assertEquals(ErrorCode.DUPLICATE_EMAIL, exception.errorCode)
	}

	// --- 닉네임 변경 ---

	@Test
	fun `닉네임을_변경한다`() {
		// given
		val user = user()
		every { userRepository.findById(USER_ID) } returns Optional.of(user)

		// when
		val response = userService.updateNickname(USER_ID, UserUpdateRequest(nickname = NEW_NICKNAME))

		// then — 더티체킹 대상 엔티티가 실제로 바뀌어야 한다.
		assertEquals(NEW_NICKNAME, user.nickname)
		assertEquals(NEW_NICKNAME, response.nickname)
	}

	@Test
	fun `존재하지_않는_유저의_닉네임_변경시_USER_NOT_FOUND_예외를_던진다`() {
		// given
		every { userRepository.findById(USER_ID) } returns Optional.empty()

		// when
		val exception = assertFailsWith<UserNotFoundException> {
			userService.updateNickname(USER_ID, UserUpdateRequest(nickname = NEW_NICKNAME))
		}

		// then
		assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
		assertEquals(USER_ID, exception.userId)
	}

	// --- 비밀번호 변경 ---

	@Test
	fun `비밀번호를_변경한다`() {
		// given
		val user = user()
		every { userRepository.findById(USER_ID) } returns Optional.of(user)
		every { passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD) } returns true
		every { passwordEncoder.encode(NEW_RAW_PASSWORD) } returns NEW_ENCODED_PASSWORD

		// when
		userService.changePassword(
			USER_ID,
			PasswordChangeRequest(currentPassword = RAW_PASSWORD, newPassword = NEW_RAW_PASSWORD),
		)

		// then
		assertEquals(NEW_ENCODED_PASSWORD, user.password)
	}

	@Test
	fun `현재_비밀번호가_일치하지_않으면_INVALID_PASSWORD_예외를_던진다`() {
		// given
		val user = user()
		every { userRepository.findById(USER_ID) } returns Optional.of(user)
		every { passwordEncoder.matches(WRONG_PASSWORD, ENCODED_PASSWORD) } returns false

		// when
		val exception = assertFailsWith<InvalidPasswordException> {
			userService.changePassword(
				USER_ID,
				PasswordChangeRequest(currentPassword = WRONG_PASSWORD, newPassword = NEW_RAW_PASSWORD),
			)
		}

		// then — 기존 비밀번호가 그대로 남아야 한다.
		assertEquals(ErrorCode.INVALID_PASSWORD, exception.errorCode)
		assertEquals(ENCODED_PASSWORD, user.password)
		verify(exactly = 0) { passwordEncoder.encode(NEW_RAW_PASSWORD) }
	}

	// --- 삭제 ---

	@Test
	fun `유저를_삭제한다`() {
		// given
		val user = user()
		every { userRepository.findById(USER_ID) } returns Optional.of(user)
		every { userRepository.delete(user) } just Runs

		// when
		userService.delete(USER_ID)

		// then
		verify(exactly = 1) { userRepository.delete(user) }
	}

	@Test
	fun `존재하지_않는_유저_삭제시_USER_NOT_FOUND_예외를_던진다`() {
		// given
		every { userRepository.findById(USER_ID) } returns Optional.empty()

		// when
		val exception = assertFailsWith<UserNotFoundException> { userService.delete(USER_ID) }

		// then
		assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
		verify(exactly = 0) { userRepository.delete(any()) }
	}

	/**
	 * `createdAt` 은 `lateinit` 이라 JPA Auditing 없이 만든 엔티티는 값이 비어 있다.
	 * `UserResponse.from` 이 이 값을 읽으므로 픽스처에서 채워 준다.
	 */
	private fun user(
		id: Long = USER_ID,
		email: String = EMAIL,
		password: String = ENCODED_PASSWORD,
		nickname: String = NICKNAME,
	): User = User(email = email, password = password, nickname = nickname, id = id).apply {
		createdAt = CREATED_AT
		updatedAt = CREATED_AT
	}

	companion object {
		private const val USER_ID = 1L
		private const val EMAIL = "learner@blanken.io"
		private const val NICKNAME = "blanken"
		private const val NEW_NICKNAME = "blanken2"
		private const val RAW_PASSWORD = "password1234"
		private const val WRONG_PASSWORD = "wrongpassword"
		private const val ENCODED_PASSWORD = "hashed-password1234"
		private const val NEW_RAW_PASSWORD = "newpassword1234"
		private const val NEW_ENCODED_PASSWORD = "hashed-newpassword1234"
		private val CREATED_AT: Instant = Instant.parse("2026-08-30T00:00:00Z")
	}
}
