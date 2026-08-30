package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.User
import io.github.ddogga.blanken.dto.common.PageResponse
import io.github.ddogga.blanken.dto.user.PasswordChangeRequest
import io.github.ddogga.blanken.dto.user.UserCreateRequest
import io.github.ddogga.blanken.dto.user.UserResponse
import io.github.ddogga.blanken.dto.user.UserUpdateRequest
import io.github.ddogga.blanken.exception.DuplicateEmailException
import io.github.ddogga.blanken.exception.InvalidPasswordException
import io.github.ddogga.blanken.exception.UserNotFoundException
import io.github.ddogga.blanken.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
	private val userRepository: UserRepository,
	private val passwordEncoder: PasswordEncoder,
) {

	@Transactional
	fun create(request: UserCreateRequest): UserResponse {
		if (userRepository.existsByEmail(request.email)) {
			throw DuplicateEmailException(request.email)
		}

		val user = User(
			email = request.email,
			password = encode(request.password),
			nickname = request.nickname,
		)

		return try {
			UserResponse.from(userRepository.saveAndFlush(user))
		} catch (e: DataIntegrityViolationException) {
			throw DuplicateEmailException(request.email)
		}
	}

	fun getById(id: Long): UserResponse = UserResponse.from(findUserOrThrow(id))

	fun getAll(pageable: Pageable): PageResponse<UserResponse> =
		PageResponse.from(userRepository.findAll(pageable), UserResponse::from)

	/** 닉네임만 변경한다. 이메일 변경은 인증,본인확인 정책이 정해진 뒤에 다룬다. */
	@Transactional
	fun updateNickname(id: Long, request: UserUpdateRequest): UserResponse {
		val user = findUserOrThrow(id)
		user.nickname = request.nickname
		return UserResponse.from(user)
	}

	@Transactional
	fun changePassword(id: Long, request: PasswordChangeRequest) {
		val user = findUserOrThrow(id)
		if (!passwordEncoder.matches(request.currentPassword, user.password)) {
			throw InvalidPasswordException()
		}
		user.password = encode(request.newPassword)
	}

	@Transactional
	fun delete(id: Long) {
		val user = findUserOrThrow(id)

		userRepository.delete(user)
	}

	private fun findUserOrThrow(id: Long): User =
		userRepository.findById(id).orElseThrow { UserNotFoundException(id) }

	/**
	 * PasswordEncoder.encode() 는 입력이 null 일 때 null 을 돌려주도록 선언돼 있어 반환 타입이 `String?` 이다.
	 * user.password의 type은 String이고, null이 아님이 검증된 입력값을 쓰므로 별도의 메서드를 구현한다.
	 */
	private fun encode(rawPassword: String): String =
		requireNotNull(passwordEncoder.encode(rawPassword)) { "비밀번호 해싱에 실패했습니다." }
}
