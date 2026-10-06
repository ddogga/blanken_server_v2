package io.github.ddogga.blanken.service

import io.github.ddogga.blanken.domain.OauthProvider
import io.github.ddogga.blanken.domain.user.User
import io.github.ddogga.blanken.domain.user.UserRole
import io.github.ddogga.blanken.domain.user.UserStatus
import io.github.ddogga.blanken.dto.auth.Oauth2UserInfo
import io.github.ddogga.blanken.dto.common.PageResponse
import io.github.ddogga.blanken.dto.user.LoginUser
import io.github.ddogga.blanken.dto.user.SignupRequest
import io.github.ddogga.blanken.dto.user.SignupResponse
import io.github.ddogga.blanken.dto.user.SignupResult
import io.github.ddogga.blanken.dto.user.UserResponse
import io.github.ddogga.blanken.dto.user.UserUpdateRequest
import io.github.ddogga.blanken.exception.UserNotFoundException
import io.github.ddogga.blanken.repository.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(readOnly = true)
class UserService(
	private val userRepository: UserRepository,
	private val passwordEncoder: PasswordEncoder,
) {


    /**
     * 일반 회원가입은 지원하지 않는다. 소셜 회원가입만 허용
     */
//	@Transactional
//	fun create(request: UserCreateRequest): UserResponse {
//		if (userRepository.existsByEmail(request.email)) {
//			throw DuplicateEmailException(request.email)
//		}
//
//		val user = User(
//			email = request.email,
//			password = encode(request.password),
//			nickname = request.nickname,
//            null,
//            null,
//            userStatus = UserStatus.ACTIVE,
//            userRole = UserRole.USER,
//		)
//
//		return try {
//			UserResponse.from(userRepository.saveAndFlush(user))
//		} catch (ex: DataIntegrityViolationException) {
//			throw DuplicateEmailException(request.email)
//		}
//	}

    /**
     * PENDING/GUEST 회원을 ACTIVE/USER로 전환
     * 토큰 발급은 하지 않음. -> 트랜잭션이 커밋된 뒤 컨트롤러에서 발급 -> DB와 토큰 내용 불일치 방지
     */
    @Transactional
    fun signup(userId: Long, request: SignupRequest): SignupResponse {

        val user = userRepository.findByIdOrNull(userId)
            ?: return SignupResponse.from(null, null, SignupResult.USER_NOT_FOUND)

        if (user.userStatus != UserStatus.PENDING) {
            return SignupResponse.from(user.id, user.userRole, SignupResult.ALREADY_COMPLETED)
        }

        user.completeSignup(
            nickname = request.nickname.trim(),
            marketingAgreed = request.agreeMarketing,
            termsAgreedAt = Instant.now()
        )

        return SignupResponse.from(
            userId = user.id,
            userRole = user.userRole,
            signupResult = SignupResult.COMPLETED
        )
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

//	@Transactional
//	fun changePassword(id: Long, request: PasswordChangeRequest) {
//		val user = findUserOrThrow(id)
//		if (!passwordEncoder.matches(request.currentPassword, user.password)) {
//			throw InvalidPasswordException()
//		}
//		user.password = encode(request.newPassword)
//	}

	@Transactional
	fun delete(id: Long) {
		val user = findUserOrThrow(id)
        user.deleteUser()
	}

    fun findLoginUser(provider: OauthProvider, providerId: String): LoginUser? =
        userRepository.findByOauthProviderAndProviderId(provider, providerId)
            ?.let { LoginUser.from(it) }

    @Transactional
    fun socialRegister(userInfo: Oauth2UserInfo): LoginUser {

        val user = User(
            email = NEW_USER_EMAIL,
            nickname = NEW_USER_NAME,
            oauthProvider = userInfo.oauthProvider,
            providerId = userInfo.providerId,
            userStatus = UserStatus.PENDING,
            userRole = UserRole.GUEST,
        )

        return LoginUser.from(userRepository.save(user))

    }


	private fun findUserOrThrow(id: Long): User =
		userRepository.findById(id).orElseThrow { UserNotFoundException(id) }

	/**
	 * PasswordEncoder.encode() 는 입력이 null 일 때 null 을 돌려주도록 선언돼 있어 반환 타입이 `String?` 이다.
	 * user.password의 type은 String이고, null이 아님이 검증된 입력값을 쓰므로 별도의 메서드를 구현한다.
	 */
	private fun encode(rawPassword: String): String =
		requireNotNull(passwordEncoder.encode(rawPassword)) { "비밀번호 해싱에 실패했습니다." }


    companion object {
        private const val NEW_USER_NAME = "새로운 유저"
        private const val NEW_USER_EMAIL = "newbie@blanken.com"
    }
}
